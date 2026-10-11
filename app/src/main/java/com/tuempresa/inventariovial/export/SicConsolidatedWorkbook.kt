package com.tuempresa.inventariovial.export

import java.io.OutputStream
import java.util.zip.*

/** Combines real exporter rows. Retains source header/media/styles, one worksheet per route. */
object SicConsolidatedWorkbook {
    private fun parts(bytes:ByteArray)=linkedMapOf<String,ByteArray>().apply {
        ZipInputStream(bytes.inputStream()).use {z->while(true){val e=z.nextEntry ?: break;if(!e.isDirectory)put(e.name,z.readBytes())}}
    }
    fun write(out:OutputStream,rows:List<Pair<String,ByteArray>>) {
        require(rows.isNotEmpty())
        val groups=rows.groupBy {it.first}.toSortedMap();val first=parts(rows.first().second)
        val styles=first.getValue("xl/styles.xml")
        val sheets=mutableListOf<Pair<String,String>>()
        for((route,entries) in groups) {
            val sources=entries.map {parts(it.second)}
            require(sources.all {it.getValue("xl/styles.xml").contentEquals(styles)}) {"Plantillas SIC incompatibles."}
            val template=sources.first().getValue("xl/worksheets/sheet1.xml").toString(Charsets.UTF_8)
            val header=Regex("<sheetData>(.*?)</sheetData>",RegexOption.DOT_MATCHES_ALL).find(template)!!.groupValues[1]
            val data=sources.flatMap {p->Regex("<row\\b[^>]*>.*?</row>",RegexOption.DOT_MATCHES_ALL)
                .findAll(p.getValue("xl/worksheets/sheet1.xml").toString(Charsets.UTF_8)).filter {Regex("\\br=\"([0-9]+)\"").find(it.value)!!.groupValues[1].toInt()>=14}.map {it.value}.toList()}
            val headings=Regex("<row\\b[^>]*>.*?</row>",RegexOption.DOT_MATCHES_ALL).findAll(header)
                .filter {Regex("\\br=\"([0-9]+)\"").find(it.value)!!.groupValues[1].toInt()<14}.joinToString(""){it.value}
            val merged=data.mapIndexed {n,xml->xml.replaceFirst(Regex("\\br=\"[0-9]+\""),"r=\"${n+14}\"")
                .replace(Regex("\\br=\"([A-Z]+)[0-9]+\"")){m->"r=\"${m.groupValues[1]}${n+14}\""}}.joinToString("")
            val sheet=template.replace(Regex("<sheetData>.*?</sheetData>",RegexOption.DOT_MATCHES_ALL),"<sheetData>$headings$merged</sheetData>")
                .replace(Regex("(<dimension ref=\"[^\"]*:[A-Z]+)[0-9]+")){m->m.groupValues[1]+(13+data.size)}
            sheets+=route to sheet
        }
        first.keys.filter {it.startsWith("xl/worksheets/")}.toList().forEach {first.remove(it)}
        sheets.forEachIndexed {n,(_,sheet)->
            first["xl/worksheets/sheet${n+1}.xml"]=sheet.toByteArray()
            val relationship=parts(rows.first().second)["xl/worksheets/_rels/sheet1.xml.rels"]
            if(relationship!=null)first["xl/worksheets/_rels/sheet${n+1}.xml.rels"]=relationship
        }
        var workbook=first.getValue("xl/workbook.xml").toString(Charsets.UTF_8)
        val names=mutableSetOf<String>()
        val declarations=sheets.mapIndexed {n,(route,_)->
            val name=route.replace(Regex("[\\\\/*?:\\[\\]]"),"_").take(31)
            require(name.isNotBlank() && names.add(name)) {"Nombre de ruta no único en Excel."}
            "<sheet name=\"${name.replace("&","&amp;").replace("\"","&quot;")}\" sheetId=\"${n+1}\" r:id=\"rId${n+1}\"/>"
        }.joinToString("")
        workbook=workbook.replace(Regex("<sheets>.*?</sheets>",RegexOption.DOT_MATCHES_ALL),"<sheets>$declarations</sheets>")
        // Remove old scopes; print geometry remains on each worksheet and its media.
        val printNames=sheets.mapIndexed {index,(route,sheet)->
            val quoted="'"+route.replace("'","''").replace("&","&amp;")+"'"
            val last=Regex("r=\"([A-Z]+)[0-9]+\"").findAll(sheet).map {it.groupValues[1]}.maxWith(compareBy<String> {it.length}.thenBy {it})
            val row=Regex("<row[^>]*r=\"([0-9]+)\"").findAll(sheet).maxOf {it.groupValues[1].toInt()}
            "<definedName name=\"_xlnm.Print_Titles\" localSheetId=\"$index\">$quoted!\$1:\$13</definedName>"+
                "<definedName name=\"_xlnm.Print_Area\" localSheetId=\"$index\">$quoted!\$A\$1:\$$last\$$row</definedName>"
        }.joinToString("")
        workbook=workbook.replace(Regex("<definedNames>.*?</definedNames>",RegexOption.DOT_MATCHES_ALL),"").replace("</workbook>","<definedNames>$printNames</definedNames></workbook>")
        first["xl/workbook.xml"]=workbook.toByteArray()
        var rels=first.getValue("xl/_rels/workbook.xml.rels").toString(Charsets.UTF_8)
        rels=rels.replace(Regex("<Relationship[^>]*Type=\"[^\"]*/worksheet\"[^>]*/>"),"")
        rels=rels.replace("</Relationships>",sheets.indices.joinToString(""){n->"<Relationship Id=\"rId${n+1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet${n+1}.xml\"/>"}+"</Relationships>")
        first["xl/_rels/workbook.xml.rels"]=rels.toByteArray()
        var types=first.getValue("[Content_Types].xml").toString(Charsets.UTF_8)
        types=types.replace(Regex("<Override PartName=\"/xl/worksheets/[^\"]+\"[^>]*/>"),"")
        types=types.replace("</Types>",sheets.indices.joinToString(""){n->"<Override PartName=\"/xl/worksheets/sheet${n+1}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"}+"</Types>")
        first["[Content_Types].xml"]=types.toByteArray()
        ZipOutputStream(out).use {zip->first.forEach {(name,bytes)->zip.putNextEntry(ZipEntry(name));zip.write(bytes);zip.closeEntry()}}
    }
}
