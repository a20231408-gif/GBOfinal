package com.tuempresa.inventariovial

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

internal fun listEntry(id:String,title:String="Puente - Correviento",state:String="QUEUED",running:Boolean=false):DriveDeliveryEntry {
    val original=deliveryFixture();val key="SIC/FICTICIO_$id"
    val p=JSONObject(original.preparedJson!!).put("key",key)
    p.put("contentHash",AutomaticDelivery.contentHash(p))
    val ack=JSONObject(original.receiptJson!!).put("key",key).put("contentHash",p.getString("contentHash"))
    val i=original.copy(key=key,recordId="FICTICIO_$id",state=state,syncedRevision=if(state=="SYNCED")2 else 0,
        receiptJson=ack.toString(),preparedJson=p.toString(),lastSyncedAt=if(state=="SYNCED")4 else null)
    val identity=DriveDeliveryIdentity(title,"PE-28H · PR 10+060",2,true,"SIC-17")
    return DriveDeliveryEntry(i,identity,DriveDeliveryPresentation.view(i,identity,running,true,true))
}

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
class DriveDeliveryListsTest {
    private val queue=DriveDeliverySection.QUEUE;private val history=DriveDeliverySection.HISTORY
    private fun selected(rows:List<DriveDeliveryEntry>,section:DriveDeliverySection)=DriveDeliveryListPresentation.select(rows,section)
    @Test fun pendingAndActiveAppearInBothSectionsWithTheSameIdentityAndView() {
        val pending=listEntry("PENDING");val active=listEntry("ACTIVE",state="UPLOADING",running=true)
        val rows=listOf(pending,active);val q=selected(rows,queue);val h=selected(rows,history)
        assertEquals(2,q.size);assertEquals(2,h.size)
        for(row in q)assertSame(row,h.single{it.intent.key==row.intent.key})
        assertEquals(DriveDeliveryStatus.UPLOADING,active.view.status)
    }
    @Test fun aValidAckHidesOnlyTheQueueRowAndANewRevisionReturnsIt() {
        val confirmed=listEntry("EDITED",state="SYNCED")
        assertTrue(confirmed.view.confirmed);assertTrue(selected(listOf(confirmed),queue).isEmpty())
        assertSame(confirmed,selected(listOf(confirmed),history).single())
        val i=confirmed.intent.copy(state="UPDATE_PENDING",revision=3)
        val id=confirmed.identity.copy(currentRevision=3)
        val updated=DriveDeliveryEntry(i,id,DriveDeliveryPresentation.view(i,id,false,true,true))
        assertFalse(updated.view.confirmed);assertEquals(2L,updated.intent.syncedRevision)
        assertEquals(listOf(updated),selected(listOf(updated),queue));assertEquals(listOf(updated),selected(listOf(updated),history))
        assertEquals(confirmed.intent.receiptJson,updated.intent.receiptJson)
    }
    @Test fun lostOrIncompleteAckErrorsAndBusyStayVisibleWithoutLosingEvidence() {
        val base=listEntry("LOST",state="UPLOADING")
        val lost=base.intent.copy(receiptJson=null)
        val blocked=base.intent.copy(key="SIC/BLOCKED",state="BLOCKED",error="Fotografía rechazada (NOT_JPEG)")
        val busy=base.intent.copy(key="SIC/BUSY",state="ERROR",error="Servidor ocupado (BUSY)")
        val invalid=listEntry("INCOMPLETE",state="SYNCED").let{r->
            val a=JSONObject(r.intent.receiptJson!!).put("files",JSONArray());r.intent.copy(receiptJson=a.toString())}
        val rows=listOf(lost,blocked,busy,invalid).map{i->DriveDeliveryEntry(i,base.identity,DriveDeliveryPresentation.view(i,base.identity,false,true,true))}
        assertEquals(4,selected(rows,queue).size);assertEquals(4,selected(rows,history).size)
        assertEquals(DriveDeliveryStatus.VERIFYING,rows[0].view.status)
        assertEquals(DriveDeliveryStatus.ATTENTION,rows[1].view.status)
        assertEquals(DriveDeliveryStatus.PENDING,rows[2].view.status)
        assertEquals(DriveDeliveryStatus.VERIFYING,rows[3].view.status)
        assertEquals("Fotografía rechazada (NOT_JPEG)",rows[1].intent.error)
        assertTrue(rows.all{it.intent.preparedJson==base.intent.preparedJson||it.intent.key.contains("INCOMPLETE")})
    }
    @Test fun historySearchFiltersAndRecentOrderingNeverDependOnVisibleNameAsIdentity() {
        val a=listEntry("A",state="SYNCED");val b=listEntry("B");val c=listEntry("C","Badén - PE-28C - PR 12+150",state="BLOCKED").let{it.copy(identity=it.identity.copy(location="PE-28C · PR 12+150"))}
        val rows=listOf(b,a,c)
        val all=selected(rows,history);assertEquals(a.intent.key,all.first().intent.key)
        assertEquals(3,all.map{it.intent.key}.distinct().size)
        assertEquals(2,DriveDeliveryListPresentation.select(rows,history,"correviento").size)
        assertEquals(listOf(c),DriveDeliveryListPresentation.select(rows,history,"baden 12+150"))
        assertEquals(2,DriveDeliveryListPresentation.select(rows,history,"PE-28H").size)
        assertEquals(listOf(a),DriveDeliveryListPresentation.select(rows,history,filter=DriveDeliveryFilter.COMPLETE))
        assertEquals(2,DriveDeliveryListPresentation.select(rows,history,filter=DriveDeliveryFilter.PENDING).size)
        assertEquals(listOf(c),DriveDeliveryListPresentation.select(rows,history,filter=DriveDeliveryFilter.ATTENTION))
        assertEquals(setOf(b.intent.key,c.intent.key),DriveDeliveryListPresentation.select(rows,queue,"no match",DriveDeliveryFilter.COMPLETE).map{it.intent.key}.toSet())
        assertEquals(all.map{it.intent.key},selected(rows.reversed(),history).map{it.intent.key})
    }
    @Test fun manyFilesRemainOneDeliveryAndOnlyFinalReceiptsCount() {
        val row=listEntry("FILES",state="SYNCED");val p=JSONObject(row.intent.preparedJson!!);val ack=JSONObject(row.intent.receiptJson!!)
        for(n in 2..3){val path="GENERAL/F0$n.jpg"
            p.getJSONArray("files").put(JSONObject().put("key","photo/ficticia$n").put("path",path).put("mimeType","image/jpeg").put("sha256","a".repeat(64)))
            ack.getJSONArray("files").put(JSONObject().put("key","photo/ficticia$n").put("requestedPath",path).put("logicalPath",ack.getString("logicalPath")+"/"+path)
                .put("fileId","FICTICIO_FILE_$n").put("folderId","FICTICIO_FOLDER").put("sha256","a".repeat(64)))
        }
        p.put("contentHash",AutomaticDelivery.contentHash(p));ack.put("contentHash",p.getString("contentHash"))
        val i=row.intent.copy(preparedJson=p.toString(),receiptJson=ack.toString())
        val complete=row.copy(intent=i,view=DriveDeliveryPresentation.view(i,row.identity,false,true,true))
        assertEquals("Archivos confirmados: 3 de 3",complete.view.files)
        assertEquals(1,selected(listOf(complete),history).size);assertTrue(selected(listOf(complete),queue).isEmpty())
        val partial=i.copy(state="UPLOADING",syncedRevision=0)
        val pending=row.copy(intent=partial,view=DriveDeliveryPresentation.view(partial,row.identity,true,true,true,progress=SyncProgress(2,"PHOTOS",2,3,1)))
        assertEquals(1,selected(listOf(pending),queue).size);assertFalse(pending.view.files.contains("2 de 3"))
    }
    @Test fun diskReopenKeepsAllDeliveriesReceiptsFilesAndBothViewsWithoutWrites()=runBlocking {
        val c=ApplicationProvider.getApplicationContext<Application>();val name="drive-history-${UUID.randomUUID()}.db"
        fun open()=Room.databaseBuilder(c,InventoryDatabase::class.java,name).build()
        val source=listOf(listEntry("HISTORY",state="SYNCED"),listEntry("PENDING"),listEntry("UNCERTAIN",state="UPLOADING"))
        open().use{db->for(row in source){
            db.inventoryDao().insertRecord(testRecord(row.intent.recordId,created=2,sic="SIC-17",route="PE-28H").copy(assetType="PUENTE"))
            db.automaticSyncDao().put(row.intent)
        }}
        open().use{db->
            val before=db.automaticSyncDao().all().sortedBy{it.key}
            val rows=db.automaticSyncDao().observe().first().map{i->val id=DriveDeliveryPresentation.identity(db,i);DriveDeliveryEntry(i,id,DriveDeliveryPresentation.view(i,id,false,true,true))}
            assertEquals(2,selected(rows,queue).size);assertEquals(3,selected(rows,history).size)
            assertEquals(1,rows.count{it.view.confirmed})
            repeat(3){DriveDeliveryListPresentation.select(rows,history,"PE-28H");selected(rows,queue)}
            assertEquals(before,db.automaticSyncDao().all().sortedBy{it.key})
            for(row in rows)assertEquals(source.single{it.intent.key==row.intent.key}.intent.preparedJson,row.intent.preparedJson)
            db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM device_delivery_choices").use{it.moveToFirst();assertEquals(0,it.getInt(0))}
            db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM local_export_jobs").use{it.moveToFirst();assertEquals(0,it.getInt(0))}
        }
    }
}
