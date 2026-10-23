# Cierre GBO — carpetas locales y Entregas Drive — 20/10/2026

**BUILD SUCCESSFUL. Release candidata firmada disponible para validación física. No aprobada todavía para distribución a ingenieros.**

## Alcance conservado

Se integran la nomenclatura pública local y una presentación simplificada de Entregas Drive sobre el código actual, conservando el hotfix SIC-20 y los cambios aprobados de Pavimentos/PDF. El propietario confirmó que 1.4.11 (25) sigue siendo candidata, sin ninguna versión 26 o superior distribuida. Se conserva 1.4.11 (25); la APK anterior no se sobrescribe.

SCAP muestra el nombre B `singularityName` y código real; SIC usa el subtipo, ruta y PR reales; Pavimentos usa superficie, tramo y muestra. El formateador compartido solo consulta datos: esta pantalla nunca llama a reserva de carpetas, preparación de exportación ni generación de archivos. Las identidades originales permanecen en el detalle técnico; dos nombres iguales no se usan como claves.

## Entregas Drive

Tarjetas con título destacado, ubicación, estado con símbolo/texto y mensaje breve. Filtros Todas/Pendientes/Atención/Completadas; atención primero y confirmadas al final. Destino confirmado, Archivos y Detalles técnicos se despliegan por separado. El diagnóstico agregado de cola se conserva en su propia sección secundaria. Los paneles técnicos de otras pantallas no fueron rediseñados.

- Pendiente de subir: turno/conexión, cambio local nuevo o reintento BUSY. Un reintento normal no aparece como error definitivo.
- Subiendo: WorkManager acredita actividad para esa identidad y revisión; no basta UPLOADING guardado.
- Verificando entrega: commit en confirmación, ACK incompleto/incierto, estado UPLOADING sin trabajador activo o recibo SYNCED que no puede revalidarse.
- Subido correctamente: ficha finalizada, revisión fuente actual igual a la entrega, estado/recibo confirmados y `AutomaticDelivery.validAck` aprobado contra la preparación persistida. Incluye identidad, revisión, contenido, rutas, hashes, archivos y recibos Excel que exige el contrato actual. No se cambia ese contrato.
- Necesita atención: conflicto, bloqueo no BUSY, falta de cierre/validación o configuración ausente.

X/Y solo se muestra al validar el ACK completo y corresponde al manifiesto de archivos de esa entrega padre. Los archivos en staging o preparados no se cuentan como entrega confirmada. Si todavía no puede acreditarse, se muestra «Confirmación de archivos pendiente». Los videos SCAP mantienen su protocolo y ACK separados; no se atribuyen al ACK padre. Una confirmación anterior no acredita una ficha editada. Si falta la preparación histórica necesaria para revalidar el recibo, se conserva una presentación prudente «Verificando entrega».

Las acciones Sincronizar ahora, Reintentar errores y Reintentar ahora llaman a las implementaciones existentes. Abrir las tarjetas y sus detalles no ejecuta envíos.

## Carpetas locales

Ejemplos: `Puente 001 - Correviento`, `Badén - PE-28C - PR 12+150`, `Pavimento rígido - Tramo 3 - Muestra 2`. Las colisiones entre UUID diferentes usan una reserva local estable como `(2)`; repetir una copia reutiliza su destino. Al guardar un nombre nuevo, la siguiente copia usa ese nombre y conserva las anteriores. ñ/tildes se conservan; componentes limitados a 96 bytes UTF-8.

Los originales, identificadores, rutas privadas, archivos históricos y verificaciones de integridad no cambian. Los planes interrumpidos mantienen su destino congelado. La regeneración explícita de una copia histórica conserva el plan anterior. El contenido de un manifiesto nuevo refleja la nueva ruta y recibe su SHA normal; no se altera el algoritmo de verificación.

## Pruebas y revisión visual

**72 casos locales distintos aprobados; 0 fallos finales y 0 omitidos.** Son 11 casos nuevos de presentación/UI y 61 regresiones relacionadas. Las 3 pruebas UI se repitieron tras ajustar exclusivamente su tema/ancho de preview; no se cuentan dos veces. Ambos lotes Gradle terminaron BUILD SUCCESSFUL. No se repitió la suite general.

| Clase | Casos |
|---|---:|
| DriveDeliveryCardUiTest | 3 |
| DriveDeliveryPresentationTest | 8 |
| HotfixBusy146Test | 4 |
| LocalCopyFinalTest | 4 |
| LocalFiles148MediaStoreTest | 1 |
| PavementDurabilityFanTest | 6 |
| PavementFinalGeometryTest | 4 |
| PavementJointFlow20261019Test | 1 |
| PavementPdfDeliveryIntegrationTest | 1 |
| PavementPdfLegibilityTest | 2 |
| PavementRigid147Test | 9 |
| PublicLocalNamesTest | 11 |
| Sic20DraftDetail111Test | 4 |
| Sic20DraftResolution111Test | 3 |
| Sic20Workspace111Test | 6 |
| SyncRecovery10DTest | 5 |

SIC-20: creación y guardado con galería simulada, foto/observaciones conservadas, volver a Inicio, Mis registros → detalle → editar para Muro/Túnel/Badén, alternancia, reapertura y bloqueo de identidad contradictoria. Carpeta pública: nombre B, históricos SIC-17A, repetición, colisiones, cambio de nombre, cancelación/reanudación, Unicode y conservación de copias anteriores. Pavimentos: esquinas, grieta D, parches, juntas, PCI/Excel, preparado PDF/fotos y no mutación de originales. Sync: reintentos BUSY, ACK perdido/incompleto, edición durante envío y cancelación de trabajador. Las pruebas de transporte utilizan respuestas ficticias, no Google real.

Previews revisados visualmente: [teléfono con fuente grande](entregas-drive-final-20261020/previews/telefono-fuente-grande.png), [tablet con BUSY](entregas-drive-final-20261020/previews/tablet-reintento-busy.png) y [detalle de archivos](entregas-drive-final-20261020/previews/tablet-detalle-archivos.png). Usan el tema real GBO y tarjetas Compose reales a ancho reducido, con datos ficticios. No se detectaron recortes ni solapamientos. Son previews de las tarjetas, no una captura del diálogo completo en un equipo físico.


Los previews se generan desde los componentes Compose reales, con datos ficticios y adaptadores locales de Android/Robolectric. Se identificarán como renderizados locales, no capturas de una tablet. Las pruebas no acreditan una subida Google ni el PDF nativo en un dispositivo.

## Archivos de esta entrega

Cambios de esta etapa, bajo `app/src/main/java/com/tuempresa/inventariovial/core/`:

- `DriveSyncCenter.kt`: tarjetas, filtros, orden y diagnóstico expandible; acciones existentes conservadas.
- `AutomaticSyncPanel.kt`: únicamente permite reutilizar internamente la observación de WorkManager; sin cambio en el comportamiento de sus paneles.
- `DriveDeliveryPresentation.kt` (nuevo): identidad de solo lectura, validación del ACK y estados visibles.
- `DriveDeliveryCard.kt` (nuevo): tarjeta y detalles.
- Pruebas nuevas: `DriveDeliveryPresentationTest.kt` y `DriveDeliveryCardUiTest.kt`.

Integrados de la etapa anterior: `localfiles/PublicLocalFolderNames.kt`, `LocalDocumentLocation.kt`, `LocalExportBuilder.kt`, `LocalCopies.kt`, `LocalCopyPresentation.kt`, y sus pruebas `PublicLocalNamesTest`, `AutomaticDocuments149Test`, `LocalFiles148MediaStoreTest`. En este cierre esos archivos no se modifican.

El hotfix SIC-20 previo se conserva en MainActivity, RecordDetailScreen, InventoryDraftStore, DashboardPresentation, StabilityDiagnostics, DraftDestination, ResumeDraftForm y los diagnósticos Sic20DraftDiagnostics/ContractualReferenceDiagnostics, con sus pruebas. Los exportadores PDF/XLSX, geometría de Pavimentos, protocolo y colas no tienen cambios en esta etapa. `app/build.gradle.kts` conserva 1.4.11/25, firma, dependencias y configuración existentes.

## APK y conservación

- APK: `D:\Downloads\GBO_PRUEBAS_8B\output\GBO_RUTA_CLARA_CANDIDATA_1.4.11_25_ENTREGAS_DRIVE_20261020\GBO_Ruta_Clara_1.4.11_25_RELEASE_CANDIDATA_ENTREGAS_DRIVE.apk`.
- versionName **1.4.11**, versionCode **25**, applicationId `com.tuempresa.inventariovial`.
- **SHA-256 APK:** `cd254fa0e42e3ca10e47d5a55bfe4c59abaf9ecb30d357c4c9f92bb2937ed2e3`.
- Certificado histórico SHA-256: `0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315`; firma válida v2/v3 y coincidente con la candidata anterior verificada. La identidad de Release histórica 1.4.9/1.4.10 ya había sido contrastada en la entrega anterior. No se inspeccionó la instalación actual de cada tablet.
- APK completo, sin split, sin testOnly y sin debuggable; alineación ZIP y 16 KiB comprobadas. Tamaño: **26405137 bytes**. minSdk **26**, targetSdk **37**, sin cambios.
- Gradle ordinario `:app:assembleRelease`, con `validateReleaseConfiguration`, `lintVitalRelease` y `verifyReleaseIdentity`: correcto. Tiempo local: **327.5 s**. Sin `gboValidationBuild` ni sustitución de configuración real.
- Identidad Room20 y activo road_reference.json iguales a los de la candidata anterior; migraciones y código de almacenamiento conservados. PdfDocument nativo presente; adaptador SyntheticPdf146 de pruebas ausente. Esto es una comprobación estática del artefacto, no una actualización física ya ejecutada.
- Los 15 campos de configuración efectiva Release ajenos a la versión conservaron sus hashes; las 5 propiedades privadas y las 12 APK anteriores permanecen idénticas. No se publican sus valores. El binario nuevo contiene la nomenclatura pública y las tarjetas nuevas, comprobado también en DEX.
- Respaldo reversible de fuentes modificadas y del APK/metadata generado antes de compilar en `tmp/entregas-drive-final-20261020/`. La candidata anterior permanece en su carpeta `output/GBO_RUTA_CLARA_CANDIDATA_1.4.11_25/` y no fue reemplazada. Ambas candidatas tienen versión 25: distinguirlas por **nombre de archivo y SHA-256**.

Advertencias no bloqueantes conservadas: opción existente `android.disallowKotlinSourceSets=false` experimental y adaptación del target JVM del JDK25 a JVM24 durante la configuración Kotlin. Sin errores de compilación ni lint vital. El verificador documental encontró separadores Windows duplicados en la ruta del informe anterior; se resolvió la referencia contra el APK conservado por SHA-256, sin modificar ese informe ni recompilar.

Evidencias: [resultado consolidado](entregas-drive-final-20261020/RESULTADOS.json), [pruebas por caso](entregas-drive-final-20261020/PRUEBAS.json), [identidad APK](entregas-drive-final-20261020/IDENTIDAD_APK.json), [huellas de fuentes de esta etapa](entregas-drive-final-20261020/FUENTES_SHA256.json) y logs Gradle de interfaz/regresiones/Release en la misma carpeta.


No hubo instalación, distribución, limpieza, publicación Google, commit ni push. No se modificaron Apps Script, GBO LAB, Servicio de Usuarios ni la lógica de longitudes entre PR distintas. Los cambios previos se conservan con respaldo reversible.

Limitación previa conservada por decisión del propietario: no se modificó el cálculo entre PR distintas ni se incorporaron equivalencias contractuales nuevas. Una referencia capturada localmente no acredita calibración contractual; comprobar ese dato conforme a la referencia aprobada antes de usarlo como medida contractual.

## Checklist físico de aceptación

1. Respaldar las fichas por los procedimientos existentes. Copiar la candidata a una tablet de prueba con Release compatible y elegir **Actualizar**. Ante rechazo de firma/versión, detenerse: no desinstalar ni borrar datos. No se ha comprobado automáticamente la instalación de cada tablet.
2. Abrir GBO, iniciar sesión normalmente y navegar. Confirmar que siguen los registros y configuración anteriores.
3. En modo avión y con datos ficticios Solo dispositivo, guardar foto/observación en Muro, Túnel y Badén; salir, abrir Mis registros/detalle/editar y confirmar subtipo y contenido.
4. Guardar una muestra rígida con los daños aprobados. Abrir PDF horizontal nativo y Excel; verificar marcas en borde, grieta D, parches y junta, fotografías y datos al reabrir.
5. Guardar una copia local ficticia. Abrir Descargas y verificar nombre legible, PDF/XLSX/medios. Repetir para comprobar reutilización; editar nombre guardado y comprobar que conserva la carpeta anterior.
6. Abrir Entregas Drive. Confirmar nombre/estado, filtros y detalles. Un pendiente o 3/6 transferidos no debe mostrarse confirmado; una edición no puede reutilizar la confirmación anterior.
7. Solo con autorización separada: una entrega ficticia controlada a Drive, verificando la revisión/ACK y archivos en el servidor. Videos SCAP requieren su ACK propio. Conciliar pendientes antes de distribuir.

**GO para iniciar la validación física del propietario. NO-GO para distribuir a los ingenieros hasta aprobar ese checklist.** No se detectó un defecto bloqueante de integridad en las verificaciones focalizadas. La prueba de actualización, PDF nativo, administrador de archivos y conciliación real con Drive siguen pendientes. Ninguna prueba simulada se presenta como evidencia Google ni como garantía de rendimiento productivo.
