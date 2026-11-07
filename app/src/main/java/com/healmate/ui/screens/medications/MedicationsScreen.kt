package com.healmate.ui.screens.medications

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healmate.domain.model.Medication
import com.healmate.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationsScreen(
    medications: List<Medication>,
    onAddMedication: (Medication) -> Unit,
    onToggleActive: (Int) -> Unit,
    onDeleteMedication: (Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredList = medications.filter {
        it.nombre.contains(searchQuery, ignoreCase = true) ||
        it.dosis.contains(searchQuery, ignoreCase = true) ||
        it.proposito.contains(searchQuery, ignoreCase = true)
    }

    val activeMedications = filteredList.filter { it.activo }
    val inactiveMedications = filteredList.filter { !it.activo }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = TealPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Medicamento")
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
            // Buscador (RF-02)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Buscar por nombre, dosis o propósito...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = null)
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            // Sección: Medicamentos Activos
            item {
                Text(
                    text = "Medicamentos Activos (${activeMedications.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            items(activeMedications) { med ->
                MedicationCard(
                    medication = med,
                    onTogglePause = { onToggleActive(med.id) },
                    onDelete = { onDeleteMedication(med.id) }
                )
            }

            // Sección: Medicamentos Inactivos / Finalizados (Edge Case 4.4)
            if (inactiveMedications.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Inactivos / Finalizados (${inactiveMedications.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }

                items(inactiveMedications) { med ->
                    MedicationCard(
                        medication = med,
                        onTogglePause = { onToggleActive(med.id) },
                        onDelete = { onDeleteMedication(med.id) }
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
}

@Composable
private fun MedicationCard(
    medication: Medication,
    onTogglePause: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (medication.activo) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
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
                        .size(48.dp)
                        .background(
                            if (medication.activo) TealPrimary.copy(alpha = 0.15f) else Color.LightGray.copy(alpha = 0.3f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Medication,
                        contentDescription = null,
                        tint = if (medication.activo) TealPrimary else Color.Gray
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = medication.nombre,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = if (medication.activo) TextPrimary else TextSecondary
                    )
                    Text(
                        text = "${medication.dosis} • ${medication.viaAdministracion}",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    if (medication.proposito.isNotEmpty()) {
                        Text(
                            text = "Propósito: ${medication.proposito}",
                            fontSize = 12.sp,
                            color = TealPrimary
                        )
                    }
                }

                // Estado Activo / Inactivo
                Surface(
                    color = if (medication.activo) GreenSuccess.copy(alpha = 0.15f) else Color.LightGray.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (medication.activo) "Activo" else "Pausado",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (medication.activo) GreenSuccess else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Frecuencia: ${medication.frecuencia}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "Próxima toma: ${medication.proximaHora}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (medication.activo) TextPrimary else TextSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onTogglePause,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (medication.activo) "Pausar" else "Reactivar",
                            fontSize = 12.sp
                        )
                    }

                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Eliminar",
                            tint = RedDanger
                        )
                    }
                }
            }
        }
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
    var via by remember { mutableStateOf("Oral") }
    var proximaHora by remember { mutableStateOf("08:00") }
    var fechaInicio by remember { mutableStateOf("2026-10-13") }
    var fechaFin by remember { mutableStateOf("") }
    var proposito by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Medicamento", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del Medicamento *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dosis,
                    onValueChange = { dosis = it },
                    label = { Text("Dosis (ej. 500mg, 1 Tableta) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = frecuencia,
                    onValueChange = { frecuencia = it },
                    label = { Text("Frecuencia (ej. Cada 8 horas) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = proximaHora,
                    onValueChange = { proximaHora = it },
                    label = { Text("Horario de Toma (ej. 08:00)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = proposito,
                    onValueChange = { proposito = it },
                    label = { Text("Propósito (ej. Dolor de cabeza)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nombre.isNotBlank() && dosis.isNotBlank() && frecuencia.isNotBlank()) {
                        val newMed = Medication(
                            id = (100..999).random(),
                            nombre = nombre,
                            dosis = dosis,
                            frecuencia = frecuencia,
                            viaAdministracion = via,
                            proximaHora = proximaHora.ifBlank { "08:00" },
                            fechaInicio = fechaInicio,
                            fechaFin = fechaFin.ifBlank { null },
                            activo = true,
                            proposito = proposito
                        )
                        onConfirm(newMed)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
