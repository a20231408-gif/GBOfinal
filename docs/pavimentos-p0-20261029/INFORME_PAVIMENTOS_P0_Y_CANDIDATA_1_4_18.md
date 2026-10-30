# Pavimentos P0 — candidata GBO Ruta Clara 1.4.18 (32)

Fecha: 29/10/2026. Proyecto: `D:\Downloads\GBO_PRUEBAS_8B`.

**Compilación: PASS.** `applicationId=com.tuempresa.inventariovial`. Configuración Release real; sin `gboValidationBuild`. Firma histórica verificada contra 1.4.17 (31), conservada como respaldo.

APK: `D:\Downloads\GBO_PRUEBAS_8B\output\GBO_RUTA_CLARA_1.4.18_32_RELEASE_CANDIDATA\GBO_Ruta_Clara_1.4.18_32_RELEASE_CANDIDATA.apk`

SHA-256: `3fe18f61f1b3455e51553b263292e40dc7d593cd7df5c6ad2c58e848c2a3edc7`

Certificado SHA-256: `0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315`.

## Implementado en Android

| Solicitud | Resultado |
|---|---|
| SIC-14, SIC-14A, SIC-15 y SIC-16 sin ubicación contractual | Faja, PR inicial/final y sus distancias opcionales. Vacíos quedan vacíos. Cero introducido voluntariamente se conserva como cero; PR con ceros iniciales se conserva como texto. No se infiere ubicación ni longitud contractual. |
| Otros datos pendientes | Excel individual exportable con campos vacíos y estado provisional. Los datos técnicos existentes se conservan. No se inventa tipo SIC para un daño cuyo equivalente no está aprobado. |
| Orden local | Fecha de registro persistida `sample.fecha`, luego `damage.createdAt`, luego UUID. La edición conserva createdAt. No usa PR, calzada, fecha de subida o exportación. Reexportar actualiza el mismo daño sin duplicar filas. Solo cambia Pavimentos. |
| Daños de junta | Código **26, sello de junta**, admite ocurrencias independientes L/M/H, UUID, segmentos/cruces interiores válidos, geometría, longitud referencial y fotografías. Acciones «Agregar otro daño de junta», editar y eliminar individualmente. Mismo segmento y defecto repetidos se rechazan; otro defecto explícitamente descrito u otro segmento puede coexistir. |
| Otras reglas | 25 y 39 ya admitían varias ocurrencias y conservan sus reglas por losa. El global 84 es polvo, no una junta, y mantiene su restricción. No se cambian grieta D, parches, escalonamiento o berma. |
| Conservación | Room **20 → 21**, una columna nullable `pavement_damages.retiredAt`. Retirar una ocurrencia 26 la excluye del cálculo activo y del consolidado nuevo, pero conserva la fila, originales y referencias fotográficas; los respalda en `DANOS_ARCHIVADOS.json`. No se renumeran correlativos históricos. |
| PCI | Una evaluación global manual, independiente de las ocurrencias; no suma, promedia ni toma su máximo. Sin evaluación: «Pendiente de evaluación global», índice no definitivo y archivos provisionales; se permite guardar, exportar y respaldar. El global histórico se conserva al añadir juntas. Un cambio individual no borra la evaluación global. El Excel separa las observaciones individuales de una única fila global PCI. |
| Pendientes y ACK | Se conserva la preparación congelada, UUID, revisión y ACK. El nuevo campo nullable no altera la huella de daños históricos sin editar. Una entrega solo se confirma con ACK completo verificado. |

El catálogo incorporado (`app/src/main/assets/pavement/catalogo_danos.json`, daño 26, manual pp.150–153) dispone una evaluación general de la unidad; no contiene un algoritmo aprobado para convertir varios L/M/H individuales en un global. Se respeta además la aclaración expresa del propietario. Las curvas y fórmulas PCI/URCI no se modificaron; curvas no validadas siguen dejando el resultado pendiente.

## Drive: respaldo individual y consolidado son resultados diferentes

V21 exige PR/distancia numéricos en cada `sicRows`. Por eso la APK **no envía filas incompatibles** ni ceros ficticios al consolidador V21. Entrega los Excel SIC como archivos del padre en:

`<RUTA>/EVALUACION_SUPERFICIAL/<TRAMO>/<SUPERFICIE>/MUESTRA <n estable>/DATOS_APP/SIC-14.xlsx`

Análogamente SIC-14A, SIC-15 o SIC-16 según la superficie. Conserva PDF, Excel nativo, JSON y fotografías. El ACK acredita esos archivos individuales; **no acredita un consolidado remoto actualizado**. La interfaz distingue respaldo confirmado de consolidado cronológico pendiente. El Excel consolidado remoto anterior no se borra ni se sustituye silenciosamente.

El cliente reconoce dinámicamente `pavementSicChronologicalVersion=1`. La carpeta `backend-propuesto-NO-PUBLICAR` contiene el helper nuevo, SyncV3Core completo derivado de V21, diff y fragmento exclusivo de capacidades para Código.gs. No se publicó nada ni se cambió ScriptLock, staging, numeración, fotos V2, SCAP, videos, Observaciones o ACK.

**Pendiente remoto:** ensayo Google y conciliación de fechas/horas de filas históricas. No se fabrican tiempos a partir de revisiones, carpetas o Drive. El candidato de servidor rechaza una mezcla sin metadatos acreditados antes de comenzar la transacción. **NO-GO para publicar ese parche todavía.** Las preparaciones antiguas no se reescriben automáticamente: una revisión ya preparada o confirmada necesita una edición/revisión explícita para incorporar documentos nuevos.

Los consolidados locales reconstruyen la fecha/hora desde Room. Las filas previamente retenidas cuyo origen ya no está disponible se conservan, se marcan provisionales y se colocan al final con desempate estable; no puede acreditarse una cronología completa de esas filas sin conciliar su origen.

Los límites remotos siguen siendo 80 archivos por revisión, 24 MiB por staging y 160 MiB por revisión. Los SIC documentales añaden uno o dos archivos por muestra y el manifiesto de retirados añade otro cuando corresponde. Si se supera un límite, se conserva el respaldo local y queda pendiente la entrega; no se descartan fotografías para hacerla pasar.

## Pruebas automatizadas locales

**94 casos focalizados Android/Room/Compose sobre Robolectric/JVM: PASS, 0 fallos finales, 0 omitidos. 6 pruebas Node del backend simulado: PASS.** Repeticiones se deduplican por clase/método. Evidencias: `evidencias/PRUEBAS_FINALES.json`, XML de cada fase y `backend-tests-final.log`.

Incluyen los cuatro SIC vacíos y con valores voluntarios, cronología y desempates, edición/reexportación, tres juntas L/M/H a través de Compose y Room, cierre/reapertura, edición individual, retiro con fotos conservadas, global histórico y manual independiente, PCI sin triple deducción, Excel/PDF, migraciones desde versiones entregadas, BUSY, pérdida de ACK y reintentos. El ensayo cruzado usa los XLSX y JSON producidos por Android: hash SHA idéntico en cliente y Core propuesto; la nulidad nueva queda canonizada sin cambiar el hash de solicitudes antiguas.

Las pruebas V21 con Drive en memoria verifican el ACK de todos los documentos sin PR y el reintento sin duplicación; el candidato ordena A/B/D/C según fecha/hora/identidad aunque sus PR indiquen otro orden. **Son simulaciones locales: no constituyen una entrega real ni mediciones Google.** Compose/Robolectric y SQLite local tampoco sustituyen una prueba física del PDF nativo en la tablet.

### Barrido histórico adicional: cinco fallos no conciliados

Se conservan completos los resultados fallidos; no se declara PASS de toda la suite histórica. Tres casos `AutomaticDocuments149Test` esperan una salida/localización/estado que requiere conciliarse con el flujo previo de exportación provisional y ACK estricto. Dos `Engineering2DataTest` esperan ausencia de condiciones iniciales o una condición única al agregar una fila «legacy» a un elemento nuevo que ya tiene ceros. Esas fuentes funcionales SCAP y LocalCopies son idénticas a las del comienzo de este encargo; no se modificaron para forzar los tests. Véase `evidencias/HISTORICAS_NO_CONCILIADAS.json`. Su análisis general queda fuera de estas dos correcciones y no se certifica distribución general.

También se corrigió el fixture de migración de PhotoCategories5: debe reconstruir la tabla histórica antes de probar 17→21; fingir `user_version=17` sobre la tabla 21 provoca una columna duplicada. La migración productiva no se volvió permisiva ni destructiva.

## Compilación y recursos

El primer intento se interrumpió en R8 por falta de memoria nativa de la JVM, confirmado en el registro de error. Se conservó la evidencia y se realizó un único reintento limitando concurrencia y ajustando la ubicación virtual del heap. Fueron opciones temporales del proceso de compilación; no cambian archivos privados ni banderas funcionales de GBO. El resultado final de compilación y verificación se recoge arriba y en `evidencias/VERIFICACION_RELEASE.json`.

## Conservación y archivos

La copia reversible de fuentes iniciales está en `tmp/pavimentos-p0-20261029/RESPALDO_ANTES.zip`. El diff de esta tarea está en `CAMBIOS_ANDROID_PARA_REVISION.patch`; la lista exacta y hashes, en `evidencias/INTEGRIDAD_Y_CAMBIOS.json` y `FUENTES_COMPILADAS_SHA256.json`.

Cambios principales: PavementSic y su panel; LocalSicConsolidates/LocalExportBuilder; PavementJointOccurrences y PavementDamageArchive nuevos; modelos/DAO/repositorio/cálculo/exportadores/esquema/interfaz de Pavimentos; AutomaticDelivery/AutomaticSync y presentación del estado de respaldo; migración 20→21; pruebas y numeración 32. Se ajustaron expectativas de versión de los tests migratorios, conservando sus comprobaciones de columnas y datos.

Las APK anteriores, propiedades privadas, firma, backend y esquemas históricos permanecen intactos. **Excepción documentada:** un test histórico regeneró tres XLSX exclusivamente ficticios en `output/GBO_RUTA_CLARA_CANDIDATA_1.4.9_23/ejemplos-ficticios`. No se recuperaron sus bytes previos; se registran los hashes antes/después y se aislaron desde ahora las salidas del test. La APK 1.4.9 y todas las demás APK se conservaron. No se tocaron registros reales ni referencias entregadas por ingenieros.

No se instaló ni distribuyó la APK, no se modificó Google, no se hicieron subidas reales y no se realizó commit/push.

## Dictamen

**GO únicamente para validación física controlada de la candidata. NO-GO para distribución general o para publicar el consolidado cronológico remoto sin las comprobaciones pendientes.** Seguir `CHECKLIST_TABLET.md`; una entrega Drive real necesita autorización separada y ACK verificable.
