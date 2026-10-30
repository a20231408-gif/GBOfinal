package com.tuempresa.inventariovial.pavement

import org.json.JSONObject

/** Individual field observations, distinct from the contractual unit-wide seal assessment. */
object PavementJointOccurrences {
    private const val KEY="_gboJointOccurrenceIds"
    private const val ASSESS="_gboSealUnitAssessment"
    fun individual(s:PavementSampleEntity,id:String)=s.sicDataJson?.let(::JSONObject)?.optJSONObject(KEY)?.optBoolean(id)==true
    fun markIndividual(s:PavementSampleEntity,id:String):String {
        val root=s.sicDataJson?.let(::JSONObject)?:JSONObject();val ids=root.optJSONObject(KEY)?:JSONObject()
        ids.put(id,true);root.put(KEY,ids);return root.toString()
    }
    fun assessment(s:PavementSampleEntity,damages:List<PavementDamageEntity> = emptyList()):String? {
        val explicit=s.sicDataJson?.let(::JSONObject)?.optJSONObject(ASSESS)?.optString("severity")?.let{PavementSeverity.canonical(it)}?.takeIf{it in listOf("L","M","H")}
        return explicit ?: damages.filter{it.code==26 && it.retiredAt==null && !individual(s,it.id)}.singleOrNull()?.severity?.let{PavementSeverity.canonical(it)}?.takeIf{it in listOf("L","M","H")}
    }
    /** Preserve an already-recorded GLOBAL rating; never derive it from individual observations. */
    fun preserveHistoricalAssessment(s:PavementSampleEntity,damages:List<PavementDamageEntity>):PavementSampleEntity {
        if(assessment(s)!=null)return s
        val legacy=damages.filter{it.code==26 && it.retiredAt==null && !individual(s,it.id)}.singleOrNull()?:return s
        val severity=PavementSeverity.canonical(legacy.severity)?.takeIf{it in listOf("L","M","H")}?:return s
        val root=s.sicDataJson?.let(::JSONObject)?:JSONObject()
        root.put(ASSESS,JSONObject().put("severity",severity).put("source","HISTORICAL_GLOBAL_PRESERVED"))
        return s.copy(sicDataJson=root.toString())
    }
    fun assess(s:PavementSampleEntity,severity:String):String {
        val v=PavementSeverity.canonical(severity);require(v in listOf("L","M","H"))
        val root=s.sicDataJson?.let(::JSONObject)?:JSONObject();root.put(ASSESS,JSONObject().put("severity",v).put("source","USER_CONFIRMED_UNIT_ASSESSMENT"));return root.toString()
    }
    fun needsAssessment(s:PavementSampleEntity,damages:List<PavementDamageEntity>)=
        (damages.count{it.code==26&&it.retiredAt==null}>1 || damages.any{it.code==26&&individual(s,it.id)})&&assessment(s,damages)==null
    /** No max/average inference: new individual ratings cannot become three GLOBAL deduct values. */
    fun calculationRecords(s:PavementSampleEntity,damages:List<PavementDamageEntity>):List<PavementDamageEntity> {
        val active=damages.filter{it.retiredAt==null};val seals=active.filter{it.code==26}
        if(seals.size<=1 && seals.none{individual(s,it.id)} && assessment(s)==null)return active
        val severity=assessment(s,active)
        val unit=seals.firstOrNull() ?: PavementDamageEntity(s.id+"_PCI26",s.id,0,s.surface,26,"Evaluación global del sello de junta",severity,"GLOBAL",null,null,null,null,null,null,null,null,null,null,null,createdAt=s.createdAt)
        return active.filter{it.code!=26}+if(severity==null)emptyList()else listOf(unit.copy(severity=severity))
    }
    fun referenceLength(s:PavementSampleEntity,marks:Set<PavementJointAnnotations.Mark>)=marks.sumOf{m->
        val(a,b)=PavementJointAnnotations.endpoints(s,m);kotlin.math.hypot(a.x-b.x,a.y-b.y)}
    fun validateNew(s:PavementSampleEntity,d:PavementDamageEntity,marks:Set<PavementJointAnnotations.Mark>,others:List<PavementDamageEntity>){
        require(marks.isNotEmpty()&&marks.all{PavementJointAnnotations.valid(s,it)}){"Selecciona una junta o segmento interior válido."}
        val same=others.filter{it.id!=d.id&&it.code==26&&PavementJointAnnotations.marks(s,it.id)==marks}
        require(same.none{it.observation.orEmpty().trim().equals(d.observation.orEmpty().trim(),true)} &&
            (same.isEmpty()||d.observation.orEmpty().isNotBlank())){"Esa junta y defecto ya están registrados. Edita la ocurrencia existente o describe un defecto distinto."}
    }
}
