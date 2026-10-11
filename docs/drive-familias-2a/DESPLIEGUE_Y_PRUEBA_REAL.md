# Despliegue de pruebas y verificación pendiente

No publicado ni ejecutado remotamente en Prompt 2A. El endpoint privado de pruebas comprobado en Prompt 1 anuncia syncProtocol=3/pathVersion=3; no se presupone actualizado. Android nuevo exige syncProtocol=3, pathVersion=4, stableFamilyNumbering=true y technicalDeliveryWithoutGps=true. Conserva la cola con UNCONFIGURED si falta compatibilidad. No se reutiliza el endpoint operativo antiguo ni el servicio de usuarios.

Paquete preparado en deployment-test/: Code.gs, SyncV3Core.gs y SyncV3.gs. Raíz de pruebas fijada a 1FH56ZlgRRZzIrfMpL35EbjyX5HsZuzyk (GBO_TEST_V3_20261011). Code.gs del paquete lee el token de la propiedad privada GBO_TEST_API_TOKEN, sin incluir su valor. El Code.gs genérico del repositorio mantiene su configuración histórica; NO copiarlo como configuración de este despliegue. Cambios de código verificables en BACKEND_CHANGES.patch.

Pasos concretos para autorizar/ejecutar por el responsable:
1. Abrir exclusivamente el proyecto Apps Script V3 de pruebas independiente. Guardar una versión de respaldo del código/despliegue y su registro ._GBO_SYNC_V3. No modificar el proyecto original/productivo.
2. Reemplazar sus tres fuentes con deployment-test/. Conservar Drive API avanzado v3 habilitado y GBO_SYNC_V3_ENABLED=true. Configurar privadamente GBO_TEST_API_TOKEN con el token de pruebas. No pegarlo en chat, informes ni Git.
3. Publicar una nueva versión SOLO de ese despliegue de pruebas, con los permisos del proyecto de pruebas. Puede conservar su URL /exec; si cambia, actualizar únicamente drive.local.properties de la copia aislada: DRIVE_WEB_APP_URL y DRIVE_API_TOKEN.
4. Comprobar GET: raíz exacta indicada, protocolo 3, pathVersion 4, stableFamilyNumbering y technicalDeliveryWithoutGps verdaderos. Comprobar token con stageRecordV3/revision 0 (INVALID_STAGE después de autenticación, sin escritura).
5. Autorizar una prueba mínima con UUID nuevos y datos ficticios en esa raíz. No reusar UUID SIC-08 de Prompt 1: ese UUID está ligado al historial SIB.

Checklist para tablet secundaria: dos alcantarillas de clases 06/07 sin GPS, fotografías ENTRADA/SALIDA/PANORAMICA, guardar offline y cerrar/validar, recuperar conexión. Exigir ALCANTARILLA 1 y 2 bajo RUTA/ALCANTARILLAS (o siguientes números libres si la raíz contiene nuevas pruebas), sin SIB. Confirmar por IDs y descarga real que Excel RUTA/EXCEL_SIC/SIC-18.xlsx contiene ambas filas ordenadas por PR numérico. Reintentar la misma revisión y comparar IDs/cantidad. Comprobar SIC-20 en BADENES y MUROS, SIC-21 en una agrupación de su clase, SIC-08/09 separados y georreferenciación externa pendiente. El histórico SIB-02 debe conservar IDs/hash y ubicación. Registrar ACK, folderIds/fileIds y evidencias sin credenciales. No declarar éxito remoto con simuladores locales.

La APK es Debug: applicationId com.tuempresa.inventariovial, versionCode 16; no instalar sobre la tablet de campo ni sobre una instalación de otra firma. Usar un equipo secundario sin registros pendientes. No se cambió firma Release ni versionCode.

Comando exacto, sustituir SERIAL_DE_PRUEBAS y verificar primero identidad del dispositivo:
```powershell
$taskAdb='D:\Android\Sdk\platform-tools\adb.exe'
$env:ANDROID_USER_HOME='C:\Users\gerbc\.android'
& $taskAdb devices -l
& $taskAdb -s SERIAL_DE_PRUEBAS shell getprop ro.product.model
& $taskAdb -s SERIAL_DE_PRUEBAS shell pm list packages com.tuempresa.inventariovial
& $taskAdb -s SERIAL_DE_PRUEBAS install 'C:\Users\gerbc\.codex\worktrees\backend-v3-pruebas\GBO_InventarioVial\app\build\outputs\apk\debug\app-debug.apk'
```
Si aparece una instalación que debe conservarse, utilizar otro dispositivo/perfil. No usar -r, desinstalar ni limpiar datos de campo. No se instaló APK en esta tarea.
