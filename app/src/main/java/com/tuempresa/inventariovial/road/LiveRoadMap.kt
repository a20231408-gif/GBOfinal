package com.tuempresa.inventariovial.road

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.cos
import kotlin.math.hypot

/** Offline map: local road_reference geometry + Room records. Supports pinch zoom and pan without internet. */
@Composable
fun LiveRoadMap(reference: RoadReferenceData, routeCode: String, assets: List<OrderedAsset>, modifier: Modifier = Modifier) {
    SideEffect{com.tuempresa.inventariovial.diagnostics.StabilityDiagnostics.screen("MAP")}
    val segments = remember(reference, routeCode) {
        reference.segments.filter { normalizedRoadCode(it.routeCode) == normalizedRoadCode(routeCode) }
    }
    if (segments.isEmpty()) {
        Text("No hay geometría local para $routeCode.", color = MaterialTheme.colorScheme.error)
        return
    }
    val routePoints = remember(segments) { segments.flatMap { it.points.sortedBy { p -> p.sequence } } }
    val sampled = remember(routePoints) {
        val step = (routePoints.size / 1600).coerceAtLeast(1)
        routePoints.filterIndexed { index, _ -> index % step == 0 }.toMutableList().apply {
            routePoints.lastOrNull()?.let { last -> if (lastOrNull() != last) add(last) }
        }
    }
    var selectedId by remember(routeCode) { mutableStateOf<String?>(null) }
    val selected = assets.firstOrNull { it.recordId == selectedId }
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurface

    var zoom by remember(routeCode) { mutableFloatStateOf(1f) }
    var pan by remember(routeCode) { mutableStateOf(Offset.Zero) }

    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("MAPA OFFLINE · ORDEN EN VIVO", style = MaterialTheme.typography.titleMedium)
                    Text("Pellizca para zoom y arrastra para desplazarte. No requiere internet.", style = MaterialTheme.typography.bodySmall)
                }
                Text(String.format(Locale.US, "%.1fx", zoom), style = MaterialTheme.typography.labelLarge)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { zoom = (zoom * 1.5f).coerceAtMost(14f) }) { Text("+") }
                OutlinedButton(onClick = { zoom = (zoom / 1.5f).coerceAtLeast(1f); if (zoom == 1f) pan = Offset.Zero }) { Text("−") }
                TextButton(onClick = { zoom = 1f; pan = Offset.Zero }) { Text("Restablecer") }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(430.dp)
                    .background(surface)
                    .pointerInput(routeCode) {
                        detectTransformGestures { centroid, panChange, zoomChange, _ ->
                            val oldZoom = zoom
                            val newZoom = (oldZoom * zoomChange).coerceIn(1f, 14f)
                            val appliedScale = if (oldZoom > 0f) newZoom / oldZoom else 1f
                            val center = Offset(size.width / 2f, size.height / 2f)

                            // Keep the geographical point beneath the fingers beneath the fingers.
                            // Previous implementation always scaled around the map center, causing the
                            // requested element to slide off-screen during pinch zoom.
                            val focalFromCenter = centroid - center
                            val scaledPan = pan * appliedScale
                            var nextPan = scaledPan + focalFromCenter * (1f - appliedScale) + panChange

                            if (newZoom <= 1.001f) {
                                nextPan = Offset.Zero
                            } else {
                                val maxX = size.width.toFloat() * newZoom
                                val maxY = size.height.toFloat() * newZoom
                                nextPan = Offset(
                                    nextPan.x.coerceIn(-maxX, maxX),
                                    nextPan.y.coerceIn(-maxY, maxY)
                                )
                            }
                            zoom = newZoom
                            pan = nextPan
                        }
                    }
            ) {
                Canvas(
                    Modifier.fillMaxSize().pointerInput(sampled, assets, zoom, pan) {
                        detectTapGestures { tap ->
                            val projector = MapProjector(sampled, size.width.toFloat(), size.height.toFloat())
                            val center = Offset(size.width / 2f, size.height / 2f)
                            fun transformed(base: Offset): Offset = Offset(
                                center.x + (base.x - center.x) * zoom + pan.x,
                                center.y + (base.y - center.y) * zoom + pan.y
                            )
                            selectedId = assets.mapNotNull { asset ->
                                val r = asset.item.record
                                val p = projector.project(r.projectedLatitude ?: r.latitude, r.projectedLongitude ?: r.longitude)
                                    ?.let(::transformed)
                                if (p == null) null else asset.recordId to hypot((p.x - tap.x).toDouble(), (p.y - tap.y).toDouble())
                            }.minByOrNull { it.second }?.takeIf { it.second <= 48.0 }?.first
                        }
                    }
                ) {
                    val projector = MapProjector(sampled, size.width, size.height)
                    val center = Offset(size.width / 2f, size.height / 2f)
                    fun transformed(base: Offset): Offset = Offset(
                        center.x + (base.x - center.x) * zoom + pan.x,
                        center.y + (base.y - center.y) * zoom + pan.y
                    )
                    segments.forEach { segment ->
                        val points = segment.points.sortedBy { it.sequence }
                        if (points.size >= 2) {
                            val step = (points.size / 1600).coerceAtLeast(1)
                            val path = Path()
                            var started = false
                            points.forEachIndexed { i, p ->
                                if (i % step == 0 || i == points.lastIndex) projector.project(p.latitude, p.longitude)?.let { q0 ->
                                    val q = transformed(q0)
                                    if (!started) { path.moveTo(q.x, q.y); started = true } else path.lineTo(q.x, q.y)
                                }
                            }
                            drawPath(path, Color.Black, style = Stroke(width = 9f))
                            drawPath(path, Color(0xFFFFD600), style = Stroke(width = 5f))
                        }
                    }
                    val first = routePoints.firstOrNull()?.let { projector.project(it.latitude, it.longitude) }?.let(::transformed)
                    val last = routePoints.lastOrNull()?.let { projector.project(it.latitude, it.longitude) }?.let(::transformed)
                    first?.let { drawCircle(Color(0xFF1B8A3C), 9f, it) }
                    last?.let { drawCircle(Color(0xFFD32F2F), 9f, it) }

                    assets.forEach { asset ->
                        val r = asset.item.record
                        val projected = projector.project(r.projectedLatitude ?: r.latitude, r.projectedLongitude ?: r.longitude)
                            ?.let(::transformed) ?: return@forEach
                        val colorFamily = AssetPresentationCatalog.colorFamily(asset.item)
                        val color = Color(0xFF000000L or colorFamily.rgb.toLong())
                        val radius = if (asset.recordId == selectedId) 12f else 8f
                        drawCircle(Color.White, radius + 3f, projected)
                        drawCircle(color, radius, projected)
                        // At low zoom only the point is emphasized. Text becomes useful once the user zooms in.
                        if (zoom >= 1.35f || asset.recordId == selectedId) {
                            drawContext.canvas.nativeCanvas.apply {
                                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                    textSize = (20f + (zoom.coerceAtMost(4f) - 1f) * 2f)
                                    this.color = android.graphics.Color.rgb(30, 30, 30)
                                    isFakeBoldText = true
                                }
                                drawText(asset.shortCode, projected.x + 11f, projected.y - 8f, paint)
                            }
                        }
                    }
                }
            }
            Text("A bajo zoom se priorizan los puntos para evitar solapamiento de etiquetas; al acercar aparecen los códigos.", style = MaterialTheme.typography.bodySmall)
            Text("Toca un punto para ver su registro y fotografía principal.", style = MaterialTheme.typography.bodySmall)
            Text("LEYENDA POR FORMATO SIC", style = MaterialTheme.typography.labelLarge)
            AssetPresentationCatalog.legend.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { family ->
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.size(10.dp).background(Color(0xFF000000L or family.rgb.toLong())))
                            Text("${family.sicGroup} · ${legendDescription(family)}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            selected?.let { asset -> SelectedAssetPreview(asset, labelColor) }
        }
    }
}

private fun legendDescription(family: AssetFamily): String = when (family.sicGroup) {
    "SIC-17" -> "Puentes"
    "SIC-18" -> "Alcantarillas"
    "SIC-19" -> "Drenaje"
    "SIC-20" -> "Badén/Túnel/Muro"
    "SIC-21" -> "Señal. horizontal"
    "SIC-22" -> "Señal. vertical"
    "SIC-23" -> "Derecho de vía"
    else -> "Otros"
}

@Composable
private fun SelectedAssetPreview(asset: OrderedAsset, labelColor: Color) {
    val record = asset.item.record
    val photo = remember(asset.item.photos) { asset.item.photos.sortedBy { it.photoIndex }.firstOrNull() }
    val path = photo?.let { p -> p.stampedPath?.takeIf { File(it).isFile } ?: p.localPath.takeIf { File(it).isFile } }
    val bitmap by produceState<android.graphics.Bitmap?>(null, path) {
        value = if (path == null) null else withContext(Dispatchers.IO) {
            runCatching{com.tuempresa.inventariovial.camera.FieldThumbnailCache.get(path,480)}.getOrNull()
        }
    }
    HorizontalDivider()
    Text("${asset.shortCode} · ${asset.assetFamily.label}", style = MaterialTheme.typography.titleMedium)
    Text(asset.axisMeasureM?.let { String.format(Locale.US, "Eje: %.3f km", it / 1000.0) } ?: "Eje: sin matching", color = labelColor)
    Text(record.distanceToRoadAxisM?.let { String.format(Locale.US, "Distancia al eje: %.1f m", it) } ?: "Distancia al eje: no determinada")
    Text("GPS: ${String.format(Locale.US, "%.9f, %.9f", record.latitude, record.longitude)} · ±${record.gpsAccuracyM ?: "?"} m")
    Text("Carpeta prevista: ${asset.sibCode ?: "SIB ?"}/${asset.folderName}")
    bitmap?.let { Image(it.asImageBitmap(), "Foto principal ${asset.shortCode}", Modifier.fillMaxWidth().heightIn(max = 280.dp), contentScale = ContentScale.Fit) }
    if (photo != null) Text("Foto ${photo.photoIndex} · ${photo.photoCategory ?: "sin categoría"} · EXIF/metadata vinculados al UUID ${photo.id}", style = MaterialTheme.typography.bodySmall)
}

private class MapProjector(points: List<RoadPolylinePoint>, width: Float, height: Float) {
    private val midLat = points.map { it.latitude }.average().takeIf { it.isFinite() } ?: 0.0
    private val cosLat = cos(Math.toRadians(midLat)).coerceAtLeast(0.1)
    private val xy = points.map { (it.longitude * cosLat) to it.latitude }
    private val minX = xy.minOfOrNull { it.first } ?: 0.0
    private val maxX = xy.maxOfOrNull { it.first } ?: 1.0
    private val minY = xy.minOfOrNull { it.second } ?: 0.0
    private val maxY = xy.maxOfOrNull { it.second } ?: 1.0
    private val pad = 24f
    private val rangeX = (maxX - minX).takeIf { it > 1e-12 } ?: 1.0
    private val rangeY = (maxY - minY).takeIf { it > 1e-12 } ?: 1.0
    private val scale = minOf((width - 2 * pad) / rangeX.toFloat(), (height - 2 * pad) / rangeY.toFloat())
    private val drawnW = rangeX * scale
    private val drawnH = rangeY * scale
    private val ox = ((width - drawnW) / 2.0).toFloat()
    private val oy = ((height - drawnH) / 2.0).toFloat()

    fun project(latitude: Double, longitude: Double): Offset? {
        if (!GeoMath.validCoordinate(latitude, longitude)) return null
        val x = (longitude * cosLat - minX) * scale + ox
        val y = (maxY - latitude) * scale + oy
        return Offset(x.toFloat(), y.toFloat())
    }
}
