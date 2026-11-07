package com.healmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.healmate.ui.navigation.Screen
import com.healmate.ui.screens.main.MainScreen
import com.healmate.ui.screens.auth.LoginScreen
import com.healmate.ui.screens.auth.RegisterScreen
import com.healmate.ui.theme.HealthMateTheme
import com.healmate.ui.viewmodel.HealthMateViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: HealthMateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HealthMateTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }
                    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

                    if (!isLoggedIn) {
                        when (currentScreen) {
                            Screen.Register -> RegisterScreen(
                                onRegisterSuccess = { nombre, email, pass ->
                                    viewModel.register(nombre, email, pass)
                                    currentScreen = Screen.Main
                                },
                                onNavigateToLogin = { currentScreen = Screen.Login }
                            )
                            else -> LoginScreen(
                                onLoginSuccess = { email, pass ->
                                    viewModel.login(email, pass)
                                    currentScreen = Screen.Main
                                },
                                onNavigateToRegister = { currentScreen = Screen.Register }
                            )
                        }
                    } else {
                        MainScreen(
                            viewModel = viewModel,
                            onLogout = { viewModel.logout() }
                        )
                    }
                }
            }
        }
    }
}
