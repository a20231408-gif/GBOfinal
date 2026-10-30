package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.export.*
import com.tuempresa.inventariovial.scap.export.*
import com.tuempresa.inventariovial.scap.domain.BridgeSicContract
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class)
class MixedSic17HotfixTest {
    private val c get()=ApplicationProvider.getApplicationContext<Context>()
    private val evidence get()=File(System.getProperty("gbo.p0.evidence"),"sic17-mixto").apply{mkdirs()}
    private fun book(bytes:ByteArray)=TemplateWorkbook(bytes.inputStream())
    private fun cell(b:TemplateWorkbook,ref:String)=b.cell(1,ref).textContent
    private fun rows(b:TemplateWorkbook)=b.sheet(1).nodes("row").filter{it.getAttribute("r").toInt()>=14}
    private fun bytes(write:(ByteArrayOutputStream)->Unit)=ByteArrayOutputStream().also(write).toByteArray()
    private suspend fun native(db:InventoryDatabase,id:String="NATIVE_B",route:String="PE-28H",pr:String="0010",m:Double=300.0,notes:String?="=OBS B\n literal ") {
        db.inventoryDao().insertRecord(testRecord(id,pr,m,sic="SIC-17",route=route).copy(status="ACTIVE",endPrCode="0012",endDistanceM=65.0,observations=notes,surveyDate="26/10/2026"))
        db.inventoryDao().insertSic17(Sic17Entity(id,"02","1","MISMO_NOMBRE","INV-B",3,71.25,6.5,"2","3","1","1","CAUCE B",8.75))
    }
    private fun notesPlain(b:TemplateWorkbook) {
        assertEquals(21,b.sheet(1).nodes("row").single{it.getAttribute("r")=="13"}.children().size)
        assertEquals("OBSERVACIONES",cell(b,"U13"));assertEquals("0",b.cell(1,"U13").getAttribute("s"))
        assertTrue(b.sheet(1).nodes("col").none{it.getAttribute("min")=="21"})
        assertEquals("A13:T16",b.sheet(1).nodes("autoFilter").single().getAttribute("ref"))
        for(n in 14..16)assertEquals("0",b.cell(1,"U$n").getAttribute("s"))
        assertTrue(b.sheet(1).nodes("f").isEmpty())
    }
    @Test fun mixedThreeInterleavedRowsEditBothOriginsReplayAndSeparateRoutes()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val repo=SicConsolidationFixtures.repo(c,db);val st=SicTestStorage()
            val a=SicConsolidationFixtures.bridge(repo,"A","11+200")
            val ar=repo.dao.snapshot(a)!!.roadRecord!!
            db.inventoryDao().updateRecord(ar.copy(observations="OBS A = literal"))
            native(db)
            val ci=SicConsolidationFixtures.bridge(repo,"C","10+100")
            repo.setSicField(a,"SIC-17","bridgeCode","MISMO_NOMBRE")
            repo.setSicField(ci,"SIC-17","bridgeCode","MISMO_NOMBRE")
            repo.setSicField(ci,"SIC-17","singularityName","")
            native(db,id="OTRA_RUTA",route="PE-3N",pr="0001",notes=null)
            val beforeSnapshots=repo.dao.allSnapshots();val beforeNative=db.inventoryDao().recordById("NATIVE_B")
            val first=LocalSicConsolidates.update(c,db,"PE-28H","SIC-17",st,a)
            val original=st.canonical("SIC-17").bytes.copyOf();var b=book(original)
            assertEquals(3,first.rows);assertEquals(2,first.pending);assertEquals(0,first.retained)
            assertEquals(listOf("0010","0010","0011"),(14..16).map{cell(b,"C$it")})
            assertEquals(listOf("100.0","300.0","200.0"),(14..16).map{cell(b,"D$it")})
            assertEquals(listOf("","CAUCE B","A"),(14..16).map{cell(b,"S$it")})
            assertEquals("OBS A = literal",cell(b,"U16"));assertEquals("=OBS B\n literal ",cell(b,"U15"));assertEquals("",cell(b,"U14"))
            assertEquals("71.25",cell(b,"L15"));assertEquals("6.5",cell(b,"M15"));assertEquals("8.75",cell(b,"T15"))
            assertEquals("0012",cell(b,"E15"));assertEquals("65.0",cell(b,"F15"));assertEquals("02",cell(b,"G15"))
            assertEquals("2",cell(b,"N15"));assertEquals("3",cell(b,"O15"));assertEquals("3",cell(b,"K15"));assertTrue(cell(b,"P15").toDouble()>40000)
            assertEquals(beforeSnapshots,repo.dao.allSnapshots());assertEquals(beforeNative,db.inventoryDao().recordById("NATIVE_B"))
            assertEquals("Download/GBO Inventario Vial/PE-28H/EXCEL_SIC/",st.canonical("SIC-17").parent)
            notesPlain(b)
            repo.setSicField(a,"SIC-17","dimension1LengthM","90.5")
            val edited=db.inventoryDao().recordById("NATIVE_B")!!
            db.inventoryDao().updateRecord(edited.copy(observations="B EDITADO",updatedAt=edited.updatedAt+1))
            val second=LocalSicConsolidates.update(c,db,"PE-28H","SIC-17",st)
            b=book(st.canonical("SIC-17").bytes)
            assertEquals(3,second.rows);assertEquals("90.5",cell(b,"L16"));assertEquals("71.25",cell(b,"L15"));assertEquals("B EDITADO",cell(b,"U15"))
            val writes=st.writes;val replay=LocalSicConsolidates.update(c,db,"PE-28H","SIC-17",st,ci)
            assertEquals(second.uri,replay.uri);assertEquals(writes,st.writes)
            assertEquals(1,st.entries.values.count{it.name=="SIC-17.xlsx"&&it.parent.contains("PE-28H")})
            assertTrue(st.entries.values.any{it.name.contains(".anterior-")&&it.bytes.contentEquals(original)})
            assertEquals(1,LocalSicConsolidates.update(c,db,"PE-3N","SIC-17",st).rows)
            assertEquals("PE-3N",cell(book(st.canonical("SIC-17","PE-3N").bytes),"A14"))
            assertTrue(db.automaticSyncDao().all().isEmpty())
            File(evidence,"SIC-17_TRES_FILAS_MIXTAS.xlsx").writeBytes(st.canonical("SIC-17").bytes)
            File(evidence,"SIC-17_ANTES_DE_EDICION.xlsx").writeBytes(original)
        }
    }
    @Test fun exactHistoricalTemplatesNormalizeSemanticallyAndKeepSourcePartsUntouched()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val repo=SicConsolidationFixtures.repo(c,db);val id=SicConsolidationFixtures.bridge(repo,"HISTORICO","10+010")
            val snapshot=repo.dao.snapshot(id)!!
            val remote=bytes{ScapSicExporter.write(it,snapshot,"SIC-17")};val original=remote.copyOf()
            assertEquals(BridgeSicContract.keys17,LocalSic17Workbook.keys)
            val new=LocalSic17Workbook.normalize(remote,"=1+2\n TEXTO")
            val b=book(new);assertEquals("0010",cell(b,"C14"));assertEquals("10.0",cell(b,"D14"));assertEquals("=1+2\n TEXTO",cell(b,"U14"))
            assertEquals("inlineStr",b.cell(1,"U14").getAttribute("t"));assertTrue(b.sheet(1).nodes("f").isEmpty())
            assertArrayEquals(original,remote)
            val first=book(SicRouteWorkbook.merge(listOf(new),"SIC-17"));val second=book(LocalSic17Workbook.normalize(new))
            assertEquals(first.parts.keys,second.parts.keys);for((p,v) in first.parts)assertArrayEquals(p,v,second.parts.getValue(p))
            File(evidence,"SIC-17_SCAP_ORIGINAL_20_COLUMNAS.xlsx").writeBytes(remote)
            File(evidence,"SIC-17_SCAP_NORMALIZADO_21_COLUMNAS.xlsx").writeBytes(new)
            native(db)
            val ordinary=db.inventoryDao().recordsForExport(null,"PE-28H").single{it.record.id=="NATIVE_B"}
            val fromNative=bytes{SicExcelWriter.write(it,listOf(ordinary),SicExportFormat.SIC17,ExportHeading())}
            val converted=book(LocalSic17Workbook.normalize(fromNative));val prior=book(fromNative)
            for((p,v) in prior.parts)assertArrayEquals(p,v,converted.parts.getValue(p))
            File(evidence,"SIC-17_INDEPENDIENTE_ORIGINAL_21_COLUMNAS.xlsx").writeBytes(fromNative)
        }
    }
    private fun rewrite(bytes:ByteArray,path:String,change:(String)->String):ByteArray {
        val b=book(bytes);b.parts[path]=change(b.parts.getValue(path).toString(Charsets.UTF_8)).toByteArray()
        return ByteArrayOutputStream().also{out->ZipOutputStream(out).use{z->for((p,v)in b.parts){z.putNextEntry(ZipEntry(p));z.write(v);z.closeEntry()}}}.toByteArray()
    }
    private fun stateDir()=File(c.filesDir,"sic_consolidated").listFiles()!!.single()
    @Test fun retainedLegacyRowIsTransformedWithoutEditingItsHashFileOrRoom()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val repo=SicConsolidationFixtures.repo(c,db);val st=SicTestStorage();val a=SicConsolidationFixtures.bridge(repo,"LEGADO")
            LocalSicConsolidates.update(c,db,"PE-28H","SIC-17",st)
            val snapshot=repo.dao.snapshot(a)!!
            val legacy=bytes{ScapSicExporter.write(it,snapshot,"SIC-17")}
            val h=com.tuempresa.inventariovial.delivery.sha256(legacy);val f=File(stateDir(),"rows/$h.xlsx").apply{writeBytes(legacy)}
            val meta=File(stateDir(),"current.json");val json=JSONObject(meta.readText());json.getJSONArray("rows").getJSONObject(0).put("sha256",h);meta.writeText(json.toString())
            repo.setSicField(a,"SIC-17","bridgeCode","") // retain the last valid row
            native(db)
            val originalSnapshot=repo.dao.snapshot(a)
            val after=LocalSicConsolidates.update(c,db,"PE-28H","SIC-17",st)
            assertEquals(2,after.rows);assertEquals(1,after.retained);assertEquals(1,after.pending)
            assertArrayEquals(legacy,f.readBytes());assertEquals(originalSnapshot,repo.dao.snapshot(a))
            assertEquals(listOf("LEGADO","CAUCE B"),rows(book(after.file.readBytes())).map{cell(book(after.file.readBytes()),"S${it.getAttribute("r")}")})
        }
    }
    @Test fun unknownTemplatesAndFormulasCannotReplaceTheLastValidWorkbook()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val repo=SicConsolidationFixtures.repo(c,db);val st=SicTestStorage();val a=SicConsolidationFixtures.bridge(repo,"PROTEGIDO")
            val before=LocalSicConsolidates.update(c,db,"PE-28H","SIC-17",st);val original=before.file.readBytes()
            val meta=File(stateDir(),"current.json");val saved=meta.readText()
            val originalRow=File(stateDir(),"rows/"+JSONObject(saved).getJSONArray("rows").getJSONObject(0).getString("sha256")+".xlsx").readBytes()
            for((path,change) in listOf<Pair<String,(String)->String>>(
                "xl/styles.xml" to {it.replace("Arial","ALTERADA")},
                "xl/worksheets/sheet1.xml" to {it.replace("Carretera","OTRO CAMPO")},
                "xl/worksheets/sheet1.xml" to {it.replace("width=\"16.0\"","width=\"99.0\"")},
                "xl/worksheets/sheet1.xml" to {it.replace("<row r=\"14\"","<row r=\"14\"").replace("</sheetData>","<row r=\"15\"><c r=\"A15\"><v>1</v></c></row></sheetData>")},
                "xl/worksheets/sheet1.xml" to {it.replace("<c r=\"L14\" s=\"4\">","<c r=\"L14\" s=\"4\"><f>A1+1</f>")}
            )) {
                val bad=rewrite(originalRow,path,change);assertFalse(bad.contentEquals(originalRow))
                assertTrue(runCatching{LocalSic17Workbook.normalize(bad)}.isFailure)
                val hash=com.tuempresa.inventariovial.delivery.sha256(bad);File(stateDir(),"rows/$hash.xlsx").writeBytes(bad)
                val modified=JSONObject(saved);modified.getJSONArray("rows").getJSONObject(0).put("sha256",hash);meta.writeText(modified.toString())
                repo.setSicField(a,"SIC-17","bridgeCode","")
                assertTrue(runCatching{LocalSicConsolidates.update(c,db,"PE-28H","SIC-17",st)}.isFailure)
                assertArrayEquals(original,st.canonical("SIC-17").bytes);assertArrayEquals(original,before.file.readBytes())
            }
        }
    }
    @Test fun mixedPublicationShortWriteAndLostPromotionRecoverWithoutDuplicateRows()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val repo=SicConsolidationFixtures.repo(c,db);val st=SicTestStorage();SicConsolidationFixtures.bridge(repo,"ANTES")
            val before=LocalSicConsolidates.update(c,db,"PE-28H","SIC-17",st);val original=before.file.readBytes()
            native(db);st.shortWrite=true
            assertTrue(runCatching{LocalSicConsolidates.update(c,db,"PE-28H","SIC-17",st)}.isFailure)
            assertArrayEquals(original,st.canonical("SIC-17").bytes)
            st.shortWrite=false;st.stopAfterPromote=true
            assertTrue(runCatching{LocalSicConsolidates.update(c,db,"PE-28H","SIC-17",st)}.isFailure)
            val after=LocalSicConsolidates.update(c,db,"PE-28H","SIC-17",st)
            assertEquals(2,after.rows);assertEquals(2,rows(book(after.file.readBytes())).size)
            assertEquals(1,st.entries.values.count{it.name=="SIC-17.xlsx"})
            assertArrayEquals(original,before.file.readBytes());assertTrue(st.entries.values.any{it.name.contains(".anterior-")&&it.bytes.contentEquals(original)})
        }
    }
    @Test fun headerFormulasSurviveMergeAndDataFormulasAreRejected()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val repo=SicConsolidationFixtures.repo(c,db);val a=SicConsolidationFixtures.bridge(repo,"FORMULA")
            val snapshot=repo.dao.snapshot(a)!!
            val source=bytes{ScapSicExporter.write(it,snapshot,"SIC-17")}
            val header=rewrite(source,"xl/worksheets/sheet1.xml"){it.replace("<row r=\"11\"", "<row r=\"11\"").replace("</sheetData>","</sheetData>")
                .replace("<c r=\"A11\" s=\"1\" t=\"inlineStr\"><is><t xml:space=\"preserve\">SIC-17</t></is></c>","<c r=\"A11\" s=\"1\"><f>1+1</f><v>2</v></c>")}
            assertTrue(book(header).sheet(1).nodes("f").isNotEmpty())
            val merged=book(SicRouteWorkbook.merge(listOf(header,header),"SIC-17"))
            assertEquals("1+1",merged.sheet(1).nodes("f").single().textContent)
            assertTrue(runCatching{LocalSic17Workbook.normalize(header)}.isFailure) // no silent stripping during adaptation
        }
    }
}
