package com.tuempresa.inventariovial

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.field.*
import com.tuempresa.inventariovial.road.ProjectRouteCatalog
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SurveyRouteSelectionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun routeAndSegmentAreOneChangeAndLaterEditsDoNotRestoreThePreviousRoute() {
        val routes = ProjectRouteCatalog.load(ApplicationProvider.getApplicationContext())
        val original = SicCoreDraft("PE-22A", "CD", "0042", "125,123456", "0043", "20", "D",
            routes.requireRoute("PE-22A").segmentCode, "DECREASING")
        var current by mutableStateOf(original)
        val changes = mutableListOf<SicCoreDraft>()
        compose.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    SicFormCore(SicFormMode.EDIT, "SIC-22", "VERTICAL", "20", testAxis(), current) {
                        changes += it
                        current = it
                    }
                }
            }
        }
        compose.onNodeWithText("PE-22A").performScrollTo().performClick()
        compose.onNodeWithText("PE-3NG").performClick()
        compose.runOnIdle {
            assertEquals(listOf(original.copy(route = "PE-3NG", segment = routes.requireRoute("PE-3NG").segmentCode)), changes)
        }
        compose.onNodeWithText("CD").performScrollTo().performClick()
        compose.onNodeWithText("UC").performClick()
        compose.runOnIdle {
            assertEquals(2, changes.size)
            assertEquals(original.copy(route = "PE-3NG", segment = routes.requireRoute("PE-3NG").segmentCode, roadbed = "UC"), current)
        }
    }
}
