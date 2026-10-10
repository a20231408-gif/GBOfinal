from pathlib import Path
root = Path(__file__).resolve().parents[1]
path = root/'app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapElements.kt'
s=path.read_text(encoding='utf-8')
s=s.replace('import androidx.compose.foundation.lazy.items','import androidx.compose.foundation.lazy.items\nimport androidx.compose.foundation.lazy.rememberLazyListState')
s=s.replace('    val codes=ScapPresentation.visibleElements(s,catalog)','''    var manualOpen by remember {mutableStateOf(false)}
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
    LaunchedEffect(destination){destination?.second?.takeIf{it.subsection=="F1"}?.let{route->indices[route.repeatableItemId]?.let{list.scrollToItem(it)}}}
''')
# Only the first list belongs to F.1.
s=s.replace('LazyColumn(Modifier.fillMaxSize(),contentPadding=', 'LazyColumn(Modifier.fillMaxSize(),state=list,contentPadding=',1)
old='''                    Text("Estado: ${ScapPresentation.presence(s,code)}")'''
s=s.replace(old,'''                    val entry=s.elements.find{it.element.elementCode==code}
                    entry?.let{ScapFieldTarget(it.element.id,"presence"){Text("Estado: ${ScapPresentation.presence(s,code)}")}}''',1)
s=s.replace('''                        OutlinedTextField(values["customName"] ?: e.description,{c.edit(s.inspection.id,e.id,"customName",it)},label={Text("Nombre del elemento")},enabled=editable,modifier=Modifier.fillMaxWidth())''','''                        ScapFieldTarget(e.id,"customName"){OutlinedTextField(values["customName"] ?: e.description,{c.edit(s.inspection.id,e.id,"customName",it)},label={Text("Nombre del elemento")},enabled=editable,modifier=Modifier.fillMaxWidth().scapFocus())}''',1)
s=s.replace('''                        OutlinedTextField(values["globalDescription"].orEmpty(),{c.edit(s.inspection.id,e.id,"globalDescription",it)},label={Text("Descripción de condición global")},enabled=editable,modifier=Modifier.fillMaxWidth(),minLines=3)''','''                        ScapFieldTarget(e.id,"globalDescription"){OutlinedTextField(values["globalDescription"].orEmpty(),{c.edit(s.inspection.id,e.id,"globalDescription",it)},label={Text("Descripción de condición global")},enabled=editable,modifier=Modifier.fillMaxWidth().scapFocus(),minLines=3)}
                        s.defects.filter{it.elementCode==code}.forEach{d->key(d.id){ScapDefectFields(s,c,catalog,d,editable)}}
                        AddDefectButton(s,c,code,editable)''',1)
s=s.replace('''            OutlinedButton(enabled=editable,onClick={searching=!searching})''','''            OutlinedButton(enabled=editable,onClick={manualOpen=true}){Text("+ Añadir elemento manual")}
            OutlinedButton(enabled=editable,onClick={searching=!searching})''',1)
s=s.replace('''    absent?.let {code->''','''    if(manualOpen)ManualElementDialog(s,c,catalog){manualOpen=false}
    absent?.let {code->''',1)
marker='''    val result=remember(s){ScapConditionCalculator.calculate(present.map {e->'''
s=s.replace(marker,'''    val list=rememberLazyListState();val destination=LocalScapDestination.current
    val indices=mutableMapOf<String,Int>();var position=1+(if(catalogOpen)catalog.elements.count{it.code.contains(query,true)||it.name.contains(query,true)} else 0)
    ScapPresentation.groups.forEach{group->val rows=present.filter{it.element.group==group};if(rows.isNotEmpty()){position++;rows.forEach{indices[it.element.id]=position++}}}
    LaunchedEffect(destination){destination?.second?.takeIf{it.subsection=="G1"}?.let{route->indices[route.repeatableItemId]?.let{list.scrollToItem(it)}}}
'''+marker)
start=s.index('fun ScapStatisticsPanel')
i=s.index('LazyColumn(Modifier.fillMaxSize(),contentPadding=',start)
s=s[:i]+s[i:].replace('LazyColumn(Modifier.fillMaxSize(),contentPadding=','LazyColumn(Modifier.fillMaxSize(),state=list,contentPadding=',1)
# Field targets wrap editable G.1 fields, including non-text choices.
start=s.index('private fun ElementConditionFields');end=s.index('@Composable private fun RemoveManualElement',start)
part=s[start:end]
part=part.replace('''    if(manual) {
        Text("Origen: Manual")
        ScapChoice("Unidad",values["unit"] ?: e.unit,catalog.options("units"),editable){c.edit(s.inspection.id,e.id,"unit",it)}
    }''','''    if(manual) Text("Origen: Manual")
    ScapFieldTarget(e.id,"unit"){ScapChoice("Unidad",values["unit"] ?: e.unit,catalog.options("units"),editable){c.edit(s.inspection.id,e.id,"unit",it)}}''')
part=part.replace('''    if(catalog.bridgeType!=null) ScapChoice("Factor de importancia",values["importanceFactor"] ?: e.importanceFactor.toString(),catalog.options("importanceFactors"),editable){c.edit(s.inspection.id,e.id,"importanceFactor",it)}''','''    if(catalog.bridgeType!=null) ScapFieldTarget(e.id,"importanceFactor"){ScapChoice("Factor de importancia",values["importanceFactor"] ?: e.importanceFactor.toString(),catalog.options("importanceFactors"),editable){c.edit(s.inspection.id,e.id,"importanceFactor",it)}}''')
for key in ['exportCode','customName']:
    line=next(x for x in part.splitlines() if 'OutlinedTextField(values["'+key+'"]' in x)
    part=part.replace(line,'    ScapFieldTarget(e.id,"'+key+'"){'+line.strip().replace('Modifier.fillMaxWidth()','Modifier.fillMaxWidth().scapFocus()')+'}')
part=part.replace('''    OutlinedTextField(values["quantity"]''','''    ScapFieldTarget(e.id,"quantity"){OutlinedTextField(values["quantity"]''')
part=part.replace('''modifier=Modifier.fillMaxWidth(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal))''','''modifier=Modifier.fillMaxWidth().scapFocus(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal))}''')
# A weighted target gives BringIntoView the actual field bounds.
part=part.replace('''        OutlinedTextField(value,''','''        Column(Modifier.weight(1f)){ScapFieldTarget(e.id,"percent$level"){OutlinedTextField(value,''')
part=part.replace('''modifier=Modifier.weight(1f),keyboardOptions''','''modifier=Modifier.fillMaxWidth().scapFocus(),keyboardOptions''')
part=part.replace('''isError=value.isNotBlank() && ScapNumbers.decimal(value)?.let {it in 0.0..100.0}!=true)''','''isError=value.isNotBlank() && ScapNumbers.decimal(value)?.let {it in 0.0..100.0}!=true)}}''')
s=s[:start]+part+s[end:]
s=s.replace('Agregar elemento manual · G.1','Agregar elemento manual · F.1 / G.1')
s=s.replace('''    OutlinedTextField(description,{description=it;c.submit {r->r.setDefectField(s.inspection.id,d.id,"description",it,d)}},label={Text("Descripción observada")},enabled=editable,modifier=Modifier.fillMaxWidth(),minLines=2)''','''    ScapFieldTarget(d.id,"defectDescription"){OutlinedTextField(description,{description=it;c.submit {r->r.setDefectField(s.inspection.id,d.id,"description",it,d)}},label={Text("Descripción observada")},enabled=editable,modifier=Modifier.fillMaxWidth().scapFocus(),minLines=2)}''')
path.write_text(s,encoding='utf-8')
