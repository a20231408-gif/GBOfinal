# GBO — Estado completo para continuar

Fecha de cierre: **30/10/2026, America/Lima**. **DESARROLLO PAUSADO por orden del usuario.** Este documento sustituye los estados anteriores de continuidad; los informes históricos conservan su fecha y no deben interpretarse como estado actual.

## A. Estado general y Git

- Repositorio: `C:\GBO`; remoto autorizado: `https://github.com/a20231408-gif/GBOfinal.git`.
- Rama única de publicación: `codex/observaciones-campo-asus-20261030`. No modificar main ni ramas históricas; no merge, force push, `push --all` ni `push --mirror`.
- Versión Android: **1.4.20 (34)**, aplicación original `com.tuempresa.inventariovial`.
- Último commit de código de la candidata APK: **`2bad57c9d7666dfe7673129bee41363bcb8eb48f`**. Sus fuentes no se modificaron durante la preparación del piloto ni esta pausa.
- Último commit del preflight antes de actualizar este documento: **`3a483cbd0a776bdf430fbf16360ca1602a2ac75b`** (WIP). El commit que contiene esta continuidad se obtiene con `git log -1 --format=%H -- docs/ESTADO_PARA_CONTINUAR.md`; el SHA final y su comprobación remota se registran también en la evidencia privada de cierre.
- Estado de publicación al redactar: **PENDIENTE de push y comprobación remota**. El resumen final debe indicar el resultado real.
- GitHub es **PÚBLICO**. El usuario autorizó explícitamente publicar fuente y documentación incluso manteniendo esa visibilidad el 30/10/2026. Esta autorización sustituye el bloqueo anterior por visibilidad. **No autoriza secretos, configuración sensible, APK, bases ni registros reales.** No volver a pedir permiso solo por ser público.
- Referencias históricas conservadas: migración ASUS `dbe7247a6f61734cbe6119bb6d4f018ca0479363`; main remoto observado `c42bb8158da6f24ff9cedbee7e4858fe104d1e31`.
- La revisión detectó un identificador privado de destino en el manual maestro nuevo. Se omitió de la historia que se publicará, conservando el manual completo, los commits originales y su bundle **privadamente**. Solo cambió ese dato documental; ningún código funcional se reescribió. Equivalencias: `2204ac3 → e3c9833`, `bf38671 → 21fb543`, `ca659b6 → 3a483cb`. Los SHA originales permanecen en el respaldo local, no en la rama pública.

Arquitectura: Android/Kotlin con interfaz Compose y ViewModel; persistencia local **Room 21**, repositorios de SIC, SCAP y Pavimentos; trabajo diferido con WorkManager; documentos y medios privados locales, publicación documental por MediaStore/SAF y copia espejo condicionada al ACK. La cola offline-first conserva UUID, revisión, paquete congelado, hashes y staging. Los servidores Apps Script de **Usuarios** y **Drive** son componentes separados. Los valores privados se incorporan desde archivos locales al compilar y no están en Git.

Puntos de entrada: `app/src/main/java/com/tuempresa/inventariovial/MainActivity.kt`, `viewmodel/InventoryViewModel.kt`, `data/database/InventoryDatabase.kt`, paquetes `core/`, `auth/`, `localfiles/`, `scap/`, `pavement/`, `export/`, `field/` y `repository/`. Los nombres parciales en las tablas siguientes son relativos a ese paquete Java salvo que se indique otra raíz.

| Componente | Estado al pausar |
|---|---|
| Android original | Release 1.4.20 (34) compilada, firma histórica verificada, **NO DISTRIBUIR**; no instalada. |
| Servicio de Usuarios | **Sin modificaciones** ni despliegues. Configuración legítima preservada en `server.local.properties`; cliente `auth/UserServiceClient.kt`, sesiones `auth/AuthGraph.kt` y `AuthSessionManager.kt`. No confundir con el servidor Drive. |
| Apps Script original GBO_Inventario / Drive | Último despliegue operativo observado: **V21**, misma URL, raíz y autenticación. **No se creó una nueva versión ni se autorizó/realizó un corte.** |
| HEAD del editor del proyecto original | Se añadió **solo `PreflightReadonly.gs`** para auditoría manual. Su ejecución se intentó, pero quedó en **«Se requiere autorización»**. No confundir cambio en HEAD con actualización del despliegue versionado V21. |
| Raíz operativa actual | Es la raíz anterior. El propietario concedió Editor a la cuenta GBO durante la preparación del piloto; el conector verificó `role=writer`. Se conservó el acceso general lector. Falta verificar `canEdit/canAddChildren` en la ejecución real del helper. No repetir la solicitud de Editor salvo que se revoque. |
| Destino futuro «1.- ARCHIVOS CAMPO» | No configurado como raíz, no migrado y sin cambios de permisos. Su permiso preexistente «cualquiera: escritor» impide considerarlo un destino listo. IDs exactos en respaldo privado. |
| Copia aislada anterior de Apps Script | Existente y preservada; no se creó otra ni se tocó durante la preparación del piloto. Sus pruebas no acreditan el servidor productivo. No usar su raíz/token/URL en campo. |
| Tablet piloto | El usuario identificó **la tablet de Julián, disponible por USB**, para el piloto posterior al corte y con respaldo. **No se comprobó aún conexión ADB, respaldo efectivo ni versión instalada; no se accedió ni instaló.** Flavio sigue sin validación física. |

## B. Observaciones de campo: implementación, archivos y evidencia

**IMPLEMENTADO** significa que existe código; no significa validado en una tablet. **PARCIAL** distingue lo local/preparado de lo operativo pendiente. Ninguna observación está acreditada físicamente en las tablets por esta intervención.

| Observación | Estado | Código / comportamiento existente | Pruebas y límites |
|---|---|---|---|
| Filtros día, mes y año | IMPLEMENTADO | `field/RecordDateFilter.kt`, `localfiles/UnifiedRecordsScreen.kt`. | `RecordDateFilterTest`; no validación física. |
| Borradores, pendientes, entregados y orden por fecha | IMPLEMENTADO | `core/DriveDeliveryPresentation.kt`, `SyncIntentPresentation` y `LocalInventoryIndex` en `UnifiedRecordsScreen.kt`; estados de cola separados del ACK. | `LocalDraftDiscard10CTest`, `RecordDateFilterTest`, selección Android final. |
| Selección individual/masiva y eliminación | PARCIAL | Selección de borradores y entregados elegibles en `UnifiedRecordsScreen.kt`; `core/LocalDraftDiscard.kt` y `LocalDraftDiscardButton.kt`. El descarte es reversible; no se borra la base Room ni registros remotos. | `LocalDraftDiscard10CTest`. No presentar archivo/ocultamiento como eliminación total de datos privados. |
| Limpieza de copias locales confirmadas | IMPLEMENTADO | `localfiles/ConfirmedLocalCleanup.kt`, `SharedDocumentStore.kt`: exige ACK íntegro de revisión vigente y plan completo, ZIP externo releído por hash, journal durable y retiro de copias públicas propias. Pendientes y fotografías originales protegidos. | `FieldCleanupAndConsolidationTest`; incluye pérdida de respuesta de borrado y reanudación. SAF/MediaStore reales pendientes. |
| Restauración | IMPLEMENTADO | `ConfirmedLocalCleanup.restore`: verifica originales retenidos antes de restaurar el listado; permite regenerar copias. Una limpieza incompleta queda visible. | ZIP corrupto, original alterado y restauración probados localmente. **No es una importación de Room desde ZIP ni una restauración universal desde otra laptop.** |
| Preparar nuevo tramo | PARCIAL | El botón prepara filtros/listado conservando historial y pendientes. `field/FieldSessionDefaults.kt` contiene configuración de sesión existente. | La ruta/tramo de la sesión debe configurarse aparte; el botón no crea por sí solo una sesión nueva. |
| Longitudes kilométricas persistidas/exportadas | IMPLEMENTADO | `field/TechnicalLengthField.kt`, `repository/TechnicalLengthStore.kt`, `export/TechnicalLengthExport.kt` y guardado transaccional. | `LinearLengthDeliveryTest`: **42+995 a 43+005 = 10 m**, guardar, reabrir, editar y exportar Excel/JSON. No confundir con el helper WIP antiguo sin persistencia. |
| Nombres descriptivos de fotografías | PARCIAL | `core/PublicDeliveryNames.kt`, `ReadablePhotoNames.kt`, `AutomaticDelivery.kt`, `AutomaticSync.kt`; `localfiles/LocalExportBuilder.kt`. Contador durable, Unicode, caracteres reservados, longitud y colisiones. | `PublicDeliveryNamesTest` y contrato Node/Kotlin. Local integrado; remoto requiere `publicNamingVersion=1` explícito. **V21 no lo anuncia**: conserva nombres V2 anteriores. No renombrar reservas históricas. |
| Carpetas locales/remotas y nombre real del puente | PARCIAL | `localfiles/PublicLocalFolderNames.kt`, `LocalDocumentLocation.kt`, `DriveMirrorPlan.kt`; candidato `backend/candidato-original-reanudable/`. Nuevos bindings pueden usar etiquetas descriptivas; los previos son inmutables. | Pruebas locales de estructura/reserva y `ScapHumanDeliveryTest`. Provisionales, revisiones históricas y subrutas SCAP conservan su estructura; no se acredita equivalencia de todos los archivos. |
| Puentes SCAP: guardado, fotos, croquis, anexos, offline | PARCIAL | Paquete `scap/`, `scap/export/ScapHumanDelivery.kt`, `core/AutomaticDelivery.kt`, `diagnostics/ScapSaveDiagnostics.kt`. Correcciones P0 y snapshots conservados. | `PhotoTransport10Test`, `SyncRecovery10DTest`, `ScapHumanDeliveryTest`, `AutomaticDocuments149Test`. Falta evidencia real de Flavio y entrega operativa grande. |
| SIC incompletos y consolidación EXCEL_SIC | PARCIAL | Exportadores SIC/SCAP, `export/LocalSicConsolidates.kt`, `localfiles/LocalCopies.kt`, motor backend `SyncV3Core.gs`. Se conservan aportes y documentos provisionales. | Documentos SIC y aportes verificados en selecciones locales/ensayo aislado. No atribuir a producción los tres SIC del ensayo de 59 archivos. |
| Pavimentos, PCI y daño 26 | IMPLEMENTADO | `pavement/PavementRepository.kt`, modelos, `PavementJointOccurrences`, `PavementJointAnnotations`, calculador existente. No se modificó el motor PCI. | `PavementFinalIntegrityTest`, `PavementJointFlow20261019Test`, `PavementP0AdditionalTest`; ocurrencias L/M/H independientes y cierre/reapertura. Validación física pendiente. |
| Consolidación al finalizar la unidad de muestra | IMPLEMENTADO | `localfiles/AutomaticLocalDocuments.kt`, `LocalCopies.kt`: trabajos automáticos solo con CLOSED; autoguardado de fallas en Room; trabajos antiguos de borrador se marcan SUPERSEDED. Copia manual provisional sigue disponible. | `AutomaticDocuments149Test`, `FieldCleanupAndConsolidationTest`. No se publica automáticamente un paquete por falla. Historial conservado; no borrado general de archivos antiguos. |
| Sincronización grande y recuperación | PARCIAL | Android `AutomaticSync.kt`/`AutomaticDelivery.kt`; backend reanudable en `backend/candidato-original-reanudable/`, derivado de `backend/aislado-commit-resumible/`. Preserva identidad, staging, hashes y ACK final. | Android 77/77, backend 14/14, paquete original 3/3 con mocks. **No desplegado en original**; journals históricos PENDING requieren revisión y no se adoptan automáticamente. |
| Progreso, BUSY y mensajes de bloqueo | IMPLEMENTADO | `core/DeliveryProgress`, presentaciones y drenaje de cola, cambios P0 `6f84c82`/`3fd2dbf`; mensajes de limpieza/restauración en `UnifiedRecordsScreen.kt`. | `SyncRecovery10DTest`, `PhotoTransport10Test`; BUSY y pérdida de ACK simulados. No equivale a corrección demostrada en Flavio. |
| Abrir registros, fotos, PDF y Excel | IMPLEMENTADO | `localfiles/LocalFilesUi.kt`, `UnifiedRecordsScreen.kt`, `SharedFileActions` en `SharedDocumentStore.kt`; acciones visibles y diagnósticos desplegables. | Documentos y componentes probados localmente; aplicaciones visor, permisos y usabilidad en tablet pendientes. |
| Migración Google/destino definitivo | PENDIENTE | Conservadas las fuentes y preparación del original, sin cambio de raíz, cuentas ni Servicio de Usuarios. | Requiere permisos apropiados, inventario completo, bindings/ACK protegidos y autorización específica. Nunca copiar ACK entre raíces. |

Bloques de código ya conservados: Work `42e8a04`; longitud `4c44a9f`; carpetas/UI `19ab997`; continuación Android `7e9739e`; backend aislado `a9e1393`; limpieza/Pavimentos `fc23a0c`; candidato original `c89ba73`; nombres `bbe742a`; identidad APK `2bad57c`. Los WIP antiguos `ce4b727`/`243ea4c` no describen por sí solos el estado posterior.

## C. Pruebas realizadas y evidencia

Últimas selecciones acreditadas, sin repeticiones durante esta pausa:

| Selección | Resultado | Evidencia local |
|---|---|---|
| Android final, 15 clases | **77/77 PASS**, 0 fallos/errores/omitidas, 109,97 s | `tmp/cierre-final-20261030/android-2/xml/`, `resultados.json`, `tests.json` |
| Backend reanudable candidato original | **14/14 PASS**, 36,83 s | `tmp/cierre-final-20261030/backend-final-2.log` |
| Paquete completo del original, con mocks | **3/3 PASS** | `tmp/cierre-final-20261030/paquete-original.log` |
| Nuevo auditor paginado, solo mocks | **3/3 PASS**, antes de esta pausa | `tmp/piloto-1.4.20-20261030/preflight-local.log`, `backend/candidato-original-reanudable/tests/PreflightReadonly.test.cjs` |

**No es la suite histórica completa.** Los tres casos nuevos del auditor no son los tres del paquete original ni un inventario real. Simulan 135 journals, JSON ilegible y cambios concurrentes.

Fallos anteriores conservados: Android primer intento 75/76, fixture que esperaba publicación automática de un borrador vacío; se ajustó la solicitud manual conservando aserciones PDF/Excel/JSON. Otros intentos del mock tuvieron Python ausente en PATH y expectativas de nombres corregidas. No sumarlos como casos nuevos ni ocultarlos. Hubo una selección histórica backend 17/25 con ocho fallos de recursos ignorados ausentes; no declararla aprobada.

Evidencia real anterior **solo del entorno aislado**: 59 archivos recuperados, 62/62 hashes/propietarios/rutas, journal COMPLETE 67/67, ACK HTTP y replay sin duplicados, BUSY. La segunda raíz ficticia quedó solo con staging **59/59**, sin commit ni ACK. No reiniciar ese ensayo ni presentarlo como entrega productiva.

No hubo instalación, lectura de datos de una tablet ni validación física. No repetir las pruebas Android aprobadas si no se cambia código o aparece un motivo concreto.

## D. APK oficial candidata, sin publicar el binario

- Archivo: `C:\GBO\tmp\cierre-final-20261030\release-1.4.20-34\GBO_1.4.20_34_CANDIDATA_NO_DISTRIBUIR.apk`.
- Versión **1.4.20 (34)**; package `com.tuempresa.inventariovial`; Release, no debug, no `.ensayo`, no split.
- SHA-256 APK: **`5c7a2346ec0f8d11eabee85178b147cba323e5479c859375b64731a7bfa20298`**.
- Certificado histórico SHA-256: **`0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315`**. No sustituir keystore ni firmar con otra identidad.
- `validateReleaseConfiguration` y `assembleRelease` PASS; build 100,86 s. Firma v2/v3, manifest, package, código 34 y alineación de 16 KiB auditados con herramientas oficiales.
- Room **21**, sin cambio de esquema en esta candidata; `LocalFiles148MigrationTest` y `PavementMigration21Test` aprobados. Esto no prueba por sí solo una actualización física con datos de campo.
- Configuración: **endpoint y token legítimos del backend productivo anterior**. Se comprobaron en DEX; los de ensayo están ausentes. No publicar valores, APK, generated BuildConfig ni configuración privada.
- Datos verificables sin secretos: `docs/release-1.4.20-34/VALIDACION_LOCAL.json` y `FUENTES_APK_SHA256.json` (614 fuentes).
- Sigue **NO DISTRIBUIR**: falta inventario operativo completo, consentimiento del preflight, nueva versión/corte autorizado, ACK del backend actualizado y piloto físico con respaldo. No recompilar si la configuración actual sigue compatible.
- APK 1.4.19 (33) y la antigua `.ensayo` permanecen en respaldos privados. No son alternativas para distribuir ni se crearon nuevas APK de ensayo.

## E. Apps Script original, permisos y operación interrumpida

**Original GBO_Inventario:** el último despliegue observado es **V21**. La UI mostraba ejecución como cuenta GBO, con URL y raíz originales. El GET observado antes de esta pausa fue HTTP 200, protocolo 3/pathVersion 4/nombres V2/panel 2; sin `commitContinuationVersion` ni `publicNamingVersion`. No se hicieron nuevas pruebas HTTP durante la pausa.

**Permisos:** el usuario confirmó la concesión de Editor a la cuenta GBO; el conector la leyó después como `writer`. No se modificaron los permisos generales ni la carpeta nueva. El consentimiento OAuth del proyecto y el permiso de la carpeta son asuntos distintos: conceder Editor no completa automáticamente la autorización de ejecución.

**Inventario operativo:** sigue incompleto. La lectura anterior estuvo limitada a 100 entradas del registro y una muestra descargada COMPLETE de ocho operaciones. Totales y estados PENDING reales son **desconocidos**. El buscador conectado no devolvió los JSON con los filtros ensayados; no interpretar ese resultado vacío como ausencia de journals.

**Nuevo WIP preservado:** `backend/candidato-original-reanudable/PreflightReadonly.gs`, función `gboPilotInventoryReadonly`, y `tests/PreflightReadonly.test.cjs`. Recorre Drive por `nextPageToken`, repite la enumeración para detectar cambios, cuenta estados y emite lotes de filas. No contiene operaciones de escritura Drive, adopción de históricos ni cambios de propiedades. Está añadido al **HEAD del editor del original**, pero no al despliegue V21. Al intentar ejecutarlo apareció **«Se requiere autorización»**; **no produjo inventario real ni resultado efectivo de canEdit/canAddChildren**. Quedó detenido al llegar la orden de pausa. Revisar consentimiento y el uso de `Session.getEffectiveUser()`/ámbitos antes de reanudar; no se cambió el manifest ni se aceptaron permisos adicionales.

**Snapshot nuevo del original:** diez archivos leídos desde su editor y guardados privadamente antes de añadir el auditor. Los diez coinciden byte a byte con la referencia original anterior, incluido manifest y Código.gs. Evidencia en `tmp/entrega-20261030/backend-privado/piloto-1.4.20/snapshot-head/` y `comparacion-original.json`. No publicar ese Código.gs configurado ni propiedades.

**Candidato preparado:** fuente versionable en `backend/candidato-original-reanudable/`; paquete completo configurado, privado, en `tmp/entrega-20261030/backend-privado/CANDIDATO_ORIGINAL_20261030/`. Reemplazos previstos `SyncV3.gs`, `SyncV3Core.gs`, `PhotoNamesV2Core.gs`; nuevo `PublicDeliveryNames.gs`; cambio acotado en capacidades de Código.gs. El resto de módulos/manifest se conserva. **Esos reemplazos no se copiaron todavía al editor del original; solo se añadió el auditor.** No se creó nueva versión de Apps Script.

La propiedad `GBO_RESUMABLE_ROOT_ID` debe coincidir con la raíz legítima actual antes de activar el candidato. No se estableció en esta preparación. Los nombres descriptivos remotos deben permanecer deshabilitados hasta verificar clientes: no activar `GBO_PUBLIC_NAMES_ENABLED` por defecto. Los valores y los IDs reales de proyecto/despliegue/raíz están en el respaldo privado, no en esta documentación pública.

Protecciones existentes: COMMIT_IN_PROGRESS es retryable **sin ACK**; staging y paquete congelado se conservan, IDs se preasignan con journal durable, replay reconcilia, BUSY protege consolidados. El presupuesto es cooperativo de 85 s; una llamada individual a Drive no es cancelable. No prometer una garantía absoluta de seis minutos para cualquier llamada externa. Journals históricos PENDING sin continuationVersion quedan bloqueados por revisión; no adoptarlos con la excepción del fixture aislado. El bloqueo global del candidato puede afectar otras entregas: conciliar ese comportamiento con la instrucción de detener solo la operación afectada antes de un corte si hay PENDING reales.

**Rollback preparado, no ejecutado:** conservar V21 y su referencia privada; revertir el **mismo despliegue**, manteniendo URL, raíz y autenticación, solo con autorización. Nunca borrar journals/staging para forzar un rollback. Si quedan PENDING reanudables o bindings descriptivos nuevos, V21 no garantiza recuperarlos: detener los envíos afectados y reconciliar. Android no debe desinstalarse, limpiarse ni bajar versionCode; cualquier reversión exige otra candidata compatible con firma histórica.

## F. Privados, respaldos y transferencia

**Nunca subir** `local.properties`, `server.local.properties`, `drive.local.properties`, `keystore.properties`, `gbo-inventario-vial-release`, `.clasp.json`/`.clasprc*`, tokens OAuth, APK/AAB, BuildConfig generado, bases reales, fotografías de campo, logs privados, journals/bindings reales ni fuentes Google configuradas. `.gitignore` contiene exclusiones portables, incluido el keystore sin extensión. Los tres `.properties` de Gradle versionados son configuración de compilación sin credenciales.

Respaldos independientes existentes, restringidos al usuario local y SYSTEM (ACL; no sustituyen cifrado para transportar):

1. `C:\Users\ASUS\GBO_RESPALDO_PRIVADO_20261030_1300` — cierre anterior y privados separados.
2. `C:\Users\ASUS\GBO_CIERRE_FINAL_PRIVADO_20261030` — candidata 1.4.20, bundle, XML/logs y privados; manifest SHA-256.
3. **`C:\Users\ASUS\GBO_PUBLICACION_PRIVADA_20261030`** — respaldo de esta publicación, manual íntegro original, historia privada anterior, snapshot reciente y evidencia de push. No publicar su contenido.

La referencia local **`codex/respaldo-privado-prepublicacion-20261030` NO DEBE PUBLICARSE**. Conserva la historia documental original con el valor omitido. El push de esta intervención debe nombrar exclusivamente la rama de trabajo, sin tags ni otras referencias. La remapación documental está en `REMAPEO_HISTORIA_DOCUMENTAL.json` del respaldo privado.

Para otra laptop, recuperar por medio privado cifrado la configuración legítima de Usuarios y Drive, las propiedades de firma y el keystore histórico. Recrear solo `sdk.dir` para su SDK; no copiar una ruta ASUS/Huawei como si fuera portable. No inventar valores ni cambiar endpoints, cuentas, dependencias o firma. Usar las versiones fijadas por el wrapper y el proyecto. La APK incluye configuración de aplicación: tratarla también como material privado hasta autorizar distribución.

## G. Qué hacer al reanudar, únicamente con nueva autorización del usuario

1. Leer este documento y `GBO_CIERRE_FINAL_CAMPO.md`; comprobar `git status`, rama, HEAD y remoto. No usar como base un WIP antiguo aislado ni repetir la migración ASUS.
2. Verificar si el despliegue original continúa V21 y si Editor sigue concedido a GBO. No volver a pedirlo si se conserva. Identificar exactamente proyecto/raíz/URL mediante el respaldo privado.
3. Resolver el consentimiento pendiente **en el original**, sin crear proyectos o apps paralelos; confirmar los ámbitos requeridos por el auditor. Ejecutar solo el preflight de lectura, conservar el resultado completo y comprobar páginas, estabilidad y estados. No reutilizar los 135 journals del mock como evidencia real.
4. Identificar entregas reales PENDING/ilegibles/inestables y conciliar cada una sin borrar/sobrescribir/adoptar a ciegas. No declarar compatibilidad operativa ni cero pendientes con un inventario incompleto.
5. Si el preflight permite avanzar, preparar la **nueva versión del mismo proyecto sin activarla**. Mantener raíz/URL/token, nombres descriptivos deshabilitados inicialmente, rollback y revisión de compatibilidad. Presentar diferencias exactas y solicitar autorización **antes de cambiar el despliegue activo**. No migrar la raíz.
6. Solo tras corte expresamente autorizado, verificar capacidades, permisos, recuperación y ACK operativo con un caso controlado autorizado; no repetir masivamente el ensayo de 59 archivos ya acreditado.
7. Comprobar conexión de la tablet de Julián y respaldo efectivo antes del piloto. Actualizar sobre la aplicación existente, conservar fichas/fotos y verificar reapertura, SCAP/SIC, ACK, integridad Drive, limpieza y Pavimentos. No desinstalar ni borrar datos. Sin esas pruebas no declarar GO; distribución general requiere autorización aparte.

No retomar estas actividades automáticamente al leer el documento. La instrucción vigente al cierre es **pausa y preservación/publicación**, no desarrollo ni despliegue.

## H. Prompt de continuación recomendado

> Retoma GBO únicamente en la rama codex/observaciones-campo-asus-20261030 del repositorio a20231408-gif/GBOfinal. Lee docs/ESTADO_PARA_CONTINUAR.md, GBO_CIERRE_FINAL_CAMPO.md y la evidencia privada de publicación. La candidata original 1.4.20 (34), con firma histórica, conserva backend anterior y está NO DISTRIBUIR. Android 77/77, backend 14/14 y paquete original 3/3 con mocks ya están acreditados; no repetirlos sin cambios. El propietario ya concedió Editor a GBO en la raíz actual y se verificó por metadatos. El auditor PreflightReadonly está en HEAD del editor original y localmente, pero su ejecución quedó bloqueada por consentimiento OAuth: NO existe inventario completo ni nueva versión/corte. Primero confirma que el usuario autoriza reanudar; luego resuelve el consentimiento y completa la lectura paginada, protegiendo los journals. No crear servidores/apps paralelos, cambiar raíz, tocar Usuarios, instalar tablets ni activar despliegues sin las autorizaciones correspondientes. La tablet prevista es la de Julián por USB, aún sin inspección ni respaldo verificado por Codex. No mostrar secretos ni subir privados; la publicación pública del código sí está autorizada. No publicar la rama local de respaldo privado. Conserva el trabajo y actualiza estados con evidencia real.
