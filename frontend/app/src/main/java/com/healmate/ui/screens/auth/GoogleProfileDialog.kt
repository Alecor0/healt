package com.healmate.ui.screens.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.healmate.data.util.isBirthDateFormat

@Composable
fun GoogleProfileDialog(
    busy: Boolean,
    error: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var birthDate by rememberSaveable { mutableStateOf("") }
    var consent by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Completa tu perfil") },
        text = {
            Column {
                Text("Ingresa tu fecha de nacimiento para crear tu cuenta en HealthMate.")
                OutlinedTextField(
                    value = birthDate,
                    onValueChange = { birthDate = it },
                    enabled = !busy,
                    label = { Text("Nacimiento (AAAA-MM-DD)") },
                    singleLine = true
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = consent, onCheckedChange = { consent = it }, enabled = !busy)
                    Text("Acepto el aviso de privacidad.")
                }
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(birthDate) },
                enabled = !busy && consent && isBirthDateFormat(birthDate)
            ) {
                Text(if (busy) "Creando cuenta…" else "Crear cuenta")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancelar") }
        }
    )
}
