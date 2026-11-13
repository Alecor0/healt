package com.healmate.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.healmate.data.model.User
import com.healmate.data.util.DateText
import java.util.Calendar
import com.healmate.ui.components.HealthMateBottomBar
import com.healmate.ui.components.HealthMateTopBar
import com.healmate.ui.navigation.Screen
import com.healmate.ui.screens.appointments.AppointmentsScreen
import com.healmate.ui.screens.dashboard.DashboardScreen
import com.healmate.ui.screens.health.HealthScreen
import com.healmate.ui.screens.medications.MedicationsScreen
import com.healmate.ui.screens.profile.ProfileScreen
import com.healmate.ui.theme.Motion
import com.healmate.ui.viewmodel.HealthMateViewModel

/** Perfil de la cuenta con sesión iniciada; se guarda en el servidor. */
class AccountProfile(
    val user: User,
    val saving: Boolean,
    val error: String?,
    val onUpdate: (User, () -> Unit) -> Unit,
    val onClearError: () -> Unit
)

/**
 * Pantalla principal con todos los módulos. Sin [account] es la demostración;
 * con [account] muestra el perfil real de quien inició sesión.
 */
@Composable
fun MainScreen(
    viewModel: HealthMateViewModel,
    onLogout: () -> Unit,
    account: AccountProfile? = null
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

    val demoUser by viewModel.currentUser.collectAsState()
    val user = account?.user ?: demoUser
    val isOffline by viewModel.isOfflineMode.collectAsState()
    val syncPending by viewModel.syncPending.collectAsState()
    val medications by viewModel.medications.collectAsState()
    val reminders by viewModel.reminders.collectAsState()
    val appointments by viewModel.appointments.collectAsState()
    val healthRecords by viewModel.healthRecords.collectAsState()
    val syncError by viewModel.syncError.collectAsState()

    // La demostración transcurre en su día de ejemplo; la cuenta real, hoy.
    val today = remember(account == null) { if (account == null) DateText.demoToday() else Calendar.getInstance() }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) { viewModel.refresh() }
    LaunchedEffect(syncError) {
        val message = syncError ?: return@LaunchedEffect
        val retry = snackbar.showSnackbar(message, actionLabel = "Reintentar", duration = SnackbarDuration.Long)
        viewModel.clearSyncError()
        if (retry == SnackbarResult.ActionPerformed) viewModel.refresh()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            HealthMateTopBar(
                title = currentScreen.title,
                isOffline = isOffline,
                syncPending = syncPending,
                onOfflineToggle = if (account == null) viewModel::toggleOfflineMode else null
            )
        },
        bottomBar = {
            HealthMateBottomBar(
                currentRoute = currentScreen.route,
                onNavigate = { newScreen -> currentScreen = newScreen }
            )
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentScreen,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            transitionSpec = {
                // Desliza un poco hacia el lado de la pestaña elegida, con fundido.
                val direction = Screen.tabDirection(initialState, targetState)
                val enter = fadeIn(Motion.standard()) +
                    slideInHorizontally(Motion.standard()) { width -> direction * width / 8 }
                val exit = fadeOut(Motion.standard(Motion.SHORT)) +
                    slideOutHorizontally(Motion.standard()) { width -> -direction * width / 8 }
                enter togetherWith exit
            },
            label = "tab"
        ) { screen ->
            Box(Modifier.fillMaxSize()) {
                when (screen) {
                    Screen.Medications -> MedicationsScreen(
                        medications = medications,
                        onAddMedication = { med -> viewModel.addMedication(med) },
                        onToggleActive = { id -> viewModel.toggleMedicationActive(id) },
                        onDeleteMedication = { id -> viewModel.deleteMedication(id) },
                        today = DateText.isoDate(today)
                    )
                    Screen.Appointments -> AppointmentsScreen(
                        appointments = appointments,
                        onAddAppointment = { app -> viewModel.addAppointment(app) },
                        onCancelAppointment = { id -> viewModel.cancelAppointment(id) },
                        today = today
                    )
                    Screen.Health -> HealthScreen(
                        healthRecords = healthRecords,
                        onSaveRecord = { rec -> viewModel.addHealthRecord(rec) },
                        today = DateText.shortDate(today)
                    )
                    Screen.Profile -> if (account != null) {
                        ProfileScreen(
                            user = account.user,
                            onUpdateProfile = account.onUpdate,
                            onLogout = onLogout,
                            saving = account.saving,
                            error = account.error,
                            onClearError = account.onClearError
                        )
                    } else {
                        ProfileScreen(
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
                    }
                    else -> DashboardScreen(
                        user = user,
                        reminders = reminders,
                        appointments = appointments,
                        healthRecords = healthRecords,
                        onUpdateReminderStatus = { id, status -> viewModel.updateReminderStatus(id, status) },
                        onSnoozeReminder = { id -> viewModel.snoozeReminder(id, 15) },
                        onNavigateToMedications = { currentScreen = Screen.Medications },
                        onNavigateToAppointments = { currentScreen = Screen.Appointments },
                        onNavigateToHealth = { currentScreen = Screen.Health },
                        today = DateText.longDate(today)
                    )
                }
            }
        }
    }
}
