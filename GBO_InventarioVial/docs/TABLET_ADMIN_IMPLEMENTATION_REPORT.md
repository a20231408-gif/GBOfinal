# TABLET_ADMIN · estabilidad · SCAP — reporte de esta iteración

1. **Base exacta:** checkout recibido `D:/Downloads/GBO_InventarioVial_INTEGRADO_FINAL_SOURCE/GBO_InventarioVial`, versión más reciente de paridad SIC v13. Snapshot de los 587 archivos recibidos y SHA-256 en `tablet-admin-stability-scap-evidence/source-before.json`. No se volvió a un APK/ZIP anterior ni a otra rama. No existe repositorio Git en este checkout.
2. **versionCode:** 13 → 14; `versionName=1.4.0-sic-production` conservado. El DEBUG v13 entregado antes era SHA-256 `ac03782a8267176119c9425c8b21934f6b2687f7ea374286b324233849300b03`. El máximo conocido era 13; no se reutiliza.
3. **Room:** 15 → 15. Todos los archivos `app/schemas` conservan el SHA-256 recibido.
4. **Migración nueva:** no. Se reutiliza `scap_values` para estado, nombres, códigos, descripción global, fechas y metadata de entrega. `TABLET_ADMIN` es una capacidad explícita en servidor; Android conserva únicamente una pista local para mostrar el botón.
5. **TABLET_ADMIN:** PIN del usuario logueado, tablet válida, usuario activo y permiso vivo en servidor. Sesión exclusiva `TABLET_ADMIN_ONLY`, 900 segundos, token en RAM en Android y hash en servidor. Cada operación verifica nuevamente identidad, vigencia, scope, revisión del usuario y permiso. No concede administración de usuarios ni acceso a propiedades del script. Alta/revocación/reactivación/rotación son centrales, bajo `LockService`; self-revoke bloqueado tanto en UI como servidor. Alias ajeno a autenticación; lista con campos permitidos sin hashes. Historial visible con huella abreviada. Se conservan hashes al revocar y reactivar. Limitación 5 intentos de autorización/5 minutos por tablet y 60 operaciones/minuto por sesión de tablet, separada del login normal.
6. **User Service:** `users-v2.4-tablet-admin`; archivos modificados `Code.gs`, `Users.gs`; nuevo `TabletAdmin.gs`, tests y README actualizados. Acciones: `authenticateTabletAdmin`, `listTablets`, `provisionTabletAdmin`, `revokeTabletAdmin`, `reactivateTabletAdmin`, `rotateTabletToken`, `getTabletAudit`. Owner ejecuta `applyTabletAdminPermission()` mediante propiedad temporal con `userId/enabled/reason`; no está en doPost. **Owner debe actualizar manualmente el deployment existente** siguiendo `TABLET_ADMIN_SERVER_DEPLOY.md`. No se desplegó desde esta tarea. Compatibilidad de acciones anteriores y `TABLET_<deviceId>` conservada. El suministro del nuevo token a la tablet destino se verifica online con la misma URL y huella antes de reemplazar su credencial cifrada; no hay alta automática.
7. **Seguridad de entrega:** Verificado: APK sin referencia a SERVER_RANDOM_KEY en DEX, sin código Apps Script/provisioning Owner, endpoints privados de BuildConfig vacíos y sin valores de credenciales operativas disponibles localmente. ZIP excluye configuraciones privadas/keystores y compara sus contenidos contra esos valores sin imprimirlos. Las claves del servidor que no existen en este PC no se descargaron ni inventaron. La evidencia está en `tablet-admin-stability-scap-evidence/artifact-verification.json`. Los tokens generados por alta/rotación solo aparecen una vez en memoria del panel; se descartan al cerrarlo. Las credenciales propias de la tablet continúan protegidas por Keystore. No se generó keystore ni se cambió applicationId, signingConfig o SHA esperado de release. APK: GBO_InventarioVial_TABLET_ADMIN_STABILITY_SCAP_DEBUG_v14.apk; 39731249 bytes; SHA-256 `4096a50a53966ca7c9011d2013923789891e518610f089dd26c2d0fa504b8b92`; certificado debug `d2e54cb086aa731ef046be300977d0b16c0d8f66fdcb08d0d9cfd2038bf0d872`; versionCode 14.
8. **Estabilidad:** sesión normal se bloquea después de 15 minutos en background, conserva la ficha y pide el PIN; no hay timeout por uso continuo. Admin usuarios conserva 5 minutos, TABLET_ADMIN 15 minutos. Ninguna expiración mata proceso/Activity. No hay evidencia de causa de cierres en tablet: falta prueba física y última salida real. Se agregan `ApplicationExitInfo` cuando está disponible y fallback local sanitizado, diagnóstico JSON acotado a 64 eventos. Cache de señales/miniaturas acotada, reducción de fotos Excel y transferencia por flujo de JPEG/Excel. Mapa usa miniatura limitada. Detalle en `SESSION_LIFECYCLE_AUDIT.md` y `APP_EXIT_DIAGNOSTIC_REPORT.md`.
9. **SCAP:** fuente física original: LOSA 17 elementos F.1, MODULAR 10. F.1 agrega descripción global y nombres/códigos editables sin perder identidad interna; UNSET/PRESENT/NOT_APPLICABLE. G.1 proyecta la misma colección exceptuando No aplica; cantidades/calificaciones vacías no toman ejemplos. Manuales y No aplica/reactivar preservan datos. Apoyos 1 y 2 iniciales, LEFT/RIGHT, apoyo 3+ y continuación exportada. F.2 es únicamente panel fotográfico en las fichas LOSA/MODULAR: 31 nombres únicos de los 32 ejemplos de cada XLSX, nombre/desc/fecha editables, cámara/importar/reemplazar/preview/remover/reordenar. El flujo fotográfico histórico queda compatible para fichas sin tipo canónico.
10. **Pendientes clicables:** `ScapFieldRoute(section,subsection,repeatableItemId,fieldKey)` y UUID estable; cambio de sección, posición de lista, `BringIntoViewRequester`, foco en texto y resaltado 1.8 s. Rutas emitidas junto al campo, sin extraer destinos de mensajes. Nuevos tests con botones reales del editor para A, apoyo 2, F.1, F.2 y G.1; capturas bajo `tablet-admin-stability-scap-evidence/screenshots`.
11. **Export incompleto:** warnings MISSING_VALUE y confirmación; entradas faltantes vacías. INVALID_VALUE que impediría datos válidos bloquea entrega, pero conserva texto crudo en borrador. Null se interpreta como cero exclusivamente al calcular; Excel conserva null. Suma distinta de 100% es warning, sin resultado global inventado. La fuente mantiene sus fórmulas y referencias #REF! originales. Continuaciones amplían el área de impresión del libro generado, conservando los rangos anteriores y todos los demás defined names; fuentes XLSX intactas.
12. **Drive incompleto:** confirmación requerida; metadata `completion.status=INCOMPLETE` y `uploadedWithWarnings=true`, separada de syncStatus. DRAFT/IN_PROGRESS pueden enviarse con revisión explícita, no se congelan al ACK. Se conserva UUID, fileId/folderId, fotos y versiones. Entrega con ubicación completa conserva v2; respaldo v1 existente para ubicación incompleta, destino mostrado explícitamente, sin inventar ruta. Ningún contrato remoto se cambió/desplegó. DEBUG validation carece de endpoint/token, por lo que la subida remota real queda en checklist físico de entorno configurado.
13. **Tests:** Baseline real: 370 pruebas; 355 PASS y 15 FAIL. Última ejecución registrada: 380 pruebas; 365 PASS y 15 FAIL; nuevos fallos: 0; identidades anteriores ausentes: 0. User Service: baseline 21 escenarios PASS, final 41 escenarios PASS (runner Node 1 archivo, 0 FAIL), `tablet-admin-tests.log`. Cambios deliberados de expectativas SCAP documentados abajo; no se quitaron identidades de tests. Logs reales baseline y final, JSON por identidad y evidencia de plantilla conservados.
14. **assembleDebug:** `BUILD SUCCESSFUL`, comando `gradlew.bat assembleDebug --no-configuration-cache --rerun-tasks -PgboValidationBuild=true` con JDK existente de Android Studio.
15. **Limitaciones:** aún se requiere deployment Owner, validación 30–60 minutos/actualización in-place en la tablet real y revisión Excel de las inconsistencias originales. DEBUG solo validación, no reemplaza release corporativa. La keystore release histórica no está disponible; no se creó otra. Límites físicos conservados para 31 filas G.1, 32 registros F.2, dos tramos, tres pilares, una junta, 15 puntos de perfil y un croquis por tipo; no se silencian excedentes. No commit, push ni despliegue automático.

## Identidades FAIL anteriores

- `com.tuempresa.inventariovial.Engineering2DataTest#sevenPiersFixedStructuresAndObservationsSurviveReopenWithoutMaterializingReminders`
- `com.tuempresa.inventariovial.Engineering2ScreenTest#f2ShowsPendingDefaultsAndEditingAnObservationPersistsWithoutAssertingPresence`
- `com.tuempresa.inventariovial.EngineeringUxTest#versionSixMigrationKeepsAllHistoricalDataAndPromotesLegacyJoint`
- `com.tuempresa.inventariovial.InventoryRoomMigrationTest#migrationsFromEveryReleasedVersionPreserveRecordsPhotosAndSic23`
- `com.tuempresa.inventariovial.PrSuggestionStorageTest#migrationNineToTenRetainsRecordsPhotosAndStartSourceAndDefaultsEndToManual`
- `com.tuempresa.inventariovial.RoadAxisStorageTest#realVersionEightMigrationPreservesManualPrPhotosAndDetailsAndNewFieldsStartNull`
- `com.tuempresa.inventariovial.RoadReferenceDeliveryTest#categoryStylesShortNamesAndExtendedDataPreserveManualAndGeometricValues`
- `com.tuempresa.inventariovial.RoadReferenceDeliveryTest#compatibleAxisHasYellowTopBlackBorderIdenticalGeometryAndSidebarLegend`
- `com.tuempresa.inventariovial.RoundTwoStorageTest#migration10to11PreservesEveryColumnOfEveryLegacyTableIncludingSecurityAndScap`
- `com.tuempresa.inventariovial.ScapExportTest#runtimeIncludesExactTemplateAndExportPreservesSheetsStylesGeometryAndCalculationFormulas`
- `com.tuempresa.inventariovial.ScapFullExportTest#SCAP_COMPLETO_TEST_reopensWithAllFieldsSevenDistinctPiersAndOriginalGeometry`
- `com.tuempresa.inventariovial.ScapStorageTest#versionFiveMigrationPreservesSicRecordsPhotosMetadataAndNewCulvertFields`
- `com.tuempresa.inventariovial.SurveyStorageTest#versionFourUpgradeRetainsOldCulvertDimensionsAndMarksNewFieldsUnknown`
- `com.tuempresa.inventariovial.UserAuthenticationTest#fieldSessionOperatorComesFromAuthenticatedUser`
- `com.tuempresa.inventariovial.UserAuthenticationTest#migration7to8PreservesEveryExistingTable`

## Cambios deliberados de tests

Se mantienen nombres/identidades de los tests. Expectativas actualizadas: defaults de F.1 frente a filas de ejemplo G.1 (incluye mapeo MODULAR 510/504), fichas con 17/10 elementos frente a `.single()`, calificaciones/missing como warnings, null calculable como 0, continuación F.1 frente a rechazo de grupo y revisión/revisión temporal explícita antes del ACK. Siguen comprobándose límites técnicos, datos malformados, tipo incompatible, repetición de bytes ZIP, propiedad de fotos y ACK antiguo. La prueba existente de fotos de fichas históricas mantiene su contrato.

## Archivos fuente cambiados / nuevos

- `app/src/main/AndroidManifest.xml`
- `app/src/main/assets/scap_modular_catalog.json`
- `app/src/main/assets/scap_slab_catalog.json`
- `app/src/main/java/com/tuempresa/inventariovial/MainActivity.kt`
- `app/src/main/java/com/tuempresa/inventariovial/auth/AdminUsersScreen.kt`
- `app/src/main/java/com/tuempresa/inventariovial/auth/AuthGraph.kt`
- `app/src/main/java/com/tuempresa/inventariovial/auth/LoginScreen.kt`
- `app/src/main/java/com/tuempresa/inventariovial/auth/UserEntities.kt`
- `app/src/main/java/com/tuempresa/inventariovial/auth/UserServiceClient.kt`
- `app/src/main/java/com/tuempresa/inventariovial/auth/UserSyncRepository.kt`
- `app/src/main/java/com/tuempresa/inventariovial/field/TrafficSignVisualSelector.kt`
- `app/src/main/java/com/tuempresa/inventariovial/road/LiveRoadMap.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/calculator/ScapConditionCalculator.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/catalog/ScapCatalog.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/data/ScapDao.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/data/ScapRepository.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/domain/ScapBridgeType.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/domain/ScapCompleteness.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/domain/ScapPresentation.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/domain/ScapValidation.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/export/ScapCanonicalExporter.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/export/ScapDelivery.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/export/ScapDriveSync.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/export/ScapHumanDelivery.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/export/ScapImageOrientation.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/export/ScapTemplateMedia.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapController.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapElements.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapExportPanel.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapForms.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapMedia.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapWorkspace.kt`
- `app/src/test/java/com/tuempresa/inventariovial/ParallelBScapCaptureTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/ScapCalculationTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/ScapCanonicalExportTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/ScapDriveTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/ScapHumanDeliveryTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/ScapStorageTest.kt`
- `backend/user-service/Code.gs`
- `backend/user-service/README.md`
- `backend/user-service/Users.gs`
- `backend/user-service/tests/service.test.cjs`
- `app/build.gradle.kts`
- `app/src/main/java/com/tuempresa/inventariovial/auth/TabletAdminRepository.kt`
- `app/src/main/java/com/tuempresa/inventariovial/auth/TabletAdminScreen.kt`
- `app/src/main/java/com/tuempresa/inventariovial/camera/FieldThumbnailCache.kt`
- `app/src/main/java/com/tuempresa/inventariovial/diagnostics/StabilityDiagnostics.kt`
- `app/src/main/java/com/tuempresa/inventariovial/diagnostics/StabilityScreen.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/domain/ScapElementState.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/domain/ScapFieldRoute.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/domain/StructuredScapCompletion.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapFieldTarget.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapPhotoRecords.kt`
- `app/src/test/java/com/tuempresa/inventariovial/ScapPendingNavigationTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/TabletAdminAndDiagnosticsTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/TabletScapIterationTest.kt`
- `backend/user-service/TabletAdmin.gs`
