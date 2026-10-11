package com.tuempresa.inventariovial.core

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/** Durable outbox. Validation lives in the form; ACK lives here, never in local exports. */
@Entity(tableName="drive_sync_outbox")
data class DriveSyncIntent(@PrimaryKey val key:String, val kind:String, val recordId:String,
    val revision:Long, val syncedRevision:Long=0, val state:String="QUEUED",
    val origin:String="AUTOMATIC", val warningsConfirmed:Boolean=false,
    val lastSyncedAt:Long?=null, val error:String?=null, val receiptJson:String?=null,
    val preparedJson:String?=null, val attempts:Int=0)

@Dao
interface AutomaticSyncDao {
    @Query("SELECT * FROM drive_sync_outbox WHERE `key`=:key") suspend fun get(key:String):DriveSyncIntent?
    @Query("SELECT * FROM drive_sync_outbox") suspend fun all():List<DriveSyncIntent>
    @Query("SELECT * FROM drive_sync_outbox") fun observe():Flow<List<DriveSyncIntent>>
    @Upsert suspend fun put(intent:DriveSyncIntent)
    @Query("UPDATE drive_sync_outbox SET state=:state,error=:error,attempts=attempts+1 WHERE `key`=:key AND revision=:revision")
    suspend fun state(key:String,revision:Long,state:String,error:String?):Int
    @Query("UPDATE drive_sync_outbox SET preparedJson=:json WHERE `key`=:key AND revision=:revision")
    suspend fun prepared(key:String,revision:Long,json:String):Int
    @Query("UPDATE drive_sync_outbox SET state='SYNCED',syncedRevision=:revision,lastSyncedAt=:time,receiptJson=:receipt,error=NULL,attempts=0 WHERE `key`=:key AND revision=:revision")
    suspend fun acknowledge(key:String,revision:Long,time:Long,receipt:String):Int
}
