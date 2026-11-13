package com.healmate.data.util

enum class PainSeverity(val label: String) {
    LEVE("Leve"),
    MODERADO("Moderado"),
    SEVERO("Severo")
}

/**
 * Validador clínico basado en estándares de salud (AHA, ADA, OMS)
 * para prevenir inconsistencias y anomalías fisiológicas (caso límite 4.6 en docs/requisitos.md).
 */
object MedicalValidator {

    /**
     * Valida el formato y rangos fisiológicos de la presión arterial (Sistólica/Diastólica en mmHg).
     * Estándar AHA: Sistólica normal < 120, Diastólica normal < 80.
     * Rangos viables para registro: Sistólica [70, 250] (máximo que admite el servidor), Diastólica [40, 150], Sistólica > Diastólica.
     */
    fun validateBloodPressure(bp: String): Pair<Boolean, String> {
        val trimmed = bp.trim()
        val parts = trimmed.split("/")
        if (parts.size != 2) {
            return Pair(false, "Formato requerido: Sistólica/Diastólica (ej. 120/80)")
        }
        val systolic = parts[0].trim().toIntOrNull()
        val diastolic = parts[1].trim().toIntOrNull()

        if (systolic == null || diastolic == null) {
            return Pair(false, "Los valores de presión deben ser números enteros")
        }
        if (systolic !in 70..250) {
            return Pair(false, "Presión sistólica fuera de rango fisiológico seguro (70-250 mmHg)")
        }
        if (diastolic !in 40..150) {
            return Pair(false, "Presión diastólica fuera de rango fisiológico seguro (40-150 mmHg)")
        }
        if (systolic <= diastolic) {
            return Pair(false, "La presión sistólica debe ser superior a la presión diastólica")
        }
        return Pair(true, "Válido")
    }

    /**
     * Valida el nivel de glucosa en sangre (mg/dL).
     * Estándar ADA: Rango clínico viable para registro [40, 500] mg/dL.
     */
    fun validateGlucose(glucose: Double): Pair<Boolean, String> {
        if (glucose < 40.0 || glucose > 500.0) {
            return Pair(false, "Nivel de glucosa fuera de rango clínico admisible (40-500 mg/dL)")
        }
        return Pair(true, "Válido")
    }

    /**
     * Valida el pulso o frecuencia cardíaca en reposo (lpm).
     * Rango clínico para adultos en reposo [40, 220] lpm.
     */
    fun validatePulse(pulse: Int): Pair<Boolean, String> {
        if (pulse !in 40..220) {
            return Pair(false, "Pulso fuera de rango clínico admisible (40-220 lpm)")
        }
        return Pair(true, "Válido")
    }

    /**
     * Valida la escala visual analógica / numérica de dolor (EVA / NRS de 1 a 10).
     */
    fun validatePainScale(pain: Int): Pair<Boolean, String> {
        if (pain !in 1..10) {
            return Pair(false, "La escala de dolor debe ser un valor entre 1 y 10")
        }
        return Pair(true, "Válido")
    }

    /**
     * Categoría de intensidad en la escala numérica de dolor (NRS):
     * 1–3 leve, 4–6 moderado, 7–10 severo.
     */
    fun painSeverity(pain: Int): PainSeverity = when (pain.coerceIn(1, 10)) {
        in 1..3 -> PainSeverity.LEVE
        in 4..6 -> PainSeverity.MODERADO
        else -> PainSeverity.SEVERO
    }

    /**
     * Calcula el porcentaje de adherencia terapéutica según dosis tomadas vs total programado.
     */
    fun calculateAdherence(taken: Int, total: Int): Int {
        if (total <= 0) return 100
        val percentage = (taken.toDouble() / total.toDouble()) * 100
        return percentage.coerceIn(0.0, 100.0).toInt()
    }
}
