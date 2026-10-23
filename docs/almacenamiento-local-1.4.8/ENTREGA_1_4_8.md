# Entrega local — GBO Ruta Clara 1.4.8 (22)

## Resultado y alcance

**Implementado y validado mediante pruebas locales:** cola independiente de copias accesibles, ajuste automático, destino explícito por ficha, exportación de documentos/fotos/croquis/videos disponibles, ZIP opcional, acciones sobre URI y exportación histórica confirmada por lotes. **Pendiente de aceptación física:** visibilidad en Archivos, lectores PDF/XLSX, compartir, reinicio de tablet, proveedores SAF y actualización sobre una instalación real autorizada. No se considera aprobada para distribución general.

El problema comprobado era la diferencia entre persistencia privada y publicación de documentos: Room, filesDir y preparaciones Drive conservaban información, pero no creaban una carpeta pública general. El mapa previo está en `MAPA_ALMACENAMIENTO_ANTES.md`; las garantías y límites, en `DISENO_Y_GARANTIAS.md`.

## Funciones implementadas

| Función | Comportamiento |
|---|---|
| Copia accesible | Android 10+: MediaStore.Downloads, `Download/GBO_Ruta_Clara/<familia>/<ruta>/<etiqueta_UUID>/R_<revision>/`. Android 8/9: carpeta local SAF elegida por el operador. |
| Automática | Inicialmente apagada. Próximos cierres válidos solicitan copia si se activa; Solo dispositivo siempre la solicita. No se exportan históricos al arrancar. |
| Destino | Por ficha antes del primer cierre/cola Drive. Predeterminado Drive y copia local según ajuste. Solo dispositivo conserva UUID y no crea entrega Drive. No cancela colas anteriores ni modifica ACK. Envío posterior requiere ADMIN existente y confirmación. |
| SIC | Exportadores contractuales actuales, complementos, observaciones disponibles, originales fotográficos y metadatos; se informa cualquier validación/faltante. |
| SCAP | Excel actual, aportes SIC disponibles, croquis, fotos originales y videos existentes no retirados. No se reconstruyen sesiones de video ni se exportan sus secretos. |
| Pavimentos | PDF actual, Excel de datos, JSON con croquis/marcas 22/26 y fotos. Aportes SIC únicamente con datos contractuales completos; no se alteran cantidades ni PCI. |
| Fotos | Copia individual, compartir, exportación por registro y ZIP. Bytes/resolución/EXIF originales; no se añade una segunda publicación automática en Galería. |
| Recuperación | Intención Room durable; plan privado congelado; escritura IS_PENDING/temporal SAF; SHA-256 y tamaño antes/después de publicar. Reintento con URI/nombre/revisión deterministas. No se sobreescribe una revisión válida de contenido diferente. |
| Históricos | Recuento y confirmación, máximo veinte revisiones nuevas por lote; procesamiento de tres fichas por tanda. Faltantes por archivo; no se modifican fichas ni estados Drive. |
| Acceso | Abrir, compartir, ubicación y guardar como mediante URI reales. Se detecta archivo movido/eliminado/permiso perdido antes de abrir y se ofrece recuperar copia. |

## Pruebas ejecutadas y límites

**76 casos distintos aprobados, cero fallos pendientes y cero omitidos.** Incluyen 29 nuevos y 47 regresiones directamente relacionadas. Resumen por caso y tiempo local en `evidencia/RESUMEN_PRUEBAS.json`; XML y logs conservados. No se suman los casos repetidos en distintas tandas.

- 16 pruebas de cola/destino: desconexión implícita sin HTTP, idempotencia, bytes originales, escritura incompleta, recuperación pendiente de publicación, pérdida de respuesta al publicar, archivo faltante, falta de espacio, acceso denegado, copia publicada alterada, revisión, ajuste apagado, Only-device, protección de ACK/cola existente, autorización ADMIN, rollback y permisos de intent.
- 5 ejecuciones de proveedor: resolver MediaStore real contra proveedor ficticio en API 29/34, búsqueda de pendientes y URI conservada; API 28 exige carpeta local y rechaza proveedor remoto. No son mediciones de una tablet ni prueban todos los proveedores SAF.
- 4 exportaciones/integración: SIC/SCAP con exportadores existentes y originales; Pavimentos 22/26 con PDF/Excel/JSON/ZIP; ausencia de permisos amplios de almacenamiento. El entorno de pruebas sustituye el render PDF nativo por el adaptador sintético ya existente: el PDF físico sigue pendiente.
- 1 migración: construye esquema 19, conserva valores de todas las tablas anteriores, crea tablas 20 y reabre una solicitud/URI interrumpidas. No sustituye la prueba de actualización de APK en tablet.
- 3 pruebas Compose: confirmación de destino, acciones/archivo ausente y consentimiento del ajuste automático.
- 47 regresiones: integridad hotfix, migraciones históricas focalizadas, Fotos V2 ordinario con transporte simulado, persistencia fotográfica y anotaciones Pavimentos, geometría/diálogo 1.4.7 y exportación/persistencia SCAP.

Los dos primeros intentos detectaron problemas del fixture de migración Windows: ruta SQLite excesivamente larga y clave opcional `indices` ausente en JSON de esquema. Se corrigió el fixture, no una migración fallida de datos productivos. La tanda final de 29 terminó correctamente. Los tiempos de los XML son de JVM local; no representan Android físico, Drive ni Apps Script.

## Conservación comprobada

La comparación se realiza contra el respaldo de fuentes **1.4.7 al iniciar esta tarea**, no contra HEAD histórico, pues el árbol contiene avances previos sin commit.

- Las cuatro configuraciones privadas locales permanecen idénticas por hash; no se incluyen valores ni hashes privados en el informe público.
- Apps Script, V20 revisado y paquete de optimización propuesto permanecen idénticos por hash. No se publica ni se llama al backend real para esta validación.
- `AutomaticDelivery.kt` conserva sus bytes. Las modificaciones en entradas de sincronización son barreras de destino y alta de solicitudes locales; no reescriben preparación Drive, HTTP, reservas V2, numeración, ACK o revisión remota. Las pruebas de regresión usan simulaciones; no certifican rendimiento de Google.
- Manifest y permisos sin cambios respecto a 1.4.7. Room recibe tres tablas adicionales mediante migración 19→20; ninguna tabla anterior se borra/reinicializa.
- APK 1.4.7 y 1.4.6 preservadas con su SHA-256 anterior. Respaldo adicional del APK genérico de compilación conservado antes de generar la candidata.
- No se instaló, desinstaló, borró, distribuyó, hizo commit ni push. No se accedió a datos reales de tablets ni se modificaron usuarios, Drive, servicios o credenciales.

La lista exacta de fuentes de producción modificadas/nuevas frente al inicio consta en `evidencia/CONSERVACION.json`; hashes de fuentes validadas, en `evidencia/FUENTES_VALIDADAS.json`. Los numerosos cambios previos de Git se conservan y no se atribuyen todos a este encargo.

## Límites operativos y decisión

Copias requieren espacio adicional: máximo 512 MiB/1000 archivos por entrega; no se purgan revisiones anteriores automáticamente. Guardar/exportar es asíncrono; esperar estado verificado. Una edición anterior a la congelación puede invalidar la revisión pendiente: se informa y se debe exportar la revisión actual. Apagar el ajuste no cancela solicitudes existentes. Proveedores pueden revocar permisos o caducar inserciones pendientes; se conserva el origen para reintento.

Un aporte SIC incompleto u original histórico ausente produce copia parcial, aunque otros documentos válidos estén disponibles. No se inventan documentos, fotos ni índices PCI. ZIP/PDF/Excel no son un respaldo restaurable de Room. La migración 20 impide considerar una reinstalación simple de 1.4.7 como rollback seguro.

**Decisión: candidata para validación física controlada, con prueba pendiente del criterio principal en una tablet.** No habilitar distribución general hasta completar `GUIA_Y_PRUEBA_FISICA.md`. La ficha ficticia Solo dispositivo permite comprobar el flujo sin escribir en Drive productivo.

## APK y firma

La identidad y el hash exactos se registran en `evidencia/IDENTIDAD_APK.json` al completar compilación/verificación. La candidata debe conservar el applicationId `com.tuempresa.inventariovial`, nombre 1.4.8, código 22 y certificado histórico SHA-256 `0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315`. No usar una APK Debug ni instalar para forzar compatibilidad.


**Compilación final aprobada:** `:app:assembleRelease --offline --console=plain`, sin flags de pruebas. Puerta de configuración y verificación de identidad/alineación aprobadas. APK: `output/GBO_RUTA_CLARA_CANDIDATA_1.4.8_22/GBO_Ruta_Clara_1.4.8_22_CANDIDATA.apk`. SHA-256: `21c1893d2a511ee04636b5fe2a9d3ffccd2c466a34a8d9e4b4b80042ab795514`. Certificado histórico coincidente; applicationId, minSdk 26, targetSdk 37 conservados. Sin adaptadores ficticios de PDF/HTTP en DEX. La configuración pública coincide con 1.4.7 salvo versión. No instalada ni distribuida.
