package com.healmate.domain.model

/**
 * Recordatorio asociado a un medicamento programado (RF-03).
 */
data class Reminder(
    val id: Int,
    val medicationId: Int,
    val nombreMedicamento: String,
    val dosis: String,
    val horaProgramada: String,
    val estado: ReminderStatus = ReminderStatus.PENDIENTE,
    val fecha: String, // ej. "13 Oct 2026"
    val pospuestoMinutos: Int = 0
)
