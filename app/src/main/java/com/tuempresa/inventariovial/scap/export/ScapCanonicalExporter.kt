package com.tuempresa.inventariovial.scap.export

import com.tuempresa.inventariovial.scap.calculator.ScapPercentages
import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot
import com.tuempresa.inventariovial.scap.domain.ScapBridgeType
import com.tuempresa.inventariovial.scap.domain.ScapFieldPolicy
import com.tuempresa.inventariovial.scap.domain.ScapNumbers
import org.json.JSONArray
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Each contract owns its bounded rows and conclusions, with no layout reconstruction. */
internal sealed class ScapCanonicalLayout(val type:ScapBridgeType,val asset:String,
    val conditionRows:Map<String,List<Int>>,val conclusions:Map<String,String>) {
    object Slab:ScapCanonicalLayout(ScapBridgeType.SLAB,"scap_slab",
        linkedMapOf("SUPERESTRUCTURA" to (22..24).toList(),"SUBESTRUCTURA" to (26..28).toList(),
            "DETALLES" to (31..34).toList(),"CAUCE" to (36..38).toList(),"ACCESOS" to (41..44).toList()),
        mapOf("g2NormativeObservation" to "B19","g2NormativeRecommendation" to "E19",
            "g2PreventiveObservation" to "B29","g2PreventiveRecommendation" to "E29",
            "g2ExecutiveObservation" to "B43","g2ExecutiveRecommendation" to "E43"))
    object Modular:ScapCanonicalLayout(ScapBridgeType.MODULAR,"scap_modular",
        linkedMapOf("SUPERESTRUCTURA" to listOf(22),"SUBESTRUCTURA" to listOf(24,25),
            "DETALLES" to listOf(27,28),"CAUCE" to (30..32).toList(),"ACCESOS" to (34..37).toList()),
        mapOf("g2NormativeObservation" to "B28","g2NormativeRecommendation" to "E28",
            "g2PreventiveObservation" to "B49","g2PreventiveRecommendation" to "E49",
            "g2ExecutiveObservation" to "B60","g2ExecutiveRecommendation" to "E60"))
    companion object { fun forType(type:ScapBridgeType)=when(type) {ScapBridgeType.SLAB->Slab;ScapBridgeType.MODULAR->Modular} }
}

internal class ScapCanonicalExporter(private val template:()->InputStream,fieldMap:String,
    private val layout:ScapCanonicalLayout) {
    private data class Field(val key:String,val owner:String,val cell:String,val kind:String,val section:String,val visible:String?)
    private val fields=JSONArray(fieldMap).let {a->List(a.length()){i->a.getJSONObject(i).let {
        Field(it.getString("key"),it.getString("owner"),it.getString("valueCell"),it.getString("kind"),it.getString("section"),it.optString("visibleWhen").ifBlank{null})
    }}}
    fun review(s:ScapInspectionSnapshot):ScapExportReview {
        val errors=mutableListOf<String>()
        if(ScapBridgeType.from(s)!=layout.type) errors+="El tipo de ficha no corresponde a la plantilla seleccionada."
        val selected=com.tuempresa.inventariovial.scap.domain.ScapElementView.applicable(s)
        if(selected.size>31) errors+="G.1 admite 31 elementos en la plantilla canónica."
        if(selected.any {it.element.group !in layout.conditionRows}) errors+="Un elemento no tiene grupo contractual para F.1."
        if(s.spans.size>2) errors+="C.1/C.2 admite dos tramos; la plantilla canónica no contiene tableros adicionales."
        if(s.substructures.count {it.kind=="PIER"}>3) errors+="C.4 admite tres pilares independientes."
        if(s.joints.size>1) errors+="C.5 admite una junta."
        if(s.profile.size>15) errors+="D.4 admite 15 puntos de perfil."
        val deckKeys=fields.filter{it.owner=="span" && it.section=="C3"}.map{it.key}
        val principal=s.spans.minByOrNull {it.spanIndex}?.let{s.values(it.id)}.orEmpty()
        if(s.spans.sortedBy{it.spanIndex}.drop(1).any {span->deckKeys.any{key->s.values(span.id)[key]?.takeIf(String::isNotBlank)?.let{it!=principal[key]}==true}})
            errors+="C.3 admite un tablero: los datos distintos de un segundo tablero requieren una continuación aprobada del formato."
        if(s.photos.size>32) errors+="F.2 admite 32 fotografías."
        if(s.sketches.groupBy {it.type}.any {it.value.size>1}) errors+="E admite un croquis de cada tipo."
        return ScapExportReview(errors,listOf("Se conserva la plantilla canónica ${layout.type}. Excel recalculará las fórmulas al abrir.",
            "La fuente contiene referencias #REF! preexistentes; requieren revisión del responsable del formato."))
    }
    fun write(output:OutputStream,s:ScapInspectionSnapshot,media:(String)->ByteArray) {
        require(review(s).errors.isEmpty()){review(s).errors.joinToString("\n")}
        val w=template().use {TemplateWorkbook(it)}
        fun set(sheet:Int,ref:String,value:Any?) {
            // Derived formulas belong to the source and must remain intact.
            if(w.formula(sheet,ref)==null) w.set(sheet,ref,value)
        }
        val common=s.values()+mapOf("categoryName" to (s.values()["categoryName"] ?: s.values()["bridgeName"] ?: s.inspection.bridgeName),"bridgeName" to s.inspection.bridgeName,"bridgeCode" to s.inspection.bridgeCode,
            "latitude" to (s.values()["latitude"] ?: s.inspection.latitude?.toString().orEmpty()),
            "longitude" to (s.values()["longitude"] ?: s.inspection.longitude?.toString().orEmpty()))
        fun put(f:Field,ref:String,values:Map<String,String>) {
            val rule=f.visible
            val visible=rule==null || if("!=" in rule) values[rule.substringBefore("!=")]!=rule.substringAfter("!=") else values[rule.substringBefore('=')]==rule.substringAfter('=')
            val independentWidth=f.section=="B" && f.key=="sidewalkWidthM"
            val raw=(if(f.kind=="NUMERIC_OR_SOURCE_STATUS" || independentWidth) values[f.key].orEmpty() else ScapFieldPolicy.exportValue(f.key,f.owner,values)).takeIf{visible}.orEmpty().trim()
            val value:Any?=when {raw.isEmpty()->null; f.kind=="CHAINAGE"->ScapNumbers.chainage(raw)
                f.kind=="NUMERIC_OR_SOURCE_STATUS"->ScapNumbers.decimal(raw) ?: raw
                f.kind=="DATE"->runCatching{ChronoUnit.DAYS.between(LocalDate.of(1899,12,30),LocalDate.parse(raw,DateTimeFormatter.ofPattern("dd/MM/uuuu")))}.getOrNull()
                f.kind in setOf("DECIMAL","SIGNED_DECIMAL","INTEGER","YEAR","PERCENT","LATITUDE","LONGITUDE")->ScapNumbers.decimal(raw)
                f.owner=="bearing" && f.key=="location"->when(raw){"LEFT"->"Izquierda";"RIGHT"->"Derecha";else->raw}
                else->raw}
            set(1,ref,value)
        }
        fields.filter {it.owner=="inspection"}.forEach {f->put(f,f.cell,
            if(f.key in setOf("jointType","jointMaterial")) s.joints.firstOrNull()?.let{s.values(it.id)}.orEmpty() else common)}
        set(1,"E63",s.spans.size.takeIf {it>0})
        for(i in 0..2) set(1,"J${63+i*2}",s.spans.sortedBy {it.spanIndex}.getOrNull(i)?.lengthM)
        val spans=s.spans.sortedBy {it.spanIndex}
        for(i in 0..1) fields.filter {it.owner=="span" && it.key!="lengthM"}.forEach {f->
            if(i==0 || f.section=="C2") put(f,if(i==0) f.cell else f.cell.replace("E","N"),spans.getOrNull(i)?.let{s.values(it.id)}.orEmpty())
        }
        fun structures(owner:String,kinds:List<String>,columns:List<String>,sourceColumn:String) {
            kinds.forEachIndexed {i,kind->
                val row=if(kind=="PIER")s.substructures.filter {it.kind==kind}.sortedBy{it.elementIndex}.getOrNull(i) else s.substructures.firstOrNull{it.kind==kind}
                fields.filter {it.owner==owner && it.key!="soil"}.forEach {f->put(f,f.cell.replace(sourceColumn,columns[i]),row?.let{s.values(it.id)}.orEmpty())}
                val soil=when(kind){"LEFT_ABUTMENT"->"E393";"RIGHT_ABUTMENT"->"G393";"PIER"->listOf("J393","L393","N393")[i];else->null}
                soil?.let {set(1,it,row?.soil)}
            }
        }
        structures("abutment",listOf("LEFT_ABUTMENT","RIGHT_ABUTMENT"),listOf("J","N"),"J")
        structures("pier",List(3){"PIER"},listOf("G","J","N"),"G")
        structures("anchor",listOf("LEFT_ANCHOR","RIGHT_ANCHOR"),listOf("G","L"),"G")
        for(i in 0..1) fields.filter {it.owner=="bearing"}.forEach {f->put(f,if(i==0)f.cell else f.cell.replace("H","J"),s.supports.sortedBy{it.supportIndex}.getOrNull(i)?.let{s.values(it.id)}.orEmpty())}
        // Append source support blocks for support 3+, leaving all original coordinates untouched.
        val supportFields=fields.filter{it.owner=="bearing"}
        s.supports.sortedBy{it.supportIndex}.drop(2).forEach {support->
            val first=supportFields.minOf{it.cell.filter(Char::isDigit).toInt()}-2
            val last=supportFields.maxOf{it.cell.filter(Char::isDigit).toInt()}+1
            val destination=w.sheet(1).nodes("row").filter{it.namespaceURI==TemplateWorkbook.MAIN}.maxOf{it.getAttribute("r").toInt()}+3
            w.copyRows(1,first,last,destination,18)
            supportFields.forEach{f->
                val row=f.cell.filter(Char::isDigit).toInt()+destination-first
                put(f,f.cell.takeWhile(Char::isLetter)+row,s.values(support.id))
                put(f,"J$row",emptyMap())
            }
            set(1,"H$destination","Apoyo ${support.supportIndex}")
            set(1,"J$destination",null)
        }
        for(owner in listOf("ACCESS_LEFT","ACCESS_RIGHT")) fields.filter {it.owner=="access"}.forEach {f->
            val ref=if(owner=="ACCESS_LEFT")f.cell else if(f.section=="C6")f.cell.replace("G","N").replace("H","P") else f.cell.replace("G","L").replace("H","N")
            put(f,ref,s.values(owner))
        }
        // Replace every actual profile input; never export the source's example measurements.
        val profile=s.profile.sortedBy{it.pointIndex}
        for(i in 0..14) {
            fields.filter{it.owner=="profile"}.forEach{f->put(f,f.cell.replace("462",(462+i).toString()),profile.getOrNull(i)?.let{s.values(it.id)}.orEmpty())}
            for(col in listOf("U","W")) set(1,"$col${462+i}",null)
        }
        for(r in 456..458) for(col in listOf("U","V","X")) set(1,"$col$r",null)
        for(r in 483..497) for(col in listOf("G","H","J","L")) set(1,"$col$r",null)
        set(1,"E448",profile.size.takeIf{it>0});set(1,"C484",null)
        val selected=com.tuempresa.inventariovial.scap.domain.ScapElementView.applicable(s).sortedWith(compareBy({com.tuempresa.inventariovial.scap.domain.ScapPresentation.groups.indexOf(it.element.group)},{it.element.elementCode}))
        layout.conditionRows.values.flatten().forEach {r->for(col in listOf("B","C","F"))set(3,"$col$r",null)}
        layout.conditionRows.forEach {(group,rows)->
            val used=mutableSetOf<Int>()
            selected.filter{it.element.group==group}.sortedBy{if(s.values(it.element.id)["stableTemplateId"]!=null)0 else 1}.forEach {entry->
                val e=entry.element
                val sourceRow=s.values(e.id)["stableTemplateId"]?.substringAfterLast(':')?.toIntOrNull()?.takeIf{it in rows && it !in used}
                val row=sourceRow ?: rows.firstOrNull{it !in used} ?: run {
                    val next=w.sheet(3).nodes("row").filter{it.namespaceURI==TemplateWorkbook.MAIN}.maxOf{it.getAttribute("r").toInt()}+2
                    w.copyRows(3,rows.first(),rows.first(),next,12);next
                }
                used+=row
                val globalCode=s.values(e.id)["customCode"] ?: e.elementCode
                set(3,"B$row",globalCode.toIntOrNull() ?: globalCode)
                set(3,"C$row",com.tuempresa.inventariovial.scap.domain.ScapElementView.name(s,e))
                val description=s.values(e.id)["globalDescription"] ?: s.defects.filter{it.elementCode==e.elementCode}.joinToString("\n"){listOf(it.description,it.locationDescription).filter(String::isNotBlank).joinToString(" · ")}
                set(3,"F$row",description)
            }
        }
        set(3,"D15",s.inspection.createdBy);set(3,"D16",null)
        for(i in 0..30) {
            val item=selected.getOrNull(i);val e=item?.element;val r=14+i
            // G.1 input columns replace template examples, including sample metrado formulas.
            val code=e?.let{com.tuempresa.inventariovial.scap.domain.ScapElementView.code(s,it)}
            w.set(5,"A$r",code?.toIntOrNull() ?: code);w.set(5,"B$r",e?.let{com.tuempresa.inventariovial.scap.domain.ScapElementView.name(s,it)})
            w.set(5,"D$r",e?.quantity);w.set(5,"E$r",e?.unit);w.set(5,"F$r",e?.let{if(s.values(it.id)["importanceFactor"]=="")null else it.importanceFactor})
            for(p in 0..5) w.set(5,"${'G'+p}$r",item?.condition?.let{ScapPercentages.values(it)[p]})
        }
        val g2=s.values("G2")
        layout.conclusions.forEach {(key,ref)->set(6,ref,g2[key])}
        // Remove source-specific free observations while retaining source-generated conclusions.
        val sampleCells=if(layout==ScapCanonicalLayout.Modular) listOf("B19","B34","E34","B41","E41","B51","E51","B53","E53","B56","B57","B58","E61","E62","E63") else emptyList()
        sampleCells.forEach {set(6,it,null)}
        ScapTemplateMedia.write(w,s,media,photoSheet=4,photoStartRow=11,canonical=true)
        w.finish(output)
    }
}
