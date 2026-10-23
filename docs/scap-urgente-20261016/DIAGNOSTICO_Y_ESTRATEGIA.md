# SCAP urgente — diagnóstico y estrategia, 16/10/2026

## Diagnóstico confirmado en las fuentes

| Síntoma | Causa y recorrido real | Clasificación / decisión |
|---|---|---|
| Cinco croquis, varios del mismo tipo | ScapMedia y ScapRepository.addSketch permiten N filas. ScapCanonicalExporter.review y ScapExcelExporter.review rechazaban grupos mayores que uno. ScapExportPanel deshabilita exportación; ScapDelivery.writeExcel/bundle y AutomaticDelivery.prepare también alcanzan esa validación. | Incompatibilidad demostrada entre captura y presentación. Corregir selección principal; conservar todos como anexos. |
| `#REF!` | ScapCanonicalExporter devuelve ese texto en `warnings`, separado de `errors`. Procede de la plantilla fuente. | Advertencia; no demuestra pérdida, fallo de Internet ni causa del bloqueo por croquis. No se modifican fórmulas ni plantilla. |
| SIC-17/17A/17B pendientes | ScapSicExporter.preview evalúa BridgeSicContract.required/valid. write exige missingRequired vacío; los botones contractuales estaban deshabilitados. Un dato inválido puede aparecer como faltante exportable porque preview lo omite; el original permanece en Room. | El bloqueo del **Excel contractual final** se conserva. Una salida provisional independiente permite gabinete sin inventar datos. |
| SIC complementario incompleto | AutomaticDelivery prepara SCAP y agrega a `sicRows` únicamente los SIC completos; los demás figuran en pendingSic. | Por sí solo, no bloquea SCAP principal. Se distingue de error de croquis, archivo dañado y ubicación. |
| SCAP con secciones pendientes | ScapRepository.complete admite pendientes con warningsConfirmed; ScapDriveSync.reviewForUpload exige confirmación y distingue BLOCKING. ScapExportPanel combina errores de estructura/valores y confirmación. | Campos ausentes: advertencia confirmable. Estructura/valor inválido o imagen ilegible: error específico. La ficha puede estar cerrada y seguir INCOMPLETE. |
| Progresiva/PR sin ubicación técnica utilizable | ScapHumanDelivery.prepare exige una ruta del catálogo y ChainageCalculator con PR/distancia calculable. AutomaticDelivery.technicalOrder añade las comprobaciones técnicas actuales. Esto ocurre antes de la entrega/reserva efectiva; 0/0 no acredita envío de archivos. | La copia provisional local funciona sin PR. No se inventa progresiva ni se asigna carpeta. No se implementa un nuevo respaldo remoto sin clasificación por falta de un contrato aprobado del servidor. |
| «Pierde datos sin Internet» | Los DAO usan Room local y transacciones. Los archivos se importan a almacenamiento privado. No se encontró una operación HTTP que borre o restaure campos desde un ACK. El ACK de revisión antigua está condicionado por revisión. | La causa exacta de la incidencia de campo **no está demostrada** sin versión/diagnóstico del equipo. Se investigan y prueban por separado cola, persistencia confirmada, navegación y recuperación. |
| Riesgo concreto de cola | El ScapController anterior continuaba las operaciones posteriores a un error y añadía la fallida a una lista reintentada al final. Un reintento antiguo podía quedar después de una edición posterior. Las acciones pendientes eran lambdas en memoria, ligadas al scope del ViewModel. | Riesgos de orden y pérdida de operaciones todavía no confirmadas demostrables localmente; no prueban que Internet sea la causa del caso real. Corrección y pruebas focalizadas, sin afirmar resuelto el incidente físico. |

`ScapCompletion` no evalúa la completitud contractual de los tres SIC con las mismas reglas que SCAP: son evaluaciones distintas. El nuevo respaldo incluye ambas listas sin presentar la inspección como contractual completa.

El acceso existente valida el PIN contra la caché local protegida de usuarios: no necesita HTTP para cada inicio de sesión. Una caché mayor de 72 horas puede exigir actualización antes de un nuevo login; no finaliza por sí sola una sesión ya abierta. Volver después de 15 minutos en segundo plano puede bloquear la sesión para pedir el mismo usuario, conservando su identidad. Son restricciones de acceso comprobables, **no borrado de datos ni causa confirmada** de este reporte. Se conservan sin modificar usuarios, PIN o política. El diagnóstico existente de última salida/memoria ayuda a distinguir cierre por Android, pérdida de visibilidad y fallos de almacenamiento.

## Versión afectada y estado de 1.4.8

El ingeniero identifica 1.4.7 en uno de los equipos; falta confirmar versionCode, modelo y Android de **cada** instalación. No se deduce el sistema Android por el modelo de una tablet anterior ni se identifica qué APK está instalada por su nombre visible. Se solicitó esa información sin pedir nuevamente las capturas.

El respaldo de fuentes 1.4.7 anterior a 1.4.8 contenía el rechazo por croquis; el desarrollo inicial 1.4.8 conservó ese exportador. La candidata 1.4.8 (22) **ya entregada no contiene este hotfix**. Sus pruebas anteriores no cubrían cinco croquis duplicados por tipo ni los escenarios nuevos de cola. Compilar aquella candidata no acreditaba resolver esta incidencia.

La inspección estática de los DEX de las tres APK conservadas encontró el mensaje de rechazo de croquis en **1.4.6, 1.4.7 y 1.4.8**. Por tanto, está presente al menos desde la candidata 1.4.6 disponible aquí; no se atribuye su introducción a 1.4.8 ni se identifica una versión anterior sin evidencia. Esta comprobación no identifica por sí sola el APK instalado en cada equipo.

Las fuentes de trabajo integran la corrección sobre los avances 1.4.8. No se cambia versión, applicationId, firma ni Room: esquema 20 sigue igual. No se genera otra APK ni se reemplaza la candidata anterior. Las APK 1.4.6/1.4.7/1.4.8 y respaldos se conservan.

## Implementación en revisión local

- Croquis principal por tipo en metadatos `scap_values`, owner `media`; no necesita migración. Uno solo conserva selección implícita compatible; varios sin selección dejan vacío el espacio contractual y generan advertencia. El operador elige explícitamente en E. Cambiarlo tras cierre exige reapertura y una revisión nueva; no se eliminan originales ni anexos.
- Exportadores canónico e histórico reutilizan esa selección. Bundle versión interna 8 conserva los N croquis y sus identidades; la versión interna de caché no cambia el protocolo productivo ni modifica las preparaciones Drive anteriores.
- Manifest SCAP incorpora captura estructurada de campos/elementos/defectos/observaciones, completitud y pendientes. No añade filas SIC provisionales al consolidado ni sustituye sus validaciones.
- Botones provisionales por SIC-17/17A/17B generan **ZIP**, no un XLSX oficial aparentemente válido: `ABRIR_PROVISIONAL.html`, `INSPECCION_PROVISIONAL.json`, fotos/croquis originales y manifiesto con hashes/faltantes. Marcación inequívoca, identidad/autor/fechas/revisión. Datos inválidos se conservan en JSON y se identifican para revisión. No es importador ni respaldo restaurable automático de Room. Los paquetes se publican usando el sistema de copias locales 1.4.8.
- El flujo de copias locales SCAP añade paquetes provisionales separados cuando los aportes SIC están pendientes; mantiene estado parcial y no inventa los XLSX definitivos.
- Cola local ordenada frente a errores, confirmación únicamente después de las operaciones, y journal privado AtomicFile para entradas de texto/selecciones `edit` y `editSic`. El journal conserva la última intención de cada campo, evita reintentar tokens superados y se elimina solo después del commit Room. Las acciones genéricas —crear filas, agregar medios, etc.— no se convierten mágicamente en operaciones recuperables antes de su commit: esperar la confirmación sigue siendo necesario.
- El procesamiento de escrituras aceptadas no depende de la red ni se cancela por abandonar el scope de la pantalla. Diagnóstico agregado de cantidad, tiempo y pendientes/fallos; sin UUID, texto de ficha, imágenes, rutas, PIN, tokens ni mensajes remotos.
- Se evita que un callback GNSS opcional de fotografía, recibido después del cierre, intente editar una ficha COMPLETE y convierta la foto ya guardada en un error de cola. No se reabre ni reescribe la revisión cerrada: conserva su estado/metadatos actuales; un fix tardío no se incorpora retroactivamente. Es una carrera concreta de asincronía, no una prueba de que Internet haya borrado la ficha de campo.

## Medidas inmediatas seguras para campo

1. No retirar ninguno de los cinco croquis para desbloquear el Excel. No reinstalar, desinstalar, limpiar caché/datos ni hacer downgrade.
2. Mantener la ficha original. Antes de salir de la pantalla, pulsar Guardar borrador cuando esté editable y esperar que desaparezca «Guardando…» y no exista error. Si está cerrada, no reabrir ni editar solo para probar; conservarla y registrar el estado observado.
3. Si aparece error de guardado, mantener la app y los valores visibles; no forzar cierre ni reiniciar para «solucionarlo». Informar el error y pedir asistencia. Una entrega Drive bloqueada no demuestra que Room esté vacío.
4. Desde el diagnóstico existente exportar el JSON agregado mediante **Exportar diagnóstico**. Puede compartirse sin terminal. Los nuevos contadores SCAP solo estarán en una compilación que incorpore este código; no se atribuyen retroactivamente a 1.4.7. Ese JSON no es respaldo de fichas.
5. Obtener un respaldo integral recuperable antes de cualquier actualización y con autorización del propietario. PDF/Excel/ZIP documentales no equivalen a ese respaldo. No se promete una herramienta de extracción integral que la APK instalada no tenga.
6. Falta de PR: no escribir cero ni estimaciones para forzar Drive. La nueva salida provisional local resuelve transferencia documental sin clasificación falsa; la entrega remota técnica permanece pendiente de ubicación válida o ampliación de servidor revisada y autorizada.

## Estrategia de hotfix

Primero validar las correcciones separadas. El núcleo de principal/anexos, persistencia y datos provisionales usa tablas ya existentes en 1.4.7 y puede evaluarse para un hotfix mínimo **sin la migración 19→20**. La integración de botones/Descargas reutiliza módulos propios de 1.4.8 y no debe copiarse a ciegas sobre 1.4.7.

No se construye una APK híbrida aquí ni se fuerza Room 20 a 19. Con la identificación de las instalaciones y la aprobación del propietario, elegir una candidata nueva con versionCode superior que corresponda a la base elegida y conserve firma/identidad. Para una tablet que ya haya migrado a 20 se requiere continuar hacia adelante. El paquete fuente separa los archivos y las dependencias para revisar esa decisión.

## Aceptación pendiente

Se ejecutan pruebas locales de cinco croquis, principal/anexos, provisionales, offline simulado, reabrir SQLite, revisión/ACK, BUSY/pérdida de respuesta y regresiones relacionadas. Los resultados finales se adjuntarán en esta carpeta. **Reabrir SQLite en Robolectric no es reiniciar una tablet.** No se ha intervenido un dispositivo real ni escrito en Google.

La incidencia de pérdida offline no se declarará resuelta en campo sin comparar campos/medios confirmados antes/después en equipo de prueba: captura A–G, 19 fotos/5 croquis, guardado concluido, navegación, cierre/reapertura, reinicio real offline, pérdida/recuperación de sesión y reconexión. Repetir con borrador y revisión reabierta; guardar identidades/hashes de prueba y verificar que ACK antiguo no marque entregada la edición nueva. No usar la ficha real del ingeniero para ensayos destructivos.
