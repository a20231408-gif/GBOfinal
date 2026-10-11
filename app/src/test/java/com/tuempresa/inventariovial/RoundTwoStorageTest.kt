package com.tuempresa.inventariovial

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.location.GeoLocation
import com.tuempresa.inventariovial.road.*
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RoundTwoStorageTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    @Test fun migration10to11PreservesEveryColumnOfEveryLegacyTableIncludingSecurityAndScap() = runBlocking {
        val name = "round2-v10.db"; context.deleteDatabase(name)
        val entities = JSONObject(File("schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/10.json").readText())
            .getJSONObject("database").getJSONArray("entities")
        val before = mutableMapOf<String, Pair<String, List<String?>>>()
        val file = context.getDatabasePath(name).apply { parentFile!!.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i); val table = entity.getString("tableName")
                old.execSQL(entity.getString("createSql").replace("\u0024{TABLE_NAME}", table))
                val indexes = entity.optJSONArray("indices") ?: JSONArray()
                for (j in 0 until indexes.length()) old.execSQL(indexes.getJSONObject(j).getString("createSql").replace("\u0024{TABLE_NAME}", table))
                val fields = entity.getJSONArray("fields")
                val columns = (0 until fields.length()).map { fields.getJSONObject(it).getString("columnName") }
                val values = (0 until fields.length()).map { when (fields.getJSONObject(it).getString("affinity")) {
                    "INTEGER" -> "1"; "REAL" -> "1.5"; "BLOB" -> "X'00'"; else -> "'legacy'"
                } }
                old.execSQL("INSERT INTO `$table` (${columns.joinToString { "`$it`" }}) VALUES (${values.joinToString()})")
                val query = "SELECT ${columns.joinToString { "quote(`$it`)" }} FROM `$table`"
                old.rawQuery(query, null).use { cursor -> cursor.moveToFirst(); before[table] = query to columns.indices.map { cursor.getString(it) } }
            }
            old.version = 10
        }
        val migrated = Room.databaseBuilder(context, InventoryDatabase::class.java, name).addMigrations(InventoryDatabase.MIGRATION_10_11).build()
        try {
            assertEquals(11, migrated.openHelper.readableDatabase.version)
            before.forEach { (table, expected) ->
                migrated.openHelper.readableDatabase.query(expected.first).use { cursor ->
                    assertTrue(table, cursor.moveToFirst())
                    assertEquals(table, expected.second, (0 until cursor.columnCount).map { cursor.getString(it) })
                    assertFalse(cursor.moveToNext())
                }
            }
            val photo = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { migrated.inventoryDao().photoById("legacy")!! }
            assertNull(photo.photoLatitude); assertNull(photo.photoLongitude); assertNull(photo.capturedAt)
            assertEquals("UNAVAILABLE_LEGACY", photo.photoLocationSource)
            val record = migrated.inventoryDao().recordById("legacy")!!
            assertEquals("MANUAL", record.routeSource); assertNull(record.suggestedPrCode)
            assertTrue(migrated.inventoryDao().fieldPrs().isEmpty())
        } finally { migrated.close(); context.deleteDatabase(name) }
    }
    @Test fun fieldPrIsProjectedAndPersistsWithoutChangingAnyExistingContractualRecord() = runBlocking {
        val name = "round2-field-pr.db"; context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, InventoryDatabase::class.java, name).build()
        try {
            val original = testRecord().copy(startPrCode = "0042", startDistanceM = 684.0)
            db.inventoryDao().insertRecord(original)
            val empty = prSuggestionReference().copy(prs = emptyList())
            val fix = GeoLocation(0.0, 0.0, .8f, 300.5)
            val field = FieldPrRepository(db.inventoryDao()).register(empty, MatchConfig(), "R", "CD", "0010", "Marca/Poste", "Hito observado", fix)
            assertEquals(10000.0, field.prAxisMeasureM, .001)
            assertEquals(fix.latitude, field.latitude, 0.0); assertEquals(fix.longitude, field.longitude, 0.0)
            assertEquals("FIELD_CAPTURED", field.source)
            assertEquals(original, db.inventoryDao().recordById(original.id))
            db.close(); db = Room.databaseBuilder(context, InventoryDatabase::class.java, name).build()
            val merged = FieldPrRepository(db.inventoryDao()).merge(empty)
            val suggestion = LocationSuggestionEngine(merged).suggest("R", "CD", 0.0, .0053, 1.0)
            assertEquals("0010", suggestion.suggestedPrCode); assertEquals(2650.0, suggestion.suggestedDistanceFromPrM!!, .001)
            assertEquals(original, db.inventoryDao().recordById(original.id))
            assertTrue(runCatching { FieldPrRepository(db.inventoryDao()).register(merged, MatchConfig(), "R", "CD", "0010", "Virtual", "", fix) }.isFailure)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
