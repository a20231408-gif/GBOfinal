package com.tuempresa.inventariovial
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.tuempresa.inventariovial.core.AutomaticSync
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import kotlinx.coroutines.runBlocking
/** Migrates already scheduled Release-15 jobs to the persistent per-record outbox. */
class DriveUploadWorker(context:Context,params:WorkerParameters):Worker(context,params) {
    override fun doWork():Result=runBlocking {
        val db=InventoryDatabase.getInstance(applicationContext)
        val delivery=inputData.getString("deliveryId")?.let {db.inventoryDao().delivery(it)}
        val records=if(delivery!=null)db.inventoryDao().activeSnapshots().filter {it.record.routeCode==delivery.routeCode && it.record.sicCode!="SCAP"}.map {it.record.id}
            else listOfNotNull(inputData.getString("photoId")?.let {db.inventoryDao().photoById(it)?.recordId}
                ?: inputData.getString("photoPath")?.let {db.inventoryDao().photoByPath(it)?.recordId})
        for(id in records)if(db.inventoryDao().recordById(id)?.status=="ACTIVE")AutomaticSync.manual(applicationContext,"SIC",id)
        Result.success()
    }
}
