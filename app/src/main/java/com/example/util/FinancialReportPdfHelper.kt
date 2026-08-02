package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.FinancialRecord
import com.example.data.model.Invoice
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FinancialReportPdfHelper {

    private val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy - hh:mm a", Locale("es", "MX"))
    private val dateOnlyFormat = SimpleDateFormat("dd/MM/yyyy", Locale("es", "MX"))

    // Letter size dimensions in points at 72dpi: 8.5" x 11" = 612 x 792 pt
    private const val PAGE_WIDTH = 612
    private const val PAGE_HEIGHT = 792

    fun generateFinancialReportPdf(
        context: Context,
        reportTitle: String,
        periodSubtitle: String,
        invoices: List<Invoice>,
        expenses: List<FinancialRecord>,
        generatedBy: String
    ) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint().apply { isAntiAlias = true }

            var yPos = 36f
            val marginLeft = 36f
            val marginRight = PAGE_WIDTH - 36f
            val contentWidth = marginRight - marginLeft

            // --- HEADER WITH LOGO ---
            try {
                val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.img_optica_logo)
                if (logoBitmap != null) {
                    val logoSize = 50
                    val scaledLogo = Bitmap.createScaledBitmap(logoBitmap, logoSize, logoSize, true)
                    canvas.drawBitmap(scaledLogo, marginLeft, yPos, paint)
                }
            } catch (_: Exception) {}

            // Header Title
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 18f
            paint.color = Color.parseColor("#0F172A")
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("ÓPTICA FAMILIAR AGS", marginLeft + 60f, yPos + 18f, paint)

            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.parseColor("#475569")
            canvas.drawText("Av. Adolfo López Mateos Pte 213, Centro, Ags • Tel: 4495543087", marginLeft + 60f, yPos + 34f, paint)

            paint.textAlign = Paint.Align.RIGHT
            paint.textSize = 9f
            canvas.drawText("Fecha: ${dateFormat.format(Date())}", marginRight, yPos + 18f, paint)
            canvas.drawText("Generado por: $generatedBy", marginRight, yPos + 32f, paint)

            yPos += 60f

            // --- DIVIDER LINE ---
            paint.color = Color.parseColor("#0D9488")
            paint.strokeWidth = 2f
            canvas.drawLine(marginLeft, yPos, marginRight, yPos, paint)
            paint.strokeWidth = 1f

            yPos += 20f

            // --- REPORT TITLE BANNER ---
            paint.textAlign = Paint.Align.LEFT
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 16f
            paint.color = Color.parseColor("#0D9488")
            canvas.drawText(reportTitle.uppercase(), marginLeft, yPos, paint)

            yPos += 14f
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.parseColor("#64748B")
            canvas.drawText("Período: $periodSubtitle", marginLeft, yPos, paint)

            yPos += 24f

            // --- FINANCIAL CALCULATIONS ---
            val totalIncomesFromSales = invoices.sumOf { it.amountPaid }
            val extraIncomes = expenses.filter { it.isIncome }.sumOf { it.amount }
            val totalIncome = totalIncomesFromSales + extraIncomes
            val totalExpenses = expenses.filter { !it.isIncome }.sumOf { it.amount }
            val netBalance = totalIncome - totalExpenses

            // --- FINANCIAL SUMMARY CARDS BOX ---
            val cardHeight = 55f
            paint.color = Color.parseColor("#F8FAFC")
            canvas.drawRoundRect(marginLeft, yPos, marginRight, yPos + cardHeight, 8f, 8f, paint)

            paint.color = Color.parseColor("#CBD5E1")
            paint.style = Paint.Style.STROKE
            canvas.drawRoundRect(marginLeft, yPos, marginRight, yPos + cardHeight, 8f, 8f, paint)
            paint.style = Paint.Style.FILL

            val col1X = marginLeft + 20f
            val col2X = marginLeft + (contentWidth / 3) + 10f
            val col3X = marginLeft + (2 * contentWidth / 3) + 10f

            // Income
            paint.textSize = 9f
            paint.color = Color.parseColor("#475569")
            canvas.drawText("TOTAL INGRESOS", col1X, yPos + 20f, paint)
            paint.textSize = 13f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = Color.parseColor("#15803D")
            canvas.drawText(numberFormat.format(totalIncome), col1X, yPos + 40f, paint)

            // Expenses
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.parseColor("#475569")
            canvas.drawText("TOTAL EGRESOS / GASTOS", col2X, yPos + 20f, paint)
            paint.textSize = 13f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = Color.parseColor("#B91C1C")
            canvas.drawText(numberFormat.format(totalExpenses), col2X, yPos + 40f, paint)

            // Net Profit
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.parseColor("#475569")
            canvas.drawText("BALANCE / UTILIDAD NETA", col3X, yPos + 20f, paint)
            paint.textSize = 13f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = if (netBalance >= 0) Color.parseColor("#0369A1") else Color.parseColor("#B91C1C")
            canvas.drawText(numberFormat.format(netBalance), col3X, yPos + 40f, paint)

            yPos += cardHeight + 25f

            // --- CATEGORY BREAKDOWN TABLE (EGRESOS) ---
            if (expenses.isNotEmpty()) {
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 11f
                paint.color = Color.parseColor("#0F172A")
                canvas.drawText("DESGLOSE DE GASTOS Y EGRESOS POR CATEGORÍA", marginLeft, yPos, paint)

                yPos += 14f

                val groupedExpenses = expenses.filter { !it.isIncome }.groupBy { it.category }
                if (groupedExpenses.isNotEmpty()) {
                    paint.textSize = 9f
                    paint.color = Color.parseColor("#0D9488")
                    // Table Header Row
                    canvas.drawRect(marginLeft, yPos, marginRight, yPos + 18f, paint)

                    paint.color = Color.WHITE
                    canvas.drawText("Categoría de Gasto", marginLeft + 10f, yPos + 13f, paint)
                    paint.textAlign = Paint.Align.RIGHT
                    canvas.drawText("Cantidad Registros", marginLeft + 320f, yPos + 13f, paint)
                    canvas.drawText("Monto Total", marginRight - 10f, yPos + 13f, paint)

                    yPos += 18f
                    paint.textAlign = Paint.Align.LEFT
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                    for ((category, list) in groupedExpenses) {
                        val catTotal = list.sumOf { it.amount }
                        paint.color = Color.parseColor("#F1F5F9")
                        canvas.drawRect(marginLeft, yPos, marginRight, yPos + 16f, paint)

                        paint.color = Color.parseColor("#1E293B")
                        canvas.drawText(category, marginLeft + 10f, yPos + 12f, paint)
                        paint.textAlign = Paint.Align.RIGHT
                        canvas.drawText("${list.size}", marginLeft + 320f, yPos + 12f, paint)
                        canvas.drawText(numberFormat.format(catTotal), marginRight - 10f, yPos + 12f, paint)

                        paint.textAlign = Paint.Align.LEFT
                        yPos += 18f
                        if (yPos > PAGE_HEIGHT - 80f) break
                    }
                    yPos += 15f
                }
            }

            // --- ITEMIZED TRANSACTIONS TABLE ---
            if (yPos < PAGE_HEIGHT - 120f) {
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 11f
                paint.color = Color.parseColor("#0F172A")
                canvas.drawText("DETALLE DE TRANSACCIONES / REGISTROS RECIENTES", marginLeft, yPos, paint)

                yPos += 14f

                // Table Header
                paint.textSize = 9f
                paint.color = Color.parseColor("#1E293B")
                canvas.drawRect(marginLeft, yPos, marginRight, yPos + 18f, paint)

                paint.color = Color.WHITE
                canvas.drawText("Fecha", marginLeft + 8f, yPos + 13f, paint)
                canvas.drawText("Concepto", marginLeft + 70f, yPos + 13f, paint)
                canvas.drawText("Categoría / Tipo", marginLeft + 280f, yPos + 13f, paint)
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText("Monto", marginRight - 8f, yPos + 13f, paint)

                yPos += 18f
                paint.textAlign = Paint.Align.LEFT
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                // Render invoices/sales first
                for (inv in invoices.take(8)) {
                    if (yPos > PAGE_HEIGHT - 60f) break
                    paint.color = Color.parseColor("#FFFFFF")
                    canvas.drawRect(marginLeft, yPos, marginRight, yPos + 16f, paint)

                    paint.color = Color.parseColor("#334155")
                    canvas.drawText(dateOnlyFormat.format(Date(inv.date)), marginLeft + 8f, yPos + 12f, paint)

                    val shortSummary = if (inv.itemsSummary.length > 32) inv.itemsSummary.take(32) + "..." else inv.itemsSummary
                    canvas.drawText("Venta ${inv.invoiceNumber} - $shortSummary", marginLeft + 70f, yPos + 12f, paint)
                    canvas.drawText("Ingreso Venta", marginLeft + 280f, yPos + 12f, paint)

                    paint.textAlign = Paint.Align.RIGHT
                    paint.color = Color.parseColor("#15803D")
                    canvas.drawText("+ ${numberFormat.format(inv.amountPaid)}", marginRight - 8f, yPos + 12f, paint)

                    paint.textAlign = Paint.Align.LEFT
                    yPos += 17f
                }

                // Render expenses
                for (exp in expenses.take(12)) {
                    if (yPos > PAGE_HEIGHT - 60f) break
                    paint.color = Color.parseColor("#F8FAFC")
                    canvas.drawRect(marginLeft, yPos, marginRight, yPos + 16f, paint)

                    paint.color = Color.parseColor("#334155")
                    canvas.drawText(dateOnlyFormat.format(Date(exp.date)), marginLeft + 8f, yPos + 12f, paint)

                    val shortTitle = if (exp.title.length > 32) exp.title.take(32) + "..." else exp.title
                    canvas.drawText(shortTitle, marginLeft + 70f, yPos + 12f, paint)
                    canvas.drawText(exp.category, marginLeft + 280f, yPos + 12f, paint)

                    paint.textAlign = Paint.Align.RIGHT
                    paint.color = if (exp.isIncome) Color.parseColor("#15803D") else Color.parseColor("#B91C1C")
                    val sign = if (exp.isIncome) "+ " else "- "
                    canvas.drawText("$sign${numberFormat.format(exp.amount)}", marginRight - 8f, yPos + 12f, paint)

                    paint.textAlign = Paint.Align.LEFT
                    yPos += 17f
                }
            }

            // --- FOOTER (TAMAÑO CARTA) ---
            val footerY = PAGE_HEIGHT - 30f
            paint.color = Color.parseColor("#94A3B8")
            paint.textSize = 8f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("ÓPTICA FAMILIAR AGS • Reporte Oficial Formato Carta • Página 1 de 1", (PAGE_WIDTH / 2).toFloat(), footerY, paint)

            pdfDocument.finishPage(page)

            // Save PDF File
            val fileName = "reporte_financiero_${System.currentTimeMillis()}.pdf"
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()

            sharePdf(context, file, "Reporte Financiero PDF (Tamaño Carta)")
        } catch (e: Exception) {
            Toast.makeText(context, "Error al generar PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun sharePdf(context: Context, file: File, title: String) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }
}
