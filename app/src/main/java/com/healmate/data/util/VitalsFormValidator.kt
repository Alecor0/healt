package com.healmate.data.util

data class VitalsInput(
    val presionArterial: String,
    val glucosa: String,
    val peso: String,
    val pulso: String
)

data class VitalsErrors(
    val presionArterial: String? = null,
    val glucosa: String? = null,
    val peso: String? = null,
    val pulso: String? = null
) {
    val isValid: Boolean
        get() = listOf(presionArterial, glucosa, peso, pulso).all { it == null }
}

/**
 * Valida todo el formulario de signos vitales de una vez para mostrar
 * cada error junto a su campo (caso límite 4.6).
 */
object VitalsFormValidator {

    fun validate(input: VitalsInput): VitalsErrors {
        val glucosa = parseDecimal(input.glucosa)
        val peso = parseDecimal(input.peso)
        val pulso = input.pulso.trim().toIntOrNull()
        return VitalsErrors(
            presionArterial = MedicalValidator.validateBloodPressure(input.presionArterial).errorOrNull(),
            glucosa = if (glucosa == null) "Ingresa la glucosa en mg/dL"
            else MedicalValidator.validateGlucose(glucosa).errorOrNull(),
            peso = if (peso == null || peso <= 0.0) "Ingresa un peso válido en kg" else null,
            pulso = if (pulso == null) "Ingresa el pulso en lpm"
            else MedicalValidator.validatePulse(pulso).errorOrNull()
        )
    }

    /** Acepta coma o punto como separador decimal. */
    fun parseDecimal(value: String): Double? = value.trim().replace(',', '.').toDoubleOrNull()

    private fun Pair<Boolean, String>.errorOrNull(): String? = if (first) null else second
}
