package com.tuempresa.inventariovial.field

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.ChoiceSelector

@Composable
fun SurveyHeader(preferences: SurveyPreferences, segment: String, route: String, roadbed: String, direction: String,
    onSegment: (String)->Unit, onRoute: (String)->Unit, onRoadbed: (String)->Unit, onDirection: (String)->Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("Ruta",style=MaterialTheme.typography.titleMedium)
        ChoiceSelector(preferences.projectRoutes.routeCodes,route) {
            onRoute(it)
            onSegment(preferences.projectRoutes.requireRoute(it).segmentCode)
        }
        ProjectRouteFields(route)
        Text("Calzada",style=MaterialTheme.typography.titleMedium)
        ChoiceSelector(com.tuempresa.inventariovial.catalog.SicCatalogRepository.roadbedCodes + "Otro",roadbed) {onRoadbed(if(it=="Otro") "" else it)}
        if(roadbed !in com.tuempresa.inventariovial.catalog.SicCatalogRepository.roadbedCodes) OutlinedTextField(roadbed,{onRoadbed(it.uppercase())},label={Text("Código de calzada")},modifier=Modifier.fillMaxWidth())
        Text("Sentido de las progresivas")
        ChoiceSelector(listOf("Creciente", "Decreciente"),if(direction=="DECREASING") "Decreciente" else "Creciente") {
            onDirection(if(it=="Decreciente") "DECREASING" else "INCREASING")
        }

    }
}
