package com.healmate.ui.screens.dashboard

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.healmate.data.model.Appointment
import com.healmate.data.model.AppointmentStatus
import com.healmate.data.model.HealthRecord
import com.healmate.data.model.Reminder
import com.healmate.data.model.ReminderStatus
import com.healmate.data.model.User
import com.healmate.data.util.HealthTrends
import com.healmate.data.util.MedicalValidator
import com.healmate.ui.components.*
import com.healmate.ui.theme.BrandMint

@Composable
fun DashboardScreen(
    user: User?,
    reminders: List<Reminder>,
    appointments: List<Appointment>,
    healthRecords: List<HealthRecord>,
    onUpdateReminderStatus: (Int, ReminderStatus) -> Unit,
    onSnoozeReminder: (Int) -> Unit,
    onNavigateToMedications: () -> Unit,
    onNavigateToAppointments: () -> Unit,
    onNavigateToHealth: () -> Unit
) {
    val pendingReminders = reminders.filter {
        it.estado == ReminderStatus.PENDIENTE || it.estado == ReminderStatus.POSPUESTO
    }
    val nextAppointment = appointments.firstOrNull { it.estado != AppointmentStatus.CANCELADA }

    // Cálculo clínico de adherencia terapéutica (RF-06)
    val takenDoses = reminders.count { it.estado == ReminderStatus.TOMADO }
    val adherence = MedicalValidator.calculateAdherence(takenDoses, reminders.size)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SummaryPanel(
                name = user?.nombre ?: "Usuario",
                takenDoses = takenDoses,
                totalDoses = reminders.size,
                adherence = adherence,
                pending = pendingReminders.size
            )
        }

        // Próximos recordatorios (RF-03)
        item {
            SectionHeader("Próximos recordatorios", "Ver todos", onNavigateToMedications)
        }
        if (pendingReminders.isEmpty()) {
            item {
                EmptyState(Icons.Default.TaskAlt, "Completaste todos tus recordatorios de medicación de hoy.")
            }
        } else {
            items(pendingReminders, key = { it.id }) { reminder ->
                ReminderCard(
                    reminder = reminder,
                    onTake = { onUpdateReminderStatus(reminder.id, ReminderStatus.TOMADO) },
                    onSnooze = { onSnoozeReminder(reminder.id) },
                    onOmit = { onUpdateReminderStatus(reminder.id, ReminderStatus.OMITIDO) }
                )
            }
        }

        // Próxima cita médica (RF-04)
        item {
            SectionHeader("Próxima cita médica", "Calendario", onNavigateToAppointments)
        }
        item {
            if (nextAppointment != null) {
                NextAppointmentCard(nextAppointment)
            } else {
                EmptyState(Icons.Default.EventAvailable, "No tienes citas programadas.")
            }
        }

        // Resumen de salud (RF-06)
        item {
            SectionHeader("Resumen de salud", "Registrar", onNavigateToHealth)
        }
        item {
            HealthSummaryCard(healthRecords)
        }
    }
}

@Composable
private fun SummaryPanel(
    name: String,
    takenDoses: Int,
    totalDoses: Int,
    adherence: Int,
    pending: Int
) {
    BrandPanel {
        BrandLabel("RESUMEN DIARIO")
        Text(text = "Hola, $name", style = MaterialTheme.typography.headlineSmall)
        Text("13 de octubre de 2026", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Adherencia terapéutica", style = MaterialTheme.typography.titleSmall)
                Text(
                    "$takenDoses de $totalDoses tomas · $pending pendientes",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = "$adherence%",
                color = BrandMint,
                style = MaterialTheme.typography.displaySmall
            )
        }
        LinearProgressIndicator(
            progress = { adherence / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = BrandMint,
            trackColor = Color.White.copy(alpha = 0.18f),
            strokeCap = StrokeCap.Round,
            drawStopIndicator = {}
        )
    }
}

@Composable
private fun ReminderCard(
    reminder: Reminder,
    onTake: () -> Unit,
    onSnooze: () -> Unit,
    onOmit: () -> Unit
) {
    HealthCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Default.Medication, MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(reminder.nombreMedicamento, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${reminder.dosis} · ${reminder.horaProgramada}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (reminder.pospuestoMinutos > 0) {
                StatusPill("+${reminder.pospuestoMinutos} min", MaterialTheme.colorScheme.tertiary)
            }
        }
        Spacer(Modifier.height(12.dp))
        // Acciones conformes a RF-03: Tomada, Pospuesta, Omitida
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onTake,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Tomada")
            }
            OutlinedButton(onClick = onSnooze, modifier = Modifier.weight(1f)) {
                Text("Posponer 15 min", maxLines = 1)
            }
            TextButton(onClick = onOmit) {
                Text("Omitir", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun NextAppointmentCard(appointment: Appointment) {
    val context = LocalContext.current
    HealthCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Default.MedicalServices, MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(appointment.especialidad, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${appointment.especialista} · ${appointment.fechaHora}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = appointment.lugar,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    val uri = Uri.parse("geo:0,0?q=${Uri.encode(appointment.lugar)}")
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Ver mapa")
            }
            FilledTonalButton(
                onClick = {
                    Toast.makeText(context, "Contactando a ${appointment.especialista}...", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Llamar")
            }
        }
    }
}

@Composable
private fun HealthSummaryCard(records: List<HealthRecord>) {
    val latest = records.firstOrNull()
    if (latest == null) {
        EmptyState(Icons.Default.MonitorHeart, "Aún no hay registros de salud. Registra tus síntomas para ver tu tendencia.")
        return
    }
    val trend = HealthTrends.painTrend(records)
    val averagePain = HealthTrends.averagePain(records)

    HealthCard {
        Text("Evolución del dolor", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Últimos ${trend.size} registros · escala 1–10",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        PainTrendChart(
            points = trend,
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .semantics {
                    contentDescription = "Gráfico de dolor: ${records.take(trend.size).reversed().joinToString { "${it.dolorEscala}" }}"
                }
        )
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            SummaryStat("Dolor promedio", "${averagePain ?: "-"}/10", Modifier.weight(1f))
            SummaryStat("Ánimo", "${latest.estadoAnimo.icon} ${latest.estadoAnimo.displayName}", Modifier.weight(1f))
            SummaryStat("Presión", latest.presionArterial, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PainTrendChart(points: List<Float>, modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val dotFill = MaterialTheme.colorScheme.surface
    Canvas(modifier) {
        listOf(0f, 0.5f, 1f).forEach { level ->
            val y = size.height * level
            drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
        if (points.isEmpty()) return@Canvas
        val pad = 6.dp.toPx()
        val offsets = points.mapIndexed { index, value ->
            val x = if (points.size == 1) size.width / 2 else pad + (size.width - 2 * pad) * index / (points.size - 1)
            // Más dolor = punto más alto
            Offset(x, pad + (size.height - 2 * pad) * (1f - value))
        }
        val path = Path().apply {
            moveTo(offsets.first().x, offsets.first().y)
            offsets.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(path, line, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        offsets.forEach {
            drawCircle(line, radius = 5.dp.toPx(), center = it)
            drawCircle(dotFill, radius = 2.5.dp.toPx(), center = it)
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.titleSmall, maxLines = 1)
    }
}
