package com.healmate

import com.healmate.data.model.Appointment
import com.healmate.data.model.AppointmentStatus
import com.healmate.data.model.HealthRecord
import com.healmate.data.model.Medication
import com.healmate.data.model.Mood
import com.healmate.data.model.ReminderStatus
import com.healmate.data.remote.AccountApi
import com.healmate.data.remote.HealthApi
import com.healmate.data.repository.RemoteHealthRepository
import com.healmate.data.repository.SessionGate
import com.healmate.data.util.DateText
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class RemoteHealthRepositoryTest {
    private val medId = "11111111-1111-1111-1111-111111111111"
    private val reminderId = "22222222-2222-2222-2222-222222222222"
    private val appointmentId = "33333333-3333-3333-3333-333333333333"

    // "Ahora" fijo: 12 de noviembre de 2026 a las 10:00, hora local.
    private val now: Calendar = Calendar.getInstance().apply {
        clear()
        set(2026, Calendar.NOVEMBER, 12, 10, 0)
    }

    private fun medication(active: Boolean = true, end: String? = null) = """
        {"id":"$medId","recordatorio_id":"$reminderId","nombre":"Paracetamol","dosis":"500mg",
        "frecuencia":"Cada 8 horas","via_administracion":"Oral","hora":"08:00","fecha_inicio":"2026-11-01",
        "fecha_fin":${end?.let { "\"$it\"" }},"activo":$active,"proposito":"Dolor"}
    """.trimIndent()

    private fun dose(state: String, minutes: Int = 0) =
        """{"recordatorio_id":"$reminderId","fecha_programada":"2026-11-12T14:00:00Z","estado":"$state","pospuesto_minutos":$minutes}"""

    private fun appointment(state: String, at: Calendar) = """
        {"id":"$appointmentId","especialista":"Dra. García","especialidad":"Cardiología","lugar":"Clínica",
        "fecha_hora":"${DateText.isoUtc(at.time)}","estado":"$state","notas":"Revisión"}
    """.trimIndent()

    private val record = """
        {"id":"44444444-4444-4444-4444-444444444444","fecha_registro":"${DateText.isoUtc(now.time)}","dolor":3,
        "estado_animo":"feliz","presion_sistolica":120,"presion_diastolica":80,"glucosa":95.5,"peso":68.5,
        "pulso":78,"notas":"Bien"}
    """.trimIndent()

    private fun later(hours: Int) = (now.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, hours) }

    private class Server(val handler: (HttpRequestData) -> Pair<HttpStatusCode, String>?) {
        val requests = mutableListOf<HttpRequestData>()
        val engine = MockEngine { request ->
            requests += request
            assertEquals("Bearer token", request.headers[HttpHeaders.Authorization])
            val (status, body) = handler(request) ?: (HttpStatusCode.OK to "[]")
            if (status.value >= 400) respondError(status) else respond(body, status)
        }

        fun body(index: Int): JsonObject {
            val content = requests[index].body as OutgoingContent.ByteArrayContent
            return Json.parseToJsonElement(content.bytes().decodeToString()).jsonObject
        }

        fun sent(method: String, path: String) =
            requests.indexOfLast { it.method.value == method && it.url.encodedPath == path }
    }

    /** Ejecuta la acción y espera a que termine su petición al servidor. */
    private fun RemoteHealthRepository.await(action: RemoteHealthRepository.() -> Unit) {
        action()
        runBlocking { scopeJobs().forEach { it.join() } }
    }

    private var scopes = mutableMapOf<RemoteHealthRepository, CoroutineScope>()
    private fun RemoteHealthRepository.scopeJobs() = scopes.getValue(this).coroutineContext.job.children.toList()

    private fun started(server: Server): RemoteHealthRepository {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val gate = object : SessionGate {
            override suspend fun <T> withToken(block: suspend (String) -> T): T = block("token")
        }
        val repo = RemoteHealthRepository(
            HealthApi(AccountApi("https://api.example.com", HttpClient(server.engine))), gate,
            { now.clone() as Calendar }, scope
        )
        scopes[repo] = scope
        repo.await { refresh() }
        return repo
    }

    @Test
    fun loadsAccountDataAndBuildsTodaysDoses() {
        val server = Server { request ->
            when (request.url.encodedPath) {
                "/medicamentos" -> HttpStatusCode.OK to "[${medication()}]"
                "/medicamentos/dosis" -> HttpStatusCode.OK to "[${dose("pospuesto", 15)}]"
                "/citas" -> HttpStatusCode.OK to "[${appointment("confirmada", later(5))}]"
                "/salud/registros" -> HttpStatusCode.OK to "[$record]"
                else -> null
            }
        }
        val repo = started(server)

        val med = repo.medications.value.single()
        assertEquals("Paracetamol", med.nombre)
        assertEquals("08:00", med.proximaHora)
        val reminder = repo.reminders.value.single()
        assertEquals(med.id, reminder.medicationId)
        assertEquals(ReminderStatus.POSPUESTO, reminder.estado)
        assertEquals(15, reminder.pospuestoMinutos)
        assertEquals("12 Nov 2026", reminder.fecha)

        // Una cita confirmada dentro de las próximas 24 horas muestra su recordatorio (RF-04).
        val shown = repo.appointments.value.single()
        assertEquals(AppointmentStatus.RECORDATORIO, shown.estado)
        assertEquals("12 Nov 2026 - 15:00", shown.fechaHora)

        val health = repo.healthRecords.value.single()
        assertEquals("120/80", health.presionArterial)
        assertEquals(Mood.FELIZ, health.estadoAnimo)
        assertEquals(95.5, health.glucosa, 0.0)

        // Las dosis se piden para el día local completo.
        val query = server.requests[server.sent("GET", "/medicamentos/dosis")].url.parameters
        assertEquals(DateText.isoUtc(DateText.startOfDay(now).time), query["desde"])
        assertNull(repo.syncError.value)
        assertFalse(repo.syncPending.value)
    }

    @Test
    fun pausedOrFinishedMedicationsCreateNoDoses() {
        for (body in listOf(medication(active = false), medication(end = "2026-11-11"))) {
            val repo = started(Server { if (it.url.encodedPath == "/medicamentos") HttpStatusCode.OK to "[$body]" else null })
            assertEquals(1, repo.medications.value.size)
            assertTrue(repo.reminders.value.isEmpty())
        }
        val lastDay = started(Server {
            if (it.url.encodedPath == "/medicamentos") HttpStatusCode.OK to "[${medication(end = "2026-11-12")}]" else null
        })
        assertEquals(1, lastDay.reminders.value.size)
    }

    @Test
    fun savesMedicationAndRecordsItsDose() {
        val server = Server { request ->
            when (request.method.value to request.url.encodedPath) {
                "POST" to "/medicamentos" -> HttpStatusCode.Created to medication()
                "PUT" to "/medicamentos/dosis" -> HttpStatusCode.OK to dose("tomado")
                "PATCH" to "/medicamentos/$medId" -> HttpStatusCode.OK to medication(active = false)
                "DELETE" to "/medicamentos/$medId" -> HttpStatusCode.NoContent to ""
                else -> null
            }
        }
        val repo = started(server)

        repo.await {
            addMedication(Medication(7, "Paracetamol", "500mg", "Cada 8 horas", proximaHora = "8:00", fechaInicio = "", fechaFin = null))
        }
        val sent = server.body(server.sent("POST", "/medicamentos"))
        assertEquals("08:00", sent.getValue("hora").jsonPrimitive.content)
        assertEquals("2026-11-12", sent.getValue("fecha_inicio").jsonPrimitive.content) // sin fecha: hoy
        val reminder = repo.reminders.value.single()
        assertEquals(ReminderStatus.PENDIENTE, reminder.estado)

        repo.await { updateReminderStatus(reminder.id, ReminderStatus.TOMADO) }
        val doseSent = server.body(server.sent("PUT", "/medicamentos/dosis"))
        assertEquals(reminderId, doseSent.getValue("recordatorio_id").jsonPrimitive.content)
        assertEquals("tomado", doseSent.getValue("estado").jsonPrimitive.content)
        assertEquals(
            DateText.isoUtc(DateText.atTime(now, "08:00")!!.time),
            doseSent.getValue("fecha_programada").jsonPrimitive.content
        )
        assertEquals(ReminderStatus.TOMADO, repo.reminders.value.single().estado)

        repo.await { toggleMedicationActive(reminder.medicationId) }
        assertFalse(repo.medications.value.single().activo)
        assertTrue(repo.reminders.value.isEmpty())

        repo.await { deleteMedication(reminder.medicationId) }
        assertTrue(repo.medications.value.isEmpty())
        assertNull(repo.syncError.value)
    }

    @Test
    fun snoozeAccumulatesMinutes() {
        var snoozed = 0
        val server = Server { request ->
            when (request.method.value to request.url.encodedPath) {
                "GET" to "/medicamentos" -> HttpStatusCode.OK to "[${medication()}]"
                "PUT" to "/medicamentos/dosis" -> {
                    snoozed += 15
                    HttpStatusCode.OK to dose("pospuesto", snoozed)
                }
                else -> null
            }
        }
        val repo = started(server)
        val id = repo.reminders.value.single().id
        repo.await { snoozeReminder(id, 15) }
        repo.await { snoozeReminder(id, 15) }
        assertEquals(30, server.body(server.sent("PUT", "/medicamentos/dosis")).getValue("pospuesto_minutos").jsonPrimitive.content.toInt())
        assertEquals(30, repo.reminders.value.single().pospuestoMinutos)
    }

    @Test
    fun nothingChangesOnScreenWhenTheServerRejectsOrIsUnreachable() {
        val rejected = started(Server { request ->
            when (request.method.value to request.url.encodedPath) {
                "GET" to "/medicamentos" -> HttpStatusCode.OK to "[${medication()}]"
                "GET" to "/medicamentos/dosis" -> HttpStatusCode.OK to "[${dose("tomado")}]"
                "PUT" to "/medicamentos/dosis" -> HttpStatusCode.Conflict to ""
                "POST" to "/salud/registros" -> HttpStatusCode.UnprocessableEntity to ""
                else -> null
            }
        })
        // Invariante 7: una dosis ya tomada no cambia.
        rejected.await { updateReminderStatus(rejected.reminders.value.single().id, ReminderStatus.OMITIDO) }
        assertEquals(ReminderStatus.TOMADO, rejected.reminders.value.single().estado)
        assertEquals("Esa dosis ya quedó registrada y no se puede cambiar.", rejected.syncError.value)
        rejected.clearSyncError()

        rejected.await {
            addHealthRecord(HealthRecord(fecha = "12 Nov 2026", dolorEscala = 3, estadoAnimo = Mood.FELIZ, presionArterial = "120/80", glucosa = 95.0, peso = 68.5, pulso = 78))
        }
        assertTrue(rejected.healthRecords.value.isEmpty())
        assertNotNull(rejected.syncError.value)
        assertFalse(rejected.syncPending.value)

        val offline = started(Server { throw java.io.IOException("sin red") })
        assertTrue(offline.isOfflineMode.value)
        assertEquals("Sin conexión. No se pudieron cargar tus datos.", offline.syncError.value)
    }

    @Test
    fun appointmentsMustBeValidAndInTheFuture() {
        val server = Server { request ->
            when (request.method.value to request.url.encodedPath) {
                "POST" to "/citas" -> HttpStatusCode.Created to appointment("confirmada", later(72))
                "PATCH" to "/citas/$appointmentId" -> HttpStatusCode.OK to appointment("cancelada", later(72))
                else -> null
            }
        }
        val repo = started(server)
        val draft = Appointment(1, "Dra. García", "Cardiología", "15 Nov 2026 - 10:00", "Clínica", notas = "Revisión")

        // Invariante 8: una cita pasada no se envía al servidor.
        repo.await { addAppointment(draft.copy(fechaHora = "12 Nov 2026 - 09:00")) }
        assertEquals("La cita debe ser en una fecha y hora futuras.", repo.syncError.value)
        repo.clearSyncError()
        repo.await { addAppointment(draft.copy(fechaHora = "15 Nov 2026 - mañana")) }
        assertNotNull(repo.syncError.value)
        repo.clearSyncError()
        assertEquals(-1, server.sent("POST", "/citas"))

        repo.await { addAppointment(draft) }
        val sent = server.body(server.sent("POST", "/citas"))
        assertEquals(
            DateText.isoUtc(DateText.parseAppointment("15 Nov 2026 - 10:00")!!.time),
            sent.getValue("fecha_hora").jsonPrimitive.content
        )
        val saved = repo.appointments.value.single()
        assertEquals(AppointmentStatus.CONFIRMADA, saved.estado)

        repo.await { cancelAppointment(saved.id) }
        assertEquals(AppointmentStatus.CANCELADA, repo.appointments.value.single().estado)
        assertNull(repo.syncError.value)
    }

    @Test
    fun savesHealthRecordWithSplitBloodPressure() {
        val server = Server { request ->
            if (request.method.value == "POST" && request.url.encodedPath == "/salud/registros") HttpStatusCode.Created to record
            else null
        }
        val repo = started(server)
        repo.await {
            addHealthRecord(HealthRecord(fecha = "12 Nov 2026", dolorEscala = 3, estadoAnimo = Mood.FELIZ, presionArterial = " 120 / 80 ", glucosa = 95.5, peso = 68.5, pulso = 78, notas = "Bien"))
        }
        val sent = server.body(server.sent("POST", "/salud/registros"))
        assertEquals("120", sent.getValue("presion_sistolica").jsonPrimitive.content)
        assertEquals("80", sent.getValue("presion_diastolica").jsonPrimitive.content)
        assertEquals("feliz", sent.getValue("estado_animo").jsonPrimitive.content)
        assertEquals("12 Nov 2026", repo.healthRecords.value.single().fecha)

        repo.await { addHealthRecord(repo.healthRecords.value.single().copy(presionArterial = "120")) }
        assertNotNull(repo.syncError.value)
        assertEquals(1, repo.healthRecords.value.size)
    }
}
