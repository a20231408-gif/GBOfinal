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
                doc.startPage(PdfDocument.PageInfo.Builder(PavementPdfLayout.PAGE_WIDTH,PavementPdfLayout.PAGE_HEIGHT,++index).create()).also{current=it}.canvas
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
            fun room(height:Float){if(page==null || y+height>565)next()}
            fun line(text:String){
                var rest=text
                do { val count=paint.breakText(rest,true,782f,null).coerceAtLeast(1).coerceAtMost(rest.length);room(15f);page!!.drawText(rest.take(count),30f,y,paint);y+=15;rest=rest.drop(count) } while(rest.isNotEmpty())
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
            fun table(rows: List<List<String>>, references:List<PavementDamageEntity> = emptyList(), caption:String?=null) {
                val widths=listOf(32f,58f,58f,310f,90f,92f,102f)
                val heads=listOf("Ítem","X (m)","Y (m)","Daño","Severidad","Cantidad","Unidad")
                fun row(cells: List<String>, header: Boolean, damage:PavementDamageEntity?=null) {
                    val lines=cells.mapIndexed { i,text -> wrap(text,widths[i]-8) }
                    val height=lines.maxOf { it.size }*13f+8f
                    if(y+height>565) { next();caption?.let{line(it)};row(heads,true) }
                    room(height)
                    var x=30f
                    cells.indices.forEach { i ->
                        paint.style=Paint.Style.FILL;paint.color=if(header)Color.rgb(225,235,244) else Color.WHITE
                        page!!.drawRect(x,y,x+widths[i],y+height,paint)
                        if(i==0 && !header && s.surface=="RIGID" && damage!=null) {
                            paint.color=PavementSeverity.color(damage.severity)
                            page!!.drawRect(x+1,y+2,x+4,y+height-2,paint)
                        }
                        paint.style=Paint.Style.STROKE;paint.color=Color.LTGRAY;paint.strokeWidth=.6f
                        page!!.drawRect(x,y,x+widths[i],y+height,paint)
                        paint.style=Paint.Style.FILL;paint.color=Color.BLACK
                        lines[i].forEachIndexed { j,text -> page!!.drawText(text,x+if(i==0 && s.surface=="RIGID" && damage!=null)6 else 4,y+14+j*13,paint) };x+=widths[i]
                    }
                    y+=height
                }
                val firstHeight=rows.firstOrNull()?.mapIndexed{i,text->wrap(text,widths[i]-8).size}?.maxOrNull()?.let{it*13f+8f} ?: 0f
                room(if(s.surface=="RIGID" && rows.isNotEmpty())21f+firstHeight+(if(caption==null)0f else 15f)else 30f)
                caption?.let{line(it)};row(heads,true);rows.forEachIndexed {i,cells->row(cells,false,references.getOrNull(i))};y+=8
            }
            if(s.anchoM>0 && s.largoM>0 && s.surface=="RIGID" && s.slabRows>0 && s.slabColumns>0) {
                val plans=PavementRigidPdfScheme.plans(s,damages)
                plans.forEachIndexed {i,p->
                    if(i>0) {
                        next()
                        line("${PavementSurface.valueOf(s.surface).label} · muestra ${s.muestra} · ${s.fecha} · ${if(s.status=="CLOSED")"Cerrada"else"Borrador"}")
                        line("Inspector: ${s.inspectorNombre}; carril: ${s.carril}; ruta: ${s.ruta}")
                        line("Progresiva ${PavementChainage.display(s.prInicial)} → ${PavementChainage.display(s.prFinal)}; ${s.anchoM} × ${s.largoM} m; área ${s.anchoM*s.largoM} m²")
                    }
                    val v=p.region
                    val pr=PavementPdfLayout.chainages(s,PavementPdfSegment(v.index,v.count,v.y0,v.y1))
                    line("Vista ${v.index}/${v.count} · Y ${fmt(v.y0)}–${fmt(v.y1)} m · X ${fmt(v.x0)}–${fmt(v.x1)} m"+
                        (if(p.count>1)" · Panel ${p.index}/${p.count}"else"")+ (pr?.let{" · PR $it"} ?: " · PR de muestra, sin inferir proyección"))
                    if(y>115f) {
                        next();line("Rígido · muestra ${s.muestra} · vista ${v.index}/${v.count} · panel ${p.index}/${p.count}")
                        line("Y ${fmt(v.y0)}–${fmt(v.y1)} m · X ${fmt(v.x0)}–${fmt(v.x1)} m · "+(pr?.let{"PR $it"} ?: "PR de muestra: ${s.prInicial} → ${s.prFinal}"))
                    }
                    // Page starts are deliberate: a large plot never inherits the remaining table space.
                    y=PavementRigidPdfScheme.draw(page!!,s,p,56f,y)
                    line("#ítem código/sev · L baja, M media, H alta, - no aplica · Llamados por zona · Y → X ↓ absolutos · Losas sin renumerar.")
                    if(p.damages.isNotEmpty())table(p.damages.map {d->listOf("#${d.correlativo}",fmt(d.x1),fmt(d.y1),"${d.code} · ${d.name}",PavementSeverity.label(d.severity),fmt(PavementRules.quantity(d)),PavementCatalog.forSurface(PavementSurface.valueOf(s.surface)).first{it.code==d.code}.unit ?: "Global")},p.damages,"Tabla de vista ${v.index}/${v.count} · panel ${p.index}/${p.count} · solo estos ítems, en orden de los distintivos superiores e inferiores")
                }
            } else if(s.anchoM>0 && s.largoM>0){
                val segments=PavementPdfLayout.segments(s.anchoM,s.largoM,slabLength=if(s.surface=="RIGID" && s.slabRows>0)s.largoM/s.slabRows else null)
                val scale=PavementPdfLayout.scale(s.anchoM).toFloat()
                for(segment in segments) {
                    val present=damages.filter { segment.contains(s,it) }
                    val height=(s.anchoM*scale).toFloat()
                    val width=(segment.length*scale).toFloat()
                    val local=present.filter {it.geometry!="GLOBAL"}.sortedBy {it.y1 ?: 0.0}
                    // Very dense legends wrap with normal text pagination, never outside the sheet.
                    val labelColumns=(width/42).toInt().coerceAtLeast(1)
                    val compactLabels=local.size<=labelColumns*3
                    val labelHeight=if(compactLabels)kotlin.math.ceil(local.size.toDouble()/labelColumns).toFloat()*14 else 0f
                    room(height+90+labelHeight)
                    line("Esquema ${segment.index}/${segment.count}: Y ${fmt(segment.start)}–${fmt(segment.end)} m → · X 0–${fmt(s.anchoM)} m ↓")
                    line(PavementPdfLayout.chainages(s,segment)?.let {"PR de sección: $it"} ?: "PR de referencia: ${PavementChainage.display(s.prInicial)} → ${PavementChainage.display(s.prFinal)} (muestra completa; sin inferir PR por sección)")
                    val c=page!!;val top=y+10;val left=64f
                    c.save();c.clipRect(left,top,left+width,top+height)
                    c.translate(left+((s.largoM-segment.start)*scale).toFloat(),top);c.rotate(90f)
                    PavementDrawing.draw(c,s,present,scale,labels=false,slabLabels=false);c.restore()
                    paint.color=Color.BLACK;paint.style=Paint.Style.STROKE;c.drawRect(left,top,left+width,top+height,paint);paint.style=Paint.Style.FILL
                    val step=PavementRuler.labelStep(segment.length,width,36f)
                    val ticks=(PavementRuler.ticks(s.largoM).filter {it>segment.start && it<segment.end && it%step==0.0}+listOf(segment.start,segment.end)).distinct().sorted()
                    paint.textSize=9f
                    for(tick in ticks) {
                        val xx=left+((tick-segment.start)*scale).toFloat()
                        if(tick!=segment.start && tick!=segment.end && minOf(xx-left,left+width-xx)<28)continue
                        c.drawLine(xx,top+height,xx,top+height+4,paint);c.drawText(fmt(tick),xx-5,top+height+15,paint)
                    }
                    c.drawText("X=0",30f,top+4,paint);c.drawText("${fmt(s.anchoM)} m",25f,top+height,paint)
                    // Slab numbers stay upright after rotating geometry. IDs remain global to the sample.
                    if(s.surface=="RIGID" && s.slabRows>0 && s.slabColumns>0) {
                        val slabH=s.largoM/s.slabRows;val slabW=s.anchoM/s.slabColumns
                        for(row in 0 until s.slabRows)for(col in 0 until s.slabColumns) {
                            val yy=(row+.85)*slabH
                            if(yy>=segment.start && yy<segment.end) {
                                val pos=PavementPdfLayout.point((col+.85)*slabW,yy,segment,scale.toDouble())
                                val text="L${row*s.slabColumns+col+1}";val tx=left+pos.x.toFloat()-7;val ty=top+pos.y.toFloat()+3
                                paint.color=Color.WHITE;c.drawRect(tx-1,ty-9,tx+paint.measureText(text)+1,ty+2,paint)
                                paint.color=Color.BLACK;c.drawText(text,tx,ty,paint)
                            }
                        }
                    }
                    if(compactLabels) local.forEachIndexed {i,d ->
                        val n=PavementRules.slabIds(d).minOrNull() ?: 1
                        val ax=d.x1 ?: ((n-1)%s.slabColumns.coerceAtLeast(1)+.5)*s.anchoM/s.slabColumns.coerceAtLeast(1)
                        val ay=d.y1 ?: ((n-1)/s.slabColumns.coerceAtLeast(1)+.5)*s.largoM/s.slabRows.coerceAtLeast(1)
                        val anchor=PavementPdfLayout.point(ax,ay,segment,scale.toDouble())
                        val labelX=left+(i%labelColumns)*width/labelColumns+2
                        val labelY=top+height+30+(i/labelColumns)*14
                        paint.color=Color.GRAY;paint.strokeWidth=.5f
                        c.drawLine(left+anchor.x.toFloat().coerceIn(0f,width),top+anchor.y.toFloat().coerceIn(0f,height),labelX+8,labelY-8,paint)
                        paint.color=Color.WHITE;c.drawRect(labelX-1,labelY-9,labelX+paint.measureText("#${d.correlativo}")+2,labelY+2,paint)
                        paint.color=Color.BLACK;c.drawText("#${d.correlativo}",labelX,labelY,paint)
                    }
                    paint.textSize=10f;y=top+height+30+labelHeight
                    line("Origen de sección: X=0, Y=${fmt(segment.start)} m. Misma escala en todas las secciones; Y aumenta hacia la derecha.")
                    // Indexed labels below the plot: never rotate text or overwrite neighbouring IDs.
                    // Coordinates in the table keep dense and cross-boundary figures unambiguous.
                    if(local.isNotEmpty())line("Daños del esquema: "+local.joinToString(" · ") {"#${it.correlativo}"})
                    if(segment.count>1)line("Un daño que continúa en otra sección conserva su número y no se vuelve a contabilizar.")
                    if(present.isEmpty())line("Sin daños registrados en esta sección.")
                    else table(present.map { d -> listOf("#${d.correlativo}",fmt(d.x1),fmt(d.y1),"${d.code} · ${d.name}",PavementSeverity.label(d.severity),fmt(PavementRules.quantity(d)),PavementCatalog.forSurface(PavementSurface.valueOf(s.surface)).first { it.code==d.code }.unit ?: "Global") })
                }
            }
            line("Registro completo · cantidades medidas por daño (no sumar las repeticiones de un daño que cruza segmentos).")
            table(damages.map { d -> listOf("#${d.correlativo}",fmt(d.x1),fmt(d.y1),"${d.code} · ${d.name}",PavementSeverity.label(d.severity),fmt(PavementRules.quantity(d)),PavementCatalog.forSurface(PavementSurface.valueOf(s.surface)).first { it.code==d.code }.unit ?: "Global") },damages)
            for(d in damages) {
                if(d.code==26) {
                    line("Daño 26 — Daño del sello de junta · ${PavementSeverity.label(d.severity)}")
                    line(PavementJointAnnotations.legend)
                    line(if(PavementJointAnnotations.needsReview(s,d.id))"Malla distinta: marcas conservadas pendientes de revisión; no se representan." else if(PavementJointAnnotations.marks(s,d.id).isEmpty())"Daño GLOBAL sin juntas marcadas." else "Referencias visuales: ${PavementJointAnnotations.marks(s,d.id).size} marcas (segmentos/nodos).")
                }
                if(d.code==22 && PavementSlabGeometry.localized(d)) {
                    val corner=runCatching {PavementCornerGeometry.resolve(s,d)}.getOrNull()
                    line(if(corner==null)"#${d.correlativo} · Rotura histórica: ubicación ambigua, revisar extremos antes de corregir." else "#${d.correlativo} · Extremos del croquis ajustados: (${fmt(corner.a.x)}, ${fmt(corner.a.y)}) a (${fmt(corner.b.x)}, ${fmt(corner.b.y)}) m; toques medidos conservados.")
                }
                if(d.geometry=="SLABS")line("#${d.correlativo} · Losas afectadas: ${d.slabs}")
                if(PavementSlabGeometry.localized(d))line("#${d.correlativo} · Ubicación ${PavementSlabGeometry.mark(d.code).name}: (${fmt(d.x1)}, ${fmt(d.y1)}) a (${fmt(d.x2)}, ${fmt(d.y2)}) m. Cantidad contractual: losas.")
                if(d.geometry=="SLABS" && !PavementSlabGeometry.localized(d))line("#${d.correlativo} · Símbolo por losa; sin ubicación parcial registrada.")
                if(PavementDamageFields.depth(d.code) && d.depthMm!=null)line("#${d.correlativo} · ${PavementDamageFields.depthLabel(d.code)}: ${fmt(d.depthMm)}")
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
            val closing=buildList {
                add("Totales por daño y severidad: cantidad | densidad (%) | VD")
                r.totals.forEach{add("${it.code} ${it.severity?:"N/A"}: ${fmt(it.quantity)} | ${if(it.density==null)"Global" else fmt(it.density)} | ${if(it.vd==null)"Pendiente" else fmt(it.vd)}")}
                add("PCI/URCI: ${r.index?:"No calculable"}; ${r.rating.orEmpty()}")
                addAll(r.pending);addAll(PavementRules.warnings(s,damages).map{"Advertencia: $it"})
            }
            if(s.surface=="RIGID")room(minOf(520f,closing.sumOf{wrap(it,782f).size}*15f))
            closing.forEach{line(it)}
            if(page!=null)finishPage()
    }
}
