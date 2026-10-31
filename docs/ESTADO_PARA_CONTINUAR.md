# GBO — Estado completo para continuar

## CHECKPOINT DE TRANSFERENCIA — 31/10/2026, America/Lima (vigente)

**DESARROLLO PAUSADO. Esta sección prevalece sobre todos los estados históricos inferiores.**
Encargo: preservar, documentar, respaldar y publicar fuente segura; no ejecutar compilaciones,
pruebas Android/HTTP, cambios Google, instalaciones ni nuevas funciones durante este cierre.
Primer checkpoint publicado y verificado: **`3c028ac1c589f0bdc8f88fb92da6944da07a6da6`**.
Después estuvieron disponibles el prompt de checkpoint y el manual privado del 31/10/2026:
se leyeron y se completa aquí su alcance documental, sin retomar desarrollo. Ambos archivos
se mantienen fuera de GitHub; el manual privado no constituye evidencia de ejecución.

### Complemento de transferencia: checklist exacto y acceso mínimo

La siguiente lista resume P01–P17 del manual privado recibido, sanitizada para publicar.
**Todos los casos están SIN EJECUTAR (0/17), no PASS.** No se realizaron en este cierre.
Su ejecución futura requiere respaldo restaurable, versión/firma comprobadas y autorización
de instalación si corresponde. Mantener modo avión; no modificar fichas históricas ni generar
entregas ficticias contra producción. Si una acción programa Drive, marcar BLOCKED hasta
disponer de un escenario local seguro autorizado. No crear otra app o servidor de ensayo.

| Caso | Comprobación prevista | Estado |
|---|---|---|
| P01 | Actualización Debug 1.4.20 a candidata compatible, sin desinstalar; datos/fotos/conteos conservados | SIN EJECUTAR |
| P02 | Ficha SCAP con conflicto: Requiere atención, sin reintento, nueve preparados y sin ACK inventado | SIN EJECUTAR |
| P03 | SIC-17 requerido cuando corresponda; 17A/B deshabilitados según flags, snapshot intacto | SIN EJECUTAR |
| P04 | Copias modernas primero e historial accesible, sin borrar carpetas | SIN EJECUTAR |
| P05 | Filtros día/mes/año, fecha sin resultados y recuperación del listado | SIN EJECUTAR |
| P06 | Estados y orden cronológico sin desapariciones ni duplicados | SIN EJECUTAR |
| P07 | Borrador ficticio estrictamente local: guardar, cerrar y reabrir | SIN EJECUTAR |
| P08 | Longitud 42+995 a 43+005 = 10 m, persistencia y exportación local segura | SIN EJECUTAR |
| P09 | Descartar y restaurar solo el borrador ficticio | SIN EJECUTAR |
| P10 | Selección individual/visibles y cancelar, sin confirmar borrado masivo | SIN EJECUTAR |
| P11 | Pavimentos: autoguardar varios daños, reabrir, sin publicar por cada cambio | SIN EJECUTAR |
| P12 | Daño 26 por losa/junta: severidades L/M/H independientes y PCI coherente | SIN EJECUTAR |
| P13 | Cierre de muestra: un paquete consolidado, solo si no genera outbox productivo | SIN EJECUTAR |
| P14 | Dos imágenes ficticias/croquis locales: persistencia/categorías, sin inventar GPS | SIN EJECUTAR |
| P15 | Nombres legibles, copia nueva y antigua diferenciadas; no acredita homología Drive | SIN EJECUTAR |
| P16 | Previsualización de limpieza: exige ACK vigente, ZIP validado y confirmación; no ejecutar borrado | SIN EJECUTAR |
| P17 | Abrir/compartir/ubicación y errores comprensibles, detalles técnicos plegables | SIN EJECUTAR |

El manual sintetiza cinco observaciones formales del PDF, **sin que en este cierre se haya
inspeccionado nuevamente ese PDF**: O01 filtros/fechas (IMPLEMENTADO), O02 carpetas legibles
(PARCIAL), O03 fotografías descriptivas (PARCIAL), O04 sello de juntas por losa/severidad
(IMPLEMENTADO), O05 eliminación manual segura (PARCIAL). Ninguna está absuelta físicamente
por este checkpoint. La matriz de abajo conserva código/pruebas y ampliaciones; O08 nuevo
tramo del manual queda actualizado por el merge: diálogo real integrado, validación WIP.
O15 sincronización de Flavio permanece sin verificar; no solicitar su tablet para el trabajo local.

**Orden al reanudar con encargo nuevo:** recuperar fuente/entorno → P01–P17 con respaldo y
autorizaciones, corrigiendo solo FAIL reproducidos → OAuth/lectura paginada y conflicto SCAP
→ proteger permisos del destino con autorización → resolver identidad/revisión entre raíces
→ corte autorizado del mismo Apps Script original → ACK/piloto real → Release/distribución.
La indisponibilidad de OAuth no bloquea trabajo local autorizado. No ejecutar este plan al
leer el checkpoint: el desarrollo sigue pausado.

**Entrega mínima al compañero:** fuente/bundle seguro, esta continuidad, manual privado por
canal restringido y requisitos de aceptación. No necesita de entrada tokens de producción,
keystore Release, datos reales ni acceso Google. Configuración Usuarios/Drive: solo si la tarea
o el perfil original la requiere, cifrada y con acceso autorizado. Firma Release: únicamente
responsable de publicación. Debug Huawei/ASUS: solo si hay necesidad justificada de actualizar
la tablet personal. Snapshot/propiedades/journals: solo operador autorizado de sincronización.
No distribuir indiscriminadamente todo el respaldo privado a cada desarrollador.

Preparación en la siguiente laptop (instrucciones futuras, no ejecutadas en este cierre):

1. Instalar Git for Windows y Android Studio con JBR compatible, SDK/Platform Tools/Build Tools
   exigidos por el proyecto. Inspeccionar wrapper y archivos Gradle existentes, sin cambiar
   versiones para sortear errores; Node solo para pruebas backend locales.
2. Clonar en carpeta nueva, comparar SHA publicado, leer continuidad/manual y crear rama propia:
   `git switch -c codex/continuacion-companero-20261031` (usar nombre nuevo si ya existe).
3. Recrear `local.properties` con sdk.dir del equipo. En PowerShell establecer por terminal
   `$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'`, comprobar
   `.\gradlew.bat --version` y, cuando se autorice reanudar validación local,
   `.\gradlew.bat :app:assembleDebug`. No instalar esa compilación automáticamente.
4. Si el perfil exige configuración privada, solicitar únicamente la indispensable; no inventar
   endpoints ni copiar tokens de ensayo. Reconfirmar flags V2/17A/17B. No pedir firma Release
   para tareas de UI y pruebas. Compilar sin configuración productiva no demuestra integración real.
5. Alternativa sin GitHub: `git bundle verify` y
   `git clone -b codex/observaciones-campo-asus-20261030 C:\RESPALDO_PRIVADO\rama-final.bundle C:\GBO`;
   establecer origin al repositorio autorizado y crear rama propia. Comparar hash del bundle
   con el manifiesto transferido por canal seguro antes de usarlo.

**Complemento privado más reciente:**
`C:\Users\ASUS\GBO_CHECKPOINT_COMPLEMENTO_PRIVADO_20261031\`.
Preserva ambos adjuntos íntegros, continuidad sanitizada, bundle final, manifiesto y verificación
de publicación. Es adicional al respaldo completo de 657 archivos; no lo reemplaza ni sobrescribe.
Para transferir código usar el bundle más reciente; para evidencias/configuración usar el respaldo
completo anterior y este complemento según el acceso mínimo necesario. Ninguno es respaldo
restaurable de la tablet.

### Estado Git y código que se transfiere

- Repositorio autorizado: `https://github.com/a20231408-gif/GBOfinal`.
- Rama exclusiva: `codex/observaciones-campo-asus-20261030`. Publicar solo esta referencia,
  sin main, tags, force push, merge a main ni la rama de respaldo privado.
- HEAD ASUS inicial: `e13ad2ca6536903f6be1141b68dfc9a6563d5a84` (árbol limpio).
- Al consultar GitHub se encontraron tres commits adicionales de la otra laptop:
  `7559623` (exclusiones privadas), `d833927` (nuevo tramo) y `fbf5a13` (continuidad/diálogo).
  Se preservaron junto con los ocho commits ASUS mediante merge sin conflictos,
  **`9817cc2d016df5e00eb297f53b037e27c89ec6cb`**; no se reescribió ningún commit.
- El cambio remoto de nuevo tramo utiliza `SessionDialog(requireRoute=true)` y
  `FieldController.startSession`; exige ruta y restablece filtros tras éxito. Sus fuentes
  están integradas, **pero la integración no está compilada ni probada**. La otra laptop
  documentó un fallo JVM de loopback antes de poder compilar. Es WIP de validación.
- Fuentes actuales: **GBO 1.4.21 (35)**, package original `com.tuempresa.inventariovial`.
  Kotlin/Compose, Room 21, WorkManager y documentos locales; servidores Usuarios y Drive
  separados. No se cambiaron esquema Room, dependencias, endpoints ni firma durante el cierre.
- La continuidad se guarda en un commit documental posterior al merge. Para obtener el SHA
  final: `git rev-parse HEAD`; compararlo con `git ls-remote origin
  refs/heads/codex/observaciones-campo-asus-20261030`. La evidencia privada y la respuesta
  final registran ambos SHA y el resultado real del push, evitando un SHA autorreferente.
- GitHub público está expresamente autorizado para fuente/documentación revisadas; quedan
  excluidos privados y binarios. No volver a solicitar permiso únicamente por visibilidad.

### Correcciones conservadas y grado de validación

Las rutas Android siguientes parten de `app/src/main/java/com/tuempresa/inventariovial/`.
**IMPLEMENTADO no significa validado físicamente.** Todas las pruebas manuales formales
de la candidata siguen pendientes.

| Observación | Estado al transferir | Fuente / evidencia local existente |
|---|---|---|
| Filtros por fecha, estados y orden | IMPLEMENTADO | `field/RecordDateFilter.kt`, `localfiles/UnifiedRecordsScreen.kt`; `RecordDateFilterTest`, `LocalDraftDiscard10CTest`. |
| Selección y limpieza segura | IMPLEMENTADO para copias confirmadas; PARCIAL para eliminación total | `ConfirmedLocalCleanup.kt`, `SharedDocumentStore.kt`, `core/LocalDraftDiscard.kt`; respaldo externo releído/hash, ACK vigente y protección de pendientes. `FieldCleanupAndConsolidationTest`. No borrar base ni originales. |
| Descarte/restauración | IMPLEMENTADO | Descarte reversible y restauración de originales retenidos; no es importación universal de Room desde ZIP. Pruebas de limpieza/descarte. |
| Nuevo tramo | PARCIAL / integración WIP | `field/FieldUi.kt`, `UnifiedRecordsScreen.kt`; commits remotos preservados. Ahora inicia sesión con ruta; no compilado ni probado tras el merge. |
| Longitud persistida y exportada | IMPLEMENTADO | `repository/TechnicalLengthStore.kt`, `export/TechnicalLengthExport.kt`; `LinearLengthDeliveryTest` (42+995 a 43+005 = 10 m). |
| Nombres y carpetas | PARCIAL | `PublicDeliveryNames.kt`, `PublicLocalFolderNames.kt`, `ReadablePhotoNames.kt`. Local moderno legible; remoto depende de capacidades que V21 no anuncia. No renombrar históricos. |
| Copias locales modernas e historial | IMPLEMENTADO | `LocalCopyPresentation.kt`, `LocalFilesUi.kt`, `UnifiedRecordsScreen.kt`; commits `6344fca`, `d6002fa`. Copias anteriores accesibles, no borradas; copia válida anterior protegida si regeneración falla. |
| Regeneración local sin reenviar | IMPLEMENTADO | `LocalCopies.kt`: solicitud manual/espejo no programa Drive; publicación automática conserva su circuito. `LocalFiles148Test`. |
| SCAP / identidad histórica | PARCIAL | `AutomaticSync.kt`, `DeliveryRejectionPolicy.kt`, `DriveDeliveryPresentation.kt`; `BridgeIdentityConflictTest`. Conflicto permanente conserva UUID, revisión, preparación y ACK; sin reintento ciego. La colisión remota concreta no se ha conciliado. |
| SIC y flags opcionales | IMPLEMENTADO en cliente | `SicContributionPolicy.kt`, `AutomaticDelivery.kt`, `ScapExportPanel.kt`; `SicContributionPolicyTest`, `DriveDeliveryCardUiTest`. Flags actuales: 17A=false, 17B=false, V2=true. No exigir opcionales ni reescribir paquetes congelados. SIC-17 requerido si corresponde. |
| Pavimentos, daño 26 y PCI | IMPLEMENTADO; físico PENDIENTE | `pavement/`, `AutomaticLocalDocuments.kt`, `LocalCopies.kt`; `PavementFinalIntegrityTest`, `PavementJointFlow20261019Test`, `PavementP0AdditionalTest`. L/M/H independientes, autoguardado; motor PCI conservado. |
| Consolidación al cerrar unidad | IMPLEMENTADO | Publicación automática solo CLOSED, trabajos borrador SUPERSEDED, manual provisional disponible; `AutomaticDocuments149Test`, `FieldCleanupAndConsolidationTest`. |
| Progreso y apertura de documentos | IMPLEMENTADO | `DriveDeliveryCard.kt`, `AutomaticSyncPanel.kt`, `LocalFilesUi.kt`; motivo legible, técnicos plegables, revisión segura, sin reintentar conflicto permanente. |
| Backend reanudable y recuperación | PARCIAL | `backend/aislado-commit-resumible/`, `backend/candidato-original-reanudable/`; fuentes y rollback preservados, no activados en original. |
| Migración Google / destino definitivo | PENDIENTE | Guarda local `AuthorizedRootGuard.gs`, auditor `PreflightReadonly.gs`; no resuelven por sí solos coexistencia entre raíces ni autorizan el corte. |

### Pruebas acreditadas; ninguna ejecutada de nuevo en este cierre

- Históricas de cierre: **Android 77/77 PASS**, backend reanudable **14/14 PASS** y
  paquete original **3/3 PASS con mocks**. No representan toda la suite histórica.
- Guarda/paquete anterior **27/27**; auditor ampliado **7/7** y propuesta **13/13**,
  solamente mocks. La variante anterior del auditor tuvo 3 casos: no sumarlos como otra
  selección nueva ni confundirlos con los 3 del paquete original.
- Último SCAP: **63 casos Android distintos acreditados PASS** por clase/método, no una
  ejecución única de 63 ni suite completa. Intento inicial 61/62 (normalización incrementaba
  intentos), implementación corregida, repetición 25/25 y final 26/26 con ACK vigente.
  XML separados y mapa `tmp/scap-conflicto-20261031/resultados-android.json`.
- Identidad backend: **10/10 PASS**, sobre V21 y candidato con mocks. No se modificó el
  servidor. Dos textos SIC finales cambiaron después de esos tests; ambas APK finales
  compilaron. No afirmar que se repitieron tests después de esos dos textos.
- Evidencia HTTP antigua **solo del ensayo archivado**: recuperación de 59 archivos,
  62/62 hashes/propietarios/rutas, journal COMPLETE 67/67, ACK, replay y BUSY.
  Segunda raíz ficticia solo staging 59/59, sin commit/ACK. No acredita producción.
- **Las 17 pruebas manuales formales TODAVÍA NO SE REALIZARON: 0/17 acreditadas** para
  la candidata 1.4.21 (35). La apertura de sesión/archivo y los conteos reportados por el
  usuario corresponden al Debug 1.4.20 instalado; no equivalen a aprobar ese checklist,
  una actualización física ni la Release. El manual recibido posteriormente ya proporciona
  su lista exacta: resumen sanitizado arriba y detalle privado, todavía sin ejecución.
- **No hay compilación ni pruebas del merge `9817cc2`**. No extender resultados previos
  al inicio de nuevo tramo integrado desde la otra laptop.

### APK preservadas: fuentes compiladas anteriores al merge

No hay nueva compilación, instalación ni distribución en este checkpoint. Las dos finales
1.4.21 provienen de `4b284629a1d93014c08b8f55c4779a69c9fbe185`; **no contienen el nuevo
tramo incorporado desde GitHub**. Si se reanuda desarrollo sobre el merge, preparar y validar
otra candidata con versión superior antes de distribuir; no reemplazar artefactos en silencio.

Directorio privado: `C:\GBO\tmp\scap-conflicto-20261031\candidata-final-1.4.21-35\`.

| Artefacto original | SHA-256 | Firma y uso |
|---|---|---|
| `GBO_1.4.21_35_DEBUG_ORIGINAL_PILOTO_LOCAL.apk` | `9d7e4d19d6850bf164fb779d7ececdada7f5cd93e36550f0398b7c33612da8b2` | Debug Huawei/ASUS; coincide con Debug instalado; solo candidato offline tras respaldo y autorización. |
| `GBO_1.4.21_35_RELEASE_HISTORICA_NO_DISTRIBUIR.apk` | `2bc49613fc293404882d9cddce6a971c294aea1f7fb42e9f092094bcd2967356` | Release histórica; NO actualiza directamente el Debug instalado de distinta firma. NO DISTRIBUIR. |

Paquete original, versión/código, alineación 16 KiB y firma acreditados en validación privada.
Certificados SHA-256: Debug `d2e54cb086aa731ef046be300977d0b16c0d8f66fdcb08d0d9cfd2038bf0d872`;
Release `0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315`.
Room 21 sin cambio de esquemas; esto no acredita actualización física con datos reales.
DEX conserva endpoints/tokens legítimos originales y excluye ensayo comprobado; valores
no publicados. APK/BuildConfig también son privados. No usar las APK intermedias de la carpeta
sin «final». La 1.4.20 Release histórica sigue íntegra en el respaldo descrito más abajo.

### Backend, Google y conflicto: bloqueos que deben conservarse

Último estado acreditado: **original V21**, mismo proyecto/deployment/URL, raíz anterior y
autenticación; Usuarios independiente intacto. No se volvió a verificar Google en este cierre.
La copia de ensayos está archivada, sin implementaciones activas; **no volver a usarla**.
El nuevo destino elegido no está activado y tiene edición pública preexistente pendiente de
tratamiento autorizado. No cambiar permisos, raíz o despliegue al restaurar esta laptop.

`PreflightReadonly.gs` está en HEAD del editor original, no en V21, pero OAuth/segundo factor
legítimo sigue pendiente. **Totales reales COMPLETE/PENDING/ilegibles desconocidos**; lectura
antigua limitada a 100. No considerar los journals de mocks inventario operativo. El candidato
es de una sola raíz: cambiar ROOT puede perder revisión/binding/ACK previo o duplicar una
primera entrega. Requiere conciliación y estrategia verificable, no solo guarda de destino.
Rollback del mismo despliegue a V21 está preparado, no ejecutado; no elimina transacciones.

BRIDGE_IDENTITY_CONFLICT compara bridgeCode global contra identityId distinto. Android
envía identidad padre roadRecordId estable y SCAP/UUID-inspección; sesión/nombre/ruta no
sustituyen UUID. También bloquea un histórico sin identityId. **Fila remota concreta no leída**:
necesita posterior comparación autorizada de key/recordId/identityId/bridgeCode/routeCode/
revisión/pathVersion. No adoptar, cambiar identidad, duplicar ficha ni reintentar para eludirlo.
La ficha con conflicto conserva nueve preparados sin ACK final; eso no prueba nueve subidas.

### Respaldo y traslado seguro

Nuevo respaldo independiente fuera del repositorio:
`C:\Users\ASUS\GBO_CHECKPOINT_TRANSFERENCIA_20261031_1_4_21\`.
Contiene bundle previo de ambas puntas, bundle final de la rama, continuidad, evidencia de
seguridad/push, XML/logs y APK privados, configuración legítima y firma en subcarpeta separada.
Los bundles se verifican y los archivos se inventarían por SHA-256. ACL usuario/SYSTEM no es
cifrado: transportar el material privado por canal cifrado y no incorporarlo a GitHub.

Privados necesarios para otra laptop, **sin publicar valores**:
`server.local.properties`, `drive.local.properties`, `keystore.properties`, keystore Release
referenciado y `.private-signing/huawei-debug.keystore`; para reproducir Debug, keystore
estándar ASUS de igual certificado. `local.properties` se conserva como referencia, pero
recrear exclusivamente sdk.dir para el SDK destino. No copiar credenciales OAuth de navegador
o Git Credential Manager: autenticar por el flujo oficial. Fuente configurada de Apps Script,
IDs/propiedades, reportes con datos y journals van separados, nunca al repositorio público.
El ZIP técnico o el pull de la APK instalada **no son respaldo restaurable de Room/fotos**.

Restauración de fuente (PowerShell; usar una carpeta nueva, no sobrescribir trabajo existente):

```powershell
git clone --branch codex/observaciones-campo-asus-20261030 --single-branch https://github.com/a20231408-gif/GBOfinal.git C:\GBO
Set-Location C:\GBO
git status
git rev-parse HEAD
git ls-remote origin refs/heads/codex/observaciones-campo-asus-20261030
# Alternativa offline: verificar el bundle y clonarlo en una carpeta nueva.
git bundle verify C:\RESPALDO_PRIVADO\rama-final.bundle
```

Usar JDK/SDK locales y versiones del wrapper/proyecto; recuperar privados por canal seguro,
verificar hashes y mantener exclusiones portables. No compilar, enviar ni instalar automáticamente.
La rama `codex/respaldo-privado-prepublicacion-20261030` y sus bundles nunca se publican.

### Prompt vigente para la siguiente sesión

> Retoma GBO desde la rama publicada codex/observaciones-campo-asus-20261030. Lee primero
> esta sección CHECKPOINT DE TRANSFERENCIA y compara HEAD/remoto con la evidencia del cierre.
> Lee el manual privado transferido por el propietario y trabaja en una rama propia codex/.
> Conserva todos los commits, incluidos los tres de la otra laptop integrados en 9817cc2.
> Fuentes 1.4.21 (35); APK finales privadas de 4b284629 anteriores al merge. Inicio de nuevo
> tramo integrado sigue sin compilar/probar. Las 17 pruebas manuales NO se realizaron (0/17);
> usa el checklist sanitizado de esta sección y el detalle del manual. No repetir ensayos HTTP
> grandes ni auditar otra vez la migración. Tras nuevo encargo, comprobar Debug local y pedir
> únicamente configuración privada indispensable; no dar por válida una compilación de la integración.
> Existe Debug 1.4.20 en la tablet personal: firma distinta de Release, sin respaldo restaurable
> acreditado; no instalar/desinstalar/limpiar sin autorización. Conflicto SCAP conserva identidad,
> nueve preparados y ausencia de ACK, sin reintento; falta conciliación remota real autorizada.
> V21 original sigue operativo, candidato reanudable no activado y copia de pruebas archivada.
> OAuth y journals completos pendientes; cambio de raíz no es seguro sin reconciliación.
> Usuarios intacto. Recupera los archivos privados por canal cifrado sin subirlos ni mostrarlos.
> Esta lectura no autoriza desarrollo, Google, instalación o corte: pide el siguiente objetivo
> al usuario y respeta las autorizaciones específicas; no solicites nuevamente permiso por
> visibilidad pública del código seguro. NO-GO para sincronización/corte/distribución general.

---

## Último cierre — conflicto SCAP y candidata 1.4.21 (35), 31/10/2026

Este cierre sustituye las notas anteriores que dejaban pendiente incrementar versión.
Código principal `d6002fae9eef2eb46e159edfcd69a986bcaf8526`; ajuste final de dos avisos SIC
`4b284629a1d93014c08b8f55c4779a69c9fbe185`. Se conservaron todos los commits anteriores.
No hubo acceso a Drive/Apps Script, HTTP real, instalación ni modificación de datos de tablet.

Conflicto: V21 y el candidato rechazan un bridgeCode ya registrado con identityId distinto
del UUID padre enviado. Comparación global exacta: no depende del nombre, ruta o progresiva.
Un registro histórico sin identityId también puede bloquear; no se adopta automáticamente.
Android usa key SCAP/UUID-inspección e identityId=roadRecordId, no la sesión activa. Las reservas
V2 se vinculan a ese UUID padre; números/nombres son etiquetas y nunca sustituyen identidad.
Se probaron identidad compartida SIC/SCAP, replay y sesión ajena sin alterar fichas históricas.
**La fila remota concreta que colisiona sigue sin conocerse**: requiere lectura autorizada de
key/recordId/identityId/bridgeCode/routeCode/revisión/pathVersion y comparación con la preparación.
No crear otra ficha, cambiar UUID/código para eludir el validador ni limpiar los archivos preparados.

Correcciones: BRIDGE_IDENTITY_CONFLICT es permanente aunque el servidor diga retryable;
normaliza marcadores antiguos ERROR/BLOCKED a CONFLICT sin incrementar intentos ni alterar
preparación/ACK/revisiones. Bloquea reintento manual/automático y conserva el bloqueo al editar.
ACK completo vigente prevalece sobre etiquetas de error obsoletas. Mensaje principal comprensible,
«Requiere atención», revisión de ficha como acción segura, datos técnicos en sección plegable.
No se implementó una liberación automática del conflicto ni se relajó el validador del backend.

Flags comprobados en la APK Debug instalada guardada y configuración legítima actual:
SIC17A=false, SIC17B=false, nombres V2=true. Nuevas preparaciones filtran aportes deshabilitados;
SIC-17 sigue requerido cuando faltan datos. Una preparación ya congelada no se reescribe:
pendientes opcionales antiguos se explican como deshabilitados en el detalle técnico.
No se inventan datos, consolidaciones completas, revisiones o ACK.

Archivos: también el selector rápido prioriza copias modernas, permite abrir anteriores y
conserva la única copia válida si la nueva no terminó. Exportación/regeneración manual y
reconciliación local no programan Drive; publicación automática conserva su circuito original.
Sin borrado, renombrado histórico ni migración de directorios. Room 21 y sus esquemas intactos.

Validación: **63 casos Android distintos** acreditados en selección acumulada, 0 pendientes
de aprobación en esa selección, no suite histórica completa. Primer intento 61/62; corrigió
contador de intentos de la normalización. Repetición relacionada 25/25 y final 26/26 incluyendo
un caso nuevo de ACK vigente. XML separados y mapa por clase/método en
`tmp/scap-conflicto-20261031/resultados-android.json`. Backend local: **10/10 PASS**, sobre fuente
V21 y candidato, sin HTTP ni mutaciones. No se cambió código servidor. Dos textos finales de
avisos SIC se ajustaron después de esas pruebas; las dos variantes finales compilaron y se
verificaron, sin repetir pruebas funcionales no afectadas por ese texto.

**Candidatas finales originales, ninguna instalada/distribuida:** package
`com.tuempresa.inventariovial`, 1.4.21 (35), origen de fuentes `4b284629`.
Directorio privado `C:\GBO\tmp\scap-conflicto-20261031\candidata-final-1.4.21-35\`:

- `GBO_1.4.21_35_DEBUG_ORIGINAL_PILOTO_LOCAL.apk`, SHA256
  `9d7e4d19d6850bf164fb779d7ececdada7f5cd93e36550f0398b7c33612da8b2`.
  Firma Debug Huawei/ASUS, coincide con la tablet personal; candidata para revisión offline
  solo tras respaldo y autorización expresa de actualización.
- `GBO_1.4.21_35_RELEASE_HISTORICA_NO_DISTRIBUIR.apk`, SHA256
  `2bc49613fc293404882d9cddce6a971c294aea1f7fb42e9f092094bcd2967356`.
  Firma Release histórica, distinta de Debug; no intentar instalarla sobre el Debug existente.

Firma/versión/paquete/alineación 16 KiB verificados. DEX acredita endpoint/token originales
de Drive y Usuarios y ausencia del endpoint V4/token de ensayo comprobados, sin mostrarlos.
La configuración no se cambió para habilitar la raíz nueva. La carpeta `candidata-1.4.21-35`
sin «final» es una compilación intermedia preservada: usar solo la final y su hash.
Las APK contienen configuración privada: no agregarlas a Git ni distribuirlas.

Prueba manual propuesta (no ejecutada): después de autorizar respaldo/actualización **Debug**,
mantener modo avión, comprobar registros/fotos conservados, abrir la ficha con conflicto:
nombre/ruta visibles, «Requiere atención», sin Reintentar, preparación intacta sin ACK inventado.
SIC-17A/B deben figurar como deshabilitados solo en detalle; SIC-17 sigue pendiente si corresponde.
Abrir documento ya confirmado y alternar historial local sin borrar ni reenviar. No sincronizar
para comprobar el conflicto. Resultado físico pendiente; corte/sincronización siguen NO-GO.
Resumen privado actualizado: `GBO_CIERRE_OPERATIVO_Y_TABLET_20261031.md`.

---

## Actualización vigente — servidor original y tablet personal (31/10/2026)

Última respuesta del usuario: Google exige segundo factor del teléfono de un ingeniero que
no está disponible hasta la mañana. No reintentar ni eludirlo; continuar con la app local.
Prueba manual Debug recibida: sesión correcta, 15 sincronizados, 0 pendientes y **1 error**;
puede abrir archivo/ubicación del SCAP «prueba 3». No son ACK verificados remotamente.
Pendiente texto del error y ejemplos de carpetas antiguas; no asumir que ese SCAP es el fallido.

Corrección Android posterior a la APK 1.4.20: el listado local agrupa la regeneración V4 con
su categoría histórica y permite ver/ocultar historial incluso con solo dos intentos.
Pruebas focalizadas **6/6 PASS**, 4 clases, 0 fallos/errores/omitidas; 8,594 s XML, Gradle 55 s.
Compilación Kotlin ordinaria posterior PASS (20 s). Fallos previos de compilación incremental
y argumento de terminal conservados en logs; no se acreditan como pruebas ejecutadas.
No se duplicó la regeneración existente ni se tocaron originales/Room/ACK/Drive; no se limpiaron
carpetas de la tablet. La limpieza física sigue pendiente de inventario y respaldo, no basta
ocultar historial. Cambio aún no instalado. **La próxima APK que incluya esta corrección debe
usar versión superior a 34**: no recompilar silenciosamente 1.4.20; las fuentes aún la conservan.
La APK Release guardada sigue siendo el artefacto original e intacto, no contiene esta corrección.

La instrucción vigente permite inventario de lectura del original, propuesta local de corte
y comprobación ADB de la tablet personal. No autoriza cambiar permisos/ROOT/despliegue,
instalar/desinstalar ni borrar datos. No reutilizar la copia archivada de ensayos.
Resumen privado único: `C:\GBO\GBO_CIERRE_OPERATIVO_Y_TABLET_20261031.md` (ignorado).

ADB detectó una tablet personal Xiaomi con el package original **1.4.20 (34) Debug/TEST_ONLY**.
El certificado instalado coincide con el Debug Huawei recuperado y el Debug ASUS actual;
la firma Release histórica es otra. **La Release preparada no puede actualizar directamente
ese Debug**. No intentar instalarla, desinstalar ni limpiar datos para sortearlo. El keystore
Huawei está ignorado y no rastreado; Debug actual usa el keystore estándar de ASUS con el
mismo certificado. SigningReport y validateReleaseConfiguration PASS. La revisión DEX del
instalado encontró la configuración productiva actual, sin mostrar valores; no acredita su
commit exacto y no se extrajeron fichas/fotos/Room. No se recompiló ninguna APK.

Discrepancia corregida: al abrir el editor original, `PreflightReadonly.gs` no aparecía,
aunque el documento anterior lo daba por guardado. Se añadió a HEAD y se comprobó su fuente
por portapapeles. Se usa Drive.About con el ámbito Drive existente; no se alteró el manifest.
El auditor paginado ahora emite journals, revisiones/ACK y bindings y detecta inconsistencias
sin doble conteo. Añade sonda de lectura del destino/cadena/capacidades/ACL. Solo la copia
privada del auditor contiene IDs configurados. El intento de ejecución solicitó consentimiento
oficial; se abrió Revisar permisos y se pidió al usuario completarlo como GBO. **No hay aún
totales reales ni sonda de destino ejecutada**, salvo que el resumen privado se actualice con
evidencia posterior. No interpretar mocks ni el viejo límite de 100 como inventario completo.

Pruebas nuevas focalizadas: auditor 7/7 y guarda/paquete 13/13, total **20/20 PASS**;
sin repetir Android ni continuidad masiva. Propuesta privada con ROOT elegido en
`tmp/piloto-operativo-20261031/PROPUESTA_ORIGINAL_NO_ACTIVAR/`; no se copiaron módulos
operativos al editor ni se creó/activó versión. V21, URL, Usuarios y ROOT real siguen intactos.
Lectura de metadatos confirma anyone:writer en destino y los dos ancestros: todavía debe
atribuirse la herencia y prepararse la restricción sin perder accesos legítimos, con autorización.

**Bloqueo adicional del cambio de ROOT:** el candidato sigue siendo de una sola raíz.
Los registros/bindings/recibos antiguos no pasan a la nueva por conservar URL/UUID. Una
edición con baseRevision previa puede fallar y una repetición de primera revisión puede
duplicarse entre raíces. La guarda no resuelve enrutamiento de identidades antiguas.
Completar conciliación y una estrategia verificable antes de autorizar corte; no aplicar
simplemente el literal ROOT nuevo del paquete preparado. No se han aprobado observaciones
manuales: se solicitó al usuario revisión inicial en modo avión, atribuible al Debug instalado.
El reporte privado contiene el checklist breve y las autorizaciones pendientes.

---

## Actualización vigente — incidencia de destino Drive (30/10/2026)

Esta actualización sustituye las instrucciones antiguas de pausa o de mantener indefinidamente
la raíz previa. La tarea autorizada actual es auditar el destino, preparar guardas **locales**
y cerrar/archivar exclusivamente los ensayos autorizados por el usuario;
no activar producción, cambiar raíz, migrar históricos, crear servidores ni otra APK.

Base publicada al comenzar: `80626e07323028b686f27c29588c82395f25eeca`, rama
`codex/observaciones-campo-asus-20261030`. Las modificaciones de esta incidencia quedan
locales; consultar `git log -1` para su commit, sin confundirlo con el remoto.
Corrección y pruebas preservadas en commit local `91d6188` (sin push).

Se localizó por navegación real en GBO la ruta `Mi unidad / 05 V CAMPO / 1.- ARCHIVOS CAMPO APK`.
Los detalles indican propietario GBO y acceso privado. El usuario entregó después el enlace
de otra carpeta descartada en el encargo y **confirmó expresamente usar ese enlace aunque
es otra carpeta**: el destino elegido pasa a ser `1.- ARCHIVOS CAMPO`, sin APK. No renombrar
ni usar automáticamente la carpeta APK observada. Los IDs,
inventario y evidencias están en `C:\GBO\GBO_CORRECCION_DESTINO_Y_LIMPIEZA.md`, **privado/ignorado**.
El conector Drive usa otra cuenta y no ve la carpeta privada; no interpretar su 404 como ausencia.

Cambio preparado: `AuthorizedRootGuard.gs`, adaptación de `SyncV3.gs` y empaquetador local
de Código.gs/VideoV1.gs. Exige ID aprobado, padre exacto, nombres confirmatorios, cadena a
Mi unidad y permisos de escritura antes de crear; rechaza fallback y bloquea todas las rutas
autenticadas sin identidad válida. El paquete privado conserva por ahora ROOT/URL/token
originales; no se establecieron las propiedades de autorización ni se cambió el servidor.
Resultado final tras adaptar la cadena anidada del destino elegido: **27/27 PASS**
(8 guarda, 5 paquete, 14 continuidad), 0 fallos/omitidas, 39,199 s;
solo mocks, sin HTTP. Android y APK 1.4.20(34) intactos; no se repitieron pruebas Android.

Dos carpetas de pruebas y la copia Script se observaron de propiedad GBO y restringidas.
Se originaron en `DriveApp.createFolder` del preparador aislado, directamente en Mi unidad.
Primera raíz: evidencia previa de journal COMPLETE 67/67, 62 hashes y ACK/replay verificados.
Segunda: staging/reservas conservados, sin commit/ACK; registro observado sin journal de commit.
No borrar ninguna: contienen evidencias/recuperación ficticia. **Cierre autorizado y ejecutado**:
las tres implementaciones de la copia (V4 HTTP, V2 y V1) están archivadas; después de recargar
Apps Script se verificó «No hay implementaciones activas» y cuatro archivadas, incluida la
HTTP archivada anteriormente. Se creó un único `ARCHIVO_TECNICO_GBO` privado en Mi unidad,
propietario GBO, y se trasladaron allí las dos raíces y la copia Script. Readback: exactamente
los tres IDs originales conservados, ya ausentes directamente de Mi unidad. No se borró
contenido ni se modificaron los permisos existentes. Antes del traslado se verificaron
los 45 archivos del respaldo previo contra su manifiesto, sin discrepancias, y el bundle.
Evidencias nuevas en `tmp/entrega-20261030/backend-privado/incidencia-destino-20261030/cierre-autorizado/`.

Un token exclusivamente de pruebas se imprimió accidentalmente durante una lectura local
de propiedades en esta intervención. No se reproduce aquí ni se incorporó a Git. Debe
mantenerse inactivo: los despliegues de ensayo ya se archivaron con autorización. Antes de
cualquier reapertura se debe invalidar el token mediante el flujo legítimo; no se cambió
credencial alguna ni se expuso el token productivo. No se hicieron nuevas peticiones HTTP,
compilaciones o pruebas durante el cierre autorizado. V21, sus datos y la APK siguen intactos.

Producción: último estado acreditado V21, URL/autenticación/raíz anteriores conservadas.
Inventario operativo aún limitado a 100 entradas y una muestra COMPLETE; el auditor
paginado del editor sigue sin ejecución real por consentimiento OAuth. No existe total
operativo fiable ni certeza de cero PENDING. **NO-GO para cambio de raíz/corte/piloto**.
Se verificó además que el destino elegido está bajo `Mi unidad / ARCHIVOS DE CAMPO / 05 V CAMPO`,
con GBO propietario y **edición pública por enlace** existente. No se cambiaron permisos.
Antes de solicitar corte: resolver esa exposición, completar inventario estable y conciliar colas
de las tablets con sus reservas/revisiones/ACK. Mantener Usuarios separado. No mover datos
reales ni instalar/distribuir la APK sin autorización. El informe privado detalla rollback
con valores previos y tratamiento separado de pendientes; no repetir ensayos masivos.

---

## Antecedentes del cierre anterior (estado histórico, subordinado a la actualización)

Fecha de cierre: **30/10/2026, America/Lima**. **DESARROLLO PAUSADO por orden del usuario.** Este documento sustituye los estados anteriores de continuidad; los informes históricos conservan su fecha y no deben interpretarse como estado actual.

## A. Estado general y Git

- Repositorio: `C:\GBO`; remoto autorizado: `https://github.com/a20231408-gif/GBOfinal.git`.
- Rama única de publicación: `codex/observaciones-campo-asus-20261030`. No modificar main ni ramas históricas; no merge, force push, `push --all` ni `push --mirror`.
- Versión Android: **1.4.20 (34)**, aplicación original `com.tuempresa.inventariovial`.
- Último commit de código de la candidata APK: **`2bad57c9d7666dfe7673129bee41363bcb8eb48f`**. Sus fuentes no se modificaron durante la preparación del piloto ni esta pausa.
- Último commit del preflight antes de actualizar este documento: **`3a483cbd0a776bdf430fbf16360ca1602a2ac75b`** (WIP). El commit que contiene esta continuidad se obtiene con `git log -1 --format=%H -- docs/ESTADO_PARA_CONTINUAR.md`; el SHA final y su comprobación remota se registran también en la evidencia privada de cierre.
- Estado de publicación: **PUSH REALIZADO Y VERIFICADO**. La publicación inicial confirmó el commit **`7204cd5c4e1fb92a7b5f0680440892a42cbb1f6d`**, con igualdad de SHA local/remoto. Este ajuste documental registra el resultado en un commit posterior; obtener su SHA exacto con `git log -1` y contrastarlo con `git ls-remote origin refs/heads/codex/observaciones-campo-asus-20261030`.
- La autenticación Git quedó **completada por el usuario** mediante Git Credential Manager y cuenta `a20231408-gif`. El bloqueo inicial de HTTPS está resuelto; no era un rechazo por visibilidad ni por secretos. La sesión del conector GitHub de Codex es independiente del transporte Git local. **No compartir tokens/contraseñas por chat.** No confundir esta autenticación completada con el consentimiento de Apps Script, que sigue pendiente.
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
   La publicación está verificada; comprobar el nuevo HEAD antes de trabajar y conservar el upstream. Este paso de preservación **no autoriza reanudar Google, despliegues, pruebas o desarrollo**. La referencia privada de respaldo nunca debe enviarse.
2. Verificar si el despliegue original continúa V21 y si Editor sigue concedido a GBO. No volver a pedirlo si se conserva. Identificar exactamente proyecto/raíz/URL mediante el respaldo privado.
3. Resolver el consentimiento pendiente **en el original**, sin crear proyectos o apps paralelos; confirmar los ámbitos requeridos por el auditor. Ejecutar solo el preflight de lectura, conservar el resultado completo y comprobar páginas, estabilidad y estados. No reutilizar los 135 journals del mock como evidencia real.
4. Identificar entregas reales PENDING/ilegibles/inestables y conciliar cada una sin borrar/sobrescribir/adoptar a ciegas. No declarar compatibilidad operativa ni cero pendientes con un inventario incompleto.
5. Si el preflight permite avanzar, preparar la **nueva versión del mismo proyecto sin activarla**. Mantener raíz/URL/token, nombres descriptivos deshabilitados inicialmente, rollback y revisión de compatibilidad. Presentar diferencias exactas y solicitar autorización **antes de cambiar el despliegue activo**. No migrar la raíz.
6. Solo tras corte expresamente autorizado, verificar capacidades, permisos, recuperación y ACK operativo con un caso controlado autorizado; no repetir masivamente el ensayo de 59 archivos ya acreditado.
7. Comprobar conexión de la tablet de Julián y respaldo efectivo antes del piloto. Actualizar sobre la aplicación existente, conservar fichas/fotos y verificar reapertura, SCAP/SIC, ACK, integridad Drive, limpieza y Pavimentos. No desinstalar ni borrar datos. Sin esas pruebas no declarar GO; distribución general requiere autorización aparte.

No retomar estas actividades automáticamente al leer el documento. La instrucción vigente al cierre es **pausa y preservación/publicación**, no desarrollo ni despliegue.

## H. Prompt de continuación recomendado

> Retoma GBO únicamente en la rama publicada codex/observaciones-campo-asus-20261030 del repositorio a20231408-gif/GBOfinal. Lee docs/ESTADO_PARA_CONTINUAR.md, GBO_CIERRE_FINAL_CAMPO.md y la evidencia privada de publicación. El push y la autenticación Git ya se completaron y se verificó el SHA remoto; comprobar el HEAD actual antes de trabajar. La autorización pública del código ya existe: no pedir nuevamente permiso solo por visibilidad. La candidata original 1.4.20 (34), con firma histórica, conserva backend anterior y está NO DISTRIBUIR. Android 77/77, backend 14/14 y paquete original 3/3 con mocks ya están acreditados; no repetirlos sin cambios. El propietario ya concedió Editor a GBO en la raíz actual y se verificó por metadatos. El auditor PreflightReadonly está en HEAD del editor original y localmente, pero su ejecución quedó bloqueada por consentimiento OAuth: NO existe inventario completo ni nueva versión/corte. Primero confirma que el usuario autoriza reanudar; luego resuelve el consentimiento de Apps Script y completa la lectura paginada, protegiendo los journals. No crear servidores/apps paralelos, cambiar raíz, tocar Usuarios, instalar tablets ni activar despliegues sin las autorizaciones correspondientes. La tablet prevista es la de Julián por USB, aún sin inspección ni respaldo verificado por Codex. No mostrar secretos ni subir privados. No publicar la rama local de respaldo privado. Conserva el trabajo y actualiza estados con evidencia real.
