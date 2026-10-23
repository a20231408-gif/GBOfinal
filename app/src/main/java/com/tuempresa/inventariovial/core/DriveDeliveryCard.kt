package com.tuempresa.inventariovial.core

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date

@Composable private fun DeliveryDetail(title:String,content:@Composable ()->Unit) {
    var expanded by rememberSaveable(title){mutableStateOf(false)}
    TextButton(onClick={expanded=!expanded},modifier=Modifier.fillMaxWidth()) {Text("${if(expanded)"−"else"+"} $title")}
    if(expanded)Column(Modifier.fillMaxWidth().padding(horizontal=8.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){content()}
}

/** No writes or background effects: callbacks remain the existing manual retry action. */
@Composable fun DriveDeliveryCard(i:DriveSyncIntent,identity:DriveDeliveryIdentity,view:DriveDeliveryView,progress:SyncProgress?=null,onRetry:()->Unit) {
    key(i.key) {
        Card(Modifier.fillMaxWidth().testTag("delivery-${i.key}")) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(identity.title,style=MaterialTheme.typography.titleLarge)
                Text(identity.format.ifBlank{when(i.kind){"SCAP"->"Ficha SCAP";"PAVEMENT"->"Pavimentos";else->"Ficha SIC"}},style=MaterialTheme.typography.labelLarge)
                Text(identity.location,style=MaterialTheme.typography.bodyMedium)
                Text("${view.status.symbol} ${view.status.label}",style=MaterialTheme.typography.titleMedium,
                    color=if(view.status==DriveDeliveryStatus.ATTENTION)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                Text(view.message)
                Text(view.files,style=MaterialTheme.typography.bodyMedium)
                DeliveryDetail("Destino confirmado") {
                    val ack=DriveDeliveryPresentation.json(i.receiptJson)
                    if(view.confirmed&&ack!=null)SelectionContainer{Column {
                        Text(ack.optString("logicalPath"))
                        Text("https://drive.google.com/drive/folders/${ack.optString("folderId")}")
                    }} else Text("Esta revisión aún no tiene un destino confirmado verificable.")
                }
                DeliveryDetail("Archivos") {
                    val prepared=DriveDeliveryPresentation.json(i.preparedJson)?.takeIf{it.optLong("revision")==identity.currentRevision}
                    val files=prepared?.optJSONArray("files")
                    if(files==null || files.length()==0)Text("Lista de archivos aún no disponible.")
                    else for(n in 0 until files.length())files.optJSONObject(n)?.let{f->
                        Text("${f.optString("path").substringAfterLast('/').ifBlank{"Archivo"}} · ${if(view.confirmed)"Confirmado"else"Sin confirmación final"}")
                    }
                    if(i.kind=="SCAP")Text("Los videos tienen una entrega y confirmación separadas.",style=MaterialTheme.typography.bodySmall)
                }
                DeliveryDetail("Detalles técnicos") {
                    SelectionContainer {Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        Text("Identidad: ${i.key}")
                        Text("Estado interno: ${i.state} · origen: ${i.origin} · intentos: ${i.attempts}")
                        Text("Revisión local: ${identity.currentRevision ?: "no disponible"} · en cola: ${i.revision} · confirmada: ${i.syncedRevision}")
                        Text("Última confirmación: ${i.lastSyncedAt?.let{DateFormat.getDateTimeInstance().format(Date(it))} ?: "Aún no confirmada"}")
                        progress?.takeIf{it.revision==i.revision}?.let{p->
                            Text(SyncProgressPresentation.phase(p));Text(SyncProgressPresentation.files(p))
                            Text(SyncProgressPresentation.elapsed(p,System.currentTimeMillis()))
                        }
                        DriveDeliveryPresentation.json(i.preparedJson)?.let{p->
                            Text("Hash de contenido preparado: ${p.optString("contentHash")}")
                            Text("Archivos preparados: ${p.optJSONArray("files")?.length() ?: 0}")
                            if(p.optJSONObject("georeferencing")?.optString("externalSubmetricStatus")=="PENDING")Text("Georreferenciación submétrica: pendiente")
                            p.optJSONArray("pendingSic")?.let{a->for(n in 0 until a.length())Text("SIC pendiente: ${a.optString(n)}")}
                        }
                        DriveDeliveryPresentation.json(i.receiptJson)?.let{ack->
                            Text("Destino del último ACK: ${ack.optString("logicalPath")}")
                            Text("Carpeta del último ACK: ${ack.optString("folderId")}")
                            Text("Hash del último ACK: ${ack.optString("contentHash")}")
                            ack.optJSONArray("retainedSicFormats")?.let{a->for(n in 0 until a.length())Text("SIC conserva revisión anterior: ${a.optString(n)}")}
                        }
                        i.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
                    }}
                }
                if(!view.confirmed&&identity.validated&&i.state!="CONFLICT")TextButton(onClick=onRetry){Text("Reintentar ahora")}
            }
        }
    }
}
