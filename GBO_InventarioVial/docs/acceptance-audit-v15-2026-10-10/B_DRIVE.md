# B. Matriz Drive

| Área | Esperado | Implementado | Observado realmente | Estado | Evidencia |
|---|---|---|---|---|---|
| SIC fotografías | RUTA/SIB/ELEMENTO N/fotos | RUTA/FAMILIA/ELEMENTO_NNN/CATEGORIA | PRUEBA_FINAL/PE-22A/SIB-02/ALCANTARILLA 1/001_PAN.jpg | FAIL | Lectura histórica real, NO envío v15 |
| SIC Excel de lote | RUTA/SIB/SIC-XX.xlsx consolidado | RUTA/FAMILIA/ELEMENTO_NNN/SIC-XX.xlsx por registro | PRUEBA_FINAL/PE-22A/SIB-02/SIC-18.xlsx + SIC19/SIC23 | FAIL | Manifest scenario09 + Drive list |
| SIC metadata | Carpeta del mismo elemento; UUID/fotos/categorías | metadata.json por UUID junto a XLSX | PRUEBA_FINAL/PE-22A/SIB-02/ALCANTARILLA 1/metadata.json | NO PROBADO | Solo histórica listada; asociación v15 pendiente |
| SCAP completo revisado | Puente individual ruta/clasificación + fotos/croquis/revisiones | RUTA/PUENTES/PUENTE_NNN; misma identidad que SIC17 compatible | No se observó una entrega SCAP v15 | NO PROBADO | HumanLayout + manifest pathVersion2; backend distinto |
| SCAP incompleto/legacy | Misma conservación de puente/archivos/revisiones; ubicación contractual por confirmar | Protocolo1: SCAP/hash-identidad/UUID/revision; fotos UUID.jpg/croquis UUID.png | No se ejecutó POST | NO PROBADO | ZIP local real protocolo1/bundle7, no validación de ruta remota |
| Pavimento asfáltico | RUTA/EVALUACION_SUPERFICIAL/TRAMO/ASFALTICO/{campo,panel,data} | RUTA/PAVIMENTOS/ASFALTICO/MUESTRA_NNN/{documentos,GENERAL} | No se observó envío v15 | FAIL | DrivePaths.pavement + uploader |
| Pavimento rígido | RUTA/EVALUACION_SUPERFICIAL/TRAMO/RIGIDO/{campo,panel,data} | RUTA/PAVIMENTOS/RIGIDO/MUESTRA_NNN/{documentos,GENERAL} | No se observó envío v15 | FAIL | Tramo solo en JSON/payload, no en logicalPath |
| No pavimentado | RUTA/EVALUACION_SUPERFICIAL/TRAMO/NO_PAVIMENTADO/{campo,panel,data} | RUTA/PAVIMENTOS/NO_PAVIMENTADO/MUESTRA_NNN/{documentos,GENERAL} | No se observó envío v15 | FAIL | GROUP_DATA copia Excel en carpeta de muestra |
| ACK→SYNCED | Identidad+bytes+ubicación confirmados de todos los archivos | SIC requiere ID/hash/protocol pero omite pathVersion/logicalPath | NO PROBADO servidor; probe local aceptó WRONG/PATH y SYNCED | FAIL | scenarios/wrong-path-ack.json |
| Retry/idempotencia | Mismo contenido sin duplicados; cambios/revisiones conservados | SIC batch fingerprint inmutable; SCAP revision; pav staging/hash | No se provocó corte real de red/ACK | NO PROBADO | Tests Node/client apoyan análisis, no E2E |
| Varias tablets/lotes | Separación y numeración acordada sin colisión | Counter por instalación; ruta con lote iniciado bloquea otro contenido | NO PROBADO dos tablets ni lote nuevo remoto | NO PROBADO | D05/D06; protocolo F |
| Endpoint publicado | Contrato compatible con APK y raíz aislada confirmada | APK GET Drive v6.2/bundle4; local v7/bundle6; Android bundle7 | GET real HTTP200, sin pathVersion/pavementProtocol anunciados | FAIL | evidence/endpoint-health.json |

Raíz del código local: PRUEBA_FINAL, id 1u7II1gvhSIHBwVxFjnx6VsAqa3O_jTIi. Su lectura funciona; no prueba que sea la raíz efectiva del endpoint publicado. Rutas leídas: PE-22A, PE-28H, PE-3NG, PE-12A, PE-28C; aparecen SIB02/SIB07 y archivos creados el30/09. No se enumeró toda la cuenta; ausencia de prueba SCAP/PAV v15 no significa ausencia absoluta de archivos.

Evidencia completa de listas/IDs/fechas: evidence/drive-observed.json y drive-other-routes.json. Copias independientes históricas: evidence/remote. Nombre histórico 001_PAN.jpg (11 caracteres), dimensiones1600×1000, EXIF identifica SIMULATED_QA y UUID; es una lámina demo con logo/texto, no una foto de campo v15. SIC23 histórico abre con hoja PE-22A y PR0049 conservado. No se publicaron scripts ni se alteraron archivos remotos.

SIC manual usa SicExcelWriter/SAF y puede agrupar por ruta; no es la cola RouteDelivery y no marca registros sincronizados. Preparar→PREPARED→enqueue/QUEUED→WorkManager con red→POST→ACK de cada archivo→verifyCurrent→SYNCED. Cada flecha remota v15 requiere el protocolo F; GET/read/list no la sustituye.
