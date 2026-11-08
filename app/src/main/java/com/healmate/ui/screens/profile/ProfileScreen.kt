package com.healmate.ui.screens.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healmate.data.model.User
import com.healmate.data.util.isBirthDateFormat
import com.healmate.ui.theme.*

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
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabecera del Perfil (RF-01)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(TealPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = user?.nombre ?: "Usuario",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = user?.email ?: "",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            onClearError()
                            showEditDialog = true
                        },
                        enabled = user != null && !saving,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Editar Perfil Médico")
                    }
                }
            }
        }

        // Ficha Médica Digital (RF-01)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Ficha Médica y Antropométrica",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ProfileRow("Fecha de Nacimiento", user?.fechaNacimiento ?: "No registrada")
                    ProfileRow("Peso", user?.peso?.let { "$it kg" } ?: "Sin registrar")
                    ProfileRow("Estatura", user?.altura?.let { "$it cm" } ?: "Sin registrar")
                    ProfileRow("Alergias conocidas", user?.alergias.orEmpty().ifBlank { "Sin registrar" })
                    ProfileRow("Condiciones crónicas", user?.condicionesCronicas.orEmpty().ifBlank { "Sin registrar" })
                    ProfileRow("Contacto de emergencia", user?.contactoEmergencia.orEmpty().ifBlank { "Sin registrar" })
                }
            }
        }

        // Sincronización y Exportación PDF (RF-07, RNF Offline)
        if (isDemo) item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Conectividad y Exportación",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
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
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = isOffline,
                            onCheckedChange = { onToggleOfflineMode() }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))

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
                        colors = ButtonDefaults.buttonColors(containerColor = CyanSecondary),
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = TealPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Seguridad y Protección de Datos",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Usa datos ficticios en la demostración.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showArcoDialog = true }) {
                        Text("Derechos ARCO (Acceso, Rectificación, Cancelación, Oposición)", fontSize = 12.sp, color = TealPrimary)
                    }
                }
            }
        }

        // Botón Cerrar Sesión (RF-08)
        item {
            Button(
                onClick = onLogout,
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RedDanger),
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
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, modifier = Modifier.weight(1f), fontSize = 13.sp, color = TextSecondary)
        Text(text = value, modifier = Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
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
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
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
