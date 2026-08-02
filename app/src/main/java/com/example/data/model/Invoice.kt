package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val patientId: Long,
    val patientName: String,
    val patientPhone: String = "",
    val patientEmail: String = "",
    val date: Long = System.currentTimeMillis(),
    val itemsSummary: String, // e.g. "Armazón Ray-Ban Titanium + Mica Fotocromática"
    val subtotal: Double,
    val discount: Double = 0.0,
    val tax: Double = 0.0, // IVA 16%
    val total: Double,
    val paymentStatus: String = "Pagado", // Pagado, Pendiente, Abono Parcial
    val amountPaid: Double = total,
    val paymentMethod: String = "Efectivo", // Efectivo, Tarjeta, Transferencia, Crédito
    val isCreditSale: Boolean = false,
    val optometristName: String = "L.O. BRISAIDA GPE GUILLEN ORTIZ",
    val notes: String = ""
)
