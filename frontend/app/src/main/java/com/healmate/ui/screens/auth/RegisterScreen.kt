package com.healmate.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healmate.data.util.RegistrationErrors
import com.healmate.data.util.formatBirthDateInput
import com.healmate.data.util.validateRegistration
import com.healmate.ui.components.pressScale

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
    var birthDate by remember { mutableStateOf(TextFieldValue("")) }
    var acceptTerms by remember { mutableStateOf(false) }
    // Los errores aparecen al pulsar "Registrarse" y se borran al corregir cada campo.
    var errors by remember { mutableStateOf(RegistrationErrors()) }

    AuthLayout(
        title = "Crea tu cuenta",
        description = "Empieza con tu perfil personal de HealthMate.",
        formTitle = "Datos de registro"
    ) {
        RegisterField(
            value = nombre,
            onChange = { nombre = it; errors = errors.copy(nombre = null) },
            label = "Nombre completo",
            icon = Icons.Default.Person,
            error = errors.nombre,
            enabled = !busy
        )

        OutlinedTextField(
            value = birthDate,
            onValueChange = {
                // Solo dígitos; los guiones se ponen solos y el cursor queda al final.
                val formatted = formatBirthDateInput(it.text)
                birthDate = TextFieldValue(formatted, TextRange(formatted.length))
                errors = errors.copy(fechaNacimiento = null)
            },
            enabled = !busy,
            label = { Text("Fecha de nacimiento") },
            placeholder = { Text("AAAA-MM-DD") },
            leadingIcon = { Icon(Icons.Default.Cake, contentDescription = null) },
            isError = errors.fechaNacimiento != null,
            supportingText = { Text(errors.fechaNacimiento ?: "Solo números, ej. 19920514") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        RegisterField(
            value = email,
            onChange = { email = it; errors = errors.copy(email = null) },
            label = "Correo electrónico",
            icon = Icons.Default.Mail,
            error = errors.email,
            enabled = !busy,
            keyboardType = KeyboardType.Email
        )

        RegisterField(
            value = password,
            onChange = { password = it; errors = errors.copy(password = null) },
            label = "Contraseña",
            icon = Icons.Default.Lock,
            error = errors.password,
            enabled = !busy,
            keyboardType = KeyboardType.Password,
            hint = "Mínimo 12 caracteres (${password.codePointCount(0, password.length)}/12)",
            visualTransformation = PasswordVisualTransformation()
        )

        // Toda la fila marca la casilla, no solo el cuadrito.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = acceptTerms,
                    enabled = !busy,
                    role = Role.Checkbox,
                    onValueChange = { acceptTerms = it; errors = errors.copy(consentimiento = null) }
                )
                .padding(vertical = 4.dp)
        ) {
            Checkbox(checked = acceptTerms, onCheckedChange = null, enabled = !busy)
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Acepto el Aviso de Privacidad y Consentimiento de Datos de Salud (LFPDPPP).",
                style = MaterialTheme.typography.bodySmall,
                color = if (errors.consentimiento != null) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (error != null) {
            AuthMessage(error, isError = true)
        }
        SlowServerHint(busy)

        Button(
            onClick = {
                val result = validateRegistration(nombre, birthDate.text, email, password, acceptTerms)
                errors = result
                if (result.isValid) onRegister(nombre, email, password, birthDate.text)
            },
            enabled = !busy,
            modifier = Modifier
                .pressScale(0.97f)
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(if (busy) "Creando cuenta…" else "Registrarse", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        TextButton(onClick = onNavigateToLogin, enabled = !busy) {
            Text("¿Ya tienes cuenta? Inicia sesión", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun RegisterField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    error: String?,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    hint: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        enabled = enabled,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        isError = error != null,
        supportingText = (error ?: hint)?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}
