package com.healmate.data.util

import com.healmate.data.model.HealthRecord
import kotlin.math.roundToInt

/** Cálculos de tendencia a partir de los registros guardados (RF-06). */
object HealthTrends {

    /** Registros más recientes primero, como los guarda el repositorio. */
    private const val WEEK = 7

    /**
     * Puntos de dolor de los últimos 7 registros, del más antiguo al más reciente,
     * escalados a 0..1 sobre la escala fija EVA/NRS 1–10.
     */
    fun painTrend(records: List<HealthRecord>): List<Float> =
        records.take(WEEK).reversed().map { (it.dolorEscala.coerceIn(1, 10) - 1) / 9f }

    /** Dolor promedio de los últimos 7 registros, o `null` si no hay registros. */
    fun averagePain(records: List<HealthRecord>): Int? {
        val recent = records.take(WEEK)
        if (recent.isEmpty()) return null
        return recent.map { it.dolorEscala }.average().roundToInt()
    }
}
