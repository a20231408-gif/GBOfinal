package com.tuempresa.inventariovial.core

import android.content.Context
import com.tuempresa.inventariovial.data.entity.PhotoEntity
import org.json.JSONObject

/** Consume a server reservation only; tablet display counters are never global photo numbers. */
object ReadablePhotoNames {
    fun remember(context:Context,p:JSONObject,ack:JSONObject) {
        require(ack.optBoolean("ok") && ack.optInt("photoNamingVersion")==2 && ack.optString("key")==p.getString("key") && ack.optLong("revision")==p.getLong("revision"))
        require(ack.optString("identityId")==p.optString("identityId",p.getString("recordId")) && ack.optString("folderId").isNotBlank())
        val prefix=if(p.getString("kind")=="PAVEMENT") "${p.getString("routeCode")}/EVALUACION_SUPERFICIAL/${p.getString("tramo")}/${p.getString("surface")}/MUESTRA " else "${p.getString("routeCode")}/${p.getString("familyPath")}/${p.getString("family")} "
        require(ack.optString("logicalPath").matches(Regex(Regex.escape(prefix)+"[1-9][0-9]*")))
        val old=DriveInventorySchema.receipt(context,ack.getString("identityId"))
        if(old!=null)require(old.optString("folderId")==ack.optString("folderId") && old.optString("logicalPath")==ack.optString("logicalPath"))
        val files=p.getJSONArray("files");val expected=(0 until files.length()).map {files.getJSONObject(it)}.filter {it.getString("key").startsWith("photo/")}
        val photos=ack.getJSONArray("photos");require(photos.length()==expected.size)
        val prefs=context.getSharedPreferences("photo_names_v2",Context.MODE_PRIVATE);val edit=prefs.edit();val seen=mutableSetOf<String>()
        for(n in 0 until photos.length()) {
            val f=photos.getJSONObject(n);val id=f.getString("id");require(seen.add(id))
            val source=expected.single {it.getString("key")=="photo/$id"};require(f.getString("category")==source.optString("photoCategory","GENERAL"))
            val name=f.getString("name")
            if(f.optBoolean("historical"))require(name==source.getString("path").substringAfterLast('/'))
            else require(name.length<=20 && name.matches(Regex("[0-9]{3}_[A-Z]{3}[1-9][0-9]*_[A-Z]{3}\\.jpg")))
            prefs.getString(id,null)?.let {require(JSONObject(it).getString("name")==name) {"Reserva fotográfica cambió; conservar la asociación anterior."}}
            edit.putString(id,f.toString())
        }
        check(edit.commit())
        if(p.getString("kind")!="PAVEMENT")DriveInventorySchema.remember(context,p,ack)
    }
    fun name(context:Context,photo:PhotoEntity)=name(context,photo.id,photo.photoCategory ?: "GENERAL")
    fun name(context:Context,id:String,category:String):String {
        val saved=context.getSharedPreferences("photo_names_v2",Context.MODE_PRIVATE).getString(id,null)
            ?: throw SyncPending("Pendiente de reservar el nombre fotográfico en el servidor.")
        val p=JSONObject(saved)
        require(p.getString("category")== category) {"El cambio de categoría requiere conciliar su nombre confirmado."}
        return p.getString("name")
    }
}
