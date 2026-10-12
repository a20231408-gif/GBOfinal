package com.tuempresa.inventariovial.core

import com.tuempresa.inventariovial.delivery.sha256
import android.content.Context
import com.tuempresa.inventariovial.data.entity.PhotoEntity

object StablePhotoNames {
    private val lock=Any()
    /** Prefix is assigned once; UUID hash prevents cross-tablet collisions. Historical ACKs stay legacy. */
    fun name(context:Context,photo:PhotoEntity,enabled:Boolean):String=synchronized(lock) {
        val legacy="F_${sha256(photo.id.toByteArray()).take(16)}.jpg"
        val prefs=context.getSharedPreferences("photo_names_9",Context.MODE_PRIVATE)
        prefs.getString(photo.id,null)?.let {return@synchronized it}
        if(!enabled || photo.driveFileId!=null)return@synchronized legacy
        val scope="next/${photo.recordId}/${photo.scapInspectionId.orEmpty()}"
        val next=maxOf(prefs.getInt(scope,0)+1,photo.photoIndex)
        val name="${next.toString().padStart(3,'0')}_$legacy"
        check(prefs.edit().putString(photo.id,name).putInt(scope,next).commit())
        name
    }
}
