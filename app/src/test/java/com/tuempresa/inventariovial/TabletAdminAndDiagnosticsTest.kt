package com.tuempresa.inventariovial

import android.app.ApplicationExitInfo
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.auth.*
import com.tuempresa.inventariovial.diagnostics.StabilityDiagnostics
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class TabletAdminAndDiagnosticsTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    @Test fun engineerRequiresExplicitHintAndScopedTokenExpiresWithoutNormalLogoutAndCannotInvokeUserActions()=runBlocking {
        val hints=TabletAdminPermissions(context);hints.replace(JSONObject().put("tabletAdminUserIds",org.json.JSONArray()))
        assertTrue(hints.users.value.isEmpty())
        val sessions=AuthSessionManager();sessions.login(UserCacheEntity("engineer","engineer","Engineer","ENGINEER",true,"cipher",1,1,1))
        hints.replace(JSONObject().put("tabletAdminUserIds",org.json.JSONArray().put("engineer")))
        assertEquals(setOf("engineer"),hints.users.value)
        var time=100L;var calls=0
        val service=object:UserService {override suspend fun call(action:String,fields:JSONObject):JSONObject {
            calls++
            return if(action=="authenticateTabletAdmin")JSONObject().put("scope","TABLET_ADMIN_ONLY").put("tabletAdminToken","a".repeat(43)).put("expiresInSeconds",900)
            else JSONObject().put("success",true)
        }}
        val repo=TabletAdminRepository(service,sessions){time};val pin="123456".toCharArray();repo.authenticate(pin)
        assertTrue(pin.all{it=='\u0000'});assertTrue(repo.authorized)
        val before=calls;assertTrue(runCatching{repo.call("createUser")}.isFailure);assertEquals(before,calls)
        time+=900_000;assertFalse(repo.authorized)
        assertEquals("SESSION_EXPIRED",(runCatching{repo.call("listTablets")}.exceptionOrNull() as UserServiceException).code)
        assertEquals("engineer",sessions.requireUser().id)
    }
    @Test fun invalidScopeNeverAuthorizesAndBackgroundLockInvalidatesAdministration()=runBlocking {
        val sessions=AuthSessionManager();sessions.login(UserCacheEntity("id","u","n","ADMIN",true,"cipher",1,1,1))
        var scope="USERS_ADMIN"
        val service=object:UserService {override suspend fun call(action:String,fields:JSONObject)=JSONObject().put("scope",scope).put("tabletAdminToken","x".repeat(43)).put("expiresInSeconds",900)}
        val repo=TabletAdminRepository(service,sessions){1L}
        assertTrue(runCatching{repo.authenticate("123456".toCharArray())}.isFailure);assertFalse(repo.authorized)
        scope="TABLET_ADMIN_ONLY";repo.authenticate("123456".toCharArray());assertTrue(repo.authorized)
        sessions.lock();assertFalse(repo.authorized)
    }
    @Test fun diagnosticIsBoundedAndNeverSerializesExceptionMessagesPathsCoordinatesOrSecrets() {
        context.getSharedPreferences("stability_diagnostics",0).edit().clear().commit()
        val secret="PIN=123456 token=SHOULD_NOT_EXIST latitude=-12.123456 user=PRIVATE"
        val error=IllegalStateException(secret).apply{stackTrace=arrayOf(StackTraceElement("com.example.Capture","save","D:/private/$secret",23))}
        repeat(80){StabilityDiagnostics.record(context,"TRIM_MEMORY",trimLevel=15)}
        StabilityDiagnostics.screen("SCAP");StabilityDiagnostics.record(context,"UNCAUGHT_EXCEPTION",error)
        val report=StabilityDiagnostics.report(context)
        assertEquals(64,report.getJSONArray("events").length());assertEquals("CRASH",report.getJSONObject("lastExit").getString("reason"))
        assertFalse(report.toString().contains(secret));assertFalse(report.toString().contains("SHOULD_NOT_EXIST"));assertFalse(report.toString().contains("-12.123456"))
        assertEquals("java.lang.IllegalStateException",report.getJSONArray("events").getJSONObject(63).getString("exceptionClass"))
        assertTrue(StabilityDiagnostics.export(context).name.matches(Regex("gbo_diagnostics_[0-9]{8}\\.json")))
    }
    @Test fun unknownExitIsReportedHonestlyAndKnownReasonsRemainDistinct() {
        context.getSharedPreferences("stability_diagnostics",0).edit().clear().commit()
        assertEquals("Causa no disponible.",StabilityDiagnostics.report(context).getJSONObject("lastExit").getString("reason"))
        assertEquals("USER_REQUESTED",StabilityDiagnostics.reason(ApplicationExitInfo.REASON_USER_REQUESTED))
        assertEquals("USER_STOPPED",StabilityDiagnostics.reason(ApplicationExitInfo.REASON_USER_STOPPED))
        assertEquals("LOW_MEMORY",StabilityDiagnostics.reason(ApplicationExitInfo.REASON_LOW_MEMORY))
        assertEquals("ANR",StabilityDiagnostics.reason(ApplicationExitInfo.REASON_ANR))
        assertEquals("SIGNALED",StabilityDiagnostics.reason(ApplicationExitInfo.REASON_SIGNALED))
    }
}
