# GBO — avance operativo, 01/11/2026

**Candidata original 1.4.22 (36), no instalada ni distribuida. NO-GO para corte o distribución.**
Rama `codex/observaciones-campo-asus-20261030`, base preservada `0d3e2f5`.
Android `eaf2a78d541e1a4c8f0468e5e7735dd447d42edc`; backend local
`185eef4ef839ee62cc21c136000d6487d5f80414`. Sin push ni cambios en main, Google o datos reales.

## Observaciones y correcciones

| Bloque | Resultado de esta intervención | Estado final |
|---|---|---|
| Copias locales e historial | Confirmadas agrupación moderna, historial secundario y explicación de faltantes. Defecto corregido: regenerar/reintentar una copia originalmente automática retenía automatic=true y podía programar Drive. Las acciones manuales ahora pasan a locales; conservan plan, originales y ACK. | IMPLEMENTADO y pruebas locales; físico pendiente |
| Abrir documentos/nombres | Se reutilizó la implementación existente y sus pruebas; no se borró/movió/renombró historial. Nuevos nombres V2 respetan reservas/bindings, incluso en el adaptador de dos raíces. | PARCIAL: homología remota y visualización física pendientes |
| Nuevo tramo, filtros y estados | Integración remota ya existente compilada. Nuevo test de diálogo: ruta requerida, cancelar conserva sesión/cola, confirmar inicia la sesión mediante transacción existente. No se duplicó la función. El cierre de sesión puede marcar registros para Usuarios; no es borrado de fichas. | IMPLEMENTADO; físico pendiente |
| Longitud | Guardado/reapertura/exportación focalizados: 42+995 a 43+005 = 10 m. Sin cambiar cálculo existente. | IMPLEMENTADO; físico pendiente |
| Limpieza/descarte/restauración | Regresiones de protección ACK/ZIP/pendientes y consolidación aprobadas; ningún borrado real. La limpieza de copias no es respaldo universal de Room. | IMPLEMENTADO; SAF/MediaStore y aceptación física pendientes |
| SIC | Flags efectivos false para 17A y 17B, true para nombres V2, comprobados también en generación de ambas variantes. SIC-17 y snapshots congelados conservados; no se inventa completitud. | IMPLEMENTADO en cliente; producción pendiente |
| SCAP/conflicto histórico | No se repitió la mitigación 1.4.21. Sobre de staging añade UUID padre/inspección y revisión base, manteniendo paquete congelado y hash. Evita perder la autoridad para una preparación histórica cacheada al enrutarla. | IMPLEMENTADO y regresión; colisión remota real SIN CONCILIAR |
| Pavimentos, daño 26, PCI | Autoguardado/consolidación y severidades independientes cubiertos en selección local. Motor PCI y Room sin cambios. Cierre ficticio físico bloqueado si programa Drive productivo. | IMPLEMENTADO previamente; físico pendiente |
| Backend reanudable | Estrategia opcional de coexistencia implementada y probada localmente; no migración masiva ni modificación de V21. | PARCIAL: no desplegado/validado en producción |

Archivos Android modificados: `app/build.gradle.kts`, `core/AutomaticDelivery.kt`,
`localfiles/LocalCopies.kt`; pruebas `LocalFiles148Test.kt`, `FieldSessionFormsTest.kt`,
`SyncRecovery10DTest.kt` (paquete `com.tuempresa.inventariovial`). Backend:
`DeliveryRootRouting.gs` nuevo, `AuthorizedRootGuard.gs`, `SyncV3.gs`, empaquetador,
fragmento de capacidades, README y tests de routing/paquete. El motor SyncV3Core no cambió.

## Pruebas exactas

| Ejecución local | Resultado | Evidencia privada |
|---|---|---|
| Copias/listado inicial | 24/24 PASS, 0 fallos/errores/omitidos | `tmp/avance-20261101/android-01/xml/` |
| Integración Android final, 11 clases | **82/82 PASS**, 0 fallos/errores/omitidos; Gradle 3m24s | `android-02/xml/`, `resultados.json`, `gradle.log` |
| Routing inicial / ampliado | 14/14 y 17/17 PASS, selecciones relacionadas | `backend-raices-01.log`, `backend-raices-02.log` |
| Integración backend final, 5 archivos de tests | **48/48 PASS**, 0 fallos/omitidos | `backend-integracion-final.log` |
| validateReleaseConfiguration, assembleDebug, assembleRelease, signingReport | PASS, Gradle 1m16s | `compilacion-candidatas.log` |

La selección Android final contiene AutomaticDocuments149 (17), BridgeIdentityConflict (6),
FieldCleanupAndConsolidation (8), FieldSessionDefaults (7), un nuevo caso FieldSessionForms,
LinearLengthDelivery (3), LocalCopyPresentation (4), LocalFiles148 (20),
PavementFinalIntegrity (5), SicContributionPolicy (2), SyncRecovery10D (9).
Backend final: routing (17), guardas (8), identidad V21/candidato (10), auditor (7), paquete (6).
Los intentos anteriores son subconjuntos: **no sumar 24+82 ni 14+17+45+48 como casos distintos**.
No es la suite histórica completa. Mocks/interceptores locales, sin HTTP real ni ensayos
masivos de Drive. **P01–P17 manuales: SIN EJECUTAR, 0/17**, también en 1.4.22.

## Dos raíces: propuesta lista para revisión local, no para corte

Se conserva el literal de la raíz antigua. `DeliveryRootRouting.gs` decide bajo ScriptLock
tras leer ambas autoridades: key, UUID padre, registro, binding, reserva, staging y journal
históricos conservan su raíz; solo identidad nueva sin autoridad previa usa el destino nuevo.
Pins durables vinculan key/UUID/raíz para clientes antiguos. No copian ACK, revisión ni archivos.
Colisión bridgeCode global, identidad repartida, ausencia ambigua de UUID o registro ilegible
bloquean. ACK perdido/replay y PENDING reanudable mantienen IDs/hashes/revisión; legacy PENDING
requiere conciliación. La iteración completa probó autoridad situada después de 100 entradas.

Configuración privada nueva exige aprobación de inventario con SHA y guarda de ambos destinos;
**no se estableció en Google**. El marcador de activación bloquea desactivar/sustituir raíces
en este candidato. `DRAIN_ONLY` permite replay exacto y terminar PENDING reanudable de la
revisión exacta, bloqueando revisiones/identidades/reservas nuevas. Volver a V21 a ciegas
después de usar el destino nuevo no es rollback seguro: V21 desconoce estas autoridades.

Límites concretos: **legacy y vídeo se bloquean en modo dual**, no están enrutados. Preparación
SCAP antigua sin UUID/pin/registro requiere revisión; el cliente nuevo aporta UUID también en
staging. PENDING sigue protegiendo los consolidados de toda su raíz y puede causar BUSY a
otras fichas de esa raíz. Cada raíz conserva su EXCEL_SIC; no existe consolidado único entre
raíces. La lectura íntegra por petición puede ser costosa: el número real de entradas y su
coste Drive deben revisarse antes de activar; un timeout bloquea sin escoger otra raíz.

Paquete local completo: `tmp/avance-20261101/PROPUESTA_ORIGINAL_FINAL_NO_ACTIVAR/`.
Conserva autenticación, manifest, URL y raíz originales; módulos operativos NO copiados al
editor ni desplegados. READMEs detallan configuración/rollback y fuentes sin secretos.

## Google y bloqueos reales

El propietario confirmó que **2FA sigue pendiente**. No se accedió a Drive, Script ni tablet.
Último estado acreditado: original V21, Usuarios independiente intacto; copia de ensayos
archivada, no utilizada. **Journals reales: total y COMPLETE/PENDING/ilegibles desconocidos**;
no atribuir los 135 del mock al servidor. Destino elegido aún no activado; permisos generales
de edición preexistentes sin nueva verificación ni cambio. Colisión SCAP histórica sin lectura
de la fila remota, por lo que no se inventó una recuperación ni se liberó su bloqueo.

Antes del corte: completar OAuth oficial; inventario estable de ambas autoridades y conciliación
de cada pendiente/identidad; revisar uso legacy/vídeo y preparaciones antiguas; corregir permisos
con autorización específica; aprobar configuración/rendimiento y diff del mismo proyecto,
deployment/URL y rollback compatible. Después, autorización expresa de corte y ACK operativo
controlado. **No basta cambiar ROOT ni aprobar mocks.**

## Candidatas preservadas

Package `com.tuempresa.inventariovial`, 1.4.22 (36), Room 21/esquemas intactos.
Directorio: `C:\GBO\tmp\avance-20261101\candidata-final-1.4.22-36\`.

| Archivo | SHA-256 | Uso |
|---|---|---|
| `GBO_1.4.22_36_DEBUG_ORIGINAL_PILOTO_LOCAL.apk` | `3d0f482327f764cde41b35dd72c98e079f657d6c7d2a630f4c4077dd65549cf7` | Firma Debug Huawei/ASUS; candidata offline, no instalada. |
| `GBO_1.4.22_36_RELEASE_HISTORICA_NO_DISTRIBUIR.apk` | `5b9186f4fe0d3a0bc4b2abe15a3a60595724c4406bcaccc0f9fdfee75ca2efbd` | Firma Release histórica distinta de Debug; NO DISTRIBUIR. |

Certificados contrastados: Debug `d2e54cb086aa731ef046be300977d0b16c0d8f66fdcb08d0d9cfd2038bf0d872`;
Release `0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315`.
Firma, manifest, versión, alineación 16 KiB y configuración original de Drive/Usuarios
verificados; cero endpoints Apps Script ajenos a los dos aprobados en DEX y token de ensayo
conocido ausente. Valores privados no publicados. Estas APK apuntan al backend original,
**no activan el routing ni el destino nuevo**. APK anteriores verificadas por hash: 1.4.20
Release y ambas 1.4.21 intactas. El Debug 1.4.21 aparece ahora localmente como `GBO_1.4.21.apk`;
su nombre largo original sigue disponible en el respaldo privado del checkpoint anterior.

## Próxima acción y decisión

Procedimiento y ficha breve: `docs/PILOTO_LOCAL_RESPALDO_20261101.md`; checklist exacto P01–P17
en continuidad/manual privado. Hace falta autorización separada para extraer/resguardar datos
reales y detener la app, luego respaldo coherente/restaurable y aprobación de actualización
Debug sobre Debug. Nada de esto se ejecutó; la última evidencia instalada sigue siendo 1.4.20.
El propietario hará navegación manual; solo recoger ADB/Logcat de errores reproducibles.

Próxima acción local: **autorizar respaldo privado y verificar recuperación antes de aprobar
instalación de Debug 1.4.22**. Release para ingenieros requiere además firma compatible por
dispositivo, P01–P17, conciliación remota, corte autorizado, ACK/integridad operativos y
autorización de distribución. **NO-GO corte/distribución; revisión local preparada.**
