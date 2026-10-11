package com.tuempresa.inventariovial.core

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.road.KmzGeometryStore
import com.tuempresa.inventariovial.road.KmzRoutePreview
import com.tuempresa.inventariovial.road.ProjectRouteCatalog
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.io.File

@Composable
fun CoreFieldTools(vm: InventoryViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSync by remember {mutableStateOf(false)}
    var showFiles by remember {mutableStateOf(false)}
    var showKmz by remember {mutableStateOf(false)}
    var files by remember { mutableStateOf<List<GeneratedFile>>(emptyList()) }
    var elements by remember {mutableStateOf<List<com.tuempresa.inventariovial.data.entity.InventoryRecordEntity>>(emptyList())}
    var extensions by remember {mutableStateOf<List<com.tuempresa.inventariovial.data.entity.SicExtensionEntity>>(emptyList())}
    var showLocation by remember { mutableStateOf(false) }
    var error by remember {mutableStateOf<String?>(null)}
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Text("Herramientas de campo", style=MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick={showSync=true}) {Text("Drive")}
                OutlinedButton(onClick={scope.launch {
                    withContext(Dispatchers.IO) {
                        val dao=com.tuempresa.inventariovial.data.database.InventoryDatabase.getInstance(context).inventoryDao()
                        elements=dao.observeAllRecords().first()
                        extensions=dao.observeSicExtensions().first()
                        files=GeneratedFileIndex.discover(context)
                    }
                    showFiles=true
                }}) { Text("Archivos") }
            }
            OutlinedButton(onClick={showKmz=true}) {Text("Importar ruta KMZ")}
        }
    }
    if(showSync) DriveSyncCenter(vm) {showSync=false}
    if(showFiles) AlertDialog(onDismissRequest={showFiles=false},title={Text("Archivos generados")},
        text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            if(files.isEmpty()) Text("Todavía no hay Excel, PDF o ZIP disponibles en el dispositivo.")
            GeneratedFileIndex.groupByElement(files.take(40),elements,extensions).forEach {(id,group)->
              val element=elements.find {it.id==id}
              Text(element?.let {"${com.tuempresa.inventariovial.sic.InventoryElements.title(it.sicCode)} · ${it.routeCode} · PR ${it.startPrCode}+${it.startDistanceM}"} ?: id,style=MaterialTheme.typography.titleMedium)
              group.forEach { file ->
                Text("${file.assetFamily} · ${File(file.localPath).name}")
                Text(if(file.syncStatus=="SYNCED") "☁ Sincronizado" else "✓ Guardado en tablet · ☁ Pendiente")
                Row {
                    TextButton(onClick={ runCatching {context.startActivity(GeneratedFileIndex.open(context,file))}.onFailure {error="No hay aplicación para abrir este archivo."} }) {Text("Abrir")}
                    TextButton(onClick={ runCatching {context.startActivity(GeneratedFileIndex.share(context,file))}.onFailure {error="No se pudo compartir el archivo."} }) {Text("Compartir")}
                }
              }
            }
            TextButton(onClick={showLocation=!showLocation}) { Text("Ver ubicación de archivos") }
            if(showLocation) Text("Ubicación: almacenamiento interno de Inventario Vial (carpetas de exportaciones SIC, SCAP y entregas de ruta). Android puede impedir abrir estas carpetas directamente desde DocumentsUI. Usa Abrir o Compartir para obtener el archivo.", style=MaterialTheme.typography.bodySmall)
            error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        }},confirmButton={TextButton(onClick={showFiles=false}){Text("Cerrar")}})
    if(showKmz) KmzImportDialog(onActivated=vm.field::reloadReference) {showKmz=false}
}

@Composable
private fun KmzImportDialog(onActivated:()->Unit,onClose: () -> Unit) {
    val context= LocalContext.current
    val scope=rememberCoroutineScope()
    val store=remember(context) { KmzGeometryStore(context) }
    val routes= remember(context) { ProjectRouteCatalog.load(context).routeCodes }
    var route by remember {mutableStateOf(routes.firstOrNull().orEmpty())}
    var selectionExpanded by remember {mutableStateOf(false)}
    var preview by remember {mutableStateOf<KmzRoutePreview?>(null)}
    var feedback by remember {mutableStateOf<String?>(null)}
    var processing by remember {mutableStateOf(false)}
    val chooser=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri!=null) scope.launch {
            processing=true
            feedback=try {
                val parsed=withContext(Dispatchers.IO) {
                    store.stage(uri,route,context.contentResolver.query(uri,null,null,null,null)?.use { cursor ->
                        val i=cursor.getColumnIndex("_display_name")
                        if(cursor.moveToFirst() && i>=0) cursor.getString(i) else "ruta.kmz"
                    } ?: "ruta.kmz")
                }
                preview=parsed
                "Geometría validada. Confirma expresamente para activarla."
            } catch (e:Exception) {preview=null;e.message ?: "No se pudo leer el KMZ"}
            processing=false
        }
    }
    AlertDialog(onDismissRequest=onClose, title={Text("Importar ruta KMZ")},
        text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Elige primero la ruta a actualizar.")
            Box {
                OutlinedButton(onClick={selectionExpanded=true}) {Text(route)}
                DropdownMenu(expanded=selectionExpanded,onDismissRequest={selectionExpanded=false}) {
                    routes.forEach { r -> DropdownMenuItem(text={Text(r)},onClick={route=r;preview=null;selectionExpanded=false}) }
                }
            }
            Button(enabled=!processing,onClick={chooser.launch(arrayOf("application/vnd.google-earth.kmz", "application/zip", "application/octet-stream", "*/*"))}) {Text("Seleccionar KMZ")}
            preview?.let { item ->
                Text("${item.sourceName} · ${item.segments.size} ejes · ${item.vertices} vértices")
                Text("Ruta: ${item.routeCode} · Hash: ${item.sha256.take(12)}…")
                Text("La geometría previa se conserva. Se calcularán progresivas geométricas desde el primer punto; revisa la compatibilidad con los PR oficiales.")
                Button(enabled=!processing,onClick={scope.launch {
                    processing=true
                    feedback=try {withContext(Dispatchers.IO) {store.activate(item)};onActivated();preview=null;"Nueva geometría activada para ${item.routeCode}."}
                        catch(e:Exception) {e.message ?: "Error al activar"}
                    processing=false
                }}) {Text("Usar esta geometría")}
            }
            val versions=remember(route,preview) {store.versions(route)}
            Text("Versiones conservadas: ${versions.size}")
            feedback?.let { Text(it) }
        }},confirmButton={TextButton(onClick=onClose){Text("Cerrar")}})
}
