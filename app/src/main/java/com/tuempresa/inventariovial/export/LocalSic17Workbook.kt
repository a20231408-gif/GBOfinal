package com.tuempresa.inventariovial.export

import com.tuempresa.inventariovial.scap.export.TemplateWorkbook
import com.tuempresa.inventariovial.scap.export.nodes
import org.w3c.dom.Element
import java.io.ByteArrayOutputStream
import java.time.LocalDate

/** Validated local adapter; never used to build sicRows, remote snapshots or ACKs. */
internal object LocalSic17Workbook {
    // Explicit semantic keys distinguish the two equally named PR/distance column pairs.
    val keys=listOf("routeCode","roadbedCode","startPrCode","startDistanceM","endPrCode","endDistanceM",
        "classCode","typeCode","bridgeCode","inventoriedCode","spans","dimension1LengthM","dimension2LowerHeightM",
        "structuralConditionCode","functionalConditionCode","surveyDate","serviceTypeCode","singularityCode","singularityName","dimension3UpperHeightM")
    private val labels=listOf("Carretera","Calzada","Código PR","Distancia (m)","Código PR","Distancia (m)",
        "Clase","Tipo","Código del puente","Inventariado","Ojos / Vanos","Dimensión 1 Longitud (m)","Dimensión 2 Altura inferior (m)",
        "Condición estructural","Condición funcional","Fecha","Tipo de servicio","Singularidad salvada","Nombre singularidad","Dimensión 3 Altura superior (m)")
    private val columns get()=SicExportFormat.SIC17.columns.also{require(it.map(SicColumn::label)==labels)}
    private fun col(n:Int)=('A'+n).toString()
    private fun output(write:(ByteArrayOutputStream)->Unit)=ByteArrayOutputStream().also(write).toByteArray()
    private fun children(e:Element)=(0 until e.childNodes.length).mapNotNull{e.childNodes.item(it) as? Element}
    private fun reject():Nothing=throw IllegalArgumentException("Plantilla SIC-17 no reconocida o alterada. Se conserva el consolidado anterior; no se transforman columnas incompatibles.")
    private fun text(c:Element?):String? {
        if(c==null)return null
        val elements=children(c)
        if(elements.isEmpty())return null
        if(c.getAttribute("t")!="inlineStr" || elements.size!=1 || elements.single().localName!="is")reject()
        val runs=children(elements.single()).filter{it.localName=="t"}
        if(runs.size!=1 || children(elements.single()).any{it.localName!="t"})reject()
        return runs.single().textContent
    }
    private fun number(c:Element):java.math.BigDecimal? {
        if(children(c).isEmpty())return null
        val elements=children(c)
        if(c.getAttribute("t").isNotEmpty() || elements.size!=1 || elements.single().localName!="v")reject()
        return elements.single().textContent.toBigDecimalOrNull() ?: reject()
    }
    private fun value(c:Element,kind:CellKind):Any?=when(kind) {
        CellKind.TEXT->text(c)
        CellKind.DECIMAL->number(c)?.let{it.toDouble().also{v->require(v.isFinite()){"Número SIC-17 no finito."}}}
        CellKind.INTEGER->number(c)?.intValueExact()
        CellKind.DATE->number(c)?.longValueExact()?.let{LocalDate.of(1899,12,30).plusDays(it)}
    }
    private fun withoutData(bytes:ByteArray):String {
        val xml=bytes.toString(Charsets.UTF_8)
        val rows=Regex("<row\\b[^>]*\\br=\"14\"[^>]*>.*?</row>",RegexOption.DOT_MATCHES_ALL)
        if(rows.findAll(xml).count()!=1)reject()
        return xml.replace(rows,"")
    }
    private fun matches(source:TemplateWorkbook,expected:ByteArray):Boolean {
        val known=TemplateWorkbook(expected.inputStream())
        return source.parts.keys==known.parts.keys && source.parts.all{(path,bytes)->
            if(path=="xl/worksheets/sheet1.xml")withoutData(bytes)==withoutData(known.parts.getValue(path))
            else bytes.contentEquals(known.parts.getValue(path))
        }
    }

    /** Only the two exact app templates are accepted, not an arbitrary 20/21-column workbook. */
    fun normalize(bytes:ByteArray,scapObservations:String?=null):ByteArray {
        require(SicRouteWorkbook.inspect(bytes,"SIC-17")==1){"Una identidad SIC-17 requiere una sola fila."}
        val book=TemplateWorkbook(bytes.inputStream());val sheet=book.sheet(1)
        val row=sheet.nodes("row").single{it.getAttribute("r")=="14"}
        val cells=children(row);val header=sheet.nodes("row").single{it.getAttribute("r")=="13"}
        val heads=children(header)
        if(cells.any{it.localName!="c"} || heads.any{it.localName!="c"})reject()
        val expectedRefs=(keys.indices).map{"${col(it)}14"}
        if(cells.map{it.getAttribute("r")} !in listOf(expectedRefs,expectedRefs+"U14"))reject()
        val route=text(cells.first()) ?: reject()
        if(heads.map(::text) !in listOf(labels,labels+"OBSERVACIONES"))reject()
        val blank=keys.associateWith<String,Any?>{null}
        val primary=output{SicExcelWriter.writeLocalSic17Row(it,route,blank,null)}
        val mapped=output{SicExcelWriter.writeMappedRow(it,"SIC-17",route,columns,List(keys.size){null})}
        val sourcePrimary=matches(book,primary)
        if(!sourcePrimary && !matches(book,mapped))reject()
        if((sourcePrimary && cells.size!=21) || (!sourcePrimary && cells.size!=20))reject()
        if(cells.any{children(it).any{child->child.localName=="f"}})reject()
        val fields=keys.mapIndexed{i,key->
            val expectedStyle=when(columns[i].kind){CellKind.TEXT->"3";CellKind.DECIMAL->"4";CellKind.INTEGER->"5";CellKind.DATE->"6"}
            if(cells[i].getAttribute("s")!=expectedStyle)reject()
            key to value(cells[i],columns[i].kind)
        }.toMap().toMutableMap()
        for(key in listOf("startPrCode","endPrCode"))fields[key]=(fields[key] as? String)?.let {
            require(it.matches(Regex("[0-9]{1,4}"))){"PR SIC-17 inválida; original conservado."};it.padStart(4,'0')
        }
        fields["classCode"]=(fields["classCode"] as? String)?.padStart(2,'0')
        val notes=if(sourcePrimary){if(cells.last().getAttribute("s")!="0")reject();text(cells.last())}else scapObservations
        return output{SicExcelWriter.writeLocalSic17Row(it,route,fields,notes)}
    }
}
