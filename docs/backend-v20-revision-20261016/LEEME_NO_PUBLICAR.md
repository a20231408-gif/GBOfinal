# Revisión V20 — NO PUBLICAR

Este paquete integra para **revisión local** los dos borradores recibidos y la instrumentación anterior, una sola vez. No altera los ocho originales ni las carpetas anteriores. No es una declaración de aptitud productiva.

1. Leer `INFORME_REVISION_V20.md`: paridad, diferencias de respuesta, pruebas y pendientes.
2. Leer `MEDICIONES_Y_LIMITES.md`: conteos exactos y tiempos **locales con servicios Google simulados**.
3. Revisar los dos archivos completos en `candidato-no-publicar` junto con `diferencias`. No copiar variantes duplicadas al editor.
4. Leer `PROPUESTA_2_HISTORICOS_NO_IMPLEMENTADA.md`: contrato transaccional y pruebas futuras; no está implementado.
5. Consultar `evidencia/RESUMEN_PRUEBAS.json` y logs. `SHA256SUMS.txt` permite verificar los archivos entregados.

## Archivo por archivo, solo para una decisión futura

- **SyncV3.gs:** eventual sustitución del contenido del módulo V20 revisado por el candidato, si se aprueba la prueba aislada y después la publicación. Incluye staging fuera del lock y telemetría segura. Mantiene `v3Store()` utilizable por VideoV1 sin parámetros.
- **SyncV3Core.gs:** eventual sustitución del motor V20 por el borrador probado de reutilización entrante, con la misma condición de aprobación. No activa una caché histórica.
- **Codigo.gs:** conservar íntegro. No se incluye como instalable. Su token sigue viniendo de su propiedad actual.
- **PhotoNamesV2Core, SicObservationsV1Core, VideoV1, VideoV1Core, VideoNamesV2Core:** conservar originales V20, sin parche nuevo. No reaplicar 10H/10K: su implementación ya está en las fuentes aportadas.
- **appsscript.json:** pendiente de revisión; no se aportó, no se crea ni sustituye.

Los originales recibidos se respaldaron íntegros en `tmp/backend-v20-revision-20261016/originales-v20` (Git los ignora). El ZIP se extrajo en `tmp/backend-v20-revision-20261016/borrador`. Las pruebas usan copias y datos ficticios; no se leen carpetas Drive reales.

## Repetir solo si cambian los candidatos o aparece un fallo nuevo

Desde la raíz de este repositorio, con Node y Python disponibles y las copias de entrada indicadas:

```powershell
node --test --test-reporter=tap docs/backend-v20-revision-20261016/pruebas/RevisionV20.test.cjs
node docs/backend-v20-revision-20261016/pruebas/MedicionesV20.cjs
```

No son comandos de Apps Script ni de despliegue. El arnés requiere los originales aportados en el tmp local; no incorpora ni distribuye credenciales. La selección de regresiones existentes se ejecutó en `tmp/backend-v20-revision-20261016/regresion`, para que sus exportaciones ficticias no sobrescribieran evidencias anteriores. En esa copia se usa la estructura de Codigo V20 con valores de raíz/token sustituidos por ficticios; nunca se ejecuta con el secreto del propietario.

## Respaldo, prueba, publicación y reversión

No se ejecutó ninguna de estas acciones Google. Para un paso futuro autorizado:

1. Propietario comprueba en Administrar implementaciones la versión numérica V20 vigente; conserva exportación de ocho fuentes, manifiesto, hashes y configuración de acceso en respaldo privado. No compartir valores de propiedades ni sobrescribirlas con ejemplos.
2. Revisar manifiesto/servicios y el delta. Probar primero en proyecto y raíz nuevos **de pruebas**, con datos ficticios y dos clientes, sin cambiar la URL/raíz del inventario ni el servicio de usuarios. No permitir dos proyectos escribiendo una misma raíz, porque no compartirían ScriptLock.
3. Superar la matriz del informe: ACK, contenidos/hash, plantillas reales, fotos V2, videos 10H, Observaciones, histórico/edición y recuperación; medir Google antes/después. No saltarse pendientes usando los tiempos Node como evidencia productiva.
4. Solo con autorización posterior, modificar los dos módulos revisados, crear una versión nueva y actualizar la implementación existente conservando URL, propiedades, permisos y raíz. No reemplazar Codigo.gs ni publicar una APK por este cambio.
5. Si se necesita volver atrás, coordinar pausa de nuevas sincronizaciones conservando colas y esperar las ejecuciones activas. Apuntar la misma implementación a V20; no borrar journals, reservas, bindings ni restaurar manifiestos viejos sobre entregas nuevas. El microparche no añade un esquema persistente propio, pero aun así se debe ensayar esa reversión en la copia de pruebas.
6. Conciliar después por UUID, revisión, archivos y ACK; recuperar datos desde respaldo sería un procedimiento distinto que requiere autorización expresa. No forma parte de cambiar la versión del código.

La versión productiva 20 permanece intacta. No hubo publicación, cambios de ScriptProperties, URL, ROOT_FOLDER_ID, permisos ni servicio de usuarios; tampoco commit, push o instalación Android.
