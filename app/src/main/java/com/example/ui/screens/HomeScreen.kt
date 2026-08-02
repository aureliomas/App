package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Appointment
import com.example.data.model.InventoryItem
import com.example.data.model.Patient
import com.example.ui.OpticaViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: OpticaViewModel,
    onNavigateToAppointments: () -> Unit,
    onNavigateToPatients: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToBilling: () -> Unit,
    onNavigateToAutomations: () -> Unit,
    onOpenAddAppointmentDialog: () -> Unit,
    onOpenAddPatientDialog: () -> Unit,
    onOpenAddInvoiceDialog: () -> Unit
) {
    val context = LocalContext.current
    val patients by viewModel.patients.collectAsState()
    val appointments by viewModel.appointments.collectAsState()
    val inventory by viewModel.inventory.collectAsState()
    val invoices by viewModel.invoices.collectAsState()

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("es", "MX")) }
    val todayDateStr = remember { SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "MX")).format(Date()) }

    // Computations for dashboard stats
    val totalRevenue = remember(invoices) { invoices.sumOf { it.amountPaid } }
    val pendingReceivables = remember(invoices) { invoices.sumOf { (it.total - it.amountPaid).coerceAtLeast(0.0) } }
    val lowStockCount = remember(inventory) { inventory.count { it.stockQuantity <= it.minStockThreshold } }
    
    val todayAppointments = remember(appointments) {
        val calNow = Calendar.getInstance()
        appointments.filter { appt ->
            val calAppt = Calendar.getInstance().apply { timeInMillis = appt.dateTime }
            calNow.get(Calendar.YEAR) == calAppt.get(Calendar.YEAR) &&
                    calNow.get(Calendar.DAY_OF_YEAR) == calAppt.get(Calendar.DAY_OF_YEAR)
        }
    }

    val lowStockItems = remember(inventory) {
        inventory.filter { it.stockQuantity <= it.minStockThreshold }
    }

    val currentMonth = remember { Calendar.getInstance().get(Calendar.MONTH) + 1 }
    val birthdayPatients = remember(patients, currentMonth) {
        patients.filter { p ->
            try {
                val parts = p.dateOfBirth.split("-")
                if (parts.size >= 2) parts[1].toInt() == currentMonth else false
            } catch (e: Exception) { false }
        }
    }

    val currentUsername by viewModel.currentUsername.collectAsState()
    val currentUserRole by viewModel.currentUserRole.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Hero Header Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_optica_hero_1785655284745),
                        contentDescription = "Óptica Header",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                        MaterialTheme.colorScheme.primary
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiary,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "ÓPTICA FAMILIAR AGS",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Bienvenido, $currentUsername",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = todayDateStr.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        // Active Session & Role Status Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
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
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Sesión Activa: $currentUsername",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when (currentUserRole) {
                                    "Administrador" -> "Permisos: Control Total • Creación de Usuarios • Reportes"
                                    "Optometrista" -> "Permisos: Expedientes Clínicos • Graduaciones • Citas"
                                    "Caja" -> "Permisos: Punto de Venta • Corte X/Z • Facturación"
                                    "Auxiliar" -> "Permisos: Atención a Clientes • Consultas de Inventario"
                                    else -> "Rol: $currentUserRole"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        color = when (currentUserRole) {
                            "Administrador" -> Color(0xFF673AB7)
                            "Optometrista" -> Color(0xFF00897B)
                            "Caja" -> Color(0xFF2E7D32)
                            "Auxiliar" -> Color(0xFFE65100)
                            else -> MaterialTheme.colorScheme.primary
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = currentUserRole,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Quick Actions Row
        item {
            Column {
                Text(
                    text = "Acciones Rápidas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        QuickActionButton(
                            icon = Icons.Default.AddAlarm,
                            label = "Nueva Cita",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            onClick = onOpenAddAppointmentDialog
                        )
                    }
                    item {
                        QuickActionButton(
                            icon = Icons.Default.PersonAdd,
                            label = "Nuevo Paciente",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            onClick = onOpenAddPatientDialog
                        )
                    }
                    item {
                        QuickActionButton(
                            icon = Icons.Default.PointOfSale,
                            label = "Nueva Venta",
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            onClick = onOpenAddInvoiceDialog
                        )
                    }
                    item {
                        QuickActionButton(
                            icon = Icons.Default.Cake,
                            label = "Cumpleaños (${birthdayPatients.size})",
                            containerColor = Color(0xFFFDE68A),
                            contentColor = Color(0xFF78350F),
                            onClick = onNavigateToAutomations
                        )
                    }
                }
            }
        }

        // Metrics Dashboard Grid
        item {
            Column {
                Text(
                    text = "Resumen de Hoy",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardMetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Citas Hoy",
                        value = "${todayAppointments.size}",
                        subtitle = "Agendadas",
                        icon = Icons.Default.CalendarMonth,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = onNavigateToAppointments
                    )
                    DashboardMetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Ingresos",
                        value = currencyFormatter.format(totalRevenue),
                        subtitle = if (pendingReceivables > 0) "Pendiente: ${currencyFormatter.format(pendingReceivables)}" else "Al día",
                        icon = Icons.Default.Payments,
                        color = Color(0xFF059669),
                        onClick = onNavigateToBilling
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardMetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Stock Bajo",
                        value = "$lowStockCount",
                        subtitle = "Productos por reponer",
                        icon = Icons.Default.Warning,
                        color = if (lowStockCount > 0) Color(0xFFD97706) else MaterialTheme.colorScheme.secondary,
                        onClick = onNavigateToInventory
                    )
                    DashboardMetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Pacientes",
                        value = "${patients.size}",
                        subtitle = "Expedientes registrados",
                        icon = Icons.Default.People,
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = onNavigateToPatients
                    )
                }
            }
        }

        // Today's Appointments Section
        item {
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Citas Programadas para Hoy",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(onClick = onNavigateToAppointments) {
                            Text("Ver todas")
                        }
                    }

                    if (todayAppointments.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No hay citas programadas para el día de hoy.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        todayAppointments.forEach { appt ->
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = appt.patientName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(appt.dateTime))} • ${appt.reason}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StatusBadge(status = appt.status)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            viewModel.sendWhatsAppMessage(
                                                context = context,
                                                phone = appt.patientPhone,
                                                message = viewModel.buildAppointmentReminderText(appt)
                                            )
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Enviar Recordatorio WhatsApp",
                                            tint = Color(0xFF25D366)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Low Stock Inventory Section
        if (lowStockItems.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706))))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Alertas de Inventario (${lowStockItems.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF78350F)
                                )
                            }
                            TextButton(onClick = onNavigateToInventory) {
                                Text("Gestionar", color = Color(0xFFD97706))
                            }
                        }
                        lowStockItems.take(3).forEach { item ->
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFFDE68A))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF78350F)
                                    )
                                    Text(
                                        text = "${item.category} • Marca: ${item.brand}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF92400E)
                                    )
                                }
                                Surface(
                                    color = Color(0xFFFEE2E2),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Quedan: ${item.stockQuantity}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF991B1B),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        modifier = Modifier.height(72.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = contentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Composable
fun DashboardMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
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
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = CircleShape,
                    color = color.copy(alpha = 0.1f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bgColor, textColor) = when (status) {
        "Confirmada" -> Color(0xFFD1FAE5) to Color(0xFF065F46)
        "Completada" -> Color(0xFFE0E7FF) to Color(0xFF3730A3)
        "Cancelada" -> Color(0xFFFEE2E2) to Color(0xFF991B1B)
        else -> Color(0xFFFEF3C7) to Color(0xFF92400E) // Pendiente
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
