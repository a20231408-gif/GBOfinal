package com.tuempresa.inventariovial.auth

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.tuempresa.inventariovial.access.DeviceAccessManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONException
import java.io.IOException
import java.util.concurrent.TimeUnit

interface UserService {
    suspend fun call(action: String, fields: JSONObject = JSONObject()): JSONObject
}
enum class UserServiceStage(val label: String) {
    CONFIGURATION("configuración local"), LOCAL_AUTHORIZATION("autorización local"),
    NETWORK("conexión"), POST("envío HTTPS"), REDIRECT("redirección"),
    HTTP("respuesta HTTP"), RESPONSE("lectura de respuesta"), JSON("contrato JSON de Usuarios"),
    USER_DATA("validación de usuarios"), LOCAL_STORAGE("almacenamiento local"), SERVER("autorización del servidor")
}

class UserServiceException(val code: String, val stage: UserServiceStage? = null, val httpStatus: Int? = null) : Exception((when(code) {
    "DEVICE_REVOKED" -> "El acceso de esta tablet fue revocado. Contacta con GBO."
    "DEVICE_REBIND_REQUIRED" -> "La huella de esta tablet cambió. GBO debe autorizar la nueva huella en el servicio de usuarios; no borres los datos de la aplicación. Después pulsa Sincronizar usuarios."
    "ADMIN_REQUIRED", "SESSION_EXPIRED" -> "Vuelve a autorizar la administración con tu PIN."
    "LAST_ADMIN" -> "No puedes quitar el último administrador activo."
    "TABLET_ADMIN_REQUIRED" -> "No tienes permiso para administrar tablets. Contacta con GBO."
    "TABLET_SELF_REVOKE" -> "No puedes revocar esta tablet desde la sesión actual."
    "TABLET_EXISTS" -> "Esta huella ya está registrada. Selecciona la tablet existente."
    "DUPLICATE_USERNAME" -> "El nombre de usuario ya está registrado."
    "RATE_LIMITED" -> "Demasiados intentos. Espera cinco minutos."
    "INVALID_CREDENTIALS" -> "Usuario o PIN incorrecto."
    "OFFLINE" -> "Esta acción requiere conexión a Internet."
    "INVALID_REQUEST" -> "Revisa los datos ingresados."
    "SERVICE_CONFIGURATION" -> "El servicio de usuarios no está disponible para esta tablet. Comprueba que el Apps Script esté implementado como aplicación web, con acceso permitido y URL /exec correcta."
    "SERVICE_ERROR" -> "Error de comunicación con el servicio de usuarios. Comprueba la implementación del Apps Script e inténtalo nuevamente."
    "NETWORK_ERROR" -> "No se pudo establecer comunicación HTTPS con el servicio de usuarios. Revisa la conexión a Internet e inténtalo nuevamente."
    "LOCAL_DEVICE_DENIED" -> "Esta tablet no está autorizada localmente. Contacta con GBO; no borres sus datos."
    "INVALID_USER_RESPONSE" -> "El servicio no devolvió una respuesta válida de Usuarios. GBO debe revisar el contrato del servicio."
    "LOCAL_STORAGE_ERROR" -> "No se pudo leer o guardar la información protegida. Conserva los datos y contacta con GBO."
    else -> "No se pudo contactar con el servicio de usuarios. Inténtalo nuevamente."
}) + (stage?.let { " Etapa: ${it.label}." } ?: "") + (httpStatus?.let { " HTTP $it." } ?: ""))

class UserServiceSettings(context: Context, private val protector: CredentialProtector) {
    private val preferences = context.getSharedPreferences("user_service", Context.MODE_PRIVATE)
    val configured get() = preferences.contains("provisioning")
    fun read(): JSONObject = JSONObject(protector.decrypt(checkNotNull(preferences.getString("provisioning", null)), "tablet-provisioning"))
    fun provision(url: String, token: String) {
        check(!configured) { "La tablet ya está configurada." }
        validateUrl(url.trim())
        require(token.matches(Regex("[A-Za-z0-9_-]{43}"))) { "Token de tablet inválido." }
        val blob = protector.encrypt(JSONObject().put("url", url.trim()).put("token", token).toString(), "tablet-provisioning")
        check(preferences.edit().putString("provisioning", blob).commit())
    }
    internal fun clearInitialProvisioning() { check(preferences.edit().remove("provisioning").commit()) }
    internal fun storeVerifiedRotation(token:String) {
        require(token.matches(Regex("[A-Za-z0-9_-]{43}")))
        val config=read().put("token",token)
        val blob=protector.encrypt(config.toString(),"tablet-provisioning")
        check(preferences.edit().putString("provisioning",blob).commit())
    }
    companion object {
        fun validateUrl(value: String) {
            val url = value.toHttpUrl()
            require(url.scheme == "https" && url.host == "script.google.com" && url.port == 443 &&
                url.username.isEmpty() && url.password.isEmpty() && url.query == null && url.fragment == null &&
                url.encodedPath.matches(Regex("/macros/s/[A-Za-z0-9_-]+/exec"))) {
                "Ingresa la URL HTTPS /exec del Apps Script de usuarios."
            }
        }
    }
}

fun hasUserServiceNetwork(context: Context): Boolean {
    val manager = context.getSystemService(ConnectivityManager::class.java)
    val caps = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
    // VALIDATED is not always reported on corporate/restricted Wi-Fi although HTTPS works.
    // The real authorization still happens server-side over HTTPS, never from this hint.
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

class HttpsUserService internal constructor(
    private val settings: UserServiceSettings,
    private val preflight: suspend () -> String,
    private val transport: UserServiceTransport
) : UserService {
    constructor(context: Context, settings: UserServiceSettings) : this(settings, {
        if (!hasUserServiceNetwork(context)) throw UserServiceException("OFFLINE", UserServiceStage.NETWORK)
        val access = DeviceAccessManager(context)
        if (!access.check().authorized) throw UserServiceException("LOCAL_DEVICE_DENIED", UserServiceStage.LOCAL_AUTHORIZATION)
        access.fingerprint()
    }, UserServiceTransport())

    private fun configuration(): JSONObject = try { settings.read() }
        catch (_: Exception) { throw UserServiceException("LOCAL_STORAGE_ERROR", UserServiceStage.CONFIGURATION) }

    override suspend fun call(action: String, fields: JSONObject): JSONObject = request(action, fields, configuration())
    /** Keep the existing URL and fingerprint. Persist only after the server accepts the supplied token. */
    suspend fun acceptRotatedToken(token:CharArray) {
        try {
            val candidate=String(token)
            require(candidate.matches(Regex("[A-Za-z0-9_-]{43}")))
            val config=configuration().put("token",candidate)
            request("syncUsers",JSONObject().put("clientVersion",-1),config)
            try { settings.storeVerifiedRotation(candidate) }
            catch (_: Exception) { throw UserServiceException("LOCAL_STORAGE_ERROR", UserServiceStage.LOCAL_STORAGE) }
        }finally{token.fill('\u0000')}
    }
    private suspend fun request(action:String,fields:JSONObject,config:JSONObject):JSONObject = withContext(Dispatchers.IO) {
        val fingerprint = try { preflight() }
        catch (cancel: CancellationException) { throw cancel }
        catch (error: UserServiceException) { throw error }
        catch (_: Exception) { throw UserServiceException("LOCAL_DEVICE_DENIED", UserServiceStage.LOCAL_AUTHORIZATION) }
        transport.request(action, fields, config, fingerprint)
    }
}

/** Isolated transport: injectable only internally for tests; production uses strict HTTPS redirects. */
internal class UserServiceTransport(private val client: Call.Factory = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).callTimeout(75, TimeUnit.SECONDS)
    .followRedirects(false).followSslRedirects(false).build()) {
    fun request(action: String, fields: JSONObject, config: JSONObject, fingerprint: String): JSONObject {
        val url: String
        val token: String
        try {
            url = config.getString("url")
            UserServiceSettings.validateUrl(url)
            token = config.getString("token")
            require(token.matches(Regex("[A-Za-z0-9_-]{43}")))
        } catch (_: Exception) { throw UserServiceException("SERVICE_CONFIGURATION", UserServiceStage.CONFIGURATION) }
        val body = JSONObject(fields.toString()).put("action", action).put("deviceId", fingerprint).put("tabletToken", token)
        var request = Request.Builder().url(url)
            .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType())).build()
        // Apps Script returns a one-time ContentService URL. Never resend the secret POST to a redirect.
        var json: JSONObject? = null
        for (attempt in 0..1) {
            val response = try { client.newCall(request).execute() }
            catch (_: IOException) { throw UserServiceException("NETWORK_ERROR", if (attempt == 0) UserServiceStage.POST else UserServiceStage.REDIRECT) }
            try { response.use {
                if (response.code in listOf(301, 302, 303, 307, 308)) {
                    if (attempt != 0) throw UserServiceException("SERVICE_CONFIGURATION", UserServiceStage.REDIRECT, response.code)
                    val target = response.header("Location")?.toHttpUrlOrNull()
                        ?: throw UserServiceException("SERVICE_CONFIGURATION", UserServiceStage.REDIRECT, response.code)
                    if (target.scheme != "https" || target.host != "script.googleusercontent.com" || target.port != 443 ||
                        target.username.isNotEmpty() || target.password.isNotEmpty() || target.fragment != null)
                        throw UserServiceException("SERVICE_CONFIGURATION", UserServiceStage.REDIRECT, response.code)
                    request = Request.Builder().url(target).get().build()
                } else {
                    if (response.code == 401 || response.code == 403) throw UserServiceException("SERVICE_CONFIGURATION", UserServiceStage.HTTP, response.code)
                    if (!response.isSuccessful) throw UserServiceException("SERVICE_ERROR", UserServiceStage.HTTP, response.code)
                    val source = response.body?.source() ?: throw UserServiceException("INVALID_USER_RESPONSE", UserServiceStage.RESPONSE)
                    val buffer = okio.Buffer()
                    while (buffer.size <= 2_000_000 && source.read(buffer, 8192) != -1L) { /* bounded response */ }
                    if (buffer.size > 2_000_000) throw UserServiceException("INVALID_USER_RESPONSE", UserServiceStage.RESPONSE)
                    json = try { JSONObject(buffer.readUtf8()) }
                    catch (_: JSONException) { throw UserServiceException("INVALID_USER_RESPONSE", UserServiceStage.JSON) }
                }
            } } catch (_: IOException) { throw UserServiceException("NETWORK_ERROR", UserServiceStage.RESPONSE) }
            if (json != null) break
        }
        val result = json ?: throw UserServiceException("INVALID_USER_RESPONSE", UserServiceStage.JSON)
        if (result.opt("success") !is Boolean) throw UserServiceException("INVALID_USER_RESPONSE", UserServiceStage.JSON)
        if (!result.getBoolean("success")) {
            // Never put arbitrary remote strings (which may contain secrets) in exceptions or diagnostics.
            val code = result.optString("error").takeIf { it in REMOTE_ERRORS } ?: "SERVICE_ERROR"
            throw UserServiceException(code, UserServiceStage.SERVER)
        }
        return result
    }
    companion object {
        private val REMOTE_ERRORS = setOf("DEVICE_REVOKED", "DEVICE_REBIND_REQUIRED", "ADMIN_REQUIRED", "SESSION_EXPIRED",
            "LAST_ADMIN", "TABLET_ADMIN_REQUIRED", "TABLET_SELF_REVOKE", "TABLET_EXISTS", "DUPLICATE_USERNAME",
            "RATE_LIMITED", "INVALID_CREDENTIALS", "INVALID_REQUEST", "SERVICE_CONFIGURATION", "SERVICE_ERROR")
    }
}
