package com.tuempresa.inventariovial.scap.export

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.AtomicFile
import com.tuempresa.inventariovial.scap.data.*
import com.tuempresa.inventariovial.scap.domain.*
import org.json.*
import java.io.File
import java.util.zip.*

/** A separate field-work document. Never enters sicRows or a contractual consolidated workbook. */
object ScapFieldBackup {
    const val STATUS="PROVISIONAL / INCOMPLETO / NO VALIDADO"
    private fun row(value:Any)=ScapCaptureRows.row(value)
    /** Explicit entity allowlist; no auth graph, preferences, credentials or remote URLs. */
    fun capture(s:ScapInspectionSnapshot):JSONObject=JSONObject().put("status",STATUS)
        .put("inspection",row(s.inspection)).put("roadRecord",s.roadRecord?.let(::row) ?: JSONObject.NULL)
        .put("values",JSONArray(s.values.filter{it.ownerId !in setOf("drive","delivery")}.sortedWith(compareBy({it.ownerId},{it.key})).map(::row)))
        .put("spans",JSONArray(s.spans.map(::row))).put("substructures",JSONArray(s.substructures.map(::row)))
        .put("supports",JSONArray(s.supports.map(::row))).put("joints",JSONArray(s.joints.map(::row)))
        .put("elements",JSONArray(s.elements.map{JSONObject().put("element",row(it.element)).put("condition",it.condition?.let(::row) ?: JSONObject.NULL)}))
        .put("defects",JSONArray(s.defects.map(::row))).put("profile",JSONArray(s.profile.map(::row)))
        .put("photos",JSONArray(s.photos.map(::row))).put("sketches",JSONArray(s.sketches.map(::row)))
        .put("sic17a",s.sic17a?.let(::row) ?: JSONObject.NULL).put("sic17b",s.sic17b?.let(::row) ?: JSONObject.NULL)

    private fun digest(input:java.io.InputStream):Pair<Long,String> {
        val h=java.security.MessageDigest.getInstance("SHA-256");val b=ByteArray(64*1024);var size=0L
        input.use{while(true){val n=it.read(b);if(n<0)break;h.update(b,0,n);size+=n}}
        return size to h.digest().joinToString(""){"%02x".format(it)}
    }
    fun write(context:Context,s:ScapInspectionSnapshot,format:String,target:File) {
        require(format in BridgeSicContract.formats)
        val mapping=when(format){"SIC-17"->ScapToSicMapper.mapToSic17(s);"SIC-17A"->ScapToSicMapper.mapToSic17A(s);else->ScapToSicMapper.mapToSic17B(s)}
        val preview=ScapSicExporter.previews(s).single{it.format==format}
        val pending=(preview.missingRequired+preview.pending+ScapSketchSelection.warnings(s)).distinct()
        val problems=mutableListOf<String>();val files=JSONArray();var bytes=0L
        val atomic=AtomicFile(target);target.parentFile!!.mkdirs();val out=atomic.startWrite()
        try {
            ZipOutputStream(object:java.io.FilterOutputStream(out){override fun close(){flush()}}).use{zip->
                fun entry(name:String,data:ByteArray){bytes+=data.size;require(bytes<=512L*1024*1024){"El paquete excede 512 MiB."};zip.putNextEntry(ZipEntry(name).apply{time=0});zip.write(data);zip.closeEntry();files.put(JSONObject().put("path",name).put("sha256",ScapDelivery.sha256(data)).put("size",data.size))}
                entry("INSPECCION_PROVISIONAL.json",capture(s).put("requestedFormat",format).put("missingRequired",JSONArray(preview.missingRequired)).put("pending",JSONArray(pending)).toString(2).toByteArray())
                fun media(id:String,location:String,name:String,image:Boolean){
                    if(location.isBlank()){problems+="Archivo sin imagen: $id";return}
                    fun stream()=if(location.startsWith("content:"))requireNotNull(context.contentResolver.openInputStream(Uri.parse(location))) else File(location.removePrefix("file://")).inputStream()
                    val before=try {
                        if(image){val b=BitmapFactory.Options().apply{inJustDecodeBounds=true};stream().use{BitmapFactory.decodeStream(it,null,b)};require(b.outWidth>0&&b.outHeight>0){"Imagen ilegible"}}
                        stream().use{digest(it)}
                    }catch(e:Exception){problems+="Archivo $id no exportado: ausente, ilegible o sin permiso; original conservado";return}
                    require(bytes+before.first<=512L*1024*1024){"El paquete excede 512 MiB"}
                    zip.putNextEntry(ZipEntry(name).apply{time=0});stream().use{it.copyTo(zip,64*1024)};zip.closeEntry();bytes+=before.first
                    // A failure after opening a ZIP entry aborts the whole atomic write.
                    require(stream().use{digest(it)}==before){"El original cambió durante el respaldo"}
                    files.put(JSONObject().put("path",name).put("sha256",before.second).put("size",before.first))
                }
                s.photos.sortedBy{it.id}.forEach{p->val path=p.originalPath.ifBlank{p.localPath};media(p.id,path,"FOTOGRAFIAS/${p.id}.${File(path).extension.ifBlank{"imagen"}}",true)}
                s.sketches.sortedBy{it.id}.forEach{media(it.id,it.localUri,"CROQUIS/${it.type}_${it.id}.${File(it.localUri).extension.takeIf{e->e.matches(Regex("[A-Za-z0-9]{1,5}"))} ?: "imagen"}",true)}
                fun esc(v:String)=v.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;")
                val fields=BridgeSicContract.fields(format,mapping.fields.associate{it.sicField to it.value})
                val html="""<!doctype html><meta charset="utf-8"><title>$format PROVISIONAL</title><h1>$format — $STATUS</h1><p>No es una entrega contractual. No contiene valores inventados. No restaura Room automáticamente.</p><p>UUID: ${esc(s.inspection.id)} · revisión: ${s.inspection.updatedAt} · autor: ${esc(s.inspection.createdBy)} · origen: GBO / SCAP · fecha de creación (epoch ms): ${s.inspection.createdAt}</p><h2>Pendientes</h2><ul>${(pending+problems).map{"<li>${esc(it)}</li>"}.joinToString("")}</ul><table border="1"><tr><th>Campo</th><th>Valor capturado/mapeado</th><th>Estado</th></tr>${fields.joinToString(""){f->val raw=mapping.fields.firstOrNull{it.sicField==f.key}?.value.orEmpty();"<tr><td>${esc(f.label)}</td><td>${esc(raw)}</td><td>${if(raw.isBlank())"PENDIENTE" else if(BridgeSicContract.valid(f,raw))"REGISTRADO; NO VALIDADO CONTRACTUALMENTE" else "VALOR A REVISAR"}</td></tr>"}}</table><p>INSPECCION_PROVISIONAL.json conserva además los valores originales, observaciones y relaciones.</p>"""
                entry("ABRIR_PROVISIONAL.html",html.toByteArray())
                val manifest=JSONObject().put("status",STATUS).put("contractual",false).put("complete",false).put("mediaVerified",problems.isEmpty()).put("missingFiles",JSONArray(problems)).put("pending",JSONArray(pending)).put("files",files).put("inspectionId",s.inspection.id).put("revision",s.inspection.updatedAt)
                zip.putNextEntry(ZipEntry("MANIFIESTO_PROVISIONAL.json").apply{time=0});zip.write(manifest.toString(2).toByteArray());zip.closeEntry()
            };atomic.finishWrite(out)
        }catch(e:Exception){atomic.failWrite(out);throw e}
    }
}
