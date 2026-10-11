package com.tuempresa.inventariovial.pavement.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.pavement.*

@Composable internal fun PavementDamageTable(damages: List<PavementDamageEntity>, selectedId: String?, onSelect: (String)->Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text("Daños · ${damages.size}",style=MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(6.dp)) {
            TableCell("Ítem",0.55f); TableCell("X",0.7f); TableCell("Y",0.7f); TableCell("Falla",2.2f); TableCell("Severidad",1.1f); TableCell("Cantidad",1f)
        }
        LazyColumn(Modifier.fillMaxWidth().heightIn(max=280.dp).height((damages.size*52).coerceIn(1,280).dp)) {
            itemsIndexed(damages,key={_,d->d.id}) { index,d ->
                Row(Modifier.fillMaxWidth().heightIn(min=52.dp).testTag("damage-row-${d.id}")
                    .semantics { selected=d.id==selectedId }.clickable { onSelect(d.id) }
                    .background(if(d.id==selectedId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                    .padding(6.dp)) {
                    TableCell("${d.correlativo}",0.55f); TableCell(PavementDisplay.number(d.x1),0.7f); TableCell(PavementDisplay.number(d.y1),0.7f)
                    TableCell(d.name,2.2f)
                    Text(PavementSeverity.label(d.severity),Modifier.weight(1.1f).background(androidx.compose.ui.graphics.Color(PavementSeverity.color(d.severity))).padding(3.dp),color=androidx.compose.ui.graphics.Color.Black,style=MaterialTheme.typography.bodySmall)
                    TableCell(PavementDisplay.number(PavementRules.quantity(d)),1f)
                }
                HorizontalDivider()
            }
        }
        if(damages.isEmpty()) Text("Sin daños registrados.")
    }
}
@Composable private fun RowScope.TableCell(text: String, weight: Float) {
    Text(text,Modifier.weight(weight).padding(end=3.dp),style=MaterialTheme.typography.bodySmall,maxLines=2,overflow=TextOverflow.Ellipsis)
}

@Composable internal fun PavementDamageActions(damage: PavementDamageEntity, importing: Boolean,
    onModify: (PavementDamageEntity)->Unit, onMove: (PavementDamageEntity)->Unit,
    onCamera: ()->Unit, onDelete: (PavementDamageEntity)->Unit, onImport: (PavementDamageEntity)->Unit) {
    Row { TextButton(onClick={onModify(damage)}) { Text("Modificar") }; TextButton(onClick={onDelete(damage)}) { Text("Eliminar") } }
    if(damage.geometry in listOf("AREA","LINEAR","POTHOLE")) OutlinedButton(onClick={onMove(damage)}) { Text("Mover en esquema") }

}

@Composable internal fun PavementSampleCard(sample: PavementSampleEntity,userId: String?,onOpen: (String)->Unit) {
    OutlinedCard(onClick={onOpen(sample.id)},modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("${sample.tramo.ifBlank { "Borrador" }} · muestra ${sample.muestra}")
            Text("${PavementHistory.dateLabel(sample.fecha)} · ${sample.inspectorNombre}")
            Text("Estado: ${PavementHistory.statusLabel(sample.status)}")
            com.tuempresa.inventariovial.core.AutomaticSyncPanel("PAVEMENT",sample.id,sample.status=="CLOSED",compact=true)
            TextButton(onClick={onOpen(sample.id)}) { Text(if(PavementHistory.canContinue(sample,userId)) "Continuar" else "Ver") }
        }
    }
}
