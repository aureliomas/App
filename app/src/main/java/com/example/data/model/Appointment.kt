package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "appointments")
data class Appointment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientId: Long,
    val patientName: String,
    val patientPhone: String,
    val dateTime: Long,
    val reason: String = "Examen de la vista", // Examen de la vista, Adaptación de LC, Ajuste, Garantía
    val status: String = "Pendiente", // Pendiente, Confirmada, Completada, Cancelada
    val notes: String = ""
)
