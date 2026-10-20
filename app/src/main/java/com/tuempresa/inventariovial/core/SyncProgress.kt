package com.tuempresa.inventariovial.core

import android.content.Context
import androidx.work.WorkInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/** Stage acknowledgements describe transferred files; only the final receipt confirms a revision. */
data class DeliveryProgress(val phase:String,val processed:Int=0,val total:Int=0)
data class SyncProgress(val revision:Long,val phase:String,val processed:Int,val total:Int,val startedAt:Long)

class SyncProgressStore private constructor(context:Context) {
    private val prefs=context.getSharedPreferences("drive_progress_10d",Context.MODE_PRIVATE)
    private val values=MutableStateFlow(read())
    val flow=values.asStateFlow()
    private fun read():Map<String,SyncProgress> = runCatching {
        val j=JSONObject(prefs.getString("progress","{}")!!)
        j.keys().asSequence().associateWith {key->j.getJSONObject(key).let {SyncProgress(it.getLong("revision"),it.getString("phase"),it.getInt("processed"),it.getInt("total"),it.getLong("startedAt"))}}
    }.getOrDefault(emptyMap())
    @Synchronized fun record(key:String,revision:Long,p:DeliveryProgress) {
        val old=values.value[key]?.takeIf {it.revision==revision}
        val next=values.value+(key to SyncProgress(revision,p.phase,p.processed,p.total,old?.startedAt ?: System.currentTimeMillis()))
        val json=JSONObject()
        next.forEach {(k,v)->json.put(k,JSONObject().put("revision",v.revision).put("phase",v.phase).put("processed",v.processed).put("total",v.total).put("startedAt",v.startedAt))}
        check(prefs.edit().putString("progress",json.toString()).commit())
        values.value=next
    }
    companion object {
        private val stores=java.util.concurrent.ConcurrentHashMap<String,SyncProgressStore>()
        fun get(context:Context)=stores.getOrPut(context.filesDir.absolutePath){SyncProgressStore(context.applicationContext)}
    }
}

object SyncProgressPresentation {
    fun running(intent:DriveSyncIntent?,work:List<WorkInfo>)=intent!=null && work.any {
        it.state==WorkInfo.State.RUNNING && it.progress.getString("activeKey")==intent.key && it.progress.getLong("activeRevision",0)==intent.revision
    }
    fun phase(p:SyncProgress)=when(p.phase) {
        "PREPARING"->"Preparando archivos"
        "PHOTOS"->"Subiendo fotografías"
        "DOCUMENTS"->"Enviando ficha y Excel"
        "CONFIRMING"->"Confirmando en Drive"
        "COMPLETE"->"Entrega confirmada"
        else->"Pendiente de confirmación"
    }
    fun files(p:SyncProgress):String = if(p.total==0 && p.phase=="PREPARING")"Cantidad de archivos aún no calculada; preparación local, sin ACK de entrega"else "Archivos procesados: ${p.processed} / ${p.total} · pendientes: ${maxOf(0,p.total-p.processed)}"+
        if(p.total>0) " · ${p.processed*100/p.total}% de archivos transferidos" else ""
    fun elapsed(p:SyncProgress,now:Long):String {
        val seconds=maxOf(0,(now-p.startedAt)/1000)
        return "Tiempo transcurrido desde el primer intento: ${seconds/60} min ${seconds%60} s"
    }
}
