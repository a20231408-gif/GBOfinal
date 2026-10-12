# Prompt 8A — Protección del Prompt 7 e integración Android V4

## Base protegida

- Prompt 7 publicado en `codex/flujo-operador-7`, commit `28f9549813b0bf5753c70d4967970fd10e0f74af`. Árbol limpio al empezar; `git push` informó Everything up-to-date y `git ls-remote` confirmó el mismo hash. Sin cambios pendientes que incorporar.
- Fuentes y recursos indispensables registrados: detalle/galería/exportación, pruebas, plantillas SCAP y mapas de referencia, reglas de compilación y evidencia del Prompt 7. No se detectaron fuentes pendientes fuera de Git.
- Rama 8A: `codex/integracion-android-8A`, creada exclusivamente desde ese commit. El hash final se entrega después del commit/push y se verifica contra la punta remota; nunca desde main.

## Causa y corrección SCAP

La prueba histórica `ScapPendingNavigationTest.tappingRealPendingButtonsReachesIdentificationSupportTwoGlobalPhotoAndMetrado` buscaba `section=A, key=bridgeName`. El catálogo canónico actual de losa define en A `categoryName` (valor inicial PUENTE) y `bridgeCode`; el nombre particular está en B como `singularityName`. `StructuredScapCompletion` genera pendientes a partir de ese catálogo y no produce el destino solicitado por el fixture.

Se cambió únicamente el destino A del fixture a `bridgeCode`, vacío en la inspección nueva. Se conservaron los destinos reales de apoyo 2/material, descripción global, fotografía y metrado. La prueba pulsa los botones de pendientes, verifica el campo visible, ruta y foco. Las capturas generadas se guardan en `app/build/outputs/8a-scap-pending/`, fuera de los documentos históricos rastreados.

El primer intento tras esa corrección reveló una segunda causa de preparación: el `Application` aislado no inicializaba WorkManager, requerido por el ViewModel. Se inicializó en la prueba, igual que en el fixture UI del Prompt 7. No se modificaron reglas, catálogos, modelos ni navegación SCAP. El caso completo aprobó. También aprobaron tres regresiones focalizadas de estructuras/valores y cierre con/sin consentimiento.

## Contrato Android V4

El cliente existente ya comprueba en GET `ok=true`, `syncProtocol=3`, `pathVersion=4`, `stableFamilyNumbering=true`, `photoCategoryVersion=1` y `technicalDeliveryWithoutGps=true`. Las primeras capacidades bloquean envíos incompatibles; la entrega técnica sin GPS exige su capacidad antes de publicar archivos.

`AutomaticDelivery` conserva familia explícita sin SIB, UUID, revision/baseRevision, categorías, hash y claves de archivo. `validAck` exige identidad/revisión/hash, carpeta/ruta numerada, archivos confirmados y consolidado en `<ruta>/EXCEL_SIC/<SIC>.xlsx`. Un ACK anterior puede avanzar la base confirmada sin ocultar una edición posterior. La cola, worker, revisiones y conflictos son los existentes.

Único ajuste productivo: constructor interno de `AutomaticDriveHttp` permite proporcionar cliente HTTP, endpoint y token a pruebas. Los valores predeterminados siguen siendo los de DriveConfig y el mismo cliente/timeouts. Las pruebas interceptan respuestas antes de llegar a la red; no leen credenciales ni contactan endpoints reales. No se agregó otra sincronización ni se cambió el contrato.

## Matriz focalizada A–I

| Caso | Evidencia ejecutada | Resultado |
|---|---|---|
| A. Borrador incompleto | `AutomaticSyncWithoutGpsTest.incompleteDraftsAreNeverDeliveredEvenWithOldGpsError` | DRAFT conservado, cero POST definitivos |
| B. Validado automático | `AutomaticSyncRegressionTest.validatedSicQueuesInSameLocalTransactionWithoutNetwork`, `duplicateClosureSchedulesOneStableRevision` | Cola local atómica y una identidad/revisión estable |
| C. Offline | Nuevas `offlineFailurePersistsSnapshotAndSameIdentityAcrossReopenAndRecovery` y existente `workManagerKeepsOfflineDrainEnqueuedWithNetworkConstraintAndSafeBackoff` | Fallo de comunicación sin ACK; instantánea/foto/observación persisten al reabrir; worker espera red con backoff |
| D. Recuperación | Nueva offline y `lostResponseRetriesSameRevisionAndIdsWithoutCreatingAnotherDelivery` | Misma identidad/revisión, una mutación simulada, reintento idempotente |
| E. Incompatibilidad | Nueva `v4HttpCapabilitiesRejectEachIncompatibleResponseBeforeAnyPost` | Siete respuestas GET incompatibles: UNCONFIGURED, cero POST/confirmaciones; respuesta compatible reconoce las capacidades |
| F. ACK exacto | Nueva `wrongAckProtocolIdentityRevisionAndPathsNeverConfirmCurrentDelivery`; `SyncedRecordEdits2BTest.editDuringUploadRetainsLateAckAsBaseAndThenDeliversNewerRevision` | Ocho ACK corruptos rechazados; ACK válido confirma su revisión, edición posterior sigue pendiente |
| G. Edición sincronizada | Caso anterior y `lostAckThenEditReplaysExactPredecessorWithoutConflictingWithOwnDelivery` | Misma identidad, base previa y revisión posterior, sin conflicto con entrega propia |
| H. Categorías/referencias | `PhotoCategories5Test.recategorizationKeepsIdentityAndRecordsExplicitChange`, `historicalNullAndGeneralShareOneStableCategoryOrder`; `DriveFamilies2ATest.categoriesUseRealCodesAndAllowFutureKeysWithoutGuessingFileNames` | UUID/historial originales, GENERAL histórico y orden contractual; rutas/categorías conservadas |
| I. Conflictos | `SyncedRecordEdits2BTest.operatorConflictPersistsLocalEditAndCannotBeClearedByAnotherSave` | Edición local conservada, CONFLICT persistente, sin reenvío automático forzado |

`DriveFamilies2ATest.allSecondaryClassesHaveAnExplicitFamilyAndNoSib` también aprobó. En los ACK corruptos se comprueban revisión/protocolo/identidad/hash y rutas de foto/Excel; se conserva la foto original sin fileId confirmado y se permite recuperación con ACK válido. Estas respuestas son dobles explícitos de pruebas, no evidencia del servidor V4 actualizado por el otro responsable.

## Ejecución y compilación

**19 pruebas Android distintas aprobadas, 0 pendientes fallidas al finalizar.** Se hicieron 20 ejecuciones de casos: primer lote 3/4 aprobado (fallo de preparación WorkManager), lote corregido 14/14 y último lote de categorías 2/2. No se repitieron los tres casos SCAP ya aprobados. Resultados por caso/intento en [VALIDACION.json](VALIDACION.json); XML y logs crudos quedan localmente en carpetas ignoradas.

`:app:assembleDebug -PgboValidationBuild=true` aprobado con el lote corregido. No se ejecutó suite completa, Node/backend ni auditoría general.

Entorno de proceso usado en este equipo:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:JAVA_TOOL_OPTIONS='-Djdk.net.unixdomain.tmpdir=C:\PROYECTOS\GBO_FINAL\GBOfinal\tmp'
```

Los filtros exactos por intento se registran en VALIDACION.json. APK local: `app/build/outputs/apk/debug/app-debug.apk`, SHA-256 `deab6dcc1c14b845a78c0e2bc1a1abf510ca25ca790bfc6323ccbad79dcf9e23`. Identidad comprobada: `com.tuempresa.inventariovial`, versionCode 16, versionName 1.4.2. Firma Debug `b9fe89ed4ea039befde6a01c341559520e0739fe834f4222f4f185c5fc8db0ec`, igual a Prompt 7 local y distinta de la histórica del otro equipo. No se modificó keystore ni configuración de firma.

## Entrega y pendientes

[PRUEBA_INTEGRADA_8B.md](../PRUEBA_INTEGRADA_8B.md) prepara el endpoint privado de pruebas, compilación correspondiente, recorrido SIC/fotos/Excel/edición/offline, SCAP/pavimentos, estados y criterios de aceptación. El APK con `gboValidationBuild=true` omite endpoints y credenciales; no es el APK configurado para enviar a Drive.

Pendientes del responsable de 8B: confirmar despliegue V4 aislado, preparar Debug con sus propiedades autorizadas e instalarlo solo en dispositivo secundario/emulador compatible, ejecutar la guía y recopilar evidencia física/remota. No se consultó Drive, desplegó Apps Script, instaló APK ni inició 8B.

Apps Script, servicio de usuarios, Room 18, applicationId, versionCode, firma y archivos del Prompt 7 permanecen protegidos. El alcance productivo se limita al punto de inyección HTTP de pruebas; se detiene 8A tras publicar su rama.
