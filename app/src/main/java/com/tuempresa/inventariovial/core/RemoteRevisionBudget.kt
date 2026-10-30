package com.tuempresa.inventariovial.core

import java.io.InputStream
import java.io.ByteArrayOutputStream

/** Existing V21 limits, explicitly independent from local ZIP64/export capacity. */
object RemoteRevisionBudget {
    const val FILES=80
    const val FILE_BYTES=24L*1024*1024
    const val REVISION_BYTES=160L*1024*1024
    fun check(sizes:List<Long>,rowBytes:Long=0){
        if(sizes.size>FILES||sizes.any{it>FILE_BYTES||it<0}||sizes.fold(rowBytes){n,v->Math.addExact(n,v)}>REVISION_BYTES)
            throw SyncPending("Drive V21 admite 80 archivos, 24 MiB por archivo en staging y 160 MiB por revisión. Esta entrega necesita un protocolo de archivos grandes; no se envió ni confirmó parcialmente. Exporta la copia local completa; originales conservados.")
    }
    fun read(input:InputStream):ByteArray {
        val out=ByteArrayOutputStream();val buffer=ByteArray(64*1024);var size=0L
        while(true){val n=input.read(buffer);if(n<0)break;size+=n;check(listOf(size));out.write(buffer,0,n)}
        return out.toByteArray()
    }
}
