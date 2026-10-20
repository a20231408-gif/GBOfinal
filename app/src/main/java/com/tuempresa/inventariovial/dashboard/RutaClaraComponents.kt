package com.tuempresa.inventariovial.dashboard

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.R

internal val RutaClaraBlue = Color(0xFF163F7B)
internal val RutaClaraBackground = Color(0xFFF3F6FA)

/** Decoration only. All text and actions remain native, selectable by UI tests. */
@Composable
internal fun RutaClaraDashboardHeader(project: String, connection: String) {
    BoxWithConstraints(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).testTag("ruta-clara-dashboard-header")) {
        val wide = maxWidth >= 700.dp && LocalDensity.current.fontScale < 1.6f
        Image(painterResource(R.drawable.gbo_dashboard_header), null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        Row(Modifier.fillMaxWidth().heightIn(min = if (wide) 160.dp else 184.dp).padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("GBO Inventario Vial", style = MaterialTheme.typography.headlineMedium, color = RutaClaraBlue)
                Text(project, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF263C55))
                Text(connection, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF263C55))
            }
            if (wide) Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
                Image(painterResource(R.drawable.gbo_auth_logo), "GBO Ingenieros Consultores",
                    Modifier.padding(12.dp).width(144.dp).aspectRatio(1387f / 871f), contentScale = ContentScale.Fit)
            }
        }
    }
}

@Composable
internal fun RutaClaraSectionHeader(title: String, @DrawableRes image: Int, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))) {
        Image(painterResource(image), null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        Text(title, Modifier.fillMaxWidth().padding(20.dp), style = MaterialTheme.typography.headlineSmall, color = RutaClaraBlue)
    }
}

@Composable
internal fun RutaClaraMapHeader() = RutaClaraSectionHeader("Orden de elementos", R.drawable.gbo_map_header,
    Modifier.testTag("ruta-clara-map-header"))

/** Original tools are passed as content: this surface owns no navigation or authorization. */
@Composable
internal fun DashboardToolsScreen(onBack: () -> Unit, onMap: () -> Unit, onExport: () -> Unit, onBridge: () -> Unit,
    secondary: @Composable () -> Unit, core: @Composable () -> Unit, account: @Composable () -> Unit, message: String?) {
    androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxSize().background(RutaClaraBackground).testTag("dashboard-tools"),
        contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text("VOLVER") }
            RutaClaraSectionHeader("Herramientas y ajustes", R.drawable.gbo_tools_header)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onMap, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Mapa y orden de elementos") }
                    OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Exportaciones avanzadas · Excel SIC") }
                    OutlinedButton(onClick = onBridge, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("SCAP y puentes") }
                }
            }
        }
        item { ToolsCard { secondary() } }
        item { ToolsCard { core() } }
        item { ToolsCard { Text("Administración y cuenta", style = MaterialTheme.typography.titleMedium); account() } }
        item { message?.let { Text(it) } }
    }
}

@Composable
private fun ToolsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
