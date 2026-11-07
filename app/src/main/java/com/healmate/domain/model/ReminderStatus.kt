package com.healmate.domain.model

/**
 * Estados permitidos para una dosis según la especificación técnica (RF-03).
 */
enum class ReminderStatus(val label: String) {
    PENDIENTE("Pendiente"),
    TOMADO("Tomada"),
    POSPUESTO("Pospuesta"),
    OMITIDO("Omitida")
}
