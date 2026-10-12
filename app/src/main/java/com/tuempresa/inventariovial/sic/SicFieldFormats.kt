package com.tuempresa.inventariovial.sic

import com.tuempresa.inventariovial.core.*
import com.tuempresa.inventariovial.export.*
import com.tuempresa.inventariovial.supplementary.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

const val ACCIDENT_TYPE_SOURCE_CONFLICT = "ACCIDENT_TYPE_SOURCE_CONFLICT"
data class FieldOption(val code:String,val name:String,val description:String="") {
    val label get() = "$code · $name"
}
data class SicField(val key:String,val label:String,val kind:CellKind=CellKind.TEXT,
    val options:List<FieldOption> = emptyList(),val required:Boolean=true)

/** MAN_8 printed pp.146/148/166/160/217. Order is the graphical contract, not database order. */
enum class SicFieldFormat(val code:String,val title:String,val family:String) {
    SIC08("SIC-08","FAJAS DE LAS CALZADAS","CALZADA_FAJAS"),
    SIC09("SIC-09","ANCHO DE LAS FAJAS","CALZADA_FAJAS"),
    SIC13("SIC-13","PUNTOS CRÍTICOS","PUNTOS_CRITICOS"),
    SIC34("SIC-34","ACCIDENTES DE TRÁNSITO","ACCIDENTES"),
    SIC18A("SIC-18A","ALCANTARILLAS — CONDICIÓN MALA","ALCANTARILLAS");

    val hasFaja get() = this in setOf(SIC08,SIC09,SIC34)
    val hasEnd get() = this!=SIC18A
    val fields:List<SicField> get() {
        val start=listOf(SicField("startPrCode","Código PR inicio"),SicField("startDistanceM","Distancia inicio (m)",CellKind.DECIMAL))
        val end=if(hasEnd) listOf(SicField("endPrCode","Código PR fin"),SicField("endDistanceM","Distancia fin (m)",CellKind.DECIMAL)) else emptyList()
        val common=listOf(SicField("routeCode","Ruta"),SicField("roadbedCode","Calzada"))+
            (if(hasFaja) listOf(SicField("faja","Faja",CellKind.INTEGER)) else emptyList())+start+end
        return common+when(this) {
            SIC08 -> listOf(SicField("typeCode","Tipo",options=fajaTypes),SicField("directionCode","Sentido del tránsito",options=directions))
            SIC09 -> listOf(SicField("usefulWidthM","Ancho útil (m)",CellKind.DECIMAL),SicField("totalWidthM","Ancho total (m)",CellKind.DECIMAL))
            SIC13 -> listOf(SicField("sideCode","Lado",options=sides),SicField("classCode","Clase",options=criticalClasses))
            SIC34 -> listOf(SicField("typeCode","Tipo",options=accidentTypes),SicField("levelCode","Nivel",options=levels),SicField("description","Descripción",required=false))
            SIC18A -> SupplementaryForms.fields(SupplementaryFormat.SIC18A).map { f ->
                SicField(f.key,f.label,if(f.key=="spans") CellKind.INTEGER else CellKind.TEXT,
                    f.options.map {FieldOption(it.substringBefore(" - "),it.substringAfter(" - "))})
            }
        }+SicField("surveyDate","Fecha",CellKind.DATE)
    }
    val columns get() = fields.map { f -> SicColumn(when(f.key) {
        "startPrCode","endPrCode" -> "Código PR"
        "startDistanceM","endDistanceM" -> "Distancia"
        "failureLocationCode" -> "Ubicación de Falla"
        "failureTypeCode" -> "Tipo de Falla"
        "functionalStateCode" -> "Estado"
        "probableCauseCode" -> "Causa probable"
        else -> f.label
    },f.kind,if(f.key=="description") 45.0 else 14.0) }
    companion object {
        val fajaTypes=listOf("Tránsito","Berma","Peaje","Cruce","Localidad","Paradero").mapIndexed {i,n->FieldOption("${i+1}",n)}
        val directions=listOf(FieldOption("C","Creciente"),FieldOption("D","Decreciente"),FieldOption("A","Ambos sentidos"))
        val sides=listOf(FieldOption("D","Derecho"),FieldOption("I","Izquierdo"))
        val criticalClasses=listOf(
            FieldOption("15","Fallas Constructivas","Comentario libre sobre fallas constructivas."),
            FieldOption("16","Fallas Geológicas","Problemas de geodinámica externa; sectores construidos sobre piroclastos."),
            FieldOption("17","Fallas Geotécnicas","Erosión de plataforma o taludes, huaycos, inestabilidad, derrumbes, caída de rocas y suelos blandos."),
            FieldOption("17A","Problemas Hidrológicos","Inundaciones, nevadas, nivel freático superficial, heladas, crecida y desborde de cauces."),
            FieldOption("17B","Geografía de la Zona","Zonas inundables, carreteras próximas a cauces y pérdida de plataforma."),
            FieldOption("17C","Zonas con Alto Deterioro","Comentario libre sobre alto deterioro."),
            FieldOption("17D","Zonas de Riesgo Probable","Puentes, pontones u otras estructuras por colapsar. Tabla III.13: Riesgo Elevado; formato SIC-13: Riesgo Probable.")
        )
        val accidentTypes=listOf("Imprudencias de los usuarios","Desperfectos mecánicos","Geometría de la vía","Estado de la vía","Falta de señalización adecuada")
            .mapIndexed {i,n->FieldOption("${i+1}",n)}+listOf(FieldOption(ACCIDENT_TYPE_SOURCE_CONFLICT,"Problemas Geotécnicos y Geológicos"),FieldOption("7","Otro"))
        val levels=listOf(FieldOption("1","BAJO","Daños materiales leves sin lesiones de usuarios."),
            FieldOption("2","MEDIO","Daños materiales significativos y/o lesiones de usuarios."),FieldOption("3","ALTO","Pérdidas humanas."))
        fun fromCode(code:String)=entries.firstOrNull {it.code==code}
        val dateFormatter:DateTimeFormatter=DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)
    }
}

data class SicFieldState(val format:SicFieldFormat,val values:Map<String,String> = emptyMap(),val sourceRecordId:String?=null) {
    fun text(key:String)=values[key].orEmpty()
    fun set(key:String,value:String)=copy(values=values+(key to value))
    fun completion():CompletionResult {
        val issues=mutableListOf<ValidationIssue>()
        fun issue(label:String,reason:String,severity:ValidationIssue.Severity=ValidationIssue.Severity.BLOCKING_EXPORT) {
            issues+=ValidationIssue(label,reason,severity)
        }
        if(text("_gpsError").isNotBlank()) issue("GPS",text("_gpsError"))
        for(f in format.fields) {
            if(format==SicFieldFormat.SIC18A && f.key in Sic18ARules.failureKeys && !Sic18ARules.failuresRequired(values)) continue
            val v=if(f.key in setOf("startPrCode","endPrCode","startDistanceM","endDistanceM"))text(f.key) else text(f.key).trim()
            if(f.key in setOf("startDistanceM","endDistanceM") && com.tuempresa.inventariovial.validation.ContractualLocationInput.distanceError(text(f.key))!=null && v.isNotBlank()) issue(f.label,"Distancia: máximo tres cifras enteras")
            if(v.isBlank()) {if(f.required) issue(f.label,"Falta completar",ValidationIssue.Severity.MISSING_USER_INPUT);continue}
            if(f.key=="typeCode" && v==ACCIDENT_TYPE_SOURCE_CONFLICT && format==SicFieldFormat.SIC34) {
                issue("Tipo","Tipo pendiente de equivalencia contractual",ValidationIssue.Severity.UNRESOLVED_MAPPING);continue
            }
            val options=if(format==SicFieldFormat.SIC18A && f.key=="typeCode" && text("classCode")=="07") (1..4).map {FieldOption("$it","")} else f.options
            if(options.isNotEmpty() && options.none {it.code==v}) issue(f.label,"Código inválido")
            when(f.kind) {
                CellKind.DECIMAL -> if(number(v)==null || number(v)!!<0) issue(f.label,"Ingresa un decimal positivo o cero")
                CellKind.INTEGER -> if(v.toIntOrNull()?.let {it>0}!=true) issue(f.label,"Ingresa un entero mayor que cero")
                CellKind.DATE -> if(runCatching {LocalDate.parse(v,SicFieldFormat.dateFormatter)}.isFailure) issue(f.label,"Usa dd/mm/aaaa válido")
                else -> Unit
            }
            if(f.key in setOf("startPrCode","endPrCode") && !v.matches(Regex("[0-9]{1,4}"))) issue(f.label,"PR de hasta cuatro dígitos")
        }
        if(format==SicFieldFormat.SIC09) {
            val useful=number(text("usefulWidthM"));val total=number(text("totalWidthM"))
            if(useful!=null && total!=null && total<useful) issue("Ancho total","Debe ser mayor o igual al ancho útil; puedes guardar borrador")
        }
        if(format==SicFieldFormat.SIC18A && sourceRecordId.isNullOrBlank()) issue("Alcantarilla","Selecciona una alcantarilla SIC-18")
        return CompletionResult(issues)
    }
    fun exportValues():List<Any?> {
        require(completion().canFinalize) {completion().issues.joinToString("\n") {"${it.field}: ${it.reason}"}}
        return format.fields.map { f -> val v=text(f.key).trim();when {
            v.isBlank() -> null
            f.key in setOf("startPrCode","endPrCode") -> v.padStart(4,'0')
            f.kind==CellKind.DATE -> LocalDate.parse(v,SicFieldFormat.dateFormatter)
            f.kind==CellKind.DECIMAL -> number(v)
            f.kind==CellKind.INTEGER -> v.toInt()
            else -> v
        } }
    }
    companion object {
        fun number(v:String):Double? {
            val text=v.trim()
            if(!text.matches(Regex("[+-]?(?:[0-9]+(?:[.,][0-9]+)?|[.,][0-9]+)(?:[eE][+-]?[0-9]+)?"))) return null
            return text.replace(',','.').toDoubleOrNull()?.takeIf {it.isFinite()}
        }
    }
}
