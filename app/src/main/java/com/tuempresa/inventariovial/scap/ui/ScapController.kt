package com.tuempresa.inventariovial.scap.ui

import android.content.Context
import androidx.room.withTransaction
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.scap.catalog.*
import com.tuempresa.inventariovial.scap.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicInteger

class ScapController(context:Context,private val db:InventoryDatabase,scope:CoroutineScope) {
    // Finishes accepted local writes across Activity/session navigation; no network dependency.
    private val writer=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val journal=ScapEditJournal(context.applicationContext)
    private val diagnostics=context.applicationContext
    private var recoveryFailed=false

    private val _catalog=MutableStateFlow<ScapCatalog?>(null);val catalog=_catalog.asStateFlow()
    private val _error=MutableStateFlow<String?>(null);val error=_error.asStateFlow()
    private val _saveState=MutableStateFlow("Sin operaciones pendientes de guardado");val saveState=_saveState.asStateFlow()
    private val _pending=MutableStateFlow(0);val pending=_pending.asStateFlow()
    private val counter=AtomicInteger()
    private val actions=Channel<suspend (ScapRepository)->Unit>(Channel.UNLIMITED)
    private val failed=mutableListOf<suspend (ScapRepository)->Unit>()
    val selected=MutableStateFlow<String?>(null)
    private val _destination=MutableStateFlow<Pair<Long,com.tuempresa.inventariovial.scap.domain.ScapFieldRoute>?>(null)
    val destination=_destination.asStateFlow()
    fun navigate(route:com.tuempresa.inventariovial.scap.domain.ScapFieldRoute){_destination.value=((_destination.value?.first ?: 0)+1) to route}
    private val _overrides=MutableStateFlow<Map<String,String>>(emptyMap());val overrides=_overrides.asStateFlow()
    private val ready=CompletableDeferred<ScapRepository>()
    val inspections=db.scapDao().observeInspections().stateIn(scope,SharingStarted.WhileSubscribed(5000),emptyList())
    @OptIn(ExperimentalCoroutinesApi::class)
    val snapshot=selected.flatMapLatest {id->if(id==null) flowOf(null) else db.scapDao().observe(id)}
        .stateIn(scope,SharingStarted.WhileSubscribed(5000),null)
    init {
        writer.launch {
            try {val c=withContext(Dispatchers.IO){ScapCatalog.load(context)};_catalog.value=c;ready.complete(ScapRepository(db,c,com.tuempresa.inventariovial.road.ProjectRouteCatalog.load(context)))}
            catch(e:Exception) {_error.value="No se pudo cargar el catálogo SCAP: ${e.message}";ready.completeExceptionally(e)}
        }
        try {journal.pending().forEach {e->restoreEdit(e)}} catch(e:Exception){recoveryFailed=true;_error.value="No se pudo recuperar una edición pendiente. Conserva los datos y solicita soporte; no borres almacenamiento.";_saveState.value="Error de recuperación local"}
        writer.launch {
            for(action in actions) {
                val started=System.nanoTime()
                try {if(failed.isNotEmpty()||recoveryFailed)failed+=action else action(ready.await())}
                catch(e:CancellationException){throw e}
                catch(e:Exception){failed+=action;_error.value="No se pudo guardar dentro de GBO. Los valores pendientes se conservan; reintenta antes de cerrar."}
                finally {
                    com.tuempresa.inventariovial.diagnostics.ScapSaveDiagnostics.record(diagnostics,(System.nanoTime()-started)/1_000_000,failed.size,counter.get()-1)
                    _pending.value=counter.decrementAndGet()
                    _saveState.value=if(failed.isNotEmpty()||recoveryFailed) "Error de guardado · reintentar" else if(counter.get()>0) "Guardando…" else "Ficha guardada dentro de GBO"
                }
            }
        }
    }
    fun submit(action:suspend (ScapRepository)->Unit) {
        _pending.value=counter.incrementAndGet();_saveState.value="Guardando…"
        check(actions.trySend(action).isSuccess)
    }
    fun retry() {if(recoveryFailed||counter.get()!=0)return;val list=failed.toList();failed.clear();_error.value=null;list.forEach(::submit)}
    fun clearError() {if(failed.isEmpty()&&!recoveryFailed) _error.value=null}
    fun select(id:String?) {if(selected.value!=id)_destination.value=null;selected.value=id;if(id!=null)submit{it.ensureBridgeDefaults(id)}}
    suspend fun savedSnapshot(id:String):ScapInspectionSnapshot {
        check(counter.get()==0 && failed.isEmpty() && !recoveryFailed) {"Espera a que termine el guardado local."}
        return withContext(Dispatchers.IO){requireNotNull(ready.await().dao.snapshot(id)){"Inspección inexistente."}}
    }
    fun value(id:String,owner:String,key:String,s:ScapInspectionSnapshot):String =
        _overrides.value["$id/$owner/$key"] ?: s.values(owner)[key].orEmpty()
    fun edit(id:String,owner:String,key:String,value:String,source:String="MANUAL") {
        _overrides.value=_overrides.value+("$id/$owner/$key" to value)
        if(key=="category") _overrides.value=_overrides.value+("$id/$owner/type" to "")
        scheduleEdit(journal.new(id,owner,key,value,source,false))
    }
    fun editSic(id:String,format:String,key:String,value:String) {
        val owner=com.tuempresa.inventariovial.scap.domain.BridgeSicContract.prefix(format).removeSuffix(".")
        _overrides.value=_overrides.value+("$id/$owner/$key" to value)
        if(format=="SIC-17" && key=="classCode") _overrides.value=_overrides.value+("$id/$owner/typeCode" to "")
        scheduleEdit(journal.new(id,format,key,value,"MANUAL_SIC",true))
    }
    private fun restoreEdit(e:ScapEditJournal.Edit){
        val owner=if(e.sic)com.tuempresa.inventariovial.scap.domain.BridgeSicContract.prefix(e.owner).removeSuffix(".")else e.owner
        _overrides.value=_overrides.value+("${e.id}/$owner/${e.key}" to e.value)
        submit {repo->
            db.withTransaction {
                if(!journal.current(e))return@withTransaction // Never replay superseded input.
                val saved=repo.dao.snapshot(e.id)
                val existing=if(e.sic)saved?.let{com.tuempresa.inventariovial.scap.domain.BridgeSicContract.manualValues(it,e.owner)[e.key]}else saved?.values(e.owner)?.get(e.key)
                if(saved?.inspection?.status!="COMPLETE" || existing!=e.value){if(e.sic)repo.setSicField(e.id,e.owner,e.key,e.value)else repo.setField(e.id,e.owner,e.key,e.value,e.source)}
            }
            journal.committed(e)
        }
    }
    private fun scheduleEdit(e:ScapEditJournal.Edit){
        // Persist before accepting the edit into the asynchronous Room queue.
        val durable=runCatching{journal.put(e)}.isSuccess
        if(durable)restoreEdit(e)else submit {repo->journal.put(e);if(e.sic)repo.setSicField(e.id,e.owner,e.key,e.value)else repo.setField(e.id,e.owner,e.key,e.value,e.source);journal.committed(e)}
    }
    fun restoreSic(id:String,format:String,key:String) {
        val owner=com.tuempresa.inventariovial.scap.domain.BridgeSicContract.prefix(format).removeSuffix(".")
        _overrides.value=_overrides.value-("$id/$owner/$key")
        submit {it.restoreSicField(id,format,key)}
    }

}
