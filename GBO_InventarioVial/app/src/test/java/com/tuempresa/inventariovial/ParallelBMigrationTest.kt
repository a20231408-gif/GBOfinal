package com.tuempresa.inventariovial

import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class ParallelBMigrationTest {
    @Test fun migrationPreservesLegacySignAndDefaultsItsProvenance() {
        val config=SupportSQLiteOpenHelper.Configuration.builder(ApplicationProvider.getApplicationContext())
            .callback(object:SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db:androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE sic22_details (recordId TEXT NOT NULL PRIMARY KEY, signalCode TEXT)")
                    db.execSQL("INSERT INTO sic22_details(recordId,signalCode) VALUES ('old','R-1')")
                }
                override fun onUpgrade(db:androidx.sqlite.db.SupportSQLiteDatabase,oldVersion:Int,newVersion:Int) = Unit
            }).build()
        FrameworkSQLiteOpenHelperFactory().create(config).use {helper->
            val db=helper.writableDatabase
            InventoryDatabase.MIGRATION_12_13.migrate(db)
            db.query("SELECT signalCode,signalCodeSource,signalDescription,signalObservation FROM sic22_details WHERE recordId='old'").use {c->
                assertTrue(c.moveToFirst());assertEquals("R-1",c.getString(0));assertEquals("CATALOG",c.getString(1))
                assertEquals("",c.getString(2));assertEquals("",c.getString(3))
            }
            db.execSQL("UPDATE sic22_details SET signalCode=NULL,signalCodeSource='UNIDENTIFIABLE',signalObservation='Placa rota' WHERE recordId='old'")
            db.query("SELECT signalCode,signalObservation FROM sic22_details WHERE recordId='old'").use {c->
                assertTrue(c.moveToFirst());assertTrue(c.isNull(0));assertEquals("Placa rota",c.getString(1))
            }
        }
    }
}
