# Backend administrativo: preparación concreta — 02/11/2026

Integración observada: 0071ddcb9489a06f0c33309a00c77d0b6efe7ccf. PR #6 revisado localmente y
contrastado en GitHub: abierto, borrador, SHA 4b6ab0ea00e550615874db4b499a7b493cb56e21, sin merge.
Trabajo separado en codex/backend-admin-preparacion-20261102, basado en esa propuesta, sin fusionarla.
El propietario reporta cuatro pruebas físicas PASS en 1.4.25 (39); no especificó aquí cuáles.
No inferir cinco PASS ni aceptación backend. Acceso administrativo operativo aún no confirmado.

| Función / evidencia local | Cambio exacto pendiente | Dependencia / prueba / riesgo |
|---|---|---|
| Code.gs doPost: token compartido de subida; lifecycleVersion=0; annulRecordV1/restoreRecordV1 rechazados | Mantener rechazo hasta verificar autoridad individual. Futuro dispatch administrativo separado, desactivado por defecto y sin aceptar token de subida como permiso. | Identidad legítima y autorización del propietario; cliente antiguo con token no puede borrar. No basta Session del ejecutor web. |
| InventoryAuthorization10C.js: propuesta de Users con PIN/tablet vigente, scopes por carretera, expiración y comprobación server-to-server | Implementar store real en servicio de Usuarios, revocación/rate-limit y comunicación autenticada; ampliar scopes específicos para inventariar, papelera y restaurar. Grant ligado a operationId, digest de plan, revisiones, raíces, caducidad, actor y aprobación de lote. | ANNUL/RESTORE actuales no autorizan purga. Hoy no hay adaptador desplegado acreditado. No modificar Usuarios sin autorización separada. |
| SyncV3.gs v3Store crea carpetas y recupera TODO PENDING al construirse | Separar lector sin efectos de escritor; añadir autoridad de tombstones antes de get/recuperación, stageRecordV3, commitRecordV3 y reservePhotoNamesV2. Revalidar dentro de ScriptLock. | Llamar v3Store para inventariar NO es solo lectura. La recuperación puede resucitar archivos de una eliminación. |
| SyncV3Core.js commit/identidad/sicRows/receipt; record_<hash>, binding_v4_<hash>, journal_<hash> | Resolver cierre por UUID, identityId, roadRecordId, aportes SIC, revision/contentHash, folderId/fileId y reservas. Conservar ACK históricos y nuevas marcas administrativas separadas. | BRIDGE_IDENTITY_CONFLICT no se elude cambiando UUID. Excel SIC consolidado compartido: excluir de papelera y regenerar filas solo con contrato específico validado. |
| Code.gs uploadScap/storeHumanScap/publishHumanScap, uploadPavementFile, uploadDeliveryFile y ramas legacy de doPost | Todas las entradas y sus replays deben consultar cerca antes de escrituras/respuesta idempotente; solicitud legacy sin identidad inequívoca debe rechazarse, no inferirla del nombre/ruta. Revisar recibos ScriptProperties delivery_record_ y Pavimentos, publicaciones SCAP current/pending. | Matriz de clientes históricos/capacidades reales pendiente; bloquear legacy ambiguo puede afectar ingenieros y requiere corte autorizado. |
| VideoV1.gs videoV1Action/videoV1Store; VIDEO_V1 y reservas PhotoNamesV2 | Consulta de cerca del padre y mediaId antes de begin/finish, reserva, reanudación y publicación. No usar parent:key=>v3Store().get(key) como lector inocuo. | Inventario separado de bytes/vídeos y sesiones pendientes. Un tombstone solo del UUID de la foto no protege el puente. |
| Candidato reanudable backend/candidato-original-reanudable/SyncV3.gs y SyncV3Core.gs | Llevar las mismas guardas a recuperación/checkpoints/commit y a la ruta resuelta por DeliveryRootRouting.gs/AuthorizedRootGuard.gs. Fuente original y candidata requieren revisión conjunta. | Coexistencia permanece desactivada. No cambiar ROOT ni asumir que root actual contiene todos los bindings. |
| Nuevo ReadonlyRegistry.gs: lector Apps Script real de registros técnicos, sin adaptador de escritura | Completar inventario de archivos: paginar STAGING/VIDEO_V1/revisiones, recorrer IDs de recibos, padres/ancestros/propiedad/permisos/hashes y referencias inversas en ambas raíces; cotejar dos lecturas. | Hoy solo metadatos de registry, nunca inventario suficiente para borrar. Identidad ejecutora no es autorización administrativa. |
| Simulación PR #6: estados, cerca y reintentos en memoria | Adaptador persistente del proyecto original: journal administrativo, cerca, verificación por ID, recuperación y ACK administrativo estricto; ningún handler añadido ahora. | Sin red/permiso/resultado incierto: conservar cerca y copias, PARCIAL, reconciliar antes de repetir. Nada de borrar journal para desbloquear. |

## Adaptador de lectura preparado y probado

backend/admin-removal-adapter/ReadonlyRegistry.gs usa Drive.About.get, Drive.Files.get/list y
DriveApp.getFileById(...).getBlob().getDataAsString(). No usa v3Store, getOrCreateFolder, PropertiesService,
setContent, setTrashed ni doPost. Exige executor y pares rootId/registryId explícitos; comprueba padre,
pagina hasta agotar, detecta IDs/nombres/tokens duplicados, JSON ilegible y cambios de modifiedTime
entre listados. Devuelve SIEMPRE usableForDeletion=false. No imprime ni envía evidencias.
No es otra simulación de eliminación: es código de acceso de lectura a las APIs del runtime Apps Script,
ensayado aislado en Node VM con esas mismas firmas API y SIN servicios de escritura disponibles.

Prueba ejecutada: node --test backend/admin-removal-adapter/ReadonlyRegistry.test.cjs, **6/6 PASS**.
Casos: paginación/doble lectura, ejecutor incorrecto, raíz incorrecta, token repetido, JSON ilegible y
inestabilidad. No se ejecutó en Google ni acredita permisos reales. No se repitió Android al no cambiarlo.
Las 20 pruebas del modelo PR #6 son históricas distintas; no sumar como validación del adaptador nuevo.

## Auditor existente: ejecución futura autorizada, solo lectura

1. Confirmar proyecto ORIGINAL, deployment/version V21, cuenta legítima y consentimiento OAuth
   oficial. La ejecución manual del auditor requiere autorización de lectura y acceso a registros;
   una cuenta iniciada en Google no prueba que pueda administrar ese despliegue.
2. Revisar PreflightReadonly.gs ya existente en el original; si falta o difiere, presentar diff y obtener
   autorización antes de editarlo. No desplegar ni ejecutar helpers de preparación de raíces.
3. Verificar servicio avanzado Drive habilitado y scopes existentes. Si exige agregar scopes/servicios,
   detener y pedir autorización del cambio; nunca eludir 2FA ni copiar tokens a logs/chat.
4. Ejecutar únicamente gboPilotInventoryReadonly desde editor con cuenta prevista; gboPilotDestinationReadonly
   requiere IDs privados de destino/ancestros legítimos configurados solo en la copia privada.
5. Guardar GBO_PILOT_SUMMARY/ROWS/RECORDS/BINDINGS/END y destino en evidencia privada fuera de Git.
   Conciliar total emitido con journals, COMPLETE/PENDING/ROLLED_BACK/UNKNOWN/ilegibles, duplicados,
   revisiones, bindings y ACK. Repetir lectura y comparar firmas. No reintentar ni reparar PENDING.
6. Cualquier cambio concurrente, permiso insuficiente, timeout o salida truncada invalida completitud.
   modifiedTime estable y receiptHashMatches NO equivalen a hashes de todos los archivos, ni ACK estricto,
   ni prueba de referencias exclusivas. Auditor actual no inventaría completamente ScriptProperties,
   vídeos, staging, ACL heredadas ni todas las raíces: son ampliaciones pendientes, no PASS implícitos.

## Escritura administrativa: diseño para implementar después de inventariar

Separar módulos candidatos AdminIdentity (verificador de autoridad, no rol declarado por Android),
AdminInventory (lectura completa), AdminOperationStore (journal/cerca), AdminTrashAdapter (papelera
por ID, verificación y reconciliación) y AdminAudit. Aún no existen esos adaptadores operativos.
No añadir funciones purga ni rutas públicas antes de aceptación independiente.

El plan canónico debe incluir IDs ordenados, UUID/identityId, ambas raíces permitidas, revisiones,
hashes, parentesco y metadatos de backup; digest recalculado en servidor. operationId no reutilizable
con otro digest. Grant individual verificable, audiencia/finalidad/caducidad y segunda aprobación para
lote; revocación comprobada de nuevo al reanudar. Otro propietario, referencia desconocida, ACK
inconsistente, outbox/journal pendiente o relación compartida bloquean la primera ficha piloto.

Persistir intención y cerca en UN registro autoritativo por operación, leído y verificado antes de tocar
Drive, bajo el mismo ScriptLock usado por TODOS los escritores del proyecto original. Drive no da una
transacción atómica de varios JSON: no prometer atomicidad multiarquivo. Si falla persistencia/lectura,
fallar cerrado. Índice de claves cercadas reconstruible desde operaciones; no depender solo de caché.
No atomizar el lote escribiendo un tombstone por ficha sin registro autoritativo común.

Una única autoridad debe cubrir las dos raíces ya conocidas, sin activar coexistencia ni cambiar ROOT.
ScriptLock no coordina otros proyectos, cargas externas o escritores que omitan la guarda: inventariarlos
y suspenderlos/obtener exclusión operativa antes de aceptar un corte. La segunda lectura por sí sola no
elimina carreras. Antes de cada efecto, comprobar concesión, revisión y propiedad otra vez.

Tras autorización: cercar → anotar intención por fileId → papelera de archivo exclusivo → consultar
trashed/parent/identidad por ID → persistir resultado. Respuesta perdida se reconcilia por ID, nunca
se crea un sustituto. Conservar backup y originales locales hasta ACK administrativo completo. Ningún
COMPLETADO de papelera representa purga permanente. Restaurar requiere conciliación y revisión nueva
antes de levantar cerca; purgar, si se solicita después, exige otra autorización y política de retención.
Auditar actor, operationId, digest, revisiones, estados, IDs mínimos y resultados; nunca PIN/token/fotos.

## Checklist de una única ficha ficticia, todavía NO autorizado

- [ ] Propietario confirma acceso operativo al proyecto original/Users y aprueba lectura privada.
- [ ] Inventario completo estable de ambas raíces/legacy; ficha FICTICIA inequívoca con UUID y revisión
      autorizados, sin dependencias/archivos compartidos, PENDING o vídeos incompletos.
- [ ] Backup independiente íntegro y recuperación probada en entorno permitido; lista exacta de IDs y
      propietarios, alcance de papelera y rollback revisados. Ninguna carpeta/borrado global.
- [ ] Adaptadores identidad/persistencia/cerca implementados sin registro productivo y pruebas aisladas
      de revocación/expiración, carreras dos clientes, locks, caída tras cerca y tras papelera, cuota/timeout,
      pérdida de ACK, IDs ajenos, fuente/ACL cambiantes, replay legacy/vídeo y restauración completas.
- [ ] Autorización separada para entorno aislado con permisos reales; no usar copia de ensayos archivada
      ni crear proyectos/carpeta de ensayo sin autorización específica. Este encargo no la concede.
- [ ] Antes de V21: comparar fuente desplegada con local, backup y reversión revisados. Rollback a código
      sin cerca puede resucitar registros: conservar cerca y pausar escritores; no basta elegir versión V21.
- [ ] Aprobación explícita de corte del MISMO proyecto/URL y luego autorización nominal de UNA operación
      sobre esa ficha ficticia. Conservación de datos de otras fichas, bloqueo de clientes antiguos y ACK
      administrativo verificados. Registrar incidencia y detener ante cualquier diferencia.

**NO-GO eliminación completa.** Próximo paso operativo mínimo: confirmar autoridad legítima y autorizar
el inventario de solo lectura del original. Ningún permiso real, journal productivo ni capacidad remota
se ha dado por verificado en esta fase. No cambios en Google, Android, APK, PR #5/#6 o integración.
