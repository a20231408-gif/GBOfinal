package com.tuempresa.inventariovial

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.Sic22Entity
import com.tuempresa.inventariovial.model.form.*
import com.tuempresa.inventariovial.repository.EngineeringEdits
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class ParallelBSignalStorageTest {
    @Test fun manualAndUnidentifiedMetadataRoundTripThroughRoomAndEditor() = runBlocking {
        val db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(),InventoryDatabase::class.java).build()
        try {
            val dao=db.inventoryDao()
            for(source in listOf(SignalCodeSource.MANUAL,SignalCodeSource.UNIDENTIFIABLE)) {
                val record=testRecord(id=source).copy(sicCode="SIC-22",endPrCode="legacy",endDistanceM=5.0)
                dao.insertRecord(record)
                dao.insertSic22(Sic22Entity(record.id,"20","1","2",if(source==SignalCodeSource.MANUAL) "X-LOCAL" else null,
                    null,"3",null,null,null,source,"Señal no catalogada","Placa rota"))
                val form=(EngineeringEdits.form(requireNotNull(dao.snapshot(record.id))) as SicFormDetail.Sic22).state
                assertEquals(source,form.signalCodeSource)
                assertEquals("Señal no catalogada",form.signalDescription)
                assertEquals("Placa rota",form.signalObservation)
                EngineeringEdits.save(db,record.id,SicFormDetail.Sic22(form.copy(signalObservation="Observación actualizada")),emptyList(),emptyMap())
                val restored=requireNotNull(dao.snapshot(record.id))
                assertEquals(source,restored.sic22?.signalCodeSource)
                assertEquals("Observación actualizada",restored.sic22?.signalObservation)
                assertEquals("legacy",restored.record.endPrCode)
                if(source==SignalCodeSource.UNIDENTIFIABLE) assertNull(restored.sic22?.signalCode)
                else assertEquals("X-LOCAL",restored.sic22?.signalCode)
            }
        } finally {db.close()}
    }
}
