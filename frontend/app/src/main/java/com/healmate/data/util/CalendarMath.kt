package com.healmate.data.util

import java.util.Calendar

object CalendarMath {

    /**
     * Celdas de la cuadrícula mensual empezando en domingo: `null` para huecos
     * y el número de día en el resto. El tamaño siempre es múltiplo de 7.
     * [month] va de 1 (enero) a 12 (diciembre).
     */
    fun monthCells(year: Int, month: Int): List<Int?> {
        require(month in 1..12) { "Mes fuera de rango: $month" }
        val calendar = Calendar.getInstance().apply {
            clear()
            set(year, month - 1, 1)
        }
        val leading = calendar.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
        val days = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val cells = List<Int?>(leading) { null } + (1..days).toList()
        val trailing = (7 - cells.size % 7) % 7
        return cells + List(trailing) { null }
    }
}
