package com.tuempresa.inventariovial.core

import android.content.Context
import androidx.work.*
import com.tuempresa.inventariovial.DriveUploadPolicy
import com.tuempresa.inventariovial.auth.AuthSessionManager
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.pavement.*
import com.tuempresa.inventariovial.scap.export.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object DriveReceiptStore {
    private fun store(context:Context)=context.getSharedPreferences("drive_receipts_v2",Context.MODE_PRIVATE)
    fun stamp(context:Context,s:PavementSampleEntity):String = s.updatedAt.toString()+":"+
        PavementGeneratedFiles.records(context).filter { it.sampleId==s.id && it.category.startsWith("SIC_") }
            .groupBy { it.category }.map { (category,rows)->category+":"+rows.maxBy { it.createdAt }.sha256 }.sorted().joinToString(";")
    fun state(context:Context,s:PavementSampleEntity):String {
        val json=store(context).getString("sample/${s.id}",null)?.let(::JSONObject) ?: return "PENDING"
        return if(json.optString("contentRevision")!=stamp(context,s)) "PENDING" else json.getString("state")
    }
    fun sample(context:Context,s:PavementSampleEntity,state:String) {
        check(store(context).edit().putString("sample/${s.id}",JSONObject().put("revision",s.updatedAt).put("contentRevision",stamp(context,s)).put("state",state).toString()).commit())
    }
    fun file(context:Context,path:String,hash:String,driveId:String) {
        check(store(context).edit().putString("file/$path",JSONObject().put("hash",hash).put("driveId",driveId).toString()).commit())
    }
    fun receipt(context:Context,path:String):JSONObject?=store(context).getString("file/$path",null)?.let(::JSONObject)
}

object PavementSyncAdapter {
    fun enqueue(context:Context,s:PavementSampleEntity,retry:Boolean=false) {
        require(s.status=="CLOSED") { "La muestra debe estar finalizada" }
        DriveUploadPolicy.requireConfiguration()
        val state=DriveReceiptStore.state(context,s)
        if(state=="SYNCED" || state=="UPLOADING") return
        val request=OneTimeWorkRequestBuilder<PavementSyncWorker>()
            .setInputData(workDataOf("sampleId" to s.id))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).build()
        DriveReceiptStore.sample(context,s,"QUEUED")
        WorkManager.getInstance(context).enqueueUniqueWork("pavement-drive-${s.id}",
            if(retry) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,request)
    }
}

class PavementSyncWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params) {
    override suspend fun doWork():Result=withContext(Dispatchers.IO) {
        val db=InventoryDatabase.getInstance(applicationContext)
        val s=inputData.getString("sampleId")?.let { db.pavementDao().sample(it) } ?: return@withContext Result.failure()
        if(s.status!="CLOSED") return@withContext Result.failure()
        if(DriveReceiptStore.state(applicationContext,s)=="SYNCED") return@withContext Result.success()
        try {
            DriveReceiptStore.sample(applicationContext,s,"UPLOADING")
            PavementDriveUploader.uploadSample(applicationContext,PavementRepository(db,AuthSessionManager()),s,db.pavementDao().damages(s.id))
            // Uploader records SYNCED only after every file has a valid ACK.
            Result.success()
        } catch(e:CancellationException) { throw e }
        catch(e:Exception) {
            DriveReceiptStore.sample(applicationContext,s,"ERROR")
            if(runAttemptCount<4) Result.retry() else Result.failure()
        }
    }
}

object UnifiedSyncAdapter {
    suspend fun enqueue(context:Context,retryOnly:Boolean):Int=withContext(Dispatchers.IO) {
        val db=InventoryDatabase.getInstance(context)
        var count=0
        db.inventoryDao().observeAllDeliveries().first().filter { if(retryOnly) it.status=="ERROR" else it.status in setOf("QUEUED","ERROR") }
            .forEach { DriveDeliveryRetry.enqueue(context,it,it.status=="ERROR").result.get();count++ }
        db.scapDao().allSnapshots().filter {
            it.roadRecord?.status in setOf("ACTIVE","DRAFT") && it.inspection.status=="COMPLETE" && it.inspection.syncStatus in (if(retryOnly) setOf("ERROR") else setOf("QUEUED","ERROR","PENDING")) &&
                it.values("delivery")["revision"]==it.inspection.updatedAt.toString() && ScapHumanLayout.saved(it)!=null
        }.forEach { ScapDriveSync.enqueue(context,it,ScapHumanLayout.saved(it));count++ }
        db.pavementDao().observeSamples().first().filter { it.status=="CLOSED" && DriveReceiptStore.state(context,it) in
            (if(retryOnly) setOf("ERROR") else setOf("PENDING","QUEUED","ERROR")) }
            .forEach { PavementSyncAdapter.enqueue(context,it,retryOnly);count++ }
        count
    }
}
