package com.tuempresa.inventariovial

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.media.ExifInterface
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.camera.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.delivery.*
import com.tuempresa.inventariovial.location.GeoLocation
import com.tuempresa.inventariovial.road.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.zip.ZipFile

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RouteDeliveryTest {
    @get:Rule val temp = TemporaryFolder()
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private fun original(): File = temp.newFile("${java.util.UUID.randomUUID()}.jpg").apply {
        val bitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.LTGRAY) }
        outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }; bitmap.recycle()
    }
    @Test fun prepareWritesActualPhotosExifMetadataAndExistingSicWorkbookWithoutChangingOriginalOrIds() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, InventoryDatabase::class.java).build()
        try {
            val file = original(); val originalHash = sha256(file.readBytes())
            val record = testRecord("A", sic = "SIC-18").copy(suggestedPrCode = "0010", suggestedDistanceFromPrM = 2650.0)
            db.inventoryDao().insertRecord(record)
            db.inventoryDao().insertSic18(Sic18Entity("A", "06", "1", 1, "1", 1.0, null, "1", "1"))
            val p = PhotoCaptureMetadata(1_750_000_000_000, GeoLocation(-12.12, -77.23, .8f, 321.25, timestamp = 1_750_000_000_000))
                .applyTo(PhotoEntity("photo-A", "A", 1, file.absolutePath, true, "keep-original-name.jpg", null, null, "PENDING", 1))
            db.inventoryDao().insertPhoto(p)
            val delivery = RouteDelivery(context, db).prepare("R", testAxis(), MatchConfig())
            assertEquals("PREPARED", delivery.batch.status)
            val manifest = JSONObject(File(delivery.batch.manifestPath).readText())
            assertFalse(manifest.toString().contains("token"))
            assertEquals(3, manifest.getJSONArray("files").length())
            val root = File(delivery.batch.manifestPath).parentFile!!
            val files = manifest.getJSONArray("files")
            for (i in 0 until files.length()) {
                val entry = files.getJSONObject(i); val prepared = File(root, entry.getString("localFile"))
                val payload = entry.getJSONObject("payload")
                assertEquals(sha256(prepared.readBytes()), payload.getString("sha256"))
                when (payload.getString("kind")) {
                    "photo" -> {
                        assertEquals("001_F01.jpg", payload.getString("fileName"))
                        assertEquals("ALCANTARILLA UUID_A", payload.getString("elementFolder"))
                        // The delivery copy carries EXIF; its image pixels and the source stay intact.
                        val originalImage=requireNotNull(android.graphics.BitmapFactory.decodeFile(file.path))
                        val deliveredImage=requireNotNull(android.graphics.BitmapFactory.decodeFile(prepared.path))
                        assertEquals(originalImage.width,deliveredImage.width);assertEquals(originalImage.height,deliveredImage.height)
                        val originalPixels=IntArray(originalImage.width*originalImage.height)
                        val deliveredPixels=IntArray(deliveredImage.width*deliveredImage.height)
                        originalImage.getPixels(originalPixels,0,originalImage.width,0,0,originalImage.width,originalImage.height)
                        deliveredImage.getPixels(deliveredPixels,0,deliveredImage.width,0,0,deliveredImage.width,deliveredImage.height)
                        assertArrayEquals(originalPixels,deliveredPixels)
                        originalImage.recycle();deliveredImage.recycle()
                        val coordinates=FloatArray(2)
                        assertTrue(ExifInterface(prepared.path).getLatLong(coordinates))
                        assertEquals(-12.12,coordinates[0].toDouble(),.00001);assertEquals(-77.23,coordinates[1].toDouble(),.00001)
                        assertEquals(.8,payload.getDouble("photoGpsAccuracyM"),.001)
                        assertEquals("ORIGINAL_CLEAN",payload.getString("photoPolicy"))
                        assertEquals(2,payload.getInt("pathVersion"))
                        assertTrue(payload.getString("logicalPath").endsWith("/GENERAL"))
                    }
                    "metadata" -> {
                        val meta = JSONObject(prepared.readText())
                        assertEquals("A", meta.getString("recordId"))
                        assertEquals("photo-A", meta.getJSONArray("photos").getJSONObject(0).getString("photoId"))
                        assertFalse(meta.toString().contains(file.absolutePath))
                        val output = File("build/outputs/round2").apply { mkdirs() }
                        File(output, "metadata-DEMO.json").writeText(meta.toString(2))
                    }
                    "sic" -> {
                        assertEquals("SIC-18.xlsx", payload.getString("fileName"))
                        assertEquals("A",payload.getString("recordId"))
                        assertEquals(2,payload.getInt("pathVersion"))
                        assertTrue(payload.getString("logicalPath").startsWith("R/ALCANTARILLAS/ALCANTARILLA UUID_"))
                        ZipFile(prepared).use { assertNotNull(it.getEntry("xl/workbook.xml")) }
                    }
                }
            }
            assertEquals(originalHash, sha256(file.readBytes()))
            assertEquals(p, kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { db.inventoryDao().photoById(p.id) })
            assertEquals(record.createdAt, db.inventoryDao().recordById("A")!!.createdAt)
            assertEquals(DeliveryNamingPolicy.FOLDER_COMPACT.name, delivery.batch.namingPolicy)
            val old = RouteDelivery.fingerprint(RouteDelivery(context, db).snapshots("R"), DeliveryNamingPolicy.FOLDER_COMPACT)
            db.inventoryDao().insertRecord(testRecord("LATE", sic = "SIC-18"))
            assertNotEquals(old, RouteDelivery.fingerprint(RouteDelivery(context, db).snapshots("R"), DeliveryNamingPolicy.FOLDER_COMPACT))
            db.inventoryDao().deliveryStatus(delivery.batch.id, "QUEUED", null)
            val conflict = runCatching { RouteDelivery(context, db).prepare("R", testAxis(), MatchConfig()) }.exceptionOrNull()
            assertTrue(conflict?.message?.contains("otro contenido") == true)
        } finally { db.close() }
    }
    @Test fun exifUsesPhotoPositionAndNeverEditsOriginalOrWatermarkMarker() {
        val original = original(); val hash = sha256(original.readBytes()); val copy = temp.newFile("stamped.jpg")
        original.copyTo(copy, overwrite = true)
        ExifInterface(copy.absolutePath).apply { setAttribute(ExifInterface.TAG_USER_COMMENT, RequiredWatermark.MARKER); saveAttributes() }
        val p = PhotoCaptureMetadata(1000, GeoLocation(10.0, 20.0, .5f, -3.0)).applyTo(PhotoEntity("p", "r", 1, original.absolutePath, true, null, null, null, "PENDING", 1))
        PhotoExif.write(copy, original, p)
        assertEquals(hash, sha256(original.readBytes()))
        assertEquals(-3.0, ExifInterface(copy.absolutePath).getAltitude(99.0), .001)
        assertTrue(runCatching { PhotoExif.write(original, original, p) }.isFailure)
        val noFix = p.copy(photoLatitude = null, photoLongitude = null, photoAltitudeM = null, photoGpsAccuracyM = null)
        PhotoExif.write(copy, original, noFix)
        assertFalse(ExifInterface(copy.absolutePath).getLatLong(FloatArray(2)))
        assertEquals(RequiredWatermark.MARKER, ExifInterface(copy.absolutePath).getAttribute(ExifInterface.TAG_USER_COMMENT))
    }
    @Test fun brandedImportGetsSeparateWritableCopyWithoutRestampingOriginal() = runBlocking {
        val original = original()
        ExifInterface(original.absolutePath).apply { setAttribute(ExifInterface.TAG_USER_COMMENT, RequiredWatermark.MARKER); saveAttributes() }
        val hash = sha256(original.readBytes())
        val marked = RequiredWatermark.prepare(context, original.absolutePath)
        assertEquals(original.canonicalPath, marked.canonicalPath)
        val copy = PhotoExif.writableCopy(original, marked, temp.newFolder())
        val photo = PhotoCaptureMetadata(1000, GeoLocation(-12.0, -77.0, 1f)).applyTo(
            PhotoEntity("import", "record", 1, original.absolutePath, true, null, null, null, "PENDING", 1))
        PhotoExif.write(copy, original, photo)
        assertEquals(hash, sha256(original.readBytes()))
        assertNotEquals(original.canonicalPath, copy.canonicalPath)
        assertTrue(ExifInterface(copy.absolutePath).getLatLong(FloatArray(2)))
        assertEquals(RequiredWatermark.MARKER, ExifInterface(copy.absolutePath).getAttribute(ExifInterface.TAG_USER_COMMENT))
    }
    @Test fun exportReviewableDemoFromRealAxisWithThreePhotosAndTwoElements() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, InventoryDatabase::class.java).build()
        try {
            val reference = RoadReferenceRepository(context).loadFromAssets()
            val axis = reference.segments.single { it.routeCode == "PE-22A" }
            val now = 1_790_640_000_000L
            val records = listOf(1700, 1900).mapIndexed { index, vertex ->
                val point = axis.points[vertex]
                testRecord("demo-element-${index + 1}", sic = "SIC-18").copy(routeCode = "PE-22A", roadbedCode = "CD",
                    latitude = point.latitude, longitude = point.longitude, gpsAccuracyM = 1.0, gpsTimestamp = now,
                    observations = "DEMO SINTÉTICA: no corresponde a una alcantarilla inspeccionada.")
            }
            records.forEachIndexed { index, r ->
                db.inventoryDao().insertRecord(r)
                db.inventoryDao().insertSic18(Sic18Entity(r.id, "06", "1", 1, "1", 1.0, null, "1", "1"))
                repeat(if (index == 0) 2 else 1) { photoIndex ->
                    val photo = PhotoEntity("demo-photo-${index + 1}-${photoIndex + 1}", r.id, photoIndex + 1,
                        original().absolutePath, photoIndex == 0, null, null, null, "PENDING", now,
                        description = "Imagen sintética para validar entrega; sin evidencia de campo.")
                    val fix = GeoLocation(requireNotNull(r.latitude) + photoIndex * .00001, requireNotNull(r.longitude), .8f, 321.25, timestamp = now)
                    db.inventoryDao().insertPhoto(PhotoCaptureMetadata(now, fix).applyTo(photo))
                }
            }
            val delivery = RouteDelivery(context, db).prepare("PE-22A", reference, MatchConfig())
            val paths = delivery.plan.folders.flatMap { it.photos }.map { it.plannedPath }
            assertEquals(listOf("PE-22A/ALCANTARILLAS/ALCANTARILLA UUID_demo-element-1/GENERAL/001_F01.jpg", "PE-22A/ALCANTARILLAS/ALCANTARILLA UUID_demo-element-1/GENERAL/002_F02.jpg",
                "PE-22A/ALCANTARILLAS/ALCANTARILLA UUID_demo-element-2/GENERAL/003_F01.jpg"), paths)
            val manifest = JSONObject(File(delivery.batch.manifestPath).readText())
            val root = File(delivery.batch.manifestPath).parentFile!!
            val output = File("build/outputs/round2/DEMO-entrega-PE-22A.zip").apply { parentFile.mkdirs() }
            java.util.zip.ZipOutputStream(output.outputStream()).use { zip ->
                val entries = manifest.getJSONArray("files")
                for (i in 0 until entries.length()) {
                    val entry = entries.getJSONObject(i); val payload = entry.getJSONObject("payload")
                    val name=payload.getString("logicalPath")+"/"+payload.getString("fileName")
                    zip.putNextEntry(java.util.zip.ZipEntry(name))
                    zip.write(File(root, entry.getString("localFile")).readBytes()); zip.closeEntry()
                }
                zip.putNextEntry(java.util.zip.ZipEntry("LEEME.txt"))
                zip.write("DEMOSTRACIÓN SINTÉTICA. Eje de referencia PE-22A real; elementos y fotos ficticios para pruebas. No subida a Drive.".toByteArray())
                zip.closeEntry()
            }
            ZipFile(output).use { assertEquals(8, it.size()) }
        } finally { db.close() }
    }
}
