package com.healmate.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.healmate.ui.components.HealthMateBottomBar
import com.healmate.ui.components.HealthMateTopBar
import com.healmate.ui.navigation.Screen
import com.healmate.ui.screens.appointments.AppointmentsScreen
import com.healmate.ui.screens.dashboard.DashboardScreen
import com.healmate.ui.screens.health.HealthScreen
import com.healmate.ui.screens.medications.MedicationsScreen
import com.healmate.ui.screens.profile.ProfileScreen
import com.healmate.ui.viewmodel.HealthMateViewModel

@Composable
fun MainScreen(
    viewModel: HealthMateViewModel,
    onLogout: () -> Unit
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

    val user by viewModel.currentUser.collectAsState()
    val isOffline by viewModel.isOfflineMode.collectAsState()
    val syncPending by viewModel.syncPending.collectAsState()
    val medications by viewModel.medications.collectAsState()
    val reminders by viewModel.reminders.collectAsState()
    val appointments by viewModel.appointments.collectAsState()
    val healthRecords by viewModel.healthRecords.collectAsState()

    Scaffold(
        topBar = {
            HealthMateTopBar(
                title = "${currentScreen.title} · Demo",
                isOffline = isOffline,
                syncPending = syncPending,
                onOfflineToggle = { viewModel.toggleOfflineMode() }
            )
        },
        bottomBar = {
            HealthMateBottomBar(
                currentRoute = currentScreen.route,
                onNavigate = { newScreen -> currentScreen = newScreen }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.Dashboard -> DashboardScreen(
                    user = user,
                    reminders = reminders,
                    appointments = appointments,
                    onUpdateReminderStatus = { id, status -> viewModel.updateReminderStatus(id, status) },
                    onSnoozeReminder = { id -> viewModel.snoozeReminder(id, 15) },
                    onNavigateToMedications = { currentScreen = Screen.Medications },
                    onNavigateToAppointments = { currentScreen = Screen.Appointments }
                )
                Screen.Medications -> MedicationsScreen(
                    medications = medications,
                    onAddMedication = { med -> viewModel.addMedication(med) },
                    onToggleActive = { id -> viewModel.toggleMedicationActive(id) },
                    onDeleteMedication = { id -> viewModel.deleteMedication(id) }
                )
                Screen.Appointments -> AppointmentsScreen(
                    appointments = appointments,
                    onAddAppointment = { app -> viewModel.addAppointment(app) },
                    onCancelAppointment = { id -> viewModel.cancelAppointment(id) }
                )
                Screen.Health -> HealthScreen(
                    healthRecords = healthRecords,
                    onSaveRecord = { rec -> viewModel.addHealthRecord(rec) }
                )
                Screen.Profile -> ProfileScreen(
                    user = user,
                    isOffline = isOffline,
                    syncPending = syncPending,
                    onUpdateProfile = { updated, onSaved ->
                        viewModel.updateProfile(updated)
                        onSaved()
                    },
                    isDemo = true,
                    onToggleOfflineMode = { viewModel.toggleOfflineMode() },
                    onLogout = onLogout
                )
                else -> DashboardScreen(
                    user = user,
                    reminders = reminders,
                    appointments = appointments,
                    onUpdateReminderStatus = { id, status -> viewModel.updateReminderStatus(id, status) },
                    onSnoozeReminder = { id -> viewModel.snoozeReminder(id, 15) },
                    onNavigateToMedications = { currentScreen = Screen.Medications },
                    onNavigateToAppointments = { currentScreen = Screen.Appointments }
                )
            }
        }
    }
}
