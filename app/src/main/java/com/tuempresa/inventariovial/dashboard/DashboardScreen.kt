package com.tuempresa.inventariovial.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.R
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.road.RoadPr
import java.time.*
import java.time.format.DateTimeFormatter

val SessionGreen=Color(0xFF176B45)
private val ToolBlue=Color(0xFF215A86)
private val PendingAmber=Color(0xFF885400)
private fun glyph(name:String,path:String)=ImageVector.Builder(name,24.dp,24.dp,24f,24f).addPath(
    pathData=PathParser().parsePathString(path).toNodes(),fill=SolidColor(Color.Black)).build()
private val Add=glyph("Nuevo","M19,13H13V19H11V13H5V11H11V5H13V11H19Z")
private val Records=glyph("Registros","M3,5H5V7H3Z M7,5H21V7H7Z M3,11H5V13H3Z M7,11H21V13H7Z M3,17H5V19H3Z M7,17H21V19H7Z")
private val Pavement=glyph("Pavimentos","M4,3H9L7,21H2Z M15,3H20L22,21H17Z M11,3H13V7H11Z M11,10H13V14H11Z M11,17H13V21H11Z")
private val Drive=glyph("Drive","M19.35,10.04C18.67,6.59 15.64,4 12,4C9.11,4 6.6,5.64 5.35,8.04C2.34,8.36 0,10.91 0,14C0,17.31 2.69,20 6,20H19C21.76,20 24,17.76 24,15C24,12.36 21.95,10.22 19.35,10.04Z")
private val Map=glyph("Mapa","M3,5L9,3L15,5L21,3V19L15,21L9,19L3,21Z M9,5V17 M15,7V19")

@Composable fun DashboardScreen(session:FieldSession?,sessions:List<FieldSession>,drafts:List<InventoryRecordEntity>,
    prs:List<RoadPr>,counts:DashboardDeliveryCounts,connected:Boolean,today:LocalDate,
    onSession:()->Unit,onNew:()->Unit,onRecords:()->Unit,onPavement:()->Unit,onDrive:()->Unit,
    onTools:()->Unit,onAllDrafts:()->Unit,onContinue:(InventoryRecordEntity)->Unit,onMap:()->Unit={}) {
    val daily=DashboardDrafts.order(DashboardDrafts.filter(drafts,sessions,today),sessions,prs)
    LazyColumn(Modifier.fillMaxSize().background(RutaClaraBackground).testTag("dashboard"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item {
            RutaClaraDashboardHeader(session?.let {"${it.project} · ${it.operator}"} ?: "Sin sesión de campo activa",
                if(connected) "Con conexión · ${counts.pending+counts.updates+counts.errors} entregas pendientes" else "Sin Internet · trabajo guardado en tablet")
        }
        item {
            BoxWithConstraints {
                if(maxWidth>=960.dp && LocalDensity.current.fontScale<1.6f) {
                    Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                        DashboardSessionCard(session,onSession,Modifier.weight(0.8f))
                        DashboardActions(onNew,onRecords,onPavement,onDrive,onMap,Modifier.weight(1.2f))
                    }
                } else Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
                    DashboardSessionCard(session,onSession)
                    DashboardActions(onNew,onRecords,onPavement,onDrive,onMap)
                }
            }
        }
        item {
            DraftSection(daily,sessions,onContinue,onAllDrafts)
        }
        item {
            Card(colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp).testTag("drive-status-summary"),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("Entregas Drive",style=MaterialTheme.typography.titleMedium,color=RutaClaraBlue)
                    HorizontalDivider(color=Color(0xFFE0E6EE))
                    Text("Sincronizados: ${counts.synced}",color=SessionGreen)
                    Text("Pendientes: ${counts.pending} · Actualizaciones pendientes: ${counts.updates}",color=PendingAmber)
                    Text("Errores: ${counts.errors}",color=if(counts.errors>0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Son entregas programadas, no archivos. Los borradores sin finalizar se muestran por separado.",style=MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {OutlinedButton(onClick=onTools,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp).testTag("tools"),
            colors=ButtonDefaults.outlinedButtonColors(containerColor=Color.White,contentColor=RutaClaraBlue)) {Text("HERRAMIENTAS Y AJUSTES")}}
    }
}

@Composable
private fun DashboardSessionCard(session:FieldSession?,onSession:()->Unit,modifier:Modifier=Modifier) {
    Card(modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(20.dp)) {
        Box(Modifier.fillMaxWidth()) {
            Image(painterResource(R.drawable.gbo_dashboard_banner),null,Modifier.matchParentSize(),contentScale=ContentScale.Crop)
            Text("SESIÓN DE CAMPO",Modifier.padding(20.dp),style=MaterialTheme.typography.titleMedium,color=RutaClaraBlue)
        }
        Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            session?.let {
                Text("${it.road ?: "Ruta por elemento"} · ${it.roadbed ?: "Calzada por elemento"}",style=MaterialTheme.typography.titleMedium)
                Text("${Instant.ofEpochMilli(it.startTime).atZone(ZoneId.systemDefault()).toLocalDate()} · ${if(it.direction=="DECREASING") "Sentido decreciente ↓" else "Sentido creciente ↑"}")
            } ?: Text("Define el contexto de tu jornada.")
            Button(onClick=onSession,colors=ButtonDefaults.buttonColors(containerColor=SessionGreen,contentColor=Color.White),
                modifier=Modifier.fillMaxWidth().heightIn(min=56.dp).testTag("session-action")) {
                Text(if(session==null) "INICIAR SESIÓN" else "CONFIGURAR SESIÓN")
            }
        }
    }
}

@Composable
private fun DashboardActions(onNew:()->Unit,onRecords:()->Unit,onPavement:()->Unit,onDrive:()->Unit,onMap:()->Unit,modifier:Modifier=Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columns=if(maxWidth>=420.dp && LocalDensity.current.fontScale<1.6f) 2 else 1
        val actions=listOf(Triple("NUEVO REGISTRO",Add,onNew),Triple("MIS REGISTROS",Records,onRecords),
            Triple("PAVIMENTOS",Pavement,onPavement),Triple("ENTREGAS DRIVE",Drive,onDrive),Triple("MAPA",Map,onMap))
        Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            actions.chunked(columns).forEach {row->
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {row.forEach {(label,icon,action)->
                    val primary=label=="NUEVO REGISTRO"
                    OutlinedButton(onClick=action,modifier=Modifier.weight(1f).heightIn(min=72.dp).testTag(label),
                        shape=RoundedCornerShape(16.dp),contentPadding=PaddingValues(horizontal=16.dp,vertical=18.dp),
                        colors=ButtonDefaults.outlinedButtonColors(containerColor=if(primary) RutaClaraBlue else Color.White,
                            contentColor=if(primary) Color.White else RutaClaraBlue)) {
                        Icon(icon,null,Modifier.size(24.dp));Spacer(Modifier.width(10.dp));Text(label,Modifier.weight(1f))
                    }
                }}
            }
        }
    }
}

@Composable fun DraftSection(drafts:List<InventoryRecordEntity>,sessions:List<FieldSession>,onContinue:(InventoryRecordEntity)->Unit,onAll:()->Unit) {
    Card(colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("BORRADORES POR COMPLETAR",style=MaterialTheme.typography.titleMedium,color=PendingAmber)
            Text("${drafts.size} borradores del día",style=MaterialTheme.typography.bodyMedium)
            if(drafts.isEmpty()) Text("No hay borradores para hoy.") else LazyColumn(
                Modifier.fillMaxWidth().heightIn(max=360.dp).testTag("daily-drafts"),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                items(drafts,key={it.id}) {DraftRow(it,sessions,onContinue)}
            }
            TextButton(onClick=onAll,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("all-drafts")) {Text("VER TODOS LOS BORRADORES")}
        }
    }
}

@Composable fun DraftRow(record:InventoryRecordEntity,sessions:List<FieldSession>,onContinue:(InventoryRecordEntity)->Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical=4.dp)) {
        Text("${DashboardDrafts.title(record)} · ${record.routeCode}",style=MaterialTheme.typography.titleSmall)
        Text(DashboardDrafts.progressive(record),style=MaterialTheme.typography.bodyMedium)
        Text(DashboardDrafts.context(record,sessions),style=MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text("Editado ${Instant.ofEpochMilli(record.updatedAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))}",
                modifier=Modifier.weight(1f).padding(top=14.dp),style=MaterialTheme.typography.bodySmall)
            TextButton(onClick={onContinue(record)},modifier=Modifier.heightIn(min=48.dp).testTag("continue-${record.id}")) {Text("CONTINUAR")}
        }
        HorizontalDivider()
    }
}

@Composable fun AllDraftsScreen(drafts:List<InventoryRecordEntity>,sessions:List<FieldSession>,prs:List<RoadPr>,
    onContinue:(InventoryRecordEntity)->Unit,onBack:()->Unit) {
    var date by remember {mutableStateOf("")};var route by remember {mutableStateOf("")}
    var bed by remember {mutableStateOf("")};var type by remember {mutableStateOf("")}
    val day=runCatching {LocalDate.parse(date)}.getOrNull()
    val invalid=date.isNotBlank() && day==null
    val rows=DashboardDrafts.order(DashboardDrafts.filter(drafts,sessions,day,route,bed,type),sessions,prs)
    BackHandler(onBack=onBack)
    LazyColumn(Modifier.fillMaxSize().background(Color.White).testTag("all-drafts-screen"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        item {TextButton(onClick=onBack) {Text("VOLVER")};RutaClaraSectionHeader("Todos los borradores",R.drawable.gbo_dashboard_banner)}
        item {OutlinedTextField(date,{date=it},label={Text("Fecha de sesión (AAAA-MM-DD) — vacío: todas")},isError=invalid,
            supportingText={if(invalid)Text("Fecha inválida; usa AAAA-MM-DD.")},modifier=Modifier.fillMaxWidth().testTag("filter-date"))}
        item {OutlinedTextField(route,{route=it},label={Text("Ruta — vacío: todas")},modifier=Modifier.fillMaxWidth().testTag("filter-route"))}
        item {OutlinedTextField(bed,{bed=it},label={Text("Calzada — vacío: todas")},modifier=Modifier.fillMaxWidth().testTag("filter-bed"))}
        item {OutlinedTextField(type,{type=it},label={Text("Buscar tipo de elemento")},modifier=Modifier.fillMaxWidth().testTag("filter-type"))}
        item {Text("${if(invalid) 0 else rows.size} borradores · orden por progresiva y sentido de sesión")}
        if(!invalid) items(rows,key={it.id}) {record->
            Text(DashboardDrafts.date(record,sessions).toString(),style=MaterialTheme.typography.labelMedium)
            DraftRow(record,sessions,onContinue)
        }
    }
}
