package com.tuempresa.inventariovial

import android.app.Application
import com.tuempresa.inventariovial.core.SicContributionPolicy
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
class SicContributionPolicyTest {
    @Test fun required17AndOptionalFlagsRemainIndependentInAllFourProfiles(){
        val p=JSONObject().put("pendingSic",JSONArray(listOf("SIC-17: fecha pendiente","SIC-17A: campo A","SIC-17B: campo B","SIC-DESCONOCIDO: conservar aviso")))
        val frozen=p.toString()
        for(a in listOf(false,true))for(b in listOf(false,true)){
            val rows=SicContributionPolicy.pending(p,a,b)
            assertTrue(rows.contains("SIC-17: fecha pendiente"));assertTrue(rows.contains("SIC-DESCONOCIDO: conservar aviso"))
            assertEquals(a,rows.any{it.startsWith("SIC-17A:")});assertEquals(b,rows.any{it.startsWith("SIC-17B:")});assertEquals(frozen,p.toString())
        }
    }
    @Test fun frozenHistoricalPendingListIsFilteredForPresentationWithoutInventingCompletionOrModifyingBytes(){
        val p=JSONObject().put("contentHash","FICTITIOUS_HASH").put("pendingSic",JSONArray(listOf("SIC-17A: pendiente","SIC-17B: pendiente")))
        val before=p.toString();assertTrue(SicContributionPolicy.pending(p,false,false).isEmpty());assertEquals(before,p.toString())
        assertFalse(p.has("receipt"));assertFalse(p.has("ok"));assertEquals("FICTITIOUS_HASH",p.getString("contentHash"))
    }
}
