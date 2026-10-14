# Matriz de observaciones 10I

Base: 3b3a698 (10H). Rama: codex/observaciones-campo-manual-10I.

Corregido en código no equivale a aceptación física del propietario. No se escriben fichas reales ni se ejecuta una nueva automatización completa de SCAP.

| Observación | Causa comprobada | Cambio o revisión | Prueba y estado | Dependencia |
| --- | --- | --- | --- | --- |
| Daño rígido ocupa toda la losa | SLABS dibujaba la receta sobre toda la celda, ignorando localización | Coordenadas existentes de Room, marcas puntuales/lineales/zonas según código; editor y renderizador PDF compartido | Corregido en código; seis casos focalizados cubren posiciones, tipos, edición, reapertura, dibujo nativo, XLSX y renderizador de páginas PDF | Aceptación física pendiente; contenedor PdfDocument no implementado en este Robolectric |
| Ahuellamiento solicita ancho de fisura | Campo visible para todo daño no GLOBAL | Visibilidad/validación según ancho_fisura_aplicable del catálogo; profundidad media para código 15; valores históricos ocultos se conservan | Corregido en código; comparación de cada entrada del catálogo y cantidad en m² sin modificar PCI/URCI | Validar pantalla en campo |
| Foto de daño parece pedir video | La versión actual usa FieldTakePicture, botones de foto y asociación por damageId, corregidos en 10C/10D | No se reescribe la funcionalidad ni se añaden videos | No reproducido en versión actual: nueva prueba de UI/callback UUID e Intent ACTION_IMAGE_CAPTURE aprobada; nueva persistencia offline en comprobación | Rotación/cámara física pendientes; conservar evidencias 10C/10D |
| Observaciones no aparecen en Excel SIC | Campo persiste en registro/metadata/HTML pero no se proyecta a columnas XLSX | Última columna OBSERVACIONES en formularios principales; valores literales, celda texto; conciliación aditiva de plantillas V3/V4 | Corregido en código: tres pruebas Android de XLSX, varias filas, vacío, texto tipo fórmula y persistencia; ocho Node de consolidación/UUID/ACK/compatibilidad | Requiere actualización manual y validación visual contractual antes de distribuir |
| Un ingeniero no puede crear usuarios | No asumir límite ni privilegios | Etapa C | Diagnóstico pendiente | Backend Usuarios publicado no incluido en el repositorio |
| Manual del servicio | Falta manual completo para personal de campo | Etapa D | En preparación | Capturas de entorno ficticio o diagramas identificados |

Room principal permanece en versión 19 y video en 1. No se cambian fórmulas, UUID, firma ni applicationId. Se conservaron seis archivos de la candidata 10H con SHA-256 en output/respaldo-10H-1.4.3, fuera de Git. La última Release distribuida sigue siendo 16; 17 / 1.4.3 se reutiliza con autorización del propietario.
