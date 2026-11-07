package com.healmate.domain.repository

import com.healmate.domain.model.HealthRecord
import kotlinx.coroutines.flow.StateFlow

interface IHealthRecordRepository {
    val healthRecords: StateFlow<List<HealthRecord>>
    fun addHealthRecord(record: HealthRecord)
}
