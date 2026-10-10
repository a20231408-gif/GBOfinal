# Diagnóstico y memoria

No se dispone de historial de salida de una tablet física en esta tarea. No se afirma que el problema observado fuera CRASH, ANR, LOW_MEMORY o timeout. El reporte pasa a registrar evidencia para la próxima reproducción.

En Android API 30+ se consulta **una** salida reciente de este paquete mediante ActivityManager.getHistoricalProcessExitReasons. Se conservan timestamp, reasonCode, reason y PSS/RSS **muestreados**. Se clasifican CRASH (Java/native), ANR, LOW_MEMORY, EXCESSIVE_RESOURCE_USAGE, USER_REQUESTED, USER_STOPPED, SIGNALED, OTHER; UNKNOWN indica «Causa no disponible.». USER_REQUESTED y USER_STOPPED se distinguen. No se leen descripción arbitraria del sistema ni trace de proceso. Referencias oficiales: [ApplicationExitInfo](https://developer.android.com/reference/android/app/ApplicationExitInfo) y [ActivityManager](https://developer.android.com/reference/android/app/ActivityManager#getHistoricalProcessExitReasons(java.lang.String,%20int,%20int)).

Android anterior o historial ausente: timestamp/version/trim/uncaught locales. Una uncaught real permite indicar CRASH con source=LOCAL_UNCAUGHT_HANDLER; ausencia de ese evento conserva causa desconocida. Handler registra y **delega al handler original**; no absorbe ni reinicia el proceso. Registros acotados a 64, stack máximo 24 frames sanitizados, clase de excepción, versión, Android, fabricante/modelo, módulo y memoria. Nunca Throwable.message/argumentos/request body/OS description/trace, PIN, tokens, hash, nombres de fichas, usuarios, fotos o coordenadas precisas. Pantalla DIAGNÓSTICO DE ESTABILIDAD y export FileProvider `gbo_diagnostics_YYYYMMDD.json`.

Riesgos de memoria encontrados por inspección de código, sin atribución causal al cierre físico:

- SCAP reunía ByteArray de **todas** las fotos JPEG a resolución original, además de workbook/ZIP. Ahora fotos originales JPEG y XLSX se transfieren al ZIP por flujo; imágenes de Excel se preparan individualmente con lado máximo 1600, conservando originales. PNG/WebP heredados se convierten individualmente para el contrato JPEG. Croquis limitados a 1200. Resolución reducida mantiene proporción/EXIF para Excel.
- Selector de 207 señales ya era lazy y cache 4 MiB; decodificación ahora muestrea a 256 para miniatura. Los 207 assets y sus hashes no cambian.
- Mapa usaba sample fijo 4, insuficiente para limitar cámaras de resolución alta: ahora vista previa usa miniatura de lado máximo 480.
- Miniaturas compartidas: LruCache 4 MiB, rechaza un elemento >1 MiB; keys por archivo/mtime/tamaño. Se libera cache en trim/low-memory, sin reciclar bitmaps aún mostrados ni tocar datos persistentes.
- Vista previa grande existente se solicita al abrir; pantallas lazy retiran referencias al salir. Mapas Canvas usan muestreo de geometría; imágenes KMZ son pequeñas y se reciclan. No se hicieron cambios de cálculo PCI/URCI ni se atribuyeron problemas a PDF/KMZ sin evidencia.

Pruebas nuevas: exclusión de mensajes secretos, bounded 64 eventos, fallback honesto, razón USER_REQUESTED/USER_STOPPED distinta, expiración scoped sin logout, JPEG original sin cambios y export de ambas plantillas. La reproducción 30–60 min y lectura real de ApplicationExitInfo están **pendientes en tablet**; no pueden reemplazarse por Robolectric.

Durante la validación local, una JVM de tests no pudo reservar memoria nativa en este PC. Se conserva el log de esa ejecución intermedia, se cerraron únicamente los procesos Gradle/Kotlin de esta tarea y se volvió a ejecutar con un solo worker, JVM 1 GiB y compilación en proceso. Ese fallo del host no es evidencia de una causa de cierres Android. No se incluyen hs_err/replay ni dumps completos del host en la entrega.
