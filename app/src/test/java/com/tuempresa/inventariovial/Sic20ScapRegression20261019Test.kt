package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.ScapRepository
import com.tuempresa.inventariovial.scap.domain.ScapBridgeType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class)
class Sic20ScapRegression20261019Test {
    @Test fun independentScapDraftKeepsUuidMediaAndObservationsAfterOfflineReopen()=runBlocking {
        val c=ApplicationProvider.getApplicationContext<Context>();val name="TEST_SCAP_${UUID.randomUUID()}.db"
        fun open()=Room.databaseBuilder(c,InventoryDatabase::class.java,name).build()
        val photo=File(c.filesDir,"TEST_SCAP_${UUID.randomUUID()}.jpg")
        val bitmap=Bitmap.createBitmap(24,24,Bitmap.Config.ARGB_8888)
        photo.outputStream().use{bitmap.compress(Bitmap.CompressFormat.JPEG,95,it)};bitmap.recycle();val original=photo.readBytes()
        var id="";var revision=0L;var photoId=""
        open().use{db->
            val repo=ScapRepository(db,ScapCatalog.load(c));id=repo.create("TEST_OPERATOR","TEST_DEVICE",null,ScapBridgeType.MODULAR)
            repo.setField(id,"G2","observations","TEST_OBSERVACIONES_SCAP")
            repo.addPhoto(id,photo.path,ScapRepository.PHOTO_CATEGORIES.first(),null);repo.saveDraft(id)
            val snapshot=repo.dao.snapshot(id)!!;revision=snapshot.inspection.updatedAt;photoId=snapshot.photos.single().id
        }
        open().use{db->
            val snapshot=ScapRepository(db,ScapCatalog.load(c)).dao.snapshot(id)!!
            assertEquals(id,snapshot.inspection.id);assertEquals(revision,snapshot.inspection.updatedAt)
            assertEquals("TEST_OBSERVACIONES_SCAP",snapshot.values("G2")["observations"])
            assertEquals(photoId,snapshot.photos.single().id);assertArrayEquals(original,photo.readBytes())
        }
    }
}
