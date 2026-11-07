package com.healmate.data.repository

import com.healmate.data.model.*
import kotlinx.coroutines.flow.StateFlow

/**
 * Interfaces segregadas siguiendo los principios SOLID (Interface Segregation y Dependency Inversion).
 */

interface IAuthRepository {
    val currentUser: StateFlow<User?>
    val isLoggedIn: StateFlow<Boolean>
    fun login(email: String, pass: String): Boolean
    fun register(nombre: String, email: String, pass: String): Boolean
    fun logout()
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
    fun toggleOfflineMode()
    fun flushOfflineQueue(): Int
}
