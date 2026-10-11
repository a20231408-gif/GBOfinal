# Entrega Prompt 2A — esquema de familias y orden técnico

11/10/2026. Rama local `codex/drive-familias-2a`, desde `2bac1be62c29cd64c15f54b853912b5306f58d26`. Checkpoint funcional: consultar `git log -1` en esta rama. Sin publicación ni merge.

## Implementado

Android y Apps Script conservan syncProtocol 3 y utilizan pathVersion 4 para nuevas entregas: RUTA → FAMILIA → ELEMENTO N → CATEGORÍA → N fotos. Se elimina SIB de todas las nuevas rutas. Identidad UUID, reserva durable bajo ScriptLock y folderId permanecen estables aunque cambie PR o se registre un elemento anterior. SIC-08/09 son independientes. SCAP conserva su paquete por inspección dentro del puente y sus fotos por categoría; su workbook individual no se mezcla con SIC-17.

Los Excel contractuales se consolidan en RUTA/EXCEL_SIC/SIC-XX.xlsx por UUID y orden numérico de calzada/sentido/PR/distancia, conservando plantillas y columnas. El inventario y KMZ incluyen familias secundarias, fajas, padres SCAP y muestras de pavimento. Sin coordenadas se mantiene una entrada sin geometría y advertencia; no se transforma X/Y de daños en GPS. La falta de GPS no bloquea entrega técnica.

[Esquema final](ESQUEMA.md), [matriz completa SIC/categorías](MATRIZ_SIC.md), [auditoría y migración controlada](AUDITORIA_Y_MIGRACION.md).

## Validación local aprobada

- 15 pruebas Android: 8 específicas de familias/orden/KMZ/proyecciones y 7 de sincronización sin GPS adaptadas al esquema 4.
- 15 casos backend contra las fuentes Apps Script y dobles locales de Drive: dos tablets/alcantarillas, reserva/reintento/fallo, cambio de PR, SIC-20/21, SIC-08/09, N fotos, consolidación, SCAP, pavimentos y rechazo de filas UUID ajenas.
- assembleDebug aprobado, configuración privada real de pruebas, sin gboValidationBuild=true. APK en app/build/outputs/apk/debug/app-debug.apk. applicationId y versionCode 16 sin cambios; no se alteró firma Release ni configuración del servicio de usuarios. APK no instalada.

Resumen verificable y SHA-256: [VALIDACION.json](VALIDACION.json). Ejemplos sintéticos y recibos simulados en examples/; sus IDs NO son archivos remotos. Logs locales, incluidas ejecuciones fallidas iniciales, conservados fuera del commit. No se repitió la suite completa ni las 39/60 pruebas históricas del Prompt 1.

## Pendientes concretos

1. El responsable debe publicar exclusivamente el paquete deployment-test/ en el proyecto Apps Script independiente de pruebas. [Pasos y checklist exactos](DESPLIEGUE_Y_PRUEBA_REAL.md); [diferencias backend](BACKEND_CHANGES.patch). La raíz autorizada es 1FH56ZlgRRZzIrfMpL35EbjyX5HsZuzyk. Su último GET conocido anuncia esquema 3; no se consultó ni modificó remotamente durante Prompt 2A. El nuevo cliente conserva la cola UNCONFIGURED hasta confirmar esquema 4 y stableFamilyNumbering.
2. Autorizar y ejecutar prueba real con UUID nuevos: verificar carpetas, categorías, fotos, filas Excel, IDs y ausencia de duplicados. Las pruebas locales no acreditan creación real en Drive. La subida SIC-08 del Prompt 1 fue real, pero corresponde al histórico SIB y no valida este esquema nuevo.
3. Instalar únicamente en dispositivo secundario sin registros pendientes; comprobar permiso denegado/GPS ausente, offline/reconexión y representación física de mapa/KMZ. No hay validación física de este checkpoint.
4. Migración histórica solo mediante respaldo y manifiesto aprobado, sin movimientos o borrados automáticos. Se conservan las limitaciones contractuales preexistentes de SIC-14A y SIC-34; no se inventaron equivalencias.
5. Prompt 2B (edición segura de sincronizados) NO iniciado.

## Trabajo protegido y separado

La copia operativa y su rama codex/sincronizacion-sin-gps permanecen intactas. El checkpoint incluye únicamente fuentes, pruebas y documentación sintética de 2A. No incluye credenciales, propiedades locales, APK, cachés, logs crudos ni temporales. Archivos preexistentes sin commit de SCAP (plantillas/mapas), proguard y herramientas/resultados de la prueba V3 permanecen locales; no se atribuyen a 2A. Las plantillas/mapas SCAP preexistentes deben conservarse para reproducir sus assets en esta copia; su incorporación a Git necesita clasificación separada.
