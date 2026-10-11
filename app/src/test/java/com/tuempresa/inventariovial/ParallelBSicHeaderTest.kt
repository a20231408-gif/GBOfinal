package com.tuempresa.inventariovial

import com.tuempresa.inventariovial.export.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

class ParallelBSicHeaderTest {
    private fun parts(bytes:ByteArray):Map<String,ByteArray> = linkedMapOf<String,ByteArray>().also {p->
        ZipInputStream(bytes.inputStream()).use {z->while(true) {val e=z.nextEntry ?: break;p[e.name]=z.readBytes()}}
    }
    @Test fun mappedFormatsShareCorporateHeaderAndPreserveTheirOwnColumnsAndTitle() {
        for(format in listOf("SIC-17A","SIC-17B","SIC-18A")) {
            val out=ByteArrayOutputStream()
            val labels=List(13) {SicColumn("${format} campo $it")}
            SicExcelWriter.writeMappedRow(out,format,"MO-104",labels,List(13) {"valor $it"})
            val p=parts(out.toByteArray());val sheet=p.getValue("xl/worksheets/sheet1.xml").toString(Charsets.UTF_8)
            assertTrue(sheet.contains(SicWorkbookHeaderWriter.CONTRACT_TITLE))
            assertTrue(sheet.contains("$format campo 12"))
            assertTrue(sheet.contains("r=\"A14\""));assertTrue(sheet.contains("valor 12"))
            assertTrue(sheet.contains("r=\"A11\""))
            assertEquals(59824,p.getValue("xl/media/sicHeader1.png").size)
            assertEquals(12602,p.getValue("xl/media/sicHeader2.jpeg").size)
            assertTrue(p.getValue("xl/drawings/sicHeader1.xml").toString(Charsets.UTF_8).contains("noChangeAspect=\"1\""))
        }
    }
}
