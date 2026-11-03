package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.Sic19Entity
import com.tuempresa.inventariovial.export.*
import com.tuempresa.inventariovial.localfiles.*
import com.tuempresa.inventariovial.pavement.*
import com.tuempresa.inventariovial.scap.export.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class)
class SicConsolidationDurabilityTest {
    private val c get()=ApplicationProvider.getApplicationContext<Context>()
    @Test fun fullRoomCloseReopenRetainsIdentityAndReusesVerifiedPublicDocument()=runBlocking {
        val name="sic.db";val st=SicTestStorage();lateinit var id:String;lateinit var before:LocalSicResult
        Room.databaseBuilder(c,InventoryDatabase::class.java,name).build().use{db->
            id=SicConsolidationFixtures.bridge(SicConsolidationFixtures.repo(c,db),"PERSISTENTE")
            before=LocalSicConsolidates.update(c,db,"PE-28H","SIC-17A",st)
        }
        Room.databaseBuilder(c,InventoryDatabase::class.java,name).build().use{db->
            assertNotNull(db.scapDao().snapshot(id));val writes=st.writes
            val after=LocalSicConsolidates.update(c,db,"PE-28H","SIC-17A",st,id)
            assertEquals(before.uri,after.uri);assertEquals(writes,st.writes);assertEquals(1,after.rows)
            assertTrue(File(after.file.parentFile,"manifest.json").isFile);assertEquals(1,db.scapDao().allSnapshots().size)
        }
    }
    @Test fun ordinarySic19ConsolidatesByUuidAndRetainsLiteralObservations()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val st=SicTestStorage()
            for((id,pr) in listOf("FIC_C" to "0012","FIC_A" to "0001","FIC_B" to "0002")) {
                db.inventoryDao().insertRecord(testRecord(id,pr,10.0,sic="SIC-19",route="PE-28H").copy(status="ACTIVE",observations="=$id"))
                db.inventoryDao().insertSic19(Sic19Entity(id,"08","2","1","1","1"))
            }
            val a=LocalSicConsolidates.update(c,db,"PE-28H","SIC-19",st)
            assertEquals(3,a.rows);val before=TemplateWorkbook(a.file.inputStream())
            assertEquals(listOf("0001","0002","0012"),(14..16).map{before.cell(1,"C$it").textContent})
            assertTrue(before.sheet(1).nodes("f").isEmpty())
            val current=db.inventoryDao().recordById("FIC_A")!!;db.inventoryDao().updateRecord(current.copy(observations="TEXTO CORREGIDO",updatedAt=current.updatedAt+1))
            val b=LocalSicConsolidates.update(c,db,"PE-28H","SIC-19",st)
            assertEquals(3,b.rows);assertTrue(TemplateWorkbook(b.file.inputStream()).sheet(1).documentElement.textContent.contains("TEXTO CORREGIDO"))
            assertTrue(db.automaticSyncDao().all().isEmpty())
        }
    }
    @Test fun pavementSic15UsesDamageIdentityAndDoesNotMixSurfaceOrChangeCalculations()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val st=SicTestStorage()
            for(n in listOf(2,1)) {
                val id="FICTICIO_SAMPLE_$n"
                var sample=PavementSampleEntity(id,"TRAMO","PE-28H",n,"Derecho","CD","00+000","00+020",10.0,20.0,"RIGID","2026-10-26","TEST","FICTICIO",updatedAt=100+n.toLong())
                val damage=PavementDamageEntity("FICTICIO_DAMAGE_$n",id,1,"RIGID",22,"FICTICIO","M","AREA",0.0,0.0,1.0,1.0,null,1.0,2.0,2.0,1.0,null,"FICTICIO",createdAt=n.toLong())
                sample=sample.copy(sicDataJson=PavementSic.update(sample,PavementSicFormat.SIC15,damage.id,mapOf("faja" to "1","prInicio" to if(n==1)"0012"else"0000","distanciaInicio" to "10","prFin" to "0012","distanciaFin" to "20","longitud" to "2","ancho" to "1","gravedad" to "2")))
                db.pavementDao().saveSample(sample);db.pavementDao().saveDamage(damage)
            }
            val before=db.pavementDao().localSamplesForSic()
            val a=LocalSicConsolidates.update(c,db,"PE-28H","SIC-15",st)
            assertEquals(2,a.rows);val book=TemplateWorkbook(a.file.inputStream())
            assertEquals(listOf("0012","0000"),book.sheet(1).nodes("row").filter{it.getAttribute("r").toInt()>=3}.map{it.children()[3].textContent})
            assertEquals(before,db.pavementDao().localSamplesForSic())
            assertTrue(runCatching{LocalSicConsolidates.update(c,db,"PE-28H","SIC-14",st)}.isFailure)
            assertTrue(st.entries.values.none{it.name=="SIC-14.xlsx"})
        }
    }
    @Test fun automaticLocalJobRefreshesConsolidatesWithoutAnUploadOrExtraScheduler()=runBlocking {
        org.robolectric.shadows.ShadowStatFs.registerStats(c.filesDir.path,1_000_000,900_000,900_000)
        SicConsolidationFixtures.db(c).use{db->
            AutomaticLocalDocuments.install(db.openHelper.writableDatabase,true)
            val r=SicConsolidationFixtures.repo(c,db);val id=SicConsolidationFixtures.bridge(r,"AUTOMATICO")
            val source=File(c.filesDir,"FICTICIO_BACKUP.txt").apply{writeText("FICTICIO")};val hash=fileDigest(source)
            val job=db.localFilesDao().jobs().single{it.state=="QUEUED"&&it.recordId==id}
            assertTrue(job.automatic)
            val prepare:suspend(Context,InventoryDatabase,LocalExportJob)->LocalPlan={_,_,j->
                // Model a newly generated plan. Legacy frozen plans must not acquire new projections.
                LocalPlan(listOf(LocalSharedFile(j.id+"/DOC",j.id,j.recordKey,"Download/GBO_Ruta_Clara/PRUEBA/${j.id}/","respaldo.txt","text/plain",hash.first,hash.second,source.path)),emptyList(),publicNamingVersion=6)
            }
            val st=SicTestStorage();kotlinx.coroutines.delay(AutomaticLocalDocuments.QUIET_MS+50)
            LocalCopyDrain.run(c,db,st,limit=30,prepare=prepare)
            for(format in listOf("SIC-17","SIC-17A","SIC-17B"))assertEquals(1,SicRouteWorkbook.inspect(st.canonical(format).bytes,format))
            val result=db.localFilesDao().job(job.id)!!
            assertEquals(result.message,"PARTIAL",result.state);assertEquals(result.total,result.completed)
            assertTrue(result.message!!.contains("PROVISIONAL"));assertTrue(db.automaticSyncDao().all().isEmpty())
            val previousUri=st.canonical("SIC-17A").uri
            SicConsolidationFixtures.bridge(r,"SEGUNDO_AUTOMATICO","11+000")
            r.setSicField(id,"SIC-17A","bridgeName","AUTOMATICO_EDITADO")
            kotlinx.coroutines.delay(AutomaticLocalDocuments.QUIET_MS+50)
            LocalCopyDrain.run(c,db,st,limit=30,prepare=prepare)
            assertEquals(2,SicRouteWorkbook.inspect(st.canonical("SIC-17A").bytes,"SIC-17A"))
            val current=LocalSicConsolidates.current(c,"PE-28H","SIC-17A",st)
            assertEquals(st.canonical("SIC-17A").uri,current.uri);assertNotEquals(previousUri,current.uri)
            assertEquals(1,st.entries.values.count{it.name=="SIC-17A.xlsx"});assertEquals(0,db.localFilesDao().pendingCount())
            assertTrue(db.automaticSyncDao().all().isEmpty())
        }
    }
    @Test fun mixedScapAndIndependentSic17ConsolidateWithoutLosingThePreviousVersion()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val st=SicTestStorage();val repo=SicConsolidationFixtures.repo(c,db)
            SicConsolidationFixtures.bridge(repo,"SCAP_VALIDO")
            val before=LocalSicConsolidates.update(c,db,"PE-28H","SIC-17",st)
            val original=st.find(before.uri).bytes.copyOf()
            val native=testRecord("FICTICIO_NATIVE", "0001",10.0,sic="SIC-17",route="PE-28H").copy(status="ACTIVE",observations="OBSERVACION NATIVA")
            db.inventoryDao().insertRecord(native)
            db.inventoryDao().insertSic17(com.tuempresa.inventariovial.data.entity.Sic17Entity(native.id,"1","1","F-NATIVE","F-NATIVE",1,8.0,2.0,"1","1","1","1","FICTICIO",3.0))
            val mixed=LocalSicConsolidates.update(c,db,"PE-28H","SIC-17",st)
            assertEquals(2,mixed.rows);assertEquals(2,SicRouteWorkbook.inspect(st.canonical("SIC-17").bytes,"SIC-17"))
            assertTrue(TemplateWorkbook(mixed.file.inputStream()).sheet(1).documentElement.textContent.contains("OBSERVACION NATIVA"))
            assertTrue(st.entries.values.any{it.name.contains(".anterior-") && it.bytes.contentEquals(original)})
            assertTrue(db.automaticSyncDao().all().isEmpty())
        }
    }
}
