package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.auth.*
import com.tuempresa.inventariovial.core.InventoryDraftStore
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.field.draftDestination
import com.tuempresa.inventariovial.model.RoadAssetType
import com.tuempresa.inventariovial.model.form.*
import com.tuempresa.inventariovial.repository.InventoryRepository
import com.tuempresa.inventariovial.export.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.*
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class)
class Sic20Recovery20261019Test {
    private val c get()=ApplicationProvider.getApplicationContext<Context>()
    private fun login(){AuthGraph.get(c).sessions.login(UserCacheEntity("TEST_SIC20","test","FICTICIO","OPERATOR",true,"",1,1,1))}
    private fun record(id:String,asset:String)=InventoryRecordEntity(id,"SIC-20",asset,"TEST","CD","1",10.0,"1",20.0,"D",null,null,null,null,"19/10/2026","OBS FICTICIA","DRAFT","PENDING","PENDING",1,1)
    private fun request(id:String,asset:String,code:String,path:String)=InventorySaveRequest("SIC-20",asset,"TEST","CD","1","10","1","20","D",Double.NaN,Double.NaN,null,"19/10/2026","OBS FICTICIA",path,
        SicFormDetail.Sic20(Sic20FormState(classCode=code,typeCode="2",dimension1M="2",dimension2M=if(code=="12")"3" else "",wallLengthMeters=if(code=="14")"10"else "")),recordId=id)

    @Test fun wallAndFordDraftsCloseReopenEditFinalizeAndExportWithoutChangingUuidOrPhoto()=runBlocking {
        login();val name="TEST_SIC20_${UUID.randomUUID()}.db"
        fun open()=Room.databaseBuilder(c,InventoryDatabase::class.java,name).build()
        val photos=mutableMapOf<String,File>()
        val requests=listOf("WALL" to "14","FORD" to "12").map{(asset,code)->
            val id="TEST_SIC20_${UUID.randomUUID()}";val f=File(c.filesDir,"$id.jpg").apply{writeBytes(byteArrayOf(1,2,3))};photos[id]=f
            request(id,asset,code,f.path)
        }
        open().use{db->for(r in requests)assertEquals(r.recordId,InventoryDraftStore.save(c,db,r))}
        open().use{db->for(r in requests) {
            val id=r.recordId!!;val row=db.inventoryDao().recordById(id)!!;val raw=InventoryDraftStore.read(c,id)!!
            val state=InventoryDraftStore.restore(raw,Sic20FormState())
            assertEquals((r.detail as SicFormDetail.Sic20).state,state)
            assertEquals(if(state.classCode=="14")RoadAssetType.WALL else RoadAssetType.FORD,draftDestination(row,null,state.classCode).asset)
            assertEquals(photos[id]!!.path,raw.getJSONArray("photoPaths").getString(0))
            InventoryDraftStore.save(c,db,r.copy(detail=SicFormDetail.Sic20(state.copy(dimension1M="2.5"))))
            val active=row.copy(assetType=Sic20Identity.contractualAsset(state.classCode),status="ACTIVE",observations="OBS EDITADA",updatedAt=3)
            val photo=PhotoEntity("PHOTO_$id",id,1,photos[id]!!.path,true,null,null,null,"PENDING",1)
            InventoryRepository(db).saveSic20(active,Sic20Entity(id,state.classCode,"2",2.5,if(state.classCode=="12")3.0 else null,"1","1",if(state.classCode=="14")10.0 else null),listOf(photo))
            val snapshot=db.inventoryDao().snapshot(id)!!
            assertEquals(id,snapshot.record.id);assertEquals(state.classCode,snapshot.sic20!!.classCode)
            assertEquals(photo,snapshot.photos.single());assertEquals("OBS EDITADA",snapshot.record.observations)
            val exported=db.inventoryDao().recordsForExport("SIC-20",null).single{it.record.id==id}
            val values=SicExportFormat.SIC20.values(exported);assertTrue(values.any{it?.toString()==state.classCode})
            val bytes=ByteArrayOutputStream().also{SicExcelWriter.write(it,listOf(exported),SicExportFormat.SIC20,ExportHeading("TEST","TEST","TEST"))}.toByteArray()
            assertTrue(bytes.size>1000);assertArrayEquals(byteArrayOf(1,2,3),photos[id]!!.readBytes())
        }}
    }
    @Test fun aliasesAreEquivalentButDifferentSubtypesAndOtherSicRemainBlocked() {
        for((old,new,code)in listOf(Triple("WALL","MURO","14"),Triple("FORD","BADEN","12"),Triple("TUNNEL","TUNEL","13"))) {
            assertTrue(sameInventoryIdentity(record("TEST",old),"SIC-20",new))
            Sic20Identity.requireDetail(new,code)
            assertTrue(runCatching{Sic20Identity.requireDetail(new,if(code=="14")"12" else "14")}.isFailure)
        }
        assertFalse(sameInventoryIdentity(record("TEST","WALL"),"SIC-20","BADEN"))
        assertFalse(sameInventoryIdentity(record("TEST","WALL"),"SIC-22","MURO"))
        assertFalse(sameInventoryIdentity(record("TEST","WALL").copy(sicCode="SIC-22"),"SIC-22","MURO"))
    }
    @Test fun conflictBetweenRoomAndRawClassBlocksBeforeWritingAndKeepsAllEvidence()=runBlocking {
        login();Room.inMemoryDatabaseBuilder(c,InventoryDatabase::class.java).build().use{db->
            val id="TEST_CONFLICT_${UUID.randomUUID()}";val file=File(c.filesDir,"$id.jpg").apply{writeBytes(byteArrayOf(4,5,6))}
            val r=request(id,"WALL","14",file.path);InventoryDraftStore.save(c,db,r)
            val before=InventoryDraftStore.read(c,id)!!.toString();val old=db.inventoryDao().recordById(id)!!
            db.inventoryDao().updateRecord(old.copy(assetType="FORD")) // Fictitious reproduction of inconsistent historical identity.
            assertTrue(runCatching{draftDestination(old.copy(assetType="FORD"),null,"14")}.isFailure)
            assertTrue(runCatching{InventoryDraftStore.save(c,db,r.copy(assetType="BADEN",detail=SicFormDetail.Sic20(Sic20FormState(classCode="12"))))}.isFailure)
            assertEquals(before,InventoryDraftStore.read(c,id)!!.toString());assertEquals("FORD",db.inventoryDao().recordById(id)!!.assetType)
            assertTrue(db.inventoryDao().snapshot(id)!!.sic20==null);assertArrayEquals(byteArrayOf(4,5,6),file.readBytes())
        }
    }
    @Test fun snapshotWithWrongPersistedDetailIsRejectedWithoutConvertingIt()=runBlocking {
        Room.inMemoryDatabaseBuilder(c,InventoryDatabase::class.java).build().use{db->
            val row=record("TEST_INCONSISTENT","FORD");db.inventoryDao().insertRecord(row)
            db.inventoryDao().insertSic20(Sic20Entity(row.id,"14","2",2.0,null,"1","1"))
            assertTrue(runCatching{requireSicSnapshot(db.inventoryDao().snapshot(row.id)!!)}.isFailure)
            assertTrue(runCatching{InventoryRepository(db).saveSic20(row.copy(status="ACTIVE",assetType="MURO"),Sic20Entity(row.id,"14","2",2.0,null,"1","1"),emptyList())}.isFailure)
            assertEquals(row,db.inventoryDao().recordById(row.id))
        }
    }
    @Test fun realViewModelFinalizationUsesNormalValidationWarningsAndPersistedPhotoForBothSubtypes() {
        initializeIdleUiWorkers();login()
        val vm=com.tuempresa.inventariovial.viewmodel.InventoryViewModel(c.applicationContext as Application)
        val db=InventoryDatabase.getInstance(c);db.syncContext=null
        val store=androidx.lifecycle.ViewModelStore().apply{put("sic20",vm)}
        fun await(test:()->Boolean) {
            val deadline=System.nanoTime()+30_000_000_000L
            while(!test() && System.nanoTime()<deadline){org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();Thread.sleep(10)}
            assertTrue("ViewModel operation completed",test())
        }
        try {for((asset,code)in listOf("WALL" to "14","FORD" to "12")) {
            val id="TEST_VM_${UUID.randomUUID()}";val photo=File(c.filesDir,"$id.jpg")
            val image=Bitmap.createBitmap(32,32,Bitmap.Config.ARGB_8888)
            photo.outputStream().use{image.compress(Bitmap.CompressFormat.JPEG,95,it)};image.recycle()
            val original=request(id,asset,code,photo.path).copy(segment="Tramo 1",direction="INCREASING")
            runBlocking{InventoryDraftStore.save(c,db,original)}
            var done=false;var error:String?=null
            vm.saveRecord(original.copy(assetType=Sic20Identity.contractualAsset(code)),{done=true},{error=it;done=true})
            await{done || vm.pendingSave.value!=null}
            if(!done){vm.confirmSaveWarnings();await{done}}
            assertNull(error)
            val saved=runBlocking{db.inventoryDao().snapshot(id)!!}
            assertEquals("ACTIVE",saved.record.status);assertEquals(code,saved.sic20!!.classCode)
            assertEquals(id,saved.record.id);assertEquals(photo.path,saved.photos.single().localPath)
        }} finally {store.clear();AuthGraph.get(c).sessions.logout()}
    }
}
