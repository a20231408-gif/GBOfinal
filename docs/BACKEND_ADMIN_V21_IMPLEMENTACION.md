# Backend administrativo basado en V21 real — 02/11/2026

Rama separada codex/backend-administrativo-v21-20261102 desde a8f4003, conservado como ancestro.
Verificación OFFLINE de metadata privada: scriptId original coincide y deploymentConfig.versionNumber=21.
No se ejecutó clasp ni se contactó Google. Capturas editor/V21 solo leídas, no modificadas ni copiadas
al repositorio. La cuenta desplegadora GBO sigue pendiente de confirmación; USER_DEPLOYING no identifica
su correo. El editor y V21 difieren en PreflightReadonly, no tratar el editor como publicación.

| Referencia V21 frente al repositorio | Diferencia funcional / decisión |
|---|---|
| SyncV3.gs, VideoV1, VideoV1Core, SicObservationsV1Core | Coinciden tras normalizar finales de línea. No se alteraron. |
| SyncV3Core, PhotoNamesV2Core, VideoNamesV2Core | V21 incorpora ScapPhotoPanelV2 y SCAP_PANEL_VERSION=2, categorías oficiales y nombres de panel; local carece de esas ramas. Preservar V21 como contrato, no reemplazar por local más antiguo. |
| ScapPhotoPanelV2 y manifest | Captura remota sin equivalente en google-apps-script/. No copiar fuente privada indiscriminadamente; extracción saneada y revisión pendientes. |
| Código principal | Configuración privada y diferencias de fuente; no copiar valores ni sustituir el archivo remoto. Comparación detallada de código principal pendiente de saneamiento. |

## Código implementado, sin conectar al router original

- AdminLifecycle.js: preflight por identidad/revisión/raíz/conjunto exacto de IDs y hashes, backup
  verificable, autorización individual consultada antes de cada efecto, cierre por operationId/digest,
  cerca duradera previa a efectos, papelera/restauración por ID, readback, journal parcial e idempotencia.
  No acepta roles/verified enviados por Android. authority.authorize debe ser adaptador servidor confiable,
  revocable y ligado a finalidad/TRASH/RESTORE/digest/revisión/operationId. Si falta, no construye ejecutor.
- AppsScriptStores.gs: adaptador real candidato de JSON persistente DriveApp, lectura posterior,
  ScriptLock y detección de duplicados/corrupción. Un JSON contiene journal+cerca, sin prometer transacción
  de varios archivos. No crea carpetas. Se niega por defecto; allowMutations NO es autorización individual,
  solo interlock del adaptador, y no debe exponerse a solicitudes. Todos los escritores deberán compartir
  bloqueo/autoridad antes de activarlo. Lookup conservador recorre operaciones y falla cerrado si ilegibles.
- Adaptador candidato de papelera usa Drive.Files.update({trashed:...}) por ID, con inspector obligatorio
  de permisos/propietario/hash/referencias/raíz. Nunca invocado contra Google. No implementa purga.
- El lector paginado y conciliador offline anteriores siguen disponibles. No son inventario exhaustivo
  ni validación completa de ACK. El núcleo nuevo requiere inventory.inspect completo, sin PENDING ni
  relaciones compartidas/conflictos; no transforma el resultado parcial del lector en autorización.

Restauración administrativa devuelve archivos de papelera con autorización RESTORE vigente y comprobación
de backup/referencias; conserva la cerca. Estado RESTAURADO NO permite reenviar versiones antiguas:
reconciliar identidad/revisión nueva y levantar cerca de manera autorizada sigue pendiente. No modifica
Room, ACK V21 ni outbox. El piloto inicial debe excluir aportes SIC compartidos y vídeos pendientes.

## Dependencias NO implementadas / activación imposible todavía

No hay autoridad operativa del servicio Users ni inspector completo de Drive, verificador de backups
reales, indexador inverso exhaustivo SIC/SCAP/vídeo/legacy o adaptador de propietario efectivo. Son
dependencias obligatorias, NO constantes ficticias que autoricen. Los grants y almacenamiento en memoria
de tests son fixtures. Persistencia/API bindings se implementaron pero solo se probaron con APIs mock.
El modelo simulation.cjs del PR #6 sigue siendo simulación distinta.

assertUploadAllowed existe, pero NO está cableado a stage/commit/reservas/legacy/vídeos/recuperación V21.
Por tanto NO hay protección productiva activa contra resurrección. Antes de recuperación, incluso al
construir v3Store, toda escritura debe consultar cerca. No llamar v3Store desde inventario. No levantar
cerca al volver a una versión antigua; rollback requiere escritor compatible o pausa de todos los envíos.
No purgar archivos/journals ni asumir que ScriptLock coordina otros proyectos o cargas manuales.

## Pruebas aisladas

node --test backend/admin-v21/*.test.cjs: 13 casos del núcleo y 3 del adaptador de persistencia.
Sin documentos reales: identidad sin permiso, revocación, dos operaciones/tablets, corte después de cerca,
respuesta perdida en papelera, archivo compartido, PENDING, conflicto SCAP, otro propietario, permiso
insuficiente, plan cambiado, replay y restauración con cerca retenida. Reconstrucción del adaptador
comprueba el JSON almacenado con firmas API reales mock; no acredita cuotas o consistencia de Drive real.
No Android, nuevas APK o pruebas HTTP. Resultados exactos al cierre de ejecución local.

## Próxima lectura real: autorización separada, todavía NO ejecutada

1. Usuario autoriza explícitamente inventario privado con su cuenta personal y alcance exacto de IDs
   de raíces/registries ya existentes; verificar scriptId y no cambiar proyectos o permisos.
2. Elegir si se usa auditor YA presente en editor. Para instalar lector nuevo se necesita aprobación
   de modificación de fuente; no usar clasp push/run por esta guía. Ejecución manual autorizada es
   independiente del hecho de tener clasp conectado. OAuth scopes adicionales requieren consentimiento.
3. Registrar identidad personal del ejecutor. Enumerar registros completos paginados, repetir lectura,
   separar accesibles personalmente, institucionales por propietario/unidad, y denegados/404. Un 404
   no demuestra inexistencia ni exclusividad. No inferir que acceso Editor consume almacenamiento GBO.
4. Guardar JSON/IDs/ACL/evidencias fuera de Git. Cruzar journals COMPLETE/PENDING/ilegibles con ACK,
   bindings, reservas, ancestros, vídeos, staging y propiedades legacy. PENDING se informa, no recupera.
5. Donde falte acceso, inventario incompleto: solicitar lectura al propietario o ejecución institucional
   autorizada. No ampliar permisos ni adoptar archivos de otro dueño automáticamente.

Para prueba real de UNA ficha ficticia: fuente saneada revisada + autoridad Users operativa revocable +
inventario exacto de IDs exclusivos estable + permisos de papelera y propietario confirmado + backup
recuperable + guardas en todos los escritores y recuperación + prueba aislada autorizada + corte explícito
del original y autorización nominal de ficha/operationId. No habilitar antes esos ejecutores. La identidad
institucional será necesaria cuando sea desplegadora/propietaria autorizada o la personal no tenga acceso;
la API personal no sustituye esa autoridad ni garantiza conservar propietario/almacenamiento GBO.

**NO-GO eliminación real y publicación.** PR #5/#6 intactos; V21, ROOT, coexistencia y tablet intactos.
