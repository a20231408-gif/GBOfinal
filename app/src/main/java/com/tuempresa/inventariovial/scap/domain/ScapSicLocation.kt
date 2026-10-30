package com.tuempresa.inventariovial.scap.domain

import com.tuempresa.inventariovial.data.entity.InventoryRecordEntity
import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot
import kotlin.math.abs

/** Location identification only. Never computes contractual lengths between different PRs. */
data class ScapPrLocation(val pr:String,val metres:Double) {
    fun matches(r:InventoryRecordEntity)=r.startPrCode.toIntOrNull()==pr.toInt() &&
        r.startDistanceM.isFinite() && abs(r.startDistanceM-metres)<0.000001
    val label get()="PR $pr + $metres m"
}

object ScapSicLocation {
    const val INHERITED="SCAP_PROGRESSIVE"
    const val CONFIRMED="SIC_CONFIRMED"
    fun parse(value:String?):ScapPrLocation? {
        val text=value?.trim() ?: return null
        // The conventional form is split into its two identifiers, never evaluated as arithmetic.
        if('+' in text) {
            val match=Regex("([0-9]{1,4})\\s*\\+\\s*([0-9]{1,3}(?:[.,][0-9]+)?)").matchEntire(text) ?: return null
            val metres=ScapNumbers.decimal(match.groupValues[2])?.takeIf{it>=0 && it<1000} ?: return null
            return ScapPrLocation(match.groupValues[1].padStart(4,'0'),metres)
        }
        // Preserve the existing SCAP input contract: a number without '+' denotes metres.
        val metres=ScapNumbers.decimal(text)?.takeIf{it>=0 && it<10_000_000} ?: return null
        return ScapPrLocation((metres/1000).toInt().toString().padStart(4,'0'),metres%1000)
    }
    fun mayInherit(r:InventoryRecordEntity)=r.locationSource!=CONFIRMED &&
        (r.startPrCode.isBlank() || r.locationSource==INHERITED)
    fun discrepancy(s:ScapInspectionSnapshot):String? {
        val r=s.roadRecord ?: return null
        val raw=s.values()["progressive"].orEmpty()
        val parsed=parse(raw)
        if(parsed==null) return if(r.locationSource==INHERITED && r.startPrCode.isNotBlank())
            "Kilometraje SCAP incompleto o inválido. La ubicación anterior se conserva; revisa la sección A antes de exportar SIC." else null
        val route=s.values()["route"].orEmpty()
        if(r.startPrCode.isBlank())return null
        if(parsed.matches(r) && (route.isBlank() || r.routeCode.isBlank() || route==r.routeCode))return null
        return "Ubicaciones distintas: SCAP ${route.ifBlank {"ruta pendiente"}} · ${parsed.label}; SIC ${r.routeCode} · PR ${r.startPrCode} + ${r.startDistanceM} m. " +
            if(r.locationSource==CONFIRMED) "Se conserva la ubicación confirmada manualmente en SIC. Puedes modificarla o elegir el kilometraje SCAP si corresponde a la misma referencia." else
                "La ubicación vinculada no está confirmada. Revisa ambas fuentes y elige o corrige la ubicación SIC."
    }
    fun needsReview(s:ScapInspectionSnapshot)=s.roadRecord?.locationSource!=CONFIRMED && discrepancy(s)!=null
}
