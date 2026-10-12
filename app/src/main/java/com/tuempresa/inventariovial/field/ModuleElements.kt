package com.tuempresa.inventariovial.field

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.RecordDetailPresentation
import com.tuempresa.inventariovial.RecordDetailScreen
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.InventoryRecordEntity
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel

/** Display numbers are allocated once per UUID, independently of GPS/list position. */
class ElementNumbers(context:Context) {
    private val prefs=context.getSharedPreferences("element_numbers_9",Context.MODE_PRIVATE)
    fun assign(records:List<InventoryRecordEntity>):Map<String,Int> = synchronized(lock) {
        val result=mutableMapOf<String,Int>();val counters=mutableMapOf<String,Int>();val edit=prefs.edit()
        records.sortedWith(compareBy({it.createdAt},{it.id})).forEach {r->
            val scope="${r.routeCode}/${r.sicCode}"
            val key="id/${r.id}"
            val next=counters.getOrPut(scope){prefs.getInt("next/$scope",0)}
            val number=prefs.getInt(key,0).takeIf{it>0} ?: (next+1).also {edit.putInt(key,it)}
            counters[scope]=maxOf(next,number);result[r.id]=number
        }
        counters.forEach {(scope,n)->edit.putInt("next/$scope",n)}
        check(edit.commit());result
    }
    companion object {private val lock=Any()}
}

@Composable fun ModuleElements(vm:InventoryViewModel,title:String,filter:(InventoryRecordEntity)->Boolean,onBack:()->Unit,familyFilter:Set<com.tuempresa.inventariovial.road.AssetFamily> = emptySet(),newForm:@Composable (()->Unit)->Unit) {
    val context=LocalContext.current;val db=remember(context){InventoryDatabase.getInstance(context)}
    val history by db.inventoryDao().observeHistory().collectAsState(emptyList())
    val records=history.map {it.record}
    val families=history.associate {it.record.id to com.tuempresa.inventariovial.road.AssetPresentationCatalog.resolve(it)}
    val sync by db.automaticSyncDao().observe().collectAsState(emptyList())
    var creating by rememberSaveable(title){mutableStateOf(false)}
    var selected by rememberSaveable(title){mutableStateOf<String?>(null)}
    var destination by rememberSaveable(title){mutableStateOf("DETAIL")}
    if(creating) {newForm {creating=false};return}
    selected?.let {id->RecordDetailScreen(vm,id,destination){selected=null};return}
    BackHandler(onBack=onBack)
    val numbers=remember(records){ElementNumbers(context).assign(records)}
    var orderRevision by remember {mutableStateOf(0)}
    val local=remember(context,orderRevision){com.tuempresa.inventariovial.road.LocalVisualOrder(context)}
    val visual=remember(records,local){records.associate {it.id to local.position(it)}}
    val visible=records.filter {filter(it) || families[it.id] in familyFilter}.sortedWith(compareBy<InventoryRecordEntity>({it.routeCode},{visual[it.id]?.meters ?: Double.MAX_VALUE},{com.tuempresa.inventariovial.road.InventoryTechnicalOrder.signed(it)},{it.createdAt},{it.id}))
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {OutlinedButton(onClick=onBack){Text("Volver")};Text(title,style=MaterialTheme.typography.headlineMedium);Button(onClick={creating=true}){Text("Nuevo elemento")}}
        if(visible.isEmpty())item {Text("Todavía no hay elementos guardados")}
        items(visible,key={it.id}) {r->
            Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("${families[r.id]?.let {com.tuempresa.inventariovial.core.DriveInventorySchema.elementLabel(it)} ?: RecordDetailPresentation.title(r)} ${numbers[r.id]}",style=MaterialTheme.typography.titleLarge)
                Text("${r.sicCode} · ${r.assetType} · ${r.routeCode}")
                com.tuempresa.inventariovial.core.DriveInventorySchema.receipt(context,r.id)?.optString("logicalPath")?.let {Text("Identidad Drive: $it")}
                Text(com.tuempresa.inventariovial.dashboard.DashboardDrafts.progressive(r))
                Text("Orden local: ${visual[r.id]?.meters?.let {"%.1f m".format(it)} ?: "sin posición"} · ${visual[r.id]?.warning}")
                com.tuempresa.inventariovial.road.ManualVisualPosition(r){orderRevision++}
                Text("${when(r.status){"DRAFT"->"Borrador";"ACTIVE"->"Finalizado";"ANNULLED"->"Anulado";else->r.status}} · ${RecordDetailPresentation.status(sync.find {it.key=="SIC/${r.id}"},r.status=="ACTIVE")}")
                var summary by remember(r.id,r.updatedAt){mutableStateOf<List<Pair<String,String>>>(emptyList())}
                LaunchedEffect(r.id,r.updatedAt) {db.inventoryDao().snapshot(r.id)?.let {summary=RecordDetailPresentation.summary(it)}}
                summary.filter {it.first.startsWith("Condición") || it.first.startsWith("Tipo")}.forEach {Text("${it.first}: ${it.second}")}
                ElementActions {page->destination=page;selected=r.id}
            }}
        }
    }
}

@Composable fun ElementActions(onAction:(String)->Unit) {
    listOf("Detalle" to "DETAIL","Editar" to "TECHNICAL","Fotografías" to "PHOTOS","Revisar y finalizar" to "REVIEW").chunked(2).forEach {pair->
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {pair.forEach {(label,page)->OutlinedButton(onClick={onAction(page)},modifier=Modifier.weight(1f).heightIn(min=56.dp)){Text(label)}}}
    }
}
