package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.export.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
class SicObservations10ITest {
    private fun record(id:String,notes:String?)=InventoryRecordEntity(id,"SIC-18","ALCANTARILLA","TEST10I","UC","0001",12.0,null,null,null,null,null,null,null,"13/10/2026",notes,"ACTIVE","PENDING","PENDING",1,1)
    private fun item(id:String,notes:String?)=InventoryRecordForExport(record(id,notes),sic18=Sic18Entity(id,"06","3",1,"2",1.0,null,"1","1"))
    private fun workbook(items:List<InventoryRecordForExport>)=ByteArrayOutputStream().also {SicExcelWriter.write(it,items,SicExportFormat.SIC18,ExportHeading("ENSAYO FICTICIO","TEST10I","PRUEBA"))}.toByteArray()
    private fun sheet(bytes:ByteArray):org.w3c.dom.Document {
        var xml=byteArrayOf();ZipInputStream(bytes.inputStream()).use {z->while(true){val e=z.nextEntry?:break;if(e.name=="xl/worksheets/sheet1.xml")xml=z.readBytes()}}
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml.inputStream())
    }
    @Test fun literalNotesAreLastColumnAndFormulaLikeTextNeverExecutes() {
        val values=listOf("PINTADO OK 57 E2","=1+1",null,"  Texto & <sin prefijo>  ")
        val bytes=workbook(values.mapIndexed{i,n->item("uuid$i",n).let {it.copy(record=it.record.copy(startDistanceM=i.toDouble()))}})
        val xml=sheet(bytes);val rows=xml.getElementsByTagName("row")
        fun cells(n:Int)=((0 until rows.length).map {rows.item(it) as org.w3c.dom.Element}.single {it.getAttribute("r")==n.toString()}).getElementsByTagName("c")
        val head=cells(13);assertEquals(SicExportFormat.SIC18.columns.size+1,head.length)
        assertEquals("OBSERVACIONES",head.item(head.length-1).textContent)
        for(i in values.indices){val row=cells(14+i);val last=row.item(row.length-1) as org.w3c.dom.Element
            assertEquals(values[i].orEmpty(),last.textContent);if(values[i]!=null)assertEquals("inlineStr",last.getAttribute("t"))}
        assertEquals(0,xml.getElementsByTagName("f").length)
        val dir=File("build/campo-10i").apply {mkdirs()};File(dir,"SIC-18-observaciones.xlsx").writeBytes(bytes)
    }
    @Test fun applicableFormatsOnlyAppendAndNeverShiftTheirPreviousColumns() {
        for(format in SicExportFormat.entries) {
            val old=format.columns;val now=SicExcelWriter.observationColumns(format)
            assertEquals(old,now.take(old.size))
            assertEquals(old.size+if(format==SicExportFormat.SIC18A)0 else 1,now.size)
        }
    }
    @Test fun snapshotAfterOfflineEditUsesSameUuidAndLatestLiteralNotes()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>();val name="sic10i-${UUID.randomUUID()}"
        fun open()=Room.databaseBuilder(context,InventoryDatabase::class.java,name).build()
        var db=open()
        try {
            db.inventoryDao().insertRecord(record("same-uuid","Inicial"))
            db.inventoryDao().updateRecord(record("same-uuid","PINTADO OK 57 E2").copy(updatedAt=2))
            db.close();db=open();val snap=db.inventoryDao().snapshot("same-uuid")!!
            assertEquals("same-uuid",snap.record.id);assertEquals("PINTADO OK 57 E2",snap.record.observations)
            val xml=sheet(workbook(listOf(item(snap.record.id,snap.record.observations))))
            assertTrue(xml.documentElement.textContent.contains("PINTADO OK 57 E2"));assertEquals(2,snap.record.updatedAt)
        } finally {db.close();context.deleteDatabase(name)}
    }
}
