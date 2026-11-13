package com.healmate.ui.navigation

sealed class Screen(val route: String, val title: String = "") {
    object Login : Screen("login", "Iniciar Sesión")
    object Register : Screen("register", "Registro de Usuario")
    object Main : Screen("main", "HealthMate")

    // Bottom Navigation Tabs
    object Dashboard : Screen("dashboard", "Inicio")
    object Medications : Screen("medications", "Medicamentos")
    object Appointments : Screen("appointments", "Citas")
    object Health : Screen("health", "Salud")
    object Profile : Screen("profile", "Perfil")

    companion object {
        /** Orden de las pestañas en la barra inferior. */
        val tabs: List<Screen> by lazy { listOf(Dashboard, Medications, Appointments, Health, Profile) }

        /**
         * Dirección de la transición entre pestañas: 1 si la nueva está a la derecha,
         * -1 si está a la izquierda y 0 si es la misma o no es una pestaña.
         */
        fun tabDirection(from: Screen, to: Screen): Int {
            val fromIndex = tabs.indexOf(from)
            val toIndex = tabs.indexOf(to)
            if (fromIndex < 0 || toIndex < 0) return 0
            return toIndex.compareTo(fromIndex)
        }
    }
}
