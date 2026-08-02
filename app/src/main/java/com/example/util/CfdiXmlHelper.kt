package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class CfdiEmisor(
    val rfc: String = "OFA180420A12",
    val nombre: String = "ÓPTICA FAMILIAR AGS S.A. DE C.V.",
    val regimenFiscal: String = "612", // 612 - Personas Físicas con Actividades Empresariales
    val codigoPostal: String = "20000"
)

data class CfdiReceptor(
    val rfc: String = "XAXX010101000",
    val nombre: String = "PÚBLICO EN GENERAL",
    val domicilioFiscal: String = "20000",
    val regimenFiscal: String = "616", // 616 - Sin obligaciones fiscales
    val usoCfdi: String = "S01", // S01 - Sin efectos fiscales / D01 Honorarios médicos
    val email: String = ""
)

data class CfdiConcepto(
    val claveProdServ: String = "85121800", // 85121800 Servicios de optometria / 42142900 Anteojos
    val claveUnidad: String = "H87", // H87 Pieza / E48 Unidad de servicio
    val cantidad: Double = 1.0,
    val descripcion: String = "Lentes Oftálmicos Graduados Antirreflejante",
    val valorUnitario: Double = 1000.0,
    val importe: Double = 1000.0,
    val descuento: Double = 0.0,
    val baseIva: Double = 1000.0,
    val importeIva: Double = 160.0
)

data class CfdiDatosFactura(
    val folio: String = "F-1001",
    val fechaIso: String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale("es", "MX")).format(Date()),
    val emisor: CfdiEmisor = CfdiEmisor(),
    val receptor: CfdiReceptor = CfdiReceptor(),
    val conceptos: List<CfdiConcepto> = listOf(CfdiConcepto()),
    val formaPago: String = "01", // 01 Efectivo, 03 Transferencia, 04 Tarjeta
    val metodoPago: String = "PUE", // PUE Pago en una sola exhibición
    val subtotal: Double = 1000.0,
    val descuento: Double = 0.0,
    val iva: Double = 160.0,
    val total: Double = 1160.0,
    val uuid: String = UUID.randomUUID().toString().uppercase(Locale.ROOT)
)

object CfdiXmlHelper {

    fun generateCfdi40Xml(datos: CfdiDatosFactura): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<cfdi:Comprobante xmlns:cfdi=\"http://www.sat.gob.mx/cfd/4\"\n")
        sb.append("    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n")
        sb.append("    xmlns:tfd=\"http://www.sat.gob.mx/TimbreFiscalDigital\"\n")
        sb.append("    xsi:schemaLocation=\"http://www.sat.gob.mx/cfd/4 http://www.sat.gob.mx/sitio_internet/cfd/4/cfdv40.xsd\"\n")
        sb.append("    Version=\"4.0\"\n")
        sb.append("    Serie=\"F\"\n")
        sb.append("    Folio=\"${datos.folio}\"\n")
        sb.append("    Fecha=\"${datos.fechaIso}\"\n")
        sb.append("    Sello=\"SELLO_DIGITAL_CFDI_EMISOR_SAT_OPTICA_FAMILIAR_AGS_SAMPLE==\"\n")
        sb.append("    FormaPago=\"${datos.formaPago}\"\n")
        sb.append("    NoCertificado=\"00001000000501234567\"\n")
        sb.append("    Certificado=\"CERTIFICADO_SAT_MIIF3DCCA8ACC...\"\n")
        sb.append("    SubTotal=\"${String.format(Locale.US, "%.2f", datos.subtotal)}\"\n")
        if (datos.descuento > 0) {
            sb.append("    Descuento=\"${String.format(Locale.US, "%.2f", datos.descuento)}\"\n")
        }
        sb.append("    Moneda=\"MXN\"\n")
        sb.append("    Total=\"${String.format(Locale.US, "%.2f", datos.total)}\"\n")
        sb.append("    TipoDeComprobante=\"I\"\n")
        sb.append("    Exportacion=\"01\"\n")
        sb.append("    MetodoPago=\"${datos.metodoPago}\"\n")
        sb.append("    LugarExpedicion=\"${datos.emisor.codigoPostal}\">\n")

        // Emisor
        sb.append("  <cfdi:Emisor Rfc=\"${datos.emisor.rfc}\" Nombre=\"${escapeXml(datos.emisor.nombre)}\" RegimenFiscal=\"${datos.emisor.regimenFiscal}\"/>\n")

        // Receptor
        sb.append("  <cfdi:Receptor Rfc=\"${datos.receptor.rfc}\" Nombre=\"${escapeXml(datos.receptor.nombre)}\" DomicilioFiscalReceptor=\"${datos.receptor.domicilioFiscal}\" RegimenFiscalReceptor=\"${datos.receptor.regimenFiscal}\" UsoCFDI=\"${datos.receptor.usoCfdi}\"/>\n")

        // Conceptos
        sb.append("  <cfdi:Conceptos>\n")
        for (c in datos.conceptos) {
            sb.append("    <cfdi:Concepto ClaveProdServ=\"${c.claveProdServ}\" Cantidad=\"${String.format(Locale.US, "%.2f", c.cantidad)}\" ClaveUnidad=\"${c.claveUnidad}\" Unidad=\"Pieza\" Descripcion=\"${escapeXml(c.descripcion)}\" ValorUnitario=\"${String.format(Locale.US, "%.2f", c.valorUnitario)}\" Importe=\"${String.format(Locale.US, "%.2f", c.importe)}\" ObjetoImp=\"02\">\n")
            sb.append("      <cfdi:Impuestos>\n")
            sb.append("        <cfdi:Traslados>\n")
            sb.append("          <cfdi:Traslado Base=\"${String.format(Locale.US, "%.2f", c.baseIva)}\" Impuesto=\"002\" TipoFactor=\"Tasa\" TasaOCuota=\"0.160000\" Importe=\"${String.format(Locale.US, "%.2f", c.importeIva)}\"/>\n")
            sb.append("        </cfdi:Traslados>\n")
            sb.append("      </cfdi:Impuestos>\n")
            sb.append("    </cfdi:Concepto>\n")
        }
        sb.append("  </cfdi:Conceptos>\n")

        // Impuestos
        sb.append("  <cfdi:Impuestos TotalImpuestosTrasladados=\"${String.format(Locale.US, "%.2f", datos.iva)}\">\n")
        sb.append("    <cfdi:Traslados>\n")
        sb.append("      <cfdi:Traslado Base=\"${String.format(Locale.US, "%.2f", datos.subtotal)}\" Impuesto=\"002\" TipoFactor=\"Tasa\" TasaOCuota=\"0.160000\" Importe=\"${String.format(Locale.US, "%.2f", datos.iva)}\"/>\n")
        sb.append("    </cfdi:Traslados>\n")
        sb.append("  </cfdi:Impuestos>\n")

        // Complemento Timbre Fiscal Digital PAC
        sb.append("  <cfdi:Complemento>\n")
        sb.append("    <tfd:TimbreFiscalDigital xmlns:tfd=\"http://www.sat.gob.mx/TimbreFiscalDigital\"\n")
        sb.append("        xsi:schemaLocation=\"http://www.sat.gob.mx/TimbreFiscalDigital http://www.sat.gob.mx/sitio_internet/cfd/TimbreFiscalDigital/TimbreFiscalDigitalv11.xsd\"\n")
        sb.append("        Version=\"1.1\"\n")
        sb.append("        UUID=\"${datos.uuid}\"\n")
        sb.append("        FechaTimbrado=\"${datos.fechaIso}\"\n")
        sb.append("        RfcProvCertif=\"SAT970701NN3\"\n")
        sb.append("        SelloCFD=\"SELLO_CFD_OPTICA_FAMILIAR_AGS_PROV_PAC==\"\n")
        sb.append("        NoCertificadoSAT=\"00001000000504465028\"\n")
        sb.append("        SelloSAT=\"SELLO_SAT_OFICIAL_PROVEEDOR_PAC_PUNTO_DE_VENTA==\"/>\n")
        sb.append("  </cfdi:Complemento>\n")

        sb.append("</cfdi:Comprobante>")
        return sb.toString()
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    fun saveAndShareXml(context: Context, xmlContent: String, folio: String) {
        try {
            val fileName = "CFDI_40_${folio.replace(" ", "_")}.xml"
            val file = File(context.cacheDir, fileName)
            file.writeText(xmlContent, Charsets.UTF_8)

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/xml"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Factura Electrónica XML - $folio")
                putExtra(Intent.EXTRA_TEXT, "Adjunto archivo XML CFDI 4.0 de Óptica Familiar Ags - Folio $folio")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Compartir XML CFDI 4.0"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
