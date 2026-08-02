package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_accounts")
data class UserAccount(
    @PrimaryKey
    val uid: String,
    val email: String,
    val displayName: String,
    val role: String, // "Administrador", "Optometrista", "Caja", "Auxiliar"
    val phone: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
