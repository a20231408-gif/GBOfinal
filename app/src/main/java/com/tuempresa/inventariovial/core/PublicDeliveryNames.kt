package com.tuempresa.inventariovial.core

import android.content.Context
import androidx.room.withTransaction
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
        require(bytes>=4)
        val result=StringBuilder();var used=0
        for(cp in text.codePoints().toArray()) {val s=String(Character.toChars(cp));if(used+s.toByteArray(Charsets.UTF_8).size>bytes)break;result.append(s);used+=s.toByteArray(Charsets.UTF_8).size}
        val name=result.toString().trimEnd(' ','.','-').ifBlank{"Ficha"}
        return if(name.substringBefore('.').uppercase(java.util.Locale.ROOT).matches(Regex("CON|PRN|AUX|NUL|COM[0-9]+|LPT[0-9]+")))component("Ficha - $name",bytes)else name
    }
    fun validFolder(family:String,name:String)=name.length<=80&&component(name,80)==name&&name.matches(Regex(Regex.escape(family)+" [1-9][0-9]*( - .+)?"))
    fun photoName(label:String,category:String,number:Int,ext:String):String {
        require(number in 1..999&&ext in setOf("jpg","png","webp","heic","bin"))
        val type=com.tuempresa.inventariovial.scap.domain.ScapPhotoPanel.forCode(category)?.folder ?: DriveInventorySchema.category(category)
        return component(label,42)+" - "+component(type.replace('_',' '),16)+" - Foto $number.$ext"
    }
    suspend fun label(db:InventoryDatabase,kind:String,id:String):String {
        val record=when(kind){"SCAP"->db.scapDao().snapshot(id)?.roadRecord;"SIC"->db.inventoryDao().recordById(id);else->null}
        val bridge=if(kind=="SCAP")db.scapDao().snapshot(id)else null
        val pavement=if(kind=="PAVEMENT")db.pavementDao().sample(id)else null
        val family=if(bridge!=null)"PUENTE"else record?.assetType ?: "MUESTRA"
        val route=record?.routeCode?.takeIf{it.isNotBlank()} ?: bridge?.values()?.get("route") ?: pavement?.ruta
        val pr=record?.startPrCode?.takeIf{it.isNotBlank()&&record.startDistanceM.isFinite()&&record.startDistanceM>=0}?.let{p->"PR ${p.trimStart('0').ifBlank{"0"}}+${java.math.BigDecimal.valueOf(record.startDistanceM).stripTrailingZeros().toPlainString().padStart(3,'0')}"} ?: pavement?.prInicial
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
        binding(c,p.getString("key"))?.let{old->require(old.getString("folderId")==a.getString("folderId")&&old.getString("publicElementFolder")==a.getString("publicElementFolder")&&old.optString("publicScapFolder")==a.optString("publicScapFolder")){"La identidad pública cambió; conserva la reserva anterior."}}
        check(c.getSharedPreferences("public_delivery_names_v1",Context.MODE_PRIVATE).edit().putString(p.getString("key"),a.toString()).commit())
    }
    fun apply(c:Context,p:JSONObject){
        binding(c,p.getString("key"))?.let{a->p.put("publicNamingVersion",VERSION).put("publicDisplayLabel",a.getString("publicDisplayLabel")).put("publicElementFolder",a.getString("publicElementFolder"))}
    }
    suspend fun localPhoto(db:InventoryDatabase,job:LocalExportJob,id:String,category:String,ext:String):String {
        val key="PUBLIC_PHOTO_LABEL_V1/"+com.tuempresa.inventariovial.delivery.sha256((job.recordKey+"/"+id).toByteArray())
        return db.withTransaction {
            db.localFilesDao().choice(key)?.let{return@withTransaction JSONObject(it.destination).getString("name")}
            val used=mutableSetOf<String>();var number=0
            db.openHelper.readableDatabase.query("SELECT destination FROM device_delivery_choices WHERE `key` GLOB 'PUBLIC_PHOTO_LABEL_V1/*'").use{rows->while(rows.moveToNext()){
                val j=JSONObject(rows.getString(0));if(j.optString("recordKey")==job.recordKey){used+=j.getString("name");number=maxOf(number,j.optInt("number"))}
            }}
            var name:String
            do{number++;name=photoName(label(db,job.kind,job.recordId),category,number,ext)}while(name in used)
            db.localFilesDao().saveChoice(DeviceDeliveryChoice(key,JSONObject().put("name",name).put("number",number).put("recordKey",job.recordKey).toString(),"PUBLIC_NAME_V1",System.currentTimeMillis()))
            name
        }
    }
}
