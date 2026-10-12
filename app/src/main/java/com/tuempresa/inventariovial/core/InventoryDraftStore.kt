package com.tuempresa.inventariovial.core

import android.content.Context
import androidx.room.withTransaction
import com.tuempresa.inventariovial.auth.AuthGraph
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.InventoryRecordEntity
import com.tuempresa.inventariovial.model.form.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.lang.reflect.Modifier
import java.util.UUID

/** Raw field text survives partial numeric edits; Room keeps the draft discoverable and blocked. */
object InventoryDraftStore {
    fun accessible(context:Context,id:String):Boolean = !file(context,id).exists() || read(context,id)!=null
    private fun fixJson(f:com.tuempresa.inventariovial.location.GeoLocation?):Any = f?.let {
        JSONObject().put("latitude",it.latitude).put("longitude",it.longitude).put("accuracy",it.horizontalAccuracy.takeIf { n->n.isFinite() }?:JSONObject.NULL)
            .put("altitude",it.altitude?:JSONObject.NULL).put("source",it.source).put("timestamp",it.timestamp)
            .put("verticalAccuracy",it.verticalAccuracy?:JSONObject.NULL).put("provider",it.provider.name)
            .put("fixType",it.fixType?:JSONObject.NULL).put("satellites",it.satellites?:JSONObject.NULL)
            .put("hdop",it.hdop?:JSONObject.NULL).put("correctionAge",it.correctionAge?:JSONObject.NULL).put("isRtkFixed",it.isRtkFixed)
    } ?: JSONObject.NULL
    fun restoreFix(json:JSONObject?):com.tuempresa.inventariovial.location.GeoLocation? = json?.let {
        com.tuempresa.inventariovial.location.GeoLocation(it.getDouble("latitude"),it.getDouble("longitude"),it.optDouble("accuracy",Double.NaN).toFloat(),
            if(it.isNull("altitude"))null else it.getDouble("altitude"),it.optString("source","TABLET_GNSS"),it.getLong("timestamp"),
            if(it.isNull("verticalAccuracy"))null else it.getDouble("verticalAccuracy").toFloat(),
            com.tuempresa.inventariovial.location.GnssProvider.valueOf(it.optString("provider","TABLET")),
            if(it.isNull("fixType"))null else it.getString("fixType"),if(it.isNull("satellites"))null else it.getInt("satellites"),
            if(it.isNull("hdop"))null else it.getDouble("hdop"),if(it.isNull("correctionAge"))null else it.getDouble("correctionAge"),it.optBoolean("isRtkFixed"))
    }
    private fun file(context:Context,id:String):File {
        require(id.matches(Regex("[A-Za-z0-9_-]{1,100}")))
        return File(File(context.filesDir,"inventory_drafts").apply { mkdirs() },"$id.json")
    }
    fun read(context:Context,id:String?):JSONObject? {
        if(id==null) return null
        val f=file(context,id);if(!f.isFile) return null
        val json=JSONObject(f.readText())
        return json.takeIf { it.getString("owner")==AuthGraph.get(context).sessions.current.value?.id }
    }
    fun <T:Any> restore(json:JSONObject?,default:T):T {
        val detail=json?.optJSONObject("detail") ?: return default
        if(detail.optString("type")!=default.javaClass.simpleName) return default
        val fields=detail.getJSONObject("fields")
        default.javaClass.declaredFields.filterNot { Modifier.isStatic(it.modifiers) || it.isSynthetic }.forEach { f ->
            if(fields.has(f.name)) {
                f.isAccessible=true
                val raw=fields.opt(f.name)
                val value=when {
                    raw==JSONObject.NULL -> null
                    f.type.isEnum -> f.type.enumConstants.first { (it as Enum<*>).name==raw.toString() }
                    f.type==Boolean::class.javaPrimitiveType -> fields.getBoolean(f.name)
                    else -> raw
                }
                f.set(default,value)
            }
        }
        return default
    }
    suspend fun save(context:Context,db:InventoryDatabase,r:InventorySaveRequest):String=withContext(Dispatchers.IO) {
        val user=AuthGraph.get(context).sessions.requireUser()
        val id=r.recordId ?: UUID.randomUUID().toString()
        val old=db.inventoryDao().recordById(id)
        require(old==null || old.status=="DRAFT") { "La ficha ya está finalizada" }
        val input=com.tuempresa.inventariovial.validation.ContractualLocationInput
        require(input.acceptPr(r.startPrCode) || old?.startPrCode==r.startPrCode){"PR: máximo cuatro dígitos"}
        require(r.endPrCode.isNullOrBlank() || input.acceptPr(r.endPrCode) || old?.endPrCode==r.endPrCode){"PR final: máximo cuatro dígitos"}
        fun offset(text:String?,historic:Double?) = text.isNullOrBlank() || input.acceptDistance(text) || (historic!=null && text.replace(',', '.').toDoubleOrNull()==historic)
        require(offset(r.startDistanceM,old?.startDistanceM) && offset(r.endDistanceM,old?.endDistanceM)){"Distancia: máximo tres cifras enteras"}
        val target=file(context,id)
        if(target.exists()) require(JSONObject(target.readText()).getString("owner")==user.id) { "Borrador de otro usuario" }
        val detail=when(val d=r.detail) {
            is SicFormDetail.Sic17->d.state;is SicFormDetail.Sic18->d.state;is SicFormDetail.Sic19->d.state
            is SicFormDetail.Sic20->d.state;is SicFormDetail.Sic21->d.state;is SicFormDetail.Sic22->d.state;is SicFormDetail.Sic23->d.state
        }
        val fields=JSONObject()
        detail.javaClass.declaredFields.filterNot {Modifier.isStatic(it.modifiers)||it.isSynthetic}.forEach { f ->
            f.isAccessible=true;val value=f.get(detail);fields.put(f.name,if(value is Enum<*>)value.name else value?:JSONObject.NULL)
        }
        val json=JSONObject().put("schema",1).put("owner",user.id).put("id",id)
            .put("startDistanceM",r.startDistanceM).put("endDistanceM",r.endDistanceM?:JSONObject.NULL)
            .put("detail",JSONObject().put("type",detail.javaClass.simpleName).put("fields",fields))
            .put("location",fixJson(r.location)).put("endLocation",fixJson(r.endLocation)).put("sideSource",r.sideSource)
            .put("photoPaths",JSONArray(r.photoPaths)).put("photoCategories",JSONObject(r.photoCategories))
            .put("photoCaptures",JSONObject().apply {r.photoCaptures.forEach { (path,capture) ->
                put(path,JSONObject().put("capturedAt",capture.capturedAt?:JSONObject.NULL).put("source",capture.source).put("replacesPath",capture.replacesPath ?: JSONObject.NULL)
                    .put("latitude",capture.fix?.latitude?:JSONObject.NULL).put("longitude",capture.fix?.longitude?:JSONObject.NULL)
                    .put("accuracy",capture.fix?.horizontalAccuracy?.takeIf {it.isFinite()}?:JSONObject.NULL)
                    .put("altitude",capture.fix?.altitude?:JSONObject.NULL).put("timestamp",capture.fix?.timestamp?:JSONObject.NULL).put("fix",fixJson(capture.fix)))
            } })
        val atomic=android.util.AtomicFile(target);val out=atomic.startWrite()
        try {out.write(json.toString().toByteArray());atomic.finishWrite(out)}catch(e:Exception){atomic.failWrite(out);throw e}
        val now=System.currentTimeMillis()
        val record=InventoryRecordEntity(id,r.sicCode,r.assetType,r.routeCode,r.roadbedCode,r.startPrCode,
            r.startDistanceM.replace(',','.').toDoubleOrNull()?:0.0,r.endPrCode,r.endDistanceM?.replace(',','.')?.toDoubleOrNull(),r.sideCode,
            r.latitude.takeIf {it.isFinite()},r.longitude.takeIf {it.isFinite()},r.location?.altitude,r.gpsAccuracyM?.toDouble()?.takeIf{it.isFinite()},
            r.surveyDate,r.observations,"DRAFT","PENDING","PENDING",old?.createdAt?:now,now,
            endLatitude=r.endLocation?.latitude?:r.endLatitude,endLongitude=r.endLocation?.longitude?:r.endLongitude,endGpsAccuracyM=(r.endLocation?.horizontalAccuracy?:r.endGpsAccuracyM)?.toDouble(),
            locationSource=if(r.latitude.isFinite()&&r.longitude.isFinite())r.locationSource else "DRAFT_LOCATION_PENDING",
            gpsTimestamp=r.location?.timestamp,gnssProvider=r.location?.provider?.name ?: "TABLET",isRtkFixed=r.location?.isRtkFixed ?: false,
            sessionId=if(old!=null) old.sessionId else r.sessionId,segment=r.segment,surveyDirection=r.direction,endLocationSource=r.endLocationSource,routeSource=r.routeSource)
        db.withTransaction {
            check(AuthGraph.get(context).sessions.requireUser().id==user.id)
            check(db.inventoryDao().recordById(id)?.status.let { it==null || it=="DRAFT" }) { "La ficha ya está finalizada" }
            db.inventoryDao().insertOrCompleteDraft(record)
        }
        id
    }
    fun completed(context:Context,id:String) { file(context,id).delete() }
}
