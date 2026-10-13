package com.tuempresa.inventariovial.field

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.tuempresa.inventariovial.ChoiceSelector
import com.tuempresa.inventariovial.data.entity.InventoryRecordEntity
import com.tuempresa.inventariovial.road.*
import com.tuempresa.inventariovial.validation.*
import com.tuempresa.inventariovial.core.ValidationIssue

enum class SicFormMode { CREATE, EDIT }
data class SicCoreDraft(val route:String,val roadbed:String,val startPr:String,val startDistance:String,
    val endPr:String="",val endDistance:String="",val side:String="D",val segment:String="",val direction:String="INCREASING") : java.io.Serializable {
    fun applyTo(r:InventoryRecordEntity,hasEnds:Boolean,hasSide:Boolean)=r.copy(routeCode=route,roadbedCode=roadbed,
        startPrCode=startPr,startDistanceM=startDistance.replace(',','.').toDouble(),
        endPrCode=if(hasEnds)endPr.ifBlank {null} else r.endPrCode,
        endDistanceM=if(hasEnds)endDistance.replace(',','.').toDoubleOrNull() else r.endDistanceM,
        sideCode=if(hasSide)side else r.sideCode,segment=segment,surveyDirection=direction)
    fun errors(hasEnds:Boolean):List<ValidationIssue> = buildList {
        fun missing(label:String,message:String) {add(ValidationIssue(label,message,ValidationIssue.Severity.MISSING_USER_INPUT))}
        if(route.isBlank())missing("Ruta","Ruta obligatoria")
        if(roadbed.isBlank())missing("Calzada","Calzada obligatoria")
        if(ContractualLocationInput.prError(startPr)!=null)missing("PR inicial","PR inicial inválido")
        if(ContractualLocationInput.distanceError(startDistance)!=null)missing("Distancia inicial","Distancia inicial inválida")
        if(hasEnds) {
            if(ContractualLocationInput.prError(endPr)!=null)missing("PR final","PR final inválido")
            if(ContractualLocationInput.distanceError(endDistance)!=null)missing("Distancia final","Distancia final inválida")
        }
    }
    companion object {fun from(r:InventoryRecordEntity)=SicCoreDraft(r.routeCode,r.roadbedCode,r.startPrCode,r.startDistanceM.toString(),r.endPrCode.orEmpty(),r.endDistanceM?.toString().orEmpty(),r.sideCode ?: "D",r.segment.orEmpty(),r.surveyDirection ?: "INCREASING")}
}

object SicFormPolicy {
    fun hasEnds(code:String,asset:String,classCode:String="")=code in setOf("SIC-17","SIC-19","SIC-20","SIC-21","SIC-23")
    fun sides(code:String,classCode:String=""):List<String> = when {
        code in setOf("SIC-17","SIC-18")->emptyList()
        code=="SIC-20" && classCode=="13"->listOf("S - Sin objeto")
        code=="SIC-20" && classCode=="14"->listOf("I - Izquierdo","D - Derecho")
        else->listOf("D - Derecho","I - Izquierdo","S - Sin objeto")
    }
}

/** Shared create/edit controls: no GNSS suggestion UI; hidden historical endpoints stay in Room. */
@Composable fun SicFormCore(mode:SicFormMode,code:String,asset:String,classCode:String,reference:RoadReferenceData,
    draft:SicCoreDraft,onChange:(SicCoreDraft)->Unit) {
    // Mode is an action context only; it never changes field visibility, labels or catalogues.
    val context=LocalContext.current
    val survey=remember(context){SurveyPreferences(context)}
    SurveyHeader(survey,draft.segment,draft.route,draft.roadbed,draft.direction,
        {onChange(draft.copy(segment=it))},{onChange(draft.copy(route=it))},{onChange(draft.copy(roadbed=it))},{onChange(draft.copy(direction=it))})
    SicPrFields(reference,draft.route,draft.roadbed,draft.startPr,draft.startDistance,if(code=="SIC-22")"elemento" else "inicio",
        {onChange(draft.copy(startPr=it))},{onChange(draft.copy(startDistance=it))})
    if(SicFormPolicy.hasEnds(code,asset,classCode))SicPrFields(reference,draft.route,draft.roadbed,draft.endPr,draft.endDistance,"fin",
        {onChange(draft.copy(endPr=it))},{onChange(draft.copy(endDistance=it))})
    val sides=SicFormPolicy.sides(code,classCode)
    if(sides.isNotEmpty()) {
        Text("Lado")
        ChoiceSelector(sides,sides.find {it.substringBefore(" - ")==draft.side} ?: draft.side){onChange(draft.copy(side=it.substringBefore(" - ")))}
    }
}

@Composable fun SicPrFields(reference:RoadReferenceData,route:String,roadbed:String,pr:String,distance:String,endpoint:String,onPr:(String)->Unit,onDistance:(String)->Unit) {
    val initial=endpoint!="fin";val point=endpoint=="elemento"
    val catalogue=ContractualPrInput.catalog(reference.prs,route,roadbed).isNotEmpty()
    com.tuempresa.inventariovial.SectionTitle(if(point)"Ubicación del elemento" else if(initial)"Ubicación inicio" else "Ubicación fin")
    ContractualPrField(value=pr,onValueChange=onPr,label={Text(if(catalogue)if(point)"Código PR" else if(initial)"Código PR inicial" else "Código PR final" else if(point)"Kilómetro de progresiva (PR)" else if(initial)"Kilómetro de progresiva inicial (PR)" else "Kilómetro de progresiva final (PR)")},placeholder={Text("4 dígitos, ej. 0010")},modifier=androidx.compose.ui.Modifier.fillMaxWidth())
    PrDistanceField(value=distance,onValueChange=onDistance,label={Text(if(catalogue)if(point)"Metros desde el PR" else if(initial)"Metros desde el PR inicial" else "Metros desde el PR final" else if(point)"Metros desde el kilómetro" else if(initial)"Metros desde el kilómetro inicial" else "Metros desde el kilómetro final")},modifier=androidx.compose.ui.Modifier.fillMaxWidth())
}
