package com.healmate.data.repository

import com.healmate.data.model.*
import kotlinx.coroutines.flow.StateFlow

/**
 * Interfaces segregadas siguiendo los principios SOLID (Interface Segregation y Dependency Inversion).
 */

interface IProfileRepository {
    val currentUser: StateFlow<User?>
    fun updateProfile(updatedUser: User)
}

interface IMedicationRepository {
    val medications: StateFlow<List<Medication>>
    fun addMedication(med: Medication)
    fun toggleMedicationActive(medId: Int)
    fun deleteMedication(medId: Int)
}

interface IReminderRepository {
    val reminders: StateFlow<List<Reminder>>
    fun updateReminderStatus(reminderId: Int, newStatus: ReminderStatus)
    fun snoozeReminder(reminderId: Int, additionalMinutes: Int = 15)
}

interface IAppointmentRepository {
    val appointments: StateFlow<List<Appointment>>
    fun addAppointment(app: Appointment)
    fun cancelAppointment(appId: Int)
}

interface IHealthRecordRepository {
    val healthRecords: StateFlow<List<HealthRecord>>
    fun addHealthRecord(record: HealthRecord)
}

interface ISyncRepository {
    val isOfflineMode: StateFlow<Boolean>
    val syncPending: StateFlow<Boolean>
    val pendingActionsQueue: StateFlow<List<OfflineAction>>
    /** Motivo del último cambio que no se pudo guardar, o null. */
    val syncError: StateFlow<String?>
    fun clearSyncError()
    /** Vuelve a cargar los datos desde su origen. */
    fun refresh()
    fun toggleOfflineMode()
    fun flushOfflineQueue(): Int
}

/** Todo lo que necesitan los módulos de la pantalla principal, sea la demostración o la cuenta real. */
interface HealthRepository :
    IProfileRepository,
    IMedicationRepository,
    IReminderRepository,
    IAppointmentRepository,
    IHealthRecordRepository,
    ISyncRepository
