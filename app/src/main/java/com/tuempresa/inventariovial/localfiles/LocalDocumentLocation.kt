package com.tuempresa.inventariovial.localfiles

import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.core.*
import org.json.JSONObject

data class LocalDocumentLocation(val base:String,val remotePath:String?,val remoteFolderId:String?,val ackRevision:Long?) {
    fun revision(value:Long)="${base}REVISIONES/R_$value/"
    fun json()=JSONObject().put("localBase",base).put("remoteLogicalPath",remotePath ?: JSONObject.NULL)
        .put("remoteFolderId",remoteFolderId ?: JSONObject.NULL).put("confirmedRemoteRevision",ackRevision ?: JSONObject.NULL)
        .put("identityStatus",if(remotePath==null)"PROVISIONAL_UUID"else"SERVER_ACK")
        .put("publicFolderNamingVersion",PublicLocalFolderNames.VERSION)
    companion object {
        suspend fun resolve(db:InventoryDatabase,job:LocalExportJob,route:String,family:String,label:String):LocalDocumentLocation {
            val i=db.automaticSyncDao().get(job.recordKey)
            val ack=i?.takeIf{it.syncedRevision>0&&it.lastSyncedAt!=null}?.receiptJson?.let{runCatching{JSONObject(it)}.getOrNull()}
                ?.takeIf{it.optBoolean("ok")&&it.optString("key")==job.recordKey&&it.optLong("revision")==i.syncedRevision&&it.optString("folderId").isNotBlank()}
            val remote=ack?.optString("logicalPath")?.takeIf{it.isNotBlank()&&it.split('/').all{p->p.isNotBlank()&&p !in setOf(".","..")}}
            // Keep ACK evidence intact, but never let Drive determine a public local folder name.
            val parent="Download/GBO_Ruta_Clara/"+("$route/$family").split('/').joinToString("/"){LocalExportNames.safe(it)}+"/"
            val name=PublicLocalFolderNames.reserve(db,job.recordKey,parent,PublicLocalFolderNames.describe(db,job,label))
            return LocalDocumentLocation(parent+name+"/",remote,ack?.optString("folderId"),ack?.optLong("revision"))
        }
    }
}
