package com.tuempresa.inventariovial.scap.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.scap.calculator.*
import com.tuempresa.inventariovial.scap.catalog.ScapCatalog
import com.tuempresa.inventariovial.scap.data.*
import com.tuempresa.inventariovial.scap.domain.*
import java.util.Locale
import java.util.UUID

private val romanGroups=listOf("I","II","III","IV","V")

/** F.1 mirrors the SCAP relation of bridge elements. Evaluation percentages live in G.1. */
@Composable
fun ScapElementPresencePanel(s:ScapInspectionSnapshot,c:ScapController,catalog:ScapCatalog,editable:Boolean) {
    var query by rememberSaveable {mutableStateOf("")}
    var searching by rememberSaveable {mutableStateOf(false)}
    var absent by remember {mutableStateOf<String?>(null)}
    var manualOpen by remember {mutableStateOf(false)}
    val codes=ScapPresentation.visibleElements(s,catalog)
    val list=rememberLazyListState();val destination=LocalScapDestination.current
    val indices=mutableMapOf<String,Int>();var position=1
    ScapPresentation.groups.forEach {group->
        position++
        codes.filter{code->(catalog.element(code)?.group ?: s.elements.find{it.element.elementCode==code}?.element?.group)==group}.forEach{code->
            s.elements.find{it.element.elementCode==code}?.element?.let{e->indices[e.id]=position;s.defects.filter{it.elementCode==code}.forEach{indices[it.id]=position}}
            position++
        }
    }
    position++
    s.defects.filter{it.elementCode==null}.forEach{indices[it.id]=position++}
    LaunchedEffect(destination){destination?.second?.takeIf{it.subsection=="F1"}?.let{route->indices[route.repeatableItemId]?.let{list.scrollToItem(it)}}}

    LazyColumn(Modifier.fillMaxSize(),state=list,contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            Text("F.1 · CONDICIÓN GLOBAL DEL PUENTE",style=MaterialTheme.typography.headlineSmall)
            Text("Describe la condición observada de cada elemento. G.1 muestra todos salvo los marcados No aplica.")
        }
        ScapPresentation.groups.forEachIndexed {index,group->
            item {Text("${romanGroups[index]}. $group",style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.primary)}
            items(codes.filter {code->(catalog.element(code)?.group ?: s.elements.find{it.element.elementCode==code}?.element?.group)==group},key={"element:$it"}) {code->
                val name=catalog.element(code)?.name ?: s.elements.first{it.element.elementCode==code}.element.description
                ScapElementCard("$code · $name") {
                    val entry=s.elements.find{it.element.elementCode==code}
                    entry?.let{ScapFieldTarget(it.element.id,"presence"){Text("Estado: ${ScapPresentation.presence(s,code)}")}}
                    s.elements.find{it.element.elementCode==code}?.element?.let {e->
                        val values=scapValues(s,c,e.id)
                        ScapFieldTarget(e.id,"customCode"){OutlinedTextField(values["customCode"] ?: e.elementCode,{c.edit(s.inspection.id,e.id,"customCode",it)},label={Text("Código para F.1")},enabled=editable,modifier=Modifier.fillMaxWidth().scapFocus())}
                        ScapFieldTarget(e.id,"customName"){OutlinedTextField(values["customName"] ?: e.description,{c.edit(s.inspection.id,e.id,"customName",it)},label={Text("Nombre del elemento")},enabled=editable,modifier=Modifier.fillMaxWidth().scapFocus())}
                        ScapFieldTarget(e.id,"globalDescription"){OutlinedTextField(values["globalDescription"].orEmpty(),{c.edit(s.inspection.id,e.id,"globalDescription",it)},label={Text("Descripción de condición global")},enabled=editable,modifier=Modifier.fillMaxWidth().scapFocus(),minLines=3)}
                        s.defects.filter{it.elementCode==code}.forEach{d->key(d.id){ScapDefectFields(s,c,catalog,d,editable)}}
                        AddDefectButton(s,c,code,editable)
                        if(values["elementSource"]=="MANUAL") RemoveManualElement(s,c,e.id,editable)
                    }
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(enabled=editable,onClick={c.submit {it.selectElement(s.inspection.id,code,true)}}) {Text("Presente")}
                        TextButton(enabled=editable,onClick={absent=code}) {Text("No aplica")}
                    }
                }
            }
        }
        item {Text("Observaciones generales",style=MaterialTheme.typography.titleMedium);AddDefectButton(s,c,null,editable)}
        items(s.defects.filter{it.elementCode==null},key={"general:${it.id}"}){d->ScapElementCard("Observación general"){ScapDefectFields(s,c,catalog,d,editable)}}
        item {
            OutlinedButton(enabled=editable,onClick={manualOpen=true}){Text("+ Añadir elemento manual")}
            OutlinedButton(enabled=editable,onClick={searching=!searching}) {Text("+ BUSCAR / AÑADIR OTRO ELEMENTO")}
            if(searching) OutlinedTextField(query,{query=it},label={Text("Buscar en los ${catalog.elements.size} elementos por código o nombre")},modifier=Modifier.fillMaxWidth())
        }
        if(searching) items(catalog.elements.filter {it.code.contains(query,true)||it.name.contains(query,true)},key={"search:${it.code}"}) {e->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("${e.code} · ${e.name}",Modifier.weight(1f))
                OutlinedButton(enabled=editable && e.code !in codes,onClick={c.submit {it.addElementForReview(s.inspection.id,e.code)}}) {Text(if(e.code in codes) "Visible" else "Añadir")}
            }
        }
    }
    if(manualOpen)ManualElementDialog(s,c,catalog){manualOpen=false}
    absent?.let {code->AlertDialog(onDismissRequest={absent=null},title={Text("Marcar $code como No aplica")},
        text={Text("Se conservarán metrados, porcentajes, observaciones y fotos. El elemento no participará en el cálculo mientras esté marcado No aplica.")},
        confirmButton={TextButton(onClick={c.submit {it.selectElement(s.inspection.id,code,false)};absent=null}){Text("Marcar y conservar")}},
        dismissButton={TextButton(onClick={absent=null}){Text("Cancelar")}})}
}

/** F.2 is descriptive: observed condition, anomalies/failures and associated evidence. */
@Composable
fun ScapConditionGlobalPanel(s:ScapInspectionSnapshot,c:ScapController,catalog:ScapCatalog,editable:Boolean,onPhoto:(String?)->Unit) {
    val present=ScapPresentation.visibleElements(s,catalog).filter {code->s.elements.any {it.element.elementCode==code && it.element.isPresent}}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            Text("F.2 · CONDICIÓN GLOBAL DEL PUENTE",style=MaterialTheme.typography.headlineSmall)
            Text("Registra aquí las observaciones, fallas, ubicación y evidencia fotográfica de los elementos confirmados en F.1.")
            if(present.isEmpty()) Text("No hay elementos marcados como presentes. Vuelve a F.1 para seleccionarlos.",color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ScapPresentation.groups.forEachIndexed {index,group->
            val groupCodes=present.filter {code->(catalog.element(code)?.group ?: s.elements.find{it.element.elementCode==code}?.element?.group)==group}
            if(groupCodes.isNotEmpty()) {
                item {Text("${romanGroups[index]}. $group",style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.primary)}
                items(groupCodes,key={"condition:$it"}) {code->
                    val name=catalog.element(code)?.name ?: s.elements.first{it.element.elementCode==code}.element.description
                    ScapElementCard("$code · $name") {
                        val defects=s.defects.filter {it.elementCode==code}.sortedBy {it.createdAt}
                        if(defects.isEmpty()) Text("Sin observaciones/fallas registradas.")
                        defects.forEach {defect->key(defect.id) {ScapDefectFields(s,c,catalog,defect,editable)}}
                        val photos=s.photos.count {photo->photo.scapElementCode==code || s.defects.any {d->d.elementCode==code && d.photoId==photo.id}}
                        Text("Fotos asociadas: $photos")
                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(enabled=editable,onClick={onPhoto(code)}) {Text("+ Añadir foto")}
                            AddDefectButton(s,c,code,editable)
                        }
                    }
                }
            }
        }
        item {
            Text("OBSERVACIONES GENERALES",style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.primary)
            AddDefectButton(s,c,null,editable)
        }
        items(s.defects.filter {it.elementCode==null || catalog.element(it.elementCode)==null},key={"general:${it.id}"}) {
            ScapElementCard("Observación general") {ScapDefectFields(s,c,catalog,it,editable)}
        }
    }
}

/** G.1 mirrors the statistical condition sheet: metrado, unit/factor and six percentages. */
@Composable
fun ScapStatisticsPanel(s:ScapInspectionSnapshot,c:ScapController,catalog:ScapCatalog,editable:Boolean) {
    var query by rememberSaveable(s.inspection.id){mutableStateOf("")}
    var catalogOpen by rememberSaveable(s.inspection.id){mutableStateOf(false)}
    var manualOpen by remember {mutableStateOf(false)}
    val present=ScapElementView.applicable(s).sortedWith(compareBy({ScapPresentation.groups.indexOf(it.element.group)},{it.element.elementCode}))
    val list=rememberLazyListState();val destination=LocalScapDestination.current
    val indices=mutableMapOf<String,Int>();var position=1+(if(catalogOpen)catalog.elements.count{it.code.contains(query,true)||it.name.contains(query,true)} else 0)
    ScapPresentation.groups.forEach{group->val rows=present.filter{it.element.group==group};if(rows.isNotEmpty()){position++;rows.forEach{indices[it.element.id]=position++}}}
    LaunchedEffect(destination){destination?.second?.takeIf{it.subsection=="G1"}?.let{route->indices[route.repeatableItemId]?.let{list.scrollToItem(it)}}}
    val result=remember(s){ScapConditionCalculator.calculate(present.map {e->
        ScapCalculationInput(e.element.elementCode,ScapElementView.factor(s,e.element),e.condition?.let{ScapPercentages.values(it)} ?: List(6){null})
    })}
    LazyColumn(Modifier.fillMaxSize(),state=list,contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            Text("G.1 · CONDICIÓN ESTADÍSTICA DEL PUENTE",style=MaterialTheme.typography.headlineSmall)
            Text("Todos los elementos de F.1 que aplican. Las celdas vacías permanecen vacías en Excel; para el cálculo se interpretan como 0. Una suma distinta de 100% se advierte sin bloquear el guardado ni la entrega.")
            if(present.isEmpty()) Text("No hay elementos presentes para evaluar.")
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled=editable,onClick={catalogOpen=!catalogOpen}){Text("Elementos de la plantilla (${catalog.elements.size})")}
                OutlinedButton(enabled=editable,onClick={manualOpen=true}){Text("+ Agregar elemento")}
            }
            if(catalogOpen) OutlinedTextField(query,{query=it},label={Text("Buscar código o descripción")},modifier=Modifier.fillMaxWidth())
        }
        if(catalogOpen) items(catalog.elements.filter{it.code.contains(query,true)||it.name.contains(query,true)},key={"option:${it.code}"}) {option->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("${option.code} · ${option.name}",Modifier.weight(1f))
                OutlinedButton(enabled=editable,onClick={c.submit{it.selectElement(s.inspection.id,option.code,true)}}){Text(if(present.any{it.element.elementCode==option.code}) "Presente" else "Añadir")}
            }
        }
        ScapPresentation.groups.forEachIndexed {index,group->
            val rows=present.filter {it.element.group==group}
            if(rows.isNotEmpty()) {
                item {Text("${romanGroups[index]}. $group",style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.primary)}
                items(rows,key={"stat:${it.element.id}"}) {entry->
                    ScapElementCard("${entry.element.elementCode} · ${entry.element.description}") {
                        ElementConditionFields(s,c,catalog,entry,editable)
                        TextButton(enabled=editable,onClick={c.submit{it.selectElement(s.inspection.id,entry.element.elementCode,false)}}){Text("No aplica · conservar datos")}
                    }
                }
            }
        }
        item {
            HorizontalDivider()
            Text("CONDICIÓN ESTADÍSTICA DEL PUENTE",style=MaterialTheme.typography.titleLarge)
            if(result.value!=null) Text("${String.format(Locale.US,"%.3f",result.value)} · ${result.classification}",style=MaterialTheme.typography.headlineMedium)
            else Text("Condición estadística pendiente por datos incompletos.")
            Text("Resultado calculado con la misma lógica de la hoja G.1 de la plantilla SCAP. Requiere revisión del ingeniero.",style=MaterialTheme.typography.bodySmall)
        }
    }
    if(manualOpen) ManualElementDialog(s,c,catalog){manualOpen=false}
}

/** G.2 maps directly to the three observation/recommendation blocks in CONCLUSIONES. */
@Composable
fun ScapRecommendationsPanel(s:ScapInspectionSnapshot,c:ScapController,editable:Boolean) {
    val values=scapValues(s,c,"G2")
    val groups=listOf(
        Triple("ACCIONES NORMATIVAS","g2NormativeObservation","g2NormativeRecommendation"),
        Triple("ACCIONES PREVENTIVAS","g2PreventiveObservation","g2PreventiveRecommendation"),
        Triple("ACCIONES EJECUTIVAS","g2ExecutiveObservation","g2ExecutiveRecommendation")
    )
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            Text("G.2 · OBSERVACIONES Y RECOMENDACIONES",style=MaterialTheme.typography.headlineSmall)
            Text("Estos textos se exportan a la hoja CONCLUSIONES. No se reutilizan los textos del ejemplo Agua Blanca.")
        }
        items(groups,key={it.first}) {(title,obsKey,recKey)->
            ScapElementCard(title) {
                OutlinedTextField(values[obsKey].orEmpty(),{c.edit(s.inspection.id,"G2",obsKey,it)},label={Text("Observaciones")},
                    enabled=editable,modifier=Modifier.fillMaxWidth(),minLines=3)
                OutlinedTextField(values[recKey].orEmpty(),{c.edit(s.inspection.id,"G2",recKey,it)},label={Text("Recomendaciones")},
                    enabled=editable,modifier=Modifier.fillMaxWidth(),minLines=3)
            }
        }
    }
}

/** Kept as a compatibility entry point for older callers/tests. */
@Composable
fun ScapElements(s:ScapInspectionSnapshot,c:ScapController,catalog:ScapCatalog,editable:Boolean,onPhoto:(String?)->Unit) =
    ScapConditionGlobalPanel(s,c,catalog,editable,onPhoto)

@Composable
private fun ElementConditionFields(s:ScapInspectionSnapshot,c:ScapController,catalog:ScapCatalog,entry:ScapElementWithCondition,editable:Boolean) {
    val e=entry.element
    val values=scapValues(s,c,e.id)
    val p=(0..5).map {i->if(values.containsKey("percent$i")) ScapNumbers.decimal(values["percent$i"]) else entry.condition?.let {ScapPercentages.values(it)[i]}}
    Text("Unidad: ${e.unit.ifBlank{"—"}} · Factor de importancia: ${values["importanceFactor"]?.ifBlank{"—"} ?: e.importanceFactor}")
    val manual=values["elementSource"]=="MANUAL"
    ScapFieldTarget(e.id,"exportCode"){OutlinedTextField(values["exportCode"] ?: e.elementCode,{c.edit(s.inspection.id,e.id,"exportCode",it)},label={Text("Código para G.1")},enabled=editable,modifier=Modifier.fillMaxWidth().scapFocus())}
    ScapFieldTarget(e.id,"customName"){OutlinedTextField(values["customName"] ?: e.description,{c.edit(s.inspection.id,e.id,"customName",it)},label={Text("Descripción / nombre del elemento")},enabled=editable,modifier=Modifier.fillMaxWidth().scapFocus())}
    if(manual) Text("Origen: Manual")
    ScapFieldTarget(e.id,"unit"){ScapChoice("Unidad",values["unit"] ?: e.unit,catalog.options("units"),editable){c.edit(s.inspection.id,e.id,"unit",it)}}
    if(catalog.bridgeType!=null) ScapFieldTarget(e.id,"importanceFactor"){ScapChoice("Factor de importancia",values["importanceFactor"] ?: e.importanceFactor.toString(),catalog.options("importanceFactors"),editable){c.edit(s.inspection.id,e.id,"importanceFactor",it)}}
    ScapFieldTarget(e.id,"quantity"){OutlinedTextField(values["quantity"] ?: e.quantity?.toString().orEmpty(),{c.edit(s.inspection.id,e.id,"quantity",it)},
        label={Text("Metrado (${e.unit})")},enabled=editable,modifier=Modifier.fillMaxWidth().scapFocus(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal))}
    (0..5).chunked(2).forEach {levels->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {levels.forEach {level->
        val value=values["percent$level"] ?: p[level]?.toString().orEmpty()
        val label=when(level){0->"0 · Muy Bueno";1->"1 · Bueno";2->"2 · Satisfactorio";3->"3 · Marginal";4->"4 · Pobre";else->"5 · Muy Pobre"}
        Column(Modifier.weight(1f)){ScapFieldTarget(e.id,"percent$level"){OutlinedTextField(value,{c.edit(s.inspection.id,e.id,"percent$level",it)},label={Text("$label (%)")},enabled=editable,
            modifier=Modifier.fillMaxWidth().scapFocus(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),
            isError=value.isNotBlank() && ScapNumbers.decimal(value)?.let {it in 0.0..100.0}!=true)}}
    }}}
    Text("Suma: ${p.filterNotNull().sum()}% · ${p.count {it!=null}}/6 niveles")
    ScapPercentages.error(p)?.let {Text(it,color=MaterialTheme.colorScheme.error)}
    if(manual)RemoveManualElement(s,c,e.id,editable)
}

@Composable private fun RemoveManualElement(s:ScapInspectionSnapshot,c:ScapController,id:String,editable:Boolean) {
    var confirm by remember{mutableStateOf(false)}
    TextButton(enabled=editable,onClick={confirm=true}){Text("Eliminar elemento manual")}
    if(confirm)AlertDialog(onDismissRequest={confirm=false},title={Text("Eliminar elemento manual")},text={Text("Se conservarán las fotografías y observaciones asociadas.")},
        confirmButton={TextButton(onClick={confirm=false;c.submit{it.removeManualElement(s.inspection.id,id)}}){Text("Eliminar")}},dismissButton={TextButton(onClick={confirm=false}){Text("Cancelar")}})
}

@Composable
private fun ManualElementDialog(s:ScapInspectionSnapshot,c:ScapController,catalog:ScapCatalog,onDismiss:()->Unit) {
    var code by remember{mutableStateOf("")};var description by remember{mutableStateOf("")}
    var quantity by remember{mutableStateOf("")};var unit by remember{mutableStateOf("")};var factor by remember{mutableStateOf("")}
    val duplicate=code.isNotBlank() && (catalog.element(code.trim())!=null || s.elements.any{it.element.elementCode==code.trim()})
    AlertDialog(onDismissRequest=onDismiss,title={Text("Agregar elemento manual · F.1 / G.1")},
        text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(code,{code=it},label={Text("Código")},isError=duplicate,modifier=Modifier.fillMaxWidth())
            if(duplicate) Text("El código ya está registrado; selecciona el elemento existente.",color=MaterialTheme.colorScheme.error)
            OutlinedTextField(description,{description=it},label={Text("Descripción")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(quantity,{quantity=it},label={Text("Metrado")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.fillMaxWidth())
            ScapChoice("Unidad",unit,catalog.options("units")){unit=it}
            ScapChoice("Factor de importancia",factor,catalog.options("importanceFactors")){factor=it}
            Text("Puedes guardar el elemento incompleto y completar su calificación en G.1.")
        }},confirmButton={TextButton(enabled=code.isNotBlank() && !duplicate,onClick={c.submit{it.addManualElement(s.inspection.id,code,description,quantity,unit,factor)};onDismiss()}){Text("Agregar elemento")}},
        dismissButton={TextButton(onClick=onDismiss){Text("Volver")}})
}

@Composable
private fun AddDefectButton(s:ScapInspectionSnapshot,c:ScapController,code:String?,editable:Boolean) {
    OutlinedButton(enabled=editable,onClick={c.submit {it.saveDefect(s.inspection.id,ScapDefectEntity(
        UUID.randomUUID().toString(),s.inspection.id,code,"","",null,false,true,System.currentTimeMillis()))}}) {Text("+ Añadir observación / falla")}
}

@Composable
internal fun ScapDefectFields(s:ScapInspectionSnapshot,c:ScapController,catalog:ScapCatalog,d:ScapDefectEntity,editable:Boolean) {
    var description by remember(d.id) {mutableStateOf(d.description)}
    var location by remember(d.id) {mutableStateOf(d.locationDescription)}
    var removing by remember {mutableStateOf(false)}
    ScapFieldTarget(d.id,"defectDescription"){OutlinedTextField(description,{description=it;c.submit {r->r.setDefectField(s.inspection.id,d.id,"description",it,d)}},label={Text("Descripción observada")},enabled=editable,modifier=Modifier.fillMaxWidth().scapFocus(),minLines=2)}
    OutlinedTextField(location,{location=it;c.submit {r->r.setDefectField(s.inspection.id,d.id,"locationDescription",it,d)}},label={Text("Ubicación")},enabled=editable,modifier=Modifier.fillMaxWidth())
    if(d.elementCode==null) ScapChoice("Elemento SCAP (opcional)","",catalog.elements.map {"${it.code} · ${it.name}"},editable) {v->c.submit {it.setDefectField(s.inspection.id,d.id,"elementCode",v.substringBefore(" · "))}}
    fun label(id:String)=s.photos.find {it.id==id}?.let {"Foto ${it.photoIndex} · ${ScapPhotoCategories.label(it.photoCategory)}"}.orEmpty()
    ScapChoice("Foto asociada",d.photoId?.let(::label).orEmpty(),s.photos.map {label(it.id)},editable) {v->c.submit {it.setDefectField(s.inspection.id,d.id,"photoId",s.photos.find {label(it.id)==v}?.id.orEmpty(),d)}}
    if(d.aiSuggested) Row {Checkbox(d.validatedByUser,{v->c.submit {it.setDefectField(s.inspection.id,d.id,"validatedByUser",v.toString())}},enabled=editable);Text("Validado por el inspector")}
    if(s.defects.any {it.id==d.id}) TextButton(enabled=editable,onClick={removing=true}) {Text("Eliminar observación")}
    if(removing) AlertDialog(onDismissRequest={removing=false},title={Text("Eliminar observación")},text={Text("La fotografía se conservará.")},
        confirmButton={TextButton(onClick={c.submit {it.removeDefect(s.inspection.id,d.id);description="";location=""};removing=false}){Text("Eliminar")}},dismissButton={TextButton(onClick={removing=false}){Text("Cancelar")}})
}
