# GBO_InventarioVial — integración final A + B + C

Fecha de validación: 08/10/2026. Base de trabajo: `C:\Users\antho\Downloads\GBO_FIX`. Se siguió la solicitud final del usuario; los reportes e instrucciones dentro de las ramas se trataron como antecedentes y evidencia, no como sustitutos de esa solicitud.

## Resultado y procedencia

Se entrega una aplicación integrada sobre el esqueleto B, con autenticación/backend A, CORE A conectado y Pavimentos C. `FINAL_MERGE_PLAN.md` se creó antes de modificar producción y contiene la comparación por archivo y resolución. No se superpusieron ciegamente los tres proyectos.

La auditoría de hashes confirma:

- Los 13 archivos de `auth/` y los 12 de `backend/user-service/` coinciden exactamente con A, incluyendo `AdminUsersScreen.kt`, `LoginScreen.kt` y `UserServiceClient.kt`.
- `gradlew`, `gradlew.bat`, JAR y propiedades del wrapper coinciden con B.
- Todas las referencias canónicas originales de `docs/reference/` de B se conservan sin cambios.
- Los assets de C no difieren de B; se preservó su conjunto común. Se incorporó la implementación C de `pavement/`, sus pruebas y documentación.
- `MainActivity` conserva navegación B, wrapper SIC-22 e integra herramientas CORE, fotos comunes, GPS plegable y borradores locales.
- `InventoryDao`, `DeliverySync`, `RoadReferenceRepository` y `file_paths.xml` contienen la integración de contratos compartidos, no una sustitución indiscriminada de directorios.

Evidencia: `final-evidence/SOURCE_AUDIT.json`. No se ejecutaron commit ni push. No se cambiaron deployments, tokens ni Script Properties externas.

## Room y preservación de datos

**Versión final: 14.** Se mantienen las migraciones 1→12 y se registra la cadena completa sin migración destructiva.

| Migración | Contenido conservado |
|---|---|
| 12→13 | B: `sic22_details.signalCodeSource`, `signalDescription`, `signalObservation` |
| 13→14 | C renumerada: dimensiones de losa y `sicDataJson`, backfill rígido, índice único con calzada y severidades B→L/A→H |

El esquema `app/schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/14.json` fue generado por KSP desde el modelo integrado; identity hash `fb0120953696504136b269514f8f4330`.

`IntegratedMigration12To14Test` crea una base v12 desde su esquema real, inserta evidencia en **cada tabla y columna publicada**, incluidos usuarios, configuración persistida en tablas, SIC/SIC-22, fotos, SCAP, Pavimentos, daños, snapshots e historial. Ejecuta 12→13→14, compara las columnas anteriores, verifica las transformaciones explícitas y cierra/reabre con Room 14. **PASS.** Este resultado no elimina los fallos históricos de pruebas de migración de versiones anteriores a v12; sus identidades siguen en el baseline.

## Auth y dispositivo

La corrección A permanece íntegra. El servicio de usuarios conserva sus contratos de huella/token, administración, PIN y roles. `node backend/user-service/tests/service.test.cjs`: **21 PASS, 0 fallos**.

El build entregado usa `-PgboValidationBuild=true`: omite endpoints, token Drive y configuración de servidor provenientes de archivos locales, sin editar esos archivos ni generar credenciales. El User Service conserva su configuración y flujo originales. La validación remota de usuario/dispositivo requiere el entorno autorizado; no fue ejecutada contra producción. La firma debug no reemplaza la identidad release de campo.

## CORE conectado y experiencia de campo

- `CoreFieldTools`: acceso a Centro Drive, Archivos generados e importación KMZ desde el inicio B.
- `DriveAssetPathRouter` + `DrivePaths`: rutas persistentes por UUID/secuencia, categorías B y superficies C; SIC y SCAP comparten la identidad del puente.
- `PhotoCaptureCard`, miniaturas y `PhotoPreview`: conectados en Inventario/señalización, categorías de alcantarilla, SCAP/puentes y Pavimentos. Conservan los repositorios y confirmaciones de eliminación de cada módulo.
- Cámara predeterminada Android y selección común de lente Camera2. La captura escribe JPEG original a almacenamiento gestionado. Preview respeta EXIF; el funcionamiento del hardware queda pendiente del checklist tablet.
- `LocationSummaryCard`: sustituye los bloques extensos GNSS de Inventario y señalización; los cálculos, fuentes y confirmaciones permanecen.
- `RecordReviewSection`: revisión SIC, SCAP y Pavimentos con imágenes; conserva los pendientes y validaciones de cada módulo.
- `InventoryDraftStore`: guarda texto parcial sin forzar conversión numérica, detalles, fotos/categorías y metadatos GNSS/RTK inicial/final mediante archivo atómico y registro Room DRAFT. Borradores propios se pueden reabrir; otros usuarios y fichas finalizadas no pueden sobrescribirlos. La última eliminación de foto también se persiste. No programa uploads de borradores.
- SCAP conserva su guardado avanzado parcial B. Pavimentos conserva sus muestras abiertas y separación entre guardar y cerrar/calcular/exportar; no se relajó el PCI.

Las nuevas pruebas de borradores preservan texto incompleto, fotos originales y metadatos RTK tras reapertura, y rechazan otro usuario y sobrescritura de ficha ACTIVE. **PASS.**

## Fotografías limpias

El flujo final no añade logo, texto, fecha, PR, ruta, GPS ni watermark a fotografías de entrega. Inventario/SIC, SCAP y Pavimentos seleccionan la imagen original mediante la política común; se retiró `RequiredWatermark.prepare` de los upload/export activos. Las utilidades históricas se conservan para compatibilidad y sus pruebas, pero no gobiernan las entregas nuevas.

Los JPEG válidos se copian sin recomprimir innecesariamente. SCAP y Pavimentos convierten otros formatos únicamente cuando el contrato JPEG lo necesita. Los originales de captura no se borran al enviar. Room, manifiestos y JSON conservan metadatos; EXIF se utiliza como complemento, sin exigir un estampado visual. El JSON nativo de Pavimentos relaciona UUID de foto/daño, fecha y categoría; no inventa un GPS que el módulo no haya capturado.

Las pruebas de entrega SIC y SCAP comparan los bytes originales y sus metadatos. Se actualizaron las expectativas antiguas que exigían watermark. Documentos históricos de marca de agua llevan aviso de sustitución por esta decisión final. **Limitación:** una imagen histórica con texto ya quemado solo puede quedar limpia si aún existe su original limpio; no se reconstruye ni se borra contenido de la imagen automáticamente.

## Drive: implementación y estado real

El repositorio contiene Apps Script. Se integró una revisión versionada en `google-apps-script/Code.gs` y un patch contra B en `DRIVE_V2_SERVER.patch`. Se conservan autenticación y protocolos actuales: Delivery 1, Pavimentos 1, SCAP 1/2. V2 agrega rutas y ACK explícitos, valida componentes/propietarios, conserva los nombres y hashes y comparte la carpeta del puente entre SIC y SCAP en ambos órdenes de envío.

**Servidor local simulado: 37 PASS. Producción: sin publicar ni verificar.** No se afirma que las carpetas V2 ya existan en el Drive real. Ver `DRIVE_V2_SERVER_CHANGE_REQUIRED.md` para payloads, ACK, compatibilidad, patch y publicación pendiente.

El centro común representa SIC/Inventario, SCAP y muestras cerradas de Pavimentos, muestra Pendientes/Subiendo/Errores y permite Sincronizar ahora/Reintentar errores mediante adapters. Las fichas con borrador, revisión faltante/vencida o registro padre anulado quedan locales. Los lotes SIC PREPARED requieren confirmación; los bloqueados requieren corrección y nueva revisión.

SIC conserva el contrato B de preparar/revisar entrega. Las fotos, `metadata.json` y el SIC individual se preparan en la ruta persistente del elemento. La vista de entrega usa esa misma ruta lógica. El envío compara la huella vigente antes de cada archivo y al terminar; una edición/anulación bloquea el lote. Los archivos se conservan para reintentar y nunca se marca éxito sin ACK de lote/hash/nombre/protocolo e IDs. La API legacy foto-a-foto permanece deshabilitada por la política del servidor B; tampoco se acepta un ACK legacy incompleto.

SCAP exige ficha COMPLETE, revisión/layout vigentes y padre no anulado antes de preparar y transmitir; también vuelve a comprobar antes del HTTP y al aceptar el ACK. El padre técnico SCAP puede conservar DRAFT según B: solo se admite junto con su inspección finalizada, no como borrador de Inventario. Conserva publicación humana e histórico. El ZIP limpio versión 6 se conserva tras el ACK. Pavimentos programa sync al cerrar y al exportar artefactos de muestra cerrada cuando la configuración está disponible. Sus copias/manifiesto preparados son durables e inmutables por revisión; los reintentos reutilizan sus bytes. Se serializan envíos concurrentes y se comprueba la vigencia de muestra/SIC antes de cada archivo y del estado final. Solo todos los ACK válidos permiten SYNCED.

Compatibilidad: un servidor antiguo puede seguir confirmando archivos admitidos en su estructura anterior. Pavimentos muestra la ruta legacy si el ACK no confirma V2. Los SIC de Pavimentos nuevos pueden requerir el patch y mantener la muestra pendiente/error en un servidor antiguo. Para SIC, una nueva ruta lógica por sí sola no demuestra la ubicación remota. La publicación y el smoke test son obligatorios para confirmar V2.

La identidad por secuencia es persistente local. El servidor rechaza colisiones de propietario; no se implementó un asignador de secuencias entre tablets. Tampoco se renombra automáticamente Drive anterior ni se modifica contenido revisado. El contrato de lote SIC B sigue rechazando cambios posteriores a una entrega iniciada para la misma ruta: necesitan un acuerdo de nueva versión/revisión; el centro no fuerza esa operación.

## Archivos y backup

`GeneratedFileIndex` descubre SIC/entregas B, ZIP y Excel SCAP durables, PDF/Excel/JSON y SIC-14/14A/15/16 de Pavimentos. Consume `PavementGeneratedFiles.records(context)` y elimina duplicados por ruta canónica. Abrir/Compartir usan FileProvider y concesión de lectura. Estado Sincronizado requiere recibo cuyo hash coincide con el artefacto; un archivo local nunca se presume remoto.

Los Excel SCAP exportados se retienen en `filesDir/scap_exports`. Pavimentos conserva manifiesto y copias finales incluso si falla el destino SAF; las copias preparadas y recibos no se borran al completar. SIC conserva los archivos inmutables del lote.

Local primero, red después: capturas y borradores se guardan localmente; las entregas autorizadas se programan con conectividad y reintento. Una fotografía SIC sin lote revisado todavía no tiene backup Drive automático; un borrador tampoco se sube. La ventana de backup depende de revisión, cierre, configuración, red y publicación del servidor. No se ofrece recuperación de una tablet perdida antes de ACK.

## Puentes, señalización y Pavimentos

Puentes preserva MODULAR/LOSA B, templates y mappings separados, G/G.1, elementos manuales, metrado/unidad/factor, captura parcial, reapertura y SIC-17/17A/17B. Las referencias `#REF!` originales se conservaron sin fórmulas inventadas: **Error existente en plantilla fuente.** Las pruebas históricas que ya discrepaban de esas fuentes siguen identificadas como baseline.

Señalización conserva búsqueda por código/nombre/descripción, código manual, sin código identificable y hito/poste puntual sin exigir posición final. Las nuevas columnas SIC-22 sobreviven a la migración B.

Pavimentos preserva C: cabecera y fecha persistentes, progresivas `00+000`, histórico, rígido por filas/columnas/dimensiones de losa, PCI por losas, L/M/H, geometrías y grietas determinísticas, zoom, escala, Ajustar, tabla, PDF segmentado y Excel sin tabla visual de iteraciones. Los snapshots cerrados son evidencia inmutable. La ausencia de curvas VD/CDV validadas conserva el índice pendiente; no se inventa una curva ni un PCI.

SIC-14/14A/15/16 permanecen funcionales con sus requisitos. SIC-14A 07/13 sigue bloqueado para filas afectadas. Son reconstrucciones de referencias visuales; **no se declara identidad con un XLSX contractual oficial**. Ver `SIC14A_PENDING_DECISION.md` y `PAVEMENT_MANUAL_MAPPING.md`.

## KMZ

Se conserva A: stage, previsualización por ejes/vértices/hash, confirmación de activación, selección/versionado atómico y fallback a referencia incluida. La referencia de campo se recarga al activar. Se mantuvo el endurecimiento XML/DTD usando propiedades compatibles con Android. No hay Google Earth SDK ni conversión automática de chainage geométrico KMZ a PR contractual.

## Pruebas finales

Comando: `gradlew.bat testDebugUnitTest --no-configuration-cache -PgboValidationBuild=true`.

| Métrica | Resultado |
|---|---:|
| total | 323 |
| passed | 308 |
| failed | 15 |
| skipped | 0 |
| newFailures | 0 |
| baselineFailures presentes | 15 |
| baselineFailures ausentes | 0 |

**Gradle devuelve BUILD FAILED por esos 15 fallos. No se oculta ni se declara verde la suite.** La comparación usa identidad exacta `class + name` contra `PARALLEL_B_BASELINE.json`. `final-evidence/TEST_RESULTS.json` enumera los 15 y acredita migración integrada/borradores. Ningún fallo nuevo permanece. Los XML completos están en `final-evidence/test-results/`.

Node User Service: 21/21 PASS. Node Apps Script: 37/37 PASS. Los logs completos están en `final-evidence/`. Cámara/GNSS físico, experiencia tablet y producción remota siguen pendientes de `FINAL_TABLET_CHECKLIST.md`.

## APK y firma

Comando: `gradlew.bat assembleDebug --no-configuration-cache --rerun-tasks -PgboValidationBuild=true`.

**BUILD SUCCESSFUL. 40 tareas ejecutadas.** APK: `output/final-delivery/GBO_InventarioVial_INTEGRADO_DEBUG.apk`, 31,863,665 bytes.

SHA-256:

```text
0c995d51fb082552301f3331de7527fbb5e0ef9280164ab91573e1bc86452a61
```

Inspección de definiciones reales de clases DEX, no búsqueda de texto: CORE 171 clases, SCAP 758 y Pavimentos 387. Se verificaron `InventoryDraftStore`, `DrivePaths`, `FieldCameraActivity`, `PavementSyncWorker`, `GeneratedFileIndex`, `PavementMigration`, `PavementDriveUploader`, Room generado y los dos templates puente incluidos. Ver `APK_CONTENT_CHECK.json`.

`apksigner verify` confirma firma válida y certificado coincidente con la keystore debug **ya existente**, alias creado el 24/09/2026. SHA-256 del certificado: `19f97236f4c4d75bba92ec72dfae2ab9a6d78b159e47e293acf92ed0e7f116b4`. **No se generó una keystore nueva.** Esta APK sirve para VALIDACIÓN; la APK de campo se firmará posteriormente con la misma keystore release GBO existente. No instalarla sobre la versión de campo esperando conservar la firma release.

## Entrega y límites pendientes

En `output/final-delivery/`: fuente ZIP integrado, APK debug, este informe, checklist, documento/patch Drive y manifest de hashes. El ZIP excluye cachés/builds/APK heredados, archivos locales de configuración/credenciales y keystores. Incluye fuentes, schemas, referencias canónicas, reportes y evidencia final.

Pendientes explícitos: publicación/verificación real Drive V2, configuración autorizada para prueba de red, smoke test tablet/cámaras/GNSS, firma release original, fuente contractual SIC-14A, XLSX oficiales SIC Pavimentos, curvas VD/CDV validadas y los 15 fallos históricos. Las limitaciones de multi-tablet, fotos históricas quemadas y revisión de lotes SIC se detallan arriba. No se simularon como resueltas.
