package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.localfiles.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class)
class StablePublicDocumentsTest {
    private val c get()=ApplicationProvider.getApplicationContext<Context>()
    private fun db()=Room.inMemoryDatabaseBuilder(c,InventoryDatabase::class.java).build()
    private val record=UUID.randomUUID().toString()
    private fun job(rev:Long)=LocalExportJob("$record-$rev","SIC/$record","SIC",record,rev,createdAt=rev)
    private fun file(rev:Long,text:String,name:String="SIC-18.xlsx"):LocalSharedFile {
        val j=job(rev);val f=File(c.filesDir,"test-stable/$record/$rev/$name").apply{parentFile!!.mkdirs();writeText(text)}
        val digest=fileDigest(f)
        return LocalSharedFile(j.id+name,j.id,j.recordKey,DriveMirrorPlan.ROOT+"PE-22A/ALCANTARILLAS/Alcantarilla $record - PR 10+120/",
            name,"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",digest.first,digest.second,f.path)
    }
    private suspend fun publish(d:InventoryDatabase,s:SicTestStorage,f:LocalSharedFile,rev:Long):LocalSharedFile {
        d.localFilesDao().saveJob(job(rev))
        return StableLocalPublication.publish(c,d,job(rev),f,s).also{d.localFilesDao().saveFile(it)}
    }
    @Test fun repeatedSavesKeepOneStableVisibleNameAndAllEarlierBytesOpenable()=runBlocking {db().use{d->
        val s=SicTestStorage();val before=d.automaticSyncDao().all()
        val originals=(1L..4L).map{rev->val f=file(rev,"FICTICIO $rev");publish(d,s,f,rev);f}
        val visible=s.entries.values.filterNot{it.name.startsWith('.')}
        assertEquals(1,visible.size);assertEquals("SIC-18.xlsx",visible.single().name)
        assertEquals(1,s.entries.values.map{it.parent}.distinct().size)
        assertFalse(visible.single().parent.contains("_PROVISIONALES"));assertFalse(visible.single().parent.contains("REVISIONES"))
        for(f in originals){val saved=d.localFilesDao().files(f.jobId).single();assertEquals(f.size to f.sha256,s.digest(Uri.parse(saved.uri)))
            assertEquals(f.size to f.sha256,fileDigest(File(saved.source)))
            assertEquals(android.content.Intent.ACTION_VIEW,SharedFileActions.view(Uri.parse(saved.uri),saved.mimeType).action)}
        assertEquals(3,d.localFilesDao().filesForRecord(job(1).recordKey).count{it.name.startsWith(".Historial - ")})
        val saved=d.localFilesDao().filesForRecord(job(1).recordKey);val jobs=d.localFilesDao().jobs()
        assertEquals(1,LocalCopyPresentation.currentFiles(jobs,saved).size)
        assertEquals(4,LocalCopyPresentation.currentFiles(jobs,saved,history=true).size)
        assertEquals(before,d.automaticSyncDao().all())
    }}
    @Test fun sameBytesReuseUriAndDoNotCreateAnotherHistoryEntry()=runBlocking {db().use{d->
        val s=SicTestStorage();val a=publish(d,s,file(1,"FICTICIO"),1);val writes=s.writes
        val b=publish(d,s,file(2,"FICTICIO"),2)
        assertEquals(a.uri,b.uri);assertEquals(writes,s.writes);assertEquals(1,s.entries.size)
    }}
    @Test fun completingAProvisionalDocumentRetainsOnlyTheFinalNameAsCurrent()=runBlocking {db().use{d->
        val s=SicTestStorage();val a=file(1,"BORRADOR","SIC-18_PROVISIONAL.xlsx");publish(d,s,a,1)
        val b=file(2,"COMPLETO","SIC-18.xlsx");publish(d,s,b,2)
        assertEquals(listOf("SIC-18.xlsx"),s.entries.values.filterNot{it.name.startsWith('.')}.map{it.name})
        val old=d.localFilesDao().files(a.jobId).single()
        assertTrue(old.name.startsWith(".Historial - "));assertEquals(a.size to a.sha256,s.digest(Uri.parse(old.uri)))
    }}
    @Test fun olderRetryCannotReplaceNewerVisibleVersion()=runBlocking {db().use{d->
        val s=SicTestStorage();val a=file(1,"ANTERIOR");publish(d,s,a,1)
        val b=file(2,"ACTUAL");val current=publish(d,s,b,2)
        val retried=publish(d,s,a,1)
        assertTrue(retried.name.startsWith(".Historial - "))
        assertEquals(b.size to b.sha256,s.digest(Uri.parse(current.uri)))
        assertEquals("SIC-18.xlsx",s.find(Uri.parse(current.uri)).name)
    }}
    @Test fun missingCurrentCopyCanBeRecoveredFromPrivateBytes()=runBlocking {db().use{d->
        val s=SicTestStorage();val f=file(1,"RECUPERABLE");val old=publish(d,s,f,1)
        s.entries.remove(Uri.parse(old.uri))
        val restored=publish(d,s,f,1)
        assertEquals(f.size to f.sha256,s.digest(Uri.parse(restored.uri)))
        assertEquals("SIC-18.xlsx",restored.name)
    }}
    @Test fun missingPriorCopyDoesNotPreventSafeNewRevisionAndHistory()=runBlocking {db().use{d->
        val s=SicTestStorage();val f=file(1,"ANTERIOR");val old=publish(d,s,f,1)
        s.entries.remove(Uri.parse(old.uri));val next=file(2,"ACTUAL");val result=publish(d,s,next,2)
        assertEquals(next.size to next.sha256,s.digest(Uri.parse(result.uri)))
        val history=d.localFilesDao().files(f.jobId).single()
        assertTrue(history.name.startsWith(".Historial - "));assertEquals(f.size to f.sha256,s.digest(Uri.parse(history.uri)))
    }}
    @Test fun interruptedPromotionReconcilesWithoutDuplicateAndRetainsHistory()=runBlocking {db().use{d->
        val s=SicTestStorage();val a=file(1,"ANTERIOR");publish(d,s,a,1)
        val b=file(2,"ACTUAL");s.stopAfterPromote=true
        assertTrue(runCatching{publish(d,s,b,2)}.isFailure)
        val resumed=publish(d,s,b,2)
        assertEquals(b.size to b.sha256,s.digest(Uri.parse(resumed.uri)));assertEquals(2,s.entries.size)
        val old=d.localFilesDao().files(a.jobId).single()
        assertTrue(old.name.startsWith(".Historial - "));assertEquals(a.size to a.sha256,s.digest(Uri.parse(old.uri)))
    }}
    @Test fun interruptedArchiveReconcilesLostRenameResult()=runBlocking {db().use{d->
        val s=SicTestStorage();val a=file(1,"ANTERIOR");publish(d,s,a,1)
        val b=file(2,"ACTUAL");s.stopAfterBackup=true
        assertTrue(runCatching{publish(d,s,b,2)}.isFailure)
        val result=publish(d,s,b,2)
        assertEquals(b.size to b.sha256,s.digest(Uri.parse(result.uri)))
        val old=d.localFilesDao().files(a.jobId).single()
        assertEquals(a.size to a.sha256,s.digest(Uri.parse(old.uri)));assertEquals(2,s.entries.size)
    }}
    @Test fun incompleteWritePreservesVisibleOldVersionAndRetryCompletes()=runBlocking {db().use{d->
        val s=SicTestStorage();val a=file(1,"ANTERIOR");val old=publish(d,s,a,1)
        val b=file(2,"ACTUAL NUEVO");s.shortWrite=true
        assertTrue(runCatching{publish(d,s,b,2)}.isFailure)
        assertEquals(a.size to a.sha256,s.digest(Uri.parse(old.uri)));assertEquals(old.name,s.find(Uri.parse(old.uri)).name)
        s.shortWrite=false;val next=publish(d,s,b,2)
        assertEquals(b.size to b.sha256,s.digest(Uri.parse(next.uri)))
    }}
    @Test fun externalEditIsNeverOverwritten()=runBlocking {db().use{d->
        val s=SicTestStorage();val a=publish(d,s,file(1,"ANTERIOR"),1)
        s.find(Uri.parse(a.uri)).bytes="EDICION EXTERNA".toByteArray()
        assertTrue(runCatching{publish(d,s,file(2,"ACTUAL"),2)}.isFailure)
        assertEquals("EDICION EXTERNA",s.find(Uri.parse(a.uri)).bytes.toString(Charsets.UTF_8))
        assertEquals(a.name,s.find(Uri.parse(a.uri)).name)
    }}
    @Test fun unknownExistingPublicFileIsNotMovedOrOverwritten()=runBlocking {db().use{d->
        val s=SicTestStorage();val f=file(1,"ANTERIOR HISTORICO")
        val slot=s.reserve(f);s.write(slot.uri,File(f.source));s.publish(slot,f)
        assertTrue(runCatching{publish(d,s,file(2,"ACTUAL"),2)}.isFailure)
        assertEquals(f.name,s.find(slot.uri).name);assertEquals(f.size to f.sha256,s.digest(slot.uri))
    }}
    @Test fun sharedPublicReferenceBlocksReplacementBeforeArchive()=runBlocking {db().use{d->
        val s=SicTestStorage();val a=publish(d,s,file(1,"COMPARTIDO"),1)
        d.localFilesDao().saveFile(a.copy(id="OTHER",recordKey="SIC/OTHER",jobId="OTHER"))
        assertTrue(runCatching{publish(d,s,file(2,"NUEVO"),2)}.isFailure)
        assertEquals(a.name,s.find(Uri.parse(a.uri)).name);assertEquals(a.size to a.sha256,s.digest(Uri.parse(a.uri)))
    }}
    @Test fun returningToEarlierBytesCanBeArchivedAgainWithoutNameCollision()=runBlocking {db().use{d->
        val s=SicTestStorage()
        for((index,text)in listOf("A","B","A","C").withIndex())publish(d,s,file(index+1L,text),index+1L)
        assertEquals(4,s.entries.size);assertEquals(1,s.entries.values.count{!it.name.startsWith('.')})
        for(f in d.localFilesDao().filesForRecord(job(1).recordKey))assertEquals(f.size to f.sha256,s.digest(Uri.parse(f.uri)))
    }}
    @Test fun severalTemplatesAndPhotographsHaveIndependentStableSlots()=runBlocking {db().use{d->
        val s=SicTestStorage()
        for(name in listOf("SIC-17.xlsx","SIC-17A_PROVISIONAL.xlsx","SIC-18A.xlsx","FOTOGRAFIA.jpg")){
            val first=file(1,"FICTICIO $name",name);val old=publish(d,s,first,1)
            val next=publish(d,s,file(2,"NUEVO $name",name),2)
            assertEquals(name,next.name);assertEquals(first.size to first.sha256,s.digest(Uri.parse(d.localFilesDao().files(first.jobId).single{it.id==old.id}.uri)))
        }
        assertEquals(4,s.entries.values.count{!it.name.startsWith('.')});assertTrue(d.automaticSyncDao().all().isEmpty())
    }}
}
