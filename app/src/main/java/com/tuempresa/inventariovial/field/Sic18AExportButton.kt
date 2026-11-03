package com.tuempresa.inventariovial.field

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.tuempresa.inventariovial.data.entity.InventoryRecordEntity
import com.tuempresa.inventariovial.export.*
import com.tuempresa.inventariovial.supplementary.SupplementaryFormState
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun Sic18AExportButton(record:InventoryRecordEntity,state:SupplementaryFormState) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    var message by remember {mutableStateOf<String?>(null)};var busy by remember {mutableStateOf(false)}
    var pending by remember {mutableStateOf<com.tuempresa.inventariovial.localfiles.ManualDocument?>(null)}
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(SicExcelWriter.XLSX_MIME)) {uri->
        val captured=pending
        if(uri==null)message="Guardado cancelado; registros y consolidado conservados."
        else if(captured!=null)scope.launch {
            busy=true
            try {com.tuempresa.inventariovial.localfiles.ManualDocumentExport.save(context,
                com.tuempresa.inventariovial.data.database.InventoryDatabase.getInstance(context),captured,uri)
                message="SIC-18A guardado y verificado · "+captured.notice
            }catch(e:kotlinx.coroutines.CancellationException){throw e}catch(e:Exception){message=e.message}
            finally{busy=false}
        }
    }
    OutlinedButton(enabled=!busy,onClick={scope.launch {
        busy=true
        try {
            val errors=state.validate();require(errors.isEmpty()){errors.joinToString("\n")}
            val db=com.tuempresa.inventariovial.data.database.InventoryDatabase.getInstance(context)
            val snapshot=withContext(Dispatchers.IO){requireNotNull(db.inventoryDao().snapshot(record.id))}
            require(snapshot.sic18a==com.tuempresa.inventariovial.data.entity.Sic18AEntity.from(record.id,state.values)){
                "Guarda los cambios de SIC-18A antes de exportar el consolidado de carretera."
            }
            pending=LocalSicConsolidates.prepareManual(context,db,record.routeCode,"SIC-18A")
            export.launch("SIC-18A.xlsx")
        }catch(e:kotlinx.coroutines.CancellationException){throw e}catch(e:Exception){message=e.message}
        finally{busy=false}
    }}) {Text("Exportar consolidado SIC-18A")}
    message?.let {Text(it)}
}
