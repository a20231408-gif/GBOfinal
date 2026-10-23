package com.tuempresa.inventariovial.core

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date

/** Switching, searching and reopening these sections have no enqueue or persistence effects. */
@Composable fun DriveDeliveryLists(rows:List<DriveDeliveryEntry>,onRetry:(DriveSyncIntent)->Unit) {
    var section by rememberSaveable{mutableStateOf(DriveDeliverySection.QUEUE)}
    var query by rememberSaveable{mutableStateOf("")}
    var filter by rememberSaveable{mutableStateOf(DriveDeliveryFilter.ALL)}
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        for(s in DriveDeliverySection.entries)FilterChip(selected=section==s,onClick={section=s},
            modifier=Modifier.testTag("drive-section-${s.name}"),label={Text(s.label)})
    }
    if(section==DriveDeliverySection.HISTORY) {
        OutlinedTextField(value=query,onValueChange={query=it},label={Text("Buscar nombre, ruta o progresiva")},
            singleLine=true,modifier=Modifier.fillMaxWidth().testTag("drive-history-search"))
        FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            for(f in DriveDeliveryFilter.entries)FilterChip(selected=filter==f,onClick={filter=f},
                modifier=Modifier.testTag("drive-history-filter-${f.name}"),label={Text(f.label)})
        }
    }
    val visible=DriveDeliveryListPresentation.select(rows,section,query,filter)
    Text(if(section==DriveDeliverySection.QUEUE)"${visible.size} entregas por completar"else"${visible.size} de ${rows.size} entregas",style=MaterialTheme.typography.bodySmall)
    if(visible.isEmpty())Text(when {
        section==DriveDeliverySection.QUEUE->DriveDeliveryListPresentation.EMPTY_QUEUE
        rows.isEmpty()->"Todavía no hay entregas registradas."
        else->"No hay entregas que coincidan con la búsqueda o el filtro."
    },modifier=Modifier.testTag("drive-list-empty"))
    for(row in visible)key(row.intent.key) {
        // Persisted revision/ACK times only; do not invent a date for legacy counters.
        if(section==DriveDeliverySection.HISTORY&&row.activityAt>=946684800000L)
            Text("Última actividad: ${DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(Date(row.activityAt))}",style=MaterialTheme.typography.bodySmall)
        DriveDeliveryCard(row.intent,row.identity,row.view,row.progress){onRetry(row.intent)}
    }
}
