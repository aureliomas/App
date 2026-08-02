package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CashCut
import com.example.data.model.Invoice
import com.example.ui.OpticaViewModel
import com.example.ui.components.TicketDialog
import com.example.util.TicketPdfHelper
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(
    viewModel: OpticaViewModel,
    onOpenAddInvoiceDialog: () -> Unit
) {
    val context = LocalContext.current
    val invoices by viewModel.invoices.collectAsState()
    val cashCuts by viewModel.cashCuts.collectAsState()
    val previewInvoice by viewModel.ticketPreviewInvoice.collectAsState()

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("es", "MX")) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy - hh:mm a", Locale("es", "MX")) }

    var selectedTabIndex by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedInvoiceForPayment by remember { mutableStateOf<Invoice?>(null) }
    var showCashCutConfirmDialog by remember { mutableStateOf<String?>(null) } // "CORTE_X" or "CORTE_Z"

    val totalCollected = remember(invoices) { invoices.sumOf { it.amountPaid } }
    val totalPending = remember(invoices) { invoices.sumOf { (it.total - it.amountPaid).coerceAtLeast(0.0) } }

    val filteredInvoices = remember(invoices, searchQuery) {
        if (searchQuery.isBlank()) invoices
        else invoices.filter { inv ->
            inv.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                    inv.patientName.contains(searchQuery, ignoreCase = true) ||
                    inv.itemsSummary.contains(searchQuery, ignoreCase = true)
        }
    }

    // Today's Sales Calculation for Corte de Caja
    val startOfDay = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
    }

    val todayInvoices = remember(invoices) { invoices.filter { it.date >= startOfDay } }
    val initialFund = 500.0

    val todayCashSales = remember(todayInvoices) {
        todayInvoices.filter { !it.isCreditSale && it.paymentMethod == "Efectivo" }.sumOf { it.amountPaid }
    }
    val todayCardSales = remember(todayInvoices) {
        todayInvoices.filter { !it.isCreditSale && it.paymentMethod == "Tarjeta" }.sumOf { it.amountPaid }
    }
    val todayTransferSales = remember(todayInvoices) {
        todayInvoices.filter { !it.isCreditSale && it.paymentMethod == "Transferencia" }.sumOf { it.amountPaid }
    }
    val todayCreditSales = remember(todayInvoices) {
        todayInvoices.filter { it.isCreditSale }.sumOf { it.total }
    }
    val todayCreditCollected = remember(todayInvoices) {
        todayInvoices.filter { it.isCreditSale }.sumOf { it.amountPaid }
    }

    val totalTodayRevenue = todayCashSales + todayCardSales + todayTransferSales + todayCreditCollected
    val expectedCashInDrawer = initialFund + todayCashSales + todayCreditCollected

    Scaffold(
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                ExtendedFloatingActionButton(
                    onClick = onOpenAddInvoiceDialog,
                    icon = { Icon(Icons.Default.PointOfSale, contentDescription = "Nueva Venta") },
                    text = { Text("Nueva Venta / POS") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main Section Header & Navigation Tabs
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                TabRow(selectedTabIndex = selectedTabIndex) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Ventas & Ticket", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Receipt, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Corte de Caja (X/Z)", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) }
                    )
                }
            }

            if (selectedTabIndex == 0) {
                // --- TAB 0: VENTAS, TICKETS Y FACTURACIÓN ---
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Financial Summary Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Resumen de Ventas y Cobranza Global",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Total Cobrado",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = currencyFormatter.format(totalCollected),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF059669)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Por Cobrar (Crédito)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = currencyFormatter.format(totalPending),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (totalPending > 0) Color(0xFFD97706) else Color(0xFF059669)
                                    )
                                }
                            }
                        }
                    }

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Buscar ticket por folio, cliente o concepto...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )

                    // Invoices List
                    if (filteredInvoices.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No se encontraron registros de ventas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(filteredInvoices, key = { it.id }) { invoice ->
                                InvoiceCard(
                                    invoice = invoice,
                                    currencyFormatter = currencyFormatter,
                                    onRecordPaymentClick = { selectedInvoiceForPayment = invoice },
                                    onViewTicketClick = { viewModel.showTicketPreview(invoice) },
                                    onShareWhatsApp = { viewModel.sendTicketWhatsApp(context, invoice) }
                                )
                            }
                        }
                    }
                }
            } else {
                // --- TAB 1: CORTE DE CAJA (CORTE X / CORTE Z) ---
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
                ) {
                    // Current Shift Real-time Drawer Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Arqueo de Caja del Día",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Turno En Curso",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(modifier = Modifier.height(12.dp))

                                CashRowItem("Fondo Inicial de Caja", currencyFormatter.format(initialFund), isBold = false)
                                CashRowItem("Ventas en Efectivo", currencyFormatter.format(todayCashSales), isBold = false)
                                CashRowItem("Ventas con Tarjeta", currencyFormatter.format(todayCardSales), isBold = false)
                                CashRowItem("Ventas con Transferencia", currencyFormatter.format(todayTransferSales), isBold = false)
                                CashRowItem("Ventas a Crédito (Otorgado)", currencyFormatter.format(todayCreditSales), isBold = false, isWarning = true)
                                CashRowItem("Abonos Cobrados de Crédito", currencyFormatter.format(todayCreditCollected), isBold = false)

                                Spacer(modifier = Modifier.height(8.dp))
                                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(modifier = Modifier.height(8.dp))

                                CashRowItem("INGRESOS TOTALES HOY", currencyFormatter.format(totalTodayRevenue), isBold = true, isHighlight = true)
                                CashRowItem("EFECTIVO ESPERADO EN CAJÓN", currencyFormatter.format(expectedCashInDrawer), isBold = true, isHighlight = true)
                            }
                        }
                    }

                    // Action Buttons for Corte X & Corte Z
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { showCashCutConfirmDialog = "CORTE_X" },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(imageVector = Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("CORTE X\n(Parcial)", textAlign = TextAlign.Center, fontSize = 12.sp, lineHeight = 14.sp)
                            }

                            Button(
                                onClick = { showCashCutConfirmDialog = "CORTE_Z" },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("CORTE Z\n(Cierre Día)", textAlign = TextAlign.Center, fontSize = 12.sp, lineHeight = 14.sp)
                            }
                        }
                    }

                    // History Title
                    item {
                        Text(
                            text = "Historial de Cortes de Caja Guardados",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (cashCuts.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "Aún no hay cortes de caja registrados hoy.",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        }
                    } else {
                        items(cashCuts, key = { it.id }) { cut ->
                            CashCutCard(cashCut = cut, currencyFormatter = currencyFormatter, dateFormatter = dateFormatter)
                        }
                    }
                }
            }
        }
    }

    // Modal Ticket Dialog Preview
    previewInvoice?.let { invoice ->
        TicketDialog(
            invoice = invoice,
            onDismiss = { viewModel.showTicketPreview(null) },
            onSendWhatsApp = { viewModel.sendTicketWhatsApp(context, it) },
            onSendEmail = { viewModel.sendTicketEmail(context, it) }
        )
    }

    // Modal Payment Collector Dialog
    selectedInvoiceForPayment?.let { invoice ->
        RecordPaymentDialog(
            invoice = invoice,
            currencyFormatter = currencyFormatter,
            onDismiss = { selectedInvoiceForPayment = null },
            onConfirmPayment = { newAmount ->
                viewModel.updateInvoicePayment(invoice, newAmount)
                selectedInvoiceForPayment = null
            }
        )
    }

    // Confirm Cash Cut Dialog
    showCashCutConfirmDialog?.let { cutType ->
        AlertDialog(
            onDismissRequest = { showCashCutConfirmDialog = null },
            title = {
                Text(if (cutType == "CORTE_X") "Generar Corte X (Arqueo Parcial)" else "Generar Corte Z (Cierre de Caja Definitivo)")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (cutType == "CORTE_X")
                            "El Corte X realiza un reporte parcial del turno actual sin cerrar la caja."
                        else
                            "El Corte Z guardará el cierre definitivo del día y dejará el registro asentado en la base de datos."
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Ingresos calculados: ${currencyFormatter.format(totalTodayRevenue)}", fontWeight = FontWeight.Bold)
                    Text("Efectivo en cajón: ${currencyFormatter.format(expectedCashInDrawer)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.recordCashCut(cutType = cutType, initialCash = initialFund)
                        Toast.makeText(context, "$cutType generado y guardado exitosamente", Toast.LENGTH_SHORT).show()
                        showCashCutConfirmDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (cutType == "CORTE_X") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(if (cutType == "CORTE_X") "Confirmar Corte X" else "Confirmar Corte Z")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCashCutConfirmDialog = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun InvoiceCard(
    invoice: Invoice,
    currencyFormatter: NumberFormat,
    onRecordPaymentClick: () -> Unit,
    onViewTicketClick: () -> Unit,
    onShareWhatsApp: () -> Unit
) {
    val dateStr = remember(invoice.date) {
        SimpleDateFormat("dd/MM/yyyy - hh:mm a", Locale("es", "MX")).format(Date(invoice.date))
    }

    val (badgeBg, badgeFg) = when (invoice.paymentStatus) {
        "Pagado" -> Color(0xFFD1FAE5) to Color(0xFF065F46)
        "Abono Parcial" -> Color(0xFFFEF3C7) to Color(0xFF92400E)
        else -> Color(0xFFFEE2E2) to Color(0xFF991B1B)
    }

    val pendingBalance = (invoice.total - invoice.amountPaid).coerceAtLeast(0.0)

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
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = invoice.invoiceNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Cliente: ${invoice.patientName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(color = badgeBg, shape = RoundedCornerShape(12.dp)) {
                    Text(
                        text = if (invoice.isCreditSale) "${invoice.paymentStatus} (Crédito)" else invoice.paymentStatus,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Concepto: ${invoice.itemsSummary}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Total (Con IVA): ${currencyFormatter.format(invoice.total)}", fontWeight = FontWeight.Bold)
                        Text(text = "Abonado: ${currencyFormatter.format(invoice.amountPaid)}", color = Color(0xFF059669))
                    }
                    if (pendingBalance > 0) {
                        Text(
                            text = "Saldo Restante (Crédito): ${currencyFormatter.format(pendingBalance)}",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = onViewTicketClick,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ver Ticket", fontSize = 12.sp)
                    }

                    IconButton(onClick = onShareWhatsApp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar WhatsApp",
                            tint = Color(0xFF25D366)
                        )
                    }
                }

                if (pendingBalance > 0) {
                    Button(
                        onClick = onRecordPaymentClick,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PriceCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Abonar")
                    }
                }
            }
        }
    }
}

@Composable
fun CashRowItem(
    label: String,
    value: String,
    isBold: Boolean = false,
    isWarning: Boolean = false,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = if (isHighlight) 14.sp else 13.sp,
            fontWeight = if (isBold || isHighlight) FontWeight.Bold else FontWeight.Normal,
            color = if (isHighlight) MaterialTheme.colorScheme.primary else Color.DarkGray
        )
        Text(
            text = value,
            fontSize = if (isHighlight) 14.sp else 13.sp,
            fontWeight = if (isBold || isHighlight) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isWarning -> Color(0xFFD97706)
                isHighlight -> MaterialTheme.colorScheme.primary
                else -> Color(0xFF0F172A)
            }
        )
    }
}

@Composable
fun CashCutCard(
    cashCut: CashCut,
    currencyFormatter: NumberFormat,
    dateFormatter: SimpleDateFormat
) {
    val isZ = cashCut.cutType == "CORTE_Z"
    val badgeBg = if (isZ) Color(0xFFFEE2E2) else Color(0xFFE0F2FE)
    val badgeFg = if (isZ) Color(0xFF991B1B) else Color(0xFF0369A1)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isZ) "🔒 CORTE Z (Cierre de Día)" else "⚡ CORTE X (Arqueo Parcial)",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = badgeFg
                )
                Surface(color = badgeBg, shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = cashCut.cutType,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "Fecha: ${dateFormatter.format(Date(cashCut.date))}",
                fontSize = 11.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Ingreso Total:", fontSize = 11.sp, color = Color.Gray)
                    Text(currencyFormatter.format(cashCut.totalIncome), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF059669))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Efectivo Cajón:", fontSize = 11.sp, color = Color.Gray)
                    Text(currencyFormatter.format(cashCut.expectedCashInDrawer), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Realizado por: ${cashCut.performedBy}",
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun RecordPaymentDialog(
    invoice: Invoice,
    currencyFormatter: NumberFormat,
    onDismiss: () -> Unit,
    onConfirmPayment: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf("${invoice.total}") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Abonar a Factura - ${invoice.invoiceNumber}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Cliente: ${invoice.patientName}")
                Text("Total Con IVA: ${currencyFormatter.format(invoice.total)}")
                Text("Abonado previamente: ${currencyFormatter.format(invoice.amountPaid)}")
                val remaining = (invoice.total - invoice.amountPaid).coerceAtLeast(0.0)
                Text("Saldo Pendiente: ${currencyFormatter.format(remaining)}", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Monto Pagado Acumulado Total ($)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: invoice.amountPaid
                    onConfirmPayment(amount)
                }
            ) {
                Text("Guardar Abono")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
