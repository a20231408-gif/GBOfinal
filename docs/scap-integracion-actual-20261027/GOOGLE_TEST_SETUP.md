# Ensayo Google aislado — ejecución personal desde el editor

**No tocar GBO_Inventario, Servicio de Usuarios ni proyectos de ensayos anteriores. No desplegar una web app ni crear una URL.** Esta prueba usa Google Drive real, pero invoca el mismo doGet/doPost dentro del editor. No mide red HTTP de una APK.

## 1. Crear y verificar

1. Crear un proyecto Apps Script NUEVO e independiente, por ejemplo `GBO_SCAP_V2_ENSAYO_20261027`. Debe estar vacío de código previo y ScriptProperties. Si se detectan propiedades existentes, detenerse y conservarlas: no borrarlas para forzar la prueba.
2. Copiar únicamente los trece `.gs` de `PROYECTO_AISLADO_SOLO_PRUEBAS`, uno por archivo: `Código`, `PhotoNameV2Core`, `SyncV3Core`, `SyncV3`, `VideoNamesV2Core`, `VideoV1`, `VideoV1Core`, `SicObservationsV1Core`, `ScapPhotoPanelV2`, `TestIsolation`, `TestHttp`, `TestVariant`, `ScapEnsayo`. Reemplazar el contenido del archivo inicial vacío, sin dejar una segunda copia de Código ni módulos duplicados. Los `.gs` de `PARCHE_PRODUCCION_NO_PUBLICAR` no se mezclan con este conjunto.
3. Mostrar el manifiesto y usar el `appsscript.json` de esa misma carpeta. Verificar V8, America/Lima y servicio avanzado **Drive v3**. No se amplían scopes explícitos. La copia de ensayo omite el bloque webapp porque no se desplegará; el manifiesto productivo permanece intacto. Si Google solicita habilitar Drive API en el proyecto Cloud asociado, hacerlo exclusivamente en este proyecto de pruebas.
4. Verificar visualmente en Código y TestIsolation que la única raíz ejecutable sea `CONFIGURAR_ID_PRIVADO`, carpeta autorizada **GBO_PRUEBAS_BACKEND_NO_PRODUCCION**. TestVariant debe decir `SCAP`. No pegar configuración, tokens o raíces productivas.
5. Subir **una sola copia** de `SCAP54_FIXTURE_INPUT.json`, sin editarla ni convertirla a documento Google, directamente a esa raíz de pruebas. SHA-256 esperado: `e8e1156c9562f37847638ea1c15162739363da8d3686e1fd7eb898bf0749ed46`. El ejecutor lo comprueba antes de reservar o subir.

`SCAP_00` genera una credencial exclusiva privada y la guarda solo en ScriptProperties del proyecto nuevo; no se pide copiar ni compartir tokens. El código rechaza raíces distintas, propiedades ajenas y referencias Drive fuera de la subraíz de esta corrida antes de escribir mediante el adaptador. Las escrituras revalidan ascendencia; no se quitó ScriptLock. Conservar el código de aislamiento sin cambios.

## 2. Ejecutar funciones en este orden

Seleccionar una función en el editor y pulsar Ejecutar. Aceptar acceso de la cuenta propietaria únicamente para el proyecto aislado. No ejecutar simultáneamente dos funciones ni activar triggers. Cada paso emite un resumen sin credenciales. No ejecutar `gboTestStartNewRun`, `gboTestUpgrade...`, `testHoldLock` ni las funciones auxiliares manualmente.

| Paso | Función | Resultado esperado / repetición |
|---|---|---|
| 1 | SCAP_00_PREPARAR_PROYECTO | PROYECTO_AISLADO_PREPARADO. Una repetición conserva la misma corrida: YA_PREPARADO_SIN_REINICIAR |
| 2 | SCAP_01_CAPACIDADES | CAPACIDADES_PASS; panel 2, fotos V2, V3/path4, video1 SCAP_ONLY y nombres video2 |
| 3 | SCAP_02_CARGAR_Y_RESERVAR | PREPARADO, 59 archivos, 54 fotos, 3 croquis; verifica fixture, categorías, SHA y reservas |
| 4 | SCAP_03_SUBIR_LOTE | Repetir hasta STAGING_COMPLETO, staged=59. Máximo 12 archivos/120 s por ejecución; normalmente cinco llamadas, más si vence el presupuesto del lote |
| 5 | SCAP_04_COMMIT_DESCARTAR_RESPUESTA | RESPUESTA_DESCARTADA_INTENCIONALMENTE. Commit real; se descarta deliberadamente el recibo cliente, no se corta la red |
| 6 | SCAP_05_REINTENTAR_MISMA_REVISION | ACK_REINTENTO_PASS, alreadyExists=true; misma identidad/revisión/hash/carpeta/archivos y recibo persistido |
| 7 | SCAP_08_VERIFICAR_LOTE | Repetir hasta VERIFICADO, verified=59, sicVerified=3. Descarga bytes de Drive y verifica SHA, ruta real, carpeta y ausencia de homónimos |
| 8 | SCAP_06_PREPARAR_REVISION_2 | REVISION_2_PREPARADA_SIN_CAMBIAR_IDENTIDAD. Cambia solo la revisión y un marcador ficticio del manifiesto |
| 9 | SCAP_03_SUBIR_LOTE | Repetir hasta 59 otra vez, esta vez para la revisión 2 |
| 10 | SCAP_07_FALLO_Y_RECUPERACION | Primera ejecución: FALLO_CONTROLADO_Y_ROLLBACK; fallo artificial en la tercera actualización Drive. Segunda ejecución: REVISION_RECUPERADA_MISMOS_IDS y repetición idempotente |
| 11 | SCAP_08_VERIFICAR_LOTE | Repetir hasta VERIFICADO, 59 y 3 SIC en la revisión recuperada |
| 12 | SCAP_09_RESULTADO_SEGURO | PASS, ambas revisiones verificadas, IDs estables, inflight=0 |

No utilizar los tiempos del simulador local ni los tiempos del aislamiento como benchmark productivo. Esta prueba no usa la retención artificial de lock de 90 segundos que conserva el helper histórico; no ejecutarla. No se está ensayando carga o velocidad.

## 3. Qué comprobar en Drive

La corrida crea una subcarpeta nueva `TEST_SCAP_<identificador>` bajo la única raíz autorizada. No reutiliza corridas BASE/CANDIDATO anteriores. Dentro:

- `TEST_AUTO_BACKEND/PUENTES/PUENTE 1/SCAP/TEST_SCAP54_20261027/`: SCAP.xlsx, manifest.json, `croquis` con 3 PNG y veinte carpetas numeradas 01…20 con 54 JPEG en total.
- Ejemplo: `01_VISTA_PANORAMICA_LADO_DERECHO/01_PUE1_PAN_DER_001.jpg`; al final `20_VISTA_DE_CAUCE_AGUAS_ABAJO/20_PUE1_CAUCE_ABAJO_054.jpg`.
- `TEST_AUTO_BACKEND/EXCEL_SIC/`: SIC-17.xlsx, SIC-17A.xlsx, SIC-17B.xlsx, cada uno con una fila de datos ficticia. Abrir o descargar copias para revisar, sin editar los archivos durante el ensayo.
- `._GBO_SYNC_V3`, STAGING, journals, revisiones y posibles respaldos son evidencia técnica necesaria. Sus copias no son duplicados de entrega. No borrarlas.
- SCAP_ESTADO.json, revisiones preparadas, dos ACK privados y RESULTADO_SEGURO_PARA_COMPARTIR.json son ayudas del ensayo. Los ACK contienen IDs Drive: conservarlos en la carpeta privada, no enviarlos sin redacción.

Los números estables y cada fileId deben permanecer iguales entre revisiones. `alreadyExists=true` se exige en el reintento del mismo commit. Un HTTP/ejecución completada, staging=59 o una reserva correcta no equivale a entrega confirmada: debe existir ACK válido y verificarse los 59 archivos más los tres Excel consolidados.

El manifiesto contiene las 54 fotos y 3 croquis; F2 de SCAP.xlsx conserva los primeros 32 espacios contractuales. La continuación HTML local no se envía ni debe aparecer como archivo adicional en el ACK.

## 4. Qué devolver y cómo detenerse sin perder evidencia

Devolver únicamente `RESULTADO_SEGURO_PARA_COMPARTIR.json`, `CAPACIDADES_SEGURAS.json` y la lista de pasos ejecutados con su estado. Si falla, devolver el nombre de la función y código TEST_* del error, duración y estado de ejecución; sin URLs, tokens, IDs completos, ACK crudos ni datos de ingenieros. Opcionalmente una captura recortada de los nombres ficticios, sin barra de direcciones.

Ante error, BUSY, timeout o resultado distinto: **detener la secuencia** y ejecutar solo `SCAP_10_DIAGNOSTICO_SOLO_LECTURA` cuando ya no haya ninguna ejecución activa. Devuelve fase, cursores e inflight sin cambiar contadores ni recuperar transacciones. No editar estado, reiniciar inflight, borrar staging, cambiar fixture o crear de nuevo las fichas para continuar. El caso se revisará con esa evidencia. Las fases correctas y sus archivos quedan persistidos.

Criterio de aprobación: doce pasos completos, resultado PASS, ambas revisiones verificadas, 59 archivos con hashes y rutas exactos, tres SIC sin filas duplicadas, mismos IDs/carpeta, reintento alreadyExists y contador inflight=0. Cualquier fallo o paso pendiente impide autorizar producción. Esto cubre el caso ficticio descrito; no declara probados videos transmitidos, caídas reales de red, todas las cuotas ni tamaños de fichas reales.
