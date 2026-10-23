# Archivos y compatibilidad del hotfix SCAP

No aplicar este conjunto como una actualización de archivos productivos a ciegas. Son fuentes Android en revisión, integradas sobre el árbol actual 1.4.8. No hay APK nueva ni cambios de Apps Script.

| Archivos | Función | Dependencia de la nueva migración de 1.4.8 |
|---|---|---|
| `scap/domain/ScapSketchSelection.kt` | Principal por tipo, advertencia, compatibilidad con uno solo. | Ninguna; usa `scap_values`. |
| `scap/data/ScapRepository.kt` | Selección explícita en ficha editable, nueva revisión y constancia de confirmación de pendientes. | Ninguna adicional. |
| `scap/export/ScapCanonicalExporter.kt`, `ScapExcelExporter.kt`, `ScapTemplateMedia.kt` | Reemplaza rechazo múltiple por principal/anexos; mantiene espacios, estilos y estructura existentes. | Ninguna adicional. |
| `scap/domain/ScapValidation.kt`, `StructuredScapCompletion.kt` | Principal pendiente como WARNING, navegación E. | Ninguna adicional. |
| `scap/export/ScapDelivery.kt`, `ScapDriveSync.kt` | Bundle interno 8, todos los croquis, captura/pending en manifest; errores de imagen con identidad; validación estructural conservada. | Ninguna adicional. No modifica HTTP, ACK, reservas ni servidor. |
| `scap/export/ScapFieldBackup.kt`, `ScapCaptureRows.kt` | ZIP provisional independiente con informe HTML, captura completa explícita y medios originales; claves JSON estables sin reflexión vulnerable a renombrado R8. | El generador no usa tablas nuevas ni copias locales; podría evaluarse para 1.4.7. |
| `scap/data/ScapEditJournal.kt` | Intenciones privadas AtomicFile de texto/selección, último token por campo; no es una base Room nueva. | Ninguna adicional. |
| `scap/ui/ScapController.kt` | Orden de cola, recuperación de ediciones pendientes, operaciones independientes del ciclo de pantalla, confirmación real. | Ninguna adicional; necesita sus pruebas y revisión de ciclo de vida. |
| `scap/ui/ScapMedia.kt`, `ScapWorkspace.kt` | Selección principal; no mostrar confirmación de borrador con errores/operaciones pendientes. | Las fuentes actuales ya contienen controles previos 1.4.8; no copiarlas enteras a 1.4.7. |
| `scap/ui/ScapExportPanel.kt` | Botones provisionales, mensajes separados y navegación a croquis/ubicación. | Integra `LocalCopies` 1.4.8 para Descargas y lista de archivos. Adaptar separadamente a SAF si se aprueba un hotfix 1.4.7. |
| `localfiles/LocalExportBuilder.kt` | Incluye ZIP provisionales cuando los SIC complementarios están incompletos, sin inventar XLSX contractual. | Sí: integración de copias locales 1.4.8 existente, esquema 20. |
| `diagnostics/ScapSaveDiagnostics.kt`, `StabilityDiagnostics.kt` | Contadores y tiempo de cola, exportados por la pantalla de diagnóstico existente. | Ninguna tabla nueva. |
| `ScapUrgentIntegrityTest.kt` | Escenarios nuevos aislados; no se incorpora a una APK productiva. | Pruebas sobre esquema actual 20 y migración 19→20 existente. |

Todas las rutas Java/Kotlin anteriores son relativas a `app/src/main/java/com/tuempresa/inventariovial`; el test está en `app/src/test/java/com/tuempresa/inventariovial`.

No se cambian `InventoryDatabase.kt`, esquemas Room, `versionCode`, `versionName`, keystore, Manifest, applicationId ni configuración privada en este encargo. `AutomaticDelivery.kt`/`AutomaticSync.kt` se inspeccionaron y probaron; no se modificaron. Las preparaciones/ACK anteriores se conservan. El bundle 8 solo afecta preparaciones nuevas; no invalida ni sobrescribe paquetes/recibos previos.

## Decisiones pendientes del propietario

1. Identificar versiones/códigos y Android reales de tablet/celular.
2. Autorizar una base y versionCode futuro después de revisar el informe y asegurar un respaldo integral recuperable. No instalar 1.4.8 (22) esperando encontrar estas correcciones: esa APK se conserva exactamente como fue entregada.
3. Realizar validación física con datos ficticios, incluyendo reinicio offline, sesión, medios y proveedor de archivos. El journal protege texto/selecciones; fotos/croquis deben haber terminado su alta Room antes del reinicio.
4. Para Drive, probar una ficha ficticia con ubicación técnica real de prueba y permiso de escritura. El ensayo automatizado usa un servidor simulado y no acredita aceptación/rendimiento del Apps Script V20 publicado.
5. Si se desea respaldo remoto sin PR, definir y revisar un contrato separado del consolidado/carpeta técnica. No se ha implementado ni activado esa ampliación de servidor.

Estado: corrección fuente integrada para revisión; aplicación de campo y publicación pendientes. La pérdida reportada no se declara resuelta físicamente.
