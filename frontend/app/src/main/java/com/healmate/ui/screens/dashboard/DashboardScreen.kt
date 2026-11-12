package com.healmate.ui.screens.dashboard

import com.healmate.ui.theme.CardTone
import com.healmate.ui.theme.TAKEN_OVERLAY_ALPHA
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.delay
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.PathMeasure
import com.healmate.ui.theme.Motion
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
import com.healmate.data.util.DateText
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
    onNavigateToHealth: () -> Unit,
    today: String = DateText.longDate(DateText.demoToday())
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
                modifier = Modifier.staggeredEntrance(0),
                name = user?.nombre ?: "Usuario",
                today = today,
                takenDoses = takenDoses,
                totalDoses = reminders.size,
                adherence = adherence,
                pending = pendingReminders.size
            )
        }

        // Próximos recordatorios (RF-03)
        item {
            SectionHeader(
                "Próximos recordatorios", "Ver todos", onNavigateToMedications,
                Modifier.staggeredEntrance(1)
            )
        }
        if (pendingReminders.isEmpty()) {
            item(key = "remindersEmpty") {
                Box(Modifier.animateItem().staggeredEntrance(2)) {
                    EmptyState(Icons.Default.TaskAlt, "Completaste todos tus recordatorios de medicación de hoy.")
                }
            }
        } else {
            itemsIndexed(pendingReminders, key = { _, it -> it.id }) { index, reminder ->
                ReminderCard(
                    modifier = Modifier.animateItem().staggeredEntrance(2 + index),
                    reminder = reminder,
                    onTake = { onUpdateReminderStatus(reminder.id, ReminderStatus.TOMADO) },
                    onSnooze = { onSnoozeReminder(reminder.id) },
                    onOmit = { onUpdateReminderStatus(reminder.id, ReminderStatus.OMITIDO) }
                )
            }
        }

        // Próxima cita médica (RF-04)
        item {
            SectionHeader(
                "Próxima cita médica", "Calendario", onNavigateToAppointments,
                Modifier.staggeredEntrance(3 + pendingReminders.size)
            )
        }
        item {
            Box(Modifier.staggeredEntrance(4 + pendingReminders.size)) {
                if (nextAppointment != null) {
                    NextAppointmentCard(nextAppointment)
                } else {
                    EmptyState(Icons.Default.EventAvailable, "No tienes citas programadas.")
                }
            }
        }

        // Resumen de salud (RF-06)
        item {
            SectionHeader(
                "Resumen de salud", "Registrar", onNavigateToHealth,
                Modifier.staggeredEntrance(5 + pendingReminders.size)
            )
        }
        item {
            Box(Modifier.staggeredEntrance(6 + pendingReminders.size)) {
                HealthSummaryCard(healthRecords)
            }
        }
    }
}

@Composable
private fun SummaryPanel(
    name: String,
    today: String,
    takenDoses: Int,
    totalDoses: Int,
    adherence: Int,
    pending: Int,
    modifier: Modifier = Modifier
) {
    // Arranca en 0 y se llena al entrar; luego sigue cada cambio de adherencia.
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val progress by animateFloatAsState(
        if (started) adherence / 100f else 0f, tween(1_200, easing = FastOutSlowInEasing), label = "adherence"
    )
    val shownPercent by animateIntAsState(
        if (started) adherence else 0, tween(1_200, easing = FastOutSlowInEasing), label = "adherencePercent"
    )

    Box(modifier) {
        BrandPanel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BrandLabel("RESUMEN DIARIO")
                    Text(text = "Hola, $name", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        today,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Adherencia terapéutica", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "$takenDoses de $totalDoses tomas · $pending pendientes",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(Modifier.width(12.dp))
                AdherenceRing(progress = progress, percent = shownPercent)
            }
        }
    }
}

/** Anillo de adherencia: el arco crece con un degradado y un punto de luz late en la punta. */
@Composable
private fun AdherenceRing(progress: Float, percent: Int) {
    val pulse by rememberPulse(1_800)
    Box(
        modifier = Modifier
            .size(104.dp)
            .semantics { contentDescription = "Adherencia terapéutica: $percent por ciento" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 10.dp.toPx()
            val inset = stroke / 2 + 4.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            drawArc(Color.White.copy(alpha = 0.14f), 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            val sweep = 360f * progress
            if (sweep > 0f) {
                rotate(-90f) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            0f to BrandMint.copy(alpha = 0.70f),
                            progress.coerceAtLeast(0.01f) to BrandMint
                        ),
                        startAngle = 0f, sweepAngle = sweep, useCenter = false,
                        topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
                val radius = arcSize.width / 2
                val angle = Math.toRadians((sweep - 90f).toDouble())
                val tip = Offset(
                    center.x + radius * kotlin.math.cos(angle).toFloat(),
                    center.y + radius * kotlin.math.sin(angle).toFloat()
                )
                drawCircle(BrandMint.copy(alpha = 0.35f * (1f - pulse)), stroke * (0.6f + pulse), tip)
                drawCircle(Color.White, stroke * 0.28f, tip)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$percent%", color = BrandMint, style = MaterialTheme.typography.titleLarge)
            Text("adherencia", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReminderCard(
    reminder: Reminder,
    onTake: () -> Unit,
    onSnooze: () -> Unit,
    onOmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    // Al marcarla, primero se celebra y después sale de la lista.
    var taken by remember(reminder.id) { mutableStateOf(false) }
    LaunchedEffect(taken) {
        if (taken) {
            delay(650)
            onTake()
        }
    }

    HealthCard(
        modifier,
        tone = CardTone.SLATE,
        overlay = if (taken) MaterialTheme.colorScheme.secondary.copy(alpha = TAKEN_OVERLAY_ALPHA) else Color.Transparent
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                if (taken) Icons.Default.CheckCircle else Icons.Default.Medication,
                if (taken) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(reminder.nombreMedicamento, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${reminder.dosis} · ${reminder.horaProgramada}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(
                visible = reminder.pospuestoMinutos > 0,
                enter = scaleIn(Motion.playful()) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                StatusPill("+${reminder.pospuestoMinutos} min", MaterialTheme.colorScheme.tertiary)
            }
        }
        Spacer(Modifier.height(12.dp))
        // Acciones conformes a RF-03: Tomada, Pospuesta, Omitida
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (!taken) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        taken = true
                    }
                },
                modifier = Modifier.pressScale(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                AnimatedContent(
                    targetState = taken,
                    transitionSpec = {
                        (scaleIn(Motion.playful(), initialScale = 0.4f) + fadeIn()) togetherWith
                            (scaleOut(targetScale = 0.4f) + fadeOut())
                    },
                    label = "take"
                ) { done ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (done) Icons.Default.TaskAlt else Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (done) "¡Listo!" else "Tomada")
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSnooze()
                },
                enabled = !taken,
                modifier = Modifier.pressScale()
            ) { Text("+15 min") }
            TextButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onOmit()
                },
                enabled = !taken,
                modifier = Modifier.pressScale()
            ) {
                Text("Omitir", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun NextAppointmentCard(appointment: Appointment) {
    val context = LocalContext.current
    HealthCard(tone = CardTone.TEAL) {
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
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(
                onClick = {
                    val uri = Uri.parse("geo:0,0?q=${Uri.encode(appointment.lugar)}")
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                }
            ) {
                Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Ver mapa")
            }
            TextButton(
                onClick = {
                    Toast.makeText(context, "Contactando a ${appointment.especialista}...", Toast.LENGTH_SHORT).show()
                }
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

    HealthCard(tone = CardTone.GREEN) {
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
    val pulse by rememberPulse()

    // La línea se dibuja de izquierda a derecha cada vez que cambian los datos.
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(points) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, Motion.standard(Motion.LONG + 200))
    }

    Canvas(modifier) {
        drawLine(grid, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
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
        val measure = PathMeasure().apply { setPath(path, false) }
        val visible = Path()
        measure.getSegment(0f, measure.length * reveal.value, visible, true)
        val revealX = offsets.first().x + (offsets.last().x - offsets.first().x) * reveal.value

        // Relleno degradado bajo la línea, que avanza junto con ella.
        val area = Path().apply {
            addPath(path)
            lineTo(offsets.last().x, size.height)
            lineTo(offsets.first().x, size.height)
            close()
        }
        clipRect(right = revealX + 1f) {
            drawPath(area, Brush.verticalGradient(listOf(line.copy(alpha = 0.28f), Color.Transparent)))
        }
        drawPath(visible, line, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))

        // El registro más reciente late cuando la línea termina de dibujarse.
        if (reveal.value >= 1f) {
            val last = offsets.last()
            drawCircle(line.copy(alpha = 0.35f * (1f - pulse)), 4.dp.toPx() + 12.dp.toPx() * pulse, last)
        }
        offsets.forEach {
            if (it.x <= revealX + 0.5f) {
                drawCircle(line, radius = 4.dp.toPx(), center = it)
                drawCircle(dotFill, radius = 2.dp.toPx(), center = it)
            }
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
