package com.healmate.domain.repository

import com.healmate.domain.model.Appointment
import kotlinx.coroutines.flow.StateFlow

interface IAppointmentRepository {
    val appointments: StateFlow<List<Appointment>>
    fun addAppointment(app: Appointment)
    fun cancelAppointment(appId: Int)
}
