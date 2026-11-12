package com.healmate.data.util

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val isoDate = Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")
private val emailShape = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

internal fun isBirthDateFormat(value: String): Boolean = isoDate.matches(value.trim())

/**
 * Da forma AAAA-MM-DD a lo que se va tecleando: conserva solo dígitos (máx. 8)
 * e inserta los guiones. "19920514" → "1992-05-14".
 */
internal fun formatBirthDateInput(raw: String): String {
    val digits = raw.filter(Char::isDigit).take(8)
    return buildString {
        digits.forEachIndexed { index, digit ->
            if (index == 4 || index == 6) append('-')
            append(digit)
        }
    }
}

/** Fecha real (no 2026-13-45), no futura y posterior a 1900, igual que exige el servidor. */
internal fun isValidBirthDate(value: String, today: Calendar = Calendar.getInstance()): Boolean {
    if (!isBirthDateFormat(value)) return false
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    val date = try {
        format.parse(value.trim()) ?: return false
    } catch (_: ParseException) {
        return false
    }
    val birth = Calendar.getInstance().apply { time = date }
    return birth.get(Calendar.YEAR) >= 1900 && !birth.after(today)
}

data class RegistrationErrors(
    val nombre: String? = null,
    val fechaNacimiento: String? = null,
    val email: String? = null,
    val password: String? = null,
    val consentimiento: String? = null
) {
    val isValid: Boolean
        get() = listOf(nombre, fechaNacimiento, email, password, consentimiento).all { it == null }
}

/** Revisa todo el formulario de registro y explica qué falta en cada campo. */
internal fun validateRegistration(
    nombre: String,
    fechaNacimiento: String,
    email: String,
    password: String,
    consentimiento: Boolean,
    today: Calendar = Calendar.getInstance()
): RegistrationErrors {
    val passwordLength = password.codePointCount(0, password.length)
    return RegistrationErrors(
        nombre = when {
            nombre.isBlank() -> "Escribe tu nombre"
            nombre.trim().length > 100 -> "Máximo 100 caracteres"
            else -> null
        },
        fechaNacimiento = when {
            fechaNacimiento.isBlank() -> "Escribe tu fecha de nacimiento"
            !isValidBirthDate(fechaNacimiento, today) -> "Fecha no válida (ej. 1992-05-14)"
            else -> null
        },
        email = if (emailShape.matches(email.trim())) null else "Escribe un correo válido",
        password = when {
            passwordLength < 12 -> "Faltan ${12 - passwordLength} caracteres (mínimo 12)"
            passwordLength > 128 -> "Máximo 128 caracteres"
            else -> null
        },
        consentimiento = if (consentimiento) null else "Acepta el aviso de privacidad para continuar"
    )
}
