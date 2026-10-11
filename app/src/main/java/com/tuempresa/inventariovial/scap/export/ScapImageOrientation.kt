package com.tuempresa.inventariovial.scap.export

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.InputStream

internal object ScapImageOrientation {
    fun read(input:InputStream):Int=runCatching {
        ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL)
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    fun apply(bitmap:Bitmap,orientation:Int):Bitmap {
        val matrix=Matrix()
        when(orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL->matrix.setScale(-1f,1f)
            ExifInterface.ORIENTATION_ROTATE_180->matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL->matrix.setScale(1f,-1f)
            ExifInterface.ORIENTATION_TRANSPOSE->{matrix.setRotate(90f);matrix.postScale(-1f,1f)}
            ExifInterface.ORIENTATION_ROTATE_90->matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE->{matrix.setRotate(-90f);matrix.postScale(-1f,1f)}
            ExifInterface.ORIENTATION_ROTATE_270->matrix.setRotate(-90f)
            else->return bitmap
        }
        return Bitmap.createBitmap(bitmap,0,0,bitmap.width,bitmap.height,matrix,true)
    }
    fun normalize(bytes:ByteArray):ByteArray {
        val orientation=bytes.inputStream().use(::read)
        if(orientation==ExifInterface.ORIENTATION_NORMAL || orientation==ExifInterface.ORIENTATION_UNDEFINED) return bytes
        val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
        var sample=1;while(maxOf(bounds.outWidth,bounds.outHeight)/sample>1600)sample*=2
        val decoded=BitmapFactory.decodeByteArray(bytes,0,bytes.size,BitmapFactory.Options().apply{inSampleSize=sample}) ?: error("Imagen EXIF ilegible")
        val oriented=apply(decoded,orientation)
        return try {ByteArrayOutputStream().use {out->check(oriented.compress(Bitmap.CompressFormat.JPEG,95,out));out.toByteArray()}}
        finally {if(oriented!==decoded) oriented.recycle();decoded.recycle()}
    }
}
