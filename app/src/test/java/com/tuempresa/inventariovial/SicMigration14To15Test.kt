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
class SicMigration14To15Test {
    @Test fun version14PreservesEveryTableColumnAndReopensVersion15() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val name="sic-14-15-${System.nanoTime()}"
        val file=context.getDatabasePath(name).also { it.parentFile!!.mkdirs() }
        val entities=JSONObject(File("schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/14.json").readText())
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
            old.version=14
        }
        fun open()=Room.databaseBuilder(context,InventoryDatabase::class.java,name)
            .addMigrations(InventoryDatabase.MIGRATION_14_15).addMigrations(*allInventoryMigrations()).allowMainThreadQueries().build()
        try {
            val room=open()
            try {
                val db=room.openHelper.writableDatabase
                assertEquals(17,db.version)
                for((table,columns) in expected) db.query("SELECT * FROM `$table`").use { c ->
                    assertTrue("Lost table $table",c.moveToFirst())
                    for((col,value) in columns) assertEquals("$table.$col",value,c.getString(c.getColumnIndexOrThrow(col)))
                    assertFalse("Unexpected duplicate in $table",c.moveToNext())
                }
                db.query("PRAGMA foreign_key_list(sic_extension_details)").use {c ->
                    var count=0
                    while(c.moveToNext()) {assertEquals("inventory_records",c.getString(c.getColumnIndexOrThrow("table")));count++}
                    assertEquals(2,count)
                }
                db.query("SELECT * FROM sic_extension_details").use {c -> assertFalse(c.moveToFirst())}
            } finally { room.close() }
            val reopened=open()
            try { assertEquals(17,reopened.openHelper.readableDatabase.version) } finally { reopened.close() }
        } finally { context.deleteDatabase(name) }
    }
}
