package com.tuempresa.inventariovial

import android.app.Application
import android.content.*
import android.content.pm.ProviderInfo
import android.net.Uri
import android.provider.DocumentsContract
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.scap.export.*
import com.tuempresa.inventariovial.localfiles.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.*
import java.io.File

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
class ScapFolderIdentityTest {
    private val c get()=ApplicationProvider.getApplicationContext<Context>()
    private lateinit var provider:SicDocumentsProvider
    private val tree=DocumentsContract.buildTreeDocumentUri("com.android.externalstorage.documents","root")
    @Before fun init(){initializeIdleUiWorkers();ShadowStatFs.registerStats(c.filesDir.path,1000000,900000,900000)
        provider=SicDocumentsProvider().apply{changeFolderDescendantIds=true;attachInfo(c,ProviderInfo().apply{authority="com.android.externalstorage.documents";exported=true;grantUriPermissions=true;readPermission="android.permission.MANAGE_DOCUMENTS";writePermission="android.permission.MANAGE_DOCUMENTS"})}
        ShadowContentResolver.registerProviderInternal("com.android.externalstorage.documents",provider)
    }
    @Test fun threeConsecutiveBridgesHaveIndependentPersistedDestinationsAndCorrectExcel()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->val repo=SicConsolidationFixtures.repo(c,db);val destinations=mutableListOf<Uri>()
            for(n in 2..4){val id=SicConsolidationFixtures.bridge(repo,"PRUEBA $n",route="PE-22A");val s=repo.dao.snapshot(id)!!
                val files=ScapFolderExport.save(c,db,s,tree);val excel=files.single{it.name=="SCAP - PE-22A - PRUEBA $n.xlsx"}
                assertEquals("Download/GBO Inventario Vial/PE-22A/PUENTES/Puente PRUEBA $n/",excel.relativePath)
                assertFalse(files.any{RecordDocuments.sicFormat(it)!=null});assertEquals(tree,ScapFolderExport.remembered(db,id))
                destinations+=ScapFolderExport.folderUri(c,db,id)!!
                assertEquals(excel.size to excel.sha256,AndroidSharedDocumentStore(c,tree).digest(Uri.parse(excel.uri)))
            };assertEquals(3,destinations.toSet().size)
            val next=SicConsolidationFixtures.bridge(repo,"PRUEBA 5",route="PE-22A")
            assertEquals(tree,ScapFolderExport.remembered(db,next)) // Reuse BASE grant, never another bridge folder.
            assertFalse(ScapFolderExport.folderUri(c,db,next) in destinations)
            assertTrue(db.automaticSyncDao().all().isEmpty())
        }
    }
    @Test fun namelessCaptureStaysPrivateThenUsesPersistedNameInsteadOfOldSnapshot()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->val repo=SicConsolidationFixtures.repo(c,db);val id=SicConsolidationFixtures.bridge(repo,"")
            val blank=repo.dao.snapshot(id)!!;val count=provider.docs.size
            assertTrue(runCatching{ScapFolderExport.save(c,db,blank,tree)}.isFailure);assertEquals(count,provider.docs.size)
            repo.setField(id,"inspection","singularityName","PRUEBA 3");assertTrue(runCatching{ScapFolderExport.save(c,db,blank,tree)}.isFailure)
            val saved=repo.dao.snapshot(id)!!;val files=ScapFolderExport.save(c,db,saved,tree)
            assertTrue(files.all{it.relativePath.contains("/Puente PRUEBA 3/")});assertFalse(provider.docs.values.any{it.name.contains("Sin nombre",true)})
        }
    }
    @Test fun renamePreservesPhotoBytesAndOldDocumentsWithChangedSafFolderIdAndRetries()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->val repo=SicConsolidationFixtures.repo(c,db);val id=SicConsolidationFixtures.bridge(repo,"ANTES")
            val image=File(c.filesDir,"foto.jpg");val bitmap=android.graphics.Bitmap.createBitmap(20,20,android.graphics.Bitmap.Config.ARGB_8888)
            image.outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG,95,it)};bitmap.recycle()
            val photo=repo.addPhotoRecord(id,"GENERAL","TEST");repo.replacePhoto(id,photo,image.path)
            val initial=ScapFolderExport.save(c,db,repo.dao.snapshot(id)!!,tree);val bytes=fileDigest(image)
            val oldUri=ScapFolderExport.folderUri(c,db,id)
            repo.setField(id,"inspection","singularityName","DESPUES");provider.stopAfterFolderRename=true
            assertTrue(runCatching{ScapFolderExport.save(c,db,repo.dao.snapshot(id)!!,tree)}.isFailure)
            val current=ScapFolderExport.save(c,db,repo.dao.snapshot(id)!!,tree)
            assertNotEquals(oldUri,ScapFolderExport.folderUri(c,db,id));assertEquals(bytes,fileDigest(image))
            assertEquals(1,provider.docs.values.count{it.name=="Puente DESPUES"});assertFalse(provider.docs.values.any{it.name=="Puente ANTES"})
            assertNotEquals(initial.first{it.mimeType=="image/jpeg"}.uri,current.first{it.mimeType=="image/jpeg"}.uri)
            assertTrue(current.any{it.mimeType=="image/jpeg"});assertTrue(initial.all{File(it.source).isFile})
            assertTrue(db.localFilesDao().filesForRecord("SCAP/$id").all{!it.relativePath.contains("/Puente ANTES/")})
            assertTrue(db.automaticSyncDao().all().isEmpty())
        }
    }
    @Test fun twinNamesAndUnknownHistoricalContentNeverAdoptAnotherBridge()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->val repo=SicConsolidationFixtures.repo(c,db);val a=SicConsolidationFixtures.bridge(repo,"IGUAL");val b=SicConsolidationFixtures.bridge(repo,"IGUAL")
            val first=ScapFolderExport.save(c,db,repo.dao.snapshot(a)!!,tree);val second=ScapFolderExport.save(c,db,repo.dao.snapshot(b)!!,tree)
            assertNotEquals(first.first().relativePath,second.first().relativePath)
            val folder=ScapFolderExport.folderUri(c,db,a)!!;provider.createDocument(DocumentsContract.getDocumentId(folder),"text/plain","AJENO.txt")
            repo.setField(a,"inspection","singularityName","CAMBIO")
            assertTrue(runCatching{ScapFolderExport.save(c,db,repo.dao.snapshot(a)!!,tree)}.isFailure)
            assertTrue(provider.docs.values.any{it.name=="Puente IGUAL"});assertEquals(second.map{it.uri},ScapFolderExport.save(c,db,repo.dao.snapshot(b)!!,tree).map{it.uri})
        }
    }    @Test fun mediaStoreRenameMovesOnlyVerifiedBridgeFilesAndKeepsUriAndBytes()=runBlocking {
        val media=SicDownloadsProvider().apply{attachInfo(c,ProviderInfo().apply{authority="media"})};ShadowContentResolver.registerProviderInternal("media",media)
        SicConsolidationFixtures.db(c).use{db->val repo=SicConsolidationFixtures.repo(c,db);val id=SicConsolidationFixtures.bridge(repo,"INICIAL")
            LocalCopies.requestCurrent(c,db,"SCAP",id);LocalCopyDrain.run(c,db,limit=30)
            val before=db.localFilesDao().filesForRecord("SCAP/$id").filter{it.state=="READY"&&it.relativePath.contains("/Puente INICIAL/")}
            assertTrue(before.isNotEmpty());val store=AndroidSharedDocumentStore(c)
            val bytes=before.associate{it.uri!! to store.digest(Uri.parse(it.uri))}
            repo.setField(id,"inspection","singularityName","RENOMBRADO")
            LocalCopies.requestCurrent(c,db,"SCAP",id);LocalCopyDrain.run(c,db,limit=30)
            val after=db.localFilesDao().filesForRecord("SCAP/$id")
            assertFalse(after.any{it.relativePath.contains("/Puente INICIAL/")})
            for(f in before){val current=after.single{it.id==f.id};assertTrue(current.relativePath.contains("/Puente RENOMBRADO/"));assertEquals(bytes[f.uri],store.digest(Uri.parse(current.uri)))}
            assertTrue(db.automaticSyncDao().all().isEmpty())
        }
    }
    @Test fun closingAndReopeningRoomRetainsBridgeSpecificSafDestination()=runBlocking {
        val name="scap-folders.db"
        fun open()=androidx.room.Room.databaseBuilder(c,com.tuempresa.inventariovial.data.database.InventoryDatabase::class.java,name).build()
        lateinit var id:String;lateinit var folder:Uri
        open().use{db->val repo=SicConsolidationFixtures.repo(c,db);id=SicConsolidationFixtures.bridge(repo,"PERSISTENTE")
            ScapFolderExport.save(c,db,repo.dao.snapshot(id)!!,tree);folder=ScapFolderExport.folderUri(c,db,id)!!}
        val count=provider.docs.size
        open().use{db->assertEquals(tree,ScapFolderExport.remembered(db,id));assertEquals(folder,ScapFolderExport.folderUri(c,db,id));assertEquals(count,provider.docs.size)}
    }
    @Test fun interruptedMediaMoveResumesSameUrisAndPreservesFrozenPlan()=runBlocking {
        val media=SicDownloadsProvider().apply{attachInfo(c,ProviderInfo().apply{authority="media"})};ShadowContentResolver.registerProviderInternal("media",media)
        SicConsolidationFixtures.db(c).use{db->val repo=SicConsolidationFixtures.repo(c,db);val id=SicConsolidationFixtures.bridge(repo,"ANTES")
            LocalCopies.requestCurrent(c,db,"SCAP",id);LocalCopyDrain.run(c,db,limit=30)
            val originalJobs=db.localFilesDao().jobs().associate{it.id to it.manifestJson};val before=db.localFilesDao().filesForRecord("SCAP/$id").filter{it.state=="READY"&&it.relativePath.contains("/Puente ANTES/")};assertTrue(before.isNotEmpty())
            repo.setField(id,"inspection","singularityName","DESPUES")
            val j=LocalExportJob("TEST_INTERRUPTED_MOVE","SCAP/$id","SCAP",id,repo.dao.inspection(id)!!.updatedAt)
            media.losePathMoveResponse=true
            assertTrue(runCatching{LocalExportBuilder.build(c,db,j)}.isFailure)
            val plan=LocalExportBuilder.build(c,db,j);assertTrue(plan.files.all{it.relativePath.contains("/Puente DESPUES/")})
            val after=db.localFilesDao().filesForRecord("SCAP/$id").associateBy{it.id}
            for(f in before){assertEquals(f.uri,after.getValue(f.id).uri);assertEquals(f.sha256,after.getValue(f.id).sha256);assertTrue(after.getValue(f.id).relativePath.contains("/Puente DESPUES/"))}
            for((key,manifest) in originalJobs)assertEquals(manifest,db.localFilesDao().job(key)!!.manifestJson)
            assertTrue(db.automaticSyncDao().all().isEmpty())
        }
    }
    @Test fun directFolderExportHonorsEditedFilenameAndIsIdempotentWithoutDrive()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->val repo=SicConsolidationFixtures.repo(c,db);val id=SicConsolidationFixtures.bridge(repo,"PRUEBA 3",route="PE-22A");val snapshot=repo.dao.snapshot(id)!!
            val files=ScapFolderExport.save(c,db,snapshot,tree,"Nombre elegido.xlsx")
            val file=files.single{it.name=="Nombre elegido.xlsx"};assertTrue(file.relativePath.endsWith("/PE-22A/PUENTES/Puente PRUEBA 3/"))
            assertEquals(file.size to file.sha256,AndroidSharedDocumentStore(c,tree).digest(Uri.parse(file.uri)))
            val count=provider.docs.size;assertEquals(files.map{it.uri},ScapFolderExport.save(c,db,snapshot,tree,"Nombre elegido.xlsx").map{it.uri});assertEquals(count,provider.docs.size)
            assertTrue(runCatching{ScapFolderExport.save(c,db,snapshot,tree,"../incorrecto.xlsx")}.isFailure)
            assertEquals(count,provider.docs.size);assertEquals(snapshot,repo.dao.snapshot(id));assertTrue(db.automaticSyncDao().all().isEmpty())
        }
    }

    @Test fun legacySharedBridgeSelectionCannotBecomeAnotherBridgesBase()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->val repo=SicConsolidationFixtures.repo(c,db)
            val a=SicConsolidationFixtures.bridge(repo,"PRUEBA 2");val b=SicConsolidationFixtures.bridge(repo,"PRUEBA 6",route="PE-3NG")
            val folder=provider.createDocument("root",DocumentsContract.Document.MIME_TYPE_DIR,"Puente (PRUEBA 2)")
            val wrong=DocumentsContract.buildTreeDocumentUri(tree.authority,folder)
            c.contentResolver.takePersistableUriPermission(wrong,Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            db.localFilesDao().saveChoice(DeviceDeliveryChoice("SCAP_EXPORT_AUTHORIZED_BASE",wrong.toString(),"LEGACY",1))
            db.localFilesDao().saveChoice(DeviceDeliveryChoice("SCAP_EXPORT_TREE/$a",wrong.toString(),"LEGACY",1))
            val count=provider.docs.size
            assertNull(ScapFolderExport.remembered(db,b));assertNull(ScapFolderExport.authorized(c,db,a))
            assertTrue(runCatching{ScapFolderExport.save(c,db,repo.dao.snapshot(b)!!,wrong)}.isFailure)
            assertEquals(count,provider.docs.size);assertTrue(db.localFilesDao().jobs().isEmpty())
            assertEquals(wrong.toString(),db.localFilesDao().choice("SCAP_EXPORT_AUTHORIZED_BASE")!!.destination)
            ScapFolderExport.save(c,db,repo.dao.snapshot(b)!!,tree)
            assertEquals(tree,ScapFolderExport.authorized(c,db,b));assertTrue(provider.docs.values.none{it.parent==folder})
        }
    }
    @Test fun parenthesizedModernFolderIsRenamedOnlyWithProvenOwnershipAndBytes()=runBlocking {
        val media=SicDownloadsProvider().apply{attachInfo(c,ProviderInfo().apply{authority="media"})};ShadowContentResolver.registerProviderInternal("media",media)
        SicConsolidationFixtures.db(c).use{db->val repo=SicConsolidationFixtures.repo(c,db);val id=SicConsolidationFixtures.bridge(repo,"PRUEBA 7",route="PE-3NG")
            val s=repo.dao.snapshot(id)!!;val parent=DriveMirrorPlan.ROOT+"PE-3NG/PUENTES/"
            val old=PublicLocalFolderNames.reserve(db,"SCAP/$id",parent,"Puente (PRUEBA 7)")
            val prepared=ScapLocalDocuments.prepare(c,s);val job=LocalExportJob("OLD_MODERN","SCAP/$id","SCAP",id,s.inspection.updatedAt,scope="SAF_TREE",state="READY")
            db.localFilesDao().saveJob(job)
            val f=LocalSharedFile("OLD_MODERN_FILE",job.id,job.recordKey,parent+old+"/",prepared.name,prepared.mime,prepared.size,prepared.sha256,prepared.source)
            c.contentResolver.takePersistableUriPermission(tree,Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            val storage=AndroidSharedDocumentStore(c,tree);val saved=StableLocalPublication.publish(c,db,job,f,storage,namespace=tree.toString());db.localFilesDao().saveFile(saved)
            ScapFolderExport.save(c,db,s,tree)
            val preserved=db.localFilesDao().filesForRecord(job.recordKey).single{it.id==saved.id}
            assertEquals(saved.size to saved.sha256,storage.digest(Uri.parse(preserved.uri)));assertTrue(File(saved.source).isFile)
            assertTrue(preserved.relativePath.contains("/Puente PRUEBA 7/"));assertTrue(provider.docs.values.none{it.name=="Puente (PRUEBA 7)"})
            assertTrue(db.automaticSyncDao().all().isEmpty())
        }
    }

}
