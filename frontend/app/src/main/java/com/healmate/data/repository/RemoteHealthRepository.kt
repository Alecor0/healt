package com.healmate.data.repository

import com.healmate.data.model.Appointment
import com.healmate.data.model.AppointmentStatus
import com.healmate.data.model.HealthRecord
import com.healmate.data.model.Medication
import com.healmate.data.model.Mood
import com.healmate.data.model.OfflineAction
import com.healmate.data.model.Reminder
import com.healmate.data.model.ReminderStatus
import com.healmate.data.model.User
import com.healmate.data.remote.AccountRequestException
import com.healmate.data.remote.HealthApi
import com.healmate.data.remote.NewAppointment
import com.healmate.data.remote.NewHealthRecord
import com.healmate.data.remote.NewMedication
import com.healmate.data.remote.RemoteAppointment
import com.healmate.data.remote.RemoteDose
import com.healmate.data.remote.RemoteHealthRecord
import com.healmate.data.remote.RemoteMedication
import com.healmate.data.util.DateText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

/** Da el token de acceso vigente a [block] y renueva la sesión si el servidor lo rechaza. */
internal interface SessionGate {
    suspend fun <T> withToken(block: suspend (String) -> T): T
}

/**
 * Medicamentos, dosis, citas y registros de salud de la cuenta, guardados en el servidor.
 * La pantalla solo cambia cuando el servidor confirma el cambio; si falla, [syncError] lo explica.
 */
internal class RemoteHealthRepository(
    private val api: HealthApi,
    private val gate: SessionGate,
    private val now: () -> Calendar = { Calendar.getInstance() },
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) : HealthRepository, AutoCloseable {

    // Las pantallas identifican cada elemento con un entero; el servidor, con un UUID.
    private val localIds = mutableMapOf<String, Int>()
    private val remoteIds = mutableMapOf<Int, String>()

    private var remoteMedications = emptyList<RemoteMedication>()
    private var todayDoses = emptyMap<String, RemoteDose>()
    private var remoteAppointments = emptyList<RemoteAppointment>()

    // El perfil de la cuenta lo gestiona AccountViewModel con /usuarios/me.
    override val currentUser: StateFlow<User?> = MutableStateFlow(null)
    override fun updateProfile(updatedUser: User) = Unit

    private val _medications = MutableStateFlow<List<Medication>>(emptyList())
    override val medications: StateFlow<List<Medication>> = _medications.asStateFlow()
    private val _reminders = MutableStateFlow<List<Reminder>>(emptyList())
    override val reminders: StateFlow<List<Reminder>> = _reminders.asStateFlow()
    private val _appointments = MutableStateFlow<List<Appointment>>(emptyList())
    override val appointments: StateFlow<List<Appointment>> = _appointments.asStateFlow()
    private val _healthRecords = MutableStateFlow<List<HealthRecord>>(emptyList())
    override val healthRecords: StateFlow<List<HealthRecord>> = _healthRecords.asStateFlow()

    private val _isOfflineMode = MutableStateFlow(false)
    override val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()
    private val _syncPending = MutableStateFlow(false)
    override val syncPending: StateFlow<Boolean> = _syncPending.asStateFlow()
    override val pendingActionsQueue: StateFlow<List<OfflineAction>> = MutableStateFlow(emptyList())
    private val _syncError = MutableStateFlow<String?>(null)
    override val syncError: StateFlow<String?> = _syncError.asStateFlow()

    override fun clearSyncError() {
        _syncError.value = null
    }

    override fun toggleOfflineMode() = refresh()
    override fun flushOfflineQueue(): Int = 0

    override fun refresh() {
        scope.launch {
            attempt(invalid = LOAD_FAILED, failed = LOAD_FAILED, offline = "Sin conexión. No se pudieron cargar tus datos.") { token ->
                val today = DateText.startOfDay(now())
                val tomorrow = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
                val loadedMedications = api.medications(token)
                val loadedDoses = api.doses(token, DateText.isoUtc(today.time), DateText.isoUtc(tomorrow.time))
                val loadedAppointments = api.appointments(token)
                val loadedRecords = api.healthRecords(token)
                remoteMedications = loadedMedications
                todayDoses = loadedDoses.associateBy { it.recordatorioId }
                remoteAppointments = loadedAppointments
                publishMedications()
                publishAppointments()
                _healthRecords.value = loadedRecords.map { it.toHealthRecord() }
            }
        }
    }

    // --- Medicamentos (RF-02) ---
    override fun addMedication(med: Medication) {
        val time = DateText.normalizeTime(med.proximaHora)
            ?: return fail("Escribe el horario de toma como HH:MM (ej. 08:00).")
        val body = NewMedication(
            nombre = med.nombre,
            dosis = med.dosis,
            frecuencia = med.frecuencia,
            viaAdministracion = med.viaAdministracion,
            hora = time,
            fechaInicio = med.fechaInicio.trim().ifBlank { DateText.isoDate(now()) },
            fechaFin = med.fechaFin?.trim()?.ifBlank { null },
            proposito = med.proposito
        )
        save("Revisa el medicamento: las fechas van como AAAA-MM-DD y el fin no puede ser anterior al inicio.") { token ->
            remoteMedications = remoteMedications + api.addMedication(token, body)
            publishMedications()
        }
    }

    override fun toggleMedicationActive(medId: Int) {
        val id = remoteIds[medId] ?: return
        val current = remoteMedications.firstOrNull { it.id == id } ?: return
        save(INVALID) { token ->
            val updated = api.setMedicationActive(token, id, !current.activo)
            remoteMedications = remoteMedications.map { if (it.id == id) updated else it }
            publishMedications()
        }
    }

    override fun deleteMedication(medId: Int) {
        val id = remoteIds[medId] ?: return
        save(INVALID) { token ->
            api.deleteMedication(token, id)
            remoteMedications = remoteMedications.filter { it.id != id }
            publishMedications()
        }
    }

    // --- Dosis del día (RF-03) ---
    override fun updateReminderStatus(reminderId: Int, newStatus: ReminderStatus) {
        val state = when (newStatus) {
            ReminderStatus.TOMADO -> "tomado"
            ReminderStatus.OMITIDO -> "omitido"
            ReminderStatus.POSPUESTO -> "pospuesto"
            ReminderStatus.PENDIENTE -> return
        }
        recordDose(reminderId, state, additionalMinutes = 0)
    }

    override fun snoozeReminder(reminderId: Int, additionalMinutes: Int) =
        recordDose(reminderId, "pospuesto", additionalMinutes)

    private fun recordDose(reminderId: Int, state: String, additionalMinutes: Int) {
        val medication = remoteMedications.firstOrNull { it.id == remoteIds[reminderId] } ?: return
        // La dosis se identifica por su hora programada local de hoy (caso límite 4.1).
        val scheduled = DateText.atTime(now(), medication.hora) ?: return
        val snoozed = (todayDoses[medication.recordatorioId]?.pospuestoMinutos ?: 0) + additionalMinutes
        val body = RemoteDose(medication.recordatorioId, DateText.isoUtc(scheduled.time), state, snoozed)
        save(INVALID) { token ->
            todayDoses = todayDoses + (medication.recordatorioId to api.recordDose(token, body))
            publishMedications()
        }
    }

    // --- Citas (RF-04) ---
    override fun addAppointment(app: Appointment) {
        val moment = DateText.parseAppointment(app.fechaHora)
            ?: return fail("Escribe la hora de la cita como HH:MM (ej. 10:30).")
        // Invariante 8: no se programan citas en el pasado.
        if (!moment.after(now())) return fail(PAST_APPOINTMENT)
        val body = NewAppointment(
            especialista = app.especialista,
            especialidad = app.especialidad,
            lugar = app.lugar,
            fechaHora = DateText.isoUtc(moment.time),
            notas = app.notas
        )
        save(PAST_APPOINTMENT) { token ->
            remoteAppointments = remoteAppointments + api.addAppointment(token, body)
            publishAppointments()
        }
    }

    override fun cancelAppointment(appId: Int) {
        val id = remoteIds[appId] ?: return
        save(INVALID) { token ->
            val updated = api.cancelAppointment(token, id)
            remoteAppointments = remoteAppointments.map { if (it.id == id) updated else it }
            publishAppointments()
        }
    }

    // --- Registros de salud (RF-05) ---
    override fun addHealthRecord(record: HealthRecord) {
        val pressure = record.presionArterial.split("/").mapNotNull { it.trim().toIntOrNull() }
        if (pressure.size != 2) return fail("Escribe la presión como sistólica/diastólica (ej. 120/80).")
        val body = NewHealthRecord(
            dolor = record.dolorEscala,
            estadoAnimo = record.estadoAnimo.name.lowercase(Locale.ROOT),
            presionSistolica = pressure[0],
            presionDiastolica = pressure[1],
            glucosa = record.glucosa,
            peso = record.peso,
            pulso = record.pulso,
            notas = record.notas
        )
        save("El servidor rechazó el registro: revisa que los valores estén dentro de los rangos permitidos.") { token ->
            _healthRecords.value = listOf(api.addHealthRecord(token, body).toHealthRecord()) + _healthRecords.value
        }
    }

    override fun close() {
        scope.cancel()
    }

    private fun fail(message: String) {
        _syncError.value = message
    }

    private fun save(invalid: String, action: suspend (String) -> Unit) {
        scope.launch {
            _syncPending.value = true
            try {
                attempt(invalid, "El servidor no pudo guardar el cambio.", "Sin conexión. El cambio no se guardó.", action)
            } finally {
                _syncPending.value = false
            }
        }
    }

    private suspend fun attempt(invalid: String, failed: String, offline: String, action: suspend (String) -> Unit) {
        try {
            gate.withToken(action)
            _isOfflineMode.value = false
        } catch (error: CancellationException) {
            throw error
        } catch (error: AccountRequestException) {
            _syncError.value = when (error.status) {
                401 -> "La sesión venció. Cierra sesión y vuelve a entrar."
                404 -> "Ese dato ya no existe en el servidor."
                409 -> "Esa dosis ya quedó registrada y no se puede cambiar."
                422 -> invalid
                else -> failed
            }
        } catch (_: Exception) {
            _isOfflineMode.value = true
            _syncError.value = offline
        }
    }

    private fun localId(remoteId: String): Int = localIds.getOrPut(remoteId) {
        (localIds.size + 1).also { remoteIds[it] = remoteId }
    }

    private fun publishMedications() {
        val today = now()
        val todayIso = DateText.isoDate(today)
        _medications.value = remoteMedications.map {
            Medication(
                id = localId(it.id),
                nombre = it.nombre,
                dosis = it.dosis,
                frecuencia = it.frecuencia,
                viaAdministracion = it.viaAdministracion,
                proximaHora = it.hora,
                fechaInicio = it.fechaInicio.orEmpty(),
                fechaFin = it.fechaFin,
                activo = it.activo,
                proposito = it.proposito
            )
        }
        // Solo generan dosis hoy los medicamentos activos y dentro de su tratamiento (caso límite 4.4).
        _reminders.value = remoteMedications
            .filter { it.activo && (it.fechaInicio ?: todayIso) <= todayIso && (it.fechaFin ?: todayIso) >= todayIso }
            .sortedBy { it.hora }
            .map {
                val dose = todayDoses[it.recordatorioId]
                Reminder(
                    id = localId(it.id),
                    medicationId = localId(it.id),
                    nombreMedicamento = it.nombre,
                    dosis = it.dosis,
                    horaProgramada = it.hora,
                    estado = when (dose?.estado) {
                        "tomado" -> ReminderStatus.TOMADO
                        "omitido" -> ReminderStatus.OMITIDO
                        "pospuesto" -> ReminderStatus.POSPUESTO
                        else -> ReminderStatus.PENDIENTE
                    },
                    fecha = DateText.shortDate(today),
                    pospuestoMinutos = dose?.pospuestoMinutos ?: 0
                )
            }
    }

    private fun publishAppointments() {
        val nowMillis = now().timeInMillis
        val dated = remoteAppointments.mapNotNull { remote ->
            DateText.parseIso(remote.fechaHora)?.let { remote to it.time }
        }
        // Primero las próximas (la más cercana arriba) y después las pasadas.
        val upcoming = dated.filter { it.second >= nowMillis }.sortedBy { it.second }
        val past = dated.filter { it.second < nowMillis }.sortedByDescending { it.second }
        _appointments.value = (upcoming + past).map { (remote, millis) ->
            Appointment(
                id = localId(remote.id),
                especialista = remote.especialista,
                especialidad = remote.especialidad,
                fechaHora = DateText.appointmentText(Calendar.getInstance().apply { timeInMillis = millis }),
                lugar = remote.lugar,
                estado = when {
                    remote.estado == "cancelada" -> AppointmentStatus.CANCELADA
                    remote.estado == "pendiente" -> AppointmentStatus.PENDIENTE
                    // Aviso preventivo de las 24 horas previas (RF-04).
                    millis - nowMillis in 0..DAY_MILLIS -> AppointmentStatus.RECORDATORIO
                    else -> AppointmentStatus.CONFIRMADA
                },
                notas = remote.notas
            )
        }
    }

    private fun RemoteHealthRecord.toHealthRecord(): HealthRecord {
        val moment = Calendar.getInstance().apply { DateText.parseIso(fechaRegistro)?.let { time = it } }
        return HealthRecord(
            id = id,
            fecha = DateText.shortDate(moment),
            dolorEscala = dolor ?: 1,
            estadoAnimo = Mood.entries.firstOrNull { it.name.equals(estadoAnimo, ignoreCase = true) } ?: Mood.NORMAL,
            presionArterial = "${presionSistolica ?: 0}/${presionDiastolica ?: 0}",
            glucosa = glucosa ?: 0.0,
            peso = peso ?: 0.0,
            pulso = pulso ?: 0,
            notas = notas
        )
    }

    private companion object {
        const val DAY_MILLIS = 24L * 60 * 60 * 1000
        const val INVALID = "El servidor rechazó el cambio."
        const val LOAD_FAILED = "No se pudieron cargar tus datos. Intenta de nuevo."
        const val PAST_APPOINTMENT = "La cita debe ser en una fecha y hora futuras."
    }
}
