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

Primer bloque: 5 pruebas de sincronización y 2 de formularios aprobadas localmente; las 2 restantes de restauración se repiten tras corregir la navegación/espera del montaje de prueba. El código principal compila. Estado final de Debug: pendiente del cierre. VersionCode 16 / versión 1.4.2, mismo applicationId y firma Debug habitual. No Release, desinstalación, borrado de campo, renombrado histórico o despliegue.

Anulación remota: sigue deshabilitada hasta disponer de autoridad individual y motor lifecycle compatible. Ver PENDIENTES_BACKEND_10D. La comprobación física se detalla en PRUEBAS_TABLET_10D.

Observación del propietario: la ficha consultada ahora muestra «Drive: Sincronizado». No se recibió UUID/revisiones ni se consultó esa ficha desde esta ejecución; no permite atribuir una causa concreta al caso real anterior.
