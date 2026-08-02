package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sku: String,
    val name: String,
    val brand: String,
    val category: String, // Armazones, Micas, Lentes de Contacto, Soluciones, Accesorios
    val stockQuantity: Int,
    val minStockThreshold: Int = 3,
    val costPrice: Double,
    val salePrice: Double,
    val notes: String = ""
)
