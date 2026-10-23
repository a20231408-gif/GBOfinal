# Resultados de implementación — GBO Ruta Clara 1.4.9 (23)

Estado: **Release candidata 1.4.9 (23) compilada, firmada y verificada; 103 casos distintos aprobados.** La candidata no se instala ni se distribuye automáticamente.

## Cambios implementados

| Función | Implementación y alcance |
|---|---|
| Copia automática de borradores | SIC, SCAP y Pavimentos solicitan copia documental al confirmar nuevas revisiones en Room; no exigen cierre, PR, Internet ni ACK. |
| Cola durable | Solicitudes en la misma transacción de captura; señal WorkManager con cinco segundos de inactividad, consolidación de revisiones no iniciadas y drenaje separado sin condición de red. La señal no cancela una exportación en curso. Recuperación periódica/al abrir. |
| Videos | Una outbox adicional en la base de videos conserva el cambio antes de transferirlo idempotentemente a la cola documental. Agrupa cambios próximos sin alterar la revisión ni el ACK del puente padre. |
| SIC provisional | XLSX con valores conocidos, campos desconocidos vacíos, marcador visible y hoja PENDIENTES con identidad, autor conocido, revisión y fecha. Los textos que empiezan por = permanecen como texto. Se mantiene OBSERVACIONES sin formato añadido. |
| Complementos existentes | Se conservan longitud técnica y observaciones HTML como anexos. SIC-18A y los complementos manuales SIC-17A/17B se incluyen; si están incompletos se generan provisionales separados. |
| SCAP provisional | Plantilla canónica derivada cuando puede representar la captura; si no puede, XLSX provisional de captura con motivo explícito. Se mantienen JSON, fotos, videos y todos los croquis originales. |
| Pavimentos | PDF borrador desde datos disponibles, Excel de datos, JSON y medios, incluidos los estados no calculables. Se reutilizan los cálculos PCI y la representación 22/26 existentes. |
| Carpetas | Ruta/familia/identidad; documentos por revisión y medios por UUID/hash. Se usa una carpeta confirmada solo con ACK guardado; sin ACK, identidad provisional. Ningún archivo publicado anterior se mueve, renombra ni sobrescribe. |
| Mis registros | Lista unificada, deduplicación del puente SCAP y registro vial vinculado, búsqueda/filtros y orden técnico de presentación. Captura, Drive de la ficha y copia local aparecen separados. |
| Acceso directo | Ver detalle, Abrir archivo, selector de documentos/revisiones, Ver ubicación y Compartir. Incorporación explícita de históricos sin archivo y reintento de fallos. URI de contenido, permisos temporales y verificación de acceso/hash. |
| Aviso SCAP sin PR | Explicación de ruta/PR/distancia, Ir a ubicación SIC y acceso al respaldo local. Se mantiene el bloqueo técnico Drive y los pendientes de gabinete confirmados. |
| Configuración | Predeterminado activado si no había preferencia; se conserva una desactivación explícita. Avisos de privacidad, espacio y límites en Herramientas y ajustes y en la elección de destino. |

Estas funciones están implementadas; no son propuestas de desarrollo. La aceptación física de la nueva APK sigue pendiente y no se sustituye con pruebas JVM.

## Validación

- **103 casos distintos en 15 suites**, con resultado final aprobado por caso. Los conteos no suman reejecuciones.
- Cinco croquis SCAP, selección principal, persistencia/journal y reapertura; PR válido y bloqueo sin PR; JSON/medios y no incorporación de provisionales en sicRows.
- Migración 19→20 conservando las filas originales del ensayo, recuperación de solicitudes y URI, fallos de escritura, interrupción después de publicar y reintentos sin duplicar medios.
- Pavimentos 22/26 y PCI, fotos V2, videos/ACK y compatibilidad de nomenclatura existentes.
- UI unificada: búsqueda/filtros por familias y acciones sin entrar al editor. Las acciones con archivos en Android real deben comprobarse físicamente.
- Los tres ejemplos finales SIC-17/17A/17B se abrieron con Microsoft Excel 16 en modo solo lectura, conservando sus hashes. Se validaron ZIP/XML/OpenXML y se revisaron visualmente las seis hojas principales/PENDIENTES mediante renderización. Excel conserva las revisiones completas como texto; el renderizador auxiliar puede mostrarlas en notación científica.
- La simulación BUSY/ACK no mide Google ni acredita rendimiento del Apps Script real. Parte de la prueba PDF JVM usa un sustituto; el PDF nativo requiere la comprobación física indicada.

Incidencias de verificación resueltas: tipo de flujo de la vista unificada; expectativa antigua de archivos ZIP en raíz; aislamiento de usuarios ficticios entre suites UI. El login productivo no se modificó. Office rechazó una relectura COM reutilizando una instancia; se completó correctamente con una instancia por libro.

## Límites y comprobaciones pendientes

1. Instalar únicamente tras autorización en una tablet cuya firma permita actualización, sin desinstalar ni borrar datos. No se ha instalado la candidata en esta ejecución.
2. Confirmar visibilidad y apertura desde Descargas, lector Excel/PDF Android, selector, Compartir, recuperación tras cierre del proceso y PDF nativo 22/26.
3. Medir con medios reales de prueba el tiempo de generación y el comportamiento de batería/almacenamiento. Cinco segundos es el periodo de agrupación, no una promesa de terminación.
4. Límite actual por copia: 512 MiB y 1000 archivos; las revisiones publicadas se conservan. Android 8/9 necesita carpeta local autorizada con el selector.
5. Los históricos sin asociación documental requieren incorporación gradual explícita. Los archivos manuales antiguos sin UUID asociado no se vinculan por suposición ni se trasladan.

El ACK documentado en la copia corresponde a la entrega de la ficha padre. Los videos tienen ACK independiente. Los archivos locales son un paquete documental, no una restauración automática de Room.

## Integridad y restricciones

Se verificaron contra el inicio de la tarea **4 configuraciones privadas y 71 archivos del backend/propuestas**, sin cambios. No se modificaron el servicio de usuarios, credenciales, identidad, applicationId, firma histórica, carpetas Drive ni Apps Script. Room permanece en 20; la base de videos conserva su versión y datos, con metadatos de outbox añadidos sin migración destructiva. No se borraron registros ni originales.

Las APK anteriores y el respaldo inicial se conservan. No se hicieron commits, push, instalación ni distribución. La optimización Apps Script anterior mantiene su encargo independiente; no se prepara ni publica un parche de servidor para estas copias locales.

Véanse COMPATIBILIDAD_Y_VERSIONADO.md, FLUJO_INGENIEROS_Y_CARPETAS.md y PRUEBA_FISICA_PENDIENTE.md. La identidad firmada de la candidata se detalla a continuación.


## Artefacto verificado de entrega

- APK: `output/GBO_RUTA_CLARA_CANDIDATA_1.4.9_23/GBO_Ruta_Clara_1.4.9_23_CANDIDATA.apk`.
- `versionName=1.4.9`, `versionCode=23`, variante **release**, APK única, sin instrumentación ni debuggable.
- `applicationId=com.tuempresa.inventariovial`.
- Certificado SHA-256 histórico: `0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315`.
- APK SHA-256: `508e68d1b3134d1b850a9dee60f4b35c9e3053e5d3073e4ea79edcce4f9756c6`.
- Firma y alineación verificadas sobre la copia entregada. Compilación ordinaria `:app:assembleRelease --offline --console=plain`, sin inicializadores ni banderas de pruebas; lint vital y R8 finalizaron correctamente (9 min 30 s).
- Rama conservada: `codex/observaciones-campo-manual-10I`. HEAD base: `5de1242581dc460ea38d45006020d022f8d4da78`. Los cambios siguen locales; este commit base no representa por sí solo el código de la APK. FUENTES_CANDIDATA_SHA256.json registra las fuentes/recursos efectivos, sin copiar configuraciones privadas.
- Verificadas intactas las cuatro APK previas comprobadas, incluidas las dos 1.4.8 (22) distintas que existían al iniciar la tarea. No se sobrescribió `app/release/app-release.apk`.

**Pendiente para autorizar distribución:** completar PRUEBA_FISICA_PENDIENTE.md en una tablet autorizada; comprobar apertura/ubicación/compartir y PDF nativo. La candidata local validada no equivale a aprobación de distribución general.
