package com.tuempresa.inventariovial.road

import android.content.Context
import org.json.JSONObject

data class ProjectRoute(val routeCode: String, val itemCode: String, val segmentCode: String) {
    val itemLabel get() = "Ítem $itemCode"
}

/** Fixed project mapping supplied with the six engineering axes. */
class ProjectRouteCatalog private constructor(val routes: List<ProjectRoute>) {
    val routeCodes get() = routes.map { it.routeCode }
    fun find(route: String?) = routes.singleOrNull { it.routeCode == normalizedRoadCode(route.orEmpty()) }
    fun requireRoute(route: String) = requireNotNull(find(route)) { "Selecciona una ruta del proyecto." }
    fun segment(route: String?, historical: String? = null): String = find(route)?.segmentCode ?: historical.orEmpty()
    companion object {
        fun load(context: Context) = parse(context.assets.open("project_routes.json").bufferedReader().use { it.readText() })
        fun parse(text: String): ProjectRouteCatalog {
            val array = JSONObject(text).getJSONArray("routes")
            val rows = (0 until array.length()).map { i -> array.getJSONObject(i).let {
                ProjectRoute(it.getString("routeCode"), it.getString("itemCode"), it.getString("segmentCode"))
            } }
            require(rows.size == 6 && rows.map { it.routeCode }.distinct().size == 6)
            require(rows.map { it.itemCode to it.segmentCode }.distinct().size == 6)
            return ProjectRouteCatalog(rows)
        }
    }
}
