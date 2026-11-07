package com.healmate.domain.repository

import com.healmate.domain.model.Reminder
import com.healmate.domain.model.ReminderStatus
import kotlinx.coroutines.flow.StateFlow

interface IReminderRepository {
    val reminders: StateFlow<List<Reminder>>
    fun updateReminderStatus(reminderId: Int, newStatus: ReminderStatus)
    fun snoozeReminder(reminderId: Int, additionalMinutes: Int = 15)
}
