package com.tuempresa.inventariovial.pavement

/** Translate without changing measured quantities; constrain the geometry to the sample. */
object PavementMovement {
    fun translate(s: PavementSampleEntity, d: PavementDamageEntity, dx: Double, dy: Double): PavementDamageEntity {
        require(dx.isFinite() && dy.isFinite())
        require(d.geometry in listOf("AREA", "LINEAR", "POTHOLE"))
        val xs=listOfNotNull(d.x1, if(d.geometry=="POTHOLE") null else d.x2)
        val ys=listOfNotNull(d.y1, if(d.geometry=="POTHOLE") null else d.y2)
        require(xs.isNotEmpty() && ys.isNotEmpty())
        val x=dx.coerceIn(-xs.min(), s.anchoM-xs.max())
        val y=dy.coerceIn(-ys.min(), s.largoM-ys.max())
        return d.copy(x1=d.x1?.plus(x), y1=d.y1?.plus(y), x2=d.x2?.plus(x), y2=d.y2?.plus(y))
    }
}
