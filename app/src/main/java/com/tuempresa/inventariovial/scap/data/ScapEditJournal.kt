package com.tuempresa.inventariovial.scap.data

import android.content.Context
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** Private write-ahead intents for text/choice edits; never credentials or diagnostics. */
class ScapEditJournal(context:Context) {
    data class Edit(val token:String,val id:String,val owner:String,val key:String,val value:String,val source:String,val sic:Boolean,val order:Long) {
        fun json()=JSONObject().put("token",token).put("id",id).put("owner",owner).put("key",key).put("value",value).put("source",source).put("sic",sic).put("order",order)
    }
    private val dir=File(context.filesDir,"scap_pending_edits")
    private fun file(e:Edit)=AtomicFile(File(dir,com.tuempresa.inventariovial.scap.export.ScapDelivery.sha256((e.id+"/"+e.owner+"/"+e.key).toByteArray())+".json"))
    fun new(id:String,owner:String,key:String,value:String,source:String,sic:Boolean)=Edit(UUID.randomUUID().toString(),id,owner,key,value,source,sic,sequence.updateAndGet{maxOf(it+1,System.currentTimeMillis()*1000)})
    fun put(e:Edit)=locked {check(dir.isDirectory||dir.mkdirs()) {"No se pudo abrir el journal privado."};val a=file(e);val out=a.startWrite();try{out.write(e.json().toString().toByteArray());a.finishWrite(out)}catch(x:Exception){a.failWrite(out);throw x}}
    private fun parse(s:String)=JSONObject(s).let{Edit(it.getString("token"),it.getString("id"),it.getString("owner"),it.getString("key"),it.getString("value"),it.getString("source"),it.getBoolean("sic"),it.getLong("order"))}
    fun pending():List<Edit> = locked {if(!dir.exists())return@locked emptyList();check(dir.isDirectory);(dir.listFiles() ?: error("Journal privado sin acceso")).filter{it.name.endsWith(".json")||it.name.endsWith(".json.bak")}.map{it.name.removeSuffix(".bak")}.distinct().map{parse(String(AtomicFile(File(dir,it)).readFully(),Charsets.UTF_8))}.sortedBy{it.order}}
    fun committed(e:Edit)=locked {val a=file(e);if(a.baseFile.exists()&&parse(String(a.readFully(),Charsets.UTF_8)).token==e.token)a.delete()}
    fun current(e:Edit):Boolean=locked {val a=file(e); a.baseFile.exists()&&parse(String(a.readFully(),Charsets.UTF_8)).token==e.token}
    companion object {private val sequence=java.util.concurrent.atomic.AtomicLong();private val lock=Any();private fun <T> locked(block:()->T):T=synchronized(lock,block)}
}
