package com.healmate

import com.healmate.data.model.Appointment
import com.healmate.data.util.DateText
import com.healmate.ui.screens.appointments.dayIn
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class DateTextTest {
    private val mexico = TimeZone.getTimeZone("America/Mexico_City") // UTC-6, sin horario de verano

    private fun moment(year: Int, month: Int, day: Int, hour: Int, minute: Int): Calendar =
        Calendar.getInstance(mexico).apply {
            clear()
            set(year, month - 1, day, hour, minute)
        }

    @Test
    fun formatsDatesAsTheScreensShowThem() {
        val day = moment(2026, 11, 5, 9, 7)
        assertEquals("5 Nov 2026", DateText.shortDate(day))
        assertEquals("5 Nov 2026", DateText.shortDate(2026, 11, 5))
        assertEquals("5 de noviembre de 2026", DateText.longDate(day))
        assertEquals("2026-11-05", DateText.isoDate(day))
        assertEquals("5 Nov 2026 - 09:07", DateText.appointmentText(day))
        assertEquals("enero", DateText.monthName(1))
        assertEquals("diciembre", DateText.monthName(12))
        assertEquals("13 Oct 2026", DateText.shortDate(DateText.demoToday()))
    }

    @Test
    fun normalizesOnlyRealClockTimes() {
        assertEquals("08:05", DateText.normalizeTime(" 8:05 "))
        assertEquals("23:59", DateText.normalizeTime("23:59"))
        assertEquals("00:00", DateText.normalizeTime("0:00"))
        for (invalid in listOf("", "8", "24:00", "12:60", "8:5", "--:--", "ocho", "08:00 am")) {
            assertNull(invalid, DateText.normalizeTime(invalid))
        }
    }

    @Test
    fun appointmentTextRoundTripsToUtc() {
        val parsed = DateText.parseAppointment("13 Oct 2026 - 10:30", mexico)!!
        assertEquals("2026-10-13T16:30:00Z", DateText.isoUtc(parsed.time))
        assertEquals("13 Oct 2026 - 10:30", DateText.appointmentText(parsed))
        // Cerca de medianoche el día UTC cambia, pero el momento es el mismo.
        val late = DateText.parseAppointment("31 Dic 2026 - 23:15", mexico)!!
        assertEquals("2027-01-01T05:15:00Z", DateText.isoUtc(late.time))
    }

    @Test
    fun rejectsAppointmentsThatAreNotRealDates() {
        for (invalid in listOf("13 Oct 2026", "13 Oct 2026 - 25:00", "31 Nov 2026 - 10:00", "13 Xyz 2026 - 10:00", "")) {
            assertNull(invalid, DateText.parseAppointment(invalid, mexico))
        }
    }

    @Test
    fun readsServerMomentsWithOrWithoutFraction() {
        val expected = moment(2026, 10, 13, 10, 30).time
        assertEquals(expected, DateText.parseIso("2026-10-13T16:30:00Z"))
        assertEquals(expected, DateText.parseIso("2026-10-13T16:30:00.123456Z"))
        assertEquals(expected, DateText.parseIso("2026-10-13T16:30:00+00:00"))
        assertEquals(expected, DateText.parseIso("2026-10-13T10:30:00-06:00"))
        assertNull(DateText.parseIso("ayer"))
    }

    @Test
    fun scheduledDoseKeepsTheDayAndUsesTheGivenTime() {
        val now = moment(2026, 11, 12, 17, 45)
        assertEquals("2026-11-12T14:00:00Z", DateText.isoUtc(DateText.atTime(now, "8:00")!!.time))
        assertEquals("2026-11-12T06:00:00Z", DateText.isoUtc(DateText.startOfDay(now).time))
        assertNull(DateText.atTime(now, "--:--"))
        assertEquals(45, now.get(Calendar.MINUTE)) // no modifica el original
    }

    @Test
    fun appointmentBelongsOnlyToItsOwnMonth() {
        val appointment = Appointment(1, "Dra. García", "Cardiología", "13 Oct 2026 - 10:00", "Clínica")
        assertEquals(13, appointment.dayIn(2026, 10))
        assertNull(appointment.dayIn(2026, 11))
        assertNull(appointment.dayIn(2027, 10))
        assertNull(appointment.copy(fechaHora = "sin fecha").dayIn(2026, 10))
    }
}
