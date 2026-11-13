package com.healmate.data.util

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Textos de fecha que muestra la app ("13 Oct 2026 - 10:00") y su conversión
 * a los formatos ISO que usa la API.
 */
object DateText {
    private val shortMonths = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
    private val longMonths = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
    )
    private val clock = Regex("([0-9]{1,2}):([0-9]{2})")
    private val appointment = Regex("([0-9]{1,2}) ([A-Za-z]{3}) ([0-9]{4}) - ([0-9]{1,2}:[0-9]{2})")

    /** Día en que transcurre la demostración: sus datos de ejemplo son del 13 de octubre de 2026. */
    fun demoToday(): Calendar = Calendar.getInstance().apply {
        clear()
        set(2026, Calendar.OCTOBER, 13, 9, 0)
    }

    /** Nombre del mes en minúsculas; [month] va de 1 a 12. */
    fun monthName(month: Int): String = longMonths[month - 1]

    /** "13 Oct 2026" */
    fun shortDate(day: Calendar): String =
        "${day.get(Calendar.DAY_OF_MONTH)} ${shortMonths[day.get(Calendar.MONTH)]} ${day.get(Calendar.YEAR)}"

    /** "13 Oct 2026"; [month] va de 1 a 12. */
    fun shortDate(year: Int, month: Int, day: Int): String = "$day ${shortMonths[month - 1]} $year"

    /** "13 de octubre de 2026" */
    fun longDate(day: Calendar): String =
        "${day.get(Calendar.DAY_OF_MONTH)} de ${longMonths[day.get(Calendar.MONTH)]} de ${day.get(Calendar.YEAR)}"

    /** "2026-10-13" */
    fun isoDate(day: Calendar): String = String.format(
        Locale.US, "%04d-%02d-%02d",
        day.get(Calendar.YEAR), day.get(Calendar.MONTH) + 1, day.get(Calendar.DAY_OF_MONTH)
    )

    /** "13 Oct 2026 - 10:00" */
    fun appointmentText(moment: Calendar): String = String.format(
        Locale.US, "%s - %02d:%02d",
        shortDate(moment), moment.get(Calendar.HOUR_OF_DAY), moment.get(Calendar.MINUTE)
    )

    /** "8:5" no es válido; "8:05" → "08:05". Devuelve null si no es una hora de 00:00 a 23:59. */
    fun normalizeTime(value: String): String? {
        val match = clock.matchEntire(value.trim()) ?: return null
        val hour = match.groupValues[1].toInt()
        val minute = match.groupValues[2].toInt()
        if (hour > 23 || minute > 59) return null
        return String.format(Locale.US, "%02d:%02d", hour, minute)
    }

    /** El mismo día de [day] a la hora "HH:mm" indicada, o null si la hora no es válida. */
    fun atTime(day: Calendar, time: String): Calendar? {
        val normalized = normalizeTime(time) ?: return null
        return (day.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, normalized.substring(0, 2).toInt())
            set(Calendar.MINUTE, normalized.substring(3).toInt())
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    /** Convierte "13 Oct 2026 - 10:00" a un momento en [zone]; null si el texto o la fecha no son válidos. */
    fun parseAppointment(text: String, zone: TimeZone = TimeZone.getDefault()): Calendar? {
        val match = appointment.matchEntire(text.trim()) ?: return null
        val month = shortMonths.indexOfFirst { it.equals(match.groupValues[2], ignoreCase = true) }
        if (month < 0) return null
        val day = Calendar.getInstance(zone).apply {
            clear()
            isLenient = false
            set(match.groupValues[3].toInt(), month, match.groupValues[1].toInt())
        }
        return try {
            day.timeInMillis // valida que el día exista (p. ej. rechaza 31 de noviembre)
            day.isLenient = true
            atTime(day, match.groupValues[4])
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /** Momento exacto en UTC para la API: "2026-10-13T16:00:00Z". */
    fun isoUtc(moment: Date): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(moment)

    /** Lee un momento ISO de la API ("2026-10-13T16:00:00.123456Z" o con "+00:00"). */
    fun parseIso(value: String): Date? {
        val normalized = value.trim()
            .replace(Regex("\\.[0-9]+"), "")
            .replace(Regex("Z$"), "+00:00")
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply { isLenient = false }.parse(normalized)
        } catch (_: ParseException) {
            null
        }
    }

    /** Inicio (00:00) del día de [moment] en su zona horaria. */
    fun startOfDay(moment: Calendar): Calendar = (moment.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}
