package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.localfiles.*
import com.tuempresa.inventariovial.media.*
import com.tuempresa.inventariovial.scap.domain.ScapPhotoPanel
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import org.robolectric.shadows.ShadowStatFs
import java.io.File
import java.util.zip.ZipFile

/** Real disk/Room producer + streaming provider; fictitious video bytes, no playback/network claim. */
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RealLargeScapP0Test {
    @Test(timeout=240000) fun wholeScapWith54Photos3SketchesAnd20VideosPublishesAbove512MiB()=runBlocking {
        val c=ApplicationProvider.getApplicationContext<Context>()
        ShadowStatFs.registerStats(c.filesDir.path,4_000_000,3_000_000,3_000_000)
        val folder=File(System.getProperty("gbo.large.evidence","../tmp/large-archive"),"scap-${java.util.UUID.randomUUID()}").apply{mkdirs()}
        SicConsolidationFixtures.db(c).use{db->
            val id=ScapP0Fixture.create(c,db,54)
            val block=ByteArray(1024*1024).also{java.util.Random(20261028).nextBytes(it)}
            repeat(20){n->
                val file=File(folder,"TEST_VIDEO_${n+1}.mp4")
                file.outputStream().buffered().use{out->repeat(30){out.write(block)}}
                val d=fileDigest(file);val category=ScapPhotoPanel.categories[n]
                VideoDatabase.get(c).videos().put(VideoEntity("TEST_VIDEO_$n","SCAP/$id",category.key,category.label,
                    file.path,"video/mp4",d.first,d.second,1000L+n))
            }
            val s=db.scapDao().snapshot(id)!!
            val job=LocalCopies.request(c,db,"SCAP",id,s.inspection.updatedAt,"ZIP")
            val destinations=linkedMapOf<Uri,File>();var published=0
            val store=object:SharedDocumentStore {
                override fun reserve(file:LocalSharedFile,knownUri:Uri?):SharedDocumentStore.Slot {
                    val uri=knownUri?:Uri.parse("content://gbo.test.large/1")
                    destinations.putIfAbsent(uri,File(folder,"PUBLICADO.zip"));return SharedDocumentStore.Slot(uri,published>0)
                }
                override fun write(uri:Uri,source:File){source.inputStream().use{input->destinations.getValue(uri).outputStream().use{input.copyTo(it,64*1024)}}}
                override fun digest(uri:Uri)=fileDigest(destinations.getValue(uri))
                override fun publish(slot:SharedDocumentStore.Slot,file:LocalSharedFile):Uri{published++;return slot.uri}
                override fun renameOwned(slot:SharedDocumentStore.Slot,from:LocalSharedFile,to:LocalSharedFile):SharedDocumentStore.Slot {
                    // This single-ZIP provider keeps a stable URI across the local promotion.
                    require(from.relativePath==to.relativePath && digest(slot.uri)==(from.size to from.sha256))
                    return SharedDocumentStore.Slot(slot.uri,true)
                }
            }
            LocalCopyDrain.run(c,db,store,20)
            val saved=db.localFilesDao().job(job.id)!!;assertEquals(saved.message,"READY",saved.state)
            val zip=db.localFilesDao().files(job.id).single();assertTrue(zip.size>512L*1024*1024)
            assertEquals(zip.size to zip.sha256,store.digest(Uri.parse(zip.uri)))
            ZipFile(zip.source).use{z->val names=z.entries().asSequence().map{it.name}.toList()
                assertEquals(54,names.count{"FOTOGRAFIAS" in it.split('/')})
                assertEquals(3,names.count{"CROQUIS" in it.split('/')})
                assertEquals(20,names.count{"VIDEOS" in it.split('/')})
                assertTrue(names.any{it.endsWith("MANIFIESTO_LOCAL.json")})
            }
            val videos=VideoDatabase.get(c).videos().all().filter{it.parentKey=="SCAP/$id"}
            assertEquals(20,videos.size);for(v in videos)assertEquals(v.sizeBytes to v.sha256,fileDigest(File(v.localPath)))
            assertEquals(0L,db.automaticSyncDao().get("SCAP/$id")!!.syncedRevision)
            File(folder,"MEDICION_SCAP.json").writeText("""{"zipBytes":${zip.size},"photos":54,"sketches":3,"videos":20,"sha256":"${zip.sha256}","publishedAndVerified":true,"provider":"LOCAL_DISK_FIXTURE","videoPlaybackTested":false}""")
        }
    }
}
