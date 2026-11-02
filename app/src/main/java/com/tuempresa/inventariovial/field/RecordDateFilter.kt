package com.tuempresa.inventariovial.field

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

/** Filter saved survey dates, never export, upload or edit timestamps. */
data class RecordDateSelection(val year:Int?=null,val month:Int?=null,val day:Int?=null) {
    fun matches(date:LocalDate?):Boolean = if(year==null && month==null && day==null) true
        else date!=null && (year==null || date.year==year) && (month==null || date.monthValue==month) && (day==null || date.dayOfMonth==day)
}

fun parseRecordDate(value:String):LocalDate? {
    for(pattern in listOf("uuuu-MM-dd","dd/MM/uuuu")) {
        val date=runCatching {LocalDate.parse(value.trim(),DateTimeFormatter.ofPattern(pattern).withResolverStyle(ResolverStyle.STRICT))}.getOrNull()
        if(date!=null)return date
    }
    return null
}

/** Display only: unknown historical text remains visible, never rewritten in Room. */
fun recordDateLabel(value:String)=parseRecordDate(value)?.format(DateTimeFormatter.ofPattern("dd/MM/uuuu")) ?: value.ifBlank{"Sin fecha registrada"}
fun recordMonthLabel(month:Int)=java.time.Month.of(month).getDisplayName(java.time.format.TextStyle.FULL,java.util.Locale.forLanguageTag("es-PE"))

@Composable fun RecordDateFilter(value:RecordDateSelection,dates:List<LocalDate>,onChange:(RecordDateSelection)->Unit) {
    Text("Fecha de registro")
    Row(Modifier.fillMaxWidth()) {
        DatePart("Año",value.year,dates.map{it.year}.distinct().sortedDescending(),Modifier.weight(1f)){onChange(value.copy(year=it))}
        DatePart("Mes",value.month,(1..12).toList(),Modifier.weight(1f)){onChange(value.copy(month=it))}
        DatePart("Día",value.day,(1..31).toList(),Modifier.weight(1f)){onChange(value.copy(day=it))}
    }
    if(value!=RecordDateSelection())TextButton(onClick={onChange(RecordDateSelection())}){Text("Limpiar fecha")}
}

@Composable private fun DatePart(label:String,value:Int?,values:List<Int>,modifier:Modifier,onChange:(Int?)->Unit) {
    var expanded by remember {mutableStateOf(false)}
    Box(modifier) {
        OutlinedButton(onClick={expanded=true}) {Text("$label: ${value?.let{if(label=="Mes")recordMonthLabel(it)else it.toString()} ?: "Todos"}")}
        DropdownMenu(expanded,onDismissRequest={expanded=false}) {
            DropdownMenuItem(text={Text("Todos")},onClick={onChange(null);expanded=false})
            values.forEach {part->DropdownMenuItem(text={Text(if(label=="Mes")recordMonthLabel(part)else part.toString())},onClick={onChange(part);expanded=false})}
        }
    }
}
