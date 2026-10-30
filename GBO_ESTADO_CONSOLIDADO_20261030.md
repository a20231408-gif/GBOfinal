# GBO — Estado consolidado del trabajo de Work

Fecha: 30/10/2026, America/Lima. Auditoría local de recuperación; no es aprobación de distribución ni de producción.

## 1. Estado verificable del repositorio

- Repositorio: `C:\GBO`, origin `https://github.com/a20231408-gif/GBOfinal.git`.
- Base e HEAD inicial: `dbe7247a6f61734cbe6119bb6d4f018ca0479363`.
- Rama activa inicial y final: `codex/observaciones-campo-asus-20261030`. Al iniciar no tenía commits propios ni upstream; una rama por sí sola no preservaba los cambios pendientes.
- Rama de migración: `codex/migracion-asus-20261029`, permanece en `dbe7247`. No se modificaron main ni referencias históricas.
- Estado inicial: 12 archivos modificados rastreados y 4 archivos nuevos (2 Kotlin y 2 documentos). Índice vacío. No se encontró AGENTS.md en el proyecto ni en C:\.
- Android conserva `1.4.18 (32)`, paquete `com.tuempresa.inventariovial`, Room 21. No hay nueva versión ni migración de esquema de Work.
- AGP 9.5.0 y KSP 2.3.6 eran cambios previos al trabajo de Work; se preservaron por separado. El contenido de configuración no se cambió durante esta auditoría.
- `git diff --check` pasó. Los commits locales incorporan los cambios; el HEAD final se obtiene con `git log -1 --format=%H`. El commit documental contiene este informe, por lo que no incluye su propio hash.

## 2. Cambios de Work encontrados y contraste con sus informes

Los dos documentos solicitados existen en `docs/observaciones-asus-20261030/` y fueron leídos completos. Sus bytes originales se conservan.

| Área | Implementación encontrada | Alcance y discrepancias |
|---|---|---|
| Filtros | Año/mes/día sobre fecha de levantamiento; ISO y dd/MM/aaaa estrictos; Borradores/Enviados/Sin confirmar | Enviados utiliza comprobación existente de ACK completo para la revisión actual, no solo estado SYNCED. La prueba UI histórica no recorre los nuevos filtros de fecha/ACK. |
| Descarte múltiple | Selección de SIC/Pavimentos DRAFT visibles, confirmación congelada por identidad/revisión y transacción de grupo | Reutiliza protecciones individuales y conserva UUID/fotos. SCAP excluido. Restauración individual existente; no hay restauración múltiple nueva. Un borrador con cola/recibo/preparación bloquea todo el grupo. |
| Longitud | Cálculo explícito kilómetros + offsets, ruta/calzada/sentido y ejemplo 42+995 → 43+5 = 10 m | Solo auxiliar visual, conforme al informe. La elección de referencia kilométrica, procedencia y resultado no se persisten ni exportan. No resuelve todavía la longitud lineal guardada. |
| Nombres fotográficos | Se ejecutaron regresiones existentes de reservas V2 | No hay cambio nuevo al protocolo ni al formato de nombres; ReadablePhotoNames10Test ya estaba en la base. Los nombres largos solicitados no están implementados. |
| Pavimentos y SCAP | Mensajes adicionales al fallar dos aserciones | No cambian validadores, fórmula PCI, juntas ni recuperación productiva. Esas funciones ya estaban en la base; no deben atribuirse como implementación nueva de Work. |
| Google | Documento de preparación de cambio de carpeta/cuenta | No hay inventario remoto, mapping real de IDs, ensayo de destino ni despliegue. Preparado significa plan, no migración lista para ejecutar. |

Los 28 casos declarados por Work coinciden con su resumen/XML conservados y se reprodujeron durante esta auditoría. No acreditan la suite completa. El informe de Work dice que no pudo medir con Python; esto describe su ejecución: existe además un diagnóstico previo en `tmp/GBO_MIG_ASUS_20261030/DIAGNOSTICO_ASUS.md` con benchmark aislado y sus limitaciones. No se mezclan ambas ejecuciones ni se reinterpretan los 21 s de Work como benchmark frío.

## 3. Archivos y clasificación completa

Rutas relativas a C:\GBO; M = modificado frente a dbe7247, A = añadido.

| Archivo | Tipo | Estado |
|---|---|---|
| app/src/main/java/com/tuempresa/inventariovial/MainActivity.kt | Android, integración longitud auxiliar | M |
| app/src/main/java/com/tuempresa/inventariovial/RecordHistoryScreen.kt | Android, longitud auxiliar en historial | M |
| app/src/main/java/com/tuempresa/inventariovial/core/LocalDraftDiscard.kt | Android, transacción de descarte múltiple | M |
| app/src/main/java/com/tuempresa/inventariovial/field/EngineeringEditScreen.kt | Android, longitud auxiliar en edición | M |
| app/src/main/java/com/tuempresa/inventariovial/field/TechnicalLengthField.kt | Android, control visual explícito | M |
| app/src/main/java/com/tuempresa/inventariovial/field/RecordDateFilter.kt | Android, filtros de fecha | A |
| app/src/main/java/com/tuempresa/inventariovial/localfiles/UnifiedRecordsScreen.kt | Android, filtros y selección/descarte | M |
| app/src/main/java/com/tuempresa/inventariovial/validation/TechnicalElementRules.kt | Android, función kilométrica auxiliar | M |
| app/src/test/java/com/tuempresa/inventariovial/ContractualLength111Test.kt | Prueba, nuevo caso kilométrico | M |
| app/src/test/java/com/tuempresa/inventariovial/LocalDraftDiscard10CTest.kt | Pruebas, rollback y restauración de grupo | M |
| app/src/test/java/com/tuempresa/inventariovial/PavementP0AdditionalTest.kt | Prueba, diagnóstico de aserción | M |
| app/src/test/java/com/tuempresa/inventariovial/SyncRecovery10DTest.kt | Prueba, diagnóstico de aserción | M |
| app/src/test/java/com/tuempresa/inventariovial/RecordDateFilterTest.kt | Pruebas puras de fecha | A |
| gradle/libs.versions.toml | Configuración previa ASUS, no atribuida a Work | M |
| docs/observaciones-asus-20261030/DIAGNOSTICO_Y_FASES.md | Documentación Work | A |
| docs/observaciones-asus-20261030/MIGRACION_GOOGLE_PREPARADA.md | Documentación Work | A |

Este informe es el único archivo versionable añadido por la auditoría. No se editaron las fuentes, pruebas, documentos de Work ni configuración: los SHA-256 de los 16 archivos coinciden con el respaldo inicial después de crear los commits.

Ignorados relevantes inspeccionados por categoría: `local.properties` (ruta SDK local), `.idea/`, `.gradle/`, `build/`, `app/build/`, `buildSrc/build/`, `buildSrc/.gradle/` y `tmp/`. Contienen configuración local, cachés, APK y evidencias; no se añadieron a Git. La enumeración de temporales produjo avisos de rutas Windows demasiado largas: no se afirma un inventario exhaustivo de cada archivo ignorado. No se copiaron bases ni fotografías reales al respaldo ni a commits.

## 4. Commits locales y respaldo

La instrucción de consolidación permite crear commits si procede. Se preservó trabajo existente, sin presentar los commits como aprobación funcional general:

| Commit | Contenido |
|---|---|
| 42e8a04 | Filtros de fecha/estado y descarte reversible múltiple, con pruebas |
| ede528d | Cálculo kilométrico auxiliar e integración visual, sin persistencia |
| f20f005 | Diagnóstico de pruebas SCAP/Pavimentos, sin debilitar aserciones |
| b130f52 | Registro separado de AGP/KSP preexistentes en ASUS |
| Commit documental posterior | Dos informes originales de Work y este consolidado |

Respaldo anterior a los commits: `tmp/consolidacion-20261030T130314Z/`.

- `fuentes-work.zip`: 16 archivos de texto revisados, incluidos los nuevos; ZIP verificado, sin APK, configuraciones privadas ni temporales.
- SHA-256 del ZIP: `dba56e6e58194265bac2019a4ecc368bb65bf36cb7c2c7993299cb79fc54675c`.
- `manifest.json`: SHA-256 individual de cada archivo; `cambios.patch`: diferencia binaria Git de rastreados frente a HEAD inicial. El ZIP complementa el parche con los no rastreados.
- XML anteriores preservados en `xml-anteriores/`; evidencia nueva en `xml-auditoria/`, `resultados.json`, `pruebas.log` y `release.log`. El init temporal usa directorios de evidencia nuevos para no sustituir los informes anteriores.
- Bundle final local `rama-consolidada.bundle` y estado final se conservan en esa misma carpeta después del commit documental. El bundle contiene historia: no es material autorizado para publicar.

Es respaldo local en el mismo equipo, no protección independiente ante pérdida del disco. No se subió a servicios externos. Se inspeccionó el contenido concreto de los archivos incluidos; no se ejecutó un nuevo escaneo exhaustivo de secretos de toda la historia. No se publica ninguna garantía universal de ausencia de secretos históricos.

## 5. Pruebas ejecutadas en esta auditoría

Gradle offline, JBR de Android Studio asignado solo al proceso, un worker, heap 2048 MiB; Robolectric local y transportes simulados. Init derivado del ejecutor de migración, únicamente con directorios de evidencia aislados. La tarea Test se fuerza a ejecutar aunque la compilación esté caliente. Log: `tmp/consolidacion-20261030T130314Z/pruebas.log`.

| Clase | Casos | Aprobados | Fallidos |
|---|---:|---:|---:|
| ContractualLength111Test | 6 | 6 | 0 |
| LocalDraftDiscard10CTest | 7 | 7 | 0 |
| PavementJointPreviewUiTest | 2 | 2 | 0 |
| PavementMigration21Test | 1 | 1 | 0 |
| PavementP0AdditionalTest | 5 | 5 | 0 |
| ReadablePhotoNames10Test | 4 | 4 | 0 |
| RecordDateFilterTest | 2 | 2 | 0 |
| UnifiedRecords149UiTest | 1 | 1 | 0 |
| AutomaticSyncRegressionTest | 21 | 21 | 0 |
| MixedSic17HotfixTest | 6 | 6 | 0 |
| ScapFinalP0Test | 9 | 8 | 1 |
| SyncRecovery10DTest | 7 | 6 | 1 |
| PavementAnnotationSync147Test | 1 | 0 | 1 |
| Total | 72 | 69 | 3 |

Cero errores y cero omitidas. Las primeras ocho clases reproducen las 28 pruebas aprobadas de Work. Tiempo Gradle comunicado: 39 s; compilación caliente, sin medición de memoria y sin comparación de hardware nueva.

Fallos conservados:

1. `PavementAnnotationSync147Test.rigidAnnotationsAndPhotoSurviveOfflineBusyLostAckAndAuthorizedUpdate`: exige explícitamente `BuildConfig.GBO_PHOTO_NAMES_V2_ENABLED`; su valor por defecto es false al faltar server.local.properties.
2. `ScapFinalP0Test.actualPreparationFailureIsActionableAndDoesNotLoseTheSnapshot`: esperaba BLOCKED, obtiene UNCONFIGURED. La reserva simulada que debe provocar la excepción depende de la ruta V2; no se modificó configuración para forzar el resultado.
3. `SyncRecovery10DTest.scapV20BlockRecoversWithFreshV21CapabilitiesAndLostAckNeverRestagesSixteenFiles`: el mensaje nuevo permite observar `PENDING_VALIDATION` porque el panel oficial SCAP requiere nombres V2; no llega a la pérdida de ACK que espera el test.

Estos fallos no acreditan un defecto del servidor productivo. Impiden aprobar la regresión integral SCAP con el perfil local actual. Los cinco fallos históricos de AutomaticDocuments149Test/Engineering2DataTest permanecen como antecedentes no reejecutados, no se suman a los tres observados.

`validateReleaseConfiguration` se ejecutó y falló con `RELEASE_BLOCKED_MISSING_SIGNING_KEY`, sin fallback Debug. La tarea termina en la primera guarda: no certifica las siguientes comprobaciones.

La APK Debug existente coincide con el hash informado por Work: `2d10ddc875d51c98b44ac41f85ac19bf10d6e47574f2aaae08a99f7700586f90`. Se comprobó su hash, sin regenerarla, instalarla ni certificarla como Release. No se ejecutó assembleRelease, instrumentación, ensayo Google ni pruebas de campo durante esta auditoría.

## 6. Bloqueos reales y recuperación privada de Release

Ausentes: `server.local.properties`, `drive.local.properties`, `keystore.properties`. Las cuatro variables de entorno `GBO_RELEASE_STORE_FILE`, `GBO_RELEASE_STORE_PASSWORD`, `GBO_RELEASE_KEY_ALIAS`, `GBO_RELEASE_KEY_PASSWORD` tampoco estaban definidas en el proceso inspeccionado. Esto acredita firma no configurada, no ausencia física del keystore en toda la laptop.

Procedimiento seguro:

1. Recuperar de Huawei/respaldo privado los archivos legítimos y el keystore histórico por canal o medio cifrado. No compartir valores en el chat ni incorporarlos al repositorio. Los .example no reemplazan configuración real.
2. Conservar cualquier configuración que aparezca después; no sobrescribirla. Adaptar solo la ruta local de GBO_RELEASE_STORE_FILE, manteniendo clave, alias, contraseñas y destinos legítimos.
3. Comprobar exclusiones Git y acceso local restringido. Mantener Usuarios separado del inventario; restaurar configuración no autoriza cambiar cuenta, carpeta, permisos o endpoints de producción.
4. Ejecutar validateReleaseConfiguration sin bypass. Exigir certificado histórico SHA-256 `0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315` y luego revisar las guardas de Usuarios/Drive.
5. Repetir las tres regresiones bloqueadas bajo el perfil legítimo. Una futura candidata distribuible necesita decisión explícita de versión/firma; no distribuir cambios nuevos reutilizando automáticamente 1.4.18 (32).

No se generó otra firma ni se inventaron credenciales o valores de banderas.

## 7. Riesgos técnicos y cobertura pendiente

- La suite focalizada verde de Work excluye los tres ensayos fallidos; no equivale a PASS general.
- Faltan pruebas Compose de los nuevos filtros de fecha/ACK, cambio de filtros con selección, confirmación y restauración desde UI. Las pruebas nuevas cubren parser y operación de grupo en Room; no el recorrido visual completo.
- Falta ampliar pruebas de descarte de grupos mixtos SIC/Pavimentos, propietario incorrecto y aparición de recibo/preparación entre selección y confirmación. Los controles existentes se reutilizan; no se ha demostrado una vulneración.
- El control kilométrico es rememberSaveable visual, sin vínculo persistido a procedencia por ficha. No confundir la etiqueta auxiliar con la longitud contractual guardada ni con un dato ya exportado. Para persistencia se necesitan modelo, migración conservadora y coherencia entre alta, edición, reapertura, JSON, Excel y payload.
- El descarte consulta archivos/preferencias además de Room. La transacción agrupa cambios de base de datos; no concede atomicidad conjunta con procesos externos. No se observaron pérdidas de datos; ensayos de concurrencia siguen pendientes.
- Las validaciones locales no certifican PDF/visores/cámara/almacenamiento en tablet ni reintentos contra Google. No se verificaron curvas PCI completas en esta selección.
- V21 es el despliegue conocido por documentación; no se consultó su estado remoto actual. No deben desplegarse referencias o propuestas como si fueran el servidor vigente.
- Los límites Drive conocidos siguen separados del ZIP local: no se ha implementado transporte nuevo de adjuntos grandes ni consolidación cronológica remota aprobada.
- AGP/KSP difieren de Huawei: se registra la divergencia, sin afirmar que los tiempos o artefactos sean idénticos.

## 8. Plan priorizado de continuación

| Fase | Trabajo | Criterio de salida |
|---|---|---|
| 1. Integridad de sincronización | Recuperar perfil privado legítimo; repetir fallos, capacidades, revisión congelada, BUSY y pérdida de ACK | Ensayos locales completos con evidencia; ninguna confirmación sin ACK íntegro; sin modificar V21 |
| 2. Recuperación de fichas | Cubrir selección/descarte/restauración en Compose y grupos mixtos; concurrencia con cola/recibos | UUID, originales y revisión conservados; rollback del grupo y exclusión SCAP verificados |
| 3. SCAP/Pavimentos | Revalidar cierres provisionales, juntas/global PCI, PDF y SIC; perfilar guardado SCAP con muchas fotos ficticias | Causa de cualquier demora identificada por fase; sin reducir evidencia ni relajar validación |
| 4. Longitud persistida | Diseñar y luego implementar referencia explícita/procedencia por registro, preservando PR históricos | Prueba 42+995→43+5=10 m tras guardar/reabrir; exportaciones y sincronización coherentes; migraciones sin pérdida |
| 5. Nomenclatura | Acordar convención compatible con reservas V2; probar límites, colisiones e histórico | Nombres estables y ACK intacto; sin renombrado masivo ni cambios de rutas congeladas |
| 6. Tablet | Preparar checklist ficticio Solo dispositivo; instalación manual solo con autorización | Filtros, recuperación, fotos, PDF/Excel y almacenamiento comprobados sin tocar datos de campo |
| 7. Google futuro | Completar inventario autorizado de solo lectura, estrategia de cuenta/raíz, mapping y ensayo aislado | Acta revisable con identidades, conteos, permisos, ACK de destino y reversión antes de autorizar ejecución |

El documento MIGRACION_GOOGLE_PREPARADA.md es el plan de partida. No se verificaron durante esta auditoría las políticas actuales de transferencia entre cuentas; deben validarse según las cuentas reales antes de seleccionar estrategia.

## 9. Próxima tarea de implementación y aprobaciones

Primero desbloquear y validar el perfil local de sincronización con la configuración legítima; no tratar ese bloqueo como necesidad de reimplementar recuperación. La próxima implementación funcional recomendada es **persistencia de la referencia kilométrica explícita y su procedencia**, después de cerrar el diseño de datos/exportación y las regresiones de integridad. El auxiliar ya existe y debe reutilizarse.

Esta auditoría no implementó funciones ni corrigió los riesgos enumerados. Los commits preservan el trabajo de Work y la divergencia previa de herramientas; no autorizan distribución.

Requieren autorización posterior: push/publicación, instalación o distribución en tablets, cualquier ensayo con escrituras reales Google, transferencia/copia de cuenta/raíz, cambio de permisos/configuración productiva y despliegues Apps Script. Nada de ello se ejecutó. El respaldo externo por un canal privado también debe acordarse; no se enviaron mensajes ni archivos a terceros.
