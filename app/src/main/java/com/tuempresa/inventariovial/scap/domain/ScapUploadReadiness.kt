package com.tuempresa.inventariovial.scap.domain

import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot

data class ScapUploadBlock(val message:String,val route:ScapFieldRoute)

/** Same rules as the uploader; never converts an unknown value to a valid zero. */
object ScapUploadReadiness {
    fun block(s:ScapInspectionSnapshot,catalog:ScapCatalog,warningsConfirmed:Boolean):ScapUploadBlock? {
        val review=ScapValidation.review(s,catalog)
        val completion=ScapCompletion.assess(s,catalog)
        val invalid=completion.pending.firstOrNull{it.severity==ScapIssueSeverity.BLOCKING}
        if(review.errors.isNotEmpty() || invalid!=null)return ScapUploadBlock(
            "Validación SCAP: "+(invalid?.let{"${it.section} · ${it.field}"} ?: review.errors.first())+
                ". Puedes guardar y exportar una copia local; corrige el valor antes de entregar a Drive.",
            invalid?.route ?: ScapFieldRoute("REVIEW"))
        if(completion.pending.isNotEmpty()&&!warningsConfirmed)return ScapUploadBlock(
            "Validación SCAP: falta confirmar el cierre con datos pendientes. La copia local se conserva.",
            ScapFieldRoute("REVIEW"))
        return null
    }
}
