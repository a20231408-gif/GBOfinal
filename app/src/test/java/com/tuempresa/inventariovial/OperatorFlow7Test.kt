package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.export.*
import com.tuempresa.inventariovial.repository.InventoryRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=Application::class)
class OperatorFlow7Test {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun db()=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
    private suspend fun save(d:InventoryDatabase,id:String,pr:String,route:String="PE-22A",status:String="ACTIVE") {
        val r=testRecord(id,pr=pr,route=route,sic="SIC-18",status=status).copy(latitude=null,longitude=null,updatedAt=1000)
        InventoryRepository(d).saveSic18(r,Sic18Entity(id,"06","1",1,"1",1.0,1.0,"1","1"),emptyList())
    }
    private fun cells(file:File):Map<String,String> = ZipFile(file).use {zip ->
        assertNotNull(zip.getEntry("[Content_Types].xml"))
        val doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(zip.getInputStream(zip.getEntry("xl/worksheets/sheet1.xml")))
        val list=doc.getElementsByTagName("c")
        (0 until list.length).associate {val e=list.item(it) as org.w3c.dom.Element;e.getAttribute("r") to e.textContent}
    }
    @Test fun savedUuidOpensSameOfflineSnapshotWithObservations()=runBlocking {db().use {d->
        save(d,"saved","10")
        val r=d.inventoryDao().recordById("saved")!!
        d.inventoryDao().updateRecord(r.copy(observations="Primera línea\nSegunda línea"))
        val s=d.inventoryDao().observeSnapshot("saved").first()!!
        assertEquals("saved",s.record.id);assertEquals("Primera línea\nSegunda línea",s.record.observations)
        assertEquals(13, SicExportFormat.SIC18.values(RecordDetailPresentation.exportRow(s)).size)
        assertEquals("QUEUED",d.automaticSyncDao().get("SIC/saved")!!.state)
    }}
    @Test fun routeWorkbookHasTwoRowsAndEditKeepsOtherRowAndColumns()=runBlocking {db().use {d->
        save(d,"a","10");save(d,"b","11");save(d,"other","12","PE-5S");save(d,"draft","13",status="DRAFT")
        val exporter=LocalSicExport(context,d,this)
        val first=exporter.prepareRoute(SicExportFormat.SIC18,"PE-22A")
        var values=cells(File(first.localPath))
        assertEquals("0010",values["C14"]);assertEquals("0011",values["C15"]);assertNull(values["C16"])
        val a=d.inventoryDao().recordById("a")!!
        d.inventoryDao().updateRecord(a.copy(startDistanceM=325.0,updatedAt=2000))
        val second=exporter.prepareRoute(SicExportFormat.SIC18,"PE-22A")
        assertEquals(first.localPath,second.localPath)
        values=cells(File(second.localPath));assertEquals("325.0",values["D14"]);assertEquals("0011",values["C15"])
        assertEquals(14,values.keys.count {it.matches(Regex("[A-Z]+14"))})
        assertEquals("OBSERVACIONES",values["N13"])
        assertEquals(setOf("a","b"),d.inventoryDao().recordsForExport("SIC-18","PE-22A").map{it.record.id}.toSet())
        assertEquals(0L,d.automaticSyncDao().get("SIC/a")!!.syncedRevision)
        assertNotNull(LocalSicExport(context,d,this).cachedRoute(SicExportFormat.SIC18,"PE-22A"))
    }}
    @Test fun realXlsxOpenAndShareUseGrantedContentUri()=runBlocking {db().use {d->
        save(d,"a","10");val f=LocalSicExport(context,d,this).prepareRoute(SicExportFormat.SIC18,"PE-22A")
        val open=GeneratedFileIndex.open(context,f)
        assertEquals(Intent.ACTION_VIEW,open.action);assertEquals("content",open.data!!.scheme)
        assertEquals(SicExcelWriter.XLSX_MIME,open.type)
        assertTrue(open.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION!=0)
        assertEquals(Intent.ACTION_CHOOSER,GeneratedFileIndex.share(context,f).action)
    }}
    @Test fun syncRequiresAckAndOlderRemoteReceiptRemainsExplicit() {
        val receipt="""{"ok":true,"key":"SIC/a","revision":1,"sicFiles":[{"format":"SIC-18","fileId":"real_file"}]}"""
        val i=DriveSyncIntent("SIC/a","SIC","a",1,1,"SYNCED",lastSyncedAt=123,receiptJson=receipt)
        assertEquals("Sincronizado",RecordDetailPresentation.status(i,true))
        assertEquals("Pendiente de sincronización",RecordDetailPresentation.status(null,true))
        assertNotEquals("Sincronizado",RecordDetailPresentation.status(i.copy(receiptJson=null),true))
        assertEquals("Actualización pendiente",RecordDetailPresentation.status(i.copy(revision=2,state="UPDATE_PENDING"),true))
        assertEquals("real_file",RecordDetailPresentation.remoteFile(i.copy(revision=2),"SIC-18"))
        assertNull(RecordDetailPresentation.remoteFile(i,"SIC-19"))
        assertNull(RecordDetailPresentation.remoteFile(i.copy(syncedRevision=2),"SIC-18"))
        assertEquals("Pendiente de confirmación",RecordDetailPresentation.status(i.copy(state="UPLOADING"),true))
        assertEquals("Error",RecordDetailPresentation.status(i.copy(state="ERROR"),true))
        assertTrue(RecordDetailPresentation.status(i.copy(state="CONFLICT"),true).startsWith("Conflicto"))
    }
    @Test fun photoGroupingKeepsGeneralOrderAndInspectionIsolation() {
        fun photo(id:String,index:Int,category:String?,record:String="a",inspection:String?=null)=
            PhotoEntity(id,record,index,"$id.jpg",false,null,null,null,"PENDING",1,photoCategory=category,scapInspectionId=inspection)
        val all=listOf(photo("second",2,"INICIO"),photo("legacy",3,null),photo("first",1,"INICIO"),
            photo("foreign",4,"INICIO","b"),photo("scap-one",5,"UPSTREAM",inspection="i1"),photo("scap-two",6,"UPSTREAM",inspection="i2"))
        val groups=RecordDetailPresentation.photos(all,"a")
        assertEquals(listOf("first","second"),groups.getValue("INICIO").map {it.id})
        assertEquals(listOf("legacy"),groups.getValue("GENERAL").map {it.id})
        assertEquals(listOf("scap-one"),RecordDetailPresentation.photos(all,"a","i1").values.flatten().map {it.id})
    }
}
