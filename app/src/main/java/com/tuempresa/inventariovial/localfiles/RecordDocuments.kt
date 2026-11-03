package com.tuempresa.inventariovial.localfiles

import android.content.Context
import android.net.Uri
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.export.LocalSicConsolidates

/** Principal document links are resolved, never persisted as mutable aliases of a shared workbook. */
object RecordDocuments {
    fun sicFormat(file:LocalSharedFile)=Regex("(SIC-[0-9]+[A-Z]?)(?:_PROVISIONAL)?\\.xlsx").matchEntire(file.name)?.groupValues?.get(1)
    fun provisional(file:LocalSharedFile)=file.name.contains("_PROVISIONAL")||file.name.contains("_BORRADOR")||file.source.contains("_PROVISIONAL")||file.source.replace('\\','/').contains("/BORRADOR/")
    fun current(jobs:List<LocalExportJob>,files:List<LocalSharedFile>,principals:List<LocalSharedFile>,history:Boolean=false):List<LocalSharedFile> {
        val formats=principals.mapNotNull(::sicFormat).toSet()
        val local=LocalCopyPresentation.currentFiles(jobs,files,history)
            .filter{if(history) !provisional(it) else sicFormat(it) !in formats}
        return (principals+local).distinctBy{it.uri}
    }
    /** Resolve again at the actual click: a newer consolidation may have archived the old URI. */
    suspend fun openTarget(c:Context,db:InventoryDatabase,kind:String,id:String,file:LocalSharedFile,history:Boolean=false,
        storage:SharedDocumentStore=AndroidSharedDocumentStore(c)):LocalSharedFile {
        val primary=file.id.startsWith("SIC_PRIMARY/")
        val target=if((!history||primary)&&sicFormat(file)!=null)
            LocalSicConsolidates.principals(c,db,kind,id,storage).firstOrNull{sicFormat(it)==sicFormat(file)}
                ?: file.also{require(!primary){"El consolidado actual no está verificado; conserva los originales y reintenta la copia local."}}
        else file
        require(target.uri!=null&&storage.digest(Uri.parse(target.uri))==(target.size to target.sha256)){"El documento no está verificado; los originales se conservan."}
        return target
    }
}
