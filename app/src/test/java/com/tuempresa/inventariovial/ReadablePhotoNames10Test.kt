package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.core.*
import org.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
class ReadablePhotoNames10Test {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun payload(id:String)=JSONObject().put("key","SIC/$id").put("kind","SIC").put("recordId",id).put("revision",1).put("pathVersion",4).put("routeCode","R10").put("familyPath","ALCANTARILLAS").put("family","ALCANTARILLA").put("files",JSONArray().put(JSONObject().put("key","photo/$id-photo").put("path","PANORAMICA/F_legacy.jpg").put("photoCategory","CULVERT_PANORAMIC")))
    private fun reservation(id:String,name:String="001_ALC7_PAN.jpg")=JSONObject().put("ok",true).put("photoNamingVersion",2).put("key","SIC/$id").put("revision",1).put("identityId",id).put("folderId","stable-folder").put("logicalPath","R10/ALCANTARILLAS/ALCANTARILLA 7").put("photos",JSONArray().put(JSONObject().put("id","$id-photo").put("category","CULVERT_PANORAMIC").put("name",name).put("historical",false)))
    @Test fun serverReservationPersistsStableNameWithoutClaimingDeliveryAck() {
        val p=payload("reserved10");ReadablePhotoNames.remember(context,p,reservation("reserved10"))
        assertEquals("001_ALC7_PAN.jpg",ReadablePhotoNames.name(context,"reserved10-photo","CULVERT_PANORAMIC"))
        assertEquals("stable-folder",DriveInventorySchema.receipt(context,"reserved10")!!.getString("folderId"))
        ReadablePhotoNames.remember(context,p,reservation("reserved10"))
        assertTrue(runCatching {ReadablePhotoNames.remember(context,p,reservation("reserved10","002_ALC7_PAN.jpg"))}.isFailure)
    }
    @Test fun wrongIdentityCategoryLengthDuplicateOrRevisionNeverOverwritesAReservation() {
        val p=payload("invalid10");val transformations=listOf<(JSONObject)->Unit>({it.put("revision",2)},{it.put("identityId","other")},{it.put("logicalPath","R10/ALCANTARILLAS/ALCANTARILLA 0")},{it.getJSONArray("photos").getJSONObject(0).put("category","GENERAL")},{it.getJSONArray("photos").getJSONObject(0).put("name","001_ALC123456789_PAN.jpg")},{it.getJSONArray("photos").put(it.getJSONArray("photos").getJSONObject(0))})
        transformations.forEach {change->assertTrue(runCatching {ReadablePhotoNames.remember(context,p,reservation("invalid10").apply(change))}.isFailure)}
        assertTrue(runCatching {ReadablePhotoNames.name(context,"invalid10-photo","CULVERT_PANORAMIC")}.isFailure)
    }
    @Test fun namingV2CapabilityGateMakesNoPostToAnOldBackend() {
        var posts=0;val client=okhttp3.OkHttpClient.Builder().addInterceptor {chain->if(chain.request().method!="GET")posts++
            okhttp3.Response.Builder().request(chain.request()).protocol(okhttp3.Protocol.HTTP_1_1).code(200).message("mock").body(okhttp3.ResponseBody.create(null,"{\"ok\":true,\"syncProtocol\":3,\"pathVersion\":4,\"stableFamilyNumbering\":true,\"photoCategoryVersion\":1}")).build()}.build()
        val http=AutomaticDriveHttp(client,"https://test.invalid/exec","")
        assertFalse(http.supportsReadablePhotoNames());assertTrue(runCatching {http.reservePhotoNames(payload("gate10"))}.isFailure);assertEquals(0,posts)
    }
    @Test fun readableCapabilityCoexistsWithLegacyV1() {
        val client=okhttp3.OkHttpClient.Builder().addInterceptor {chain->okhttp3.Response.Builder().request(chain.request()).protocol(okhttp3.Protocol.HTTP_1_1).code(200).message("mock").body(okhttp3.ResponseBody.create(null,"{\"ok\":true,\"syncProtocol\":3,\"pathVersion\":4,\"stableFamilyNumbering\":true,\"photoCategoryVersion\":1,\"photoNamingVersion\":1,\"readablePhotoNamingVersion\":2}")).build()}.build()
        assertTrue(AutomaticDriveHttp(client,"https://test.invalid/exec","").supportsReadablePhotoNames())
    }
}
