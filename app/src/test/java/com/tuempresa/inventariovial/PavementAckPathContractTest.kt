package com.tuempresa.inventariovial

import android.app.Application
import com.tuempresa.inventariovial.core.AutomaticDelivery
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class)
class PavementAckPathContractTest {
    private val root="PE-12A/EVALUACION_SUPERFICIAL/TRAMO_FICTICIO/RIGIDO/MUESTRA 7"

    private fun fixture(photoNamingVersion:Int=1):Pair<JSONObject,JSONObject> {
        val source=listOf(
            "field" to "HOJAS_CAMPO/HC_002.pdf",
            "sample_json" to "DATOS_APP/M_002.json",
            "photo/UUID_FICTICIO" to "PANEL_FOTOGRAFICO/001_F01.jpg"
        )
        val prepared=JSONObject().put("kind","PAVEMENT").put("key","PAVEMENT/UUID_FICTICIO")
            .put("recordId","UUID_FICTICIO").put("revision",2).put("routeCode","PE-12A")
            .put("tramo","TRAMO_FICTICIO").put("surface","RIGIDO").put("contentHash","HASH_FICTICIO")
            .put("photoNamingVersion",photoNamingVersion).put("sicRows",JSONArray())
            .put("files",JSONArray(source.map {(key,path)->JSONObject().put("key",key).put("path",path).put("sha256","SHA_FICTICIO_$key")}))
        val received=JSONObject().put("ok",true).put("syncProtocol",3).put("pathVersion",4)
            .put("key",prepared.getString("key")).put("revision",2).put("contentHash","HASH_FICTICIO")
            .put("folderId","CARPETA_FICTICIA").put("logicalPath",root).put("sicFiles",JSONArray())
            .put("files",JSONArray(source.mapIndexed {index,(key,path)->
                val finalPath=when(key){
                    "field"->"HOJAS_CAMPO/HC_007.pdf"
                    "sample_json"->"DATOS_APP/M_007.json"
                    else->"PANEL_FOTOGRAFICO/M007_001_F01.jpg"
                }
                JSONObject().put("key",key).put("sha256","SHA_FICTICIO_$key")
                    .put("fileId","ARCHIVO_FICTICIO_$index").put("folderId","CARPETA_FICTICIA")
                    .put("requestedPath",path).put("logicalPath","$root/$finalPath")
            }))
        return prepared to received
    }

    @Test fun exactServerRenamesAreAcceptedWithoutChangingFrozenPreparation() {
        for(version in listOf(1,2)) {
            val (prepared,received)=fixture(version)
            val frozen=prepared.toString()
            assertTrue(AutomaticDelivery.validAck(prepared,received))
            assertEquals(frozen,prepared.toString())
            assertEquals("UUID_FICTICIO",prepared.getString("recordId"))
            assertEquals(2L,prepared.getLong("revision"))
            assertEquals("HASH_FICTICIO",prepared.getString("contentHash"))
        }
    }

    @Test fun wrongFinalSubfolderCannotConfirmPavementFiles() {
        val (prepared,received)=fixture()
        val frozen=prepared.toString()
        received.getJSONArray("files").getJSONObject(0).put("logicalPath","$root/DATOS_APP/HC_007.pdf")
        assertFalse(AutomaticDelivery.validAck(prepared,received))
        assertEquals(frozen,prepared.toString())
        assertEquals("UUID_FICTICIO",prepared.getString("recordId"))
        assertEquals("HASH_FICTICIO",prepared.getString("contentHash"))
    }

    @Test fun wrongFinalNameCannotConfirmPavementPhoto() {
        val (prepared,received)=fixture(2)
        received.getJSONArray("files").getJSONObject(2).put("logicalPath","$root/PANEL_FOTOGRAFICO/OTRA_FOTO.jpg")
        assertFalse(AutomaticDelivery.validAck(prepared,received))
    }

    @Test fun publicFolderLabelStillUsesStableServerSequence() {
        val (prepared,received)=fixture(2)
        val labeled="MUESTRA 7 - Muestra ficticia"
        prepared.put("publicNamingVersion",1).put("publicElementFolder",labeled)
        received.put("logicalPath",root.substringBeforeLast('/')+"/"+labeled)
        val files=received.getJSONArray("files")
        for(n in 0 until files.length()) {
            val file=files.getJSONObject(n)
            file.put("logicalPath",file.getString("logicalPath").replace(root,received.getString("logicalPath")))
        }
        assertTrue(AutomaticDelivery.validAck(prepared,received))
    }
}
