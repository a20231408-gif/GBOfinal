# Correcciones de campo — GBO Ruta Clara — 23/10/2026

Las correcciones Android están implementadas y compiladas para pruebas JVM/Robolectric: **83 casos distintos aprobados**, sin fallos pendientes en esas comprobaciones. Hay **3 pruebas Node aisladas aprobadas** del parche V20, con XLSX reales y Drive simulado. No se ejecutaron pruebas físicas ni Google, no se generó APK y se conserva **1.4.12 (26)**.

**La reproducción integral de Drive sigue PARCIAL:** el espejo local acredita los archivos preparados de la ficha mediante su ACK completo, pero no dispone de los bytes del consolidado global `EXCEL_SIC` de otras tablets. Tampoco inventa nombres de videos históricos sin confirmación V2 propia. El paquete local de más de 32 fotos funciona sin alterar F.2; la exportación remota conserva sus límites actuales. No presentar esos límites como resueltos en producción.

Checkpoint recuperable: `607d499300859321aa7a282b10ee7dfde17d14f8`, tag local `checkpoint-campo-20261023-antes`. Rama de trabajo: `codex/correcciones-campo-20261023`. Respaldo adicional de las fuentes anteriores en `tmp/campo-20261023/FUENTES_ANTES.zip`. Los cambios nuevos permanecen locales, sin commit/push ni publicación de servidor.

## Estado por observación

| # | Causa verificada y corrección | Evidencia / estado |
|---|---|---|
| 1 | `LocalDocumentLocation` imponía carpetas descriptivas y `LocalExportBuilder.add` añadía hashes a nombres de medios. Las copias generadas localmente pasan a `_PROVISIONALES`, con UUID estable. `DriveMirrorPlan` replica las rutas y bytes preparados solo tras `AutomaticDelivery.validAck`, con SHA comprobado. Videos V2 requieren su propia confirmación. | **PARCIAL:** pruebas de dos identidades/tablets ficticias, ACK tardío, bytes alterados, reexportación y revisión histórica. Consolidado global y nombres de videos V1 no se inventan. |
| 2 | Android ordenaba por calzada/sentido, el consolidado SCAP por eje GNSS y PavementSic conservaba el orden de captura de daños. Se añade `SicTableOrder`: PR numérico, metros y UUID; cada ruta sigue en su libro/hoja. | **Resuelto localmente.** XLSX SIC-19 y SIC-15 reales comprobados; parche V20 separado probado con SIC-18 real. Google requiere revisión/publicación autorizada. |
| 3 | El staging seguía el orden del manifiesto, que podía situar fotos antes que documentos. `DocumentTransferOrder` cambia solo la iteración HTTP: XLSX/PDF, otros archivos, fotos y videos. | **Resuelto para archivos en staging.** Se comprueba el adaptador HTTP, hash/manifiesto intactos y reutilización de stages tras perder la respuesta del commit. Las filas SIC van en `sicRows`, dentro del commit final; no se adelanta su ACK. |
| 4 | La preparación y los journals ya reutilizan revisiones/stages. Las evidencias Google disponibles incluyen sobrecarga de aislamiento de laboratorio, no un benchmark productivo comparable. | **Sin mejora remota acreditada.** No se añadieron paralelismo, reintentos agresivos ni optimizaciones especulativas. Véase RENDIMIENTO_Y_LIMITES.md. |
| 5 | El diálogo inicial mostraba `Double.toString()`. Ahora presenta X1/Y1/X2/Y2 con dos decimales según locale y acepta punto/coma. Registra si se editó el campo para conservar íntegro el Double original cuando no se toca. | **Implementado y probado en JVM.** No se cambia geometría, cantidad, PCI ni precisión persistida. Pendiente prueba física del diálogo. |
| 6 | Había tres tarjetas y formularios debajo, dentro de un mismo scroll. Ahora hay pestañas fijas SIC-17/17A/17B y solo un formulario visible, con su scroll independiente. | **Implementado y probado en Compose/Room:** edición, cambio de pestaña, persistencia y una sola inspección. |
| 7 | Gran parte de las equivalencias ya existía en el mapper; faltaba respaldo del código/nombre persistidos y visibilidad de discrepancias. Se conserva la proyección antes de exportar, se añade ese respaldo y se advierte cuando una edición SIC difiere de SCAP. | **Implementado para equivalencias documentadas**, sin sobreescritura manual ni campos inventados. Matriz en MATRIZ_CAMPOS_SCAP_SIC.md. |
| 8 | Revisión repetía todos los datos y formularios SIC, fotos e históricos. Se deja resumen, pendientes con acceso y acciones superiores. Datos extensos, diagnóstico y archivos/historial requieren expansión explícita. | **Implementado; Compose local aprobado.** El estado Drive compacto permanece separado del estado de copia local. No se borraron historiales. |
| 9 | Dos exportadores imponen 32; `ScapTemplateMedia` escribe 32 filas y dispone de anclajes limitados. Videos están en otra base y no se cuentan como fotos. | **Resuelto para paquete LOCAL:** F.2 original con las primeras 32, CONTINUACION_FOTOGRAFICA.html y todas las evidencias en ZIP. Prueba con 33 originales y hashes intactos. **Remoto pendiente:** no se relajó el límite contractual ni su validación. |
| 10 | «VER EXCEL SIC / SCAP» solo cambiaba a Revisión: no generaba archivos. Se sustituye por EXPORTAR SIC (elige formato) y EXPORTAR SCAP, visibles sin recorrer el formulario. | **Implementado.** Prueba UI de generación SCAP real y apertura de ACTION_CREATE_DOCUMENT. Abrir el selector no se anuncia como copia verificada. Después de verificar el destino elegido se ofrece Abrir último Excel guardado. Exportadores SIC conservados y probados localmente. |
| 11 | El texto genérico provenía de `ScapSicPreview.missingRequired`, no prueba por sí solo ausencia del catálogo PR. Se nombran los campos requeridos y la alternativa provisional, diferenciándola del consolidado contractual. | **Implementado.** No se alteró el cálculo de longitud entre PR ni se inventaron equivalencias. |
| 12 | Se conservan busy/picking de SicExportScreen, busy de PavementOutputPanel, cola persistente e idempotencia de copias. En SCAP se añaden acciones deshabilitadas/progreso superior, validación y resultado visible; guardar/cerrar se protegen durante escritura/exportación. | **Validación focalizada, no auditoría completa de todos los botones de la aplicación.** Compose prueba bloqueo de acciones, fuente grande y exportación real; faltan verificaciones físicas del conjunto de flujos. |

## Etapas y límites

A/P0: documentos locales y reconciliación segura implementados. La copia integral de Drive sigue limitada por los bytes globales no disponibles y recibos históricos de video; no se disimula con un Excel de una sola ficha. La continuación fotográfica es documental y LOCAL: no altera la plantilla contractual ni las reglas remotas. No se mezcló ese trabajo con un despliegue pendiente.

B/P1: orden tabular y datos SIC implementados en Android. El `.gs` completo está en `backend-parche`, como propuesta separada. C/P2: pestañas, acciones y revisión compacta implementadas. D/P3: diagnóstico acotado, sin afirmar una aceleración Google no medida.

## Cómo usar y reconciliar

1. Guardar la ficha y pulsar **Guardar en Descargas** o **Guardar paquete ZIP**. Sin Internet se conserva una copia con identidad provisional, nunca un correlativo global inventado.
2. Para más de 32 fotos, **EXPORTAR SCAP** ofrece el paquete local con continuación; revisar el resultado persistido en **Abrir archivos locales e historial**. Los requisitos distintos de las fotografías siguen validándose; no se declaran completos campos desconocidos.
3. Cuando la revisión tenga ACK íntegro, abrir sus archivos locales y pulsar **Reconciliar con Drive confirmado**. Se comprueban también revisión actual y bytes SHA. No se encola sincronización por consultar/copiar.
4. Una segunda publicación de los mismos bytes reutiliza el destino. Si ya consta una versión publicada con bytes distintos en esa ruta, la revisión nueva se publica aparte bajo `_REVISIONES_CONFIRMADAS/R_<revision>/`, conservando el árbol relativo y la copia anterior. Esto es un archivo de revisión local, **no un traslado ni una copia física en la raíz final**. Si Android detecta conflictos no conocidos, se deja parcial y se informa; no se sobrescribe.
5. Los consolidados globales deben obtenerse del Drive confirmado mediante un procedimiento de lectura autorizado. El recibo `sicFiles` contiene identidad/hash, no sus bytes. No se descargó producción ni se agregó un endpoint.

Ejemplo ficticio de espejo acreditado:

```text
Descargas/GBO Inventario Vial/
  PE-28H/PUENTES/PUENTE 1/GENERAL/F01.jpg
  PE-28H/PUENTES/PUENTE 2/GENERAL/F01.jpg
  PE-28H/PUENTES/PUENTE 2/VIDEOS/GENERAL/001_PUE2_GEN.mp4  # solo con ACK propio
  _PROVISIONALES/PE-28H/ALCANTARILLAS/<UUID>/REVISIONES/R_<revision>/...
  _REVISIONES_CONFIRMADAS/R_<revision>/PE-28H/PUENTES/PUENTE 1/...
```

Los números definitivos proceden de `SyncV3.allocateNumber` y sus bindings bajo ScriptLock. Android tiene números/orden locales de presentación, pero no puede acreditarlos como numeración global. `ScapHumanDelivery.plan` conserva su contrato actual: su orden de presentación no sustituye al ACK. Los originales privados mantienen nombres técnicos/UUID; el transporte usa copias preparadas. El espejo usa exactamente el nombre público confirmado, incluso si es técnico. Cambiar nombres físicos remotos requiere una convención/autorización aparte.

## Pruebas, previews e integridad

83 pruebas Android distintas con resultado final PASS; 3 pruebas Node aisladas PASS. Se ejecutaron solo clases relacionadas, sin suite general. El primer ensayo detectó una expectativa incorrecta sobre el separador decimal es-PE, dos expectativas antiguas de nomenclatura y un error al guardar el XLSX del simulador Node y una lectura de prueba SIC-15 que asumía referencias de celda explícitas. Se corrigieron esas pruebas; no se ocultan como fallos productivos. Los resultados por clase están en evidencia/PRUEBAS_ANDROID.json.

Previews revisados de componentes Compose reales con datos ficticios, **no capturas de tablet**: [SIC-17A](previews/01-sic17a-pestanas-reales-compose.png), [acciones durante preparación](previews/02-acciones-reales-compose.png), [teléfono con fuente grande](previews/03-telefono-fuente-grande-compose.png). No se observaron controles solapados o fuera de vista en esos componentes; no acreditan la pantalla física completa.

Ejemplos reales generados y abiertos estructuralmente: [SIC-19 local](ejemplos/SIC-19-ORDENADO-FICTICIO.xlsx) , [SIC-15 local](ejemplos/SIC-15-ORDENADO-FICTICIO.xlsx) y [SIC-18 consolidado simulado](backend-parche/SIC-18-CONSOLIDADO-FICTICIO.xlsx). El ejemplo numérico termina 0000+301, 0000+820, 0002+229, 0011+000, 0012+058. A/B/C/D termina A/B/D/C sin renumerar carpetas.

Se verificaron por SHA las cinco propiedades locales y las 14 APK anteriores: intactas. Ninguna fuente original de Apps Script, credencial, endpoint, versionado, firma, esquema Room o migración fue cambiado. Se añadió una consulta DAO, sin cambiar tablas. Sin instalación, envíos reales, cambios en Drive, usuarios o servicios. Avisos AGP/JDK preexistentes no bloqueantes. No hay aprobación para una nueva Release ni para distribución.

Prueba física pendiente: actualizar solo con APK expresamente autorizada; abrir ficha ficticia, editar SIC en tres pestañas, girar/reabrir, exportar SIC y SCAP, cancelar/confirmar selector, probar 33 fotos y revisar ZIP, coordenadas sin editar y editadas, abrir copias y reconciliar solo después de un envío real separadamente autorizado. Conservar originales y conciliar archivos/ACK. No usar datos de ingenieros para esas escrituras.

Las dos fotografías de referencia mencionadas no estaban incluidas como imágenes en el adjunto de esta tarea. Se contrastaron los ejemplos textuales con el código; no se afirma haber inspeccionado esas capturas.
