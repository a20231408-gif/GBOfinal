package com.tuempresa.inventariovial.field

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.TrafficSignCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

data class SignalAsset(val code:String,val name:String,val asset:String?,val verified:Boolean)
object SignalAssetCatalog {
    @Volatile private var manifest:Map<String,SignalAsset>?=null
    private val cache=object:LruCache<String,Bitmap>(4*1024*1024) {
        override fun sizeOf(key:String,value:Bitmap)=value.allocationByteCount
    }
    @Synchronized fun entries(context:Context):Map<String,SignalAsset> {
        manifest?.let {return it}
        val json=context.assets.open("signs/sign_assets_manifest.json").bufferedReader().use {JSONArray(it.readText())}
        return (0 until json.length()).associate { i -> val row=json.getJSONObject(i)
            val code=row.getString("code");code to SignalAsset(code,row.getString("name"),
                if(row.isNull("asset")) null else row.getString("asset"),row.getBoolean("verified"))
        }.also {manifest=it}
    }
    fun verifiedAsset(context:Context,code:String,source:String="CATALOG"):SignalAsset? =
        if(source!="CATALOG") null else entries(context)[code]?.takeIf {it.verified && it.asset!=null && TrafficSignCatalog.containsCode(code)}
    internal fun cachedBytes():Int=cache.size()
    fun clearThumbnails(){cache.evictAll()}
    fun bitmap(context:Context,code:String):Bitmap? {
        cache.get(code)?.let {return it}
        val asset=verifiedAsset(context,code)?.asset ?: return null
        val decoded=runCatching {
            val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
            context.assets.open("signs/$asset").use{BitmapFactory.decodeStream(it,null,bounds)}
            var sample=1;while(maxOf(bounds.outWidth,bounds.outHeight)/sample>256)sample*=2
            context.assets.open("signs/$asset").use{BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply{inSampleSize=sample})}
        }.getOrNull()
        decoded?.let {cache.put(code,it)}
        return decoded
    }
}

@Composable
fun TrafficSignImage(code:String,name:String,source:String="CATALOG",modifier:Modifier=Modifier.size(112.dp)) {
    val context=LocalContext.current
    var bitmap by remember(code,source) {mutableStateOf<Bitmap?>(null)}
    LaunchedEffect(code,source) {
        bitmap=if(source=="CATALOG") withContext(Dispatchers.IO) {SignalAssetCatalog.bitmap(context,code)} else null
    }
    Box(modifier.testTag("signal-image-$code"),contentAlignment=Alignment.Center) {
        if(bitmap!=null) Image(bitmap!!.asImageBitmap(),"Código $code, $name",Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
        else Text(if(source=="UNIDENTIFIABLE") "Sin código identificable" else "Imagen no disponible",style=MaterialTheme.typography.labelSmall)
    }
}

/** Shared by every catalog signal capture/edit surface; rows load only visible thumbnails. */
@Composable
fun TrafficSignVisualSelector(typeCode:String,selectedCode:String,onSelect:(String)->Unit) {
    var searching by rememberSaveable {mutableStateOf(false)}
    var query by rememberSaveable(typeCode) {mutableStateOf("")}
    val selected=TrafficSignCatalog.signsForType(typeCode).find {it.code==selectedCode}
    selected?.let { sign ->
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            TrafficSignImage(sign.code,sign.name)
            Column(Modifier.weight(1f)) {Text(sign.code,style=MaterialTheme.typography.titleMedium);Text(sign.name)}
        }
    }
    OutlinedButton(onClick={searching=true},modifier=Modifier.fillMaxWidth()) {Text("Buscar señal por código o nombre")}
    if(searching) AlertDialog(onDismissRequest={searching=false},title={Text("Código de señal")},text={
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(query,{query=it},label={Text("Buscar señal…")},supportingText={Text("Código, nombre o descripción")},singleLine=true,modifier=Modifier.fillMaxWidth())
            val results=TrafficSignCatalog.search(typeCode,query)
            if(results.isEmpty()) Text("Sin coincidencias. Puedes usar Código no listado.")
            LazyColumn(Modifier.fillMaxWidth().heightIn(max=420.dp)) {
                items(results,key={it.code}) {sign->
                    TextButton(onClick={onSelect(sign.code);searching=false},modifier=Modifier.fillMaxWidth().testTag("signal-option-${sign.code}")) {
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                            TrafficSignImage(sign.code,sign.name,modifier=Modifier.size(64.dp))
                            Column(Modifier.weight(1f)) {Text(sign.code,style=MaterialTheme.typography.titleSmall);Text(sign.name,style=MaterialTheme.typography.bodySmall)}
                        }
                    }
                }
            }
        }
    },confirmButton={TextButton(onClick={searching=false}){Text("Cerrar")}})
}
