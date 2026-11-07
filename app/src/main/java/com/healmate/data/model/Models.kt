package com.healmate.data.model

import java.util.UUID

/**
 * Estados permitidos para una dosis según la especificación técnica (RF-03).
 */
enum class ReminderStatus(val label: String) {
    PENDIENTE("Pendiente"),
    TOMADO("Tomada"),
    POSPUESTO("Pospuesta"),
    OMITIDO("Omitida")
}

/**
 * Estados de citas médicas (RF-04).
 */
enum class AppointmentStatus(val label: String) {
    CONFIRMADA("Confirmada"),
    RECORDATORIO("Recordatorio 24h"),
    PENDIENTE("Pendiente"),
    CANCELADA("Cancelada")
}

/**
 * Registro de estado de ánimo (RF-05).
 */
enum class Mood(val displayName: String, val icon: String) {
    TRISTE("Triste", "😢"),
    NORMAL("Normal", "😐"),
    FELIZ("Feliz", "😊"),
    EMOCIONADO("Emocionado", "🤩")
}

/**
 * Entidad Usuario conforme a RF-01 y Tabla 6 del informe técnico.
 */
data class User(
    val id: Int = 1,
    val nombre: String = "Alfredo Hernández",
    val email: String = "alfredo.hernandez@example.com",
    val fechaNacimiento: String = "1992-05-14",
    val peso: Double = 68.5, // kg
    val altura: Int = 175, // cm
    val alergias: String = "Penicilina",
    val condicionesCronicas: String = "Hipertensión, Diabetes Tipo 2",
    val contactoEmergencia: String = "María Hernández (Esposa) - 555-0192"
)

/**
 * Entidad Medicamento conforme a RF-02 y Tablas 7 y 8 del informe técnico.
 */
data class Medication(
    val id: Int,
    val nombre: String,
    val dosis: String,
    val frecuencia: String,
    val viaAdministracion: String = "Oral",
    val proximaHora: String,
    val fechaInicio: String = "2026-10-01",
    val fechaFin: String? = null,
    val activo: Boolean = true,
    val proposito: String = ""
)

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

/**
 * Registro diario de síntomas y signos vitales (RF-05).
 */
data class HealthRecord(
    val id: String = UUID.randomUUID().toString(),
    val fecha: String,
    val dolorEscala: Int, // 1 a 10 (Escala EVA/NRS)
    val estadoAnimo: Mood,
    val presionArterial: String, // ej. "120/80" (Sistólica/Diastólica mmHg)
    val glucosa: Double, // mg/dL
    val peso: Double, // kg
    val pulso: Int, // lpm (frecuencia cardíaca en reposo)
    val notas: String = ""
)

/**
 * Acción registrada en cola durante el modo offline (Invariante FIFO y RNF de portabilidad).
 */
data class OfflineAction(
    val id: String = UUID.randomUUID().toString(),
    val actionType: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)
