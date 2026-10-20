package com.tuempresa.inventariovial.field

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Only used to initialize a new dialog; never rewrites a persisted session. */
internal fun defaultSessionProjectName(day: LocalDate): String =
    "Inventario Vial - ${day.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT))}"
