package com.tuempresa.inventariovial.core

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import com.tuempresa.inventariovial.data.entity.PhotoEntity
import java.io.File

/** Convert staging only; never mutate the camera/gallery original or draw a watermark. */
object DeliveryJpeg {
    fun copy(original:File,target:File,photo:PhotoEntity?=null) {
        require(original.canonicalPath!=target.canonicalPath && original.isFile)
        val bounds=BitmapFactory.Options().apply {inJustDecodeBounds=true}
        BitmapFactory.decodeFile(original.absolutePath,bounds)
        require(bounds.outWidth>0 && bounds.outHeight>0) {"Fotografía ilegible."}
        if(bounds.outMimeType=="image/jpeg")original.copyTo(target,overwrite=true)
        else {
            val bitmap=requireNotNull(BitmapFactory.decodeFile(original.absolutePath)) {"Fotografía no decodificable."}
            try {target.outputStream().use {check(bitmap.compress(Bitmap.CompressFormat.JPEG,95,it))}}
            finally {bitmap.recycle()}
            val old=runCatching {ExifInterface(original.absolutePath)}.getOrNull()
            val exif=ExifInterface(target.absolutePath)
            listOf("Orientation","DateTime","DateTimeOriginal","DateTimeDigitized","GPSLatitude","GPSLatitudeRef","GPSLongitude","GPSLongitudeRef","GPSAltitude","GPSAltitudeRef","GPSDateStamp","GPSTimeStamp").forEach {tag->old?.getAttribute(tag)?.let {exif.setAttribute(tag,it)}}
            exif.saveAttributes()
        }
        if(photo!=null)com.tuempresa.inventariovial.camera.PhotoExif.write(target,original,photo)
    }
}
