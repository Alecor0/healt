package com.healmate.ui.screens.appointments

import com.healmate.ui.theme.CardTone
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
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
import com.healmate.data.util.DateText
import java.util.Calendar
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import com.healmate.ui.components.*
import com.healmate.ui.theme.Motion

@Composable
fun AppointmentsScreen(
    appointments: List<Appointment>,
    onAddAppointment: (Appointment) -> Unit,
    onCancelAppointment: (Int) -> Unit,
    today: Calendar = DateText.demoToday()
) {
    // Mes que muestra el calendario (1 a 12); empieza en el mes actual.
    var year by remember { mutableIntStateOf(today.get(Calendar.YEAR)) }
    var month by remember { mutableIntStateOf(today.get(Calendar.MONTH) + 1) }
    var selectedDay by remember { mutableIntStateOf(today.get(Calendar.DAY_OF_MONTH)) }
    val monthName = DateText.monthName(month)
    fun moveMonth(step: Int) {
        val moved = Calendar.getInstance().apply {
            clear()
            set(year, month - 1, 1)
            add(Calendar.MONTH, step)
        }
        year = moved.get(Calendar.YEAR)
        month = moved.get(Calendar.MONTH) + 1
        selectedDay = 1
    }
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingCancel by remember { mutableStateOf<Appointment?>(null) }

    val daysWithAppointments = remember(appointments, year, month) {
        appointments
            .filter { it.estado != AppointmentStatus.CANCELADA }
            .mapNotNull { it.dayIn(year, month) }
            .toSet()
    }
    val filteredAppointments = appointments.filter { it.dayIn(year, month) == selectedDay }

    val listState = rememberLazyListState()
    val fabExpanded by rememberFabExpanded(listState)
    val haptics = LocalHapticFeedback.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                expanded = fabExpanded,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Programar") },
                modifier = Modifier.pressScale()
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Calendario mensual (RF-04)
            item {
                Box(Modifier.staggeredEntrance(0)) {
                    MonthCalendar(
                        year = year,
                        month = month,
                        onPreviousMonth = { moveMonth(-1) },
                        onNextMonth = { moveMonth(1) },
                        selectedDay = selectedDay,
                        daysWithAppointments = daysWithAppointments,
                        onSelectDay = {
                            if (it != selectedDay) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedDay = it
                        }
                    )
                }
            }

            item {
                // El encabezado del día se desliza hacia el lado del día elegido.
                AnimatedContent(
                    targetState = selectedDay to filteredAppointments.size,
                    transitionSpec = {
                        val forward = targetState.first >= initialState.first
                        (slideInHorizontally { if (forward) it / 3 else -it / 3 } + fadeIn()) togetherWith
                            (slideOutHorizontally { if (forward) -it / 3 else it / 3 } + fadeOut())
                    },
                    label = "dayHeader",
                    modifier = Modifier.staggeredEntrance(1)
                ) { (day, count) ->
                    SectionHeader("$day de $monthName · $count citas")
                }
            }

            if (filteredAppointments.isEmpty()) {
                item(key = "empty") {
                    Box(Modifier.animateItem()) {
                        EmptyState(Icons.Default.EventAvailable, "No hay citas programadas para este día.")
                    }
                }
            }
            itemsIndexed(filteredAppointments, key = { _, it -> it.id }) { index, app ->
                AppointmentCard(
                    appointment = app,
                    onCancel = { pendingCancel = app },
                    modifier = Modifier.animateItem().staggeredEntrance(2 + index)
                )
            }
        }
    }

    if (showAddDialog) {
        AddAppointmentDialog(
            dateText = DateText.shortDate(year, month, selectedDay),
            title = "Programar cita · $selectedDay de $monthName",
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
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onCancelAppointment(app.id)
                pendingCancel = null
            },
            onDismiss = { pendingCancel = null }
        )
    }
}

/** Día del mes de la cita si cae en ese mes: "13 Oct 2026 - 10:00" → 13 en octubre de 2026. */
internal fun Appointment.dayIn(year: Int, month: Int): Int? {
    val day = fechaHora.substringBefore(" ").toIntOrNull() ?: return null
    return day.takeIf { fechaHora.startsWith(DateText.shortDate(year, month, day) + " ") }
}

@Composable
private fun MonthCalendar(
    year: Int,
    month: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    selectedDay: Int,
    daysWithAppointments: Set<Int>,
    onSelectDay: (Int) -> Unit
) {
    val cells = remember(year, month) { CalendarMath.monthCells(year, month) }
    val monthName = DateText.monthName(month)
    val colors = MaterialTheme.colorScheme

    HealthCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${monthName.replaceFirstChar { it.uppercase() }} de $year",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onPreviousMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mes anterior")
            }
            IconButton(onClick = onNextMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Mes siguiente")
            }
        }
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
                            val circle by animateColorAsState(
                                if (selected) colors.primary else Color.Transparent, Motion.gentle(), label = "day"
                            )
                            val dayText by animateColorAsState(
                                if (selected) colors.onPrimary else colors.onSurface, Motion.gentle(), label = "dayText"
                            )
                            val scale by animateFloatAsState(if (selected) 1f else 0.92f, Motion.playful(), label = "dayScale")
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .graphicsLayer { scaleX = scale; scaleY = scale }
                                    .clip(CircleShape)
                                    .background(circle)
                                    .clickable { onSelectDay(day) }
                                    .semantics {
                                        contentDescription = "$day de $monthName" + if (hasAppointment) ", con citas" else ""
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = day.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (selected || hasAppointment) FontWeight.Bold else FontWeight.Normal,
                                        color = dayText
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
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
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
        modifier = modifier,
        tone = CardTone.TEAL,
        overlay = if (cancelled) Color.Black.copy(alpha = 0.35f) else Color.Transparent
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
        Spacer(Modifier.height(4.dp))
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
    dateText: String,
    title: String,
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
        title = { Text(title) },
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
                        fechaHora = "$dateText - ${hora.trim()}",
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
