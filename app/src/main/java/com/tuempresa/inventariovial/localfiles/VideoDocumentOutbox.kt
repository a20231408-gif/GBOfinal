package com.tuempresa.inventariovial.localfiles

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.withTransaction
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.media.VideoDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A tiny transactional outbox in the video DB bridges the two existing databases safely. */
object VideoDocumentOutbox {
    fun install(sql:SupportSQLiteDatabase){
        sql.execSQL("CREATE TABLE IF NOT EXISTS gbo_local_video_changes (`key` TEXT NOT NULL PRIMARY KEY,token TEXT NOT NULL)")
        sql.execSQL("""CREATE TRIGGER IF NOT EXISTS gbo_local_video_insert AFTER INSERT ON videos BEGIN
            INSERT OR REPLACE INTO gbo_local_video_changes (`key`,token) VALUES (NEW.parentKey,NEW.id||'/'||NEW.retired); END""")
        sql.execSQL("""CREATE TRIGGER IF NOT EXISTS gbo_local_video_retire AFTER UPDATE OF retired ON videos WHEN NEW.retired!=OLD.retired BEGIN
            INSERT OR REPLACE INTO gbo_local_video_changes (`key`,token) VALUES (NEW.parentKey,NEW.id||'/'||NEW.retired); END""")
    }
    suspend fun transfer(c:Context)=withContext(Dispatchers.IO){
        val video=VideoDatabase.get(c);val sql=video.openHelper.writableDatabase
        val pending=sql.query("SELECT `key`,token FROM gbo_local_video_changes").use{q->buildList{while(q.moveToNext())add(q.getString(0) to q.getString(1))}}
        if(pending.isEmpty())return@withContext
        val db=InventoryDatabase.getInstance(c)
        for((key,token)in pending){
            if(LocalCopySettings.enabled(c)&&key.startsWith("SCAP/")){
                val id=key.removePrefix("SCAP/");val revision=DeliveryDestination.revision(db,"SCAP",id) ?: continue
                val suffix=com.tuempresa.inventariovial.delivery.sha256(token.toByteArray())
                val jobId="VIDEO_LOCAL/$key/$revision/$suffix"
                db.withTransaction{
                    val dao=db.localFilesDao();val base=dao.jobForRevision(key,revision,"FULL")
                    if(base==null||base.state!="QUEUED"||base.manifestJson!=null){
                        for(old in dao.jobs().filter{it.recordKey==key&&it.revision==revision&&it.automatic&&it.scope.startsWith("FULL_MEDIA_")&&it.state=="QUEUED"&&it.manifestJson==null&&it.id!=jobId})dao.saveJob(old.copy(state="SUPERSEDED",message="Agrupada con los medios más recientes"))
                        if(dao.job(jobId)==null)dao.saveJob(LocalExportJob(jobId,key,"SCAP",id,revision,scope="FULL_MEDIA_$suffix",automatic=true))
                    }
                }
            }
            // A crash before this delete replays the identical job ID; it cannot duplicate the request.
            sql.execSQL("DELETE FROM gbo_local_video_changes WHERE `key`=? AND token=?",arrayOf(key,token))
        }
    }
}
