package com.tuempresa.inventariovial.core

import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.*

/** Manual compatibility check is confined to the existing technical diagnostics screen. */
@Composable internal fun PhotoCompatibilityDiagnostic() {
    val scope=rememberCoroutineScope()
    var checking by remember {mutableStateOf(false)}
    var diagnostic by remember {mutableStateOf<String?>(null)}
    Text("Protocolo fotográfico V2: ${if(com.tuempresa.inventariovial.BuildConfig.GBO_PHOTO_NAMES_V2_ENABLED) "habilitado" else "deshabilitado"}")
    TextButton(enabled=!checking && com.tuempresa.inventariovial.DriveUploadPolicy.configurationError()==null,onClick={
        checking=true
        scope.launch {
            try {diagnostic=withContext(Dispatchers.IO){AutomaticDriveHttp().photoNamingDiagnostic().text()}}
            catch(cancel:CancellationException) {throw cancel}
            catch(_:Exception) {diagnostic="Compatibilidad del servidor no comprobada. Revisa la conexión y vuelve a consultar."}
            finally {checking=false}
        }
    }) {Text(if(checking) "Verificando compatibilidad…" else "Verificar compatibilidad de fotografías con el servidor")}
    diagnostic?.let {Text(it,style=MaterialTheme.typography.bodySmall)}
}
