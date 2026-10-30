# Rendimiento: comparación acotada y límites

No se ejecutaron peticiones Google ni se atribuye la lentitud de campo a una causa única. Los commits de 172–174 s del laboratorio incluían 947–1030 verificaciones guard.metadata.get y cerca de 118 s de esa protección: no son tiempos del servidor productivo sin esos helpers.

| Etapa | Código/evidencia revisados | Resultado de esta entrega |
|---|---|---|
| Generación Excel/PDF | AutomaticDelivery.prepare, cache prepared-path4.json, ScapDelivery.bundle | Se conserva cache por revisión y sourceHash. No se eliminan comprobaciones ni se fuerza regeneración por abrir una tarjeta. |
| Nombres/fotos | reservePhotoNamesV2, copias preparadas SHA y JPEG, PhotoNamesIntegration10ETest | Contrato y reservas sin cambios. Cinco regresiones V2 aprobadas. |
| Transferencia | AutomaticDriveHttp.post y journal stages-* | Prioridad documental explícita sin reordenar el manifiesto. Adaptador HTTP interceptado: SCAP.xlsx antes de foto; dos stages una sola vez y dos intentos de commit tras respuesta perdida. |
| BUSY/reintentos | AutomaticSync y HotfixBusy146Test / SyncRecovery10DTest | Reglas, WorkManager y confirmación sin cambios; nueve regresiones focalizadas aprobadas. |
| Servidor/Drive/Excel/ACK | Parche de ordenamiento aislado, tres tests Node | No es optimización de rendimiento. El ScriptLock y transacciones se conservan. Tiempos Node incluyen simulación y procesos ZIP Python; no se extrapolan a Google. |

Comparación antes/después verificable del cambio: antes se iteraba [foto, documento] si así venía el manifiesto; después se transfiere [documento, foto]. Se conserva el orden original del JSON/contentHash y el total de archivos. La prueba del adaptador ejecuta el orden nuevo, conserva la preparación original y verifica reutilización de stages; no es un benchmark ejecutado contra el adaptador anterior. No se afirma reducción del tiempo total ni de latencia Google. Las filas SIC incorporadas en sicRows siguen viajando con el commit final, después del staging obligatorio; no pueden entregarse o confirmarse por separado con el protocolo actual.

Las métricas locales seguras existentes CAPABILITIES, RESERVE, PHOTO/FILE_PREPARE, STAGE_REUSED, *_SEND, *_WAIT_ACK, *_HTTP y ACK siguen disponibles en el diagnóstico de estabilidad; no se añadieron tokens, URL, fotografías o datos de ficha a logs. No se introdujeron nuevas esperas, reintentos agresivos, paralelismo ni modificaciones del backend productivo.

P3 no se declara resuelto: para demostrar una mejora real hace falta comparar ejecuciones Google autorizadas, con igual revisión/bytes/carga y sin mezclar el costo del aislamiento de laboratorio. Se preservan las evidencias previas. La mejora implementada es de prioridad y respuesta visible, no una promesa de aceleración productiva.
