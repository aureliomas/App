package com.example.data

import com.example.data.dao.OpticaDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class OpticaRepository(private val dao: OpticaDao) {

    val patients: Flow<List<Patient>> = dao.getAllPatients()
    val appointments: Flow<List<Appointment>> = dao.getAllAppointments()
    val inventory: Flow<List<InventoryItem>> = dao.getAllInventoryItems()
    val invoices: Flow<List<Invoice>> = dao.getAllInvoices()
    val clinicalRecords: Flow<List<ClinicalRecord>> = dao.getAllClinicalRecords()
    val cashCuts: Flow<List<CashCut>> = dao.getAllCashCuts()
    val userAccounts: Flow<List<UserAccount>> = dao.getAllUserAccounts()
    val financialRecords: Flow<List<FinancialRecord>> = dao.getAllFinancialRecords()

    fun getClinicalRecordsForPatient(patientId: Long): Flow<List<ClinicalRecord>> {
        return dao.getRecordsForPatient(patientId)
    }

    suspend fun getUserAccountByEmail(email: String): UserAccount? = dao.getUserAccountByEmail(email)
    suspend fun getUserAccountByUid(uid: String): UserAccount? = dao.getUserAccountByUid(uid)
    suspend fun insertUserAccount(user: UserAccount) = dao.insertUserAccount(user)
    suspend fun updateUserAccount(user: UserAccount) = dao.updateUserAccount(user)
    suspend fun deleteUserAccount(user: UserAccount) = dao.deleteUserAccount(user)

    suspend fun insertPatient(patient: Patient): Long = dao.insertPatient(patient)
    suspend fun insertPatientsBatch(patients: List<Patient>): List<Long> = dao.insertPatientsBatch(patients)
    suspend fun updatePatient(patient: Patient) = dao.updatePatient(patient)
    suspend fun deletePatient(patient: Patient) = dao.deletePatient(patient)

    suspend fun insertFinancialRecord(record: FinancialRecord): Long = dao.insertFinancialRecord(record)
    suspend fun updateFinancialRecord(record: FinancialRecord) = dao.updateFinancialRecord(record)
    suspend fun deleteFinancialRecord(record: FinancialRecord) = dao.deleteFinancialRecord(record)

    suspend fun insertClinicalRecord(record: ClinicalRecord): Long = dao.insertClinicalRecord(record)

    suspend fun insertAppointment(appointment: Appointment): Long = dao.insertAppointment(appointment)
    suspend fun updateAppointment(appointment: Appointment) = dao.updateAppointment(appointment)
    suspend fun deleteAppointment(appointment: Appointment) = dao.deleteAppointment(appointment)

    suspend fun insertInventoryItem(item: InventoryItem): Long = dao.insertInventoryItem(item)
    suspend fun updateInventoryItem(item: InventoryItem) = dao.updateInventoryItem(item)
    suspend fun deleteInventoryItem(item: InventoryItem) = dao.deleteInventoryItem(item)

    suspend fun insertInvoice(invoice: Invoice): Long = dao.insertInvoice(invoice)
    suspend fun updateInvoice(invoice: Invoice) = dao.updateInvoice(invoice)

    suspend fun insertCashCut(cashCut: CashCut): Long = dao.insertCashCut(cashCut)

    suspend fun seedInitialDataIfEmpty() {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        // 1. Seed Patients
        val p1Id = dao.insertPatient(
            Patient(
                fullName = "María Fernanda Gómez",
                phone = "4495543087",
                email = "maria.gomez@gmail.com",
                dateOfBirth = "1992-08-05",
                address = "Av Adolfo López Mateos #213A, Col Centro, Ags",
                notes = "Sensible a la luz. Prefiere armazones ligeros de titanio."
            )
        )

        val p2Id = dao.insertPatient(
            Patient(
                fullName = "Carlos Alberto Mendoza",
                phone = "4491234567",
                email = "carlos.mendoza@hotmail.com",
                dateOfBirth = "1985-11-14",
                address = "Calle Madero #102, Col Centro, Ags",
                notes = "Usuario de lente progresivo. Trabaja en computadora +8 hrs."
            )
        )

        val p3Id = dao.insertPatient(
            Patient(
                fullName = "Sofia Elena Ramírez",
                phone = "4499876543",
                email = "sofia.ramirez@yahoo.com",
                dateOfBirth = "1998-08-12",
                address = "Calle Zaragoza #405, Ags",
                notes = "Usa lentes de contacto miópicos y armazón de descanso."
            )
        )

        // 2. Seed Clinical Records
        val oneYearAgo = now - (365L * 24 * 60 * 60 * 1000)
        dao.insertClinicalRecord(
            ClinicalRecord(
                patientId = p1Id,
                date = oneYearAgo,
                odSphere = "-2.25",
                odCylinder = "-0.75",
                odAxis = "175°",
                odAddition = "+0.00",
                oiSphere = "-2.00",
                oiCylinder = "-0.50",
                oiAxis = "180°",
                oiAddition = "+0.00",
                pupillaryDistance = "63 mm",
                lensType = "Mica Antirreflejante Crizal",
                diagnosis = "Miopía moderada y astigmatismo leve",
                optometristName = "L.O. BRISAIDA GPE GUILLEN ORTIZ",
                nextExamDate = now - (10L * 24 * 60 * 60 * 1000)
            )
        )

        dao.insertClinicalRecord(
            ClinicalRecord(
                patientId = p2Id,
                date = now - (180L * 24 * 60 * 60 * 1000),
                odSphere = "+1.50",
                odCylinder = "-1.00",
                odAxis = "90°",
                odAddition = "+2.00",
                oiSphere = "+1.75",
                oiCylinder = "-1.25",
                oiAxis = "85°",
                oiAddition = "+2.00",
                pupillaryDistance = "65 mm",
                lensType = "Progresivo Digital Blue Light",
                diagnosis = "Presbicia y Hipermetropía con Astigmatismo",
                optometristName = "L.O. BRISAIDA GPE GUILLEN ORTIZ",
                nextExamDate = now + (185L * 24 * 60 * 60 * 1000)
            )
        )

        // 3. Seed Appointments
        val today10am = cal.apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
        }.timeInMillis

        val today3pm = cal.apply {
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 30)
        }.timeInMillis

        dao.insertAppointment(
            Appointment(
                patientId = p1Id,
                patientName = "María Fernanda Gómez",
                patientPhone = "4495543087",
                dateTime = today10am,
                reason = "Examen de la vista anual",
                status = "Confirmada",
                notes = "Revisión por fatiga visual."
            )
        )

        dao.insertAppointment(
            Appointment(
                patientId = p2Id,
                patientName = "Carlos Alberto Mendoza",
                patientPhone = "4491234567",
                dateTime = today3pm,
                reason = "Ajuste de armazón y micas progresivas",
                status = "Pendiente",
                notes = "Requiere ajustar plaquetas nasales."
            )
        )

        // 4. Seed Inventory
        dao.insertInventoryItem(
            InventoryItem(
                sku = "ARM-RB-5154",
                name = "Armazón Clubmaster Classic Titanium",
                brand = "Ray-Ban",
                category = "Armazones",
                stockQuantity = 8,
                minStockThreshold = 3,
                costPrice = 1200.0,
                salePrice = 2850.0,
                notes = "Estilo clásico unisex en color negro/dorado."
            )
        )

        dao.insertInventoryItem(
            InventoryItem(
                sku = "ARM-OAK-8051",
                name = "Armazón Deportivo Holbrook RX",
                brand = "Oakley",
                category = "Armazones",
                stockQuantity = 2,
                minStockThreshold = 4,
                costPrice = 1400.0,
                salePrice = 3100.0,
                notes = "Ultra ligero O-Matter."
            )
        )

        dao.insertInventoryItem(
            InventoryItem(
                sku = "MIC-CRZ-160",
                name = "Mica Policarbonato Anti-Reflejante Crizal",
                brand = "Essilor",
                category = "Micas",
                stockQuantity = 15,
                minStockThreshold = 5,
                costPrice = 450.0,
                salePrice = 1250.0,
                notes = "Filtro UV400 y antireflejo."
            )
        )

        // 5. Seed Invoices / Sales
        val sub1 = 3275.86
        val tax1 = 524.14
        val total1 = 3800.0

        dao.insertInvoice(
            Invoice(
                invoiceNumber = "FAC-2026-001",
                patientId = p1Id,
                patientName = "María Fernanda Gómez",
                patientPhone = "4495543087",
                patientEmail = "maria.gomez@gmail.com",
                date = now - (2 * 60 * 60 * 1000), // Today
                itemsSummary = "Armazón Ray-Ban Clubmaster + Mica Policarbonato Crizal",
                subtotal = sub1,
                discount = 0.0,
                tax = tax1,
                total = total1,
                paymentStatus = "Pagado",
                amountPaid = total1,
                paymentMethod = "Tarjeta",
                isCreditSale = false,
                optometristName = "L.O. BRISAIDA GPE GUILLEN ORTIZ",
                notes = "Venta pagada con tarjeta de débito."
            )
        )

        val sub2 = 4568.97
        val tax2 = 731.03
        val total2 = 5300.0

        dao.insertInvoice(
            Invoice(
                invoiceNumber = "FAC-2026-002",
                patientId = p2Id,
                patientName = "Carlos Alberto Mendoza",
                patientPhone = "4491234567",
                patientEmail = "carlos.mendoza@hotmail.com",
                date = now - (1 * 60 * 60 * 1000), // Today
                itemsSummary = "Armazón Oakley Holbrook + Mica Progresiva Digital Blue",
                subtotal = sub2,
                discount = 0.0,
                tax = tax2,
                total = total2,
                paymentStatus = "Abono Parcial",
                amountPaid = 3000.0,
                paymentMethod = "Efectivo",
                isCreditSale = true,
                optometristName = "L.O. BRISAIDA GPE GUILLEN ORTIZ",
                notes = "Venta a crédito. Anticipo de $3,000 en efectivo."
            )
        )

        // 6. Seed Default Staff Accounts
        dao.insertUserAccount(
            UserAccount(
                uid = "admin_default_uid",
                email = "admin@optica.com",
                displayName = "L.O. BRISAIDA GPE GUILLEN ORTIZ",
                role = "Administrador",
                phone = "4495543087"
            )
        )

        dao.insertUserAccount(
            UserAccount(
                uid = "opto_default_uid",
                email = "optometrista@optica.com",
                displayName = "Dr. Carlos Mendoza",
                role = "Optometrista",
                phone = "4491234567"
            )
        )

        dao.insertUserAccount(
            UserAccount(
                uid = "caja_default_uid",
                email = "caja@optica.com",
                displayName = "Ana Gómez",
                role = "Caja",
                phone = "4499876543"
            )
        )

        dao.insertUserAccount(
            UserAccount(
                uid = "aux_default_uid",
                email = "auxiliar@optica.com",
                displayName = "Pedro Ruiz",
                role = "Auxiliar",
                phone = "4493332211"
            )
        )

        // 7. Seed Sample Financial Records / Egresos
        dao.insertFinancialRecord(
            FinancialRecord(
                title = "Pago Renta Local Comercial Centro",
                amount = 7500.0,
                category = "Renta",
                isIncome = false,
                date = now - (5 * 24 * 60 * 60 * 1000),
                registeredBy = "Administrador",
                notes = "Renta correspondiente al mes en curso."
            )
        )

        dao.insertFinancialRecord(
            FinancialRecord(
                title = "Servicio de Luz CFE",
                amount = 1250.0,
                category = "Luz",
                isIncome = false,
                date = now - (4 * 24 * 60 * 60 * 1000),
                registeredBy = "Administrador",
                notes = "Factura CFE bimestral."
            )
        )

        dao.insertFinancialRecord(
            FinancialRecord(
                title = "Servicio de Internet y Teléfono Telmex",
                amount = 680.0,
                category = "Internet",
                isIncome = false,
                date = now - (3 * 24 * 60 * 60 * 1000),
                registeredBy = "Administrador",
                notes = "Paquete empresarial 200 Megas."
            )
        )

        dao.insertFinancialRecord(
            FinancialRecord(
                title = "Pago a Laboratorio Oftálmico Essilor",
                amount = 4200.0,
                category = "Pago a Laboratorio",
                isIncome = false,
                date = now - (2 * 24 * 60 * 60 * 1000),
                registeredBy = "L.O. BRISAIDA GPE GUILLEN ORTIZ",
                notes = "Biselado de micas progresivas y tratamiento Crizal."
            )
        )

        dao.insertFinancialRecord(
            FinancialRecord(
                title = "Compra de Mercancía - Lote Armazones Ray-Ban & Oakley",
                amount = 8900.0,
                category = "Compra de Mercancía",
                isIncome = false,
                date = now - (1 * 24 * 60 * 60 * 1000),
                registeredBy = "Administrador",
                notes = "Surtido de armazones para la temporada."
            )
        )

        dao.insertFinancialRecord(
            FinancialRecord(
                title = "Sueldos y Nomina Quincenal Personal",
                amount = 12000.0,
                category = "Sueldos",
                isIncome = false,
                date = now - (6 * 24 * 60 * 60 * 1000),
                registeredBy = "Administrador",
                notes = "Pago de sueldo al personal de óptica."
            )
        )
    }
}
