package com.tuempresa.inventariovial.export

import android.content.Context
import android.net.Uri
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class PreparedSicExport(val file: File, val isZip: Boolean, val recordCount: Int, val workbookCount: Int,val displayName:String=file.name,val sourceRecordIds:List<String> = emptyList())
data class LocalExportState(val busy: Boolean = false, val prepared: PreparedSicExport? = null, val message: String? = null)
data class SicSelectionReview(val complete:Int=0,val pending:List<String> = emptyList(),val blocking:List<String> = pending)

class LocalSicExport(private val context: Context, private val database: InventoryDatabase, private val scope: CoroutineScope) {
    private fun routeFile(format: SicExportFormat, route: String): File {
        val routeKey = UUID.nameUUIDFromBytes(route.toByteArray(Charsets.UTF_8))
        return File(context.cacheDir, "sic_exports/routes/$routeKey/${format.code}.xlsx")
    }

    fun cachedRoute(format: SicExportFormat, route: String): com.tuempresa.inventariovial.core.GeneratedFile? {
        val file = routeFile(format, route)
        if (!file.isFile || file.length() == 0L) return null
        return com.tuempresa.inventariovial.core.GeneratedFile("${format.code}/$route", null, "SIC",
            format.code, file.absolutePath, SicExcelWriter.XLSX_MIME, file.lastModified(), "LOCAL")
    }

    /** Local route snapshot only. This file is never uploaded as a global replacement. */
    suspend fun prepareRoute(format: SicExportFormat, route: String): com.tuempresa.inventariovial.core.GeneratedFile = withContext(Dispatchers.IO) {
        if(format==SicExportFormat.SIC17){
            val result=LocalSicConsolidates.update(context,database,route,format.code)
            val cache=routeFile(format,route);check(cache.parentFile!!.isDirectory||cache.parentFile!!.mkdirs())
            val a=android.util.AtomicFile(cache);val out=a.startWrite()
            try{result.file.inputStream().use{it.copyTo(out,64*1024)};a.finishWrite(out)}catch(e:Exception){a.failWrite(out);throw e}
            require(com.tuempresa.inventariovial.localfiles.fileDigest(cache)==com.tuempresa.inventariovial.localfiles.fileDigest(result.file))
            return@withContext requireNotNull(cachedRoute(format,route))
        }
        val rows = database.inventoryDao().recordsForExport(format.code, route).filter { format.appliesTo(it) }
        require(rows.isNotEmpty()) { "No hay fichas validadas de este SIC y ruta en la tablet." }
        rows.forEach { format.values(it) }
        val file = routeFile(format, route)
        check(file.parentFile!!.isDirectory || file.parentFile!!.mkdirs())
        val atomic = android.util.AtomicFile(file)
        val stream = atomic.startWrite()
        try { SicExcelWriter.write(stream, rows, format, savedHeading); atomic.finishWrite(stream) }
        catch (e: Exception) { atomic.failWrite(stream); throw e }
        requireNotNull(cachedRoute(format, route))
    }

    private val mutableState = MutableStateFlow(LocalExportState())
    val state = mutableState.asStateFlow()
    private val preferences = context.getSharedPreferences("sic_export", Context.MODE_PRIVATE)
    val savedHeading: ExportHeading get() = ExportHeading(preferences.getString("project", "").orEmpty(),
        preferences.getString("road", "").orEmpty(), preferences.getString("section", "").orEmpty())

    /** The review uses the same contractual projection as the writer. */
    suspend fun review(format:SicExportFormat?,route:String?):SicSelectionReview = withContext(Dispatchers.IO) {
        val records=database.inventoryDao().recordsForExport(format?.code,route)
        val errors=records.mapNotNull {item->
            val signalPending=item.sic22?.let(com.tuempresa.inventariovial.model.form.SignalIdentificationPolicy::completionPending).orEmpty()
            if(signalPending.isNotEmpty()) return@mapNotNull "SIC-22 · ${item.record.routeCode}: ${signalPending.joinToString()}"
            runCatching {(format ?: SicExportFormat.entries.single {it.code==item.record.sicCode}).values(item)}
                .exceptionOrNull()?.let {"${item.record.sicCode} · ${item.record.routeCode}: ${it.message}"}
        }
        SicSelectionReview(records.size-errors.size,errors,errors.filterNot{it.startsWith("SIC-17 ·")})
    }

    fun prepare(format: SicExportFormat?, route: String?, heading: ExportHeading) {
        if (mutableState.value.busy) return
        val previous = mutableState.value.prepared
        mutableState.value = LocalExportState(busy = true)
        scope.launch {
            try {
                val prepared = withContext(Dispatchers.IO) {
                    // Keep prior prepared snapshots recoverable; choosing a destination never replaces them.
                    val records = database.inventoryDao().recordsForExport(format?.code, route)
                    require(records.isNotEmpty()) { "No hay registros activos para exportar con esta selección." }
                    val directory = File(context.cacheDir, "sic_exports")
                    check(directory.isDirectory || directory.mkdirs()) { "No se pudo preparar la carpeta de exportación." }
                    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                    val file = if(format!=null&&route!=null)LocalSicConsolidates.prepareManual(context,database,route,format.code).let{File(it.source)} else {
                        val target=File(directory,"${format?.code ?: "SIC_INVENTARIO"}_${timestamp}_${UUID.randomUUID().toString().take(8)}.${if(format==null)"zip"else"xlsx"}")
                        target.outputStream().buffered().use{LocalSicWorkbookPolicy.selection(it,database,records,format,heading)}
                        target
                    }
                    preferences.edit().putString("project", heading.project).putString("road", heading.road)
                        .putString("section", heading.section).apply()
                    PreparedSicExport(file, format == null, if(format!=null&&route!=null)SicRouteWorkbook.inspect(file.readBytes(),format.code)else records.size, records.map { it.record.sicCode }.distinct().size,
                        com.tuempresa.inventariovial.localfiles.PublicLocalFolderNames.component("${format?.code ?: "Inventario SIC"} - ${route ?: "Todas las rutas"}")+(if(format==null)".zip"else".xlsx"),records.map{it.record.id})
                }
                mutableState.value = LocalExportState(prepared = prepared,
                    message = "Archivo preparado con ${prepared.recordCount} registros. Elige dónde guardarlo.")
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                mutableState.value = LocalExportState(message = error.message ?: "No se pudo generar el archivo.")
            }
        }
    }

    fun save(uri: Uri) {
        if (mutableState.value.busy) return
        val prepared = mutableState.value.prepared
        if (prepared == null) {
            mutableState.value = LocalExportState(message = "El archivo preparado ya no está disponible. Vuelve a generarlo.")
            return
        }
        mutableState.value = mutableState.value.copy(busy = true, message = "Guardando archivo…")
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    require(prepared.file.isFile) { "El archivo temporal ya no está disponible. Vuelve a generarlo." }
                    val document=com.tuempresa.inventariovial.localfiles.ManualDocumentExport.freeze(context,prepared.file,"SIC",
                        if(prepared.isZip)"application/zip"else SicExcelWriter.XLSX_MIME,prepared.displayName)
                    com.tuempresa.inventariovial.localfiles.ManualDocumentExport.save(context,database,document,uri)

                }
                mutableState.value = LocalExportState(prepared = prepared,
                    message = "ARCHIVO GUARDADO · Verificado en la ubicación elegida: ${prepared.displayName} (${prepared.recordCount} registros, ${prepared.workbookCount} Excel).")
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                mutableState.value = LocalExportState(prepared = prepared,
                    message = "No se completó el guardado: ${error.message ?: "comprueba el espacio y el destino"}. Puedes reintentarlo.")
            }
        }
    }
}
