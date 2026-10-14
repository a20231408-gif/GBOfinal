package com.tuempresa.inventariovial.pavement

object PavementRules {
    fun slabIds(d: PavementDamageEntity) = d.slabs.split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()
    fun quantity(d: PavementDamageEntity): Double = when (d.geometry) {
        "LINEAR" -> d.lengthM ?: 0.0
        "AREA" -> d.areaM2 ?: 0.0
        "POTHOLE" -> if(isLargePothole(d)) d.areaM2?.div(if(d.code==13)0.47 else 0.65) ?: 0.0 else 1.0
        "SLABS" -> slabIds(d).size.toDouble()
        else -> 0.0
    }
    fun isLargePothole(d: PavementDamageEntity) = (d.code==13 && (d.diameterM?:0.0)>.75) || (d.code==85 && (d.diameterM?:0.0)>.9)
    fun validate(s: PavementSampleEntity, d: PavementDamageEntity, existing: List<PavementDamageEntity>) {
        require(s.anchoM > 0 && s.largoM > 0) { "Guarda las dimensiones de muestra." }
        val definition = PavementCatalog.forSurface(PavementSurface.valueOf(s.surface)).firstOrNull { it.code == d.code }
        require(definition != null && d.surface == s.surface && d.geometry == definition.geometry.name) { "Daño incompatible con la superficie." }
        require(if (definition.severities.isEmpty()) d.severity == null else PavementSeverity.canonical(d.severity) in definition.severities) { "Selecciona la severidad." }
        fun point(x: Double?, y: Double?) {
            require(x != null && y != null && x.isFinite() && y.isFinite() && x in 0.0..s.anchoM && y in 0.0..s.largoM) { "Coordenadas fuera de la muestra." }
        }
        when (d.geometry) {
            "AREA", "LINEAR" -> { point(d.x1, d.y1); point(d.x2, d.y2); require(quantity(d).isFinite() && quantity(d) > 0) { "La medida debe ser positiva." } }
            "POTHOLE" -> {
                point(d.x1, d.y1); require(d.diameterM != null && d.diameterM.isFinite() && d.diameterM > 0) { "Ingresa el diámetro medido." }
                if(isLargePothole(d)) {
                    require(d.areaM2!=null && d.areaM2.isFinite() && d.areaM2>0) { "Hueco grande: ingresa el área medida para calcular huecos equivalentes (manual pp.108/233)." }
                    if(d.code==13)require(PavementSeverity.canonical(d.severity) in listOf("M","H")) { "Hueco mayor de 0.75 m: gravedad M/H según profundidad (p.108)." }
                }
            }
            "SLABS" -> require(d.slabs.split(',').all { it.trim().toIntOrNull() != null } && slabIds(d).isNotEmpty() && slabIds(d).all { it in 1..(s.slabRows * s.slabColumns) }) { "Selecciona losas válidas." }
            "GLOBAL" -> require(existing.none { it.id != d.id && it.code == d.code }) { "Este daño global ya fue registrado; modifícalo." }
        }
        PavementSlabGeometry.validate(s,d)
        if(PavementDamageFields.crackWidth(d.code))require(d.crackWidthMm == null || (d.crackWidthMm.isFinite() && d.crackWidthMm >= 0)) { "Ancho de fisura inválido." }
        if(PavementDamageFields.depth(d.code))require(d.depthMm == null || (d.depthMm.isFinite() && d.depthMm > 0)) { "Profundidad inválida." }
        if (d.geometry == "AREA") require(existing.filter { it.id != d.id && it.code == d.code }.sumOf { quantity(it) } + quantity(d) <= s.anchoM * s.largoM + 1e-7) { "El total de este daño supera el área de muestra." }
        if (d.code == 82) require(existing.filter { it.id != d.id && it.code == 82 }.sumOf { quantity(it) } + quantity(d) <= 2 * s.largoM + 1e-7) { "Drenaje lateral: máximo 2 × largo." }
        if (d.code == 81) require(existing.filter { it.id != d.id && it.code == 81 }.sumOf { quantity(it) } + quantity(d) <= s.largoM + 1e-7) { "Sección transversal: máximo largo de unidad (p.225)." }
    }
    fun warnings(s: PavementSampleEntity, damages: List<PavementDamageEntity>): List<String> = buildList {
        if(damages.any { isLargePothole(it) && (it.areaM2==null || it.areaM2<=0) }) add("Huecos grandes históricos: falta área medida para cantidad equivalente (pp.108/233).")
        if (s.surface != "RIGID" && s.anchoM * s.largoM !in 140.0..320.0) add("Área fuera de 140–320 m² (advertencia).")
        if (s.surface == "RIGID") {
            if (s.slabRows * s.slabColumns !in 12..28) add("Número de losas fuera de 20 ± 8.")
            if (s.slabRows > 0 && s.largoM / s.slabRows >= 8) add("Longitud de losa debe ser menor que 8 m.")
        }
        fun overlap(a: PavementDamageEntity, b: PavementDamageEntity): Boolean {
            val ax = listOfNotNull(a.x1, a.x2); val ay = listOfNotNull(a.y1, a.y2)
            val bx = listOfNotNull(b.x1, b.x2); val by = listOfNotNull(b.y1, b.y2)
            return ax.size == 2 && ay.size == 2 && bx.size == 2 && by.size == 2 && ax.min() < bx.max() && bx.min() < ax.max() && ay.min() < by.max() && by.min() < ay.max()
        }
        for ((a, b) in listOf(2 to 12, 19 to 20)) if (damages.filter { it.code == a }.any { x -> damages.any { it.code == b && overlap(x,it) } }) add("Exclusión: daños $a y $b se superponen; revisa el registro.")
        if (damages.any { it.code == 23 && PavementSeverity.canonical(it.severity) in listOf("M", "H") && damages.any { other -> other.id != it.id && slabIds(it).intersect(slabIds(other)).isNotEmpty() } }) add("Losa dividida M/H excluye otros daños en la misma losa (p.140).")
    }
    /** Cuadros 5-6 (p.108), 5-16 (p.233). No inferir límites ambiguos ni rangos ausentes. */
    fun potholeSeverity(code:Int,diameterM:Double,depthMm:Double):String? {
        if(!diameterM.isFinite() || !depthMm.isFinite() || diameterM<=0 || depthMm<=0) return null
        if(code==13 && diameterM>0.75) return if(depthMm<=25)"M" else "A"
        if(depthMm<13) return null
        if(code==13){
            if(diameterM>0.75) return if(depthMm<=25)"M" else "A"
            if(diameterM<0.1 || diameterM==0.2 || diameterM==0.45) return null
            val col=if(diameterM<0.2)0 else if(diameterM<0.45)1 else 2
            val row=if(depthMm<=25)0 else if(depthMm<=50)1 else 2
            return listOf(listOf("B","B","M"),listOf("B","M","A"),listOf("M","M","A"))[row][col]
        }
        if(code==85){
            if(depthMm>50 && depthMm<51 || diameterM==0.6) return null
            val col=if(diameterM<0.3)0 else if(diameterM<0.6)1 else 2
            val row=if(depthMm<=50)0 else if(depthMm<=100)1 else 2
            return listOf(listOf("B","B","M"),listOf("B","M","A"),listOf("M","A","A"))[row][col]
        }
        return null
    }
}
