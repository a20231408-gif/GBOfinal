# Archivos de esta reparación

Actualizar en el editor **de PRUEBAS**, por el propietario, únicamente:

| Archivo | Cambio |
|---|---|
| BASE_V20_PRUEBAS/TestIsolation.gs | Revisión 2; getters de ID locales, caché de lecturas acotada a exclusión válida, invalidación en mutaciones y cadena fresca obligatoria en cada escritura, incluida ascendencia real de la subraíz. |
| BASE_V20_PRUEBAS/TestHttp.gs | Marca/trace seguros, capacidad inflight, inspección exclusivamente lectora de recibos/journals/checksums/staging. Sin recuperación ni modificación de contador en esa inspección. |

Los mismos dos archivos se entregan **idénticos** en CANDIDATO_OPTIMIZADO_PRUEBAS; mantenerlos para una comparación posterior, sin cambiar ahora la variante BASE.

En la computadora, usar el ejecutor de esta carpeta nueva:

| Archivo | Cambio |
|---|---|
| ejecutar.cjs | Progreso por etapa/familia/padres, resúmenes seguros del ACK, validación de respuesta vinculada, condiciones de fallo, timeout clasificado, parada ante resultado incierto, drenaje de ambos clientes, guardado de ACK antes de replay e intentos separados para recuperación. |
| recuperacion.cjs (nuevo) | Comprobación offline de las tres evidencias y fixtures; diagnóstico lector; pin de corrida/epoch; permiso de reanudación condicionado a reconciliación viva y video idéntico. |
| EJECUTAR_GOOGLE.ps1 | Modo D predeterminado; R explícito y N separado. Entrada oculta de credencial exclusiva, conservación del entorno y salida nueva, sin sobreescribir la fuente. |
| comparar.py | Rechazo de continuación como benchmark o de helpers distintos entre BASE/CANDIDATO. |

El resto de los ocho módulos de cada variante, manifiesto y fixtures se incluyen para mantener el paquete autocontenido pero **no se cambiaron y no deben reemplazarse en el editor para esta reparación**. Se conservan las referencias anteriores; las guías v1 llevan aviso de antecedente. Seguir LEEME_REPARACION_BASE_V20.md.

## Pruebas locales

**35 casos distintos aprobados, cero fallos finales:** 22 regresiones de aislamiento/contrato, un recorrido completo de BASE/CANDIDATO/UPGRADE y 12 casos nuevos de recuperación/seguridad. Las reejecuciones focalizadas no se suman como casos nuevos. Ver `evidencia/regresion-final-v2.tap`, `recuperacion-contrato-final.tap` y `staging-readonly-final.tap`.

Cubren: idempotencia, UUID/revisiones, SHA, archivos y XLSX, rollback, SCAP/video, caches que no autorizan escrituras, subraíz/archivo movidos afuera después de una lectura, propiedades ajenas, diagnósticos sin escrituras de Drive/propiedades, PENDING que no se recupera automáticamente, staging de revisión equivocada que bloquea permiso, flags/carpeta/identidad/hash incompatibles, respuesta sin métricas, timeout incierto, cambio de epoch/fixtures, espera de ambos clientes y reanudación simulada sin reenviar los padres ni alterar los tres archivos fuente.

Comparación del guard: 1793→744 consultas en tres commits simulados, mismas escrituras funcionales/ACK/unzip. Es **medición local de operaciones**, no latencia Google después del parche ni rendimiento productivo. Las 95 pruebas del candidato anterior siguen siendo antecedente; no se repitió la auditoría general.

Se leyeron además las evidencias reales originales, validando ocho ACK/canonical/fixtures y conservando sus hashes. No se consultó la implementación Google, no se publicó ni se ejecutó una prueba real nueva. Android, APK, fuentes V20, candidato nativo, propiedades y ZIP anterior se conservan.
