package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.FinancialRecord
import com.example.data.model.Invoice
import com.example.data.model.Patient
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelCsvHelper {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "MX"))
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "MX"))

    fun exportPatientsToCsv(context: Context, patients: List<Patient>) {
        try {
            val file = File(context.cacheDir, "pacientes_optica_familiar.csv")
            val outputStream = FileOutputStream(file)

            // UTF-8 BOM for Excel compatibility with accents
            outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

            val header = "ID,Nombre Completo,Teléfono,Correo,Fecha de Nacimiento,Dirección,Fecha Registro,Notas\n"
            outputStream.write(header.toByteArray(Charsets.UTF_8))

            patients.forEach { p ->
                val line = "${p.id},\"${clean(p.fullName)}\",\"${clean(p.phone)}\",\"${clean(p.email)}\",\"${clean(p.dateOfBirth)}\",\"${clean(p.address)}\",\"${dateFormat.format(Date(p.registrationDate))}\",\"${clean(p.notes)}\"\n"
                outputStream.write(line.toByteArray(Charsets.UTF_8))
            }
            outputStream.close()

            shareFile(context, file, "text/csv", "Exportar Base de Datos de Pacientes (Excel)")
        } catch (e: Exception) {
            Toast.makeText(context, "Error al exportar pacientes: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun parsePatientsFromContent(content: String): List<Patient> {
        val result = mutableListOf<Patient>()
        val lines = content.lines()

        for ((index, line) in lines.withIndex()) {
            if (line.isBlank()) continue
            // Skip header line if present
            if (index == 0 && (line.contains("Nombre", ignoreCase = true) || line.contains("ID", ignoreCase = true))) {
                continue
            }

            val delimiter = if (line.contains(";")) ";" else ","
            val cols = line.split(delimiter).map { unquote(it.trim()) }

            if (cols.size >= 2) {
                // Determine column positions safely
                val fullName = if (cols.size > 1 && cols[0].toLongOrNull() != null) cols[1] else cols[0]
                val phone = if (cols.size > 2) cols[2] else ""
                val email = if (cols.size > 3) cols[3] else ""
                val dob = if (cols.size > 4) cols[4] else ""
                val address = if (cols.size > 5) cols[5] else ""
                val notes = if (cols.size > 7) cols[7] else if (cols.size > 6) cols[6] else "Importado de Excel"

                if (fullName.isNotBlank()) {
                    result.add(
                        Patient(
                            fullName = fullName,
                            phone = phone,
                            email = email,
                            dateOfBirth = dob.ifBlank { "1990-01-01" },
                            address = address,
                            notes = notes
                        )
                    )
                }
            }
        }
        return result
    }

    fun exportSalesReportToCsv(context: Context, title: String, invoices: List<Invoice>) {
        try {
            val fileName = "reporte_ventas_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)

            outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

            val header = "REPORTE DE VENTAS - ÓPTICA FAMILIAR AGS\n" +
                    "Título: $title\n" +
                    "Generado: ${dateFormat.format(Date())}\n\n" +
                    "Folio,Fecha,Cliente,Teléfono,Concepto,Subtotal,Descuento,Impuesto,Total,Abonado,Estado Pago,Metodo Pago,Atendido Por\n"
            outputStream.write(header.toByteArray(Charsets.UTF_8))

            var totalSales = 0.0
            var totalPaid = 0.0

            invoices.forEach { inv ->
                totalSales += inv.total
                totalPaid += inv.amountPaid
                val line = "\"${inv.invoiceNumber}\",\"${dateFormat.format(Date(inv.date))}\",\"${clean(inv.patientName)}\",\"${clean(inv.patientPhone)}\",\"${clean(inv.itemsSummary)}\",${inv.subtotal},${inv.discount},${inv.tax},${inv.total},${inv.amountPaid},\"${inv.paymentStatus}\",\"${inv.paymentMethod}\",\"${clean(inv.optometristName)}\"\n"
                outputStream.write(line.toByteArray(Charsets.UTF_8))
            }

            val summaryLine = "\nTOTALES,,,,, ,,,$totalSales,$totalPaid,,,\n"
            outputStream.write(summaryLine.toByteArray(Charsets.UTF_8))

            outputStream.close()

            shareFile(context, file, "text/csv", "Exportar Reporte de Ventas (Excel)")
        } catch (e: Exception) {
            Toast.makeText(context, "Error al generar reporte Excel: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun exportFinancialClosingToCsv(
        context: Context,
        title: String,
        totalIncome: Double,
        totalExpenses: Double,
        netProfit: Double,
        invoices: List<Invoice>,
        expenses: List<FinancialRecord>
    ) {
        try {
            val fileName = "cierre_financiero_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)

            outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

            val header = "CIERRE FINANCIERO Y REPORTES - ÓPTICA FAMILIAR AGS\n" +
                    "Período: $title\n" +
                    "Fecha Emisión: ${dateFormat.format(Date())}\n\n" +
                    "RESUMEN GENERAL\n" +
                    "Total Ingresos (Ventas),${currencyFormat.format(totalIncome)}\n" +
                    "Total Egresos (Gastos Generales),${currencyFormat.format(totalExpenses)}\n" +
                    "Utilidad / Balance Neto,${currencyFormat.format(netProfit)}\n\n" +
                    "DESGLOSE DE EGRESOS Y GASTOS GENERALES\n" +
                    "ID,Fecha,Concepto / Descripción,Categoría,Monto,Registrado Por,Notas\n"
            outputStream.write(header.toByteArray(Charsets.UTF_8))

            expenses.forEach { exp ->
                val line = "${exp.id},\"${dateFormat.format(Date(exp.date))}\",\"${clean(exp.title)}\",\"${exp.category}\",${exp.amount},\"${clean(exp.registeredBy)}\",\"${clean(exp.notes)}\"\n"
                outputStream.write(line.toByteArray(Charsets.UTF_8))
            }

            val salesHeader = "\n\nDESGLOSE DE VENTAS E INGRESOS\n" +
                    "Folio,Fecha,Cliente,Concepto,Total,Abonado,Estado Pago\n"
            outputStream.write(salesHeader.toByteArray(Charsets.UTF_8))

            invoices.forEach { inv ->
                val line = "\"${inv.invoiceNumber}\",\"${dateFormat.format(Date(inv.date))}\",\"${clean(inv.patientName)}\",\"${clean(inv.itemsSummary)}\",${inv.total},${inv.amountPaid},\"${inv.paymentStatus}\"\n"
                outputStream.write(line.toByteArray(Charsets.UTF_8))
            }

            outputStream.close()

            shareFile(context, file, "text/csv", "Exportar Cierre Financiero (Excel)")
        } catch (e: Exception) {
            Toast.makeText(context, "Error al generar cierre Excel: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }

    private fun clean(input: String): String {
        return input.replace("\"", "'").replace("\n", " ")
    }

    private fun unquote(input: String): String {
        return if (input.startsWith("\"") && input.endsWith("\"")) {
            input.substring(1, input.length - 1)
        } else input
    }
}
