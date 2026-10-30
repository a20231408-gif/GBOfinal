package com.tuempresa.inventariovial.export

import com.tuempresa.inventariovial.scap.export.TemplateWorkbook
import com.tuempresa.inventariovial.scap.export.nodes
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Same single-sheet source parts as V20; never redesigns a contractual template. */
internal object SicRouteWorkbook {
    private val row=Regex("<row\\b[^>]*>.*?</row>",RegexOption.DOT_MATCHES_ALL)
    private val data=Regex("<sheetData>(.*?)</sheetData>",RegexOption.DOT_MATCHES_ALL)
    private fun number(xml:String)=Regex("\\br=\"([0-9]+)\"").find(xml)!!.groupValues[1].toInt()
    fun start(format:String)=if(format in setOf("SIC-14","SIC-14A","SIC-15","SIC-16"))3 else 14
    fun inspect(bytes:ByteArray,format:String):Int {
        val book=TemplateWorkbook(bytes.inputStream())
        require(book.parts.keys.count{it.matches(Regex("xl/worksheets/sheet[0-9]+\\.xml"))}==1 && "xl/sharedStrings.xml" !in book.parts){"El consolidado requiere una sola hoja contractual compatible."}
        require(book.document("xl/workbook.xml").nodes("sheet").size==1)
        book.document("xl/styles.xml");val sheet=book.sheet(1)
        val numbers=sheet.nodes("row").map{it.getAttribute("r").toInt()}
        require(numbers.distinct().size==numbers.size && numbers==numbers.sorted()){"Filas XLSX inválidas."}
        // OOXML permits implicit cell addresses (the existing Pavement writer uses them).
        for(r in sheet.nodes("row")) {
            var column=0;val used=mutableSetOf<Int>()
            for(c in r.childNodes.let{nodes->(0 until nodes.length).mapNotNull{nodes.item(it) as? org.w3c.dom.Element}}.filter{it.localName=="c"}) {
                val ref=c.getAttribute("r")
                column=if(ref.isBlank())column+1 else TemplateWorkbook.column(ref)
                require(column>0 && used.add(column) && (ref.isBlank()||ref.filter(Char::isDigit)==r.getAttribute("r"))){"Celdas XLSX duplicadas o fuera de su fila."}
            }
        }
        return numbers.count{it>=start(format)}
    }
    fun merge(rows:List<ByteArray>,format:String):ByteArray {
        require(rows.isNotEmpty()){"No hay filas válidas para $format en esta ruta."}
        val books=rows.map{bytes->require(inspect(bytes,format)==1){"Cada identidad debe aportar una sola fila."};TemplateWorkbook(bytes.inputStream())}
        val first=books.first();val start=start(format)
        fun signature(b:TemplateWorkbook):List<String> {
            val sheet=b.parts.getValue("xl/worksheets/sheet1.xml").toString(Charsets.UTF_8)
            return listOf(b.parts.getValue("xl/styles.xml").toString(Charsets.UTF_8),
                Regex("<cols>.*?</cols>",RegexOption.DOT_MATCHES_ALL).find(sheet)?.value.orEmpty(),
                row.findAll(sheet).singleOrNull{number(it.value)==start-1}?.value.orEmpty())
        }
        val signature=signature(first)
        require(books.all{signature(it)==signature}){"Plantillas de $format incompatibles. Se conserva el consolidado anterior; requiere conciliar las plantillas de origen."}
        val template=first.parts.getValue("xl/worksheets/sheet1.xml").toString(Charsets.UTF_8)
        val head=row.findAll(data.find(template)!!.groupValues[1]).filter{number(it.value)<start}.joinToString(""){it.value}
        val body=books.mapIndexed{index,b->
            val xml=row.findAll(b.parts.getValue("xl/worksheets/sheet1.xml").toString(Charsets.UTF_8)).single{number(it.value)>=start}.value
            require(!Regex("<f\\b").containsMatchIn(xml)){"Fórmulas de fila no admitidas por el contrato V20; se conserva el documento anterior."}
            xml.replaceFirst(Regex("\\br=\"[0-9]+\""),"r=\"${start+index}\"")
                .replace(Regex("\\br=\"([A-Z]+)[0-9]+\"")){m->"r=\"${m.groupValues[1]}${start+index}\""}
        }.joinToString("")
        val last=start-1+books.size
        var merged=template.replace(data,"<sheetData>$head$body</sheetData>")
            .replace(Regex("(<dimension ref=\"[^\"]*:[A-Z]+)[0-9]+")){it.groupValues[1]+last}
        // The shared local SIC-17 primary template has a filter; include every merged row,
        // retaining its original contractual columns (OBSERVACIONES remains outside it).
        if(format=="SIC-17")merged=merged.replace(Regex("(<autoFilter ref=\"A13:[A-Z]+)[0-9]+")){it.groupValues[1]+last}
        first.parts["xl/worksheets/sheet1.xml"]=merged.toByteArray()
        // V20 updates only data rows, dimension and print extent; all remaining parts stay byte-identical.
        first.parts["xl/workbook.xml"]=first.parts.getValue("xl/workbook.xml").toString(Charsets.UTF_8)
            .replace(Regex("(!\\\$A\\\$[0-9]+:\\\$[A-Z]+\\\$)[0-9]+")){it.groupValues[1]+last}.toByteArray()
        return ByteArrayOutputStream().also{out->ZipOutputStream(out).use{zip->first.parts.forEach{(path,bytes)->zip.putNextEntry(ZipEntry(path).apply{time=0});zip.write(bytes);zip.closeEntry()}}}.toByteArray()
            .also{require(inspect(it,format)==books.size){"No se verificaron todas las filas del consolidado."}}
    }
}
