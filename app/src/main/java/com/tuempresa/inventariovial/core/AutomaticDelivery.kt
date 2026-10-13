package com.tuempresa.inventariovial.core

import android.content.Context
import android.util.Base64
import com.tuempresa.inventariovial.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.delivery.*
import com.tuempresa.inventariovial.export.*
import com.tuempresa.inventariovial.pavement.*
import com.tuempresa.inventariovial.road.*
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.export.*
import kotlinx.coroutines.flow.first
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.*
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

interface AutomaticDriveTransport {
    fun supportsV3():Boolean
    fun supportsTechnicalDeliveryWithoutGps():Boolean = false

    fun supportsFamilyPaths():Boolean = false
    fun supportsReadablePhotoNames():Boolean = false
    fun reservePhotoNames(payload:JSONObject):JSONObject = throw SyncPending("El servicio no admite reservas fotográficas V2.")
    fun post(payload:JSONObject):JSONObject
}
class AutomaticDriveHttp internal constructor(
    private val client:OkHttpClient=OkHttpClient.Builder().connectTimeout(30,TimeUnit.SECONDS).readTimeout(180,TimeUnit.SECONDS).writeTimeout(180,TimeUnit.SECONDS).build(),
    private val endpoint:String=DriveConfig.WEB_APP_URL,
    private val apiToken:String=DriveConfig.API_TOKEN
):AutomaticDriveTransport {
    private val capabilities:JSONObject by lazy {
        client.newCall(Request.Builder().url(endpoint).get().build()).execute().use {
            if(!it.isSuccessful)throw java.io.IOException("HTTP ${it.code}")
            JSONObject(it.body?.string() ?: "{}")
        }
    }
    override fun supportsV3()=capabilities.optBoolean("ok") && capabilities.optInt("syncProtocol")==3
    override fun supportsFamilyPaths()=supportsV3() && capabilities.optInt("pathVersion")==4 && capabilities.optBoolean("stableFamilyNumbering") && capabilities.optInt("photoCategoryVersion")==1
    override fun supportsReadablePhotoNames()=supportsFamilyPaths() && capabilities.optInt("readablePhotoNamingVersion")==2
    override fun reservePhotoNames(payload:JSONObject):JSONObject {
        if(!supportsReadablePhotoNames())throw SyncPending("El backend aún no anuncia readablePhotoNamingVersion=2. Cola conservada.")
        val request=JSONObject().put("action","reservePhotoNamesV2").put("photoNamingVersion",2)
        for(key in listOf("pathVersion","kind","key","recordId","revision","baseRevision","identityId","routeCode","familyPath","family","tramo","surface"))if(payload.has(key))request.put(key,payload.get(key))
        val files=payload.getJSONArray("files");request.put("photos",JSONArray((0 until files.length()).map {files.getJSONObject(it)}.filter {it.getString("key").startsWith("photo/")}.map {JSONObject().put("id",it.getString("key").substringAfter("photo/")).put("category",it.optString("photoCategory","GENERAL"))}))
        return send(request)
    }
    override fun supportsTechnicalDeliveryWithoutGps()=supportsV3() && capabilities.optBoolean("technicalDeliveryWithoutGps")
    private fun send(payload:JSONObject)=client.newCall(Request.Builder().url(endpoint)
        .post(JSONObject(payload.toString()).put("token",apiToken).toString().toRequestBody("application/json; charset=utf-8".toMediaType())).build()).execute().use {
        if(!it.isSuccessful)throw java.io.IOException("HTTP ${it.code}")
        JSONObject(it.body?.string() ?: "{}")
    }
    override fun post(payload:JSONObject):JSONObject {
        if(payload.optInt("photoNamingVersion")==1 && capabilities.optInt("photoNamingVersion") !in setOf(1,2))throw SyncPending("El backend aún no admite prefijos fotográficos. Cola conservada.")
        if(payload.optInt("photoNamingVersion")==2 && !supportsReadablePhotoNames())throw SyncPending("El backend no admite nombres fotográficos V2; copia conservada.")
        val commit=JSONObject(payload.toString());val files=commit.getJSONArray("files")
        for(n in 0 until files.length()) {
            val file=files.getJSONObject(n);val local=File(file.getString("localFile"));val bytes=local.readBytes()
            require(sha256(bytes)==file.getString("sha256")) {"La copia preparada cambió."}
            if(file.getString("mimeType")=="image/jpeg")require(JpegEnvelope.valid(bytes)) {"La copia preparada no es un JPEG completo. Original conservado."}
            val staged=JSONObject(file.toString()).apply {remove("localFile");put("base64",Base64.encodeToString(bytes,Base64.NO_WRAP))}
            val ack=send(JSONObject().put("action","stageRecordV3").put("syncProtocol",3).put("key",commit.getString("key"))
                .put("revision",commit.getLong("revision")).put("file",staged))
            if(!ack.optBoolean("ok"))throw SyncRejection(ack.optString("code"),ack.optBoolean("retryable"))
            require(ack.optString("key")==commit.getString("key") && ack.optLong("revision")==commit.getLong("revision") &&
                ack.optString("fileKey")==file.getString("key") && ack.optString("sha256")==file.getString("sha256") && ack.optString("stageFileId").isNotBlank())
            file.remove("localFile");file.put("stageFileId",ack.getString("stageFileId"))
        }
        return send(commit)
    }
}

object AutomaticDelivery {
    /** Contractual PR ordering is not a GNSS fix and never populates coordinates. */
    suspend fun technicalOrder(c:Context,r:com.tuempresa.inventariovial.data.entity.InventoryRecordEntity):Pair<Double,String> {
        val reference=RoadReferenceRepository(c).loadFromAssets()
        val matched=RoadAxisPositioner(reference,com.tuempresa.inventariovial.field.FieldSettings.load(c).matchConfig()).position(r)
        val axis=(matched.axisMeasureM ?: r.axisMeasureM)?.takeIf {it.isFinite() && it>=0}
        if(axis!=null)return axis to "GNSS_MAP_MATCH"
        val position=ChainageCalculator(reference.prs).calculate(r)
        return (position.chainageM?.takeIf {it.isFinite() && it>=0}
            ?: throw SyncPending("Revisa el PR y la distancia de la ficha para ordenar la entrega técnica.")) to "CONTRACTUAL_PR"
    }
    fun georeferencing(r:com.tuempresa.inventariovial.data.entity.InventoryRecordEntity):JSONObject {
        val available=r.latitude!=null && r.longitude!=null &&
            (r.endPrCode==null || r.endLatitude!=null && r.endLongitude!=null)
        return JSONObject().put("status",if(available) "COORDINATES_AVAILABLE" else "PENDING_EXTERNAL")
            .put("externalSubmetricStatus","PENDING").put("captureSource",r.locationSource)
    }
    suspend fun order(c:Context,db:InventoryDatabase,i:DriveSyncIntent):Pair<String,Double> {
        val r=if(i.kind=="SIC")db.inventoryDao().recordById(i.recordId) else if(i.kind=="SCAP")db.scapDao().snapshot(i.recordId)?.let {s->s.roadRecord?.copy(latitude=s.inspection.latitude ?: s.roadRecord.latitude,longitude=s.inspection.longitude ?: s.roadRecord.longitude)} else null
        if(r==null)return i.key to Double.MAX_VALUE
        val routing=r
        val axis=runCatching {technicalOrder(c,routing).first}.getOrNull()
        val snapshot=db.inventoryDao().snapshot(r.id)
        val family=snapshot?.let {RoadOrdering.orderAssetsForRoute(listOf(RouteDelivery.row(it))).singleOrNull()?.assetFamily?.folderLabel} ?: r.assetType
        return (r.routeCode+"/"+family) to (axis ?: Double.MAX_VALUE)
    }
    suspend fun validated(db:InventoryDatabase,i:DriveSyncIntent)=when(i.kind) {
        "SIC"->db.inventoryDao().recordById(i.recordId)?.status=="ACTIVE"
        "SCAP"->db.scapDao().inspection(i.recordId)?.status=="COMPLETE"
        "PAVEMENT"->db.pavementDao().sample(i.recordId)?.status=="CLOSED"
        else->false
    }
    private suspend fun source(db:InventoryDatabase,i:DriveSyncIntent):String = when(i.kind) {
        "SIC"->RouteDelivery.fingerprint(listOfNotNull(db.inventoryDao().snapshot(i.recordId)),DeliveryNamingPolicy.FOLDER_COMPACT)
        "SCAP"->db.scapDao().snapshot(i.recordId)?.let {s->s.copy(inspection=s.inspection.copy(syncStatus=""),
            values=s.values.filter {it.ownerId !in setOf("delivery","drive","completion")}.sortedWith(compareBy({it.ownerId},{it.key})),
            photos=s.photos.sortedBy {it.id}.map {it.copy(syncStatus="",driveFolderId=null,driveFileId=null)})}.toString()
        else->{val s=db.pavementDao().sample(i.recordId);val d=db.pavementDao().damages(i.recordId)
            s.toString()+d.toString()+d.map {damage->db.pavementDao().photos(damage.id).map {photo->
                // Preserve the schema-18 fingerprint of unmodified pending snapshots after adding history columns.
                "PavementDamagePhotoEntity(id=${photo.id}, damageId=${photo.damageId}, localPath=${photo.localPath}, createdAt=${photo.createdAt})"
            }}.toString()+db.pavementDao().observeSections().first().filter {it.ruta==s?.ruta && it.tramo==s.tramo && it.surface==s.surface}.toString()}
    }
    suspend fun current(db:InventoryDatabase,i:DriveSyncIntent,p:JSONObject)=
        db.automaticSyncDao().get(i.key)?.let {it.revision==i.revision && it.syncedRevision==p.getLong("baseRevision")}==true && validated(db,i) && sha256(source(db,i).toByteArray())==p.getString("sourceHash")

    suspend fun prepare(c:Context,db:InventoryDatabase,i:DriveSyncIntent,readable:Boolean=BuildConfig.GBO_PHOTO_NAMES_V2_ENABLED,persist:Boolean=true):JSONObject {
        if(!validated(db,i))throw SyncPending("Valida o confirma el cierre antes de enviar.")
        val directory=File(c.filesDir,"automatic_drive/${sha256(i.key.toByteArray())}/${i.revision}").apply {check(isDirectory || mkdirs())}
        val cache=File(directory,"prepared-path4.json")
        if(cache.isFile) {
            val stored=JSONObject(cache.readText())
            if(db.automaticSyncDao().get(i.key)?.revision==i.revision && validated(db,i) && sha256(source(db,i).toByteArray())==stored.getString("sourceHash")) {
                stored.put("baseRevision",i.syncedRevision);cache.writeText(stored.toString());return stored
            }
            throw SyncPending("La ficha cambió; guarda y valida la revisión actual.")
        }
        fun photoName(photo:com.tuempresa.inventariovial.data.entity.PhotoEntity)=if(readable)ReadablePhotoNames.name(c,photo) else StablePhotoNames.name(c,photo,BuildConfig.GBO_PHOTO_NUMBERS_ENABLED)
        val files=JSONArray();val rows=JSONArray()
        val p=JSONObject().put("action","commitRecordV3").put("syncProtocol",3).put("pathVersion",4)
            .put("key",i.key).put("kind",i.kind).put("recordId",i.recordId).put("revision",i.revision)
            .put("baseRevision",i.syncedRevision).put("origin",i.origin).put("warningsConfirmed",i.warningsConfirmed)
            .put("files",files).put("sicRows",rows).put("removeMissingOwnedFiles",true)
        if(readable)p.put("photoNamingVersion",2)
        fun add(key:String,path:String,mime:String,bytes:ByteArray,aggregate:Boolean=false) {
            require(path.split('/').all {FileNameComplianceValidator.safeComponent(it) && it.length<=80}) {"Nombre de entrega inválido: $path"}
            require(bytes.isNotEmpty()) {"Archivo vacío: $path"}
            val copy=File(directory,"file_${sha256(key.toByteArray())}");copy.writeBytes(bytes)
            files.put(JSONObject().put("key",key).put("path",path).put("mimeType",mime)
                .put("sha256",sha256(bytes)).put("localFile",copy.absolutePath).put("aggregate",aggregate))
        }
        when(i.kind) {
            "SIC"->{
                val s=requireNotNull(db.inventoryDao().snapshot(i.recordId))
                val r=s.record
                p.put("photoCategoryVersion",1).put("photoHasEnds",!r.endPrCode.isNullOrBlank() && r.endDistanceM!=null)
                    .put("photoClassCode",s.sic21?.classCode ?: "").put("photoTypeCode",s.sic21?.typeCode ?: "")
                val source:com.tuempresa.inventariovial.data.entity.InventorySnapshot? = null
                val (axis,orderSource)=technicalOrder(c,r)
                val geo=georeferencing(r)
                val item=RouteDelivery.row(s);val asset=RoadOrdering.orderAssetsForRoute(listOf(item)).single()
                if(asset.assetFamily==AssetFamily.OTROS)throw SyncPending("Pendiente de clasificación de familia.")
                p.put("routeCode",asset.routeCode).put("familyPath",DriveInventorySchema.familyPath(asset.assetFamily))
                    .put("family",DriveInventorySchema.elementLabel(asset.assetFamily)).put("orderAxis",axis)
                    .put("orderSource",orderSource).put("georeferencing",geo)
                    .put("technicalDeliveryWithoutGps",orderSource=="CONTRACTUAL_PR" || geo.getString("status")=="PENDING_EXTERNAL")
                val record=requireNotNull(db.inventoryDao().recordsForExport(null,r.routeCode).singleOrNull {it.record.id==r.id})
                val lengthPrs=com.tuempresa.inventariovial.road.FieldPrRepository(db.inventoryDao()).merge(RoadReferenceRepository(c).loadFromAssets()).prs
                val length=if(TechnicalLengthExport.applies(record))TechnicalLengthExport.metadata(record,lengthPrs) else null
                if(length!=null) {
                    val bytes=java.io.ByteArrayOutputStream()
                    TechnicalLengthExport.write(bytes,record,lengthPrs)
                    add("technical-length","longitud.xlsx",SicExcelWriter.XLSX_MIME,bytes.toByteArray())
                }
                record.sic17?.bridgeCode?.let {p.put("bridgeCode",it)}
                val formats=SicExportFormat.entries.filter {it.appliesTo(record)}
                formats.forEach {format->
                    val f=File(directory,"${format.code}.xlsx")
                    f.outputStream().use {SicExcelWriter.write(it,listOf(record),format,ExportHeading())}
                    rows.put(JSONObject().put("recordId",r.id).put("format",format.code).put("orderAxis",axis).put("orderSource",orderSource)
                        .put("base64",Base64.encodeToString(f.readBytes(),Base64.NO_WRAP)).put("sha256",sha256(f.readBytes())))
                }
                if(r.sicCode=="SIC-17") {
                    val pending=JSONArray()
                    for(format in listOf("SIC-17A","SIC-17B")) {
                        val preview=ScapSicExporter.supplement(s,format)
                        if(preview.missingRequired.isEmpty()) {
                            val bytes=java.io.ByteArrayOutputStream()
                            SicExcelWriter.writeMappedRow(bytes,format,r.routeCode,preview.columns,preview.values,if(format=="SIC-17A")setOf(7,8) else emptySet())
                            rows.put(JSONObject().put("recordId",r.id).put("format",format).put("orderAxis",axis).put("orderSource",orderSource)
                                .put("base64",Base64.encodeToString(bytes.toByteArray(),Base64.NO_WRAP)).put("sha256",sha256(bytes.toByteArray())))
                        }else pending.put(format+": "+preview.missingRequired.joinToString())
                    }
                    p.put("pendingSic",pending)
                }
                s.photos.filter {it.scapInspectionId==null}.sortedBy {it.photoIndex}.forEachIndexed {n,photo->
                    val name=photoName(photo)
                    val target=File(directory,name)
                    DeliveryJpeg.copy(File(photo.originalPath.ifBlank {photo.localPath}),target,photo)
                    add("photo/${photo.id}","${DriveInventorySchema.category(photo.photoCategory)}/$name","image/jpeg",target.readBytes())
                    files.getJSONObject(files.length()-1).put("photoCategory",photo.photoCategory ?: "GENERAL")
                }
                add("metadata",if(source!=null) "W_${sha256(r.id.toByteArray()).take(8)}.json" else "metadata.json","application/json",JSONObject().put("recordId",r.id).put("revision",i.revision)
                    .put("routeCode",r.routeCode).put("sicCode",r.sicCode).put("pr",r.startPrCode).put("distanceM",r.startDistanceM)
                    .put("latitude",r.latitude ?: JSONObject.NULL).put("longitude",r.longitude ?: JSONObject.NULL).put("surveyDate",r.surveyDate)
                    .put("endLatitude",r.endLatitude ?: JSONObject.NULL).put("endLongitude",r.endLongitude ?: JSONObject.NULL)
                    .put("georeferencing",geo).put("orderSource",orderSource).put("technicalValidationStatus","VALIDATED")
                    .put("pathVersion",4).put("familyPath",p.getString("familyPath")).put("identityId",r.id)
                    .put("sourceRecordId",s.sicExtension?.sourceRecordId ?: JSONObject.NULL)
                    .put("observations",r.observations ?: JSONObject.NULL)
                    .put("technicalLength",length ?: JSONObject.NULL)
                    .put("photoChanges",JSONArray(db.inventoryDao().photoHistory(r.id).map {h->JSONObject().put("photoId",h.photoId).put("revision",h.revision).put("action",h.action)}))
                    .put("photos",JSONArray(s.photos.filter {it.scapInspectionId==null}.map {photo->JSONObject()
                        .put("id",photo.id).put("recordId",r.id).put("sicCode",r.sicCode).put("roadbedCode",r.roadbedCode)
                        .put("order",photo.photoIndex).put("categoryOrder",s.photos.filter {it.scapInspectionId==null && (it.photoCategory ?: "GENERAL")== (photo.photoCategory ?: "GENERAL")}.sortedBy {it.photoIndex}.indexOfFirst {it.id==photo.id}+1)
                        .put("category",photo.photoCategory ?: "GENERAL").put("revision",i.revision)
                        .put("nameSource",if(readable)"SERVER_RESERVED_UUID" else "LOCAL_UUID_SEQUENCE").put("contractualLabelStatus","PENDING_EXTERNAL_DATABASE")
                        .put("fileKey","photo/${photo.id}").put("previousFileId",photo.driveFileId ?: JSONObject.NULL)
                        .put("path","${DriveInventorySchema.category(photo.photoCategory)}/${photoName(photo)}")
                        .put("capturedAt",photo.capturedAt ?: JSONObject.NULL)
                        .put("latitude",photo.photoLatitude ?: JSONObject.NULL).put("longitude",photo.photoLongitude ?: JSONObject.NULL)
                        .put("accuracyM",photo.photoGpsAccuracyM ?: JSONObject.NULL)})).toString().toByteArray())
            }
            "SCAP"->{
                val s=requireNotNull(db.scapDao().snapshot(i.recordId))
                p.put("photoCategoryVersion",1).put("photoHasEnds",false).put("photoClassCode","").put("photoTypeCode","")
                ScapDriveSync.reviewForUpload(s,ScapCatalog.load(c).forSnapshot(s),i.warningsConfirmed)
                val layout=ScapHumanDelivery.prepare(c,db,s)
                val r=requireNotNull(s.roadRecord)
                val located=r.copy(latitude=s.inspection.latitude ?: r.latitude,longitude=s.inspection.longitude ?: r.longitude)
                val (axis,orderSource)=technicalOrder(c,located)
                p.put("identityId",s.inspection.roadRecordId).put("bridgeCode",s.inspection.bridgeCode)
                    .put("routeCode",layout.routeCode).put("familyPath","PUENTES").put("family","PUENTE")
                    .put("orderAxis",axis).put("orderSource",orderSource).put("georeferencing",georeferencing(located))
                    .put("technicalDeliveryWithoutGps",orderSource=="CONTRACTUAL_PR" || located.latitude==null || located.longitude==null)
                val bundle=ScapDelivery.bundle(c,s,layout)
                ZipInputStream(bundle.inputStream()).use {zip->
                    while(true) {
                        val e=zip.nextEntry ?: break
                        if(e.isDirectory)continue
                        val metadata=if(e.name.startsWith("fotos/")) {
                            val photoId=layout.photoPaths.entries.single {it.value==e.name}.key
                            val photo=s.photos.single {it.id==photoId}
                            "SCAP/${s.inspection.id}/"+DriveInventorySchema.category(photo.photoCategory)+"/${photoName(photo)}"
                        } else "SCAP/${s.inspection.id}/"+e.name
                        val key=when {e.name.startsWith("fotos/")->"photo/"+layout.photoPaths.entries.single {it.value==e.name}.key
                            e.name.startsWith("croquis/")->"sketch/"+layout.sketchPaths.entries.single {it.value==e.name}.key
                            else->e.name}
                        add(key,metadata,when {e.name.endsWith(".jpg")->"image/jpeg";e.name.endsWith(".png")->"image/png";e.name.endsWith(".xlsx")->SicExcelWriter.XLSX_MIME;else->"application/json"},if(e.name=="manifest.json")JSONObject(zip.readBytes().toString(Charsets.UTF_8)).apply {remove("logicalPath");remove("delivery");put("pathVersion",4);put("familyPath","PUENTES");put("pathsRelativeTo","BRIDGE_FOLDER");put("destinationAuthority","SERVER_ACK");put("revision",i.revision)
                            optJSONArray("files")?.let { entries -> for(n in 0 until entries.length()) {
                                val f=entries.getJSONObject(n);val oldPath=f.optString("path")
                                if(oldPath.startsWith("fotos/")) {
                                    val photo=s.photos.single {layout.photoPaths[it.id]==oldPath}
                                    f.put("path","SCAP/${s.inspection.id}/"+DriveInventorySchema.category(photo.photoCategory)+"/${photoName(photo)}")
                                } else f.put("path","SCAP/${s.inspection.id}/"+oldPath)
                            } }
}.toString().toByteArray() else zip.readBytes())
                    }
                }
                for(n in 0 until files.length()) {
                    val entry=files.getJSONObject(n)
                    if(entry.getString("key").startsWith("photo/")) entry.put("photoCategory",s.photos.single {"photo/${it.id}"==entry.getString("key")}.photoCategory ?: "GENERAL")
                }
                ScapSicExporter.previews(s).filter {it.missingRequired.isEmpty()}.forEach {preview->
                    val f=File(directory,"${preview.format}.xlsx")
                    f.outputStream().use {ScapSicExporter.write(it,s,preview.format)}
                    rows.put(JSONObject().put("recordId",r.id).put("bridgeCode",s.inspection.bridgeCode).put("format",preview.format)
                        .put("orderAxis",p.getDouble("orderAxis")).put("orderSource",orderSource).put("base64",Base64.encodeToString(f.readBytes(),Base64.NO_WRAP)).put("sha256",sha256(f.readBytes())))
                }
                p.put("pendingSic",JSONArray(ScapSicExporter.previews(s).filter {it.missingRequired.isNotEmpty()}.map {it.format+": "+it.missingRequired.joinToString()}))
            }
            "PAVEMENT"->{
                val s=requireNotNull(db.pavementDao().sample(i.recordId));val damages=db.pavementDao().damages(s.id)
                val photos=damages.associate {it.id to db.pavementDao().photos(it.id)}
                val result=PavementCalculator(c).calculate(s,damages)
                val data=PavementExportSample(s,damages,result)
                val surface=when(s.surface) {"ASPHALT"->"ASFALTICO";"RIGID"->"RIGIDO";else->"NO_PAVIMENTADO"}
                p.put("routeCode",s.ruta).put("tramo",s.tramo).put("surface",surface).put("family","MUESTRA")
                val pdf=File(directory,"HC_${s.muestra.toString().padStart(3,'0')}.pdf")
                pdf.outputStream().use {PavementPdf.write(it,data,photos)}
                add("field","HOJAS_CAMPO/${pdf.name}","application/pdf",pdf.readBytes())
                val excel=File(directory,"M_${s.muestra.toString().padStart(3,'0')}.xlsx")
                excel.outputStream().use {PavementExcel.write(it,listOf(data),db.pavementDao().observeSections().first())}
                // Sample-scoped names are assigned centrally to prevent tablet counters colliding.
                add("sample_excel","DATOS_APP/${excel.name}",SicExcelWriter.XLSX_MIME,excel.readBytes())
                add("sample_json","DATOS_APP/M_${s.muestra.toString().padStart(3,'0')}.json","application/json",PavementDriveUploader.nativeJson(s,damages,result,photos).toString().toByteArray())
                var n=0
                photos.entries.sortedBy {it.key}.forEach {(_,items)->items.sortedBy {it.createdAt}.forEach {photo->
                    n++;val name=if(readable)ReadablePhotoNames.name(c,photo.id,"GENERAL") else "%03d_F%02d.jpg".format(n,n);val f=File(directory,name)
                    DeliveryJpeg.copy(File(photo.localPath),f)
                    add("photo/${photo.id}","PANEL_FOTOGRAFICO/$name","image/jpeg",f.readBytes())
                    files.getJSONObject(files.length()-1).put("photoCategory","GENERAL")
                }}
                PavementSicFormat.entries.filter {it.surface==s.surface}.forEach {format->
                    if(PavementSic.status(s,damages,format).complete) {
                        for(damage in damages) {
                            val status=PavementSic.status(s,listOf(damage),format)
                            val row=status.rows.single();val values=row.values
                            val f=File(directory,"${format.label}_${damage.id}.xlsx")
                            f.outputStream().use {PavementSic.write(it,s,listOf(damage),format)}
                            rows.put(JSONObject().put("recordId",s.id+"_"+damage.id).put("format",format.label)
                                .put("roadbedCode",s.calzada).put("direction","INCREASING")
                                .put("startPrNumber",values[3].toString().toInt()).put("startDistanceM",values[4])
                                .put("orderAxis",values[3].toString().toInt()*1000.0+(values[4] as Number).toDouble()).put("orderSource","CONTRACTUAL_PR")
                                .put("base64",Base64.encodeToString(f.readBytes(),Base64.NO_WRAP)).put("sha256",sha256(f.readBytes())))
                        }
                    }
                }
            }
        }
        if(i.kind!="PAVEMENT") {
            val r=if(i.kind=="SIC")requireNotNull(db.inventoryDao().recordById(i.recordId)) else requireNotNull(db.scapDao().snapshot(i.recordId)?.roadRecord)
            p.put("technicalDeliveryVersion",1).put("roadbedCode",r.roadbedCode)
                .put("direction",InventoryTechnicalOrder.direction(r)).put("startPrNumber",r.startPrCode.toInt())
                .put("startDistanceM",r.startDistanceM)
            for(n in 0 until rows.length()) rows.getJSONObject(n).put("roadbedCode",r.roadbedCode)
                .put("direction",InventoryTechnicalOrder.direction(r)).put("startPrNumber",r.startPrCode.toInt()).put("startDistanceM",r.startDistanceM)
        }
        p.put("sourceHash",sha256(source(db,i).toByteArray()))
        if(!readable && i.kind in setOf("SIC","SCAP") && (BuildConfig.GBO_PHOTO_NUMBERS_ENABLED || (0 until files.length()).any {files.getJSONObject(it).optString("path").substringAfterLast('/').matches(Regex("[0-9]{3,10}_F_[a-f0-9]{16}\\.jpg"))})) p.put("photoNamingVersion",1)
        val content=contentHash(p)
        p.put("contentHash",content)
        if(!persist)return p
        val atomic=android.util.AtomicFile(cache);val out=atomic.startWrite()
        try {out.write(p.toString().toByteArray());atomic.finishWrite(out)}catch(e:Exception){atomic.failWrite(out);throw e}
        return p
    }
    fun contentHash(p:JSONObject):String {
        fun value(row:JSONObject,key:String):String = row.opt(key).let {if(it is Number)JSONObject.numberToString(it) else row.optString(key)}
        fun lines(array:JSONArray,keys:List<String>)=(0 until array.length()).map {n->
            val row=array.getJSONObject(n);keys.joinToString("\t"){value(row,it)}}.sorted().joinToString("\n")
        val header=listOf("key","revision","routeCode","familyPath","family","tramo","surface","identityId","roadbedCode","direction","startPrNumber","startDistanceM").joinToString("\t"){value(p,it)}
        val photos=if(p.optInt("photoCategoryVersion")==1) "\nphotoCategoryVersion=1\n"+listOf("photoHasEnds","photoClassCode","photoTypeCode").joinToString("\t"){value(p,it)}+"\n"+lines(p.getJSONArray("files"),listOf("key","photoCategory")) else ""
        val technical=if(p.optInt("technicalDeliveryVersion")==1) "\n"+listOf("technicalDeliveryVersion","orderAxis","orderSource","technicalDeliveryWithoutGps").joinToString("\t"){value(p,it)}+
            "\t"+p.getJSONObject("georeferencing").getString("status")+"\t"+p.getJSONObject("georeferencing").getString("externalSubmetricStatus")+
            "\n"+lines(p.getJSONArray("sicRows"),listOf("recordId","format","orderAxis","orderSource")) else ""
        return sha256((header+"\n"+lines(p.getJSONArray("files"),listOf("key","path","mimeType","sha256"))+"\n"+
            lines(p.getJSONArray("sicRows"),listOf("recordId","format","sha256"))+technical+"\npathVersion=4\n"+lines(p.getJSONArray("sicRows"),listOf("recordId","format","roadbedCode","direction","startPrNumber","startDistanceM"))+photos+(if(p.has("photoNamingVersion")) "\nphotoNamingVersion=${p.getInt("photoNamingVersion")}" else "")).toByteArray())
    }
    fun validAck(p:JSONObject,a:JSONObject,previous:JSONObject?=null):Boolean {
        if(previous?.optInt("pathVersion")==4) {
            if(a.optString("folderId")!=previous.optString("folderId") || a.optString("logicalPath")!=previous.optString("logicalPath"))return false
            for(arrayName in listOf("files","sicFiles")) {
                val old=previous.optJSONArray(arrayName) ?: continue
                val next=a.optJSONArray(arrayName) ?: return false
                val field=if(arrayName=="files") "key" else "format"
                for(n in 0 until next.length()) {
                    val item=next.optJSONObject(n) ?: return false
                    for(k in 0 until old.length()) {
                        val before=old.optJSONObject(k) ?: return false
                        if(item.optString(field)==before.optString(field) && item.optString("fileId")!=before.optString("fileId"))return false
                    }
                }
            }
        }
        if(!a.optBoolean("ok") || a.optInt("syncProtocol")!=3 || a.optInt("pathVersion")!=4 ||
            a.optString("key")!=p.getString("key") || a.optLong("revision")!=p.getLong("revision") ||
            a.optString("contentHash")!=p.getString("contentHash") || a.optString("folderId").isBlank())return false
        if(p.optInt("technicalDeliveryVersion")==1 &&
            (a.optString("technicalDeliveryStatus")!="DELIVERED" ||
                a.optString("georeferencingStatus")!=p.getJSONObject("georeferencing").getString("status") ||
                a.optString("externalGeoreferencingStatus")!="PENDING"))return false
        val expectedPrefix=if(p.getString("kind")=="PAVEMENT") "${p.getString("routeCode")}/EVALUACION_SUPERFICIAL/${p.getString("tramo")}/${p.getString("surface")}" else "${p.getString("routeCode")}/${p.getString("familyPath")}/"
        if(if(p.getString("kind")=="PAVEMENT") !a.optString("logicalPath").matches(Regex(Regex.escape(expectedPrefix)+"/MUESTRA [1-9][0-9]*")) else !a.optString("logicalPath").startsWith(expectedPrefix))return false
        if(p.getString("kind")!="PAVEMENT" && !a.optString("logicalPath").matches(Regex(Regex.escape(expectedPrefix+p.getString("family"))+" [1-9][0-9]*")))return false
        val files=a.optJSONArray("files") ?: return false;val expected=p.getJSONArray("files")
        val ack=(0 until files.length()).map {files.optJSONObject(it) ?: return false}
        if(ack.map {it.optString("key")}.toSet().size!=ack.size || ack.size!=expected.length())return false
        if(!(0 until expected.length()).all {n->val e=expected.getJSONObject(n);val r=ack.singleOrNull {it.optString("key")==e.getString("key")}
            r!=null && r.optString("sha256")==e.getString("sha256") && r.optString("fileId").isNotBlank() && r.optString("folderId").isNotBlank() &&
                r.optString("requestedPath")==e.getString("path") && (if(p.getString("kind")=="PAVEMENT") r.optString("logicalPath").startsWith(a.getString("logicalPath")+"/") else r.optString("logicalPath")==a.getString("logicalPath")+"/"+e.getString("path"))})return false
        val aggregates=a.optJSONArray("sicFiles") ?: return false
        val rows=p.getJSONArray("sicRows")
        return (0 until rows.length()).all {n->val e=rows.getJSONObject(n)
            (0 until aggregates.length()).any {k->val f=aggregates.getJSONObject(k)
                f.optString("format")==e.getString("format") && f.optString("fileId").isNotBlank() &&
                    (p.optInt("technicalDeliveryVersion")!=1 || f.optString("folderId").isNotBlank() && f.optString("sha256").matches(Regex("[a-f0-9]{64}"))) &&
                    f.optString("logicalPath")=="${p.getString("routeCode")}/EXCEL_SIC/${e.getString("format")}.xlsx" &&
                    f.optJSONArray("recordIds")?.let {ids->(0 until ids.length()).any {ids.getString(it)==e.getString("recordId")}}==true}}
    }
}
