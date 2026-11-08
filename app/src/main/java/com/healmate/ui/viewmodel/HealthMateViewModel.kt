package com.healmate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healmate.data.model.*
import com.healmate.data.repository.HealthMateRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HealthMateViewModel(
    private val repository: HealthMateRepository = HealthMateRepository()
) : ViewModel() {

    // --- StateFlows Observables ---
    val currentUser: StateFlow<User?> = repository.currentUser.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), repository.currentUser.value
    )

    val isOfflineMode: StateFlow<Boolean> = repository.isOfflineMode.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), repository.isOfflineMode.value
    )

    val syncPending: StateFlow<Boolean> = repository.syncPending.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), repository.syncPending.value
    )

    val pendingActionsQueue: StateFlow<List<OfflineAction>> = repository.pendingActionsQueue.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), repository.pendingActionsQueue.value
    )

    val medications: StateFlow<List<Medication>> = repository.medications.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), repository.medications.value
    )

    val reminders: StateFlow<List<Reminder>> = repository.reminders.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), repository.reminders.value
    )

    val appointments: StateFlow<List<Appointment>> = repository.appointments.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), repository.appointments.value
    )

    val healthRecords: StateFlow<List<HealthRecord>> = repository.healthRecords.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), repository.healthRecords.value
    )

    // --- Perfil de demostración (RF-01) ---
    fun updateProfile(user: User) = repository.updateProfile(user)

    // --- Operaciones de Medicamentos (RF-02) ---
    fun addMedication(med: Medication) = repository.addMedication(med)

    fun toggleMedicationActive(medId: Int) = repository.toggleMedicationActive(medId)

    fun deleteMedication(medId: Int) = repository.deleteMedication(medId)

    // --- Operaciones de Recordatorios (RF-03) ---
    fun updateReminderStatus(reminderId: Int, newStatus: ReminderStatus) {
        repository.updateReminderStatus(reminderId, newStatus)
    }

    fun snoozeReminder(reminderId: Int, minutes: Int = 15) {
        repository.snoozeReminder(reminderId, minutes)
    }

    // --- Operaciones de Citas (RF-04) ---
    fun addAppointment(app: Appointment) = repository.addAppointment(app)

    fun cancelAppointment(appId: Int) = repository.cancelAppointment(appId)

    // --- Operaciones de Salud (RF-05, RF-06) ---
    fun addHealthRecord(record: HealthRecord) = repository.addHealthRecord(record)

    // --- Operaciones de Red y Sincronización (Invariantes Offline) ---
    fun toggleOfflineMode() = repository.toggleOfflineMode()

    fun flushOfflineQueue(): Int = repository.flushOfflineQueue()
}
