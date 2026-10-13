# Parche Apps Script 10 — no desplegado

Actualizar el mismo proyecto **GBO_Inventario**, después de revisión del propietario. No crear otro proyecto ni modificar GBO - Servicio de Usuarios. Este trabajo no hizo despliegue ni cambios de propiedades remotas.

## Archivos preparados

| Archivo del repositorio | Archivo en el editor Apps Script | Cambio |
|---|---|---|
| google-apps-script/Code.gs | Code.gs | Capacidades, reserva V2, respaldos legacy internos, rechazo seguro de anulación |
| google-apps-script/SyncV3.gs | SyncV3.gs | Asignador estable reutilizado, reserva bajo lock, validación nombre reservado, respaldo técnico |
| google-apps-script/SyncV3Core.js | SyncV3Core.gs | Payload/hash V2 y rutas; V1 conservado |
| google-apps-script/PhotoNamesV2Core.js | PhotoNamesV2Core.gs (nuevo) | Catálogo y reservas permanentes por UUID |
| google-apps-script/VideoV1Core.js | VideoV1Core.gs | Solo SCAP/Puente |

Conservar VideoV1.gs y los demás archivos existentes compatibles; no es necesario reemplazar el adaptador de video por este cambio. Los `.js` son fuentes de motor del repositorio, se cargan como `.gs` en Apps Script, sin `module.exports` de entorno Node (estos archivos usan guardas para Node).

**Preservar ROOT_FOLDER_ID, API_TOKEN, propiedades, permisos, URL de despliegue y configuración real del proyecto.** La raíz declarada en el repositorio no coincide con la raíz anunciada por el servicio real; no copiar a ciegas los valores de configuración del encabezado de Code.gs. Verificar el ID real de raíz de forma privada antes de publicar. Nunca pegar tokens/URLs privados en incidencias o logs públicos.

## Activación escalonada

1. Respaldar fuente y propiedades privadas del proyecto y el contenido técnico por ID; inventariar capacidades actuales con GET. Mantener `GBO_PHOTO_NAMES_V2_ENABLED` ausente/false y video remoto false.
2. Revisar/cargar los archivos anteriores, comprobar motor Drive avanzado ya utilizado, lock, zona horaria y configuración real. Publicar una versión del mismo despliegue únicamente cuando lo autorice el propietario. Comprobar `syncProtocol=3`, `pathVersion=4`, `stableFamilyNumbering=true`, `photoCategoryVersion=1`, `photoNamingVersion=1`, `readablePhotoNamingVersion=0`, `inventoryLifecycleVersion=0`, `technicalRevisionStorage=1` y `videoScope=SCAP_ONLY`. V1 continúa anunciado aunque luego se active V2.
3. Probar una ficha completamente ficticia; ACK/idempotencia/Excel/rollback y ausencia de modificaciones a inventario ajeno. Comprobar que nuevos respaldos están bajo la raíz técnica y los Excel vigentes en RUTA/EXCEL_SIC. No mover REVISIONES anteriores.
4. Para nombres nuevos: propiedad de Script `GBO_PHOTO_NAMES_V2_ENABLED=true`, GET `readablePhotoNamingVersion=2` (legacy `photoNamingVersion=1`). Cliente actualizado solicita `reservePhotoNamesV2` con payload V2, baseRevision e identidad y verifica respuesta. Solo después habilitar `GBO_PHOTO_NAMES_V2_ENABLED=true` en server.local.properties de la compilación de prueba. Las preparaciones históricas se reenvían sin cambiar su firma. No habilitar simultáneamente en tablets de campo sin prueba ficticia.
5. Volver la propiedad a false detiene nuevas reservas; no borra las existentes. Una cola V2 queda pendiente de servicio compatible, no se transforma silenciosamente a V1 ni pierde sus nombres/UUID. Mantener compatibilidad de entrega de snapshots V2 ya reservados antes de retirar el motor. Los respaldos generados ya tienen referencias por ID y deben permanecer recuperables al revertir la fuente.

NOT_JPEG corregido en Android no requiere desplegar ni debilitar el validador: el servicio real ya aceptó el JPEG ficticio corregido. Video remoto requiere además que la versión publicada anuncie videoProtocol=1, videoScope=SCAP_ONLY y habilitación explícita Android; permanece false en esta entrega.

Anulación/restauración **NO disponible**: `inventoryLifecycleVersion=0`, comandos retornan `INDIVIDUAL_INVENTORY_AUTH_REQUIRED`. Falta autorización individual con alcance de inventario, no un token de Drive o TABLET_ADMIN_ONLY. No hay procedimiento de activación segura para C todavía; véase ANULACION_CONTROLADA_10.
