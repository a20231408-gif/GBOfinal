package com.tuempresa.inventariovial.pavement

import com.tuempresa.inventariovial.catalog.SicCatalogRepository
import java.time.LocalDate

/** Numeric chainage is independent of the contractual PR code and distance. */
object PavementChainage {
    fun display(text: String) = runCatching { format(parse(text)) }.getOrDefault(text)
    fun parse(text: String): Double {
        val parts = text.trim().replace(',', '.').split('+')
        require(parts.size in 1..2) { "Progresiva: usa 00+000 o metros." }
        val metres = if (parts.size == 1) parts[0].toDouble() else {
            val km = parts[0].toInt()
            val remainder = parts[1].toDouble()
            require(km >= 0 && remainder in 0.0..<1000.0) { "Progresiva inválida." }
            km * 1000.0 + remainder
        }
        require(metres.isFinite() && metres >= 0) { "Progresiva inválida." }
        return metres
    }
    fun format(metres: Double): String {
        require(metres.isFinite() && metres >= 0)
        // Round to millimetres before splitting, so 999.9999 carries into the next km.
        val mm = kotlin.math.round(metres * 1000).toLong()
        val km = mm / 1_000_000
        val remainder = mm % 1_000_000
        val whole = (remainder / 1000).toString().padStart(3, '0')
        val fraction = (remainder % 1000).toString().padStart(3, '0').trimEnd('0')
        return km.toString().padStart(2, '0') + "+" + whole + if (fraction.isEmpty()) "" else ".$fraction"
    }
}

object PavementHeader {
    val lanes = listOf("Derecho", "Izquierdo")
    val roadbeds get() = SicCatalogRepository.roadbedCodes
    fun lane(value: String) = when (value) { "D" -> "Derecho"; "I" -> "Izquierdo"; else -> value }
    fun slabWidth(s: PavementSampleEntity) = s.slabWidthM ?: if (s.slabColumns > 0) s.anchoM / s.slabColumns else 0.0
    fun slabLength(s: PavementSampleEntity) = s.slabLengthM ?: if (s.slabRows > 0) s.largoM / s.slabRows else 0.0
    fun totalSlabs(s: PavementSampleEntity) = s.slabRows * s.slabColumns
    fun scope(a: PavementSampleEntity, b: PavementSampleEntity) =
        a.ruta == b.ruta && a.tramo == b.tramo && a.calzada == b.calzada && a.surface == b.surface
    fun duplicate(s: PavementSampleEntity, samples: List<PavementSampleEntity>) =
        samples.any { it.id != s.id && scope(it, s) && it.muestra == s.muestra }
    fun nextNumber(s: PavementSampleEntity, samples: List<PavementSampleEntity>) =
        (samples.filter { it.id != s.id && scope(it, s) }.maxOfOrNull { it.muestra } ?: 0) + 1

    fun newSample(id: String, surface: PavementSurface, userId: String, inspector: String,
                  samples: List<PavementSampleEntity>, date: LocalDate = LocalDate.now(),
                  fieldSession: com.tuempresa.inventariovial.data.entity.FieldSession? = null): PavementSampleEntity {
        val defaults = com.tuempresa.inventariovial.field.FieldSessionDefaults.location(fieldSession)
        val previous = samples.filter { it.surface == surface.name && it.inspectorUserId == userId && it.anchoM > 0 && it.largoM > 0 &&
            (defaults.route.isBlank() || it.ruta == defaults.route) && (defaults.roadbed.isBlank() || it.calzada == defaults.roadbed) }
            .maxByOrNull { it.createdAt }
        val initial = previous?.prFinal?.let { runCatching { PavementChainage.parse(it) }.getOrNull() } ?: 0.0
        val draft = PavementSampleEntity(id, defaults.segment.ifBlank { previous?.tramo.orEmpty() }, defaults.route.ifBlank { previous?.ruta.orEmpty() }, 1,
            lane(previous?.carril.orEmpty()).ifBlank { lanes.first() }, defaults.roadbed.ifBlank { previous?.calzada.orEmpty() },
            PavementChainage.format(initial), PavementChainage.format(initial + (previous?.largoM ?: 0.0)),
            previous?.anchoM ?: 0.0, previous?.largoM ?: 0.0, surface.name, date.toString(), userId, inspector,
            slabRows = previous?.slabRows ?: 0, slabColumns = previous?.slabColumns ?: 0,
            laneWidthM = previous?.laneWidthM, slabWidthM = previous?.let { slabWidth(it) }, slabLengthM = previous?.let { slabLength(it) })
        return draft.copy(muestra = nextNumber(draft, samples))
    }
    fun normalize(s: PavementSampleEntity): PavementSampleEntity {
        val rigid = s.surface == "RIGID"
        val width = if (rigid) s.slabColumns * slabWidth(s) else s.anchoM
        val length = if (rigid) s.slabRows * slabLength(s) else s.largoM
        require(width.isFinite() && length.isFinite() && width > 0 && length > 0) { "Dimensiones positivas." }
        if (rigid) require(s.slabRows > 0 && s.slabColumns > 0 && s.slabRows.toLong() * s.slabColumns <= 1000) { "Define filas y columnas (máximo 1000 losas)." }
        val initial = PavementChainage.parse(s.prInicial)
        return s.copy(carril = lane(s.carril), prInicial = PavementChainage.format(initial),
            prFinal = PavementChainage.format(initial + length), anchoM = width, largoM = length,
            slabWidthM = if (rigid) slabWidth(s) else null, slabLengthM = if (rigid) slabLength(s) else null)
    }
}

/** Canonical storage L/M/H; legacy B/A is accepted on read and at repository boundaries. */
object PavementSeverity {
    fun canonical(value: String?): String? = when (value) { "B" -> "L"; "A" -> "H"; else -> value }
    fun legacy(value: String?): String? = when (canonical(value)) { "L" -> "B"; "H" -> "A"; else -> value }
    fun label(value: String?): String = when (canonical(value)) { "L" -> "L · Bajo"; "M" -> "M · Medio"; "H" -> "H · Alto"; else -> "N/A" }
    fun rank(value: String?) = when (canonical(value)) { "H" -> 3; "M" -> 2; "L" -> 1; else -> 0 }
    fun color(value: String?) = when (canonical(value)) { "L" -> 0xFFFFC107.toInt(); "M" -> 0xFFFF8C00.toInt(); "H" -> 0xFFD32F2F.toInt(); else -> 0xFF455A64.toInt() }
}
