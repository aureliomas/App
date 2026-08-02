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
import com.example.data.model.ClinicalRecord
import com.example.data.model.Patient
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ClinicalRecordPdfHelper {

    private val dateFormat = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "MX"))
    private val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale("es", "MX"))

    // Standard Letter dimensions at 72 dpi: 612 x 792 points
    private const val PAGE_WIDTH = 612
    private const val PAGE_HEIGHT = 792

    fun generateClinicalRecordPdf(
        context: Context,
        patient: Patient,
        record: ClinicalRecord
    ) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas
            val paint = Paint().apply { isAntiAlias = true }

            var yPos = 36f
            val marginLeft = 36f
            val marginRight = PAGE_WIDTH - 36f
            val contentWidth = marginRight - marginLeft

            // --- 1. ENCABEZADO CON LOGO Y DATOS DE LA ÓPTICA ---
            try {
                val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.img_optica_logo)
                if (logoBitmap != null) {
                    val logoSize = 54
                    val scaledLogo = Bitmap.createScaledBitmap(logoBitmap, logoSize, logoSize, true)
                    canvas.drawBitmap(scaledLogo, marginLeft, yPos, paint)
                }
            } catch (_: Exception) {}

            // Header Text
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 18f
            paint.color = Color.parseColor("#0F172A")
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("ÓPTICA FAMILIAR AGS", marginLeft + 64f, yPos + 18f, paint)

            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.parseColor("#475569")
            canvas.drawText("Av. Adolfo López Mateos Pte 213, Centro, Ags • Tel: 4495543087", marginLeft + 64f, yPos + 34f, paint)
            canvas.drawText("Servicios Oftálmicos y Optometría Especializada", marginLeft + 64f, yPos + 48f, paint)

            paint.textAlign = Paint.Align.RIGHT
            paint.textSize = 9f
            canvas.drawText("Folio Exp: #${record.id}", marginRight, yPos + 18f, paint)
            canvas.drawText("Fecha: ${dateTimeFormat.format(Date(record.date))}", marginRight, yPos + 32f, paint)

            yPos += 64f

            // --- SEPARATOR LINE ---
            paint.color = Color.parseColor("#0D9488")
            paint.strokeWidth = 2.5f
            canvas.drawLine(marginLeft, yPos, marginRight, yPos, paint)
            paint.strokeWidth = 1f

            yPos += 20f

            // --- MAIN DOCUMENT TITLE BANNER ---
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 15f
            paint.color = Color.parseColor("#0D9488")
            canvas.drawText("EXPEDIENTE, DATOS GENERALES Y DIAGNÓSTICO", PAGE_WIDTH / 2f, yPos, paint)

            yPos += 22f

            // --- 2. DATOS GENERALES DEL PACIENTE ---
            drawSectionHeader(canvas, paint, "1. DATOS GENERALES DEL PACIENTE", marginLeft, yPos, contentWidth)
            yPos += 24f

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 9.5f
            paint.color = Color.parseColor("#1E293B")
            paint.textAlign = Paint.Align.LEFT

            canvas.drawText("Nombre Paciente: ", marginLeft + 8f, yPos, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(patient.fullName, marginLeft + 95f, yPos, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Teléfono: ", marginLeft + 350f, yPos, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(patient.phone.ifBlank { "No registrado" }, marginLeft + 400f, yPos, paint)

            yPos += 15f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Fecha Nacimiento: ", marginLeft + 8f, yPos, paint)
            canvas.drawText(patient.dateOfBirth.ifBlank { "N/A" }, marginLeft + 95f, yPos, paint)

            canvas.drawText("Correo: ", marginLeft + 350f, yPos, paint)
            canvas.drawText(patient.email.ifBlank { "N/A" }, marginLeft + 400f, yPos, paint)

            yPos += 15f
            canvas.drawText("Dirección: ", marginLeft + 8f, yPos, paint)
            canvas.drawText(patient.address.ifBlank { "Aguascalientes, Ags." }, marginLeft + 95f, yPos, paint)

            yPos += 25f

            // --- 3. HISTORIAL PATOLÓGICO Y ENTREVISTA CLÍNICA ---
            drawSectionHeader(canvas, paint, "2. HISTORIAL PATOLÓGICO Y ENTREVISTA CLÍNICA", marginLeft, yPos, contentWidth)
            yPos += 24f

            drawFieldRow(canvas, paint, "Historial Patológico Personal:", record.personalPathology, marginLeft + 8f, yPos)
            yPos += 16f
            drawFieldRow(canvas, paint, "Antecedentes Patológicos Familiares:", record.familyPathology, marginLeft + 8f, yPos)
            yPos += 16f
            drawFieldRow(canvas, paint, "Entrevista Clínica (Síntomas / Cefalea):", record.clinicalInterview, marginLeft + 8f, yPos)

            yPos += 25f

            // --- 4. PRUEBAS OPTOMÉTRICAS (MÍNIMO 5 PRUEBAS) ---
            drawSectionHeader(canvas, paint, "3. RESULTADOS DE PRUEBAS OPTOMÉTRICAS", marginLeft, yPos, contentWidth)
            yPos += 24f

            drawTestBox(canvas, paint, "1. Agudeza Visual (AV sin/con corr.)", record.testVisualAcuity, marginLeft, yPos, contentWidth / 2f - 6f)
            drawTestBox(canvas, paint, "2. Refracción / Retinoscopía", record.testRefraction, marginLeft + contentWidth / 2f + 6f, yPos, contentWidth / 2f - 6f)
            yPos += 42f

            drawTestBox(canvas, paint, "3. Oftalmoscopía / Fondo de Ojo", record.testOphthalmoscopy, marginLeft, yPos, contentWidth / 2f - 6f)
            drawTestBox(canvas, paint, "4. Biomicroscopía / Lampara Hendidura", record.testBiomicroscopy, marginLeft + contentWidth / 2f + 6f, yPos, contentWidth / 2f - 6f)
            yPos += 42f

            drawTestBox(canvas, paint, "5. Visión de Color (Ishihara) / Cover Test", record.testIshiharaColor, marginLeft, yPos, contentWidth)
            yPos += 46f

            // --- 5. GRADUACIÓN FINAL Y RECETA (TABLA FORMATO RECETA) ---
            drawSectionHeader(canvas, paint, "4. GRADUACIÓN FINAL Y RECETA (PRESCRIPCIÓN)", marginLeft, yPos, contentWidth)
            yPos += 24f

            // Table Header
            paint.color = Color.parseColor("#0F172A")
            canvas.drawRect(marginLeft, yPos, marginRight, yPos + 20f, paint)

            paint.color = Color.WHITE
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 9.5f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("OJO / RECETA", marginLeft + 12f, yPos + 14f, paint)
            canvas.drawText("ESFERA (ESF)", marginLeft + 110f, yPos + 14f, paint)
            canvas.drawText("CILINDRO (CYL)", marginLeft + 210f, yPos + 14f, paint)
            canvas.drawText("EJE", marginLeft + 330f, yPos + 14f, paint)
            canvas.drawText("ADICIÓN (ADD)", marginLeft + 420f, yPos + 14f, paint)

            yPos += 20f

            // OD Row
            paint.color = Color.parseColor("#F8FAFC")
            canvas.drawRect(marginLeft, yPos, marginRight, yPos + 22f, paint)
            paint.color = Color.parseColor("#0F172A")
            canvas.drawText("RX OD (Ojo Derecho)", marginLeft + 12f, yPos + 15f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(record.odSphere, marginLeft + 110f, yPos + 15f, paint)
            canvas.drawText(record.odCylinder, marginLeft + 210f, yPos + 15f, paint)
            canvas.drawText(record.odAxis, marginLeft + 330f, yPos + 15f, paint)
            canvas.drawText(record.odAddition, marginLeft + 420f, yPos + 15f, paint)

            yPos += 22f

            // OI Row
            paint.color = Color.parseColor("#FFFFFF")
            canvas.drawRect(marginLeft, yPos, marginRight, yPos + 22f, paint)
            paint.color = Color.parseColor("#0F172A")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("RX OI (Ojo Izquierdo)", marginLeft + 12f, yPos + 15f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(record.oiSphere, marginLeft + 110f, yPos + 15f, paint)
            canvas.drawText(record.oiCylinder, marginLeft + 210f, yPos + 15f, paint)
            canvas.drawText(record.oiAxis, marginLeft + 330f, yPos + 15f, paint)
            canvas.drawText(record.oiAddition, marginLeft + 420f, yPos + 15f, paint)

            yPos += 26f

            // DIS, ALT, TRATAMIENTO Grid
            paint.color = Color.parseColor("#F1F5F9")
            canvas.drawRoundRect(marginLeft, yPos, marginRight, yPos + 40f, 6f, 6f, paint)

            paint.color = Color.parseColor("#1E293B")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("DIS (Distancia Interpupilar): ", marginLeft + 10f, yPos + 16f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(record.pupillaryDistance, marginLeft + 150f, yPos + 16f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("ALT (Altura de pastilla): ", marginLeft + 270f, yPos + 16f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(record.segmentHeight, marginLeft + 390f, yPos + 16f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("TRATAMIENTO: ", marginLeft + 10f, yPos + 32f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(record.treatment, marginLeft + 90f, yPos + 32f, paint)

            yPos += 52f

            // --- 6. ANOTACIONES FINALES Y DIAGNÓSTICO ---
            drawSectionHeader(canvas, paint, "5. ANOTACIONES FINALES Y DIAGNÓSTICO OPTOMÉTRICO", marginLeft, yPos, contentWidth)
            yPos += 24f

            drawFieldRow(canvas, paint, "Diagnóstico Optométrico:", record.diagnosis, marginLeft + 8f, yPos)
            yPos += 18f
            drawFieldRow(canvas, paint, "Anotaciones / Observaciones:", record.finalAnnotations, marginLeft + 8f, yPos)

            // --- 7. PIE DE PÁGINA CON RESPONSABLE SANITARIO ---
            val footerY = PAGE_HEIGHT - 90f
            paint.color = Color.parseColor("#94A3B8")
            paint.strokeWidth = 1f
            canvas.drawLine(PAGE_WIDTH / 2f - 120f, footerY, PAGE_WIDTH / 2f + 120f, footerY, paint)

            paint.textAlign = Paint.Align.CENTER
            paint.color = Color.parseColor("#0F172A")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 10.5f
            canvas.drawText(record.optometristName, PAGE_WIDTH / 2f, footerY + 16f, paint)

            paint.color = Color.parseColor("#0D9488")
            paint.textSize = 9f
            canvas.drawText("RESPONSABLE SANITARIO", PAGE_WIDTH / 2f, footerY + 28f, paint)

            paint.color = Color.parseColor("#64748B")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 8f
            canvas.drawText("Cédula Profesional y Licencia de Práctica Optométrica • Óptica Familiar Ags", PAGE_WIDTH / 2f, footerY + 42f, paint)

            pdfDocument.finishPage(page)

            // Save PDF File
            val fileName = "expediente_clinico_${patient.fullName.replace(" ", "_")}_${record.id}.pdf"
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()

            sharePdf(context, file, "Expediente Clínico y Diagnóstico PDF")
        } catch (e: Exception) {
            Toast.makeText(context, "Error al generar PDF de Expediente: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun drawSectionHeader(canvas: Canvas, paint: Paint, title: String, x: Float, y: Float, width: Float) {
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRect(x, y - 12f, x + width, y + 6f, paint)
        paint.color = Color.parseColor("#0D9488")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(title, x + 8f, y, paint)
    }

    private fun drawFieldRow(canvas: Canvas, paint: Paint, label: String, value: String, x: Float, y: Float) {
        paint.color = Color.parseColor("#475569")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9.5f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(label, x, y, paint)

        val labelWidth = paint.measureText(label)
        paint.color = Color.parseColor("#0F172A")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(value.ifBlank { "Sin observaciones" }, x + labelWidth + 6f, y, paint)
    }

    private fun drawTestBox(canvas: Canvas, paint: Paint, title: String, value: String, x: Float, y: Float, width: Float) {
        paint.color = Color.parseColor("#F8FAFC")
        canvas.drawRoundRect(x, y, x + width, y + 36f, 6f, 6f, paint)
        paint.color = Color.parseColor("#CBD5E1")
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(x, y, x + width, y + 36f, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#0F172A")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9f
        canvas.drawText(title, x + 8f, y + 14f, paint)

        paint.color = Color.parseColor("#334155")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8.5f
        val shortVal = if (value.length > 55) value.take(55) + "..." else value
        canvas.drawText(shortVal.ifBlank { "Resultado normal" }, x + 8f, y + 28f, paint)
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
