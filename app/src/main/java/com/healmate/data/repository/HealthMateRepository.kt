package com.healmate.data.repository

import com.healmate.domain.model.*
import com.healmate.domain.repository.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class HealthMateRepository : 
    IAuthRepository,
    IMedicationRepository,
    IReminderRepository,
    IAppointmentRepository,
    IHealthRecordRepository,
    ISyncRepository {

    // --- Autenticación y Perfil (RF-01, RF-08) ---
    private val _currentUser = MutableStateFlow<User?>(
        User(
            id = 1,
            nombre = "Alfredo Hernández",
            email = "alfredo.hernandez@example.com",
            fechaNacimiento = "1992-05-14",
            peso = 68.5,
            altura = 175,
            alergias = "Penicilina",
            condicionesCronicas = "Hipertensión, Diabetes Tipo 2",
            contactoEmergencia = "María Hernández (Esposa) - 555-0192"
        )
    )
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(true)
    override val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // --- Sincronización y Modo Offline (RNF Portabilidad, Invariante FIFO) ---
    private val _isOfflineMode = MutableStateFlow(false)
    override val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    private val _syncPending = MutableStateFlow(false)
    override val syncPending: StateFlow<Boolean> = _syncPending.asStateFlow()

    private val _pendingActionsQueue = MutableStateFlow<List<OfflineAction>>(emptyList())
    override val pendingActionsQueue: StateFlow<List<OfflineAction>> = _pendingActionsQueue.asStateFlow()

    // --- Medicamentos (RF-02) ---
    private val _medications = MutableStateFlow<List<Medication>>(
        listOf(
            Medication(1, "Paracetamol", "500mg", "Cada 8 horas", "Oral", "16:00", "2026-10-01", null, true, "Alivio de dolor leve"),
            Medication(2, "Telmisartán", "40mg", "Cada 24 horas", "Oral", "08:00", "2026-09-15", null, true, "Presión arterial"),
            Medication(3, "Atorvastatina", "20mg", "Una vez al día", "Oral", "22:00", "2026-09-01", null, true, "Control de colesterol"),
            Medication(4, "Topiramato", "100mg", "2 cada 24 horas", "Oral", "07:00", "2026-08-20", null, true, "Tratamiento neurológico"),
            Medication(5, "Insulina Glargina", "30 Unidades", "Cada 24 horas", "Subcutánea", "20:00", "2026-07-10", null, true, "Control de glucemia"),
            Medication(6, "Amoxicilina", "500mg", "Finalizado", "Oral", "--:--", "2026-09-01", "2026-09-10", false, "Infección tratada")
        )
    )
    override val medications: StateFlow<List<Medication>> = _medications.asStateFlow()

    // --- Recordatorios (RF-03) ---
    private val _reminders = MutableStateFlow<List<Reminder>>(
        listOf(
            Reminder(101, 1, "Paracetamol", "500mg (1 Tableta)", "16:00", ReminderStatus.PENDIENTE, "13 Oct 2026"),
            Reminder(102, 5, "Insulina Glargina", "30 Unidades", "20:00", ReminderStatus.PENDIENTE, "13 Oct 2026"),
            Reminder(103, 3, "Atorvastatina", "20mg (1 Tableta)", "22:00", ReminderStatus.PENDIENTE, "13 Oct 2026"),
            Reminder(104, 2, "Telmisartán", "40mg (1 Tableta)", "08:00", ReminderStatus.TOMADO, "13 Oct 2026")
        )
    )
    override val reminders: StateFlow<List<Reminder>> = _reminders.asStateFlow()

    // --- Citas Médicas (RF-04) ---
    private val _appointments = MutableStateFlow<List<Appointment>>(
        listOf(
            Appointment(201, "Dr. Maria Garcia", "Cardiología", "13 Oct 2026 - 10:00", "Centro Medico Norte", AppointmentStatus.CONFIRMADA, "Revisión trimestral de electrocardiograma"),
            Appointment(202, "Dra. Laura Mtz.", "Odontología", "13 Oct 2026 - 12:30", "Clínica Dental Smile", AppointmentStatus.RECORDATORIO, "Limpieza y profilaxis"),
            Appointment(203, "Dra. Natalia Tovar", "Psiquiatría", "13 Oct 2026 - 16:00", "Torre Medica H.", AppointmentStatus.RECORDATORIO, "Seguimiento y ajuste de tratamiento")
        )
    )
    override val appointments: StateFlow<List<Appointment>> = _appointments.asStateFlow()

    // --- Registros de Salud y Signos Vitales (RF-05, RF-06) ---
    private val _healthRecords = MutableStateFlow<List<HealthRecord>>(
        listOf(
            HealthRecord("r1", "13 Oct 2026", 3, Mood.FELIZ, "120/80", 95.0, 68.5, 78, "Me siento mucho mejor hoy pero aún persiste un dolor leve."),
            HealthRecord("r2", "12 Oct 2026", 4, Mood.NORMAL, "122/82", 98.0, 68.7, 76, "Jornada tranquila."),
            HealthRecord("r3", "11 Oct 2026", 5, Mood.NORMAL, "118/79", 102.0, 68.9, 80, "Sin novedades importantes.")
        )
    )
    override val healthRecords: StateFlow<List<HealthRecord>> = _healthRecords.asStateFlow()

    // --- Implementación IAuthRepository ---
    override fun login(email: String, pass: String): Boolean {
        if (email.isNotBlank() && pass.isNotBlank()) {
            _currentUser.value = User(
                nombre = if (email.contains("@")) email.substringBefore("@").replaceFirstChar { it.uppercase() } else "Usuario",
                email = email
            )
            _isLoggedIn.value = true
            return true
        }
        return false
    }

    override fun register(nombre: String, email: String, pass: String): Boolean {
        if (nombre.isNotBlank() && email.isNotBlank() && pass.isNotBlank()) {
            _currentUser.value = User(nombre = nombre, email = email)
            _isLoggedIn.value = true
            return true
        }
        return false
    }

    override fun logout() {
        _isLoggedIn.value = false
        _currentUser.value = null
    }

    override fun updateProfile(updatedUser: User) {
        _currentUser.value = updatedUser
        enqueueOfflineAction("UPDATE_PROFILE", "Actualización de perfil para ${updatedUser.nombre}")
    }

    // --- Implementación IMedicationRepository ---
    override fun addMedication(med: Medication) {
        _medications.value = _medications.value + med
        if (med.activo) {
            val newReminder = Reminder(
                id = (1000..9999).random(),
                medicationId = med.id,
                nombreMedicamento = med.nombre,
                dosis = med.dosis,
                horaProgramada = med.proximaHora,
                estado = ReminderStatus.PENDIENTE,
                fecha = "13 Oct 2026"
            )
            _reminders.value = _reminders.value + newReminder
        }
        enqueueOfflineAction("ADD_MEDICATION", "Alta de medicamento: ${med.nombre}")
    }

    override fun toggleMedicationActive(medId: Int) {
        _medications.value = _medications.value.map { m ->
            if (m.id == medId) m.copy(activo = !m.activo) else m
        }
        enqueueOfflineAction("TOGGLE_MEDICATION", "Cambio de estado activo para medicamento ID $medId")
    }

    override fun deleteMedication(medId: Int) {
        _medications.value = _medications.value.filter { it.id != medId }
        _reminders.value = _reminders.value.filter { it.medicationId != medId }
        enqueueOfflineAction("DELETE_MEDICATION", "Eliminación de medicamento ID $medId")
    }

    // --- Implementación IReminderRepository ---
    override fun updateReminderStatus(reminderId: Int, newStatus: ReminderStatus) {
        _reminders.value = _reminders.value.map { r ->
            if (r.id == reminderId) r.copy(estado = newStatus) else r
        }
        enqueueOfflineAction("UPDATE_REMINDER", "Dosis de recordatorio $reminderId marcada como ${newStatus.label}")
    }

    override fun snoozeReminder(reminderId: Int, additionalMinutes: Int) {
        _reminders.value = _reminders.value.map { r ->
            if (r.id == reminderId) {
                r.copy(
                    estado = ReminderStatus.POSPUESTO,
                    pospuestoMinutos = r.pospuestoMinutos + additionalMinutes
                )
            } else r
        }
        enqueueOfflineAction("SNOOZE_REMINDER", "Recordatorio $reminderId pospuesto por $additionalMinutes minutos")
    }

    // --- Implementación IAppointmentRepository ---
    override fun addAppointment(app: Appointment) {
        _appointments.value = _appointments.value + app
        enqueueOfflineAction("ADD_APPOINTMENT", "Cita con ${app.especialista} (${app.especialidad})")
    }

    override fun cancelAppointment(appId: Int) {
        _appointments.value = _appointments.value.map { a ->
            if (a.id == appId) a.copy(estado = AppointmentStatus.CANCELADA) else a
        }
        enqueueOfflineAction("CANCEL_APPOINTMENT", "Cancelación de cita ID $appId")
    }

    // --- Implementación IHealthRecordRepository ---
    override fun addHealthRecord(record: HealthRecord) {
        _healthRecords.value = listOf(record) + _healthRecords.value
        enqueueOfflineAction("ADD_HEALTH_RECORD", "Registro de signos vitales para fecha ${record.fecha}")
    }

    // --- Implementación ISyncRepository (Manejo Offline e Invariante FIFO) ---
    override fun toggleOfflineMode() {
        _isOfflineMode.value = !_isOfflineMode.value
        if (!_isOfflineMode.value) {
            flushOfflineQueue()
        }
    }

    override fun flushOfflineQueue(): Int {
        val count = _pendingActionsQueue.value.size
        _pendingActionsQueue.value = emptyList()
        _syncPending.value = false
        return count
    }

    private fun enqueueOfflineAction(actionType: String, description: String) {
        if (_isOfflineMode.value) {
            val action = OfflineAction(
                actionType = actionType,
                description = description,
                timestamp = System.currentTimeMillis()
            )
            _pendingActionsQueue.value = _pendingActionsQueue.value + action
            _syncPending.value = true
        }
    }
}
