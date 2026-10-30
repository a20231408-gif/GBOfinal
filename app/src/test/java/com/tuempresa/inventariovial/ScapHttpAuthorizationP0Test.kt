package com.tuempresa.inventariovial

import com.tuempresa.inventariovial.core.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

class ScapHttpAuthorizationP0Test {
    private fun transport(code:Int,capabilitiesOk:Boolean)=AutomaticDriveHttp(OkHttpClient.Builder().addInterceptor {chain->
        val get=chain.request().method=="GET"
        val success=get && capabilitiesOk
        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(if(success)200 else code)
            .message("SYNTHETIC").body((if(success)"""{"ok":true,"syncProtocol":3,"pathVersion":4,"stableFamilyNumbering":true,"photoCategoryVersion":1,"readablePhotoNamingVersion":2}"""else "{}").toResponseBody("application/json".toMediaType())).build()
    }.build(),"https://example.invalid/p0","FICTITIOUS_ONLY")

    @Test fun authorizationOnCapabilitiesOrReservationIsNotRetriedAsBrokenConnectivity() {
        for(code in listOf(401,403))for(capabilitiesOk in listOf(false,true)) {
            val http=transport(code,capabilitiesOk)
            val failure=runCatching {if(capabilitiesOk)http.reservePhotoNames(JSONObject().put("files",JSONArray())) else http.supportsV3()}.exceptionOrNull()
            assertTrue(failure is SyncRejection)
            assertEquals("HTTP_$code",(failure as SyncRejection).code);assertFalse(failure.retryable)
        }
    }
    @Test fun serverFailureRemainsRecoverableCommunicationFailure() {
        val failure=runCatching{transport(503,false).supportsV3()}.exceptionOrNull()
        assertTrue(failure is SyncCommunicationFailure)
    }
}
