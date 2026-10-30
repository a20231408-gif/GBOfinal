# Candidata Release GBO Ruta Clara 1.4.13 (27)

**Compilación: PASS. GO para validación física offline. NO-GO para distribución hasta validar en tablet y completar compatibilidad del servidor.**

- APK: `D:\Downloads\GBO_PRUEBAS_8B\output\GBO_RUTA_CLARA_1.4.13_27_RELEASE_CANDIDATA\GBO_Ruta_Clara_1.4.13_27_RELEASE_CANDIDATA.apk`
- applicationId: `com.tuempresa.inventariovial`; versionName: `1.4.13`; versionCode: `27`.
- SHA-256 APK: `e76ce0076af96396d129670a5de118de0b56c603c2dbba73942810952e1dd4bd`.
- Firma Release v2/v3 verificada con Android Build Tools, mismo certificado que las APK históricas 1.4.9 (23) y 1.4.12 (26): `0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315`.
- APK completo, no depurable, sin instrumentación ni splits; alineación ZIP y páginas de 16 KiB verificadas. Tamaño: 26437813 bytes.
- Gradle ordinario `:app:assembleRelease`, configuración legítima, sin `gboValidationBuild`. Verificados `validateReleaseConfiguration`, `lintVitalRelease` y `verifyReleaseIdentity`. Duración: 611.47 segundos.
- 67 pruebas Android focalizadas y 46 Node aprobadas. JVM/Robolectric/Compose/Room y backend simulado; no son pruebas físicas ni escrituras Google. No se repitieron las auditorías anteriores.
- Se conservaron los 15 campos efectivos de configuración compilada, cinco archivos privados y catorce APK anteriores. Room 20, applicationId, firma, progresivas SCAP→SIC y servicios conservados. Solo dos líneas de Gradle cambiaron para la numeración autorizada.

## Capacidad actual y límite operativo

La consulta GET real, sin token ni escritura, confirmó fotos V2 y videos SCAP V1 con nombres V2. **Falta `scapPhotoPanelVersion=2`.** Las categorías oficiales nuevas no se envían al backend actual: fotos en `PENDING_VALIDATION` y videos en `BACKEND_REQUIRED`, con mensaje explícito y originales/cola conservados. No se inventa un ACK. Los flujos históricos siguen sus categorías y nombres anteriores.

Para habilitar Drive completo se necesita revisar y probar los cuatro `.gs` de `backend-parche-NO-PUBLICADO/` y el fragmento mínimo de capacidades para Codigo.gs. No se sustituyó ni publicó ningún módulo productivo. Instrucciones de paridad, prueba coordinada y reversión: `ENTREGA_Y_ACTIVACION.md`. La APK puede capturar y exportar localmente mientras espera esa capacidad; esa espera es una limitación real de la candidata.

Las pruebas locales verificaron lista 01–20, categorías persistidas tras cerrar/reabrir Room, imágenes y descripciones ordenadas en XLSX/ZIP, copias locales, nomenclatura de video, rechazo previo a POST incompatible e idempotencia simulada. Los videos de fixture usan bytes sintéticos: falta grabar, reproducir y subir un MP4 real. Los históricos ambiguos permanecen identificados sin asignación inventada ni renombrado remoto. Más de 32 fotos siguen en el anexo local existente; no se cambia el límite contractual remoto.

## Actualización manual y aceptación física

1. Conservar el respaldo disponible y usar una tablet de prueba con Release de firma compatible. La firma comprobada es la de los APK históricos; no se inspeccionó la instalación concreta de esta tablet. Si tiene Debug con otra firma, detenerse.
2. Copiar el APK y elegir **Actualizar**. Si Android rechaza la instalación, conservar la aplicación y sus datos; no desinstalar ni limpiar almacenamiento.
3. En modo avión, comprobar que abren los registros/configuración previos. Crear una ficha ficticia y verificar los veinte apartados; asociar fotos 01,03,08,15,20 y video 18.
4. Guardar borrador, cerrar y reabrir; comprobar UUID/asociaciones y originales. Abrir XLSX/ZIP, copia pública y video real para revisar orden, nombres y reproducción. Revisar también la progresiva SCAP→SIC y una ficha histórica.
5. Con el servidor actual, comprobar el aviso de backend pendiente para esas nuevas categorías, sin exigir una entrega confirmada. Toda escritura Google de prueba se autoriza y coordina por separado; tras actualizar el backend se comprobarán ACK padre/video y ausencia de duplicados.

Avisos de compilación no bloqueantes: APIs Android/Compose deprecadas y condición redundante en PavementDialogs. No se cambió código aprobado para silenciarlos. No se instaló ni distribuyó la APK, no se modificaron datos ni servidores y no se realizó commit/push.
