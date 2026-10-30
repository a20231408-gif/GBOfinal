# GBO_Inventario — V21 publicada y capacidades verificadas

**Publicación autorizada completada: V20 → V21 en el mismo despliegue.** Verificación real por GET: 2026-10-27T23:52:04-05:00 (Lima). HTTP 200, ok=true y scapPhotoPanelVersion=2.

Se conservaron deployment ID, URL, descripción, manifiesto y los otros tres despliegues. La comparación de la respuesta completa confirma que todos los campos anteriores son idénticos; el único campo añadido es scapPhotoPanelVersion=2. Siguen syncProtocol=3, pathVersion=4, stableFamilyNumbering=true, photoCategoryVersion=1, readablePhotoNamingVersion=2, videoProtocol=1, videoScope=SCAP_ONLY, readableVideoNamingVersion=2 y technicalDeliveryWithoutGps=true.

Antes de publicar se descargó otra vez la versión inmutable 21 y se verificaron los diez archivos por SHA-256. El despliegue aún estaba en V20 y mantenía su descripción. Se actualizó exclusivamente ese despliegue mediante clasp update-deployment; no se creó un proyecto ni una URL. Código y manifiesto desplegados son los del candidato ya probado.

## Resultado y límites

- 19 pruebas locales aprobadas con las fuentes descargadas: 20 categorías, 54 fotos, 3 croquis, SCAP.xlsx/manifiesto, SIC-17/17A/17B, hashes, idempotencia, recuperación e históricos; además, adaptadores de video simulados.
- Publicación Google y capacidades del endpoint real: PASS.
- 1362 archivos locales protegidos de Android, APK, backend local y configuración: hashes intactos. Sin compilación, instalación ni Git commit/push.
- Sin cambios de ScriptProperties, raíz, permisos, Servicio de Usuarios o contratos. No se enviaron fixtures ni fichas desde el agente. Las tablets pueden comenzar a procesar sus colas automáticamente ahora que se anuncia panel 2.
- **ACK de una ficha real, recepción completa y archivos Drive: PENDIENTES de la prueba del operador.** Una capacidad correcta no equivale a entrega. No se declara todavía probado en campo el parche ni se atribuyen mejoras de velocidad.

El respaldo privado original permanece en D:/Downloads/GBO_CLASP_PRIVADO/SCAP_V2_20261027/RESPALDO_ANTES_SCAP_V2_PRIVADO.zip; V20 sigue disponible para reversión. No se leyó, copió ni imprimió el archivo de autenticación. Los IDs y URL se conservan solo en la carpeta privada. El estado anterior se conserva en ESTADO_PREVIO_A_PUBLICACION.md y el plan anterior en PLAN_PREVIO_PUBLICACION_Y_REVERSION.md.

Siguiente paso: PRUEBA_CONTROLADA_TABLET.md. Evidencia saneada: evidencias/PUBLICACION_V21_GOOGLE_REAL.json. Comparación de código y pruebas: COMPARACION_Y_PRUEBAS.md. Reversión: REVERSION_Y_VERIFICACION.md.
