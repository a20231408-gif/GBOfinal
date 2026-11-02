# Punto de control previo a lectura real — 02/11/2026

Revisión OFFLINE de capturas privadas, sin Google/clasp/run/push. HEAD inicial fa6a72e.
Las capturas originales no se modificaron. Prueba auxiliar guardada fuera de Git en la carpeta
privada clasp. Dos casos funcionales PASS: cuenta personal rechazada antes de raíz; cuenta GBO
ficticia obtiene resumen vacío con servicios mock exclusivamente de lectura y doble paginación.
Esto NO acredita permisos ni resultados reales. Un primer ensayo de carga bloqueó acceso a servicios;
mostró la inicialización de lectura de propiedad descrita abajo. No se leyó el token real.

## Auditor existente exacto

gboPilotInventoryReadonly() en PreflightReadonly del EDITOR de GBO_Inventario, scriptId original
verificado previamente. No está en la captura publicada V21. No es endpoint HTTP ni parte de V21.
Requiere executor reconocimientoinventariovial@gmail.com; personal produce PREFLIGHT_WRONG_EXECUTOR.
Que el despliegue web ejecute como desplegador no convierte una ejecución manual personal en GBO.

El cuerpo usa Drive.About.get, Drive.Files.get/list, DriveApp.getFolderById/getFoldersByName,
iteradores, getFileById/getBlob/getDataAsString, JSON y console.log. No llama v3Store, recuperación,
createFile/createFolder, setters, setTrashed, escrituras de propiedades ni HTTP externo.
Si falta registro técnico, falla; no lo crea. Las capacidades canEdit/canAddChildren son datos leídos,
no permisos que el auditor utilice para escribir.

**Dependencia global detectada:** al cargar Código del editor, el proyecto consulta
PropertiesService.getScriptProperties().getProperty('GBO_DRIVE_API_TOKEN'). No escribe ni muestra el
valor, pero invocar el auditor ejecuta esa inicialización del proyecto. La prueba offline permitió
solo ese getter mock para continuar; no se invocó Google ni se conoció el valor real.
Manifest editor/V21 no declara oauthScopes; contiene servicio avanzado Drive v3. La plataforma infiere
scopes del proyecto completo, que también contiene funciones de escritura. No se puede certificar que
el consentimiento del proyecto pida exclusivamente lectura. No cambiar manifest/scopes por este encargo.

## Alcance de la función y faltantes

| Datos | Auditor existente | Ampliación necesaria |
|---|---|---|
| Journals | Todos los hijos no trashed del registro técnico, paginados dos veces; COMPLETE/PENDING/ROLLED_BACK/otros, UNKNOWN, ilegibles y avisos | No cubre objetos técnicos fuera de ese registro ni demuestra snapshot transaccional. |
| ACK/revisión/UUID | Metadatos de record/receipt y comparación contentHash/revisión; identityId y key | No valida ACK íntegro ni hashes de bytes de archivos. |
| Identidad/binding | binding y binding_v4, folderId, scope, logicalPath, duplicados por nombre | No cierra todas las relaciones SIC/SCAP, filas compartidas o autoridades entre raíces. |
| Archivos | Para journal no completo sin key, consulta descripción de hasta tres operaciones para inferir key | No inventaría todos los fileId, backups, staging o vídeos. |
| Reservas | Cuenta nombres photo_names_v2 | No lee su contenido ni inventaría reservas de vídeos. |
| Permisos/propietarios | Capacidades de raíz visibles al ejecutor | No enumera propietarios/ACL/ancestros. Destino tiene otra función separada, también bloqueada a personal. |
| Prueba/real | No hay clasificación autorizada | Nombres como prueba no autorizan retiro. Lista explícita de UUID/revisión del propietario, sin inferir. |

El auditor solo lee ROOT_FOLDER_ID antiguo y ._GBO_SYNC_V3, más IDs de operaciones descritos.
No recorre ambas raíces. Segunda raíz/ancestros/ACL requieren autorización de alcance independiente;
no activan coexistencia. ACL puede no ser completamente visible para un editor. Denegado/404 no
significa inexistente; registrar NO CONSULTABLE y no dar por completo el inventario.

## Ejecución recomendada a autorizar (preparada localmente, NO ejecutada)

Lectura externa local por API Drive v3, con identidad OAuth personal legítima, sin ejecutar función
Apps Script. Rutina local inventoryOriginalReadonly() preparada en inventario-personal-readonly.cjs fuera de Git, sintaxis validada con node --check: About.get para
confirmar cuenta; Files.get del ROOT antiguo ya configurado, localizar exclusivamente el registro
existente por Files.list paginado; leer JSON record/journal/binding/reservas por IDs; repetir listados
y conciliar offline. No ejecutar gboPilotInventoryReadonly ni inicialización de Código. No pedir ni usar
token de subida. Ninguna operación write/create/update/delete ni recorrido de nuevo Drive autorizado.

OAuth: requiere Drive de solo lectura (drive.readonly para contenido JSON; metadata.readonly no basta).
Los grants actuales de clasp deben comprobarse sin imprimir credenciales. Si son insuficientes,
detener y pedir consentimiento oficial adicional; no ampliar permisos automáticamente. Esta vía no
garantiza que la cuenta personal pueda ver los archivos institucionales. En ese caso solo el propietario
puede conceder lectura específica o autorizar ejecución institucional; no cambiar permisos aquí.

Alternativa institucional: ejecutar exactamente gboPilotInventoryReadonly() manualmente en editor
original como GBO tras consentimiento/2FA y autorización explícita, aceptando inicialización y scopes
del proyecto. Sin clasp run/deploy. No está disponible como ejecución personal sin modificar fuente.

## Evidencia privada y aceptación

La ejecución produciría cuenta ejecutora, nombres de carpetas, UUID, revisiones, IDs Drive, bindings,
ACK resumidos, errores y, si se aprueba su ampliación, propietarios/ACL. Datos sensibles, no GitHub.
Antes de leer: crear carpeta privada nueva fuera de C:\GBO y sincronización de terceros, ACL propietario
y SYSTEM. Guardar JSON originales/logs allí, SHA-256 y resumen agregado saneado; no tokens, cookies,
PIN ni fotos en informes compartidos. No volcar salida del auditor a chat. Console.log del auditor
institucional deja además evidencia sensible en logs de ejecución de Google: no es salida solo local.
Guardar páginas completas y marcador final; truncamiento/timeout/inestabilidad invalida completitud.

No se efectuó ninguna ejecución contra Google. La autorización siguiente debe indicar vía, cuenta,
raíz antigua y alcance. No habilita escritura, corrección de journals, cambios de permisos/ROOT,
tombstones, segunda raíz, eliminación o publicación. NO-GO productivo y de borrado permanece.
