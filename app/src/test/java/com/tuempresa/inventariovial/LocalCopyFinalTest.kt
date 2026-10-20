package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.localfiles.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowStatFs
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class)
class LocalCopyFinalTest {
    private val c get()=ApplicationProvider.getApplicationContext<Context>()
    private fun db()=Room.inMemoryDatabaseBuilder(c,InventoryDatabase::class.java).build()
    private class Storage:SharedDocumentStore {
        val bytes=mutableMapOf<String,ByteArray>();var writes=0
        override fun reserve(f:LocalSharedFile,knownUri:Uri?)=SharedDocumentStore.Slot(Uri.parse("content://test/${f.id}"),bytes.containsKey("content://test/${f.id}"))
        override fun write(uri:Uri,source:File){writes++;bytes[uri.toString()]=source.readBytes()}
        override fun digest(uri:Uri)=bytes.getValue(uri.toString()).let{it.size.toLong() to com.tuempresa.inventariovial.delivery.sha256(it)}
        override fun publish(slot:SharedDocumentStore.Slot,file:LocalSharedFile)=slot.uri
    }
    private fun plan(j:LocalExportJob):LocalPlan {
        val f=File(c.filesDir,"TEST_FINAL_${j.id}.txt").apply{writeText("FICTICIO revision ${j.revision}")};val digest=fileDigest(f)
        return LocalPlan(listOf(LocalSharedFile(j.id,j.id,j.recordKey,"Download/TEST/R_${j.revision}/","FICTICIO.txt","text/plain",digest.first,digest.second,f.path)),emptyList())
    }
    @Test fun twoSimultaneousRequestsAndDrainsOnlyCreateOneJobAndOneWrite()=runBlocking {db().use{d->
        ShadowStatFs.registerStats(c.filesDir.path,1_000_000,900_000,900_000)
        val s=visualSample().copy(updatedAt=1);d.pavementDao().saveSample(s)
        coroutineScope{repeat(12){launch(Dispatchers.IO){LocalCopies.requestCurrent(c,d,"PAVEMENT",s.id)}}}
        assertEquals(1,d.localFilesDao().jobs().size);val storage=Storage()
        coroutineScope{repeat(2){launch{LocalCopyDrain.run(c,d,storage,prepare={_,_,j->plan(j)})}}}
        assertEquals(1,storage.writes);val j=d.localFilesDao().jobs().single();assertEquals("READY",j.state)
        LocalCopies.retry(c,d,j.id);LocalCopies.retry(c,d,j.id);LocalCopyDrain.run(c,d,storage)
        assertEquals(1,storage.writes);assertTrue(d.automaticSyncDao().all().isEmpty())
    }}
    @Test fun editDuringPreparationFailsVisiblyKeepsPriorCopyAndLocalRetryUsesCurrentRevision()=runBlocking {db().use{d->
        ShadowStatFs.registerStats(c.filesDir.path,1_000_000,900_000,900_000)
        val s=visualSample().copy(updatedAt=1);d.pavementDao().saveSample(s);val storage=Storage()
        LocalCopies.requestCurrent(c,d,"PAVEMENT",s.id);LocalCopyDrain.run(c,d,storage,prepare={_,_,j->plan(j)})
        val first=d.localFilesDao().jobs().single();assertEquals("READY",first.state)
        d.pavementDao().saveSample(s.copy(updatedAt=2));LocalCopies.requestCurrent(c,d,"PAVEMENT",s.id)
        val started=CompletableDeferred<Unit>();val changed=CompletableDeferred<Unit>()
        val worker=async {LocalCopyDrain.run(c,d,storage,prepare={_,_,j->started.complete(Unit);changed.await();plan(j)})}
        started.await();d.pavementDao().saveSample(s.copy(updatedAt=3));changed.complete(Unit);worker.await()
        val failed=d.localFilesDao().jobs().single{it.revision==2L}
        assertEquals("FAILED",failed.state);assertTrue(LocalCopyPresentation.result(failed).contains("cambió durante"))
        assertTrue(LocalCopyPresentation.context(first,3,true).contains("Revisión anterior"))
        assertTrue(LocalCopyPresentation.context(failed,3,true).contains("Revisión anterior"))
        assertEquals("READY",d.localFilesDao().job(first.id)!!.state);assertEquals(1,storage.writes)
        LocalCopies.retry(c,d,failed.id)
        assertEquals("FAILED",d.localFilesDao().job(failed.id)!!.state)
        assertEquals(3L,d.localFilesDao().jobs().single{it.state=="QUEUED"}.revision)
        LocalCopyDrain.run(c,d,storage,prepare={_,_,j->plan(j)})
        assertEquals(2,storage.writes);assertTrue(d.automaticSyncDao().all().isEmpty())
    }}
    @Test fun reopeningRetainsSuccessAndFailureWithoutUiRequestsOrRemoteJobs()=runBlocking {
        val name="copy_${UUID.randomUUID()}.db"
        fun open()=Room.databaseBuilder(c,InventoryDatabase::class.java,name).build()
        val s=visualSample().copy(updatedAt=2)
        open().use{d->d.pavementDao().saveSample(s)
            d.localFilesDao().saveJob(LocalExportJob("OLD","PAVEMENT/${s.id}","PAVEMENT",s.id,1,state="READY",completed=4,total=4))
            d.localFilesDao().saveJob(LocalExportJob("NOW","PAVEMENT/${s.id}","PAVEMENT",s.id,2,state="FAILED",message="Copia local fallida: TEST_REAL_FAILURE"))}
        open().use{d->
            val jobs=d.localFilesDao().jobs();assertEquals(2,jobs.size)
            val old=jobs.single{it.id=="OLD"};val now=jobs.single{it.id=="NOW"}
            assertTrue(LocalCopyPresentation.context(old,2,true).contains("anterior"))
            assertTrue(LocalCopyPresentation.context(now,2,true).contains("actual"))
            assertEquals(1,Regex("Copia local completada").findAll(LocalCopyPresentation.result(old)).count())
            assertTrue(LocalCopyPresentation.result(now).contains("TEST_REAL_FAILURE"))
            assertTrue(d.automaticSyncDao().all().isEmpty())
        }
    }
    @Test fun preparationUnknownFileCountIsNotShownAsAnAcknowledgement() {
        val text=SyncProgressPresentation.files(SyncProgress(1,"PREPARING",0,0,1))
        assertFalse(text.contains("0 / 0"));assertTrue(text.contains("sin ACK"))
    }
}
