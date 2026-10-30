# Corrección puntual del falso fallo FORCED_BUSY

**Causa confirmada en la evidencia local de la corrida indicada:** un problema de temporización y aserción del ensayo artificial. No se demostró un fallo de negocio del backend.

Se leyeron resultado.json, eventos.jsonl y avance.json de la corrida BASE incompleta de tres eventos, conservados en Descargas. El resultado apunta a ejecutar.cjs:136, `UNCLASSIFIED_LOCAL_FAILURE`, aserción verdadera. La comparación con el ejecutor v2 identifica exactamente la exigencia de que existiese algún BUSY en el historial.

| Evidencia real proporcionada por el propietario | Resultado |
|---|---|
| `testLockStatus` | OK; ida y vuelta 3,323 s. El evento antiguo no guardaba el contenido de held/remaining. |
| `testHoldLock` | OK; lock adquirido, retenido **35,196 s**; llamada 37,889 s. |
| `stageRecordV3` dirigido | **OK**, lock adquirido tras **29,935 s**, retenido 17,145 s; llamada 50,566 s. |
| Después del stage | Aserción local de BUSY; INCOMPLETE; cero padres confirmados según avance. |

La retención de 35 s no garantizaba que quedasen 30 s al llegar la petición competidora al lock. El éxito del stage es compatible con el protocolo. No debe eliminarse el staging creado, cambiarse su respuesta ni contabilizarse esta corrida como benchmark. Los eventos antiguos no contienen timestamps server suficientes para reconstruir la llegada exacta de cada ejecución; no se inventa ese detalle. También se detectó otra corrida anterior completa en la misma carpeta: se conserva sin mezclar sus evidencias con esta incidencia.

## Parche implementado

- **TestHttp.gs, idéntico BASE/CANDIDATO:** retención artificial 90 s, probeId efímero vinculado a la corrida para consultar al holder correcto; estado con margen restante; marcador de ensayo v2 en capacidades/respuestas. Liberación en finally incluso si falla la limpieza del marcador. No cambia la política de aislamiento ni las comprobaciones Drive.
- **forced-busy.cjs (nuevo):** comprueba margen >=60 s descontando RTT y demora local, una sola petición competidora con el pequeño metadata del fixture en vez de la foto de campo, respuesta BUSY y métricas de su lock; espera al holder siempre. Si la contención no se demuestra, condición segura `FORCED_BUSY_INCONCLUSIVE` con checks numéricos/booleanos y razón, sin IDs, URL o tokens.
- **ejecutar.cjs:** integra la prueba dirigida sin reintentos artificiales ocultos; guarda BUSY=true únicamente con prueba completa. Clasifica el resultado inconcluso en TEST_HARNESS, conserva eventos/staging, exige cero peticiones activas al empezar y evita sobrescribir una corrida incompleta. No reescribe ACK ni marca fichas confirmadas por un stage exitoso.
- **comparar.py:** aparta completamente la contención artificial del rendimiento normal y exige ensayo v2 equivalente y verificado en ambas variantes. Mantiene los BUSY naturales en el benchmark.

Los ocho módulos de negocio de cada variante y TestIsolation se conservan byte por byte respecto del paquete v2. El nuevo ZIP es incremental y excluye los módulos protegidos. La incorporación del parámetro de correlación corresponde exclusivamente a acciones testHoldLock/testLockStatus; no altera peticiones ni contratos Android 1.4.7/1.4.9.

## Pruebas reales locales

**54 pruebas Node aprobadas, 0 fallos; 1 regresión Python aprobada. Sin llamadas Google.**

- 18 focalizadas: handler BASE real ejecutado localmente con reloj/LockService simulados, holder de 35 s y llegada a 5,065 s → adquisición válida a 29,935 s; el nuevo ensayo lo clasifica como inconcluso. También márgenes insuficientes, RTT, demora antes de enviar, probe ajeno, métricas ausentes, BUSY ajeno/anterior, retención sin confirmar y liberación tras error.
- 1 integración del ejecutor: stage exitoso dirigido produce INCOMPLETE/FORCED_BUSY_INCONCLUSIVE, dominio TEST_HARNESS, cero commits, staging preservado, holder terminado y evidencia inmutable. Nueva entrada sobre la misma corrida o con inflight positivo se bloquea.
- 35 regresiones existentes pertinentes: aislamiento, referencias externas, hash, idempotencia, rollback/recuperación, ACK, diagnósticos de solo lectura y ejecución simulada completa BASE/CANDIDATO con 11 padres por variante, dos clientes, Observaciones, fotos V2, videos, filas y hashes Excel equivalentes. Se mantiene el ensayo de historial BASE→CANDIDATO simulado. No son tiempos Google.
- 1 prueba Python: holder 90 s + sondeo + competidor artificial excluidos; BUSY natural conservado.

Consultar evidencia/PRUEBAS_Y_PRESERVACION.json y diferencias/PARCHE.diff. Los tiempos Google del ensayo fallido se describen como evidencia recibida, nunca como mejora productiva. Pendiente: nueva BASE completa y CANDIDATO comparable ejecutados por el propietario en Google, y revisión de sus logs/Excel. No se declara aptitud productiva.

## Ejecución local reproducible

Desde una **copia v2 completa con este parche aplicado**, con Node y Python disponibles:

```powershell
$env:GBO_TEST_PYTHON = (Get-Command python).Source
node --test .\pruebas\ForcedBusy.test.cjs .\pruebas\RunnerForcedBusy.test.cjs .\pruebas\Aislamiento.test.cjs .\pruebas\Recuperacion.test.cjs .\pruebas\Ejecutor.test.cjs
python .\pruebas\ComparadorForcedBusy.test.py
```

Esas pruebas usan mocks y archivos temporales locales; no requieren token real. Las instrucciones Google del propietario están separadas en GOOGLE_TEST_SETUP.md. No hubo publicación, instalación, ejecución real, borrado, commit ni push.
