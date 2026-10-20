package com.tuempresa.inventariovial.localfiles

import com.tuempresa.inventariovial.export.ProvisionalSicExcel
import com.tuempresa.inventariovial.pavement.PavementExcel
import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot
import com.tuempresa.inventariovial.scap.export.ScapFieldBackup
import org.json.*
import java.io.*

/** Readable fallback when the canonical layout cannot faithfully contain an incomplete capture. */
object ProvisionalScapWorkbook {
    fun write(out:OutputStream,s:ScapInspectionSnapshot,reason:String) {
        val rows=mutableListOf<List<Any?>>(listOf(ProvisionalSicExcel.MARKER),listOf("Campo / sección","Valor registrado"))
        fun flatten(path:String,value:Any?) {
            when(value){
                is JSONObject->value.keys().asSequence().toList().sorted().forEach {key->
                    if(key !in setOf("localPath","originalPath","stampedPath","localUri"))flatten("$path/$key",value.opt(key))
                }
                is JSONArray->for(n in 0 until value.length())flatten("$path/${n+1}",value.opt(n))
                else->rows+=listOf(path,value.takeUnless{it==JSONObject.NULL})
            }
        }
        flatten("SCAP",ScapFieldBackup.capture(s))
        val body=ByteArrayOutputStream();PavementExcel.workbook(body,listOf("SCAP" to rows))
        ProvisionalSicExcel.decorate(out,body.toByteArray(),s.inspection.id,s.inspection.createdBy,s.inspection.updatedAt,s.inspection.createdAt,
            listOf(ProvisionalSicExcel.Pending("Plantilla SCAP","SCAP",reason,"REVISAR")))
    }
}
