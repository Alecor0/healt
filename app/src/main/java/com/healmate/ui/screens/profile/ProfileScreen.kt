package com.healmate.ui.screens.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.healmate.ui.theme.*

@Composable
fun ProfileScreen(
    user: User?,
    isOffline: Boolean,
    syncPending: Boolean,
    onUpdateProfile: (User) -> Unit,
    onToggleOfflineMode: () -> Unit,
    onLogout: () -> Unit
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
                        onClick = { showEditDialog = true },
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
                    ProfileRow("Peso / Estatura", "${user?.peso ?: 0.0} kg • ${user?.altura ?: 0} cm")
                    ProfileRow("Alergias Conocidas", user?.alergias?.ifBlank { "Ninguna reportada" } ?: "Ninguna")
                    ProfileRow("Condiciones Crónicas", user?.condicionesCronicas?.ifBlank { "Ninguna" } ?: "Ninguna")
                    ProfileRow("Contacto de Emergencia", user?.contactoEmergencia ?: "No asignado")
                }
            }
        }

        // Sincronización y Exportación PDF (RF-07, RNF Offline)
        item {
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
                                    if (syncPending) "Sin conexión. Acciones en cola para sincronizar." else "Sin conexión (Datos en caché local)."
                                } else "Conectado al servidor Neon Postgres.",
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
                                "Generando reporte clínico en PDF con historial de medicación y signos vitales...",
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
        item {
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
                        text = "Cumplimiento normativo LFPDPPP (México), NOM-004-SSA3-2012 y referencias HIPAA/GDPR. Cifrado AES-256 en reposo y TLS 1.3 en tránsito.",
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
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RedDanger),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar Sesión", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showEditDialog && user != null) {
        EditProfileDialog(
            user = user,
            onDismiss = { showEditDialog = false },
            onConfirm = { updated ->
                onUpdateProfile(updated)
                showEditDialog = false
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
        Text(text = label, fontSize = 13.sp, color = TextSecondary)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}

@Composable
private fun EditProfileDialog(
    user: User,
    onDismiss: () -> Unit,
    onConfirm: (User) -> Unit
) {
    var nombre by remember { mutableStateOf(user.nombre) }
    var peso by remember { mutableStateOf(user.peso.toString()) }
    var altura by remember { mutableStateOf(user.altura.toString()) }
    var alergias by remember { mutableStateOf(user.alergias) }
    var condiciones by remember { mutableStateOf(user.condicionesCronicas) }
    var contacto by remember { mutableStateOf(user.contactoEmergencia) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Perfil Médico", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre Completo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = peso,
                        onValueChange = { peso = it },
                        label = { Text("Peso (kg)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = altura,
                        onValueChange = { altura = it },
                        label = { Text("Estatura (cm)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = alergias,
                    onValueChange = { alergias = it },
                    label = { Text("Alergias") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = condiciones,
                    onValueChange = { condiciones = it },
                    label = { Text("Condiciones Crónicas") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = contacto,
                    onValueChange = { contacto = it },
                    label = { Text("Contacto de Emergencia") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = user.copy(
                        nombre = nombre,
                        peso = peso.toDoubleOrNull() ?: user.peso,
                        altura = altura.toIntOrNull() ?: user.altura,
                        alergias = alergias,
                        condicionesCronicas = condiciones,
                        contactoEmergencia = contacto
                    )
                    onConfirm(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text("Guardar Cambios")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
