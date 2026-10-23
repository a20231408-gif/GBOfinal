# Propuesta de backend 1.4.6 — NO desplegada

Estos archivos completos sirven para comparar y validar un posible cambio. No se afirma que coincidan con el Apps Script publicado. No se accedió a `/exec`, al editor de Apps Script ni a Drive de producción. El propietario debe autorizar expresamente esa comprobación antes de recomendar o publicar el parche.

## Qué contiene

- `SyncV3.gs`: mismo ScriptLock global y espera de 30 segundos en staging, commit y reserva. Añade medidas `prepareMs`, `waitMs`, `heldMs`, adquisición y acumulados de fases (registro/recuperación, lectura, XLSX, transacción, archivos y agregados), sin UUID, usuario, rutas ni credenciales. Únicamente la decodificación y SHA-256 del archivo de staging se anticipan al lock. Todas las operaciones Drive, recuperación, reservas y consolidación siguen protegidas.
- `SyncV3Core.gs`: copia completa, sin cambios, del motor local `google-apps-script/SyncV3Core.js`. No sustituir un motor publicado diferente por esta copia sin comparar.
- No incluye `Code.gs`, propiedades, autorizaciones, cambios de raíz, videos 10H ni OBSERVACIONES 10K. Sus pendientes conservan su propia revisión y autorización.

La propuesta **no elimina la contención global**. Mover la consolidación XLSX o las escrituras a Drive fuera del lock exige otro diseño con protección por recurso compartido y recuperación demostrada; no se hace en este hotfix. No se presentan milisegundos de simuladores como tiempos de Google.

## Condiciones previas y despliegue coordinado (solo con aprobación posterior)

1. Respaldar fuentes publicadas, versión del despliegue y configuración en custodia privada. Anotar identidad de ejecución y servicio Drive avanzado; no volcar tokens en el informe.
2. Comparar las fuentes exactas de la versión que atiende el `/exec` instalado, no solo el código más reciente del editor. Una respuesta de capacidades no prueba igualdad de fuentes.
3. Conservar reservas, bindings, journals, revisiones, numeración y `._GBO_SYNC_V3`. No reiniciarlos. Si difiere el backend publicado, preparar un merge revisable sobre esa base.
4. Validar en un proyecto/raíz aislados autorizados, con el mismo contrato y sin datos de ingenieros. Ejecutar dos clientes concurrentes, con una cuenta y con cuentas GBO diferentes: SIC-22 con fotos V2, SCAP y Pavimentos. Verificar locks, ACK, reenvío de revisión idéntica, pérdida de respuesta, ediciones y ambas filas de consolidado. Los tests Node incluidos simulan estas intercalaciones; no sustituyen esta prueba real.
5. Medir por operación en las ejecuciones de Apps Script. Los logs nuevos son internos, no se exponen como datos del operador ni añaden endpoints.
6. Publicar una nueva versión en el despliegue existente solo después de autorización; conservar URL, identidad, permisos, propiedades y raíz. Probar únicamente fichas ficticias autorizadas.
7. Reversión: seleccionar la versión anterior del mismo despliegue. Conservar todo el estado de Drive y volver a comprobar sus ACK; no restaurar una copia antigua de los journals sobre escrituras nuevas.

## Pruebas reproducibles locales

Desde la raíz: `node --test google-apps-script/Hotfix146.test.cjs`.

El mismo harness ejecuta el adaptador local y la propuesta: 7 casos de BUSY, reservas V2, staging idempotente, dos UUID/operadores, consolidado ordenado, familias SIC/SCAP/Pavimentos y telemetría sin información sensible. Utiliza Drive y LockService simulados, sin HTTP. El motor transaccional se compara byte por byte.

Con la ampliación de rendimiento: `node --test google-apps-script/HotfixLoad146.test.cjs` ejecuta esos 7 casos y dos cargas adicionales (9 aprobadas). Cada carga entrega 17 SIC y 11 muestras, con 168 archivos definitivos, BUSY y respuestas perdidas, sin duplicar IDs ni filas. Los 32,6/50,1 segundos observados son del simulador Node/Python y no demuestran una mejora en Google ni una comparación controlada; véase `docs/RENDIMIENTO_SINCRONIZACION_1_4_6.md`.

## Qué es indispensable

Las correcciones de clave primaria y navegación son Android y no requieren este parche. El cliente puede reintentar BUSY contra el contrato local actual. No está demostrado que este parche opcional sea necesario en producción: primero faltan paridad de fuentes y medición autorizada. La APK no puede garantizar capacidad ni eliminar por sí sola una sección crítica larga del servidor.
