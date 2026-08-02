package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClinicalRecord
import com.example.data.model.Patient
import com.example.ui.OpticaViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientsScreen(
    viewModel: OpticaViewModel,
    onOpenAddPatientDialog: () -> Unit,
    onOpenAddClinicalRecordDialog: (Patient) -> Unit
) {
    val context = LocalContext.current
    val patients by viewModel.patients.collectAsState()
    val records by viewModel.clinicalRecords.collectAsState()
    val searchQuery by viewModel.patientSearchQuery.collectAsState()
    val selectedPatientId by viewModel.selectedPatientId.collectAsState()

    val filteredPatients = remember(patients, searchQuery) {
        if (searchQuery.isBlank()) patients
        else patients.filter { p ->
            p.fullName.contains(searchQuery, ignoreCase = true) ||
                    p.phone.contains(searchQuery) ||
                    p.email.contains(searchQuery, ignoreCase = true)
        }
    }

    val selectedPatient = remember(patients, selectedPatientId) {
        patients.find { it.id == selectedPatientId }
    }

    var showImportDialog by remember { mutableStateOf(false) }

    if (showImportDialog) {
        com.example.ui.components.ImportPatientsDialog(
            onDismiss = { showImportDialog = false },
            onConfirmImport = { content ->
                viewModel.importPatientsFromCsvContent(content) { count ->
                    android.widget.Toast.makeText(
                        context,
                        "¡Se importaron $count pacientes correctamente a la base de datos!",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
                showImportDialog = false
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenAddPatientDialog,
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = "Nuevo Paciente") },
                text = { Text("Nuevo Paciente") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setPatientSearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar por nombre, teléfono o correo...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setPatientSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            // Excel Import/Export Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showImportDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Importar Excel", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        com.example.util.ExcelCsvHelper.exportPatientsToCsv(context, patients)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exportar Excel", fontSize = 12.sp)
                }
            }

            // Content List or Detail
            if (selectedPatient != null) {
                // Show Patient Expediente Detail View
                PatientDetailCard(
                    patient = selectedPatient,
                    records = records.filter { it.patientId == selectedPatient.id },
                    onBackClick = { viewModel.selectPatient(null) },
                    onAddRecordClick = { onOpenAddClinicalRecordDialog(selectedPatient) },
                    onSendWhatsAppReminder = {
                        viewModel.sendWhatsAppMessage(
                            context = context,
                            phone = selectedPatient.phone,
                            message = viewModel.buildExamReviewReminderText(selectedPatient)
                        )
                    },
                    onSendBirthdayGreeting = {
                        viewModel.sendWhatsAppMessage(
                            context = context,
                            phone = selectedPatient.phone,
                            message = viewModel.buildBirthdayGreetingText(selectedPatient)
                        )
                    }
                )
            } else {
                // Patient List
                if (filteredPatients.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No se encontraron pacientes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(filteredPatients, key = { it.id }) { patient ->
                            val patientRecords = records.filter { it.patientId == patient.id }
                            val lastRecord = patientRecords.maxByOrNull { it.date }

                            PatientSummaryCard(
                                patient = patient,
                                lastRecord = lastRecord,
                                onClick = { viewModel.selectPatient(patient.id) },
                                onWhatsAppClick = {
                                    viewModel.sendWhatsAppMessage(
                                        context = context,
                                        phone = patient.phone,
                                        message = "Hola *${patient.fullName}*, te saludamos de *ÓPTICA FAMILIAR AGS*. ¿En qué podemos ayudarte el día de hoy?"
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PatientSummaryCard(
    patient: Patient,
    lastRecord: ClinicalRecord?,
    onClick: () -> Unit,
    onWhatsAppClick: () -> Unit
) {
    val lastExamStr = remember(lastRecord) {
        if (lastRecord != null) {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(lastRecord.date))
        } else "Sin exámenes previas"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = patient.fullName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = patient.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "📞 ${patient.phone} • ✉️ ${patient.email}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Última graduación: $lastExamStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(onClick = onWhatsAppClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar WhatsApp",
                    tint = Color(0xFF25D366)
                )
            }
        }
    }
}

@Composable
fun PatientDetailCard(
    patient: Patient,
    records: List<ClinicalRecord>,
    onBackClick: () -> Unit,
    onAddRecordClick: () -> Unit,
    onSendWhatsAppReminder: () -> Unit,
    onSendBirthdayGreeting: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Expediente Clínico",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Patient Personal Info Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = patient.fullName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "📱 Teléfono: ${patient.phone}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "✉️ Correo: ${patient.email}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "🎂 Fecha Nacimiento: ${patient.dateOfBirth}", style = MaterialTheme.typography.bodyMedium)
                    if (patient.address.isNotBlank()) {
                        Text(text = "🏠 Dirección: ${patient.address}", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (patient.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "📝 Notas clínicas: ${patient.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onSendWhatsAppReminder,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Recordatorio Examen", style = MaterialTheme.typography.labelSmall)
                        }
                        Button(
                            onClick = onSendBirthdayGreeting,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Cake, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Felicitación", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // Section Title & Add New Prescription Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Historial de Graduaciones (${records.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onAddRecordClick,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nueva Graduación")
                }
            }
        }

        if (records.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay graduaciones registradas para este paciente.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(records, key = { it.id }) { record ->
                PrescriptionCard(patient = patient, record = record)
            }
        }
    }
}

@Composable
fun PrescriptionCard(patient: Patient, record: ClinicalRecord) {
    val context = LocalContext.current
    val dateStr = remember(record.date) {
        SimpleDateFormat("dd 'de' MMMM yyyy", Locale("es", "MX")).format(Date(record.date))
    }
    val nextExamStr = remember(record.nextExamDate) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(record.nextExamDate))
    }
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Examen Clínico # ${record.id}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = record.optometristName,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Optical Graduation Table (RECETA)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(8.dp)
            ) {
                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("OJO", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Text("ESF", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Text("CYL", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Text("EJE", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Text("ADD", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }
                HorizontalDivider()
                // OD Row
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("RX OD", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(record.odSphere, modifier = Modifier.weight(1f))
                    Text(record.odCylinder, modifier = Modifier.weight(1f))
                    Text(record.odAxis, modifier = Modifier.weight(1f))
                    Text(record.odAddition, modifier = Modifier.weight(1f))
                }
                // OI Row
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("RX OI", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(record.oiSphere, modifier = Modifier.weight(1f))
                    Text(record.oiCylinder, modifier = Modifier.weight(1f))
                    Text(record.oiAxis, modifier = Modifier.weight(1f))
                    Text(record.oiAddition, modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "DIS (DIP): ${record.pupillaryDistance} | ALT: ${record.segmentHeight}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (record.treatment.isNotBlank()) {
                Text(
                    text = "TRATAMIENTO: ${record.treatment}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium
                )
            }

            if (record.diagnosis.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Diagnóstico: ${record.diagnosis}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Text("Historial y Entrevista:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("• Patología Propia: ${record.personalPathology}", style = MaterialTheme.typography.bodySmall)
                Text("• Patología Familiar: ${record.familyPathology}", style = MaterialTheme.typography.bodySmall)
                Text("• Entrevista (Cefalea/Sint.): ${record.clinicalInterview}", style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(6.dp))
                Text("Pruebas Optométricas (5 Pruebas):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("1. Agudeza Visual: ${record.testVisualAcuity}", style = MaterialTheme.typography.bodySmall)
                Text("2. Refracción: ${record.testRefraction}", style = MaterialTheme.typography.bodySmall)
                Text("3. Oftalmoscopía: ${record.testOphthalmoscopy}", style = MaterialTheme.typography.bodySmall)
                Text("4. Biomicroscopía: ${record.testBiomicroscopy}", style = MaterialTheme.typography.bodySmall)
                Text("5. Visión Color / Tono: ${record.testIshiharaColor}", style = MaterialTheme.typography.bodySmall)

                if (record.finalAnnotations.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Anotaciones Finales: ${record.finalAnnotations}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text("Responsable Sanitario: ${record.optometristName}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF0D9488))
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { isExpanded = !isExpanded }) {
                    Text(if (isExpanded) "Ocultar detalles" else "Ver 5 pruebas y antecedentes")
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }

                OutlinedButton(
                    onClick = {
                        com.example.util.ClinicalRecordPdfHelper.generateClinicalRecordPdf(context, patient, record)
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Exportar PDF", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
