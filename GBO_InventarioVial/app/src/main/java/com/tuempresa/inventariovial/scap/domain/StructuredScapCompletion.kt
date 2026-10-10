package com.tuempresa.inventariovial.scap.domain

import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot

/** Routes are generated alongside fields, without parsing validation messages. */
internal object StructuredScapCompletion {
    fun assess(snapshot:ScapInspectionSnapshot,catalog:ScapCatalog):ScapCompleteness {
        val complete=mutableListOf<ScapPendingField>();val pending=mutableListOf<ScapPendingField>()
        fun add(section:String,label:String,owner:String,key:String,missing:Boolean,invalid:Boolean=false) {
            val item=ScapPendingField(section,label,owner,key,if(invalid)ScapIssueSeverity.BLOCKING else ScapIssueSeverity.WARNING)
            if(missing||invalid)pending+=item else complete+=item
        }
        val s=ScapPresentation.withVisibleStructures(snapshot)
        val owners=listOf("inspection","ACCESS_LEFT","ACCESS_RIGHT")+s.spans.map{it.id}+s.substructures.map{it.id}+s.supports.map{it.id}+s.joints.map{it.id}+s.profile.map{it.id}
        owners.forEach {owner->
            val kind=ScapValidation.ownerType(s,owner);val values=s.values(owner)
            val rowName=s.supports.find{it.id==owner}?.let{"Apoyo ${it.supportIndex} · "} ?:
                s.spans.find{it.id==owner}?.let{"Tramo ${it.spanIndex} · "} ?: s.joints.find{it.id==owner}?.let{"Junta ${it.jointIndex} · "}.orEmpty()
            (if(kind=="joint")catalog.fields("C5","joint") else catalog.fields.filter{it.owner==kind && !(kind=="inspection" && it.key in setOf("segment","jointType","jointMaterial"))}).filter{it.visible(values) && ScapFieldPolicy.enabled(it,values)}.distinctBy{it.section+it.key}.forEach {field->
                add(field.section,rowName+field.label,owner,field.key,values[field.key].isNullOrBlank(),ScapValidation.fieldError(field,values[field.key].orEmpty(),values,catalog)!=null)
            }
        }
        if(s.spans.isEmpty())add("C2","Añadir tramo","inspection","addSpan",true)
        ScapElementView.applicable(s).forEach {entry->
            val e=entry.element;val v=s.values(e.id);val name=ScapElementView.name(s,e)
            add("F1","$name · Estado",e.id,"presence",ScapElementView.state(s,e)==ScapElementState.UNSET)
            add("F1","$name · Descripción de condición global",e.id,"globalDescription",v["globalDescription"].isNullOrBlank())
            add("F1","$name · Nombre",e.id,"customName",name.isBlank())
            add("F1","$name · Código",e.id,"customCode",v["customCode"]=="")
            add("G1","$name · Código",e.id,"exportCode",ScapElementView.code(s,e).isBlank())
            add("G1","$name · Metrado",e.id,"quantity",e.quantity==null,
                v["quantity"]?.takeIf(String::isNotBlank)?.let{ScapNumbers.decimal(it)?.let{n->n>=0}!=true}==true)
            add("G1","$name · Unidad",e.id,"unit",e.unit.isBlank())
            add("G1","$name · Factor de importancia",e.id,"importanceFactor",v["importanceFactor"]=="",
                v["importanceFactor"]?.takeIf(String::isNotBlank)?.let{ScapNumbers.decimal(it)?.let{n->n in 0.0..1.0}!=true}==true)
            val p=entry.condition?.let{com.tuempresa.inventariovial.scap.calculator.ScapPercentages.values(it)} ?: List(6){null}
            if(p.all{it==null})add("G1","$name · Calificación pendiente",e.id,"percent0",true)
            else if(com.tuempresa.inventariovial.scap.calculator.ScapPercentages.error(p)!=null)add("G1","$name · Revisar suma de calificación",e.id,"percent0",true)
            (0..5).forEach{i->val raw=v["percent$i"].orEmpty();if(raw.isNotBlank()&&ScapNumbers.decimal(raw)?.let{it in 0.0..100.0}!=true)add("G1","$name · Porcentaje $i inválido",e.id,"percent$i",false,true)}
        }
        s.photos.forEach {p->
            add("F2","Foto ${p.photoIndex} · Nombre",p.id,"photoName",ScapElementView.photoName(s,p.id,p.photoCategory).isBlank())
            add("F2","Foto ${p.photoIndex} · Descripción",p.id,"photoDescription",p.description.isNullOrBlank())
            add("F2","Foto ${p.photoIndex} · Imagen",p.id,"photo",p.localPath.isBlank())
            val date=s.values(p.id)["photoDate"]
            if(date=="")add("F2","Foto ${p.photoIndex} · Fecha",p.id,"photoDate",true)
            if(!date.isNullOrBlank())add("F2","Foto ${p.photoIndex} · Fecha inválida",p.id,"photoDate",false,
                runCatching{java.time.LocalDate.parse(date,java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT))}.isFailure)
        }
        if(s.photos.isEmpty())add("F2","Añadir registro fotográfico","inspection","addPhoto",true)
        s.defects.forEach{d->if(d.description.isBlank())add("F1","Observación · Descripción",d.id,"defectDescription",true)}
        for(type in listOf("ELEVATION","PLAN","CROSS_SECTION"))if(s.sketches.none{it.type==type})add("E","Croquis $type","inspection",type,true)
        return ScapCompleteness(complete.distinctBy{it.id},pending.distinctBy{it.id})
    }
}
