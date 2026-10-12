package com.tuempresa.inventariovial.field

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.ui.theme.*

enum class ConditionTone { FAVORABLE, INTERMEDIATE, UNFAVORABLE, UNSPECIFIED }
data class ConditionOption(val code:String,val label:String,val tone:ConditionTone,val description:String="")
object ContractConditionCatalog {
    /** Meaning comes from the official label, never from its numeric code. */
    fun options(labels:List<String>) = labels.map { text ->
        val code=text.substringBefore(" - ");val label=text.substringAfter(" - ")
        val tone=when {
            label.startsWith("Buena",true) || label.startsWith("Bueno",true) -> ConditionTone.FAVORABLE
            label.startsWith("Regular",true) -> ConditionTone.INTERMEDIATE
            label.startsWith("Mala",true) || label.startsWith("Malo",true) -> ConditionTone.UNFAVORABLE
            else -> ConditionTone.UNSPECIFIED
        }
        ConditionOption(code,label,tone)
    }
}

@Composable fun CatalogConditionSelector(title:String,selected:String,labels:List<String>,enabled:Boolean=true,onSelect:(String)->Unit) =
    VisualConditionSelector(title,selected,ContractConditionCatalog.options(labels),enabled,onSelect)

@Composable fun VisualConditionSelector(title:String,selected:String,options:List<ConditionOption>,enabled:Boolean=true,onSelect:(String)->Unit) {
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(title,style=MaterialTheme.typography.titleLarge)
        options.forEach { option ->
            val active=selected==option.code
            val color=when(option.tone) {
                ConditionTone.FAVORABLE->FieldSuccess
                ConditionTone.INTERMEDIATE->FieldPending
                ConditionTone.UNFAVORABLE->MaterialTheme.colorScheme.error
                ConditionTone.UNSPECIFIED->MaterialTheme.colorScheme.outline
            }
            OutlinedCard(onClick={onSelect(option.code)},enabled=enabled,
                modifier=Modifier.fillMaxWidth().heightIn(min=64.dp).testTag("condition-$title-${option.code}").semantics { this.selected=active },
                border=BorderStroke(if(active)2.dp else 1.dp,if(active)color else MaterialTheme.colorScheme.outlineVariant),
                colors=CardDefaults.outlinedCardColors(containerColor=if(active)color.copy(alpha=0.12f) else MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text("${if(active) "✓" else "○"} ${option.code} · ${option.label}${if(active) " · Seleccionado" else ""}",
                        style=MaterialTheme.typography.titleMedium,color=color)
                    if(option.description.isNotBlank())Text(option.description,style=MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun ConditionSelector(title:String,selected:String,descriptions:Map<String,String>,onSelect:(String)->Unit) {
    // Existing SIC-18/19/20 contract explicitly defines this three-code scale.
    val functional=title.contains("funcional",ignoreCase=true)
    VisualConditionSelector(title,selected,listOf(
        ConditionOption("1",if(functional) "BUENA" else "BUENO",ConditionTone.FAVORABLE,descriptions["1"].orEmpty()),
        ConditionOption("2","REGULAR",ConditionTone.INTERMEDIATE,descriptions["2"].orEmpty()),
        ConditionOption("3",if(functional) "MALA" else "MALO",ConditionTone.UNFAVORABLE,descriptions["3"].orEmpty())),onSelect=onSelect)
}
