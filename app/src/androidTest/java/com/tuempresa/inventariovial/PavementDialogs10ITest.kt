package com.tuempresa.inventariovial

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tuempresa.inventariovial.pavement.PavementDamageEntity
import com.tuempresa.inventariovial.pavement.ui.DamageDialog
import com.tuempresa.inventariovial.ui.theme.InventarioVialTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Run on an isolated Android emulator. No save callbacks, accounts or remote writes. */
@RunWith(AndroidJUnit4::class)
class PavementDialogs10ITest {
    @get:Rule val compose=createComposeRule()
    private fun damage(code:Int)=PavementDamageEntity("test10i","sample10i",1,if(code==15)"ASPHALT" else "RIGID",code,
        if(code==15)"Ahuellamiento" else "Parcheo pequeño","M",if(code==15)"AREA" else "SLABS",.5,1.0,1.5,2.0,null,1.0,1.0,1.0,null,null,"",slabs=if(code==15)"" else "1")
    @Test fun rutFields() {
        compose.setContent {InventarioVialTheme {DamageDialog(damage(15),"",{}, {})}}
        compose.onNodeWithText("Ancho de fisura (mm), si aplica").assertDoesNotExist()
        compose.onNodeWithText("Profundidad media del ahuellamiento (mm)").performScrollTo().assertIsDisplayed()
    }
    @Test fun slabFields() {
        compose.setContent {InventarioVialTheme {DamageDialog(damage(30),"",{}, {})}}
        compose.onNodeWithText("Localizar dentro de las losas indicadas").assertExists()
        compose.onNodeWithText("X1 / centro X (m)").assertExists()
        compose.onNodeWithText("X2 (m)").assertExists()
    }
}
