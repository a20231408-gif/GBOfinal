# GBO Inventario Vial — punto de control

Fecha: 2026-10-11 (America/Lima)

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

- `buildSrc/src` **no está registrado en Git**. Está ignorado por `.gitignore` mediante `**/build/`; el archivo observado es `buildSrc/src/main/java/com/gbo/build/ValidateReleaseConfigurationTask.java`.
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
