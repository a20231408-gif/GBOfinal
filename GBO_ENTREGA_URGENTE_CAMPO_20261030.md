# GBO — Entrega urgente de campo — 30/10/2026

**NO-GO para distribución y corte.** Android dispone de una candidata firmada y regresiones locales aprobadas. El ensayo real SCAP de 59 archivos agotó el tiempo de Apps Script durante el commit: staging completo, journal PENDING, 14 operaciones, cero registros confirmados y ningún ACK final verificado. No se declara resuelto el incidente de Flavio.

## 1. Correcciones terminadas y pendientes

Terminadas:

- Los seis fallos de P0.3 quedaron conciliados con el contrato vigente. Se corrigieron fixtures V2, disposición provisional, condiciones históricas y expectativas de ACK/publicación parcial. Se conservaron las comprobaciones de identidad, revisión, hashes, originales, BUSY y recuperación; un recibo antiguo incompleto sigue sin acreditar una revisión nueva.
- La referencia kilométrica explícita se guarda con la ficha y el borrador, reaparece al reabrir y alimenta JSON y Excel de la entrega preparada. `42+995 → 43+005 = 10 m`, también probado tras reapertura y modificación. Se conserva por separado la longitud medida/contractual y la compatibilidad histórica. Guardado y referencia quedan dentro de una transacción Room.
- Se corrigió un defecto real de nombres locales: editar el texto visible generaba otra reserva de carpeta. Ahora se conserva la reserva por padre/UUID, incluidas las antiguas, sin renombrar históricos.
- Mis registros incorpora Pendientes, orden por fecha y detalles técnicos desplegables; conserva filtros de fecha y descarte reversible individual/masivo de Work. La preparación de limpieza cuenta candidatos con ACK y copia local de la revisión actual; **no elimina entregados ni libera espacio**.
- Archivos locales resume la última publicación por registro/ámbito y permite consultar históricos. Reconciliar con Drive queda desplegable. No se borraron snapshots ni originales.

Pendientes explícitos:

- Cerrar el timeout real del commit SCAP grande y demostrar recuperación íntegra de su journal antes de volver a intentar el corte.
- Limpieza efectiva de copias entregadas con respaldo verificable; nombres remotos largos y nombre real del puente; equivalencia pública local/remota. Requieren diseño compatible y pruebas conjuntas, sin renombrar bindings históricos.
- Pavimentos conserva las ocurrencias y severidades de junta existentes, cubiertas por regresiones. Se redujo la duplicación visible; **no se cambió la programación de publicaciones automáticas para consolidar solo al finalizar la unidad** ni el cálculo PCI.
- Validación física, PDF/video en el nuevo entorno, BUSY concurrente real y tráfico HTTP Android contra el despliegue nuevo.

## 2. Archivos y commits locales

Rama: `codex/observaciones-campo-asus-20261030`. HEAD de código: `05a0ead63a5944d613709ee9328130a01aab1097`. Se conservaron Work y todos sus ancestros. La rama de migración permanece en `dbe7247`. Sin push ni cambios en main.

| Commit | Contenido |
|---|---|
| `aa1b6dc` | Fixture JPEG V2 e informes de cierre P0.2/P0.3 |
| `e14ef96` | Cinco archivos de regresiones históricas actualizados |
| `19ab997` | LocalFilesUi, UnifiedRecordsScreen, PublicLocalFolderNames y su prueba |
| `4c44a9f` | Persistencia de longitud y prueba LinearLengthDeliveryTest |
| `05a0ead` | Identidad de candidata 1.4.19 (33) |

La longitud afecta MainActivity, InventoryDraftStore, TechnicalLengthExport, EngineeringEditScreen, SicFormCore, TechnicalLengthField, InventorySaveRequest, EngineeringEdits, TechnicalLengthStore e InventoryViewModel. Los nombres completos se recuperan con `git show --stat <commit>`.

La revisión de los 21 archivos de código/pruebas/configuración modificados no encontró valores privados conocidos, claves privadas ni binarios. Propiedades, keystore, APK y evidencias permanecen ignorados. No se borraron ni incorporaron capturas existentes. Respaldo local verificable: `tmp/entrega-20261030/respaldo-git-local.bundle`; configuraciones y respaldo Google en `backend-privado/`, con ACL restringida a ASUS/SYSTEM. Este informe se conserva en un commit documental posterior.

## 3. Resultados exactos de pruebas

| Comprobación ejecutada | Resultado |
|---|---|
| Selección Android final, 32 clases | 194/194 PASS; 0 errores, 0 omitidas; 100,42 s |
| Dos clases complementarias de la selección original | 4/4 PASS; 0 errores, 0 omitidas; 30,95 s |
| Total distinto, 34 clases | **198/198 PASS**; incluye exactamente los 77 casos originales |
| validateReleaseConfiguration | PASS, firma histórica, sin bypass |
| assembleRelease offline | FAIL: dependencia pública lint-gradle:32.5.0 no estaba en caché |
| assembleRelease con descarga oficial habilitada | PASS, 52,53 s; lint no desactivado |
| Auditoría oficial de APK | PASS: identidad, firma, integridad y alineación de 16 KiB |
| Tres suites Node históricas de backend, repetición | **17 PASS / 25; 8 FAIL**, 0 omitidas |
| Ensayo real Google de 59 archivos | **FAIL por timeout; sin ACK final** |

Los ocho fallos Node corresponden a artefactos históricos ausentes: catálogo oficial, fragmento de capacidades y TestIsolation. No se fabricaron esas referencias. La primera corrida tuvo 16/25: el noveno fallo era la protección contra sobrescribir un ACK ya existente; repetir con una salida nueva permitió ese caso. Se preservan ambas corridas y fallos intermedios Android; no se suman ejecuciones repetidas como casos nuevos.

Evidencia: `tmp/entrega-20261030/final3/xml/`, `cobertura77/xml/`, `resultado-final-consolidado.json`, `backend-simulado2.log`, `backend-real-resultado.json` y `release-online/`. Hay un ZIP local de evidencia Android. **No se ejecutó la suite histórica completa**: otros 167 archivos `*Test.kt` quedan enumerados en el consolidado. Las pruebas incluyen migraciones Room, descarte, fotos/croquis, SIC/SCAP, daño 26, revisiones y recuperación simulada; no equivalen a pruebas físicas.

## 4. Apps Script nuevo y cuenta ejecutora

Se creó `GBO_Inventario_PRUEBAS_GBO_20261030_NO_CORTE` bajo `reconocimientoinventariovial@gmail.com`. Propietario exclusivo y acceso general **Restringido**, comprobados en Compartir. Implementación **versión 1**, ejecutada como GBO, acceso **Solo yo**. No es todavía un endpoint habilitado ni validado para las tablets.

El código real del original fue respaldado privadamente: ocho módulos coinciden byte a byte con `backend/referencia-v21/`; el manifest coincide semánticamente. Código.gs difiere en la raíz y el comentario de referencia, no se asumió igualdad total. Se observó la implementación original versión 21. La vista de activadores de la sesión muestra cero; no acredita activadores privados de otros propietarios ni la identidad ejecutora del despliegue original.

La copia usa una raíz privada protegida por `GBO_ISOLATED_ROOT_ID`, sin importar propiedades/bindings productivos. El ensayo verificó identidad/propietario de esa raíz, capacidades protocolo 3/V4/nombres V2/panel V2, reserva y rechazo de token inválido mediante el router real desde el editor. No fue una petición HTTP externa.

**Incidencia real:** el escenario ficticio contiene 54 fotografías, tres croquis, SCAP.xlsx, manifest y tres aportes SIC esperados. Lotes confirmados 14/28/42/56; el último alcanzó **59 en staging**. Ejecución del 30/10, **10:56:01–11:02:00, Lima**: tiempo máximo excedido. Inspección posterior de solo lectura, 11:05:20: journal **PENDING**, **14 operaciones creadas**, **0 registros**, `verifiedAckFile=false`. No se acreditaron commit, consolidación SIC, propietarios/hashes finales ni replay de ACK. No se volvió a ejecutar a ciegas ni se limpió esa evidencia.

El diagnóstico de lectura se añadió solo al código de la copia después de la versión 1; no se publicó sobre ella ni sobre producción. Fuentes, IDs, URLs y propiedades administrativas quedan en el respaldo privado.

## 5. Carpeta Drive de destino

`1.- ARCHIVOS CAMPO` pertenece a GBO; el proyecto original tiene otro propietario. Ambos recursos originales reportan acceso general de edición (`anyone/writer`), que no fue alterado. Sigue pendiente acordar y comprobar los accesos legítimos antes de restringirlos.

Las escrituras se hicieron exclusivamente en una raíz **privada de pruebas en Mi unidad de GBO**, distinta de la carpeta definitiva. No se escribieron datos de campo ni se migraron entregas reales. El propietario de la raíz de prueba está acreditado; no se extrapola a todos los archivos finales porque el commit no terminó.

## 6. Configuración Android

La candidata es la alternativa compatible con el backend actual: `drive.local.properties`, `server.local.properties` y `keystore.properties` conservan los bytes del respaldo previo. Se comparó BuildConfig Release con los valores legítimos sin exponerlos: URLs y token coinciden, Usuarios y Drive permanecen separados. V2=true; SIC17A=false; SIC17B=false.

SDK ASUS operativo mediante local.properties. Room sigue en 21; no se cambiaron AGP/Kotlin/KSP/Gradle. No se sustituyó endpoint, cuenta ni raíz. Un futuro cambio consume `DRIVE_WEB_APP_URL`/token de Drive y exige recompilar y verificar otra candidata; no sustituir `SERVER_BASE_URL` de Usuarios.

## 7. APK candidata y firma

Archivo local, **NO DISTRIBUIR**:
`C:\GBO\tmp\entrega-20261030\release-online\GBO_1.4.19_33_CANDIDATA_NO_DISTRIBUIR.apk`

- Versión **1.4.19 (33)**, package `com.tuempresa.inventariovial`, minSdk 26/targetSdk 37.
- APK única, no debug/test/split; firma v2/v3 y zipalign de 16 KiB verificados.
- Certificado SHA-256: `0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315`.
- APK SHA-256: `0aa733c794f097eefce06be3a2c1fb0ba555d45cd53702ade51b552165dc4254`.
- Identidad, configuración y manifest de fuentes: `release-online/identidad-verificada.json`, `configuracion-verificada.json`, `fuentes-candidata.json`.

No se instaló ni distribuyó. Compatibilidad de actualización comprobada estáticamente por package/firma/esquema; instalación sobre cada tablet pendiente de autorización y comprobación física.

## 8. Actualización y rollback preparados

1. Con autorización de lectura, registrar en **cada** tablet versión/código, firma/procedencia, sesión/rol, permisos, red/batería, última sincronización, cola, mensaje exacto y cantidades pendientes. No pulsar reintentos para diagnosticar.
2. Comparar revisiones local/preparada/confirmada y ACK actual; enumerar SCAP/SIC/fotos/croquis/video esperados y contrastar destino solo con acceso autorizado. Registrar capacidades realmente observadas; no deducir flags por número de versión. Flavio y Julián permanecen **sin verificar**.
3. Antes del corte, drenar y acreditar pendientes en el backend anterior o mantener esa tablet en él. No transferir paquetes preparados ni copiar reservas, bindings, manifiestos, agregados o recibos entre raíces. El aislamiento persistente por backend/raíz del outbox aún no está implementado.
4. Tras pruebas y autorización, actualizar sin desinstalar ni borrar datos, con respaldo privado verificable, y comprobar reapertura, originales y cola. No se ejecutó este paso.
5. Mantener backend y configuración anteriores. Si falla la validación, no iniciar cargas al nuevo destino. Tras escrituras reales nuevas se requiere conciliación por raíz/UUID/revisión/hash; no alternar endpoints a ciegas.
6. Android 33→32 no es un rollback ordinario seguro. Preparar reversión de código con firma histórica y **versionCode superior**, sin forzar downgrade, desinstalación o restaurar una base antigua sobre datos nuevos.

Guía administrativa concreta conservada en `tmp/entrega-20261030/backend-privado/TRANSICION_Y_ROLLBACK.md`.

## 9. Decisión y siguiente tarea técnica

**NO-GO.** El hito Android está validado localmente dentro de la selección indicada; el backend nuevo no supera la entrega grande real y la candidata no está habilitada para campo. También faltan autenticación HTTP compatible con Android, permisos finales, pruebas físicas y resolución de pendientes entre raíces.

**Próxima tarea:** instrumentar y corregir exclusivamente en el entorno aislado el coste/recuperación de `SyncV3.commit` con 59 archivos. La evidencia sitúa la interrupción durante una transacción pendiente, sin recibo guardado. Perfilar validación de staging, lecturas Drive, creación de carpetas, escritura y journal; probar interrupciones, rollback/reintento y ACK íntegro dentro del límite. No ampliar timeouts del cliente ni omitir hashes/journal para producir un PASS. Si el commit requiere reanudación por lotes, versionar y probar el contrato con Android antes del corte. No atribuir este diagnóstico al caso Flavio sin su evidencia.

## 10. Acciones del propietario

- Autorizar lectura no destructiva de ambas tablets y del destino de sus entregas para completar el checklist, sin instalación ni cargas.
- Identificar quién debe conservar acceso al proyecto/carpeta de producción; aprobar una restricción concreta solo después de verificar ese listado.
- Reservar la autorización de instalación/distribución/corte para cuando el ensayo grande, HTTP, respaldo y validación física estén cerrados. **Ahora no se solicita aprobar una candidata como apta.** No se requieren contraseñas, códigos ni tokens por chat.

No hubo push, instalación, distribución, modificación del Servicio de Usuarios ni despliegue/cambio de raíz/permisos de producción.
