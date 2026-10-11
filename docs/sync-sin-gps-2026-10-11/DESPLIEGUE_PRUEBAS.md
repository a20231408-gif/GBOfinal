# Despliegue aislado y verificación con tablet

## Estado remoto comprobado

Se leyó el endpoint configurado en `drive.local.properties` (`DRIVE_WEB_APP_URL`), sin enviar fichas ni fotografías. Su GET respondió `ok=true`, versión `v6.2 · encarpetado estricto por elemento + SCAP v2`, sin `syncProtocol`, `technicalDeliveryWithoutGps` ni raíz efectiva verificable. El endpoint de Drive es distinto del servicio de usuarios. Esta comprobación identifica la configuración local; no demuestra qué URL contiene la APK instalada en la tablet.

Google Drive confirmó la existencia de [PRUEBA_FINAL](https://drive.google.com/drive/folders/1u7II1gvhSIHBwVxFjnx6VsAqa3O_jTIi), folderId `1u7II1gvhSIHBwVxFjnx6VsAqa3O_jTIi`, bajo `13. PUCP_SOFTWARE`. Tiene carpetas de rutas existentes. Su nombre y el ROOT_FOLDER_ID del código local no demuestran aislamiento respecto del despliegue productivo.

No se creó `GBO_TEST_SIN_GPS_20261011`, no se publicó Apps Script y no hubo subidas remotas. La prueba real queda pendiente exclusivamente por falta de aislamiento confirmado y de URL de pruebas compatible.

## Pasos que el responsable debe autorizar y ejecutar

1. Consultar **en el proyecto productivo existente, sin editar ni publicar**, la raíz efectiva usada por su versión desplegada. Confirmar por ID y relación de carpetas que PRUEBA_FINAL y la futura subcarpeta de pruebas no reciben entregas productivas. Si PRUEBA_FINAL pertenece al árbol usado para producción, elegir otra raíz de pruebas y comunicar su ID. No basta comparar nombres.
2. Una vez comprobado el aislamiento, autorizar y crear exclusivamente `GBO_TEST_SIN_GPS_20261011` dentro de PRUEBA_FINAL. Registrar el nuevo folderId. No usar PRUEBA_FINAL directamente como raíz del backend de prueba: ya tiene contenido anterior.
3. Autorizar **un proyecto Apps Script nuevo e independiente**, por ejemplo `GBO TEST SIN GPS 20261011`, sin copiar despliegues, propiedades, tokens ni IDs de producción. El paquete local `deployment-test/` tiene tres fuentes: `Code.gs`, `SyncV3Core.gs` y `SyncV3.gs`. La primera contiene un marcador de raíz que impide asumir un destino real.
4. En ese proyecto nuevo, sustituir únicamente ROOT_FOLDER_ID por el folderId de la subcarpeta exclusiva. Configurar un token nuevo de pruebas mediante un canal privado; no reutilizar el de producción ni incluirlo en informes. Reemplazar el marcador API_TOKEN solo en el proyecto de prueba.
5. Habilitar el servicio avanzado **Drive API v3** en Servicios. Si el proyecto usa un proyecto Cloud estándar, habilitar también Google Drive API allí. Configurar la propiedad del script `GBO_SYNC_V3_ENABLED=true`.
6. Autorizar y crear **Implementar → Nueva implementación → Aplicación web**, ejecutada con la cuenta que administra la carpeta de pruebas. El cliente Android actual usa token de aplicación y no login OAuth de Google: el acceso de la web app debe admitir ese cliente según las políticas de la organización. Este permiso corresponde solo al backend de Drive de pruebas. Revisar y aceptar los permisos solicitados para el proyecto nuevo; no editar el proyecto de usuarios.
7. Registrar la URL nueva terminada en `/exec`. Antes de compilar/configurar una tablet, leer su GET y exigir `ok=true`, `syncProtocol=3`, `pathVersion=3`, `technicalDeliveryWithoutGps=true` y `rootFolderId` exactamente igual a la subcarpeta exclusiva. La raíz no puede ser la productiva ni PRUEBA_FINAL directamente. Verificar además el ROOT_FOLDER_ID del proyecto desplegado.
8. Preparar una copia de trabajo local para pruebas. Configurar **solo** `drive.local.properties` con la URL/token de pruebas; preservar su configuración original fuera de Git y no cambiar `server.local.properties`, autenticación de tablets, applicationId ni firma histórica. No compilar una APK conectada al endpoint original para este ensayo.

Guías oficiales: [web apps y nueva implementación](https://developers.google.com/apps-script/guides/web), [servicios avanzados](https://developers.google.com/apps-script/guides/services/advanced), [Drive avanzado](https://developers.google.com/apps-script/advanced/drive).

## Diferencias a desplegar

`backend-changes.patch` compara las fuentes locales anteriores con las corregidas; no representa un diff descargado del despliegue productivo. La versión remota observada es más antigua que la fuente local V3, por lo que **hay que cargar los tres archivos completos del paquete en el proyecto nuevo**.

- Code.gs: GET anuncia la capacidad y rootFolderId; protocolo/pathVersion 3 solo cuando V3 está habilitado y Drive avanzado existe.
- SyncV3Core.gs: confirma entrega técnica, georreferenciación pendiente y fuente de orden; protege estos campos con hash para las entregas nuevas; conserva compatibilidad de hash con solicitudes anteriores.
- SyncV3.gs: acepta orden contractual por PR sin exigir coordenadas. Mantiene IDs y nombres ya asignados, exclusión mutua, staging, revisiones y rollback. La secuencia de carpetas de entregas por PR queda asignada al ingresar; no representa un orden submétrico certificado ni renumera carpetas anteriores.
- Android: requiere backend compatible antes de enviar las entregas nuevas. El backend observado actualmente deja los registros conservados como `UNCONFIGURED`, sin marcarlos sincronizados.

## APK y restricciones de instalación

Se pidió compilación Debug de validación: `-PgboValidationBuild=true` omite endpoints y credenciales locales, también la configuración local del servicio de usuarios. Esa APK acredita compilación y pruebas locales; **no es la APK configurada para la prueba remota**.

Después de configurar el entorno aislado, compilar sin ese flag para la prueba real. Una APK Debug usa certificado Debug y no actualiza una tablet con Release histórico. No desinstalar la aplicación de campo ni borrar datos para forzar su instalación: usar un dispositivo/perfil de pruebas o preparar luego un Release con la firma histórica.

El código mantiene versionCode **16**, versionName **1.4.2** y applicationId `com.tuempresa.inventariovial`. Si el último Release instalado sigue siendo 15, 16 es superior. Para un Release nuevo que también actualice tablets que ya tengan 16, el siguiente debe ser **17**. Esta tarea no incrementó la versión ni generó Release.

## Ensayo real A–H

1. Confirmar previamente GET y carpeta aislada. Registrar modelo/tablet, APK instalada y versión. Desactivar GPS y denegar permisos de ubicación; activar modo avión.
2. Crear SIC-08 y SIC-09 técnicamente completos con PR/distancia válidos y observaciones. Añadir fotografías reales sin coordenadas. Guardar/validar expresamente. Crear además un borrador incompleto de control.
3. Cerrar y reabrir la aplicación offline: fichas, fotografías y observaciones deben conservarse; las fichas validadas deben estar en cola. El borrador no debe enviarse. No inventar latitud/longitud cero.
4. Recuperar Internet. WorkManager exige conexión para el trabajo inmediato y mantiene recuperación periódica de 15 minutos; Android puede diferir la ejecución por restricciones del sistema. Mantener la app abierta inicialmente y usar el botón existente «Reintentar ahora» si se requiere una comprobación inmediata.
5. Revisar el panel existente de sincronización: guardar el recibo de la revisión, el destino y los IDs. Confirmar en Drive las rutas `RAÍZ_TEST/RUTA/SIB-02/FAJA N/`, fotografías bajo su categoría, metadata JSON y Excel SIC-08/SIC-09 consolidados en `RAÍZ_TEST/RUTA/SIB-02/`. Revisar filas UUID y fotografías visibles, sin tocar carpetas anteriores.
6. Exigir antes de aceptar la entrega: `key`, `revision`, `contentHash`, `folderId`; para cada archivo `fileId`, `folderId`, `requestedPath`, `logicalPath`, `sha256`; para Excel `format`, `fileId`, `folderId`, `sha256` y `recordIds`. El recibo debe indicar `technicalDeliveryStatus=DELIVERED`, `georeferencingStatus=PENDING_EXTERNAL`, `externalGeoreferencingStatus=PENDING`.
7. Abrir cada ID confirmado mediante Drive; verificar su ubicación dentro de la raíz de pruebas y descargar/abrir fotos y Excel. El recibo del backend por sí solo no sustituye esta comprobación remota independiente.
8. Reintentar sin editar la ficha y comparar folderId, fileIds, cantidad de carpetas, fotografías y filas Excel. Deben conservarse. Reproducir además pérdida de conexión/ respuesta, recuperar Internet y comprobar una única entrega. Una ficha ya sincronizada no genera otro envío al pulsar reintentar; para probar el reintento HTTP tras perder la respuesta hay que interrumpir una entrega en curso.
9. Comprobar que los cuatro bloqueados antiguos por el mensaje exacto de georreferenciación pasan a cola al abrir la app actualizada. Otros bloqueos y borradores deben conservarse. Si falla cualquier comprobación, conservar recibos/archivos y suspender únicamente esa prueba remota.

## Registro de evidencia pendiente

Guardar en un informe sin credenciales: ID de raíz productiva confirmado por su responsable, ID de raíz de pruebas, fecha/resultado GET, UUID/revisión de las fichas, folderIds/fileIds reales, hashes, cantidad antes/después del reintento y resultado de apertura de fotos/Excel. Los recibos bajo `examples/` y `backend-results/` son **simulados**; no son IDs de Google Drive real.
