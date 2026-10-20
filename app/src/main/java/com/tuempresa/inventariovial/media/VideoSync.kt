package com.tuempresa.inventariovial.media

import android.content.Context
import androidx.work.*
import com.tuempresa.inventariovial.BuildConfig
import com.tuempresa.inventariovial.DriveConfig
import com.tuempresa.inventariovial.core.SyncIntentPresentation
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit

interface VideoTransport {
    fun available():Boolean
    fun begin(video:VideoEntity,revision:Long):JSONObject
    fun offset(video:VideoEntity):Long
    fun chunk(video:VideoEntity,start:Long,count:Int):Long
    fun finish(video:VideoEntity):JSONObject
}
class VideoHttp internal constructor(
    private val client:OkHttpClient=OkHttpClient.Builder().connectTimeout(30,TimeUnit.SECONDS).readTimeout(120,TimeUnit.SECONDS).writeTimeout(120,TimeUnit.SECONDS).followRedirects(false).build(),
    private val endpoint:String=DriveConfig.WEB_APP_URL,
    private val apiToken:String=DriveConfig.API_TOKEN
):VideoTransport {
    private val scripts=client.newBuilder().followRedirects(true).build()
    private var namingVersion=0
    private fun request(payload:JSONObject):JSONObject=scripts.newCall(Request.Builder().url(endpoint).post(payload.put("token",apiToken).toString().toRequestBody("application/json".toMediaType())).build()).execute().use {require(it.isSuccessful);JSONObject(it.body?.string() ?: "{}")}
    override fun available():Boolean {
        namingVersion=0
        return scripts.newCall(Request.Builder().url(endpoint).get().build()).execute().use {
            it.isSuccessful && JSONObject(it.body?.string() ?: "{}").let {c->
                val available=c.optBoolean("ok") && c.optInt("videoProtocol")==1 && c.optString("videoScope")=="SCAP_ONLY"
                if(available && c.optInt("readableVideoNamingVersion")==2)namingVersion=2
                available
            }
        }
    }
    private fun payload(v:VideoEntity,action:String)=JSONObject().put("action",action).put("videoProtocol",1).put("parentKey",v.parentKey).put("mediaId",v.id).put("category",v.category).put("categoryLabel",v.categoryLabel).put("mimeType",v.mimeType).put("sizeBytes",v.sizeBytes).put("sha256",v.sha256)
    private fun namedPayload(v:VideoEntity,action:String)=payload(v,action).also {if(namingVersion==2)it.put("videoNamingVersion",2)}
    override fun begin(video:VideoEntity,revision:Long)=request(namedPayload(video,"beginVideoV1").put("parentRevision",revision).put("resetSession",video.fileId!=null && video.sessionUrl==null)).also {if(it.optBoolean("ok"))VideoNames.validate(it,video,namingVersion)}
    override fun finish(video:VideoEntity)=request(namedPayload(video,"finishVideoV1").put("parentRevision",video.parentRevision).put("fileId",video.fileId)).also {if(it.optBoolean("ok"))VideoNames.validate(it,video,namingVersion)}
    private fun session(v:VideoEntity):String=requireNotNull(v.sessionUrl).also {require(VideoPolicy.safeSession(it))}
    override fun offset(video:VideoEntity):Long=client.newCall(Request.Builder().url(session(video)).put(ByteArray(0).toRequestBody(video.mimeType.toMediaType())).header("Content-Range","bytes */${video.sizeBytes}").build()).execute().use {
        when(it.code){308->VideoPolicy.rangeOffset(it.header("Range"),video.sizeBytes);200,201->video.sizeBytes;404,410->throw ExpiredVideoSession();else->throw java.io.IOException("Consulta de carga HTTP ${it.code}")}
    }
    override fun chunk(video:VideoEntity,start:Long,count:Int):Long {
        val body=object:RequestBody() {
            override fun contentType()=video.mimeType.toMediaType()
            override fun contentLength()=count.toLong()
            override fun writeTo(sink:BufferedSink) {RandomAccessFile(video.localPath,"r").use {file->file.seek(start);var left=count;val b=ByteArray(8192);while(left>0){val n=file.read(b,0,minOf(left,b.size));require(n>0);sink.write(b,0,n);left-=n}}}
        }
        return client.newCall(Request.Builder().url(session(video)).put(body).header("Content-Range","bytes $start-${start+count-1}/${video.sizeBytes}").build()).execute().use {
            when(it.code){308->VideoPolicy.rangeOffset(it.header("Range"),video.sizeBytes);200,201->video.sizeBytes;404,410->throw ExpiredVideoSession();else->throw java.io.IOException("Carga HTTP ${it.code}")}
        }
    }
}
class ExpiredVideoSession:java.io.IOException("Sesión de carga caducada; se abrirá otra para el mismo archivo")
object VideoSync {
    private val mutex=Mutex()
    fun enqueue(context:Context) {
        // Room is the durable queue. An unavailable scheduler must never invalidate
        // a photo ACK or cause an imported video's original to be deleted.
        val manager=try {WorkManager.getInstance(context)} catch(e:IllegalStateException) {
            android.util.Log.w("VideoSync","Planificador no disponible; videos conservados para el próximo inicio")
            return
        }
        manager.enqueueUniqueWork("field-videos-9",ExistingWorkPolicy.APPEND_OR_REPLACE,
            OneTimeWorkRequestBuilder<VideoWorker>().setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).build())
    }
    suspend fun run(context:Context,dao:VideoDao=VideoDatabase.get(context).videos(),transport:VideoTransport=VideoHttp(),enabled:Boolean=BuildConfig.GBO_VIDEO_REMOTE_ENABLED,revision:suspend (String)->Long?={key->
        val db=InventoryDatabase.getInstance(context);val intent=db.automaticSyncDao().get(key)
        val parts=key.split('/');val valid=when(parts[0]){"SIC"->db.inventoryDao().recordById(parts[1])?.status=="ACTIVE";"SCAP"->db.scapDao().inspection(parts[1])?.status=="COMPLETE";else->db.pavementDao().sample(parts[1])?.status=="CLOSED"}
        intent?.takeIf {valid && SyncIntentPresentation.confirmed(it)}?.syncedRevision
    }):Boolean=mutex.withLock {
        var retry=false
        for(initial in dao.all().filter {!it.retired && it.state!="SYNCED"}) {
            var v=dao.get(initial.id) ?: continue
            if(com.tuempresa.inventariovial.localfiles.DeliveryDestination.deviceOnly(InventoryDatabase.getInstance(context),v.parentKey))continue
            if(!VideoPolicy.bridgeOnly(v.parentKey)){dao.saveSync(v.copy(state="SCOPE_RESTRICTED",error="Video histórico conservado; entrega restringida a Puentes / SCAP"));continue}
            if(!enabled){dao.saveSync(v.copy(state="BACKEND_REQUIRED",error="Subida remota de videos deshabilitada hasta validar Apps Script"));continue}
            val parent=revision(v.parentKey)
            if(parent==null){dao.saveSync(v.copy(state="WAITING_PARENT",error="Finaliza y sincroniza primero la ficha"));continue}
            try {
                require(File(v.localPath).length()==v.sizeBytes && VideoPolicy.hash(File(v.localPath))==v.sha256){"El original cambió; carga detenida"}
                if(!transport.available()){dao.saveSync(v.copy(state="BACKEND_REQUIRED",error="El servidor no anuncia videoProtocol=1"));continue}
                val begin=transport.begin(v,parent)
                require(begin.optBoolean("ok")){"Servidor rechazó inicio: ${begin.optString("code")}"}
                require(begin.optString("mediaId")==v.id && begin.optString("sha256")==v.sha256 && begin.optString("parentKey")==v.parentKey && begin.optString("fileId").isNotBlank())
                VideoNames.accept(context,v,begin)
                v=v.copy(fileId=begin.getString("fileId"),parentRevision=parent,state="UPLOADING",error=null)
                if(!begin.optBoolean("complete")) {
                    val url=begin.getString("sessionUrl");require(VideoPolicy.safeSession(url));v=v.copy(sessionUrl=url);dao.saveSync(v)
                    var offset=transport.offset(v)
                    while(offset<v.sizeBytes) {
                        if(dao.get(v.id)?.retired==true)break
                        val next=transport.chunk(v,offset,minOf(VideoPolicy.CHUNK_BYTES.toLong(),v.sizeBytes-offset).toInt())
                        require(next>offset && next<=v.sizeBytes){"La carga no avanzó"};offset=next
                        v=v.copy(uploadedBytes=offset);dao.saveSync(v)
                    }
                }
                if(dao.get(v.id)?.retired==true)continue
                val ack=transport.finish(v)
                require(ack.optBoolean("ok") && ack.optBoolean("complete") && ack.optInt("videoProtocol")==1 && ack.optString("mediaId")==v.id && ack.optString("parentKey")==v.parentKey && ack.optString("sha256")==v.sha256 && ack.optLong("sizeBytes")==v.sizeBytes && ack.optString("fileId")==v.fileId){"Confirmación de video incompleta"}
                VideoNames.accept(context,v,ack)
                dao.saveSync(v.copy(state="SYNCED",sessionUrl=null,uploadedBytes=v.sizeBytes,error=null))
            }catch(e:CancellationException){throw e}
            catch(e:ExpiredVideoSession){dao.saveSync(v.copy(state="ERROR",sessionUrl=null,error=e.message));retry=true}
            // Transport exceptions can contain a bearer resumable-session URL.
            // Only public, fixed diagnostics reach Room/the operator's screen.
            catch(e:Exception){dao.saveSync(v.copy(state="ERROR",error=if(e is java.io.IOException)
                "Conexión interrumpida durante la entrega del video. Reintenta; el original y la cola se conservan."
                else "No se pudo validar la entrega del video. Revisa la confirmación del servidor; el original se conserva."));retry=true}
        }
        retry
    }
}
class VideoWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params) {
    override suspend fun doWork()=withContext(Dispatchers.IO){if(VideoSync.run(applicationContext))Result.retry() else Result.success()}
}
