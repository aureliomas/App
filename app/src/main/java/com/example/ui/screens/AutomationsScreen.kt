package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Patient
import com.example.ui.OpticaViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationsScreen(viewModel: OpticaViewModel) {
    val context = LocalContext.current
    val patients by viewModel.patients.collectAsState()
    val records by viewModel.clinicalRecords.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Revisiones Pendientes, 1: Cumpleaños

    val now = System.currentTimeMillis()
    val calNow = Calendar.getInstance()
    val currentMonth = calNow.get(Calendar.MONTH) + 1

    // Patients with overdue or upcoming revision (within 30 days)
    val revisionPatients = remember(patients, records) {
        patients.filter { p ->
            val pRecords = records.filter { it.patientId == p.id }
            val lastRecord = pRecords.maxByOrNull { it.date }
            if (lastRecord != null) {
                // If nextExamDate is due or less than 30 days away
                lastRecord.nextExamDate <= now + (30L * 24 * 60 * 60 * 1000)
            } else true // No records yet
        }
    }

    // Birthday patients this month
    val birthdayPatients = remember(patients, currentMonth) {
        patients.filter { p ->
            try {
                val parts = p.dateOfBirth.split("-")
                if (parts.size >= 2) parts[1].toInt() == currentMonth else false
            } catch (e: Exception) { false }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Title Header
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MarkChatRead,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Centro de Automatizaciones",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Recordatorios por WhatsApp y felicitaciones por correo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Tabs Selector
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Próximas Revisiones (${revisionPatients.size})") },
                icon = { Icon(Icons.Default.Visibility, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Cumpleaños del Mes (${birthdayPatients.size})") },
                icon = { Icon(Icons.Default.Cake, contentDescription = null) }
            )
        }

        if (selectedTab == 0) {
            // Revisiones
            if (revisionPatients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay revisiones anuales pendientes este mes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(revisionPatients, key = { it.id }) { patient ->
                        val pRecords = records.filter { it.patientId == patient.id }
                        val lastRecord = pRecords.maxByOrNull { it.date }

                        AutomationPatientCard(
                            patient = patient,
                            subtitle = if (lastRecord != null)
                                "Próxima revisión: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(lastRecord.nextExamDate))}"
                            else "Sin examen previo registrado",
                            messageText = viewModel.buildExamReviewReminderText(patient),
                            onSendWhatsApp = {
                                viewModel.sendWhatsAppMessage(
                                    context = context,
                                    phone = patient.phone,
                                    message = viewModel.buildExamReviewReminderText(patient)
                                )
                            },
                            onSendEmail = {
                                viewModel.sendEmailMessage(
                                    context = context,
                                    email = patient.email,
                                    subject = "Recordatorio de Examen Visual Anual - ÓPTICA FAMILIAR AGS",
                                    body = viewModel.buildExamReviewReminderText(patient)
                                )
                            }
                        )
                    }
                }
            }
        } else {
            // Cumpleaños
            if (birthdayPatients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay cumpleañeros registrados en este mes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(birthdayPatients, key = { it.id }) { patient ->
                        AutomationPatientCard(
                            patient = patient,
                            subtitle = "Cumpleaños: ${patient.dateOfBirth}",
                            messageText = viewModel.buildBirthdayGreetingText(patient),
                            onSendWhatsApp = {
                                viewModel.sendWhatsAppMessage(
                                    context = context,
                                    phone = patient.phone,
                                    message = viewModel.buildBirthdayGreetingText(patient)
                                )
                            },
                            onSendEmail = {
                                viewModel.sendEmailMessage(
                                    context = context,
                                    email = patient.email,
                                    subject = "¡Feliz Cumpleaños te desea ÓPTICA FAMILIAR AGS! 🥳",
                                    body = viewModel.buildBirthdayGreetingText(patient)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AutomationPatientCard(
    patient: Patient,
    subtitle: String,
    messageText: String,
    onSendWhatsApp: () -> Unit,
    onSendEmail: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = patient.fullName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Message Preview Box
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = messageText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSendWhatsApp,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WhatsApp", color = Color.White, style = MaterialTheme.typography.labelMedium)
                }

                if (patient.email.isNotBlank()) {
                    OutlinedButton(
                        onClick = onSendEmail,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Correo", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
