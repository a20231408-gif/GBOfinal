package com.tuempresa.inventariovial.pavement

import org.json.JSONObject
import java.io.OutputStream

enum class PavementSicFormat(val label: String, val surface: String, val source: String, val types: List<String>, val holeCode: Int) {
    SIC14("SIC-14", "ASPHALT", "Formato SIC-14, imagen adjunta 1", listOf("Piel de cocodrilo","Fisuras longitudinales","Deformación","Ahuellamiento visco-elástico","Reparaciones o bacheo","Desprendimiento","Huecos","Fisuras transversales","Exudación","Daño de berma pavimentada","Daño de berma no pavimentada"),7),
    SIC14A("SIC-14A", "ASPHALT", "Formato SIC-14a, imagen adjunta 2", listOf("Piel de cocodrilo","Exudación","Agrietamiento en bloque","Abultamiento y hundimientos","Corrugación","Depresión","Grieta de borde","Grieta de reflexión de junta","Desnivel carril / berma","Grietas longitudinales y transversales","Parcheo","Pulimiento de agregados","Huecos","Cruce de vía férrea","Ahuellamiento","Desplazamiento","Grieta parabólica (slippage)","Hinchamiento","Desprendimiento de agregados","Daño de berma pavimentada","Daño de berma no pavimentada"),13),
    SIC15("SIC-15", "RIGID", "Formato SIC-15, imagen adjunta 3", listOf("Desnivel entre losas","Fisuras longitudinales","Fisuras transversales","Fisuras de esquina","Fisuras oblicuas","Reparaciones o bacheo","Despostillamiento de juntas","Desprendimiento","Huecos","Tratamiento superficial","Daño de berma pavimentada","Daño de berma no pavimentada"),9),
    SIC16("SIC-16", "UNSURFACED", "Formato SIC-16, imagen adjunta 5", listOf("Deformación","Erosión","Huecos","Encalaminento","Lodozal","Cruce de agua"),3)
}

data class PavementSicRow(val damageId: String, val values: List<Any?>, val missing: List<String>)
data class PavementSicStatus(val format: PavementSicFormat, val rows: List<PavementSicRow>, val blockers: List<String>) {
    val missingCount get() = rows.sumOf { it.missing.size } + blockers.size
    val complete get() = rows.isNotEmpty() && missingCount == 0
}

/** Contractual SIC codes are independent of manual PCI/URCI codes. No implicit broad conversion. */
object PavementSic {
    const val manualOrigin = "MANUAL_PENDING_EXTERNAL_SOURCE"
    val headers = listOf("Ruta","Calzada","Faja","Código PR inicio","Distancia inicio","Código PR fin","Distancia fin","Tipo de daño","Longitud","Ancho","Nivel de gravedad","Clase de densidad","Fecha")
    val manualFields = listOf("faja","prInicio","distanciaInicio","prFin","distanciaFin","tipo","longitud","ancho","gravedad","densidad")
    fun knownType(format: PavementSicFormat, code: Int): Int? = when(format) {
        PavementSicFormat.SIC14 -> mapOf(1 to 1, 2 to 9, 11 to 5, 13 to 7, 15 to 4, 19 to 6)[code]
        PavementSicFormat.SIC14A -> code.takeIf { it in 1..19 }
        PavementSicFormat.SIC15 -> mapOf(22 to 4, 25 to 1, 29 to 6, 30 to 6, 39 to 7)[code]
        PavementSicFormat.SIC16 -> if(code==85)3 else null
    }
    fun supplement(s: PavementSampleEntity, format: PavementSicFormat, damageId: String): JSONObject =
        s.sicDataJson?.let { JSONObject(it).optJSONObject(format.name)?.optJSONObject(damageId) } ?: JSONObject()
    fun value(s: PavementSampleEntity, format: PavementSicFormat, damageId: String, key: String) =
        supplement(s,format,damageId).optJSONObject(key)?.optString("value").orEmpty()
    fun update(s: PavementSampleEntity, format: PavementSicFormat, damageId: String, values: Map<String,String>): String {
        require(format.surface==s.surface)
        val root=s.sicDataJson?.let(::JSONObject)?:JSONObject()
        val rows=root.optJSONObject(format.name)?:JSONObject()
        val row=rows.optJSONObject(damageId)?:JSONObject()
        values.forEach { (key,text) ->
            require(key in manualFields)
            if(text.isBlank())row.remove(key)else row.put(key,JSONObject().put("value",text.trim()).put("origin",manualOrigin))
        }
        rows.put(damageId,row);root.put(format.name,rows)
        return root.toString()
    }
    fun status(s: PavementSampleEntity, damages: List<PavementDamageEntity>, format: PavementSicFormat): PavementSicStatus {
        require(s.surface==format.surface)
        val rows=damages.map { d ->
            val missing=mutableListOf<String>()
            fun text(key: String)=value(s,format,d.id,key)
            fun numeric(key: String, fallback: Double? = null, zero: Boolean = false): Double? {
                val raw=text(key);val number=if(raw.isBlank())fallback else raw.replace(',','.').toDoubleOrNull()
                if(number==null || !number.isFinite() || if(zero)number<0 else number<=0) { missing+=key;return null };return number
            }
            fun code(key: String, range: IntRange, fallback: Int?=null): Int? {
                val raw=text(key);val n=if(raw.isBlank())fallback else raw.toIntOrNull()
                if(n==null || n !in range) { missing+=key;return null };return n
            }
            fun pr(key: String): String? = text(key).takeIf { it.matches(Regex("[0-9]{4}")) } ?: run { missing+=key;null }
            val type=code("tipo",1..format.types.size,knownType(format,d.code))
            val faja=if(format==PavementSicFormat.SIC16)2 else code("faja",1..Int.MAX_VALUE)
            val initial=pr("prInicio");val distanceInitial=numeric("distanciaInicio",zero=true)
            val final=pr("prFin");val distanceFinal=numeric("distanciaFin",zero=true)
            val length=numeric("longitud",when(d.geometry) { "LINEAR","AREA" -> d.lengthM; "POTHOLE" -> d.diameterM; else -> null })
            val width=numeric("ancho",when(d.geometry) { "LINEAR","AREA" -> d.widthM?.takeIf { it>0 }; "POTHOLE" -> d.diameterM; else -> null })
            val severity=code("gravedad",1..3,when(PavementSeverity.canonical(d.severity)) { "L" -> 1; "M" -> 2; "H" -> 3; else -> null })
            val density=if(type==format.holeCode)code("densidad",1..3)else null
            if(s.ruta.isBlank())missing+="ruta"
            if(s.calzada !in PavementHeader.roadbeds)missing+="calzada"
            if(s.fecha.isBlank())missing+="fecha"
            PavementSicRow(d.id,listOf(s.ruta,s.calzada,faja,initial,distanceInitial,final,distanceFinal,type,length,width,severity,density,s.fecha),missing)
        }
        val blockers=buildList {
            if(damages.isEmpty())add("Sin daños capturados")
            // Source explicitly labels density for 07, but its type list labels potholes as 13.
            if(format==PavementSicFormat.SIC14A && rows.any{it.values[7] in listOf(7,13)})add("Plantilla SIC-14A contradictoria: densidad indica 07; huecos está en 13. Confirmar regla contractual.")
        }
        return PavementSicStatus(format,rows,blockers)
    }
    fun write(output: OutputStream, s: PavementSampleEntity, damages: List<PavementDamageEntity>, format: PavementSicFormat) {
        val status=status(s,damages,format)
        require(status.complete) { "${format.label}: faltan ${status.missingCount}. ${status.blockers.joinToString()}" }
        PavementExcel.workbook(output,listOf(format.label to (listOf(headers)+status.rows.map { it.values })),locationGroups=true)
    }
}
