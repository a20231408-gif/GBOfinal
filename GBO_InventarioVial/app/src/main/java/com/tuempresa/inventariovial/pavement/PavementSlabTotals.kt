package com.tuempresa.inventariovial.pavement

/** Manual pp.136–190: count a slab once at its highest observed severity for a given code. */
object PavementSlabTotals {
    fun records(damages: List<PavementDamageEntity>): List<PavementDamageEntity> {
        val divided = damages.filter { it.code == 23 && PavementSeverity.rank(it.severity) >= 2 }.flatMap { PavementRules.slabIds(it) }.toSet()
        val global = damages.filter { it.geometry != "SLABS" }.map { it.copy(severity=PavementSeverity.canonical(it.severity)) }
        val expanded = damages.filter { it.geometry == "SLABS" }.flatMap { d ->
            PavementRules.slabIds(d).filter { d.code == 23 || it !in divided }.map { d.copy(slabs=it.toString(),severity=PavementSeverity.canonical(d.severity)) }
        }
        return global + expanded.groupBy { it.code to it.slabs }.values.map { rows ->
            val worst = rows.maxBy { PavementSeverity.rank(it.severity) }
            // p.159: two medium linear cracks in one slab are recorded as high severity.
            if (worst.code == 28 && rows.count { it.severity == "M" } >= 2) worst.copy(severity="H") else worst
        }
    }
}
