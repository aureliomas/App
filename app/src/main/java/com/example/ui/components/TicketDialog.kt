package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.model.Invoice
import com.example.util.TicketPdfHelper
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TicketDialog(
    invoice: Invoice,
    onDismiss: () -> Unit,
    onSendWhatsApp: (Invoice) -> Unit,
    onSendEmail: (Invoice) -> Unit
) {
    val context = LocalContext.current
    val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
    val dateFormat = SimpleDateFormat("dd/MM/yyyy - hh:mm a", Locale("es", "MX"))

    val subtotalCalculated = if (invoice.subtotal > 0) invoice.subtotal else (invoice.total / 1.16)
    val taxCalculated = if (invoice.tax > 0) invoice.tax else (invoice.total - subtotalCalculated)
    val pendingBalance = (invoice.total - invoice.amountPaid).coerceAtLeast(0.0)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header Bar with Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ticket Digital de Venta",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Receipt Container (Thermal ticket style on mobile screen)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // --- HEADER ÓPTICA (CENTERED) ---
                        Image(
                            painter = painterResource(id = R.drawable.img_optica_logo),
                            contentDescription = "Logo ÓPTICA FAMILIAR AGS",
                            modifier = Modifier
                                .height(65.dp)
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Text(
                            text = "ÓPTICA FAMILIAR AGS",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Av Adolfo López Mateos #213A col Centro.",
                            fontSize = 12.sp,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "AGUASCALIENTES AGS MÉXICO",
                            fontSize = 12.sp,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Tel / WhatsApp: 449 554 30 87",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Atendió: L.O. BRISAIDA GPE GUILLEN ORTIZ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = Color.Gray, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // --- TICKET META ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "FOLIO: ${invoice.invoiceNumber}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = invoice.paymentStatus.uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (invoice.paymentStatus == "Pagado") Color(0xFF166534) else Color(0xFFB45309)
                            )
                        }
                        Text(
                            text = "Fecha: ${dateFormat.format(Date(invoice.date))}",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = Color.LightGray, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // --- CUSTOMER DATA ---
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "DATOS DEL CLIENTE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "Nombre: ${invoice.patientName}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            if (invoice.patientPhone.isNotBlank()) {
                                Text(text = "Teléfono: ${invoice.patientPhone}", fontSize = 12.sp, color = Color.DarkGray)
                            }
                            if (invoice.patientEmail.isNotBlank()) {
                                Text(text = "Correo: ${invoice.patientEmail}", fontSize = 12.sp, color = Color.DarkGray)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = Color.LightGray, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // --- PURCHASE DESCRIPTION ---
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "DESCRIPCIÓN DE COMPRA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = invoice.itemsSummary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = Color(0xFF1E293B)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = Color.LightGray, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // --- AMOUNTS & TAX ---
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Importe Subtotal (Sin IVA):", fontSize = 12.sp, color = Color.Gray)
                                Text(numberFormat.format(subtotalCalculated), fontSize = 12.sp, color = Color.DarkGray)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Importe IVA (16%):", fontSize = 12.sp, color = Color.Gray)
                                Text(numberFormat.format(taxCalculated), fontSize = 12.sp, color = Color.DarkGray)
                            }

                            if (invoice.discount > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Descuento:", fontSize = 12.sp, color = Color(0xFF166534))
                                    Text("- ${numberFormat.format(invoice.discount)}", fontSize = 12.sp, color = Color(0xFF166534))
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("TOTAL (Con IVA):", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text(numberFormat.format(invoice.total), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Monto Abonado / Pagado:", fontSize = 12.sp, color = Color.DarkGray)
                                Text(numberFormat.format(invoice.amountPaid), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            if (pendingBalance > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Saldo Pendiente (Crédito):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    Text(numberFormat.format(pendingBalance), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Forma de Pago:", fontSize = 12.sp, color = Color.Gray)
                                Text(invoice.paymentMethod, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = Color.Gray, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // --- FOOTER (CENTERED) ---
                        Text(
                            text = "GRACIAS POR SU PREFERENCIA",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "SÍGUENOS EN NUESTRAS REDES SOCIALES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Óptica Familiar ags",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- ACTION BUTTONS ---
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            TicketPdfHelper.shareTicketPdf(context, invoice)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Descargar / Compartir PDF")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onSendWhatsApp(invoice) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF25D366))
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WhatsApp", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { onSendEmail(invoice) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Correo", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
