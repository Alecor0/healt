package com.healmate.data.remote

import io.ktor.http.HttpMethod
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Medicamento del servidor: identificadores UUID y hora "HH:mm". */
internal data class RemoteMedication(
    val id: String,
    val recordatorioId: String,
    val nombre: String,
    val dosis: String,
    val frecuencia: String,
    val viaAdministracion: String,
    val hora: String,
    val fechaInicio: String?,
    val fechaFin: String?,
    val activo: Boolean,
    val proposito: String
)

internal data class NewMedication(
    val nombre: String,
    val dosis: String,
    val frecuencia: String,
    val viaAdministracion: String,
    val hora: String,
    val fechaInicio: String,
    val fechaFin: String?,
    val proposito: String
)

/** Estado de una dosis programada; [fechaProgramada] es un momento ISO. */
internal data class RemoteDose(
    val recordatorioId: String,
    val fechaProgramada: String,
    val estado: String,
    val pospuestoMinutos: Int
)

internal data class RemoteAppointment(
    val id: String,
    val especialista: String,
    val especialidad: String,
    val lugar: String,
    val fechaHora: String,
    val estado: String,
    val notas: String
)

internal data class NewAppointment(
    val especialista: String,
    val especialidad: String,
    val lugar: String,
    val fechaHora: String,
    val notas: String
)

internal data class RemoteHealthRecord(
    val id: String,
    val fechaRegistro: String,
    val dolor: Int?,
    val estadoAnimo: String?,
    val presionSistolica: Int?,
    val presionDiastolica: Int?,
    val glucosa: Double?,
    val peso: Double?,
    val pulso: Int?,
    val notas: String
)

internal data class NewHealthRecord(
    val dolor: Int,
    val estadoAnimo: String,
    val presionSistolica: Int,
    val presionDiastolica: Int,
    val glucosa: Double,
    val peso: Double,
    val pulso: Int,
    val notas: String
)

/** Rutas /medicamentos, /citas y /salud. Cada llamada usa el token de acceso de la sesión. */
internal class HealthApi(private val api: AccountApi) {

    suspend fun medications(token: String): List<RemoteMedication> =
        api.request(HttpMethod.Get, "/medicamentos", token).jsonArray.map { it.jsonObject.toMedication() }

    suspend fun addMedication(token: String, medication: NewMedication): RemoteMedication =
        api.request(HttpMethod.Post, "/medicamentos", token, buildJsonObject {
            put("nombre", medication.nombre)
            put("dosis", medication.dosis)
            put("frecuencia", medication.frecuencia)
            put("via_administracion", medication.viaAdministracion)
            put("hora", medication.hora)
            put("fecha_inicio", medication.fechaInicio)
            put("fecha_fin", medication.fechaFin)
            put("proposito", medication.proposito)
        }).jsonObject.toMedication()

    suspend fun setMedicationActive(token: String, id: String, active: Boolean): RemoteMedication =
        api.request(HttpMethod.Patch, "/medicamentos/$id", token, buildJsonObject { put("activo", active) })
            .jsonObject.toMedication()

    suspend fun deleteMedication(token: String, id: String) {
        api.request(HttpMethod.Delete, "/medicamentos/$id", token)
    }

    /** Dosis con hora programada en [from, until), ambos momentos ISO en UTC. */
    suspend fun doses(token: String, from: String, until: String): List<RemoteDose> =
        api.request(HttpMethod.Get, "/medicamentos/dosis", token, query = mapOf("desde" to from, "hasta" to until))
            .jsonArray.map { it.jsonObject.toDose() }

    suspend fun recordDose(token: String, dose: RemoteDose): RemoteDose =
        api.request(HttpMethod.Put, "/medicamentos/dosis", token, buildJsonObject {
            put("recordatorio_id", dose.recordatorioId)
            put("fecha_programada", dose.fechaProgramada)
            put("estado", dose.estado)
            put("pospuesto_minutos", dose.pospuestoMinutos)
        }).jsonObject.toDose()

    suspend fun appointments(token: String): List<RemoteAppointment> =
        api.request(HttpMethod.Get, "/citas", token).jsonArray.map { it.jsonObject.toAppointment() }

    suspend fun addAppointment(token: String, appointment: NewAppointment): RemoteAppointment =
        api.request(HttpMethod.Post, "/citas", token, buildJsonObject {
            put("especialista", appointment.especialista)
            put("especialidad", appointment.especialidad)
            put("lugar", appointment.lugar)
            put("fecha_hora", appointment.fechaHora)
            put("notas", appointment.notas)
        }).jsonObject.toAppointment()

    suspend fun cancelAppointment(token: String, id: String): RemoteAppointment =
        api.request(HttpMethod.Patch, "/citas/$id", token, buildJsonObject { put("estado", "cancelada") })
            .jsonObject.toAppointment()

    suspend fun healthRecords(token: String): List<RemoteHealthRecord> =
        api.request(HttpMethod.Get, "/salud/registros", token).jsonArray.map { it.jsonObject.toHealthRecord() }

    suspend fun addHealthRecord(token: String, record: NewHealthRecord): RemoteHealthRecord =
        api.request(HttpMethod.Post, "/salud/registros", token, buildJsonObject {
            put("dolor", record.dolor)
            put("estado_animo", record.estadoAnimo)
            put("presion_sistolica", record.presionSistolica)
            put("presion_diastolica", record.presionDiastolica)
            put("glucosa", record.glucosa)
            put("peso", record.peso)
            put("pulso", record.pulso)
            put("notas", record.notas)
        }).jsonObject.toHealthRecord()
}

private fun JsonObject.text(name: String): String = getValue(name).jsonPrimitive.content
private fun JsonObject.textOrNull(name: String): String? = getValue(name).jsonPrimitive.contentOrNull

private fun JsonObject.toMedication() = RemoteMedication(
    id = text("id"),
    recordatorioId = text("recordatorio_id"),
    nombre = text("nombre"),
    dosis = text("dosis"),
    frecuencia = text("frecuencia"),
    viaAdministracion = text("via_administracion"),
    hora = text("hora"),
    fechaInicio = textOrNull("fecha_inicio"),
    fechaFin = textOrNull("fecha_fin"),
    activo = getValue("activo").jsonPrimitive.boolean,
    proposito = text("proposito")
)

private fun JsonObject.toDose() = RemoteDose(
    recordatorioId = text("recordatorio_id"),
    fechaProgramada = text("fecha_programada"),
    estado = text("estado"),
    pospuestoMinutos = getValue("pospuesto_minutos").jsonPrimitive.int
)

private fun JsonObject.toAppointment() = RemoteAppointment(
    id = text("id"),
    especialista = text("especialista"),
    especialidad = text("especialidad"),
    lugar = text("lugar"),
    fechaHora = text("fecha_hora"),
    estado = text("estado"),
    notas = text("notas")
)

private fun JsonObject.toHealthRecord() = RemoteHealthRecord(
    id = text("id"),
    fechaRegistro = text("fecha_registro"),
    dolor = getValue("dolor").jsonPrimitive.intOrNull,
    estadoAnimo = textOrNull("estado_animo"),
    presionSistolica = getValue("presion_sistolica").jsonPrimitive.intOrNull,
    presionDiastolica = getValue("presion_diastolica").jsonPrimitive.intOrNull,
    glucosa = getValue("glucosa").jsonPrimitive.doubleOrNull,
    peso = getValue("peso").jsonPrimitive.doubleOrNull,
    pulso = getValue("pulso").jsonPrimitive.intOrNull,
    notas = text("notas")
)
