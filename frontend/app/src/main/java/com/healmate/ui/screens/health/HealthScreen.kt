package com.healmate.ui.screens.health

import com.healmate.ui.theme.CardTone
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.animation.core.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healmate.data.model.HealthRecord
import com.healmate.data.model.Mood
import com.healmate.data.util.DateText
import com.healmate.data.util.MedicalValidator
import com.healmate.data.util.PainSeverity
import com.healmate.data.util.VitalsErrors
import com.healmate.data.util.VitalsFormValidator
import com.healmate.data.util.VitalsInput
import com.healmate.ui.components.*
import com.healmate.ui.theme.Motion
import kotlin.math.roundToInt

@Composable
fun HealthScreen(
    healthRecords: List<HealthRecord>,
    onSaveRecord: (HealthRecord) -> Unit,
    today: String = DateText.shortDate(DateText.demoToday())
) {

    var dolorEscala by remember { mutableFloatStateOf(3f) }
    var selectedMood by remember { mutableStateOf(Mood.FELIZ) }
    var presionArterial by remember { mutableStateOf("120/80") }
    var glucosa by remember { mutableStateOf("95") }
    var peso by remember { mutableStateOf("68.5") }
    var pulso by remember { mutableStateOf("78") }
    var notas by remember { mutableStateOf("") }
    var errors by remember { mutableStateOf(VitalsErrors()) }
    var saved by remember { mutableStateOf(false) }
    LaunchedEffect(saved) {
        if (saved) {
            delay(1_800)
            saved = false
        }
    }
    val saveColor by animateColorAsState(
        if (saved) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
        Motion.gentle(), label = "saveColor"
    )
    val shaker = remember { ShakeController() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    val pain = dolorEscala.roundToInt()
    val severity = MedicalValidator.painSeverity(pain)
    val severityColor by animateColorAsState(severityColor(severity), Motion.gentle(), label = "severity")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Registro diario de síntomas y signos vitales (RF-05)
        item {
            HealthCard(tone = CardTone.GREEN) {
                Text("Registro diario", style = MaterialTheme.typography.titleLarge)
                Text(
                    today,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Nivel de dolor", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    AnimatedContent(
                        targetState = pain,
                        transitionSpec = {
                            val up = targetState > initialState
                            (slideInVertically { if (up) it else -it } + fadeIn()) togetherWith
                                (slideOutVertically { if (up) -it else it } + fadeOut())
                        },
                        label = "pain"
                    ) { value ->
                        Text("$value/10", style = MaterialTheme.typography.titleMedium, color = severityColor)
                    }
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
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedMood = mood
                            },
                            modifier = Modifier.weight(1f).pressScale(0.92f)
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
                        icon = Icons.Default.MonitorHeart,
                        error = errors.presionArterial,
                        keyboardType = KeyboardType.Text,
                        modifier = Modifier.weight(1f)
                    )
                    VitalField(
                        value = glucosa,
                        onChange = { glucosa = it; errors = errors.copy(glucosa = null) },
                        label = "Glucosa (mg/dL)",
                        icon = Icons.Default.Bloodtype,
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
                        icon = Icons.Default.MonitorWeight,
                        error = errors.peso,
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                    VitalField(
                        value = pulso,
                        onChange = { pulso = it; errors = errors.copy(pulso = null) },
                        label = "Pulso (lpm)",
                        icon = Icons.Default.Favorite,
                        beating = true,
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
                        if (!result.isValid) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch { shaker.shake() }
                            return@Button
                        }
                        onSaveRecord(
                            HealthRecord(
                                fecha = today,
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
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        saved = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shake(shaker)
                        .pressScale(0.97f),
                    colors = ButtonDefaults.buttonColors(containerColor = saveColor)
                ) {
                    // Confirmación: el botón se vuelve verde y muestra "Guardado" un momento.
                    AnimatedContent(
                        targetState = saved,
                        transitionSpec = {
                            (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                        },
                        label = "save"
                    ) { done ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (done) Icons.Default.CheckCircle else Icons.Default.Save, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (done) "Registro guardado" else "Guardar registro")
                        }
                    }
                }
            }
        }

        item { SectionHeader("Historial (${healthRecords.size})", modifier = Modifier.staggeredEntrance(1)) }

        if (healthRecords.isEmpty()) {
            item(key = "empty") { EmptyState(Icons.Default.MonitorHeart, "Tus registros guardados aparecerán aquí.") }
        }
        itemsIndexed(healthRecords, key = { _, it -> it.id }) { index, record ->
            HealthRecordCard(record, Modifier.animateItem().staggeredEntrance(2 + index))
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
    val container by animateColorAsState(
        if (selected) colors.primaryContainer else colors.surfaceVariant, Motion.gentle(), label = "mood"
    )
    val emojiScale by animateFloatAsState(if (selected) 1.15f else 1f, Motion.playful(), label = "moodScale")
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = container
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(mood.icon, fontSize = 24.sp, modifier = Modifier.graphicsLayer { scaleX = emojiScale; scaleY = emojiScale })
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
    icon: ImageVector,
    error: String?,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    beating: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        leadingIcon = {
            if (beating) HeartbeatIcon(icon) else Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        modifier = modifier
    )
}

/** Corazón que late a ~70 lpm: dos golpes rápidos y una pausa. */
@Composable
private fun HeartbeatIcon(icon: ImageVector) {
    val beat by rememberInfiniteTransition(label = "heart").animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 860
                1.22f at 110
                1f at 220
                1.14f at 330
                1f at 470
            }
        ),
        label = "heartBeat"
    )
    Icon(
        icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier.size(20.dp).graphicsLayer { scaleX = beat; scaleY = beat }
    )
}

@Composable
private fun HealthRecordCard(record: HealthRecord, modifier: Modifier = Modifier) {
    val severity = MedicalValidator.painSeverity(record.dolorEscala)
    HealthCard(modifier, tone = CardTone.SLATE) {
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
