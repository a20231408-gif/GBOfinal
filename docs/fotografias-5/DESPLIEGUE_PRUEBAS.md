# Paquete integrado de pruebas — fotografías V3

No se ha publicado ningún servicio ni se ha modificado Drive remoto en el Prompt 5. Se conservan syncProtocol=3, pathVersion=4, numeración por familia y conciliación de revisiones 2B. La prueba real está pendiente.

El responsable debe revisar y copiar **las fuentes integradas de esta rama**, no el paquete documental antiguo de 2B:

- `google-apps-script/Code.gs`: GET anuncia `photoCategoryVersion=1`.
- `google-apps-script/SyncV3Core.js`, guardar como `SyncV3Core.gs` en el editor Apps Script: valida categoría por familia/modelo, deriva el destino y exige coincidencia exacta con la ruta/UUID. Incluye categorías y contexto en el hash canónico.
- `google-apps-script/SyncV3.gs`: fuente V3 existente de staging, revisión base, ScriptLock, confirmaciones, actualización por fileId, archivado y consolidación de todas las filas. Se incorpora al despliegue integrado aunque no requiere cambios de código en este prompt.

El despliegue anterior debe conservarse para recuperación. Ejecutar estos pasos exclusivamente en el proyecto independiente de pruebas, después de autorización del responsable:

1. Verificar proyecto, URL `/exec` y raíz aislada autorizada `1FH56ZlgRRZzIrfMpL35EbjyX5HsZuzyk`. No usar raíz, URL ni token operativos. No alterar el proyecto original ni el servicio de usuarios.
2. Respaldar fuentes/versión actuales del proyecto de pruebas. Copiar el paquete integrado; conservar las propiedades privadas y la configuración específica de ese proyecto. Revisar que la raíz siga siendo la aislada; no copiar una raíz productiva desde otra configuración.
3. Confirmar servicio avanzado Drive y propiedad `GBO_SYNC_V3_ENABLED=true` según el despliegue V3 existente. Crear una nueva versión SOLO del despliegue de pruebas; el usuario conserva control sobre esta publicación.
4. GET debe confirmar `ok=true`, `syncProtocol=3`, `pathVersion=4`, `stableFamilyNumbering=true`, `technicalDeliveryWithoutGps=true`, `photoCategoryVersion=1` y la raíz esperada. No imprimir el token. Diagnóstico privado de token con la operación segura existente, si está habilitada.
5. En el dispositivo secundario verificar las propiedades privadas reales (`DRIVE_WEB_APP_URL`, `DRIVE_API_TOKEN`), sin pegar tokens en Git ni chat. La APK Debug de esta entrega ya utiliza las propiedades locales copiadas idénticas; si cambia la URL/token, recompilar localmente.

El cliente HTTP deja la cola en UNCONFIGURED si faltan estas capacidades. Las revisiones ya preparadas de 2B se pueden reintentar sin reinterpretarlas; su contrato previo sigue admitido. Las nuevas fotos llevan `photoCategoryVersion=1`. No ejecutar migraciones históricas ni renumerar carpetas automáticamente.

## Verificación física y real, todavía pendiente

- Dispositivo secundario limpio; no instalar Debug sobre la tablet de campo ni sus pendientes. `adb devices -l`, elegir explícitamente el serial autorizado; `adb -s <SERIAL> install <ruta absoluta de app-debug.apk>`. Si hay una firma distinta instalada, usar otro dispositivo; no desinstalar la aplicación de campo.
- Crear datos ficticios: un badén/guardavía con 3 INICIO, 2 FIN, 5 OBSERVACION; señal con varias PANORAMICA/ELEMENTO; alcantarilla con varias ENTRADA/SALIDA/PANORAMICA. Mantener GPS opcional y nulo.
- Guardar offline; cerrar/reabrir y confirmar categorías, contadores y miniaturas. Borrador incompleto no debe salir. Validar o cerrar la ficha; recuperar conexión y verificar ACK.
- Verificar Drive: ruta/familia/elemento numerado/categoría. Registrar folderId, UUID y fileId obtenidos del ACK; descargar JPEG y Excel para comprobar contenido. Nada de SIB ni carpetas de material/tipo/condición.
- Comprobar metadata/manifest: UUID de foto, categoría, orden, revisión y clave `photo/<UUID>`; correlacionar con fileIds del recibo. Confirmar Excel contractual y que una segunda ficha sigue en el consolidado.
- Añadir OBSERVACION, guardar y sincronizar; reemplazar una foto y recategorizar otra. UUID/folderId/fileId de las fotos existentes deben mantenerse. Verificar original histórico en REVISIONES; foto movida sin duplicado.
- Retirar una foto con confirmación y guardar: se retira de la entrega activa y se archiva solo el archivo propiedad de esa revisión. Cancelar el diálogo no cambia nada.
- Reintentar y comprobar mismos IDs y cantidades; simular dos operadores con la misma base: conflicto explícito, sin pérdida local ni sobreescritura de la revisión ganadora.
- Dos inspecciones SCAP del mismo puente: misma carpeta PUENTE N, diferentes subcarpetas SCAP/<UUID>; conservar sus categorías, manifiestos y Excel individuales.
- Entrega técnica DELIVERED/SYNCED únicamente con ACK verificable; georreferenciación externa sigue PENDING. Anotar evidencia real sin credenciales.

Los dobles locales de Drive prueban la lógica y bytes de archivos, pero no acreditan una subida a Google Drive ni una validación física de cámara/Galería Android.
