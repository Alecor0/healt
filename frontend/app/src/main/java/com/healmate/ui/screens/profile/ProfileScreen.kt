package com.healmate.ui.screens.profile

import com.healmate.ui.theme.CardTone
import com.healmate.ui.components.staggeredEntrance
import com.healmate.ui.components.pressScale
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healmate.data.model.User
import com.healmate.data.util.isBirthDateFormat
import com.healmate.ui.components.BrandLabel
import com.healmate.ui.components.BrandPanel
import com.healmate.ui.components.HealthCard
import com.healmate.ui.theme.BrandMint

@Composable
fun ProfileScreen(
    user: User?,
    onUpdateProfile: (User, () -> Unit) -> Unit,
    onLogout: () -> Unit,
    isDemo: Boolean = false,
    isOffline: Boolean = false,
    syncPending: Boolean = false,
    saving: Boolean = false,
    error: String? = null,
    onClearError: () -> Unit = {},
    onToggleOfflineMode: () -> Unit = {}
) {
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }
    var showArcoDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            BrandPanel(Modifier.staggeredEntrance(0)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier.size(52.dp)
                            .background(BrandMint.copy(alpha = 0.16f), MaterialTheme.shapes.medium),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = BrandMint)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        BrandLabel("MI PERFIL")
                        Text(
                            text = user?.nombre ?: "Usuario",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }
                user?.let {
                    Text(it.email, style = MaterialTheme.typography.bodyMedium)
                }
                FilledTonalButton(
                    onClick = {
                        onClearError()
                        showEditDialog = true
                    },
                    modifier = Modifier.pressScale(),
                    enabled = user != null && !saving,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Editar perfil")
                }
            }
        }

        // Ficha Médica Digital (RF-01)
        item {
            HealthCard(Modifier.staggeredEntrance(1), tone = CardTone.SLATE) {
                Column {
                    Text(
                        text = "Ficha de salud",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val fields = listOf(
                        "Fecha de nacimiento" to (user?.fechaNacimiento ?: "Sin registrar"),
                        "Peso" to (user?.peso?.let { "$it kg" } ?: "Sin registrar"),
                        "Estatura" to (user?.altura?.let { "$it cm" } ?: "Sin registrar"),
                        "Alergias conocidas" to user?.alergias.orEmpty().ifBlank { "Sin registrar" },
                        "Condiciones crónicas" to user?.condicionesCronicas.orEmpty().ifBlank { "Sin registrar" },
                        "Contacto de emergencia" to user?.contactoEmergencia.orEmpty().ifBlank { "Sin registrar" }
                    )
                    fields.forEachIndexed { index, (label, value) ->
                        ProfileRow(label, value)
                    }
                }
            }
        }

        // Sincronización y Exportación PDF (RF-07, RNF Offline)
        if (isDemo) item {
            HealthCard(Modifier.staggeredEntrance(2), tone = CardTone.TEAL) {
                Column {
                    Text(
                        text = "Conectividad y Exportación",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Modo Fuera de Línea (Offline)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isOffline) {
                                    if (syncPending) "Hay acciones en la cola de demostración." else "Modo sin conexión simulado."
                                } else "Demostración con datos temporales.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isOffline,
                            onCheckedChange = { onToggleOfflineMode() }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botón de exportar PDF (RF-07)
                    Button(
                        onClick = {
                            Toast.makeText(
                                context,
                                "La exportación PDF aún no está disponible.",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exportar Historial Médico a PDF")
                    }
                }
            }
        }

        // Seguridad y Derechos ARCO (Normativa Mexicana e Internacional)
        if (isDemo) item {
            HealthCard(Modifier.staggeredEntrance(3), tone = CardTone.GREEN) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Seguridad y Protección de Datos",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Usa datos ficticios en la demostración.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showArcoDialog = true }) {
                        Text("Derechos ARCO (Acceso, Rectificación, Cancelación, Oposición)", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Botón Cerrar Sesión (RF-08)
        item {
            Button(
                onClick = onLogout,
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().staggeredEntrance(4).pressScale(0.97f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isDemo) "Salir de la demostración" else "Cerrar sesión", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showEditDialog && user != null) {
        EditProfileDialog(
            user = user,
            saving = saving,
            error = error,
            onDismiss = {
                onClearError()
                showEditDialog = false
            },
            onConfirm = { updated ->
                onUpdateProfile(updated) { showEditDialog = false }
            }
        )
    }

    if (showArcoDialog) {
        AlertDialog(
            onDismissRequest = { showArcoDialog = false },
            title = { Text("Derechos ARCO", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Conforme a la Ley Federal de Protección de Datos Personales (LFPDPPP), tienes derecho a Acceder a tus datos de salud, Rectificar información inexacta, Cancelar el tratamiento de tus datos o ejercer Oposición respecto a su procesamiento con fines estadísticos.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showArcoDialog = false }) {
                    Text("Entendido")
                }
            }
        )
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = label, modifier = Modifier.weight(1f), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, modifier = Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun EditProfileDialog(
    user: User,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (User) -> Unit
) {
    var nombre by remember { mutableStateOf(user.nombre) }
    var fecha by remember { mutableStateOf(user.fechaNacimiento) }
    var peso by remember { mutableStateOf(user.peso?.toString().orEmpty()) }
    var altura by remember { mutableStateOf(user.altura?.toString().orEmpty()) }
    var alergias by remember { mutableStateOf(user.alergias) }
    var condiciones by remember { mutableStateOf(user.condicionesCronicas) }
    var contacto by remember { mutableStateOf(user.contactoEmergencia) }

    val weight = peso.trim().replace(',', '.').toDoubleOrNull()
    val height = altura.trim().toIntOrNull()
    val validWeight = peso.isBlank() || weight != null && weight.isFinite() && weight > 0
    val validHeight = altura.isBlank() || height != null && height > 0
    val validDate = isBirthDateFormat(fecha)

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Editar Perfil Médico", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    enabled = !saving,
                    label = { Text("Nombre Completo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fecha,
                    onValueChange = { fecha = it },
                    enabled = !saving,
                    label = { Text("Nacimiento (AAAA-MM-DD)") },
                    isError = !validDate,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = peso,
                        onValueChange = { peso = it },
                        enabled = !saving,
                        label = { Text("Peso (kg)") },
                        isError = !validWeight,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = altura,
                        onValueChange = { altura = it },
                        enabled = !saving,
                        label = { Text("Estatura (cm)") },
                        isError = !validHeight,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = alergias,
                    onValueChange = { alergias = it },
                    enabled = !saving,
                    label = { Text("Alergias") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
                OutlinedTextField(
                    value = condiciones,
                    onValueChange = { condiciones = it },
                    enabled = !saving,
                    label = { Text("Condiciones Crónicas") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = contacto,
                    onValueChange = { contacto = it },
                    enabled = !saving,
                    label = { Text("Contacto de Emergencia") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !saving && nombre.trim().length in 1..100 && contacto.length <= 100 &&
                    validDate && validWeight && validHeight,
                onClick = {
                    val updated = user.copy(
                        nombre = nombre.trim(),
                        fechaNacimiento = fecha.trim(),
                        peso = weight,
                        altura = height,
                        alergias = alergias,
                        condicionesCronicas = condiciones,
                        contactoEmergencia = contacto
                    )
                    onConfirm(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(if (saving) "Guardando…" else "Guardar cambios")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) {
                Text("Cancelar")
            }
        }
    )
}
