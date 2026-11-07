package com.healmate.domain.repository

import com.healmate.domain.model.Medication
import kotlinx.coroutines.flow.StateFlow

interface IMedicationRepository {
    val medications: StateFlow<List<Medication>>
    fun addMedication(med: Medication)
    fun toggleMedicationActive(medId: Int)
    fun deleteMedication(medId: Int)
}
