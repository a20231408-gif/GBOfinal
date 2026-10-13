# Cierre operativo GBO 10H

Base aprobada: e181dce552f49a6d82087c577222213afeb6fa5a (10F). Rama de entrega: `codex/cierre-operativo-10H`.

## Etapa 1 terminada y publicada

Commit `8e9d0d0c4dce23e97ac3bbfbe0633ba4fb29b1ff`, verificado igual en GitHub antes de pasar a Release. Reserva legible de videos nuevos SCAP, servidor como autoridad, categorías reales LOSA/MODULAR y ficha anterior, negociación independiente de Video V1, carpetas legibles y comprobación del nombre en Android. Se preservan UUID, mediaId/fileId, hashes, originales, históricos y mecanismo reanudable.

Se corrigió exclusivamente dentro del flujo de video el rechazo de etiquetas SCAP reales que incluyen `/`, como “Carretera / tablero”: la etiqueta es metadata, no una ruta; la identidad sigue siendo el código validado y la carpeta nueva se normaliza en el servidor.

**23/23 pruebas focalizadas aprobadas:** 18 Node de catálogo, consecutivos, bloqueo, interrupción antes/después del diario, rutas, formatos, ACK/hash, versiones anteriores y reversión; cinco Android de negociación, recibos malformados, conservación de etiqueta/identidad/original y pérdida de ACK final. No se repiten las baterías 8B–10F. La ejecución Android requirió compilar el conjunto de fuentes de pruebas, pero solo ejecutó los cinco métodos nuevos. Un intento inicial no arrancó por la ruta predeterminada de caché Gradle; se resolvió usando la caché existente, sin correcciones de la aplicación.

Código completo y procedimiento manual: NOMBRES_VIDEOS_10H.md y CAMBIOS_APPS_SCRIPT_10H.md. No se ha desplegado Apps Script ni se ha escrito en Drive de producción. GET real confirma V3/V4, fotografías V2 y videos SCAP V1; nombres de video V2 todavía no publicados. La raíz publicada difiere de Code.gs local: conservarla e incorporar únicamente el campo nuevo GET.

## Login y alcance preservado

El login de usuario/PIN existente se conserva y se considera aprobado para la primera entrega controlada. El PIN se valida contra la caché cifrada sincronizada por HTTPS desde el /exec de usuarios provisionado. ACCESS_CONTROL_ENABLED=false omite un gate adicional de dispositivo y **no desactiva ese login**. No se encontró un bypass concreto causado por esa bandera en el flujo revisado. No se modifica pantalla, usuarios, Apps Script de Usuarios ni flags; la autorización avanzada para anular se pospone. Alcance y límites de esta revisión en CHECKLIST_RELEASE_10H.md.

Un único módulo app conserva SIC, SCAP, Pavimentos, fotografías V2, videos y offline. No se cambian campos aprobados, orientación, cámara fotográfica, cálculos PCI/URCI, esquema Room ni numeración de activos.

## Candidata y pendientes

La clave histórica y el certificado fueron verificados por validateReleaseConfiguration antes de compilar. applicationId histórico conservado. El propietario confirmó última Release distribuida 16 / 1.4.2 y autorizó preparar 17 / 1.4.3. **Debug y Release compiladas correctamente** en una única ejecución ordinaria (10m 41s). validateReleaseConfiguration y verifyReleaseIdentity pasaron. La candidata mantiene firma histórica, no tiene DEBUGGABLE ni TEST_ONLY, y no incluye ComposeViewAdapter de Debug ni TestActivity de pruebas. Ambas APK contienen SIC, SCAP, Pavimentos, fotos V2 y cliente de videos/nombres; las dos tienen fotos V2=true, videos remotos=true y gate adicional=false. Inspección sanitizada en apk-debug.json y apk-release.json. No se publica la APK en Git.

La tablet Debug no se instala, desinstala ni modifica durante 10H. Su firma difiere de Release: no se ofrece una actualización Release encima de Debug. No se repite la automatización ADB del formulario. El auxiliar local no publicado tools/scap_ui_10g.py se conserva. app/release permanece fuera del commit; los artefactos de entrega inspeccionados se identifican por su ruta app/build/outputs y hash, sin borrar otros artefactos locales.

Falta la actualización manual del backend de nombres y la prueba física focalizada en una instalación compatible. Las pruebas físicas SIC/edición/giro/cámara/offline/sync/Pavimentos aprobadas por el propietario se aceptan sin repetición. Anulación remota, tombstones, restauración, reconstrucción Excel y renumeración posterior quedan pospuestas, conservando diseños y descarte de borradores locales.

Privacidad: cero coincidencias de valores privados en el árbol de texto revisado; configuraciones, contraseñas, keystore y APK fuera de Git. La comprobación no revisó todo el historial Git. Evidencias sanitizadas: campo-10h/evidencia.

## Artefactos locales y prueba de actualización

- Release candidata: `app/build/outputs/apk/release/app-release.apk`, 24.460.491 bytes. SHA-256: `1846315080bca71a989cba455089aae4d078c4303d19ca291f5ea6375c536f82`.
- Certificado Release SHA-256: `0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315`, idéntico al histórico.
- Debug ordinaria: `app/build/outputs/apk/debug/app-debug.apk`, 40.305.994 bytes. SHA-256: `5e2997481e2c730ce6198225886e72148d2361db612e8fe66030b4581c457bf3`.
- Certificado Debug SHA-256: `d2e54cb086aa731ef046be300977d0b16c0d8f66fdcb08d0d9cfd2038bf0d872`.

Para reconstruir desde Android Studio, abrir este proyecto, conservar archivos locales privados, seleccionar app/release y generar la APK con la clave histórica; también puede ejecutarse `gradlew.bat :app:assembleRelease` (el guard y auditoría de firma son parte del build existente). Debug se genera con app/debug o `gradlew.bat :app:assembleDebug`, sin flags de funcionalidades de prueba.

Instalar la Release solo como actualización de una instalación Release con certificado compatible, en la tablet elegida por el propietario; detenerse ante un rechazo, sin desinstalar. No instalarla sobre la Debug actual. Comparar identidad, fichas y pendientes antes/después, iniciar sesión con el usuario/PIN existente y ejecutar solo la prueba focalizada de videos ficticios después de actualizar manualmente el backend. La candidata está preparada; la actualización física y el cambio manual de servidor siguen pendientes.
