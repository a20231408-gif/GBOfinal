package com.tuempresa.inventariovial

import android.app.Application
import android.content.*
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.export.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import java.io.File

internal class SicDocumentsProvider:DocumentsProvider() {
    data class Doc(val id:String,var name:String,val mime:String,val parent:String?,val file:File)
    val docs=linkedMapOf<String,Doc>();private var sequence=0;var stopAfterBackup=false;var stopAfterPromotion=false
    override fun onCreate():Boolean{docs["root"]=Doc("root","Descargas",DocumentsContract.Document.MIME_TYPE_DIR,null,File(context!!.filesDir,"FICTICIO_TREE"));return true}
    private fun cursor(rows:List<Doc>,projection:Array<out String>?):Cursor {
        val columns=projection ?: arrayOf("document_id","_display_name","mime_type","flags")
        return MatrixCursor(columns).apply{rows.forEach{doc->addRow(columns.map{when(it){"document_id"->doc.id;"_display_name"->doc.name;"mime_type"->doc.mime;"flags"->DocumentsContract.Document.FLAG_SUPPORTS_WRITE or DocumentsContract.Document.FLAG_SUPPORTS_RENAME or DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE;else->null}})}}
    }
    override fun queryRoots(projection:Array<out String>?)=MatrixCursor(arrayOf("root_id","document_id","title","flags")).apply{addRow(arrayOf("root","root","FICTICIO",0))}
    override fun queryDocument(documentId:String,projection:Array<out String>?)=cursor(listOf(docs.getValue(documentId)),projection)
    override fun queryChildDocuments(parentDocumentId:String,projection:Array<out String>?,sortOrder:String?)=cursor(docs.values.filter{it.parent==parentDocumentId},projection)
    override fun isChildDocument(parentDocumentId:String,documentId:String):Boolean {
        var id:String?=documentId
        while(id!=null){if(id==parentDocumentId)return true;id=docs[id]?.parent};return false
    }
    override fun createDocument(parentDocumentId:String,mimeType:String,displayName:String):String {
        val id="doc_${++sequence}";val file=File(context!!.filesDir,"FICTICIO_TREE/$id").apply{parentFile!!.mkdirs();createNewFile()}
        docs[id]=Doc(id,displayName,mimeType,parentDocumentId,file);return id
    }
    override fun renameDocument(documentId:String,displayName:String):String {
        val doc=docs.remove(documentId)!!;val id="doc_${++sequence}"
        docs[id]=doc.copy(id=id,name=displayName)
        if(stopAfterBackup&&(displayName.contains(".anterior-")||displayName.startsWith(".Historial - "))){stopAfterBackup=false;throw CancellationException("FICTICIO_SAF_LOST_BACKUP_URI")}
        if(stopAfterPromotion&&displayName.startsWith("SIC-")){stopAfterPromotion=false;throw CancellationException("FICTICIO_SAF_LOST_FINAL_URI")}
        return id
    }
    override fun openDocument(documentId:String,mode:String,signal:CancellationSignal?):ParcelFileDescriptor =
        ParcelFileDescriptor.open(docs.getValue(documentId).file,if(mode=="r")ParcelFileDescriptor.MODE_READ_ONLY else ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_TRUNCATE)
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=Application::class)
class SicConsolidationSafTest {
    private val c get()=ApplicationProvider.getApplicationContext<Context>()
    private lateinit var provider:SicDocumentsProvider
    private val tree=DocumentsContract.buildTreeDocumentUri("com.android.externalstorage.documents","root")
    @Before fun attach(){
        provider=SicDocumentsProvider().apply{attachInfo(c,ProviderInfo().apply{authority="com.android.externalstorage.documents";exported=true;grantUriPermissions=true;readPermission="android.permission.MANAGE_DOCUMENTS";writePermission="android.permission.MANAGE_DOCUMENTS"})}
        ShadowContentResolver.registerProviderInternal("com.android.externalstorage.documents",provider)
        c.contentResolver.takePersistableUriPermission(tree,Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        c.getSharedPreferences("gbo_local_copies",Context.MODE_PRIVATE).edit().putString("legacyTree",tree.toString()).commit()
    }
    @Test fun authorizedSafTreeCreatesFoldersAndReplacesWithChangedDocumentIds()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val repo=SicConsolidationFixtures.repo(c,db);val id=SicConsolidationFixtures.bridge(repo,"ANTES")
            val a=LocalSicConsolidates.update(c,db,"PE-28H","SIC-17A")
            repo.setSicField(id,"SIC-17A","bridgeName","DESPUES")
            val b=LocalSicConsolidates.update(c,db,"PE-28H","SIC-17A")
            assertNotEquals(a.uri,b.uri);assertEquals(1,b.rows)
            assertTrue(provider.docs.values.any{it.name=="EXCEL_SIC"});assertEquals(1,provider.docs.values.count{it.name=="SIC-17A.xlsx"})
            assertTrue(provider.docs.values.any{it.name.contains(".anterior-")});assertEquals(1,SicRouteWorkbook.inspect(b.file.readBytes(),"SIC-17A"))
        }
    }
    @Test fun lostSafRenameResponsesRecoverUsingExactBytesWithoutOverwritingAnotherDocument()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val repo=SicConsolidationFixtures.repo(c,db);val id=SicConsolidationFixtures.bridge(repo,"ANTES")
            LocalSicConsolidates.update(c,db,"PE-28H","SIC-17A")
            for(backup in listOf(true,false)) {
                repo.setSicField(id,"SIC-17A","bridgeName","CAMBIO_$backup")
                provider.stopAfterBackup=backup;provider.stopAfterPromotion=!backup
                assertTrue(runCatching{LocalSicConsolidates.update(c,db,"PE-28H","SIC-17A")}.isFailure)
                val after=LocalSicConsolidates.update(c,db,"PE-28H","SIC-17A")
                assertEquals(1,after.rows);assertEquals(1,provider.docs.values.count{it.name=="SIC-17A.xlsx"})
            }
        }
    }
    @Test fun revokedSafPermissionDoesNotReportSuccessOrAlterSourceRows()=runBlocking {
        c.contentResolver.releasePersistableUriPermission(tree,Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        SicConsolidationFixtures.db(c).use{db->
            val repo=SicConsolidationFixtures.repo(c,db);SicConsolidationFixtures.bridge(repo,"PENDIENTE")
            assertTrue(runCatching{LocalSicConsolidates.update(c,db,"PE-28H","SIC-17A")}.isFailure)
            assertTrue(provider.docs.values.none{it.name=="SIC-17A.xlsx"});assertEquals(1,repo.dao.allSnapshots().size)
        }
    }
    @Test fun stableElementDocumentsRecoverChangedSafIdsAndKeepHistoryOpenable()=runBlocking {
        val storage=com.tuempresa.inventariovial.localfiles.AndroidSharedDocumentStore(c)
        SicConsolidationFixtures.db(c).use{db->
            val key="SIC/TEST_STABLE_SAF"
            suspend fun publish(rev:Long):com.tuempresa.inventariovial.localfiles.LocalSharedFile {
                val job=com.tuempresa.inventariovial.localfiles.LocalExportJob("SAF_$rev",key,"SIC","TEST_STABLE_SAF",rev,createdAt=rev)
                db.localFilesDao().saveJob(job)
                val f=File(c.filesDir,"stable-saf-$rev.xlsx").apply{writeText("FICTICIO $rev")}
                val h=com.tuempresa.inventariovial.localfiles.fileDigest(f)
                val file=com.tuempresa.inventariovial.localfiles.LocalSharedFile(job.id,job.id,key,
                    "Download/GBO Inventario Vial/PE-22A/ALCANTARILLAS/TEST_STABLE_SAF/","SIC-18.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",h.first,h.second,f.path)
                return com.tuempresa.inventariovial.localfiles.StableLocalPublication.publish(c,db,job,file,storage)
                    .also{db.localFilesDao().saveFile(it)}
            }
            val first=publish(1)
            for(rev in 2L..3L) {
                provider.stopAfterBackup=rev==2L;provider.stopAfterPromotion=rev==3L
                assertTrue(runCatching{publish(rev)}.isFailure)
                val next=publish(rev)
                assertEquals(next.size to next.sha256,storage.digest(Uri.parse(next.uri)))
                assertEquals(1,provider.docs.values.count{it.name=="SIC-18.xlsx"})
            }
            val history=db.localFilesDao().files("SAF_1").single()
            assertNotEquals(first.uri,history.uri);assertTrue(history.name.startsWith(".Historial - "))
            assertEquals(history.size to history.sha256,storage.digest(Uri.parse(history.uri)))
            assertTrue(File(history.source).isFile);assertTrue(db.automaticSyncDao().all().isEmpty())
        }
    }
}
