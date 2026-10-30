# Decisión productiva — permanece PENDIENTE

La preparación de este ZIP no autoriza una publicación. Primero deben satisfacerse todos los controles de integridad; una mejora de tiempo nunca compensa una divergencia de datos.

## Rechazo inmediato

- Cualquier escritura fuera de la subraíz de ensayo, referencia externa aceptada, secreto en logs/evidencias o mezcla de variante/run/epoch.
- UUID o revisiones perdidas, duplicación de padres/fotos/videos/filas, cambio de identidad/carpeta en un replay/edición, ACK sin bytes/hash correctos o journal PENDING sin recuperación.
- Observaciones literales alteradas, fórmula ejecutada, estilo añadido a su contenido, nota histórica perdida o celda que no se vacía por edición explícita.
- Cambio de número estable/carpeta al editar; orden de filas distinto de A/B/D/C en el caso base; alteración de estructura/partes de plantilla fuera de la consolidación prevista.
- No compatibilidad de V3/V4, reservas V2, SCAP_ONLY o begin/finish/checksum; rechazo inesperado de una petición válida de 1.4.7/1.4.9.

## Evidencia obligatoria antes de autorizar

1. Inventario privado y respaldo de los ocho módulos V20, versión desplegada y manifiesto original revisado. El ZIP no certifica este último porque todavía falta.
2. BASE y CANDIDATO ejecutados en Google en el mismo proyecto de pruebas, sin ejecuciones activas durante el cambio; resultado íntegro del comparador y revisión visual de los Excel/PDF/fotos/video ficticios.
3. UPGRADE real sobre historial generado por BASE: misma carpeta y UUID, revisión 3→4, replay idéntico, stale rechazado y demás padres intactos. Revisar además históricos de plantillas aprobadas SIC-15/16/SCAP y retención de formatos; los fixtures SIC-14/SIC-18 no certifican todos los formatos.
4. BUSY real observado, posterior ACK correcto de todos los padres y ningún reintento infinito. Ensayo de pérdida de respuesta registrado como inyección del cliente, no presentado como caída física de Internet. Ensayar una interrupción/cancelación controlada en Google y recuperar el journal antes de dar por cubierta la terminación abrupta.
5. Las solicitudes válidas deben conservar las respuestas. Aceptar expresamente las diferencias conocidas de precedencia de errores inválidos; no llamarlas equivalencia universal.
6. Cero duplicados: una versión activa por key, UUID de fila una sola vez, archivo lógico único y video con el mismo fileId al reintentar. Conservar revisiones y respaldos técnicos intencionales; no contarlos como duplicados de entrega.

## Criterio cuantitativo propuesto para la optimización

Fijar antes del ensayo: al menos tres pares comparables de carga 30/familia; al menos 30 commits válidos medidos por familia y variante, aparte de replay/diagnóstico. Comparar medianas por corrida, p95 y tiempo total hasta ACK por familia, con rango y tamaño de muestra. El usuario puede adoptar otros umbrales antes de medir, no elegirlos después para forzar aprobación.

- Éxito final 100% del conjunto ficticio, sin falsos ACK, duplicados ni cuotas/fallos no recuperados.
- Reducción verificable del número de unzip del candidato para el mismo corpus, sin aumento no explicado de escrituras funcionales Drive.
- Para recomendar publicación por rendimiento: mediana de tiempo bajo ScriptLock menor al menos **10%** en el lote completo, y ningún p95 normal de commit/tiempo hasta ACK por familia empeora más del **10%**, en los tres pares o con explicación de una variación ajena corroborada por logs y una nueva medición comparable. Excluir el BUSY provocado de esos percentiles; incluir los BUSY naturales y su costo total en la conciliación del lote.
- Si el ahorro solo aparece en Node, el tiempo Google es ruidoso, no mejora de forma consistente o el bloqueo dominante está en otro punto, **no recomendar reemplazo por rendimiento**. Conservar V20 y la evidencia; no agregar optimizaciones especulativas.

## Publicación y reversión futuras

Solo tras cumplir lo anterior y aprobación del propietario: usar los dos archivos candidatos originales revisados (los indicados por sus SHA fuente), no las copias instrumentadas/aisladas de este ZIP. No sustituir Codigo, usuarios, APK, propiedades, raíz o manifiesto productivo. Crear versión nueva y actualizar la **implementación existente**, manteniendo URL; preparar antes la vuelta a V20. Coordinar final de trabajos activos y conciliar UUID/ACK tras cada cambio. Volver a V20 no implica borrar carpetas, journals, reservas o entregas nuevas.

Estado actual: pruebas locales acreditadas; **mediciones y aceptación Google pendientes, autorización productiva no concedida**.
