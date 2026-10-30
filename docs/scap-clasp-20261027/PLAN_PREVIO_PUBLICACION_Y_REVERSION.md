# Publicación condicionada y reversión

No se cambió el despliegue. La ejecución de este plan requiere autorización expresa del propietario.

## Antes y después de autorizar

1. Confirmar que el mismo despliegue identificado privadamente continúa en V20 y que V21 conserva los hashes verificados. Si cambió algo, detenerse.
2. Coordinar las tablets: anunciar panel 2 puede desbloquear automáticamente entregas existentes. No modificar flags ni datos para simular una prueba aislada.
3. Actualizar ese despliegue existente a la versión 21; conservar deployment ID, descripción/configuración y URL. No ejecutar create-deployment ni crear otra URL.
4. Consultar la URL existente solo por GET: exigir ok=true, panel=2 y conservar V3/path4, numeración estable, fotos V2, video1/SCAP_ONLY y nombres video2, más el resto de capacidades. Conservar evidencia saneada. No enviar fixtures a producción.
5. Coordinar un único reintento del operador en la APK 1.4.15 (29). No se necesita otra APK. El momento de reintento depende de la cola, las condiciones de Android y una consulta actualizada de capacidades; no se garantiza que cualquier botón reencole inmediatamente un PENDING_VALIDATION.
6. Comparar ACK real con identidad/revisión/contentHash esperados; carpeta estable, fileIds, SHA-256, categorías y archivos recibidos, manifiesto y Excel SIC. Staging o progreso parcial no equivalen a entrega. Evitar compartir tokens, URL o identificadores completos.

## Si falla

Conservar ACK/error, preparación local, fotografías, staging y journals. No borrar ni renombrar registros. Detener nuevos reintentos controlados mientras se diagnostica.

Para revertir código publicado, actualizar el mismo despliegue nuevamente a **V20**, usando el deployment ID privado respaldado; la URL permanece igual. No restaurar contadores ni ScriptProperties, no borrar Drive y no desinstalar Android. Revertir el despliegue no deshace datos ya confirmados. Si una transacción quedó pendiente, examinar su journal antes de repetir o intentar limpieza.

El HEAD del editor permanecería en V21 aunque se vuelva a desplegar V20: esto no modifica lo servido por el /exec versionado. Si se quisiera restaurar también el código del editor, preparar primero una restauración comparada desde el respaldo y obtener autorización para ese paso adicional. Con V20, las categorías nuevas volverán a esperar compatibilidad; deben conservarse localmente.
