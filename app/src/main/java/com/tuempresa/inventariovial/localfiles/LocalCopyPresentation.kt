package com.tuempresa.inventariovial.localfiles

/** One result per persisted job. Historical results are never advertised as the current revision. */
object LocalCopyPresentation {
    fun context(job:LocalExportJob,current:Long?,latest:Boolean):String {
        val revision=when {current==null->"Revisión guardada ${job.revision}";job.revision==current->"Revisión actual ${job.revision}";else->"Revisión anterior ${job.revision}; la ficha actual es $current"}
        val scope=when(job.scope){"FULL"->"Documentos y medios";"PHOTOS"->"Fotografías";"ZIP"->"Paquete ZIP";else->"Documento"}
        return "$revision · $scope"+if(!latest)" · intento anterior"else""
    }
    fun result(job:LocalExportJob):String = when(job.state) {
        "QUEUED"->"Copia local pendiente"
        "RUNNING"->if(job.total==0)"Preparando copia local; cantidad de archivos aún no calculada"else"Guardando copia local: ${job.completed}/${job.total} verificados"
        "READY"->"Copia local completada: ${job.completed} archivos verificados."
        else->job.message ?: if(job.state=="PARTIAL")"Copia local parcial: reintentar"else"Copia local fallida: reintentar"
    }
}
