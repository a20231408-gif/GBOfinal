package com.tuempresa.inventariovial.scap.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.localfiles.*
import kotlinx.coroutines.*

/** Room jobs/files, not a transient dialog or a private generated filename, are the result. */
@Composable internal fun ScapLocalExportStatus(id:String,revision:Long,controller:ScapController,onStatus:(Boolean,String)->Unit) {
    val context=LocalContext.current;val db=controller.localDatabase;val scope=rememberCoroutineScope()
    val jobs by remember(id,db){db.localFilesDao().observeJobsFor("SCAP/$id")}.collectAsState(emptyList())
    val files by remember(id,db){db.localFilesDao().observeFilesFor("SCAP/$id")}.collectAsState(emptyList())
    val job=jobs.firstOrNull{it.scope.startsWith("ZIP")}
    var verified by remember(id){mutableStateOf<LocalSharedFile?>(null)}
    var result by remember(id){mutableStateOf<String?>(null)}
    var detail by remember(id){mutableStateOf<String?>(null)}
    var checking by remember(id){mutableStateOf(false)}
    val archiveProgress by LocalArchiveProgress.flow.collectAsState()
    val inProgress=job?.state in setOf("QUEUED","RUNNING") || checking
    val jobFiles=files.filter{it.jobId==job?.id}
    LaunchedEffect(job,jobFiles) {
        verified=null;detail=null
        if(job==null){result=null;return@LaunchedEffect}
        result=when(job.state){"QUEUED","RUNNING"->"Preparando ZIP de respaldo…";"READY"->"Verificando ZIP de respaldo…";else->"No se pudo guardar ZIP de respaldo"}
        if(job.state=="READY" || job.state=="PARTIAL" && files.any{it.jobId==job.id&&it.state=="READY"&&it.mimeType=="application/zip"}) {
            checking=true
            try {
                verified=withContext(Dispatchers.IO){
                    val f=files.single{it.jobId==job.id&&it.mimeType=="application/zip"&&it.state=="READY"}
                    val uri=Uri.parse(requireNotNull(f.uri));require(uri.scheme=="content")
                    val store=AndroidSharedDocumentStore(context);store.checkSlot(SharedDocumentStore.Slot(uri,true),f)
                    require(store.digest(uri)==(f.size to f.sha256)){"La copia pública cambió o quedó incompleta. El original sigue en GBO."}
                    f
                }
                result=if(job.state=="READY")"ZIP de respaldo guardado correctamente"else"ZIP de respaldo guardado parcialmente"
                if(job.state=="PARTIAL")detail=LocalCopyDrain.publicError(Exception(job.message))
            }catch(e:CancellationException){throw e}catch(e:Exception){result="No se pudo verificar ZIP de respaldo";detail=LocalCopyDrain.publicError(e)}
            finally{checking=false}
        }else if(job.state !in setOf("QUEUED","RUNNING"))detail=LocalCopyDrain.publicError(Exception(job.message))
    }
    LaunchedEffect(inProgress,result){result?.let{onStatus(inProgress,it)}}
    if(job!=null)Card(Modifier.fillMaxWidth().testTag("scap-local-result")) {
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Text(result ?: "Preparando ZIP de respaldo…")
            archiveProgress[job.id]?.takeIf{inProgress}?.let{Text(LocalArchiveProgress.label(it))}
            if(inProgress){LinearProgressIndicator(Modifier.fillMaxWidth());Text(if(job.state=="QUEUED")"Solicitud guardada en la tablet; espera la ejecución de Android. Este resultado queda disponible."
                else "Generando y verificando la copia local. Puedes volver a consultar este resultado.")}
            if(job.revision!=revision)Text("Corresponde a una revisión anterior; vuelve a exportar para incluir los cambios actuales.")
            verified?.let{file->
                Text(if(Build.VERSION.SDK_INT<29)"Carpeta autorizada / "+file.relativePath.removePrefix("Download/")+file.name
                    else file.relativePath.replace("Download/","Descargas/")+file.name)
                Text("El paquete conserva originales y pendientes. Una copia provisional no es una entrega contractual completa.",style=MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    TextButton(modifier=Modifier.testTag("scap-open-file"),onClick={scope.launch {
                        try {
                            val uri=Uri.parse(file.uri)
                            withContext(Dispatchers.IO){require(AndroidSharedDocumentStore(context).digest(uri)==(file.size to file.sha256)){"El archivo fue movido o perdió acceso. El original preparado se conserva."}}
                            context.startActivity(SharedFileActions.view(uri,file.mimeType).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }catch(e:CancellationException){throw e}catch(e:android.content.ActivityNotFoundException){detail="No hay una aplicación compatible para abrir ZIP. El archivo guardado se conserva."}
                        catch(e:Exception){detail=LocalCopyDrain.publicError(e)}
                    }}){Text("ABRIR ZIP DE RESPALDO")}

                }
            }
            detail?.let{Text(it)}
            if(!inProgress && verified==null)TextButton(onClick={scope.launch {
                try{if(job.revision==revision)LocalCopies.retry(context,db,job.id)else controller.localScapPackage(id).await()}
                catch(e:CancellationException){throw e}catch(e:Exception){detail=LocalCopyDrain.publicError(e)}
            }}){Text("REINTENTAR COPIA LOCAL")}
        }
    }
}
