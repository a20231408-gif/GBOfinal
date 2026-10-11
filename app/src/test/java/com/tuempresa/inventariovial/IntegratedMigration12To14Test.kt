package com.tuempresa.inventariovial

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class IntegratedMigration12To14Test {
    @Test fun everyReleasedTableAndColumnSurvivesAndRoom14Reopens() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val name="integrated-12-14-${System.nanoTime()}"
        val file=context.getDatabasePath(name).also { it.parentFile!!.mkdirs() }
        val entities=JSONObject(File("schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/12.json").readText())
            .getJSONObject("database").getJSONArray("entities")
        val expected=linkedMapOf<String,Map<String,String?>>()
        SQLiteDatabase.openOrCreateDatabase(file,null).use { old ->
            for(i in 0 until entities.length()) {
                val e=entities.getJSONObject(i);val table=e.getString("tableName")
                old.execSQL(e.getString("createSql").replace("\u0024{TABLE_NAME}",table))
                val indices=e.optJSONArray("indices") ?: org.json.JSONArray()
                for(j in 0 until indices.length()) old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\u0024{TABLE_NAME}",table))
                val row=ContentValues();val fields=e.getJSONArray("fields")
                for(j in 0 until fields.length()) {
                    val f=fields.getJSONObject(j);val column=f.getString("columnName")
                    when(f.getString("affinity")) {
                        "INTEGER" -> row.put(column,1L)
                        "REAL" -> row.put(column,1.25)
                        else -> row.put(column,"keep")
                    }
                }
                if(table=="pavement_samples") {
                    row.put("surface","RIGID");row.put("anchoM",5.0);row.put("largoM",40.0)
                    row.put("slabRows",10);row.put("slabColumns",2);row.put("status","CLOSED")
                    row.put("calculationSnapshot","{\"immutable\":true}")
                }
                if(table=="pavement_damages") row.put("severity","A")
                if(table=="user_cache") row.put("role","OPERATOR")
                val names=row.keySet().toList()
                old.execSQL("INSERT INTO `$table` (${names.joinToString { "`$it`" }}) VALUES (${names.joinToString { "?" }})",names.map {row.get(it)}.toTypedArray())
                old.rawQuery("SELECT * FROM `$table`",null).use { c ->
                    assertTrue(c.moveToFirst());expected[table]=c.columnNames.associateWith { col -> c.getString(c.getColumnIndexOrThrow(col)) }
                }
            }
            old.version=12
        }
        fun open()=Room.databaseBuilder(context,InventoryDatabase::class.java,name)
            .addMigrations(InventoryDatabase.MIGRATION_12_13,InventoryDatabase.MIGRATION_13_14,InventoryDatabase.MIGRATION_14_15).addMigrations(*allInventoryMigrations()).allowMainThreadQueries().build()
        try {
            val room=open()
            try {
                val db=room.openHelper.writableDatabase
                assertEquals(17,db.version)
                for((table,columns) in expected) db.query("SELECT * FROM `$table`").use { c ->
                    assertTrue("Lost table $table",c.moveToFirst())
                    for((col,value) in columns) assertEquals("$table.$col",if(table=="pavement_damages" && col=="severity") "H" else value,c.getString(c.getColumnIndexOrThrow(col)))
                    assertFalse("Unexpected duplicate in $table",c.moveToNext())
                }
                db.query("SELECT signalCodeSource,signalDescription,signalObservation FROM sic22_details").use { c ->
                    assertTrue(c.moveToFirst());assertEquals("CATALOG",c.getString(0));assertEquals("",c.getString(1));assertEquals("",c.getString(2))
                }
                db.query("SELECT slabWidthM,slabLengthM,sicDataJson FROM pavement_samples").use { c ->
                    assertTrue(c.moveToFirst());assertEquals(2.5,c.getDouble(0),0.0);assertEquals(4.0,c.getDouble(1),0.0);assertTrue(c.isNull(2))
                }
            } finally { room.close() }
            val reopened=open()
            try { assertEquals(17,reopened.openHelper.readableDatabase.version) } finally { reopened.close() }
        } finally { context.deleteDatabase(name) }
    }
}
