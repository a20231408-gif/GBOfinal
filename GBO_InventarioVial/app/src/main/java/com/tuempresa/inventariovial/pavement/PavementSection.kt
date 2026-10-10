package com.tuempresa.inventariovial.pavement

import androidx.room.Entity

/** Manual pp.55–56, equations 4–6; p.60 applies the same procedure to URCI. */
@Entity(tableName="pavement_sections", primaryKeys=["ruta","tramo","surface"])
data class PavementSectionEntity(
    val ruta:String, val tramo:String, val surface:String,
    val areaM2:Double, val additionalSampleIds:String,
    val inspectorUserId:String
) {
    fun additionalIds()=additionalSampleIds.split(',').filter{it.isNotBlank()}.toSet()
}

data class PavementSectionResult(val index:Double?, val explanation:String)

object PavementSectionCalculation {
    fun calculate(samples:List<PavementExportSample>, section:PavementSectionEntity?):PavementSectionResult {
        if(samples.isEmpty()) return PavementSectionResult(null,"No hay muestras.")
        val key=samples.first().sample
        if(samples.any{it.sample.tramo!=key.tramo || it.sample.ruta!=key.ruta || it.sample.surface!=key.surface})
            return PavementSectionResult(null,"No mezcles rutas, tramos ni superficies.")
        if(samples.any{it.result.index==null || it.sample.status!="CLOSED"})
            return PavementSectionResult(null,"Pendiente: muestras abiertas o índices no calculables.")
        fun area(row:PavementExportSample)=row.sample.anchoM*row.sample.largoM
        if(samples.any{!area(it).isFinite() || area(it)<=0 || it.result.index!! !in 0.0..100.0})
            return PavementSectionResult(null,"Área o índice inválido.")
        fun average(rows:List<PavementExportSample>)=rows.sumOf{it.result.index!!*area(it)}/rows.sumOf{area(it)}
        if(section==null) return PavementSectionResult(average(samples),"Media de muestras exportadas (Ec.4). Configura sección para incluir muestras adicionales.")
        if(section.ruta!=key.ruta || section.tramo!=key.tramo || section.surface!=key.surface || !section.areaM2.isFinite() || section.areaM2<=0)
            return PavementSectionResult(null,"Configuración de sección inválida.")
        if(samples.sumOf{area(it)}>section.areaM2+1e-6)
            return PavementSectionResult(null,"Las áreas inspeccionadas superan el área de la sección.")
        val ids=section.additionalIds()
        if(!samples.map{it.sample.id}.containsAll(ids)) return PavementSectionResult(null,"Faltan muestras adicionales en la exportación.")
        val additional=samples.filter{it.sample.id in ids}
        val representative=samples.filter{it.sample.id !in ids}
        if(representative.isEmpty()) return PavementSectionResult(null,"Se necesita al menos una muestra representativa.")
        if(additional.isEmpty()) return PavementSectionResult(average(representative),"PCI/URCI de sección ponderado por área (Ec.4).")
        val a=additional.sumOf{area(it)}
        val result=(average(representative)*(section.areaM2-a)+average(additional)*a)/section.areaM2
        return PavementSectionResult(result,"Ec.4–6: ${representative.size} representativas, ${additional.size} adicionales; área sección ${section.areaM2} m²; área adicionales $a m².")
    }
}
