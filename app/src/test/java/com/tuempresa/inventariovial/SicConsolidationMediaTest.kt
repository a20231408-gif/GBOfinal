package com.tuempresa.inventariovial

import android.app.Application
import android.content.*
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.export.*
import com.tuempresa.inventariovial.localfiles.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import java.io.File

/** Actual AndroidSharedDocumentStore over a controlled Downloads provider; no physical tablet. */
internal class SicDownloadsProvider:ContentProvider() {
    data class Entry(val id:Long,val values:ContentValues,val file:File)
    val entries=linkedMapOf<Long,Entry>();var mutateFinalName=false
    override fun onCreate()=true
    override fun getType(uri:Uri)=SicExcelWriter.XLSX_MIME
    override fun query(uri:Uri,projection:Array<out String>?,selection:String?,selectionArgs:Array<out String>?,sortOrder:String?):Cursor {
        val cols=projection ?: arrayOf("_id","_display_name","relative_path","is_pending","owner_package_name")
        val id=uri.lastPathSegment?.toLongOrNull()
        val matches=entries.values.filter{(id==null||it.id==id)&&(selectionArgs==null || it.values.getAsString(MediaStore.MediaColumns.RELATIVE_PATH)==selectionArgs[0]&&it.values.getAsString(MediaStore.MediaColumns.DISPLAY_NAME)==selectionArgs[1])}
        return MatrixCursor(cols).apply{matches.forEach{row->addRow(cols.map{if(it=="_id")row.id else row.values.get(it)})}}
    }
    override fun insert(uri:Uri,values:ContentValues?):Uri {
        val id=(entries.keys.maxOrNull() ?: 0)+1
        val v=ContentValues(requireNotNull(values));if(!v.containsKey(MediaStore.MediaColumns.OWNER_PACKAGE_NAME))v.put(MediaStore.MediaColumns.OWNER_PACKAGE_NAME,context!!.packageName)
        val file=File(context!!.filesDir,"FICTICIO_DOWNLOADS/$id").apply{parentFile!!.mkdirs();createNewFile()}
        entries[id]=Entry(id,v,file);return ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI,id)
    }
    override fun update(uri:Uri,values:ContentValues?,selection:String?,selectionArgs:Array<out String>?):Int {
        val row=entries[ContentUris.parseId(uri)] ?: return 0
        row.values.putAll(values)
        if(mutateFinalName && values?.getAsString(MediaStore.MediaColumns.DISPLAY_NAME)?.let{it.startsWith("SIC-")}==true)
            row.values.put(MediaStore.MediaColumns.DISPLAY_NAME,"FICTICIO_NOMBRE_CAMBIADO.xlsx")
        return 1
    }
    override fun delete(uri:Uri,selection:String?,selectionArgs:Array<out String>?):Int=error("No deletion is authorized even in this test provider")
    override fun openFile(uri:Uri,mode:String):ParcelFileDescriptor {
        val entry=entries.getValue(ContentUris.parseId(uri))
        return ParcelFileDescriptor.open(entry.file,if(mode=="r")ParcelFileDescriptor.MODE_READ_ONLY else ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_TRUNCATE)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class)
class SicConsolidationMediaTest {
    private val c get()=ApplicationProvider.getApplicationContext<Context>()
    private lateinit var provider:SicDownloadsProvider
    @Before fun attach(){provider=SicDownloadsProvider().apply{attachInfo(c,ProviderInfo().apply{authority="media"})};ShadowContentResolver.registerProviderInternal("media",provider)}
    @Test fun realMediaStoreAdapterPublishesVerifiedCanonicalPathAndReplacesWithRecoverableBackup()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val r=SicConsolidationFixtures.repo(c,db);val first=SicConsolidationFixtures.bridge(r,"FICTICIO_A")
            val a=LocalSicConsolidates.update(c,db,"PE-28H","SIC-17A")
            val old=provider.entries.getValue(ContentUris.parseId(a.uri));val bytes=old.file.readBytes()
            SicConsolidationFixtures.bridge(r,"FICTICIO_B","11+100")
            val b=LocalSicConsolidates.update(c,db,"PE-28H","SIC-17A")
            val canonical=provider.entries.values.single{it.values.getAsString(MediaStore.MediaColumns.DISPLAY_NAME)=="SIC-17A.xlsx"}
            assertEquals("Download/GBO Inventario Vial/PE-28H/EXCEL_SIC/",canonical.values.getAsString(MediaStore.MediaColumns.RELATIVE_PATH))
            assertEquals(0,canonical.values.getAsInteger(MediaStore.MediaColumns.IS_PENDING).toInt())
            assertEquals(2,SicRouteWorkbook.inspect(canonical.file.readBytes(),"SIC-17A"));assertEquals(2,b.rows)
            assertTrue(old.values.getAsString(MediaStore.MediaColumns.DISPLAY_NAME).contains(".anterior-"));assertArrayEquals(bytes,old.file.readBytes())
            r.setSicField(first,"SIC-17A","bridgeName","EDITADO")
            val edited=LocalSicConsolidates.update(c,db,"PE-28H","SIC-17A")
            val replay=LocalSicConsolidates.update(c,db,"PE-28H","SIC-17A")
            assertEquals(edited.uri,replay.uri);assertEquals(2,replay.rows);assertTrue(db.automaticSyncDao().all().isEmpty())
        }
    }
    @Test fun fileOwnedByAnotherPackageIsNotOverwrittenOrRenamed()=runBlocking {
        val external=provider.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,ContentValues().apply{
            put(MediaStore.MediaColumns.DISPLAY_NAME,"SIC-17A.xlsx");put(MediaStore.MediaColumns.RELATIVE_PATH,"Download/GBO Inventario Vial/PE-28H/EXCEL_SIC/")
            put(MediaStore.MediaColumns.OWNER_PACKAGE_NAME,"ficticio.otro.propietario");put(MediaStore.MediaColumns.IS_PENDING,0)
        })
        val source=provider.entries.getValue(ContentUris.parseId(external)).file.apply{writeText("FICTICIO_OTRO_PROPIETARIO")}
        SicConsolidationFixtures.db(c).use{db->
            SicConsolidationFixtures.bridge(SicConsolidationFixtures.repo(c,db),"FICTICIO")
            val error=runCatching{LocalSicConsolidates.update(c,db,"PE-28H","SIC-17A")}.exceptionOrNull()
            assertNotNull(error);assertTrue(error!!.message!!.contains("propietario"));assertEquals("FICTICIO_OTRO_PROPIETARIO",source.readText())
            assertEquals("SIC-17A.xlsx",provider.entries.getValue(ContentUris.parseId(external)).values.getAsString(MediaStore.MediaColumns.DISPLAY_NAME))
        }
    }
    @Test fun providerRenamingTheFinalFileCannotProduceAFalseSuccess()=runBlocking {
        SicConsolidationFixtures.db(c).use{db->
            val r=SicConsolidationFixtures.repo(c,db);val id=SicConsolidationFixtures.bridge(r,"ANTES")
            val old=LocalSicConsolidates.update(c,db,"PE-28H","SIC-17A")
            r.setSicField(id,"SIC-17A","bridgeName","DESPUES");provider.mutateFinalName=true
            assertTrue(runCatching{LocalSicConsolidates.update(c,db,"PE-28H","SIC-17A")}.isFailure)
            assertTrue(old.file.isFile);assertTrue(provider.entries.values.any{it.values.getAsString(MediaStore.MediaColumns.DISPLAY_NAME).contains(".anterior-")})
        }
    }
}
