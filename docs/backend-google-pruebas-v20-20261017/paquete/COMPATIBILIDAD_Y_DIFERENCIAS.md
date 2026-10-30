# Fuentes, equivalencia y alcance

Los ocho originales son los aportados por el propietario y declarados sin cambios tras V20, 15/10/2026 07:07. No se obtuvo una instantánea por API de Google. Se conserva su SHA-256 y la referencia a la evidencia anterior de 95 casos distintos aprobados. No se cuentan las reejecuciones como casos nuevos.

| Módulo | BASE | CANDIDATO |
|---|---|---|
| Codigo.gs | Despacho y autenticación V20. Raíz sustituida por la autorizada; doGet/doPost internos detrás de barrera de ensayo. | Idéntico a BASE. Nunca se propone reemplazar Codigo productivo. |
| SyncV3.gs | Lógica original de staging/lock/almacén. | Candidato revisado: decode/SHA de staging antes del lock, guard de file.key y la instrumentación de fases ya revisada, una sola vez. Todas las escrituras y recuperación continúan protegidas. |
| SyncV3Core.gs | Motor V20; seams de medición comunes. | Reutiliza únicamente el workbook ya validado de la misma fila/formato/SHA y ownerKey en este commit. Históricos/otros formatos siguen desempaquetándose y conciliando Observaciones. |
| PhotoNamesV2Core.gs | Original exacto. | Idéntico. |
| SicObservationsV1Core.gs | Original 10K exacto. | Idéntico. |
| VideoV1.gs | Original 10H con servicios dirigidos al adaptador de ensayo. | Idéntico. |
| VideoV1Core.gs | Original exacto. | Idéntico. |
| VideoNamesV2Core.gs | Original 10H exacto. | Idéntico. |

En los módulos que usan servicios Google se sustituyen referencias por facades `GboTest*`. No se cambia el contrato de negocio. `v3Store` tiene una envoltura común para medir operaciones; el motor tiene llamadas de medición de validación y consolidación. El cuerpo de la transacción y sus reservas/commit/rollback no se rediseñan. La respuesta añade un único campo `_gboTest` de telemetría; el resto de campos es el ACK original y se compara quitando ese campo. El campo `_test` de entrada es una barrera del ensayo, fuera del hash canónico. Las APK reales no se conectan a esta implementación.

Los únicos archivos que difieren entre las dos carpetas ejecutables son `SyncV3.gs`, `SyncV3Core.gs` y `TestVariant.gs`. No se incorporó la propuesta de caché durable de históricos ni se duplicaron 10H/10K/fotos V2. El candidato es exactamente el revisado, antes de las adaptaciones comunes; los hashes fuente lo acreditan.

## Diferencias de error ya conocidas (no ocultar)

- Staging válido bajo lock ocupado: BUSY/retryable en ambos; ningún ACK definitivo ni escritura de la petición BUSY.
- Hash/tamaño inválido con lock ocupado: V20 puede responder BUSY; candidato rechaza `HASH_OR_FILE_SIZE` antes de pedir el lock.
- file.key no textual: V20 podía responder `STAGE_COMMUNICATION_ERROR` retryable; candidato devuelve `INVALID_STAGE` no retryable.
- Un rechazo antes del lock no ejecuta recuperación de journals en esa petición. La siguiente petición válida conserva la recuperación normal. No hay equivalencia universal de precedencia de errores.

## Compatibilidad Android 1.4.7 y 1.4.9

Se usan `syncProtocol=3`, `pathVersion=4`, `stageRecordV3`, `commitRecordV3`, reservas V2, SHA-256 de bytes y hash canónico del motor original. SIC y Pavimentos conservan filas por UUID, SCAP conserva padre PUENTE/identityId y los videos siguen `videoProtocol=1`, `SCAP_ONLY`, begin/finish, comprobación de checksum y tamaño.

La lectura de `AutomaticLocalDocuments.kt`, `LocalCopies.kt` y `LocalExportBuilder.kt` en 1.4.9 confirma que su outbox es local: trabajos `local_export_jobs`, nombres por revisión, publicación a almacenamiento local y workers `gbo-local-copy-*`. No se invoca una reserva, preparación Drive o HTTP desde ese exportador. Referenciar `DriveInventorySchema` para nombres locales no envía datos. La copia no cambia el UUID/ACK padre ni crea por sí misma otra entrega remota. El backend recibe el mismo protocolo que 1.4.7.

El ensayo modela esa ausencia de nueva petición y además fuerza un commit repetido de la misma muestra/revisión/hash: debe devolver alreadyExists y los mismos IDs. Esto prueba idempotencia del servidor, **no ejecución física de los workers de una APK ni ausencia universal de errores Android**. No se recompila Android. Se preserva `_gboJointAnnotations` como JSON opaco con hash; no se transforma.

## Cobertura y pendientes

- SIC-18: A/B/C/D, orden final A/B/D/C, números estables reservados inicialmente en ese orden; fotos V2 y Observaciones como texto literal, herencia de cliente legacy y borrado explícito.
- Pavimentos: dos muestras asfálticas SIC-14 (tres y dos filas), PDF ficticio, JSON y fotografía. La carga adicional añade tres filas por muestra. No sustituye la validación contractual visual de las plantillas rígidas/no pavimentadas SIC-15/16 ni sus PDF nativos Android.
- SCAP: dos padres, PDF y fotos V2; un video reanudable sobre el primer padre. No ejecuta formularios ni datos de ingenieros.
- Reintentos y recuperaciones: BUSY, staging/replay y pérdida de respuesta en Google preparados; rollback y reintento probados localmente. La interrupción forzada de un proceso Google dejando un journal PENDING requiere ensayo controlado adicional; no se confunde una excepción con rollback normal con un proceso terminado abruptamente.
- El ensayo UPGRADE conserva los IDs e historial BASE al cambiar únicamente el candidato; está implementado, pero pendiente de ejecución Google.

## Protección y límites de seguridad

La barrera fija la raíz autorizada, exige proyecto fijado por ID y propiedades exclusivamente `GBO_TEST_*`. Cada corrida tiene subraíz propia. Los IDs aportados en la petición se validan antes del handler; cada mutación vuelve a comprobar ascendencia nativa. Se rechazan objetos externos, otras corridas, shortcuts, huérfanos, múltiples padres, ciclos, copias sin padre, updates con cambio de padres, IDs no reservados y fetch a destinos distintos de la apertura reanudable Drive de un archivo dentro de la corrida. Solo se permiten las acciones de protocolo ensayadas y diagnósticos acotados; los handlers legacy quedan sin acceso HTTP.

Los helpers son código de confianza revisado, **no una sandbox para ejecutar código arbitrario pegado por un editor con permisos de propietario**. Google no ofrece una transacción atómica entre comprobar ascendencia y mutar el objeto: no debe haber movimientos externos concurrentes ni otro proyecto escritor sobre las subcarpetas de ensayo. La protección verifica antes de cada escritura dentro del flujo entregado; no controla acciones manuales ajenas. No se añaden funciones de borrado/limpieza. Un fallo de referencia puede dejar operaciones previas válidas dentro del ensayo y su journal; nunca habilita escribir fuera de la subraíz.
