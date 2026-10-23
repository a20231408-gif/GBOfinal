package com.tuempresa.inventariovial.core

import java.text.Normalizer
import java.util.Locale

data class DriveDeliveryEntry(val intent:DriveSyncIntent,val identity:DriveDeliveryIdentity,val view:DriveDeliveryView,val progress:SyncProgress?=null) {
    val activityAt:Long get()=maxOf(identity.currentRevision ?: 0,intent.revision,intent.lastSyncedAt ?: 0)
}
enum class DriveDeliverySection(val label:String) { QUEUE("En la carga"), HISTORY("Mis entregas") }
enum class DriveDeliveryFilter(val label:String) {
    ALL("Todas"),PENDING("Pendientes"),COMPLETE("Completadas"),ATTENTION("Necesitan atención")
}

/** Both sections select from the same read-only rows and the existing strict ACK presentation. */
object DriveDeliveryListPresentation {
    const val EMPTY_QUEUE="No hay entregas pendientes. Puedes consultar tus archivos enviados en Mis entregas"
    private fun searchable(text:String)=Normalizer.normalize(text,Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"),"").lowercase(Locale.ROOT)

    fun select(rows:List<DriveDeliveryEntry>,section:DriveDeliverySection,query:String="",filter:DriveDeliveryFilter=DriveDeliveryFilter.ALL):List<DriveDeliveryEntry> {
        val selected=if(section==DriveDeliverySection.QUEUE)rows.filter{!it.view.confirmed}
            else {
                val terms=searchable(query).trim().split(Regex("\\s+")).filter{it.isNotBlank()}
                rows.filter{r->
                    val text=searchable("${r.identity.title} ${r.identity.location} ${r.identity.format}")
                    terms.all{text.contains(it)}&&when(filter) {
                        DriveDeliveryFilter.ALL->true
                        DriveDeliveryFilter.PENDING->!r.view.confirmed
                        DriveDeliveryFilter.COMPLETE->r.view.confirmed
                        DriveDeliveryFilter.ATTENTION->r.view.status==DriveDeliveryStatus.ATTENTION
                    }
                }
            }
        return if(section==DriveDeliverySection.QUEUE)selected.sortedWith(compareBy<DriveDeliveryEntry>{it.view.status.priority}.thenByDescending{it.activityAt}.thenBy{it.intent.key})
            else selected.sortedWith(compareByDescending<DriveDeliveryEntry>{it.activityAt}.thenBy{it.intent.key})
    }
}
