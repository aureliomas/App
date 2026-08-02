package com.example.data.dao

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OpticaDao {
    // --- PATIENTS ---
    @Query("SELECT * FROM patients ORDER BY fullName ASC")
    fun getAllPatients(): Flow<List<Patient>>

    @Query("SELECT * FROM patients WHERE id = :patientId LIMIT 1")
    suspend fun getPatientById(patientId: Long): Patient?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: Patient): Long

    @Update
    suspend fun updatePatient(patient: Patient)

    @Delete
    suspend fun deletePatient(patient: Patient)

    // --- CLINICAL RECORDS ---
    @Query("SELECT * FROM clinical_records WHERE patientId = :patientId ORDER BY date DESC")
    fun getRecordsForPatient(patientId: Long): Flow<List<ClinicalRecord>>

    @Query("SELECT * FROM clinical_records ORDER BY date DESC")
    fun getAllClinicalRecords(): Flow<List<ClinicalRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClinicalRecord(record: ClinicalRecord): Long

    // --- APPOINTMENTS ---
    @Query("SELECT * FROM appointments ORDER BY dateTime ASC")
    fun getAllAppointments(): Flow<List<Appointment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: Appointment): Long

    @Update
    suspend fun updateAppointment(appointment: Appointment)

    @Delete
    suspend fun deleteAppointment(appointment: Appointment)

    // --- INVENTORY ---
    @Query("SELECT * FROM inventory_items ORDER BY name ASC")
    fun getAllInventoryItems(): Flow<List<InventoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItem(item: InventoryItem): Long

    @Update
    suspend fun updateInventoryItem(item: InventoryItem)

    @Delete
    suspend fun deleteInventoryItem(item: InventoryItem)

    // --- INVOICES ---
    @Query("SELECT * FROM invoices ORDER BY date DESC")
    fun getAllInvoices(): Flow<List<Invoice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: Invoice): Long

    @Update
    suspend fun updateInvoice(invoice: Invoice)

    // --- CASH CUTS ---
    @Query("SELECT * FROM cash_cuts ORDER BY date DESC")
    fun getAllCashCuts(): Flow<List<CashCut>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashCut(cashCut: CashCut): Long

    // --- USER ACCOUNTS ---
    @Query("SELECT * FROM user_accounts ORDER BY displayName ASC")
    fun getAllUserAccounts(): Flow<List<UserAccount>>

    @Query("SELECT * FROM user_accounts WHERE uid = :uid LIMIT 1")
    suspend fun getUserAccountByUid(uid: String): UserAccount?

    @Query("SELECT * FROM user_accounts WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserAccountByEmail(email: String): UserAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserAccount(user: UserAccount)

    @Update
    suspend fun updateUserAccount(user: UserAccount)

    @Delete
    suspend fun deleteUserAccount(user: UserAccount)

    // --- FINANCIAL RECORDS / GASTOS GENERALES ---
    @Query("SELECT * FROM financial_records ORDER BY date DESC")
    fun getAllFinancialRecords(): Flow<List<FinancialRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinancialRecord(record: FinancialRecord): Long

    @Update
    suspend fun updateFinancialRecord(record: FinancialRecord)

    @Delete
    suspend fun deleteFinancialRecord(record: FinancialRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatientsBatch(patients: List<Patient>): List<Long>
}
