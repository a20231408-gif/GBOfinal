package com.tuempresa.inventariovial.pavement

/** Technical inputs are independent of the sketch and of the measured PCI quantity. */
object PavementDamageFields {
    // catalogo_danos.json: ancho_fisura_aplicable, including legacy codes.
    fun crackWidth(code:Int)=code in setOf(1,3,7,8,10,17,22,24,28,37)
    fun depth(code:Int)=code in setOf(13,15,85,86)
    fun depthLabel(code:Int)=when(code){15->"Profundidad media del ahuellamiento (mm)";86->"Profundidad de huellas (mm)";else->"Profundidad máxima (mm)"}
}

enum class SlabMark { POINT, LINE, AREA }

/** Reuses the nullable coordinates already in Room. SLABS remains the contractual quantity. */
object PavementSlabGeometry {
    fun mark(code:Int)=when(code){21,25,27,28,37,39->SlabMark.LINE;32,33->SlabMark.POINT;else->SlabMark.AREA}
    fun localized(d:PavementDamageEntity)=d.geometry=="SLABS" && listOf(d.x1,d.y1,d.x2,d.y2).any {it!=null}
    fun slabAt(s:PavementSampleEntity,x:Double,y:Double):Int {
        require(s.slabRows>0 && s.slabColumns>0)
        return (y/s.largoM*s.slabRows).toInt().coerceIn(0,s.slabRows-1)*s.slabColumns+
            (x/s.anchoM*s.slabColumns).toInt().coerceIn(0,s.slabColumns-1)+1
    }
    fun validate(s:PavementSampleEntity,d:PavementDamageEntity) {
        if(!localized(d))return // Historical records have no measured location.
        fun point(x:Double?,y:Double?) {
            require(x!=null && y!=null && x.isFinite() && y.isFinite() && x in 0.0..s.anchoM && y in 0.0..s.largoM){"Ubicación fuera de la muestra."}
        }
        point(d.x1,d.y1)
        if(mark(d.code)==SlabMark.POINT){require(d.x2==null && d.y2==null){"La ubicación puntual utiliza solo X1 e Y1."}}
        else {
            point(d.x2,d.y2)
            require(d.x1!=d.x2 || d.y1!=d.y2){"Marca dos puntos diferentes."}
            if(mark(d.code)==SlabMark.AREA)require(d.x1!=d.x2 && d.y1!=d.y2){"La zona parcial debe tener ancho y largo."}
        }
        // Every slab intersected by the bounding zone must have been selected.
        val a=slabAt(s,minOf(d.x1!!,d.x2?:d.x1),minOf(d.y1!!,d.y2?:d.y1))
        val b=slabAt(s,maxOf(d.x1,d.x2?:d.x1),maxOf(d.y1,d.y2?:d.y1))
        val selected=PavementRules.slabIds(d)
        for(row in (a-1)/s.slabColumns..(b-1)/s.slabColumns)
            for(col in (a-1)%s.slabColumns..(b-1)%s.slabColumns)
                require(row*s.slabColumns+col+1 in selected){"La geometría debe quedar en las losas indicadas."}
    }
}
