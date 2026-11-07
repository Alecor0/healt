package com.healmate.domain.model

/**
 * Registro de estado de ánimo (RF-05).
 */
enum class Mood(val displayName: String, val icon: String) {
    TRISTE("Triste", "😢"),
    NORMAL("Normal", "😐"),
    FELIZ("Feliz", "😊"),
    EMOCIONADO("Emocionado", "🤩")
}
