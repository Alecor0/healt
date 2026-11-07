package com.healmate.domain.model

/**
 * Estados de citas médicas (RF-04).
 */
enum class AppointmentStatus(val label: String) {
    CONFIRMADA("Confirmada"),
    RECORDATORIO("Recordatorio 24h"),
    PENDIENTE("Pendiente"),
    CANCELADA("Cancelada")
}
