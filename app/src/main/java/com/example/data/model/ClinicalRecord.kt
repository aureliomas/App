package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clinical_records")
data class ClinicalRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientId: Long,
    val date: Long = System.currentTimeMillis(),
    
    // --- HISTORIAL PATOLÓGICO Y ENTREVISTA CLÍNICA ---
    val personalPathology: String = "Ninguna reportada",
    val familyPathology: String = "Ninguna reportada",
    val clinicalInterview: String = "Sin cefalea recurrente ni astenopia severa",
    
    // --- RESULTADOS DE AL MENOS 5 PRUEBAS OPTOMÉTRICAS ---
    val testVisualAcuity: String = "OD 20/30 | OI 20/40",
    val testRefraction: String = "OD -1.00 -0.50 x 180° | OI -1.25 -0.75 x 175°",
    val testOphthalmoscopy: String = "Fondo de ojo normal, papila de bordes nítidos",
    val testBiomicroscopy: String = "Córnea transparente, película lagrimal estable",
    val testIshiharaColor: String = "Visión de color normal (12/12)",
    
    // --- GRADUACIÓN FINAL Y RECETA ---
    // Ojo Derecho (OD)
    val odSphere: String = "-1.00",
    val odCylinder: String = "-0.50",
    val odAxis: String = "180°",
    val odAddition: String = "0.00",
    
    // Ojo Izquierdo (OI)
    val oiSphere: String = "-1.25",
    val oiCylinder: String = "-0.75",
    val oiAxis: String = "175°",
    val oiAddition: String = "0.00",
    
    // Parámetros de Armazón y Lente
    val pupillaryDistance: String = "62 mm", // DIS
    val segmentHeight: String = "18 mm",    // ALT
    val treatment: String = "Micas Policristal + Antirreflejante Crizal + Filtro Azul", // TRATAMIENTO
    val lensType: String = "Monofocal",
    
    // --- ANOTACIONES FINALES Y DIAGNÓSTICO ---
    val finalAnnotations: String = "Paciente refiere buena adaptación visual. Se recomienda control anual.",
    val diagnosis: String = "Astigmatismo Miópico Compuesto Ambos Ojos",
    val optometristName: String = "L. Opt. Brisaida Gpe Guillen Ortiz",
    val nextExamDate: Long = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000)
)

