package com.healmate.ui.screens.medications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.healmate.data.model.Medication
import com.healmate.ui.components.*

@Composable
fun MedicationsScreen(
    medications: List<Medication>,
    onAddMedication: (Medication) -> Unit,
    onToggleActive: (Int) -> Unit,
    onDeleteMedication: (Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Medication?>(null) }

    val filteredList = medications.filter {
        it.nombre.contains(searchQuery, ignoreCase = true) ||
            it.dosis.contains(searchQuery, ignoreCase = true) ||
            it.proposito.contains(searchQuery, ignoreCase = true)
    }
    val activeMedications = filteredList.filter { it.activo }
    val inactiveMedications = filteredList.filter { !it.activo }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Agregar") }
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
            // Buscador (RF-02)
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar por nombre, dosis o propósito") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda")
                            }
                        }
                    },
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )
            }

            item { SectionHeader("Activos (${activeMedications.size})") }

            if (activeMedications.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Default.Medication,
                        if (searchQuery.isBlank()) "No tienes medicamentos activos."
                        else "Ningún medicamento coincide con \"$searchQuery\"."
                    )
                }
            }
            items(activeMedications, key = { it.id }) { med ->
                MedicationCard(
                    medication = med,
                    onTogglePause = { onToggleActive(med.id) },
                    onDelete = { pendingDelete = med }
                )
            }

            // Inactivos / Finalizados (caso límite 4.4)
            if (inactiveMedications.isNotEmpty()) {
                item { SectionHeader("Inactivos / Finalizados (${inactiveMedications.size})") }
                items(inactiveMedications, key = { it.id }) { med ->
                    MedicationCard(
                        medication = med,
                        onTogglePause = { onToggleActive(med.id) },
                        onDelete = { pendingDelete = med }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddMedicationDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { newMed ->
                onAddMedication(newMed)
                showAddDialog = false
            }
        )
    }

    pendingDelete?.let { med ->
        ConfirmDialog(
            title = "Eliminar ${med.nombre}",
            message = "El medicamento dejará de aparecer en tu catálogo y no generará más recordatorios.",
            confirmLabel = "Eliminar",
            onConfirm = {
                onDeleteMedication(med.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun MedicationCard(
    medication: Medication,
    onTogglePause: () -> Unit,
    onDelete: () -> Unit
) {
    val active = medication.activo
    val accent = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    HealthCard(
        containerColor = if (active) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Default.Medication, accent, size = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(medication.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${medication.dosis} · ${medication.viaAdministracion}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusPill(
                text = if (active) "Activo" else "Pausado",
                color = if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (medication.proposito.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = medication.proposito,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            InfoItem(Icons.Default.Repeat, medication.frecuencia)
            InfoItem(Icons.Default.Schedule, "Próxima: ${medication.proximaHora}")
        }
        medication.fechaFin?.let {
            Spacer(Modifier.height(4.dp))
            InfoItem(Icons.Default.EventBusy, "Hasta $it")
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onTogglePause) {
                Icon(
                    if (active) Icons.Default.PauseCircleOutline else Icons.Default.PlayCircleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(if (active) "Pausar" else "Reactivar")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Eliminar ${medication.nombre}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun InfoItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AddMedicationDialog(
    onDismiss: () -> Unit,
    onConfirm: (Medication) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var dosis by remember { mutableStateOf("") }
    var frecuencia by remember { mutableStateOf("") }
    var proximaHora by remember { mutableStateOf("08:00") }
    var fechaInicio by remember { mutableStateOf("2026-10-13") }
    var fechaFin by remember { mutableStateOf("") }
    var proposito by remember { mutableStateOf("") }
    var showErrors by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar medicamento") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RequiredField(nombre, { nombre = it }, "Nombre *", showErrors)
                RequiredField(dosis, { dosis = it }, "Dosis (ej. 500 mg) *", showErrors)
                RequiredField(frecuencia, { frecuencia = it }, "Frecuencia (ej. cada 8 horas) *", showErrors)
                OptionalField(proximaHora, { proximaHora = it }, "Horario de toma (ej. 08:00)")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionalField(fechaInicio, { fechaInicio = it }, "Inicio", Modifier.weight(1f))
                    OptionalField(fechaFin, { fechaFin = it }, "Fin (opcional)", Modifier.weight(1f))
                }
                OptionalField(proposito, { proposito = it }, "Propósito (ej. dolor de cabeza)")
            }
        },
        confirmButton = {
            Button(onClick = {
                if (nombre.isBlank() || dosis.isBlank() || frecuencia.isBlank()) {
                    showErrors = true
                    return@Button
                }
                onConfirm(
                    Medication(
                        id = (100..999).random(),
                        nombre = nombre.trim(),
                        dosis = dosis.trim(),
                        frecuencia = frecuencia.trim(),
                        proximaHora = proximaHora.ifBlank { "08:00" },
                        fechaInicio = fechaInicio,
                        fechaFin = fechaFin.ifBlank { null },
                        activo = true,
                        proposito = proposito.trim()
                    )
                )
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun RequiredField(value: String, onChange: (String) -> Unit, label: String, showErrors: Boolean) {
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

@Composable
private fun OptionalField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = modifier
    )
}
