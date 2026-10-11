# Backend de pruebas y verificación real — Prompt 2B

El GET consultado el 11/10/2026 confirma protocolo 3 y raíz aislada `1FH56ZlgRRZzIrfMpL35EbjyX5HsZuzyk`, pero todavía pathVersion 3 y sin stableFamilyNumbering. No se hizo ninguna escritura remota 2B. Este servicio no es compatible con las rutas nuevas; Android conserva la cola sin enviarla. BACKEND_STATUS.json registra solamente capacidades públicas, sin URL privada ni token.

## Pasos del responsable

1. Abrir SOLO el proyecto Apps Script independiente de pruebas; respaldar la versión y el registro ._GBO_SYNC_V3. No utilizar el proyecto original/productivo ni el servicio de usuarios.
2. Copiar los tres archivos de deployment-test/: Code.gs, SyncV3Core.gs y SyncV3.gs. El paquete mantiene la raíz aislada y obtiene el token de la propiedad privada GBO_TEST_API_TOKEN. Configurar GBO_SYNC_V3_ENABLED=true y Drive API avanzado v3 habilitado.
3. Publicar una versión nueva SOLO del despliegue de pruebas. Conservar su URL /exec o actualizar únicamente DRIVE_WEB_APP_URL y DRIVE_API_TOKEN en drive.local.properties de la copia aislada, sin pegar credenciales en informes/chat/Git.
4. Comprobar GET: syncProtocol=3, pathVersion=4, stableFamilyNumbering=true, technicalDeliveryWithoutGps=true y raíz exacta. Diagnóstico seguro del token: stageRecordV3 con revisión 0 debe devolver INVALID_STAGE después de autenticar, sin escribir.
5. Ejecutar la prueba siguiente en dispositivo secundario y datos ficticios, con UUID nuevos. El SIC-08 histórico SIB no debe migrarse automáticamente.

## Checklist verificable

- Crear alcantarilla A sin GPS y añadir fotos ENTRADA/SALIDA/PANORAMICA; finalizar offline, recuperar conexión y registrar ACK/revisión/folderId/fileIds/sha256. Confirmar descargando archivos reales, no sólo mirando el estado de Android.
- Crear alcantarilla B en la misma ruta, con PR menor. Confirmar dos carpetas estables y ambas filas en RUTA/EXCEL_SIC/SIC-18.xlsx.
- Reabrir A, corregir condición a Regular y finalizar. Exigir Actualización pendiente y, tras ACK, misma carpeta/folderId, mismos IDs de fotos intactas y mismo fileId del consolidado. Descargar Excel y comprobar celda corregida y fila B intacta.
- Cambiar A a PR anterior a B. Verificar nuevo orden en inventario/Excel/mapa y mismo número de carpeta; sin GPS, conservar advertencia sin punto inventado. Fotografías siguen accesibles.
- Añadir una foto, cambiar categoría de otra y retirar una de la entrega. Comprobar nueva foto por ID, recategorización con el mismo fileId y foto retirada/versión anterior en REVISIONES. Los originales locales y archivos de B permanecen intactos.
- Reintentar exactamente la misma revisión, incluso después de perder la respuesta: mismos IDs, sin nuevas carpetas ni duplicación de filas. El backend debe responder alreadyExists=true cuando corresponde.
- Dos operadores con copias del mismo UUID y baseRevision: publicar una revisión desde A y luego enviar la revisión de B basada en la anterior. Exigir REVISION_CONFLICT, STALE_REVISION o BASE_REVISION_CONFLICT y estado Conflicto · edición local conservada. Confirmar que la revisión ganadora y todos los archivos ajenos no cambian.
- Para simular dos copias, usar exclusivamente datos ficticios y una copia controlada de pruebas que conserve UUID y recibo base. La app no incorpora un nuevo importador de registros remotos en esta tarea. Los escenarios de dos operadores están cubiertos por pruebas locales.
- Registrar revisión remota/local y evidencia descargada. Entrega técnica DELIVERED y georreferenciación externa PENDING siguen separadas.

## Conflicto: procedimiento seguro

No se reajusta baseRevision ni se sustituye automáticamente la versión remota. Conservar y respaldar la edición local, comparar con la revisión ganadora y conciliar con el responsable. El botón de reintento queda deshabilitado en CONFLICT y nuevas ediciones no borran ese estado. Una interfaz de descarga/mezcla/resolución de conflictos no forma parte de esta entrega; no modificar directamente la base en producción para forzarla.

## APK

Usar solamente un emulador/dispositivo secundario sin datos pendientes y firma compatible. No instalar en la tablet de campo. APK Debug: applicationId com.tuempresa.inventariovial, versionCode 16. Identificar primero SERIAL_DE_PRUEBAS y ejecutar:

```powershell
$taskAdb='D:\Android\Sdk\platform-tools\adb.exe'
& $taskAdb devices -l
& $taskAdb -s SERIAL_DE_PRUEBAS shell getprop ro.product.model
& $taskAdb -s SERIAL_DE_PRUEBAS shell pm list packages com.tuempresa.inventariovial
& $taskAdb -s SERIAL_DE_PRUEBAS install 'C:\Users\gerbc\.codex\worktrees\backend-v3-pruebas\GBO_InventarioVial\app\build\outputs\apk\debug\app-debug.apk'
```

Si existe una instalación que debe conservarse, usar otro dispositivo/perfil. No desinstalar ni borrar datos. No se cambió firma histórica ni applicationId y no se instaló esta APK.
