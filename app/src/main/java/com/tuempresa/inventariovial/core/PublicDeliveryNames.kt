package com.tuempresa.inventariovial.core

import android.content.Context
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.localfiles.DeviceDeliveryChoice
import com.tuempresa.inventariovial.localfiles.LocalExportJob
import org.json.JSONObject
import java.text.Normalizer

/** Negotiated labels, never new identities. Existing reservations and frozen deliveries are immutable. */
object PublicDeliveryNames {
    const val VERSION=1
    fun component(value:String,bytes:Int=64):String {
        val text=Normalizer.normalize(value,Normalizer.Form.NFC).replace(Regex("[\\p{Cc}\\p{Cf}<>:\"/\\\\|?*]")," ")
            .replace(".."," ").replace(Regex("\\s+")," ").trim(' ','.','-').ifBlank{"Sin referencia"}
        val result=StringBuilder();var used=0
        text.codePoints().toArray().forEach{cp->val s=String(Character.toChars(cp));if(used+s.toByteArray().size<=bytes){result.append(s);used+=s.toByteArray().size}}
        return result.toString().trimEnd(' ','.','-')
    }
    fun validFolder(family:String,name:String)=name.length<=80&&component(name,96)==name&&name.matches(Regex(Regex.escape(family)+" [1-9][0-9]* - .+"))
    suspend fun label(db:InventoryDatabase,kind:String,id:String):String {
        val record=when(kind){"SCAP"->db.scapDao().snapshot(id)?.roadRecord;"SIC"->db.inventoryDao().recordById(id);else->null}
        val bridge=if(kind=="SCAP")db.scapDao().snapshot(id)else null
        val pavement=if(kind=="PAVEMENT")db.pavementDao().sample(id)else null
        val family=if(bridge!=null)"PUENTE"else record?.assetType ?: "MUESTRA"
        val route=record?.routeCode?.takeIf{it.isNotBlank()} ?: bridge?.values()?.get("route") ?: pavement?.ruta
        val pr=record?.startPrCode?.takeIf{it.isNotBlank()}?.let{p->"PR ${p.trimStart('0').ifBlank{"0"}}+${java.math.BigDecimal.valueOf(record.startDistanceM).stripTrailingZeros().toPlainString().padStart(3,'0')}"} ?: pavement?.prInicial
        val name=bridge?.values()?.get("singularityName")?.takeIf{it.isNotBlank()} ?: bridge?.inspection?.bridgeName?.takeUnless{it.equals("PUENTE",true)} ?:
            record?.let{db.inventoryDao().snapshot(it.id)?.sic17a?.bridgeName} ?: pavement?.let{"${it.tramo} - Muestra ${it.muestra}"}
        return component(listOfNotNull(family,route,pr,name?.takeIf{it.isNotBlank()}).joinToString(" - "))
    }
    fun binding(c:Context,key:String):JSONObject?=c.getSharedPreferences("public_delivery_names_v1",Context.MODE_PRIVATE).getString(key,null)?.let(::JSONObject)
    fun remember(c:Context,p:JSONObject,a:JSONObject){
        if(a.optInt("publicNamingVersion")!=VERSION)return
        require(validFolder(p.getString("family"),a.getString("publicElementFolder")))
        require(a.getString("logicalPath").substringAfterLast('/')==a.getString("publicElementFolder"))
        require(component(a.getString("publicDisplayLabel"))==a.getString("publicDisplayLabel"))
        if(p.getString("kind")=="SCAP")require(a.getString("publicScapFolder").matches(Regex("INSPECCION [1-9][0-9]*")))
        binding(c,p.getString("key"))?.let{old->require(old.getString("folderId")==a.getString("folderId")&&old.getString("publicElementFolder")==a.getString("publicElementFolder")&&old.optString("publicScapFolder")==a.optString("publicScapFolder")){"La identidad pública cambió; conserva la reserva anterior."}}
        check(c.getSharedPreferences("public_delivery_names_v1",Context.MODE_PRIVATE).edit().putString(p.getString("key"),a.toString()).commit())
    }
    fun apply(c:Context,p:JSONObject){
        binding(c,p.getString("key"))?.let{a->p.put("publicNamingVersion",VERSION).put("publicDisplayLabel",a.getString("publicDisplayLabel")).put("publicElementFolder",a.getString("publicElementFolder"));if(p.getString("kind")=="SCAP")p.put("publicScapFolder",a.getString("publicScapFolder"))}
    }
    suspend fun localPhoto(db:InventoryDatabase,job:LocalExportJob,id:String,category:String,ext:String):String {
        val key="PUBLIC_PHOTO_LABEL_V1/"+com.tuempresa.inventariovial.delivery.sha256((job.recordKey+"/"+id).toByteArray())
        db.localFilesDao().choice(key)?.let{return JSONObject(it.destination).getString("name")}
        val type=com.tuempresa.inventariovial.scap.domain.ScapPhotoPanel.forCode(category)?.folder ?: DriveInventorySchema.category(category)
        val name=component(label(db,job.kind,job.recordId),42)+" - "+component(type.replace('_',' '),22)+" - "+com.tuempresa.inventariovial.delivery.sha256(id.toByteArray()).take(6)+".$ext"
        db.localFilesDao().saveChoice(DeviceDeliveryChoice(key,JSONObject().put("name",name).put("recordKey",job.recordKey).toString(),"PUBLIC_NAME_V1",System.currentTimeMillis()))
        return name
    }
}
