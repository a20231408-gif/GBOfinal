package com.tuempresa.inventariovial

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.tuempresa.inventariovial.core.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SyncActions38UiTest {
    @get:Rule val ui=createComposeRule()
    @Test fun schedulingDisablesDuplicateActionsAndShowsIndeterminateFeedback() {
        var requests=0
        ui.setContent{MaterialTheme{Column{DriveSyncActions(true,null,null,{requests++},{requests++})}}}
        ui.onNodeWithText("Sincronizar ahora").assertIsNotEnabled()
        ui.onNodeWithText("Reintentar errores").assertIsNotEnabled()
        ui.onNodeWithText("Programando revisión de la cola…").assertIsDisplayed()
        assertEquals(0,requests)
    }
    @Test fun zeroNewRequestsAndExistingQueueAreVisibleBesideActionsWithoutClaimingDelivery() {
        var sync=0;var retry=0
        val message="0 solicitudes nuevas. También se revisará la cola existente; la entrega requiere ACK."
        val status=SyncProgressPresentation.workStatus(emptyList(),false)
        ui.setContent{MaterialTheme{Column{DriveSyncActions(false,message,status,{sync++},{retry++})}}}
        ui.onNodeWithText(message).assertIsDisplayed()
        ui.onNodeWithText(status).assertIsDisplayed()
        ui.onNodeWithText("Sincronizar ahora").performClick()
        ui.onNodeWithText("Reintentar errores").performClick()
        assertEquals(1,sync);assertEquals(1,retry)
        ui.onNodeWithText("Entregado con ACK",substring=true).assertDoesNotExist()
    }
    @Test fun persistedProgressIsExplicitlyHistoricalWhenNoWorkerIsActive() {
        val i=deliveryFixture().copy(state="ERROR",syncedRevision=0,receiptJson=null)
        val identity=DriveDeliveryIdentity("Puente ficticio","Ruta ficticia",2,true)
        val p=SyncProgress(2,"CONFIRMING",3,3,1)
        val view=DriveDeliveryPresentation.view(i,identity,false,true,true,progress=p)
        ui.setContent{MaterialTheme{DriveDeliveryCard(i,identity,view,p){}}}
        ui.onNodeWithText("3 / 3",substring=true).assertDoesNotExist()
        ui.onNodeWithText("+ Detalles técnicos").performClick()
        ui.onNodeWithText("Último progreso guardado;",substring=true).assertExists()
        assertFalse(view.confirmed)
    }
}
