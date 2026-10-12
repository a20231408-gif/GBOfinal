# Auditoría acotada del inicio — Prompt 6

Base: `codex/fotografias-sic-5`, `24df7d3562d823cc1382754b8a3950aed7f88e0e`.
Trabajo aislado: `codex/dashboard-sesion-6`, directorio `prompt6`. No partir de main.

| Función / dato | Origen previo y destino conservado |
|---|---|
| Inicio y navegación | `MainActivity.kt`: `InventoryWorkspace`, `HomeScreen`, estados existentes de ficha/historial/pavimentos/Excel/mapa. Home delega ahora a `dashboard/DashboardHome.kt`. |
| Sesión | `FieldController.session` → `InventoryDao.observeSession()` → `field_sessions`. Configuración reutiliza `SessionDialog`; mantiene operador, UUID y `startTime`. Finalización queda en herramientas. |
| Fecha real de sesión | `FieldSession.startTime`, epoch persistido, convertido a día local del dispositivo. `endTime` marca cierre. No hace falta fecha paralela ni migración. |
| Borradores | `FieldController.drafts` → `InventoryDao.observeDrafts()` (`status=DRAFT`). Se mantienen comprobación de propietario `InventoryDraftStore.accessible` y relaciones explícitas de fichas existentes `InventoryElements.parentId`. |
| Ubicación | `InventoryRecordEntity.routeCode`, `roadbedCode`, `startPrCode`, `startDistanceM`, `endPrCode`, `endDistanceM`. No se deriva desde GPS ni UUID. |
| Fecha original / modificación | `InventoryRecordEntity.createdAt` / `updatedAt`. `surveyDate` es texto de ficha y no se usa para inventar fecha de sesión. |
| Orden de PR | `FieldController.reference.prs`, `ContractualPrInput.catalog`, `RoadPr.chainageM` para ordenar posiciones oficiales del catálogo; dentro del PR se compara distancia. Sin catálogo: tupla numérica PR/distancia, sin multiplicar por 1000. No se cambia `InventoryTechnicalOrder` ni cálculo de longitud. |
| Entregas | Outbox existente `automaticSyncDao.observe()`; lectura de revisión local/confirmada, estado, recibo y fecha de ACK. Acceso principal a `DriveSyncCenter`. No se modifican workers ni backend. |
| Mis registros | `RecordHistoryScreen`, incluyendo su acceso existente a SCAP y edición segura. |
| Nuevo registro | `InventoryElements.home` → callbacks existentes `onAssetSelected` / `onNewSic`. Conserva todas las familias y formularios. |
| Mapa y orden | Callback anterior `onSync` en MainActivity → `AssetOrderScreen`, disponible en herramientas. |
| Pavimentos | `PavementWorkspace`, acceso principal. Sus propios borradores siguen en ese espacio. |
| SCAP / puentes | `ScapWorkspace`, acceso en herramientas y en selector de elementos. Inspecciones y borradores SCAP conservan su flujo propio. |
| Exportaciones | `SicExportScreen`, `KmlControls`, `ObservationReportButton`, archivos generados y KMZ en `CoreFieldTools`. |
| Autenticación / administración | `AuthHomePanel` en herramientas, conservando permisos ADMIN/TABLET_ADMIN, logout, administración de tablets y diagnóstico de estabilidad. |

No se investigaron otras áreas del proyecto ni se cambiaron sus flujos internos.

## Criterio histórico y sesiones múltiples

Dashboard: solo borradores cuyo día de sesión real sea el día local actual. Si no hay sesión asociada recuperable, usa `createdAt`, nunca `updatedAt` ni la sesión activa nueva. El listado completo no restringe fechas por defecto y permite fecha/ruta/calzada/tipo. Los registros antiguos no se reescriben ni se eliminan.

La configuración de una sesión conserva fecha/operador/identidad; puede cambiar proyecto, ruta, calzada y sentido para nuevas capturas. No modifica la ubicación guardada de borradores previos. Su contexto incluye ruta/calzada propias y proyecto/operador/hora de sesión. Cambiar el sentido de la sesión reordena la presentación de sus borradores; no renumera carpetas ni altera orden contractual remoto. Para una nueva jornada se finaliza la sesión y se inicia otra, conservando ambas fechas.

El orden separa ruta, calzada, sesión y sentido. PR sin ubicación válida o no reconocido en un catálogo presente va al final; empates por creación y UUID interno. No fusiona registros con igual PR. La lista observa los datos persistidos y se recalcula tras guardar; con la app abierta se revisa el día local cada 30 segundos. UUID se utiliza únicamente en claves/callbacks de navegación, no en textos visibles del dashboard.

## Trabajo anterior

El worktree de Prompt 5 y sus logs, resultados iniciales, plantillas SCAP, herramientas y proguard sin commit permanecen intactos. En Prompt 6 se copian únicamente recursos de compilación y propiedades locales privadas; quedan fuera del checkpoint. Prompt 4 y la copia operativa también se conservan. Ningún cambio anterior se atribuye al Prompt 6.
