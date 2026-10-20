package com.tuempresa.inventariovial.scap.domain

import com.tuempresa.inventariovial.scap.data.*

/** Presentation metadata in existing scap_values; never changes/deletes an attachment. */
object ScapSketchSelection {
    fun key(type:String)="principalSketch.$type"
    fun principal(s:ScapInspectionSnapshot,type:String):ScapSketchEntity? {
        val rows=s.sketches.filter{it.type==type}
        val chosen=s.values("media")[key(type)]
        return if(chosen!=null)rows.singleOrNull{it.id==chosen} else rows.singleOrNull()
    }
    fun warnings(s:ScapInspectionSnapshot)=ScapRepository.SKETCH_TYPES.mapNotNull{type->
        if(s.sketches.any{it.type==type}&&principal(s,type)==null)"Croquis principal pendiente de selección · $type; todos se conservan como anexos." else null
    }
}
