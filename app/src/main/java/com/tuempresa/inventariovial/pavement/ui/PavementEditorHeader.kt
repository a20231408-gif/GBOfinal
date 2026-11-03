package com.tuempresa.inventariovial.pavement.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.pavement.PavementSampleEntity
import com.tuempresa.inventariovial.pavement.PavementSurface

/** Navigation, title and actions never compete for the same narrow phone column. */
@Composable internal fun PavementEditorHeader(sample:PavementSampleEntity,editable:Boolean,canReopen:Boolean,
    onBack:()->Unit,onReview:()->Unit,onReopen:()->Unit) {
    Surface(color=MaterialTheme.colorScheme.primary,contentColor=MaterialTheme.colorScheme.onPrimary,
        shape=androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp).testTag("pavement-editor-header"),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                TextButton(onClick=onBack,modifier=Modifier.width(48.dp),contentPadding=PaddingValues(0.dp),
                    colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.onPrimary)) {Text("←")}
                Text("Fallas en pavimentos",Modifier.weight(1f).testTag("pavement-editor-title"),style=MaterialTheme.typography.titleLarge)
            }
            TextButton(onClick=onBack,colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.onPrimary)) {Text("Volver a Muestras")}
            Text("${PavementSurface.valueOf(sample.surface).label} · ${sample.tramo} · muestra ${sample.muestra}",
                Modifier.fillMaxWidth().testTag("pavement-editor-context"),style=MaterialTheme.typography.bodyMedium)
            if(editable)Button(onClick=onReview,modifier=Modifier.fillMaxWidth().testTag("pavement-review"),
                colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.colorScheme.surface,contentColor=MaterialTheme.colorScheme.primary)){Text("Revisar y finalizar")}
            else if(canReopen)OutlinedButton(onClick=onReopen,modifier=Modifier.fillMaxWidth(),
                colors=ButtonDefaults.outlinedButtonColors(contentColor=MaterialTheme.colorScheme.onPrimary)){Text("Reabrir para corregir")}
        }
    }
}
