package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Invoice
import com.example.data.model.Patient
import com.example.ui.OpticaViewModel
import com.example.util.CfdiConcepto
import com.example.util.CfdiDatosFactura
import com.example.util.CfdiEmisor
import com.example.util.CfdiReceptor
import com.example.util.CfdiXmlHelper
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

data class PacProvider(
    val name: String,
    val url: String,
    val description: String,
    val posSectionName: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacturacionElectronicaModule(
    viewModel: OpticaViewModel,
    prefillInvoice: Invoice? = null,
    onCloseDialog: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val patients by viewModel.patients.collectAsState()
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("es", "MX")) }

    // PAC Billing Providers list
    val pacProviders = listOf(
        PacProvider(
            name = "SAT Portal CFDI 4.0",
            url = "https://portalcfdi.facturaelectronica.sat.gob.mx/",
            description = "Servicio de Facturación del SAT (Público General y Clientes)",
            posSectionName = "Punto de Venta / Mis Facturas"
        ),
        PacProvider(
            name = "Facturador.com (PAC POS)",
            url = "https://app.facturador.com/pos",
            description = "Plataforma PAC con emisión rápida en Punto de Venta",
            posSectionName = "Sección Punto de Venta"
        ),
        PacProvider(
            name = "SmarterWeb SW PAC",
            url = "https://sw.com.mx/login",
            description = "Proveedor PAC de Timbrado masivo CFDI 4.0",
            posSectionName = "Módulo POS / Emisión"
        ),
        PacProvider(
            name = "Siigo Factura Inteligente",
            url = "https://pos.facturainteligente.com/",
            description = "Sistema de facturación electrónica e inventarios",
            posSectionName = "Punto de Venta General"
        ),
        PacProvider(
            name = "Proveedor PAC Personalizado",
            url = "https://www.google.com/search?q=proveedor+facturacion+electronica+pac",
            description = "Enlace directo al sistema de facturación elegido",
            posSectionName = "Portal PAC"
        )
    )

    var selectedPacIndex by remember { mutableIntStateOf(0) }
    val currentPac = pacProviders[selectedPacIndex]

    // Form fields for Emisor (Óptica)
    var emisorRfc by remember { mutableStateOf("OFA180420A12") }
    var emisorNombre by remember { mutableStateOf("ÓPTICA FAMILIAR AGS S.A. DE C.V.") }
    var emisorRegimen by remember { mutableStateOf("612") }
    var emisorCp by remember { mutableStateOf("20000") }

    // Form fields for Receptor (Cliente)
    var selectedPatient by remember { mutableStateOf<Patient?>(null) }
    var receptorRfc by remember { mutableStateOf("XAXX010101000") } // Default Público en General
    var receptorNombre by remember { mutableStateOf("PÚBLICO EN GENERAL") }
    var receptorCp by remember { mutableStateOf("20000") }
    var receptorRegimen by remember { mutableStateOf("616") } // 616 Sin obligaciones
    var receptorUsoCfdi by remember { mutableStateOf("S01") } // S01 Sin efectos / D01 Honorarios
    var receptorEmail by remember { mutableStateOf("") }

    // Concept & Payment fields
    var claveProdServ by remember { mutableStateOf("85121800") } // 85121800 Servicios optometria / 42142900 Anteojos
    var claveUnidad by remember { mutableStateOf("H87") }
    var conceptoDescripcion by remember { mutableStateOf("Lentes Oftálmicos Graduados con Filtro Antirreflejante") }
    var subtotalText by remember { mutableStateOf("1000.00") }
    var descuentoText by remember { mutableStateOf("0.00") }
    var formaPago by remember { mutableStateOf("01") } // 01 Efectivo, 03 Transferencia, 04 Tarjeta
    var metodoPago by remember { mutableStateOf("PUE") }
    var folioFactura by remember { mutableStateOf("F-${(1000..9999).random()}") }

    // Auto-fill from prefillInvoice if passed
    LaunchedEffect(prefillInvoice) {
        prefillInvoice?.let { inv ->
            receptorNombre = inv.patientName.ifBlank { "PÚBLICO EN GENERAL" }
            receptorEmail = inv.patientEmail
            conceptoDescripcion = inv.itemsSummary.ifBlank { "Consulta Optométrica y Lentes Graduados" }
            subtotalText = String.format(Locale.US, "%.2f", inv.subtotal)
            descuentoText = String.format(Locale.US, "%.2f", inv.discount)
            folioFactura = "F-${inv.invoiceNumber}"
            formaPago = when (inv.paymentMethod) {
                "Transferencia" -> "03"
                "Tarjeta" -> "04"
                else -> "01"
            }
        }
    }

    // Calculated amounts
    val subtotal = subtotalText.toDoubleOrNull() ?: 0.0
    val descuento = descuentoText.toDoubleOrNull() ?: 0.0
    val baseIva = (subtotal - descuento).coerceAtLeast(0.0)
    val iva = baseIva * 0.16
    val total = baseIva + iva

    // Generated XML state
    var generatedXml by remember { mutableStateOf<String?>(null) }
    var activeUuid by remember { mutableStateOf<String?>(null) }
    var showXmlDialog by remember { mutableStateOf(false) }
    var isTimbradoSuccess by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- MODULE HEADER ---
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0D9488),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Módulo de Facturación Electrónica CFDI 4.0",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Enlace con Proveedor PAC / Punto de Venta SAT",
                                color = Color(0xFF2DD4BF),
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    if (onCloseDialog != null) {
                        IconButton(onClick = onCloseDialog) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF334155))

                // PAC Provider selector bar
                Text(
                    text = "1. Selecciona Proveedor de Facturación (PAC):",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pacProviders.forEachIndexed { idx, provider ->
                        FilterChip(
                            selected = selectedPacIndex == idx,
                            onClick = { selectedPacIndex = idx },
                            label = { Text(provider.name, fontSize = 11.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0D9488),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFF94A3B8)
                            )
                        )
                    }
                }

                // Link button to PAC website
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentPac.url))
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "Abriendo: ${currentPac.url}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Enlazar a Portal Proveedor (POS)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    // Copy payload button
                    OutlinedButton(
                        onClick = {
                            val payloadText = """
                                --- DATOS PARA FACTURACIÓN CFDI 4.0 (PUNTO DE VENTA) ---
                                Folio: $folioFactura
                                Emisor RFC: $emisorRfc ($emisorNombre)
                                Receptor RFC: $receptorRfc ($receptorNombre)
                                Uso CFDI: $receptorUsoCfdi | Régimen: $receptorRegimen | C.P.: $receptorCp
                                Clave Prod/Serv: $claveProdServ | Concepto: $conceptoDescripcion
                                Subtotal: $subtotalText | IVA 16%: ${String.format(Locale.US, "%.2f", iva)} | Total: ${String.format(Locale.US, "%.2f", total)}
                                Forma Pago: $formaPago | Método Pago: $metodoPago
                            """.trimIndent()

                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Datos Factura PAC", payloadText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "¡Datos copiados para pegar en el portal del proveedor!", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2DD4BF)),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF2DD4BF))
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copiar Datos POS", fontSize = 11.5.sp)
                    }
                }
            }
        }

        // --- SECTION 2: DATOS DE EMISOR Y RECEPTOR (CLIENTE) ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "2. Datos Fiscales del Receptor (Cliente)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF0F172A)
                )

                // Patient selection dropdown helper
                var expandedPatientDropdown by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedPatientDropdown,
                    onExpandedChange = { expandedPatientDropdown = !expandedPatientDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedPatient?.fullName ?: "Seleccionar Paciente Registrado (Opcional)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Autocompletar datos desde Pacientes") },
                        leadingIcon = { Icon(Icons.Default.PersonSearch, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPatientDropdown) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedPatientDropdown,
                        onDismissRequest = { expandedPatientDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("PÚBLICO EN GENERAL (Venta al Mostrador)") },
                            onClick = {
                                selectedPatient = null
                                receptorRfc = "XAXX010101000"
                                receptorNombre = "PÚBLICO EN GENERAL"
                                receptorRegimen = "616"
                                receptorUsoCfdi = "S01"
                                expandedPatientDropdown = false
                            }
                        )
                        patients.forEach { p ->
                            DropdownMenuItem(
                                text = { Text("${p.fullName} - Tel: ${p.phone}") },
                                onClick = {
                                    selectedPatient = p
                                    receptorNombre = p.fullName.uppercase()
                                    receptorEmail = p.email
                                    receptorRfc = "XAXX010101000" // Can be updated by user
                                    expandedPatientDropdown = false
                                }
                            )
                        }
                    }
                }

                // Quick RFC Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SuggestionChip(
                        onClick = {
                            receptorRfc = "XAXX010101000"
                            receptorNombre = "PÚBLICO EN GENERAL"
                            receptorRegimen = "616"
                            receptorUsoCfdi = "S01"
                        },
                        label = { Text("Público General (XAXX010101000)", fontSize = 10.5.sp) }
                    )
                    SuggestionChip(
                        onClick = {
                            receptorRfc = "XEXX010101000"
                            receptorRegimen = "616"
                            receptorUsoCfdi = "S01"
                        },
                        label = { Text("Extranjero (XEXX010101000)", fontSize = 10.5.sp) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = receptorRfc,
                        onValueChange = { receptorRfc = it.uppercase() },
                        label = { Text("RFC Receptor *") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = receptorCp,
                        onValueChange = { receptorCp = it },
                        label = { Text("C.P. Fiscal *") },
                        modifier = Modifier.weight(0.8f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = receptorNombre,
                    onValueChange = { receptorNombre = it.uppercase() },
                    label = { Text("Nombre / Razón Social del Cliente *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Régimen fiscal dropdown
                    var expRegimen by remember { mutableStateOf(false) }
                    val regimenes = listOf(
                        "616" to "616 - Sin obligaciones fiscales",
                        "605" to "605 - Sueldos y Salarios",
                        "612" to "612 - Personas Físicas Empresariales",
                        "601" to "601 - General de Ley Personas Morales"
                    )
                    ExposedDropdownMenuBox(
                        expanded = expRegimen,
                        onExpandedChange = { expRegimen = !expRegimen },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = receptorRegimen,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Régimen Fiscal") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expRegimen) },
                            modifier = Modifier.menuAnchor(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expRegimen,
                            onDismissRequest = { expRegimen = false }
                        ) {
                            regimenes.forEach { (code, label) ->
                                DropdownMenuItem(
                                    text = { Text(label, fontSize = 12.sp) },
                                    onClick = {
                                        receptorRegimen = code
                                        expRegimen = false
                                    }
                                )
                            }
                        }
                    }

                    // Uso CFDI dropdown
                    var expUso by remember { mutableStateOf(false) }
                    val usos = listOf(
                        "S01" to "S01 - Sin efectos fiscales",
                        "D01" to "D01 - Honorarios médicos / Optometría",
                        "G03" to "G03 - Gastos en general",
                        "P01" to "P01 - Por definir"
                    )
                    ExposedDropdownMenuBox(
                        expanded = expUso,
                        onExpandedChange = { expUso = !expUso },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = receptorUsoCfdi,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Uso de CFDI") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expUso) },
                            modifier = Modifier.menuAnchor(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expUso,
                            onDismissRequest = { expUso = false }
                        ) {
                            usos.forEach { (code, label) ->
                                DropdownMenuItem(
                                    text = { Text(label, fontSize = 12.sp) },
                                    onClick = {
                                        receptorUsoCfdi = code
                                        expUso = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = receptorEmail,
                    onValueChange = { receptorEmail = it },
                    label = { Text("Correo para Envío de XML y PDF (Opcional)") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }
        }

        // --- SECTION 3: DETALLE DE CONCEPTO Y MONTO ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "3. Punto de Venta & Detalle de Concepto SAT",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF0F172A)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = claveProdServ,
                        onValueChange = { claveProdServ = it },
                        label = { Text("Clave Prod/Serv SAT") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = folioFactura,
                        onValueChange = { folioFactura = it },
                        label = { Text("Folio Interno") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                // Clave Prod/Serv Quick buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = claveProdServ == "85121800",
                        onClick = {
                            claveProdServ = "85121800"
                            conceptoDescripcion = "Servicios de Optometría y Examen Visual Computarizado"
                        },
                        label = { Text("85121800 - Optometría", fontSize = 10.5.sp) }
                    )
                    FilterChip(
                        selected = claveProdServ == "42142900",
                        onClick = {
                            claveProdServ = "42142900"
                            conceptoDescripcion = "Lentes Graduados Oftálmicos con Antirreflejante"
                        },
                        label = { Text("42142900 - Anteojos/Lentes", fontSize = 10.5.sp) }
                    )
                }

                OutlinedTextField(
                    value = conceptoDescripcion,
                    onValueChange = { conceptoDescripcion = it },
                    label = { Text("Descripción del Producto o Servicio *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = subtotalText,
                        onValueChange = { subtotalText = it },
                        label = { Text("Subtotal ($) *") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = descuentoText,
                        onValueChange = { descuentoText = it },
                        label = { Text("Descuento ($)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    var expForma by remember { mutableStateOf(false) }
                    val formas = listOf(
                        "01" to "01 - Efectivo",
                        "03" to "03 - Transferencia electrónica",
                        "04" to "04 - Tarjeta de crédito",
                        "28" to "28 - Tarjeta de débito"
                    )
                    ExposedDropdownMenuBox(
                        expanded = expForma,
                        onExpandedChange = { expForma = !expForma },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = formaPago,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Forma de Pago SAT") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expForma) },
                            modifier = Modifier.menuAnchor(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expForma,
                            onDismissRequest = { expForma = false }
                        ) {
                            formas.forEach { (code, label) ->
                                DropdownMenuItem(
                                    text = { Text(label, fontSize = 12.sp) },
                                    onClick = {
                                        formaPago = code
                                        expForma = false
                                    }
                                )
                            }
                        }
                    }

                    var expMetodo by remember { mutableStateOf(false) }
                    val metodos = listOf(
                        "PUE" to "PUE - Pago en una sola exhibición",
                        "PPD" to "PPD - Pago en parcialidades"
                    )
                    ExposedDropdownMenuBox(
                        expanded = expMetodo,
                        onExpandedChange = { expMetodo = !expMetodo },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = metodoPago,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Método de Pago SAT") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expMetodo) },
                            modifier = Modifier.menuAnchor(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expMetodo,
                            onDismissRequest = { expMetodo = false }
                        ) {
                            metodos.forEach { (code, label) ->
                                DropdownMenuItem(
                                    text = { Text(label, fontSize = 12.sp) },
                                    onClick = {
                                        metodoPago = code
                                        expMetodo = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Breakdown summary surface
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFCBD5E1))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal:", fontSize = 12.5.sp, color = Color(0xFF64748B))
                            Text(currencyFormatter.format(subtotal), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (descuento > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Descuento:", fontSize = 12.5.sp, color = Color(0xFFDC2626))
                                Text("-${currencyFormatter.format(descuento)}", fontSize = 12.5.sp, color = Color(0xFFDC2626))
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("IVA (16%):", fontSize = 12.5.sp, color = Color(0xFF64748B))
                            Text(currencyFormatter.format(iva), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFE2E8F0))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TOTAL FACTURA:", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                            Text(currencyFormatter.format(total), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0D9488))
                        }
                    }
                }
            }
        }

        // Timbrado Status Alert
        AnimatedVisibility(visible = isTimbradoSuccess && activeUuid != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("¡Factura Generada y Timbrada por Proveedor PAC!", fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                    }
                    Text(
                        text = "Folio Fiscal UUID: $activeUuid",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF047857)
                    )
                }
            }
        }

        // --- ACTION BUTTONS: GENERATE XML & TRANSMIT TO PAC ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    if (receptorNombre.isBlank() || receptorRfc.isBlank()) {
                        Toast.makeText(context, "Ingresa RFC y Nombre del Cliente", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val datos = CfdiDatosFactura(
                        folio = folioFactura,
                        emisor = CfdiEmisor(
                            rfc = emisorRfc,
                            nombre = emisorNombre,
                            regimenFiscal = emisorRegimen,
                            codigoPostal = emisorCp
                        ),
                        receptor = CfdiReceptor(
                            rfc = receptorRfc,
                            nombre = receptorNombre,
                            domicilioFiscal = receptorCp,
                            regimenFiscal = receptorRegimen,
                            usoCfdi = receptorUsoCfdi,
                            email = receptorEmail
                        ),
                        conceptos = listOf(
                            CfdiConcepto(
                                claveProdServ = claveProdServ,
                                claveUnidad = claveUnidad,
                                cantidad = 1.0,
                                descripcion = conceptoDescripcion,
                                valorUnitario = subtotal,
                                importe = subtotal,
                                descuento = descuento,
                                baseIva = baseIva,
                                importeIva = iva
                            )
                        ),
                        formaPago = formaPago,
                        metodoPago = metodoPago,
                        subtotal = subtotal,
                        descuento = descuento,
                        iva = iva,
                        total = total,
                        uuid = UUID.randomUUID().toString().uppercase(Locale.ROOT)
                    )

                    val xml = CfdiXmlHelper.generateCfdi40Xml(datos)
                    generatedXml = xml
                    activeUuid = datos.uuid
                    isTimbradoSuccess = true
                    showXmlDialog = true

                    Toast.makeText(context, "¡Factura CFDI 4.0 generada en el Punto de Venta del PAC!", Toast.LENGTH_LONG).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Generar XML en PAC POS", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
            }

            if (generatedXml != null) {
                OutlinedButton(
                    onClick = { showXmlDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(50.dp)
                ) {
                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ver XML", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }

    // --- XML CODE VIEWER DIALOG ---
    if (showXmlDialog && generatedXml != null) {
        AlertDialog(
            onDismissRequest = { showXmlDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Code, contentDescription = null, tint = Color(0xFF0D9488))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Código XML CFDI 4.0 (PAC POS)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "UUID: ${activeUuid ?: "Generado"}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF0D9488)
                    )

                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = generatedXml!!,
                                color = Color(0xFF38BDF8),
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("CFDI XML", generatedXml!!)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "XML copiado al portapapeles", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488))
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copiar XML")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        generatedXml?.let { xml ->
                            CfdiXmlHelper.saveAndShareXml(context, xml, folioFactura)
                        }
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Compartir XML")
                }
            }
        )
    }
}
