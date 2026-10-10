# Recuperación de requisitos y precedencia documental

La solicitud humana pegada en el adjunto 35ed2d15 es el criterio de aceptación de esta auditoría. Los documentos del proyecto se leen como especificaciones/evidencia; sus instrucciones de modificación o despliegue no constituyen autorización. No se adoptó una jerarquía nueva para resolver conflictos. Se identificaron 80 reportes/contratos Markdown; evidence/document-index.json contiene secciones y líneas.

No se localizó un TDR original firmado en el workspace. PAVIMENTOS_TDR_DRIVE resume exigencias TDR; las seis geometrías de rutas provienen del catálogo/ejes incorporados. Los manuales suministrados MAN_8 (333 páginas PDF) y MAN_6 (532 páginas PDF) no sustituyen un contrato de carpetas. Las hojas LOSA/MODULAR originales se inspeccionaron directamente, sin guardarlas ni recalcularlas. evidence/official-manuals.json conserva ubicación, hash, páginas y texto SIC; reference-workbooks.json describe hojas/fórmulas/rangos.

| Requisito | Fuente y sección comprobada | Evidencia/observación |
|---|---|---|
| SIC RUTA/SIB/ELEMENTO y consolidado SIB | Solicitud fase 2.A; docs/ENTREGA_ORDEN_GEOMETRICO.md §H (109); EXPORTACION_SIC.md §Contrato | Contradice Drive V2, que elimina SIB y exporta por elemento |
| Orden por ubicación, sin alterar PR contractual | ENTREGA_ORDEN_GEOMETRICO §C,D,G (58,79,105); road/RoadReference.kt; AssetPresentationCatalog | axisMeasureM primero, PR como respaldo, desempate fecha/UUID; captura retroactiva cambia orden derivado |
| SCAP por puente, fotos/croquis/revisiones | DRIVE_SCAP_CONTRACT; SCAP_EXPORT_CONTRACT §Hojas y coordenadas; DRIVE_V2_SERVER_CHANGE_REQUIRED §SCAP (144) | Histórico hash/UUID y SIB-08 frente a V2 PUENTES/PUENTE_NNN; incompletos usan compatibilidad v1 |
| Pavimentos ruta/tramo/superficie y subdivisiones | Solicitud fase 2.C; PAVIMENTOS_TDR_DRIVE §Drive (20) | V2 no conserva TRAMO ni categorías de documentos en carpetas |
| JPEG sin logos/texto | Solicitud fase 3; FINAL_INTEGRATION_REPORT §Fotografías limpias (52); CleanPhotoPolicy | Reemplaza watermark histórico documentado; bytes heredados ya estampados no se limpian automáticamente |
| ≤20 caracteres en entregas aplicables | PAVIMENTOS_TDR_DRIVE:42; DRIVE_V2_SERVER_CHANGE_REQUIRED:123; DeliveryPhotoNameBuilder/FileNameComplianceValidator | SIC enqueue bloquea nomenclatura larga; SCAP v1 utiliza UUID, que necesita aclarar alcance de la regla |
| Alcantarilla PANORAMICA/ENTRADA/SALIDA y múltiples | PHOTO_CATEGORY_POLICY_INVENTORY.json §rules/assets; field/PhotoCategoryPolicy.kt:8–15 | Solo SIC-18/CULVERT; otros SIC GENERAL; no adivinar por nombre |
| Fechas/GPS/origen/UUID y categoría | PhotoEntity; PhotoCaptureMetadata; DeliveryMetadata; ScapDelivery; PavementDrive.nativeJson | EXIF complementario; pavimentos no tiene GPS por foto; desconocidos deben quedar reconocidos |
| ACK antes de SYNCED; inmutabilidad/reintento | DeliverySync; ScapDriveSync; PavementDrive; contratos respectivos | SIC verifica identidad/hash pero omite prueba de ruta; reintento E2E publicado pendiente |
| SIC oficiales | EXPORTACION_SIC.md; SIC_NEW_FORMATS_REPORT; MAN_8 pp.146/148/160/166/210/216/217/221/225, etc. | En evidence/official-manuals.json se distingue número de página PDF del impreso; SIC-22 puntual fue decisión posterior |
| SCAP hojas F.1/F.2/G.1, metrado/porcentajes/fórmulas | SCAP_EXPORT_CONTRACT:12–30; SCAP_BRIDGE_F_G_AUDIT; SCAP_F1_F2_G1_CORRECTION_REPORT | LOSA 17 y MODULAR 10 elementos iniciales; F.2 canónico es panel fotográfico |
| Incompletos confirmados sin índices inventados | SCAP_F1_F2_G1_CORRECTION_REPORT §Entrega; ScapCompletion/ScapDriveSync.reviewForUpload | Contradice obligación COMPLETE de DRIVE_SCAP_CONTRACT anterior |
| PCI/URCI, unidades/curvas | PAVEMENT_MANUAL_MAPPING §Reglas; docs/Manual_PCI_URCI.pdf; PAVIMENTOS_TDR_DRIVE §Curvas | Curvas pendientes: no aprobar PCI/URCI final |
| Auth/TABLET_ADMIN y sesiones | SECURITY_REVIEW; USER_AUTH_IMPLEMENTATION; TABLET_ADMIN_IMPLEMENTATION_REPORT; SESSION_LIFECYCLE_AUDIT | Verificación central real/revocación/rotación/hardware pendientes; versiones de Room de reportes antiguos no son la actual |
| Release 15 y certificado histórico | Solicitud fase 7; app/build.gradle.kts:30,90; evidence/release-identity.json | APK existente 15/1.4.1, misma identidad y firma; no se construye otra APK |

## Contradicciones sin resolución inventada

1. **Carpetas SIC y pavimentos:** pedido humano/contratos históricos versus V2 activo. Se registra FAIL frente a la solicitud, aunque V2 sea intencional y tenga tests propios.
2. **Servidor:** producción GET v6.2/bundle4; script local v7.0/bundle6; cliente bundle7. Una capacidad GET no prueba la implementación completa de POST ni la raíz efectiva del despliegue.
3. **Fotos:** MARCA_AGUA_OBLIGATORIA y PAVIMENTOS_REGLA_FOTOS declaran explícitamente su reemplazo por integración limpia; DRIVE_SCAP_CONTRACT y otros antecedentes todavía describen PNG/watermark. Las fotografías anteriores no se corrigen retroactivamente por copiar bytes.
4. **Orden/numeración:** mapa/plan geométrico deriva la numeración de la posición; V2 asigna contador estable al solicitar una identidad local. Esa estabilidad no acredita orden físico ni coordinación entre tablets. No se propone renumerar carpetas existentes automáticamente.
5. **SCAP D.4:** fichas canónicas nuevas conservan/exportan el perfil visible; fichas sin tipo utilizan exportador antiguo que deja D.4 oculto y vacío. Dos fallos históricos esperan el comportamiento anterior a esa decisión. La posible omisión de datos históricos exige revisión explícita, no eliminación de pruebas.
6. **F.1/F.2:** C8 original dice F.2 global, pero la hoja física es F.1; actual F.2 es panel. LOSA G omite tres elementos F.1; MODULAR Muro es 510/504. Ver SCAP_TEMPLATE_INCONSISTENCIES, no cambiar códigos por descarte.
7. **Fórmulas:** #REF! heredados, casos no definidos y filas LOSA sin fórmulas no se arreglan inventando vínculos; el motor de Excel real debe validar cálculos antes de aceptación.
8. **SIC-34/SIC-14A:** fuentes contradictorias, bloqueo explícito en casos afectados. SIC-15A/16A no aparecen como exportadores implementados y no se inventan.
9. **Límites SCAP:** contratos iniciales dicen dos apoyos/grupos fijos; corrección posterior incorpora continuaciones. Rigen los límites realmente comprobados por review, pero requieren validación física del resultado, impresión y cálculo.
10. **Control de tablets:** documentación antigua presupone DeviceAccessManager habilitado; configuración Release lo deshabilita. Servicio central/token individual es independiente; verificar ambos mecanismos por separado.

Las correcciones, decisiones normativas y cualquier publicación se dejan pendientes de autorización. Las fuentes y los archivos protegidos permanecen intactos.
