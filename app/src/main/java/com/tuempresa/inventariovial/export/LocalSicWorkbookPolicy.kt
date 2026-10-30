package com.tuempresa.inventariovial.export

import com.tuempresa.inventariovial.scap.export.*
import org.w3c.dom.Element
import java.io.ByteArrayOutputStream

/** Local rows/properties only. No remote workbook, source template or Room capture is modified. */
object LocalSicWorkbookPolicy {
    fun primary17Values(s:com.tuempresa.inventariovial.data.entity.InventorySnapshot):Map<String,Any?>{
        val r=s.record
        val detail=com.tuempresa.inventariovial.server.ServerPayload.encode(s).optJSONObject("sic")?.optJSONObject("SIC-17") ?: org.json.JSONObject()
        val raw=LocalSic17Workbook.keys.associateWith{key->when(key){
            "routeCode"->r.routeCode;"roadbedCode"->r.roadbedCode;"startPrCode"->r.startPrCode
            "startDistanceM"->if(r.startPrCode.isNotBlank())r.startDistanceM.toString()else ""
            "endPrCode"->r.endPrCode.orEmpty();"endDistanceM"->if(!r.endPrCode.isNullOrBlank())r.endDistanceM?.toString().orEmpty()else ""
            "surveyDate"->r.surveyDate;else->detail.opt(key).takeUnless{it==org.json.JSONObject.NULL}?.toString().orEmpty()
        }}
        val typed=LocalSic17Workbook.keys.mapIndexed{i,key->key to raw[key]?.takeIf{it.isNotBlank()}?.let{value->when(SicExportFormat.SIC17.columns[i].kind){
            CellKind.TEXT->value;CellKind.INTEGER->value.toIntOrNull();CellKind.DECIMAL->value.replace(',','.').toDoubleOrNull()?.takeIf{it.isFinite()}
            CellKind.DATE->runCatching{java.time.LocalDate.parse(value,java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT))}.getOrNull()
        }}}.toMap()
        return typed
    }
    fun primary17(out:java.io.OutputStream,s:com.tuempresa.inventariovial.data.entity.InventorySnapshot){
        SicExcelWriter.writeLocalSic17Row(out,s.record.routeCode,primary17Values(s),s.record.observations)
    }
    suspend fun selection(out:java.io.OutputStream,db:com.tuempresa.inventariovial.data.database.InventoryDatabase,
        records:List<com.tuempresa.inventariovial.data.entity.InventoryRecordForExport>,format:SicExportFormat?,heading:ExportHeading){
        if(format==null){
            java.util.zip.ZipOutputStream(out).use{zip->
                for(f in SicExportFormat.entries){
                    val selected=records.filter{f.appliesTo(it)};if(selected.isEmpty())continue
                    val b=ByteArrayOutputStream();selection(b,db,selected,f,heading)
                    zip.putNextEntry(java.util.zip.ZipEntry("${f.code}.xlsx"));zip.write(b.toByteArray());zip.closeEntry()
                }
            };return
        }
        if(format!=SicExportFormat.SIC17){SicExcelWriter.write(out,records,format,heading);return}
        val snapshots=records.filter{format.appliesTo(it)}.map{requireNotNull(db.inventoryDao().snapshot(it.record.id))}
        require(snapshots.isNotEmpty())
        val raw=ByteArrayOutputStream();SicExcelWriter.writeLocalSic17Rows(raw,snapshots.map{it.record to primary17Values(it)},heading)
        val pending=snapshots.count{primary17Values(it).values.any{v->v==null}||it.record.status!="ACTIVE"}
        out.write(status(raw.toByteArray(),pending>0,pending))
    }
    private fun children(e:Element)=(0 until e.childNodes.length).mapNotNull{e.childNodes.item(it) as? Element}
    private fun hasValue(e:Element)=children(e).any{it.localName=="f"||it.textContent.isNotBlank()}
    fun retainKnown(previous:ByteArray,current:ByteArray,format:String):Pair<ByteArray,Int> {
        // Reuse the exact structural compatibility checks, never line up unrelated templates.
        SicRouteWorkbook.merge(listOf(previous,current),format)
        val old=TemplateWorkbook(previous.inputStream());val next=TemplateWorkbook(current.inputStream())
        val row=next.sheet(1).nodes("row").single{it.getAttribute("r")==SicRouteWorkbook.start(format).toString()}
        val prior=children(old.sheet(1).nodes("row").single{it.getAttribute("r")==SicRouteWorkbook.start(format).toString()}).associateBy{it.getAttribute("r")}
        val rowNumber=SicRouteWorkbook.start(format)
        val rowPattern=Regex("""<row\b[^>]*\br="$rowNumber"[^>]*>.*?</row>""",RegexOption.DOT_MATCHES_ALL)
        val cellPattern=Regex("""<c\b[^>]*\br="([A-Z]+)$rowNumber"[^>]*?(?:/>|(?<!/)>.*?</c>)""",RegexOption.DOT_MATCHES_ALL)
        val path="xl/worksheets/sheet1.xml"
        val oldXml=old.parts.getValue(path).toString(Charsets.UTF_8)
        val xml=next.parts.getValue(path).toString(Charsets.UTF_8)
        val oldRow=rowPattern.findAll(oldXml).single().value
        val newRow=rowPattern.findAll(xml).single().value
        val rawPrior=cellPattern.findAll(oldRow).associate{it.groupValues[1]+rowNumber to it.value}
        val empty=children(row).filter{!hasValue(it)}.map{it.getAttribute("r")}.toSet()
        var retained=0
        val patched=cellPattern.replace(newRow){m->
            val ref=m.groupValues[1]+rowNumber
            val before=prior[ref]
            if(ref in empty && before!=null && hasValue(before)) {retained++;rawPrior.getValue(ref)}else m.value
        }
        // Alter only data cells. Serializing a DOM would also reorder header attributes,
        // invalidating the existing strict SIC-17 historical-template recognizer.
        next.parts[path]=rowPattern.replace(xml){patched}.toByteArray()
        val bytes=ByteArrayOutputStream().also{out->java.util.zip.ZipOutputStream(out).use{zip->
            next.parts.forEach{(name,data)->zip.putNextEntry(java.util.zip.ZipEntry(name).apply{time=0});zip.write(data);zip.closeEntry()}
        }}.toByteArray()
        return bytes to retained
    }

    /** Mark the actual XLSX without altering any contractual worksheet cell, formula or style. */
    fun status(bytes:ByteArray,provisional:Boolean,pending:Int):ByteArray {
        if(!provisional)return bytes
        val w=TemplateWorkbook(bytes.inputStream())
        val ns="http://schemas.openxmlformats.org/officeDocument/2006/custom-properties"
        val vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes"
        if("docProps/custom.xml" !in w.parts)w.parts["docProps/custom.xml"]="""<?xml version="1.0" encoding="UTF-8"?><Properties xmlns="$ns" xmlns:vt="$vt"/>""".toByteArray()
        val props=w.document("docProps/custom.xml")
        for((key,value)in listOf("Estado GBO" to ProvisionalSicExcel.MARKER,"Pendientes GBO" to pending.toString())){
            val item=props.nodes("property").firstOrNull{it.getAttribute("name")==key} ?: props.createElementNS(ns,"property").apply{
                setAttribute("fmtid","{D5CDD505-2E9C-101B-9397-08002B2CF9AE}");setAttribute("pid",((props.nodes("property").maxOfOrNull{it.getAttribute("pid").toIntOrNull() ?: 1} ?: 1)+1).toString());setAttribute("name",key);props.documentElement.appendChild(this)}
            while(item.hasChildNodes())item.removeChild(item.firstChild)
            item.appendChild(props.createElementNS(vt,"vt:lpwstr").apply{textContent=value})
        }
        val relationships=w.document("_rels/.rels");val type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/custom-properties"
        if(relationships.nodes("Relationship").none{it.getAttribute("Type")==type})relationships.documentElement.appendChild(relationships.createElementNS("http://schemas.openxmlformats.org/package/2006/relationships","Relationship").apply{setAttribute("Id","rIdGboLocalStatus");setAttribute("Type",type);setAttribute("Target","docProps/custom.xml")})
        val types=w.document("[Content_Types].xml")
        if(types.nodes("Override").none{it.getAttribute("PartName")=="/docProps/custom.xml"})types.documentElement.appendChild(types.createElementNS("http://schemas.openxmlformats.org/package/2006/content-types","Override").apply{setAttribute("PartName","/docProps/custom.xml");setAttribute("ContentType","application/vnd.openxmlformats-officedocument.custom-properties+xml")})
        val rendered=ByteArrayOutputStream().also{w.writeCopy(it)}.toByteArray()
        val parts=TemplateWorkbook(rendered.inputStream()).parts
        return ByteArrayOutputStream().also{out->java.util.zip.ZipOutputStream(out).use{zip->
            parts.forEach{(path,data)->zip.putNextEntry(java.util.zip.ZipEntry(path).apply{time=0});zip.write(data);zip.closeEntry()}
        }}.toByteArray()
    }
}
