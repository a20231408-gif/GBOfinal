package com.tuempresa.inventariovial.validation

import com.tuempresa.inventariovial.model.form.*
import com.tuempresa.inventariovial.road.*

object TechnicalElementRules {
    fun sides(detail:SicFormDetail):Set<String> = when((detail as? SicFormDetail.Sic20)?.state?.classCode) {
        "13" -> setOf("S")
        "14" -> setOf("I","D")
        else -> setOf("I","D","S")
    }
    // Official SIC-21 catalog: metal is Acero (1), concrete is Concreto (2).
    fun selectSafetyType(state:Sic21FormState,type:String,isNew:Boolean) = state.copy(typeCode=type,
        materialCode=if(isNew && state.classCode=="19" && type!=state.typeCode) when(type){"1"->"1";"4"->"2";else->state.materialCode} else state.materialCode)
    fun linear(detail:SicFormDetail) = when(detail) {
        is SicFormDetail.Sic19 -> true
        is SicFormDetail.Sic20 -> detail.state.classCode in setOf("12","14")
        is SicFormDetail.Sic21 -> detail.state.classCode=="20" || detail.state.classCode=="19" && detail.state.typeCode=="1"
        else -> false
    }
}

data class LengthEndpoint(val route:String,val bed:String,val pr:String,val distance:String)
data class TechnicalLength(val metres:Double?=null,val source:String="UNRESOLVED",val warning:String?=null)

/** PR is an identifier. Only scoped catalog chainage (or offsets of the same PR) proves a distance. */
object ContractualLength {
    fun calculate(start:LengthEndpoint,end:LengthEndpoint,prs:List<RoadPr>,direction:String):TechnicalLength {
        fun unresolved(message:String)=TechnicalLength(warning=message)
        if(start.route.isBlank() || start.bed.isBlank() || end.route.isBlank() || end.bed.isBlank())
            return unresolved("Completa ruta y calzada de ambos extremos.")
        if(normalizedRoadCode(start.route)!=normalizedRoadCode(end.route) || normalizedRoadCode(start.bed)!=normalizedRoadCode(end.bed))
            return unresolved("Los extremos deben corresponder a la misma ruta y calzada.")
        if(listOf(start,end).any {ContractualLocationInput.prError(it.pr)!=null || ContractualLocationInput.distanceError(it.distance)!=null})
            return unresolved("Completa las progresivas inicial y final para calcular longitud.")
        val a=start.distance.replace(',','.').toDouble();val b=end.distance.replace(',','.').toDouble()
        val scoped=ContractualPrInput.catalog(prs,start.route,start.bed)
        val same=normalizedPr(start.pr)==normalizedPr(end.pr)
        val delta:Double
        val source:String
        if(same) {delta=b-a;source="SAME_PR_OFFSETS"} else {
            val first=scoped.singleOrNull {normalizedPr(it.prCode)==normalizedPr(start.pr)}
            val last=scoped.singleOrNull {normalizedPr(it.prCode)==normalizedPr(end.pr)}
            if(first==null || last==null || !first.chainageM.isFinite() || !last.chainageM.isFinite())
                return unresolved("Falta equivalencia contractual PR → distancia acumulada en el catálogo; conserva la longitud medida.")
            delta=last.chainageM+b-first.chainageM-a;source="OFFICIAL_PR_CHAINAGE"
        }
        if(direction !in setOf("INCREASING","DECREASING")) return unresolved("Selecciona el sentido del recorrido.")
        if(direction=="INCREASING" && delta<0 || direction=="DECREASING" && delta>0)
            return unresolved("La progresiva final contradice el sentido del recorrido.")
        return TechnicalLength(kotlin.math.abs(delta),source)
    }
    fun calculate(r:com.tuempresa.inventariovial.data.entity.InventoryRecordEntity,prs:List<RoadPr>) = calculate(
        LengthEndpoint(r.routeCode,r.roadbedCode,r.startPrCode,r.startDistanceM.toString()),
        LengthEndpoint(r.routeCode,r.roadbedCode,r.endPrCode.orEmpty(),r.endDistanceM?.toString().orEmpty()),prs,r.surveyDirection ?: "INCREASING")
}
