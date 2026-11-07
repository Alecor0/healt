package com.healmate.domain.repository

import com.healmate.domain.model.OfflineAction
import kotlinx.coroutines.flow.StateFlow

interface ISyncRepository {
    val isOfflineMode: StateFlow<Boolean>
    val syncPending: StateFlow<Boolean>
    val pendingActionsQueue: StateFlow<List<OfflineAction>>
    fun toggleOfflineMode()
    fun flushOfflineQueue(): Int
}
