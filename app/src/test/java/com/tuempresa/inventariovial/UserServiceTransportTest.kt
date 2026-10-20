package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.auth.*
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.Source
import okio.Timeout
import okio.buffer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/** Every HTTP exchange is intercepted in memory; no request can reach a real service. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class UserServiceTransportTest {
    private val url = "https://script.google.com/macros/s/FAKE_TEST/exec"
    private val token = "a".repeat(43)
    private val requests = mutableListOf<Request>()
    private lateinit var settings: UserServiceSettings
    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("user_service", Context.MODE_PRIVATE).edit().clear().commit()
        // Test-only opaque vault, so persisted preferences do not contain test plaintext either.
        val vault = mutableMapOf<String, String>()
        settings = UserServiceSettings(context, object : CredentialProtector {
            override fun encrypt(plaintext: String, purpose: String): String = java.util.UUID.randomUUID().toString().also { vault[it] = plaintext }
            override fun decrypt(ciphertext: String, purpose: String): String = vault.getValue(ciphertext)
        })
    }
    private fun transport(reply: (Request, Int) -> Response): UserServiceTransport {
        val client = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).addInterceptor { chain ->
            requests += chain.request()
            reply(chain.request(), requests.size)
        }.build()
        return UserServiceTransport(client)
    }
    private fun response(request: Request, code: Int = 200, body: String = "{\"success\":true}", location: String? = null): Response =
        Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(code).message("fixture")
            .body(body.toResponseBody("application/json".toMediaType())).apply { if (location != null) header("Location", location) }.build()
    private fun config() = JSONObject().put("url", url).put("token", token)
    private fun invoke(transport: UserServiceTransport) = transport.request("syncUsers", JSONObject().put("clientVersion", -1), config(), "fictitious-fingerprint")
    private fun failure(block: () -> Unit): UserServiceException = try { block(); error("Expected rejection") }
        catch (error: UserServiceException) { error }

    @Test fun firstConfigurationValidatesAndDoesNotOverwrite() {
        assertTrue(runCatching { settings.provision("https://example.invalid/exec", token) }.isFailure)
        assertFalse(settings.configured)
        assertTrue(runCatching { settings.provision(url, "invalid") }.isFailure)
        assertFalse(settings.configured)
        settings.provision(url, token)
        assertEquals(config().toString(), settings.read().toString())
        assertTrue(runCatching { settings.provision(url, "b".repeat(43)) }.isFailure)
        assertEquals(token, settings.read().getString("token"))
    }
    @Test fun postUsesOnlyUsersContract() {
        invoke(transport { r, _ -> response(r) })
        val request = requests.single()
        assertEquals("POST", request.method)
        val json = JSONObject(Buffer().also { request.body!!.writeTo(it) }.readUtf8())
        assertEquals(setOf("action", "clientVersion", "deviceId", "tabletToken"), json.keys().asSequence().toSet())
        assertEquals(token, json.getString("tabletToken"))
    }
    @Test fun appsScriptRedirectIsGetWithoutPostBodyOrAuthorization() {
        val t = transport { r, n -> if (n == 1) response(r, 302, location = "https://script.googleusercontent.com/macros/echo?fixture=one") else response(r) }
        assertTrue(invoke(t).getBoolean("success"))
        assertEquals(2, requests.size)
        assertEquals("GET", requests[1].method)
        assertNull(requests[1].body)
        assertNull(requests[1].header("Authorization"))
        assertFalse(requests[1].url.toString().contains(token))
    }
    @Test fun unsafeOrMalformedRedirectsNeverReceiveSecondRequest() {
        for (target in listOf("http://script.googleusercontent.com/echo", "https://evil.invalid/echo", "https://script.googleusercontent.com.evil.invalid/echo",
            "https://script.googleusercontent.com:444/echo", "https://user:pass@script.googleusercontent.com/echo", "/relative", "not a URL", "https://script.googleusercontent.com/echo#fragment")) {
            requests.clear()
            val error = failure { invoke(transport { r, _ -> response(r, 302, location = target) }) }
            assertEquals(UserServiceStage.REDIRECT, error.stage)
            assertEquals(1, requests.size)
            assertFalse(error.toString().contains(target))
        }
    }
    @Test fun secondRedirectAndMissingLocationAreRejected() {
        val error = failure { invoke(transport { r, _ -> response(r, 302, location = "https://script.googleusercontent.com/echo") }) }
        assertEquals(UserServiceStage.REDIRECT, error.stage)
        assertEquals(2, requests.size)
        requests.clear()
        assertEquals(UserServiceStage.REDIRECT, failure { invoke(transport { r, _ -> response(r, 302) }) }.stage)
        assertEquals(1, requests.size)
    }
    @Test fun allSupportedRedirectCodesKeepSecretsInInitialPostOnly() {
        for (code in listOf(301, 302, 303, 307, 308)) {
            requests.clear()
            invoke(transport { r, n -> if (n == 1) response(r, code, location = "https://script.googleusercontent.com/echo") else response(r) })
            assertEquals("GET", requests.last().method)
            assertNull(requests.last().body)
        }
    }
    @Test fun httpErrorsShowStageAndStatusWithoutPayload() {
        for (code in listOf(401, 403, 404, 429, 500, 503)) {
            val error = failure { invoke(transport { r, _ -> response(r, code, body = token) }) }
            assertEquals(UserServiceStage.HTTP, error.stage)
            assertEquals(code, error.httpStatus)
            assertFalse(error.toString().contains(token))
        }
    }
    @Test fun htmlMalformedAndInventoryContractAreNotSuccessfulUserResponses() {
        for (payload in listOf("<html>Login</html>", "{broken", "{\"ok\":true}", "{\"success\":\"true\"}", "{\"success\":1}", "{}")) {
            assertEquals(UserServiceStage.JSON, failure { invoke(transport { r, _ -> response(r, body = payload) }) }.stage)
        }
    }
    @Test fun revokedAndInvalidCredentialsRemainDenials() {
        for (code in listOf("DEVICE_REVOKED", "DEVICE_REBIND_REQUIRED", "INVALID_CREDENTIALS")) {
            val error = failure { invoke(transport { r, _ -> response(r, body = JSONObject().put("success", false).put("error", code).toString()) }) }
            assertEquals(code, error.code)
            assertEquals(UserServiceStage.SERVER, error.stage)
        }
    }
    @Test fun arbitraryRemoteErrorNeverEntersException() {
        val error = failure { invoke(transport { r, _ -> response(r, body = JSONObject().put("success", false).put("error", "secret=$token").toString()) }) }
        assertEquals("SERVICE_ERROR", error.code)
        assertFalse(error.toString().contains(token))
        assertNull(error.cause)
    }
    @Test fun transportFailureBeforeResponseIsDistinctFromBodyFailure() {
        assertEquals(UserServiceStage.POST, failure { invoke(transport { _, _ -> throw IOException(token) }) }.stage)
        val body = object : ResponseBody() {
            override fun contentType(): MediaType? = null
            override fun contentLength() = -1L
            override fun source() = object : Source {
                override fun read(sink: Buffer, byteCount: Long): Long = throw IOException(token)
                override fun timeout() = Timeout.NONE
                override fun close() = Unit
            }.buffer()
        }
        val error = failure { invoke(transport { r, _ -> response(r).newBuilder().body(body).build() }) }
        assertEquals(UserServiceStage.RESPONSE, error.stage)
        assertFalse(error.toString().contains(token))
    }
    @Test fun excessiveResponseIsBounded() {
        assertEquals(UserServiceStage.RESPONSE, failure { invoke(transport { r, _ -> response(r, body = " ".repeat(2_000_001)) }) }.stage)
    }
    @Test fun invalidLocalConfigurationDoesNotSendPost() {
        val t = transport { r, _ -> response(r) }
        val error = failure { t.request("syncUsers", JSONObject(), config().put("token", "bad"), "fixture") }
        assertEquals(UserServiceStage.CONFIGURATION, error.stage)
        assertTrue(requests.isEmpty())
    }
    @Test fun offlineAndUnauthorizedDeviceNeverSendPost() = runBlocking {
        settings.provision(url, token)
        for ((code, stage) in listOf("OFFLINE" to UserServiceStage.NETWORK, "LOCAL_DEVICE_DENIED" to UserServiceStage.LOCAL_AUTHORIZATION)) {
            val service = HttpsUserService(settings, { throw UserServiceException(code, stage) }, transport { r, _ -> response(r) })
            val error = runCatching { service.call("syncUsers") }.exceptionOrNull() as UserServiceException
            assertEquals(stage, error.stage)
            assertTrue(requests.isEmpty())
        }
    }
    @Test fun rejectedRotationKeepsExistingCredentialAndErasesInput() = runBlocking {
        settings.provision(url, token)
        val candidate = "b".repeat(43).toCharArray()
        val service = HttpsUserService(settings, { "fixture" }, transport { r, _ -> response(r, body = "{\"success\":false,\"error\":\"DEVICE_REVOKED\"}") })
        assertTrue(runCatching { service.acceptRotatedToken(candidate) }.isFailure)
        assertEquals(token, settings.read().getString("token"))
        assertTrue(candidate.all { it == '\u0000' })
    }
    @Test fun acceptedRotationKeepsUrlAndFingerprintAndErasesInput() = runBlocking {
        settings.provision(url, token)
        val candidate = "b".repeat(43).toCharArray()
        val service = HttpsUserService(settings, { "fixture" }, transport { r, _ -> response(r, body = "{\"success\":true,\"version\":1,\"changed\":true,\"users\":[]}") })
        service.acceptRotatedToken(candidate)
        assertEquals("b".repeat(43), settings.read().getString("token"))
        assertEquals(url, settings.read().getString("url"))
        assertTrue(candidate.all { it == '\u0000' })
        val body = JSONObject(Buffer().also { requests.single().body!!.writeTo(it) }.readUtf8())
        assertEquals("fixture", body.getString("deviceId"))
        assertEquals(-1, body.getInt("clientVersion"))
    }
    @Test fun malformedRotationReplyDoesNotReplaceCredential() = runBlocking {
        settings.provision(url, token)
        val candidate = "b".repeat(43).toCharArray()
        val service = HttpsUserService(settings, { "fixture" }, transport { r, _ -> response(r, body = "{\"ok\":true}") })
        assertTrue(runCatching { service.acceptRotatedToken(candidate) }.isFailure)
        assertEquals(token, settings.read().getString("token"))
        assertTrue(candidate.all { it == '\u0000' })
    }
}
