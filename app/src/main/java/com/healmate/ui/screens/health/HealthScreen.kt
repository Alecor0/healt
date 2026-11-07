package com.healmate.ui.screens.health

import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healmate.domain.model.HealthRecord
import com.healmate.domain.model.Mood
import com.healmate.domain.validation.MedicalValidator
import com.healmate.ui.theme.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(
    healthRecords: List<HealthRecord>,
    onSaveRecord: (HealthRecord) -> Unit
) {
    val context = LocalContext.current

    var dolorEscala by remember { mutableStateOf(3f) }
    var selectedMood by remember { mutableStateOf(Mood.FELIZ) }
    var presionArterial by remember { mutableStateOf("120/80") }
    var glucosa by remember { mutableStateOf("95") }
    var peso by remember { mutableStateOf("68.5") }
    var pulso by remember { mutableStateOf("78") }
    var notas by remember { mutableStateOf("Me siento mucho mejor hoy pero aún persiste un dolor leve en el costado.") }

    var bpError by remember { mutableStateOf<String?>(null) }
    var glucoseError by remember { mutableStateOf<String?>(null) }
    var pulseError by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Formulario de Registro Diario de Síntomas y Signos Vitales (RF-05)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Registro Diario de Salud",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Fecha: 13 Oct 2026",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Escala de dolor visual / numérica (1 a 10 - EVA/NRS)
                    Text(
                        text = "Nivel de Dolor (Escala EVA 1-10): ${dolorEscala.roundToInt()}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Slider(
                        value = dolorEscala,
                        onValueChange = { dolorEscala = it },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = if (dolorEscala > 6) RedDanger else TealPrimary,
                            activeTrackColor = if (dolorEscala > 6) RedDanger else TealPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Selector de Estado de Ánimo
                    Text(
                        text = "Estado de Ánimo",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Mood.entries.forEach { mood ->
                            val isSelected = selectedMood == mood
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedMood = mood },
                                label = { Text("${mood.icon} ${mood.displayName}", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TealPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Signos Vitales con Validación Médica
                    Text(
                        text = "Signos Vitales",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TealPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = presionArterial,
                            onValueChange = {
                                presionArterial = it
                                bpError = null
                            },
                            label = { Text("Presión (mmHg)", fontSize = 11.sp) },
                            isError = bpError != null,
                            supportingText = bpError?.let { { Text(it, fontSize = 10.sp, color = RedDanger) } },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = glucosa,
                            onValueChange = {
                                glucosa = it
                                glucoseError = null
                            },
                            label = { Text("Glucosa (mg/dL)", fontSize = 11.sp) },
                            isError = glucoseError != null,
                            supportingText = glucoseError?.let { { Text(it, fontSize = 10.sp, color = RedDanger) } },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = peso,
                            onValueChange = { peso = it },
                            label = { Text("Peso (kg)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = pulso,
                            onValueChange = {
                                pulso = it
                                pulseError = null
                            },
                            label = { Text("Pulso (lpm)", fontSize = 11.sp) },
                            isError = pulseError != null,
                            supportingText = pulseError?.let { { Text(it, fontSize = 10.sp, color = RedDanger) } },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Notas Adicionales
                    OutlinedTextField(
                        value = notas,
                        onValueChange = { notas = it },
                        label = { Text("Notas Adicionales de Síntomas") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            // Validación médica estricta (Edge Case 4.6)
                            val (bpValid, bpMsg) = MedicalValidator.validateBloodPressure(presionArterial)
                            if (!bpValid) {
                                bpError = bpMsg
                                Toast.makeText(context, bpMsg, Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val glucDouble = glucosa.toDoubleOrNull() ?: -1.0
                            val (glucValid, glucMsg) = MedicalValidator.validateGlucose(glucDouble)
                            if (!glucValid) {
                                glucoseError = glucMsg
                                Toast.makeText(context, glucMsg, Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val pulseInt = pulso.toIntOrNull() ?: -1
                            val (pulseValid, pulseMsg) = MedicalValidator.validatePulse(pulseInt)
                            if (!pulseValid) {
                                pulseError = pulseMsg
                                Toast.makeText(context, pulseMsg, Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val newRecord = HealthRecord(
                                fecha = "13 Oct 2026",
                                dolorEscala = dolorEscala.roundToInt(),
                                estadoAnimo = selectedMood,
                                presionArterial = presionArterial.trim(),
                                glucosa = glucDouble,
                                peso = peso.toDoubleOrNull() ?: 68.5,
                                pulso = pulseInt,
                                notas = notas
                            )
                            onSaveRecord(newRecord)
                            Toast.makeText(context, "Registro de salud validado y guardado", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardar Registro", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Sección: Historial de Registros
        item {
            Text(
                text = "Historial de Registros (${healthRecords.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        items(healthRecords) { record ->
            HealthRecordCard(record = record)
        }
    }
}

@Composable
private fun HealthRecordCard(record: HealthRecord) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = record.estadoAnimo.icon, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = record.fecha, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(text = "Ánimo: ${record.estadoAnimo.displayName}", fontSize = 12.sp, color = TextSecondary)
                    }
                }

                Surface(
                    color = if (record.dolorEscala > 5) RedDanger.copy(alpha = 0.15f) else GreenSuccess.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Dolor: ${record.dolorEscala}/10",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (record.dolorEscala > 5) RedDanger else GreenSuccess
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                VitalBadge("Presión", record.presionArterial)
                VitalBadge("Glucosa", "${record.glucosa} mg/dL")
                VitalBadge("Peso", "${record.peso} kg")
                VitalBadge("Pulso", "${record.pulso} lpm")
            }

            if (record.notas.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Notas: ${record.notas}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun VitalBadge(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 11.sp, color = TextSecondary)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}
