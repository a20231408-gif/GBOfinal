package com.tuempresa.inventariovial.localfiles

import android.content.Context
import androidx.room.InvalidationTracker
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicLong
import com.tuempresa.inventariovial.data.database.InventoryDatabase

/** Outbox inserts commit with the capture. No scan or export of historical records on open. */
object AutomaticLocalDocuments {
    const val QUIET_MS=5000L
    fun install(sql:SupportSQLiteDatabase,enabled:Boolean) {
        sql.execSQL("INSERT OR REPLACE INTO device_delivery_choices (`key`,destination,selectedBy,selectedAt) VALUES ('LOCAL_AUTOMATIC',?,'DEVICE_SETTING',0)",arrayOf(if(enabled)"ON"else"OFF"))
        for((table,kind,eligible) in listOf(Triple("inventory_records","SIC","NEW.sicCode!='SCAP' AND NEW.status IN ('DRAFT','ACTIVE') AND NOT EXISTS (SELECT 1 FROM scap_inspections WHERE roadRecordId=NEW.id)"),Triple("scap_inspections","SCAP","NEW.status IN ('DRAFT','IN_PROGRESS','COMPLETE')"),Triple("pavement_samples","PAVEMENT","NEW.status IN ('DRAFT','CLOSED')"))) {
            for(event in listOf("INSERT","UPDATE OF updatedAt")) {
                val name="gbo_local_${table}_${event.substringBefore(' ').lowercase()}"
                sql.execSQL("""CREATE TRIGGER IF NOT EXISTS $name AFTER $event ON $table
                    WHEN $eligible AND (SELECT destination FROM device_delivery_choices WHERE `key`='LOCAL_AUTOMATIC')='ON'
                    ${if(event.startsWith("UPDATE"))"AND NEW.updatedAt!=OLD.updatedAt"else""}
                    BEGIN
                    UPDATE local_export_jobs SET state='SUPERSEDED',message='Agrupada en una revisión más reciente'
                        WHERE recordKey='$kind/'||NEW.id AND automatic=1 AND state='QUEUED' AND manifestJson IS NULL AND revision!=NEW.updatedAt;
                    INSERT OR IGNORE INTO local_export_jobs (id,recordKey,kind,recordId,revision,scope,state,message,manifestJson,automatic,createdAt,completed,total)
                        VALUES ('AUTO/$kind/'||NEW.id||'/'||NEW.updatedAt,'$kind/'||NEW.id,'$kind',NEW.id,NEW.updatedAt,'FULL','QUEUED',NULL,NULL,1,CAST(strftime('%s','now') AS INTEGER)*1000,0,0);
                    END""")
            }
        }
    }
    fun observe(c:Context,db:InventoryDatabase) {
        val sequence=AtomicLong(0);val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
        db.invalidationTracker.addObserver(object:InvalidationTracker.Observer("local_export_jobs") {
            override fun onInvalidated(tables:Set<String>){scope.launch{runCatching{val next=db.localFilesDao().requestSequence();if(next>sequence.getAndAccumulate(next){a,b->maxOf(a,b)})LocalCopies.wake(c)}}}
        })
        LocalCopies.wake(c)
    }
}
