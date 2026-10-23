package com.tuempresa.inventariovial.localfiles

import androidx.room.withTransaction
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.delivery.sha256
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale

/** Public display paths only. Never used for originals, Drive, UUIDs or content verification. */
object PublicLocalFolderNames {
    const val VERSION=2
    private const val PREFIX="LOCAL_PUBLIC_FOLDER_V2/"
    fun component(value:String):String {
        var text=Normalizer.normalize(value,Normalizer.Form.NFC)
            .replace(Regex("[\\p{Cc}\\p{Cf}]")," ")
            .replace(Regex("[<>:\"/\\\\|?*]")," - ")
            .replace(Regex("\\s+")," ").replace(Regex("(?: - ){2,}")," - ").trim(' ','.','-')
        if(text.isBlank())text="Ficha sin referencia"
        if(text.substringBefore('.').uppercase(Locale.ROOT).matches(Regex("CON|PRN|AUX|NUL|COM[0-9]+|LPT[0-9]+")))text="Ficha - $text"
        return fit(text,96).trimEnd(' ','.','-')
    }
    private fun fit(value:String,bytes:Int):String {
        val out=StringBuilder();var used=0;var i=0
        while(i<value.length){val cp=value.codePointAt(i);val size=when{cp<128->1;cp<2048->2;cp<65536->3;else->4}
            if(used+size>bytes)break
            out.appendCodePoint(cp);used+=size;i+=Character.charCount(cp)
        };return out.toString()
    }
    private fun comparable(value:String)=Normalizer.normalize(value,Normalizer.Form.NFC).uppercase(Locale.ROOT)
    /** Durable collision suffixes are local display disambiguators, never contractual numbers. */
    suspend fun reserve(db:InventoryDatabase,recordKey:String,parent:String,proposed:String):String=db.withTransaction {
        require(parent.startsWith("Download/GBO_Ruta_Clara/")&&parent.endsWith('/'))
        val base=component(proposed);val dao=db.localFilesDao()
        val key=PREFIX+sha256((parent+"\u0000"+recordKey+"\u0000"+base).toByteArray())
        dao.choice(key)?.let {row->
            val j=JSONObject(row.destination)
            check(j.getString("recordKey")==recordKey&&j.getString("parent")==parent&&j.getString("base")==base)
            val name=j.getString("name");check(component(name)==name);return@withTransaction name
        }
        val used=mutableSetOf<String>()
        // One-shot reads on this transaction's connection, never Flow collection while holding it.
        db.openHelper.readableDatabase.query("SELECT destination FROM device_delivery_choices WHERE `key` GLOB 'LOCAL_PUBLIC_FOLDER_V2/*'").use {rows->
            while(rows.moveToNext()) {
                // Fail closed on a damaged binding; never silently assign its folder to another UUID.
                val j=JSONObject(rows.getString(0))
                if(j.getString("parent")==parent)used+=comparable(j.getString("name"))
            }
        }
        fun retain(path:String){if(path.startsWith(parent))path.removePrefix(parent).substringBefore('/').takeIf{it.isNotBlank()}?.let{used+=comparable(it)}}
        // Published copies and frozen plans from earlier APKs also retain their namespace.
        db.openHelper.readableDatabase.query("SELECT relativePath FROM local_shared_files").use{rows->while(rows.moveToNext())retain(rows.getString(0))}
        dao.jobs().mapNotNull{it.manifestJson}.forEach{json->LocalPlan.parse(json).files.forEach{retain(it.relativePath)}}
        var name=base;var suffix=1
        while(comparable(name)in used){suffix++;val tail=" ($suffix)";name=fit(base,96-tail.toByteArray().size).trimEnd()+tail}
        val value=JSONObject().put("recordKey",recordKey).put("parent",parent).put("base",base).put("name",name)
        dao.saveChoice(DeviceDeliveryChoice(key,value.toString(),"LOCAL_PUBLIC_FOLDER_V2",System.currentTimeMillis()))
        name
    }
    suspend fun describe(db:InventoryDatabase,job:LocalExportJob,label:String):String=when(job.kind){
        "SCAP"->{
            val s=requireNotNull(db.scapDao().snapshot(job.recordId));val v=s.values()
            val name=(v["singularityName"] ?: s.inspection.bridgeName.takeUnless{it.equals("PUENTE",true)}).orEmpty()
            val code=s.inspection.bridgeCode.trim()
            val prefix="Puente"+if(code.isBlank())""else" $code"
            if(name.isNotBlank())"$prefix - $name"else if(code.isNotBlank())prefix else location(prefix,s.roadRecord)
        }
        "SIC"->{
            val s=requireNotNull(db.inventoryDao().snapshot(job.recordId))
            val title=when(label){"BADEN"->"Badén";"TUNEL"->"Túnel";else->label.lowercase(Locale.ROOT).replaceFirstChar{it.titlecase(Locale.ROOT)}}
            val code=(s.sic17a?.bridgeCode?.takeIf{it.isNotBlank()} ?: s.sic17?.bridgeCode).orEmpty().trim()
            val prefix=title+if(code.isBlank())""else" $code"
            val name=s.sic17a?.bridgeName.orEmpty()
            if(name.isNotBlank())"$prefix - $name"else location(prefix,s.record)
        }
        "PAVEMENT"->{
            val s=requireNotNull(db.pavementDao().sample(job.recordId))
            val title=when(s.surface){"RIGID"->"Pavimento rígido";"ASPHALT"->"Pavimento asfáltico";else->"Pavimento no pavimentado"}
            val section=s.tramo.ifBlank{"Tramo pendiente"}.let{if(it.all(Char::isDigit))"Tramo $it"else it}
            "$title - $section - ${if(s.muestra>0)"Muestra ${s.muestra}"else"Muestra pendiente"}"
        }
        else->label
    }
    private fun location(title:String,r:com.tuempresa.inventariovial.data.entity.InventoryRecordEntity?):String {
        val route=r?.routeCode?.takeIf{it.isNotBlank()}
        val pr=r?.startPrCode?.takeIf{it.isNotBlank()}
        val metres=r?.startDistanceM?.takeIf{it.isFinite()&&it>=0}?.let{
            java.math.BigDecimal.valueOf(it).stripTrailingZeros().toPlainString().let{n->val p=n.split('.');p[0].padStart(3,'0')+if(p.size>1)"."+p[1]else""}
        }
        return listOfNotNull(title,route,if(pr!=null&&metres!=null)"PR ${pr.trimStart('0').ifBlank{"0"}}+$metres"else null)
            .joinToString(" - ").let{if(route==null&&pr==null)"$it - Sin referencia"else it}
    }
}
