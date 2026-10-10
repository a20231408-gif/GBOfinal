package com.tuempresa.inventariovial.road

import android.content.Context
import android.net.Uri
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/** Import requires explicit route selection; never infer route from filename. */
data class KmzRoutePreview(val routeCode: String, val sourceName: String, val sha256: String,
    val segments: List<RoadSegment>, val kml: ByteArray) {
    val vertices: Int get() = segments.sumOf { it.points.size }
}

object KmzRouteParser {
    private const val MAX_INPUT = 10 * 1024 * 1024
    private const val MAX_KML = 6 * 1024 * 1024

    fun extractKmz(bytes: ByteArray): ByteArray {
        require(bytes.size in 1..MAX_INPUT) { "KMZ demasiado grande o vacío" }
        var kml: ByteArray? = null
        var entries = 0
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries++
                require(entries <= 50) { "KMZ contiene demasiados archivos" }
                if (!entry.isDirectory && entry.name.endsWith(".kml", ignoreCase = true)) {
                    val out = ByteArrayOutputStream()
                    val buf = ByteArray(8192)
                    while (true) {
                        val read = zip.read(buf)
                        if (read < 0) break
                        require(out.size() + read <= MAX_KML) { "KML excede el tamaño permitido" }
                        out.write(buf, 0, read)
                    }
                    if (kml == null || entry.name.substringAfterLast('/').equals("doc.kml", true)) kml = out.toByteArray()
                }
                zip.closeEntry()
            }
        }
        return requireNotNull(kml) { "El KMZ no contiene un KML válido" }
    }

    fun parseKml(bytes: ByteArray, routeCode: String, sourceName: String = "archivo.kml"): KmzRoutePreview {
        require(bytes.size in 1..MAX_KML && routeCode.isNotBlank() && '/' !in routeCode && '\\' !in routeCode)
        val safeFactory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            setXIncludeAware(false)
            isExpandEntityReferences = false
            // Some Android document factories do not implement JAXP ACCESS_EXTERNAL_*.
            // DOCTYPE and external entities are already explicitly disabled above.
            runCatching { setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "") }
            runCatching { setAttribute("http://javax.xml.XMLConstants/property/accessExternalSchema", "") }
        }
        val document = safeFactory.newDocumentBuilder().parse(ByteArrayInputStream(bytes))
        val lineNodes = document.getElementsByTagNameNS("*", "LineString")
        require(lineNodes.length in 1..50) { "Se requiere al menos un eje LineString y no más de 50" }
        val route = normalizedRoadCode(routeCode)
        val segments = (0 until lineNodes.length).map { index ->
            val node = lineNodes.item(index) as Element
            val coordinates = node.getElementsByTagNameNS("*", "coordinates").item(0)?.textContent
                ?: error("Faltan coordenadas en eje ${index + 1}")
            val xy = coordinates.trim().split(Regex("\\s+")).map { part ->
                val components = part.split(',')
                require(components.size >= 2) { "Coordenada KML incompleta" }
                val lon = components[0].toDouble()
                val lat = components[1].toDouble()
                require(GeoMath.validCoordinate(lat, lon)) { "Coordenada KML fuera de rango" }
                lat to lon
            }.fold(mutableListOf<Pair<Double,Double>>()) { list, p ->
                if (list.lastOrNull() != p) list.add(p)
                list
            }
            require(xy.size in 2..25000) { "El eje debe contener entre 2 y 25 000 vértices" }
            var measure = 0.0
            val points = xy.mapIndexed { i, p ->
                if (i > 0) {
                    val previous = xy[i - 1]
                    val delta = GeoMath.distanceM(previous.first, previous.second, p.first, p.second)
                    require(delta > 0.001) { "Vértices repetidos" }
                    measure += delta
                }
                RoadPolylinePoint(route, "EJE", "${route}_IMPORT_${index+1}", i, p.first, p.second, measure)
            }
            RoadSegment(route, "EJE", "${route}_IMPORT_${index+1}", points)
        }
        require(segments.sumOf { it.points.size } <= 50000) { "Demasiados vértices en el KMZ" }
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        return KmzRoutePreview(route, sourceName.take(100), digest, segments, bytes)
    }

    fun parseKmz(bytes: ByteArray, route: String, sourceName: String = "archivo.kmz") =
        parseKml(extractKmz(bytes), route, sourceName)
}

/** Versioned, atomic selection: bundled reference remains the fallback if the active KML fails. */
class KmzGeometryStore(private val context: Context) {
    private val directory = File(context.filesDir, "road_geometry_versions")
    private val preferences = context.getSharedPreferences("active_road_geometry", Context.MODE_PRIVATE)

    fun stage(uri: Uri, route: String, source: String): KmzRoutePreview {
        val out = ByteArrayOutputStream()
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "No se pudo abrir el KMZ" }
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                require(out.size() + read <= 10 * 1024 * 1024) { "KMZ demasiado grande" }
                out.write(buffer, 0, read)
            }
        }
        return KmzRouteParser.parseKmz(out.toByteArray(), route, source)
    }

    fun activate(preview: KmzRoutePreview) {
        // Revalidate before making persistent; never change the bundled reference.
        val checked = KmzRouteParser.parseKml(preview.kml, preview.routeCode, preview.sourceName)
        require(checked.sha256 == preview.sha256 && checked.segments.isNotEmpty())
        // A raw KML line has no calibrated official PR anchor. Never silently break
        // a bundled route that already contains official PR chainages.
        val official = JsonRoadReferenceImporter().parse(
            context.assets.open("road_reference.json").bufferedReader().use { it.readText() }
        )
        require(official.prs.none { normalizedRoadCode(it.routeCode) == checked.routeCode }) {
            "La ruta tiene PR oficiales: se requiere calibrar el KMZ antes de activarlo. Se conserva la geometría anterior."
        }
        val versionDirectory = File(directory, checked.routeCode).apply { check(isDirectory || mkdirs()) }
        val version = File(versionDirectory, "${checked.sha256}.kml")
        if (!version.exists()) {
            val temp = File.createTempFile("pending_", ".kml", versionDirectory)
            try {
                temp.outputStream().use { it.write(checked.kml); it.fd.sync() }
                check(temp.renameTo(version)) { "No se pudo conservar la geometría anterior" }
            } finally { temp.delete() }
        }
        val saved = preferences.edit()
            .putString("active_${checked.routeCode}", checked.sha256)
            .putString("source_${checked.routeCode}", checked.sourceName)
            .putLong("date_${checked.routeCode}", System.currentTimeMillis()).commit()
        check(saved) { "No se pudo activar la geometría; conserva la ruta anterior" }
    }

    fun active(route: String): KmzRoutePreview? = runCatching {
        val safeRoute = normalizedRoadCode(route)
        val hash = preferences.getString("active_$safeRoute", null) ?: return null
        require(hash.matches(Regex("[a-f0-9]{64}")))
        val bytes = File(directory, "$safeRoute/$hash.kml").readBytes()
        val parsed = KmzRouteParser.parseKml(bytes, safeRoute, preferences.getString("source_$safeRoute", "KMZ").orEmpty())
        require(parsed.sha256 == hash)
        parsed
    }.getOrNull()

    fun versions(route: String): List<String> = File(directory, normalizedRoadCode(route)).listFiles()
        ?.filter { it.isFile && it.extension == "kml" }?.map { it.nameWithoutExtension }?.sorted() ?: emptyList()
}
