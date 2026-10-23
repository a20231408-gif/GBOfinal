package com.tuempresa.inventariovial.localfiles

import android.content.Context
import android.net.Uri
import android.os.StatFs
import android.util.AtomicFile
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.export.*
import com.tuempresa.inventariovial.media.VideoDatabase
import com.tuempresa.inventariovial.pavement.*
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.export.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import org.json.*
import java.io.File
import java.io.OutputStream
import java.text.Normalizer
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class LocalPlan(val files:List<LocalSharedFile>,val problems:List<String>,val publicNamingVersion:Int=1) {
    fun json()=JSONObject().put("files",JSONArray(files.map{f->JSONObject().put("id",f.id).put("jobId",f.jobId).put("recordKey",f.recordKey)
        .put("relativePath",f.relativePath).put("name",f.name).put("mime",f.mimeType).put("size",f.size).put("sha256",f.sha256).put("source",f.source)}))
        .put("problems",JSONArray(problems)).put("publicFolderNamingVersion",publicNamingVersion).toString()
    companion object {
        fun parse(value:String):LocalPlan {val j=JSONObject(value);val a=j.getJSONArray("files");return LocalPlan((0 until a.length()).map{n->val f=a.getJSONObject(n)
            LocalSharedFile(f.getString("id"),f.getString("jobId"),f.getString("recordKey"),f.getString("relativePath"),f.getString("name"),f.getString("mime"),f.getLong("size"),f.getString("sha256"),f.getString("source"))},j.getJSONArray("problems").let{p->(0 until p.length()).map{p.getString(it)}},j.optInt("publicFolderNamingVersion",1))}
    }
}
object LocalExportNames {
    fun safe(value:String):String {
        val text=Normalizer.normalize(value,Normalizer.Form.NFD).replace(Regex("\\p{M}+"),"").replace(Regex("[^A-Za-z0-9._-]+"),"_").trim('.','_').ifBlank{"SIN_REFERENCIA"}
        return if(text.length<=60)text else text.take(48)+"_"+com.tuempresa.inventariovial.delivery.sha256(value.toByteArray()).take(10)
    }
    fun root(kind:String,route:String,id:String,revision:Long,label:String)=
        "Download/GBO_Ruta_Clara/${when(kind){"PAVEMENT"->"PAVIMENTOS";"PHOTO"->"FOTOGRAFIAS";else->kind}}/${safe(route)}/${safe(label)}_${safe(id)}/R_$revision/"
}
/** Uses existing contractual exporters; never uses a Drive preparation, reservation, receipt or HTTP. */
object LocalExportBuilder {
    const val MAX_BYTES=512L*1024*1024
    fun work(context:Context,job:LocalExportJob)=File(context.filesDir,"local_exports/${job.id}").apply{check(isDirectory||mkdirs())}
    fun space(context:Context,bytes:Long){require(bytes in 0..MAX_BYTES){"La entrega excede 512 MiB. Exporta fotografías por separado o divide el lote."};check(StatFs(context.filesDir.path).availableBytes>bytes+32L*1024*1024){"Espacio insuficiente. Libera espacio y reintenta; la ficha y los originales se conservan."}}
    private fun atomic(file:File,write:(OutputStream)->Unit){file.parentFile!!.mkdirs();val a=AtomicFile(file);val out=a.startWrite();try{write(object:java.io.FilterOutputStream(out){override fun close(){flush()}});a.finishWrite(out)}catch(e:Exception){a.failWrite(out);throw e}}
    suspend fun build(context:Context,db:InventoryDatabase,job:LocalExportJob):LocalPlan {
        val dir=work(context,job);space(context,0)
        val files=mutableListOf<LocalSharedFile>();val problems=mutableListOf<String>();var root="";var location:LocalDocumentLocation?=null;var captureState="";val pendingFields=mutableListOf<String>()
        val exportScope=job.scope.removeSuffix("_READABLE_V2")
        val photosOnly=exportScope=="PHOTOS"
        fun add(file:File,relative:String,mime:String){
            require(relative.split('/').all{it.isNotBlank()&&it!=".."&&it!="."})
            if(!file.isFile){problems+="Falta $relative";return}
            val (size,hash)=fileDigest(file);if(size==0L){problems+="Archivo vacío: $relative";return}
            val media=relative.substringBefore('/') in setOf("FOTOGRAFIAS","CROQUIS","VIDEOS")
            val actual=if(media)relative.substringBeforeLast('.')+"_"+hash.take(16)+"."+relative.substringAfterLast('.')else relative
            val folder=actual.substringBeforeLast('/',"");val name=actual.substringAfterLast('/')
            val targetRoot=if(media)location?.base ?: root else root
            files+=LocalSharedFile(com.tuempresa.inventariovial.delivery.sha256((job.id+"/"+relative).toByteArray()),job.id,job.recordKey,targetRoot+if(folder.isBlank())""else"$folder/",name,mime,size,hash,file.absolutePath)
        }
        suspend fun attempt(label:String,block:suspend ()->Unit){try{block()}catch(e:CancellationException){throw e}catch(e:Exception){problems+="$label: ${e.message?.take(180) ?: "no se pudo generar"}"}}
        fun document(name:String,mime:String,write:(OutputStream)->Unit){val f=File(dir,name);atomic(f,write);add(f,name,mime)}
        fun original(id:String,path:String,category:String="GENERAL") {
            val f=File(path);if(!f.isFile){problems+="Falta fotografía $id";return}
            val header=f.inputStream().use{val b=ByteArray(16);val n=it.read(b);b.take(maxOf(0,n)).toByteArray()}
            val (ext,mime)=when {
                header.size>=2&&header[0]==0xff.toByte()&&header[1]==0xd8.toByte()->"jpg" to "image/jpeg"
                header.size>=4&&header[0]==0x89.toByte()&&String(header,1,3)=="PNG"->"png" to "image/png"
                header.size>=12&&String(header,0,4)=="RIFF"&&String(header,8,4)=="WEBP"->"webp" to "image/webp"
                header.size>=12&&String(header,4,4)=="ftyp"->"heic" to "image/heic"
                else->f.extension.lowercase().ifBlank{"bin"} to "application/octet-stream"
            }
            add(f,"FOTOGRAFIAS/${LocalExportNames.safe(category)}/${LocalExportNames.safe(id)}.$ext",mime)
        }
        when(job.kind){
            "SIC"->{
                val snapshot=requireNotNull(db.inventoryDao().snapshot(job.recordId)){"La ficha ya no está disponible."};val r=snapshot.record;captureState=r.status
                require(r.updatedAt==job.revision){"La revisión cambió. Exporta la revisión guardada actual."}
                
                val family=com.tuempresa.inventariovial.road.AssetPresentationCatalog.resolve(com.tuempresa.inventariovial.data.entity.InventoryRecordWithPhotos(r,snapshot.photos,snapshot.sic23,snapshot.sic18,snapshot.sic20,snapshot.sic19,snapshot.sic18a,snapshot.sicExtension,snapshot.sic21))
                location=LocalDocumentLocation.resolve(db,job,r.routeCode,com.tuempresa.inventariovial.core.DriveInventorySchema.familyPath(family),com.tuempresa.inventariovial.core.DriveInventorySchema.elementLabel(family))
                root=location.revision(job.revision)+if(job.scope.startsWith("FULL_MEDIA_"))"MEDIOS_${job.scope.removePrefix("FULL_MEDIA_").take(16)}/"else""
                space(context,snapshot.photos.filter{it.scapInspectionId==null}.sumOf{File(it.originalPath.ifBlank{it.localPath}).length()}+16L*1024*1024)
                if(!photosOnly){
                    val row=com.tuempresa.inventariovial.data.entity.InventoryRecordForExport(r,snapshot.sic17,snapshot.sic18,snapshot.sic19,snapshot.sic20,snapshot.sic21,snapshot.sic22,snapshot.sic23,snapshot.sic18a,snapshot.sicExtension)
                    val format=SicExportFormat.entries.single{it.code==r.sicCode}
                    val author=r.sessionId?.let{db.inventoryDao().sessionById(it)?.operator}.orEmpty()
                    val valid=r.status=="ACTIVE" && runCatching{format.values(row)}.isSuccess && snapshot.sic22?.let{com.tuempresa.inventariovial.model.form.SignalIdentificationPolicy.completionPending(it).isEmpty()}!=false
                    attempt("Excel SIC"){
                        if(valid)document("${r.sicCode}.xlsx",SicExcelWriter.XLSX_MIME){SicExcelWriter.write(it,listOf(row),format,ExportHeading())}
                        else document("${r.sicCode}_PROVISIONAL.xlsx",SicExcelWriter.XLSX_MIME){ProvisionalSicExcel.write(it,snapshot,author)}
                    }
                    if(snapshot.sic18a!=null)attempt("SIC-18A"){
                        val complete=valid && runCatching{SicExportFormat.SIC18A.values(row)}.isSuccess
                        if(complete)document("SIC-18A.xlsx",SicExcelWriter.XLSX_MIME){SicExcelWriter.write(it,listOf(row),SicExportFormat.SIC18A,ExportHeading())}
                        else document("SIC-18A_PROVISIONAL.xlsx",SicExcelWriter.XLSX_MIME){ProvisionalSicExcel.write(it,snapshot,author,"SIC-18A")}
                    }
                    if(r.sicCode=="SIC-17")for(code in listOf("SIC-17A","SIC-17B"))attempt(code){
                        val p=ScapSicExporter.supplement(snapshot,code)
                        if(valid&&p.missingRequired.isEmpty())document("$code.xlsx",SicExcelWriter.XLSX_MIME){SicExcelWriter.writeMappedRow(it,code,r.routeCode,p.columns,p.values,if(code=="SIC-17A")setOf(7,8)else emptySet())}
                        else document("${code}_PROVISIONAL.xlsx",SicExcelWriter.XLSX_MIME){ProvisionalSicExcel.writeBridgeSupplement(it,snapshot,code,author)}
                    }
                    if(TechnicalLengthExport.applies(row))attempt("Longitud técnica"){
                        val reference=com.tuempresa.inventariovial.road.FieldPrRepository(db.inventoryDao()).merge(com.tuempresa.inventariovial.road.RoadReferenceRepository(context).loadFromAssets())
                        document("LONGITUD.xlsx",SicExcelWriter.XLSX_MIME){TechnicalLengthExport.write(it,row,reference.prs)}
                    }
                    if(!r.observations.isNullOrBlank())document("OBSERVACIONES.html","text/html"){it.write(ObservationReport.render(listOf(r)).toByteArray())}
                    document("FICHA.json","application/json"){it.write(com.tuempresa.inventariovial.server.ServerPayload.encode(snapshot).put("extension",snapshot.sicExtension?.let{e->JSONObject(e.values())}).put("author",author).toString(2).toByteArray())}
                }
                snapshot.photos.filter{it.scapInspectionId==null}.forEach{original(it.id,it.originalPath.ifBlank{it.localPath},it.photoCategory ?: "GENERAL")}
                document("datos_fotografias.json","application/json"){out->out.write(JSONObject().put("recordId",r.id).put("revision",job.revision).put("route",r.routeCode).put("observations",r.observations ?: JSONObject.NULL)
                    .put("photos",JSONArray(snapshot.photos.filter{it.scapInspectionId==null}.map{p->JSONObject().put("id",p.id).put("category",p.photoCategory).put("order",p.photoIndex).put("capturedAt",p.capturedAt).put("latitude",p.photoLatitude).put("longitude",p.photoLongitude).put("accuracyM",p.photoGpsAccuracyM)})).toString(2).toByteArray())}
            }
            "SCAP"->{
                val s=requireNotNull(db.scapDao().snapshot(job.recordId));captureState=s.inspection.status;pendingFields+=com.tuempresa.inventariovial.scap.domain.ScapCompletion.assess(s,ScapCatalog.load(context).forSnapshot(s)).pending.map{"${it.section}: ${it.field}"};require(s.inspection.updatedAt==job.revision){"La revisión SCAP cambió."}
                
                location=LocalDocumentLocation.resolve(db,job,s.roadRecord?.routeCode?.takeIf{it.isNotBlank()} ?: s.values()["route"].orEmpty(),"PUENTES","PUENTE")
                root=location.revision(job.revision)+if(job.scope.startsWith("FULL_MEDIA_"))"MEDIOS_${job.scope.removePrefix("FULL_MEDIA_").take(16)}/"else""
                val videos=if(photosOnly)emptyList()else VideoDatabase.get(context).videos().all().filter{it.parentKey==job.recordKey&&!it.retired}
                space(context,s.photos.sumOf{File(it.originalPath.ifBlank{it.localPath}).length()}+videos.sumOf{it.sizeBytes}+16L*1024*1024)
                if(!photosOnly){
                    attempt("Excel SCAP"){
                        val isFinal=s.inspection.status=="COMPLETE" && s.values("completion")["status"]=="COMPLETE"
                        val raw=File(dir,"SCAP.generating.xlsx")
                        val generated=try{ScapDelivery.writeExcel(context,s,raw);true}catch(e:CancellationException){throw e}catch(e:Exception){
                            val reason=LocalCopyDrain.publicError(e);pendingFields+="Plantilla SCAP: $reason"
                            document("SCAP_PROVISIONAL.xlsx",SicExcelWriter.XLSX_MIME){ProvisionalScapWorkbook.write(it,s,reason)};false
                        }
                        val name=if(isFinal)"SCAP.xlsx"else"SCAP_PROVISIONAL.xlsx"
                        if(generated)document(name,SicExcelWriter.XLSX_MIME){out->if(isFinal)raw.inputStream().use{it.copyTo(out)}else ProvisionalSicExcel.decorate(out,raw.readBytes(),s.inspection.id,s.inspection.createdBy,job.revision,s.inspection.createdAt,
                            com.tuempresa.inventariovial.scap.domain.ScapCompletion.assess(s,ScapCatalog.load(context).forSnapshot(s)).pending.map{ProvisionalSicExcel.Pending(it.field,it.section,"Dato pendiente de captura")},markerCell="C4")}
                    }
                    for(p in ScapSicExporter.previews(s))attempt(p.format){
                        if(p.missingRequired.isEmpty()&&s.inspection.status=="COMPLETE")document("${p.format}.xlsx",SicExcelWriter.XLSX_MIME){ScapSicExporter.write(it,s,p.format)}
                        else document("${p.format}_PROVISIONAL.xlsx",SicExcelWriter.XLSX_MIME){ProvisionalSicExcel.write(it,s,p.format)}
                    }
                    document("INSPECCION.json","application/json"){it.write(ScapFieldBackup.capture(s).toString(2).toByteArray())}
                    for(sketch in s.sketches)attempt("Croquis ${sketch.id}"){
                        val f=File(dir,"croquis_${LocalExportNames.safe(sketch.id)}.png")
                        atomic(f){out->if(sketch.localUri.startsWith("content:"))requireNotNull(context.contentResolver.openInputStream(Uri.parse(sketch.localUri))).use{it.copyTo(out)}else File(sketch.localUri.removePrefix("file://")).inputStream().use{it.copyTo(out)}}
                        val mime=LocalCopies.photoMime(f);val ext=when(mime){"image/jpeg"->"jpg";"image/png"->"png";"image/webp"->"webp";"image/heic"->"heic";else->"bin"}
                        add(f,"CROQUIS/${LocalExportNames.safe(sketch.type)}_${LocalExportNames.safe(sketch.id)}.$ext",mime)
                    }
                    for(v in videos){val f=File(v.localPath);if(!f.isFile)problems+="Falta video ${v.id}"else if(fileDigest(f)!=(v.sizeBytes to v.sha256))problems+="El video ${v.id} cambió; se conserva sin reescribir."else add(f,"VIDEOS/${LocalExportNames.safe(v.categoryLabel)}/${v.id}.${f.extension}",v.mimeType)}
                }
                s.photos.forEach{original(it.id,it.originalPath.ifBlank{it.localPath},it.photoCategory ?: "GENERAL")}
                document("datos_fotografias.json","application/json"){it.write(JSONObject().put("inspectionId",s.inspection.id).put("revision",job.revision).put("photos",JSONArray(s.photos.map{p->JSONObject().put("id",p.id).put("category",p.photoCategory).put("description",p.description).put("capturedAt",p.capturedAt).put("latitude",p.photoLatitude).put("longitude",p.photoLongitude).put("accuracyM",p.photoGpsAccuracyM)})).toString(2).toByteArray())}
            }
            "PAVEMENT"->{
                val s=requireNotNull(db.pavementDao().sample(job.recordId));captureState=s.status;require(s.updatedAt==job.revision){"La revisión de la muestra cambió."}
                
                location=LocalDocumentLocation.resolve(db,job,s.ruta,"EVALUACION_SUPERFICIAL/${s.tramo}/${when(s.surface){"ASPHALT"->"ASFALTICO";"RIGID"->"RIGIDO";else->"NO_PAVIMENTADO"}}","MUESTRA")
                root=location.revision(job.revision)+if(job.scope.startsWith("FULL_MEDIA_"))"MEDIOS_${job.scope.removePrefix("FULL_MEDIA_").take(16)}/"else""
                val damages=db.pavementDao().damages(s.id);val photos=damages.associate{it.id to db.pavementDao().photos(it.id)}
                space(context,photos.values.flatten().sumOf{File(it.localPath).length()}+16L*1024*1024)
                if(!photosOnly){
                    val result=PavementCalculator(context).calculate(s,damages);pendingFields+=result.pending;val data=PavementExportSample(s,damages,result)
                    attempt("PDF de inspección"){document(if(s.status=="CLOSED")"Inspeccion.pdf"else"Inspeccion_BORRADOR.pdf","application/pdf"){PavementPdf.write(it,data,photos)}}
                    attempt("Excel de datos"){val sections=db.pavementDao().observeSections().first();document("Datos.xlsx",SicExcelWriter.XLSX_MIME){PavementExcel.write(it,listOf(data),sections)}}
                    document("Muestra.json","application/json"){it.write(PavementDriveUploader.nativeJson(s,damages,result,photos).toString(2).toByteArray())}
                    for(format in PavementSicFormat.entries.filter{it.surface==s.surface}){
                        val status=PavementSic.status(s,damages,format)
                        if(status.complete)attempt(format.label){document("${format.label}.xlsx",SicExcelWriter.XLSX_MIME){PavementSic.write(it,s,damages,format)}}
                        else pendingFields+="${format.label}: aporte contractual pendiente de completar"
                    }
                }
                photos.values.flatten().forEach{original(it.id,it.localPath)}
            }
            else->error("Familia no reconocida para generar la copia local.")
        }
        require(DeliveryDestination.revision(db,job.kind,job.recordId)==job.revision){"La ficha cambió durante la preparación. Reintenta la revisión actual."}
        require(files.size<=1000){"La entrega contiene demasiados archivos; exporta por lotes."};space(context,files.sumOf{it.size})
        document("MANIFIESTO_LOCAL.json","application/json"){out->out.write(JSONObject().put("recordKey",job.recordKey).put("revision",job.revision).put("documentaryCopyOnly",true).put("roomRestorable",false)
            .put("captureState",captureState).put("pending",JSONArray(pendingFields.distinct())).put("remoteState",if(location?.ackRevision==job.revision)"PARENT_ACK_CONFIRMED"else"PARENT_NOT_CONFIRMED_FOR_THIS_REVISION").put("localState","GENERATED_PUBLICATION_REQUIRES_VERIFICATION").put("manifestNotSelfHashed",true).put("remoteConfirmationScope","PARENT_DELIVERY_ONLY_VIDEOS_HAVE_SEPARATE_ACK").put("localFilesExpected",files.size+1).put("generationComplete",problems.isEmpty()).put("publicationRequiresVerification",true)
            .put("location",location?.json()).put("problems",JSONArray(problems)).put("files",JSONArray(files.map{JSONObject().put("path",it.relativePath+it.name).put("sha256",it.sha256).put("size",it.size)})).toString(2).toByteArray())}
        if(exportScope=="ZIP"){
            val zip=File(dir,"Entrega_${LocalExportNames.safe(job.recordId)}_R${job.revision}.zip")
            atomic(zip){out->ZipOutputStream(out).use{z->
                for(f in files){z.putNextEntry(ZipEntry(f.relativePath.removePrefix(location?.base ?: root)+f.name).apply{time=0});File(f.source).inputStream().use{it.copyTo(z,64*1024)};z.closeEntry()}

            }}
            files.clear();add(zip,zip.name,"application/zip")
        }
        return LocalPlan(files,problems.distinct(),PublicLocalFolderNames.VERSION)
    }
}
