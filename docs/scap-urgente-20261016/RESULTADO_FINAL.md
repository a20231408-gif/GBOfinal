# SCAP urgente — resultado local y decisión

**Fuentes corregidas y compilación Kotlin aprobada. 82 pruebas distintas aprobadas; cero fallos pendientes y cero omitidas. No se generó, publicó ni instaló una APK nueva. No está aprobado para distribución ni se declara resuelto el incidente físico de pérdida offline.**

## Estado por incidencia

| Tema | Estado y resultado |
|---|---|
| Cinco croquis | **COMPLETADO localmente.** Varios por tipo admitidos; todos siguen como anexos. Principal explícito para el espacio existente del Excel; sin selección, WARNING y espacio vacío. Un único croquis conserva compatibilidad. |
| `#REF!` | **CONFIRMADO:** el exportador lo devuelve como advertencia de plantilla, no como error bloqueante ni fallo de conexión. Plantillas/fórmulas no modificadas. |
| SCAP incompleto | **COMPLETADO localmente:** cierre y respaldo con confirmación de pendientes; captura estructurada y pendientes en manifest. Errores reales de estructura/valores/medios siguen bloqueando y se distinguen de faltantes. SIC complementarios incompletos no bloquean por sí solos el SCAP principal. |
| SIC-17/17A/17B provisional | **COMPLETADO localmente:** ZIP independiente, marcado PROVISIONAL / INCOMPLETO / NO VALIDADO, HTML legible, JSON con identidad/autor/fecha/revisión y medios originales. No es un XLSX oficial ni se incorpora al consolidado. Falta de campos no obliga a inventar datos. |
| SIC definitivo en gabinete | **APROBADO en ensayo ficticio:** se completaron los tres formatos, se exportaron XLSX válidos y se conservaron identidad, croquis y provisional anterior. Las validaciones contractuales permanecen. |
| Posible pérdida offline | **PARCIAL:** no se reprodujo eliminación de datos confirmados en Room. Sí se corrigieron riesgos concretos de orden de reintento, operaciones pendientes en memoria y callback GNSS posterior al cierre. Se añadió journal privado de texto/selecciones y diagnóstico seguro. Reinicio y conectividad en dispositivo real siguen pendientes. |
| SCAP sin ubicación técnica | **PENDIENTE de información/contrato:** respaldo provisional local disponible; Drive técnico continúa exigiendo ubicación calculable. No se inventó PR ni se creó una carpeta provisional remota. Un contrato adicional de servidor requeriría revisión/autorización separada. |
| Versiones de campo | **PENDIENTE:** falta confirmar Android/modelo y versión/versionCode exactos de tablet y celular. Las APK conservadas 1.4.6/1.4.7/1.4.8 contienen el rechazo antiguo; esto no identifica automáticamente la APK instalada. |
| Candidata 1.4.8 entregada | **PRESERVADA:** mismo hash y APK. No contiene este hotfix. El código de trabajo sí integra la corrección sobre sus avances, sin nueva migración Room ni cambio de versión. |

## Evidencia de pruebas

Hay **15 casos nuevos** y **67 regresiones focalizadas**, contados por clase/método sin sumar repeticiones. Las nuevas pruebas cubren:

- 19 fotos y cinco croquis, selección principal/anexos, espacios y estilos del Excel, conservación de originales y bundle repetible sin duplicados.
- Tres provisionales con pendientes, archivo corrupto identificado por UUID, ausencia de clasificación falsa sin PR y finalización contractual posterior de los tres SIC.
- Reapertura SQLite con campos/medios/revisión y hashes iguales; journal recuperado sin depender del scope de pantalla; error de cola seguido de reintento ordenado; journal inaccesible sin caída ni confirmación falsa; semántica de campos dependientes conservada.
- Servidor simulado: desconexión, BUSY sin bandera retryable, pérdida de respuesta después de commit, reenvío de los mismos bytes/revisión y ACK completo; cinco anexos y UUID conservados, sin SIC incompleto en `sicRows`.
- Callback GNSS tardío que no reescribe una ficha cerrada ni bloquea la cola; diagnóstico sin valores privados y claves JSON explícitas estables frente a renombrado R8.

Regresiones: BridgeSicFlow (5), integridad 1.4.6 (13), builders locales 1.4.8 (4), migración 19→20 (1), Pavimentos rígidos 1.4.7 (9), Fotos V2 ordinario con transporte ficticio (5), exportación canónica SCAP (5), ACK/Drive SCAP (5), SCAP completo (2), disposición humana (3), persistencia SCAP (7), videos (3) y acceso/sesión offline (5).

Los XML y tiempos **locales** están en `evidencia/PRUEBAS.json` y sus subcarpetas. La primera compilación tuvo dos paréntesis sobrantes en botones de navegación; se corrigieron antes de ejecutar los ensayos. Las compilaciones posteriores y las tandas focalizadas terminaron correctamente. Los mensajes de decodificador de imagen incompleta corresponden al archivo deliberadamente corrupto de prueba, cuyo rechazo pasó.

**No son mediciones Google ni pruebas físicas.** No se ensayó reinicio Android, visibilidad/lectores/SAF de una tablet, sincronización productiva ni actualización de APK. Reabrir SQLite y recrear el controlador no sustituyen esos ensayos. No se demuestra la causa exacta de lo que reportó el ingeniero.

## Conservación y entrega

Se verifican por hash las APK 1.4.6, 1.4.7 y 1.4.8, las cuatro configuraciones privadas, Room/esquema 20, versión y archivos protegidos de Apps Script y su optimización. `AutomaticDelivery.kt` y el protocolo actual no se reescribieron. No se cambiaron firma, applicationId, usuarios, despliegues, Drive ni registros reales. No hubo commit ni push.

El paquete `output/SCAP_URGENTE_20261016_SOLO_FUENTES_NO_APK.zip` contiene fuentes completas de esta corrección, diferencias para revisión, informes, XML y ejemplos exclusivamente ficticios. No contiene keystore, propiedades privadas ni APK. El detalle exacto está en `evidencia/FUENTES.json`, `CONSERVACION.json` y `PAQUETE.json`.

Los ejemplos en `ensayo-ficticio` incluyen bundle SCAP, tres provisionales, tres XLSX contractuales y comparación de datos antes/después de reabrir SQLite. Los ZIP y hashes del manifiesto se comprueban nuevamente al empaquetar. No usar esos valores como datos de inventario.

## Decisión operativa

**No distribuir como solución de campo todavía.** Conservar la ficha real; no retirar croquis, forzar PR, borrar datos ni reinstalar. Obtener diagnóstico agregado y versiones exactas; revisar `DIAGNOSTICO_Y_ESTRATEGIA.md` y elegir la base de hotfix con respaldo recuperable.

El núcleo puede evaluarse para 1.4.7 sin agregar la migración de copias locales; su interfaz de exportación requeriría adaptación separada. La integración actual utiliza 1.4.8 y conserva esquema 20. Ningún downgrade es parte de la solución. La prueba física pendiente está en `PRUEBA_FISICA_PENDIENTE.md`; cualquier instalación o escritura productiva exige autorización posterior.
