package com.tuempresa.inventariovial.localfiles

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName="device_delivery_choices")
data class DeviceDeliveryChoice(@PrimaryKey val key:String,val destination:String,val selectedBy:String,val selectedAt:Long)

@Entity(tableName="local_export_jobs",indices=[Index("recordKey")])
data class LocalExportJob(@PrimaryKey val id:String,val recordKey:String,val kind:String,val recordId:String,val revision:Long,
    val scope:String="FULL",val state:String="QUEUED",val message:String?=null,val manifestJson:String?=null,
    val automatic:Boolean=false,val createdAt:Long=System.currentTimeMillis(),val completed:Int=0,val total:Int=0)

@Entity(tableName="local_shared_files",indices=[Index("jobId"),Index("recordKey")])
data class LocalSharedFile(@PrimaryKey val id:String,val jobId:String,val recordKey:String,val relativePath:String,val name:String,
    val mimeType:String,val size:Long,val sha256:String,val source:String,val uri:String?=null,val state:String="PENDING")

@Dao interface LocalFilesDao {
    @Query("SELECT * FROM device_delivery_choices WHERE `key`=:key") suspend fun choice(key:String):DeviceDeliveryChoice?
    @Query("SELECT * FROM device_delivery_choices") fun choices():Flow<List<DeviceDeliveryChoice>>
    @Upsert suspend fun saveChoice(choice:DeviceDeliveryChoice)
    @Query("SELECT * FROM local_export_jobs ORDER BY createdAt,id") suspend fun jobs():List<LocalExportJob>
    @Query("SELECT * FROM local_export_jobs WHERE state IN ('QUEUED','RUNNING') ORDER BY createdAt,id LIMIT :limit") suspend fun pending(limit:Int):List<LocalExportJob>
    @Query("SELECT COUNT(*) FROM local_export_jobs WHERE state IN ('QUEUED','RUNNING')") suspend fun pendingCount():Int
    @Query("SELECT recordKey||'/'||revision FROM local_export_jobs WHERE scope LIKE 'FULL%'") suspend fun requestedRevisions():List<String>
    @Query("SELECT * FROM local_export_jobs WHERE state!='SUPERSEDED' ORDER BY createdAt DESC,id") fun observeJobs():Flow<List<LocalExportJob>>
    @Query("SELECT * FROM local_export_jobs WHERE (:key IS NULL OR recordKey=:key) AND state!='SUPERSEDED' ORDER BY createdAt DESC,id LIMIT 60") fun observeJobsFor(key:String?):Flow<List<LocalExportJob>>
    @Query("SELECT * FROM local_export_jobs WHERE id=:id") suspend fun job(id:String):LocalExportJob?
    @Query("SELECT * FROM local_export_jobs WHERE recordKey=:key AND revision=:revision AND scope=:scope AND state!='SUPERSEDED' ORDER BY createdAt DESC LIMIT 1") suspend fun jobForRevision(key:String,revision:Long,scope:String):LocalExportJob?
    @Query("SELECT IFNULL(MAX(rowid),0) FROM local_export_jobs") suspend fun requestSequence():Long
    @Upsert suspend fun saveJob(job:LocalExportJob)
    @Query("SELECT * FROM local_shared_files WHERE jobId=:id ORDER BY name,id") suspend fun files(id:String):List<LocalSharedFile>
    @Query("SELECT * FROM local_shared_files WHERE recordKey=:key") suspend fun filesForRecord(key:String):List<LocalSharedFile>
    @Query("SELECT COUNT(*) FROM local_shared_files WHERE recordKey!=:key AND state!='CLEANED' AND (uri=:uri OR (relativePath=:path AND name=:name))")
    suspend fun otherFileReferences(key:String,uri:String,path:String,name:String):Int
    @Query("""SELECT COUNT(*) FROM local_shared_files a JOIN local_shared_files b
        ON a.recordKey!=b.recordKey AND (
            (a.source!='' AND a.source=b.source) OR
            (a.state!='CLEANED' AND b.state!='CLEANED' AND (
                (a.uri IS NOT NULL AND a.uri=b.uri) OR
                (a.relativePath=b.relativePath AND a.name=b.name))))
        WHERE a.recordKey=:key""")
    suspend fun sharedDraftReferences(key:String):Int
    @Query("""WITH refs AS (
        SELECT 'SIC/'||recordId AS owner,localPath AS path FROM photos
        UNION SELECT 'SIC/'||recordId,originalPath FROM photos
        UNION SELECT 'SIC/'||recordId,stampedPath FROM photos
        UNION SELECT 'PAVEMENT/'||d.sampleId,p.localPath FROM pavement_damage_photos p
            JOIN pavement_damages d ON p.damageId=d.id)
        SELECT COUNT(*) FROM refs a JOIN refs b ON a.path=b.path AND a.owner!=b.owner
        WHERE a.owner=:key AND a.path IS NOT NULL AND a.path!=''""")
    suspend fun sharedDraftPhotoReferences(key:String):Int
    @Query("SELECT * FROM local_shared_files ORDER BY rowid DESC") fun observeFiles():Flow<List<LocalSharedFile>>
    @Query("SELECT * FROM local_shared_files WHERE (:key IS NULL OR recordKey=:key) ORDER BY rowid DESC LIMIT 200") fun observeFilesFor(key:String?):Flow<List<LocalSharedFile>>
    @Upsert suspend fun saveFile(file:LocalSharedFile)
}
