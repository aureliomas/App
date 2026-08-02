package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import java.text.SimpleDateFormat
import java.util.*

// --- ADD APPOINTMENT DIALOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppointmentDialog(
    patients: List<Patient>,
    onDismiss: () -> Unit,
    onConfirm: (Appointment) -> Unit
) {
    var selectedPatient by remember { mutableStateOf(patients.firstOrNull()) }
    var patientDropdownExpanded by remember { mutableStateOf(false) }

    var selectedCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    var reason by remember { mutableStateOf("Examen de la vista") }
    var notes by remember { mutableStateOf("") }

    val context = LocalContext.current
    val dateStr = remember(selectedCalendar.timeInMillis) {
        SimpleDateFormat("EEE, dd/MM/yyyy - hh:mm a", Locale("es", "MX")).format(selectedCalendar.time)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agendar Nueva Cita", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Patient Selector Dropdown
                Text("Seleccionar Paciente:", style = MaterialTheme.typography.labelMedium)
                ExposedDropdownMenuBox(
                    expanded = patientDropdownExpanded,
                    onExpandedChange = { patientDropdownExpanded = !patientDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedPatient?.fullName ?: "Seleccionar paciente...",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = patientDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = patientDropdownExpanded,
                        onDismissRequest = { patientDropdownExpanded = false }
                    ) {
                        patients.forEach { patient ->
                            DropdownMenuItem(
                                text = { Text("${patient.fullName} (${patient.phone})") },
                                onClick = {
                                    selectedPatient = patient
                                    patientDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Date & Time Picker trigger
                OutlinedTextField(
                    value = dateStr,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fecha y Hora") },
                    trailingIcon = {
                        IconButton(onClick = {
                            val c = selectedCalendar
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            val newCal = Calendar.getInstance().apply {
                                                set(year, month, dayOfMonth, hourOfDay, minute)
                                            }
                                            selectedCalendar = newCal
                                        },
                                        c.get(Calendar.HOUR_OF_DAY),
                                        c.get(Calendar.MINUTE),
                                        false
                                    ).show()
                                },
                                c.get(Calendar.YEAR),
                                c.get(Calendar.MONTH),
                                c.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Seleccionar fecha")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Reason selector
                Text("Motivo de la cita:", style = MaterialTheme.typography.labelMedium)
                val reasons = listOf("Examen de la vista", "Adaptación de Lentes de Contacto", "Ajuste de armazón", "Entrega de anteojos", "Garantía / Revisión")
                var reasonExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = reasonExpanded,
                    onExpandedChange = { reasonExpanded = !reasonExpanded }
                ) {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reasonExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = reasonExpanded,
                        onDismissRequest = { reasonExpanded = false }
                    ) {
                        reasons.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r) },
                                onClick = {
                                    reason = r
                                    reasonExpanded = false
                                }
                            )
                        }
                    }
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas adicionales") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = selectedPatient != null,
                onClick = {
                    val patient = selectedPatient ?: return@Button
                    onConfirm(
                        Appointment(
                            patientId = patient.id,
                            patientName = patient.fullName,
                            patientPhone = patient.phone,
                            dateTime = selectedCalendar.timeInMillis,
                            reason = reason,
                            status = "Pendiente",
                            notes = notes
                        )
                    )
                }
            ) {
                Text("Agendar Cita")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// --- ADD PATIENT DIALOG ---
@Composable
fun AddPatientDialog(
    onDismiss: () -> Unit,
    onConfirm: (Patient) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("1995-01-01") }
    var address by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Nuevo Paciente", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Nombre Completo *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Teléfono (WhatsApp) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo Electrónico") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dateOfBirth,
                    onValueChange = { dateOfBirth = it },
                    label = { Text("Fecha de Nacimiento (AAAA-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Dirección") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas Médicas / Observaciones") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = fullName.isNotBlank() && phone.isNotBlank(),
                onClick = {
                    onConfirm(
                        Patient(
                            fullName = fullName,
                            phone = phone,
                            email = email,
                            dateOfBirth = dateOfBirth,
                            address = address,
                            notes = notes
                        )
                    )
                }
            ) {
                Text("Guardar Paciente")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// --- ADD CLINICAL RECORD / PRESCRIPTION DIALOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClinicalRecordDialog(
    patient: Patient,
    onDismiss: () -> Unit,
    onConfirm: (ClinicalRecord) -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Historial & Pruebas, 1 = Graduación & Diagnóstico

    // Historial Patológico & Entrevista
    var personalPathology by remember { mutableStateOf("Negativo a diabetes e hipertensión.") }
    var familyPathology by remember { mutableStateOf("Sin antecedentes de glaucoma ni catarata.") }
    var clinicalInterview by remember { mutableStateOf("Refiere cefalea ocasional vespertina y astenopia al usar pantalla.") }

    // 5 Pruebas Optométricas
    var testVisualAcuity by remember { mutableStateOf("OD 20/40 (con corr. 20/20) | OI 20/50 (con corr. 20/20)") }
    var testRefraction by remember { mutableStateOf("OD -1.50 -0.50 x 180° | OI -1.75 -0.75 x 175°") }
    var testOphthalmoscopy by remember { mutableStateOf("Fondo de ojo normal, papila de bordes nítidos, sin exudados.") }
    var testBiomicroscopy by remember { mutableStateOf("Córnea transparente, película lagrimal estable sin blefaritis.") }
    var testIshiharaColor by remember { mutableStateOf("Visión de color normal (12/12 láminas Ishihara).") }

    // Receta / Graduación Final
    var odSphere by remember { mutableStateOf("-1.50") }
    var odCylinder by remember { mutableStateOf("-0.50") }
    var odAxis by remember { mutableStateOf("180°") }
    var odAddition by remember { mutableStateOf("0.00") }

    var oiSphere by remember { mutableStateOf("-1.75") }
    var oiCylinder by remember { mutableStateOf("-0.75") }
    var oiAxis by remember { mutableStateOf("175°") }
    var oiAddition by remember { mutableStateOf("0.00") }

    var pupillaryDistance by remember { mutableStateOf("63 mm") } // DIS
    var segmentHeight by remember { mutableStateOf("18 mm") }     // ALT
    var treatment by remember { mutableStateOf("Micas Policristal + Antirreflejante Crizal + Filtro Azul (Blue Light)") } // TRATAMIENTO
    var lensType by remember { mutableStateOf("Monofocal") }

    // Anotaciones Finales & Diagnóstico
    var diagnosis by remember { mutableStateOf("Astigmatismo Miópico Compuesto Ambos Ojos") }
    var finalAnnotations by remember { mutableStateOf("Se recomienda uso continuo de anteojos para trabajo en computadora y control anual.") }
    var optometristName by remember { mutableStateOf("L. Opt. Brisaida Gpe Guillen Ortiz") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Expediente y Graduación Clínica", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text("Paciente: ${patient.fullName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tab Selection Header
                SecondaryTabRow(selectedTabIndex = activeTab) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("1. Historial y 5 Pruebas", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("2. Receta y Diagnóstico", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (activeTab == 0) {
                        // --- SECTION 1: HISTORIAL PATOLÓGICO Y ENTREVISTA CLÍNICA ---
                        Text("Historial Patológico y Entrevista", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall)

                        OutlinedTextField(
                            value = personalPathology,
                            onValueChange = { personalPathology = it },
                            label = { Text("Historial Patológico Propio (Diabetes, HTA, Alergias)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = familyPathology,
                            onValueChange = { familyPathology = it },
                            label = { Text("Antecedentes Patológicos Familiares (Glaucoma, Catarata)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = clinicalInterview,
                            onValueChange = { clinicalInterview = it },
                            label = { Text("Entrevista Clínica (Cefalea, Fatiga visual, Ardor)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // --- SECTION 2: 5 PRUEBAS OPTOMÉTRICAS ---
                        Text("Resultados de 5 Pruebas Optométricas", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall)

                        OutlinedTextField(
                            value = testVisualAcuity,
                            onValueChange = { testVisualAcuity = it },
                            label = { Text("1. Agudeza Visual (AV sin / con corrección OD, OI)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = testRefraction,
                            onValueChange = { testRefraction = it },
                            label = { Text("2. Refracción / Retinoscopía / Autorefractómetro") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = testOphthalmoscopy,
                            onValueChange = { testOphthalmoscopy = it },
                            label = { Text("3. Oftalmoscopía / Fondo de Ojo (Papila, Mácula)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = testBiomicroscopy,
                            onValueChange = { testBiomicroscopy = it },
                            label = { Text("4. Biomicroscopía / Lámpara de Hendidura") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = testIshiharaColor,
                            onValueChange = { testIshiharaColor = it },
                            label = { Text("5. Visión de Color (Ishihara) / Cover Test / Tono") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // --- SECTION 3: RECETA / GRADUACIÓN FINAL ---
                        Text("RECETA FINAL (PRESCRIPCIÓN)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall)

                        // RX OD
                        Text("RX OD (Ojo Derecho)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(value = odSphere, onValueChange = { odSphere = it }, label = { Text("ESF.") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = odCylinder, onValueChange = { odCylinder = it }, label = { Text("CYL.") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = odAxis, onValueChange = { odAxis = it }, label = { Text("EJE") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = odAddition, onValueChange = { odAddition = it }, label = { Text("ADD") }, modifier = Modifier.weight(1f))
                        }

                        // RX OI
                        Text("RX OI (Ojo Izquierdo)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(value = oiSphere, onValueChange = { oiSphere = it }, label = { Text("ESF.") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = oiCylinder, onValueChange = { oiCylinder = it }, label = { Text("CYL.") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = oiAxis, onValueChange = { oiAxis = it }, label = { Text("EJE") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = oiAddition, onValueChange = { oiAddition = it }, label = { Text("ADD") }, modifier = Modifier.weight(1f))
                        }

                        // DIS, ALT, TRATAMIENTO
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(value = pupillaryDistance, onValueChange = { pupillaryDistance = it }, label = { Text("DIS (DIP)") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = segmentHeight, onValueChange = { segmentHeight = it }, label = { Text("ALT (Altura)") }, modifier = Modifier.weight(1f))
                        }

                        OutlinedTextField(
                            value = treatment,
                            onValueChange = { treatment = it },
                            label = { Text("TRATAMIENTO (Micas, Antirreflejante, Filtro Azul, Poly...)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // --- SECTION 4: ANOTACIONES FINALES Y DIAGNÓSTICO ---
                        Text("Diagnóstico y Anotaciones Finales", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall)

                        OutlinedTextField(
                            value = diagnosis,
                            onValueChange = { diagnosis = it },
                            label = { Text("Diagnóstico Optométrico Final *") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = finalAnnotations,
                            onValueChange = { finalAnnotations = it },
                            label = { Text("Anotaciones Finales / Observaciones") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )

                        OutlinedTextField(
                            value = optometristName,
                            onValueChange = { optometristName = it },
                            label = { Text("Responsable Sanitario *") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = diagnosis.isNotBlank(),
                onClick = {
                    onConfirm(
                        ClinicalRecord(
                            patientId = patient.id,
                            personalPathology = personalPathology.trim(),
                            familyPathology = familyPathology.trim(),
                            clinicalInterview = clinicalInterview.trim(),
                            testVisualAcuity = testVisualAcuity.trim(),
                            testRefraction = testRefraction.trim(),
                            testOphthalmoscopy = testOphthalmoscopy.trim(),
                            testBiomicroscopy = testBiomicroscopy.trim(),
                            testIshiharaColor = testIshiharaColor.trim(),
                            odSphere = odSphere.trim(),
                            odCylinder = odCylinder.trim(),
                            odAxis = odAxis.trim(),
                            odAddition = odAddition.trim(),
                            oiSphere = oiSphere.trim(),
                            oiCylinder = oiCylinder.trim(),
                            oiAxis = oiAxis.trim(),
                            oiAddition = oiAddition.trim(),
                            pupillaryDistance = pupillaryDistance.trim(),
                            segmentHeight = segmentHeight.trim(),
                            treatment = treatment.trim(),
                            lensType = lensType.trim(),
                            diagnosis = diagnosis.trim(),
                            finalAnnotations = finalAnnotations.trim(),
                            optometristName = optometristName.trim()
                        )
                    )
                }
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Generar y Guardar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}


// --- ADD INVENTORY ITEM DIALOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInventoryItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (InventoryItem) -> Unit
) {
    var sku by remember { mutableStateOf("ARM-${(100..999).random()}") }
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("Ray-Ban") }
    var category by remember { mutableStateOf("Armazones") }
    var stockQuantityText by remember { mutableStateOf("10") }
    var minStockThresholdText by remember { mutableStateOf("3") }
    var costPriceText by remember { mutableStateOf("500") }
    var salePriceText by remember { mutableStateOf("1200") }

    val categories = listOf("Armazones", "Micas", "Lentes de Contacto", "Soluciones", "Accesorios")
    var categoryExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Producto en Inventario", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre del Producto *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = sku, onValueChange = { sku = it }, label = { Text("Código / SKU *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Marca") }, modifier = Modifier.fillMaxWidth())

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = stockQuantityText, onValueChange = { stockQuantityText = it }, label = { Text("Stock") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = minStockThresholdText, onValueChange = { minStockThresholdText = it }, label = { Text("Stock Mín.") }, modifier = Modifier.weight(1f))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = costPriceText, onValueChange = { costPriceText = it }, label = { Text("Costo ($)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = salePriceText, onValueChange = { salePriceText = it }, label = { Text("Precio Venta ($)") }, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && sku.isNotBlank(),
                onClick = {
                    onConfirm(
                        InventoryItem(
                            sku = sku,
                            name = name,
                            brand = brand,
                            category = category,
                            stockQuantity = stockQuantityText.toIntOrNull() ?: 10,
                            minStockThreshold = minStockThresholdText.toIntOrNull() ?: 3,
                            costPrice = costPriceText.toDoubleOrNull() ?: 0.0,
                            salePrice = salePriceText.toDoubleOrNull() ?: 0.0
                        )
                    )
                }
            ) { Text("Guardar Producto") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// --- ADD INVOICE / POS SALE DIALOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInvoiceDialog(
    patients: List<Patient>,
    inventoryItems: List<InventoryItem>,
    onDismiss: () -> Unit,
    onConfirm: (Invoice) -> Unit
) {
    var selectedPatient by remember { mutableStateOf(patients.firstOrNull()) }
    var patientDropdownExpanded by remember { mutableStateOf(false) }

    var itemsSummary by remember { mutableStateOf("Armazón Ray-Ban Titanium + Mica Anti-Reflejante Crizal") }
    var totalWithTaxText by remember { mutableStateOf("3200") }
    var initialPaymentText by remember { mutableStateOf("3200") }
    var isCreditSale by remember { mutableStateOf(false) }

    var paymentMethod by remember { mutableStateOf("Efectivo") }
    var paymentMethodExpanded by remember { mutableStateOf(false) }

    var customPhone by remember { mutableStateOf(selectedPatient?.phone ?: "4495543087") }
    var customEmail by remember { mutableStateOf(selectedPatient?.email ?: "") }

    LaunchedEffect(selectedPatient) {
        selectedPatient?.let { p ->
            customPhone = p.phone
            customEmail = p.email
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Punto de Venta / Emitir Ticket", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Type of sale selector (Contado vs Crédito)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tipo de Venta:", fontWeight = FontWeight.Bold)
                    FilterChip(
                        selected = isCreditSale,
                        onClick = {
                            isCreditSale = !isCreditSale
                            if (isCreditSale) {
                                initialPaymentText = "${((totalWithTaxText.toDoubleOrNull() ?: 0.0) * 0.5).toInt()}"
                            } else {
                                initialPaymentText = totalWithTaxText
                            }
                        },
                        label = { Text(if (isCreditSale) "Venta a Crédito" else "Venta de Contado") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isCreditSale) Icons.Default.CreditScore else Icons.Default.CheckCircle,
                                contentDescription = null
                            )
                        }
                    )
                }

                // Patient Dropdown
                Text("Cliente / Paciente:", style = MaterialTheme.typography.labelMedium)
                ExposedDropdownMenuBox(
                    expanded = patientDropdownExpanded,
                    onExpandedChange = { patientDropdownExpanded = !patientDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedPatient?.fullName ?: "Seleccionar cliente...",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = patientDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = patientDropdownExpanded,
                        onDismissRequest = { patientDropdownExpanded = false }
                    ) {
                        patients.forEach { p ->
                            DropdownMenuItem(
                                text = { Text("${p.fullName} (${p.phone})") },
                                onClick = {
                                    selectedPatient = p
                                    patientDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customPhone,
                        onValueChange = { customPhone = it },
                        label = { Text("Tel. WhatsApp") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = customEmail,
                        onValueChange = { customEmail = it },
                        label = { Text("Correo") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = itemsSummary,
                    onValueChange = { itemsSummary = it },
                    label = { Text("Descripción de compra *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = totalWithTaxText,
                    onValueChange = {
                        totalWithTaxText = it
                        if (!isCreditSale) initialPaymentText = it
                    },
                    label = { Text("Total con IVA 16% ($) *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = initialPaymentText,
                    onValueChange = { initialPaymentText = it },
                    label = { Text(if (isCreditSale) "Anticipo / Abono Inicial ($)" else "Monto Pagado ($)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Método de Pago:", style = MaterialTheme.typography.labelMedium)
                val methods = listOf("Efectivo", "Tarjeta", "Transferencia")
                ExposedDropdownMenuBox(
                    expanded = paymentMethodExpanded,
                    onExpandedChange = { paymentMethodExpanded = !paymentMethodExpanded }
                ) {
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentMethodExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = paymentMethodExpanded,
                        onDismissRequest = { paymentMethodExpanded = false }
                    ) {
                        methods.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m) },
                                onClick = {
                                    paymentMethod = m
                                    paymentMethodExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedPatient != null && itemsSummary.isNotBlank() && totalWithTaxText.isNotBlank(),
                onClick = {
                    val p = selectedPatient ?: return@Button
                    val total = totalWithTaxText.toDoubleOrNull() ?: 0.0
                    val subtotal = total / 1.16
                    val tax = total - subtotal
                    val amountPaid = (initialPaymentText.toDoubleOrNull() ?: total).coerceAtMost(total)

                    val status = when {
                        amountPaid >= total -> "Pagado"
                        amountPaid > 0 -> "Abono Parcial"
                        else -> "Pendiente"
                    }

                    onConfirm(
                        Invoice(
                            invoiceNumber = "FAC-2026-${(100..999).random()}",
                            patientId = p.id,
                            patientName = p.fullName,
                            patientPhone = customPhone,
                            patientEmail = customEmail,
                            itemsSummary = itemsSummary,
                            subtotal = subtotal,
                            discount = 0.0,
                            tax = tax,
                            total = total,
                            paymentStatus = status,
                            amountPaid = amountPaid,
                            paymentMethod = paymentMethod,
                            isCreditSale = isCreditSale,
                            optometristName = "L.O. BRISAIDA GPE GUILLEN ORTIZ"
                        )
                    )
                }
            ) { Text("Emitir Venta & Generar Ticket") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// --- IMPORT PATIENTS FROM EXCEL/CSV DIALOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPatientsDialog(
    onDismiss: () -> Unit,
    onConfirmImport: (String) -> Unit
) {
    var csvContent by remember { mutableStateOf("") }

    val sampleCsv = """
        Nombre Completo,Teléfono,Correo,Fecha de Nacimiento,Dirección,Notas
        Juan Carlos Pérez,4491112233,juan.perez@gmail.com,1982-04-12,Av. Universidad #101,Cliente frecuente
        Guadalupe López,4492223344,lupe.lopez@hotmail.com,1995-10-20,Calle Nieto #502,Sensibilidad a la luz
        Roberto Hernández,4493334455,roberto.h@yahoo.com,1988-03-12,Av. Convención #304,Micas progresivas
        Martha Elena Silva,4494445566,martha.silva@outlook.com,1976-07-28,Calle Madero #120,Alergia al armazón plástico
    """.trimIndent()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Importar Base de Pacientes desde Excel", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Pega aquí el contenido copiado de tu archivo de Excel o CSV con las columnas: Nombre, Teléfono, Correo, Fecha Nacimiento, Dirección, Notas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = { csvContent = sampleCsv },
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cargar Ejemplo de Prueba (Excel/CSV)")
                }

                OutlinedTextField(
                    value = csvContent,
                    onValueChange = { csvContent = it },
                    label = { Text("Contenido Excel / CSV") },
                    placeholder = { Text("Nombre, Teléfono, Correo, FechaNac, Dirección, Notas...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    maxLines = 10
                )
            }
        },
        confirmButton = {
            Button(
                enabled = csvContent.isNotBlank(),
                onClick = {
                    onConfirmImport(csvContent)
                }
            ) {
                Text("Importar Pacientes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// --- ADD FINANCIAL RECORD / GASTOS GENERALES DIALOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFinancialRecordDialog(
    currentUserRole: String,
    onDismiss: () -> Unit,
    onConfirm: (FinancialRecord) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) } // false = Egreso/Gasto, true = Ingreso
    var notes by remember { mutableStateOf("") }

    val categories = listOf(
        "Gasto",
        "Compra de Mercancía",
        "Pago a Laboratorio",
        "Renta",
        "Agua",
        "Luz",
        "Internet",
        "Comisión",
        "Sueldos",
        "Insumos",
        "Auto",
        "Gastos en General",
        "Ingreso Extra"
    )

    var selectedCategory by remember { mutableStateOf("Renta") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isIncome) "Registrar Nuevo Ingreso" else "Registrar Nuevo Egreso / Gasto General",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Type Switcher Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isIncome,
                        onClick = {
                            isIncome = false
                            if (selectedCategory == "Ingreso Extra") selectedCategory = "Gasto"
                        },
                        label = { Text("🔴 Egreso / Gasto") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isIncome,
                        onClick = {
                            isIncome = true
                            selectedCategory = "Ingreso Extra"
                        },
                        label = { Text("🟢 Ingreso Extra") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Concept Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Concepto / Descripción") },
                    placeholder = { Text("Ej. Pago Renta Local, Pago CFE, Insumos...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Monto ($ MXN)") },
                    placeholder = { Text("0.00") },
                    leadingIcon = { Text("$", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría de Gasto / Tipo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas o Observaciones (Opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank() && amountText.toDoubleOrNull() != null && (amountText.toDoubleOrNull() ?: 0.0) > 0,
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    onConfirm(
                        FinancialRecord(
                            title = title.trim(),
                            amount = amt,
                            category = selectedCategory,
                            isIncome = isIncome,
                            notes = notes.trim(),
                            registeredBy = currentUserRole
                        )
                    )
                }
            ) {
                Text("Guardar Movimiento")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

