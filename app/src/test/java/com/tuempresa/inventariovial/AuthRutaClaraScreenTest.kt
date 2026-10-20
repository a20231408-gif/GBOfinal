package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.auth.*
import com.tuempresa.inventariovial.ui.theme.InventarioVialTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w800dp-h1280dp-port-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AuthRutaClaraScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val app get() = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var graph: AuthGraph

    @Before fun setup() {
        // AuthGraph keeps an Application/preferences reference; each Robolectric case
        // must use its own sandbox, including after an orientation qualifier changes.
        AuthGraph::class.java.getDeclaredField("instance").apply { isAccessible = true }.set(null, null)
        Settings.Secure.putString(app.contentResolver, Settings.Secure.ANDROID_ID, "a10c1a0000000001")
        app.getSharedPreferences("user_service", Context.MODE_PRIVATE).edit().clear().commit()
        graph = AuthGraph.get(app)
        graph.database.syncContext = null
    }

    private fun configured() = runBlocking {
        // Isolated Robolectric preferences, never a real token or service configuration.
        app.getSharedPreferences("user_service", Context.MODE_PRIVATE).edit()
            .putString("provisioning", "UI_TEST_NOT_A_CREDENTIAL").commit()
        graph.database.userDao().saveSyncState(UserSyncStateEntity(usersVersion = 1, lastSuccessfulSyncAt = 1))
    }

    @Test fun portraitSetupKeepsFingerprintFieldsAndOriginalValidation() {
        compose.setContent { InventarioVialTheme { LoginScreen(graph) } }
        compose.onNodeWithTag("auth-column").assertExists()
        compose.onNodeWithText("Huella de esta tablet:").performScrollTo().assertIsDisplayed()
        compose.onNode(hasSetTextAction() and hasText("URL HTTPS del servicio de usuarios"))
            .performScrollTo().performTextInput("https://example.invalid/exec")
        compose.onNode(hasSetTextAction() and hasText("Token de esta tablet"))
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Guardar configuración y sincronizar").performScrollTo().performClick()
        compose.onNodeWithText("No se pudo guardar. Revisa la URL HTTPS /exec y el token asignado por GBO.")
            .performScrollTo().assertIsDisplayed()
        assertFalse(graph.settings.configured)
    }

    @Test @Config(qualifiers = "w1280dp-h800dp-land-mdpi")
    fun landscapeLoginKeepsOriginalControlsAndPinFiltering() {
        configured()
        compose.setContent { InventarioVialTheme { LoginScreen(graph) } }
        compose.onNodeWithTag("auth-landscape").assertExists()
        compose.onNode(hasSetTextAction() and hasText("Usuario")).performScrollTo().performTextInput("FICTICIO_VISUAL")
        compose.onNode(hasSetTextAction() and hasText("PIN")).performScrollTo().performTextInput("12x345678901234")
        compose.runOnIdle {
            assertEquals("123456789012", ViewModelProvider(compose.activity)[LoginFormInputs::class.java].pin)
        }
        compose.onNodeWithText("Iniciar sesión").performScrollTo().assertIsEnabled()
        compose.onNodeWithText("Sincronizar usuarios").performScrollTo().assertIsEnabled()
        compose.onNodeWithText("Actualizar token autorizado de esta tablet").performScrollTo().performClick()
        compose.onNodeWithText("Verificar y actualizar token").performScrollTo().assertIsNotEnabled()
    }

    @Test @Config(qualifiers = "w1280dp-h800dp-land-mdpi")
    fun largeTextAndShortViewportKeepSetupActionReachable() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                InventarioVialTheme { Box(Modifier.height(300.dp)) { LoginScreen(graph) } }
            }
        }
        compose.onNodeWithTag("auth-column").assertExists()
        compose.onNode(hasSetTextAction() and hasText("URL HTTPS del servicio de usuarios"))
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Guardar configuración y sincronizar").performScrollTo().assertIsDisplayed()
    }

    @Test fun rotationRetainsOnlyInMemoryInputsAndLeavingAccessClearsThem() {
        compose.activity.setContent { InventarioVialTheme { LoginScreen(graph) } }
        compose.onNode(hasSetTextAction() and hasText("URL HTTPS del servicio de usuarios"))
            .performScrollTo().performTextInput("https://example.invalid/exec")
        compose.onNode(hasSetTextAction() and hasText("Token de esta tablet"))
            .performScrollTo().performTextInput("FICTICIO_NO_CREDENCIAL")
        val before = ViewModelProvider(compose.activity)[LoginFormInputs::class.java]
        compose.activityRule.scenario.recreate()
        compose.activity.setContent { InventarioVialTheme { LoginScreen(graph) } }
        compose.waitForIdle()
        val after = ViewModelProvider(compose.activity)[LoginFormInputs::class.java]
        assertSame(before, after)
        assertEquals("https://example.invalid/exec", after.url)
        assertEquals("FICTICIO_NO_CREDENCIAL", after.token)
        compose.activity.setContent { }
        compose.waitForIdle()
        assertEquals("", after.token)
        assertEquals("", after.url)
        assertFalse(graph.settings.configured)
    }

    @Test fun lockedLoginKeepsUsernameReadOnlyAndTokenRotationDisabled() {
        configured()
        compose.setContent { InventarioVialTheme { LoginScreen(graph, "FICTICIO_BLOQUEADO") } }
        compose.onNode(hasText("Usuario") and hasText("FICTICIO_BLOQUEADO")).performScrollTo().assertExists()
        compose.onNodeWithText("Actualizar token autorizado de esta tablet").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Vuelve a ingresar tu PIN para continuar. La ficha abierta se conserva.")
            .performScrollTo().assertIsDisplayed()
    }
    @Test fun syncingAndSafeDiagnosticRemainVisibleAndPreventDuplicateSyncClicks() {
        configured()
        val busy = AuthGraph::class.java.getDeclaredField("mutableBusy").apply { isAccessible = true }
            .get(graph) as kotlinx.coroutines.flow.MutableStateFlow<Boolean>
        val message = AuthGraph::class.java.getDeclaredField("mutableMessage").apply { isAccessible = true }
            .get(graph) as kotlinx.coroutines.flow.MutableStateFlow<String?>
        busy.value = true
        message.value = UserServiceException("SERVICE_ERROR", UserServiceStage.HTTP, 503).message
        compose.setContent { InventarioVialTheme { LoginScreen(graph) } }
        compose.onNodeWithText("Actualizando…").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText(message.value!!).performScrollTo().assertIsDisplayed()
        compose.runOnIdle { busy.value = false }
        compose.onNodeWithText("Sincronizar usuarios").performScrollTo().assertIsEnabled()
    }
}
