package com.tuempresa.inventariovial.localfiles

import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

interface SharedDocumentStore {
    data class Slot(val uri:Uri,val published:Boolean)
    fun reserve(file:LocalSharedFile,knownUri:Uri?=null):Slot
    fun checkSlot(slot:Slot,file:LocalSharedFile) {}
    fun write(uri:Uri,source:File)
    fun digest(uri:Uri):Pair<Long,String>
    fun publish(slot:Slot,file:LocalSharedFile):Uri
}

fun streamDigest(input:InputStream):Pair<Long,String> = input.use { stream->
    val digest=MessageDigest.getInstance("SHA-256");val buffer=ByteArray(64*1024);var size=0L
    while(true){val n=stream.read(buffer);if(n<0)break;size+=n;digest.update(buffer,0,n)}
    size to digest.digest().joinToString(""){"%02x".format(it)}
}
fun fileDigest(file:File)=streamDigest(file.inputStream())

/** URI only. Never turns content:// into a fictitious java.io.File. */
class AndroidSharedDocumentStore(private val context:Context):SharedDocumentStore {
    private val resolver=context.contentResolver
    private val prefs=context.getSharedPreferences("gbo_local_copies",Context.MODE_PRIVATE)
    private val media get()=Build.VERSION.SDK_INT>=29
    private val collection get()=MediaStore.Downloads.EXTERNAL_CONTENT_URI
    private fun findMedia(file:LocalSharedFile):SharedDocumentStore.Slot? {
        val result=mutableListOf<SharedDocumentStore.Slot>()
        val projection=arrayOf(MediaStore.MediaColumns._ID,MediaStore.MediaColumns.IS_PENDING,MediaStore.MediaColumns.OWNER_PACKAGE_NAME)
        val selection="${MediaStore.MediaColumns.RELATIVE_PATH}=? AND ${MediaStore.MediaColumns.DISPLAY_NAME}=?"
        val args=arrayOf(file.relativePath,file.name)
        val cursor=if(Build.VERSION.SDK_INT>=30)resolver.query(collection,projection,android.os.Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION,selection);putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,args)
            putInt(MediaStore.QUERY_ARG_MATCH_PENDING,MediaStore.MATCH_INCLUDE)
        },null)else {
            @Suppress("DEPRECATION") val pending=MediaStore.setIncludePending(collection)
            resolver.query(pending,projection,selection,args,null)
        }
        cursor?.use {c->
            while(c.moveToNext()) {
                require(c.getString(2)==context.packageName){"Existe un archivo de otro propietario con ese nombre. No se sobrescribió."}
                result+=SharedDocumentStore.Slot(ContentUris.withAppendedId(collection,c.getLong(0)),c.getInt(1)==0)
            }
        }
        require(result.size<=1){"Hay varias copias con el mismo nombre; revisa Descargas antes de reintentar."}
        return result.singleOrNull()
    }
    private fun mediaQuery(uri:Uri,projection:Array<String>):android.database.Cursor? = if(Build.VERSION.SDK_INT>=30)
        resolver.query(uri,projection,android.os.Bundle().apply{putInt(MediaStore.QUERY_ARG_MATCH_PENDING,MediaStore.MATCH_INCLUDE)},null)
        else {@Suppress("DEPRECATION") val pending=MediaStore.setIncludePending(uri);resolver.query(pending,projection,null,null,null)}
    override fun reserve(file:LocalSharedFile,knownUri:Uri?):SharedDocumentStore.Slot {
        if(!media)return reserveTree(file)
        if(knownUri!=null) {
            val prior=mediaQuery(knownUri,arrayOf(MediaStore.MediaColumns.IS_PENDING,MediaStore.MediaColumns.OWNER_PACKAGE_NAME))?.use{c->
                if(c.moveToFirst()){require(c.getString(1)==context.packageName){"La copia no pertenece a GBO."};SharedDocumentStore.Slot(knownUri,c.getInt(0)==0)}else null
            }
            if(prior!=null)return prior
        }
        findMedia(file)?.let{return it}
        val values=ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME,file.name);put(MediaStore.MediaColumns.MIME_TYPE,file.mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH,file.relativePath);put(MediaStore.MediaColumns.IS_PENDING,1)
        }
        val uri=requireNotNull(resolver.insert(collection,values)){"No se pudo reservar el archivo en Descargas."}
        return SharedDocumentStore.Slot(uri,false)
    }
    override fun checkSlot(slot:SharedDocumentStore.Slot,file:LocalSharedFile){
        if(!media)return
        mediaQuery(slot.uri,arrayOf(MediaStore.MediaColumns.DISPLAY_NAME,MediaStore.MediaColumns.RELATIVE_PATH))?.use{c->
            check(c.moveToFirst()&&c.getString(0)==file.name&&c.getString(1)==file.relativePath){"Android cambió la ubicación/nombre solicitado. La copia no se marcó completa."}
        } ?: error("Android no confirmó el nombre del archivo reservado.")
    }
    override fun write(uri:Uri,source:File) {
        requireNotNull(resolver.openOutputStream(uri,"wt")){"Android denegó la escritura del documento."}.use {out->source.inputStream().buffered().use {it.copyTo(out,64*1024)}}
    }
    override fun digest(uri:Uri)=streamDigest(requireNotNull(resolver.openInputStream(uri)){"El documento ya no está disponible."})
    override fun publish(slot:SharedDocumentStore.Slot,file:LocalSharedFile):Uri {
        if(slot.published)return slot.uri
        if(media){check(resolver.update(slot.uri,ContentValues().apply{put(MediaStore.MediaColumns.IS_PENDING,0)},null,null)==1){"Android no confirmó la publicación en Descargas."};return slot.uri}
        return requireNotNull(DocumentsContract.renameDocument(resolver,slot.uri,file.name)){"El proveedor no permite finalizar la copia. El original se conserva."}
    }
    private fun children(parent:Uri):List<Pair<Uri,Pair<String,String>>> {
        val uri=DocumentsContract.buildChildDocumentsUriUsingTree(parent,DocumentsContract.getDocumentId(parent))
        val rows=mutableListOf<Pair<Uri,Pair<String,String>>>()
        resolver.query(uri,arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE),null,null,null)?.use {c->
            while(c.moveToNext())rows+=DocumentsContract.buildDocumentUriUsingTree(parent,c.getString(0)) to (c.getString(1) to c.getString(2))
        };return rows
    }
    private fun reserveTree(file:LocalSharedFile):SharedDocumentStore.Slot {
        val saved=prefs.getString("legacyTree",null) ?: error("Android 8/9: elige una carpeta local en Herramientas → Copias locales. La ficha sigue dentro de GBO.")
        val tree=Uri.parse(saved);require(tree.authority=="com.android.externalstorage.documents"){"Elige almacenamiento de la tablet, no un proveedor en Internet."}
        require(resolver.persistedUriPermissions.any {it.uri==tree&&it.isReadPermission&&it.isWritePermission}){"Vuelve a autorizar la carpeta local en Herramientas."}
        var parent=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree))
        // The chosen directory is a base; never infer its filesystem path.
        for(name in file.relativePath.removePrefix(Environment.DIRECTORY_DOWNLOADS+"/").split('/').filter{it.isNotBlank()}) {
            val found=children(parent).filter{it.second.first==name};require(found.size<=1){"Carpeta duplicada en el destino."}
            parent=found.singleOrNull()?.also {require(it.second.second==DocumentsContract.Document.MIME_TYPE_DIR)}?.first
                ?: requireNotNull(DocumentsContract.createDocument(resolver,parent,DocumentsContract.Document.MIME_TYPE_DIR,name))
        }
        val files=children(parent);val final=files.filter{it.second.first==file.name};require(final.size<=1)
        final.singleOrNull()?.let{return SharedDocumentStore.Slot(it.first,true)}
        val partialName=".${file.name}.gbo_partial";val partial=files.filter{it.second.first==partialName};require(partial.size<=1)
        return SharedDocumentStore.Slot(partial.singleOrNull()?.first ?: requireNotNull(DocumentsContract.createDocument(resolver,parent,file.mimeType,partialName)),false)
    }
}

object SharedFileActions {
    fun view(uri:Uri,mime:String)=Intent(Intent.ACTION_VIEW).setDataAndType(uri,mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).apply{clipData=ClipData.newRawUri("Documento GBO",uri)}
    fun share(uri:Uri,mime:String)=Intent.createChooser(Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM,uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).apply{clipData=ClipData.newRawUri("Documento GBO",uri)},"Compartir archivo").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    fun downloads()=Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
