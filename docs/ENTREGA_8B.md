# Entrega Prompt 8B — integración real Drive V4

Fecha: 11 de octubre de 2026, Lima (ejecuciones también fechadas 12 de octubre en UTC). Rama: `codex/integracion-real-8B`. Base 8A publicada y verificada: `29ae399cb09273a8e3aa1fea694fea97038c3fd4`. Prompt 7 publicado y verificado: `28f9549813b0bf5753c70d4967970fd10e0f74af`. El commit de entrega es la punta publicada de esta rama; consultar `git rev-parse origin/codex/integracion-real-8B`. No se fusionó main ni se sobrescribieron 7/8A.

## Resultado y alcance

Android entregó registros SIC y fotografías al Apps Script existente y recibió ACK V4 reales. Se comprobaron edición, consolidación, reintento idempotente, conflicto concurrente y recuperación automática con la red desactivada y cierre real del proceso. No se detectó un defecto de producción que justificara cambiar fuentes Android o servidor: esta entrega añade dos pruebas Android y documentación verificable.

Se utilizó un emulador nuevo `GBO_8B_Controlado`, API 35, `emulator-5558`, con bases de prueba separadas. La Samsung SM-T733 conectada fue identificada por el usuario como secundaria de pruebas; no se instaló en ella porque su certificado instalado difiere del Debug local. Se conservaron sus datos. Los dos operadores son dos almacenes Room independientes contra el mismo servidor y token, no dos usuarios autenticados del servicio de usuarios ni dos tablets físicas.

Las imágenes son cinco JPEG auténticos generados en Android con rótulos PRUEBA 8B; prueban archivos, categorías, persistencia y transporte. No prueban cámara ni importación desde la interfaz. La prueba principal utiliza los repositorios y `AutomaticSyncDrain` existentes, con POST HTTP reales. El caso offline usa la base de la aplicación y el `AutomaticSyncWorker` existente, sin drenaje manual ni sincronizador adicional. Son entregas técnicas por PR contractual sin GPS observado: los ACK distinguen DELIVERED de georreferenciación PENDING_EXTERNAL; no se declara ubicación GPS verificada.

## Respuestas solicitadas

| Pregunta | Resultado demostrado |
|---|---|
| 1. ¿Android conecta al V4 publicado? | Sí: GET y POST reales, syncProtocol 3, pathVersion 4, categorías 1, ORIGINAL_CLEAN, familias estables y raíz autorizada. |
| 2. ¿SIC se entrega automáticamente? | Sí: los repositorios generan la cola; el caso offline quedó QUEUED y el worker existente confirmó al restablecer conexión. Otros casos ejecutan directamente el drenaje existente para controlar revisiones. La finalización mediante botones físicos queda pendiente. |
| 3. ¿Fotos llegan a carpetas correctas? | Sí: cuatro fotos ENTRADA/SALIDA separadas por alcantarilla, más una GENERAL offline. Cinco hashes remotos coinciden con ACK; píxeles y dimensiones coinciden con originales y el hash local permanece igual. La copia EXIF existente cambia bytes, no píxeles: no se afirma igualdad byte a byte. |
| 4. ¿Excel es real y consolidado? | Sí: se descargó y abrió el ZIP/OOXML remoto, SIC-18 con 13 columnas y dos filas; SIC-08 con dos filas. Hashes coinciden con últimos ACK. Se verificó separación por formato, no por segunda ruta. |
| 5. ¿Ediciones no duplican? | Sí: UUID, carpeta y archivo global se conservan. SIC-18 final mantiene A con condición 1 y B con condición 2; no queda la condición obsoleta 3. |
| 6. ¿Offline se recupera? | Sí: Wi-Fi/datos desactivados, guardado con foto, force-stop, reapertura todavía sin red, recuperación por WorkManager; misma revisión y una identidad remota. |
| 7. ¿Conflictos y ACK controlados? | Sí: pérdida controlada del ACK después de respuesta real y replay `alreadyExists`; BASE_REVISION_CONFLICT mantiene edición local 3 y remoto válido. Resolución manual y edición durante un POST en vuelo no se ejecutaron. |
| 8. ¿SCAP/pavimentos funcionan? | Exportación original SCAP y bundle con foto probados en Android (2 casos). PDF/XLSX de muestra de pavimentos probado localmente (1 caso). No se acreditan entrega remota de estos módulos, navegación física ni cálculo PCI con curvas completas. |
| 9. ¿Qué se ejecutó contra Drive? | Cuatro registros SIC ficticios, cinco fotos, dos formatos Excel, edición, dos bases independientes, retry, conflicto, offline/worker, rechazos de token ausente/incorrecto, hash y protocolo de staging incompatibles. Lectura independiente con el plugin Google Drive. |
| 10. ¿Qué impide producción segura? | Servicio de usuarios sin configuración local, revocación individual no verificada con token compartido, firma incompatible con la tablet conectada y pendientes físicos/módulos. El token histórico no estaba disponible para una prueba específica. No se distribuye una actualización de producción. |

## Validaciones e intentos

Siete casos distintos terminaron aprobados: autenticación real (1), secuencia SIC real (1), offline en dos fases (2), SCAP Android local (2), pavimentos local (1). Además, dos rechazos HTTP reales y lecturas independientes de archivos. No se repitieron suites 7/8A ni auditorías generales.

La primera compilación del harness falló por acceso a un helper interno desde androidTest; se corrigió con digest local. Hubo dos ejecuciones SIC fallidas antes de cualquier POST de entrega: el fixture guardaba dos veces detalle tipado SIC-18 con inserción ABORT en vez del borrador real. Se corrigió únicamente el fixture usando `SicFieldRepository` y un SIC-08 incompleto; la secuencia final aprobó en 427,372 s. Un intento inicial instaló un test APK anterior mientras se regeneraba, y el caso de auth sobrescribió el archivo común de evidencia: se separaron nombres de evidencia y se reinstaló. No se atribuyen estos fallos al servidor ni se ocultan como pruebas aprobadas.

Offline: creación 6,185 s y recuperación 103,809 s. SCAP Android: 2/2 en 11,136 s. Pavimentos: 1/1, sin fallos ni errores en XML. Los lectores de evidencia requirieron corregir expectativas del harness de lectura (`12.0` numérico y encabezado `Ruta` para SIC-08); los archivos remotos no se modificaron para hacerlo pasar.

## Compilaciones y protección

Debug conectado compilado con propiedades locales reales, sin `gboValidationBuild`; instalado únicamente en el emulador nuevo. `applicationId=com.tuempresa.inventariovial`, versionCode 16, versionName 1.4.2 y certificado Debug preexistente `b9fe89ed4ea039befde6a01c341559520e0739fe834f4222f4f185c5fc8db0ec` conservados. Room 18, keystore y Apps Script intactos.

| Artefacto local ignorado | SHA-256 | Uso |
|---|---|---|
| `output/8b-private/app-debug-connected.apk` | `14f067dd09a8e39b742e09b0ac72ed19a3fb6ed47fdac92e85b238a33f47e230` | Prueba conectada privada; contiene configuración local, no compartir. |
| `output/8b-private/app-debug-validation.apk` | `64a7e37cf81b59a2515d76443b5011e18f7b39d8aaec79102a4a479f99740d82` | Debug sin token, endpoint Drive ni URL de usuarios; compilación final aprobada. |

La APK final en `app/build/outputs/apk/debug/app-debug.apk` es la de validación. Su identidad, firma y alineación se verificaron. La APK conectada instalada en el emulador continúa privada; no se publicó ni se incorporó a Git. `drive.local.properties`, `server.local.properties`, APK, logs y bases crudas están ignorados. No se alteró ni publicó Apps Script, no se tocó el servicio de usuarios, no se borró contenido remoto y no se inició Prompt 9.

## Evidencia y conservación

[Matriz](MATRIZ_VALIDACION_8B.md), [procedimiento y pendientes](PRUEBAS_REALES_DRIVE_8B.md), [ACK Android](integracion-real-8b/evidencia/ANDROID_REAL.json), [offline automático](integracion-real-8b/evidencia/OFFLINE_WORKMANAGER_REAL.json), [lectura SIC-18/fotos](integracion-real-8b/evidencia/LECTURA_DRIVE.json), [lectura SIC-08/foto offline](integracion-real-8b/evidencia/LECTURA_OFFLINE_DRIVE.json), [estructura remota](integracion-real-8b/evidencia/ESTRUCTURA_DRIVE.json) y [registro de archivos](integracion-real-8b/ARCHIVOS_DRIVE.md). La raíz definitiva autorizada es [GBO Inventario Vial](https://drive.google.com/drive/folders/1FQBVRD3BK1rOBfC8NdRzPQt3fvU40V23).

Los archivos ficticios se conservan identificados por UUID y observaciones PRUEBA 8B. Las fichas Excel contractuales no incluyen columna de observaciones: reconocer sus filas por ruta/PR/distancia y el registro adjunto. El servidor creó además su registro, journals, bindings, agregados y STAGING; la lista observada está en la evidencia. No realizar limpiezas masivas ni eliminar archivos internos sin autorización específica.
