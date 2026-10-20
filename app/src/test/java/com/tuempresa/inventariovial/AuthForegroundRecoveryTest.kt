package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.auth.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AuthForegroundRecoveryTest {
    private lateinit var graph: AuthGraph
    private val user = UserCacheEntity("fixture", "operator", "Ficticio", "OPERATOR", true, "opaque", 1, 1, 1)
    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AuthGraph::class.java.getDeclaredField("instance").apply { isAccessible = true }.set(null, null)
        Settings.Secure.putString(context.contentResolver, Settings.Secure.ANDROID_ID, "recovery-fixture")
        // No service configuration: foreground cannot enqueue a remote user sync.
        context.getSharedPreferences("user_service", Context.MODE_PRIVATE).edit().clear().commit()
        graph = AuthGraph.get(context)
        graph.database.syncContext = null
        graph.sessions.login(user)
    }
    @Test fun shortBackgroundKeepsCurrentSessionAndIdentity() {
        graph.background()
        ShadowSystemClock.advanceBy(Duration.ofMinutes(14))
        graph.foreground()
        assertEquals(user.id, graph.sessions.requireUser().id)
        assertFalse(graph.sessions.current.value!!.locked)
    }
    @Test fun fifteenMinutesRequiresSameUserToResumeWithoutDiscardingSession() {
        graph.background()
        ShadowSystemClock.advanceBy(Duration.ofMinutes(15))
        graph.foreground()
        assertEquals(user.id, graph.sessions.current.value!!.id)
        assertTrue(graph.sessions.current.value!!.locked)
        assertTrue(runCatching { graph.sessions.requireUser() }.isFailure)
        assertTrue(runCatching { graph.sessions.login(user.copy(id = "different")) }.isFailure)
        // PIN verification before this step is independently covered by UserAuthenticationTest.
        graph.sessions.login(user)
        assertFalse(graph.sessions.current.value!!.locked)
        assertEquals(user.id, graph.sessions.requireUser().id)
    }
}
