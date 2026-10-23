package com.tuempresa.inventariovial.core

import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.localfiles.LocalExportJob
import com.tuempresa.inventariovial.localfiles.PublicLocalFolderNames
import org.json.JSONObject

data class DriveDeliveryIdentity(val title:String,val location:String,val currentRevision:Long?,val validated:Boolean,val format:String="")
enum class DriveDeliveryStatus(val label:String,val symbol:String,val priority:Int) {
    ATTENTION("Necesita atención","!",0), VERIFYING("Verificando entrega","◌",1),
    UPLOADING("Subiendo","↑",2), PENDING("Pendiente de subir","…",3), COMPLETE("Subido correctamente","✓",4)
}
data class DriveDeliveryView(val status:DriveDeliveryStatus,val message:String,val files:String,val confirmed:Boolean)

/** Read-only presentation. Never reserves folders, prepares exports or mutates an outbox. */
object DriveDeliveryPresentation {
    fun json(value:String?):JSONObject?=runCatching{value?.let(::JSONObject)}.getOrNull()
    suspend fun identity(db:InventoryDatabase,i:DriveSyncIntent):DriveDeliveryIdentity {
        var label=when(i.kind){"SCAP"->"Puente";"PAVEMENT"->"Pavimento";else->"Elemento SIC"}
        var location="Ubicación no disponible";var revision:Long?=null;var validated=false
        var format=when(i.kind){"SCAP"->"Ficha SCAP";"PAVEMENT"->"Pavimentos";else->"Ficha SIC"}
        when(i.kind) {
            "SIC"->db.inventoryDao().snapshot(i.recordId)?.let{s->
                val r=s.record
                val family=com.tuempresa.inventariovial.road.AssetPresentationCatalog.resolve(
                    com.tuempresa.inventariovial.data.entity.InventoryRecordWithPhotos(r,s.photos,s.sic23,s.sic18,s.sic20,s.sic19,s.sic18a,s.sicExtension,s.sic21))
                label=DriveInventorySchema.elementLabel(family);location=r.routeCode.ifBlank{"Ruta pendiente"}
                revision=r.updatedAt;validated=r.status=="ACTIVE";format=r.sicCode
            }
            "SCAP"->db.scapDao().snapshot(i.recordId)?.let{s->
                location=(s.roadRecord?.routeCode ?: s.values()["route"].orEmpty()).ifBlank{"Ruta pendiente"}
                revision=s.inspection.updatedAt;validated=s.inspection.status=="COMPLETE"
            }
            "PAVEMENT"->db.pavementDao().sample(i.recordId)?.let{s->
                location=s.ruta.ifBlank{"Ruta pendiente"};revision=s.updatedAt;validated=s.status=="CLOSED"
            }
        }
        // describe is a read-only formatter; reserve and LocalDocumentLocation are never called.
        val title=if(revision==null)label else PublicLocalFolderNames.describe(db,
            LocalExportJob("DISPLAY_ONLY",i.key,i.kind,i.recordId,i.revision),label)
        return DriveDeliveryIdentity(title,location,revision,validated,format)
    }
    fun confirmed(i:DriveSyncIntent,currentRevision:Long?):Boolean=runCatching {
        currentRevision==i.revision && SyncIntentPresentation.confirmed(i) &&
            json(i.preparedJson)?.let{p->p.optString("key")==i.key && p.optLong("revision")==i.revision &&
                json(i.receiptJson)?.let{AutomaticDelivery.validAck(p,it)}==true}==true
    }.getOrDefault(false)

    fun view(i:DriveSyncIntent,identity:DriveDeliveryIdentity,running:Boolean,connected:Boolean,configured:Boolean,
             retrying:Boolean=false,progress:SyncProgress?=null):DriveDeliveryView {
        val current=identity.currentRevision==i.revision
        val complete=identity.validated&&confirmed(i,identity.currentRevision)
        val busy=i.error?.contains("(BUSY)")==true
        val status=when {
            i.state=="CONFLICT" || i.state=="BLOCKED"&&!busy->DriveDeliveryStatus.ATTENTION
            !identity.validated || identity.currentRevision==null || i.state in setOf("DRAFT","PENDING_VALIDATION")->DriveDeliveryStatus.ATTENTION
            complete->DriveDeliveryStatus.COMPLETE
            !configured || i.state=="UNCONFIGURED"->DriveDeliveryStatus.ATTENTION
            !current->DriveDeliveryStatus.PENDING
            running && progress?.revision==i.revision && progress.phase=="CONFIRMING"->DriveDeliveryStatus.VERIFYING
            running->DriveDeliveryStatus.UPLOADING
            busy || retrying || i.error?.contains("Esperando para reintentar")==true->DriveDeliveryStatus.PENDING
            i.state in setOf("UPLOADING","SYNCED","ERROR")->DriveDeliveryStatus.VERIFYING
            else->DriveDeliveryStatus.PENDING
        }
        val message=when(status) {
            DriveDeliveryStatus.COMPLETE->"Revisión actual confirmada por el servidor."
            DriveDeliveryStatus.UPLOADING->if(progress?.revision==i.revision&&progress.phase=="PHOTOS")"Subiendo fotografías"else"Envío en curso."
            DriveDeliveryStatus.VERIFYING->"Falta confirmar la entrega completa. La copia local se conserva."
            DriveDeliveryStatus.ATTENTION->when {
                i.state=="CONFLICT"->"Revisa el conflicto de edición antes de reenviar."
                !identity.validated || i.state in setOf("DRAFT","PENDING_VALIDATION")->"Revisa y finaliza la ficha."
                !configured || i.state=="UNCONFIGURED"->"Revisa la configuración de Drive."
                else->"Revisa el motivo en Detalles técnicos. La ficha se conserva."
            }
            DriveDeliveryStatus.PENDING->when {
                !current->"Hay cambios locales pendientes de entregar."
                !connected->"Esperando conexión a Internet."
                busy || retrying->"Esperando para reintentar."
                else->"Esperando turno para enviar."
            }
        }
        // Transfer/preparation counts are not final confirmations. Never present them as X/Y ACK.
        val count=if(complete)json(i.receiptJson)?.optJSONArray("files")?.length()else null
        val files=if(count!=null)"Archivos confirmados: $count de $count"else"Confirmación de archivos pendiente."
        return DriveDeliveryView(status,message,files,complete)
    }
}
