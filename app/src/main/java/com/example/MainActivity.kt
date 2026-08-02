package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.OpticaDatabase
import com.example.data.OpticaRepository
import com.example.data.model.Patient
import com.example.ui.OpticaViewModel
import com.example.ui.OpticaViewModelFactory
import com.example.ui.components.*
import com.example.util.ClinicalRecordPdfHelper
import com.example.ui.screens.*
import com.example.ui.theme.OpticaCareTheme

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Inicio", Icons.Default.Home)
    object Appointments : Screen("appointments", "Citas", Icons.Default.CalendarMonth)
    object Patients : Screen("patients", "Pacientes", Icons.Default.People)
    object Inventory : Screen("inventory", "Inventario", Icons.Default.Inventory2)
    object Billing : Screen("billing", "POS / Caja", Icons.Default.ReceiptLong)
    object Financial : Screen("financial", "Finanzas & Gastos", Icons.Default.AccountBalance)
    object Automations : Screen("automations", "Recordatorios", Icons.Default.MarkChatRead)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
        } catch (_: Exception) {}

        val database = OpticaDatabase.getDatabase(applicationContext)
        val repository = OpticaRepository(database.opticaDao())
        val factory = OpticaViewModelFactory(repository)

        setContent {
            OpticaCareTheme {
                val viewModel: OpticaViewModel = viewModel(factory = factory)
                val context = LocalContext.current

                var showSplash by remember { mutableStateOf(true) }

                if (showSplash) {
                    SplashScreen(onTimeout = { showSplash = false })
                } else {
                    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
                    val currentUsername by viewModel.currentUsername.collectAsState()
                    val currentUserRole by viewModel.currentUserRole.collectAsState()

                    if (!isLoggedIn) {
                        LoginScreen(viewModel = viewModel)
                    } else {
                        var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

                        // Dialog States
                        var showAddAppointmentDialog by remember { mutableStateOf(false) }
                        var showAddPatientDialog by remember { mutableStateOf(false) }
                        var showAddRecordPatient by remember { mutableStateOf<Patient?>(null) }
                        var showAddInventoryDialog by remember { mutableStateOf(false) }
                        var showAddInvoiceDialog by remember { mutableStateOf(false) }
                        var showAddFinancialDialog by remember { mutableStateOf(false) }
                        var showUserManagementDialog by remember { mutableStateOf(false) }

                        val patients by viewModel.patients.collectAsState()
                        val inventory by viewModel.inventory.collectAsState()

                        Scaffold(
                            topBar = {
                                OpticaTopAppBar(
                                    title = currentScreen.title,
                                    username = currentUsername,
                                    userRole = currentUserRole,
                                    onOpenUserManagement = { showUserManagementDialog = true },
                                    onLogoutClick = { viewModel.logout() }
                                )
                            },
                            bottomBar = {
                                OpticaBottomNavigationBar(
                                    currentScreen = currentScreen,
                                    onScreenSelected = { currentScreen = it }
                                )
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                when (currentScreen) {
                                    Screen.Home -> HomeScreen(
                                        viewModel = viewModel,
                                        onNavigateToAppointments = { currentScreen = Screen.Appointments },
                                        onNavigateToPatients = { currentScreen = Screen.Patients },
                                        onNavigateToInventory = { currentScreen = Screen.Inventory },
                                        onNavigateToBilling = { currentScreen = Screen.Billing },
                                        onNavigateToAutomations = { currentScreen = Screen.Automations },
                                        onOpenAddAppointmentDialog = { showAddAppointmentDialog = true },
                                        onOpenAddPatientDialog = { showAddPatientDialog = true },
                                        onOpenAddInvoiceDialog = { showAddInvoiceDialog = true }
                                    )

                                    Screen.Appointments -> AppointmentsScreen(
                                        viewModel = viewModel,
                                        onOpenAddAppointmentDialog = { showAddAppointmentDialog = true }
                                    )

                                    Screen.Patients -> PatientsScreen(
                                        viewModel = viewModel,
                                        onOpenAddPatientDialog = { showAddPatientDialog = true },
                                        onOpenAddClinicalRecordDialog = { patient ->
                                            showAddRecordPatient = patient
                                        }
                                    )

                                    Screen.Inventory -> InventoryScreen(
                                        viewModel = viewModel,
                                        onOpenAddInventoryDialog = { showAddInventoryDialog = true }
                                    )

                                    Screen.Billing -> BillingScreen(
                                        viewModel = viewModel,
                                        onOpenAddInvoiceDialog = { showAddInvoiceDialog = true }
                                    )

                                    Screen.Financial -> FinancialScreen(
                                        viewModel = viewModel,
                                        onOpenAddFinancialDialog = { showAddFinancialDialog = true }
                                    )

                                    Screen.Automations -> AutomationsScreen(
                                        viewModel = viewModel
                                    )
                                }
                            }

                            // --- DIALOG MODALS ---
                            if (showAddFinancialDialog) {
                                AddFinancialRecordDialog(
                                    currentUserRole = currentUserRole,
                                    onDismiss = { showAddFinancialDialog = false },
                                    onConfirm = { record ->
                                        viewModel.saveFinancialRecord(record)
                                        showAddFinancialDialog = false
                                    }
                                )
                            }

                            if (showAddAppointmentDialog) {
                                AddAppointmentDialog(
                                    patients = patients,
                                    onDismiss = { showAddAppointmentDialog = false },
                                    onConfirm = { appointment ->
                                        viewModel.saveAppointment(appointment)
                                        showAddAppointmentDialog = false
                                    }
                                )
                            }

                            if (showAddPatientDialog) {
                                AddPatientDialog(
                                    onDismiss = { showAddPatientDialog = false },
                                    onConfirm = { patient ->
                                        viewModel.savePatient(patient)
                                        showAddPatientDialog = false
                                    }
                                )
                            }

                            showAddRecordPatient?.let { patient ->
                                AddClinicalRecordDialog(
                                    patient = patient,
                                    onDismiss = { showAddRecordPatient = null },
                                    onConfirm = { record ->
                                        viewModel.saveClinicalRecord(record)
                                        ClinicalRecordPdfHelper.generateClinicalRecordPdf(context, patient, record)
                                        showAddRecordPatient = null
                                    }
                                )
                            }

                            if (showAddInventoryDialog) {
                                AddInventoryItemDialog(
                                    onDismiss = { showAddInventoryDialog = false },
                                    onConfirm = { item ->
                                        viewModel.saveInventoryItem(item)
                                        showAddInventoryDialog = false
                                    }
                                )
                            }

                            if (showAddInvoiceDialog) {
                                AddInvoiceDialog(
                                    patients = patients,
                                    inventoryItems = inventory,
                                    onDismiss = { showAddInvoiceDialog = false },
                                    onConfirm = { invoice ->
                                        viewModel.createInvoice(invoice)
                                        showAddInvoiceDialog = false
                                    }
                                )
                            }

                            if (showUserManagementDialog) {
                                UserManagementDialog(
                                    viewModel = viewModel,
                                    onDismiss = { showUserManagementDialog = false }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpticaTopAppBar(
    title: String,
    username: String,
    userRole: String,
    onOpenUserManagement: () -> Unit,
    onLogoutClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.img_optica_logo),
                        contentDescription = "Logo ÓPTICA FAMILIAR AGS",
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ÓPTICA FAMILIAR AGS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "$title • $username",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                    RoleBadgeSmall(role = userRole)
                }
            }
        },
        actions = {
            if (userRole == "Administrador") {
                IconButton(onClick = onOpenUserManagement) {
                    Icon(
                        imageVector = Icons.Default.ManageAccounts,
                        contentDescription = "Gestión de Usuarios",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onLogoutClick) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Cerrar Sesión",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun RoleBadgeSmall(role: String) {
    val color: Color
    val label: String
    when (role) {
        "Administrador" -> {
            color = Color(0xFF673AB7)
            label = "👑 Admin"
        }
        "Optometrista" -> {
            color = Color(0xFF00897B)
            label = "👁️ Optometrista"
        }
        "Caja" -> {
            color = Color(0xFF2E7D32)
            label = "💳 Caja"
        }
        "Auxiliar" -> {
            color = Color(0xFFE65100)
            label = "📋 Auxiliar"
        }
        else -> {
            color = Color.Gray
            label = role
        }
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun OpticaBottomNavigationBar(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit
) {
    val screens = listOf(
        Screen.Home,
        Screen.Appointments,
        Screen.Patients,
        Screen.Inventory,
        Screen.Billing,
        Screen.Financial,
        Screen.Automations
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        screens.forEach { screen ->
            NavigationBarItem(
                selected = currentScreen.route == screen.route,
                onClick = { onScreenSelected(screen) },
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title, style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
}
