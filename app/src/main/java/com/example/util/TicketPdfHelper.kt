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
import com.example.data.model.Invoice
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object TicketPdfHelper {

    fun generateTicketPdf(context: Context, invoice: Invoice): File? {
        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 384 // 80mm thermal receipt width in points @ 120dpi
            val pageHeight = 700
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paint = Paint()
            paint.color = Color.BLACK
            paint.isAntiAlias = true

            val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
            val dateFormat = SimpleDateFormat("dd/MM/yyyy - hh:mm a", Locale("es", "MX"))

            var yPos = 25f

            // --- HEADER LOGO & TITLE (CENTERED) ---
            try {
                val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.img_optica_logo)
                if (logoBitmap != null) {
                    val targetSize = 60
                    val scaledLogo = Bitmap.createScaledBitmap(logoBitmap, targetSize, targetSize, true)
                    canvas.drawBitmap(scaledLogo, ((pageWidth - targetSize) / 2).toFloat(), yPos, paint)
                    yPos += targetSize + 10f
                }
            } catch (_: Exception) {}

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 16f
            paint.textAlign = Paint.Align.CENTER

            canvas.drawText("ÓPTICA FAMILIAR AGS", (pageWidth / 2).toFloat(), yPos, paint)
            yPos += 20f

            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Av Adolfo López Mateos #213A col Centro.", (pageWidth / 2).toFloat(), yPos, paint)
            yPos += 14f
            canvas.drawText("AGUASCALIENTES AGS MÉXICO", (pageWidth / 2).toFloat(), yPos, paint)
            yPos += 14f
            canvas.drawText("Tel / WhatsApp: 449 554 30 87", (pageWidth / 2).toFloat(), yPos, paint)
            yPos += 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Atendió: L.O. BRISAIDA GPE GUILLEN ORTIZ", (pageWidth / 2).toFloat(), yPos, paint)
            yPos += 20f

            // Separator line
            paint.strokeWidth = 1f
            canvas.drawLine(15f, yPos, (pageWidth - 15).toFloat(), yPos, paint)
            yPos += 18f

            // --- TICKET META ---
            paint.textAlign = Paint.Align.LEFT
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("FOLIO: ${invoice.invoiceNumber}", 15f, yPos, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Fecha: ${dateFormat.format(Date(invoice.date))}", 15f, yPos + 14f, paint)
            yPos += 32f

            // --- CLIENT DETAILS ---
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("DATOS DEL CLIENTE:", 15f, yPos, paint)
            yPos += 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Nombre: ${invoice.patientName}", 15f, yPos, paint)
            yPos += 14f
            if (invoice.patientPhone.isNotBlank()) {
                canvas.drawText("Teléfono: ${invoice.patientPhone}", 15f, yPos, paint)
                yPos += 14f
            }
            if (invoice.patientEmail.isNotBlank()) {
                canvas.drawText("Correo: ${invoice.patientEmail}", 15f, yPos, paint)
                yPos += 14f
            }

            yPos += 6f
            canvas.drawLine(15f, yPos, (pageWidth - 15).toFloat(), yPos, paint)
            yPos += 18f

            // --- DESCRIPCIÓN DE COMPRA ---
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("DESCRIPCIÓN DE COMPRA:", 15f, yPos, paint)
            yPos += 16f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            // Wrap item text if long
            val itemsText = invoice.itemsSummary
            val maxTextWidth = pageWidth - 30
            val words = itemsText.split(" ")
            var currentLine = ""
            for (word in words) {
                if (paint.measureText("$currentLine $word") < maxTextWidth) {
                    currentLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                } else {
                    canvas.drawText(currentLine, 15f, yPos, paint)
                    yPos += 14f
                    currentLine = word
                }
            }
            if (currentLine.isNotEmpty()) {
                canvas.drawText(currentLine, 15f, yPos, paint)
                yPos += 18f
            }

            canvas.drawLine(15f, yPos, (pageWidth - 15).toFloat(), yPos, paint)
            yPos += 18f

            // --- IMPORTE E IMPORTE CON IVA ---
            val subtotalCalculated = if (invoice.subtotal > 0) invoice.subtotal else (invoice.total / 1.16)
            val taxCalculated = if (invoice.tax > 0) invoice.tax else (invoice.total - subtotalCalculated)

            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("Subtotal (Sin IVA):", 15f, yPos, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(numberFormat.format(subtotalCalculated), (pageWidth - 15).toFloat(), yPos, paint)
            yPos += 16f

            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("IVA (16%):", 15f, yPos, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(numberFormat.format(taxCalculated), (pageWidth - 15).toFloat(), yPos, paint)
            yPos += 16f

            if (invoice.discount > 0) {
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText("Descuento:", 15f, yPos, paint)
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText("- ${numberFormat.format(invoice.discount)}", (pageWidth - 15).toFloat(), yPos, paint)
                yPos += 16f
            }

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 12f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("TOTAL (Con IVA):", 15f, yPos, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(numberFormat.format(invoice.total), (pageWidth - 15).toFloat(), yPos, paint)
            yPos += 20f

            paint.textSize = 10f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("Importe Abonado:", 15f, yPos, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(numberFormat.format(invoice.amountPaid), (pageWidth - 15).toFloat(), yPos, paint)
            yPos += 16f

            val pendingBalance = (invoice.total - invoice.amountPaid).coerceAtLeast(0.0)
            if (pendingBalance > 0) {
                paint.color = Color.RED
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText("Saldo Pendiente (Crédito):", 15f, yPos, paint)
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText(numberFormat.format(pendingBalance), (pageWidth - 15).toFloat(), yPos, paint)
                yPos += 16f
                paint.color = Color.BLACK
            }

            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("Forma de Pago: ${invoice.paymentMethod}", 15f, yPos, paint)
            yPos += 16f
            canvas.drawText("Estado: ${invoice.paymentStatus.uppercase()}", 15f, yPos, paint)
            yPos += 22f

            canvas.drawLine(15f, yPos, (pageWidth - 15).toFloat(), yPos, paint)
            yPos += 22f

            // --- FOOTER (CENTERED) ---
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("GRACIAS POR SU PREFERENCIA", (pageWidth / 2).toFloat(), yPos, paint)
            yPos += 16f
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("SÍGUENOS EN NUESTRAS REDES SOCIALES", (pageWidth / 2).toFloat(), yPos, paint)
            yPos += 14f
            canvas.drawText("Óptica Familiar Ags • Cuidando tu salud visual", (pageWidth / 2).toFloat(), yPos, paint)

            pdfDocument.finishPage(page)

            val file = File(context.cacheDir, "Ticket_${invoice.invoiceNumber}.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()

            return file
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error generando PDF: ${e.message}", Toast.LENGTH_LONG).show()
            return null
        }
    }

    fun shareTicketPdf(context: Context, invoice: Invoice) {
        val file = generateTicketPdf(context, invoice) ?: return
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Ticket de Compra - ${invoice.invoiceNumber} - Óptica Familiar Ags")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Hola ${invoice.patientName}, adjuntamos tu ticket digital de compra en Óptica Familiar Ags.\n\n" +
                            "Folio: ${invoice.invoiceNumber}\nTotal: $${invoice.total}\n\n¡Gracias por su preferencia!"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Enviar Ticket PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error al compartir PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun buildFormattedTicketText(invoice: Invoice): String {
        val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
        val dateFormat = SimpleDateFormat("dd/MM/yyyy - hh:mm a", Locale("es", "MX"))
        val subtotalCalculated = if (invoice.subtotal > 0) invoice.subtotal else (invoice.total / 1.16)
        val taxCalculated = if (invoice.tax > 0) invoice.tax else (invoice.total - subtotalCalculated)
        val pending = (invoice.total - invoice.amountPaid).coerceAtLeast(0.0)

        return """
            🕶️ *ÓPTICA FAMILIAR AGS* 🕶️
            📍 Av Adolfo López Mateos #213A col Centro. AGUASCALIENTES AGS MÉXICO.
            📞 WhatsApp: 449 554 30 87
            👩‍⚕️ Atendió: L.O. BRISAIDA GPE GUILLEN ORTIZ.
            ------------------------------------------
            📄 *TICKET DE VENTA: ${invoice.invoiceNumber}*
            📅 Fecha: ${dateFormat.format(Date(invoice.date))}
            👤 Cliente: *${invoice.patientName}*
            📱 Tel: ${invoice.patientPhone.ifBlank { "N/A" }}
            ------------------------------------------
            🛍️ *DESCRIPCIÓN DE COMPRA:*
            ${invoice.itemsSummary}
            ------------------------------------------
            Subtotal (Sin IVA): ${numberFormat.format(subtotalCalculated)}
            IVA (16%): ${numberFormat.format(taxCalculated)}
            ${if (invoice.discount > 0) "Descuento: -${numberFormat.format(invoice.discount)}\n" else ""}
            *TOTAL (Con IVA): ${numberFormat.format(invoice.total)}*
            
            Abonado / Pagado: ${numberFormat.format(invoice.amountPaid)}
            ${if (pending > 0) "⚠️ *Saldo Pendiente (Crédito): ${numberFormat.format(pending)}*\n" else ""}
            Método de Pago: ${invoice.paymentMethod}
            Estado: *${invoice.paymentStatus.uppercase()}*
            ------------------------------------------
            ✨ *GRACIAS POR SU PREFERENCIA* ✨
            📲 *SÍGUENOS EN NUESTRAS REDES SOCIALES*
        """.trimIndent()
    }
}
