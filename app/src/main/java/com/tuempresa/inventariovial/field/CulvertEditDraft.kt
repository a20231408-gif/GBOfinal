package com.tuempresa.inventariovial.field

import android.content.Context
import android.util.AtomicFile
import com.tuempresa.inventariovial.model.form.Sic18FormState
import org.json.JSONObject
import org.json.JSONArray
import java.io.File

/** Raw editing drafts for an ACTIVE culvert; never replace contractual Room data until finalization. */
data class CulvertEditingDraft(val base:Sic18FormState,val extra:Map<String,String>,val photos:List<String>,val categories:Map<String,String>,
    val captures:Map<String,com.tuempresa.inventariovial.camera.PhotoCaptureMetadata> = emptyMap(),val baseRevision:Long?=null,val observations:String?=null,val core:SicCoreDraft?=null)
object CulvertEditDraft {
    private fun file(context:Context,id:String,owner:String):File {
        require(id.matches(Regex("[A-Za-z0-9_-]{1,100}")) && owner.isNotBlank())
        val ownerKey=java.security.MessageDigest.getInstance("SHA-256").digest(owner.toByteArray()).joinToString("") {"%02x".format(it)}
        return File(File(context.filesDir,"culvert_edit_drafts/$ownerKey").apply {mkdirs()},"$id.json")
    }
    private fun map(j:JSONObject)=j.keys().asSequence().associateWith {j.getString(it)}
    fun save(context:Context,id:String,owner:String,draft:CulvertEditingDraft) {
        val base=draft.base
        val json=JSONObject().put("owner",owner).put("id",id).put("base",JSONObject(mapOf(
            "classCode" to base.classCode,"typeCode" to base.typeCode,"spans" to base.spans,
            "crossSectionCode" to base.crossSectionCode,"dimension1M" to base.dimension1M,"dimension2M" to base.dimension2M,
            "structuralConditionCode" to base.structuralConditionCode,"functionalConditionCode" to base.functionalConditionCode,"sectionShape" to base.sectionShape,
            "structuralDamagePercent" to base.structuralDamagePercent,"functionalObstructionPercent" to base.functionalObstructionPercent)))
            .put("observations",draft.observations ?: JSONObject.NULL).put("baseRevision",draft.baseRevision ?: JSONObject.NULL).put("extra",JSONObject(draft.extra)).put("photos",JSONArray(draft.photos)).put("categories",JSONObject(draft.categories))
            .put("captures",JSONObject().apply {draft.captures.forEach {(path,c)->
                put(path,JSONObject().put("capturedAt",c.capturedAt ?: JSONObject.NULL).put("source",c.source).put("replacesPath",c.replacesPath ?: JSONObject.NULL).put("fix",c.fix?.let {f->
                    JSONObject().put("latitude",f.latitude).put("longitude",f.longitude).put("accuracy",f.horizontalAccuracy.takeIf {it.isFinite()} ?: JSONObject.NULL)
                        .put("altitude",f.altitude ?: JSONObject.NULL).put("timestamp",f.timestamp).put("source",f.source).put("provider",f.provider.name)
                        .put("verticalAccuracy",f.verticalAccuracy ?: JSONObject.NULL).put("fixType",f.fixType ?: JSONObject.NULL)
                        .put("satellites",f.satellites ?: JSONObject.NULL).put("hdop",f.hdop ?: JSONObject.NULL).put("correctionAge",f.correctionAge ?: JSONObject.NULL).put("isRtkFixed",f.isRtkFixed)
                } ?: JSONObject.NULL))
            }})
        draft.core?.let {c->json.put("core",JSONObject().put("route",c.route).put("roadbed",c.roadbed).put("startPr",c.startPr).put("startDistance",c.startDistance).put("endPr",c.endPr).put("endDistance",c.endDistance).put("side",c.side).put("segment",c.segment).put("direction",c.direction))}
        val atomic=AtomicFile(file(context,id,owner));val out=atomic.startWrite()
        try {out.write(json.toString().toByteArray());atomic.finishWrite(out)} catch(e:Exception){atomic.failWrite(out);throw e}
    }
    fun read(context:Context,id:String,owner:String):CulvertEditingDraft? {
        val f=file(context,id,owner);if(!f.isFile)return null
        val j=JSONObject(f.readText());require(j.getString("owner")==owner && j.getString("id")==id)
        val b=j.getJSONObject("base");val p=j.getJSONArray("photos")
        val captures=j.optJSONObject("captures") ?: JSONObject()
        return CulvertEditingDraft(Sic18FormState(b.getString("classCode"),b.getString("typeCode"),b.getString("spans"),b.getString("crossSectionCode"),b.getString("dimension1M"),b.getString("dimension2M"),b.getString("structuralConditionCode"),b.getString("functionalConditionCode"),sectionShape=b.optString("sectionShape"),structuralDamagePercent=b.optString("structuralDamagePercent"),functionalObstructionPercent=b.optString("functionalObstructionPercent")),
            map(j.getJSONObject("extra")),(0 until p.length()).map {p.getString(it)},map(j.getJSONObject("categories")),
            captures.keys().asSequence().associateWith {path->val c=captures.getJSONObject(path)
                com.tuempresa.inventariovial.camera.PhotoCaptureMetadata(if(c.isNull("capturedAt"))null else c.getLong("capturedAt"),com.tuempresa.inventariovial.core.InventoryDraftStore.restoreFix(c.optJSONObject("fix")),c.getString("source"),if(c.isNull("replacesPath"))null else c.getString("replacesPath"))},baseRevision=if(j.isNull("baseRevision"))null else j.getLong("baseRevision"),observations=if(j.isNull("observations"))null else j.getString("observations"),core=j.optJSONObject("core")?.let {c->SicCoreDraft(c.getString("route"),c.getString("roadbed"),c.getString("startPr"),c.getString("startDistance"),c.optString("endPr"),c.optString("endDistance"),c.optString("side","D"),c.optString("segment"),c.optString("direction","INCREASING"))})
    }
    fun clear(context:Context,id:String,owner:String) {file(context,id,owner).delete()}
}
