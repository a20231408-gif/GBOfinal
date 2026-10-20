# Rendimiento de sincronización — GBO Ruta Clara 1.4.6 (20)

15/10/2026. Estado: **no apta todavía para distribución general**. La incidencia real 2/17 SIC y 1/11 muestras necesita contrastar estados, ACK y tiempos de los dispositivos y del servidor publicados. No se accedió a producción ni se atribuyen esas proporciones a una causa que no se haya medido. Las correcciones de integridad SIC-22 y navegación se conservan.

## Lo confirmado y lo que falta

Se confirmaron costes y defectos locales concretos:

1. El transporte se creaba para cada ficha y su GET de capacidades se almacenaba solamente dentro de esa instancia. Un lote podía negociar capacidades una vez por ficha. Ahora reutiliza un transporte durante cada vaciado, con observadores de progreso y rendimiento asociados a la ficha actual; vuelve a negociar en el siguiente vaciado.
2. La reserva V2 usaba `AutomaticDelivery.prepare(readable=false,persist=false)`, que renderizaba PDF/XLSX y copiaba/transcodificaba fotos, antes de repetir la preparación con los nombres reservados. SIC y Pavimentos ahora construyen una descripción de identidad, destino, categorías y nombres previos, sin renderizar documentos ni copiar píxeles para esa reserva. La validación de ubicación SIC se mantiene. La preparación final conserva sus validaciones, JPEG y hashes. SCAP conserva el procedimiento de revisión/preparación aprobado; no se ha reescrito su renderizado.
3. Ante pérdida de comunicación, cada siguiente ficha podía iniciar otro intento de red. Ante BUSY global, cada ficha podía esperar el mismo ScriptLock. Ahora se pospone ese vaciado cuando ocurre una interrupción del transporte HTTP o BUSY, conservando los pendientes. Un ACK inválido o una ficha local inválida siguen tratándose por registro; no se disfrazan de corte de Internet.
4. APPEND_OR_REPLACE permitía acumular solicitudes de vaciado por cierres y pulsaciones. Ahora se coalescen con trabajo único KEEP. Las intenciones persisten en Room. No se cancelan subidas ni se eliminan trabajos históricos de WorkManager; una cadena ya existente puede terminar trabajos vacíos.
5. «Sincronizar ahora» recorría también CONFLICT; `AutomaticSync.retry` lo rechaza y la excepción podía interrumpir la programación de las demás fichas. El lote omite conflictos, bloqueados y pendientes de validación que requieren intervención. Una ficha que deje de ser válida durante esa programación tampoco impide programar las independientes. No se borra ni se concilia el conflicto.
6. El contador del dashboard aceptaba cualquier recibo no nulo como confirmación, incluso `{}`; ahora usa el mismo control de identidad/revisión de ACK de los paneles. BUSY transitorio se cuenta como pendiente/espera, y un estado SYNCED sin recibo coherente no se presenta como entrega confirmada. El encabezado incluye las entregas con error entre las pendientes de completar.

Esto demuestra problemas de coste y presentación del código, **no demuestra cuál de ellos explica cada ficha real pendiente**. Faltan los estados/mensajes de ejemplo solicitados al propietario y la observación autorizada del despliegue. Una conexión anunciada de 1000 Mbps tampoco mide la velocidad de subida sostenida, latencia, disponibilidad ni tiempo de ejecución del script.

## Estados por familia y cantidades

El diagnóstico exportable incluye `localRecords` por familia SIC/superficie de Pavimentos/SCAP y estado local; `outboxByFamily` contiene los siguientes estados. Son dos ejes diferentes: un borrador local puede no tener ninguna entrada en la cola.

| Estado de entrega | Interpretación | Acción segura |
|---|---|---|
| DRAFT | Sin finalización válida; no debe entregarse automáticamente | Completar, revisar y finalizar la misma ficha |
| QUEUED | Revisión programada; no confirmada | Mantener conexión y permitir ejecución de Android |
| UPDATE_PENDING | Existe una base confirmada y cambios posteriores | Confirmar cierre si corresponde y esperar su nueva entrega |
| UPLOADING | Estado persistido de una revisión | Contrastar con progreso y trabajador RUNNING; puede ser una ejecución interrumpida |
| ERROR | Comunicación, confirmación incompleta o fallo recuperable | Revisar causa; BUSY/corte se reintentan conservando el snapshot |
| BLOCKED | Rechazo o defecto que requiere corrección | Revisar mensaje; no repetir indiscriminadamente |
| CONFLICT | Base/revisión remota incompatible | Conciliación individual; no sobrescribir ni borrar |
| PENDING_VALIDATION | Falta validación, orden o cierre apropiado | Revisar la ficha; más ancho de banda no lo corrige |
| UNCONFIGURED | Configuración o capacidades del servidor no acreditadas | Comprobar configuración/capacidades con autorización |
| SYNCED | Solo se muestra confirmada con recibo coherente de esa revisión | Conservar ACK; una edición posterior vuelve a quedar pendiente |

`validatedWithoutOutboxByKind` identifica fichas cerradas/activas sin intención de entrega, sin reenviarlas automáticamente. `safeErrorCategories` agrupa exclusivamente códigos permitidos, sin copiar mensajes arbitrarios. El diagnóstico de WorkManager cuenta RUNNING/ENQUEUED/otros estados e intentos por trabajo único; nunca exporta sus UUID, entradas ni progreso con identidad de ficha.

El dashboard conserva un resumen sencillo; el centro de sincronización separa guardados/sin finalizar por familia, fichas pendientes y confirmadas, archivos preparados conocidos y archivos confirmados. El desglose técnico de todos los estados está en el diagnóstico existente y en su JSON exportado.

Una ficha no es un archivo. Una muestra puede requerir PDF, XLSX de muestra, JSON, varias fotos y aportes a uno o más consolidados SIC. `knownFilesAwaitingRecordAck` cuenta archivos de preparaciones actuales sin ACK final: algunos pueden estar ya en staging y no necesitar otra transferencia. Las fichas todavía sin preparación tienen cantidad de archivos **desconocida**, no cero. Los recibos Excel de varias muestras pueden apuntar al mismo archivo consolidado; no deben sumarse como archivos únicos. Generar PDF/XLSX localmente no confirma Drive.

## Qué se mide

Android conserva un máximo de 256 muestras recientes más acumulados de tamaño acotado por familia general/fase. Usa reloj monótono; registra únicamente familia permitida SIC/SCAP/PAVEMENT, fase permitida, duración, bytes y resultado permitido. No registra fotos, contenido de fichas, nombres de usuarios, coordenadas, rutas, UUID de inventario, tokens, cuerpos HTTP, URL ni errores remotos arbitrarios. El diagnóstico se exporta desde la pantalla de estabilidad existente; no se envía solo.

| Fase | Medición local | Límite de interpretación |
|---|---|---|
| Preparación / descripción de reserva | CPU/archivos locales durante esa fase | No es transferencia a Drive |
| Capacidades / reserva | Tiempo HTTP y, para reserva, duración de la operación | Una respuesta BUSY no confirma entrega |
| Cada foto/archivo | Preparación de bytes/base64, escritura de petición, espera de respuesta, total HTTP y reutilización de staging | Los tiempos se solapan; no sumar SEND+WAIT+HTTP otra vez |
| Commit | Escritura de petición y espera de respuesta HTTP | Incluye red, lock y trabajo de servidor; Android no puede separarlos por sí solo |
| ACK | Validación del recibo y persistencia de su base/confirmación | Un HTTP 200 no equivale a un ACK válido |
| Entrega/vaciado | Tiempo total observado de ejecución activa | No incluye todo el tiempo que Android mantuvo el trabajo sin ejecutarlo |
| Espera/reintentos | Estados e intentos de WorkManager, resultados BUSY/HTTP_ERROR | No se promete una hora exacta de ejecución ni se usa la revisión como reloj de encolado |

En la propuesta **no desplegada**, Apps Script mide preparación, espera y duración del lock, recuperación/registro, lectura de manifiestos, lectura de staging, desempaquetado y empaquetado XLSX, transacción, archivos, agregados y guardado. Son acumulados por fase de una ejecución, sin datos del inventario. Algunos son intervalos anidados y no deben sumarse como fases independientes.

## Trabajo protegido del backend

`getScriptLock().tryLock(30000)` es global para ese script, independientemente del usuario/PIN GBO. Sus secciones actuales incluyen:

- Reserva: recorrer/recuperar journals, leer registro y bindings, asignar número estable, crear/validar carpeta y persistir nombres.
- Staging: decodificar/hash, abrir/crear registro y staging, escribir y releer bytes para verificar hash.
- Commit: recuperar journals; leer/validar archivos y filas; recorrer manifiestos del registro; ordenar todas las filas del consolidado; desempaquetar los XLSX de sus propietarios; empaquetar el consolidado; respaldar/escribir/verificar archivos; actualizar manifiesto, binding y journal; construir el ACK.

Para N aportes al mismo consolidado, la reconstrucción recorre 1+2+…+N filas entre los commits, además de validaciones por petición. No es una subida binaria que pueda acelerarse proporcionalmente a los Mbps. El coste exacto de las llamadas Drive y de XLSX en Google todavía no está medido.

La propuesta separada solo mueve decodificación/hash puros de staging antes del lock y añade mediciones; conserva toda lectura/escritura Drive y la transacción bajo protección. No se reduce el lock para “pasar” una prueba. Optimizar por ámbitos, índices o construir XLSX fuera del lock exige validar la generación de todos los manifiestos al recuperar el bloqueo y conservar rollback/journals; queda como propuesta posterior, condicionada a mediciones y paridad de fuentes. No se recomienda desplegar un motor local encima de otro publicado sin compararlos.

## Carga aislada y tiempos medidos

Fuentes reproducibles: `google-apps-script/HotfixLoad146.test.cjs` y `app/src/test/java/com/tuempresa/inventariovial/HotfixPerformance146Test.kt`. Evidencias bajo `docs/hotfix-1.4.6/evidencia/` al cerrar la ejecución.

Backend local simulado: **9 pruebas, 9 aprobadas**, incluyendo las 7 regresiones de integridad del adaptador y dos cargas completas. Cada carga contiene 17 SIC y 11 muestras ficticias, dos dispositivos/operadores, fotos V2, 6 respuestas BUSY y pérdidas de 4 respuestas de staging y 4 ACK de commit. Resultado: **28/28 ACK, 168 archivos definitivos únicos, 172 llamadas de staging y 32 commits**, con ambas familias/consolidados completos y ordenados. Los reenvíos no cambian IDs ni números.

| Entorno | Tiempo de ejecución observado de la carga |
|---|---:|
| Node/Python ZIP, adaptador local sin cambios | 32,6 s |
| Node/Python ZIP, propuesta instrumentada | 50,1 s |

Son mediciones de este equipo, con Drive en memoria y ZIP mediante subprocesos Python, mientras había trabajo de compilación. **No son tiempos de Apps Script, no son una comparación controlada de velocidad y no prueban que la propuesta acelere el servidor.** En la ejecución instrumentada se observaron 251 desempaquetados (37,645 s acumulados en el simulador) y 28 empaquetados (4,115 s); el perfil demuestra qué trabajo se repite en ese harness, no su duración en Google.

La serialización modelada de esa carga sumó 2.352.300 bytes sin token. Solo como cota matemática de transferencia ideal: 9,41 s a 2 Mbps, 2,35 s a 8 Mbps y 0,019 s a 1000 Mbps. Excluye latencia, CPU, Drive, lock, pérdidas y esperas. Las fotos son sintéticas y pequeñas; no equivale al peso de fotos reales de campo. El ancho de banda por sí solo no predice la finalización.

La prueba Android añade dos Room independientes, 17 SIC + 11 muestras con documentos y fotografías reales sintéticas, transporte HTTP interceptado, corte completo de Internet, BUSY y pérdidas de ACK, comprobando todos los UUID, hashes, preservación de originales, staging reutilizado, GET por vaciado y confirmaciones. **28/28 confirmadas en 63,469 s**, 78 archivos únicos, 79 llamadas staging (una repetición por respuesta perdida), 29 commits (un ACK perdido) y 8 GET para 8 vaciados. Se transmitieron 3.988.586 bytes en el interceptor; incluye serialización y un token exclusivamente ficticio. Se añade una latencia artificial de 2 ms por petición. No es una conexión Starlink ni un servidor Google.

La primera ejecución Android dejó 17 SIC confirmados y 11 muestras en error de preparación; una prueba directa encontró `document is closed!` al iniciar el PDF recién creado. El runtime Robolectric instalado no implementa esa creación nativa (`mNativeDocument` se comprueba por separado). **No se ha cambiado el exportador PDF de la aplicación para ocultar ese fallo de infraestructura.** La carga se verifica con un shadow exclusivamente de pruebas que ejecuta el renderizador real de páginas sobre bitmaps y los empaqueta en un PDF raster válido. Esto acredita la preparación y el protocolo con ese adaptador, no la generación PDF nativa ni su rendimiento en una tablet. Esa comprobación física permanece obligatoria; los cinco casos V2 ordinarios y el diagnóstico de estados ya pasaron sin ese adaptador.

En la comparación aislada de reserva, SIC: 897 ms para la descripción ligera frente a 3.483 ms para la preparación completa previa; Pavimentos: 37 ms frente a 6.573 ms. Se comprobó igualdad de los campos enviados a reserva y de identidades/categorías/rutas de fotos, y ausencia de archivos renderizados por la descripción nueva. Son muestras individuales de este equipo, con PDF de pruebas y calentamiento diferente; no constituyen una estimación de ahorro por tablet ni un SLA. Las fases no medidas individualmente (ordenamiento/consultas Room y esperas del planificador) tampoco deben atribuirse por resta a una sola causa.

## Campo con Starlink o red más lenta

La condición sigue siendo CONNECTED, sin exigir Wi-Fi no medido. El cierre conserva datos y cola offline. Al fallar una comunicación se detiene ese vaciado, se conserva preparación/ACK verificado y se aplica reintento exponencial desde 30 s con dispersión de 1–5 s. Tras cuatro reintentos inmediatos queda la recuperación periódica de 15 minutos; no se descarta la entrega. Android, batería, restricciones de segundo plano, cierre forzado y disponibilidad de red pueden retrasarla más. Existe una ventana entre el fin de un vaciado y un nuevo cierre: la cola sigue siendo durable y el recuperador periódico cubre una notificación inmediata coalescida.

La pérdida de respuesta después de escribir se recupera reintentando el mismo snapshot y las mismas claves, no generando otra ficha. Las subidas de archivos de este protocolo se reanudan entre archivos mediante staging verificado, **no por bytes dentro de una fotografía incompleta**. Los videos tienen su protocolo propio y no se reescriben en esta tarea. No se establece todavía una tasa garantizada para Starlink; requiere una prueba física autorizada con tamaños reales y cortes reales.

## Correcciones Android, backend y bloqueos de distribución

| Trabajo | Situación |
|---|---|
| Android: integridad, routing, reintento, coalescencia, reserva ligera, negociación por lote, parada ante corte y contadores | Implementado; se verifica con la batería focalizada |
| Android: diagnóstico seguro por fase/estado/archivos | Implementado en el diagnóstico existente; sin envío automático |
| Apps Script: medición de lock/fases y validación pura antes del lock | Propuesta completa separada, pruebas aisladas; no publicada |
| Apps Script: consolidación fuera del lock o índices/caché de manifiestos | Propuesta pendiente; no implementada ni recomendada sin medir y validar consistencia |
| Paridad publicado/local y latencias reales Drive/Apps Script | Pendiente de acceso autorizado |
| Causa individual de los 15/17 SIC y 10/11 muestras no visibles | No determinada sin sus estados/ACK. No se crean ni borran fichas para investigarlo |
| Distribución general 1.4.6 | **Bloqueada** hasta contrastar la incidencia real y aprobar la prueba física/backend correspondiente |

Para cerrar la incidencia: obtener un diagnóstico consultivo de los dispositivos afectados, clasificar estados y archivos, verificar versión/propiedades publicadas con autorización, medir tiempos del servidor y probar dos clientes con datos ficticios autorizados bajo una conexión representativa. Las mejoras locales y una carga aislada aprobada no sustituyen ese cierre operativo. La candidata, si supera sus pruebas Android, es para revisión y validación controlada; no se instala ni distribuye automáticamente.

Cierre de construcción: candidata **1.4.6 (20) compilada y firmada históricamente**, con controles de identidad/alineación aprobados. Archivo `output/GBO_RUTA_CLARA_HOTFIX_1.4.6_20/GBO_Ruta_Clara_1.4.6_20_CANDIDATA.apk`; SHA-256 `ae61c79fefdb6e02d9af7a6ffdf7abbaa26a710d42e2ba75fd67db8affbd4ea0`. El bloqueo de distribución anterior se mantiene; no se instaló ni publicó nada.
