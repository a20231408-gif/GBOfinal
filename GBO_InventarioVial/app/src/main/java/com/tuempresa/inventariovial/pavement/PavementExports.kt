package com.tuempresa.inventariovial.pavement

import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.tuempresa.inventariovial.camera.loadCorrectlyOrientedBitmap
import com.tuempresa.inventariovial.pavement.ui.PavementDrawing
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class PavementExportSample(val sample: PavementSampleEntity, val damages: List<PavementDamageEntity>, val result: PavementResult)

object PavementExcel {
    val headers=listOf("Progresiva Inicial","Progresiva Final","Daño","Severidad","X","Y","Ancho","Longitud","Ancho de Fisura","Observación","Carril","Ancho Carril","Inspeccionado")
    fun write(output:OutputStream, samples:List<PavementExportSample>, sections:List<PavementSectionEntity> = emptyList()) {
        require(samples.map{it.sample.surface}.distinct().size==1) { "Exporta una superficie por archivo." }
        val rows=mutableListOf<List<Any?>>(headers)
        val summary=mutableListOf<List<Any?>>(listOf("Tramo","Muestra","Fecha","Superficie","Área (m²)","Estado","PCI/URCI","Calificación","Pendientes"))
        val detail=mutableListOf<List<Any?>>(listOf("Tramo","Muestra","Correlativo","Código","Severidad","X2","Y2","Diámetro","Losas","Cantidad","Densidad (%)","VD"))
        samples.forEach { (s,damages,r) ->
            damages.forEach { d ->
                rows.add(listOf(PavementChainage.display(s.prInicial),PavementChainage.display(s.prFinal),"${d.code} ${d.name}",PavementSeverity.canonical(d.severity)?:"N/A",d.x1,d.y1,d.widthM,d.lengthM,d.crackWidthMm,d.observation,s.carril,s.laneWidthM,d.inspectorName.ifBlank{s.inspectorNombre}))
                val t=r.totals.firstOrNull{it.code==d.code && PavementSeverity.canonical(it.severity)==PavementSeverity.canonical(d.severity)}
                detail.add(listOf(s.tramo,s.muestra,d.correlativo,d.code,PavementSeverity.canonical(d.severity),d.x2,d.y2,d.diameterM,d.slabs,PavementRules.quantity(d),t?.density,t?.vd))
            }
            summary.add(listOf(s.tramo,s.muestra,s.fecha,PavementSurface.valueOf(s.surface).label,s.anchoM*s.largoM,if(s.status=="CLOSED")"Cerrada" else "Borrador",r.index,r.rating,r.pending.joinToString("; ")))
        }
        samples.groupBy{it.sample.ruta to it.sample.tramo}.forEach { (key,group) ->
            val area=group.sumOf{it.sample.anchoM*it.sample.largoM}
            val section=sections.firstOrNull{it.ruta==key.first && it.tramo==key.second && it.surface==group.first().sample.surface}
            val result=PavementSectionCalculation.calculate(group,section)
            summary.add(listOf("${key.first} / ${key.second}",if(section==null)"MUESTRAS EXPORTADAS" else "SECCIÓN","",group.first().sample.surface,section?.areaM2?:area,"",result.index,result.index?.let{PciCorrection.rating(it)},result.explanation))
        }
        workbook(output,listOf("Fallas" to rows,"Resumen" to summary,"Detalle" to detail))
    }
    private fun xml(s:String)=s.filter{it=='\n'||it=='\r'||it=='\t'||it.code>=32}.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;")
    internal fun workbook(out:OutputStream,sheets:List<Pair<String,List<List<Any?>>>>, locationGroups: Boolean = false) {
        val main="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
        val rel="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
        val pkg="http://schemas.openxmlformats.org/package/2006/relationships"
        ZipOutputStream(out).use { zip ->
            fun entry(path:String,body:String){zip.putNextEntry(ZipEntry(path));zip.write(("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+body).toByteArray());zip.closeEntry()}
            entry("[Content_Types].xml","<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"+sheets.indices.joinToString(""){"<Override PartName=\"/xl/worksheets/sheet${it+1}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"}+"</Types>")
            entry("_rels/.rels","<Relationships xmlns=\"$pkg\"><Relationship Id=\"rId1\" Type=\"$rel/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>")
            entry("xl/workbook.xml","<workbook xmlns=\"$main\" xmlns:r=\"$rel\"><sheets>"+sheets.mapIndexed{i,s->"<sheet name=\"${xml(s.first)}\" sheetId=\"${i+1}\" r:id=\"rId${i+1}\"/>"}.joinToString("")+"</sheets></workbook>")
            entry("xl/_rels/workbook.xml.rels","<Relationships xmlns=\"$pkg\">"+sheets.indices.joinToString(""){"<Relationship Id=\"rId${it+1}\" Type=\"$rel/worksheet\" Target=\"worksheets/sheet${it+1}.xml\"/>"}+"<Relationship Id=\"rIdStyles\" Type=\"$rel/styles\" Target=\"styles.xml\"/></Relationships>")
            entry("xl/styles.xml","<styleSheet xmlns=\"$main\"><fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Arial\"/></font><font><b/><sz val=\"11\"/><name val=\"Arial\"/></font></fonts><fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill><fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFC4D69B\"/><bgColor indexed=\"64\"/></patternFill></fill></fills><borders count=\"2\"><border/><border><left style=\"thin\"/><right style=\"thin\"/><top style=\"thin\"/><bottom style=\"thin\"/></border></borders><cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs><cellXfs count=\"2\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyAlignment=\"1\"><alignment vertical=\"top\" wrapText=\"1\"/></xf><xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"1\" xfId=\"0\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\" wrapText=\"1\"/></xf></cellXfs><cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles></styleSheet>")
            sheets.forEachIndexed { i,(_,rows) ->
                val headerRows=if(locationGroups)2 else 1
                val sourceRows=if(locationGroups)listOf(listOf("","","","Ubicación inicio","","Ubicación fin","","","","","","",""))+rows else rows
                val widths=if(locationGroups)listOf(14,10,8,12,12,12,12,32,12,12,12,12,14)else List(rows.first().size){24}
                val cols=widths.mapIndexed { index,width -> "<col min=\"${index+1}\" max=\"${index+1}\" width=\"$width\" customWidth=\"1\"/>" }.joinToString("")
                entry("xl/worksheets/sheet${i+1}.xml","<worksheet xmlns=\"$main\"><sheetPr><pageSetUpPr fitToPage=\"1\"/></sheetPr><sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"$headerRows\" topLeftCell=\"A${headerRows+1}\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews><cols>$cols</cols><sheetData>"+sourceRows.mapIndexed{r,cells->"<row r=\"${r+1}\"${if(r<headerRows)" ht=\"32\" customHeight=\"1\"" else ""}>"+cells.joinToString(""){v->val style=if(r<headerRows)1 else 0;if(v is Number)"<c s=\"$style\"><v>$v</v></c>" else "<c s=\"$style\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${xml(v?.toString().orEmpty())}</t></is></c>"}+"</row>"}.joinToString("")+"</sheetData>"+(if(locationGroups)"<mergeCells count=\"2\"><mergeCell ref=\"D1:E1\"/><mergeCell ref=\"F1:G1\"/></mergeCells>" else "")+"<pageMargins left=\"0.25\" right=\"0.25\" top=\"0.5\" bottom=\"0.5\" header=\"0.2\" footer=\"0.2\"/><pageSetup paperSize=\"9\" orientation=\"landscape\" fitToWidth=\"1\" fitToHeight=\"0\"/></worksheet>")
            }
        }
    }
}

object PavementPdf {
    fun write(output:OutputStream, data:PavementExportSample, photos:Map<String,List<PavementDamagePhotoEntity>>) {
        val doc=PdfDocument()
        var current:PdfDocument.Page?=null
        var index=0
        try {
            render(data,photos,{
                doc.startPage(PdfDocument.PageInfo.Builder(595,842,++index).create()).also{current=it}.canvas
            },{ current?.let{doc.finishPage(it)};current=null })
            doc.writeTo(output)
        } finally { current?.let{doc.finishPage(it)};doc.close() }
    }
    /** Same page renderer is used for PDF output and bitmap layout verification. */
    fun render(data:PavementExportSample, photos:Map<String,List<PavementDamagePhotoEntity>>, startPage:()->Canvas, finishPage:()->Unit) {
            val (s,damages,r)=data
            fun fmt(value:Double?)=value?.let{String.format(java.util.Locale.US,"%.3f",it).trimEnd('0').trimEnd('.')}?:"—"
            var page:Canvas?=null;var y=45f;var pageNumber=0
            val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{textSize=10f;color=Color.BLACK}
            fun next(){if(page!=null)finishPage();page=startPage();pageNumber++;y=45f;page!!.drawText("GBO · Inspección de pavimentos · ${s.tramo.take(45)} · pág. $pageNumber",30f,25f,paint)}
            fun room(height:Float){if(page==null || y+height>810)next()}
            fun line(text:String){
                var rest=text
                do { val count=paint.breakText(rest,true,535f,null).coerceAtLeast(1).coerceAtMost(rest.length);room(15f);page!!.drawText(rest.take(count),30f,y,paint);y+=15;rest=rest.drop(count) } while(rest.isNotEmpty())
            }
            line("${PavementSurface.valueOf(s.surface).label} · muestra ${s.muestra} · ${s.fecha} · ${if(s.status=="CLOSED")"Cerrada" else "Borrador"}")
            line("Inspector: ${s.inspectorNombre}; carril: ${s.carril}; ruta: ${s.ruta}")
            line("Progresiva ${PavementChainage.display(s.prInicial)} → ${PavementChainage.display(s.prFinal)}; ${s.anchoM} × ${s.largoM} m; área ${s.anchoM*s.largoM} m²")
            fun wrap(text: String, width: Float): List<String> {
                if(text.isEmpty())return listOf("")
                val result=mutableListOf<String>();var rest=text.replace('\n',' ')
                while(rest.isNotEmpty()) { val count=paint.breakText(rest,true,width,null).coerceAtLeast(1).coerceAtMost(rest.length);result+=rest.take(count);rest=rest.drop(count) }
                return result
            }
            fun table(rows: List<List<String>>) {
                val widths=listOf(28f,36f,36f,160f,75f,55f,45f)
                val heads=listOf("Ítem","X (m)","Y (m)","Daño","Severidad","Cantidad","Unidad")
                fun row(cells: List<String>, header: Boolean) {
                    val lines=cells.mapIndexed { i,text -> wrap(text,widths[i]-8) }
                    val height=lines.maxOf { it.size }*13f+8f
                    if(y+height>810) { next();row(heads,true) }
                    room(height)
                    var x=30f
                    cells.indices.forEach { i ->
                        paint.style=Paint.Style.FILL;paint.color=if(header)Color.rgb(225,235,244) else Color.WHITE
                        page!!.drawRect(x,y,x+widths[i],y+height,paint)
                        paint.style=Paint.Style.STROKE;paint.color=Color.LTGRAY;paint.strokeWidth=.6f
                        page!!.drawRect(x,y,x+widths[i],y+height,paint)
                        paint.style=Paint.Style.FILL;paint.color=Color.BLACK
                        lines[i].forEachIndexed { j,text -> page!!.drawText(text,x+4,y+14+j*13,paint) };x+=widths[i]
                    }
                    y+=height
                }
                room(30f);row(heads,true);rows.forEach { row(it,false) };y+=8
            }
            if(s.anchoM>0 && s.largoM>0){
                val segments=PavementPdfLayout.segments(s.anchoM,s.largoM)
                for(segment in segments) {
                    val present=damages.filter { segment.contains(s,it) }
                    val scale=minOf(20.0,420/s.anchoM).toFloat()
                    val height=(segment.length*scale).toFloat()
                    room(height+65)
                    line("Esquema ${segment.index}/${segment.count}: ${fmt(segment.start)}–${fmt(segment.end)} m · X: 0–${fmt(s.anchoM)} m")
                    val c=page!!;val top=y+8;val left=72f;val width=(s.anchoM*scale).toFloat()
                    c.save();c.clipRect(left,top,left+width,top+height);c.translate(left,top-((s.largoM-segment.end)*scale).toFloat())
                    PavementDrawing.draw(c,s,present,scale,labels=false);c.restore()
                    paint.color=Color.BLACK;paint.style=Paint.Style.STROKE;c.drawRect(left,top,left+width,top+height,paint);paint.style=Paint.Style.FILL
                    val step=PavementRuler.labelStep(segment.length,height,24f)
                    for(tick in PavementRuler.ticks(s.largoM).filter { it>=segment.start && it<=segment.end && (it%step==0.0 || it==segment.start || it==segment.end) }) {
                        val yy=top+((segment.end-tick)*scale).toFloat();c.drawLine(left-5,yy,left,yy,paint);c.drawText(fmt(tick),32f,yy+3,paint)
                    }
                    // Number labels are packed in a separate margin with leaders; no overlapping text on geometry.
                    val local=present.filter{it.geometry!="GLOBAL"}.sortedByDescending { it.y1 ?: PavementRules.slabIds(it).minOrNull()?.let { n -> (n-1)/s.slabColumns*s.largoM/s.slabRows } ?: 0.0 }
                    val labelsPerColumn=(height/13).toInt().coerceAtLeast(1)
                    val columns=kotlin.math.ceil(local.size.toDouble()/labelsPerColumn).toInt().coerceAtLeast(1)
                    val columnWidth=(565-left-width-10)/columns
                    val dense=local.size>labelsPerColumn
                    var lastLabelY=top-1
                    local.forEachIndexed { i,d ->
                        val metricY=d.y1 ?: PavementRules.slabIds(d).minOrNull()?.let { n -> ((n-1)/s.slabColumns+.5)*s.largoM/s.slabRows } ?: 0.0
                        val anchorY=(top+((segment.end-metricY)*scale).toFloat()).coerceIn(top,top+height)
                        val metricX=d.x1 ?: s.anchoM/2
                        paint.color=PavementSeverity.color(d.severity)
                        val labelY=if(dense)top+12+(i%labelsPerColumn)*13 else maxOf(anchorY+3,lastLabelY+13).coerceAtMost(top+height-(local.size-i-1)*13)
                        lastLabelY=labelY
                        val labelX=left+width+10+(i/labelsPerColumn)*columnWidth
                        if(!dense)c.drawLine(left+(metricX*scale).toFloat(),anchorY,labelX-2,labelY-3,paint)
                        c.drawRect(labelX-1,labelY-10,labelX+paint.measureText("#${d.correlativo}")+2,labelY+2,paint)
                        paint.color=if(PavementSeverity.canonical(d.severity)=="H")Color.WHITE else Color.BLACK
                        c.drawText("#${d.correlativo}",labelX,labelY,paint)
                    }
                    paint.color=Color.BLACK;y=top+height+18
                    line(if(dense)"Ítems en leyenda; posiciones X/Y en tabla. Figuras recortadas al segmento."else"Y en metros; origen inferior izquierdo. Figuras recortadas al segmento.")
                    table(present.map { d -> listOf("#${d.correlativo}",fmt(d.x1),fmt(d.y1),"${d.code} · ${d.name}",PavementSeverity.label(d.severity),fmt(PavementRules.quantity(d)),PavementCatalog.forSurface(PavementSurface.valueOf(s.surface)).first { it.code==d.code }.unit ?: "Global") })
                }
            }
            line("Registro completo · cantidades medidas por daño (no sumar las repeticiones de un daño que cruza segmentos).")
            table(damages.map { d -> listOf("#${d.correlativo}",fmt(d.x1),fmt(d.y1),"${d.code} · ${d.name}",PavementSeverity.label(d.severity),fmt(PavementRules.quantity(d)),PavementCatalog.forSurface(PavementSurface.valueOf(s.surface)).first { it.code==d.code }.unit ?: "Global") })
            for(d in damages) {
                if(d.geometry=="SLABS")line("#${d.correlativo} · Losas afectadas: ${d.slabs}")
                if(!d.observation.isNullOrBlank())line("#${d.correlativo}: ${d.observation}")
                photos[d.id].orEmpty().forEach { photo ->
                    val bitmap=loadCorrectlyOrientedBitmap(photo.localPath)
                    if(bitmap==null) line("#${d.correlativo}: foto no disponible ${photo.id}") else {
                        try {
                            val scale=minOf(250f/bitmap.width,180f/bitmap.height);val w=bitmap.width*scale;val h=bitmap.height*scale
                            room(h+35);line("#${d.correlativo} · fotografía");page!!.drawBitmap(bitmap,null,RectF(30f,y,30f+w,y+h),paint);y+=h+20
                        } finally {bitmap.recycle()}
                    }
                }
            }
            line("Totales por daño y severidad: cantidad | densidad (%) | VD")
            r.totals.forEach{line("${it.code} ${it.severity?:"N/A"}: ${fmt(it.quantity)} | ${if(it.density==null)"Global" else fmt(it.density)} | ${if(it.vd==null)"Pendiente" else fmt(it.vd)}")}
            line("PCI/URCI: ${r.index?:"No calculable"}; ${r.rating.orEmpty()}")
            r.pending.forEach{line(it)}
            PavementRules.warnings(s,damages).forEach{line("Advertencia: $it")}
            if(page!=null)finishPage()
    }
}
