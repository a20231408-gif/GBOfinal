package com.tuempresa.inventariovial

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf
import android.os.Looper

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class)
class InventoryLifecycle10Test {
    @Test fun directStatusCallIsRejectedBeforeDatabaseMutation() = runBlocking {
        val app=ApplicationProvider.getApplicationContext<Application>()
        val db=InventoryDatabase.getInstance(app)
        val vm=InventoryViewModel(app)
        var message:String?=null
        vm.setRecordStatus("nonexistent-identity",false) {message=it}
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(com.tuempresa.inventariovial.core.InventoryLifecyclePolicy.unavailableReason,message)
        assertNull(db.inventoryDao().recordById("nonexistent-identity"))
        assertFalse(com.tuempresa.inventariovial.core.InventoryLifecyclePolicy.enabled)
    }
}
