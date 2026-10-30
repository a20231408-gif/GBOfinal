package com.tuempresa.inventariovial.pavement

import java.util.Locale

object PavementCoordinateDisplay {
    fun text(value:Double?,locale:Locale=Locale.getDefault())=value?.let{String.format(locale,"%.2f",it)}.orEmpty()
    fun value(text:String,original:Double?,edited:Boolean):Double =
        if(!edited && original!=null)original else text.replace(',','.').toDouble().also{require(it.isFinite()){"Número no válido."}}
}
