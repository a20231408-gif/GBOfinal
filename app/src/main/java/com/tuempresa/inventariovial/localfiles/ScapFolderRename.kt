package com.tuempresa.inventariovial.localfiles

import android.content.*
import android.net.Uri
import android.provider.MediaStore
import android.util.AtomicFile
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.delivery.sha256
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Local-only, verified MediaStore relocation. No filesystem paths, remote identity or deletion.
 * The durable intent precedes effects; a retry accepts only the exact old/new slot and bytes.
 */
object ScapFolderRename {
    private fun write(f:File,j:JSONObject){f.parentFile!!.mkdirs();val a=AtomicFile(f);val o=a.startWrite();try{o.write(j.toString().toByteArray());a.finishWrite(o)}catch(e:Exception){a.failWrite(o);throw e}}
    suspend fun move(c:Context,db:InventoryDatabase,owner:String,from:String,to:String) {
        require(owner.startsWith("SCAP/")&&from.startsWith(DriveMirrorPlan.ROOT)&&to.startsWith(DriveMirrorPlan.ROOT))
        val journal=File(c.filesDir,"scap_folder_moves/"+sha256((owner+from+to).toByteArray())+".json")
        var j=if(journal.isFile)JSONObject(AtomicFile(journal).openRead().use{it.readBytes().toString(Charsets.UTF_8)})else null
        val safFiles=db.localFilesDao().filesForRecord(owner).filter{it.state=="READY"&&it.relativePath.startsWith(from)&&it.uri!=null&&Uri.parse(it.uri).authority!="media"}
        if(safFiles.isNotEmpty())moveSaf(c,db,owner,from,to,safFiles)
        val files=db.localFilesDao().filesForRecord(owner).filter{it.state=="READY"&&it.relativePath.startsWith(from)&&it.uri!=null&&Uri.parse(it.uri).authority=="media"}
        if(j==null) {
            // Unknown historical SAF locations require the explicit folder workflow; never guess their owner.
            require(files.all{Uri.parse(it.uri).authority=="media"}){"Carpeta histórica SAF: vuelve a elegirla para verificarla; no se movió contenido."}
            val all=mutableSetOf<String>()
            c.contentResolver.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI,arrayOf(MediaStore.MediaColumns._ID,MediaStore.MediaColumns.OWNER_PACKAGE_NAME),
                "substr(${MediaStore.MediaColumns.RELATIVE_PATH},1,?)=?",arrayOf(from.length.toString(),from),null)?.use {q->
                while(q.moveToNext()){require(q.getString(1)==c.packageName){"La carpeta contiene archivos ajenos; no se renombró."};all+=ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI,q.getLong(0)).toString()}
            } ?: error("Android no permitió inventariar la carpeta histórica.")
            require(all==files.map{it.uri!!}.toSet()){"La carpeta tiene contenido no acreditado; requiere revisión controlada."}
            val unique=files.distinctBy{it.uri};val store=AndroidSharedDocumentStore(c)
            for(f in unique){require(store.digest(Uri.parse(f.uri))==(f.size to f.sha256));require(db.localFilesDao().otherFileReferences(owner,f.uri!!,f.relativePath,f.name)==0)}
            j=JSONObject().put("owner",owner).put("from",from).put("to",to).put("phase","PREPARED")
                .put("files",JSONArray(unique.map{JSONObject().put("uri",it.uri).put("path",it.relativePath).put("name",it.name).put("size",it.size).put("sha256",it.sha256)}))
            write(journal,j)
        }
        require(j.getString("owner")==owner&&j.getString("from")==from&&j.getString("to")==to)
        val store=AndroidSharedDocumentStore(c);val entries=j.getJSONArray("files")
        // Preflight the ENTIRE batch before its next effect, including destinations and shared references.
        for(n in 0 until entries.length()) {
            val f=entries.getJSONObject(n);val uri=Uri.parse(f.getString("uri"));val path=f.getString("path");val target=to+path.removePrefix(from)
            require(store.digest(uri)==(f.getLong("size") to f.getString("sha256"))){"Una copia cambió; originales y journal conservados."}
            require(db.localFilesDao().otherFileReferences(owner,uri.toString(),path,f.getString("name"))==0)
            c.contentResolver.query(uri,arrayOf(MediaStore.MediaColumns.RELATIVE_PATH,MediaStore.MediaColumns.DISPLAY_NAME,MediaStore.MediaColumns.OWNER_PACKAGE_NAME),null,null,null)?.use {q->
                require(q.moveToFirst()&&q.getString(0) in setOf(path,target)&&q.getString(1)==f.getString("name")&&q.getString(2)==c.packageName)
            } ?: error("No se pudo comprobar una copia.")
            c.contentResolver.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI,arrayOf(MediaStore.MediaColumns._ID),
                "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND ${MediaStore.MediaColumns.DISPLAY_NAME}=?",arrayOf(target,f.getString("name")),null)?.use {q->
                while(q.moveToNext())require(ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI,q.getLong(0))==uri){"Destino ocupado; no se sobrescribió."}
            } ?: error("No se pudo comprobar el destino.")
        }
        for(n in 0 until entries.length()) {
            val f=entries.getJSONObject(n);val uri=Uri.parse(f.getString("uri"));val target=to+f.getString("path").removePrefix(from)
            check(c.contentResolver.update(uri,ContentValues().apply{put(MediaStore.MediaColumns.RELATIVE_PATH,target)},null,null)==1)
            val descriptor=LocalSharedFile("CHECK","CHECK",owner,target,f.getString("name"),"application/octet-stream",f.getLong("size"),f.getString("sha256"),"")
            store.checkSlot(SharedDocumentStore.Slot(uri,true),descriptor)
            require(store.digest(uri)==(f.getLong("size") to f.getString("sha256")))
        }
        rebasePrivate(c,owner,from,to)
        for(f in files)db.localFilesDao().saveFile(f.copy(relativePath=to+f.relativePath.removePrefix(from)))
        val alias="SCAP_FOLDER_ALIAS/"+sha256((owner+from).toByteArray())
        db.localFilesDao().saveChoice(DeviceDeliveryChoice(alias,JSONObject().put("owner",owner).put("from",from).put("to",to).toString(),"SCAP_FOLDER_ALIAS",System.currentTimeMillis()))
        j.put("phase","DONE");write(journal,j)
    }
    /** Old frozen plans remain untouched; only their owned public path is resolved at publication. */
    suspend fun current(db:InventoryDatabase,f:LocalSharedFile):LocalSharedFile {
        var path=f.relativePath;val seen=mutableSetOf<String>()
        val aliases=db.openHelper.readableDatabase.query("SELECT destination FROM device_delivery_choices WHERE `key` GLOB 'SCAP_FOLDER_ALIAS/*'").use{q->buildList{while(q.moveToNext())add(JSONObject(q.getString(0)))}}
        while(true){require(seen.add(path)){"Alias de carpeta circular."};val a=aliases.singleOrNull{it.getString("owner")==f.recordKey&&path.startsWith(it.getString("from"))} ?: break
            path=a.getString("to")+path.removePrefix(a.getString("from"))}
        return f.copy(relativePath=path)
    }
    private suspend fun moveSaf(c:Context,db:InventoryDatabase,owner:String,from:String,to:String,files:List<LocalSharedFile>) {
        val tree=db.localFilesDao().choice("SCAP_EXPORT_TREE/"+owner.substringAfter('/'))?.destination?.let(Uri::parse)
            ?: error("Carpeta histórica SAF sin autorización por puente. Se conserva y requiere intervención controlada.")
        val storage=AndroidSharedDocumentStore(c,tree);val parentPath=from.trimEnd('/').substringBeforeLast('/')+"/"
        require(to.trimEnd('/').substringBeforeLast('/')+"/"==parentPath){"No se permite cambiar de carretera al renombrar."}
        val parent=storage.directory(parentPath,false);val targetName=to.trimEnd('/').substringAfterLast('/')
        val journal=File(c.filesDir,"scap_folder_moves/saf-"+sha256((owner+from+to).toByteArray())+".json")
        fun children(uri:Uri):List<Pair<String,Uri>> {
            val out=mutableListOf<Pair<String,Uri>>()
            c.contentResolver.query(android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(uri,android.provider.DocumentsContract.getDocumentId(uri)),arrayOf(android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME,android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID),android.os.Bundle.EMPTY,null)?.use{q->while(q.moveToNext())out+=q.getString(0) to android.provider.DocumentsContract.buildDocumentUriUsingTree(uri,q.getString(1))} ?: error("No se pudo inventariar SAF.")
            return out
        }
        fun inventory(uri:Uri,prefix:String=""):Map<String,Uri> {
            val out=linkedMapOf<String,Uri>()
            for((name,u) in children(uri)) {
                val mime=c.contentResolver.query(u,arrayOf(android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE),android.os.Bundle.EMPTY,null)?.use{q->require(q.moveToFirst());q.getString(0)}
                if(mime==android.provider.DocumentsContract.Document.MIME_TYPE_DIR)out.putAll(inventory(u,prefix+name+"/"))else require(out.put(prefix+name,u)==null)
            };return out
        }
        var j=if(journal.isFile)JSONObject(journal.readText())else null
        if(j==null) {
            val original=storage.directory(from,false);require(children(parent).none{it.first==targetName}){"Destino ocupado; no se renombró."}
            val before=inventory(original);val known=files.distinctBy{it.uri}.associateBy{it.relativePath.removePrefix(from)+it.name}
            require(before.keys==known.keys){"Contenido histórico no acreditado; carpeta conservada."}
            for((path,u) in before){val f=known.getValue(path);require(u.toString()==f.uri&&storage.digest(u)==(f.size to f.sha256));require(db.localFilesDao().otherFileReferences(owner,f.uri!!,f.relativePath,f.name)==0)}
            j=JSONObject().put("original",original.toString()).put("target",targetName).put("files",JSONArray(known.map{(path,f)->JSONObject().put("path",path).put("uri",f.uri).put("size",f.size).put("hash",f.sha256)}));write(journal,j)
        }
        val entries=j.getJSONArray("files");val old=Uri.parse(j.getString("original"));val candidates=children(parent)
        val exists=candidates.any{it.second==old};val renamed=if(exists) {
            require(candidates.none{it.first==targetName&&it.second!=old})
            val present=inventory(old);require(present.keys==(0 until entries.length()).map{entries.getJSONObject(it).getString("path")}.toSet())
            for(n in 0 until entries.length()){val f=entries.getJSONObject(n);val u=present.getValue(f.getString("path"));require(storage.digest(u)==(f.getLong("size") to f.getString("hash")))}
            requireNotNull(android.provider.DocumentsContract.renameDocument(c.contentResolver,old,targetName))
        }else candidates.singleOrNull{it.first==targetName}?.second ?: error("Renombrado pendiente; conserva el journal y reelige la carpeta.")
        val after=inventory(renamed);require(after.keys==(0 until entries.length()).map{entries.getJSONObject(it).getString("path")}.toSet())
        val uris=mutableMapOf<String,String>()
        for(n in 0 until entries.length()){val f=entries.getJSONObject(n);val uri=after.getValue(f.getString("path"));require(storage.digest(uri)==(f.getLong("size") to f.getString("hash")));uris[f.getString("uri")]=uri.toString()}
        rebasePrivate(c,owner,from,to,tree.toString(),uris)
        for(f in files)db.localFilesDao().saveFile(f.copy(uri=uris.getValue(f.uri!!),relativePath=to+f.relativePath.removePrefix(from)))
        j.put("phase","DONE");write(journal,j)
    }
    private fun rebasePrivate(c:Context,owner:String,from:String,to:String,namespace:String="",uris:Map<String,String> = emptyMap()) {
        val root=File(c.filesDir,"local_publications_v5")
        for(dir in root.listFiles().orEmpty().filter{it.isDirectory}) {
            val current=File(dir,"current.json");if(!current.isFile)continue
            val j=JSONObject(current.readText());val f=j.getJSONObject("file")
            if(f.optString("recordKey")!=owner||!f.getString("relativePath").startsWith(from))continue
            val path=to+f.getString("relativePath").removePrefix(from)
            val prefix=if(namespace.isBlank())""else namespace+"\u0000"
            if(dir.name!=sha256((prefix+f.getString("relativePath")+"\u0000"+f.getString("name")).toByteArray()))continue
            val dest=File(root,sha256((prefix+path+"\u0000"+f.getString("name")).toByteArray()))
            if(!dest.exists())check(dir.copyRecursively(dest)) else {
                val existing=JSONObject(File(dest,"current.json").readText()).getJSONObject("file")
                require(existing.optString("recordKey")==owner&&existing.getString("relativePath") in setOf(f.getString("relativePath"),path)&&existing.getString("sha256")==f.getString("sha256")){"Metadatos de destino ajenos; no se sobrescribieron."}
            }
            // Keep original private directories/bytes intact; resume copies after interruption.
            for(file in dest.walkTopDown().filter{it.isFile&&it.extension=="json"&&!it.relativeTo(dest).invariantSeparatorsPath.startsWith("versions/")}) {
                val value=JSONObject(file.readText());fun replace(o:Any?) {when(o){is JSONObject->for(k in o.keys().asSequence().toList()){val v=o.get(k);if(k=="relativePath"&&v is String&&v.startsWith(from))o.put(k,to+v.removePrefix(from))else if(v is String&&v in uris)o.put(k,uris.getValue(v))else replace(v)};is JSONArray->for(n in 0 until o.length())replace(o.get(n))}}
                replace(value);write(file,value)
            }
        }
    }
}
