package com.tuempresa.inventariovial.field

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.tuempresa.inventariovial.road.ProjectRouteCatalog

@Composable
fun ProjectRouteFields(route: String) {
    val context = LocalContext.current
    val catalog = remember(context) { ProjectRouteCatalog.load(context) }
    val selected = catalog.find(route)
    Text(selected?.let { "${it.itemLabel} · ${it.segmentCode}" } ?: "Ítem y tramo: pendientes de seleccionar ruta.")
}
