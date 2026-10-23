package com.tuempresa.inventariovial

import android.app.Application
import android.content.*
import android.database.MatrixCursor
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.localfiles.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import java.io.File

/** Fake provider exercises actual resolver calls. Does not certify visibility in Android's Files app. */
@RunWith(RobolectricTestRunner::class) @Config(sdk=[29,34],application=Application::class)
class LocalFiles148MediaStoreTest {
    private val c get()=ApplicationProvider.getApplicationContext<Context>()
    private class Provider(private val c:Context):ContentProvider(){
        val rows=linkedMapOf<Long,ContentValues>();val data=linkedMapOf<Long,File>();var includedPending=false
        override fun onCreate()=true
        override fun getType(uri:Uri)=rows[uri.lastPathSegment?.toLongOrNull()]?.getAsString(MediaStore.MediaColumns.MIME_TYPE)
        override fun insert(uri:Uri,values:ContentValues?):Uri{val id=rows.size+1L;rows[id]=ContentValues(values).apply{put("_id",id);put(MediaStore.MediaColumns.OWNER_PACKAGE_NAME,c.packageName)};data[id]=File(c.filesDir,"media-test-$id");return ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI,id)}
        override fun query(uri:Uri,projection:Array<out String>?,queryArgs:Bundle?,cancellationSignal:CancellationSignal?):android.database.Cursor{
            if(queryArgs?.getInt(MediaStore.QUERY_ARG_MATCH_PENDING)==MediaStore.MATCH_INCLUDE)includedPending=true
            return query(uri,projection,queryArgs?.getString(ContentResolver.QUERY_ARG_SQL_SELECTION),queryArgs?.getStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS),null)
        }
        override fun query(uri:Uri,projection:Array<out String>?,selection:String?,selectionArgs:Array<out String>?,sortOrder:String?):android.database.Cursor{
            if(uri.toString().contains("includePending"))includedPending=true
            val cols=projection?:arrayOf("_id");val cursor=MatrixCursor(cols);val id=uri.lastPathSegment?.toLongOrNull()
            for((n,values)in rows){if(id!=null&&n!=id)continue
                if(selectionArgs!=null&&(values.getAsString(MediaStore.MediaColumns.RELATIVE_PATH)!=selectionArgs[0]||values.getAsString(MediaStore.MediaColumns.DISPLAY_NAME)!=selectionArgs[1]))continue
                cursor.addRow(cols.map{values.get(it)}.toTypedArray())
            };return cursor
        }
        override fun update(uri:Uri,values:ContentValues?,selection:String?,selectionArgs:Array<out String>?):Int{val row=rows[uri.lastPathSegment!!.toLong()]?:return 0;row.putAll(values);return 1}
        override fun delete(uri:Uri,selection:String?,selectionArgs:Array<out String>?)=throw AssertionError("No automatic deletion allowed")
        override fun openFile(uri:Uri,mode:String):ParcelFileDescriptor{val f=data[uri.lastPathSegment!!.toLong()]!!;return ParcelFileDescriptor.open(f,if(mode.contains('w'))ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_TRUNCATE else ParcelFileDescriptor.MODE_READ_ONLY)}
    }
    @Test fun interruptedInsertIsFoundIncludingPendingAndPublishedOnlyAfterHashCheck(){
        val provider=Provider(c);provider.attachInfo(c,android.content.pm.ProviderInfo().apply{authority="media";exported=false});ShadowContentResolver.registerProviderInternal("media",provider)
        val f=File(c.filesDir,"synthetic.pdf").apply{writeText("%PDF-SYNTHETIC-LOCAL-148")};val h=fileDigest(f)
        val entry=LocalSharedFile("TEST","JOB","SIC/TEST","Download/GBO_Ruta_Clara/SIC/PE-22A/TEST/R_1/",f.name,"application/pdf",h.first,h.second,f.path)
        var storage=AndroidSharedDocumentStore(c);val first=storage.reserve(entry);assertFalse(first.published);assertEquals(1,provider.rows.size)
        storage=AndroidSharedDocumentStore(c);val recovered=storage.reserve(entry);assertEquals(first.uri,recovered.uri);assertFalse(recovered.published);assertTrue(provider.includedPending)
        storage.write(recovered.uri,f);assertEquals(h,storage.digest(recovered.uri));val uri=storage.publish(recovered,entry);assertEquals(0,provider.rows.values.single().getAsInteger(MediaStore.MediaColumns.IS_PENDING).toInt())
        assertTrue(storage.reserve(entry).published);assertEquals(uri,storage.reserve(entry).uri);assertEquals(1,provider.rows.size)
    }
    @Test fun knownPendingUriDoesNotAllocateAgain(){
        val provider=Provider(c);provider.attachInfo(c,android.content.pm.ProviderInfo().apply{authority="media";exported=false});ShadowContentResolver.registerProviderInternal("media",provider)
        val entry=LocalSharedFile("TEST","JOB","SIC/TEST","Download/GBO_Ruta_Clara/","test.pdf","application/pdf",1,"a".repeat(64),"TEST_SOURCE")
        val store=AndroidSharedDocumentStore(c);val slot=store.reserve(entry);store.checkSlot(slot,entry)
        provider.rows.values.single().put(MediaStore.MediaColumns.DISPLAY_NAME,"unexpected-name.pdf")
        assertTrue(runCatching{store.checkSlot(slot,entry)}.isFailure);assertEquals(slot.uri,store.reserve(entry,slot.uri).uri);assertEquals(1,provider.rows.size)
    }
    @Test fun readableUnicodePathsRetainOwnershipAndVerification(){
        val provider=Provider(c);provider.attachInfo(c,android.content.pm.ProviderInfo().apply{authority="media";exported=false});ShadowContentResolver.registerProviderInternal("media",provider)
        val f=File(c.filesDir,"ficticio.txt").apply{writeText("FICTICIO")};val h=fileDigest(f)
        val entry=LocalSharedFile("TEST_NAME","JOB","SCAP/FICTICIO","Download/GBO_Ruta_Clara/PE-28H/PUENTES/Puente 001 - Ñandú/REVISIONES/R_1/",f.name,"text/plain",h.first,h.second,f.path)
        val storage=AndroidSharedDocumentStore(c);val slot=storage.reserve(entry);storage.checkSlot(slot,entry)
        storage.write(slot.uri,f);assertEquals(h,storage.digest(slot.uri));storage.publish(slot,entry)
        assertEquals(entry.relativePath,provider.rows.values.single().getAsString(MediaStore.MediaColumns.RELATIVE_PATH))
        provider.rows.values.single().put(MediaStore.MediaColumns.OWNER_PACKAGE_NAME,"otro.propietario")
        assertTrue(runCatching{storage.reserve(entry)}.isFailure);assertTrue(runCatching{storage.reserve(entry,slot.uri)}.isFailure)
        assertEquals(1,provider.rows.size)
    }
    @Test @Config(sdk=[28]) fun legacyRequiresLocalFolder(){
        c.getSharedPreferences("gbo_local_copies",0).edit().clear().commit()
        val entry=LocalSharedFile("TEST","JOB","SIC/TEST","Download/GBO_Ruta_Clara/","test.pdf","application/pdf",1,"a".repeat(64),"TEST_SOURCE")
        val result=runCatching{AndroidSharedDocumentStore(c).reserve(entry)};assertTrue(result.isFailure);assertTrue(result.exceptionOrNull()!!.message!!.contains("Android 8/9"))
        c.getSharedPreferences("gbo_local_copies",0).edit().putString("legacyTree","content://cloud.example/tree/test").commit()
        assertTrue(runCatching{AndroidSharedDocumentStore(c).reserve(entry)}.exceptionOrNull()!!.message!!.contains("almacenamiento de la tablet"))
    }
}
