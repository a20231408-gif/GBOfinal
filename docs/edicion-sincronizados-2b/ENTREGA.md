# Entrega Prompt 2B — edición de registros sincronizados

11/10/2026. Rama local `codex/edicion-sincronizados-2b`, desde el checkpoint 2A `99b15aae88b2e69cb28a4715c87acb854fe6e12b`. Hash del checkpoint 2B consultable con `git log -1`. Sin publicación, merge ni cambios en la copia operativa.

## Comportamiento implementado

El editor real de alcantarillas y el editor técnico general permiten guardar registros con coordenadas ausentes. El UUID permanece igual y el guardado incrementa revisión, conserva el último recibo/base confirmada y programa la cola V3 existente: Actualización pendiente. El Excel se marca pendiente hasta un ACK válido. Los borradores no se envían.

Se conserva la revisión abierta de formularios y borradores de alcantarilla; un formulario local obsoleto se rechaza sin sobrescribir los datos nuevos. Los campos de progresiva ya escritos no se reinician por un ACK de fondo. SIC-08/09 y su editor también verifican la revisión abierta. Si una respuesta llega después de otra edición, confirma sólo la base remota y deja pendiente el contenido nuevo. Si se perdió la respuesta, la revisión preparada anterior se reintenta exactamente antes de enviar la nueva. No se reajusta la base usando la revisión de otro operador.

Apps Script reutiliza ScriptLock y el registro V3 para actualizar por UUID/fileId. La consolidación parte de todas las filas almacenadas del servidor, nunca del subconjunto de una tablet. Se refuerza la prohibición de usar identityId para reemplazar otra ficha SIC. Conflictos de revisión/base o revisiones obsoletas informan remoteRevision, conservan la edición local en CONFLICT y no se reenvían automáticamente. Nuevos guardados no borran el conflicto. La conciliación necesita revisión humana; no se añadió un sistema paralelo ni una interfaz de descarga/mezcla automática.

Las fotos intactas no se reescriben. Cambios de categoría mantienen UUID/fileId; fotografías añadidas usan UUID propio. Reemplazos de bytes actualizan el mismo fileId y respaldan la versión anterior. Archivos retirados de la entrega se archivan en REVISIONES, sin borrar originales locales ni archivos ajenos. Metadata y Excel se actualizan con respaldo/journal y rollback si una escritura falla. El cliente rechaza ACKs que cambien folderId/ruta o fileIds ya confirmados.

Cambiar PR actualiza orden técnico de inventario, Excel y mapa/KMZ, manteniendo el número y folderId. Se conserva la jerarquía 2A sin SIB: syncProtocol 3, pathVersion 4. La entrega técnica y la georreferenciación externa siguen separadas; no se fabrican coordenadas.

## Evidencia aprobada

- 17 pruebas Android: 10 nuevas y 7 de regresión sin GPS. Cubren condición/PR, ACK tardío, respuesta perdida más edición, conflicto persistente, fotografías/originales, formulario obsoleto, rechazo remoto mientras se guarda otra revisión, ACK incompleto, ACK con identidad alterada y reapertura de base de datos.
- 8 pruebas backend sobre las fuentes Apps Script reales con dobles locales de Drive. Comparan las filas completas del Excel generado por Android después de editar, incluyendo la ficha del otro ingeniero; IDs estables, categorías, reemplazo/archivo de fotos, conflictos entre operadores, rollback y reintentos sin duplicados.
- Debug compilado con la configuración privada real de pruebas, sin gboValidationBuild=true. VersionCode 16/applicationId/firma Release sin cambios; configuración de usuarios sin cambios. APK no instalada.

[VALIDACION.json](VALIDACION.json) registra SHA-256, resultados y límites. examples/ contiene payloads ficticios de Android, recibos LOCALES SIMULADOS y SIC-18-after-edit-local.xlsx; esos IDs no representan archivos reales de Drive. Los logs iniciales fallidos y los finales aprobados se conservan fuera del commit. La evidencia 2A permanece intacta. No se ejecutó la suite completa.

| Caso pedido | Evidencia local | Estado remoto |
|---|---|---|
| A/B/C: crear y corregir condición | initial-a/condition-a, Android + comparación de filas Excel backend | Pendiente |
| D/E: editar PR sin cambiar identidad | progressive-a, orden numérico y mismos folderId/fileIds | Pendiente |
| F/G: conservar segunda alcantarilla | initial-b y Excel con ambas filas completas | Pendiente |
| H/I: dos operadores/conflicto | versión base/collision/stale + edición local persistente | Pendiente |
| J: reintentos | mismos IDs sin nuevas carpetas/archivos; respuesta perdida | Pendiente |
| K: fotografías | categoría, alta, reemplazo y retirada con archivo de originales | Pendiente |

## Backend actual y pendientes

El GET real confirmó la raíz aislada autorizada, pero todavía syncProtocol 3/pathVersion 3 y sin stableFamilyNumbering. Por eso no se intentó actualizar ningún registro remoto. No declarar éxito remoto a partir de estas pruebas locales.

1. Publicar por el responsable exclusivamente los tres archivos de deployment-test/ en el proyecto Apps Script de pruebas independiente; no producción. Mantener rootFolderId 1FH56ZlgRRZzIrfMpL35EbjyX5HsZuzyk y el token como propiedad privada. [Pasos exactos, instalación secundaria y checklist](DESPLIEGUE_Y_PRUEBA_REAL.md); [diferencias backend](BACKEND_CHANGES.patch).
2. Confirmar GET compatible, autenticación y pruebas físicas de A–K con UUID nuevos; descargar fotos/Excel y comprobar IDs/hash. No instalar sobre la tablet de campo.
3. Conflictos detectados requieren conciliación manual con respaldo local; no hay resolución automática ni importador nuevo. Movimientos de ruta/familia y migración SIB histórica conservan las restricciones de 2A y requieren autorización/manifiesto.
4. No se subió esta rama. Fuentes y documentación sintética se protegen mediante checkpoint local; logs, APK/configuración privada y los archivos preexistentes de SCAP/proguard/herramientas siguen separados.
