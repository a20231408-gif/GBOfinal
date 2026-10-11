package com.tuempresa.inventariovial.pavement.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.tuempresa.inventariovial.auth.AuthGraph
import com.tuempresa.inventariovial.camera.createPhotoFile
import com.tuempresa.inventariovial.camera.loadCorrectlyOrientedBitmap
import com.tuempresa.inventariovial.pavement.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.*

@Composable
fun PavementWorkspace(onBack: () -> Unit) {
    val context = LocalContext.current
    val graph = remember { AuthGraph.get(context) }
    val repo = remember { PavementRepository(graph.database, graph.sessions) }
    val session by graph.sessions.current.collectAsState()
    val samples by remember { repo.samples() }.collectAsState(emptyList())
    var id by rememberSaveable { mutableStateOf<String?>(null) }
    var surface by rememberSaveable { mutableStateOf(PavementSurface.ASPHALT) }
    var filter by rememberSaveable { mutableStateOf("") }
    var inspectorFilter by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    BackHandler { if (id != null) id = null else onBack() }
    if (id != null) {
        val sample by remember(id) { repo.observeSample(id!!) }.collectAsState(null)
        sample?.let { s -> key(s.id) { PavementEditor(s, repo, session?.id, onBack = { id = null }) } }
    } else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(color=MaterialTheme.colorScheme.primary,contentColor=MaterialTheme.colorScheme.onPrimary,shape=androidx.compose.foundation.shape.RoundedCornerShape(20.dp)) {
            Row(Modifier.fillMaxWidth().padding(20.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text("GBO · INGENIERÍA EN CAMPO",style=MaterialTheme.typography.labelLarge)
                    Text("Fallas en pavimentos",style=MaterialTheme.typography.headlineMedium)
                    Text("PCI / URCI · esquema X/Y · registro fotográfico",style=MaterialTheme.typography.bodyLarge)
                }
                Image(androidx.compose.ui.res.painterResource(com.tuempresa.inventariovial.R.mipmap.ic_launcher_foreground),"GBO Ingenieros Consultores",modifier=Modifier.size(92.dp))
            }
        }
        TextButton(onClick = onBack) { Text("← Inicio") }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()) {
            PavementSurface.entries.forEach { item ->
                FilterChip(selected=surface==item,onClick={surface=item},label={Text(item.label)})
            }
        }
        Field("Filtrar tramo / ruta", filter) { filter = it }
        Choice("Inspector",inspectorFilter,listOf("")+samples.map{it.inspectorUserId}.distinct(),{ id -> if(id.isBlank())"Todos" else samples.firstOrNull{it.inspectorUserId==id}?.inspectorNombre ?: id }){inspectorFilter=it}
        Button(onClick = { scope.launch { runCatching { repo.newSample(surface) }.onSuccess { id = it }.onFailure { error = it.message } } }, modifier = Modifier.heightIn(min = 56.dp)) { Text("Nueva muestra") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("MUESTRAS", style=MaterialTheme.typography.titleMedium)
        PavementHistory.visible(samples, surface, filter, inspectorFilter).forEach { s ->
            PavementSampleCard(s,session?.id) { id=it }
        }
    }
}

@Composable internal fun Field(label: String, value: String, onValue: (String) -> Unit) {
    OutlinedTextField(value, onValue, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth())
}
@Composable internal fun <T> Choice(label: String, value: T, values: List<T>, name: (T) -> String, onValue: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("$label: ${name(value)}") }
        DropdownMenu(expanded, { expanded = false }) { values.forEach { v -> DropdownMenuItem(text = { Text(name(v)) }, onClick = { onValue(v); expanded = false }) } }
    }
}

@Composable
private fun PavementEditor(sample: PavementSampleEntity, repo: PavementRepository, userId: String?, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val surface = PavementSurface.valueOf(sample.surface)
    val editable = sample.status != "CLOSED" && sample.inspectorUserId == userId
    val damages by remember(sample.id) { repo.damages(sample.id) }.collectAsState(emptyList())
    var header by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf<PavementDamageEntity?>(null) }
    var selected by remember { mutableStateOf<String?>(null) }
    var movingId by remember { mutableStateOf<String?>(null) }
    var definition by remember { mutableStateOf(PavementCatalog.forSurface(surface).first()) }
    var showMeasurement by remember { mutableStateOf(false) }
    var severity by remember { mutableStateOf(definition.severities.firstOrNull()) }
    var first by remember { mutableStateOf<Pair<Double,Double>?>(null) }
    var slabSet by remember { mutableStateOf(emptySet<Int>()) }
    var message by remember { mutableStateOf("") }
    var closeConfirm by remember { mutableStateOf(false) }
    var photoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var photoDamage by rememberSaveable { mutableStateOf<String?>(null) }
    var photoRevision by remember { mutableIntStateOf(0) }
    val camera = rememberLauncherForActivityResult(com.tuempresa.inventariovial.core.FieldTakePicture()) { ok ->
        val path = photoPath; val damageId = photoDamage
        photoPath=null; photoDamage=null
        if(!ok && path!=null) scope.launch { withContext(Dispatchers.IO) { File(path).delete() } }
        if (ok && path != null && damageId != null) scope.launch {
            runCatching {
                repo.savePhoto(PavementDamagePhotoEntity(UUID.randomUUID().toString(), damageId, path), sample.id)
            }.onSuccess { photoRevision++; message = "✓ Guardado en tablet" }.onFailure { withContext(Dispatchers.IO) { File(path).delete() }; message = it.message.orEmpty() }
        }
    }
    var importing by remember { mutableStateOf(false) }
    var pickerDamage by rememberSaveable { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val damageId=pickerDamage; pickerDamage=null
        if(uri!=null && damageId!=null) scope.launch {
            importing=true
            var file:File?=null
            try {
                val imported=withContext(Dispatchers.IO) { PavementPhotos.importImage(context,uri) }
                file=imported
                repo.savePhoto(PavementDamagePhotoEntity(UUID.randomUUID().toString(),damageId,imported.absolutePath),sample.id)
                photoRevision++; message="Imagen guardada en el daño."
            } catch(e:Exception) { withContext(Dispatchers.IO) { file?.delete() }; message=e.message.orEmpty() }
            finally { importing=false }
        }
    }
    fun launchCamera() { runCatching { val photo = createPhotoFile(context, "PAVIMENTO"); photoPath = photo.file.absolutePath; photoDamage = selected; camera.launch(photo.uri) }.onFailure { message = it.message.orEmpty() } }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) launchCamera() else message = "Permite acceso a la cámara para tomar fotos." }
    fun newDamage(x: Double? = null, y: Double? = null, x2: Double? = null, y2: Double? = null): PavementDamageEntity {
        val width = if (x != null && x2 != null) abs(x2 - x) else null
        val length = if (y != null && y2 != null) abs(y2 - y) else null
        return PavementDamageEntity(UUID.randomUUID().toString(), sample.id, 0, sample.surface, definition.code, definition.name,
            severity, definition.geometry.name, x, y, x2, y2, null, width,
            if (definition.geometry == PavementGeometry.LINEAR && width != null && length != null) hypot(width,length) else length,
            if (definition.geometry == PavementGeometry.AREA && width != null && length != null) width*length else null,
            if (definition.geometry == PavementGeometry.POTHOLE) 1.0 else null, null, "", slabs = slabSet.sorted().joinToString(","))
    }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Surface(color=MaterialTheme.colorScheme.primary,contentColor=MaterialTheme.colorScheme.onPrimary,shape=androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=10.dp),horizontalArrangement = Arrangement.spacedBy(8.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                TextButton(onClick = onBack,colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.onPrimary)) { Text("← Muestras") }
                Column(Modifier.weight(1f)) {
                    Text("Fallas en pavimentos",style=MaterialTheme.typography.titleLarge)
                    Text("${surface.label} · ${sample.tramo} · muestra ${sample.muestra}",style=MaterialTheme.typography.bodyMedium)
                }
                if(!editable && sample.inspectorUserId==userId) TextButton(onClick={scope.launch {runCatching {repo.reopenSample(sample.id)}.onFailure {message=it.message.orEmpty()}}}) {Text("Reabrir para corregir")}
                if (editable) Button(onClick = { closeConfirm = true },colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.colorScheme.surface,contentColor=MaterialTheme.colorScheme.primary)) { Text("Cierre") }
            }
        }
        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.primary)
        BoxWithConstraints(Modifier.weight(1f)) {
            val wide = maxWidth >= 800.dp
            val scheme: @Composable () -> Unit = {
                Column {
                    Text("Origen inferior izquierdo · x/ancho → · y/largo ↑ · metros")
                    if(movingId!=null) TextButton(onClick={movingId=null}) { Text("Terminar movimiento") }
                    PavementScheme(sample, damages, slabSet, first, editable,
                        moving=damages.firstOrNull{it.id==movingId}, highlightedDamageId=selected,
                        onMove={ moved -> scope.launch {
                            runCatching{repo.saveDamage(moved)}.onSuccess{message="Daño movido y guardado."}.onFailure{message=it.message.orEmpty()}
                        } }) { x, y ->
                        message="Y = ${String.format(java.util.Locale.US,"%.1f",y)} m"
                        if (sample.anchoM <= 0 || sample.largoM <= 0) { message = "Guarda la cabecera antes de dibujar." }
                        else when (definition.geometry) {
                            PavementGeometry.GLOBAL -> message = "Usa Registrar global; no requiere dibujo."
                            PavementGeometry.SLABS -> {
                                if (sample.slabRows > 0 && sample.slabColumns > 0) {
                                    val col = (x / sample.anchoM * sample.slabColumns).toInt().coerceIn(0,sample.slabColumns-1)
                                    val row = (y / sample.largoM * sample.slabRows).toInt().coerceIn(0,sample.slabRows-1)
                                    val n = row*sample.slabColumns+col+1
                                    slabSet = if (n in slabSet) slabSet-n else slabSet+n
                                } else message = "Define filas y columnas de losas en la cabecera."
                            }
                            PavementGeometry.POTHOLE -> { draft = newDamage(x,y) }
                            else -> { val start = first; if (start == null) first = x to y else { draft = newDamage(start.first,start.second,x,y); first = null } }
                        }
                    }
                    Text(if(movingId!=null) "Arrastra en el esquema para mover el daño seleccionado. Al soltar se guarda." else if (first == null) "Línea/área: toca inicio y final. Hueco: toca centro. Losas: toca las afectadas." else "Toca el punto final.")
                    if (first != null || slabSet.isNotEmpty()) TextButton(onClick = { first = null; slabSet = emptySet() }) { Text("Limpiar selección") }
                }
            }
            val panel: @Composable () -> Unit = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Inspector: ${sample.inspectorNombre} · Fecha de creación: ${PavementHistory.dateLabel(sample.fecha)}")
                    Text("Progresiva ${PavementChainage.display(sample.prInicial)} → ${PavementChainage.display(sample.prFinal)} · Carril ${sample.carril}")
                    Text("${sample.anchoM} × ${sample.largoM} m · Área ${sample.anchoM*sample.largoM} m²")
                    if (editable) OutlinedButton(onClick = { message=""; header = true }) { Text("Modificar cabecera") }
                    if(sample.surface=="RIGID") Text("${sample.slabRows} filas × ${sample.slabColumns} columnas = ${PavementHeader.totalSlabs(sample)} losas · longitud: ${sample.slabRows} × ${PavementHeader.slabLength(sample)} = ${sample.largoM} m")
                    PavementRules.warnings(sample, damages).forEach { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (editable) {
                        Choice("Daño", definition, PavementCatalog.forSurface(surface), { "${it.code} · ${it.name}" }) { definition = it; severity = it.severities.firstOrNull(); first = null; slabSet = emptySet(); movingId=null }
                        if (definition.severities.isNotEmpty()) Choice("Severidad", severity ?: definition.severities.first(), definition.severities, { PavementSeverity.label(it) }) { severity = it }
                        else Text("Severidad: no aplica según catálogo.")
                        val help = remember(definition.code, severity) { PavementTechnicalText.severity(context, definition.code, severity) }
                        Text(help, style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick={showMeasurement=!showMeasurement}){Text(if(showMeasurement)"Ocultar reglas de medición" else "Ver reglas de medición del manual")}
                        if(showMeasurement) Text(remember(definition.code){PavementTechnicalText.measurement(context,definition.code)},style=MaterialTheme.typography.bodySmall)
                        if (definition.geometry == PavementGeometry.GLOBAL || definition.geometry == PavementGeometry.SLABS) Button(onClick = { message=""; draft = newDamage() }) { Text(if (definition.geometry == PavementGeometry.GLOBAL) "Registrar global" else "Aceptar ${slabSet.size} losas") }
                    }
                    PavementDamageTable(damages, selected) { selected=it; movingId=null }
                    damages.firstOrNull { it.id==selected }?.let { d ->
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(10.dp)) {
                                Text("${d.code}-#${d.correlativo} ${d.name} · ${d.severity ?: "N/A"}")
                                Text("X: ${PavementDisplay.number(d.x1)} m · Y: ${PavementDisplay.number(d.y1)} m")
                                Text("Cantidad: ${PavementDisplay.number(PavementRules.quantity(d))} · ${d.observation.orEmpty()}")
                                if(editable) PavementDamageActions(d,importing,
                                    onModify={ chosen -> message="";draft=chosen;movingId=null },
                                    onMove={ chosen -> movingId=chosen.id;first=null;slabSet=emptySet();message="Arrastra en el esquema para mover ${chosen.code}-#${chosen.correlativo}." },
                                    onCamera={ if(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) launchCamera() else permission.launch(Manifest.permission.CAMERA) },
                                    onDelete={ chosen -> scope.launch {runCatching{repo.deleteDamage(chosen)}.onFailure{message=it.message.orEmpty()}} },
                                    onImport={ chosen -> pickerDamage=chosen.id;picker.launch("image/*") })
                                if (selected == d.id) key(d.id) { PhotoGallery(d.id, photoRevision, repo, editable, { photoRevision++ }, { message=it },
                                    onCamera={if(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) launchCamera() else permission.launch(Manifest.permission.CAMERA)},
                                    onImport={pickerDamage=d.id;picker.launch("image/*")}) }
                            }
                        }
                    }
                    PavementOutputPanel(sample, damages, repo)
                    PavementSicPanel(sample, damages, repo, sample.inspectorUserId==userId)
                }
            }
            if (wide) Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { Box(Modifier.weight(0.56f)) { scheme() }; Box(Modifier.weight(0.44f)) { panel() } }
            else Column { Box(Modifier.height(420.dp)) { scheme() }; Box(Modifier.weight(1f)) { panel() } }
        }
    }
    if (header) SampleDialog(sample, message, { header = false }) { updated -> scope.launch { runCatching { repo.saveSample(updated) }.onSuccess { header = false; message = "Cabecera guardada." }.onFailure { message = it.message.orEmpty() } } }
    draft?.let { damage -> DamageDialog(damage, message, { draft = null }) { updated -> scope.launch { runCatching { repo.saveDamage(updated) }.onSuccess { draft = null; slabSet = emptySet(); message = "Daño guardado." }.onFailure { message = it.message.orEmpty() } } } }
if (closeConfirm) AlertDialog(onDismissRequest = { closeConfirm = false }, title = { Text("Cerrar muestra") }, text = { Text("Se conservarán los registros y la muestra quedará de solo lectura. El índice permanecerá pendiente si faltan curvas. Revisa antes las advertencias y las medidas.") }, confirmButton = { TextButton(onClick = { scope.launch { runCatching { repo.closeSample(sample.id, PavementCalculator(context))
                val saved=repo.observeSample(sample.id).first()!!
                if(com.tuempresa.inventariovial.DriveUploadPolicy.configurationError()==null) com.tuempresa.inventariovial.core.PavementSyncAdapter.enqueue(context,saved) }.onFailure { message = it.message.orEmpty() }; closeConfirm = false } }) { Text("Cerrar") } }, dismissButton = { TextButton(onClick = { closeConfirm = false }) { Text("Volver") } })
}

@Composable internal fun PhotoGallery(id: String, revision: Int, repo: PavementRepository, editable: Boolean, onChanged: () -> Unit, onError: (String) -> Unit, onCamera: ()->Unit = {}, onImport: ()->Unit = {}) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val photos by produceState<List<PavementDamagePhotoEntity>>(emptyList(),id,revision) { value=repo.photos(id) }
    var remove by remember(id) { mutableStateOf<PavementDamagePhotoEntity?>(null) }
    com.tuempresa.inventariovial.core.PhotoCaptureCard("Fotografías del daño",photos.map {File(it.localPath)},
        onTakePhoto={onCamera()},onSelectFile=onImport,onDelete={f->remove=photos.firstOrNull { File(it.localPath)==f }},editable=editable)
    remove?.let { photo -> AlertDialog(onDismissRequest={remove=null}, title={Text("Eliminar fotografía")},text={Text("¿Eliminar esta fotografía del daño?")},
        confirmButton={TextButton(onClick={remove=null;scope.launch {
            runCatching { repo.deletePhoto(photo.id,id,File(context.filesDir,"images")) }.onSuccess { onChanged() }.onFailure { onError(it.message.orEmpty()) }
        }}) { Text("Eliminar") }}, dismissButton={TextButton(onClick={remove=null}) { Text("Cancelar") }}) }

}

@Composable private fun PavementPhotoImage(path:String) {
    val bitmap by produceState<android.graphics.Bitmap?>(null,path) { value=withContext(Dispatchers.IO){loadCorrectlyOrientedBitmap(path)} }
    DisposableEffect(bitmap) { onDispose { bitmap?.recycle() } }
    bitmap?.let { Image(it.asImageBitmap(),"Fotografía del daño",Modifier.fillMaxWidth().height(180.dp)) }
    if(bitmap==null) Text("Foto no disponible")
}
