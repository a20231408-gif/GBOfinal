# Entrega 10D — 13/10/2026

Base: 4640476d9390bff650ce672c61a425b66e29f8ca, publicada en codex/pavimentos-anulacion-10C. Continuación: codex/ajustes-finales-10D. Commits por etapas; comparar HEAD con el commit remoto de esa rama. No se despliega Apps Script ni se modifica configuración privada.

## Sincronización

Que existan fotos, metadata y Excel en Drive no acredita por sí solo el ACK final. El servidor V3 conserva recibo idempotente por key/revision/contentHash. Antes, cada reintento volvía a preparar todos los stage requests; además, la UI interpretaba UPLOADING de Room como actividad real, incluso cuando Android había detenido o diferido el worker.

El transporte conserva atómicamente las confirmaciones verificadas de staging, ligadas a endpoint, UUID, revisión, contentHash y SHA-256 del archivo. Reutiliza esos stageFileId en el mismo commit; no regenera los archivos preparados ni cambia UUID, binding o baseRevision. Si se pierde la respuesta de un stage, se repite únicamente ese stage idempotente. Si se pierde la respuesta final, se repite commitRecordV3 y se exige el mismo ACK completo. Excel inline forma parte de ese commit y puede retransmitirse; no se afirma que se haya añadido un endpoint de consulta inexistente.

La interfaz consulta WorkManager y exige worker RUNNING con la misma key/revision para mostrar actividad. Incluye fase, archivos preparados procesados/pendientes, porcentaje de archivos transferidos y tiempo desde el primer intento, incluyendo esperas. No es porcentaje de confirmación contractual. Sin muestras suficientes de velocidad se muestra Tiempo restante no disponible. Una cancelación conserva snapshot y queda pendiente; una edición durante envío solo utiliza el ACK anterior como base y mantiene pendiente la nueva revisión.

## Alta, edición y orientación

Los SIC antiguos comparten SicFormCore en alta y edición: ruta/calzada/sentido, PR/distancia y catálogo de lado. Alcantarillas y vertical no muestran PR final ni sugerencias GNSS. Los componentes Sic17Fields–Sic23Fields siguen proporcionando el formulario técnico y sus catálogos; los formatos de SicFieldWorkspace ya reutilizan un mismo esquema para crear/editar. Puentes SCAP mantiene su workspace propio.

La entrada EDITAR del detalle abre la ficha técnica completa y deja de apilar el antiguo editor parcial de ubicación. Los valores históricos ocultos se conservan. La escritura valida campos visibles y mantiene UUID y revisión previa como guardia contra edición concurrente.

La ruta activa, tipo de elemento, borrador, campos crudos, categorías, destinos de cámara/galería, coordenadas auxiliares y diálogos se conservan mediante rememberSaveable. Los estados de formulario/metadata se pueden serializar en SavedInstanceState; no incluyen PIN/tokens ni bitmaps. La carga de Room/JSON no sobrescribe un formulario ya restaurado. Los borradores guardados por el editor técnico se reconstruyen desde Room aunque no exista JSON de captura. Recrear el formulario no vuelve a guardar las mismas fotos por sí solo.

## Validación y límites

Resultados e intentos en MATRIZ_10D y campo-10d/evidencia. Las pruebas locales usan datos ficticios y respuestas simuladas; no equivalen a cámara física, proceso Android real, Huawei o Drive real. Los fallos iniciales de compilación y el intento interrumpido por presión de memoria se conservan. No se repiten baterías generales 8B/9/10.

Resultado local: **22 casos focalizados aprobados, sin fallos pendientes**, con todos los intentos fallidos conservados. Ambas APK Debug compiladas y verificadas: ordinaria y prueba V2. VersionCode 16 / versión 1.4.2, mismo applicationId y firma Debug habitual. No Release, desinstalación, borrado de campo, renombrado histórico o despliegue.

Anulación remota: sigue deshabilitada hasta disponer de autoridad individual y motor lifecycle compatible. Ver PENDIENTES_BACKEND_10D. La comprobación física se detalla en PRUEBAS_TABLET_10D.

Observación del propietario: la ficha consultada ahora muestra «Drive: Sincronizado». No se recibió UUID/revisiones ni se consultó esa ficha desde esta ejecución; no permite atribuir una causa concreta al caso real anterior.

Etapas publicadas: 1ddbafcc260929637a6bba5e9a0e4cb22b71b6a3 y bb10c4644d0481c87ca98f1fad45c092b7b5cdc9, ambas verificadas con git ls-remote. La segunda mantiene diálogos/campos de Pavimentos con estado serializable sin cambiar tablas, cálculos PCI/URCI o malla.

La vista previa Camera2 llenaba TextureView sin compensar la proporción del buffer: ahora aplica escala uniforme y compensación del display, incluyendo giro de 180 grados mediante DisplayListener. Se basa en la [guía oficial Camera2](https://developer.android.com/codelabs/android-camera2-preview); el resultado físico en Huawei sigue pendiente. El lector de fotos continúa respetando los ocho estados EXIF y usa ContentScale.Fit sin reescribir originales.

V2: compilación de prueba preparada con la opción Gradle -PgboPhotoNamesV2Test=true solo para Debug. Instrucciones en PRUEBAS_TABLET_10D. No se activa el flag en configuraciones privadas ni la capacidad publicada.

APK de pruebas V2: app/build/outputs/apk/debug/app-debug-10d-names-v2-test.apk, generada con configuración local, GBO_PHOTO_NAMES_V2_ENABLED=true y video remoto false. SHA-256 37f2b721e1056b4a19fe1f9569dfddc65f0f2484ba8a0eacbfc96ddb5e385b9a. Solo para ficha/dispositivo de prueba coordinados; servidor sigue anunciando capacidad V2 0 según el propietario. No se instaló ni se efectuó escritura real de Drive.

Fuente de compilación: f8f2904be01eb4beb7ed95701aa6027097ac165b (incluye las etapas críticas y el ajuste del entorno de prueba). La opción Debug true/false explícita fue publicada en 4bf32fae6704e83459c1a3b16e5be7ffc483a67a. El último commit de cierre añadirá resultados/documentación; no cambia el código compilado.

Los fallos de pruebas UI se resolvieron con navegación/espera/desplazamiento adecuados y WorkManager en la Application sintética. Se conserva el error de invocación Gradle V2 (opción --tests asociada a assembleDebug) junto a su ejecución corregida exitosa. No quedan casos focalizados fallidos; esto no acredita pruebas físicas de cámara/rotación/Drive en la Huawei.

APK para la primera comprobación en tablet: **app/build/outputs/apk/debug/app-debug-10d.apk** (copia de app-debug.apk). Compilación Debug ordinaria SUCCESS en 2 min 33 s; nombres V2 false, video remoto false. SHA-256 **a57857e2a4cd182b09db97dc7b3b02c39d286524153acc7fc5aff63c3b127318**; 40897573 bytes. Firma Debug verificada con certificado d2e54cb086aa731ef046be300977d0b16c0d8f66fdcb08d0d9cfd2038bf0d872. Configuración compilada: endpoint Drive, token Drive y endpoint de usuarios presentes; control de acceso conserva false según configuración local. No se exportan valores privados.

Antes de entregar a ingenieros: instalar como actualización únicamente sobre una firma compatible, conservar la instalación/datos si Android rechaza la firma y completar el protocolo Huawei/Drive ficticio de PRUEBAS_TABLET_10D. No hay pruebas físicas 10D realizadas, ni escritura/despliegue de Apps Script en esta ejecución. Anulación remota bloqueada por falta de autoridad efectiva; activación V2/video remotos pendientes de coordinación del propietario.
