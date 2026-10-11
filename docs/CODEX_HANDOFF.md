# Estado vigente — Prompt 2B, 2026-10-11

Rama `codex/edicion-sincronizados-2b` en copia aislada backend-v3-pruebas, desde 2A `99b15aae88b2e69cb28a4715c87acb854fe6e12b`. Checkpoint funcional consultable con `git log -1`. Sin push ni merge; copia operativa y modificaciones anteriores preservadas.

Edición sincronizada implementada sobre la misma cola V3: UUID/folderId/fileIds estables, revisión/base confirmada, actualización pendiente automática sin GPS, protección de formularios/borradores obsoletos, ACK tardío/respuesta perdida, conflictos explícitos sin sobrescritura ni reintento forzado. Excel se consolida desde el registro completo del servidor. Foto recategorizada conserva fileId; altas, reemplazos y retiradas conservan originales/historial. Sin SIB nuevo ni cambio de numeración por PR. syncProtocol 3/pathVersion 4. Estados de entrega técnica y georreferenciación externa separados.

Aprobadas: 17 Android (10 de 2B + 7 sin GPS), 8 backend locales y assembleDebug. Sin suite completa; resultados iniciales fallidos y finales conservados. Evidencia y SHA de APK: [VALIDACION.json](edicion-sincronizados-2b/VALIDACION.json); [informe](edicion-sincronizados-2b/ENTREGA.md). Los formularios conservan la revisión abierta durante ACKs de fondo; la verificación física de esa interacción sigue pendiente. El caso final comprueba que un rechazo remoto durante otro guardado mantiene la edición más reciente en CONFLICT.

GET remoto consultado: raíz aislada `1FH56ZlgRRZzIrfMpL35EbjyX5HsZuzyk`, syncProtocol=3/pathVersion=3, sin stableFamilyNumbering. No hubo escrituras remotas 2B. Pendiente que el responsable despliegue deployment-test/ SOLO en proyecto de pruebas y ejecute el checklist real; hasta protocolo de carpetas compatible la cola permanece UNCONFIGURED. Nunca tocar producción, servicio de usuarios ni tablet de campo. Firma/applicationId/versionCode 16 sin cambios.

Conflictos quedan en CONFLICT con edición local conservada, base antigua y versión remota informada. Reintento deshabilitado y nuevos guardados no borran conflicto. La conciliación requiere respaldo y revisión humana: no se implementó merge automático/importador remoto. Migración histórica y cambio de ruta/familia no se ejecutan automáticamente. No comenzar otra tarea sin instrucciones.

El checkpoint excluye credenciales, propiedades locales, keystores, APK, caches, logs y temporales. Archivos preexistentes de SCAP/proguard/herramientas/resultados V3 y logs 2A/2B se mantienen sin commit, separados de la entrega. Configuración del servicio de usuarios idéntica a la copia operativa.

---

## Historial previo; prevalece el estado vigente de arriba

# Estado vigente — Prompt 2A, 2026-10-11

Rama: `codex/drive-familias-2a` en worktree aislado backend-v3-pruebas. Base: `2bac1be62c29cd64c15f54b853912b5306f58d26`; hash de checkpoint funcional consultable con `git log -1`. No se subió esta rama ni se mezcló con main. Copia operativa y cambios anteriores conservados.

Implementación local completa: nuevas rutas por familia sin SIB, UUID/número remoto durable bajo ScriptLock, categorías existentes con N fotos, Excel por ruta/SIC en EXCEL_SIC, SIC-08/09 independientes, paquetes SCAP individuales, pavimentos y familias secundarias incluidos en orden/mapa/KMZ. Orden numérico separado de identidad y GPS; sin geometría no se inventa un punto. syncProtocol 3, pathVersion 4. Historial SIB y columnas Room intactos; migración preparada, no ejecutada.

Aprobadas: 15 Android (8 nuevas + 7 sin GPS) y 15 backend locales; assembleDebug con configuración real de pruebas, sin flag validation. 39 Android/60 backend del Prompt 1 son resultados históricos reportados, no repetidos. Logs iniciales fallidos y resultados finales conservados localmente. Ver [entrega y pendientes](drive-familias-2a/ENTREGA.md) y [VALIDACION.json](drive-familias-2a/VALIDACION.json).

Actualización del estado remoto histórico: Prompt 1 sí comprobó una subida real SIC-08 sin GPS, fotos y Excel con descarga/IDs y reintento estable bajo SIB-02. Raíz aislada verificada: `1FH56ZlgRRZzIrfMpL35EbjyX5HsZuzyk` (GBO_TEST_V3_20261011). No confundir con el endpoint operativo antiguo ni con PRUEBA_FINAL. Esa verificación no acredita el esquema 4. Backend de pruebas pendiente de desplegar el paquete compatible pathVersion 4; Android conserva cola UNCONFIGURED hasta entonces. Ninguna escritura remota o despliegue en 2A.

Pendientes: publicación por responsable exclusivamente en proyecto de pruebas, GET/capacidades y diagnóstico seguro del token, prueba real mínima de nuevas familias/IDs/Excel/reintento y prueba física secundaria. Restricciones: no producción, no servicio de usuarios, no APK sobre tablet de campo, no firma/applicationId/versionCode cambiados, no migración ni borrado automático, no suite completa. Siguiente tarea funcional: Prompt 2B edición segura de sincronizados, todavía NO iniciado.

Sin commit se conservan los archivos preexistentes de proguard, SCAP templates/maps y herramientas/resultados V3, más logs locales 2A. No incluirlos en una entrega 2A por atribución automática. Credenciales/configuraciones privadas y APK/cachés excluidos.

---

## Historial de puntos de control anteriores

Los estados siguientes son históricos; para decisiones actuales prevalece el estado vigente de arriba.

# GBO Inventario Vial — punto de control

Fecha: 2026-10-11 (America/Lima)

## Continuación: entrega técnica sin GPS

La rama activa es ahora `codex/sincronizacion-sin-gps`, creada desde el checkpoint `85d0659`. El checkpoint anterior (`24e1830`, `d4e491d`, `85d0659`) conserva los cambios SIC-08/SIC-09 y el avance mezclado previo; no se descartó contenido.

La tarea posterior permite entrega técnica sin coordenadas, recupera exclusivamente bloqueos GPS antiguos, excluye borradores y confirma recibos/IDs antes de sincronizar. Validación final: 39 pruebas Android focalizadas, assembleDebug y 60 pruebas locales del backend aprobadas. Las ejecuciones inicialmente fallidas también se conservaron. No se ejecutó la suite completa ni se volvió a validar físicamente la tablet.

Commit de entrega Prompt 1: **`ae58e7346e1cef5dd0ce5e61a3adc7dc4fa108bd`**, en la rama **`codex/sincronizacion-sin-gps`**, con asunto `feat(sync): entrega tecnica automatica sin GPS y pruebas del Prompt 1`. Incluye 29 archivos revisados: AutomaticDelivery/AutomaticSync, una línea de estado en el detalle existente, InventoryDao, SicFieldRepository, pruebas focalizadas, documentación, fixtures sintéticas y las fuentes completas del backend Drive necesarias para V3. El backend contenía implementación previa no registrada: se incorpora como base requerida, sin atribuir todas sus funcionalidades al Prompt 1. Los demás archivos anteriores se conservan fuera del commit. `buildSrc/src` sigue registrado en Git; configuración privada, applicationId y firma histórica sin cambios.

Se conservan los resultados reportados de 39 pruebas Android y 60 pruebas backend; no se volvieron a ejecutar en esta preparación Git. La evidencia resumida sin datos privados se incluye en `sync-sin-gps-2026-10-11/VALIDACION.json`; APKs, caches, logs crudos y temporales permanecen locales. Las fixtures incluidas son sintéticas, con coordenadas nulas y fotografías generadas por las pruebas.

Siguiente tarea: **edición segura de registros sincronizados** (Prompt 2). No se inicia en esta preparación. El responsable autorizó `origin=https://github.com/a20231408-gif/GBOfinal.git`. La entrega se publica exclusivamente en `codex/sincronizacion-sin-gps`, sin merge a main ni force push. La rama no existía en origin antes de la publicación; main tenía el hash `c42bb8158da6f24ff9cedbee7e4858fe104d1e31`. Comprobar que `git ls-remote origin refs/heads/codex/sincronizacion-sin-gps` coincide con el HEAD local y que main conserva ese hash.

Antes del commit se comprobó `git diff --cached --check` sin errores y una lista explícita de 29 archivos, sin propiedades locales, keystores, APK, caches, logs crudos ni temporales. Se revisaron 636 archivos del índice y 709 blobs del índice/historial, comparando patrones de secretos y los valores secretos locales sin exponerlos: cero coincidencias. Se revisó también contenido XML de las fixtures Excel y archivos embebidos en payloads. Los tokens del código entregado son marcadores o valores ficticios de pruebas; no se incluyeron credenciales reales. Un commit documental posterior registra este hash de entrega, sin modificar funcionalidades.

El endpoint local observado responde v6.2 sin protocolo 3. PRUEBA_FINAL existe, pero su separación de la raíz productiva no está confirmada. No hubo subidas ni despliegues. La prueba remota queda pendiente de carpeta/proyecto/URL aislados y autorización del responsable para crearlos/publicarlos. No cambiar servicio de usuarios.

Informe detallado, resultados, paquete y pasos para tablet: [entrega sin GPS](sync-sin-gps-2026-10-11/ENTREGA.md) y [despliegue de pruebas](sync-sin-gps-2026-10-11/DESPLIEGUE_PRUEBAS.md). Estado histórico del checkpoint a continuación; sus restricciones de no modificar funcionalidades correspondían al punto de control anterior, antes de que se autorizara esta tarea.

## Estado protegido

- Rama creada para este punto de recuperación: `codex/punto-control-2026-10-11`.
- Rama de trabajo anterior: `codex/sic08-sic09-captura-independiente`.
- Base común observada: commit `66610e8` (`Baseline local: Release 15 y correcciones previas`).
- No había cambios staged al iniciar este control. No se hizo `reset`, `clean`, `add -f` ni se eliminaron archivos.
- `git diff --stat` al inicio: 67 archivos modificados, 400 inserciones y 1.445 eliminaciones. Hay además archivos nuevos y artefactos de trabajo que aparecen en `git status --untracked-files=all`.

La rama tendrá un commit de recuperación selectivo para el código fuente, pruebas y este handoff. El contenido mezclado y los artefactos que requieren revisión individual quedarán sin commit y se conservarán en el working tree.

## Cambios terminados

Según las pruebas focalizadas conservadas en `docs/sic08-sic09-fix-2026-10-11/` y la validación física informada por el equipo:

- SIC-08 y SIC-09 tienen pantallas, selección, validación, guardado, reapertura y edición independientes.
- Guardado local sin GPS, ubicación apagada, permisos de ubicación denegados y sin Internet.
- Observaciones multilínea opcionales, persistentes y editables.
- Coordenadas ausentes permanecen nulas; los datos GNSS, fotografías, trayectorias e identificadores históricos se conservan.
- Ruta y calzada se heredan de la sesión para fichas nuevas sin sobrescribir datos propios de fichas existentes.
- Exportaciones SIC-08/SIC-09 mantienen sus columnas contractuales; las observaciones se entregan mediante el complemento documentado y metadata.
- La migración Room 16→17 y las migraciones históricas seleccionadas pasaron en las pruebas focalizadas.
- `assembleDebug` terminó correctamente y el artefacto se conserva en `app/build/outputs/apk/debug/app-debug.apk`.

La validación física en tablet se registra aquí como resultado comunicado por el usuario; este control no la repitió.

## Cambios preexistentes o mezclados

No se atribuyen todos los cambios del working tree a SIC-08/SIC-09.

- Drive y entrega: `DriveStatusPanel.kt`, `DriveUploadWorker.kt`, `core/DriveAdapters.kt`, `core/DriveSyncCenter.kt`, `delivery/DeliverySync.kt`, `RouteDelivery.kt` y archivos relacionados.
- SCAP: catálogos en `app/src/main/assets/scap_*.json`, repositorio, mapeadores, validación, exportación, sincronización y UI bajo `app/src/main/java/com/tuempresa/inventariovial/scap/`.
- Pavimentos: archivos bajo `pavement/` y sus pruebas.
- SIC-08/SIC-09 y persistencia común: `sic/`, `RecordHistoryScreen.kt`, `data/database/InventoryDatabase.kt`, `data/entity/InventoryRecordEntity.kt`, `data/dao/InventoryDao.kt`, exportaciones, entrega y consumidores de coordenadas opcionales.
- Otros cambios existentes: autenticación, diagnóstico, repositorios generales, pruebas de navegación/flujo y documentos de iteraciones anteriores.

Los archivos nuevos principales de la corrección incluyen `sic/FajaCaptureForm.kt`, `core/AutomaticDelivery.kt`, `core/AutomaticSync*.kt`, `export/SicConsolidatedWorkbook.kt`, `RoomTestResources.kt`, `Sic0809CaptureTest.kt`, `Sic0809ScreenTest.kt` y los esquemas Room 16/17. El resto de archivos nuevos listados por Git incluye documentación, backend, herramientas, logs y artefactos de empaquetado preexistentes; deben revisarse por separado antes de preparar un commit.

## Pruebas aprobadas y pendientes

Las pruebas focalizadas conservadas fueron 14/14 aprobadas: 7 casos de datos SIC-08/SIC-09, 4 casos de pantalla y 3 pruebas de migración histórica. No se ejecutó la suite completa por instrucción del usuario.

Pendientes:

- No hay Release firmado generado en este punto de control.
- No se repitió la validación física, Drive real ni la suite completa.
- Debe separarse el contenido mezclado antes de cualquier commit funcional.
- Deben revisarse y clasificar los artefactos no fuente (`app/release/`, logs, `docs/`, `backend/`, `tools/`) antes de agregarlos.

## Git, buildSrc y configuración privada

- Al iniciar este control, `buildSrc/src` **no estaba registrado en Git** y su archivo estaba ignorado por `.gitignore` mediante `**/build/`. Se incorporó explícitamente al checkpoint como `buildSrc/src/main/java/com/gbo/build/ValidateReleaseConfigurationTask.java`, sin cambiar su contenido funcional.
- No había archivos staged. Por ello, la revisión de “preparado para Git” aplica al working tree candidato, no a un índice ya preparado.
- `drive.local.properties` y `server.local.properties` existen localmente y están ignorados por `.gitignore`. `keystore.properties` no apareció como archivo local; solo existe `keystore.properties.example`.
- No se encontraron archivos `.jks` ni `.keystore` en la búsqueda realizada.
- Los archivos de ejemplo contienen nombres de claves con valores de ejemplo vacíos o marcadores; no contienen credenciales reales según la inspección por nombres de clave y valores redactados.
- La búsqueda de patrones de tokens, contraseñas, claves privadas y configuraciones locales no encontró un secreto operativo en los archivos candidatos. Las coincidencias restantes son nombres de configuración, validaciones de prueba, documentación de ejemplo y código que lee `BuildConfig`; no se deben convertir en secretos al hacer stage.

## Restricciones para continuar

- Conservar esta rama y el working tree hasta clasificar cambios por tarea.
- No ejecutar suite completa ni modificar funcionalidades en este punto de control.
- No agregar credenciales, propiedades locales, keystores, APKs de Release ni artefactos generados sin revisión explícita.
- No cambiar la firma histórica ni `applicationId` (`com.tuempresa.inventariovial`).
- El último Release instalado usa versionCode 15; el código preparado usa versionCode 16. El siguiente versionCode esperado después de 16 es 17.
- No atribuir automáticamente los cambios de Drive, SCAP, pavimentos o iteraciones anteriores a SIC-08/SIC-09.

## Comprobación de seguridad

El estado queda listo para continuar con seguridad a nivel de recuperación Git: rama creada, commit selectivo de recuperación, cambios restantes conservados y handoff documentado. Antes de publicar o fusionar hay que separar tareas, revisar el diff de cada grupo y preparar commits selectivos sin archivos privados ni generados.
