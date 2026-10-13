package com.tuempresa.inventariovial.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.security.MessageDigest
import java.util.UUID

@Entity(tableName="videos",indices=[Index("parentKey")])
data class VideoEntity(@PrimaryKey val id:String,val parentKey:String,val category:String,val categoryLabel:String,
    val localPath:String,val mimeType:String,val sizeBytes:Long,val sha256:String,val createdAt:Long,
    val state:String="BACKEND_REQUIRED",val error:String?=null,val fileId:String?=null,val sessionUrl:String?=null,
    val uploadedBytes:Long=0,val parentRevision:Long=0,val retired:Boolean=false)
@Dao interface VideoDao {
    @Query("SELECT * FROM videos WHERE parentKey=:key ORDER BY createdAt,id") fun observe(key:String):Flow<List<VideoEntity>>
    @Query("SELECT * FROM videos ORDER BY createdAt,id") suspend fun all():List<VideoEntity>
    @Query("SELECT * FROM videos WHERE id=:id") suspend fun get(id:String):VideoEntity?
    @Query("UPDATE videos SET state=:state,error=:error,fileId=:fileId,sessionUrl=:url,uploadedBytes=:bytes,parentRevision=:revision WHERE id=:id AND retired=0")
    suspend fun sync(id:String,state:String,error:String?,fileId:String?,url:String?,bytes:Long,revision:Long)
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun put(video:VideoEntity)
}
/** Separate additive database: no destructive migration of field inventory or users. */
@Database(entities=[VideoEntity::class],version=1,exportSchema=true)
abstract class VideoDatabase:RoomDatabase() {
    abstract fun videos():VideoDao
    companion object {
        @Volatile private var instance:VideoDatabase?=null
        fun get(context:Context)=instance ?: synchronized(this) {instance ?: Room.databaseBuilder(context.applicationContext,VideoDatabase::class.java,"field_video_9.db").build().also {instance=it}}
    }
}
object VideoPolicy {
    fun bridgeOnly(key:String)=key.matches(Regex("SCAP/[A-Za-z0-9_-]{1,80}"))
    const val MAX_BYTES=32L*1024*1024
    const val CHUNK_BYTES=512*1024
    val mimeTypes=setOf("video/mp4","video/webm","video/3gpp")
    fun hash(file:File):String {val digest=MessageDigest.getInstance("SHA-256");file.inputStream().use {input->val b=ByteArray(8192);while(true){val n=input.read(b);if(n<0)break;digest.update(b,0,n)}};return digest.digest().joinToString(""){"%02x".format(it)}}
    fun safeSession(url:String):Boolean=runCatching {val uri=java.net.URI(url);uri.scheme=="https" && uri.host=="www.googleapis.com" && uri.port in listOf(-1,443) && uri.userInfo==null && uri.path.startsWith("/upload/drive/v3/files")}.getOrDefault(false)
    fun rangeOffset(range:String?,size:Long):Long {
        if(range==null)return 0
        val m=Regex("bytes=0-([0-9]+)").matchEntire(range) ?: error("Respuesta de reanudación inválida")
        val next=m.groupValues[1].toLong()+1;require(next in 0..size);return next
    }
}
suspend fun VideoDao.saveSync(v:VideoEntity)=sync(v.id,v.state,v.error,v.fileId,v.sessionUrl,v.uploadedBytes,v.parentRevision)
class VideoStorage(private val context:Context,private val dao:VideoDao=VideoDatabase.get(context).videos(),private val inspect:(File)->Unit={file->
    val retriever=MediaMetadataRetriever();try {retriever.setDataSource(file.path);require(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)=="yes"){"El archivo no contiene una pista de video"}} finally {retriever.release()}
},private val schedule:(Context)->Unit=VideoSync::enqueue) {
    suspend fun import(key:String,category:String,label:String,uri:Uri):VideoEntity {
        require(VideoPolicy.bridgeOnly(key)) {"Los videos son exclusivos de Puentes / SCAP."}
        // Labels such as "Carretera / tablero" are real SCAP categories.
        // Only the validated category key is used as identity/path component.
        require(category.matches(Regex("[A-Z0-9_]{1,80}")) && label.isNotBlank() && label.length<=120 && label.none {it<' '})
        val mime=context.contentResolver.getType(uri) ?: error("No se conoce el MIME del video")
        require(mime in VideoPolicy.mimeTypes){"Formato no admitido: usa MP4, WebM o 3GP"}
        val id=UUID.randomUUID().toString();val directory=File(context.filesDir,"videos").apply {check(isDirectory || mkdirs())}
        val suffix=when(mime){"video/webm"->"webm";"video/3gpp"->"3gp";else->"mp4"}
        val file=File(directory,"$id.$suffix")
        try {
            context.contentResolver.openInputStream(uri).use {input->requireNotNull(input);file.outputStream().use {output->
                var count=0L;val b=ByteArray(8192);while(true){val n=input.read(b);if(n<0)break;count+=n;require(count<=VideoPolicy.MAX_BYTES){"Video excede 32 MiB; recorta o reduce su calidad"};output.write(b,0,n)}
                require(count>0);output.fd.sync()
            }}
            inspect(file)
            val video=VideoEntity(id,key,category,label,file.path,mime,file.length(),VideoPolicy.hash(file),System.currentTimeMillis())
            dao.put(video)
            runCatching {schedule(context)}.onFailure {android.util.Log.w("VideoStorage","Original y cola conservados; programación pendiente")}
            return video
        }catch(e:Exception){file.delete();throw e}
    }
    suspend fun retire(id:String) {dao.get(id)?.let {dao.put(it.copy(retired=true,state="RETIRED",sessionUrl=null,error=null))}}
    suspend fun retry(id:String) {dao.get(id)?.takeIf {!it.retired && VideoPolicy.bridgeOnly(it.parentKey)}?.let {dao.saveSync(it.copy(state="QUEUED",error=null));VideoSync.enqueue(context)}}
}
