# GBO P0.3 — Cierre de regresiones y preparación de campo

30/10/2026, Lima. Rama `codex/observaciones-campo-asus-20261030`, base `0131135`. Fuentes 1.4.18 (32). Sin funcionalidades nuevas ni cambios productivos.

## Corrección del fixture

En `PhotoTransport10Test.rejectedCachedRevisionIsBackedUpAndRebuiltWithSameIdentityAndPhoto` se incorporó la negociación de capacidades y reserva V2 mediante `AutomaticDriveHttp` con interceptor HTTP ficticio, antes de preparar la entrega. La reserva se registra con el validador existente; usa familia FAJA, nombre `001_FAJ1_GEN.jpg`, revisión e identidad de la petición y binding de carpeta estable. No hay conexión real ni desactivación de V2.

Se conservaron todas las aserciones anteriores. Se comprueban además identidad, revisión, reserva/ruta estable, hash del contenido y archivo reparado, fotografía original intacta, manifiesto y bytes de la copia rechazada conservados y ausencia de nuevas reservas tras recuperar. No se modificó código productivo.

## Archivos modificados

- `app/src/test/java/com/tuempresa/inventariovial/PhotoTransport10Test.kt`: únicamente fixture y aserciones adicionales.
- `local.properties`, ignorado: únicamente sdk.dir; las demás propiedades se verificaron iguales en memoria y sus líneas se conservaron.
- Este informe.
- Ejecutores, logs y evidencias nuevos bajo `tmp/p03-20261030/`, ignorado. El informe P0.2 previo permanece sin modificar. No se hizo commit ni push.

## Resultados exactos de pruebas

Ejecutores derivados del init de migración, offline, V2 legítimo habilitado, sin cambiar flags. Cada fase tiene directorio nuevo, XML copiados tras ejecución y sandbox propio para las capturas/salidas relativas de pruebas históricas. No se sobrescribieron evidencias de fases anteriores.

| Selección | Casos | PASS | FAIL | Errores / omitidas |
|---|---:|---:|---:|---:|
| PhotoTransport10Test, clase completa | 6 | 6 | 0 | 0 / 0 |
| Selección completa P0.2 | 77 | 77 | 0 | 0 / 0 |
| Regresiones adicionales | 62 | 56 | 6 | 0 / 0 |

Los seis casos JPEG están incluidos en los 77: no se suman dos veces. Entre las dos selecciones hay **139 casos distintos: 133 PASS y 6 FAIL**. No se declara aprobada la suite completa.

Las 12 clases de la selección de 77 se ejecutaron completas: PavementAnnotationSync147Test (1), ScapFinalP0Test (9), SyncRecovery10DTest (7), HotfixPerformance146Test (3), AutomaticSyncRegressionTest (21), DriveDeliveryPresentationTest (11), DriveDeliveryCardUiTest (4), DriveDeliveryListsTest (6), DriveDeliveryListsUiTest (3), ReadablePhotoNames10Test (4), PhotoTransport10Test (6) y ScapHttpAuthorizationP0Test (2).

Selección adicional para comprobar incompatibilidades históricas, trabajo de Work, SIC y SCAP:

| Clase | PASS / casos | FAIL |
|---|---:|---:|
| AutomaticDocuments149Test | 14 / 17 | 3 |
| Engineering2DataTest | 2 / 4 | 2 |
| LocalDraftDiscard10CTest | 7 / 7 | 0 |
| ContractualLength111Test | 6 / 6 | 0 |
| RecordDateFilterTest | 2 / 2 | 0 |
| PavementP0AdditionalTest | 5 / 5 | 0 |
| MixedSic17HotfixTest | 6 / 6 | 0 |
| ScapUrgentIntegrityTest | 14 / 15 | 1 |

Evidencia de cierre: `tmp/p03-20261030/cierre/` (`jpeg-clase.log`, `xml-jpeg/`, `tests.log`, `xml/`, `resultados.json`, init, evidence y sandbox). Primera ejecución satisfactoria conservada en `validacion/`; adicionales en `adicionales/`. La repetición de cierre conserva el código de nombre FAJ usado por la referencia V21 y la aserción de familia FAJA. Resultados repetidos no se cuentan como casos nuevos.

La guarda `validateReleaseConfiguration` también pasó, con el certificado histórico, sin bypass. No se ensambló, instaló ni distribuyó APK. Los ensayos fueron exclusivamente locales con datos ficticios.

## Estado SDK

sdk.dir apunta ahora a `C:/Users/ASUS/AppData/Local/Android/Sdk`. Se comprobó la plataforma instalada y la igualdad del resto de propiedades antes de escribir.

Gradle se ejecutó en procesos cmd nuevos, con daemon de un solo uso y **sin ANDROID_HOME ni ANDROID_SDK_ROOT en el entorno del proceso**. JAVA_HOME apuntó al JBR ASUS. Las pruebas completas y la guarda Release funcionaron sin rutas Huawei ni avisos de SDK inexistente. No se modificaron AGP, KSP, Kotlin, Gradle ni versiones Android. No se requiere una ruta de SDK externa a local.properties para repetir la validación.

## Bloqueos restantes

Los seis fallos adicionales se preservan sin alterar aserciones ni fuentes:

1. `AutomaticDocuments149Test.scapWithoutPrStillBuildsAllSketchesAndOriginalsOffline`: falla una expectativa de disposición de archivos locales (línea 91).
2. `AutomaticDocuments149Test.confirmedAckDoesNotRenamePublicFolderOrMoveEarlierPlan`: espera ruta confirmada; obtiene null.
3. `AutomaticDocuments149Test.interruptedLocalPublicationResumesWithoutDuplicatingMediaWhenDriveIsBusy`: espera READY; obtiene PARTIAL.
4. `Engineering2DataTest.editingOnePercentagePreservesHistoricalConditionsWithoutValueRows`: espera 80.0; obtiene 0.0.
5. `Engineering2DataTest.sevenPiersFixedStructuresAndObservationsSurviveReopenWithoutMaterializingReminders`: espera condición inexistente; encuentra condición inicial con ceros.
6. `ScapUrgentIntegrityTest.scapFiveAnnexesKeepUuidPreparedBytesAndAckAcrossOfflineBusyAndLostResponse`: el tercer drenaje no devuelve el reintento esperado (línea 145). El mock usa nombres `001_PUE1_GEN.jpg`; el contrato de panel oficial V2 exige nombres por categoría, como número/abreviatura/índice. Esa incompatibilidad del mock requiere conciliación específica; el caso no acredita su recuperación final ni demuestra por sí solo un fallo productivo.

Los primeros cinco son antecedentes documentados que se reprodujeron; deben conciliarse contra el comportamiento aprobado antes de concluir si corresponde modificar fixture, expectativa o implementación. No se corrigieron dentro de esta intervención limitada al fixture JPEG.

Siguen sin verificarse las capacidades reales del despliegue, las versiones/configuración efectivas de las tablets, su revisión/ACK y los archivos en destino. No se declara resuelto el incidente de Flavio ni se autoriza distribución por el PASS de los 77 casos.

## Checklist corto — Flavio y Julián

Usar la tabla comparativa existente en `GBO_P01_ENTORNO_Y_VALIDACION.md`. Solo con autorización de lectura; actualmente no se accedió a los dispositivos.

1. **Versión instalada:** anotar versión/versionCode, procedencia del build y hora de captura en cada tablet. No deducir flags de la versión ni instalar otra APK para recogerlos.
2. **Contexto:** leer sesión/rol, permisos pertinentes, red y restricciones de batería; comparar en la misma ventana. No cerrar sesión, cambiar permisos, alternar conexión ni modificar configuración.
3. **Bloqueo:** copiar el código y mensaje visible de Flavio, saneando nombres, IDs y enlaces. Anotar pantalla, filtros, estado del trabajador y cantidad de fichas pendientes. No pulsar Sincronizar para obtener el diagnóstico inicial.
4. **Revisión y ACK:** anotar revisión local, preparada y confirmada de la ficha correspondiente. Confirmado exige ACK íntegro de la revisión actual; staging 16/16 o un ACK anterior no bastan. Si la UI no permite acreditarlo, marcar No verificable sin exportar payload/base de datos.
5. **SCAP/SIC esperados:** desde el detalle disponible, listar archivos preparados/confirmados, fotos/croquis y videos por separado; formatos sicRows, pendingSic y sicFiles/retainedSicFormats cuando sean observables. El consolidado SIC puede estar en EXCEL_SIC, no dentro de PUENTE n. Contrastar carpeta/ruta y archivos en Drive únicamente si se autoriza acceso de solo lectura; sin ese permiso queda pendiente, sin subidas ni creación de fichas.
6. **Compatibilidad y evidencia faltante:** registrar los flags/capacidades ya observados y su fecha (protocolo 3, V4, nombres V2/panel). No hacer nuevas peticiones ni asumir valores si no hay diagnóstico visible. Antes de cambiar APK deben conocerse ambas versiones, el motivo de bloqueo, las revisiones y la evidencia de ACK/archivos; identificar explícitamente cada dato todavía desconocido.

No extraer secretos, bases, fotos reales ni APK. Las capturas, si se autorizan, deben recortarse/sanearse localmente antes de compartirlas. No instalar, reenviar, editar o borrar fichas para completar esta tabla.

## Condiciones para iniciar P0.4

- Puede prepararse la comparación de campo de solo lectura con la ficha anterior; su ejecución exige autorización expresa para ambas tablets y para cualquier lectura de Drive.
- P0.4 debe clasificar los seis fallos adicionales y conciliar primero el mock V2 de ScapUrgentIntegrityTest, sin desactivar V2 ni relajar ACK. Si se confirma un defecto productivo, proponer una corrección acotada y su regresión antes de implementarla.
- Una futura instalación/envío necesita autorización separada, candidata con identidad/versión revisadas y destino de pruebas aprobado. No cambiar APK, cuenta, raíz, V21 o permisos basándose solo en resultados locales.
- No hay GO de distribución general ni afirmación de resolución del caso Flavio: falta evidencia física de ACK vigente y archivos SCAP/SIC completos en el destino esperado.
