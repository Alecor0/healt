package com.healmate.ui.screens.appointments

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.healmate.data.model.Appointment
import com.healmate.data.model.AppointmentStatus
import com.healmate.data.util.CalendarMath
import com.healmate.ui.components.*

// Mes de la demostración: los datos de ejemplo están en octubre de 2026.
private const val DEMO_YEAR = 2026
private const val DEMO_MONTH = 10

@Composable
fun AppointmentsScreen(
    appointments: List<Appointment>,
    onAddAppointment: (Appointment) -> Unit,
    onCancelAppointment: (Int) -> Unit
) {
    var selectedDay by remember { mutableIntStateOf(13) }
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingCancel by remember { mutableStateOf<Appointment?>(null) }

    val daysWithAppointments = remember(appointments) {
        appointments
            .filter { it.estado != AppointmentStatus.CANCELADA }
            .mapNotNull { it.dayOfMonth() }
            .toSet()
    }
    val filteredAppointments = appointments.filter { it.dayOfMonth() == selectedDay }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Programar") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Calendario mensual (RF-04)
            item {
                MonthCalendar(
                    selectedDay = selectedDay,
                    daysWithAppointments = daysWithAppointments,
                    onSelectDay = { selectedDay = it }
                )
            }

            item { SectionHeader("$selectedDay de octubre · ${filteredAppointments.size} citas") }

            if (filteredAppointments.isEmpty()) {
                item { EmptyState(Icons.Default.EventAvailable, "No hay citas programadas para este día.") }
            }
            items(filteredAppointments, key = { it.id }) { app ->
                AppointmentCard(appointment = app, onCancel = { pendingCancel = app })
            }
        }
    }

    if (showAddDialog) {
        AddAppointmentDialog(
            day = selectedDay,
            onDismiss = { showAddDialog = false },
            onConfirm = { newApp ->
                onAddAppointment(newApp)
                showAddDialog = false
            }
        )
    }

    pendingCancel?.let { app ->
        ConfirmDialog(
            title = "Cancelar cita",
            message = "¿Cancelar la cita de ${app.especialidad} con ${app.especialista} (${app.fechaHora})?",
            confirmLabel = "Cancelar cita",
            onConfirm = {
                onCancelAppointment(app.id)
                pendingCancel = null
            },
            onDismiss = { pendingCancel = null }
        )
    }
}

/** "13 Oct 2026 - 10:00" → 13 */
private fun Appointment.dayOfMonth(): Int? = fechaHora.split(" ").firstOrNull()?.toIntOrNull()

@Composable
private fun MonthCalendar(
    selectedDay: Int,
    daysWithAppointments: Set<Int>,
    onSelectDay: (Int) -> Unit
) {
    val cells = remember { CalendarMath.monthCells(DEMO_YEAR, DEMO_MONTH) }
    val colors = MaterialTheme.colorScheme

    HealthCard {
        Text("Octubre de 2026", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            listOf("Do", "Lu", "Ma", "Mi", "Ju", "Vi", "Sá").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day != null) {
                            val selected = day == selectedDay
                            val hasAppointment = day in daysWithAppointments
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) colors.primary else Color.Transparent)
                                    .clickable { onSelectDay(day) }
                                    .semantics {
                                        contentDescription = "$day de octubre" + if (hasAppointment) ", con citas" else ""
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = day.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (selected || hasAppointment) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) colors.onPrimary else colors.onSurface
                                    )
                                    Box(
                                        Modifier
                                            .size(5.dp)
                                            .background(
                                                when {
                                                    !hasAppointment -> Color.Transparent
                                                    selected -> colors.onPrimary
                                                    else -> colors.secondary
                                                },
                                                CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppointmentCard(
    appointment: Appointment,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val cancelled = appointment.estado == AppointmentStatus.CANCELADA
    val statusColor = when (appointment.estado) {
        AppointmentStatus.CONFIRMADA -> MaterialTheme.colorScheme.secondary
        AppointmentStatus.RECORDATORIO -> MaterialTheme.colorScheme.tertiary
        AppointmentStatus.CANCELADA -> MaterialTheme.colorScheme.error
        AppointmentStatus.PENDIENTE -> MaterialTheme.colorScheme.primary
    }

    HealthCard(
        containerColor = if (cancelled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Default.Event, statusColor)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(appointment.especialidad, style = MaterialTheme.typography.titleMedium)
                Text(
                    appointment.especialista,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusPill(appointment.estado.label, statusColor)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Detail(Icons.Default.Schedule, appointment.fechaHora.substringAfter(" - ", appointment.fechaHora))
            Detail(Icons.Default.Place, appointment.lugar)
        }
        if (appointment.notas.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                appointment.notas,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = {
                val uri = Uri.parse("geo:0,0?q=${Uri.encode(appointment.lugar)}")
                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            }) {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Ver mapa")
            }
            if (!cancelled) {
                TextButton(onClick = onCancel) {
                    Text("Cancelar cita", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun Detail(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}

@Composable
private fun AddAppointmentDialog(
    day: Int,
    onDismiss: () -> Unit,
    onConfirm: (Appointment) -> Unit
) {
    var especialista by remember { mutableStateOf("") }
    var especialidad by remember { mutableStateOf("") }
    var hora by remember { mutableStateOf("11:00") }
    var lugar by remember { mutableStateOf("") }
    var notas by remember { mutableStateOf("") }
    var showErrors by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Programar cita · $day de octubre") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DialogField(especialista, { especialista = it }, "Especialista / médico *", showErrors)
                DialogField(especialidad, { especialidad = it }, "Especialidad *", showErrors)
                DialogField(hora, { hora = it }, "Hora (ej. 10:30) *", showErrors)
                DialogField(lugar, { lugar = it }, "Lugar / centro médico *", showErrors)
                DialogField(notas, { notas = it }, "Notas o indicaciones previas", showErrors = false)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (listOf(especialista, especialidad, hora, lugar).any { it.isBlank() }) {
                    showErrors = true
                    return@Button
                }
                onConfirm(
                    Appointment(
                        id = (200..999).random(),
                        especialista = especialista.trim(),
                        especialidad = especialidad.trim(),
                        fechaHora = "$day Oct 2026 - ${hora.trim()}",
                        lugar = lugar.trim(),
                        estado = AppointmentStatus.CONFIRMADA,
                        notas = notas.trim()
                    )
                )
            }) { Text("Confirmar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Volver") } }
    )
}

@Composable
private fun DialogField(value: String, onChange: (String) -> Unit, label: String, showErrors: Boolean) {
    val error = showErrors && value.isBlank()
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        isError = error,
        supportingText = if (error) {
            { Text("Campo obligatorio") }
        } else null,
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}
