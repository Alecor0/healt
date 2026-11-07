package com.healmate.domain.model

/**
 * Entidad Medicamento conforme a RF-02 y Tablas 7 y 8 del informe técnico.
 */
data class Medication(
    val id: Int,
    val nombre: String,
    val dosis: String,
    val frecuencia: String,
    val viaAdministracion: String = "Oral",
    val proximaHora: String,
    val fechaInicio: String = "2026-10-01",
    val fechaFin: String? = null,
    val activo: Boolean = true,
    val proposito: String = ""
)
