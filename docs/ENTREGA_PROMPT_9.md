# Prompt 9 + 9B — continuación de campo

Rama: `codex/ajustes-campo-9`. Base publicada del Prompt 9: `37544481981be2bb27ef5153b2b9c0e0169e1127`. Se conserva la corrección aprobada `e2b3b5a`; no se reinició desde 8B.

El usuario comunicó que ambos cierres ya desaparecieron en su tablet. Esto es **aceptación manual comunicada**, sin ADB, Logcat ni prueba instrumentada ejecutada por Codex. Las mejoras 9B requieren las comprobaciones de `PRUEBAS_TABLET_9.md`. La falta de dispositivo no detuvo su implementación.

## Funciones Android

- Condición compartida: tarjetas táctiles de 64 dp, selección explícita y colores obtenidos del significado textual del catálogo, conservando códigos SIC-17…22. Se aplica también a la captura SIC desde SCAP. Las condiciones estadísticas SCAP 0…5 y la severidad de pavimentos conservan sus escalas propias.
- Fotografías: categorías CORE existentes y nombres exactos de las plantillas SCAP aparecen vacíos, con contadores. Captura/importación múltiple, consulta a pantalla completa, reemplazo y retiro conservan UUID y archivos originales. GENERAL histórico sigue visible. Mostrar categorías no escribe fotografías ni outbox.
- Listas: componente común de acciones para SIC, SCAP y pavimentos. Alcantarillas, señalización y puntos críticos incluyen borradores e históricos. El número local por UUID persiste; la identidad Drive se muestra por separado. Los campos contractuales permanecen intactos.
- Revisión/finalización: fajas pasan por resumen; ediciones históricas permiten borrador y confirmación validada. SCAP mantiene sus validaciones de cierre; pavimentos muestra cabecera y daños antes de confirmar. Abrir un resumen no solicita entrega. La confirmación válida usa WorkManager existente.
- GNSS/KML: importación asociada explícitamente a ruta, almacenamiento offline atómico/versionado, acumulación sobre líneas, sentido invertible, detección de cruces ambiguos, límites de precisión/distancia reales y corrección manual local. Se reutilizan ejes incluidos cuando no hay importación. Se puede capturar la posición actual del operador respecto al eje. El mapa muestra el eje importado. Los huecos entre segmentos no se suman como distancia recta y su orden sigue el archivo: debe revisarse.
- Ajustes: ayuda sobre 30 m al eje, precisión máxima de matching 15 m, recorrido 10 m, separación 3 m y aviso 1 m (valores actuales configurables mostrados en pantalla). Los dos botones de salida usan el color de error Material 3 y conservan sus verificaciones de seguridad.
- Videos: MP4/WebM/3GP, múltiples por categoría, importación y cámara externa Android, miniatura/reproductor, originales offline y Room independiente `field_video_9.db` v1. SHA-256 por streaming y máximo 32 MiB por archivo. Retiro conserva el original. La ficha debe finalizar y recibir ACK antes de enviar videos.

## Protocolos preparados, pendientes del servidor

`photoNamingVersion=1` incorpora `001_F_<hashUUID>.jpg` para nuevas identidades. Una asociación persistente evita cambios por reordenación o reintento. UUID/hash evita colisiones entre tablets; el prefijo es local y no promete un contador global entre operadores. Los archivos ya confirmados conservan su nombre. El servidor rechaza renombres históricos implícitos. El rotulado contractual externo está identificado como pendiente en metadata, sin inventar una base externa.

`videoProtocol=1` solicita a Apps Script una sesión de Drive y envía binario en bloques de 512 KiB, con consulta de offset/HTTP 308 y reinicio sobre el mismo fileId si caduca. El backend reserva el ID antes de crear el archivo, verifica propietario, carpeta, tamaño y SHA-256 calculado por Drive. El cliente exige ACK completo. Ningún video local se presenta como sincronizado.

Ambas ampliaciones están deshabilitadas por defecto en Android. Videos también exige `GBO_VIDEO_V1_ENABLED=true` en Apps Script y `videoProtocol=1`. El protocolo fotográfico existente continúa sin cambios mientras no se active el nuevo perfil. **No se desplegó Apps Script, no se enviaron videos reales ni se renombraron archivos remotos.** Ver `PENDIENTES_SERVIDOR_9.md`.

## Compatibilidad y entrega

La base de inventario sigue en Room 18. La base multimedia es adicional; no usa migración destructiva. No se modificaron encabezados/columnas contractuales Excel, applicationId, versionCode, keystore, usuarios, permisos administrativos ni la firma histórica. El orden local no entra en revisiones de fichas, numeración remota o rutas de entrega. `._GBO_SYNC_V3` conserva staging, bindings y journals existentes; el parche añade únicamente su registro de videos cuando el propietario lo despliegue.

El cierre original compiló Debug con `-PgboValidationBuild=true`: omite endpoints/credenciales de esta máquina y mantiene videos/prefijos deshabilitados. La continuación exclusiva de ordenamiento utiliza `-PgboValidationBuild=false` para cargar la configuración local real; ver `ordenamiento-9b/INFORME.md` para la APK vigente. No desinstalar la aplicación de campo ni borrar almacenamiento para instalarla: Debug y Release pueden tener certificados distintos.

El directorio preexistente ajeno `app/release/` se conserva y excluye de los commits. La evidencia histórica del primer Prompt 9 queda archivada en `campo-9b/ANTES_9B_*.md` y `campo-9/`; la evidencia 9B queda en `campo-9b/`.

Consulte la matriz completa de implementación y validación en `MATRIZ_VALIDACION_9.md`. El commit final se informa en la entrega y se obtiene con `git rev-parse HEAD`; el documento no incluye su propio hash.

## Cierre histórico solicitado por el usuario (antes de la continuación de ordenamiento)

Se detuvo la ampliación al recibir la instrucción de cierre inmediato. Se entregan cambios guardados y publicados, con aceptación física/remota pendiente. La tercera ejecución focalizada pasó 30/30; la última prueba en curso terminó con 5 aprobadas y 6 fallos de AssetOrderingTest. No hubo investigación, correcciones ni repetición posteriores. La matriz detalla cada fallo y las dependencias. No presentar esta APK como versión íntegramente aprobada para producción.

Backend que debe actualizar posteriormente el propietario: `google-apps-script/Code.gs`, `SyncV3Core.js` (como .gs), `VideoV1Core.js` (como VideoV1Core.gs) y `VideoV1.gs`; conservar `SyncV3.gs` actual. No se desplegó ninguno. Fotos numeradas y videos remotos permanecen desactivados.

Compilación Debug final: **BUILD SUCCESSFUL**, 56 segundos, `:app:assembleDebug -PgboValidationBuild=true --console=plain`. Log: `campo-9b/evidencia/compilacion-debug.log`.

APK: `app/build/outputs/apk/debug/app-debug.apk`, 40,190,144 bytes. SHA-256: `ebe8e6e2166522048aa04541f01653a72c2e7cd1b60a9bc97589bd1fecb2f00f`. `applicationId=com.tuempresa.inventariovial`, `versionCode=16`, `versionName=1.4.2`; conservados. Se generó únicamente Debug; no Release ni despliegue de servidor.

Instalación y prueba:
1. Copiar el APK Debug a la tablet de validación y abrirlo; permitir instalación desde esa fuente si Android lo solicita.
2. Si Android permite actualizar con su firma actual, instalar conservando datos. Si indica firma incompatible, **no desinstalar ni borrar almacenamiento de campo**: usar otra tablet/perfil de validación o coordinar la instalación compatible.
3. Ejecutar `PRUEBAS_TABLET_9.md`, empezando por listas, revisión/borrador, fotos históricas y videos offline. Esta compilación omite endpoints/credenciales y mantiene ambas ampliaciones remotas desactivadas.
4. Registrar los resultados. Estas instrucciones y hash corresponden al APK histórico de validación; los seis fallos se resolvieron en la continuación siguiente.

## Continuación autorizada: seis fallos de AssetOrderingTest

Se investigó y comprobó que las seis expectativas precedían al contrato Drive V4 del commit `99b15aa`. Se actualizaron exclusivamente esos casos y se reforzaron conservación de identidad/fotos, números confirmados, rutas y orden independiente. No se modificó código de producción ni Apps Script. Resultado: **40/40 pruebas focalizadas aprobadas, incluidas 8/8 de AssetOrderingTest**. No se afirma aceptación física/remota ni suite completa.

Configuración local real preparada mediante `drive.local.properties` y `server.local.properties`, ignorados y sin valores publicados. Debug conectado: `.\gradlew.bat :app:assembleDebug -PgboValidationBuild=false --console=plain`. El control de acceso sigue desactivado en la configuración local actual; fotos numeradas y videos remotos también permanecen desactivados. Firma y versionCode sin cambios, sin Release ni despliegue. Consulte `ordenamiento-9b/INFORME.md` para causas, evidencia, resultado de compilación e instalación del APK vigente.

