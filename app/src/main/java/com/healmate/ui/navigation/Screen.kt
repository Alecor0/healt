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
}
