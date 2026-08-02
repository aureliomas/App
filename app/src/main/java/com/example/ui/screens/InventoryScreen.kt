package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.InventoryItem
import com.example.ui.OpticaViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: OpticaViewModel,
    onOpenAddInventoryDialog: () -> Unit
) {
    val inventory by viewModel.inventory.collectAsState()
    val searchQuery by viewModel.inventorySearchQuery.collectAsState()
    val selectedCategory by viewModel.inventoryCategory.collectAsState()

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("es", "MX")) }

    val categories = listOf("Todas", "Armazones", "Micas", "Lentes de Contacto", "Soluciones", "Accesorios")

    val filteredInventory = remember(inventory, searchQuery, selectedCategory) {
        inventory.filter { item ->
            val matchesCategory = (selectedCategory == "Todas" || item.category == selectedCategory)
            val matchesSearch = searchQuery.isBlank() ||
                    item.name.contains(searchQuery, ignoreCase = true) ||
                    item.brand.contains(searchQuery, ignoreCase = true) ||
                    item.sku.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    val lowStockCount = remember(inventory) {
        inventory.count { it.stockQuantity <= it.minStockThreshold }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenAddInventoryDialog,
                icon = { Icon(Icons.Default.Add, contentDescription = "Agregar Producto") },
                text = { Text("Nuevo Producto") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
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
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setInventorySearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar por SKU, nombre o marca...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setInventorySearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            // Category Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { viewModel.setInventoryCategory(category) },
                        label = { Text(category) }
                    )
                }
            }

            // Low stock banner alert
            if (lowStockCount > 0) {
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFD97706)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Atención: Hay $lowStockCount producto(s) por debajo del inventario mínimo.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF92400E)
                        )
                    }
                }
            }

            // Inventory Item List
            if (filteredInventory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No se encontraron productos en inventario.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredInventory, key = { it.id }) { item ->
                        InventoryItemCard(
                            item = item,
                            currencyFormatter = currencyFormatter,
                            onIncreaseStock = { viewModel.adjustStock(item, 1) },
                            onDecreaseStock = { viewModel.adjustStock(item, -1) },
                            onDelete = { viewModel.deleteInventoryItem(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InventoryItemCard(
    item: InventoryItem,
    currencyFormatter: NumberFormat,
    onIncreaseStock: () -> Unit,
    onDecreaseStock: () -> Unit,
    onDelete: () -> Unit
) {
    val (statusLabel, statusBg, statusFg) = when {
        item.stockQuantity == 0 -> Triple("Agotado", Color(0xFFFEE2E2), Color(0xFF991B1B))
        item.stockQuantity <= item.minStockThreshold -> Triple("Stock Bajo", Color(0xFFFEF3C7), Color(0xFF92400E))
        else -> Triple("En Stock", Color(0xFFD1FAE5), Color(0xFF065F46))
    }

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
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "SKU: ${item.sku} • Marca: ${item.brand}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Categoría: ${item.category}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Precio de Venta: ${currencyFormatter.format(item.salePrice)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Costo: ${currencyFormatter.format(item.costPrice)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Quick Stock Adjuster
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onDecreaseStock,
                        enabled = item.stockQuantity > 0,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Restar stock")
                    }

                    Text(
                        text = "${item.stockQuantity}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    IconButton(
                        onClick = onIncreaseStock,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Sumar stock", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar producto", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
