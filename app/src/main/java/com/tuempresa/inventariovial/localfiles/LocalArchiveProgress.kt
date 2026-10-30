package com.tuempresa.inventariovial.localfiles

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Transient byte progress; durable job/URI/hash results remain in Room. No credentials or paths. */
object LocalArchiveProgress {
    private val values=MutableStateFlow<Map<String,VerifiedZipArchive.Progress>>(emptyMap())
    val flow=values.asStateFlow()
    @Synchronized fun record(job:String,p:VerifiedZipArchive.Progress){values.value=(values.value-job).entries.toList().takeLast(31).associate{it.toPair()}+(job to p)}
    fun label(p:VerifiedZipArchive.Progress):String {
        val action=when(p.phase){"VERIFYING"->"Verificando integridad";"READY"->"Paquete verificado";else->"Escribiendo ZIP"}
        return "$action: ${p.bytes/(1024*1024)} / ${p.total/(1024*1024)} MiB"
    }
}
