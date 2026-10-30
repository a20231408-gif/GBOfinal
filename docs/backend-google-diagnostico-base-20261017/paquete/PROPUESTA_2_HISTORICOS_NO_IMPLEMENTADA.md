# Segunda propuesta: reducir descompresiones de históricos

**Diseño para revisión. No implementado.** No introduce cambios de producción, propiedades, permisos ni protocolo Android. No se incluye un motor que active esta caché.

## Problema que permanece

El borrador solo reutiliza `row.workbook` de filas entrantes aceptadas por `validate()` durante ese commit. Los históricos y las plantillas auxiliares de Observaciones continúan descomprimiéndose. Para N altas de una fila, mismo formato/ámbito, sin Observaciones heredadas: V20 usa `N + N(N+1)/2` unzip; el borrador usa `N(N+1)/2`. No elimina el crecimiento cuadrático acumulado. Las ediciones pueden descomprimir además el libro anterior y `observationsTemplateBase64`.

## Etapa A: memoización exclusivamente por ejecución

Después de recuperar todos los journals PENDING, crear un mapa en el almacén de esa ejecución, nunca en una variable global compartida. Clave: SHA-256 de los bytes realmente decodificados, fila inicial, versión del parser y del contrato de plantilla/Observaciones. Conservar `validate()` y la comparación de SHA declarada para entradas. No aceptar un valor de `row.workbook` proporcionado por el cliente: siempre lo reconstruye la validación.

Cachear únicamente el libro **crudo** descomprimido, antes de `inherit`, `select`, `plain` o del merge. Entregar copias defensivas de filas, partes y metadatos; una transformación de un consumidor no debe cambiar el siguiente. Reaplicar herencia de Observaciones contra el anterior correspondiente a ese UUID/formato en cada consolidación. Si se decidiera cachear el resultado efectivo, la clave tendría que incorporar también la huella del origen de la nota heredada; este diseño evita esa complejidad.

No memorizar manifiestos mutables sin invalidación. Las escrituras/restauraciones deben invalidar sus lecturas; la recuperación tiene precedencia sobre cualquier caché. Esta etapa evita repeticiones dentro del mismo commit, pero no elimina volver a abrir N fuentes históricas en el siguiente commit. Medir primero el ahorro incremental sobre el borrador actual.

## Etapa B: fragmentos derivados durables, bajo transacción

Solo si las mediciones Google demuestran que compensa frente a nuevas lecturas/escrituras Drive. Posible ubicación técnica futura dentro de `._GBO_SYNC_V3`; no se crea ahora.

El manifiesto original, XLSX base64 y sus hashes siguen siendo autoridad recuperable. Un fragmento incluiría versión del esquema/parser/Observaciones, SHA del origen, fila inicial, firma de plantilla, fila XML original, columna de nota, descriptor completo de partes y hash propio del contenido derivado. Las referencias por dueño incluyen key/UUID/formato/revisión aceptada. No basta una caché de la fila de texto: la plantilla, estilos, imágenes, relaciones y partes no modificadas deben poder reconstruirse exactamente.

Para cada lectura se comprueba identidad y hash de los bytes fuente decodificados, además del formato/versiones/hashes del fragmento. No se confía solo en el SHA escrito en un nombre. Fragmento ausente, incompatible, duplicado o corrupto: volver al XLSX fuente y regenerar. El origen corrupto conserva el rechazo correspondiente; una caché no debe ocultarlo. El hash del derivado detecta corrupción accidental, no prueba por sí solo que el XML deriva del XLSX frente a un escritor malicioso. Antes de habilitar hits durables debe demostrarse el supuesto de un único escritor autorizado y la integridad de los metadatos existentes, sin ampliar permisos; si no se puede garantizar, no habilitar esa etapa.

**Estrategia transaccional obligatoria:**

1. Mantener ScriptLock global y la recuperación PENDING antes de leer manifiestos, índices o fragmentos.
2. Validar entrada y seleccionar una generación de fuentes por UUID/revisión/hash. Construir los fragmentos crudos y el consolidado como preparación; no emitir ACK.
3. Extender explícitamente el journal para las nuevas referencias de caché, con antes/después y propiedad de cada archivo. Preparar copias/ID de archivos según un diseño revisado: no insertar escrituras de caché sueltas antes de `begin()`.
4. Publicar los archivos y Excel; comprobar sus hashes de lectura. Guardar manifiesto y referencias de generación en la misma transacción. Conservar número de carpeta, bindings y fileId ya existentes.
5. Marcar COMPLETE únicamente cuando todos los recursos requeridos están durables y verificados. El ACK actual continúa acreditando archivos, consolidado y revisión completa.
6. Si hay interrupción con PENDING, restaurar originales y referencias anteriores, invalidar la generación incompleta y reconstruir desde fuente cuando sea necesario. Los objetos temporales sin referencia no son autoridad ni un hit válido; no eliminarlos automáticamente durante recuperación de campo.
7. Tras COMPLETE y pérdida del ACK, devolver el mismo recibo al reintentar. No generar otro fragmento visible, número o archivo definitivo por el reintento.

No se modifica el significado del ACK ni se difiere el Excel a un trabajo posterior. No se libera el lock durante la publicación. Desplazar el merge fuera del lock exigiría otro diseño de generaciones/fencing, fuera de esta propuesta.

## Pruebas que deben preceder a su implementación/publicación

| Caso futuro | Invariante y resultado exigidos |
|---|---|
| Caché fría, caliente y ausente | Igualdad del ACK, XML y todas las partes XLSX; conteos antes/después, no solo tiempo. |
| 1, 5, 25 y 100 históricos; varias filas por muestra y formatos | Coste marginal documentado y memoria acotada; sin límite artificialmente retirado. |
| Edición del mismo UUID con SHA distinto/igual | Solo nueva revisión aceptada; mismo folderId/fileId; hit correcto por bytes, sin valor de otro registro. |
| GLOBAL 26 y referencias Android 1.4.7 | JSON y PDF originales conservados; los fragmentos SIC no inventan mediciones ni mapeos. |
| Nota nueva, legacy, heredada, vacía y borrada explícitamente | Herencia reevaluada; literal, sin fórmula, ni formato adicional 10K. |
| Plantilla antigua, nueva, incompatible, parser nuevo | Hit solo de versiones compatibles; fallback/rechazo equivalente. |
| Fragmento/índice/parte dañados, duplicados o de otro origen | Nunca ACK con contenido incorrecto; fallback fuente o error explícito. |
| Fallo al crear fragmento, escribir Excel, guardar referencias/manifiesto y marcar COMPLETE | PENDING recuperable, bytes anteriores íntegros, ninguna referencia parcial publicada. |
| Pérdida de respuesta y dos clientes intercalados | BUSY y reintentos estables; un único dueño/UUID y una fila final por identidad. |
| Retorno al código V20 | V20 ignora artefactos nuevos y sigue usando fuentes; no reinterpreta `record_`, `binding_` ni `journal_`. Probarlo, no asumirlo. |

Estas pruebas de la segunda propuesta **no se han ejecutado**: aún no existe su implementación. Las pruebas del microparche actual no validan este diseño futuro. La siguiente decisión es aprobar primero este contrato y sus fallos inyectados; después implementar en copia aislada, medir en Google y volver a revisar.
