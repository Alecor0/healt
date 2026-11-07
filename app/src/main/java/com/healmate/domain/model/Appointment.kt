package com.healmate.domain.model

/**
 * Cita médica programada (RF-04).
 */
data class Appointment(
    val id: Int,
    val especialista: String,
    val especialidad: String,
    val fechaHora: String, // ej. "13 Oct 2026 - 10:00"
    val lugar: String,
    val estado: AppointmentStatus = AppointmentStatus.CONFIRMADA,
    val notas: String = ""
)
