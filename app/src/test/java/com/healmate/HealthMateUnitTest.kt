package com.healmate

import com.healmate.data.model.*
import com.healmate.data.repository.HealthMateRepository
import com.healmate.data.util.MedicalValidator
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class HealthMateUnitTest {

    private lateinit var repository: HealthMateRepository

    @Before
    fun setUp() {
        repository = HealthMateRepository()
    }

    @Test
    fun testInitialStateHasSeedData() {
        assertNotNull(repository.currentUser.value)
        assertEquals("Alfredo Hernández", repository.currentUser.value?.nombre)
        assertTrue(repository.medications.value.isNotEmpty())
        assertTrue(repository.reminders.value.isNotEmpty())
        assertTrue(repository.appointments.value.isNotEmpty())
        assertTrue(repository.healthRecords.value.isNotEmpty())
    }

    @Test
    fun testUpdateReminderStatusToTakenAndOmitted() {
        val initialReminder = repository.reminders.value.first()
        val reminderId = initialReminder.id

        // Marcar como tomado
        repository.updateReminderStatus(reminderId, ReminderStatus.TOMADO)
        var updated = repository.reminders.value.first { it.id == reminderId }
        assertEquals(ReminderStatus.TOMADO, updated.estado)

        // Marcar como omitido (RF-03)
        repository.updateReminderStatus(reminderId, ReminderStatus.OMITIDO)
        updated = repository.reminders.value.first { it.id == reminderId }
        assertEquals(ReminderStatus.OMITIDO, updated.estado)
    }

    @Test
    fun testSnoozeReminderIncrementsMinutes() {
        val reminder = repository.reminders.value.first()
        val initialMinutes = reminder.pospuestoMinutos

        repository.snoozeReminder(reminder.id, 15)
        val updated = repository.reminders.value.first { it.id == reminder.id }

        assertEquals(ReminderStatus.POSPUESTO, updated.estado)
        assertEquals(initialMinutes + 15, updated.pospuestoMinutos)
    }

    @Test
    fun testAddMedicationWithDates() {
        val initialSize = repository.medications.value.size
        val newMed = Medication(
            id = 99,
            nombre = "Ibuprofeno",
            dosis = "400mg",
            frecuencia = "Cada 8 horas",
            viaAdministracion = "Oral",
            proximaHora = "18:00",
            fechaInicio = "2026-10-13",
            fechaFin = "2026-10-20",
            activo = true,
            proposito = "Antiinflamatorio"
        )

        repository.addMedication(newMed)

        assertEquals(initialSize + 1, repository.medications.value.size)
        assertTrue(repository.medications.value.any { it.nombre == "Ibuprofeno" })
    }

    @Test
    fun testToggleMedicationActiveState() {
        val med = repository.medications.value.first()
        val medId = med.id
        val initialActiveState = med.activo

        repository.toggleMedicationActive(medId)
        val updatedMed = repository.medications.value.first { it.id == medId }

        assertEquals(!initialActiveState, updatedMed.activo)
    }

    @Test
    fun testAddHealthRecord() {
        val initialCount = repository.healthRecords.value.size
        val newRecord = HealthRecord(
            fecha = "13 Oct 2026",
            dolorEscala = 2,
            estadoAnimo = Mood.EMOCIONADO,
            presionArterial = "115/75",
            glucosa = 90.0,
            peso = 68.0,
            pulso = 72,
            notas = "Excelente día"
        )

        repository.addHealthRecord(newRecord)

        assertEquals(initialCount + 1, repository.healthRecords.value.size)
        assertEquals(Mood.EMOCIONADO, repository.healthRecords.value.first().estadoAnimo)
    }

    @Test
    fun testOfflineActionFIFOQueue() {
        assertFalse(repository.isOfflineMode.value)

        // Activar modo offline
        repository.toggleOfflineMode()
        assertTrue(repository.isOfflineMode.value)

        // Agregar acciones en offline
        repository.addAppointment(
            Appointment(999, "Dr. Pérez", "General", "14 Oct 2026 - 10:00", "Clínica B")
        )
        repository.updateReminderStatus(101, ReminderStatus.TOMADO)

        assertTrue(repository.syncPending.value)
        assertEquals(2, repository.pendingActionsQueue.value.size)
        assertEquals("ADD_APPOINTMENT", repository.pendingActionsQueue.value[0].actionType)
        assertEquals("UPDATE_REMINDER", repository.pendingActionsQueue.value[1].actionType)

        // Volver online y verificar flush
        repository.toggleOfflineMode()
        assertFalse(repository.isOfflineMode.value)
        assertFalse(repository.syncPending.value)
        assertEquals(0, repository.pendingActionsQueue.value.size)
    }

    // --- Pruebas de Validaciones Médicas Clínicas (AHA, ADA, OMS) ---

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
