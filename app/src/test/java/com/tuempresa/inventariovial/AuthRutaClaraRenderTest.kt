package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.auth.*
import com.tuempresa.inventariovial.ui.theme.InventarioVialTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Actual Compose/Android rendering in Robolectric; these are NOT tablet captures. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AuthRutaClaraRenderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun render(configured: Boolean, landscape: Boolean, filename: String) = runBlocking {
        AuthGraph::class.java.getDeclaredField("instance").apply { isAccessible = true }.set(null, null)
        val app = ApplicationProvider.getApplicationContext<Context>()
        Settings.Secure.putString(app.contentResolver, Settings.Secure.ANDROID_ID, "a10c1a0000000001")
        app.getSharedPreferences("user_service", Context.MODE_PRIVATE).edit().clear().commit()
        val graph = AuthGraph.get(app)
        graph.database.syncContext = null
        if (configured) {
            app.getSharedPreferences("user_service", Context.MODE_PRIVATE).edit()
                .putString("provisioning", "UI_TEST_NOT_A_CREDENTIAL").commit()
            graph.database.userDao().saveSyncState(UserSyncStateEntity(usersVersion = 1, lastSuccessfulSyncAt = 1))
        }
        assertEquals(configured, graph.settings.configured)
        val image = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(app.resources,
            if (configured) R.drawable.gbo_login_background else R.drawable.gbo_setup_background, image)
        assertEquals(if (landscape) 2560 else 1600, image.outWidth)
        assertEquals(if (landscape) 1600 else 2560, image.outHeight)
        compose.setContent { InventarioVialTheme { LoginScreen(graph) } }
        compose.onNodeWithTag(if (landscape) "auth-landscape" else "auth-column").assertExists()
        if (configured) {
            // Wait for the asynchronous Room observation before capturing the ready form.
            compose.waitUntil(5_000) {
                compose.onAllNodes(hasText("Iniciar sesión") and isEnabled()).fetchSemanticsNodes().size == 1
            }
        }
        compose.waitForIdle()
        val target = File("build/outputs/ruta-clara/renderizados-locales/$filename.png")
        checkNotNull(target.parentFile).mkdirs()
        // PixelCopy requires a real Window renderer. Draw the actual Android view tree
        // synchronously for local visual QA; do not label this as a device screenshot.
        val bitmap = compose.runOnIdle {
            val view = compose.activity.window.decorView
            Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
        }
        target.outputStream().use { output ->
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
        }
        bitmap.recycle()
    }

    @Test @Config(qualifiers = "w800dp-h1280dp-port-mdpi")
    fun loginPortrait() = render(true, false, "login-vertical")

    @Test @Config(qualifiers = "w1280dp-h800dp-land-mdpi")
    fun loginLandscape() = render(true, true, "login-horizontal")

    @Test @Config(qualifiers = "w800dp-h1280dp-port-mdpi")
    fun setupPortrait() = render(false, false, "configuracion-vertical")

    @Test @Config(qualifiers = "w1280dp-h800dp-land-mdpi")
    fun setupLandscape() = render(false, true, "configuracion-horizontal")
}
