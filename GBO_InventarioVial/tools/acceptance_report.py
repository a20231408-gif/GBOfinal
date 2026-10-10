"""Build evidence-indexed Markdown/JSON matrices. Never approve unexecuted physical workflows."""
from pathlib import Path
import json,re,hashlib,zipfile,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'docs/acceptance-audit-v15-2026-10-10'
def read(rel):return json.loads((OUT/rel).read_text(encoding='utf-8'))
def write(name,text):(OUT/name).write_text(text+'\n',encoding='utf-8')
def save(name,value):write(name,json.dumps(value,ensure_ascii=False,indent=2))
def table(headers,rows):
    def cell(v):return str(v).replace('|','/').replace('\n',' ')
    return '| '+' | '.join(headers)+' |\n|'+'|'.join('---' for _ in headers)+'|\n'+'\n'.join('| '+' | '.join(map(cell,row))+' |' for row in rows)
artifacts=read('evidence/artifact-inspection.json')
lookup={x['file']:x for x in artifacts}
inventory=read('scenarios/format-inventory.json')

formats=[]
for f in inventory['sic']:
    item=lookup.get('scenarios/all-sic/'+f['code']+'.xlsx',{})
    formats.append({'format':f['code'],'fields_exported':f['columns'],'columns':f['columnCount'],
        'source':'export/SicExportFormat.kt; sic/SicFieldFormats.kt; SicCatalogRepository; Manual IV; EXPORTACION_SIC',
        'physical_open':'NO PROBADO','required_fields_catalogs':'NO PROBADO','route_pr_calzada_gps':'NO PROBADO',
        'save_edit_reopen':'NO PROBADO','photos':'NO PROBADO','validation':'NO PROBADO',
        'independent_xlsx_read':'PASS' if item and 'inspection_error' not in item and f['matchingRecords']>0 else 'FAIL',
        'official_export_integrity':'NO PROBADO','drive_folder_contract':'FAIL','real_upload':'NO PROBADO',
        'acceptance':'FAIL','local_evidence':item.get('file'),'records':f['matchingRecords'],
        'limitation':'SC34 causa conflictiva bloqueada' if f['code']=='SIC-34' else 'SIC18A comparte padre/fotos' if f['code']=='SIC-18A' else 'SIC22 puntual, decision posterior al manual' if f['code']=='SIC-22' else 'Flujo local emulado; Drive D01/D02/D03/D04'})
for f in inventory.get('bridgeSupplements',[]):
    formats.append({'format':f['format'],'fields_exported':f['fields'],'columns':len(f['fields']),
      'physical_open':'NO PROBADO','required_fields_catalogs':'NO PROBADO','route_pr_calzada_gps':'NO PROBADO','save_edit_reopen':'NO PROBADO',
      'photos':'NO PROBADO','validation':'NO PROBADO','independent_xlsx_read':'NO PROBADO','official_export_integrity':'NO PROBADO',
      'drive_folder_contract':'NO PROBADO','real_upload':'NO PROBADO','acceptance':'NO PROBADO','local_evidence':'scenarios/format-inventory.json',
      'source':'SupplementaryForms; BridgeSicContract; ScapSicExporter; ScapToSicMapper',
      'limitation':'Standalone flag false; projection from SCAP implemented. Incomplete audit bridges do not qualify for export; no blank fabricated file.'})
for kind,folder in [('LOSA','04-puente-losa'),('MODULAR','05-puente-modular'),('HISTORICO','legacy')]:
    key=f'scenarios/{folder}/SCAP_{kind}.xlsx';item=lookup.get(key,{})
    formats.append({'format':'SCAP '+kind,'fields_exported':['A','B','C1-C10','D1-D4','E','F1','F2','G1','G2','SIC supplement'],
      'columns':'9 hojas canónicas / histórico distinto','physical_open':'NO PROBADO','required_fields_catalogs':'NO PROBADO',
      'route_pr_calzada_gps':'NO PROBADO','save_edit_reopen':'NO PROBADO','photos':'NO PROBADO','validation':'NO PROBADO',
      'independent_xlsx_read':'PASS' if item and 'inspection_error' not in item else 'NO PROBADO',
      'official_export_integrity':'NO PROBADO','drive_folder_contract':'NO PROBADO','real_upload':'NO PROBADO','acceptance':'NO PROBADO',
      'local_evidence':key if item else 'evidence/full-junit; legacy D4 findings E10/E11',
      'source':'ScapBridgeType; ScapCanonicalLayout/Exporter; ScapExcelExporter; SCAP_EXPORT_CONTRACT; original templates',
      'limitation':'Partial audit bridge, formulas #REF! inherited; physical Excel recalculation/printing pending; D02/D08'})
for surface,folder,code in [('ASPHALT','06-asfaltico','ASF'),('RIGID','07-rigido','RIG'),('UNSURFACED','08-no-pavimentado','NPV')]:
    formats.append({'format':'PAV '+surface,'fields_exported':['muestra','ruta/tramo/calzada','PR','damages','dimensions','severity','quantities','photos','calculation'],
      'columns':'13 columnas Fallas + Resumen/Detalle','physical_open':'NO PROBADO','required_fields_catalogs':'NO PROBADO','route_pr_calzada_gps':'NO PROBADO',
      'save_edit_reopen':'NO PROBADO','photos':'NO PROBADO','validation':'NO PROBADO','independent_xlsx_read':'PASS',
      'official_export_integrity':'NO PROBADO','drive_folder_contract':'FAIL','real_upload':'NO PROBADO','acceptance':'FAIL',
      'local_evidence':f'scenarios/{folder}/PAV_{code}.xlsx; M_001.json; calculation.json',
      'source':'PavementModels/Catalog/Rules/Calculator/Exports/Drive; Manual_PCI_URCI; PAVEMENT_MANUAL_MAPPING',
      'limitation':'D01/D02; PCI/URCI null with missing curves; PDF generation unavailable in Robolectric, not approved'})
for code,folder in [('SIC-14','06-asfaltico'),('SIC-14A','06-asfaltico'),('SIC-15','07-rigido'),('SIC-16','08-no-pavimentado')]:
    formats.append({'format':code,'fields_exported':['Ruta','Calzada','Faja','PR inicio/fin','distancias','tipo','longitud','ancho','gravedad','densidad','fecha'],
      'columns':13,'physical_open':'NO PROBADO','required_fields_catalogs':'NO PROBADO','route_pr_calzada_gps':'NO PROBADO',
      'save_edit_reopen':'NO PROBADO','photos':'NO PROBADO','validation':'NO PROBADO','independent_xlsx_read':'PASS',
      'official_export_integrity':'NO PROBADO','drive_folder_contract':'FAIL','real_upload':'NO PROBADO','acceptance':'FAIL',
      'local_evidence':f'scenarios/{folder}/{code}.xlsx','source':'PavementSic; PAVEMENT_MANUAL_MAPPING; visual references',
      'limitation':'Reference reconstructed from images, not identity to unavailable official XLSX; manual supplemental fields. SIC14A07/13 blocked.'})
save('A_FORMATOS.json',formats)
write('A_FORMATOS.md','# A. Inventario automático de formatos\n\n'+
 f'{len(formats)} formatos/flujos: 12 SIC inventario, dos complementos de puente, dos SCAP canónicos + histórico, tres superficies y cuatro SIC de pavimentos. Enum runtime en scenarios/format-inventory.json; campos/estados completos en A_FORMATOS.json. SIC-15A/16A no están implementados como exportadores. Flags standalone SIC17A/B false no eliminan la proyección SCAP existente.\n\n'+
 'PASS en Lectura XLSX significa exclusivamente archivo real generado por el exportador Android, abierto con openpyxl, ZIP CRC/XML válidos y al menos una fila sintética. No aprueba formulario, normas, cálculos ni Drive. Las funciones físicas de apertura, campos/catálogos, ruta/tramo/calzada/PR/GPS, guardar/editar/reabrir, fotos y validación tienen NO PROBADO; el JSON las enumera individualmente.\n\n'+
 table(['Formato','Columnas/hojas','Lectura XLSX','Integridad oficial','Carpetas Drive','Subida real','Estado aceptación','Evidencia local'],
 [[x['format'],x['columns'],x['independent_xlsx_read'],x['official_export_integrity'],x['drive_folder_contract'],x['real_upload'],x['acceptance'],x['local_evidence']] for x in formats])+
 '\n\nLos incompletos SCAP del fixture son deliberados: no certifican cobertura de todos los campos, apoyos, tipos, porcentajes o límites. Fuente/contradicciones en REQUISITOS_Y_CONTRADICCIONES.md. Repetir protocolo F en cada fila; el estado FAIL de carpetas compara con la solicitud, no simula una subida fallida.')

drive=[
 ['SIC fotografías','RUTA/SIB/ELEMENTO N/fotos','RUTA/FAMILIA/ELEMENTO_NNN/CATEGORIA','PRUEBA_FINAL/PE-22A/SIB-02/ALCANTARILLA 1/001_PAN.jpg','FAIL','Lectura histórica real, NO envío v15'],
 ['SIC Excel de lote','RUTA/SIB/SIC-XX.xlsx consolidado','RUTA/FAMILIA/ELEMENTO_NNN/SIC-XX.xlsx por registro','PRUEBA_FINAL/PE-22A/SIB-02/SIC-18.xlsx + SIC19/SIC23','FAIL','Manifest scenario09 + Drive list'],
 ['SIC metadata','Carpeta del mismo elemento; UUID/fotos/categorías','metadata.json por UUID junto a XLSX','PRUEBA_FINAL/PE-22A/SIB-02/ALCANTARILLA 1/metadata.json','NO PROBADO','Solo histórica listada; asociación v15 pendiente'],
 ['SCAP completo revisado','Puente individual ruta/clasificación + fotos/croquis/revisiones','RUTA/PUENTES/PUENTE_NNN; misma identidad que SIC17 compatible','No se observó una entrega SCAP v15','NO PROBADO','HumanLayout + manifest pathVersion2; backend distinto'],
 ['SCAP incompleto/legacy','Misma conservación de puente/archivos/revisiones; ubicación contractual por confirmar','Protocolo1: SCAP/hash-identidad/UUID/revision; fotos UUID.jpg/croquis UUID.png','No se ejecutó POST','NO PROBADO','ZIP local real protocolo1/bundle7, no validación de ruta remota'],
 ['Pavimento asfáltico','RUTA/EVALUACION_SUPERFICIAL/TRAMO/ASFALTICO/{campo,panel,data}','RUTA/PAVIMENTOS/ASFALTICO/MUESTRA_NNN/{documentos,GENERAL}','No se observó envío v15','FAIL','DrivePaths.pavement + uploader'],
 ['Pavimento rígido','RUTA/EVALUACION_SUPERFICIAL/TRAMO/RIGIDO/{campo,panel,data}','RUTA/PAVIMENTOS/RIGIDO/MUESTRA_NNN/{documentos,GENERAL}','No se observó envío v15','FAIL','Tramo solo en JSON/payload, no en logicalPath'],
 ['No pavimentado','RUTA/EVALUACION_SUPERFICIAL/TRAMO/NO_PAVIMENTADO/{campo,panel,data}','RUTA/PAVIMENTOS/NO_PAVIMENTADO/MUESTRA_NNN/{documentos,GENERAL}','No se observó envío v15','FAIL','GROUP_DATA copia Excel en carpeta de muestra'],
 ['ACK→SYNCED','Identidad+bytes+ubicación confirmados de todos los archivos','SIC requiere ID/hash/protocol pero omite pathVersion/logicalPath','NO PROBADO servidor; probe local aceptó WRONG/PATH y SYNCED','FAIL','scenarios/wrong-path-ack.json'],
 ['Retry/idempotencia','Mismo contenido sin duplicados; cambios/revisiones conservados','SIC batch fingerprint inmutable; SCAP revision; pav staging/hash','No se provocó corte real de red/ACK','NO PROBADO','Tests Node/client apoyan análisis, no E2E'],
 ['Varias tablets/lotes','Separación y numeración acordada sin colisión','Counter por instalación; ruta con lote iniciado bloquea otro contenido','NO PROBADO dos tablets ni lote nuevo remoto','NO PROBADO','D05/D06; protocolo F'],
 ['Endpoint publicado','Contrato compatible con APK y raíz aislada confirmada','APK GET Drive v6.2/bundle4; local v7/bundle6; Android bundle7','GET real HTTP200, sin pathVersion/pavementProtocol anunciados','FAIL','evidence/endpoint-health.json']]
save('B_DRIVE.json',[dict(zip(['area','expected','implemented','observed','status','evidence'],r)) for r in drive])
write('B_DRIVE.md','# B. Matriz Drive\n\n'+table(['Área','Esperado','Implementado','Observado realmente','Estado','Evidencia'],drive)+
 '\n\nRaíz del código local: PRUEBA_FINAL, id 1u7II1gvhSIHBwVxFjnx6VsAqa3O_jTIi. Su lectura funciona; no prueba que sea la raíz efectiva del endpoint publicado. Rutas leídas: PE-22A, PE-28H, PE-3NG, PE-12A, PE-28C; aparecen SIB02/SIB07 y archivos creados el30/09. No se enumeró toda la cuenta; ausencia de prueba SCAP/PAV v15 no significa ausencia absoluta de archivos.\n\n'+
 'Evidencia completa de listas/IDs/fechas: evidence/drive-observed.json y drive-other-routes.json. Copias independientes históricas: evidence/remote. Nombre histórico 001_PAN.jpg (11 caracteres), dimensiones1600×1000, EXIF identifica SIMULATED_QA y UUID; es una lámina demo con logo/texto, no una foto de campo v15. SIC23 histórico abre con hoja PE-22A y PR0049 conservado. No se publicaron scripts ni se alteraron archivos remotos.\n\n'+
 'SIC manual usa SicExcelWriter/SAF y puede agrupar por ruta; no es la cola RouteDelivery y no marca registros sincronizados. Preparar→PREPARED→enqueue/QUEUED→WorkManager con red→POST→ACK de cada archivo→verifyCurrent→SYNCED. Cada flecha remota v15 requiere el protocolo F; GET/read/list no la sustituye.')

source=(ROOT/'app/src/main/java/com/tuempresa/inventariovial/road/AssetPresentationCatalog.kt').read_text(encoding='utf-8')
families=re.findall(r'^\s+(\w+)\("([^"]+)", "([^"]+)", "([^"]+)", "([^"]+)",',source,re.M)
photos=[]
for key,abbrev,label,prefix,family in families:
    category='PANORAMICA/ENTRADA/SALIDA; repetibles' if key=='ALCANTARILLA' else 'GENERAL; repetible'
    fam='SENALIZACION_VERTICAL' if key=='SENALIZACION_VERTICAL' else 'SENALIZACION_HORIZONTAL' if key=='SENALIZACION_HORIZONTAL' else family
    photos.append([label,category,'NNN_FNN.jpg','10–11+; ≤20 exigido al encolar','FotoEntity+DeliveryMetadata; EXIF conservado si existe',f'RUTA/{fam}/{prefix}_NNN/CATEGORIA','NO PROBADO; PNG→JPG FAIL demostrado'])
photos.extend([
 ['SCAP LOSA/MODULAR F.2','31 nombres únicos de32 captions originales, editable; repetibles','Humano NNN_FNN.jpg; v1 UUID.jpg','Humano≤20; UUID usual40, alcance límite por confirmar','UUID/elementCode/fecha/GPS/origen/description en manifest; captions/foto','V2 puente individual; v1 fotos bajo inspección','NO PROBADO cámara/subida; JPEG de ZIP inspeccionado'],
 ['SCAP histórico','9 primarias RIGHT_SIDE/LEFT_SIDE/UPSTREAM/DOWNSTREAM/ROAD_DECK/INFORMATIVE_SIGN/RAILINGS_SIDEWALKS_PIPES/DEFECT/BRIDGE_ELEMENT; adicionales','Humano compacto o UUID.jpg','Según flujo','PhotoEntity/manifest; sin inferir categorías por nombre','Puente revisado/legacy separado','NO PROBADO'],
 ['Pavimentos 3 superficies','GENERAL, asociado al UUID del daño','NNN_DNNN.jpg','12 caracteres usuales;≤20','UUID foto/daño, createdAt/categoría JSON; no GPS propio en modelo','RUTA/PAVIMENTOS/SUPERFICIE/MUESTRA_NNN/GENERAL','NO PROBADO; copia PNG sin conversión por diseño']])
save('C_FOTOGRAFIAS.json',[dict(zip(['type','categories','names','length','metadata','folder','status'],r)) for r in photos])
write('C_FOTOGRAFIAS.md','# C. Fotografías por todos los tipos implementados\n\n'+table(['Tipo','Categorías','Nomenclatura','Límite','Metadatos','Destino implementado','Estado'],photos)+
 '\n\nFamilias obtenidas automáticamente de AssetFamily (16). Reglas por SIC en PHOTO_CATEGORY_POLICY_INVENTORY.json y PhotoCategoryPolicy: solamente alcantarillas reciben tres categorías; históricos desconocidos permanecen GENERAL. Campos completos del modelo: PhotoEntity.kt; manifiestos de scenarios09 conservan UUID, orden y metadatos.\n\n'+
 'Verificaciones físicas por tipo (todas NO PROBADO): cámara, galería, ocho orientaciones EXIF, reemplazo, eliminación, múltiples, guardar/reabrir, exportación y subida. Las imágenes sintéticas solo comprueban transporte sin cambios, decodificación/relación de archivos; no acreditan legibilidad de una inspección ni GNSS real. Prueba negativa independiente: el archivo JPG del probe decodifica como PNG; D04.\n\n'+
 'El nombre compacto SIC tiene secuencia de entrega por ruta y photoIndex; DrivePathPlanner ordena por eje/PR y desempates, luego photoIndex/UUID. DrivePaths conserva contador de identidad local; no es la misma numeración geométrica. SCAP humano ordena puentes/fotos antes de preparar; pavimentos correlativo de daño/createdAt. Estabilidad entre nuevas capturas/tablets y ausencia de colisiones remotas: NO PROBADO.\n\n'+
 'CleanPhotoPolicy no agrega watermark, pero tampoco verifica JPEG ni elimina texto existente. JPEG original de captura se mantiene byte por byte; SCAP genera versiones limitadas para workbook preservando proporción y convierte otros formatos del ZIP. EXIF no se promete si el original no lo tiene; faltantes deben ser visibles, no rellenados. D01/D02 impiden aprobar destinos remotos.')

manifest=read('scenarios/09-misma-ruta-sib/manifest.local.json')
payloads=[x['payload'] for x in manifest['files']]
scenarios=[]
definitions=[('01-alcantarilla','Alcantarilla','SIC-18'),('02-baden','Badén','SIC-20'),('03-senales','Señales horizontal/vertical','SIC-21/SIC-22'),
 ('04-puente-losa','Puente LOSA','SCAP'),('05-puente-modular','Puente MODULAR','SCAP'),('06-asfaltico','Asfáltico','PAV'),('07-rigido','Rígido','PAV'),
 ('08-no-pavimentado','No pavimentado','PAV'),('09-misma-ruta-sib','Varios elementos ruta/SIB','MULTI'),('10-dos-rutas','Dos rutas','MULTI')]
for folder,label,format_code in definitions:
    files=[x for x in artifacts if x['file'].startswith('scenarios/'+folder+'/')]
    local_paths=[];names=[]
    if folder[:2] in ('01','02','03'):
        codes=format_code.split('/')
        record_ids={p.get('recordId') for p in payloads if p.get('sicCode') in codes and p['kind']=='sic'}
        selected=[p for p in payloads if p.get('recordId') in record_ids]
        local_paths=sorted(set(p['logicalPath'] for p in selected));names=[p['fileName'] for p in selected]
        # Copies come from the actual prepared payload; preserve exact transmitted file names.
        for e in manifest['files']:
            if e['payload'] in selected:
                src=OUT/'scenarios/09-misma-ruta-sib'/e['localFile']
                if src.is_file():
                    dest=OUT/'scenarios'/folder/'delivery'/e['payload']['logicalPath']/e['payload']['fileName']
                    dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(src.read_bytes())
        expected='PE-22A/SIB-02/'+('ALCANTARILLA 1/' if folder.startswith('01') else 'BADEN 1/' if folder.startswith('02') else 'SEÑALIZACION HORIZONTAL 1/ y SEÑALIZACION VERTICAL 1/')
        obtained='Archivos locales reales preparados; carpeta V2 diferente del contrato. Nombre por payload, bytes comprobables.'
        state='FAIL'
    elif folder[:2] in ('04','05'):
        fixture=read('scenarios/'+folder+'/fixture.json')
        expected='PE-28H/clasificación aprobada/puente individual; ficha/fotos/croquis/revisiones'
        local_paths=['ZIP protocolo1 incompleto: no logicalPath V2; ubicación externa NO PROBADO']
        names=[Path(x['file']).name for x in files]
        obtained='Room snapshot guardado/leído; XLSX nueve hojas y ZIP bundle7. Ficha incompleta; no aprobación del cálculo ni envío.';state='NO PROBADO'
    elif folder[:2] in ('06','07','08'):
        surface={'06':'ASFALTICO','07':'RIGIDO','08':'NO_PAVIMENTADO'}[folder[:2]]
        expected=f'PE-22A/EVALUACION_SUPERFICIAL/TRAMO_AUDITORIA/{surface}/07_HOJAS_CAMPO,08_PANEL_FOTOGRAFICO,DATA_APP'
        local_paths=[f'PE-22A/PAVIMENTOS/{surface}/MUESTRA_NNN',f'PE-22A/PAVIMENTOS/{surface}/MUESTRA_NNN/GENERAL']
        names=[Path(x['file']).name for x in files]
        obtained='Room y XLSX/JSON reales; índice pendiente por curvas; intento PDF cero bytes en Robolectric. Carpeta implementada sin tramo.';state='FAIL'
    else:
        expected='PE-22A/SIB correspondiente/elemento N y consolidados por lote; PE-3N separado en escenario10'
        local_paths=sorted(set(p['logicalPath'] for p in payloads)) if folder.startswith('09') else ['Excel PE-3N separado; preparación/subida real de segundo lote NO PROBADO']
        names=[Path(x['file']).name for x in files]
        obtained='Varios elementos de SIB02 y otras familias, payload UUID/fotos/XLSX; escenario10 exporta PE-3N sin mezclar hoja PE-22A.';state='FAIL' if folder.startswith('09') else 'NO PROBADO'
    scenarios.append({'scenario':folder,'description':label,'data_source':f'scenarios/{folder}/fixture.json; data-*.json; AcceptanceDeliveryAuditTest.kt',
        'expected_drive_path':expected,'client_prepared_paths':local_paths,'real_drive_path':'NO PROBADO (no POST v15)',
        'expected_names':'SIC-XX.xlsx a nivel SIB; JPEG compactos de payload; SCAP.xlsx/croquis según flujo; HC_001.pdf/M_001.json/PAV_*.xlsx',
        'actual_local_names':names,'obtained':obtained,'status':state,'files':[{'file':x['file'],'sha256':x['sha256'],'size':x['size']} for x in files]})
save('ESCENARIOS.json',scenarios)
write('ESCENARIOS.md','# Diez escenarios reproducibles\n\n'+
 'Datos sintéticos, aislados en SQLite de Robolectric; no pertenecen a una tablet operativa. Exportadores Kotlin reales (sin reescribir archivos con Python). JSON completo: ESCENARIOS.json y fixture/data-*.json. Cada archivo tiene hash/tamaño en evidence/artifact-inspection.json; manifiesto transmisible sin token en escenario09.\n\n'+
 table(['Caso','Datos/evidencia','Ruta esperada','Preparada por cliente / obtenida','Ruta real v15','Estado'],
 [[s['scenario'],s['data_source'],s['expected_drive_path'],'; '.join(s['client_prepared_paths'])+'; '+s['obtained'],s['real_drive_path'],s['status']] for s in scenarios])+
 '\n\nNombres exactos reales/archivos/hash por caso: ESCENARIOS.json. Para casos01–03 se copian los **bytes del staging** bajo delivery/logicalPath/fileName, sin sustituir sus nombres por nombres acordados inventados. Los consolidados de all-sic son exportación manual, distinta de los XLSX por elemento del staging. Escenario09 incluye SIB02 y otras familias; no pretende que todos pertenezcan al mismo SIB.\n\n'+
 'Puentes LOSA/MODULAR: un elemento manual con metrado12.75ml y porcentajes80/20, una foto JPEG sintética, croquis sintéticos y campos de identificación. Son incompletos; los ejemplos originales de la fuente no se usan para completar información. Pavimentos: PE22A/TRAMO_AUDITORIA/CD, muestra1, 5×46m, PR001+000→001+046; primera falla del catálogo por superficie. Rígido10×2losas de2.5×4.6m. PCI/URCI permanece null y las cuatro salidas SIC usan suplementos manuales tipo1. Las curvas faltantes no se sustituyen.\n\n'+
 'Reproducción: ejecutar gradlew.bat testDebugUnitTest --tests *AcceptanceDeliveryAuditTest con JDK y recursos documentados en REPRODUCIR.md, luego acceptance_inspect.py artifacts y acceptance_report.py. Los probes adicionales wrong-path-ack y photo-png-contract-probe comprueban defectos, no son servidores reales. F exige repetir01–10 en hardware/despliegue aislado antes de aceptación.')

save('evidence/scenario-copy-index.json',{'generated_by':'AcceptanceDeliveryAuditTest Kotlin production exporters',
    'inspection_by':'acceptance_inspect.py using openpyxl/Pillow/PDFium/ZIP/XML',
    'remote_writes':False,'scenarios':len(scenarios),'format_rows':len(formats),'photo_type_rows':len(photos)})
print('Matrices generated:',len(formats),'formats,',len(photos),'photo types,',len(scenarios),'scenarios')
