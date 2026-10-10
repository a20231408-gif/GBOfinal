package com.tuempresa.inventariovial

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.field.SignalAssetCatalog
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class) @Config(sdk=[28])
class SignalAssetCatalogTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    @Test fun everyVerifiedGraphicExistsDecodesHasRealCodeAndUniqueHash() {
        val entries=SignalAssetCatalog.entries(context)
        val catalog=listOf("1","2","3").flatMap {TrafficSignCatalog.signsForType(it)}
        assertEquals(catalog.map {it.code}.toSet(),entries.keys)
        val hashes=mutableSetOf<String>()
        for(row in entries.values.filter {it.verified}) {
            assertEquals(catalog.single {it.code==row.code}.name,row.name)
            val bytes=context.assets.open("signs/${row.asset}").use {it.readBytes()}
            val hash=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
            assertTrue("Duplicated graphic ${row.code}",hashes.add(hash))
            val bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.size)
            assertNotNull(row.code,bitmap);assertTrue(bitmap.width in 1..320);assertTrue(bitmap.height in 1..320)
        }
    }
    @Test fun traceabilityUsesPrintedPagesCropsAndSameCodeName() {
        val json=context.assets.open("signs/sign_assets_manifest.json").bufferedReader().use {JSONArray(it.readText())}
        for(i in 0 until json.length()) {
            val row=json.getJSONObject(i)
            if(!row.getBoolean("verified")) continue
            assertEquals("MAN_6",row.getString("source"));assertTrue(row.getInt("sourcePrintedPage")>0)
            assertTrue(row.getJSONObject("crop").getDouble("width")>0);assertTrue(row.getJSONObject("crop").getDouble("height")>0)
            assertEquals(row.getInt("sourcePrintedPage")+1,row.getInt("sourcePdfPage"))
        }
    }
    @Test fun searchCodeNameAndPrefixRetainCorrectSignals() {
        assertEquals("R-1",TrafficSignCatalog.search("1","PARE").single().code)
        assertTrue(TrafficSignCatalog.search("1","R-1").any {it.code=="R-1"})
        assertTrue(TrafficSignCatalog.search("2","P-1").map {it.code}.containsAll(listOf("P-1A","P-1B")))
        assertEquals("r_1.webp",SignalAssetCatalog.verifiedAsset(context,"R-1")!!.asset)
        assertEquals("i_2a.webp",SignalAssetCatalog.verifiedAsset(context,"I-2A")!!.asset)
    }
    @Test fun manualAndUnidentifiedNeverReceiveCatalogImage() {
        assertNull(SignalAssetCatalog.verifiedAsset(context,"R-1","MANUAL"))
        assertNull(SignalAssetCatalog.verifiedAsset(context,"R-1","UNIDENTIFIABLE"))
        assertNull(SignalAssetCatalog.verifiedAsset(context,"UNKNOWN"))
    }
    @Test fun repeatedScrollingDecodesOnlyBoundedThumbnailsAndEvictsCache() {
        repeat(3) {SignalAssetCatalog.entries(context).keys.forEach {code->
            val b=SignalAssetCatalog.bitmap(context,code)
            if(b!=null) assertTrue(b.width<=320 && b.height<=320)
        }}
        assertTrue(SignalAssetCatalog.cachedBytes()<=4*1024*1024)
    }
}
