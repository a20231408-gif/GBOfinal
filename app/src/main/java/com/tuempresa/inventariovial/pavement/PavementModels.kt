package com.tuempresa.inventariovial.pavement

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

enum class PavementSurface(val label: String, val indexLabel: String) {
    ASPHALT("Asfáltico", "PCI"), RIGID("Rígido", "PCI"), UNSURFACED("No pavimentado", "URCI")
}

enum class PavementGeometry { LINEAR, AREA, POTHOLE, SLABS, GLOBAL }

data class PavementDamageDefinition(
    val code: Int, val name: String, val unit: String?, val geometry: PavementGeometry,
    val severities: List<String> = listOf("L", "M", "H")
)

object PavementCatalog {
    private val asphalt = listOf(
        1 to "Piel de cocodrilo", 2 to "Exudación", 3 to "Agrietamiento en bloque", 4 to "Abultamientos y hundimientos",
        5 to "Corrugación", 6 to "Depresión", 7 to "Grieta de borde", 8 to "Grieta de reflexión de junta",
        9 to "Desnivel carril/berma", 10 to "Grietas longitudinales y transversales", 11 to "Parcheo y acometidas",
        12 to "Pulimento de agregados", 13 to "Huecos", 14 to "Cruce de vía férrea", 15 to "Ahuellamiento",
        16 to "Desplazamiento", 17 to "Grieta parabólica", 18 to "Hinchamiento", 19 to "Desprendimiento de agregado", 20 to "Meteorización"
    )
    private val rigid = listOf(
        21 to "Rotura por pandeo", 22 to "Rotura de esquina", 23 to "Losa dividida", 24 to "Grieta de durabilidad D",
        25 to "Escalonamiento", 26 to "Daño del sello de junta", 27 to "Desnivel carril/berma", 28 to "Grietas lineales",
        29 to "Parcheo grande", 30 to "Parcheo pequeño", 31 to "Pulimento de agregados", 32 to "Desprendimientos",
        33 to "Bombeo", 34 to "Punzonamiento", 35 to "Cruce de vía férrea", 36 to "Descascaramiento",
        37 to "Grietas de retracción", 38 to "Astillamiento de esquina", 39 to "Astillamiento de junta"
    )
    private val unsurfaced = listOf(81 to "Sección transversal inadecuada", 82 to "Drenaje lateral inadecuado", 83 to "Corrugaciones", 84 to "Polvo", 85 to "Huecos", 86 to "Huellas", 87 to "Agregado suelto")
    fun forSurface(surface: PavementSurface): List<PavementDamageDefinition> = when (surface) {
        PavementSurface.ASPHALT -> asphalt.map { definition(it.first, it.second, surface) }
        PavementSurface.RIGID -> rigid.map { definition(it.first, it.second, surface) }
        PavementSurface.UNSURFACED -> unsurfaced.map { definition(it.first, it.second, surface) }
    }
    private fun definition(code: Int, name: String, surface: PavementSurface): PavementDamageDefinition {
        val global = code == 26 || code == 84
        val pothole = code == 13 || code == 85
        val slabs = surface == PavementSurface.RIGID
        val linear = code in setOf(4, 7, 8, 9, 10, 81, 82, 87)
        val unit = when { global -> null; pothole -> "Un."; slabs -> "Losas"; linear -> "m"; else -> "m²" }
        val severity = when (code) { 12, 31, 32, 33, 37 -> emptyList(); 19 -> listOf("M", "H"); else -> listOf("L", "M", "H") }
        return PavementDamageDefinition(code, name, unit, when { global -> PavementGeometry.GLOBAL; pothole -> PavementGeometry.POTHOLE; slabs -> PavementGeometry.SLABS; linear -> PavementGeometry.LINEAR; else -> PavementGeometry.AREA }, severity)
    }
}

@Entity(tableName = "pavement_samples", indices = [Index(value = ["ruta", "tramo", "calzada", "surface", "muestra"], unique = true), Index("inspectorUserId"), Index("surface")])
data class PavementSampleEntity(
    @PrimaryKey val id: String,
    val tramo: String,
    val ruta: String,
    val muestra: Int,
    val carril: String,
    val calzada: String,
    val prInicial: String,
    val prFinal: String,
    val anchoM: Double,
    val largoM: Double,
    val surface: String,
    val fecha: String,
    val inspectorUserId: String,
    val inspectorNombre: String,
    val status: String = "DRAFT",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0") val slabRows: Int = 0,
    @ColumnInfo(defaultValue = "0") val slabColumns: Int = 0,
    val laneWidthM: Double? = null,
    val calculationSnapshot: String? = null,
    val slabWidthM: Double? = null,
    val slabLengthM: Double? = null,
    val sicDataJson: String? = null
)

@Entity(tableName = "pavement_damages", foreignKeys = [ForeignKey(entity = PavementSampleEntity::class, parentColumns = ["id"], childColumns = ["sampleId"], onDelete = ForeignKey.CASCADE)], indices = [Index("sampleId")])
data class PavementDamageEntity(
    @PrimaryKey val id: String,
    val sampleId: String,
    val correlativo: Int,
    val surface: String,
    val code: Int,
    val name: String,
    val severity: String?,
    val geometry: String,
    val x1: Double?, val y1: Double?, val x2: Double?, val y2: Double?, val diameterM: Double?,
    val widthM: Double?, val lengthM: Double?, val areaM2: Double?, val quantity: Double?,
    val crackWidthMm: Double?, val observation: String?, val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "''") val slabs: String = "",
    @ColumnInfo(defaultValue = "''") val inspectorUserId: String = "",
    @ColumnInfo(defaultValue = "''") val inspectorName: String = "",
    val depthMm: Double? = null
)

@Entity(tableName = "pavement_damage_photos", foreignKeys = [ForeignKey(entity = PavementDamageEntity::class, parentColumns = ["id"], childColumns = ["damageId"], onDelete = ForeignKey.CASCADE)], indices = [Index("damageId")])
data class PavementDamagePhotoEntity(@PrimaryKey val id: String, val damageId: String, val localPath: String, val createdAt: Long = System.currentTimeMillis())
