# Prueba integrada 8B — Android y Drive V4

Guía preparada en 8A. Su ejecución física y remota está pendiente; no acredita una entrega a Drive.

## Preparación por el responsable

1. Usar una tablet secundaria o un emulador con datos ficticios y una cuenta autorizada para pruebas. Registrar modelo, Android, orientación, versión de la app y commit. No instalar sobre la tablet principal. Si existe una app con otra firma, usar otro dispositivo/emulador: no desinstalar una copia con datos para forzar la actualización.
2. El responsable del servidor debe proporcionar un despliegue de pruebas `/exec`, token de pruebas y una raíz Drive aislada. Confirmar esa raíz antes de introducir datos. No utilizar producción. Esta guía no contiene URLs privadas ni credenciales y no requiere cambios al servicio de usuarios.
3. Comprobar el GET del endpoint con una herramienta del responsable. Debe anunciar `ok=true`, `syncProtocol=3`, `pathVersion=4`, `stableFamilyNumbering=true`, `photoCategoryVersion=1` y `technicalDeliveryWithoutGps=true`. Si falta una capacidad, detener la prueba de envío y conservar la cola; no interpretar un HTTP 200 como entrega confirmada.
4. La URL/token se incorporan al compilar; no hay un selector de endpoint en la app. En una copia local dedicada a pruebas, copiar `drive.local.properties.example` a `drive.local.properties` y completar `DRIVE_WEB_APP_URL` y `DRIVE_API_TOKEN` con los valores autorizados. Mantener el archivo fuera de Git. No mostrar sus valores en capturas, registros o chat.
5. El APK de 8A se compila con `-PgboValidationBuild=true`: omite los endpoints y credenciales locales y, por tanto, sirve para revisión local, no para envío real. Para 8B el responsable debe preparar una compilación Debug ordinaria con la configuración de pruebas, sin ese indicador. Comando desde la copia dedicada: `.\gradlew.bat :app:assembleDebug`. No generar Release ni modificar firma, applicationId, versionCode o Room. La firma Debug local puede diferir de la histórica; verificarla antes de instalar en el dispositivo secundario.

## Recorrido reproducible

| Paso | Acción | Resultado exigido |
|---|---|---|
| 1. Sesión | Configurar una sesión de prueba con ruta/calzada del catálogo y sentido. Anotar fecha y operador ficticio. | Ubicación contractual coherente; sin inventar GPS. |
| 2. Borrador | Crear una ficha SIC-08 o SIC-09 y dejar un dato obligatorio incompleto. Guardar borrador y volver a Mis registros. | Mismo borrador recuperable; ninguna entrega definitiva ni archivo remoto de esa revisión. |
| 3. Validación | Completar PR/distancias válidos, campos obligatorios y una observación identificable como prueba. Puede mantenerse GPS ausente. Finalizar la ficha. | Abre el detalle del mismo UUID. La entrega se programa automáticamente; no requiere exportar manualmente. |
| 4. Fotos | En el editor, capturar/importar dos imágenes con referencias visuales distintas. Para categorías INICIO/FIN, repetir con un elemento longitudinal que las admita; SIC-08/09 usan GENERAL. Finalizar los cambios. | Galería muestra categoría, cantidad y contenido correcto; originales locales conservados. Permisos/cámara funcionan. |
| 5. Envío | Con conexión, abrir el detalle y desplegar información Drive o entrar a Entregas Drive. Esperar al worker; si es necesario, usar Reintentar ahora. | En cola/Subiendo → Sincronizado solo con ACK válido. Revisión confirmada coincide con la enviada; se informa destino. Android puede diferir el worker. Registrar la espera y cualquier error. |
| 6. Drive | Con la cuenta de pruebas, revisar los IDs/rutas confirmados. Abrir el XLSX con un lector real y descargar/abrir las imágenes para comprobarlas. | Familias bajo ruta/elemento numerado, sin nuevas carpetas SIB. Fotografías en categorías correctas. Consolidado en `<ruta>/EXCEL_SIC/<SIC>.xlsx` con la fila del registro y filas previas conservadas. |
| 7. Edición | Anotar UUID, revisión/base confirmada, folderId y fileIds. Editar observación/condición, reemplazar una imagen y finalizar. Esperar ACK. | Mismo UUID y carpeta; revisión posterior, baseRevision de la confirmación previa. XLSX conserva otras filas. Reemplazo conserva referencias conforme a su clave y archiva antecedentes según el servidor. |
| 8. Sin Internet | Desactivar Wi-Fi/datos, crear y validar otro registro con foto o editar uno sincronizado. Cerrar/reabrir la app y revisar ficha/galería/XLSX local. | Datos sobreviven. Pendiente de conexión o Actualización pendiente; nunca confirmado por generar un archivo local. El worker puede seguir en espera sin mostrar Error. |
| 9. Recuperación | Restablecer conexión, esperar el worker y, si hace falta, solicitar el reintento existente. | Se confirma la misma identidad/revisión; no crea otra carpeta ni fila duplicada. Comprobar IDs y contenido en Drive. |
| 10. XLSX | Desde VER EXCEL SIC, generar/abrir y compartir la copia real con un lector y una aplicación receptora de pruebas. Repetir sin Internet con la copia local disponible. | XLSX válido con filas de esa ruta/SIC, permiso de lectura correcto y columnas contractuales. Es una copia local; el consolidado global se verifica aparte en Drive. |
| 11. SCAP | Crear dos inspecciones ficticias (losa y modular), con nombres/fotos distintos. En una, guardar borrador, abrir Ver pendientes y navegar a código A, apoyo 2, descripción global, foto y metrado. Revisar y confirmar cierre, incluidos pendientes permitidos. | Cada destino abre su campo real. Cierre sin consentimiento no entrega; con cierre autorizado programa la inspección correcta. Ficha, fotos y Excel corresponden solo a esa inspección bajo su puente/SCAP. Confirmar ACK y archivos reales. |
| 12. Pavimentos | Crear muestra ficticia con tipo/superficie del catálogo, dimensiones, daño y foto. Revisar advertencias, cerrar muestra y comprobar entrega. Abrirla desde el inventario ordenado. | Misma muestra; fotos/Excel propios y estado real. PCI/URCI conserva su cálculo y advertencias; no inventa resultado si faltan curvas. En Drive verificar ruta/EVALUACION_SUPERFICIAL/tramo/superficie/MUESTRA N. |

Repetir detalle, galería y regreso en vertical/horizontal. Abrir un registro ajeno al anterior para comprobar aislamiento de fotografías.

## Estados y situaciones negativas

- **Borrador / Pendiente de validación:** completar o corregir los datos indicados antes de entregar.
- **Sin configurar:** revisar endpoint y capacidades con el responsable; la cola se conserva.
- **En cola / Pendiente de conexión / Subiendo:** esperar conexión y ejecución de Android. Guardar localmente no confirma el servidor.
- **Error:** una comunicación o confirmación falló; conservar evidencia y usar el reintento existente.
- **Actualización pendiente:** existe ACK de una revisión anterior y falta confirmar la edición actual.
- **Bloqueado:** corregir el rechazo indicado; no fabricar una confirmación.
- **Conflicto:** conservar la edición local y pedir conciliación al responsable. No reenviar forzando otra identidad ni borrar datos.
- **Sincronizado:** exigir ACK y revisión coincidentes, además de comprobar archivos/contenido en la raíz de pruebas.

Para probar rechazo por capacidades o conflictos reales, coordinar un escenario aislado con el responsable del servidor. No degradar producción ni modificar el servidor desde esta guía. Las respuestas incompatibles, ACK obsoletos y conflictos ya tienen cobertura simulada local en 8A; eso no sustituye la evidencia física/remota.

## Evidencia y aceptación

Guardar fuera del repositorio: commit/APK y SHA-256, certificado, dispositivo/Android, hora de cada paso, capturas de estados/galería/lector XLSX, UUID/revisión/baseRevision, IDs confirmados, rutas y contenido de Drive, conteo de filas antes/después, prueba de originales, reintentos y reinicio sin conexión. Registrar por caso APROBADO/FALLIDO/NO EJECUTADO con resultado esperado y observado. Sanitizar tokens, URL privada, cuentas y datos personales antes de compartir.

Aceptar 8B solo si se completan los pasos, las fotos y XLSX reales se abren, se conserva identidad y datos durante edición/offline/reintento, no se pierden filas de otros registros y los estados coinciden con ACK válidos. Registrar por separado cualquier pendiente, espera de Android, fallo de permisos o ausencia de despliegue compatible. La evidencia simulada de 8A no permite declarar Drive real aprobado.
