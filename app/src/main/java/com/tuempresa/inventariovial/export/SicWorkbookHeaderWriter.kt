package com.tuempresa.inventariovial.export

import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Corporate heading from SIC17_MO-104!F5 and its two original logo media parts.
 * Applies only a heading; each SIC retains its columns, title and data projection.
 */
object SicWorkbookHeaderWriter {
    const val CONTRACT_TITLE = "SERVICIO DE CONSULTORIA PARA EL INVENTARIO ESPECIALIZADO DE LA INFRAESTRUCTURA VIAL EN LOS TRAMOS DE LA RED VIAL NACIONAL NO CONCESIONADA"
    private const val MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    private const val REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    private const val PACKAGE = "http://schemas.openxmlformats.org/package/2006/relationships"
    private fun column(n:Int):String = if(n<=26) ('A'+n-1).toString() else column((n-1)/26)+('A'+(n-1)%26)

    fun write(output:OutputStream, workbook:ByteArray, columns:Int, mappedRow:Boolean=false) {
        require(columns>=9)
        // SIC-13 has nine contractual columns; keep the heading within that table.
        val leftColumns=if(columns<11) 2 else 5
        val rightColumns=if(columns<11) 2 else 4
        val parts=linkedMapOf<String,ByteArray>()
        ZipInputStream(workbook.inputStream()).use {zip->
            while(true) {val e=zip.nextEntry ?: break; if(!e.isDirectory) parts[e.name]=zip.readBytes()}
        }
        val worksheets=parts.keys.filter {it.matches(Regex("xl/worksheets/sheet[0-9]+\\.xml"))}
        worksheets.forEachIndexed {i,path->
            var sheet=parts.getValue(path).toString(Charsets.UTF_8)
            if(mappedRow) {
                sheet=Regex("(r|ref)=\"([^\"]+)\"").replace(sheet) {m->
                    val value=if(m.groupValues[1]=="r" && m.groupValues[2].all(Char::isDigit))
                        (m.groupValues[2].toInt()+10).toString()
                    else Regex("([A-Z]+)([0-9]+)").replace(m.groupValues[2]) {r->r.groupValues[1]+(r.groupValues[2].toInt()+10)}
                    "${m.groupValues[1]}=\"$value\""
                }
            }
            val rows=buildString {
                for(r in 1..4) {
                    append("<row r=\"$r\" ht=\"30\" customHeight=\"1\">")
                    if(r==1) append("<c r=\"${column(leftColumns+1)}1\" s=\"1\" t=\"inlineStr\"><is><t>$CONTRACT_TITLE</t></is></c>")
                    append("</row>")
                }
            }
            sheet=sheet.replace("<sheetData>","<sheetData>$rows")
            val merges="<mergeCell ref=\"A1:${column(leftColumns)}4\"/><mergeCell ref=\"${column(leftColumns+1)}1:${column(columns-rightColumns)}4\"/><mergeCell ref=\"${column(columns-rightColumns+1)}1:${column(columns)}4\"/>"
            sheet=sheet.replace(Regex("<mergeCells[^>]*>"),"<mergeCells>").replace("</mergeCells>","$merges</mergeCells>")
            sheet=sheet.replace(Regex("<dimension ref=\"A5:"),"<dimension ref=\"A1:")
            sheet=sheet.replace("</worksheet>","<drawing xmlns:r=\"$REL\" r:id=\"corporateHeader\"/></worksheet>")
            parts[path]=sheet.toByteArray()
            val draw="sicHeader${i+1}"
            parts["xl/worksheets/_rels/${path.substringAfterLast('/')}.rels"]=("<Relationships xmlns=\"$PACKAGE\"><Relationship Id=\"corporateHeader\" Type=\"$REL/drawing\" Target=\"../drawings/$draw.xml\"/></Relationships>").toByteArray()
            parts["xl/drawings/$draw.xml"]=drawing(columns).toByteArray()
            parts["xl/drawings/_rels/$draw.xml.rels"]=("<Relationships xmlns=\"$PACKAGE\"><Relationship Id=\"rId1\" Type=\"$REL/image\" Target=\"../media/sicHeader1.png\"/><Relationship Id=\"rId2\" Type=\"$REL/image\" Target=\"../media/sicHeader2.jpeg\"/></Relationships>").toByteArray()
        }
        for((source,target) in listOf("image1.png" to "sicHeader1.png","image2.jpeg" to "sicHeader2.jpeg")) {
            parts["xl/media/$target"]=requireNotNull(javaClass.getResourceAsStream("/sic_header/$source")) {"Falta logo corporativo $source"}.use {it.readBytes()}
        }
        val additions="<Default Extension=\"png\" ContentType=\"image/png\"/><Default Extension=\"jpeg\" ContentType=\"image/jpeg\"/>"+
            worksheets.indices.joinToString("") {"<Override PartName=\"/xl/drawings/sicHeader${it+1}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.drawing+xml\"/>"}
        parts["[Content_Types].xml"]=parts.getValue("[Content_Types].xml").toString(Charsets.UTF_8).replace("</Types>","$additions</Types>").toByteArray()
        if(!mappedRow) parts["xl/workbook.xml"]=parts.getValue("xl/workbook.xml").toString(Charsets.UTF_8).replace("!\$A\$5:","!\$A\$1:").toByteArray()
        ZipOutputStream(output).use {zip->parts.forEach {(path,bytes)->zip.putNextEntry(ZipEntry(path));zip.write(bytes);zip.closeEntry()}}
    }

    private fun drawing(columns:Int):String {
        fun picture(id:Int,col:Int,width:Long,height:Long,crop:String) = """
          <xdr:oneCellAnchor><xdr:from><xdr:col>$col</xdr:col><xdr:colOff>100000</xdr:colOff><xdr:row>1</xdr:row><xdr:rowOff>100000</xdr:rowOff></xdr:from><xdr:ext cx="$width" cy="$height"/>
          <xdr:pic><xdr:nvPicPr><xdr:cNvPr id="$id" name="Corporate logo $id"/><xdr:cNvPicPr><a:picLocks noChangeAspect="1"/></xdr:cNvPicPr></xdr:nvPicPr><xdr:blipFill><a:blip r:embed="rId$id"/>$crop<a:stretch><a:fillRect/></a:stretch></xdr:blipFill><xdr:spPr><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></xdr:spPr></xdr:pic><xdr:clientData/></xdr:oneCellAnchor>
        """.trimIndent()
        return "<xdr:wsDr xmlns:xdr=\"http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing\" xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" xmlns:r=\"$REL\">"+
            picture(1,0,1743075,400050,"<a:srcRect r=\"74851\" b=\"-1731\"/>")+
            picture(2,columns-if(columns<11) 2 else 4,if(columns<11) 1900000 else 2876550,if(columns<11) 318788 else 482642,"<a:srcRect/>")+"</xdr:wsDr>"
    }
}
