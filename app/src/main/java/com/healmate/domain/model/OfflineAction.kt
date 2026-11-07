package com.healmate.domain.model

import java.util.UUID

/**
 * Acción registrada en cola durante el modo offline (Invariante FIFO y RNF de portabilidad).
 */
data class OfflineAction(
    val id: String = UUID.randomUUID().toString(),
    val actionType: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)
