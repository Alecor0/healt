package com.healmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.healmate.ui.screens.MainScreen
import com.healmate.ui.screens.auth.LoginScreen
import com.healmate.ui.screens.auth.RegisterScreen
import com.healmate.ui.screens.profile.ProfileScreen
import com.healmate.ui.theme.HealthMateTheme
import com.healmate.ui.viewmodel.AccountViewModel
import com.healmate.ui.viewmodel.HealthMateViewModel

class MainActivity : ComponentActivity() {

    private val demoViewModel: HealthMateViewModel by viewModels()
    private val accountViewModel: AccountViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HealthMateTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showDemo by rememberSaveable { mutableStateOf(false) }
                    var showRegistration by rememberSaveable { mutableStateOf(false) }
                    val account by accountViewModel.state.collectAsStateWithLifecycle()

                    when {
                        showDemo -> MainScreen(demoViewModel, onLogout = { showDemo = false })
                        account.user != null -> Surface(Modifier.safeDrawingPadding()) {
                            ProfileScreen(
                                user = account.user,
                                onUpdateProfile = accountViewModel::updateProfile,
                                onLogout = {
                                    accountViewModel.logout()
                                    showRegistration = false
                                },
                                onClearError = accountViewModel::clearError,
                                saving = account.busy,
                                error = account.error
                            )
                        }
                        showRegistration -> RegisterScreen(
                            busy = account.busy,
                            error = account.error,
                            onRegister = accountViewModel::register,
                            onNavigateToLogin = {
                                accountViewModel.clearError()
                                showRegistration = false
                            }
                        )
                        else -> LoginScreen(
                            configured = accountViewModel.isConfigured,
                            busy = account.busy,
                            error = account.error,
                            onLogin = accountViewModel::login,
                            onNavigateToRegister = {
                                accountViewModel.clearError()
                                showRegistration = true
                            },
                            onOpenDemo = { showDemo = true }
                        )
                    }
                }
            }
        }
    }
}
