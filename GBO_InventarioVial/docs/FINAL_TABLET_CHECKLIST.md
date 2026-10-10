# Smoke test de tablet — integración final

Todos los puntos siguientes están **pendientes de ejecución en la tablet real**. Las pruebas JVM/Node y la compilación no sustituyen este checklist. Registrar modelo, Android, operador autorizado, fecha, versión de servidor y evidencia de cada resultado. Usar una instalación de validación con datos respaldados. El APK entregado es debug; la versión de campo deberá usar la misma keystore release GBO existente.

## Login y dispositivo

- [ ] Configurar el User Service existente por el flujo autorizado; verificar usuario/PIN válidos e inválidos.
- [ ] Sincronizar usuarios; comprobar roles, operador de sesión, baja y bloqueo de acceso.
- [ ] Confirmar huella/token/dispositivo contra el registro autorizado del servidor. No regenerar tokens ni cambiar Script Properties.
- [ ] Probar reapertura offline con el usuario habilitado y caché válida; comprobar bloqueo/reautenticación al volver del segundo plano.

## Inventario, señalización y obras de arte

- [ ] Abrir SIC-17/18/19/20/21/22/23 desde la navegación B; volver al inicio y abrir las herramientas CORE.
- [ ] Guardar borrador con campos vacíos y texto numérico parcial; cerrar/reabrir y verificar observaciones, detalle, fecha, fotos y GPS inicial/final.
- [ ] Cambiar de usuario; confirmar que un borrador propio del usuario anterior no puede editarse desde el nuevo usuario.
- [ ] Finalizar con faltantes: comprobar validaciones vigentes. Completar y confirmar revisión antes de guardar.
- [ ] Tomar foto con cámara Android predeterminada y cada lente ofrecida por Camera2; probar cancelar, rotación, segundo plano y permiso denegado.
- [ ] Seleccionar JPEG/imagen de archivo; comprobar copia local independiente del proveedor. Abrir preview, cerrar y eliminar; repetir hasta eliminar la última foto y reabrir el borrador.
- [ ] Comparar foto original y entregada: sin texto/logo/watermark añadido. Verificar fecha/GPS/metadatos JSON/Room y orientación. Una marca ya quemada en una foto histórica no se puede borrar automáticamente.
- [ ] Obtener GPS; comprobar resumen plegado y detalles a demanda, precisión, PR y edición manual. En lineales conservar inicio/fin y fuentes; no adoptar PR geométrico silenciosamente.
- [ ] Señalización: buscar código/nombre/descripción, introducir código manual, indicar sin código identificable y registrar hito puntual sin ubicación final.
- [ ] Alcantarilla: N fotos en PANORAMICA, ENTRADA y SALIDA; revisar miniaturas, preview y persistencia por categoría.
- [ ] Anular una ficha pendiente y durante una entrega; confirmar que no se envían nuevos archivos ni se marca el lote completo como sincronizado.

## Puentes y SCAP

- [ ] Crear MODULAR y LOSA; comprobar templates/mappings separados, G/G.1, elementos manuales, metrado, unidad y factor.
- [ ] Guardar incompleto, cerrar app y reabrir; preservar vanos, pilares, apoyos, juntas, fotos, croquis y observaciones.
- [ ] Comprobar galería común, selección de cámara, importación, eliminación y preview.
- [ ] Revisar miniaturas y pendientes. Confirmar cierre/revisión vigente antes de preparar Drive.
- [ ] Exportar SCAP y SIC-17/17A/17B; reabrir Excel y comprobar hojas, unidades y fórmulas según las fuentes B.
- [ ] Verificar que los `#REF!` preexistentes se reportan como **Error existente en plantilla fuente**. No exigir una fórmula inventada ni tratarlo como cambio del exporter.
- [ ] Abrir/compartir el Excel desde Archivos generados tras finalizar la exportación, sin depender del archivo temporal SAF.

## Pavimentos

- [ ] Asfalto, rígido y no pavimentado: cabecera secuencial, fecha persistente, histórico, calzada y progresivas encadenadas `00+000`.
- [ ] Guardar muestra incompleta y reabrir; verificar que el cierre/cálculo no acepta datos inválidos ni inventa PCI si faltan curvas VD/CDV.
- [ ] Rígido: filas/columnas, dimensiones de losa y PCI por losas; conservar snapshot de muestra cerrada.
- [ ] Registrar daños L/M/H, puntos, líneas, áreas y grietas determinísticas. Probar zoom, escala dinámica, malla y Ajustar.
- [ ] Fotos: cámara/importación, miniaturas, preview y eliminación; cerrar y reabrir.
- [ ] Exportar PDF segmentado y Excel; comprobar tabla/datos y ausencia de tabla visual de iteraciones en Excel.
- [ ] Exportar SIC-14/15/16 con suplementos requeridos; comprobar que SIC-14A 07/13 se bloquea con explicación contractual.
- [ ] Confirmar registro durable de cada PDF/Excel/SIC en Archivos generados y abrir/compartirlo.

## Drive y protección local

- [ ] El APK de validación omite configuración Drive local. Verificar el aviso; probar red únicamente con un build/configuración autorizados y las credenciales existentes.
- [ ] Sin red, capturar/guardar/exportar y verificar almacenamiento local. Conectar Starlink y comprobar trabajos autorizados pendientes.
- [ ] SIC: preparar y revisar la entrega, confirmar y comprobar programación. Un borrador o lote PREPARED no se sube con Sincronizar ahora.
- [ ] SCAP: ficha finalizada, revisión vigente y registro padre no anulado; Pavimentos: muestra cerrada. Probar automático, Sincronizar ahora y Reintentar errores en el centro único.
- [ ] Confirmar Pendientes/Subiendo/Errores y estado por archivo/elemento. Sin ACK/hash/ID válido nunca mostrar Sincronizado.
- [ ] Simular HTTP/error/ACK incompleto y pérdida de respuesta. Reintentar y verificar los mismos bytes/UUID, sin duplicados.
- [ ] Confirmar servidor V2 publicado y estructura real RUTA/familia/elemento/categoría. Comprobar compartir carpeta del puente tanto SIC→SCAP como SCAP→SIC.
- [ ] Cambiar/anular ficha durante envío y comprobar interrupción. Cambiar muestra/SIC generado y comprobar nueva revisión pendiente.
- [ ] Probar varios elementos y tablets; verificar rechazo seguro de colisiones de secuencias antes de operación conjunta.
- [ ] Abrir y compartir SIC, SCAP, PDF/Excel/SIC Pavimentos; confirmar estado únicamente contra ACK guardado y hash actual.

## KMZ

- [ ] Importar KMZ válido con ruta elegida; comprobar stage y resumen de ejes/vértices/hash.
- [ ] Cancelar sin activar: referencia anterior intacta.
- [ ] Confirmar Usar esta geometría y comprobar versión activa y actualización de referencia.
- [ ] Reabrir app; comprobar versionado y fallback ante archivo activo inválido. No transformar chainage KMZ en PR contractual automáticamente.
- [ ] Rechazar rutas/entradas inseguras, XML con DTD y geometría inválida. No hay Google Earth SDK.

## Actualización y firma de campo

- [ ] Con copia autorizada de base v12 y archivos/fotos, actualizar usando la identidad release GBO existente; verificar v14 y todos los datos. No desinstalar para simular una actualización.
- [ ] Confirmar `12→13` SIC-22 y `13→14` Pavimentos, preservación de usuarios/configuración/fotos/SCAP/historial/snapshots.
- [ ] Validar la keystore release original antes de distribuir a campo; no sustituirla por la firma debug entregada.
