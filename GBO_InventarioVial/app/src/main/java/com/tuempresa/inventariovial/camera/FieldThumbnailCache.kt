package com.tuempresa.inventariovial.camera

import android.graphics.Bitmap
import android.util.LruCache

/** Only small disposable previews; never owns capture files or persistent records. */
object FieldThumbnailCache {
    private val cache=object:LruCache<String,Bitmap>(4*1024*1024){override fun sizeOf(key:String,value:Bitmap)=value.byteCount}
    @Synchronized fun get(path:String,maxSize:Int=480):Bitmap? {
        val file=java.io.File(path);val key="$path:$maxSize:${file.lastModified()}"
        cache.get(key)?.let{return it}
        return loadCorrectlyOrientedBitmap(path,maxSize)?.also{if(it.byteCount<=1024*1024)cache.put(key,it)}
    }
    fun clear(){cache.evictAll()} // No recycle: a currently rendered Image may still reference the bitmap.
}
