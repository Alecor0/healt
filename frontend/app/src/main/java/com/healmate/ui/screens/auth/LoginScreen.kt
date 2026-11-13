package com.healmate.ui.screens.auth

import com.healmate.ui.components.pressScale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.healmate.R

@Composable
fun LoginScreen(
    configured: Boolean,
    googleConfigured: Boolean,
    busy: Boolean,
    error: String?,
    onLogin: (String, String) -> Unit,
    onGoogleLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onOpenDemo: () -> Unit
) {
    AuthLayout(
        title = "Tu salud, a tu ritmo",
        description = "Medicamentos, citas y registros de salud en un mismo espacio.",
        formTitle = "Bienvenido"
    ) {
        when {
            error != null -> AuthMessage(error, isError = true)
            !configured -> AuthMessage("El servidor aún no está configurado.", isError = false)
        }
        if (busy) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Iniciando sesión…", style = MaterialTheme.typography.bodySmall)
        }
        SlowServerHint(busy)
        // El correo siempre está disponible; Google es un acceso adicional y solo aparece si está configurado.
        EmailLoginFields(configured && !busy, onLogin, onNavigateToRegister)
        if (googleConfigured) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            OutlinedButton(
                onClick = onGoogleLogin,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).pressScale(0.97f),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, Color(0xFF747775)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF1F1F1F)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_google_sign_in),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text("Continuar con Google", fontWeight = FontWeight.Medium)
            }
        }
        TextButton(
            onClick = onOpenDemo,
            enabled = !busy,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Explorar demostración")
        }
    }
}

@Composable
private fun EmailLoginFields(
    enabled: Boolean,
    onLogin: (String, String) -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        label = { Text("Correo electrónico") },
        leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        label = { Text("Contraseña") },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }, enabled = enabled) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                )
            }
        },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Button(
        onClick = { onLogin(email, password) },
        enabled = enabled && email.isNotBlank() && password.isNotBlank(),
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).pressScale(0.97f)
    ) {
        Text("Iniciar sesión")
    }
    TextButton(onClick = onNavigateToRegister, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Text("Crear una cuenta con correo")
    }
}
