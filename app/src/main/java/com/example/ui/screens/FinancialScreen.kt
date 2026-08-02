package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.data.model.FinancialRecord
import com.example.data.model.Invoice
import com.example.ui.OpticaViewModel
import com.example.util.ExcelCsvHelper
import com.example.util.FinancialReportPdfHelper
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialScreen(
    viewModel: OpticaViewModel,
    onOpenAddFinancialDialog: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Gastos Generales, 1 = Reportes Excel & PDF

    val financialRecords by viewModel.financialRecords.collectAsState()
    val invoices by viewModel.invoices.collectAsState()
    val categoryFilter by viewModel.financialCategoryFilter.collectAsState()
    val typeFilter by viewModel.financialTypeFilter.collectAsState()
    val userRole by viewModel.currentUserRole.collectAsState()

    // Calculated totals
    val totalIncomesFromSales = remember(invoices) { invoices.sumOf { it.amountPaid } }
    val extraIncomes = remember(financialRecords) { financialRecords.filter { it.isIncome }.sumOf { it.amount } }
    val totalIncome = totalIncomesFromSales + extraIncomes

    val totalExpenses = remember(financialRecords) { financialRecords.filter { !it.isIncome }.sumOf { it.amount } }
    val netBalance = totalIncome - totalExpenses

    val numberFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "MX")) }

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = onOpenAddFinancialDialog,
                    icon = { Icon(Icons.Default.Add, contentDescription = "Nuevo Movimiento") },
                    text = { Text("Registrar Gasto / Ingreso") },
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Navigation Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gastos Generales", fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reportes (Excel / PDF)", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            if (selectedTab == 0) {
                // --- TAB 1: GASTOS GENERALES & MOVIMIENTOS ---
                // Summary Metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Ingresos Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Total Ingresos", fontSize = 11.sp, color = Color(0xFF166534), fontWeight = FontWeight.SemiBold)
                            Text(numberFormat.format(totalIncome), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                        }
                    }

                    // Egresos Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Total Egresos", fontSize = 11.sp, color = Color(0xFF991B1B), fontWeight = FontWeight.SemiBold)
                            Text(numberFormat.format(totalExpenses), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB91C1C))
                        }
                    }

                    // Balance Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = if (netBalance >= 0) Color(0xFFE0F2FE) else Color(0xFFFEE2E2))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Utilidad / Neto", fontSize = 11.sp, color = Color(0xFF0369A1), fontWeight = FontWeight.SemiBold)
                            Text(numberFormat.format(netBalance), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (netBalance >= 0) Color(0xFF0369A1) else Color(0xFFB91C1C))
                        }
                    }
                }

                // Type Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = typeFilter == "Todos",
                        onClick = { viewModel.setFinancialTypeFilter("Todos") },
                        label = { Text("Todos") }
                    )
                    FilterChip(
                        selected = typeFilter == "Egresos",
                        onClick = { viewModel.setFinancialTypeFilter("Egresos") },
                        label = { Text("🔴 Egresos") }
                    )
                    FilterChip(
                        selected = typeFilter == "Ingresos",
                        onClick = { viewModel.setFinancialTypeFilter("Ingresos") },
                        label = { Text("🟢 Ingresos") }
                    )
                }

                // Category Filter Scroll Row
                val categories = listOf(
                    "Todas", "Gasto", "Compra de Mercancía", "Pago a Laboratorio",
                    "Renta", "Agua", "Luz", "Internet", "Comisión", "Sueldos",
                    "Insumos", "Auto", "Gastos en General", "Ingreso Extra"
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = categoryFilter == cat,
                            onClick = { viewModel.setFinancialCategoryFilter(cat) },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                // Filtered List
                val filteredRecords = remember(financialRecords, categoryFilter, typeFilter) {
                    financialRecords.filter { rec ->
                        val matchesCategory = (categoryFilter == "Todas" || rec.category == categoryFilter)
                        val matchesType = when (typeFilter) {
                            "Egresos" -> !rec.isIncome
                            "Ingresos" -> rec.isIncome
                            else -> true
                        }
                        matchesCategory && matchesType
                    }
                }

                if (filteredRecords.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No hay movimientos financieros registrados.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                } else {
                    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "MX")) }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredRecords, key = { it.id }) { rec ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                color = if (rec.isIncome) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = rec.category,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (rec.isIncome) Color(0xFF15803D) else Color(0xFF991B1B),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = dateFormat.format(Date(rec.date)),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = rec.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )

                                        if (rec.notes.isNotBlank()) {
                                            Text(
                                                text = rec.notes,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = (if (rec.isIncome) "+ " else "- ") + numberFormat.format(rec.amount),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (rec.isIncome) Color(0xFF15803D) else Color(0xFFB91C1C)
                                        )

                                        IconButton(
                                            onClick = { viewModel.deleteFinancialRecord(rec) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // --- TAB 2: HERRAMIENTA DE GENERACIÓN DE REPORTES EN EXCEL Y PDF ---
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Generador de Reportes de Ventas y Cierre Financiero",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Report Card 1: Ventas del Día
                    ReportGeneratorCard(
                        title = "Ventas del Día",
                        subtitle = "Reporte de ventas de hoy en tiempo real",
                        icon = Icons.Default.Today,
                        onExportPdf = {
                            val now = Calendar.getInstance()
                            val todayInvoices = invoices.filter { inv ->
                                val invCal = Calendar.getInstance().apply { timeInMillis = inv.date }
                                invCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                                        invCal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
                            }
                            FinancialReportPdfHelper.generateFinancialReportPdf(
                                context = context,
                                reportTitle = "REPORTE DE VENTAS DEL DÍA",
                                periodSubtitle = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "MX")).format(Date()),
                                invoices = todayInvoices,
                                expenses = emptyList(),
                                generatedBy = userRole
                            )
                        },
                        onExportExcel = {
                            val now = Calendar.getInstance()
                            val todayInvoices = invoices.filter { inv ->
                                val invCal = Calendar.getInstance().apply { timeInMillis = inv.date }
                                invCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                                        invCal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
                            }
                            ExcelCsvHelper.exportSalesReportToCsv(
                                context = context,
                                title = "Ventas del Día - ${SimpleDateFormat("dd/MM/yyyy", Locale("es", "MX")).format(Date())}",
                                invoices = todayInvoices
                            )
                        }
                    )

                    // Report Card 2: Ventas del Mes
                    ReportGeneratorCard(
                        title = "Ventas del Mes",
                        subtitle = "Resumen y desglose de ventas del mes en curso",
                        icon = Icons.Default.CalendarMonth,
                        onExportPdf = {
                            val now = Calendar.getInstance()
                            val monthInvoices = invoices.filter { inv ->
                                val invCal = Calendar.getInstance().apply { timeInMillis = inv.date }
                                invCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                                        invCal.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                            }
                            FinancialReportPdfHelper.generateFinancialReportPdf(
                                context = context,
                                reportTitle = "REPORTE DE VENTAS DEL MES",
                                periodSubtitle = SimpleDateFormat("MMMM 'de' yyyy", Locale("es", "MX")).format(Date()),
                                invoices = monthInvoices,
                                expenses = emptyList(),
                                generatedBy = userRole
                            )
                        },
                        onExportExcel = {
                            val now = Calendar.getInstance()
                            val monthInvoices = invoices.filter { inv ->
                                val invCal = Calendar.getInstance().apply { timeInMillis = inv.date }
                                invCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                                        invCal.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                            }
                            ExcelCsvHelper.exportSalesReportToCsv(
                                context = context,
                                title = "Ventas del Mes - ${SimpleDateFormat("MMMM yyyy", Locale("es", "MX")).format(Date())}",
                                invoices = monthInvoices
                            )
                        }
                    )

                    // Report Card 3: Cierre Financiero de Mes
                    ReportGeneratorCard(
                        title = "Reporte Financiero de Cierre de Mes",
                        subtitle = "Ingresos por ventas + Gastos Generales (Egresos) = Utilidad Neta (PDF Carta)",
                        icon = Icons.Default.AccountBalance,
                        onExportPdf = {
                            val now = Calendar.getInstance()
                            val monthInvoices = invoices.filter { inv ->
                                val invCal = Calendar.getInstance().apply { timeInMillis = inv.date }
                                invCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                                        invCal.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                            }
                            val monthExpenses = financialRecords.filter { exp ->
                                val expCal = Calendar.getInstance().apply { timeInMillis = exp.date }
                                expCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                                        expCal.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                            }

                            FinancialReportPdfHelper.generateFinancialReportPdf(
                                context = context,
                                reportTitle = "REPORTE FINANCIERO Y CIERRE DE MES",
                                periodSubtitle = SimpleDateFormat("MMMM 'de' yyyy", Locale("es", "MX")).format(Date()),
                                invoices = monthInvoices,
                                expenses = monthExpenses,
                                generatedBy = userRole
                            )
                        },
                        onExportExcel = {
                            val now = Calendar.getInstance()
                            val monthInvoices = invoices.filter { inv ->
                                val invCal = Calendar.getInstance().apply { timeInMillis = inv.date }
                                invCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                                        invCal.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                            }
                            val monthExpenses = financialRecords.filter { exp ->
                                val expCal = Calendar.getInstance().apply { timeInMillis = exp.date }
                                expCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                                        expCal.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                            }

                            val monthSalesTotal = monthInvoices.sumOf { it.amountPaid }
                            val monthExpensesTotal = monthExpenses.filter { !it.isIncome }.sumOf { it.amount }
                            val monthNet = monthSalesTotal - monthExpensesTotal

                            ExcelCsvHelper.exportFinancialClosingToCsv(
                                context = context,
                                title = "Cierre Financiero - ${SimpleDateFormat("MMMM yyyy", Locale("es", "MX")).format(Date())}",
                                totalIncome = monthSalesTotal,
                                totalExpenses = monthExpensesTotal,
                                netProfit = monthNet,
                                invoices = monthInvoices,
                                expenses = monthExpenses
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ReportGeneratorCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onExportPdf: () -> Unit,
    onExportExcel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onExportPdf,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PDF (Carta)", fontSize = 12.sp)
                }

                Button(
                    onClick = onExportExcel,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Excel (.csv)", fontSize = 12.sp)
                }
            }
        }
    }
}
