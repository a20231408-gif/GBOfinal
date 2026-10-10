package com.tuempresa.inventariovial.scap.domain

import com.tuempresa.inventariovial.scap.data.*

enum class ScapElementState { UNSET, PRESENT, NOT_APPLICABLE }
/** F.1 and G.1 are views of these SAME stable entities; the visible/exported code is not the key. */
object ScapElementView {
    fun state(s:ScapInspectionSnapshot,e:ScapElementEntity)=when(s.values(e.id)["presence"]) {
        "UNSET","PENDING"->ScapElementState.UNSET
        "NOT_APPLICABLE","ABSENT"->ScapElementState.NOT_APPLICABLE
        else->if(e.isPresent)ScapElementState.PRESENT else ScapElementState.NOT_APPLICABLE
    }
    fun applicable(s:ScapInspectionSnapshot)=s.elements.filter{state(s,it.element)!=ScapElementState.NOT_APPLICABLE}
    fun name(s:ScapInspectionSnapshot,e:ScapElementEntity)=s.values(e.id)["customName"] ?: e.description
    fun code(s:ScapInspectionSnapshot,e:ScapElementEntity)=s.values(e.id)["exportCode"] ?: e.elementCode
    fun description(s:ScapInspectionSnapshot,e:ScapElementEntity)=s.values(e.id)["globalDescription"].orEmpty()
    fun factor(s:ScapInspectionSnapshot,e:ScapElementEntity):Double?=
        if(s.values(e.id).containsKey("importanceFactor"))ScapNumbers.decimal(s.values(e.id)["importanceFactor"]) else e.importanceFactor
    fun photoName(s:ScapInspectionSnapshot,id:String,category:String?)=s.values(id)["photoName"] ?: ScapPhotoCategories.label(category)
    fun photoDescription(s:ScapInspectionSnapshot,id:String,category:String?,description:String?):String {
        val name=photoName(s,id,category);return if(description.isNullOrBlank())name else "$name: $description"
    }
}
