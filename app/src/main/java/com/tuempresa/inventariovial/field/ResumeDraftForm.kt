package com.tuempresa.inventariovial.field

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.*
import com.tuempresa.inventariovial.core.InventoryDraftStore
import com.tuempresa.inventariovial.data.entity.InventorySnapshot
import com.tuempresa.inventariovial.diagnostics.Sic20DraftDiagnostics
import com.tuempresa.inventariovial.model.RoadAssetType
import com.tuempresa.inventariovial.model.form.*
import com.tuempresa.inventariovial.viewmodel.InventoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The home card, module list and records detail must resolve the same persisted identity. No writes. */
internal fun resolveDraftDestination(context:Context,snapshot:InventorySnapshot):DraftDestination {
    val r=snapshot.record
    try {
        require(r.status=="DRAFT") {"La ficha ya no es un borrador; vuelve al detalle."}
        check(InventoryDraftStore.accessible(context,r.id)) {"No se puede recuperar este borrador con la sesión actual."}
        val raw=InventoryDraftStore.read(context,r.id)
        val fields=raw?.getJSONObject("detail")?.getJSONObject("fields")
        if(r.sicCode=="SIC-20")Sic20DraftDiagnostics.record(context,"DRAFT_READ",r.assetType,snapshot.sic20?.classCode,fields?.optString("typeCode"),fields?.optString("classCode"))
        requireSicSnapshot(snapshot)
        raw?.let{requireSicRawDetail(r.sicCode,it.getJSONObject("detail").getString("type"))}
        return draftDestination(r,snapshot.sic21?.classCode ?: snapshot.sic20?.classCode,
            if(r.sicCode=="SIC-20")fields?.optString("classCode")else null).also{destination->
            if(r.sicCode=="SIC-20")Sic20DraftDiagnostics.record(context,"DRAFT_ROUTED",destination.asset.name,Sic20Identity.classOf(destination.asset.name),fields?.optString("typeCode"),fields?.optString("classCode"))
        }
    } catch(e:Exception) {
        if(r.sicCode=="SIC-20")Sic20DraftDiagnostics.record(context,"DRAFT_BLOCKED",r.assetType,snapshot.sic20?.classCode)
        throw e
    }
}

@Composable internal fun ResumeDraftForm(vm:InventoryViewModel,snapshot:InventorySnapshot,onBack:()->Unit) {
    val context=LocalContext.current;val r=snapshot.record
    // A normal draft save updates Room while this editor is open: do not dispose its unsaved state.
    var destination by remember(r.id){mutableStateOf<DraftDestination?>(null)}
    var blocked by remember(r.id){mutableStateOf(false)}
    LaunchedEffect(r.id) {
        try{destination=withContext(Dispatchers.IO){resolveDraftDestination(context,snapshot)}}
        catch(cancel:kotlinx.coroutines.CancellationException){throw cancel}
        catch(_:Exception){blocked=true}
    }
    if(blocked) {
        Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("Borrador conservado")
            Text("No se puede acreditar el tipo o subtipo de este borrador. Se conservan UUID, fotografías y datos. Solicita revisión; no se convirtió a otro tipo.")
            OutlinedButton(onClick=onBack){Text("Volver")}
        }
    } else destination?.let{d->
        if(d.asset==RoadAssetType.SIGNALIZATION)SignalizationFormScreen(requireNotNull(d.signal),vm,onBack,onBack,r)
        else AssetFormScreen(d.asset,vm,onBack,onBack,r)
    } ?: Text("Comprobando tipo de borrador…")
}
