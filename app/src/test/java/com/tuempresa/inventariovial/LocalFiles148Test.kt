package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.auth.*
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.localfiles.*
import com.tuempresa.inventariovial.pavement.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import org.robolectric.shadows.ShadowStatFs
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class,shadows=[SyntheticPdf146::class])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LocalFiles148Test {
    private val c get()=ApplicationProvider.getApplicationContext<Context>()
    private val user=AuthSession("TEST_USER","test","FICTICIO",UserRole.ENGINEER)
    @Before fun setUp()=kotlinx.coroutines.runBlocking{ShadowStatFs.registerStats(c.filesDir.path,1_000_000,900_000,900_000);LocalCopySettings.enabled(c,false)}
    private fun db()=Room.inMemoryDatabaseBuilder(c,InventoryDatabase::class.java).build()
    private fun sample(id:String="TEST_LOCAL_148",status:String="DRAFT",revision:Long=100)=PavementSampleEntity(id,"TRAMO_TEST","PE-22A",1,"Derecho","CD","00+000","00+020",10.0,20.0,"RIGID","2026-10-16",user.id,"FICTICIO",status=status,updatedAt=revision,slabRows=4,slabColumns=2)
    private fun source(name:String="original.jpg")=File(c.filesDir,"images/$name").apply{parentFile!!.mkdirs();writeBytes(byteArrayOf(0xff.toByte(),0xd8.toByte(),1,2,3,0xff.toByte(),0xd9.toByte()))}
    private class FakeStorage:SharedDocumentStore {
        data class Entry(val uri:Uri,var bytes:ByteArray=byteArrayOf(),var published:Boolean=false)
        val entries=linkedMapOf<String,Entry>();var writes=0;var shortWrite=false;var deny=false;var cancelBeforePublish=false;var cancelAfterPublish=false
        override fun reserve(file:LocalSharedFile,knownUri:Uri?):SharedDocumentStore.Slot{if(deny)throw SecurityException("DENIED");val e=entries.getOrPut(file.relativePath+file.name){Entry(Uri.parse("content://test.local/${entries.size+1}"))};return SharedDocumentStore.Slot(e.uri,e.published)}
        private fun find(uri:Uri)=entries.values.single{it.uri==uri}
        override fun write(uri:Uri,source:File){writes++;find(uri).bytes=source.readBytes().let{if(shortWrite)it.take(2).toByteArray()else it}}
        override fun digest(uri:Uri)=find(uri).bytes.let{it.size.toLong() to com.tuempresa.inventariovial.delivery.sha256(it)}
        override fun publish(slot:SharedDocumentStore.Slot,file:LocalSharedFile):Uri{if(cancelBeforePublish){cancelBeforePublish=false;throw CancellationException("SIMULATED_PROCESS_STOP")};find(slot.uri).published=true;if(cancelAfterPublish){cancelAfterPublish=false;throw CancellationException("SIMULATED_LOST_PUBLISH_RESULT")};return slot.uri}
    }
    private suspend fun job(db:InventoryDatabase,files:List<File> = listOf(source()),id:String="JOB_148",issues:List<String> = emptyList()):LocalExportJob {
        val j=LocalExportJob(id,"SIC/TEST_RECORD","SIC","TEST_RECORD",100,scope="DOCUMENT")
        val entries=files.map{f->val h=fileDigest(f);LocalSharedFile(id+f.name,id,j.recordKey,"Download/GBO_Ruta_Clara/SIC/PE-22A/TEST_RECORD/R_100/",f.name,"image/jpeg",h.first,h.second,f.path)}
        return j.copy(manifestJson=LocalPlan(entries,issues).json()).also{db.localFilesDao().saveJob(it)}
    }
    @Test fun originalBytesCopiedAndReplaysReuseUriWithoutNewWrites()=runBlocking{db().use {d->
        val f=source();val before=f.readBytes();val j=job(d,listOf(f));val s=FakeStorage();LocalCopyDrain.run(c,d,s)
        assertEquals("READY",d.localFilesDao().job(j.id)!!.state);assertArrayEquals(before,s.entries.values.single().bytes);assertArrayEquals(before,f.readBytes());assertTrue(s.entries.values.single().published)
        LocalCopies.retry(c,d,j.id);LocalCopyDrain.run(c,d,s);assertEquals(1,s.entries.size);assertEquals(1,s.writes)
    }}
    @Test fun shortWriteCannotBeMarkedReadyAndRetryReusesPendingSlot()=runBlocking{db().use{d->
        val j=job(d);val s=FakeStorage().apply{shortWrite=true};LocalCopyDrain.run(c,d,s)
        assertEquals("PARTIAL",d.localFilesDao().job(j.id)!!.state);assertFalse(s.entries.values.single().published);assertTrue(d.localFilesDao().files(j.id).none{it.state=="READY"})
        s.shortWrite=false;LocalCopies.retry(c,d,j.id);LocalCopyDrain.run(c,d,s);assertEquals("READY",d.localFilesDao().job(j.id)!!.state);assertEquals(1,s.entries.size)
    }}
    @Test fun completedBytesStillPendingMustBePublishedAfterProcessInterruption()=runBlocking{db().use{d->
        val j=job(d);val s=FakeStorage().apply{cancelBeforePublish=true}
        try{LocalCopyDrain.run(c,d,s);fail()}catch(_:CancellationException){}
        assertEquals("RUNNING",d.localFilesDao().job(j.id)!!.state);assertEquals("WRITING",d.localFilesDao().files(j.id).single().state);assertFalse(s.entries.values.single().published)
        LocalCopyDrain.run(c,d,s);assertEquals(1,s.entries.size);assertTrue(s.entries.values.single().published);assertEquals("READY",d.localFilesDao().job(j.id)!!.state)
    }}
    @Test fun lostPublicationResultDoesNotCreateAnotherFile()=runBlocking{db().use{d->
        val j=job(d);val s=FakeStorage().apply{cancelAfterPublish=true};try{LocalCopyDrain.run(c,d,s);fail()}catch(_:CancellationException){}
        assertTrue(s.entries.values.single().published);LocalCopyDrain.run(c,d,s);assertEquals(1,s.entries.size);assertEquals(1,s.writes);assertEquals("READY",d.localFilesDao().job(j.id)!!.state)
    }}
    @Test fun partialHistoryExportsValidFilesAndReportsMissingOnes()=runBlocking{db().use{d->
        val j=job(d,issues=listOf("Falta fotografía TEST_MISSING"));val s=FakeStorage();LocalCopyDrain.run(c,d,s)
        assertEquals("PARTIAL",d.localFilesDao().job(j.id)!!.state);assertTrue(d.localFilesDao().job(j.id)!!.message!!.contains("TEST_MISSING"));assertEquals("READY",d.localFilesDao().files(j.id).single().state)
    }}
    @Test fun permissionDeniedPreservesOriginalAndAllowsExplicitRetry()=runBlocking{db().use{d->
        val f=source();val h=fileDigest(f);val j=job(d,listOf(f));val s=FakeStorage().apply{deny=true};LocalCopyDrain.run(c,d,s);assertEquals("PARTIAL",d.localFilesDao().job(j.id)!!.state);assertEquals(h,fileDigest(f));assertTrue(s.entries.isEmpty())
        s.deny=false;LocalCopies.retry(c,d,j.id);LocalCopyDrain.run(c,d,s);assertEquals("READY",d.localFilesDao().job(j.id)!!.state)
    }}
    @Test fun insufficientSpaceNeverPublishesOrLosesTheSource()=runBlocking{db().use{d->
        val f=source();val j=job(d,listOf(f));ShadowStatFs.registerStats(c.filesDir.path,100,1,1);val s=FakeStorage();LocalCopyDrain.run(c,d,s)
        assertEquals("FAILED",d.localFilesDao().job(j.id)!!.state);assertTrue(f.isFile);assertTrue(s.entries.isEmpty())
    }}
    @Test fun externallyChangedPublishedRevisionIsNotOverwritten()=runBlocking{db().use{d->
        val j=job(d);val s=FakeStorage();LocalCopyDrain.run(c,d,s);s.entries.values.single().bytes=byteArrayOf(99);LocalCopies.retry(c,d,j.id);LocalCopyDrain.run(c,d,s)
        assertEquals("PARTIAL",d.localFilesDao().job(j.id)!!.state);assertArrayEquals(byteArrayOf(99),s.entries.values.single().bytes);assertEquals(1,s.writes)
    }}
    @Test fun editingCreatesANewRevisionWithoutOverwritingTheOldExport()=runBlocking{db().use{d->
        val a=job(d,listOf(source("a.jpg")),"A");val s=FakeStorage();LocalCopyDrain.run(c,d,s)
        val b=job(d,listOf(source("b.jpg")),"B");LocalCopyDrain.run(c,d,s);assertEquals(2,s.entries.size);assertEquals("READY",d.localFilesDao().job(a.id)!!.state);assertEquals("READY",d.localFilesDao().job(b.id)!!.state)
    }}
    @Test fun noAutomaticHistoryScanAndSettingsDefaultOff()=runBlocking{db().use{d->
        d.syncContext=c;d.pavementDao().saveSample(sample(status="CLOSED"));assertFalse(LocalCopySettings.enabled(c));LocalCopies.onClosed(d,"PAVEMENT",sample().id,100,"AUTOMATIC");assertTrue(d.localFilesDao().jobs().isEmpty())
        LocalCopySettings.enabled(c,true);assertTrue(d.localFilesDao().jobs().isEmpty());LocalCopies.onClosed(d,"PAVEMENT",sample().id,100,"MANUAL_APP");assertTrue(d.localFilesDao().jobs().isEmpty())
        LocalCopies.onClosed(d,"PAVEMENT",sample().id,100,"AUTOMATIC");LocalCopies.onClosed(d,"PAVEMENT",sample().id,100,"AUTOMATIC");assertEquals(1,d.localFilesDao().jobs().size)
    }}
    @Test fun deviceOnlyClosureQueuesLocalAndNeverEnqueuesDrive()=runBlocking{db().use{d->
        d.syncContext=c;val s=sample();d.pavementDao().saveSample(s);DeliveryDestination.choose(d,"PAVEMENT",s.id,true,user)
        val sessions=AuthSessionManager().apply{login(UserCacheEntity(user.id,"test","FICTICIO","ENGINEER",true,"",0,0,0))};PavementRepository(d,sessions).closeSample(s.id)
        assertEquals("CLOSED",d.pavementDao().sample(s.id)!!.status);assertTrue(d.automaticSyncDao().all().isEmpty());assertEquals(1,d.localFilesDao().jobs().size)
        var requests=0;UnifiedSyncAdapter.enqueue(d,false){_,_,_->requests++};assertEquals(0,requests)
    }}
    @Test fun existingDriveQueueAndAckCannotBeChangedToDeviceOnly()=runBlocking{db().use{d->
        val s=sample();d.pavementDao().saveSample(s);val i=DriveSyncIntent("PAVEMENT/${s.id}","PAVEMENT",s.id,101,100,"UPDATE_PENDING",receiptJson="HISTORICAL_ACK")
        d.automaticSyncDao().put(i);assertTrue(runCatching{DeliveryDestination.choose(d,"PAVEMENT",s.id,true,user)}.isFailure);assertEquals(i,d.automaticSyncDao().get(i.key))
    }}
    @Test fun administratorCanAuthorizeLaterWithSameUuidAndRevision()=runBlocking{db().use{d->
        val s=sample();d.pavementDao().saveSample(s);DeliveryDestination.choose(d,"PAVEMENT",s.id,true,user);d.pavementDao().saveSample(s.copy(status="CLOSED"))
        assertTrue(runCatching{DeliveryDestination.authorizeDrive(d,"PAVEMENT",s.id,user)}.isFailure)
        DeliveryDestination.authorizeDrive(d,"PAVEMENT",s.id,user.copy(role=UserRole.ADMIN));val intent=d.automaticSyncDao().get("PAVEMENT/${s.id}")!!;assertEquals(s.id,intent.recordId);assertEquals(100L,intent.revision);assertFalse(DeliveryDestination.deviceOnly(d,intent.key))
    }}
    @Test fun closureTransactionRollbackAlsoRollsBackTheLocalRequest()=runBlocking{db().use{d->
        d.syncContext=c;LocalCopySettings.enabled(c,true);d.pavementDao().saveSample(sample())
        try{d.withTransaction{LocalCopies.onClosed(d,"PAVEMENT",sample().id,100,"AUTOMATIC");throw IllegalStateException("SIMULATED")}}catch(_:IllegalStateException){}
        assertTrue(d.localFilesDao().jobs().isEmpty());assertEquals("DRAFT",d.pavementDao().sample(sample().id)!!.status)
    }}
    @Test fun unsafeRouteNamesAreBoundedAndNeverTraverseDirectories(){
        val path=LocalExportNames.root("SIC","../RUTA\\A","TEST_148",123,"ALCANTARILLA");assertFalse(path.contains("../"));assertFalse(path.contains('\\'));assertTrue(path.startsWith("Download/GBO_Ruta_Clara/SIC/"));assertTrue(path.endsWith("R_123/"))
    }
    @Test fun publicOpenAndShareUseRealContentUrisAndReadGrants(){
        val uri=Uri.parse("content://media/external/downloads/123");val view=SharedFileActions.view(uri,"application/pdf");assertEquals(uri,view.data);assertTrue(view.flags and android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION!=0);assertEquals(uri,view.clipData!!.getItemAt(0).uri)
        val inner=SharedFileActions.share(uri,"application/pdf").getParcelableExtra<android.content.Intent>(android.content.Intent.EXTRA_INTENT)!!;assertEquals(uri,inner.getParcelableExtra<Uri>(android.content.Intent.EXTRA_STREAM));assertTrue(inner.flags and android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION!=0)
    }
}
