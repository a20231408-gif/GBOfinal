package com.tuempresa.inventariovial.export

import com.tuempresa.inventariovial.data.entity.InventorySnapshot
import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot
import com.tuempresa.inventariovial.scap.domain.*
import com.tuempresa.inventariovial.scap.export.*
import org.json.JSONObject
import java.io.*

/** Local-only derivative. Contractual writers and sicRows continue to reject incomplete capture. */
object ProvisionalSicExcel {
    const val MARKER="PROVISIONAL — PENDIENTE DE COMPLETAR EN GABINETE"
    data class Pending(val field:String,val section:String,val reason:String,val state:String="PENDIENTE")
    fun write(output:OutputStream,s:ScapInspectionSnapshot,format:String) {
        val mapping=when(format){"SIC-17"->ScapToSicMapper.mapToSic17(s);"SIC-17A"->ScapToSicMapper.mapToSic17A(s);"SIC-17B"->ScapToSicMapper.mapToSic17B(s);else->error("Formato no soportado")}
        val fields=BridgeSicContract.fields(format,mapping.fields.associate{it.sicField to it.value})
        val keys=if(format=="SIC-17")BridgeSicContract.keys17 else fields.map{it.key}
        val preview=ScapSicExporter.previews(s).single{it.format==format}
        val values=keys.map {key->mapping.fields.lastOrNull{it.sicField==key}?.value?.takeIf{it.isNotBlank()}}
        val pending=fields.mapNotNull {f->val v=values.getOrNull(keys.indexOf(f.key));when {
            v==null->Pending(f.label,format,"Sin dato registrado")
            !BridgeSicContract.valid(f,v)->Pending(f.label,format,"Valor capturado inválido; conservado literalmente","REVISAR")
            else->null
        }}+mapping.pending.map{Pending("Correspondencia",format,it,"REVISAR")}
        mapped(output,format,s.roadRecord?.routeCode ?: s.values()["route"].orEmpty(),preview.columns,values,
            s.inspection.id,s.inspection.createdBy,s.inspection.updatedAt,s.inspection.createdAt,pending)
    }
    fun write(output:OutputStream,s:InventorySnapshot,author:String="",formatCode:String=s.record.sicCode) {
        val r=s.record;val format=SicExportFormat.entries.single{it.code==formatCode}
        val json=com.tuempresa.inventariovial.server.ServerPayload.encode(s).optJSONObject("sic")?.optJSONObject(r.sicCode) ?: JSONObject()
        val columns=format.columns.toMutableList();val values:List<Any?>
        val fieldFormat=format.fieldFormat
        if(fieldFormat!=null){
            val raw=com.tuempresa.inventariovial.sic.SicFieldRepository.core(r)+(if(format==SicExportFormat.SIC18A)s.sic18?.let{com.tuempresa.inventariovial.supplementary.Sic18AStatus.inherited(it,s.sic18a)}.orEmpty()else s.sicExtension?.values().orEmpty())
            values=fieldFormat.fields.map{raw[it.key]?.takeIf(String::isNotBlank)}
        }else {
            fun known(key:String):Any?=json.opt(key).takeUnless{it==JSONObject.NULL || it==""}
            val detailKeys=when(format){
                SicExportFormat.SIC17->listOf("classCode","typeCode","bridgeCode","inventoriedCode","spans","dimension1LengthM","dimension2LowerHeightM","structuralConditionCode","functionalConditionCode","@date","serviceTypeCode","singularityCode","singularityName","dimension3UpperHeightM")
                SicExportFormat.SIC18->listOf("classCode","typeCode","spans","crossSectionCode","dimension1M","dimension2M","structuralConditionCode","functionalConditionCode","@date")
                SicExportFormat.SIC19->listOf("classCode","typeCode","crossSectionCode","structuralConditionCode","functionalConditionCode","@date")
                SicExportFormat.SIC20->listOf("classCode","typeCode","dimension1M","dimension2M","structuralConditionCode","functionalConditionCode","@date")
                SicExportFormat.SIC21->listOf("classCode","typeCode","materialCode","conditionCode","@date")
                SicExportFormat.SIC22->listOf("classCode","typeCode","materialCode","signalCode","kilometerPostNumber","conditionCode","@date")
                SicExportFormat.SIC23->listOf("classCode","typeCode","widthM","description","@date")
                else->emptyList()
            }
            values=listOf(r.routeCode,r.roadbedCode,r.startPrCode.takeIf{it.isNotBlank()},r.startDistanceM.takeIf{r.startPrCode.isNotBlank()})+
                (if(format.hasEndLocationColumns)listOf(r.endPrCode,r.endDistanceM.takeIf{!r.endPrCode.isNullOrBlank()})else emptyList())+
                (if(format in listOf(SicExportFormat.SIC17,SicExportFormat.SIC18))emptyList()else listOf(r.sideCode))+
                detailKeys.map{if(it=="@date")r.surveyDate.takeIf(String::isNotBlank)else known(it)}
        }
        val withNotes=if(format!=SicExportFormat.SIC18A && columns.none{it.label.equals("OBSERVACIONES",true)}){columns+=SicColumn("OBSERVACIONES");values+r.observations}else values
        require(columns.size==withNotes.size){"Proyección provisional incompatible: ${format.code}"}
        val pending=columns.indices.filter{withNotes[it]==null||withNotes[it]==""}.map{Pending(columns[it].label,r.sicCode,"Sin dato registrado; revisar aplicabilidad en gabinete")}+
            Pending("Validación contractual",r.sicCode,"Captura ${r.status}; completar y confirmar antes de utilizar como entrega oficial","NO VALIDADO")
        mapped(output,format.code,r.routeCode,columns,withNotes,r.id,s.sicExtension?.ownerUserId ?: author,r.updatedAt,r.createdAt,pending)
    }
    fun writeBridgeSupplement(out:OutputStream,s:InventorySnapshot,format:String,author:String) {
        require(format in setOf("SIC-17A","SIC-17B"))
        val manual=if(format=="SIC-17A")s.sic17a?.values().orEmpty()else s.sic17b?.values().orEmpty()
        val raw=manual+mapOf("bridgeCode" to (manual["bridgeCode"].takeUnless{it.isNullOrBlank()} ?: s.sic17?.bridgeCode.orEmpty()))
        val fields=BridgeSicContract.fields(format,raw);val preview=ScapSicExporter.supplement(s,format)
        val values=fields.map{raw[it.key]?.takeIf(String::isNotBlank)}
        val pending=fields.mapNotNull {f->val v=raw[f.key].orEmpty();when {
            v.isBlank()->Pending(f.label,format,"Sin dato registrado")
            !BridgeSicContract.valid(f,v)->Pending(f.label,format,"Valor capturado conservado; revisar contrato","REVISAR")
            else->null
        }}
        mapped(out,format,s.record.routeCode,preview.columns,values,s.record.id,author,s.record.updatedAt,s.record.createdAt,pending)
    }
    private fun column(n:Int):String=if(n<=26)('A'+n-1).toString()else column((n-1)/26)+('A'+(n-1)%26)
    private fun mapped(out:OutputStream,format:String,route:String,columns:List<SicColumn>,values:List<Any?>,id:String,author:String,revision:Long,date:Long,pending:List<Pending>){
        val body=ByteArrayOutputStream();SicExcelWriter.writeMappedRow(body,format,route,columns,values)
        val notes=columns.indexOfFirst{it.label.equals("OBSERVACIONES",true)}
        val bytes=if(notes>=0){
            val w=TemplateWorkbook(body.toByteArray().inputStream());val ref=column(notes+1)
            listOf(13,14).forEach{w.cell(1,"$ref$it").removeAttribute("s")}
            w.sheet(1).nodes("col").filter{it.getAttribute("min")=="${notes+1}"&&it.getAttribute("max")=="${notes+1}"}.forEach{it.parentNode.removeChild(it)}
            ByteArrayOutputStream().also{w.writeCopy(it)}.toByteArray()
        }else body.toByteArray()
        decorate(out,bytes,id,author,revision,date,pending,1,"A11")
    }
    /** Adds a visible marker and audit sheet to a COPY, never edits a source template. */
    fun decorate(out:OutputStream,bytes:ByteArray,id:String,author:String,revision:Long,date:Long,pending:List<Pending>,markerSheet:Int=1,markerCell:String="A1"){
        val w=TemplateWorkbook(bytes.inputStream());w.set(markerSheet,markerCell,MARKER)
        val workbook=w.document("xl/workbook.xml");val sheets=workbook.nodes("sheets").single()
        val index=workbook.nodes("sheet").maxOf{it.getAttribute("sheetId").toInt()}+1
        val rel="gboPending$index";val main="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
        sheets.appendChild(workbook.createElementNS(main,"sheet").apply{setAttribute("name","PENDIENTES");setAttribute("sheetId",index.toString());setAttributeNS("http://schemas.openxmlformats.org/officeDocument/2006/relationships","r:id",rel)})
        val relationships=w.document("xl/_rels/workbook.xml.rels")
        relationships.documentElement.appendChild(relationships.createElementNS("http://schemas.openxmlformats.org/package/2006/relationships","Relationship").apply{
            setAttribute("Id",rel);setAttribute("Type","http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet");setAttribute("Target","worksheets/sheet$index.xml")})
        val types=w.document("[Content_Types].xml");types.documentElement.appendChild(types.createElementNS("http://schemas.openxmlformats.org/package/2006/content-types","Override").apply{setAttribute("PartName","/xl/worksheets/sheet$index.xml");setAttribute("ContentType","application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml")})
        fun esc(v:String)=v.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;")
        val rows=listOf(listOf(MARKER),listOf("UUID",id),listOf("Autor",author),listOf("Revisión",revision.toString()),listOf("Creación epoch ms",date.toString()),listOf("Revisión UTC",java.time.Instant.ofEpochMilli(revision).toString()),listOf("Uso","Copia documental; no restaura Room ni es una fila SIC contractual"),listOf("Campo","Sección","Motivo","Estado"))+pending.map{listOf(it.field,it.section,it.reason,it.state)}
        w.parts["xl/worksheets/sheet$index.xml"]=("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"$main\"><sheetViews><sheetView workbookViewId=\"0\"/></sheetViews><cols><col min=\"1\" max=\"2\" width=\"30\" customWidth=\"1\"/><col min=\"3\" max=\"3\" width=\"85\" customWidth=\"1\"/><col min=\"4\" max=\"4\" width=\"22\" customWidth=\"1\"/></cols><sheetData>"+rows.mapIndexed{r,row->"<row r=\"${r+1}\">"+row.mapIndexed{c,v->"<c r=\"${column(c+1)}${r+1}\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${esc(v)}</t></is></c>"}.joinToString("")+"</row>"}.joinToString("")+"</sheetData></worksheet>").toByteArray()
        w.writeCopy(out)
    }
}
