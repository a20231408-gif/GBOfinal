# Prompt 7 — Flujo del operador

Base verificada: `codex/dashboard-sesion-6`, commit `c93aa5ad3c729285a56182b964f29cf3ad9b253a`. Trabajo en `codex/flujo-operador-7`. Fecha de entrega: 2026-10-11 (America/Lima).

## Resultado funcional

- Detalle único por UUID para fichas SIC, accesible después de finalizar, desde Mis registros e inventario ordenado. Título del elemento y progresivas visibles; UUID interno.
- VER FICHA consulta los campos del modelo contractual, catálogos y complementos SIC-17A/17B/18A; incluye observaciones, dimensiones complementarias y longitud técnica existente. SIC-08 y SIC-09 conservan identidad independiente.
- VER FOTOGRAFÍAS reutiliza miniaturas/ampliación del Prompt 5: categoría almacenada, contador, orden, GENERAL histórico, originales e historial. El filtro exige registro e inspección exactos.
- VER EXCEL SIC reutiliza SicExcelWriter para un XLSX local por SIC/ruta con todos los registros locales activos aplicables. Caché reutilizable sin conexión, escritura atómica, apertura y compartir mediante GeneratedFileIndex y URI con permiso de lectura. El archivo local nunca reemplaza el consolidado global.
- El enlace de Drive solo aparece cuando un ACK conserva el fileId del SIC correspondiente, su clave y revisión confirmada. Se muestra la revisión y se advierte sobre cambios pendientes. Consultar Drive requiere conexión/acceso; no se simula una descarga ni un visor de Excel.
- EDITAR reutiliza los editores técnicos y de ubicación. Regresa al mismo detalle, actualizado por Room, conservando comprobaciones de revisión y conflictos. Anular/restaurar permanece como herramienta secundaria.
- SCAP abre su inspección original y sus secciones existentes de ficha, panel fotográfico y exportación; no comparte fotos de otras inspecciones. Pavimentos abre la muestra original desde el inventario ordenado y conserva sus vistas, PCI/URCI, fotos y exportadores propios.

## Entrega automática

No se agregó sincronizador, cola, worker, endpoint ni cambio de Apps Script. Los guardados siguen pasando por los repositorios actuales: cierre válido programa entrega, borrador incompleto no programa entrega definitiva. Se conservan UUID, revisiones/baseRevision, conflictos, reintentos y ACK. La presentación exige ACK de la revisión actual antes de mostrar Sincronizado. Generar o compartir una copia local no confirma Drive ni inicia un mecanismo paralelo.

La consolidación global sigue en el backend existente. Las pruebas de dos operadores utilizan dobles locales; **no certifican Drive real**.

## Verificación

Ver [MATRIZ.md](MATRIZ.md) y [VALIDACION.json](VALIDACION.json). Solo se ejecutaron selecciones focalizadas. Los XML/logs crudos y resultados iniciales fallidos se conservan localmente en carpetas ignoradas y archivos `.log`.

Para este equipo, el JDK de Android Studio necesitó una ruta temporal corta para sus sockets internos. Configuración de proceso, sin cambio del proyecto:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:JAVA_TOOL_OPTIONS='-Djdk.net.unixdomain.tmpdir=C:\PROYECTOS\GBO_FINAL\GBOfinal\tmp'
.\gradlew.bat :app:testDebugUnitTest --tests '*OperatorFlow7*' :app:assembleDebug -PgboValidationBuild=true --no-daemon
```

Para el arnés Node existente se definió `GBO_AUDIT_PYTHON` con el Python instalado en este equipo; su valor por defecto apuntaba al equipo del desarrollador anterior. No se modificó el backend.

## Archivos

- `RecordDetailScreen.kt`, `RecordDetailPresentation.kt`: consulta, galería, Excel, estado y navegación por identidad.
- `RecordHistoryScreen.kt`, `MainActivity.kt`, `road/AssetOrderScreen.kt`: entradas al detalle y reutilización del editor de ubicación.
- `sic/SicFieldWorkspace.kt`, `field/CulvertUnifiedScreen.kt`: finalizar → detalle; retorno de edición sin apilar otra ficha.
- `data/dao/InventoryDao.kt`: observación de la instantánea existente; sin tablas ni migración.
- `export/LocalSicExport.kt`: copia contractual por SIC/ruta y caché local.
- `core/AutomaticSyncPanel.kt`: presentación de confirmación basada en ACK.
- `scap/ui/ScapWorkspace.kt`, `pavement/ui/PavementWorkspace.kt`: destinos existentes por inspección/muestra; accesos SCAP.
- `OperatorFlow7Test.kt`, `OperatorFlow7ScreenTest.kt`, fixture de presentación en `AutomaticSyncRegressionTest.kt`: pruebas focalizadas.
- Esta entrega, matriz, evidencia, capturas y `docs/CODEX_HANDOFF.md`.

Las rutas de código anteriores son relativas a `app/src/main/java/com/tuempresa/inventariovial/`; las pruebas a `app/src/test/java/com/tuempresa/inventariovial/`.

## Límites y pendientes

- Falta validación física en dispositivo secundario: apertura con aplicación XLSX real, compartir, cámara/importación, rotación real y accesibilidad. Las capturas son renders nativos de Compose/Robolectric con datos ficticios, no de tablet de campo.
- Falta prueba Drive real con despliegue aislado compatible. No se desplegó Apps Script, no se instaló APK y no se usaron credenciales privadas en la compilación de validación.
- Una regresión antigua `ScapPendingNavigationTest` falla al buscar un pendiente inexistente en su fixture (`NoSuchElementException`, antes de montar Compose). No se cambiaron los catálogos ni las reglas SCAP para hacerla pasar. Se conserva ese resultado y se verifica aparte el nuevo acceso por inspección con `OperatorFlow7ScreenTest`.
- Compilación Debug de validación; no es una entrega Release. Configuración de firma, applicationId, versionCode 16, Room 18/photo_history, syncProtocol 3/pathVersion 4/photoCategoryVersion 1 permanecen sin cambios.

No se inició auditoría final, no se fusionó main y no se sobrescribió la rama del Prompt 6.

## Resultado final de pruebas y compilación

29 pruebas Android distintas aprobadas (9 nuevas: 5 de datos/XLSX/estados y 4 de interfaz; 20 regresiones focalizadas) y 8 pruebas backend locales aprobadas. La prueba antigua de pendientes SCAP descrita arriba permanece fallida: 30 casos Android distintos ejecutados en total, sin presentar ese fallo como aprobado. Los intentos intermedios están desglosados en VALIDACION.json.

`:app:assembleDebug -PgboValidationBuild=true` aprobado. APK local excluido de Git: SHA-256 `4ec97a900def4ef428873ae7385ce3cc659a33034084c561af3ef7d07cb6afc9`. Manifest comprobado: `com.tuempresa.inventariovial`, versionCode 16, versionName 1.4.2.

**Firma de validación:** el certificado Debug de este equipo es `b9fe89ed4ea039befde6a01c341559520e0739fe834f4222f4f185c5fc8db0ec`, diferente del Debug histórico registrado en Prompt 6. No se cambió la configuración de firma ni se creó una firma de Release. No se acredita equivalencia de certificados entre equipos; este APK no se instaló ni se entrega como actualización de campo.

Capturas verificadas visualmente: [vertical](screenshots/01-detalle-vertical-datos-ficticios.png) y [horizontal](screenshots/02-detalle-horizontal-datos-ficticios.png), generadas por la interfaz real con el tema de la aplicación y datos ficticios.
