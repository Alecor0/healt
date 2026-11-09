package com.healmate.ui.screens.health

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healmate.data.model.HealthRecord
import com.healmate.data.model.Mood
import com.healmate.data.util.MedicalValidator
import com.healmate.data.util.PainSeverity
import com.healmate.data.util.VitalsErrors
import com.healmate.data.util.VitalsFormValidator
import com.healmate.data.util.VitalsInput
import com.healmate.ui.components.*
import kotlin.math.roundToInt

@Composable
fun HealthScreen(
    healthRecords: List<HealthRecord>,
    onSaveRecord: (HealthRecord) -> Unit
) {
    val context = LocalContext.current

    var dolorEscala by remember { mutableFloatStateOf(3f) }
    var selectedMood by remember { mutableStateOf(Mood.FELIZ) }
    var presionArterial by remember { mutableStateOf("120/80") }
    var glucosa by remember { mutableStateOf("95") }
    var peso by remember { mutableStateOf("68.5") }
    var pulso by remember { mutableStateOf("78") }
    var notas by remember { mutableStateOf("") }
    var errors by remember { mutableStateOf(VitalsErrors()) }

    val pain = dolorEscala.roundToInt()
    val severity = MedicalValidator.painSeverity(pain)
    val severityColor = severityColor(severity)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Registro diario de síntomas y signos vitales (RF-05)
        item {
            HealthCard {
                Text("Registro diario", style = MaterialTheme.typography.titleLarge)
                Text(
                    "13 Oct 2026",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Nivel de dolor", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Text("$pain/10", style = MaterialTheme.typography.titleMedium, color = severityColor)
                    Spacer(Modifier.width(8.dp))
                    StatusPill(severity.label, severityColor)
                }
                Slider(
                    value = dolorEscala,
                    onValueChange = { dolorEscala = it },
                    valueRange = 1f..10f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = severityColor,
                        activeTrackColor = severityColor
                    )
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Sin dolor", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Máximo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(Modifier.height(16.dp))
                Text("Estado de ánimo", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Mood.entries.forEach { mood ->
                        MoodOption(
                            mood = mood,
                            selected = selectedMood == mood,
                            onClick = { selectedMood = mood },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text("Signos vitales", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VitalField(
                        value = presionArterial,
                        onChange = { presionArterial = it; errors = errors.copy(presionArterial = null) },
                        label = "Presión (mmHg)",
                        error = errors.presionArterial,
                        keyboardType = KeyboardType.Text,
                        modifier = Modifier.weight(1f)
                    )
                    VitalField(
                        value = glucosa,
                        onChange = { glucosa = it; errors = errors.copy(glucosa = null) },
                        label = "Glucosa (mg/dL)",
                        error = errors.glucosa,
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VitalField(
                        value = peso,
                        onChange = { peso = it; errors = errors.copy(peso = null) },
                        label = "Peso (kg)",
                        error = errors.peso,
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                    VitalField(
                        value = pulso,
                        onChange = { pulso = it; errors = errors.copy(pulso = null) },
                        label = "Pulso (lpm)",
                        error = errors.pulso,
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text("Notas de síntomas (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        // Validación médica antes de guardar (caso límite 4.6)
                        val result = VitalsFormValidator.validate(VitalsInput(presionArterial, glucosa, peso, pulso))
                        errors = result
                        if (!result.isValid) return@Button
                        onSaveRecord(
                            HealthRecord(
                                fecha = "13 Oct 2026",
                                dolorEscala = pain,
                                estadoAnimo = selectedMood,
                                presionArterial = presionArterial.trim(),
                                glucosa = VitalsFormValidator.parseDecimal(glucosa)!!,
                                peso = VitalsFormValidator.parseDecimal(peso)!!,
                                pulso = pulso.trim().toInt(),
                                notas = notas.trim()
                            )
                        )
                        notas = ""
                        Toast.makeText(context, "Registro guardado", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Guardar registro")
                }
            }
        }

        item { SectionHeader("Historial (${healthRecords.size})") }

        if (healthRecords.isEmpty()) {
            item { EmptyState(Icons.Default.MonitorHeart, "Tus registros guardados aparecerán aquí.") }
        }
        items(healthRecords, key = { it.id }) { record ->
            HealthRecordCard(record)
        }
    }
}

@Composable
private fun severityColor(severity: PainSeverity): Color = when (severity) {
    PainSeverity.LEVE -> MaterialTheme.colorScheme.secondary
    PainSeverity.MODERADO -> MaterialTheme.colorScheme.tertiary
    PainSeverity.SEVERO -> MaterialTheme.colorScheme.error
}

@Composable
private fun MoodOption(mood: Mood, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) colors.primaryContainer else colors.surface,
        border = androidx.compose.foundation.BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) colors.primary else colors.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(mood.icon, fontSize = 24.sp)
            Text(
                mood.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun VitalField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    error: String?,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        modifier = modifier
    )
}

@Composable
private fun HealthRecordCard(record: HealthRecord) {
    val severity = MedicalValidator.painSeverity(record.dolorEscala)
    HealthCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(record.estadoAnimo.icon, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(record.fecha, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Ánimo: ${record.estadoAnimo.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusPill("Dolor ${record.dolorEscala}/10", severityColor(severity))
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            VitalStat("Presión", record.presionArterial, Modifier.weight(1f))
            VitalStat("Glucosa", "${record.glucosa.formatVital()} mg/dL", Modifier.weight(1f))
            VitalStat("Peso", "${record.peso.formatVital()} kg", Modifier.weight(1f))
            VitalStat("Pulso", "${record.pulso} lpm", Modifier.weight(1f))
        }
        if (record.notas.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(
                record.notas,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun VitalStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

/** 95.0 → "95", 68.5 → "68.5" */
private fun Double.formatVital(): String = if (this % 1.0 == 0.0) toInt().toString() else toString()
