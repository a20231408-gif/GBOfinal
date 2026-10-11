package com.tuempresa.inventariovial.validation

/** Text rules apply only to contractual PR codes and offsets, never to dimensions or chainage. */
object ContractualLocationInput {
    fun prError(value:String):String? = if(value.matches(Regex("[0-9]{1,4}"))) null else "PR: ingresa de 1 a 4 dígitos."
    fun distanceError(value:String):String? = if(value.matches(Regex("[0-9]{1,3}([.,][0-9]+)?")) &&
        value.replace(',', '.').toDoubleOrNull()?.isFinite()==true) null else "Distancia: máximo 3 cifras enteras; conserva los decimales."
    fun acceptPr(value:String) = value.length<=4 && value.all {it in '0'..'9'}
    fun acceptDistance(value:String) = value.matches(Regex("[0-9]{0,3}([.,][0-9]*)?"))
    fun optionalCoordinateError(lat:String,lon:String):String? {
        if(lat.isBlank() && lon.isBlank()) return null
        val a=lat.replace(',', '.').toDoubleOrNull();val o=lon.replace(',', '.').toDoubleOrNull()
        return if(a!=null && o!=null && a.isFinite() && o.isFinite() && a in -90.0..90.0 && o in -180.0..180.0) null else "Completa coordenadas válidas o deja ambos campos vacíos."
    }
}
