package com.tuempresa.inventariovial.diagnostics

import android.content.Context
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.delivery.sha256
import com.tuempresa.inventariovial.road.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Temporary 1.4.11 comparison: counts/digests only. No field IDs, descriptions, coordinates or records. */
object ContractualReferenceDiagnostics {
    suspend fun snapshot(c:Context,db:InventoryDatabase):JSONObject=withContext(Dispatchers.IO){runCatching {
        val bytes=c.assets.open("road_reference.json").use{it.readBytes()}
        val bundled=JsonRoadReferenceImporter().parse(bytes.toString(Charsets.UTF_8))
        val local=db.inventoryDao().fieldPrs()
        fun fingerprint(prs:List<RoadPr>)=sha256(prs.map{
            listOf(normalizedRoadCode(it.routeCode),normalizedRoadCode(it.roadbedCode),normalizedPr(it.prCode),it.chainageM.toString()).joinToString("\t")
        }.sorted().joinToString("\n").toByteArray())
        val reported=local.filter{normalizedRoadCode(it.routeCode)=="PE-28H"&&normalizedRoadCode(it.roadbedCode)=="CD"}
        JSONObject().put("available",true).put("assetSha256",sha256(bytes)).put("bundledPrCount",bundled.prs.size)
            .put("bundledPrFingerprint",fingerprint(bundled.prs)).put("localFieldPrCount",local.size)
            .put("localFieldPrFingerprint",fingerprint(local.map{it.asRoadPr()}))
            .put("reportedCaseStartLocalMatches",reported.count{normalizedPr(it.prCode)=="0010"})
            .put("reportedCaseEndLocalMatches",reported.count{normalizedPr(it.prCode)=="0011"})
            .put("scope","LOCAL_REFERENCE_ONLY; FIELD_CAPTURED does not certify contractual calibration")
    }.getOrElse{JSONObject().put("available",false)}}
}
