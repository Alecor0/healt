package com.healmate

import com.healmate.data.util.formatBirthDateInput
import com.healmate.data.util.isValidBirthDate
import com.healmate.data.util.validateRegistration
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class RegistrationInputTest {
    private val today = Calendar.getInstance().apply { clear(); set(2026, Calendar.NOVEMBER, 9) }

    @Test
    fun birthDateDashesAreInsertedWhileTyping() {
        assertEquals("1992", formatBirthDateInput("1992"))
        assertEquals("1992-0", formatBirthDateInput("19920"))
        assertEquals("1992-05-14", formatBirthDateInput("19920514"))
        // Ignora lo que no es dígito y no pasa de 8 dígitos.
        assertEquals("1992-05-14", formatBirthDateInput("1992-05-14999"))
        assertEquals("", formatBirthDateInput("abc"))
    }

    @Test
    fun onlyRealPastDatesAreValid() {
        assertTrue(isValidBirthDate("1992-05-14", today))
        assertTrue(isValidBirthDate("2024-02-29", today)) // bisiesto
        assertFalse(isValidBirthDate("2023-02-29", today))
        assertFalse(isValidBirthDate("2026-13-45", today))
        assertFalse(isValidBirthDate("2027-01-01", today)) // futura
        assertFalse(isValidBirthDate("1850-01-01", today))
        assertFalse(isValidBirthDate("14/05/1992", today))
    }

    @Test
    fun completeFormIsValid() {
        val errors = validateRegistration("Ana", "1992-05-14", "ana@example.com", "una clave larga", true, today)
        assertTrue(errors.isValid)
    }

    @Test
    fun eachMissingFieldExplainsWhatIsWrong() {
        val errors = validateRegistration(" ", "", "ana@", "corta", false, today)
        assertFalse(errors.isValid)
        assertEquals("Escribe tu nombre", errors.nombre)
        assertEquals("Escribe tu fecha de nacimiento", errors.fechaNacimiento)
        assertEquals("Escribe un correo válido", errors.email)
        assertEquals("Faltan 7 caracteres (mínimo 12)", errors.password)
        assertNotNull(errors.consentimiento)
    }
}
