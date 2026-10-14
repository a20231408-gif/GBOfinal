package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.auth.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.pavement.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
class PavementPhotoPersistence10ITest {
    @Test fun photoOffline()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        // Short method/file names keep the native Windows SQLite path valid.
        val name="p10i.db"
        fun open()=Room.databaseBuilder(context,InventoryDatabase::class.java,name).build()
        var db=open();val user=AuthSessionManager().apply {login(UserCacheEntity("test10i","test10i","Inspector ficticio","OPERATOR",true,"",0,0,0))}
        val original=File(context.cacheDir,"original10i.png").apply {writeBytes(java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9ZpdtxsAAAAASUVORK5CYII="))}
        val replacement=File(context.cacheDir,"replacement10i.png").apply {writeBytes(original.readBytes())}
        try {
            db.pavementDao().saveSample(PavementSampleEntity("s10i","T","TEST10I",1,"Derecho","UC","00+000","00+010",5.0,10.0,"ASPHALT","2026-10-13","test10i","Inspector ficticio"))
            for(id in listOf("a","b"))db.pavementDao().saveDamage(PavementDamageEntity(id,"s10i",if(id=="a")1 else 2,"ASPHALT",7,"Grieta de borde","L","LINEAR",1.0,1.0,2.0,3.0,null,1.0,2.0,null,null,null,""))
            val repo=PavementRepository(db,user);repo.savePhoto(PavementDamagePhotoEntity("pa","a",original.path),"s10i");repo.savePhoto(PavementDamagePhotoEntity("pb","b",original.path),"s10i")
            assertTrue(runCatching {repo.replacePhoto("pa",PavementDamagePhotoEntity("wrong","b",replacement.path),"s10i")}.isFailure)
            repo.replacePhoto("pa",PavementDamagePhotoEntity("replacement","a",replacement.path),"s10i")
            repo.deletePhoto("pb","b",context.filesDir);db.close();db=open()
            val reopened=PavementRepository(db,user);assertEquals("a",reopened.photos("a").single().damageId)
            assertEquals("replacement",reopened.photos("a").single().id);assertTrue(reopened.photos("b").isEmpty())
            assertEquals("replacement",db.pavementDao().photo("pa")!!.replacedByPhotoId)
            assertEquals(1,reopened.photoHistory("b").first().size);assertTrue(original.exists() && replacement.exists())
            val payload=PavementDriveUploader.nativeJson(db.pavementDao().sample("s10i")!!,db.pavementDao().damages("s10i"),PavementResult(emptyList(),emptyList(),null,null,emptyList()),mapOf("a" to reopened.photos("a")))
            assertEquals("a",payload.getJSONArray("photos").getJSONObject(0).getString("damageId"))
            assertEquals("replacement",payload.getJSONArray("photos").getJSONObject(0).getString("photoId"))
            assertTrue(db.automaticSyncDao().all().isEmpty())
        } finally {db.close();context.deleteDatabase(name);original.delete();replacement.delete()}
    }
}
