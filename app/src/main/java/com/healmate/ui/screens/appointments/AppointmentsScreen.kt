package com.healmate.ui.screens.appointments

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healmate.data.model.Appointment
import com.healmate.data.model.AppointmentStatus
import com.healmate.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentsScreen(
    appointments: List<Appointment>,
    onAddAppointment: (Appointment) -> Unit,
    onCancelAppointment: (Int) -> Unit
) {
    var selectedDay by remember { mutableStateOf(13) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Días que tienen citas agendadas
    val daysWithAppointments = remember(appointments) {
        appointments.mapNotNull { app ->
            val dayPart = app.fechaHora.split(" ").firstOrNull()?.toIntOrNull()
            dayPart
        }.toSet()
    }

    // Citas filtradas para el día seleccionado
    val filteredAppointments = appointments.filter { app ->
        val dayPart = app.fechaHora.split(" ").firstOrNull()?.toIntOrNull()
        dayPart == selectedDay
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = TealPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Programar Cita")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Encabezado de Calendario Mensual (RF-04)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Octubre de 2026",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Row {
                                IconButton(onClick = {}) { Icon(Icons.Default.ChevronLeft, contentDescription = null) }
                                IconButton(onClick = {}) { Icon(Icons.Default.ChevronRight, contentDescription = null) }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Encabezados de días de la semana
                        val daysOfWeek = listOf("Do", "Lu", "Ma", "Mi", "Ju", "Vi", "Sá")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            daysOfWeek.forEach { day ->
                                Text(
                                    text = day,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    modifier = Modifier.width(36.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Cuadrícula de 31 días del mes
                        Column {
                            for (week in 0..4) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    for (dayOfWeek in 0..6) {
                                        val dayNumber = week * 7 + dayOfWeek + 1
                                        if (dayNumber <= 31) {
                                            val isSelected = dayNumber == selectedDay
                                            val hasAppointment = daysWithAppointments.contains(dayNumber)
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(
                                                        when {
                                                            isSelected -> TealPrimary
                                                            hasAppointment -> TealPrimary.copy(alpha = 0.2f)
                                                            else -> Color.Transparent
                                                        },
                                                        CircleShape
                                                    )
                                                    .clickable { selectedDay = dayNumber },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = dayNumber.toString(),
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isSelected || hasAppointment) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (hasAppointment && !isSelected) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.dp)
                                                                .background(TealPrimary, CircleShape)
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.size(36.dp))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }

            // Sección: Citas del día seleccionado
            item {
                Text(
                    text = "Citas del $selectedDay de Octubre (${filteredAppointments.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            if (filteredAppointments.isNotEmpty()) {
                items(filteredAppointments) { app ->
                    AppointmentCard(
                        appointment = app,
                        onCancel = { onCancelAppointment(app.id) }
                    )
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No hay citas programadas para el $selectedDay de Octubre.",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddAppointmentDialog(
            defaultDay = selectedDay,
            onDismiss = { showAddDialog = false },
            onConfirm = { newApp ->
                onAddAppointment(newApp)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AppointmentCard(
    appointment: Appointment,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(CyanSecondary.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = CyanSecondary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appointment.especialidad,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "${appointment.especialista} • ${appointment.fechaHora}",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = appointment.lugar,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                // Estado de la Cita (RF-04)
                Surface(
                    color = when (appointment.estado) {
                        AppointmentStatus.CONFIRMADA -> GreenSuccess.copy(alpha = 0.15f)
                        AppointmentStatus.RECORDATORIO -> OrangeWarning.copy(alpha = 0.15f)
                        AppointmentStatus.CANCELADA -> RedDanger.copy(alpha = 0.15f)
                        else -> TealPrimary.copy(alpha = 0.15f)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = appointment.estado.label,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (appointment.estado) {
                            AppointmentStatus.CONFIRMADA -> GreenSuccess
                            AppointmentStatus.RECORDATORIO -> OrangeWarning
                            AppointmentStatus.CANCELADA -> RedDanger
                            else -> TealPrimary
                        }
                    )
                }
            }

            if (appointment.notas.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Notas: ${appointment.notas}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(appointment.lugar)}"))
                    context.startActivity(intent)
                }) {
                    Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ver Mapa", fontSize = 12.sp)
                }

                if (appointment.estado != AppointmentStatus.CANCELADA) {
                    TextButton(onClick = onCancel) {
                        Text("Cancelar Cita", fontSize = 12.sp, color = RedDanger)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddAppointmentDialog(
    defaultDay: Int,
    onDismiss: () -> Unit,
    onConfirm: (Appointment) -> Unit
) {
    var especialista by remember { mutableStateOf("") }
    var especialidad by remember { mutableStateOf("") }
    var hora by remember { mutableStateOf("11:00") }
    var lugar by remember { mutableStateOf("") }
    var notas by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Programar Cita Médica", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = especialista,
                    onValueChange = { especialista = it },
                    label = { Text("Nombre del Especialista / Médico *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = especialidad,
                    onValueChange = { especialidad = it },
                    label = { Text("Especialidad Médica *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = hora,
                    onValueChange = { hora = it },
                    label = { Text("Hora de la Cita (ej. 10:30) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = lugar,
                    onValueChange = { lugar = it },
                    label = { Text("Lugar / Centro Médico *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text("Notas o Instrucciones Previas") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (especialista.isNotBlank() && especialidad.isNotBlank() && lugar.isNotBlank()) {
                        val newApp = Appointment(
                            id = (200..999).random(),
                            especialista = especialista,
                            especialidad = especialidad,
                            fechaHora = "$defaultDay Oct 2026 - $hora",
                            lugar = lugar,
                            estado = AppointmentStatus.CONFIRMADA,
                            notas = notas
                        )
                        onConfirm(newApp)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text("Confirmar Cita")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
