package com.tuempresa.inventariovial
import android.app.Application
import android.content.Context
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34],application=Application::class)
class PavementMigration21Test {
 @Test fun migration20To21PreservesEveryOriginalColumnAndAllPendingStates()=runBlocking {
  val c=ApplicationProvider.getApplicationContext<Context>();val name="p21.db";val file=c.getDatabasePath(name).apply{parentFile!!.mkdirs()}
  val entities=JSONObject(File("schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/20.json").readText()).getJSONObject("database").getJSONArray("entities")
  val expected=linkedMapOf<String,Map<String,String?>>()
  SQLiteDatabase.openOrCreateDatabase(file,null).use{old->
   for(n in 0 until entities.length()){
    val e=entities.getJSONObject(n);val table=e.getString("tableName");old.execSQL(e.getString("createSql").replace("\${TABLE_NAME}",table));val indexes=e.optJSONArray("indices")?:org.json.JSONArray()
    for(i in 0 until indexes.length())old.execSQL(indexes.getJSONObject(i).getString("createSql").replace("\${TABLE_NAME}",table))
    val fields=e.getJSONArray("fields");val row=ContentValues()
    for(i in 0 until fields.length()){val f=fields.getJSONObject(i);val col=f.getString("columnName");when(f.getString("affinity")){"INTEGER"->row.put(col,123L);"REAL"->row.put(col,1.25);else->row.put(col,"TEST_PRESERVED_P21")}}
    val cols=row.keySet().toList();old.execSQL("INSERT INTO `$table` (${cols.joinToString{"`$it`"}}) VALUES (${cols.joinToString{"?"}})",cols.map{row.get(it)}.toTypedArray())
    old.rawQuery("SELECT * FROM `$table`",null).use{q->assertTrue(q.moveToFirst());expected[table]=q.columnNames.associateWith{q.getString(q.getColumnIndexOrThrow(it))}}
   };old.version=20
  }
  fun open()=Room.databaseBuilder(c,InventoryDatabase::class.java,name).addMigrations(InventoryDatabase.MIGRATION_20_21).build()
  repeat(2){open().use{d->val sql=d.openHelper.writableDatabase;assertEquals(21,sql.version)
   for((table,cols)in expected)sql.query("SELECT * FROM `$table`").use{q->assertTrue(q.moveToFirst());for((col,v)in cols)assertEquals("$table.$col",v,q.getString(q.getColumnIndexOrThrow(col)))}
   sql.query("SELECT retiredAt FROM pavement_damages").use{q->assertTrue(q.moveToFirst());assertTrue(q.isNull(0))}
  }}
 }
}
