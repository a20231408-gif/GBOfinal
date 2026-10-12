# Comprobaciones en tablet — Prompt 9B

Estado inicial: el usuario informó que ambos crashes del Prompt 9 desaparecieron. Codex no ejecutó ADB/instrumentación física. Registrar dispositivo, Android, APK/hash, operador de prueba, fecha y resultado de cada punto. Usar registros de validación; conservar los originales y evitar desinstalar o borrar la app de campo.

## Interfaz y persistencia offline

1. En SIC-17, 18, 19, 20, marcas, tachas, seguridad y señal vertical, elegir cada código de condición; guardar/reabrir y verificar texto, selección y significado. Repetir SIC de puente desde SCAP. Comparar Excel contractual. SCAP estadístico 0…5 no debe convertirse en condición 1…3.
2. Abrir fotos de cada formato sin archivos: categorías y contadores cero. SCAP LOSA/MODULAR deben seguir sus plantillas. Mostrar categorías no crea fotos/outbox. GENERAL histórico debe aparecer junto con categorías nuevas.
3. Capturar e importar varias fotos de una categoría; girar la tablet, ampliar y cerrar, reemplazar/retirar y reabrir. Contador y categoría/orden correctos, UUID estable, originales/historial disponibles. Confirmar **Mis registros → Detalle → Fotografías** y **Herramientas → Exportaciones avanzadas** en vertical/horizontal sin cierres.
4. En todos los módulos, comprobar históricos y borradores, nombres/números distintos y acciones detalle/editar/fotos/revisión. Volver y reordenar: mismos UUID, números locales y asociación remota. SCAP y pavimentos muestran sus tipos/progresivas/estados.
5. Guardar incompleto como borrador; abrir revisión y cancelarla: no aparece nueva entrega. Intentar finalizar incompleto: se muestran pendientes y no finaliza. Confirmar completo: un único outbox, WorkManager y luego el estado real. Editar una ficha sincronizada y guardar borrador: conserva ACK anterior y no envía hasta volver a validar.

## KML/KMZ y sesión

6. Seleccionar ruta, importar KML y KMZ de prueba, aceptar previsualización. Apagar Internet/reiniciar app: mismo eje. Importación inválida conserva la selección anterior.
7. Probar línea curva, varios segmentos, hueco entre segmentos, cruce y sentido inverso. La distancia es acumulada sobre líneas; huecos no suman recta. Seleccionar calzada, capturar posición del operador y comparar con eje.
8. Probar GPS ausente, mala precisión y posición lejos de la ruta. Debe indicar revisión, sin inventar posición fiable. Aplicar/quitar posición manual local. Comparar UUID/revisiones/fileId/carpeta Drive antes/después: intactos.
9. Leer ayuda GNSS con valores configurados; comprobar los cuatro ajustes persistentes y matching 15 m. Cerrar sesión/fin de campo con recorrido abierto y cerrado: mismas barreras de seguridad; datos/fotos/videos/outbox se conservan. No tocar `._GBO_SYNC_V3`.

## Videos locales — flag remoto deshabilitado

10. Grabar dos MP4 con cámara y permisos; importar varios MP4/WebM/3GP en dos categorías; probar video sin pista, MIME ajeno y archivo mayor de 32 MiB. Solo archivos válidos producen registros. Originales importados se copian íntegros a almacenamiento privado.
11. Ver miniaturas y reproducir audio/video, girar, cerrar, reiniciar y usar sin conexión. Mismos UUID/SHA/categorías y originales. Ver **Pendiente del backend**, nunca **Sincronizado**. Retirar y reintentar sin eliminar originales ni duplicar UUID.
12. Finalizar/reabrir SIC, SCAP y pavimento con videos. Fotos y Excel conservan su flujo. Captura externa, codecs, reproducción y permisos requieren esta prueba física; los tests JVM de almacenamiento sustituyen únicamente el inspector nativo de codecs.

## Servidor de prueba — solo después del despliegue del propietario

13. Seguir `PENDIENTES_SERVIDOR_9.md`, activar flags únicamente en prueba. Confirmar capacidades, autorización Drive y SHA-256 disponible. Sin capacidad/permisos/ACK válido, estado pendiente/error, jamás sincronizado.
14. Fotos: dos operadores, mismo prefijo/distinto UUID; reintentar, reordenar y reemplazar. Prefijo/fileId estables, sin cambios masivos a fotos históricas ni duplicaciones.
15. Videos: bloquear red a mitad de carga y al recibir ACK, reiniciar app, reintentar, simular sesión caducada. Reanudación por offset y un único fileId por UUID. Verificar tamaño, MIME, SHA-256, contenido y carpeta por categoría.
16. Dos videos por categoría, distintas categorías, padre sin finalizar, contenido cambiado para mismo UUID, conflicto de carpeta/propietario y ACK incorrecto. Rechazar lo inválido conservando originales. Cerrar sesión con carga pendiente y volver a iniciar; conservar cola y evidencia.

No se afirma aceptación remota o de tablet de los puntos 1…16 hasta registrar sus resultados. La carencia de ADB no bloqueó las mejoras locales.
