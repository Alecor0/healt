package com.healmate.domain.validation

import org.junit.Assert.*
import org.junit.Test

class MedicalValidatorTest {

    @Test
    fun testMedicalValidatorBloodPressure() {
        // Válido
        val (valid1, _) = MedicalValidator.validateBloodPressure("120/80")
        assertTrue(valid1)

        // Incompleto / Formato inválido
        val (invalid1, _) = MedicalValidator.validateBloodPressure("120-80")
        assertFalse(invalid1)

        // Sistólica menor que diastólica (fisiológicamente imposible)
        val (invalid2, _) = MedicalValidator.validateBloodPressure("70/110")
        assertFalse(invalid2)

        // Fuera de rango vital
        val (invalid3, _) = MedicalValidator.validateBloodPressure("350/80")
        assertFalse(invalid3)
    }

    @Test
    fun testMedicalValidatorGlucose() {
        assertTrue(MedicalValidator.validateGlucose(95.0).first)
        assertFalse(MedicalValidator.validateGlucose(10.0).first) // Demasiado bajo / incompatible
        assertFalse(MedicalValidator.validateGlucose(600.0).first) // Extremo
    }

    @Test
    fun testMedicalValidatorPulse() {
        assertTrue(MedicalValidator.validatePulse(75).first)
        assertFalse(MedicalValidator.validatePulse(20).first)
        assertFalse(MedicalValidator.validatePulse(250).first)
    }

    @Test
    fun testMedicalValidatorPainScale() {
        assertTrue(MedicalValidator.validatePainScale(5).first)
        assertFalse(MedicalValidator.validatePainScale(0).first)
        assertFalse(MedicalValidator.validatePainScale(11).first)
    }

    @Test
    fun testAdherenceCalculation() {
        assertEquals(100, MedicalValidator.calculateAdherence(4, 4))
        assertEquals(50, MedicalValidator.calculateAdherence(2, 4))
        assertEquals(0, MedicalValidator.calculateAdherence(0, 5))
        assertEquals(100, MedicalValidator.calculateAdherence(0, 0))
    }
}
