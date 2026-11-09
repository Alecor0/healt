package com.healmate.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healmate.data.util.isBirthDateFormat

@Composable
fun RegisterScreen(
    busy: Boolean,
    error: String?,
    onRegister: (String, String, String, String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var acceptTerms by remember { mutableStateOf(false) }

    AuthLayout(
        title = "Crea tu cuenta",
        description = "Empieza con tu perfil personal de HealthMate.",
        formTitle = "Datos de registro"
    ) {
        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            enabled = !busy,
            label = { Text("Nombre Completo") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = birthDate,
            onValueChange = { birthDate = it },
            enabled = !busy,
            label = { Text("Nacimiento (AAAA-MM-DD)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            enabled = !busy,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            label = { Text("Correo Electrónico") },
            leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            enabled = !busy,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            supportingText = { Text("Usa de 12 a 128 caracteres.") },
            label = { Text("Contraseña") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = acceptTerms,
                enabled = !busy,
                onCheckedChange = { acceptTerms = it }
            )
            Text(
                text = "Acepto el Aviso de Privacidad y Consentimiento de Datos de Salud (LFPDPPP).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (error != null) {
            AuthMessage(error, isError = true)
        }
        Button(
            onClick = { onRegister(nombre, email, password, birthDate) },
            enabled = !busy && acceptTerms && nombre.trim().length in 1..100 &&
                isBirthDateFormat(birthDate) &&
                email.isNotBlank() && password.codePointCount(0, password.length) in 12..128,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (busy) "Creando cuenta…" else "Registrarse", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        TextButton(onClick = onNavigateToLogin, enabled = !busy) {
            Text("¿Ya tienes cuenta? Inicia Sesión", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}
