package com.tuempresa.inventariovial.scap.domain

import com.tuempresa.inventariovial.catalog.SicCatalogRepository
import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot
import com.tuempresa.inventariovial.supplementary.*

data class SicCompleteness(
    val complete: List<String>, val missing: List<String>, val notApplicable: List<String>,
    val unresolved: List<String>, val requiredMissing: List<String>
) {
    val isComplete get() = requiredMissing.isEmpty()
}

/** MTC Manual de Inventarios Viales, pp. 207–212, 242–243;
 * existing project catalogs and discrepancies: docs/ARCHITECTURE_REVIEW.md.
 * No equivalence is inferred from a SCAP dropdown position or a structural row.
 */
object BridgeSicContract {
    const val SOURCE = "Manual de Inventarios Viales MTC, fichas SIC-17A/B pp. 207–212 y tipos pp. 242–243"
    val formats = listOf("SIC-17", "SIC-17A", "SIC-17B")
    val keys17 = listOf("routeCode","roadbedCode","startPrCode","startDistanceM","endPrCode","endDistanceM",
        "classCode","typeCode","bridgeCode","inventoriedCode","spans","dimension1LengthM","dimension2LowerHeightM",
        "structuralConditionCode","functionalConditionCode","surveyDate","serviceTypeCode","singularityCode","singularityName","dimension3UpperHeightM")
    val locationKeys = setOf("routeCode","roadbedCode","startPrCode","startDistanceM","endPrCode","endDistanceM")
    fun prefix(format: String) = when(format) { "SIC-17" -> "sic17."; "SIC-17A" -> "sic17a."; "SIC-17B" -> "sic17b."; else -> error("Formato no soportado") }
    fun fields(format: String, values: Map<String,String> = emptyMap()): List<SupplementaryField> = when(format) {
        "SIC-17A" -> SupplementaryForms.fields(SupplementaryFormat.SIC17A)
        "SIC-17B" -> SupplementaryForms.fields(SupplementaryFormat.SIC17B).map {
            when(it.key) {
                "maximumCapacity" -> it.copy(label="Capacidad máxima")
                "designLoad" -> it.copy(label="Sobrecarga de diseño")
                "wearingSurface" -> it.copy(label="Superficie de desgaste",kind=FieldKind.CODE)
                else -> it
            }
        }
        "SIC-17" -> keys17.map { key ->
            val label=when(key) {
                "routeCode" -> "Ruta"; "roadbedCode" -> "Calzada"; "startPrCode" -> "Código PR"; "startDistanceM" -> "Distancia desde PR (m)"
                "endPrCode" -> "Código PR final"; "endDistanceM" -> "Distancia final desde PR (m)"
                "bridgeCode" -> "Código del puente"; "spans" -> "Número de vanos"; "surveyDate" -> "Fecha"
                "dimension1LengthM" -> "Longitud total (m)"; "dimension2LowerHeightM" -> "Altura libre inferior (m)"
                "dimension3UpperHeightM" -> "Altura libre superior (m)"; else -> ScapSicSupplement.labels[key] ?: key
            }
            val options=if(key=="roadbedCode") SicCatalogRepository.roadbedCodes else ScapSicSupplement.options(key,values.mapKeys {"sic17."+it.key})
            SupplementaryField(key,label,when {
                options.isNotEmpty() -> FieldKind.CODE
                key=="spans" -> FieldKind.INTEGER
                key.endsWith("M") -> FieldKind.DECIMAL
                else -> FieldKind.TEXT
            },options)
        }
        else -> error("Formato no soportado")
    }
    fun unresolved(format:String): Map<String,String> = if(format=="SIC-17B") mapOf(
        "maximumCapacity" to "Capacidad máxima: pendiente de definición contractual (unidad y diferencia respecto a sobrecarga de diseño)."
    ) else emptyMap()
    // Completeness covers all defined columns, including optional entries. Export minimum remains
    // explicit, and optional missing input is never disguised as a contract-mapping failure.
    fun required(format:String): Set<String> = when(format) {
        "SIC-17" -> keys17.toSet()-setOf("singularityName")
        "SIC-17A" -> setOf("bridgeCode","bridgeName","latitude","longitude","altitude","lanes","roadwayWidthM","sidewalkWidthM","deckWidthM","superstructureWidthM","alignmentCode")
        "SIC-17B" -> fields(format).map{it.key}.toSet()-setOf("maximumCapacity","designLoad","comments")
        else -> emptySet()
    }
    fun valid(field:SupplementaryField,value:String): Boolean {
        if(value.isBlank()) return false
        val number=ScapNumbers.decimal(value)
        return when(field.kind) {
            FieldKind.TEXT -> when(field.key) {
                "startPrCode","endPrCode" -> value.matches(Regex("[0-9]{1,4}"))
                "surveyDate" -> runCatching { java.time.LocalDate.parse(value,java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT)) }.isSuccess
                else -> true
            }
            FieldKind.CODE -> field.options.any{it.substringBefore(" - ")==value}
            FieldKind.DESCRIBED_CODE -> value in field.options
            FieldKind.INTEGER -> value.toIntOrNull()?.let { if(field.key in setOf("lanes","spans")) it>0 else it>=0 }==true
            FieldKind.YEAR -> value.toIntOrNull()?.let {value.length==4 && it in 1000..java.time.Year.now().value}==true
            FieldKind.DECIMAL -> number!=null && number.isFinite() && number>=0
            FieldKind.SIGNED_DECIMAL -> number?.isFinite()==true
            FieldKind.LATITUDE -> number!=null && number.isFinite() && number in -90.0..90.0
            FieldKind.LONGITUDE -> number!=null && number.isFinite() && number in -180.0..180.0
        }
    }
    fun assess(mapping:ScapSicMapping): SicCompleteness {
        val values=mapping.fields.filter{it.status in setOf(ScapMappingStatus.DIRECT,ScapMappingStatus.TRANSFORM)}.associate{it.sicField to it.value}
        val complete=mutableListOf<String>();val missing=mutableListOf<String>();val na=mutableListOf<String>();val requiredMissing=mutableListOf<String>()
        val blocked=unresolved(mapping.format)
        for(field in fields(mapping.format,values)) {
            if(field.key in blocked) continue
            if(valid(field,values[field.key].orEmpty())) {
                if(values[field.key]=="0" && field.kind==FieldKind.CODE && field.options.any {it.startsWith("0 - No aplica")}) na+=field.label
                else complete+=field.label
            } else {
                missing+=field.label
                if(field.key in required(mapping.format)) requiredMissing+=field.label
            }
        }
        return SicCompleteness(complete,missing,na,blocked.values.toList(),requiredMissing)
    }
    fun manualValues(s:ScapInspectionSnapshot,format:String): Map<String,String> = when(format) {
        "SIC-17A" -> s.sic17a?.values().orEmpty()
        "SIC-17B" -> s.sic17b?.values().orEmpty()
        else -> s.values().filterKeys{it.startsWith("sic17.")}.mapKeys{it.key.removePrefix("sic17.")}
    }
    fun withManual(s:ScapInspectionSnapshot,format:String,inherited:List<ScapMappedField>): List<ScapMappedField> {
        val prefix=prefix(format);val stored=manualValues(s,format)
        val keys=fields(format).map{it.key}.toSet()-unresolved(format).keys-(if(format=="SIC-17") locationKeys else emptySet())
        val overrides=keys.filter { !stored[it].isNullOrBlank() || s.values().containsKey(prefix+"override."+it) }.toSet()
        val manual=overrides.mapNotNull { key ->
            val raw=stored[key].orEmpty()
            if(raw.isBlank()) null else {
                // Historical described-code values are preserved in Room; export only the SIC code.
                val value=if(key=="wearingSurface") raw.substringBefore(" - ") else raw
                if(key=="wearingSurface" && value=="3" && !raw.contains(" - ") && s.values()[prefix+"wearingSurfaceDescription"].isNullOrBlank()) return@mapNotNull null
                val prior=inherited.firstOrNull{it.sicField==key}
                ScapMappedField(prefix+key,key,value,ScapMappingStatus.DIRECT,
                    if(prior!=null) "Valor SIC revisado; origen SCAP: ${prior.scapField}. No modifica SCAP." else "Captura SIC; $SOURCE")
            }
        }
        return inherited.filter{it.sicField !in overrides}+manual
    }
}
