package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Appointment
import com.example.data.model.Patient
import com.example.ui.OpticaViewModel
import java.text.SimpleDateFormat
import java.util.*

data class PromoOffer(
    val title: String,
    val description: String,
    val imageRes: Int,
    val badge: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromoWebScreen(
    viewModel: OpticaViewModel
) {
    val context = LocalContext.current

    // Available promotional images & offers
    val promoList = listOf(
        PromoOffer(
            title = "¡PROMO 2X1 EN LENTES COMPLETOS!",
            description = "En la compra de tu primer par de anteojos graduados con antirreflejante, llévate el segundo par ¡TOTALMENTE GRATIS! Ideal para repuesto o sol.",
            imageRes = R.drawable.promo_banner_2x1_1785660626859,
            badge = "OFERTA DEL MES"
        ),
        PromoOffer(
            title = "EXAMEN DE LA VISTA GRATIS + PROGRESIVOS",
            description = "Realiza tu examen visual computarizado sin costo en la compra de tus lentes progresivos digitales con filtro antireflejante Crizal.",
            imageRes = R.drawable.promo_banner_exam_1785660649244,
            badge = "SALUD VISUAL"
        ),
        PromoOffer(
            title = "DESCUENTO FAMILIAR EN ARMAZONES DE MARCA",
            description = "Aprovecha hasta 30% de descuento directo en armazones oftálmicos seleccionados para toda la familia en Óptica Familiar Ags.",
            imageRes = R.drawable.img_optica_hero_1785655284745,
            badge = "ESPECIAL FAMILIAR"
        )
    )

    var selectedPromoIndex by remember { mutableIntStateOf(0) }
    var showChangePromoDialog by remember { mutableStateOf(false) }

    val currentPromo = promoList[selectedPromoIndex]

    // Form fields for appointment
    var patientName by remember { mutableStateOf("") }
    var patientPhone by remember { mutableStateOf("") }
    var selectedService by remember { mutableStateOf("Examen de la Vista Computarizado") }
    var appointmentDate by remember { mutableStateOf("Mañana - 11:00 AM") }
    var notes by remember { mutableStateOf("Deseo aprovechar la promoción del mes 2x1.") }

    var isBookedSuccess by remember { mutableStateOf(false) }

    val serviceOptions = listOf(
        "Examen de la Vista Computarizado",
        "Adaptación de Lentes de Contacto",
        "Lentes Progresivos / Bifocales",
        "Graduación / Cambio de Micas",
        "Ajuste y Mantenimiento de Armazón"
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(scrollState)
    ) {
        // --- WEB HEADER SIMULATION BANNER ---
        Surface(
            color = Color(0xFF0F172A),
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_optica_logo),
                                contentDescription = "Logo",
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ÓPTICA FAMILIAR AGS",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "www.opticafamiliarags.com",
                                color = Color(0xFF2DD4BF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Admin button to change promo banner
                    OutlinedButton(
                        onClick = { showChangePromoDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF2DD4BF)
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(Color(0xFF2DD4BF), Color(0xFF2DD4BF)))
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cambiar Promo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- HERO PROMOTIONAL BANNER SECTION ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    ) {
                        Image(
                            painter = painterResource(id = currentPromo.imageRes),
                            contentDescription = currentPromo.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Gradient Overlay for readability
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.75f)
                                        ),
                                        startY = 100f
                                    )
                                )
                        )

                        // Promo Badge Top Right
                        Surface(
                            color = Color(0xFF0D9488),
                            shape = RoundedCornerShape(bottomStart = 12.dp, topEnd = 20.dp),
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Text(
                                text = currentPromo.badge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        // Text overlay at bottom
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = currentPromo.title,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Text(
                            text = currentPromo.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF334155),
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Fast Call to Actions Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // WhatsApp Button
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("https://wa.me/524495543087?text=Hola%20Óptica%20Familiar%20Ags,%20quisiera%20información%20de%20la%20promoción")
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Abriendo WhatsApp: 4495543087", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WhatsApp", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            // Call Button
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:4495543087")
                                    }
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Llamar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // --- CONTACT & LOCATION INFO CARD ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "📍 Óptica Familiar Ags - Ubicación y Contacto",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF0F172A)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF0D9488), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Av. Adolfo López Mateos Pte 213, Centro, Aguascalientes, Ags.",
                        fontSize = 12.5.sp,
                        color = Color(0xFF475569)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF0D9488), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Teléfono Directo: 449 554 3087",
                        fontSize = 12.5.sp,
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF0D9488), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Horario: Lunes a Sábado de 10:00 AM a 8:00 PM",
                        fontSize = 12.5.sp,
                        color = Color(0xFF475569)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- APPOINTMENT BOOKING FORM (AGENDAR CITA) ---
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(1.dp, Color(0xFF0D9488).copy(alpha = 0.3f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF0D9488), modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AGENDAR CITA EN LÍNEA",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Completa tus datos para registrar tu cita directamente en el sistema",
                            fontSize = 11.5.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                AnimatedVisibility(visible = isBookedSuccess) {
                    Surface(
                        color = Color(0xFFDCFCE7),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "¡Cita Registrada con Éxito!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF15803D)
                                )
                                Text(
                                    text = "Tu cita ha sido guardada en el sistema de Óptica Familiar Ags. Te esperamos.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF166534)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = patientName,
                    onValueChange = { patientName = it },
                    label = { Text("Nombre Completo del Paciente *") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = patientPhone,
                    onValueChange = { patientPhone = it },
                    label = { Text("Teléfono de Contacto (WhatsApp) *") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Service Dropdown
                var expandedService by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedService,
                    onExpandedChange = { expandedService = !expandedService }
                ) {
                    OutlinedTextField(
                        value = selectedService,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Servicio / Motivo de Consulta") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedService) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedService,
                        onDismissRequest = { expandedService = false }
                    ) {
                        serviceOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    selectedService = opt
                                    expandedService = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = appointmentDate,
                    onValueChange = { appointmentDate = it },
                    label = { Text("Fecha y Hora Deseada (ej. Mañana 11:00 AM)") },
                    leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Comentarios o Promoción a Aplicar") },
                    leadingIcon = { Icon(Icons.Default.StickyNote2, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = {
                        if (patientName.isBlank() || patientPhone.isBlank()) {
                            Toast.makeText(context, "Por favor ingresa tu Nombre y Teléfono", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // Register patient if not existing
                        val newPatient = Patient(
                            fullName = patientName.trim(),
                            phone = patientPhone.trim(),
                            email = "",
                            dateOfBirth = "1990-01-01",
                            address = "Aguascalientes, Ags.",
                            notes = "Registrado desde la Página Web Promocional"
                        )
                        viewModel.savePatient(newPatient)

                        // Register appointment
                        val appt = Appointment(
                            patientId = 0L,
                            patientName = patientName.trim(),
                            patientPhone = patientPhone.trim(),
                            dateTime = System.currentTimeMillis() + (24L * 3600L * 1000L),
                            reason = "$selectedService - $appointmentDate",
                            status = "Pendiente",
                            notes = notes.trim()
                        )
                        viewModel.saveAppointment(appt)

                        isBookedSuccess = true
                        Toast.makeText(context, "¡Cita registrada correctamente en Óptica Familiar Ags!", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AGENDAR CITA Y REGISTRAR", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- FOOTER BRANDING ---
        Surface(
            color = Color(0xFF0F172A),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ÓPTICA FAMILIAR AGS",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Cuidando tu visión con calidez, tecnología y los mejores precios de Aguascalientes.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.5.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // --- DIALOG TO CHANGE PROMOTIONAL BANNER ---
    if (showChangePromoDialog) {
        AlertDialog(
            onDismissRequest = { showChangePromoDialog = false },
            title = {
                Text(
                    text = "Seleccionar Imagen Promocional del Mes",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Elige la promoción principal que verán los pacientes en la página web:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    promoList.forEachIndexed { idx, promo ->
                        Card(
                            onClick = {
                                selectedPromoIndex = idx
                                showChangePromoDialog = false
                                Toast.makeText(context, "Promoción actualizada: ${promo.badge}", Toast.LENGTH_SHORT).show()
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedPromoIndex == idx) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = promo.imageRes),
                                    contentDescription = promo.title,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = promo.badge,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = promo.title,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                }
                                if (selectedPromoIndex == idx) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showChangePromoDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}
