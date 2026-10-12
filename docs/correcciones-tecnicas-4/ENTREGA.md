# Prompt 4 — correcciones técnicas

Base: `bbbf7598598b01624e421836923d39e9a3d1f326` (Prompt 3 publicado). Rama de trabajo: `codex/correcciones-tecnicas-sic-4`. Copia aislada: `prompt4` dentro del workspace D:. El worktree backend-v3-pruebas y sus 10 modificaciones rastreadas/38 entradas no rastreadas se conservan; no se incorporan a este encargo.

## Matriz elemento → cambio → prueba

| Elemento | Cambio | Verificación focalizada |
|---|---|---|
| SIC-18 / 18A | Función siempre antes de condiciones; selección independiente; mismo UUID | TechnicalRules4ScreenTest: orden visual; TechnicalRules4Test: validación y persistencia |
| SIC-18A BUENA | Función obligatoria en captura nueva; fallas opcionales, antecedentes conservados, exportables | Cambio REGULAR → BUENA, edición de función, persistencia y XML XLSX |
| SIC-18A REGULAR / MALA | Secciones de fallas visibles y obligatorias; detalle completo editable | Validación de ambos estados, controles habilitados, reapertura Room |
| Túnel SIC-20 clase 13 | Selector limitado a S / Sin objeto; validación al guardar y editar | Rechazo I/D, aceptación S; otras dimensiones intactas |
| Muro SIC-20 clase 14 | Selector I/D; S excluido; valores antiguos sin migración silenciosa | Rechazo S en captura; preservación explícita del lado sin editar |
| Guardavía SIC-21 clase 19/tipo 1 | Material inicial Acero, código 1 existente | Catálogo, selección nueva y conservación de selección histórica |
| Resalto SIC-21 clase 19/tipo 4 | Concreto, código 2, al seleccionar tipo nuevo | Catálogo, persistencia Room y exportación SIC |
| Muros | Cálculo automático, modo medido o histórico conservado, recalculado al editar extremos en AUTO | Misma PR/diferente PR, sentido decreciente, actualización y modo manual |
| Cunetas y guardavías | Longitud derivada de ambos extremos, pantalla + complemento Excel + metadata V3 | Familias elegibles, cálculo, XML complementario |
| Tachas por tramo | Cálculo solo con ambos extremos suficientes; no inventar longitud puntual | Datos faltantes, actualización de progresiva |
| Badenes | Longitud longitudinal calculada; anchos contractuales no se reemplazan | Familia elegible y separación de dimensiones |
| Componentes compartidos | GPS opcional, observaciones, límites y edición con revisiones | ContractualForms3Test y SyncedRecordEdits2BTest focalizados |

## Criterio contractual de longitudes

El proyecto tiene dos mecanismos diferentes: `SurveyOrder.chainage` estima PR como kilómetro y `RoadPr.chainageM` identifica la medida acumulada del catálogo de ruta/calzada. El primero sirve en flujos previos de ordenamiento, pero **no demuestra una equivalencia física entre dos códigos PR**. Este encargo no altera esos flujos anteriores.

El nuevo cálculo usa `chainageM + distancia desde PR` para cada extremo cuando hay dos PR diferentes, con coincidencia única en la misma ruta/calzada. Dentro del mismo PR, resta los offsets porque el origen común se cancela. Sin equivalencia o datos suficientes devuelve pendiente y una advertencia. No usa diferencia de códigos × 1000. La longitud física es positiva en sentido decreciente; el sentido contradictorio se advierte. Se conservan decimales internamente y se muestran dos decimales en metros, conforme a los estilos Excel existentes.

Los modelos/plantillas actuales de SIC-19/20/21 no incluyen una columna contractual de longitud longitudinal. SIC-20 conserva `wallLengthMeters` fuera de las columnas principales. Se mantiene el contrato y se genera `LONGITUD_<UUID>.xlsx` en exportación por elemento y `longitud.xlsx` con clave estable `technical-length` en la entrega V3. Metadata incluye cálculo, medida histórica/manual, longitud aplicable, origen, unidad y advertencia. El backend utiliza la misma infraestructura existente de archivos, ACK, revisiones y fileId; no hay nuevo protocolo ni otro sistema de sincronización.

Para muro, el modo se guarda en `sic_extension_details` existente, con `_lengthMode=AUTO/MEASURED/HISTORICAL`. Un muro anterior sin modo conserva la longitud histórica. Reabrir no recalcula ni reemplaza una medición. Cambiar progresivas actualiza la longitud persistida solo en AUTO; el complemento muestra el cálculo nuevo junto a la medición conservada. Un ingeniero puede seleccionar explícitamente el cálculo en captura o ingresar una medida validada. No se alteran anchos de badenes ni dimensiones de túneles.

## Revisión necesaria del ingeniero

- Confirmar/importar catálogo contractual PR → distancia acumulada para rutas sin catálogo. La aplicación conserva pendiente un cálculo entre PR distintos sin esa evidencia.
- Confirmar si la entrega contractual debe admitir una futura columna de longitud en los consolidados; esta tarea usa complemento para preservar las plantillas actuales.
- El catálogo denomina METAL como **Acero (1)**. No se crea un código distinto.
- El encabezado contractual SIC-18A históricamente dice «condición mala». Se conservan encabezados y columnas oficiales; el formulario aplica ahora las reglas solicitadas de REGULAR/MALA y función independiente.
- Validación presencial de cada control y envío real contra pruebas V3/pathVersion 4 siguen pendientes. El endpoint de pruebas conocido del handoff anterior anuncia pathVersion 3; no se ejecutan escrituras remotas en este encargo.

## Restricciones y resultados

Sin publicación Apps Script, instalación APK, cambios de credenciales, servicio de usuarios, applicationId o firma Release. Room conserva versión/esquema 17. No se cambian dashboard, navegación, categorías fotográficas, SCAP, PCI/URCI ni mapa/KMZ. No se ejecuta suite completa.

**51 casos distintos aprobados**: 15 reglas/persistencia/exportación de Prompt 4, 3 pantalla de Prompt 4, 11 flujos de elemento, 12 regresiones contractuales del Prompt 3 y 10 edición sincronizada 2B. La última selección afectada aprobó 16/16 y `assembleDebug`. No se ejecutó suite completa. Detalles, nombres y SHA APK en [VALIDACION.json](VALIDACION.json).

Primer resultado: 38 casos, un fallo por la cabecera Excel que exige al menos nueve columnas; se corrigió el complemento añadiendo la ubicación completa, sin alterar contratos SIC. Segundo: 29 casos, un fallo en fixture GPS anterior sin origen explícito; se corrigió la fixture conforme a Prompt 3, sin cambiar el comportamiento GPS. Logs/resultados de ambas ejecuciones conservados localmente sin commit, junto al primer error de acceso al caché Gradle reparado usando el caché configurado del usuario.

La revisión focalizada de `google-apps-script/SyncV3Core.js` y `SyncV3.gs` confirmó que `files` acepta el complemento con clave estable y lo actualiza por fileId. No se necesita cambiar Apps Script para este archivo. La prueba de preparación Android confirma syncProtocol 3/pathVersion 4, nulos GPS y modificación del hash de longitud al editar progresiva. Esto es evidencia local, no una subida real.

APK Debug compilada con configuración de pruebas copiada idéntica, sin `gboValidationBuild=true`, en `app/build/outputs/apk/debug/app-debug.apk`. applicationId/versionCode 16 y certificado Debug del Prompt 3 conservados. No se instaló. El checkpoint incluye solo fuentes/pruebas/documentación de esta entrega; no APK, secretos, propiedades privadas, recursos locales preexistentes ni resultados crudos.
