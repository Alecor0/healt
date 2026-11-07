package com.healmate.ui.screens.dashboard

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healmate.data.model.Appointment
import com.healmate.data.model.Reminder
import com.healmate.data.model.ReminderStatus
import com.healmate.data.model.User
import com.healmate.data.util.MedicalValidator
import com.healmate.ui.theme.*

@Composable
fun DashboardScreen(
    user: User?,
    reminders: List<Reminder>,
    appointments: List<Appointment>,
    onUpdateReminderStatus: (Int, ReminderStatus) -> Unit,
    onSnoozeReminder: (Int) -> Unit,
    onNavigateToMedications: () -> Unit,
    onNavigateToAppointments: () -> Unit
) {
    val context = LocalContext.current
    val pendingReminders = reminders.filter { it.estado == ReminderStatus.PENDIENTE }
    val todayAppointment = appointments.firstOrNull()

    // Cálculo clínico de adherencia terapéutica (RF-06)
    val takenDoses = reminders.count { it.estado == ReminderStatus.TOMADO }
    val totalDoses = reminders.size
    val adherencePercentage = MedicalValidator.calculateAdherence(takenDoses, totalDoses)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tarjeta de Bienvenida y Adherencia Terapéutica
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "¡Buenos días, ${user?.nombre ?: "Usuario"}!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Hoy es 13 de Octubre de 2026",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Adherencia terapéutica: $adherencePercentage% ($takenDoses de $totalDoses tomas)",
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Sección 1: PRÓXIMOS RECORDATORIOS (RF-03)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PRÓXIMOS RECORDATORIOS",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                TextButton(onClick = onNavigateToMedications) {
                    Text("Ver todos", color = TealPrimary, fontSize = 12.sp)
                }
            }
        }

        if (pendingReminders.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = GreenSuccess
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("¡Excelente! Has completado todos tus recordatorios de medicación por hoy.", fontSize = 14.sp)
                    }
                }
            }
        } else {
            items(pendingReminders) { reminder ->
                ReminderDashboardCard(
                    reminder = reminder,
                    onTake = { onUpdateReminderStatus(reminder.id, ReminderStatus.TOMADO) },
                    onSnooze = { onSnoozeReminder(reminder.id) },
                    onOmit = { onUpdateReminderStatus(reminder.id, ReminderStatus.OMITIDO) }
                )
            }
        }

        // Sección 2: PRÓXIMA CITA MÉDICA (RF-04)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PRÓXIMA CITA MÉDICA",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                TextButton(onClick = onNavigateToAppointments) {
                    Text("Calendario", color = TealPrimary, fontSize = 12.sp)
                }
            }

            todayAppointment?.let { app ->
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
                                    imageVector = Icons.Default.MedicalServices,
                                    contentDescription = null,
                                    tint = CyanSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Cita ${app.especialidad}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "${app.especialista} • ${app.fechaHora}",
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = app.lugar,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(app.lugar)}")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                    context.startActivity(mapIntent)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ver Mapa", fontSize = 12.sp)
                            }
                            Button(
                                onClick = {
                                    Toast.makeText(context, "Contactando a ${app.especialista}...", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanSecondary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Llamar", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Sección 3: RESUMEN DE SALUD SEMANAL (RF-06)
        item {
            Text(
                text = "RESUMEN DE SALUD SEMANAL",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Evolución de Síntomas",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Tendencia de bienestar (Últimos 7 días)",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    ) {
                        val path = Path().apply {
                            moveTo(0f, size.height * 0.7f)
                            lineTo(size.width * 0.2f, size.height * 0.5f)
                            lineTo(size.width * 0.4f, size.height * 0.65f)
                            lineTo(size.width * 0.6f, size.height * 0.3f)
                            lineTo(size.width * 0.8f, size.height * 0.25f)
                            lineTo(size.width, size.height * 0.2f)
                        }
                        drawPath(
                            path = path,
                            color = TealPrimary,
                            style = Stroke(width = 6f)
                        )
                        val points = listOf(
                            Offset(0f, size.height * 0.7f),
                            Offset(size.width * 0.2f, size.height * 0.5f),
                            Offset(size.width * 0.4f, size.height * 0.65f),
                            Offset(size.width * 0.6f, size.height * 0.3f),
                            Offset(size.width * 0.8f, size.height * 0.25f),
                            Offset(size.width, size.height * 0.2f)
                        )
                        points.forEach { pt ->
                            drawCircle(color = CyanSecondary, radius = 8f, center = pt)
                            drawCircle(color = Color.White, radius = 4f, center = pt)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        SymptomBadge("Dolor Promedio", "3/10", GreenSuccess)
                        SymptomBadge("Ánimo", "Positivo", TealPrimary)
                        SymptomBadge("Presión", "120/80", CyanSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderDashboardCard(
    reminder: Reminder,
    onTake: () -> Unit,
    onSnooze: () -> Unit,
    onOmit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(TealPrimary.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Medication,
                        contentDescription = null,
                        tint = TealPrimary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reminder.nombreMedicamento,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${reminder.dosis} • ${reminder.horaProgramada}" +
                                if (reminder.pospuestoMinutos > 0) " (+${reminder.pospuestoMinutos} min)" else "",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botones de acción conformes a RF-03 ("Tomada", "Pospuesta", "Omitida")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onTake,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tomado", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onSnooze,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Posponer", fontSize = 11.sp, color = OrangeWarning)
                }

                TextButton(
                    onClick = onOmit,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(0.9f)
                ) {
                    Text("Omitir", fontSize = 11.sp, color = RedDanger)
                }
            }
        }
    }
}

@Composable
private fun SymptomBadge(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "$label: ", fontSize = 12.sp, color = TextSecondary)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
