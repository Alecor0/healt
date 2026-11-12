package com.healmate

import com.healmate.data.model.HealthRecord
import com.healmate.data.model.Mood
import com.healmate.data.util.CalendarMath
import com.healmate.data.util.HealthTrends
import com.healmate.data.util.MedicalValidator
import com.healmate.data.util.PainSeverity
import com.healmate.data.util.VitalsFormValidator
import com.healmate.data.util.VitalsInput
import org.junit.Assert.*
import org.junit.Test

class DesignLogicTest {

    // --- CalendarMath ---

    @Test
    fun october2026StartsOnThursday() {
        val cells = CalendarMath.monthCells(2026, 10)
        assertEquals(listOf(null, null, null, null, 1, 2, 3), cells.take(7))
        assertEquals(31, cells.filterNotNull().size)
        assertEquals(0, cells.size % 7)
    }

    @Test
    fun leapFebruaryHas29Days() {
        assertEquals(29, CalendarMath.monthCells(2028, 2).filterNotNull().last())
        assertEquals(28, CalendarMath.monthCells(2026, 2).filterNotNull().last())
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidMonthIsRejected() {
        CalendarMath.monthCells(2026, 13)
    }

    // --- Intensidad de dolor (NRS) ---

    @Test
    fun painSeverityFollowsNrsBands() {
        assertEquals(PainSeverity.LEVE, MedicalValidator.painSeverity(1))
        assertEquals(PainSeverity.LEVE, MedicalValidator.painSeverity(3))
        assertEquals(PainSeverity.MODERADO, MedicalValidator.painSeverity(4))
        assertEquals(PainSeverity.MODERADO, MedicalValidator.painSeverity(6))
        assertEquals(PainSeverity.SEVERO, MedicalValidator.painSeverity(7))
        assertEquals(PainSeverity.SEVERO, MedicalValidator.painSeverity(10))
    }

    // --- HealthTrends ---

    private fun record(pain: Int) =
        HealthRecord(fecha = "x", dolorEscala = pain, estadoAnimo = Mood.NORMAL, presionArterial = "120/80", glucosa = 95.0, peso = 70.0, pulso = 70)

    @Test
    fun painTrendIsOldestFirstAndScaled() {
        // El repositorio guarda los más recientes primero.
        val trend = HealthTrends.painTrend(listOf(record(10), record(1)))
        assertEquals(listOf(0f, 1f), trend)
    }

    @Test
    fun painTrendKeepsOnlyLastSevenRecords() {
        val records = (1..10).map { record(it) }
        assertEquals(7, HealthTrends.painTrend(records).size)
        assertEquals(4, HealthTrends.averagePain(records)) // promedio de 1..7
    }

    @Test
    fun averagePainIsNullWithoutRecords() {
        assertNull(HealthTrends.averagePain(emptyList()))
        assertTrue(HealthTrends.painTrend(emptyList()).isEmpty())
    }

    // --- VitalsFormValidator ---

    @Test
    fun validVitalsPassAndAcceptDecimalComma() {
        val errors = VitalsFormValidator.validate(VitalsInput("120/80", "95,5", "68,5", "78"))
        assertTrue(errors.isValid)
        assertEquals(68.5, VitalsFormValidator.parseDecimal("68,5")!!, 0.0)
    }

    @Test
    fun everyInvalidFieldReportsItsOwnError() {
        val errors = VitalsFormValidator.validate(VitalsInput("1200/80", "-10", "abc", "0"))
        assertFalse(errors.isValid)
        assertNotNull(errors.presionArterial)
        assertNotNull(errors.glucosa)
        assertNotNull(errors.peso)
        assertNotNull(errors.pulso)
    }

    @Test
    fun emptyFieldsAreRejected() {
        val errors = VitalsFormValidator.validate(VitalsInput("", "", "", ""))
        assertFalse(errors.isValid)
        assertEquals("Ingresa la glucosa en mg/dL", errors.glucosa)
        assertEquals("Ingresa el pulso en lpm", errors.pulso)
    }
}
