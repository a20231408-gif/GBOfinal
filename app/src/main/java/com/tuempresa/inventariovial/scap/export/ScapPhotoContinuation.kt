package com.tuempresa.inventariovial.scap.export

import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot

/** Documentary companion only; the bounded contractual workbook and remote protocol are unchanged. */
object ScapPhotoContinuation {
    private fun escape(s:String)=s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;")
    fun html(s:ScapInspectionSnapshot):String = "<!doctype html><meta charset=\"utf-8\"><title>Continuación fotográfica SCAP</title>"+
        "<h1>Evidencias de ${escape(s.inspection.bridgeName)}</h1><p>F.2 conserva las primeras 32 fotografías. " +
        "Las adicionales se entregan por separado, sin ampliar la plantilla contractual. Este índice y todos los originales acompañan el paquete local. " +
        "Los videos no se cuentan como fotografías.</p><ol>"+
        com.tuempresa.inventariovial.scap.domain.ScapPhotoPanel.ordered(s).mapIndexed {n,p->
            "<li>${if(n<32)"F.2"else"Continuación"} · ${escape(com.tuempresa.inventariovial.scap.domain.ScapElementView.photoName(s,p.id,p.photoCategory))} · ${escape(p.description.orEmpty())}</li>"
        }.joinToString("")+"</ol>"
}
