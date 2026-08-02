package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patients")
data class Patient(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fullName: String,
    val phone: String,
    val email: String,
    val dateOfBirth: String, // Format YYYY-MM-DD
    val address: String = "",
    val registrationDate: Long = System.currentTimeMillis(),
    val notes: String = ""
)
