package com.tuempresa.inventariovial

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.localfiles.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
class LocalFiles148MigrationTest {
    @Test fun migrates147AndReopens()=runBlocking{
        val c=ApplicationProvider.getApplicationContext<Context>();val name="m148.db";val file=c.getDatabasePath(name).apply{parentFile!!.mkdirs()}
        val entities=JSONObject(File("schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/19.json").readText()).getJSONObject("database").getJSONArray("entities")
        val expected=linkedMapOf<String,Map<String,String?>>()
        SQLiteDatabase.openOrCreateDatabase(file,null).use{old->
            for(n in 0 until entities.length()){
                val e=entities.getJSONObject(n);val table=e.getString("tableName");old.execSQL(e.getString("createSql").replace("\u0024{TABLE_NAME}",table));val indexes=e.optJSONArray("indices") ?: org.json.JSONArray()
                for(i in 0 until indexes.length())old.execSQL(indexes.getJSONObject(i).getString("createSql").replace("\u0024{TABLE_NAME}",table))
                val fields=e.getJSONArray("fields");val row=ContentValues()
                for(i in 0 until fields.length()){val f=fields.getJSONObject(i);val column=f.getString("columnName");when(f.getString("affinity")){"INTEGER"->row.put(column,123L);"REAL"->row.put(column,1.25);else->row.put(column,"TEST_PRESERVED_147")}}
                val cols=row.keySet().toList();old.execSQL("INSERT INTO `$table` (${cols.joinToString{"`$it`"}}) VALUES (${cols.joinToString{"?"}})",cols.map{row.get(it)}.toTypedArray())
                old.rawQuery("SELECT * FROM `$table`",null).use{cursor->assertTrue(cursor.moveToFirst());expected[table]=cursor.columnNames.associateWith{cursor.getString(cursor.getColumnIndexOrThrow(it))}}
            };old.version=19
        }
        fun open()=Room.databaseBuilder(c,InventoryDatabase::class.java,name).addMigrations(InventoryDatabase.MIGRATION_19_20).build()
        open().use{d->val db=d.openHelper.writableDatabase;assertEquals(20,db.version)
            for((table,columns)in expected)db.query("SELECT * FROM `$table`").use{cursor->assertTrue(cursor.moveToFirst());for((col,value)in columns)assertEquals("$table.$col",value,cursor.getString(cursor.getColumnIndexOrThrow(col)))}
            assertTrue(d.localFilesDao().jobs().isEmpty());assertNull(d.localFilesDao().choice("SIC/TEST"));d.localFilesDao().saveJob(LocalExportJob("TEST_PERSISTENT_JOB","SIC/TEST","SIC","TEST",1,state="RUNNING"))
            d.localFilesDao().saveFile(LocalSharedFile("TEST_URI","TEST_PERSISTENT_JOB","SIC/TEST","Download/GBO_Ruta_Clara/","test.pdf","application/pdf",1,"a".repeat(64),"TEST_PRIVATE_SOURCE","content://media/external/downloads/42","WRITING"))
        }
        open().use{d->assertEquals("RUNNING",d.localFilesDao().job("TEST_PERSISTENT_JOB")!!.state);assertEquals("content://media/external/downloads/42",d.localFilesDao().files("TEST_PERSISTENT_JOB").single().uri)}
        // The fixture is isolated. No existing app database or tablet is opened.
    }
}
