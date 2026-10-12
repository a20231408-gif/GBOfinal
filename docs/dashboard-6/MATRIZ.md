# Requisitos → implementación → verificación focalizada

Fuentes abreviadas bajo `app/src/main/java/com/tuempresa/inventariovial/`.
Pruebas bajo `app/src/test/java/com/tuempresa/inventariovial/`.

| Requisito | Archivos | Pruebas / evidencia |
|---|---|---|
| 1. Sesión activa visible | `dashboard/DashboardScreen.kt`, `DashboardHome.kt` | `Dashboard6ScreenTest.portraitSessionProgressiveAndAccessibleActions` |
| 2. Botón verde iniciar/configurar | `DashboardScreen.kt`, `field/FieldUi.kt` | Prueba anterior y `noSessionShowsStartAndDriveStatesAreDistinct`; captura vertical del botón real. |
| 3. Cuatro accesos principales | `DashboardScreen.kt`, `DashboardHome.kt`, `MainActivity.kt` | `landscapeLargeFontNavigationOfFourMainActionsAndTools`; callbacks de navegación real revisados en MainActivity. |
| 4. Herramientas secundarias | `DashboardHome.kt`, `FieldUi.kt` | Prueba de acceso a herramientas; delegaciones conservadas a mapa/Excel/SCAP/GNSS/KML/KMZ/archivos/cuenta. Verificación de ejecución en tablet pendiente. |
| 5. Borradores del día | `DashboardPresentation.kt`, `DashboardHome.kt` | `todayUsesRealSessionDate`, `localZoneSetsDayWithoutUtcAssumption`, `dayFilterKeepsOldInFullList` |
| 6. Días anteriores | `DashboardScreen.kt`, `DashboardPresentation.kt` | `historicalDraftUsesOriginalCreationNotModificationOrSurveyText`, `allDraftsFiltersHistoricalRowsAndContinuesOriginal` |
| 7. Fecha estable al editar | `InventoryDao.kt`, `FieldController.kt`, `core/InventoryDraftStore.kt`, `sic/SicFieldRepository.kt`, `MainActivity.kt` | `configureKeepsSessionDateOperatorIdentityAndSavedDraftContext`, `dateRemainsStableAfterEditingOldDraft`, ambos casos de persistencia de sesión histórica nula. |
| 8. UUID oculto | `DashboardScreen.kt`, `DashboardPresentation.kt` | `portraitSessionProgressiveAndAccessibleActions`, `progressiveDisplaysPaddedCodeAndDistanceWithoutUuid`; UUID conserva clave/callback interno. |
| 9. PR visible | `DashboardPresentation.kt` | `progressiveDisplaysPaddedCodeAndDistanceWithoutUuid`, `linearProgressiveIncludesEnd`, captura vertical. |
| 10. Orden creciente | `DashboardPresentation.kt` | `increasingOrderIsNumeric`, `catalogueSpecialOrderIsRespectedWithoutKmConversion` |
| 11. Orden decreciente | `DashboardPresentation.kt` | `decreasingOrderReversesPrAndDistance` |
| 12. Distancias dentro del PR | `DashboardPresentation.kt` | Casos creciente/decreciente con `0010+050.00` y `0010+125.50`. |
| 13. Dos registros igual PR | `DashboardPresentation.kt` | `equalProgressiveRetainsBothWithStableTie` |
| 14. Sin PR | `DashboardPresentation.kt`, `DashboardScreen.kt` | `missingProgressiveNeverFallsBackToIdentity`, `missingOrUnknownPrIsLastAcrossRoutes` |
| 15. Rutas/calzadas/sesiones distintas | `DashboardPresentation.kt`, `DashboardScreen.kt` | `routesBedsSessionsAndDirectionsStayDistinct`, `filtersSelectDateRouteBedAndTypeTogether` |
| 16. Muchos borradores, scroll contenido | `DashboardScreen.kt` | `fiftyDraftsScrollInsideBoundedListAndResumeExactData`: 50 filas, máximo 360 dp, última fila accesible. |
| 17. Continuar correcto, conservar datos | `DashboardHome.kt`, `MainActivity.kt`, stores existentes | Prueba de 50 filas y filtrado histórico conservan el registro original; `legacyDraftReopensWithPhotosObservationsAndNoInventedSession` comprueba texto incompleto, observación y categoría/ruta fotográfica al reabrir el store existente. |
| 18. Estado Drive comprensible y confirmado | `DashboardPresentation.kt`, `DashboardScreen.kt` | `syncCountsRequireAckAndExcludeIncompleteDrafts`, `noSessionShowsStartAndDriveStatesAreDistinct`; sin modificar workers/backend ni reinterpretar georreferenciación externa. |
| 19. Accesibilidad/tamaño/fuente | `DashboardScreen.kt` | Pruebas Compose con acciones semánticas, botones principales mínimo 56/64 dp y fuente 130% en horizontal; capturas 800×1280 y 1280×800. Prueba física y lector de pantalla pendientes. |
| 20. Navegación sin perder destinos | `DashboardHome.kt`, `MainActivity.kt` | `newRecordChooserPreservesEveryExistingDestination`, callbacks de los cuatro accesos y herramientas; accesos autorizados delegados a componentes originales. |
| Guardado reactivo reordena inmediatamente | `DashboardPresentation.kt`, Room existente | `savedPrReordersImmediatelyFromNewSnapshot`, `storedDraftsRemainDiscoverableAndReactiveWithoutParallelStore` |
| Sin nueva BD/migración/cambio de protocolo | Entidades/esquemas/worker/backend/Gradle intactos | Diff selectivo contra Prompt 5; Room 18 y photo_history conservados. |

No se equipara la cobertura de callbacks/datos con una prueba física de todos los flujos internos. Esos flujos se reutilizan; la revisión visual y de navegación en tablet es parte del procedimiento de entrega.
