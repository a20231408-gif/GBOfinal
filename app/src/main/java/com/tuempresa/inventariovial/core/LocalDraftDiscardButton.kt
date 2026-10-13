package com.tuempresa.inventariovial.core

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.tuempresa.inventariovial.auth.AuthGraph
import kotlinx.coroutines.launch

@Composable
fun LocalDraftDiscardButton(kind:String,id:String,restore:Boolean=false,onChanged:()->Unit={},onError:(String)->Unit={}) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var confirm by remember(id,restore) {mutableStateOf(false)}
    var busy by remember(id) {mutableStateOf(false)}
    var error by remember(id) {mutableStateOf<String?>(null)}
    TextButton(enabled=!busy,onClick={confirm=true;error=null}) {Text(if(restore) "Recuperar borrador local" else "Descartar borrador local")}
    error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
    if(confirm)AlertDialog(onDismissRequest={if(!busy)confirm=false},
        title={Text(if(restore) "Recuperar borrador" else "Descartar borrador local")},
        text={Text(if(restore) "Se recuperará la ficha con los mismos UUID y fotografías, sin enviar nada a Drive." else "Se ocultará del listado activo solo si no tiene entrega pendiente, en proceso o confirmada. La ficha y las fotos se conservarán. Podrás recuperarlo desde Mostrar descartados.")},
        confirmButton={TextButton(enabled=!busy,onClick={busy=true;scope.launch {
            try {
                val graph=AuthGraph.get(context)
                LocalDraftDiscard(graph.database,context).change(kind,id,restore,graph.sessions.current.value?.id)
                confirm=false;onChanged()
            }catch(e:Exception) {error=e.message;onError(e.message.orEmpty());confirm=false}
            finally {busy=false}
        }}) {Text(if(restore) "Recuperar" else "Confirmar descarte")}},
        dismissButton={TextButton(enabled=!busy,onClick={confirm=false}) {Text("Cancelar")}})
}
