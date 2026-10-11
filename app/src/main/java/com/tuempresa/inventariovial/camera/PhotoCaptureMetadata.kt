package com.tuempresa.inventariovial.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.tuempresa.inventariovial.data.entity.PhotoEntity
import com.tuempresa.inventariovial.location.GeoLocation
import com.tuempresa.inventariovial.location.TabletLocationProvider
import com.tuempresa.inventariovial.road.GeoMath
import kotlin.math.abs

data class PhotoCaptureMetadata(val capturedAt: Long?, val fix: GeoLocation?, val source: String = if (fix == null) "UNAVAILABLE_AT_CAPTURE" else "GNSS_AT_CAPTURE") {
    fun applyTo(photo: PhotoEntity) = photo.copy(capturedAt = capturedAt,
        photoLatitude = fix?.latitude, photoLongitude = fix?.longitude, photoAltitudeM = fix?.altitude?.takeIf { it.isFinite() },
        photoGpsAccuracyM = fix?.horizontalAccuracy?.toDouble()?.takeIf { it.isFinite() && it >= 0 }, photoGpsTimestamp = fix?.timestamp?.takeIf { it > 0 },
        photoGnssProvider = if (source == "EXIF_ORIGINAL") null else fix?.provider?.name, photoSatellites = fix?.satellites, photoHdop = fix?.hdop,
        photoIsRtkFixed = if (source == "EXIF_ORIGINAL") null else fix?.isRtkFixed, photoLocationSource = source)
    companion object {
        fun fromOriginal(path: String): PhotoCaptureMetadata {
            return runCatching {
                val exif = android.media.ExifInterface(path)
                val captured = exif.getAttribute(android.media.ExifInterface.TAG_DATETIME_ORIGINAL)?.let {
                    runCatching { java.text.SimpleDateFormat("yyyy:MM:dd HH:mm:ss", java.util.Locale.US).parse(it)?.time }.getOrNull()
                }
                val coords = FloatArray(2)
                val fix = if (exif.getLatLong(coords)) GeoLocation(coords[0].toDouble(), coords[1].toDouble(),
                    exif.getAttributeDouble("GPSHPositioningError", Double.NaN).toFloat(),
                    exif.getAltitude(Double.NaN).takeIf { it.isFinite() }, timestamp = captured ?: 0) else null
                PhotoCaptureMetadata(captured, fix, if (fix == null) "UNAVAILABLE_IMPORTED" else "EXIF_ORIGINAL")
            }.getOrDefault(PhotoCaptureMetadata(null, null, "UNAVAILABLE_IMPORTED"))
        }

        fun validFix(fix: GeoLocation?, capturedAt: Long): GeoLocation? = fix?.takeIf {
            GeoMath.validCoordinate(it.latitude, it.longitude) && it.horizontalAccuracy.isFinite() &&
                it.horizontalAccuracy >= 0 && abs(it.timestamp - capturedAt) <= 30_000
        }
        /** Independent sample at camera completion; never uses the element's representative position. */
        fun capture(context: Context, capturedAt: Long = System.currentTimeMillis(), onResult: (PhotoCaptureMetadata) -> Unit) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                onResult(PhotoCaptureMetadata(capturedAt, null)); return
            }
            try { TabletLocationProvider(context).getCurrentLocation(
                { onResult(PhotoCaptureMetadata(capturedAt, validFix(it, capturedAt))) },
                { onResult(PhotoCaptureMetadata(capturedAt, null)) })
            } catch (_: Exception) { onResult(PhotoCaptureMetadata(capturedAt, null)) }
        }
    }
}
