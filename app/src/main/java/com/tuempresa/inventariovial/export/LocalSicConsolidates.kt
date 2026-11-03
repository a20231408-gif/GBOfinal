package com.tuempresa.inventariovial.export

import android.content.Context
import android.net.Uri
import android.util.AtomicFile
import androidx.room.withTransaction
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.localfiles.*
import com.tuempresa.inventariovial.scap.export.ScapSicExporter
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile

data class LocalSicResult(val route:String,val format:String,val uri:Uri,val file:File,val rows:Int,val pending:Int,val retained:Int) {
    val location get()="Descargas/GBO Inventario Vial/$route/EXCEL_SIC/$format.xlsx"
    val message get()="$format actualizado: $rows filas locales. $location"+
        (if(pending+retained>0)" · $pending fichas pendientes; $retained filas/celdas conservan datos previos. PROVISIONAL: completar en gabinete." else "")+
        " · Incluye únicamente los registros disponibles en esta tablet."
}

/** Local-only projection. Never uploads a route workbook or alters remote ACK/revisions. */
object LocalSicConsolidates {
    private val mutex=Mutex()
    /** Resolve the CURRENT verified route document, never a cached URI or an older retained row. */
    suspend fun principals(context:Context,db:InventoryDatabase,kind:String,id:String,
        storage:SharedDocumentStore=AndroidSharedDocumentStore(context)):List<LocalSharedFile> {
        if(kind!="SIC")return emptyList()
        val snapshot=db.inventoryDao().snapshot(id) ?: return emptyList();val r=snapshot.record
        if(r.status!="ACTIVE"||r.sicCode=="SIC-17"||r.routeCode.isBlank())return emptyList()
        val formats=listOf(r.sicCode)+if(r.sicCode=="SIC-18"&&snapshot.sic18a!=null)listOf("SIC-18A")else emptyList()
        val verified=mutex.withLock {formats.mapNotNull{format->
            try {
                val dir=File(context.filesDir,"sic_consolidated/"+key(r.routeCode,format))
                if(read(File(dir,"journal.json"))?.optString("phase")!="DONE")return@mapNotNull null
                val meta=read(File(dir,"current.json")) ?: return@mapNotNull null
                require(meta.getString("route")==r.routeCode&&meta.getString("format")==format)
                val rows=meta.getJSONArray("rows")
                if((0 until rows.length()).none{n->val row=rows.getJSONObject(n);row.getString("id")==id&&row.getLong("revision")==r.updatedAt})return@mapNotNull null
                val f=descriptor(meta).copy(id="SIC_PRIMARY/$id/$format",recordKey="SIC/$id",uri=meta.getString("uri"),state="READY")
                require(fileDigest(File(f.source))==(f.size to f.sha256))
                storage.checkSlot(SharedDocumentStore.Slot(Uri.parse(f.uri),true),f)
                require(storage.digest(Uri.parse(f.uri))==(f.size to f.sha256))
                f
            }catch(e:CancellationException){throw e}catch(e:Exception){null}
        }}
        val current=db.inventoryDao().recordById(id)
        return if(current?.updatedAt==r.updatedAt&&current.status=="ACTIVE"&&current.routeCode==r.routeCode)verified else emptyList()
    }
    /** Public visibility only: the builder's individual XLSX and frozen row remain private.
     * Never suppress a draft, missing/stale row, failed publication, explicit ZIP or special family.
     * Existing frozen manifests are intentionally not passed through this policy by the drain.
     */
    suspend fun preferConsolidates(context:Context,db:InventoryDatabase,job:LocalExportJob,plan:LocalPlan,
        storage:SharedDocumentStore):LocalPlan {
        if(job.kind!="SIC"||!job.scope.startsWith("FULL")||plan.publicNamingVersion!=5)return plan
        val record=db.inventoryDao().recordById(job.recordId) ?: return plan
        if(record.status!="ACTIVE"||record.sicCode=="SIC-17"||record.routeCode.isBlank())return plan
        return mutex.withLock {
            val privateRows=mutableListOf<LocalSharedFile>();val publicPaths=mutableListOf<String>()
            val visible=plan.files.filterNot{file->
                val format=Regex("(SIC-[0-9]+[A-Z]?)(?:_PROVISIONAL)?\\.xlsx").matchEntire(file.name)?.groupValues?.get(1)
                if(format!=record.sicCode && !(record.sicCode=="SIC-18"&&format=="SIC-18A"))false
                else try {
                    val dir=File(context.filesDir,"sic_consolidated/"+key(record.routeCode,requireNotNull(format)))
                    val meta=read(File(dir,"current.json"))
                    val rows=meta?.optJSONArray("rows")
                    val covered=rows!=null && (0 until rows.length()).any{n->val row=rows.getJSONObject(n)
                        row.getString("id")==record.id&&row.getLong("revision")==job.revision}
                    if(!covered || read(File(dir,"journal.json"))?.optString("phase")!="DONE")false
                    else {
                        require(meta!!.getString("route")==record.routeCode && meta.getString("format")==format)
                        val consolidated=descriptor(requireNotNull(meta))
                        require(fileDigest(File(consolidated.source))==(consolidated.size to consolidated.sha256))
                        val slot=SharedDocumentStore.Slot(Uri.parse(meta.getString("uri")),true)
                        storage.checkSlot(slot,consolidated)
                        (storage.digest(slot.uri)==(consolidated.size to consolidated.sha256)).also{verified->
                            if(verified){privateRows+=file;publicPaths+=consolidated.relativePath+consolidated.name}
                        }
                    }
                }catch(e:CancellationException){throw e}catch(e:Exception){false} // Fail open for visibility, never lose the fallback document.
            }
            if(privateRows.isEmpty())return@withLock plan
            // The public manifest must not claim that a private per-record workbook was published.
            val files=visible.map{file->if(file.name!="MANIFIESTO_LOCAL.json")file else {
                val manifest=JSONObject(File(file.source).readText()).put("localFilesExpected",visible.size)
                    .put("privateSicWorkbooks",JSONArray(privateRows.map{JSONObject().put("name",it.name).put("sha256",it.sha256).put("size",it.size)}))
                    .put("publicSicConsolidates",JSONArray(publicPaths.distinct()))
                    .put("files",JSONArray(visible.filter{it.id!=file.id}.map{JSONObject().put("path",it.relativePath+it.name).put("sha256",it.sha256).put("size",it.size)}))
                atomic(File(file.source),manifest.toString(2).toByteArray())
                val digest=fileDigest(File(file.source));file.copy(size=digest.first,sha256=digest.second)
            }}
            plan.copy(files=files)
        }
    }
    private fun key(route:String,format:String)=com.tuempresa.inventariovial.delivery.sha256("$route\u0000$format".toByteArray())
    private fun atomic(file:File,bytes:ByteArray) {
        file.parentFile!!.let{check(it.isDirectory||it.mkdirs())}
        val a=AtomicFile(file);val stream=a.startWrite()
        try{stream.write(bytes);a.finishWrite(stream)}catch(e:Exception){a.failWrite(stream);throw e}
    }
    private fun read(file:File):JSONObject?=if(file.exists()||File(file.path+".bak").exists())JSONObject(AtomicFile(file).openRead().use{it.readBytes().toString(Charsets.UTF_8)})else null
    private fun save(file:File,value:JSONObject)=atomic(file,value.toString().toByteArray())
    private data class Input(val id:String,val revision:Long,val pr:Long?,val metres:Double?,val bytes:ByteArray?,val problem:String?=null,val provisional:Boolean=false,val registeredDate:String?=null,val registeredAt:Long?=null)
    private fun input(id:String,revision:Long,pr:String?,metres:Double?,provisional:Boolean=false,registeredDate:String?=null,registeredAt:Long?=null,write:(ByteArrayOutputStream)->Unit):Input {
        val validPr=pr?.trim()?.toLongOrNull()?.takeIf{it>=0}
        val m=metres?.takeIf{it.isFinite()&&it>=0}
        return try{val b=ByteArrayOutputStream();write(b);Input(id,revision,validPr,m,b.toByteArray(),provisional=provisional,registeredDate=registeredDate,registeredAt=registeredAt)}
        catch(e:CancellationException){throw e}
        catch(e:Exception){Input(id,revision,validPr,m,null,LocalCopyDrain.publicError(e))}
    }
    private suspend fun collect(db:InventoryDatabase,route:String,format:String):List<Input> = db.withTransaction {
        val rows=mutableListOf<Input>()
        val inspections=db.scapDao().allSnapshots().filter{(it.roadRecord?.routeCode?.takeIf(String::isNotBlank) ?: it.values()["route"])==route && it.roadRecord?.status !in setOf("DISCARDED","VOID","DELETED")}
        val latest=inspections.groupBy{it.inspection.roadRecordId}.values.map{it.maxWith(compareBy<com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot>{s->s.inspection.updatedAt}.thenBy{s->s.inspection.id})}
        val linked=latest.map{it.inspection.roadRecordId}.toSet()
        if(format in setOf("SIC-17","SIC-17A","SIC-17B"))for(s in latest) {
            val preview=ScapSicExporter.previews(s).single{it.format==format}
            rows+=input(s.inspection.roadRecordId,s.inspection.updatedAt,s.roadRecord?.startPrCode,s.roadRecord?.startDistanceM,preview.missingRequired.isNotEmpty()||s.inspection.status!="COMPLETE"){out->
                require(s.roadRecord?.startPrCode?.toIntOrNull()!=null && !com.tuempresa.inventariovial.scap.domain.ScapSicLocation.needsReview(s)){"Ubicación pendiente: se conserva fuera del consolidado de ruta."}
                if(format=="SIC-17") {
                    val row=ByteArrayOutputStream();ScapSicExporter.writeLocal(row,s,format)
                    out.write(LocalSic17Workbook.normalize(row.toByteArray(),s.roadRecord?.observations))
                }else ScapSicExporter.writeLocal(out,s,format)
            }
        }
        val ordinary=(db.inventoryDao().recordsForExport(null,route)+
            if(format in setOf("SIC-17","SIC-17A","SIC-17B"))db.inventoryDao().localBridgeRecordsForExport(route)else emptyList())
            .distinctBy{it.record.id}.filter{it.record.id !in linked}
        val native=SicExportFormat.entries.firstOrNull{it.code==format}
        for(r in ordinary) {
            val bridgeSnapshot=if(format=="SIC-17")db.inventoryDao().snapshot(r.record.id)else null
            if(native!=null && native.appliesTo(r))rows+=input(r.record.id,r.record.updatedAt,r.record.startPrCode,r.record.startDistanceM,
                format=="SIC-17"&&(r.record.status!="ACTIVE"||runCatching{native.values(r)}.isFailure)){out->
                if(format=="SIC-17"){
                    require(r.record.startPrCode.toIntOrNull()!=null){"Ubicación pendiente: copia individual provisional."}
                    LocalSicWorkbookPolicy.primary17(out,requireNotNull(bridgeSnapshot))
                }
                else SicExcelWriter.write(out,listOf(r),native,ExportHeading())
            }
            if(r.record.sicCode=="SIC-17" && format in setOf("SIC-17A","SIC-17B")) {
                val snapshot=requireNotNull(db.inventoryDao().snapshot(r.record.id))
                rows+=input(r.record.id,r.record.updatedAt,r.record.startPrCode,r.record.startDistanceM,
                    ScapSicExporter.supplement(snapshot,format).missingRequired.isNotEmpty()||r.record.status!="ACTIVE"){out->
                    require(r.record.startPrCode.toIntOrNull()!=null){"Ubicación pendiente: copia individual provisional."}
                    val p=ScapSicExporter.supplement(snapshot,format)
                    SicExcelWriter.writeMappedRow(out,format,route,p.columns,p.values,if(format=="SIC-17A")setOf(7,8)else emptySet())
                }
            }
        }
        val pavement=com.tuempresa.inventariovial.pavement.PavementSicFormat.entries.firstOrNull{it.label==format}
        if(pavement!=null)for(s in db.pavementDao().localSamplesForSic().filter{it.ruta==route && it.surface==pavement.surface}) {
            for(d in db.pavementDao().damages(s.id)) {
                val status=com.tuempresa.inventariovial.pavement.PavementSic.status(s,listOf(d),pavement)
                val location=status.rows.singleOrNull()?.values
                rows+=input(d.id,s.updatedAt,location?.getOrNull(3)?.toString(),(location?.getOrNull(4) as? Number)?.toDouble(),provisional=!status.complete || com.tuempresa.inventariovial.pavement.PavementJointOccurrences.needsAssessment(s,listOf(d)),registeredDate=s.fecha,registeredAt=d.createdAt){
                    com.tuempresa.inventariovial.pavement.PavementSic.write(it,s,listOf(d),pavement)
                }
            }
        }
        require(rows.map{it.id}.distinct().size==rows.size){"Identidades duplicadas en el consolidado local; original conservado."}
        rows
    }
    private fun descriptor(meta:JSONObject,name:String=meta.getString("format")+".xlsx"):LocalSharedFile {
        val hash=meta.getString("sha256");val file=File(meta.getString("file"));val route=meta.getString("route")
        return LocalSharedFile("SIC_LOCAL/"+key(route,meta.getString("format")),"SIC_LOCAL/"+key(route,meta.getString("format")),"SIC_LOCAL/"+key(route,meta.getString("format")),
            DriveMirrorPlan.ROOT+"$route/EXCEL_SIC/",name,SicExcelWriter.XLSX_MIME,meta.getLong("size"),hash,file.path)
    }
    private fun result(meta:JSONObject)=LocalSicResult(meta.getString("route"),meta.getString("format"),Uri.parse(meta.getString("uri")),File(meta.getString("file")),meta.getJSONArray("rows").length(),meta.optInt("pending"),meta.optInt("retained"))
    /** Resolve the current route document; an older saved URI may now identify a backup. Read only. */
    suspend fun current(context:Context,route:String,format:String,storage:SharedDocumentStore=AndroidSharedDocumentStore(context)):LocalSicResult = withContext(Dispatchers.IO){mutex.withLock {
        val dir=File(context.filesDir,"sic_consolidated/"+key(route,format))
        require(read(File(dir,"journal.json"))?.optString("phase")=="DONE"){"La publicación local está pendiente; vuelve a exportar para recuperarla."}
        val meta=requireNotNull(read(File(dir,"current.json"))){"Exporta primero el consolidado de esta ruta."}
        val file=descriptor(meta);val slot=SharedDocumentStore.Slot(Uri.parse(meta.getString("uri")),true)
        storage.checkSlot(slot,file)
        require(storage.digest(slot.uri)==(file.size to file.sha256)){"El consolidado público cambió; se conserva su versión privada."}
        result(meta)
    }}
    /** Recover a prepared/publication journal before collecting a newer snapshot. No deletion or counter reset. */
    private fun recover(dir:File,storage:SharedDocumentStore):JSONObject? {
        val journalFile=File(dir,"journal.json");val journal=read(journalFile) ?: return read(File(dir,"current.json"))
        if(journal.optString("phase")=="DONE")return read(File(dir,"current.json"))
        val next=journal.getJSONObject("next");val target=descriptor(next);val source=File(target.source)
        require(fileDigest(source)==(target.size to target.sha256) && SicRouteWorkbook.inspect(source.readBytes(),next.getString("format"))==next.getJSONArray("rows").length()){"Preparación dañada; versión anterior conservada."}
        val staging=target.copy(name=".${target.name}.preparacion-${next.getLong("generation")}.xlsx")
        var slot=journal.optString("stageUri").takeIf{it.isNotBlank()}?.let{SharedDocumentStore.Slot(Uri.parse(it),journal.optBoolean("stagePublished"))}
        if(slot==null) {
            slot=storage.reserve(staging);journal.put("stageUri",slot.uri.toString()).put("stagePublished",slot.published);save(journalFile,journal)
        }
        if(journal.optString("phase") in setOf("PREPARED","STAGING")) {
            journal.put("phase","STAGING");save(journalFile,journal)
            // Re-find a SAF partial/final staging document after a lost publication response.
            slot=storage.reserve(staging,slot.uri)
            journal.put("stageUri",slot.uri.toString()).put("stagePublished",slot.published);save(journalFile,journal)
            storage.checkSlot(slot,staging)
            if(!slot.published){storage.write(slot.uri,source);require(storage.digest(slot.uri)==(target.size to target.sha256)){"La escritura preparada quedó incompleta; el consolidado anterior se conserva."};val uri=storage.publish(slot,staging);slot=SharedDocumentStore.Slot(uri,true)}
            require(storage.digest(slot.uri)==(target.size to target.sha256)){"No se verificó el XLSX preparado; consolidado anterior conservado."}
            journal.put("stageUri",slot.uri.toString()).put("stagePublished",true).put("phase","BACKUP");save(journalFile,journal)
        }
        val previous=journal.optJSONObject("previous")
        if(journal.optString("phase")=="BACKUP") {
            if(previous!=null) {
                val old=descriptor(previous);val oldSlot=SharedDocumentStore.Slot(Uri.parse(previous.getString("uri")),true)
                val backup=storage.renameOwned(oldSlot,old,old.copy(name=".${old.name}.anterior-${next.getLong("generation")}.xlsx"))
                require(storage.digest(backup.uri)==(old.size to old.sha256)){"No se verificó el respaldo público; se conserva la versión privada anterior."}
                journal.put("backupUri",backup.uri.toString());save(journalFile,journal)
            }
            journal.put("phase","PROMOTE");save(journalFile,journal)
        }
        slot=SharedDocumentStore.Slot(Uri.parse(journal.getString("stageUri")),true)
        val final=storage.renameOwned(slot,staging,target)
        storage.checkSlot(final,target)
        require(storage.digest(final.uri)==(target.size to target.sha256)){"No se verificó el archivo final; respaldo conservado."}
        next.put("uri",final.uri.toString());save(File(dir,"current.json"),next)
        save(File(File(next.getString("file")).parentFile,"manifest.json"),next)
        journal.put("phase","DONE").put("finalUri",final.uri.toString());save(journalFile,journal)
        return next
    }
    suspend fun update(context:Context,db:InventoryDatabase,route:String,format:String,
        storage:SharedDocumentStore=AndroidSharedDocumentStore(context),selectedInspection:String?=null):LocalSicResult = withContext(Dispatchers.IO){mutex.withLock {
        require(route.matches(Regex("[A-Za-z0-9][A-Za-z0-9_-]{0,63}"))){"Completa una ruta válida antes de exportar."}
        require(format in SicExportFormat.entries.map{it.code}+listOf("SIC-17A","SIC-17B","SIC-14","SIC-14A","SIC-15","SIC-16"))
        val dir=File(context.filesDir,"sic_consolidated/"+key(route,format)).apply{check(isDirectory||mkdirs())}
        RandomAccessFile(File(dir,"publication.lock"),"rw").use{lockFile->lockFile.channel.lock().use {
            val previous=recover(dir,storage)
            val inputs=collect(db,route,format)
            if(selectedInspection!=null) {
                val selected=requireNotNull(db.scapDao().snapshot(selectedInspection))
                require(inputs.any{r->r.id==selected.inspection.roadRecordId && r.bytes!=null}){"La ficha no se incorporó al consolidado de esta ruta."}
            }
            val entries=linkedMapOf<String,JSONObject>()
            previous?.getJSONArray("rows")?.let{array->for(n in 0 until array.length()){val row=array.getJSONObject(n);entries[row.getString("id")]=row}}
            if(com.tuempresa.inventariovial.pavement.PavementSicFormat.entries.any{it.label==format})
                for(sample in db.pavementDao().localSamplesForSic().filter{it.ruta==route})for(damage in db.pavementDao().retiredDamages(sample.id))entries.remove(damage.id)
            var pending=0;var retained=entries.keys.count{id->inputs.none{it.id==id}}
            for(input in inputs) {
                if(input.bytes==null){if(input.id in entries)retained++ else pending++;continue}
                require(SicRouteWorkbook.inspect(input.bytes,format)==1)
                val compatible=if(format=="SIC-17")LocalSic17Workbook.normalize(input.bytes)else input.bytes
                val previousRow=entries[input.id]?.getString("sha256")?.let{h->File(dir,"rows/$h.xlsx").takeIf{it.isFile}?.readBytes()?.also{require(com.tuempresa.inventariovial.delivery.sha256(it)==h){"La fila histórica cambió; no se sustituyó."}}}
                val retainedCells=if(previousRow!=null&&format in setOf("SIC-17","SIC-17A","SIC-17B"))LocalSicWorkbookPolicy.retainKnown(if(format=="SIC-17")LocalSic17Workbook.normalize(previousRow)else previousRow,compatible,format)else compatible to 0
                if(retainedCells.second>0)retained++
                if(input.provisional)pending++
                val normalized=SicRouteWorkbook.merge(listOf(retainedCells.first),format)
                val hash=com.tuempresa.inventariovial.delivery.sha256(normalized);val rowFile=File(dir,"rows/$hash.xlsx")
                if(!rowFile.exists())atomic(rowFile,normalized)
                require(fileDigest(rowFile).second==hash)
                entries[input.id]=JSONObject().put("id",input.id).put("revision",input.revision).put("pr",input.pr ?: JSONObject.NULL)
                    .put("metres",input.metres ?: JSONObject.NULL).put("sha256",hash)
                    .put("registeredDate",input.registeredDate ?: JSONObject.NULL).put("registeredAt",input.registeredAt ?: JSONObject.NULL)
            }
            val pavementFormat=com.tuempresa.inventariovial.pavement.PavementSicFormat.entries.any{it.label==format}
            val ordered=if(pavementFormat)entries.values.sortedWith(compareBy<JSONObject>{it.optString("registeredDate").takeIf{v->v.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}"))} ?: "9999-99-99"}
                .thenBy{if(it.isNull("registeredAt"))Long.MAX_VALUE else it.getLong("registeredAt")}.thenBy{it.getString("id")})else
                entries.values.sortedWith(compareBy<JSONObject>{if(it.isNull("pr"))Long.MAX_VALUE else it.getLong("pr")}
                .thenBy{if(it.isNull("metres"))Double.POSITIVE_INFINITY else it.getDouble("metres")}.thenBy{it.getString("id")})
            require(ordered.isNotEmpty()){"No hay filas válidas para $format en $route. Completa los datos del formato elegido."}
            val merged=SicRouteWorkbook.merge(ordered.map{row->
                val h=row.getString("sha256");require(h.matches(Regex("[a-f0-9]{64}")))
                val original=File(dir,"rows/$h.xlsx").readBytes().also{require(com.tuempresa.inventariovial.delivery.sha256(it)==h)}
                // Retained historical rows are transformed in memory; original files/hashes remain recoverable.
                if(format=="SIC-17")LocalSic17Workbook.normalize(original)else original
            },format)
            val bytes=LocalSicWorkbookPolicy.status(merged,(pavementFormat||format in setOf("SIC-17","SIC-17A","SIC-17B"))&&pending+retained>0,pending+retained)
            val hash=com.tuempresa.inventariovial.delivery.sha256(bytes)
            val oldSame=previous!=null && previous.optString("sha256")==hash && storage.digest(Uri.parse(previous.getString("uri")))==(previous.getLong("size") to hash)
            val next=if(oldSame)previous!!.put("rows",JSONArray(ordered)).put("pending",pending).put("retained",retained).also{save(File(dir,"current.json"),it)} else {
                val generation=maxOf(System.currentTimeMillis(),(previous?.optLong("generation") ?: 0)+1)
                val source=File(dir,"versions/$generation/$format.xlsx");atomic(source,bytes)
                val meta=JSONObject().put("route",route).put("format",format).put("generation",generation).put("sha256",hash).put("size",bytes.size.toLong())
                    .put("file",source.path).put("rows",JSONArray(ordered)).put("pending",pending).put("retained",retained)
                save(File(dir,"journal.json"),JSONObject().put("phase","PREPARED").put("next",meta).put("previous",previous ?: JSONObject.NULL))
                requireNotNull(recover(dir,storage))
            }
            val file=descriptor(next);val uri=next.getString("uri")
            db.withTransaction {
                db.localFilesDao().saveJob(LocalExportJob(file.jobId,file.recordKey,"SIC_LOCAL",file.id,next.getLong("generation"),scope="ROUTE_SIC",state="READY",message=result(next).message,completed=1,total=1))
                db.localFilesDao().saveFile(file.copy(uri=uri,state="READY"))
            }
            result(next)
        }}
    }}
    suspend fun refreshRecord(context:Context,db:InventoryDatabase,kind:String,id:String,storage:SharedDocumentStore=AndroidSharedDocumentStore(context)):List<String> {
        val route=when(kind){"SCAP"->db.scapDao().snapshot(id)?.let{it.roadRecord?.routeCode?.takeIf(String::isNotBlank) ?: it.values()["route"]};"SIC"->db.inventoryDao().recordById(id)?.routeCode;"PAVEMENT"->db.pavementDao().sample(id)?.ruta;else->null}
        if(route.isNullOrBlank())return emptyList()
        val formats=when(kind){
            "SCAP"->listOf("SIC-17","SIC-17A","SIC-17B")
            "SIC"->db.inventoryDao().recordById(id)?.sicCode?.let{code->if(code=="SIC-17")listOf(code,"SIC-17A","SIC-17B")else if(code=="SIC-18")listOf(code,"SIC-18A")else listOf(code)}.orEmpty()
            "PAVEMENT"->com.tuempresa.inventariovial.pavement.PavementSicFormat.entries.map{it.label}
            else->emptyList()
        }
        val issues=mutableListOf<String>()
        for(format in formats)try{val result=update(context,db,route,format,storage);if(result.pending+result.retained>0)issues+=result.message}
        catch(e:CancellationException){throw e}catch(e:Exception){issues+="$format: "+LocalCopyDrain.publicError(e)}
        return issues
    }
}
