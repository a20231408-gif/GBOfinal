package com.tuempresa.inventariovial.scap.export

import com.tuempresa.inventariovial.export.*
import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot
import com.tuempresa.inventariovial.scap.domain.*
import com.tuempresa.inventariovial.supplementary.*
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class ScapSicPreview(val format:String,val columns:List<SicColumn>,val values:List<Any?>,
    val missingRequired:List<String>,val missingOptional:List<String>,val pending:List<String>)

object ScapSicExporter {
    private val accepted=setOf(ScapMappingStatus.DIRECT,ScapMappingStatus.TRANSFORM)
    fun previews(s:ScapInspectionSnapshot)=listOf(ScapToSicMapper.mapToSic17(s),ScapToSicMapper.mapToSic17A(s),ScapToSicMapper.mapToSic17B(s)).map(::preview)
    fun preview(mapping:ScapSicMapping):ScapSicPreview {
        val safe=mapping.fields.filter{it.status in accepted && it.value.isNotBlank()}.associate{it.sicField to it.value}
        val keys:List<String>;val columns:List<SicColumn>;val required:Set<String>
        if(mapping.format=="SIC-17") {
            keys=BridgeSicContract.keys17
            columns=SicExportFormat.SIC17.columns
            required=BridgeSicContract.required(mapping.format)
        } else {
            require(mapping.format in setOf("SIC-17A","SIC-17B")) {"Formato no soportado"}
            val fields=BridgeSicContract.fields(mapping.format);keys=fields.map{it.key}
            columns=fields.map {SicColumn(it.label,when(it.kind){FieldKind.YEAR,FieldKind.INTEGER->CellKind.INTEGER;FieldKind.DECIMAL,FieldKind.SIGNED_DECIMAL,FieldKind.LATITUDE,FieldKind.LONGITUDE->CellKind.DECIMAL;else->CellKind.TEXT},24.0)}
            // Required capture is shared with the visible completeness contract.
            required=BridgeSicContract.required(mapping.format)
        }
        val contract=BridgeSicContract.fields(mapping.format,safe).associateBy{it.key}
        val values=keys.mapIndexed{i,key->safe[key]?.takeIf{BridgeSicContract.valid(contract.getValue(key),it) && key !in BridgeSicContract.unresolved(mapping.format)}?.let{raw->when(columns[i].kind){
            CellKind.TEXT->raw
            CellKind.INTEGER->raw.toIntOrNull()
            CellKind.DECIMAL->ScapNumbers.decimal(raw)
            CellKind.DATE->runCatching{LocalDate.parse(raw,DateTimeFormatter.ofPattern("dd/MM/uuuu"))}.getOrNull()
        }}}
        val missing=keys.indices.filter{values[it]==null && keys[it] !in BridgeSicContract.unresolved(mapping.format)}
        return ScapSicPreview(mapping.format,columns,values,missing.filter{keys[it] in required}.map{contract.getValue(keys[it]).label},
            missing.filter{keys[it] !in required}.map{contract.getValue(keys[it]).label},mapping.pending)
    }
    fun write(output:OutputStream,s:ScapInspectionSnapshot,format:String) {
        val preview=previews(s).single{it.format==format}
        require(preview.missingRequired.isEmpty()){"$format: faltan ${preview.missingRequired.joinToString()}"}
        SicExcelWriter.writeMappedRow(output,format,s.roadRecord?.routeCode?.takeIf{it.isNotBlank()} ?: s.values()["route"].orEmpty(),preview.columns,preview.values,if(format=="SIC-17A") setOf(7,8) else emptySet())
    }
}
