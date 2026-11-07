package com.healmate.domain.model

import java.util.UUID

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
