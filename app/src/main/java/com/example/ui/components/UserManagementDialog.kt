package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.UserAccount
import com.example.ui.OpticaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementDialog(
    viewModel: OpticaViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val users by viewModel.userAccounts.collectAsState()
    val currentUserRole by viewModel.currentUserRole.collectAsState()
    val currentUserEmail by viewModel.currentUserEmail.collectAsState()

    var showAddUserModal by remember { mutableStateOf(false) }
    var selectedRoleFilter by remember { mutableStateOf("Todos") }

    val filteredUsers = remember(users, selectedRoleFilter) {
        if (selectedRoleFilter == "Todos") users
        else users.filter { it.role == selectedRoleFilter }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "Gestión de Usuarios y Permisos",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Panel de Administrador • Firebase Auth",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                            }
                        },
                        actions = {
                            Button(
                                onClick = { showAddUserModal = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Nuevo Usuario", fontSize = 13.sp)
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp)
                ) {
                    // Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val roles = listOf("Todos", "Administrador", "Optometrista", "Caja", "Auxiliar")
                        roles.forEach { role ->
                            FilterChip(
                                selected = selectedRoleFilter == role,
                                onClick = { selectedRoleFilter = role },
                                label = { Text(role, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (filteredUsers.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.GroupOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No se encontraron usuarios en esta categoría",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(filteredUsers, key = { it.uid }) { user ->
                                UserAccountCard(
                                    user = user,
                                    isSelf = user.email.equals(currentUserEmail, ignoreCase = true),
                                    onRoleChange = { newRole ->
                                        viewModel.updateUserRole(user, newRole)
                                        Toast.makeText(context, "Rol de ${user.displayName} actualizado a $newRole", Toast.LENGTH_SHORT).show()
                                    },
                                    onDeleteUser = {
                                        viewModel.deleteUserAccount(user)
                                        Toast.makeText(context, "Usuario eliminado", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddUserModal) {
        AddUserModal(
            viewModel = viewModel,
            onDismiss = { showAddUserModal = false }
        )
    }
}

@Composable
fun UserAccountCard(
    user: UserAccount,
    isSelf: Boolean,
    onRoleChange: (String) -> Unit,
    onDeleteUser: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showConfirmDelete by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Role Avatar Badge
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = CircleShape,
                    color = getRoleBadgeColor(user.role).copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = getRoleIcon(user.role),
                            fontSize = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (isSelf) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Tú",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    RoleBadge(role = user.role)
                }
            }

            // Action Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    OutlinedButton(
                        onClick = { showMenu = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Rol", style = MaterialTheme.typography.labelSmall)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        listOf("Administrador", "Optometrista", "Caja", "Auxiliar").forEach { roleName ->
                            DropdownMenuItem(
                                text = { Text(roleName) },
                                onClick = {
                                    onRoleChange(roleName)
                                    showMenu = false
                                }
                            )
                        }
                    }
                }

                if (!isSelf) {
                    IconButton(onClick = { showConfirmDelete = true }) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Eliminar usuario",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("Eliminar Usuario") },
            text = { Text("¿Estás seguro de que deseas eliminar a ${user.displayName} (${user.email})?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteUser()
                        showConfirmDelete = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun RoleBadge(role: String) {
    val (color, label) = when (role) {
        "Administrador" -> Color(0xFF673AB7) to "👑 Administrador"
        "Optometrista" -> Color(0xFF00897B) to "👁️ Optometrista"
        "Caja" -> Color(0xFF2E7D32) to "💳 Caja / POS"
        "Auxiliar" -> Color(0xFFE65100) to "📋 Auxiliar"
        else -> Color.Gray to role
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

fun getRoleBadgeColor(role: String): Color {
    return when (role) {
        "Administrador" -> Color(0xFF673AB7)
        "Optometrista" -> Color(0xFF00897B)
        "Caja" -> Color(0xFF2E7D32)
        "Auxiliar" -> Color(0xFFE65100)
        else -> Color.Gray
    }
}

fun getRoleIcon(role: String): String {
    return when (role) {
        "Administrador" -> "👑"
        "Optometrista" -> "👁️"
        "Caja" -> "💳"
        "Auxiliar" -> "📋"
        else -> "👤"
    }
}

@Composable
fun AddUserModal(
    viewModel: OpticaViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf("Optometrista") }

    var isSubmitting by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Registrar Nuevo Usuario",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Nombre Completo") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo Electrónico (Firebase Auth)") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña (mínimo 6 caracteres)") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text(
                    text = "Asignar Rol de Usuario:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        "Optometrista" to "Exámenes visuales, expediente clínico y citas",
                        "Caja" to "Cobros POS, caja chica y corte de caja",
                        "Auxiliar" to "Atención a clientes y consulta de agenda",
                        "Administrador" to "Acceso total del sistema y gestión de usuarios"
                    ).forEach { (roleOption, description) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedRole == roleOption) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent,
                            border = if (selectedRole == roleOption) CardDefaults.outlinedCardBorder() else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedRole == roleOption,
                                    onClick = { selectedRole = roleOption }
                                )
                                Column {
                                    Text(
                                        text = roleOption,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (statusMessage != null) {
                    Surface(
                        color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = statusMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isSubmitting = true
                    statusMessage = null
                    viewModel.createStaffUserByAdmin(
                        context = context,
                        displayName = displayName,
                        email = email,
                        pass = password,
                        role = selectedRole
                    ) { success, message ->
                        isSubmitting = false
                        isError = !success
                        statusMessage = message
                        if (success) {
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            onDismiss()
                        }
                    }
                },
                enabled = !isSubmitting && displayName.isNotBlank() && email.isNotBlank() && password.length >= 6
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Crear Usuario")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
