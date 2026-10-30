package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.export.*
import com.tuempresa.inventariovial.scap.export.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class)
class SicLocalConsolidationTest {
    private val c get()=ApplicationProvider.getApplicationContext<Context>()
    private val evidence get()=File(System.getProperty("gbo.p0.evidence"),"consolidados").apply{mkdirs()}
    private fun book(bytes:ByteArray)=TemplateWorkbook(bytes.inputStream())
    private fun values(bytes:ByteArray,column:String)=book(bytes).sheet(1).nodes("c").filter{it.getAttribute("r").matches(Regex("$column(?:1[4-9]|[2-9][0-9]+)"))}.map{it.textContent}
    @Test fun threeBridgesEditReplayAndEveryFormatStayInOneFilePerRoute()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val repo=SicConsolidationFixtures.repo(c,db);val store=SicTestStorage();val ids=mutableListOf<String>()
            for((name,pr) in listOf("Correviento" to "10+100","Santa Rosa" to "12+300","Tercer Puente" to "11+200")) {
                ids+=SicConsolidationFixtures.bridge(repo,name,pr)
                for(format in listOf("SIC-17","SIC-17A","SIC-17B")) {
                    val result=LocalSicConsolidates.update(c,db,"PE-28H",format,store,ids.last())
                    assertEquals(ids.size,result.rows);assertEquals(ids.size,result.pending);assertEquals(0,result.retained)
                    assertEquals(ids.size,SicRouteWorkbook.inspect(store.find(result.uri).bytes,format))
                }
            }
            assertEquals(listOf("Correviento","Tercer Puente","Santa Rosa"),values(store.canonical("SIC-17A").bytes,"A"))
            repo.setSicField(ids.first(),"SIC-17A","bridgeName","Correviento corregido")
            val edit=LocalSicConsolidates.update(c,db,"PE-28H","SIC-17A",store,ids.first())
            val writes=store.writes
            val replay=LocalSicConsolidates.update(c,db,"PE-28H","SIC-17A",store,ids.first())
            assertEquals(edit.uri,replay.uri);assertEquals(writes,store.writes)
            assertEquals(listOf("Correviento corregido","Tercer Puente","Santa Rosa"),values(store.find(edit.uri).bytes,"A"))
            val original=java.io.ByteArrayOutputStream().also{ScapSicExporter.write(it,repo.dao.snapshot(ids[0])!!,"SIC-17A")}.toByteArray()
            val template=book(original);val actual=book(store.find(edit.uri).bytes)
            for((path,bytes) in template.parts)if(path !in setOf("xl/workbook.xml","xl/worksheets/sheet1.xml","_rels/.rels","[Content_Types].xml"))assertArrayEquals(path,bytes,actual.parts.getValue(path))
            for(node in listOf("mergeCell","col","pageSetup","pageMargins","drawing"))assertEquals(node,template.sheet(1).nodes(node).map{it.attributes.let{a->(0 until a.length).map{n->a.item(n).nodeName+"="+a.item(n).nodeValue}}},actual.sheet(1).nodes(node).map{it.attributes.let{a->(0 until a.length).map{n->a.item(n).nodeName+"="+a.item(n).nodeValue}}})
            assertEquals("SIC-17A",actual.document("xl/workbook.xml").nodes("sheet").single().getAttribute("name"))
            File(evidence,"SIC-17A_TRES_PUENTES_FICTICIOS.xlsx").writeBytes(store.find(edit.uri).bytes)
            File(evidence,"SIC-17A_FILA_ORIGEN_FICTICIA.xlsx").writeBytes(original)
            for(format in listOf("SIC-17","SIC-17B"))File(evidence,"${format}_TRES_PUENTES_FICTICIOS.xlsx").writeBytes(store.canonical(format).bytes)
            assertTrue(db.automaticSyncDao().all().isEmpty());assertEquals(3,repo.dao.allSnapshots().size)
        }
    }
    @Test fun separateRoutesAndMissingPrSortLastWithoutAddingTechnicalColumns()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val r=SicConsolidationFixtures.repo(c,db);val st=SicTestStorage()
            val a=SicConsolidationFixtures.bridge(r,"SIN_PR");val b=SicConsolidationFixtures.bridge(r,"PR_VALIDO","1+050")
            SicConsolidationFixtures.bridge(r,"OTRA_RUTA","2+100","PE-22A")
            val road=r.dao.snapshot(a)!!.roadRecord!!;db.inventoryDao().updateRecord(road.copy(startPrCode=""))
            val x=LocalSicConsolidates.update(c,db,"PE-28H","SIC-17A",st)
            assertEquals(listOf("PR_VALIDO"),values(st.find(x.uri).bytes,"A"))
            val y=LocalSicConsolidates.update(c,db,"PE-22A","SIC-17A",st)
            assertEquals(listOf("OTRA_RUTA"),values(st.find(y.uri).bytes,"A"));assertNotEquals(x.uri,y.uri)
            assertEquals(16,book(st.find(x.uri).bytes).sheet(1).nodes("row").single{it.getAttribute("r")=="13"}.children().size)
            assertTrue(db.automaticSyncDao().all().isEmpty());assertNotEquals(a,b)
        }
    }
    @Test fun incompleteEditRetainsLastValidRowAndDoesNotClaimCompleteUpdate()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val r=SicConsolidationFixtures.repo(c,db);val st=SicTestStorage();val id=SicConsolidationFixtures.bridge(r,"CONSERVAR")
            val old=LocalSicConsolidates.update(c,db,"PE-28H","SIC-17A",st)
            val hash=com.tuempresa.inventariovial.localfiles.fileDigest(old.file)
            r.setSicField(id,"SIC-17A","bridgeName","")
            val a=LocalSicConsolidates.update(c,db,"PE-28H","SIC-17A",st)
            assertEquals(1,a.retained);assertEquals(listOf("CONSERVAR"),values(st.find(a.uri).bytes,"A"));assertNotNull(hash)
            assertTrue(a.message.contains("PROVISIONAL"))
            val exported=LocalSicConsolidates.update(c,db,"PE-28H","SIC-17A",st,id)
            assertEquals(1,exported.rows);assertTrue(exported.retained>0);assertEquals(listOf("CONSERVAR"),values(st.find(exported.uri).bytes,"A"))
        }
    }
    @Test fun concurrentRequestsDoNotDuplicateRowsOrPhysicalCanonicalFiles()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val r=SicConsolidationFixtures.repo(c,db);val st=SicTestStorage()
            for(n in 1..3)SicConsolidationFixtures.bridge(r,"CONCURRENTE_$n","$n+000")
            val results=coroutineScope{(1..3).map{async(Dispatchers.IO){LocalSicConsolidates.update(c,db,"PE-28H","SIC-17A",st)}}.awaitAll()}
            assertTrue(results.all{it.rows==3});assertEquals(1,results.map{it.uri}.distinct().size);assertEquals(1,st.entries.values.count{it.name=="SIC-17A.xlsx"})
        }
    }
    @Test fun interruptedShortWriteKeepsOldCanonicalAndRecoversSameJournal()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val r=SicConsolidationFixtures.repo(c,db);val st=SicTestStorage();val id=SicConsolidationFixtures.bridge(r,"ANTES")
            val old=LocalSicConsolidates.update(c,db,"PE-28H","SIC-17A",st);val bytes=st.find(old.uri).bytes.copyOf()
            r.setSicField(id,"SIC-17A","bridgeName","DESPUES");st.shortWrite=true
            assertTrue(runCatching{LocalSicConsolidates.update(c,db,"PE-28H","SIC-17A",st)}.isFailure)
            assertArrayEquals(bytes,st.canonical("SIC-17A").bytes)
            st.shortWrite=false;val now=LocalSicConsolidates.update(c,db,"PE-28H","SIC-17A",st)
            assertEquals(listOf("DESPUES"),values(st.find(now.uri).bytes,"A"));assertArrayEquals(bytes,old.file.readBytes())
        }
    }
    @Test fun stopAfterBackingUpAndLostFinalResponseAreRecoverableWithoutDuplicates()=runBlocking {
        for(afterBackup in listOf(true,false))SicConsolidationFixtures.db(c).use{db->
            val isolated=object:ContextWrapper(c){override fun getFilesDir()=File(c.filesDir,"FAULT_$afterBackup").apply{mkdirs()}}
            val r=SicConsolidationFixtures.repo(c,db);val st=SicTestStorage();val id=SicConsolidationFixtures.bridge(r,"ANTES")
            val before=LocalSicConsolidates.update(isolated,db,"PE-28H","SIC-17A",st)
            r.setSicField(id,"SIC-17A","bridgeName","RECUPERADO")
            st.stopAfterBackup=afterBackup;st.stopAfterPromote=!afterBackup
            assertTrue(runCatching{LocalSicConsolidates.update(isolated,db,"PE-28H","SIC-17A",st)}.exceptionOrNull() is CancellationException)
            val after=LocalSicConsolidates.update(isolated,db,"PE-28H","SIC-17A",st)
            assertEquals(listOf("RECUPERADO"),values(st.find(after.uri).bytes,"A"));assertEquals(1,st.entries.values.count{it.name=="SIC-17A.xlsx"})
            assertTrue(before.file.isFile);assertTrue(st.entries.values.any{it.name.contains(".anterior-")})
        }
    }
    @Test fun publicExternalChangesAndDeniedAccessNeverOverwriteLastValidPrivateCopy()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val r=SicConsolidationFixtures.repo(c,db);val st=SicTestStorage();val id=SicConsolidationFixtures.bridge(r,"ORIGINAL")
            val before=LocalSicConsolidates.update(c,db,"PE-28H","SIC-17A",st);val bytes=before.file.readBytes()
            st.find(before.uri).bytes="FICTICIO_OTRO_ARCHIVO".toByteArray()
            r.setSicField(id,"SIC-17A","bridgeName","CAMBIADO")
            assertTrue(runCatching{LocalSicConsolidates.update(c,db,"PE-28H","SIC-17A",st)}.isFailure)
            assertArrayEquals(bytes,before.file.readBytes());assertEquals("FICTICIO_OTRO_ARCHIVO",st.find(before.uri).bytes.toString(Charsets.UTF_8))
            st.deny=true;assertTrue(runCatching{LocalSicConsolidates.update(c,db,"PE-28H","SIC-17B",st)}.isFailure)
        }
    }
    @Test fun twoOfflineTabletsOnlyContainTheirOwnAvailableRows()=runBlocking {
        val outputs=mutableListOf<ByteArray>()
        for(name in listOf("TABLET_A","TABLET_B"))SicConsolidationFixtures.db(c).use{db->
            val isolated=object:ContextWrapper(c){override fun getFilesDir()=File(c.filesDir,name).apply{mkdirs()}}
            val r=SicConsolidationFixtures.repo(c,db);val st=SicTestStorage();SicConsolidationFixtures.bridge(r,name)
            val result=LocalSicConsolidates.update(isolated,db,"PE-28H","SIC-17A",st)
            assertEquals(1,result.rows);assertTrue(result.message.contains("únicamente"));outputs+=st.find(result.uri).bytes
        }
        assertEquals(listOf("TABLET_A"),values(outputs[0],"A"));assertEquals(listOf("TABLET_B"),values(outputs[1],"A"))
    }
}
