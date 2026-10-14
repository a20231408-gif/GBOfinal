package com.tuempresa.inventariovial

import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.export.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

class SicPlainObservations10KTest {
    private fun parts(bytes:ByteArray)=linkedMapOf<String,String>().also {map->ZipInputStream(bytes.inputStream()).use{z->while(true){val e=z.nextEntry?:break;map[e.name]=z.readBytes().toString(Charsets.UTF_8)}}}
    private fun col(n:Int):String=if(n<=26)('A'+n-1).toString() else col((n-1)/26)+('A'+(n-1)%26)
    @Test fun notesAreNormalTextAndOutsideContractualFilterMergesAndLogos() {
        val r=testRecord("test10k",sic="SIC-18").copy(observations=" =1+1 ")
        val item=InventoryRecordForExport(r,sic18=Sic18Entity(r.id,"06","1",1,"2",1.0,null,"1","1"))
        val bytes=ByteArrayOutputStream().also{SicExcelWriter.write(it,listOf(item),SicExportFormat.SIC18,ExportHeading("TEST","TEST","TEST"))}.toByteArray()
        val p=parts(bytes);val xml=p.getValue("xl/worksheets/sheet1.xml")
        val document=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml.byteInputStream())
        val last=col(SicExportFormat.SIC18.columns.size+1);val contract=col(SicExportFormat.SIC18.columns.size)
        val cells=document.getElementsByTagName("c")
        for(row in listOf(13,14)) {
            val cell=(0 until cells.length).map{cells.item(it) as org.w3c.dom.Element}.single{it.getAttribute("r")=="$last$row"}
            assertEquals("0",cell.getAttribute("s"));assertEquals("inlineStr",cell.getAttribute("t"))
            assertEquals(if(row==13)"OBSERVACIONES" else " =1+1 ",cell.textContent)
        }
        assertTrue(xml.contains("<autoFilter ref=\"A13:${contract}14\""))
        assertFalse(xml.contains("<mergeCell ref=\"A5:${last}5\""))
        assertFalse(xml.contains("min=\"${SicExportFormat.SIC18.columns.size+1}\""))
        val styles=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(p.getValue("xl/styles.xml").byteInputStream())
        val normal=(styles.getElementsByTagName("cellXfs").item(0) as org.w3c.dom.Element).getElementsByTagName("xf").item(0) as org.w3c.dom.Element
        for(attr in listOf("numFmtId","fontId","fillId","borderId"))assertEquals("0",normal.getAttribute(attr))
        assertEquals(0,document.getElementsByTagName("f").length)
        assertTrue(p.getValue("xl/workbook.xml").contains("\$${last}\$14"))
        assertTrue(xml.contains("<mergeCell ref=\"A1:E4\"/>"))
    }
}
