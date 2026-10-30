# Parche puntual FORCED_BUSY — proyecto independiente de PRUEBAS

Este ZIP es incremental sobre **GBO_REPARACION_ENSAYO_BASE_V20_HELPERS_V2_20261017**. No es otro backend completo. No contiene Codigo.gs, SyncV3.gs ni SyncV3Core.gs. Solo el propietario publica o ejecuta en Google. Producción permanece V20.

## Archivos que se actualizan

| Destino | Acción |
|---|---|
| Editor Google **GBO_Inventario_PRUEBAS_V20 / TestHttp.gs** | Sustituir su contenido completo por `BASE_V20_PRUEBAS/TestHttp.gs`. No crear una segunda definición de doPost/doGet. |
| Copia local `BASE_V20_PRUEBAS/TestHttp.gs` | Actualizar con el archivo homónimo del ZIP. |
| Copia local `CANDIDATO_OPTIMIZADO_PRUEBAS/TestHttp.gs` | Actualizar con el archivo homónimo del ZIP; es idéntico al de BASE. |
| `ejecutor/ejecutar.cjs` | Sustituir. |
| `ejecutor/forced-busy.cjs` | Añadir junto al ejecutor; es indispensable. |
| `ejecutor/comparar.py` | Sustituir; excluye toda la contención artificial. |
| `pruebas/ForcedBusy.test.cjs`, `RunnerForcedBusy.test.cjs`, `ComparadorForcedBusy.test.py` | Añadir, solo pruebas locales. |
| `pruebas/Ejecutor.test.cjs` | Sustituir, solo pruebas locales. |

Se conservan **TestIsolation.gs, TestVariant.gs, appsscript.json, los ocho módulos de cada variante, recuperacion.cjs, EJECUTAR_GOOGLE.ps1 y todos los fixtures originales**. No importar archivos de pruebas Node/Python al editor Google. La guía nueva sustituye solamente el procedimiento para esta BASE limpia; no seguir para ella la antigua instrucción de reanudar el ensayo de 119 eventos.

## 1. Conservar evidencias y actualizar la misma implementación de pruebas

1. Detener otros lanzadores. Esperar a que terminen naturalmente las ejecuciones activas de PRUEBAS. Consultar `gboTestReadConfiguration` en ese proyecto: exigir **inflight=0**. No editar ni poner a cero un contador positivo. Ante una discrepancia, detenerse y diagnosticar.
2. Conservar la carpeta completa del ensayo fallido y sus `resultado.json`, `eventos.jsonl`, `avance.json`, video original y configuración privada. No borrar ni mover el staging generado. Conservar también todas las corridas anteriores, incluida cualquier BASE completada; no usar sus tiempos en la nueva comparación.
3. Descomprimir el parche en una carpeta aparte. Duplicar **localmente** el paquete v2 completo en otra carpeta y aplicar allí los archivos de la tabla. Así quedan intactos el paquete anterior y sus resultados. No pegar únicamente el ZIP incremental en una carpeta vacía: necesita los módulos, fixtures, lanzador y recuperacion.cjs del paquete v2.
4. Abrir exclusivamente **GBO_Inventario_PRUEBAS_V20** y respaldar privadamente el TestHttp actual. Confirmar que sigue seleccionada BASE y que se conservan sus ocho módulos V20 sin cambios. Sustituir **solo TestHttp.gs** con el archivo BASE del parche. No tocar Codigo, SyncV3, SyncV3Core ni TestIsolation.
5. En **Implementar → Administrar implementaciones**, seleccionar **la implementación de PRUEBAS existente**, pulsar Editar y elegir **Nueva versión**. Guardar conservando su URL `/exec` y los permisos/identidad de ejecución existentes. No crear otra URL, no abrir ni editar la implementación productiva.
6. Consultar el GET de la URL de PRUEBAS, sin token en la URL. Debe anunciar `_gboTest.harnessRevision=2`, `guardPolicy=FRESH_WRITES_LOCKED_READ_CACHE_V2`, **`forcedBusyTestVersion=2`**, **`forcedBusyHoldMs=90000`** e `inflight=0`. El ejecutor también lo exige antes del primer POST. No compartir la respuesta completa: contiene identificadores de la corrida.

## 2. Iniciar una BASE nueva, preservando la anterior

1. En el editor de PRUEBAS, con **BASE** seleccionada, ejecutar **una sola vez `gboTestStartNewRun`**, después de verificar que no hay clientes activos e inflight=0. Esta función existente comprueba raíz/configuración, exige cero en curso y crea una **nueva subcarpeta `TEST_BASE_<runId>`**, nuevo runId y epoch dentro de la raíz autorizada. Actualiza únicamente la selección de corrida del proyecto independiente. No borra ni reutiliza la subcarpeta anterior; no es una autorización para resetear un contador positivo.
2. Ejecutar `gboTestReadConfiguration` para comprobar BASE, nueva corrida e inflight=0. Nunca ejecutar `gboTestResumePreviousBase` para este benchmark.
3. En PowerShell abrir **`ejecutor/EJECUTAR_GOOGLE.ps1` de la copia local parcheada**: por ejemplo `& '.\ejecutor\EJECUTAR_GOOGLE.ps1'` desde la raíz de ese paquete. Elegir **N** (corrida nueva), **0 altas adicionales**, y la ruta del **mismo video ficticio original**, sin regenerarlo. El lanzador mantiene perfil de fotos **CAMPO**. Indicar exclusivamente ID/URL del proyecto independiente y credencial exclusiva de prueba por entrada oculta. No incluir credenciales en comandos ni documentos.
4. No lanzar una segunda instancia. Primero se verá `FORCED_BUSY_WAIT_HOLDER`, luego `FORCED_BUSY_COMPETITOR` y `FORCED_BUSY_DRAIN_HOLDER`. La retención artificial dura **90 segundos** desde adquirir el lock; la ida y vuelta Google añade tiempo. No interrumpirla por parecer detenida. Se drena la petición holder antes de continuar o informar el fallo.
5. Solo tras BUSY verificado comienzan la preparación y las ocho fichas. El resultado se guarda en **`RESULTADOS_REPARACION/BASE_<nuevo runId>`**. El ejecutor impide sobrescribir una corrida incompleta ya existente. No elegir R ni D para retomar esta corrida de cero padres: su staging se conserva como evidencia, pero no es una BASE limpia.
6. Si aparece **FORCED_BUSY_INCONCLUSIVE**, no implica fallo del protocolo: consultar `failure.checks.reason`, el resultado dirigido y sus locks. Puede haber una espera de transporte/arranque mayor al margen. Conservar todo y comprobar ejecuciones/inflight; no repetir automáticamente holders de 90 s, no iniciar la carga pesada y no borrar el stage que pudiera haberse creado. Una nueva tentativa requiere otra corrida limpia, con el mismo procedimiento.
7. Si aparece `TRANSPORT_UNCERTAIN_REQUIRES_DIAGNOSTIC`, `HARNESS_METRICS_MISSING` u otra condición de seguridad, conservarla y detenerse. No resetear contadores ni cambiar ACK. El parche no promete resolver fallos de transporte reales.

## 3. CANDIDATO comparable, después de BASE completa

1. Conservar el resultado BASE completo y sus XLSX. Esperar a cero ejecuciones activas/inflight. En el MISMO proyecto de pruebas conservar este TestHttp idéntico y TestIsolation v2. Cambiar únicamente los **dos módulos candidatos previamente aprobados localmente** (SyncV3.gs y SyncV3Core.gs del paquete v2) y seleccionar TestVariant=CANDIDATO como indica aquel paquete. Este parche **no cambia ni entrega nuevas versiones** de esos módulos.
2. Actualizar la misma implementación de PRUEBAS a una nueva versión manteniendo la URL. Crear **nueva subcorrida CANDIDATO** mediante `gboTestStartNewRun`, nunca reutilizar la BASE. Misma raíz ficticia principal, distinta subcarpeta/epoch, mismo video exacto, fotos CAMPO, extra=0 y los mismos helpers.
3. Lanzar N con esas mismas condiciones. No simultanear corridas BASE y CANDIDATO. El ensayo de dos clientes ya sucede dentro de cada corrida.
4. Comparar solo dos resultados completos nuevos con `python .\ejecutor\comparar.py '<BASE nueva>\resultado.json' '<CANDIDATO nuevo>\resultado.json' --out COMPARACION_NUEVA_GOOGLE.json` (reemplazar rutas). El comparador exige integridad, fixtures/carga iguales, revisión del ensayo coincidente y BUSY artificial verificado en ambas variantes.
5. **Se excluyen del benchmark todos los eventos FORCED_BUSY: holder, sondeos y competidor, sus 90 s, esperas de lock y operaciones.** Las métricas se conservan aparte en `artificialBase`/`artificialCandidate`. Los BUSY naturales posteriores permanecen en los tiempos normales. No usar duración total del proceso ni restar simplemente 90 s: las llamadas artificiales pueden solaparse y tienen latencias adicionales.

## Criterio y límites

PASS de contención exige respuesta de **la petición competidora concreta** `ok=false`, `code=BUSY`, `retryable=true`, telemetría enlazada a esa solicitud/corrida, un lock `acquired=false` con espera de al menos **29 s** (tolerancia de observación sobre tryLock(30000)), y holder completado con lock retenido al menos **90 s**. No basta ningún BUSY anterior. Se requiere además margen conservador >=60 s al enviar el competidor; se descuenta el RTT completo del sondeo y el tiempo local posterior, sin comparar relojes Google/PC.

Las regresiones locales prueban el ejecutor y la integridad en un entorno simulado. No son mediciones de Google ni certifican rendimiento productivo. Este parche no autoriza publicar en producción, no cambia ACK ni elimina ScriptLock, y no modifica Android ni Servicio de Usuarios.
