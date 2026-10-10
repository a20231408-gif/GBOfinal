package com.tuempresa.inventariovial.scap.domain

import com.tuempresa.inventariovial.scap.data.*

/** Visible reminders are projections; opening an inspection never asserts technical presence. */
object ScapPresentation {
    val defaultElements = listOf("104","110","111","202","205","302","311","353","372","401","402","501","511","530")
    val groups = listOf("SUPERESTRUCTURA","SUBESTRUCTURA","DETALLES","CAUCE","ACCESOS")
    val fixedStructures = listOf("LEFT_ABUTMENT","RIGHT_ABUTMENT","LEFT_ANCHOR","RIGHT_ANCHOR")
    /** Contractual C.4 visual order from the SCAP template. PIER is repeatable; anchors follow all piers. */
    val c4Order = listOf("LEFT_ABUTMENT","RIGHT_ABUTMENT","PIER","LEFT_ANCHOR","RIGHT_ANCHOR")
    fun fixedId(id:String,kind:String) = "$id:fixed:$kind"
    fun emptyStructure(id:String,kind:String) = ScapSubstructureEntity(fixedId(id,kind),id,kind,1,"","","","","")
    fun withVisibleStructures(s:ScapInspectionSnapshot) = s.copy(substructures = s.substructures +
        fixedStructures.filter { kind -> s.substructures.none { it.kind==kind } }.map { emptyStructure(s.inspection.id,it) })
    fun visibleElements(s:ScapInspectionSnapshot) = (defaultElements + s.elements.map { it.element.elementCode } + s.defects.mapNotNull {it.elementCode}).distinct()
    fun visibleElements(s:ScapInspectionSnapshot,catalog:com.tuempresa.inventariovial.scap.catalog.ScapCatalog) =
        ((if(catalog.bridgeType==null) defaultElements else catalog.defaultElements)+s.elements.map{it.element.elementCode}+s.defects.mapNotNull{it.elementCode}).distinct()
    fun presence(s:ScapInspectionSnapshot,code:String):String {
        val e=s.elements.find {it.element.elementCode==code}?.element ?: return "Sin completar"
        return when(ScapElementView.state(s,e)){ScapElementState.UNSET->"Sin completar";ScapElementState.PRESENT->"Presente";ScapElementState.NOT_APPLICABLE->"No aplica"}
    }
}

object ScapPhotoCategories {
    val primary = linkedMapOf("RIGHT_SIDE" to "Vista lado derecho", "LEFT_SIDE" to "Vista lado izquierdo",
        "UPSTREAM" to "Aguas arriba", "DOWNSTREAM" to "Aguas abajo", "ROAD_DECK" to "Carretera / tablero",
        "INFORMATIVE_SIGN" to "Señal informativa", "RAILINGS_SIDEWALKS_PIPES" to "Barandas / veredas / tuberías",
        "DEFECT" to "Anomalías o fallas", "BRIDGE_ELEMENT" to "Otros elementos del puente")
    val labels = primary + linkedMapOf("GENERAL" to "Vista general", "TRANSVERSE" to "Vista transversal",
        "ELEMENT" to "Elemento", "ACCESS" to "Acceso", "CHANNEL" to "Cauce", "OTHER" to "Otra")
    fun label(code:String?) = labels[code] ?: code.orEmpty().ifBlank { "Sin categoría (histórica)" }
}
