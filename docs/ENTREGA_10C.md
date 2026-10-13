# Entrega 10C — 12/10/2026

Base publicada verificada: 0782319afb3d3866f7d418994767ba64d3920b4b, rama codex/correcciones-campo-10. Continuación: **codex/pavimentos-anulacion-10C**. HEAD de esa rama identifica el commit final de entrega; comprobar contra git ls-remote. No se rehacen los Prompts 8/9/10.

## Fotografías por daño

Causa: PavementDamageActions recibía cámara/importación pero no mostraba controles; el acceso dependía de seleccionar la fila y descubrir la galería inferior. La tabla ahora muestra **Tomar foto** y **Ver fotos (cantidad)** por UUID de daño. Ver fotos abre la galería exacta, con cámara, importación, miniaturas, preview, reemplazo y retiro. Está disponible dentro de la muestra y en el panel general Fotografías; ambas vistas observan las mismas filas de pavement_damage_photos, sin repositorio paralelo. Dos daños del mismo código se distinguen por damageId y sampleId.

El destino queda fijado antes de pedir permiso/abrir cámara o selector, y persiste en estado guardado durante la actividad externa; cambiar selección no reasigna la foto recibida. Cámara cancelada no registra ni retira nada. Reemplazar conserva el original anterior y usa UUID nuevo, confirmando nueva foto y retiro anterior en la misma transacción. Retirar conserva bytes, UUID y relación con daño; historial consultable offline. Los archivos completos se conservan también si falla o se cancela su vinculación; solo se limpia un temporal vacío. No se permiten sobrescrituras de una foto existente ni eliminar un daño con evidencia, incluida evidencia retirada.

Las importaciones conservan sus bytes y EXIF, aunque el archivo privado tenga extensión .jpg; DeliveryJpeg ya aprobado genera y comprueba la copia JPEG final. No se toca el comportamiento de fotos de SIC/SCAP ni se añaden videos. Se mantienen cálculos PCI/URCI, malla, severidad y catálogos. Room pasa de esquema 18 a **19**, agregando únicamente retiredAt y replacedByPhotoId, con migración sin eliminación de datos. El versionCode Android no cambia. La huella de snapshots y el orden previo de fotos de igual fecha se preservan para que los campos nuevos no rompan un reintento histórico.

La muestra abierta guarda las fotos offline y actualiza updatedAt; la cola existente se genera al Revisar y finalizar. Para corregir una muestra cerrada, usar Reabrir para corregir, añadir/retirar/reemplazar y volver a finalizar. La cola conserva ACK anterior/baseRevision y snapshots pendientes; no se confirma con una exportación local. El panel general muestra las fotos activas; las retiradas se consultan en Historial conservado. No se borra contenido remoto cuando se retira una foto; el servidor mantiene sus respaldos y referencias históricas.

## Borradores exclusivamente locales

Desde Mis registros/detalle SIC y tarjetas de muestras Pavimentos: **Descartar borrador local** con confirmación. Es reversible: cambia a DISCARDED_LOCAL y oculta del listado normal, sin borrar ficha, daños ni fotos. Mostrar anulados y descartados / Mostrar borradores descartados permite **Recuperar borrador local**. Conserva UUID y archivos y no crea cola ni hace HTTP.

La comprobación se repite en una transacción antes de cambiar estado: debe ser DRAFT, sin ninguna intención de entrega (también DRAFT reabierto, SYNCED, ERROR o ACK incierto), sin preparación conservada ni binding/recibo, sin foto subida/en proceso, sin referencia en manifiesto de entrega de ruta. Un manifiesto no legible bloquea el descarte. Un recorrido activo también lo bloquea. Pavimentos exige inspector creador. Para SIC histórico no existe propietario individual inequívoco en el esquema: la operación es solo local, reversible y auditada con el usuario disponible, nunca un permiso administrativo remoto. SCAP vinculado queda excluido del descarte local en esta entrega.

Se registra auditoría local con acción, fecha, UUID, tipo y huella de tablet; fotos originales conservadas. DISCARDED_LOCAL se excluye del antiguo envío de registros al servicio y de posiciones sobre eje. No confundir este descarte con anulación contractual ni eliminación de Drive.

## Anulación remota pendiente

La operación remota continúa deshabilitada: el servicio actual no acredita permiso individual de inventario y no se cuenta en el repositorio con su fuente autoritativa para integrar el verificador de forma segura. Se prepara InventoryAuthorization10C.js para el servicio de usuarios con emisión e introspección de concesión opaca, PIN verificado, permisos explícitos, ficha/ruta/tablet ligadas, vencimiento y revocación; siete pruebas locales de contrato aprobadas entre dos ejecuciones (6 + 1). No está conectado al router publicado ni se anuncia como capacidad activa.

**No están implementados aún** la cola de anulación, el tombstone/ACK transaccional, exclusión Excel, restauración remota ni renumeración continua después de anular. El diseño verificable y los pasos de integración están en PENDIENTES_BACKEND_10C. No se usa el token Drive ni TABLET_ADMIN como autorización. No se modificó ni desplegó Apps Script de producción o Servicio de Usuarios.

## Validación, APK y límites

Resultados y fallos conservados en MATRIZ_10C y campo-10c/evidencia: **18 casos Android distintos aprobados** acumulados en las ejecuciones focalizadas y **7 pruebas del contrato de autorización** (6 + 1). Pruebas de repositorio/Compose/transporte con backend simulado son locales: no sustituyen cámara física, reinicio de Android ni Drive real. No se realizaron escrituras remotas en 10C ni se alteraron tablets de campo.

APK Debug: **BUILD SUCCESSFUL** con `assembleDebug -PgboValidationBuild=false`; app/build/outputs/apk/debug/app-debug.apk, usando drive.local.properties/server.local.properties privados. Endpoints y token presentes, firma Debug verificada. Mantener banderas remotas de nombres V2/video desactivadas. No publicar APK o configuración en Git. Solo Debug, mismo applicationId, firma habitual y versionCode 16 / versión 1.4.2. Fuente Android: fc3854a33f29015c51e8ee5b919f9f45e5768f6d; el commit de documentación posterior no cambia la APK. SHA-256: eaea285a0793964b682f1d9a0d4955dfdad41c92d54d2960ba05c56cb1c236a7. Detalle en campo-10c/evidencia/apk-debug-10c.json; privacidad verificada en privacidad-10c.json.

Instalar solo en dispositivo de pruebas o como actualización con firma compatible. Copiar app-debug.apk a la tablet, abrirlo y autorizar al gestor de archivos para instalar si Android lo solicita. Por USB con ADB, usar `adb install -r app/build/outputs/apk/debug/app-debug.apk` desde la raíz del proyecto. Si la tablet tiene Release y el instalador rechaza Debug por firma, **no desinstalar ni borrar almacenamiento** para forzarlo. Guía de instalación general en PRUEBAS_TABLET_10.

Prueba inmediata: muestra ficticia → crear dos daños del mismo tipo → Tomar foto del primero y dos fotos adicionales → Ver fotos y comprobar cantidad/miniaturas → Fotografías general → segundo daño sin mezcla → cerrar app offline y volver → finalizar con conexión, esperar ACK. Repetir importación/EXIF y reemplazo/retiro comprobando historial. Probar descarte/recuperación solo sobre borrador ficticio nunca entregado. Para registros de campo, esperar autorización específica antes de escrituras.

Detalle de limitación del test de transporte: solo el contenedor PDF se simula por ausencia de handle PDF nativo en el JVM de Windows; la fotografía, conversión JPEG, hashes, Excel, preparación, cola y comprobación de ACK usan el código de aplicación. La confirmación viene de backend simulado, no Drive real. No se cambió el generador PDF de Android. La comprobación de documento PDF real requiere la prueba en tablet.
