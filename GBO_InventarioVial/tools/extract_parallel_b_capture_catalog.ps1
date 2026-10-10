# Read-only JSON extraction; run from the repository root after field-map extraction.
$legacyFields=Get-Content app/src/main/assets/scap_fields.json -Raw|ConvertFrom-Json
$legacyOptions=Get-Content app/src/main/assets/scap_options.json -Raw|ConvertFrom-Json
function Shift($address){ return [regex]::Replace($address,'\d+', {param($match) ([int]$match.Value+1).ToString()}) }
foreach($type in 'slab','modular') {
 $mapPath=if($type -eq 'slab'){'docs/SCAP_LOSA_FIELD_MAP.json'}else{'docs/SCAP_MODULAR_FIELD_MAP.json'}
 $map=Get-Content $mapPath -Raw|ConvertFrom-Json
 $sheet=$map.sheets|Where-Object name -eq 'A-D.- FICHA'; $cells=@{}; foreach($c in $sheet.cells){$cells[$c.cell]=$c.value}
 $aux=$map.sheets|Where-Object name -eq 'AUXILIAR'; $auxCells=@{}; foreach($c in $aux.cells){$auxCells[$c.cell]=$c.value}
 $elements=@(); foreach($c in $aux.cells){if($c.cell -match '^C(\d+)$' -and $c.value -is [ValueType] -and [int]$Matches[1] -ge 5){$r=[int]$Matches[1]; $elements+=@{code=[string]$c.value;name=([string]$auxCells["D$r"]).Trim();unit=([string]$auxCells["E$r"]).Trim();importanceFactor=$auxCells["F$r"];group=([string]$auxCells["G$r"]).Trim();source="AUXILIAR!C${r}:G${r}"}}}
 $fields=@();foreach($field in $legacyFields.fields){if($field.key -eq 'segment'){continue}; $clone=$field|ConvertTo-Json -Depth 6|ConvertFrom-Json; $clone.labelCell=Shift $clone.labelCell; $clone.valueCell=Shift $clone.valueCell; $clone.source='A-D.- FICHA!'+$clone.valueCell; if($clone.key -eq 'utmEasting'){$clone.key='latitude';$clone.label='Latitud';$clone.kind='LATITUDE'};if($clone.key -eq 'utmNorthing'){$clone.key='longitude';$clone.label='Longitud';$clone.kind='LONGITUDE'}; $fields+=$clone}
 $options=@{};foreach($property in $legacyOptions.catalogs.PSObject.Properties){$range=($property.Value.source -replace '^A-D.- FICHA!','') -replace ' .*','';$range=Shift $range; if($range -match '^([A-Z]+)(\d+):([A-Z]+)(\d+)$' -and $Matches[1] -eq $Matches[3]){$col=$Matches[1];$values=@(); foreach($r in ([int]$Matches[2]..[int]$Matches[4])){if($cells.ContainsKey("$col$r")){$v=([string]$cells["$col$r"]).Trim();if($v -and $v -notin $values){$values+=$v}}};$options[$property.Name]=@{options=@($values);source='A-D.- FICHA!'+$range}}else{$options[$property.Name]=@{options=@();source='A-D.- FICHA!'+$range}}}
 $types=@{};foreach($property in $legacyOptions.typesByCategory.PSObject.Properties){$range=Shift ($property.Value.source -replace '^A-D.- FICHA!','');$range -match '^([A-Z]+)(\d+):[A-Z]+(\d+)$'|Out-Null;$col=$Matches[1];$values=@();foreach($r in ([int]$Matches[2]..[int]$Matches[3])){if($cells.ContainsKey("$col$r")){$values+=([string]$cells["$col$r"]).Trim()}};$types[$property.Name]=@{options=$values;source='A-D.- FICHA!'+$range}}
 $g=$map.sheets|Where-Object name -eq 'G.- CONDICION ESTADISTICA';$defaults=@($g.cells|Where-Object {$_.cell -match '^A(1[4-9]|2[0-9]|3[0-9]|4[0-4])$' -and $_.value -is [ValueType]}|ForEach-Object {[string]$_.value})
 $options['units']=@{options=@($elements.unit|Select-Object -Unique);source="AUXILIAR!E5:E$($aux.maxRow)"};$options['importanceFactors']=@{options=@($elements.importanceFactor|Sort-Object -Unique|ForEach-Object {[string]$_});source="AUXILIAR!F5:F$($aux.maxRow)"}
 $corrections=@{fordDistanceM='G340';fordPeriod='G342';fordMinDepthM='G344';fordSoil='G346';existingDetour='G348';detourNeeded='G350';parallelLengthM='L342';parallelSubstructure='L344';parallelType='L346';profileReference='N450';scourProtectionType='L478'}
 foreach($field in $fields){if($corrections.ContainsKey($field.key)){$field.valueCell=$corrections[$field.key];$field.source='A-D.- FICHA!'+$field.valueCell}}
 $gCells=@{};foreach($cell in $g.cells){$gCells[$cell.cell]=$cell.value}
 foreach($row in 14..44){$code=[string]$gCells["A$row"];if($code -in $defaults){$item=$elements|Where-Object code -eq $code;$item.unit=([string]$gCells["E$row"]).Trim();$item.importanceFactor=$gCells["F$row"];$item.source="G.- CONDICION ESTADISTICA!A${row}:F${row}"}}
 $options['units'].options=@($options['units'].options+$elements.unit|Select-Object -Unique)
 $options['units'].source="AUXILIAR!E5:E$($aux.maxRow) y G.- CONDICION ESTADISTICA!E14:E44"
 $numericKinds=@('YEAR','INTEGER','DECIMAL','SIGNED_DECIMAL','PERCENT','DIMENSION_RANGE')
 foreach($field in $fields){
  if($field.kind -notin $numericKinds){continue}
  $observed=$cells[$field.valueCell]
  if($observed -isnot [string] -or -not $observed.Trim() -or $observed.StartsWith('=')){continue}
  $numeric=0.0
  if([double]::TryParse($observed.Trim(),[Globalization.NumberStyles]::Float,[Globalization.CultureInfo]::InvariantCulture,[ref]$numeric)){continue}
  if($field.kind -eq 'DIMENSION_RANGE' -and $observed.Trim() -match '^\d+(\.\d+)?\s*-\s*\d+(\.\d+)?\s*m?$'){continue}
  $field|Add-Member -NotePropertyName numericKind -NotePropertyValue $field.kind -Force
  $field.kind='NUMERIC_OR_SOURCE_STATUS'
  $name=$field.key+$field.section+$field.owner+'ObservedStatus'
  $field|Add-Member -NotePropertyName catalog -NotePropertyValue $name -Force
  $options[$name]=@{options=@($observed.Trim());source=('A-D.- FICHA!'+$field.valueCell+' (valor observado; no lista de validación)');observedValue=$true}
 }
 $asset=@{sourceSha256=$map.sha256;source=$map.source;type=$type.ToUpper();header=[string]$cells['F1'];elements=$elements;fields=$fields;catalogs=$options;typesByCategory=$types;defaultElements=$defaults}
 $asset|ConvertTo-Json -Depth 12|Set-Content "app/src/main/assets/scap_${type}_catalog.json" -Encoding utf8
 Write-Output "$type elements=$($elements.Count), fields=$($fields.Count), defaults=$($defaults -join ',')"
}
