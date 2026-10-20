package com.tuempresa.inventariovial

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.delivery.RouteDelivery
import com.tuempresa.inventariovial.gis.KmlExporter
import com.tuempresa.inventariovial.road.*
import com.tuempresa.inventariovial.sic.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.json.JSONObject
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28], application=android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DriveFamilies2ATest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    @Test fun allSecondaryClassesHaveAnExplicitFamilyAndNoSib() {
        val base=testRecord(route="PE-22A").copy(latitude=null,longitude=null)
        val expected19=listOf("CUNETAS","CANALES","BAJADAS_AGUA","ZANJAS_DRENAJE","ZANJAS_CORONACION","CUNETAS_BANQUETA")
        for((i,code) in listOf("08","09","10","11","12","13").withIndex()) {
            val item=InventoryRecordWithPhotos(base,emptyList(),sic19=Sic19Entity(base.id,code,"1","1","1","1"))
            assertEquals(expected19[i],DriveInventorySchema.familyPath(AssetPresentationCatalog.resolve(item)))
        }
        for((code,path) in mapOf("21" to "DERECHO_DE_VIA","22" to "ZONAS_URBANAS","23" to "PUNTOS_ESPECIFICOS","24" to "CANTERAS")) {
            val item=InventoryRecordWithPhotos(base.copy(sicCode="SIC-23"),emptyList(),sic23=Sic23Entity(base.id,code,null,null,""))
            assertEquals(path,DriveInventorySchema.familyPath(AssetPresentationCatalog.resolve(item)))
        }
        for((code,path) in mapOf("18" to "MARCAS","19" to "SEGURIDAD_VIAL","20" to "TACHAS")) {
            val item=InventoryRecordWithPhotos(base.copy(sicCode="SIC-21"),emptyList(),sic21=Sic21Entity(base.id,code,"1","5","1"))
            assertEquals("SENALIZACION_Y_SEGURIDAD_VIAL/$path",DriveInventorySchema.familyPath(AssetPresentationCatalog.resolve(item)))
        }
    }
    @Test fun orderingUsesRouteRoadbedDirectionNumericProgressiveAndUuid() {
        fun item(id:String,pr:String,direction:String="INCREASING",bed:String="CD")=InventoryRecordWithPhotos(testRecord(id,pr,0.0,route="PE-22A",roadbed=bed).copy(surveyDirection=direction,latitude=null,longitude=null),emptyList())
        val rows=listOf(item("b","10"),item("a","2"),item("d","2","DECREASING"),item("c","10","DECREASING"),item("tie","2"))
        assertEquals(listOf("c","d","a","tie","b"),RoadOrdering.orderAssetsForRoute(rows).map {it.recordId})
        val changed=rows.map {if(it.record.id=="b")it.copy(record=it.record.copy(startPrCode="1"))else it}
        assertEquals(listOf("c","d","b","a","tie"),RoadOrdering.orderAssetsForRoute(changed).map {it.recordId})
    }
    @Test fun categoriesUseRealCodesAndAllowFutureKeysWithoutGuessingFileNames() {
        assertEquals("AGUAS_ARRIBA",DriveInventorySchema.category("UPSTREAM"))
        assertEquals("AGUAS_ABAJO",DriveInventorySchema.category("DOWNSTREAM"))
        assertEquals("ENTRADA",DriveInventorySchema.category("CULVERT_INLET"))
        assertEquals("INICIO",DriveInventorySchema.category("INICIO"))
        assertEquals("GENERAL",DriveInventorySchema.category(null))
        assertThrows(IllegalArgumentException::class.java) {DriveInventorySchema.category("../secret")}
    }
    @Test fun kmzIncludesFajasCriticalAccidentsAndSecondaryFamiliesWithoutInventedCoordinates() {
        val records=listOf("SIC-08","SIC-09","SIC-13","SIC-34","SIC-17","SIC-18","SIC-22").mapIndexed {i,code->
            testSnapshot(testRecord("uuid-$i",route="PE-22A",sic=code).copy(latitude=null,longitude=null))
        }
        val kml=KmlExporter.render(records)
        records.forEach {assertTrue(kml.contains(it.record.id))}
        assertTrue(kml.contains("FAJAS") || kml.contains("CALZADA_FAJAS"));assertTrue(kml.contains("ANCHOS_FAJA"))
        assertFalse(kml.contains("<Point>"));assertFalse(kml.contains("<coordinates>"));assertFalse(kml.contains("SIB-"))
    }
    @Test fun newFajaFormatsNeverShareIdentityEvenWithLegacySourceLink()=runBlocking(Dispatchers.IO) {
        Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build().use {db->
            SicFieldRepository(db).save("independent08",fajaState(SicFieldFormat.SIC08),"tester",true)
            SicFieldRepository(db).save("independent09",fajaState(SicFieldFormat.SIC09).copy(sourceRecordId="independent08"),"tester",true)
            val a=AutomaticDelivery.prepare(context,db,db.automaticSyncDao().get("SIC/independent08")!!)
            val b=AutomaticDelivery.prepare(context,db,db.automaticSyncDao().get("SIC/independent09")!!)
            assertEquals("FAJAS_CALZADA",a.getString("familyPath"));assertEquals("ANCHOS_FAJA",b.getString("familyPath"))
            assertFalse(b.has("identityId"));assertFalse(b.has("sibCode"));assertEquals("independent09",b.getString("recordId"))
        }
    }
    @Test fun actualAndroidPayloadsAndOfficialExcelRowsForBackendTests()=runBlocking(Dispatchers.IO) {
        Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build().use {db->
            val specs=listOf(Triple("culvert-a","SIC-18","06"),Triple("culvert-b","SIC-18","07"),Triple("ford-a","SIC-20","12"),Triple("wall-a","SIC-20","14"),Triple("tunnel-a","SIC-20","13"),Triple("mark-a","SIC-21","18"),Triple("safety-a","SIC-21","19"),Triple("stud-a","SIC-21","20"))
            for((index,spec) in specs.withIndex()) {
                val(id,code,cls)=spec
                val r=testRecord(id,pr=if(index==1)"2" else "10",route="PE-22A",sic=code).copy(assetType="",latitude=null,longitude=null,surveyDate="11/10/2026",surveyDirection="INCREASING",updatedAt=1000L+index)
                db.inventoryDao().insertRecord(r)
                when(code) {
                    "SIC-18" -> db.inventoryDao().insertSic18(Sic18Entity(id,cls,"1",1,"1",1.0,1.0,"1","1"))
                    "SIC-20" -> db.inventoryDao().insertSic20(Sic20Entity(id,cls,"1",2.0,1.0,"1","1"))
                    "SIC-21" -> db.inventoryDao().insertSic21(Sic21Entity(id,cls,"1","5","1"))
                }
                val f=File(context.cacheDir,"$id.jpg");val bitmap=Bitmap.createBitmap(32,24,Bitmap.Config.ARGB_8888)
                f.outputStream().use {bitmap.compress(Bitmap.CompressFormat.JPEG,95,it)};bitmap.recycle()
                val category=if(code=="SIC-18")"CULVERT_INLET" else "GENERAL"
                db.inventoryDao().insertPhoto(PhotoEntity("photo-$id",id,1,f.path,true,null,null,null,"PENDING",1,photoCategory=category))
                val intent=DriveSyncIntent("SIC/$id","SIC",id,r.updatedAt)
                db.automaticSyncDao().put(intent)
                val payload=AutomaticDelivery.prepare(context,db,intent)
                assertEquals(4,payload.getInt("pathVersion"));assertFalse(payload.has("sibCode"))
                val target=File("build/outputs/cierre-1.4.5/drive-familias/examples/$id").apply {mkdirs()}
                val remote=JSONObject(payload.toString())
                for(n in 0 until remote.getJSONArray("files").length()) {
                    val file=remote.getJSONArray("files").getJSONObject(n);val bytes=File(file.getString("localFile")).readBytes()
                    file.remove("localFile");file.put("base64",android.util.Base64.encodeToString(bytes,android.util.Base64.NO_WRAP))
                }
                File(target,"payload.json").writeText(remote.toString(2))
            }
        }
    }
    @Test fun pavementFormatsParticipateWithoutSyntheticGpsAndKeepOriginalHeaders() {
        val sample=com.tuempresa.inventariovial.pavement.PavementSampleEntity("sample-a","TRAMO","PE-22A",1,"1","CD","0010","0011",3.0,10.0,"ASPHALT","11/10/2026","tester","Tester",status="CLOSED")
        val damage=com.tuempresa.inventariovial.pavement.PavementDamageEntity("damage-a",sample.id,1,"ASPHALT",1,"Ficticio","L","AREA",null,null,null,null,null,1.0,2.0,2.0,null,null,"Prueba")
        val fields=mapOf("faja" to "1","prInicio" to "0010","distanciaInicio" to "12.5","prFin" to "0011","distanciaFin" to "20","longitud" to "2","ancho" to "1","gravedad" to "1")
        val format=com.tuempresa.inventariovial.pavement.PavementSicFormat.SIC14
        val updated=sample.copy(sicDataJson=com.tuempresa.inventariovial.pavement.PavementSic.update(sample,format,damage.id,fields))
        val rows=InventoryOrderingProjection.pavement(updated,listOf(damage))
        assertTrue(rows.any {it.record.sicCode=="SIC-14"});assertTrue(rows.any {it.record.sicCode=="SIC-14A"})
        assertTrue(rows.all {it.record.latitude==null && it.record.longitude==null})
        assertTrue(RoadOrdering.orderAssetsForRoute(rows.map(RouteDelivery::row)).none {it.assetFamily==AssetFamily.OTROS})
        val target=File("build/outputs/cierre-1.4.5/drive-familias/examples/pavement-a").apply {mkdirs()}
        File(target,"SIC-14.xlsx").outputStream().use {com.tuempresa.inventariovial.pavement.PavementSic.write(it,updated,listOf(damage),format)}
    }

    @Test fun visibleIdentityIsStableWhenTechnicalOrderChanges() {
        val row=RoadOrdering.orderAssetsForRoute(listOf(InventoryRecordWithPhotos(testRecord("stable-id",sic="SIC-18",route="PE-22A"),emptyList()))).single()
        assertEquals(row.shortCode,row.copy(sequence=99).shortCode)
        val bound=row.copy(remoteLogicalPath="PE-22A/ALCANTARILLAS/ALCANTARILLA 7")
        assertEquals("ALC-7",bound.shortCode);assertEquals("ALCANTARILLA 7",bound.copy(sequence=1).folderName)
    }

}
