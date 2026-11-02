package com.tuempresa.inventariovial.localfiles

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

/** Phase A only. Confirmation text cannot enable an unvalidated destructive operation. */
@Composable fun RemovalPreviewDialog(rows:List<RemovalPreview.Row>,onClose:()->Unit) {
    var text by remember(rows){mutableStateOf("")}
    var second by remember(rows){mutableStateOf(false)}
    AlertDialog(onDismissRequest=onClose,title={Text("Revisar eliminación: ${rows.size} registros")},text={
        Column(Modifier.verticalScroll(rememberScrollState())) {
            rows.forEach {r ->
                Text(r.item.name);Text("${r.item.family} · ${r.item.route} · ${r.item.progressive}")
                Text("UUID: ${r.item.id} · revisión ${r.item.revision}")
                Text("Copias documentales indexadas: ${r.documents}; IDs remotos en ACK válido: ${r.remoteIds ?: "desconocidos"}")
                Text("${r.sync} · ACK actual válido: ${r.validCurrentAck}")
                Text("Referencias compartidas o vínculos conocidos: ${r.sharedOrLinked}. Inventario completo aún no acreditado.")
                if(r.revisionChanged)Text("La ficha cambió. Cierra y vuelve a revisar.")
                HorizontalDivider()
            }
            Text("Previsualización de solo lectura. No confundir con descarte reversible ni limpieza de copias. No confirma propiedad ni permisos de Drive.")
            OutlinedTextField(text,{text=it},label={Text("Escribe exactamente ELIMINAR")})
            if(rows.size>1)Row {Checkbox(second,{second=it});Text("Segunda confirmación administrativa solicitada; pendiente de verificación del servidor.")}
            Text(if(RemovalPreview.confirmationMatches(text,rows.size,second))"Confirmación escrita completa; ejecución aún bloqueada."else"Confirmación escrita incompleta.")
            Text(RemovalPreview.BLOCK)
        }
    },confirmButton={TextButton(enabled=false,onClick={}){Text("Eliminar definitivamente (no habilitado)")}},
        dismissButton={TextButton(onClick=onClose){Text("Conservar y volver")}})
}
