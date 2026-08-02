package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cash_cuts")
data class CashCut(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val cutType: String, // "CORTE_X" (Arqueo Parcial) or "CORTE_Z" (Cierre de Caja)
    val initialCash: Double = 500.0,
    val cashSales: Double = 0.0,
    val cardSales: Double = 0.0,
    val transferSales: Double = 0.0,
    val creditSales: Double = 0.0,
    val creditPaymentsCollected: Double = 0.0,
    val totalIncome: Double = 0.0,
    val expectedCashInDrawer: Double = 0.0,
    val performedBy: String = "L.O. BRISAIDA GPE GUILLEN ORTIZ",
    val notes: String = ""
)
