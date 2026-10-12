# Estado vigente — Prompt 6, 2026-10-11

Rama `codex/dashboard-sesion-6`, worktree aislado `D:/Downloads/GBO_InventarioVial_INTEGRADO_FINAL_SOURCE/GBO_InventarioVial/prompt6`, desde Prompt 5 `24df7d3562d823cc1382754b8a3950aed7f88e0e`. Checkpoint funcional local: punta de `codex/dashboard-sesion-6`, hash consultable con `git rev-parse codex/dashboard-sesion-6` y entregado después del commit. Sin push/merge. **Prompt 6 cerrado; no iniciar Prompt 7.**

Dashboard Material 3: sesión verde iniciar/configurar, identidad/fecha/sentido, cuatro accesos reales (nuevo registro, mis registros, pavimentos, entregas Drive), herramientas secundarias conservadas y resumen Drive basado en ACK/revisión/recibo. Borradores del día por fecha real de sesión; fallback documentado a creación original para históricos sin sesión, nunca última edición. Lista acotada a 360 dp, progresiva visible en lugar de UUID, orden numérico creciente/decreciente con catálogo oficial y empates estables, filtro completo fecha/ruta/calzada/tipo y continuación por UUID original. Se preservan propietario y relaciones explícitas de fichas previas; SCAP/pavimentos mantienen sus listados propios.

Fecha reutilizada de `FieldSession.startTime`: configuración aditiva sobre la tabla existente conserva identidad, operador y fecha. Ediciones de borradores históricos no inventan sesión aunque esté activa una nueva; asignación de sessionId corregida en callbacks y stores existentes. Room sigue en 18, sin migración. No cambia fotografía categorizada/photo_history, longitudes técnicas, revisiones 2B, observaciones/GPS opcional, backend/worker, autenticación ni TABLET_ADMIN.

**27 pruebas distintas aprobadas**: 17 presentación/orden, 4 Robolectric persistencia, 6 Compose. Ejecución corregida 26/27 y repetición individual del caso horizontal 1/1. `assembleDebug` aprobado. Primer fallo de compilación y ejecuciones con 4/27 y 1/27 fallos conservados en logs/XML locales, excluidos del commit; no se declaran aprobados. Suite completa no ejecutada. Tres capturas reales de Compose bajo Robolectric Native con datos ficticios, inspeccionadas visualmente, incluyen vertical, horizontal con fuente 130% y filtros históricos. [Entrega, capturas y pendientes](dashboard-6/ENTREGA.md), [matriz](dashboard-6/MATRIZ.md), [evidencia](dashboard-6/VALIDACION.json).

APK Debug local: `app/build/outputs/apk/debug/app-debug.apk`, SHA-256 `4d65f70cfbac4370c6cb3d671deee6f14003babe66be2508878ef7fdd58e030e`. Firma Debug comprobada idéntica al Prompt 5: `d2e54cb086aa731ef046be300977d0b16c0d8f66fdcb08d0d9cfd2038bf0d872`. applicationId `com.tuempresa.inventariovial`, versionCode 16, syncProtocol 3/pathVersion 4 y PhotoCategoryVersion 1 intactos. No Release, APK instalada, Apps Script publicado ni operación remota de Drive. Configuración privada copiada idéntica y excluida del checkpoint.

Pendientes: prueba física en dispositivo secundario, lector de pantalla y ejecución real de todas las herramientas; comprobación remota aislada ya pendiente del Prompt 5. No asumir que endpoint local es de pruebas. [Criterios de sesión histórica y alcance](dashboard-6/AUDITORIA_INICIO.md).

Trabajo preexistente de copia operativa y worktrees 4/5 intacto. En prompt6 quedan sin commit únicamente proguard/plantillas SCAP/herramientas copiados para compilar y logs/resultados crudos locales. Propiedades, APK y caches ignorados. Commit selectivo de fuentes, pruebas, documentación y capturas sintéticas; sin credenciales ni artefactos privados.

---

## Historial anterior; prevalece el estado vigente

# Estado vigente — Prompt 5, 2026-10-11

Rama `codex/fotografias-sic-5`, worktree aislado `D:/Downloads/GBO_InventarioVial_INTEGRADO_FINAL_SOURCE/GBO_InventarioVial/prompt5`, desde Prompt 4 `fec797c73d6eca00a009a3e53752b40276ecad35`. Incluye 2A/2B sobre la misma infraestructura V3. El checkpoint local de entrega es HEAD de esta rama (`git log -1`); hash exacto entregado después del commit. Sin push ni merge.

Implementado: sección FOTOGRAFÍAS con N imágenes por categoría, cámara, importación múltiple, contador, miniaturas/ampliación, reemplazo y retirada confirmada. INICIO/FIN/OBSERVACION en modelos longitudinales correspondientes y entrada/salida física del badén; PANORAMICA/ELEMENTO en SIC-22; claves CULVERT_* y UUID 18/18A conservados. SIC-08/09 y puntos mantienen GENERAL. Fotos históricas GENERAL/null visibles, sin reasignación automática. [Matriz completa](fotografias-5/MATRIZ.md).

Foto nueva con UUID estable, categoría/orden/capturedAt/metadatos propios y estado pendiente; reemplazo conserva UUID, índice y fileId. Cambio de categoría explícito conserva identidad. Room 17 → 18 agrega solamente photo_history, sin transformar tablas/valores originales, para conservar vínculos de originales, categoría previa, acciones y revisiones. Borradores guardan relación de reemplazo. No se eliminan archivos originales. Galería técnica SIC del puente excluye imágenes SCAP; cada inspección mantiene sus categorías existentes y paquete/fotos bajo SCAP/<inspectionUUID> dentro de PUENTE N.

V3/pathVersion 4, cola sin GPS, UUID/folderId/numeración, revisión base, conflictos, ACK verificable, staging y consolidación completa del servidor conservados. Categorías/contexto firmados mediante photoCategoryVersion=1; backend valida carpeta y nombre a partir del código/UUID. Reintento por la misma clave mantiene fileId; cambios y retiradas se archivan mediante el mecanismo 2B existente. Metadata/manifest conserva la relación foto/registro/SIC/calzada/categoría/orden/revisión/clave; recibos aportan IDs actuales. No se cambian columnas contractuales ni se generan rutas SIB nuevas.

**25 casos Android distintos y 19 backend locales aprobados**, sin suite completa. Android: 19 nuevos de categoría/persistencia/migración/preparación, 2 interfaz retrato/paisaje, 3 de revisiones/ACK 2B y 1 regresión de originales/exportación SCAP. Última selección: 4/4 aprobadas + assembleDebug. Backend: primera ejecución 18/2 fallos, 16 aprobadas; última selección 3/3 (dos corregidas y un borde nuevo), total 19 distintas aprobadas. Fallos iniciales y logs/resultados locales conservados: dos errores de compilación de adaptación UI; fixture SIC-22 inválida corregida a R-1 del catálogo; dos aserciones backend corregidas para permitir el registro interno V3 y exigir ausencia de escrituras técnicas. [Evidencia sanitizada](fotografias-5/VALIDACION.json), [entrega](fotografias-5/ENTREGA.md).

APK Debug compilada con propiedades reales copiadas idénticas y sin gboValidationBuild=true; firma Debug SHA256 `d2e54cb086aa731ef046be300977d0b16c0d8f66fdcb08d0d9cfd2038bf0d872`, igual a Prompt 4. applicationId com.tuempresa.inventariovial/versionCode 16/firma histórica conservados. No APK instalada; no publicación Apps Script, escritura remota ni cambio del servicio de usuarios. Propiedades, tokens, keystores, APK, caches, logs y resultados crudos excluidos del checkpoint.

Pendiente: responsable debe actualizar SOLO el proyecto Apps Script aislado con fuentes integradas Code.gs + SyncV3Core.js (como .gs) + SyncV3.gs. GET esperado: protocolo 3, jerarquía 4, stableFamilyNumbering=true, technicalDeliveryWithoutGps=true, photoCategoryVersion=1 y raíz aislada autorizada 1FH56ZlgRRZzIrfMpL35EbjyX5HsZuzyk. Último estado remoto conocido de encargos anteriores era pathVersion 3; no se volvió a consultar ni se acredita subida real de esta entrega. Cliente deja cola UNCONFIGURED si faltan capacidades. [Despliegue y checklist físico/remoto](fotografias-5/DESPLIEGUE_PRUEBAS.md).

Validación pendiente en tablet secundaria: cámara/importación real/reinicio, migración con respaldo, IDs/JPEG/Excel/metadata reales en Drive, archivos históricos/SCAP por inspección, reintentos y dos operadores. Categorías históricas desconocidas se conservan y pueden necesitar reclasificación explícita para reenviar. No hay migración remota automática ni resolución automática de conflictos.

Trabajo anterior preservado en Prompt 4 y copia backend-v3-pruebas, incluidos SCAP, proguard, Drive, pavimentos, herramientas y modificaciones sin commit. En prompt5 quedan sin commit solo recursos auxiliares copiados para compilar y logs/resultados crudos. Checkpoint selectivo de fuentes/esquema/pruebas/fixtures sintéticas/documentación. **Detenerse al terminar Prompt 5; Prompt 6 no iniciado.**

---

## Historial anterior; prevalece el estado vigente

# Estado vigente — Prompt 4, 2026-10-11

Rama `codex/correcciones-tecnicas-sic-4` en worktree aislado `D:/Downloads/GBO_InventarioVial_INTEGRADO_FINAL_SOURCE/GBO_InventarioVial/prompt4`, basada en Prompt 3 `bbbf7598598b01624e421836923d39e9a3d1f326` (rama anterior subida a GitHub y main intacta). El commit funcional local es HEAD de esta rama, consultable con `git log -1`; el hash exacto se entrega al usuario después del commit. Sin push de Prompt 4 ni merge.

Implementado: función SIC-18A siempre antes de condiciones e independiente; fallas para REGULAR/MALA, selección persistente y editable aun tras completar; BUENA conserva antecedentes y exportación del detalle en el mismo UUID. Túnel solo S/Sin objeto, muro I/D, con validaciones equivalentes y preservación de lados históricos no editados. Acero (1) para guardavía nuevo y Concreto (2) al seleccionar resalto nuevo; históricos y materiales elegidos se conservan.

Longitudes por progresivas completas: diferencia de offsets del mismo PR o `RoadPr.chainageM + offset` en catálogo de la misma ruta/calzada. No convertir diferencias de códigos PR en kilómetros. Sentido decreciente produce metros positivos; datos insuficientes/equivalencia ausente/sentido contradictorio devuelven pendiente y advertencia. Muros AUTO se recalculan al cambiar progresivas; modo medido/histórico y valores anteriores se conservan. Modo persistido en tabla de extensión existente, sin migración Room. Cunetas, guardavías, tachas por tramo y badenes muestran cálculo; no se reinterpretan anchos/dimensiones como longitud.

Las plantillas oficiales actuales no tienen columna longitudinal para esas familias: Excel complementario por UUID y metadata V3 conservan longitud calculada, medida/histórica y aplicable, origen/unidad/advertencias. La clave `technical-length` usa el mecanismo V3 existente de archivos/fileId, revisión y ACK. No se cambia el consolidado ni sus columnas. La revisión local del backend confirma archivos genéricos y actualización por clave/fileId; Apps Script no modificado.

**51 casos Android distintos aprobados**, 18 nuevos y 33 regresiones focalizadas. Última selección afectada: 16/16 aprobadas y `assembleDebug` exitoso. Primer resultado: 38 casos/1 fallo (complemento Excel con menos de nueve columnas para cabecera corporativa), corregido incluyendo ubicación completa; segundo: 29 casos/1 fallo (fixture GPS antigua sin procedencia explícita del Prompt 3), reparado solo en fixture. Resultados iniciales y logs locales conservados, sin suite completa ni pruebas backend/remotas. [Matriz y entrega](correcciones-tecnicas-4/ENTREGA.md), [evidencia resumida](correcciones-tecnicas-4/VALIDACION.json).

Firma Debug verificada idéntica a Prompt 3: `d2e54cb086aa731ef046be300977d0b16c0d8f66fdcb08d0d9cfd2038bf0d872`. applicationId `com.tuempresa.inventariovial`, versionCode 16, Room 17, syncProtocol 3/pathVersion 4 conservados. Configuraciones Drive/usuarios copiadas idénticas, no modificadas ni incluidas en Git. No APK instalada ni despliegue de Apps Script.

Pendientes: validación física en dispositivo de pruebas; prueba real de archivos/IDs/Excel/reintentos contra backend aislado compatible pathVersion 4 (última observación histórica del endpoint: pathVersion 3). Ingeniero debe revisar catálogo de PR en rutas sin equivalencia, el uso del complemento de longitud y la denominación contractual SIC-18A «condición mala» que se conserva en encabezados oficiales. No se declara verificada una subida real de Prompt 4.

Trabajo previo protegido: copia operativa y worktree backend-v3-pruebas sin cambios; permanecen allí sus 10 archivos rastreados modificados y 38 entradas no rastreadas. En el worktree Prompt 4 quedan sin commit recursos SCAP/proguard/herramientas copiados para compilar, logs/resultados crudos y seis JSON 2B regenerados por regresiones. No se atribuyen al Prompt 4 ni se descartan. Checkpoint selectivo excluye tokens, propiedades locales, keystores, APK, caches y logs privados. Detenerse al completar Prompt 4: no iniciar fotografías, dashboard ni navegación; no producción, servicio de usuarios, nueva firma ni siguiente prompt sin instrucciones.

---

## Historial anterior; prevalece el estado vigente

# Estado histórico — Prompt 3, 2026-10-11

Rama `codex/formularios-sic-3`, copia aislada backend-v3-pruebas, desde 2B `433b958831aa58f935fc5ff7941ab31a435911f5`. Checkpoint local consultable con `git log -1`; sin push, merge ni cambio de copia operativa.

Capturas SIC simplificadas: sin recorrido/GPS obligatorio, una sección opcional plegable, entrada manual sin certificación GNSS, coordenadas null y preservación histórica. PR hasta cuatro dígitos y distancia hasta tres cifras enteras, rechazo rojo inmediato y sin recortes; decimales conservados. OBSERVACIONES finales multilinea y persistentes en el mismo UUID y editores/borradores; contratos Excel intactos. SCAP solo adopta límites en su bloque UBICACIÓN SIC; dimensiones/CHAINAGE, pavimentos, PCI/URCI, mapa/KMZ y orden/numeración permanecen. Room 17 y Apps Script sin cambios. Ruta/calzada siguen heredadas de sesión en nuevas fichas; históricos conservan las propias.

Aprobadas **44 pruebas Android focalizadas** (18 nuevas + 26 de regresión) y **assembleDebug**, 0 fallos/omisiones. Última selección afectada: 27 aprobadas + assembleDebug. Dos fallos intermedios de aislamiento Application/WorkManager en Robolectric conservados, corregidos en fixtures sin cambiar servicios productivos. Sin suite completa ni pruebas backend nuevas. Primeras 31 aprobadas y compilación inicialmente fallida conservadas localmente. [Evidencia y SHA APK](formularios-sic-3/VALIDACION.json), [entrega](formularios-sic-3/ENTREGA.md), [matriz de 28 casos y checklist](formularios-sic-3/MATRIZ_SIC.md). Firma Debug comprobada idéntica; applicationId/versionCode 16 sin cambios. No APK instalada.

Pendientes: prueba física completa de las familias y panel/observaciones; subida real del esquema V3/pathVersion 4 con IDs/archivos verificados. No se ejecutó verificación remota ni publicación Apps Script ni uso de Drive productivo en Prompt 3. Protocolo de entrega 3/jerarquía 4 y control de revisiones 2B conservados; endpoint aislado conocido aún pathVersion 3. Servicio de usuarios y server.local.properties conservados.

Sin commit quedan avances preexistentes SCAP/proguard/herramientas/resultados, logs locales y diez JSON regenerados por regresiones bajo docs/edicion-sincronizados-2b (originales protegidos en 433b958). No se descartaron. Checkpoint excluye propiedades privadas, tokens, keystores, APK/caches/logs crudos. Detener cambios para revisión; Prompt 4 NO iniciado. No cambiar SIC-18A técnico, cálculos, materiales, fotografías, dashboard, navegación, Excel ni backend.

---

## Historial anterior; prevalece el estado vigente

# Estado vigente — Prompt 2B, 2026-10-11

Rama `codex/edicion-sincronizados-2b` en copia aislada backend-v3-pruebas, desde 2A `99b15aae88b2e69cb28a4715c87acb854fe6e12b`. Checkpoint funcional consultable con `git log -1`. Sin push ni merge; copia operativa y modificaciones anteriores preservadas.

Edición sincronizada implementada sobre la misma cola V3: UUID/folderId/fileIds estables, revisión/base confirmada, actualización pendiente automática sin GPS, protección de formularios/borradores obsoletos, ACK tardío/respuesta perdida, conflictos explícitos sin sobrescritura ni reintento forzado. Excel se consolida desde el registro completo del servidor. Foto recategorizada conserva fileId; altas, reemplazos y retiradas conservan originales/historial. Sin SIB nuevo ni cambio de numeración por PR. syncProtocol 3/pathVersion 4. Estados de entrega técnica y georreferenciación externa separados.

Aprobadas: 17 Android (10 de 2B + 7 sin GPS), 8 backend locales y assembleDebug. Sin suite completa; resultados iniciales fallidos y finales conservados. Evidencia y SHA de APK: [VALIDACION.json](edicion-sincronizados-2b/VALIDACION.json); [informe](edicion-sincronizados-2b/ENTREGA.md). Los formularios conservan la revisión abierta durante ACKs de fondo; la verificación física de esa interacción sigue pendiente. El caso final comprueba que un rechazo remoto durante otro guardado mantiene la edición más reciente en CONFLICT.

GET remoto consultado: raíz aislada `1FH56ZlgRRZzIrfMpL35EbjyX5HsZuzyk`, syncProtocol=3/pathVersion=3, sin stableFamilyNumbering. No hubo escrituras remotas 2B. Pendiente que el responsable despliegue deployment-test/ SOLO en proyecto de pruebas y ejecute el checklist real; hasta protocolo de carpetas compatible la cola permanece UNCONFIGURED. Nunca tocar producción, servicio de usuarios ni tablet de campo. Firma/applicationId/versionCode 16 sin cambios.

Conflictos quedan en CONFLICT con edición local conservada, base antigua y versión remota informada. Reintento deshabilitado y nuevos guardados no borran conflicto. La conciliación requiere respaldo y revisión humana: no se implementó merge automático/importador remoto. Migración histórica y cambio de ruta/familia no se ejecutan automáticamente. No comenzar otra tarea sin instrucciones.

El checkpoint excluye credenciales, propiedades locales, keystores, APK, caches, logs y temporales. Archivos preexistentes de SCAP/proguard/herramientas/resultados V3 y logs 2A/2B se mantienen sin commit, separados de la entrega. Configuración del servicio de usuarios idéntica a la copia operativa.

---

## Historial previo; prevalece el estado vigente de arriba

# Estado vigente — Prompt 2A, 2026-10-11

Rama: `codex/drive-familias-2a` en worktree aislado backend-v3-pruebas. Base: `2bac1be62c29cd64c15f54b853912b5306f58d26`; hash de checkpoint funcional consultable con `git log -1`. No se subió esta rama ni se mezcló con main. Copia operativa y cambios anteriores conservados.

Implementación local completa: nuevas rutas por familia sin SIB, UUID/número remoto durable bajo ScriptLock, categorías existentes con N fotos, Excel por ruta/SIC en EXCEL_SIC, SIC-08/09 independientes, paquetes SCAP individuales, pavimentos y familias secundarias incluidos en orden/mapa/KMZ. Orden numérico separado de identidad y GPS; sin geometría no se inventa un punto. syncProtocol 3, pathVersion 4. Historial SIB y columnas Room intactos; migración preparada, no ejecutada.

Aprobadas: 15 Android (8 nuevas + 7 sin GPS) y 15 backend locales; assembleDebug con configuración real de pruebas, sin flag validation. 39 Android/60 backend del Prompt 1 son resultados históricos reportados, no repetidos. Logs iniciales fallidos y resultados finales conservados localmente. Ver [entrega y pendientes](drive-familias-2a/ENTREGA.md) y [VALIDACION.json](drive-familias-2a/VALIDACION.json).

Actualización del estado remoto histórico: Prompt 1 sí comprobó una subida real SIC-08 sin GPS, fotos y Excel con descarga/IDs y reintento estable bajo SIB-02. Raíz aislada verificada: `1FH56ZlgRRZzIrfMpL35EbjyX5HsZuzyk` (GBO_TEST_V3_20261011). No confundir con el endpoint operativo antiguo ni con PRUEBA_FINAL. Esa verificación no acredita el esquema 4. Backend de pruebas pendiente de desplegar el paquete compatible pathVersion 4; Android conserva cola UNCONFIGURED hasta entonces. Ninguna escritura remota o despliegue en 2A.

Pendientes: publicación por responsable exclusivamente en proyecto de pruebas, GET/capacidades y diagnóstico seguro del token, prueba real mínima de nuevas familias/IDs/Excel/reintento y prueba física secundaria. Restricciones: no producción, no servicio de usuarios, no APK sobre tablet de campo, no firma/applicationId/versionCode cambiados, no migración ni borrado automático, no suite completa. Siguiente tarea funcional: Prompt 2B edición segura de sincronizados, todavía NO iniciado.

Sin commit se conservan los archivos preexistentes de proguard, SCAP templates/maps y herramientas/resultados V3, más logs locales 2A. No incluirlos en una entrega 2A por atribución automática. Credenciales/configuraciones privadas y APK/cachés excluidos.

---

## Historial de puntos de control anteriores

Los estados siguientes son históricos; para decisiones actuales prevalece el estado vigente de arriba.

# GBO Inventario Vial — punto de control

Fecha: 2026-10-11 (America/Lima)

## Continuación: entrega técnica sin GPS

La rama activa es ahora `codex/sincronizacion-sin-gps`, creada desde el checkpoint `85d0659`. El checkpoint anterior (`24e1830`, `d4e491d`, `85d0659`) conserva los cambios SIC-08/SIC-09 y el avance mezclado previo; no se descartó contenido.

La tarea posterior permite entrega técnica sin coordenadas, recupera exclusivamente bloqueos GPS antiguos, excluye borradores y confirma recibos/IDs antes de sincronizar. Validación final: 39 pruebas Android focalizadas, assembleDebug y 60 pruebas locales del backend aprobadas. Las ejecuciones inicialmente fallidas también se conservaron. No se ejecutó la suite completa ni se volvió a validar físicamente la tablet.

Commit de entrega Prompt 1: **`ae58e7346e1cef5dd0ce5e61a3adc7dc4fa108bd`**, en la rama **`codex/sincronizacion-sin-gps`**, con asunto `feat(sync): entrega tecnica automatica sin GPS y pruebas del Prompt 1`. Incluye 29 archivos revisados: AutomaticDelivery/AutomaticSync, una línea de estado en el detalle existente, InventoryDao, SicFieldRepository, pruebas focalizadas, documentación, fixtures sintéticas y las fuentes completas del backend Drive necesarias para V3. El backend contenía implementación previa no registrada: se incorpora como base requerida, sin atribuir todas sus funcionalidades al Prompt 1. Los demás archivos anteriores se conservan fuera del commit. `buildSrc/src` sigue registrado en Git; configuración privada, applicationId y firma histórica sin cambios.

Se conservan los resultados reportados de 39 pruebas Android y 60 pruebas backend; no se volvieron a ejecutar en esta preparación Git. La evidencia resumida sin datos privados se incluye en `sync-sin-gps-2026-10-11/VALIDACION.json`; APKs, caches, logs crudos y temporales permanecen locales. Las fixtures incluidas son sintéticas, con coordenadas nulas y fotografías generadas por las pruebas.

Siguiente tarea: **edición segura de registros sincronizados** (Prompt 2). No se inicia en esta preparación. El responsable autorizó `origin=https://github.com/a20231408-gif/GBOfinal.git`. La entrega se publica exclusivamente en `codex/sincronizacion-sin-gps`, sin merge a main ni force push. La rama no existía en origin antes de la publicación; main tenía el hash `c42bb8158da6f24ff9cedbee7e4858fe104d1e31`. Comprobar que `git ls-remote origin refs/heads/codex/sincronizacion-sin-gps` coincide con el HEAD local y que main conserva ese hash.

Antes del commit se comprobó `git diff --cached --check` sin errores y una lista explícita de 29 archivos, sin propiedades locales, keystores, APK, caches, logs crudos ni temporales. Se revisaron 636 archivos del índice y 709 blobs del índice/historial, comparando patrones de secretos y los valores secretos locales sin exponerlos: cero coincidencias. Se revisó también contenido XML de las fixtures Excel y archivos embebidos en payloads. Los tokens del código entregado son marcadores o valores ficticios de pruebas; no se incluyeron credenciales reales. Un commit documental posterior registra este hash de entrega, sin modificar funcionalidades.

El endpoint local observado responde v6.2 sin protocolo 3. PRUEBA_FINAL existe, pero su separación de la raíz productiva no está confirmada. No hubo subidas ni despliegues. La prueba remota queda pendiente de carpeta/proyecto/URL aislados y autorización del responsable para crearlos/publicarlos. No cambiar servicio de usuarios.

Informe detallado, resultados, paquete y pasos para tablet: [entrega sin GPS](sync-sin-gps-2026-10-11/ENTREGA.md) y [despliegue de pruebas](sync-sin-gps-2026-10-11/DESPLIEGUE_PRUEBAS.md). Estado histórico del checkpoint a continuación; sus restricciones de no modificar funcionalidades correspondían al punto de control anterior, antes de que se autorizara esta tarea.

## Estado protegido

- Rama creada para este punto de recuperación: `codex/punto-control-2026-10-11`.
- Rama de trabajo anterior: `codex/sic08-sic09-captura-independiente`.
- Base común observada: commit `66610e8` (`Baseline local: Release 15 y correcciones previas`).
- No había cambios staged al iniciar este control. No se hizo `reset`, `clean`, `add -f` ni se eliminaron archivos.
- `git diff --stat` al inicio: 67 archivos modificados, 400 inserciones y 1.445 eliminaciones. Hay además archivos nuevos y artefactos de trabajo que aparecen en `git status --untracked-files=all`.

La rama tendrá un commit de recuperación selectivo para el código fuente, pruebas y este handoff. El contenido mezclado y los artefactos que requieren revisión individual quedarán sin commit y se conservarán en el working tree.

## Cambios terminados

Según las pruebas focalizadas conservadas en `docs/sic08-sic09-fix-2026-10-11/` y la validación física informada por el equipo:

- SIC-08 y SIC-09 tienen pantallas, selección, validación, guardado, reapertura y edición independientes.
- Guardado local sin GPS, ubicación apagada, permisos de ubicación denegados y sin Internet.
- Observaciones multilínea opcionales, persistentes y editables.
- Coordenadas ausentes permanecen nulas; los datos GNSS, fotografías, trayectorias e identificadores históricos se conservan.
- Ruta y calzada se heredan de la sesión para fichas nuevas sin sobrescribir datos propios de fichas existentes.
- Exportaciones SIC-08/SIC-09 mantienen sus columnas contractuales; las observaciones se entregan mediante el complemento documentado y metadata.
- La migración Room 16→17 y las migraciones históricas seleccionadas pasaron en las pruebas focalizadas.
- `assembleDebug` terminó correctamente y el artefacto se conserva en `app/build/outputs/apk/debug/app-debug.apk`.

La validación física en tablet se registra aquí como resultado comunicado por el usuario; este control no la repitió.

## Cambios preexistentes o mezclados

No se atribuyen todos los cambios del working tree a SIC-08/SIC-09.

- Drive y entrega: `DriveStatusPanel.kt`, `DriveUploadWorker.kt`, `core/DriveAdapters.kt`, `core/DriveSyncCenter.kt`, `delivery/DeliverySync.kt`, `RouteDelivery.kt` y archivos relacionados.
- SCAP: catálogos en `app/src/main/assets/scap_*.json`, repositorio, mapeadores, validación, exportación, sincronización y UI bajo `app/src/main/java/com/tuempresa/inventariovial/scap/`.
- Pavimentos: archivos bajo `pavement/` y sus pruebas.
- SIC-08/SIC-09 y persistencia común: `sic/`, `RecordHistoryScreen.kt`, `data/database/InventoryDatabase.kt`, `data/entity/InventoryRecordEntity.kt`, `data/dao/InventoryDao.kt`, exportaciones, entrega y consumidores de coordenadas opcionales.
- Otros cambios existentes: autenticación, diagnóstico, repositorios generales, pruebas de navegación/flujo y documentos de iteraciones anteriores.

Los archivos nuevos principales de la corrección incluyen `sic/FajaCaptureForm.kt`, `core/AutomaticDelivery.kt`, `core/AutomaticSync*.kt`, `export/SicConsolidatedWorkbook.kt`, `RoomTestResources.kt`, `Sic0809CaptureTest.kt`, `Sic0809ScreenTest.kt` y los esquemas Room 16/17. El resto de archivos nuevos listados por Git incluye documentación, backend, herramientas, logs y artefactos de empaquetado preexistentes; deben revisarse por separado antes de preparar un commit.

## Pruebas aprobadas y pendientes

Las pruebas focalizadas conservadas fueron 14/14 aprobadas: 7 casos de datos SIC-08/SIC-09, 4 casos de pantalla y 3 pruebas de migración histórica. No se ejecutó la suite completa por instrucción del usuario.

Pendientes:

- No hay Release firmado generado en este punto de control.
- No se repitió la validación física, Drive real ni la suite completa.
- Debe separarse el contenido mezclado antes de cualquier commit funcional.
- Deben revisarse y clasificar los artefactos no fuente (`app/release/`, logs, `docs/`, `backend/`, `tools/`) antes de agregarlos.

## Git, buildSrc y configuración privada

- Al iniciar este control, `buildSrc/src` **no estaba registrado en Git** y su archivo estaba ignorado por `.gitignore` mediante `**/build/`. Se incorporó explícitamente al checkpoint como `buildSrc/src/main/java/com/gbo/build/ValidateReleaseConfigurationTask.java`, sin cambiar su contenido funcional.
- No había archivos staged. Por ello, la revisión de “preparado para Git” aplica al working tree candidato, no a un índice ya preparado.
- `drive.local.properties` y `server.local.properties` existen localmente y están ignorados por `.gitignore`. `keystore.properties` no apareció como archivo local; solo existe `keystore.properties.example`.
- No se encontraron archivos `.jks` ni `.keystore` en la búsqueda realizada.
- Los archivos de ejemplo contienen nombres de claves con valores de ejemplo vacíos o marcadores; no contienen credenciales reales según la inspección por nombres de clave y valores redactados.
- La búsqueda de patrones de tokens, contraseñas, claves privadas y configuraciones locales no encontró un secreto operativo en los archivos candidatos. Las coincidencias restantes son nombres de configuración, validaciones de prueba, documentación de ejemplo y código que lee `BuildConfig`; no se deben convertir en secretos al hacer stage.

## Restricciones para continuar

- Conservar esta rama y el working tree hasta clasificar cambios por tarea.
- No ejecutar suite completa ni modificar funcionalidades en este punto de control.
- No agregar credenciales, propiedades locales, keystores, APKs de Release ni artefactos generados sin revisión explícita.
- No cambiar la firma histórica ni `applicationId` (`com.tuempresa.inventariovial`).
- El último Release instalado usa versionCode 15; el código preparado usa versionCode 16. El siguiente versionCode esperado después de 16 es 17.
- No atribuir automáticamente los cambios de Drive, SCAP, pavimentos o iteraciones anteriores a SIC-08/SIC-09.

## Comprobación de seguridad

El estado queda listo para continuar con seguridad a nivel de recuperación Git: rama creada, commit selectivo de recuperación, cambios restantes conservados y handoff documentado. Antes de publicar o fusionar hay que separar tareas, revisar el diff de cada grupo y preparar commits selectivos sin archivos privados ni generados.
