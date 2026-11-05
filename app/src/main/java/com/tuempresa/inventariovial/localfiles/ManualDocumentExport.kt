package com.tuempresa.inventariovial.localfiles

import android.content.Context
import android.net.Uri
import android.util.AtomicFile
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.ZipFile

/** A frozen local document, independent of delivery choices, remote reservations and ACK. */
data class ManualDocument(val source:String,val name:String,val mime:String,val kind:String,
    val size:Long,val sha256:String,val revision:Long,val notice:String="",val ownerKey:String?=null) {
    fun json()=JSONObject().put("source",source).put("name",name).put("mime",mime).put("kind",kind)
        .put("size",size).put("sha256",sha256).put("revision",revision).put("notice",notice).put("ownerKey",ownerKey ?: JSONObject.NULL).toString()
    companion object {
        fun parse(text:String)=JSONObject(text).let{ManualDocument(it.getString("source"),it.getString("name"),
            it.getString("mime"),it.getString("kind"),it.getLong("size"),it.getString("sha256"),it.getLong("revision"),it.optString("notice"),it.optString("ownerKey").takeIf{v->v.isNotBlank()&&v!="null"})}
    }
}

object ManualDocumentExport {
    fun verifyXlsx(file:File) {
        ZipFile(file).use {zip->
            for(name in listOf("[Content_Types].xml","xl/workbook.xml","xl/worksheets/sheet1.xml"))
                require(zip.getEntry(name)!=null){"El archivo generado no es un XLSX completo. Original conservado."}
            val seen=mutableSetOf<String>();var total=0L
            for(entry in zip.entries()) {
                require(seen.add(entry.name)&&seen.size<=2000){"Estructura XLSX inválida."}
                if(entry.isDirectory)continue
                val crc=CRC32();var size=0L;val buffer=ByteArray(64*1024)
                zip.getInputStream(entry).use{input->while(true){val n=input.read(buffer);if(n<0)break
                    size+=n;total+=n;require(total<=256L*1024*1024){"XLSX demasiado grande para verificar."};crc.update(buffer,0,n)}}
                require(size==entry.size&&crc.value==entry.crc){"No se verificó la integridad del XLSX."}
            }
        }
    }
    /** Freeze before opening the picker: later edits/cancellation cannot change these bytes. */
    suspend fun freeze(c:Context,file:File,kind:String,mime:String,name:String,revision:Long=0,notice:String="",ownerKey:String?=null):ManualDocument=withContext(Dispatchers.IO) {
        require(name.isNotBlank()&&name.none{it=='/'||it=='\\'||it<' '})
        val digest=fileDigest(file);require(digest.first>0)
        if(mime==com.tuempresa.inventariovial.export.SicExcelWriter.XLSX_MIME)verifyXlsx(file)
        val copy=File(c.filesDir,"manual_documents/${UUID.randomUUID()}/${if(notice.startsWith("BORRADOR"))"BORRADOR/"else""}$name")
        check(copy.parentFile!!.isDirectory||copy.parentFile!!.mkdirs());val atomic=AtomicFile(copy);val out=atomic.startWrite()
        try{file.inputStream().use{it.copyTo(out)};atomic.finishWrite(out)}catch(e:Exception){atomic.failWrite(out);throw e}
        require(fileDigest(copy)==digest)
        ManualDocument(copy.path,name,mime,kind,digest.first,digest.second,revision,notice,ownerKey)
    }
    /** CreateDocument normally returns an empty new URI. Never overwrite nonempty unrelated bytes. */
    suspend fun save(c:Context,db:InventoryDatabase,document:ManualDocument,uri:Uri):Uri=withContext(Dispatchers.IO) {
        require(uri.scheme=="content"){"Selecciona un documento con el selector de Android."}
        val source=File(document.source);val expected=document.size to document.sha256
        require(fileDigest(source)==expected){"La copia preparada cambió; el original se conserva."}
        val store=AndroidSharedDocumentStore(c)
        val before=store.digest(uri) // An unreadable destination is not assumed empty.
        require(before.first==0L||before==expected){"El destino ya contiene otro archivo. Elige un nombre nuevo; no se sobrescribió."}
        if(before!=expected) {
            requireNotNull(c.contentResolver.openOutputStream(uri,"wt")).use{out->source.inputStream().use{it.copyTo(out,64*1024)}}
        }
        require(store.digest(uri)==expected){"La escritura quedó incompleta. Copia privada conservada; vuelve a elegir un archivo nuevo."}
        LocalCopies.rememberSaf(c,db,source,document.kind,document.mime,uri,document.ownerKey,document.revision.takeIf{it>0})
        uri
    }
}
