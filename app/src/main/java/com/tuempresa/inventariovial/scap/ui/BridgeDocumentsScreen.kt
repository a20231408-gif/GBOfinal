package com.tuempresa.inventariovial.scap.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.localfiles.*
import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot
import com.tuempresa.inventariovial.scap.export.ScapDocumentNames

/** Projection only: entering or changing filters never requests export or sync. */
object BridgeDocuments {
    fun category(f:LocalSharedFile):String=when {
        f.mimeType.startsWith("image/") && (f.relativePath+f.name).contains(Regex("croquis|sketch",RegexOption.IGNORE_CASE))->"Croquis"
        f.mimeType.startsWith("image/")->"Fotografías"
        f.mimeType=="application/pdf"->"PDF"
        f.mimeType=="application/zip"->"ZIP"
        f.name.endsWith(".xlsx",true)->"Excel"
        else->"Otros archivos"
    }
    fun label(f:LocalSharedFile,provisional:Boolean=false)=if(provisional || (f.relativePath+f.name).contains(Regex("provisional|borrador",RegexOption.IGNORE_CASE)))
        "PROVISIONAL · copia local" else "Verificado localmente · no acredita ACK remoto"
    fun visible(jobs:List<LocalExportJob>,files:List<LocalSharedFile>,history:Boolean,principals:List<LocalSharedFile> = emptyList())=
        (principals+LocalCopyPresentation.currentFiles(jobs,files,history).filter{if(history) it.mimeType!="application/json"&&!RecordDocuments.provisional(it) else it.mimeType!="application/json"&&RecordDocuments.sicFormat(it)==null}).distinctBy{it.uri}
}

@Composable internal fun BridgeDocumentsScreen(s:ScapInspectionSnapshot,c:ScapController,onBack:()->Unit) {
    val key="SCAP/${s.inspection.id}";val db=c.localDatabase
    val jobs by remember(key,db){db.localFilesDao().observeJobsFor(key)}.collectAsState(emptyList())
    val files by remember(key,db){db.localFilesDao().observeFilesFor(key)}.collectAsState(emptyList())
    val principals=rememberRecordPrincipals("SCAP",s.inspection.id,s.inspection.updatedAt)
    BridgeDocumentsContent(ScapDocumentNames.bridge(s),s.roadRecord?.routeCode.orEmpty(),
        s.values()["progressive"].orEmpty(),s.inspection.updatedAt,jobs,files,
        currentProvisional=s.inspection.status!="COMPLETE" || s.values("completion")["status"]=="INCOMPLETE",onBack=onBack,principals=principals)
}

@Composable internal fun BridgeDocumentsContent(name:String,route:String,progressive:String,revision:Long,
    jobs:List<LocalExportJob>,files:List<LocalSharedFile>,currentProvisional:Boolean=false,principals:List<LocalSharedFile> = emptyList(),onBack:()->Unit) {
    var history by rememberSaveable(name){mutableStateOf(false)}
    val current=BridgeDocuments.visible(jobs,files,false,principals)
    val older=BridgeDocuments.visible(jobs,files,true,principals).any{f->current.none{it.id==f.id}}
    val shown=BridgeDocuments.visible(jobs,files,history,principals)
    BackHandler(onBack=onBack)
    LazyColumn(Modifier.fillMaxSize().testTag("bridge-documents").padding(horizontal=16.dp),
        contentPadding=PaddingValues(vertical=12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        item {
            TextButton(onClick=onBack){Text("Volver a la ficha")}
            Text("Documentos del puente",style=MaterialTheme.typography.headlineSmall)
            Text(name.ifBlank{"Puente sin nombre"},style=MaterialTheme.typography.titleLarge)
            Text("${route.ifBlank{"Carretera pendiente"}} · ${progressive.ifBlank{"Progresiva pendiente"}}")
            Text("Revisión de la ficha: $revision")
            Text("Acceso interno a archivos existentes. Abrir o compartir usa el URI exacto; una copia local no confirma una entrega Drive.")
            LocalCopyPresentation.latest(jobs).forEach{job->Text(LocalCopyPresentation.result(job))}
            if(current.isEmpty())Text("Todavía no hay copia local verificada. Conserva la ficha; aquí aparecerán los archivos cuando termine el trabajo local.")
            else Text("Última copia válida disponible; comprueba la revisión de cada documento.")
            if(older)TextButton(onClick={history=!history}){Text(if(history)"Ocultar revisiones anteriores"else"Ver revisiones anteriores")}
            else Text("No hay versiones anteriores disponibles.")
        }
        for(category in listOf("Excel","PDF","ZIP","Fotografías","Croquis","Otros archivos")) {
            val group=shown.filter{BridgeDocuments.category(it)==category}
            if(group.isNotEmpty()) {
                item{Text(category,style=MaterialTheme.typography.titleMedium)}
                items(group,key={it.id}){file->
                    val job=jobs.find{it.id==file.jobId}
                    Text("Revisión ${job?.revision ?: "no identificada"}${if(job!=null&&job.revision!=revision)" · anterior a la ficha"else""}")
                    Text(BridgeDocuments.label(file,currentProvisional && job?.revision==revision))
                    Text("Ubicación: ${file.relativePath}",style=MaterialTheme.typography.bodySmall)
                    Text("Android puede abrir solo Descargas o el árbol autorizado, sin seleccionar esta carpeta. Usa ABRIR ARCHIVO para el documento exacto.",style=MaterialTheme.typography.bodySmall)
                    SavedFileActions(file,ownerKey=file.recordKey,history=history)
                }
            }
        }
    }
}
