package com.tuempresa.inventariovial

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.field.*
import com.tuempresa.inventariovial.localfiles.*
import com.tuempresa.inventariovial.pavement.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
class TabletObservations37Test {
    private fun item(n:Int,capture:String)=LocalInventoryItem("SIC","FICTITIOUS_$n","SIC","Ficha $n","PE-12A","42+995",null,"CD","INCREASING",capture,1,"2026-10-29")
    @Test fun discardedRecordsNeverBecomeActivePendingEvenWhenHistoryIsVisible() {
        // Thirty active plus four discarded reproduces 30/34 Todos, formerly 7/11 Pendientes.
        val rows=(0 until 34).map{item(it,if(it>=30)"DISCARDED_LOCAL"else"ACTIVE")}
        fun shown(filter:String,history:Boolean)=rows.filter{LocalInventoryIndex.matchesCapture(it,filter,history,it.id.substringAfterLast('_').toInt() in 7..29)}
        assertEquals(30,shown("Todos",false).size);assertEquals(34,shown("Todos",true).size)
        assertEquals(7,shown("Pendientes",false).size);assertEquals(7,shown("Pendientes",true).size)
        assertEquals(4,shown("Descartados",false).size)
        assertEquals(23,shown("Enviados",true).size)
        assertTrue(shown("Descartados",false).none{it.capture=="DRAFT"})
        val thirty=rows.take(26)+rows.takeLast(4)
        assertEquals(26,thirty.count{LocalInventoryIndex.matchesCapture(it,"Todos",false,false)})
        assertEquals(4,thirty.count{LocalInventoryIndex.matchesCapture(it,"Descartados",false,false)})
        assertEquals(4,shown("Descartados",false).count{RecordDateSelection(year=2026,month=10,day=29).matches(parseRecordDate(it.surveyDate))})
        assertFalse(LocalInventoryIndex.matchesCapture(item(100,"ANNULLED"),"Pendientes",true,false))
    }
    @Test fun discardedRowsAndIndividualRestorationPersistAcrossDatabaseReopenWithoutQueueOrAckChanges()=runBlocking {
        val c=ApplicationProvider.getApplicationContext<Application>()
        android.provider.Settings.Secure.putString(c.contentResolver,android.provider.Settings.Secure.ANDROID_ID,"FICTITIOUS_DEVICE_37")
        val name="fictitious-discard-37.db"
        fun open()=Room.databaseBuilder(c,InventoryDatabase::class.java,name).build()
        c.deleteDatabase(name)
        try {
            open().use{db->
                (0 until 34).forEach{db.inventoryDao().insertRecord(testRecord("FICTITIOUS_P09_$it",status=if(it>=30)"DRAFT"else"ACTIVE"))}
                val confirmed=deliveryFixture()
                db.automaticSyncDao().put(confirmed)
                LocalDraftDiscard(db,c).changeMany((30 until 34).map{LocalDraftDiscard.Candidate("SIC","FICTITIOUS_P09_$it",1)},"FICTITIOUS_OPERATOR")
                assertEquals(confirmed,db.automaticSyncDao().get(confirmed.key))
            }
            open().use{db->
                (30 until 34).forEach{assertEquals("DISCARDED_LOCAL",db.inventoryDao().recordById("FICTITIOUS_P09_$it")!!.status)}
                val before=db.automaticSyncDao().all()
                LocalDraftDiscard(db,c).change("SIC","FICTITIOUS_P09_30",restore=true,userId="FICTITIOUS_OPERATOR")
                assertEquals(before,db.automaticSyncDao().all())
            }
            open().use{db->
                assertEquals("DRAFT",db.inventoryDao().recordById("FICTITIOUS_P09_30")!!.status)
                assertEquals("DISCARDED_LOCAL",db.inventoryDao().recordById("FICTITIOUS_P09_31")!!.status)
                assertEquals(1,db.automaticSyncDao().all().size)
            }
        }finally{c.deleteDatabase(name)}
    }
    @Test fun datesAreDisplayOnlyAndSpanishMonthsAreExplicit() {
        assertEquals("29/10/2026",recordDateLabel("2026-10-29"))
        assertEquals("29/10/2026",recordDateLabel("29/10/2026"))
        assertEquals("31/02/2026",recordDateLabel("31/02/2026"))
        assertEquals("octubre",recordMonthLabel(10))
        assertEquals("29/10/2026",PavementHistory.dateLabel("2026-10-29"))
    }
    @Test fun globalJointQuantityIsNotPresentedAsZeroAndDoesNotAlterPciInput() {
        val joint=PavementDamageEntity("FICTITIOUS_JOINT","FICTITIOUS_SAMPLE",1,"RIGID",26,"Sello de junta","H","GLOBAL",null,null,null,null,null,null,null,null,null,null,null,createdAt=1)
        assertEquals(0.0,PavementRules.quantity(joint),0.0)
        assertEquals("No aplica (global)",PavementDisplay.quantity(joint))
        assertEquals("H",joint.severity);assertEquals("GLOBAL",joint.geometry)
    }
}
