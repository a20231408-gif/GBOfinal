package com.tuempresa.inventariovial.localfiles

/** One result per persisted job. Historical results are never advertised as the current revision. */
object LocalCopyPresentation {
    fun scope(scope:String)=scope.removeSuffix("_LAYOUT_V5").removeSuffix("_LAYOUT_V4").removeSuffix("_LAYOUT_V3").removeSuffix("_READABLE_V2")
    /** Layout regeneration is another generation of the same copy, not another document category. */
    fun latest(jobs:List<LocalExportJob>)=jobs.sortedWith(compareByDescending<LocalExportJob>{it.revision}
        .thenByDescending{it.createdAt}.thenByDescending{when{it.scope.endsWith("_LAYOUT_V5")->5;it.scope.endsWith("_LAYOUT_V4")->4;it.scope.endsWith("_LAYOUT_V3")->3;it.scope.endsWith("_READABLE_V2")->2;else->1}}
        .thenBy{it.id}).distinctBy{it.recordKey to scope(it.scope)}
    fun currentFiles(jobs:List<LocalExportJob>,files:List<LocalSharedFile>,history:Boolean=false):List<LocalSharedFile>{
        val ready=files.filter{it.state=="READY"}
        if(history)return ready
        val visible=ready.filterNot{it.name.startsWith(".Historial - ")}
        val ids=latest(jobs.filter{job->visible.any{it.jobId==job.id}}).map{it.id}.toSet()
        return visible.filter{it.jobId in ids}
    }
    fun context(job:LocalExportJob,current:Long?,latest:Boolean):String {
        val revision=when {current==null->"Revisión guardada ${job.revision}";job.revision==current->"Revisión actual ${job.revision}";else->"Revisión anterior ${job.revision}; la ficha actual es $current"}
        val scope=when(scope(job.scope)){"FULL"->"Documentos y medios";"PHOTOS"->"Fotografías";"ZIP"->"Paquete ZIP";DriveMirrorPlan.SCOPE->"Copia del Drive confirmado";else->"Documento"}
        return "$revision · $scope"+if(!latest)" · intento anterior"else""
    }
    fun result(job:LocalExportJob):String = when(job.state) {
        "QUEUED"->"Copia local pendiente"
        "RUNNING"->if(job.total==0)"Preparando copia local; cantidad de archivos aún no calculada"else"Guardando copia local: ${job.completed}/${job.total} verificados"
        "READY"->"Copia local completada: ${job.completed} archivos verificados."
        else->job.message ?: if(job.state=="PARTIAL")"Copia local parcial: reintentar"else"Copia local fallida: reintentar"
    }
}
