# Entregas Drive — En la carga y Mis entregas — 21/10/2026

**Implementado y validado en fuentes: 26 pruebas dirigidas aprobadas.** La APK candidata anterior se conserva y no se recompila en esta tarea.

## Comportamiento

Al abrir Entregas Drive se selecciona **En la carga**. Solo muestra las entregas que requieren seguimiento: pendiente, ejecución activa, verificación de ACK o atención por error/bloqueo. Usa la confirmación estricta ya implementada para la revisión actual, no la cantidad de archivos preparados o copiados localmente.

Cuando llega un ACK completo y válido, la entrega deja de mostrarse en la cola. No se elimina ningún registro, archivo, recibo, preparación o metadato. **Mis entregas** permite consultar todas las entregas persistidas en esta tablet, incluidas las confirmadas y las incompletas. La estructura existente conserva una fila por identidad de ficha, su revisión actual y el último ACK: una edición muestra la revisión nueva pendiente y conserva la confirmación anterior en detalles técnicos.

| Situación de la revisión actual | En la carga | Mis entregas |
|---|---|---|
| Pendiente / esperando conexión o reintento | Visible | Visible |
| Preparación o subida en curso | Visible | Visible |
| Resultado incierto / ACK perdido o incompleto | Visible, verificando | Visible, verificando |
| Error o bloqueo | Visible, con atención y motivo técnico | Visible |
| Confirmación completa válida | Se oculta del filtro | Permanece confirmada |
| Nueva revisión tras confirmar otra | Vuelve a aparecer | Muestra la revisión pendiente |

Cola vacía: «No hay entregas pendientes. Puedes consultar tus archivos enviados en Mis entregas».

## Identificación, búsqueda y orden

Se reutilizan el nombre descriptivo, la tarjeta y el validador de estados de la mejora anterior. Se añade la etiqueta del formato real de ficha (por ejemplo SIC-20, Ficha SCAP o Pavimentos). UUID, hashes y revisiones permanecen en Detalles técnicos. Archivos y Destino confirmado siguen expandibles. Se conserva una tarjeta por ficha/entrega, aunque tenga varias fotografías o documentos.

Mis entregas incorpora búsqueda por nombre/ruta/progresiva y formato, sin distinguir mayúsculas o tildes, más filtros **Todas / Pendientes / Completadas / Necesitan atención**. La búsqueda y estos filtros no afectan a la cola. El historial se ordena por la fecha más reciente disponible de la revisión o confirmación; se desempata mediante la identidad estable, sin usar el nombre como clave. Los contadores históricos sin fecha interpretable no reciben una fecha inventada. En la cola se prioriza atención/verificación/actividad, conservando el orden reciente dentro de cada grupo.

## Implementación y conservación

`DriveSyncCenter` observa una única lista Room de entregas y calcula una única representación de estado por identidad. Entrega esas mismas filas a `DriveDeliveryLists`. `DriveDeliveryListPresentation.select` solo selecciona y ordena la lista en memoria; no llama a DAO de escritura ni prepara/envía archivos.

Cambiar de sección, buscar o desplegar detalles no encola trabajos. Los botones explícitos de sincronizar/reintentar conservan sus callbacks existentes. No se añade un archivo histórico separado, otra cola ni una migración.

Archivos modificados:

- `app/src/main/java/com/tuempresa/inventariovial/core/DriveSyncCenter.kt`: integra las dos secciones compartidas.
- `app/src/main/java/com/tuempresa/inventariovial/core/DriveDeliveryPresentation.kt`: añade al modelo de presentación la etiqueta de formato; no cambia el validador ACK ni los estados existentes.
- `app/src/main/java/com/tuempresa/inventariovial/core/DriveDeliveryCard.kt`: muestra la etiqueta de formato, conservando sus detalles y acciones.
- Nuevos: `core/DriveDeliveryListPresentation.kt` y `core/DriveDeliveryLists.kt`.
- Pruebas nuevas: `app/src/test/java/com/tuempresa/inventariovial/DriveDeliveryListsTest.kt` y `DriveDeliveryListsUiTest.kt`.

Se conservan los nombres descriptivos de carpetas locales, SIC-20, archivos originales, protocolos, WorkManager, reintentos, ACK, Room, UUID y configuración privada. No se modifican Apps Script, carpetas Drive, usuarios ni datos reales. No se hicieron commit/push, instalación ni distribución.

## Validación y ejemplos

**26 pruebas locales aprobadas, 0 fallos y 0 omitidas. BUILD SUCCESSFUL.** `:app:testDebugUnitTest` ordinario y focalizado, con JVM/Robolectric, Compose nativo y Room/SQLite reales sobre datos ficticios. No se ejecutó la suite general ni se generó una APK.

| Clase | Casos |
|---|---:|
| DriveDeliveryListsTest | 6 |
| DriveDeliveryListsUiTest | 3 |
| DriveDeliveryPresentationTest | 8 |
| HotfixBusy146Test | 4 |
| SyncRecovery10DTest | 5 |

Los 6 casos nuevos de lógica y persistencia verifican presencia en ambas listas, retirada solo del filtro tras ACK válido, nueva revisión, ACK perdido/incompleto, bloqueo y BUSY, búsqueda/orden/filtros, nombres iguales, tres fotografías agrupadas y reapertura de una base Room en disco conservando todas sus entregas, preparaciones y recibos. Las 3 pruebas Compose nuevas verifican transiciones reactivas, detalles expandibles, filtros que no alteran la cola, mensaje de cola vacía con fuente grande y cero llamadas de reenvío al consultar/cambiar/buscar. Se conservaron y ejecutaron 8 casos previos de presentación/ACK, 5 de recuperación de sincronización y 4 de BUSY con transportes ficticios.

Se revisaron visualmente los tres previews: [En la carga](cola-historial-drive-20261021/previews/01-en-la-carga.png), [Mis entregas](cola-historial-drive-20261021/previews/02-mis-entregas.png) y [cola vacía en teléfono con fuente grande](cola-historial-drive-20261021/previews/03-cola-vacia-telefono.png). La vista de historial muestra una entrega confirmada y otra pendiente con igual nombre, como tarjetas independientes. No se observaron recortes de texto ni solapamientos; el listado conserva su desplazamiento vertical. Son previews de los componentes compartidos, sin la envoltura completa del diálogo de producción.

Conservación comprobada por SHA-256 contra el estado inicial de esta tarea: **785 fuentes/configuración de proyecto**, con cambios exclusivamente en los 3 archivos de presentación previstos; **5 propiedades privadas y 13 APK anteriores idénticas**, sin APK nuevas. Respaldo byte por byte de los tres originales en `tmp/cola-historial-drive-20261021/FUENTES_ANTES.zip`. Los cuerpos existentes de validación ACK y cálculo de estado se compararon literalmente y permanecen iguales.

Evidencias: [RESULTADOS.json](cola-historial-drive-20261021/RESULTADOS.json), [PRUEBAS.json](cola-historial-drive-20261021/PRUEBAS.json), [huellas de fuentes](cola-historial-drive-20261021/FUENTES_SHA256.json) y log Gradle en esa carpeta. Avisos no bloqueantes preexistentes de opción AGP experimental y acceso nativo/JDK; no hubo errores de compilación.


Los ejemplos se renderizan desde los componentes Compose reales, con el tema GBO y datos ficticios bajo Robolectric. Son previews locales; no capturas de una tablet ni pruebas Google.

## APK y siguiente paso

La candidata Release 1.4.11 (25) anterior, con SHA-256 `cd254fa0e42e3ca10e47d5a55bfe4c59abaf9ecb30d357c4c9f92bb2937ed2e3`, se conserva en `output/GBO_RUTA_CLARA_CANDIDATA_1.4.11_25_ENTREGAS_DRIVE_20261020/`. **Todavía no contiene esta separación de cola/historial.**

No se generó una APK nueva. Esta mejora debe incorporarse en una próxima compilación expresamente autorizada, sin sobrescribir la candidata anterior y confirmando entonces la numeración disponible.

**GO para integrar esta mejora en la siguiente candidata autorizada.** Pendiente de compilarla y comprobarla físicamente antes de distribuir. Al probar: abrir En la carga y Mis entregas sin iniciar envío, contrastar la misma ficha/estado en ambas, comprobar el mensaje vacío y buscar por ruta/PR. Una prueba con ACK real requiere autorización separada y datos ficticios; estas simulaciones no se presentan como confirmaciones Google.
