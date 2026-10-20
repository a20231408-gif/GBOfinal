package com.tuempresa.inventariovial

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import com.tuempresa.inventariovial.data.database.InventoryDatabase

/** UI fixtures inspect Room/outbox; transport suites explicitly execute their fake transports. */
internal fun initializeIdleUiWorkers() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    InventoryDatabase.getInstance(context).syncContext = null
    try {
        WorkManager.getInstance(context)
    } catch (_: IllegalStateException) {
        // Robolectric does not run AndroidX Startup's production initializer.
        WorkManager.initialize(context, Configuration.Builder().setExecutor {}.setTaskExecutor {}.build())
    }
}
