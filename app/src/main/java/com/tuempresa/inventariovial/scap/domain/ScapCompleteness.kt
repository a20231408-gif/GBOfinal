package com.tuempresa.inventariovial.scap.domain

import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot

data class ScapPendingField(val section:String,val field:String,val ownerId:String="inspection",val key:String="",
    val severity:ScapIssueSeverity=ScapIssueSeverity.WARNING) {
    val id get()="$section/$ownerId/$key"
    val label get()=field
    val code get()=if(severity==ScapIssueSeverity.BLOCKING)"INVALID_VALUE" else "MISSING_VALUE"
    val route get()=ScapFieldRoute(section.take(1),section,ownerId,key)
}
data class ScapCompleteness(val complete:List<ScapPendingField>,val pending:List<ScapPendingField>)

/** Stable navigation destinations refer to capture section and row, never a dropdown index. */
object ScapCompletion {
    fun assess(snapshot:ScapInspectionSnapshot,catalog:ScapCatalog):ScapCompleteness = StructuredScapCompletion.assess(snapshot,catalog)
}
