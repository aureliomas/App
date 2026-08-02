package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "financial_records")
data class FinancialRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String, // "Gasto", "Compra de Mercancía", "Pago a Laboratorio", "Renta", "Agua", "Luz", "Internet", "Comisión", "Sueldos", "Insumos", "Auto", "Gastos en General", "Ingreso / Venta"
    val isIncome: Boolean, // true = Ingreso, false = Egreso
    val date: Long = System.currentTimeMillis(),
    val registeredBy: String = "Administrador",
    val notes: String = ""
)
