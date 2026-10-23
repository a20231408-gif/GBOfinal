package com.tuempresa.inventariovial

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.auth.*
import com.tuempresa.inventariovial.core.InventoryDraftStore
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.dashboard.DashboardDrafts
import com.tuempresa.inventariovial.diagnostics.Sic20DraftDiagnostics
import com.tuempresa.inventariovial.field.resolveDraftDestination
import com.tuempresa.inventariovial.model.RoadAssetType
import com.tuempresa.inventariovial.model.form.*
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
class Sic20DraftResolution111Test {
    private val app get()=ApplicationProvider.getApplicationContext<Application>()
    @Test fun historicalEquivalentAliasesAndMissingRawClassAreReadWithoutRewritingEvidence()=runBlocking {
        initializeIdleUiWorkers()
        com.tuempresa.inventariovial.localfiles.LocalCopySettings.enabled(app,false)
        AuthGraph.get(app).sessions.login(UserCacheEntity("TEST111_RESOLVE","test","FICTICIO","ENGINEER",true,"",1,1,1))
        val db=InventoryDatabase.getInstance(app);db.syncContext=null
        for((asset,code,expected)in listOf(Triple("WALL","14",RoadAssetType.WALL),Triple("TUNNEL","13",RoadAssetType.TUNNEL),Triple("FORD","12",RoadAssetType.FORD))) {
            val id="TEST111_LEGACY_${UUID.randomUUID()}"
            val r=InventorySaveRequest("SIC-20",asset,"PE-28H","CD","0010","10","0011","180","D",Double.NaN,Double.NaN,null,"20/10/2026","TEST111 OBS","",
                SicFormDetail.Sic20(Sic20FormState(classCode=code)),recordId=id)
            InventoryDraftStore.save(app,db,r)
            val file=File(app.filesDir,"inventory_drafts/$id.json")
            val raw=InventoryDraftStore.read(app,id)!!;raw.getJSONObject("detail").getJSONObject("fields").remove("classCode")
            file.writeText(raw.toString()) // Fictitious older shape, no mutation of any device/production record.
            val before=file.readBytes()
            assertEquals(expected,resolveDraftDestination(app,db.inventoryDao().snapshot(id)!!).asset)
            assertArrayEquals(before,file.readBytes());assertEquals(asset,db.inventoryDao().recordById(id)!!.assetType)
        }
    }
    @Test fun unrecognizedSubtypeIsNeverPresentedAsFord() {
        val r=testRecord("TEST111_UNKNOWN",sic="SIC-20").copy(assetType="UNKNOWN")
        assertTrue(DashboardDrafts.title(r).contains("pendiente"))
        assertEquals("Túnel",DashboardDrafts.title(r.copy(assetType="TÚNEL")))
        assertTrue(runCatching{com.tuempresa.inventariovial.field.draftDestination(r)}.isFailure)
    }
    @Test fun temporaryDiagnosticsAreBoundedAndNeverCaptureArbitraryTextOrIdentity() {
        val secret="TEST_PRIVATE_TOKEN_DO_NOT_EXPORT"
        repeat(100){Sic20DraftDiagnostics.record(app,"FORM_SELECTED",secret,secret,secret,secret)}
        val report=Sic20DraftDiagnostics.report(app);val events=report.getJSONArray("events")
        assertEquals(64,events.length());assertFalse(report.toString().contains(secret))
        for(n in 0 until events.length()) {
            val e=events.getJSONObject(n)
            assertEquals(setOf("seq","stage","asset","class","type","rawClass"),e.keys().asSequence().toSet())
            assertEquals("UNKNOWN",e.getString("asset"));assertEquals("UNKNOWN",e.getString("class"))
        }
    }
}
