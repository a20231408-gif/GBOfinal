package com.tuempresa.inventariovial

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.auth.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.pavement.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class)
class PavementJointFlow20261019Test {
    @Test fun oneConfirmationValidatesAndPersistsGlobalAndMarksAtomicallyWithSameUuid()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val db=Room.inMemoryDatabaseBuilder(context,InventoryDatabase::class.java).build()
        val sessions=AuthSessionManager();sessions.login(UserCacheEntity("test","test","FICTICIO","OPERATOR",true,"",0,0,0))
        try {
            val s=visualSample();db.pavementDao().saveSample(s)
            val repo=PavementRepository(db,sessions)
            val d=visualDamage(26,1,0.0,0.0,0.0,0.0,1).copy(geometry="GLOBAL",x1=null,y1=null,x2=null,y2=null,slabs="")
            val bad=setOf(PavementJointAnnotations.Mark("V",0,999))
            assertTrue(runCatching {repo.saveDamageWithJointMarks(d,bad)}.isFailure)
            assertTrue(db.pavementDao().damages(s.id).isEmpty())
            val marks=setOf(PavementJointAnnotations.Mark("V",0,1))
            repo.saveDamageWithJointMarks(d,marks)
            val saved=db.pavementDao().damages(s.id).single();assertEquals(d.id,saved.id);assertEquals(1,saved.correlativo)
            assertEquals(marks,PavementJointAnnotations.marks(db.pavementDao().sample(s.id)!!,d.id))
            repo.saveDamageWithJointMarks(saved.copy(severity="H"),marks)
            assertEquals(1,db.pavementDao().damages(s.id).size);assertEquals("H",db.pavementDao().damage(d.id)!!.severity)
            assertEquals(0.0,PavementRules.quantity(saved),0.0)
        }finally {db.close()}
    }
}
