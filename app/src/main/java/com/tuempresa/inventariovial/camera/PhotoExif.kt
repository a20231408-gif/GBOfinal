package com.tuempresa.inventariovial.camera

import android.media.ExifInterface
import com.tuempresa.inventariovial.data.entity.PhotoEntity
import com.tuempresa.inventariovial.location.GeoLocation
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object PhotoExif {
    /** A previously branded import can itself be the original. Never edit that file in place. */
    fun writableCopy(original: File, stamped: File, directory: File): File {
        if (original.canonicalPath != stamped.canonicalPath) return stamped
        check(directory.isDirectory || directory.mkdirs())
        return stamped.copyTo(File(directory, "${java.util.UUID.randomUUID()}.jpg"))
    }
    fun supportsHorizontalAccuracy(): Boolean = ExifInterface::class.java.fields.any { it.name == "TAG_GPS_H_POSITIONING_ERROR" }
    private val gpsTags = listOf("GPSLatitude", "GPSLatitudeRef", "GPSLongitude", "GPSLongitudeRef",
        "GPSAltitude", "GPSAltitudeRef", "GPSHPositioningError", "GPSTimeStamp", "GPSDateStamp")
    fun location(exif: ExifInterface, fix: GeoLocation?) {
        if (fix == null) return
        exif.setAttribute("GPSLatitude", coordinate(fix.latitude))
        exif.setAttribute("GPSLatitudeRef", if (fix.latitude < 0) "S" else "N")
        exif.setAttribute("GPSLongitude", coordinate(fix.longitude))
        exif.setAttribute("GPSLongitudeRef", if (fix.longitude < 0) "W" else "E")
        fix.altitude?.takeIf { it.isFinite() }?.let {
            exif.setAttribute("GPSAltitude", "${(abs(it) * 1000).roundToLong()}/1000")
            exif.setAttribute("GPSAltitudeRef", if (it < 0) "1" else "0")
        }
        if (supportsHorizontalAccuracy() && fix.horizontalAccuracy.isFinite() && fix.horizontalAccuracy >= 0)
            exif.setAttribute("GPSHPositioningError", "${(fix.horizontalAccuracy * 1000).roundToLong()}/1000")
    }
    /** Only the watermarked delivery copy is writable; UserComment (watermark marker) is untouched. */
    fun write(stamped: File, original: File, photo: PhotoEntity, description: String? = null) {
        require(stamped.canonicalPath != original.canonicalPath) { "EXIF requiere una copia de la fotografía." }
        val exif = ExifInterface(stamped.absolutePath)
        val originalExif = runCatching { ExifInterface(original.absolutePath) }.getOrNull()
        gpsTags.forEach { exif.setAttribute(it, originalExif?.getAttribute(it)) }
        if (photo.photoLatitude != null && photo.photoLongitude != null) {
            location(exif, GeoLocation(photo.photoLatitude, photo.photoLongitude,
                photo.photoGpsAccuracyM?.toFloat() ?: Float.NaN, photo.photoAltitudeM,
                timestamp = photo.photoGpsTimestamp ?: photo.capturedAt ?: photo.createdAt))
        }
        val date = photo.capturedAt?.let { SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(Date(it)) }
            ?: originalExif?.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
        date?.let { exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, it) }
        description?.let { exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, it) }
        exif.saveAttributes()
    }
    private fun coordinate(value: Double): String {
        val degrees = abs(value).toInt()
        val minutes = ((abs(value) - degrees) * 60).toInt()
        val seconds = ((abs(value) - degrees) * 3600 - minutes * 60) * 1_000_000
        return "$degrees/1,$minutes/1,${seconds.roundToLong()}/1000000"
    }
}
