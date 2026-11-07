package com.healmate.domain.model

/**
 * Entidad Usuario conforme a RF-01 y Tabla 6 del informe técnico.
 */
data class User(
    val id: Int = 1,
    val nombre: String = "Alfredo Hernández",
    val email: String = "alfredo.hernandez@example.com",
    val fechaNacimiento: String = "1992-05-14",
    val peso: Double = 68.5, // kg
    val altura: Int = 175, // cm
    val alergias: String = "Penicilina",
    val condicionesCronicas: String = "Hipertensión, Diabetes Tipo 2",
    val contactoEmergencia: String = "María Hernández (Esposa) - 555-0192"
)
