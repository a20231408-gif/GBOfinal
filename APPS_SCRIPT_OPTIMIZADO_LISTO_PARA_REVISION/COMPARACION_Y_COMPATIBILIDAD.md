# Comparación de fuentes y compatibilidad

## Tres bases que no deben confundirse

- **Publicado:** no se dispone todavía de sus fuentes exactas. Toda celda de paridad productiva sigue pendiente.
- **Referencia local:** `google-apps-script/SyncV3.gs`, `SyncV3Core.js`, módulos PhotoNames/SicObservations/Video. El adaptador y motor locales coinciden con las copias de 10K; esto es un hecho local, no prueba de publicación.
- **Propuesto:** el adaptador completo de 1.4.6. Solo cambia la validación pura previa a staging y añade mediciones. Su motor adjunto es idéntico al local. El diff íntegro revisable está en `diferencias/SyncV3-local-a-propuesta.diff`.

## Diferencias, función por función

| Función | Referencia local | Propuesta | Riesgo/condición |
|---|---|---|---|
| `v3Enabled` | Lee propiedad existente | Sin cambio | No activa ni crea propiedades. |
| `stageRecordV3` | Decodifica y comprueba hash dentro del lock | Hace esos cálculos puros antes de adquirirlo | Todas las consultas, staging y verificación de bytes guardados siguen bajo lock. Un archivo inválido puede rechazarse antes de esperar BUSY; no cambia archivos ni ACK. No desactiva NOT_JPEG del commit. |
| `commitRecordV3` | ScriptLock + motor + ACK | Mismo flujo con tiempos | No permite commits paralelos; no quita validación ni reordena consolidación. |
| `reservePhotoNamesV2` | Reserva durable con ScriptLock | Igual, con tiempos | Misma identidad/binding, categorías y nombres. |
| `v3Store(timing)` | Almacén sin parámetro | Parámetro opcional y wrappers de medición | Sin parámetro devuelve el mismo contrato que requieren videos. Recuperación, reservas, archivos y journals no cambian. |
| `v3Phase`, `v3LockTiming` | No existen | Helpers nuevos de diagnóstico numérico | Nombres estáticos de fase/operación; sin UUID, usuarios, payloads, URLs ni credenciales. El fallo del logger no cambia ACK; se libera el lock antes de registrar el resumen. |
| `SyncV3Core` | Motor vigente local | Copia idéntica | Ninguna mejora de ordenamiento, XLSX o protocolo implementada aquí. No reemplazar una variante productiva distinta. |

La cifra `heldMs` se cierra inmediatamente después de `releaseLock`: incluye el pequeño costo de liberarlo; no es una traza precisa de cada llamada Drive. `prepareMs`/`waitMs` pueden valer cero según la operación. Las fases se anidan y parte del trabajo interno no está desglosado. Comparar cada métrica consigo misma y con la duración de la ejecución, no sumar todos sus acumulados.

## Matriz de compatibilidad

| Área | Evidencia local y contrato conservado | Qué falta contra publicado |
|---|---|---|
| SIC | Casos con fotos y filas ordenadas; dos UUID/operadores, misma revisión idempotente y actualización sin duplicar carpeta/Excel. | `doPost`, adaptador y motor exactos; extensiones de formato presentes en producción. |
| SCAP | Conserva identidad de puente y UUID de inspección; no deriva a otra familia. Consulta de padre de VideoV1 con `v3Store()` comprobada. | Módulos SCAP y ramas legacy completas de Code para detectar diferencias; prueba física autorizada. |
| Pavimentos | Ruta/tramo/superficie, PDF/XLSX/JSON y staging siguen siendo archivos del protocolo. La carga previa aislada entregó 11 muestras. | Exportaciones y manifestaciones reales se contrastan solo con datos ficticios autorizados; no usar fichas reales. |
| Fotos V2 | Reservas bajo lock, mismos bytes/categorías/nombres/fileId en reintento; SHA y NOT_JPEG conservados. | PhotoNamesV2 y enrutamiento V2 publicados; no basta con capacidad=2. |
| Carpetas estables | `allocateNumber`, reservas previas, binding por identidad y rechazo de reubicación permanecen intactos. | Esquema publicado de bindings y diarios, sin pedir sus valores/datos reales. |
| Orden y consolidado | `compareTechnical`: carretera como ámbito; calzada/sentido/PR y distancia, UUID de desempate; ninguna renumeración. | Paridad de comparator, mergeSheet, plantillas/firmas y transacción. |
| Observaciones SIC | Integración nueva del adaptador empaquetado con módulo 10K local: texto literal, clientes antiguos/nuevos, edición, vaciado explícito y estilo solo en columna final. | **Bloqueo concreto:** comprobar que el módulo publicado tiene `plain` y conserva `inspect/select/inherit/printExtent`. Una versión 10I puede anunciar 1 y carecer de `plain`. |
| Videos / 10H | Archivos de video no modificados. Regresiones locales de nombres, sesiones, SHA/ACK, reservas antiguas e interrupciones; llamada opcional al almacén y mismo lock comprobados con el candidato. | Verificar VideoV1/Core/VideoNamesV2 y compatibilidad de diarios V2 antes de cualquier reemplazo/reversión. No se probaron sesiones Google reales. |
| BUSY, revisiones, ACK | Espera 30000, BUSY reintentable, hash, revisión/base, respuesta repetida, journals y rollback conservados; pruebas aisladas de colisión y respuesta perdida. | Comparar manejo real de excepciones y fases de lock. Hace falta carga real autorizada de dos ejecuciones, no solo intercalación simulada. |
| Seguridad / Code | No se entregó un Code alternativo; se mantiene su autorización antes del dispatch. No cambia scopes/propiedades/raíz/usuarios. | Code completo redactado, manifiesto y modalidad de ejecución actuales. No inferir equivalencia de autorizaciones por GET. |

## 10H y 10K permanecen separados

10H corresponde a VideoV1/Core/VideoNamesV2 y un campo GET de capacidades; no se añade ni se retira de este paquete. 10K corresponde al estilo de Observaciones y la conciliación 10I, y comparte los archivos SyncV3/Core usados como base local. Esa dependencia obliga a revisar funciones coincidentes: no instalar el adaptador hotfix sobre un módulo antiguo ni copiar después el SyncV3 de 10K encima del hotfix, porque se perderían sus mediciones.

Si producción ya tiene 10K idéntico, el delta podría limitarse al adaptador. Si tiene una combinación distinta, se necesita un merge sobre el código publicado conservando sus funciones revisadas; todavía no existe ese archivo final integrado. Si no hay módulo Observaciones, no añadirlo silenciosamente: revisar contrato y alcance expresamente.

La lectura de capacidades guardada es evidencia de protocolos anunciados en esa fecha, no de las fuentes que los implementan. No se declara listo para producción ninguno de estos reemplazos.
