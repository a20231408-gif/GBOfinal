# Eliminación controlada — propuesta por fases, 02/11/2026

Base `0071ddcb9489a06f0c33309a00c77d0b6efe7ccf`, rama `codex/eliminacion-controlada-20261102`.
PR #1–#4 integrados; PR #5 sigue abierto, SHA observado `65d0c794729bdbca73803eb29af1508c5ee7f002`,
sin fusionar ni incorporar en esta propuesta. No tablet, APK nueva, Google, red HTTP de GBO ni producción.

## Resultado operativo

| Operación | Disponible hoy / recuperación | Propuesta y límite |
|---|---|---|
| A: descartar | Borradores SIC/Pavimentos exclusivamente locales; restauración individual. Guarda atómica del lote, ACK/outbox/reservas/vínculos/archivos compartidos intactos. | Sin cambios; no equivale a borrar bytes o fichas. SCAP y fichas protegidas se rechazan. |
| B: liberar copias locales | ConfirmedLocalCleanup retira duplicados públicos propios tras ZIP verificado, revisión/ACK actual y controles de referencias compartidas. Conserva Room, originales y remoto. | Restaurar devuelve visibilidad; regenerar documentos es otra acción, no recupera automáticamente el URI. Aceptación física SAF pendiente; no se ejecutó limpieza real. |
| C: eliminar registro completo | **Bloqueada**. No hay contrato administrativo productivo verificado. | UI de inventario conocido/confirmación sin ejecutor. No borra Room, archivos ni referencias remotas. |
| D: pruebas seleccionadas | **Bloqueada** igual que C; no hay borrado global. | Revisión de seleccionados actualmente visibles y cantidad exacta, no de una carpeta o base completa. |

Implementado en fuente: RemovalPreview.kt, RemovalPreviewDialog.kt y accesos por ficha/selección
en UnifiedRecordsScreen.kt. Sin cambios de esquema, versión, sincronización, filtros o borrado existente.
Copias indexadas no significa inventario exhaustivo del almacenamiento; IDs del ACK no significan
permisos actuales ni propiedad exclusiva. Solo se cuentan IDs remotos distintos con ACK estricto de la
revisión actual; inválido/ausente muestra desconocido. Cambios de revisión se advierten. Los vínculos
conocidos se muestran conservadoramente; falta inventario servidor, vídeos, rutas legacy, permisos
SAF y consumidores desconocidos. Por ello el botón definitivo permanece SIEMPRE deshabilitado.
Escribir exactamente ELIMINAR y la segunda casilla administrativa para lotes son requisitos necesarios,
no autorizaciones: no crean journal, tombstone, outbox ni solicitud remota. No se elimina la única copia.

## Lectura del contrato existente

La fuente local google-apps-script/Code.gs autentica subida mediante token de aplicación compartido.
annulRecordV1/restoreRecordV1 responden INDIVIDUAL_INVENTORY_AUTH_REQUIRED. No acredita autorización
administrativa individual. AutomaticDelivery.validAck exige protocolo, clave, revisión, contentHash,
IDs/rutas/hashes y aportes SIC; la propuesta reutiliza este validador sin debilitarlo.
AutomaticSyncDao conserva outbox/receipt/prepared incluso históricos; LocalDraftDiscard los protege.
SyncV3 candidato usa LockService y guardas de raíz; PreflightReadonly propone inventario paginado
estable de journals/bindings/reservas. Esta lectura local NO acredita el estado efectivo de V21,
permisos o journals productivos. No se ejecutó ningún auditor Google ni se cambió código desplegado.

## Contrato administrativo propuesto, NO endpoint productivo

1. **Inventariar** por (tipo, UUID, revisión, raíz/binding existente) desde el servidor legítimo.
   Retornar un plan con operationId único, digest canónico, caducidad, actor, alcance y versiones
   exactas. Enumerar IDs de archivos/carpetas y propietarios/permisos efectivos, hashes, revisiones,
   padres, ACK, staging, reservas, journals y referencias cruzadas incluyendo otras tablets.
   Inventariar ambas raíces sin activar coexistencia ni modificar ROOT. Una raíz desconocida bloquea.
2. **Autorizar** mediante identidad individual verificada por servidor y permiso administrativo,
   ligado al digest/operationId/actor/raíces/revisiones/caducidad. Token compartido, rol local o casilla
   no bastan. Lote: segunda aprobación administrativa verificable sobre el conjunto exacto.
   Cualquier cambio invalida el plan completo. Otro propietario/ingeniero bloquea este primer alcance.
3. **Aplicar cerca de exclusión y tombstone duradero** en la misma transacción del journal antes de
   tocar Drive. Todos los caminos de escritura (legacy, V3, reserva V2, reanudación, vídeos y SIC)
   deben rechazar el UUID eliminado y sus revisiones antiguas, incluso desde clientes desactualizados.
   No basta cancelar WorkManager local ni cambiar una fila de outbox. No se implementó/activó esta
   cerca en producción; por eso no se solicita eliminación desde Android.
4. **Papelera primero**, únicamente por IDs exclusivos y verificados, bajo bloqueo/revalidación
   servidor y autorización vigente. Nunca borrar carpeta por nombre, árbol entero o archivo compartido.
   SIC agregado de varias fichas requiere retirar la fila autorizada y regenerar la publicación con
   control de revisión; no eliminar el Excel compartido. Relación SCAP/SIC o propietario ambiguo:
   bloquear el componente afectado, sin adoptar otro UUID ni raíz.
5. **Verificar/reconciliar** por IDs tras cada paso; respuesta perdida consulta estado antes de repetir.
   Journal persiste antes/después de cada efecto, con claves idempotentes por archivo y exclusión
   entre operaciones solapadas. Caducar/renovar bloqueos, controlar concurrencia y comparar revisiones
   de nuevo antes de cada efecto son requisitos del adaptador real, ausente en esta propuesta.
6. **Limpieza local** solo tras ACK administrativo estricto ligado a operationId/digest/revisión y
   verificación remota. Conservar respaldo íntegro recuperable y referencias compartidas. Retener
   auditoría/tombstone sin tokens ni datos personales innecesarios. No borrar journals indiscriminadamente:
   cerrar y referenciar sus sucesores, conservando recibos históricos necesarios para conciliación.
7. **Purgar definitivamente** sería una operación administrativa posterior, con autorización explícita
   independiente, plazo de retención y advertencia de irreversibilidad. No restaurable desde la UI.
   La restauración desde papelera exige reconciliar referencias, revisión y permisos en servidor antes
   de levantar tombstones. Ni restauración remota ni purga están implementadas en V21 aquí.

Estados previstos: PREVISUALIZADO → AUTORIZADO → PENDIENTE_DE_SERVIDOR → ELIMINANDO_REMOTO →
VERIFICANDO_REMOTO → LIMPIANDO_LOCAL → COMPLETADO. Preflight inválido: BLOQUEADO. Efectos ya iniciados
o resultado incierto: PARCIAL, con cerca y pendientes conservados, sin borrar copias locales.
COMPLETADO en el modelo significa papelera verificada y retirada simulada de duplicados exclusivos,
NO purga permanente. Sin red/permiso/Drive: detener, conservar operación y verificar antes de reintentar.

## Evidencia y estado de fases

Modelo ejecutable: backend/admin-removal-proposal/simulation.cjs; pruebas simulation.test.cjs.
Es exclusivamente una simulación con adaptadores en memoria y snapshots JSON: no tiene transporte,
acceso Google, firma de permisos, autenticación, locks distribuidos o journal de disco productivo.
El campo verified del grant es una aserción de fixture, NO una implementación de seguridad.
La restauración simulada solo solicita conciliación y conserva la cerca; no devuelve bytes remotos.
La purga simulada solo prueba que la interfaz no ofrece recuperación después de ese estado.

- **A PARCIAL:** previsualización/confirmaciones/guardas locales implementadas; inventario exhaustivo
  y aceptación física pendientes. Sin ejecución destructiva habilitada.
- **B PARCIAL:** contrato definido y modelo de estados/idempotencia con mocks; adaptador administrativo
  autenticado/durable para el proyecto original aún pendiente. No confundir modelo con backend listo.
- **C PENDIENTE:** autorización separada para entorno aislado, datos ficticios y permisos reales,
  con concurrencia, reinicios, pérdidas de ACK, restauración y coste de lecturas acreditados.
- **D BLOQUEADA:** requiere A/B completas, C aprobada, revisión del PR y corte explícito del original.
  Antes de APK nueva, integrar deliberadamente y asignar versión superior a 39; no compilar/distribuir
  la fuente de esta rama como si fuese la APK instalada 1.4.25.

Android: **142/142 casos únicos PASS**, 25 clases, cero fallos/errores/omitidos, 53 segundos.
Incluye los 137 de integración y cinco nuevos. Perfil V2 mocks: gboValidationBuild=true,
:app:testDebugUnitTest, init-scripts tools/migracion/pruebas.init.gradle y
tools/validacion/v2-mocks.init.gradle; selección anterior más RemovalPreviewTest.
XML/log/desglose: tmp/eliminacion-controlada-20261102/verificado/ (no publicados).
No es suite histórica completa ni validación física o backend real.
Incidencias conservadas: tres selecciones iniciales 140/142 PASS por la premisa de outbox global
vacío; se identificó un conflicto SCAP ficticio de otra prueba. Ahora se exige igualdad EXACTA
del outbox previo/posterior y cero intenciones de los borradores operados. No se borró el fixture
ni se relajaron guardas productivas. Un intento de aislamiento por ancho causó timeout Compose
(8/9 PASS); se retiró, separando pruebas Room de la prueba UI con inicializador de workers inactivos.
Focalizadas finales 9/9 PASS. Todos los intentos/XML están separados; no sumar repeticiones.
Backend: `node --test backend/admin-removal-proposal/simulation.test.cjs`: **20/20 PASS**.
Cubre individual, conjunto filtrado, confirmación exacta/doble, otro ingeniero, ACK/binding, outbox,
vínculo, fotografía compartida, propietario remoto, red, error Drive/parcial, replay/reinicio,
cerca contra resubida, restauración solicitada y purga no recuperable. Solo mocks, cero borrados reales.

**NO-GO borrado remoto/definitivo.** Próximo trabajo: revisar alcance y mecanismo de identidad
administrativa del servidor original; después implementar el adaptador B sin activarlo y acreditar C.
