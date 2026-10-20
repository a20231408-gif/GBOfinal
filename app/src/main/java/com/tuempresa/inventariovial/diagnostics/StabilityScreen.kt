package com.tuempresa.inventariovial.diagnostics

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable fun StabilityScreen(onBack:()->Unit) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    SideEffect{StabilityDiagnostics.screen("DIAGNOSTICS")}
    val report=remember {StabilityDiagnostics.report(context)};val exit=report.getJSONObject("lastExit")
    var message by remember{mutableStateOf<String?>(null)}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        TextButton(onClick=onBack){Text("← Inicio")}
        Text("DIAGNÓSTICO DE ESTABILIDAD",style=MaterialTheme.typography.headlineSmall)
        Text("Versión ${report.getString("versionName")} · ${report.getInt("versionCode")}")
        Text("Última salida: ${exit.getString("reason")}")
        if(exit.has("timestamp"))Text("Fecha: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss",Locale.getDefault()).format(Date(exit.getLong("timestamp")))}")
        val memory=report.getJSONObject("memory")
        Text("Memoria de la app: ${memory.getLong("heapUsedBytes")/1048576} MB de ${memory.getLong("heapMaxBytes")/1048576} MB")
        Text("Memoria disponible del sistema: ${memory.getLong("systemAvailableBytes")/1048576} MB")
        Text("El registro de Android puede no estar disponible. Este diagnóstico no contiene fotos, credenciales, textos de fichas ni coordenadas.")
        com.tuempresa.inventariovial.core.PhotoCompatibilityDiagnostic()
        com.tuempresa.inventariovial.core.SyncQueueOverview(technical=true)
        Button(onClick={scope.launch {try {
            val file=withContext(Dispatchers.IO){StabilityDiagnostics.exportWithSync(context)}
            val uri=FileProvider.getUriForFile(context,context.packageName+".fileprovider",file)
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("application/json").putExtra(Intent.EXTRA_STREAM,uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),"Exportar diagnóstico"))
        }catch(_:Exception){message="No se pudo exportar el diagnóstico."}}}){Text("Exportar diagnóstico")}
        message?.let{Text(it)}
    }
}
