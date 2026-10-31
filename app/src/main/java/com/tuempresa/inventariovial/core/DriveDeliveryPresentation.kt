package com.tuempresa.inventariovial.core

import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.localfiles.LocalExportJob
import com.tuempresa.inventariovial.localfiles.PublicLocalFolderNames
import org.json.JSONObject
import android.content.Context
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.domain.*

data class DriveDeliveryIdentity(val title:String,val location:String,val currentRevision:Long?,val validated:Boolean,val format:String="",
    val issue:String?=null,val target:ScapFieldRoute?=null,val provisional:Boolean=false,val onlyDevice:Boolean=false)
enum class DriveDeliveryStatus(val label:String,val symbol:String,val priority:Int) {
    ATTENTION("Requiere atención","!",0), VERIFYING("Verificando entrega","◌",1),
    PREPARING("Preparando entrega","…",2), UPLOADING("Subiendo","↑",2), PENDING("Pendiente de subir","…",3), COMPLETE("Subido correctamente","✓",4)
}
data class DriveDeliveryView(val status:DriveDeliveryStatus,val message:String,val files:String,val confirmed:Boolean,
    val review:Boolean=false,val retryAllowed:Boolean=true,val progressText:String?=null)

/** Read-only presentation. Never reserves folders, prepares exports or mutates an outbox. */
object DriveDeliveryPresentation {
    fun json(value:String?):JSONObject?=runCatching{value?.let(::JSONObject)}.getOrNull()
    suspend fun identity(db:InventoryDatabase,i:DriveSyncIntent,context:Context?=null):DriveDeliveryIdentity {
        var label=when(i.kind){"SCAP"->"Puente";"PAVEMENT"->"Pavimento";else->"Elemento SIC"}
        var location="Ubicación no disponible";var revision:Long?=null;var validated=false
        var issue:String?=null;var target:ScapFieldRoute?=null;var provisional=false
        val onlyDevice=com.tuempresa.inventariovial.localfiles.DeliveryDestination.deviceOnly(db,i.key)
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
                provisional=s.values("completion")["status"]=="INCOMPLETE"
                if(context!=null) {
                    val catalog=ScapCatalog.load(context).forSnapshot(s)
                    provisional=ScapCompletion.assess(s,catalog).pending.isNotEmpty()
                    val consent=i.warningsConfirmed || s.inspection.status=="COMPLETE" && s.inspection.updatedAt==i.revision && i.preparedJson==null &&
                        s.values.any{it.ownerId=="completion"&&it.key=="uploadedWithWarnings"&&it.value=="true"&&it.source=="USER_CONFIRMED"}
                    ScapUploadReadiness.block(s,catalog,consent)?.let{issue=it.message;target=it.route}
                }
            }
            "PAVEMENT"->db.pavementDao().sample(i.recordId)?.let{s->
                location=s.ruta.ifBlank{"Ruta pendiente"};revision=s.updatedAt;validated=s.status=="CLOSED"
                provisional=json(i.preparedJson)?.optJSONArray("pendingSic")?.length()?.let{it>0}==true
            }
        }
        // describe is a read-only formatter; reserve and LocalDocumentLocation are never called.
        val title=if(revision==null)label else PublicLocalFolderNames.describe(db,
            LocalExportJob("DISPLAY_ONLY",i.key,i.kind,i.recordId,i.revision),label)
        return DriveDeliveryIdentity(title,location,revision,validated,format,issue,target,provisional,onlyDevice)
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
        val error=i.error.orEmpty()
        val bridgeConflict=!complete&&DeliveryRejectionPolicy.bridgeIdentityConflict(i)
        val localNaming=error.contains("panel oficial SCAP requiere nombres",true)
        val authorization=error.contains("UNAUTHORIZED",true)||error.contains("FORBIDDEN",true)||error.contains("(HTTP_401)")||error.contains("(HTTP_403)")
        val incompatible=error.contains("compatibilidad",true)||error.contains("panel fotográfico SCAP",true)||
            error.contains("readablePhotoNamingVersion")||error.contains("backend",true)&&i.state=="UNCONFIGURED"
        val preparation=error.startsWith("No se pudieron preparar")
        val status=when {
            bridgeConflict || i.state=="CONFLICT" || i.state=="BLOCKED"&&!busy->DriveDeliveryStatus.ATTENTION
            !identity.validated || identity.currentRevision==null || i.state in setOf("DRAFT","PENDING_VALIDATION")->DriveDeliveryStatus.ATTENTION
            complete->DriveDeliveryStatus.COMPLETE
            identity.onlyDevice || identity.issue!=null || preparation&&!running->DriveDeliveryStatus.ATTENTION
            !configured || i.state=="UNCONFIGURED"->DriveDeliveryStatus.ATTENTION
            !current->DriveDeliveryStatus.PENDING
            running && progress?.revision==i.revision && progress.phase=="CONFIRMING"->DriveDeliveryStatus.VERIFYING
            running && progress?.revision==i.revision && progress.phase=="PREPARING"->DriveDeliveryStatus.PREPARING
            running->DriveDeliveryStatus.UPLOADING
            busy || retrying || i.error?.contains("Esperando para reintentar")==true->DriveDeliveryStatus.PENDING
            i.state=="ERROR"->DriveDeliveryStatus.ATTENTION
            i.state in setOf("UPLOADING","SYNCED")->DriveDeliveryStatus.VERIFYING
            else->DriveDeliveryStatus.PENDING
        }
        val message=when(status) {
            DriveDeliveryStatus.COMPLETE->if(identity.provisional)"Respaldo con datos pendientes confirmado por el servidor. No acredita finalización contractual."else"Revisión actual confirmada por el servidor."
            DriveDeliveryStatus.PREPARING->"Preparando archivos"
            DriveDeliveryStatus.UPLOADING->progress?.takeIf{it.revision==i.revision}?.let{SyncProgressPresentation.phase(it)} ?: "Envío en curso."
            DriveDeliveryStatus.VERIFYING->"Falta confirmar la entrega completa. La copia local se conserva."
            DriveDeliveryStatus.ATTENTION->when {
                bridgeConflict->DeliveryRejectionPolicy.BRIDGE_MESSAGE
                i.state=="CONFLICT"->"Revisa el conflicto de edición antes de reenviar."
                identity.currentRevision==null->"No se pudo consultar la ficha local. Conserva los datos y solicita soporte."
                identity.onlyDevice->"Destino Solo dispositivo. La ficha está guardada localmente; no se programó una nueva entrega Drive."
                localNaming->"Esta instalación no tiene habilitados los nombres fotográficos V2 requeridos por SCAP. Solicita verificar la versión y configuración legítima de la app; la ficha y sus fotos se conservan."
                authorization->"El servicio rechazó la autorización de esta entrega. Solicita revisar el acceso y la configuración con soporte; no borres la ficha ni las fotografías."
                incompatible->"La última consulta indicó incompatibilidad del servidor. Reintentar ahora volverá a consultar sus capacidades; la ficha y la preparación se conservan."
                identity.issue!=null->identity.issue
                !identity.validated->"La ficha puede guardarse, pero faltan datos o confirmar el cierre de esta revisión para entregarla."
                preparation->"No se pudieron preparar los archivos para esta entrega. La ficha y los medios se conservan; revisa el error y reintenta."
                i.state=="DRAFT"->"La ficha ya está cerrada, pero su entrega no está programada. Pulsa Reintentar ahora; no necesitas finalizarla otra vez."
                i.state=="PENDING_VALIDATION"->when {
                    error.contains("ordenamiento",true)||error.contains("PR y la distancia")->"Revisa la ubicación PR y metros para ordenar la entrega. La ficha ya está guardada."
                    error.contains("cambió",true)||error.contains("cambios locales",true)->"Hay una revisión o preparación anterior pendiente. Revisa los cambios guardados antes de reenviar."
                    error.startsWith("Validación SCAP:")->error
                    else->"La entrega requiere revisar su validación o preparación. Consulta el motivo en Detalles técnicos; el cierre local se conserva."
                }
                !configured || i.state=="UNCONFIGURED"->"Revisa la configuración de Drive."
                error.contains("(NOT_JPEG)")->"Una fotografía fue rechazada por su formato JPEG. Original conservado; revisa la evidencia y reintenta."
                i.state=="ERROR"->"El último intento falló y no hay una entrega confirmada. Reintenta o solicita asistencia; la ficha y sus archivos se conservan."
                else->"El servidor o la preparación bloquearon esta entrega. Consulta el motivo en Detalles técnicos. La ficha se conserva."
            }
            DriveDeliveryStatus.PENDING->when {
                !current->"Hay cambios locales pendientes de entregar."
                !connected->"Guardado en la tablet. Esperando conexión a Internet y ejecución de Android."
                busy || retrying->"Esperando para reintentar."
                else->"Esperando turno para enviar."
            }
        }
        // Transfer/preparation counts are not final confirmations. Never present them as X/Y ACK.
        val count=if(complete)json(i.receiptJson)?.optJSONArray("files")?.length()else null
        val files=if(count!=null)"Archivos confirmados: $count de $count"else"Confirmación de archivos pendiente."
        val review=identity.currentRevision!=null && !identity.onlyDevice && (bridgeConflict || identity.issue!=null || !identity.validated ||
            i.state=="PENDING_VALIDATION"&&!incompatible)
        return DriveDeliveryView(status,message,files,complete,review,
            !bridgeConflict && !complete && identity.validated && identity.issue==null && !identity.onlyDevice && i.state!="CONFLICT",
            progress?.takeIf{running && current && it.revision==i.revision && it.phase in setOf("PHOTOS","DOCUMENTS","CONFIRMING") && it.total>0}?.let{SyncProgressPresentation.files(it)})
    }
}
