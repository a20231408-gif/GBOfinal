> ANTECEDENTE DEL PAQUETE V1. Para la corrida BASE existente manda LEEME_REPARACION_BASE_V20.md y el informe de diagnostico v2. No ejecutar crear corrida/reiniciar/limpiar al seguir este documento anterior.

# GBO_Inventario_PRUEBAS_V20 — ensayo aislado

**17/10/2026. Preparado para revisión y ejecución por el propietario. No publicado. No aprobado para producción.**

Raíz autorizada única: `CONFIGURAR_ID_PRIVADO`, nombre `GBO_PRUEBAS_BACKEND_NO_PRODUCCION`.
No conectar tablets ni sustituir su URL. El inventario productivo V20, su implementación y el Servicio de Usuarios permanecen fuera de este procedimiento.

## 1. Qué copiar y qué conservar

| Carpeta | Uso |
|---|---|
| `BASE_V20_PRUEBAS` | Ocho módulos V20 completos, con configuración y adaptadores comunes de aislamiento/medición. |
| `CANDIDATO_OPTIMIZADO_PRUEBAS` | Los mismos módulos; únicamente SyncV3 y SyncV3Core contienen el candidato previamente revisado. TestVariant identifica la variante. |
| `FUENTES_V20_REFERENCIA_NO_IMPORTAR` | Siete originales byte a byte, como `.gs.txt`. Codigo completo conserva la lógica pero tiene la raíz privada omitida; no es una copia binaria de ese archivo. NO importar esta carpeta. |
| `ejecutor` | Dos clientes ficticios, generación local de video, comparador y lanzador con credencial oculta. |
| `pruebas`, `evidencia` | Pruebas nuevas y antecedentes locales. Ninguno acredita tiempos Google. |

Los originales privados íntegros siguen en el repositorio de trabajo, `tmp/backend-v20-revision-20261016/originales-v20`. Sus hashes, los del candidato y los de los archivos adaptados figuran en `evidencia/PROCEDENCIA.json`. No se incluye una configuración ejecutable con raíz productiva. Las diferencias se detallan en `COMPATIBILIDAD_Y_DIFERENCIAS.md`.

## 2. Crear solamente el proyecto independiente

1. En Apps Script, crear un proyecto **nuevo**, llamado `GBO_Inventario_PRUEBAS_V20`. No abrir ni editar `GBO_Inventario` o `GBO - Servicio de Usuarios` para pegar estos archivos.
2. En Configuración del proyecto, anotar su **ID de secuencia de comandos**. Debe ser el del proyecto nuevo; no el ID de implementación `/exec`.
3. Crear estos once archivos de secuencia de comandos, usando el contenido completo de `BASE_V20_PRUEBAS`: `Codigo`, `SyncV3`, `SyncV3Core`, `PhotoNamesV2Core`, `SicObservationsV1Core`, `VideoV1`, `VideoV1Core`, `VideoNamesV2Core`, `TestIsolation`, `TestHttp`, `TestVariant`. Google añade `.gs`. No dejar otra definición de `doGet`/`doPost` en un archivo inicial. No pegar variantes duplicadas.
4. Activar Mostrar archivo de manifiesto y pegar el `appsscript.json` entregado. En Servicios, comprobar **Drive API v3**, identificador `Drive`. Si requiere habilitar la API de Drive en Cloud, hacerlo únicamente en el proyecto Cloud asociado a esta copia de pruebas.
5. **Límite conocido:** no se recibió el manifiesto original V20. El entregado declara V8/Drive v3 y permite que Apps Script infiera los scopes de los servicios ya usados por los ocho módulos. No añade Gmail, usuarios, Sheets ni administración de despliegues. No se certifica igualdad de scopes, zona horaria o librerías con un manifiesto original no aportado. Compararlo antes de una decisión productiva.
6. No crear disparadores. No vincular este proyecto a una raíz compartida con otro escritor de inventario. Durante cada corrida, no mover carpetas/archivos manualmente ni ejecutar otras herramientas sobre su subcarpeta.

## 3. Propiedades y credencial exclusiva

En **este proyecto nuevo**, definir manualmente solo:

| Propiedad | Valor |
|---|---|
| `GBO_TEST_SCRIPT_ID` | ID del proyecto nuevo obtenido en el paso anterior. |
| `GBO_TEST_CONFIRM` | `ONLY_CONFIGURAR_ID_PRIVADO` |
| `GBO_TEST_TOKEN` | Credencial nueva aleatoria de 32 bytes expresados como 64 caracteres hexadecimales, generada con un gestor de contraseñas/CSPRNG. Guardarla privadamente. |

No copiar propiedades del servidor real. En particular, **no crear** `GBO_DRIVE_API_TOKEN`, `ROOT_FOLDER_ID` ni flags productivos. La presencia de cualquier propiedad sin prefijo `GBO_TEST_` bloquea la copia antes de acceder a una operación de escritura. El adaptador devuelve capacidades de prueba activas exclusivamente dentro de este proyecto; no lee sus valores productivos.

No pegar la credencial en documentos, conversaciones, JSON de fixtures ni código fuente. El lanzador la solicita con entrada oculta y la transmite en el cuerpo HTTPS. No se añade a evidencias ni argumentos de proceso; se usa temporalmente en el entorno del proceso local. No hay tokens predefinidos reales en el ZIP.

## 4. Abrir una corrida BASE y su implementación de pruebas

1. Seleccionar y ejecutar **`gboTestStartNewRun`** desde el editor. Autorizar únicamente esta copia. Antes de crear la subcarpeta verifica ID del proyecto, propiedades, constante de raíz y nombre de la carpeta autorizada.
2. El resultado será una subcarpeta `TEST_BASE_<runId>`. Dentro se crean `TEST_AUTO_BACKEND`, rutas humanas y `._GBO_SYNC_V3`. Ningún registro existente en la raíz se importa o modifica.
3. Ejecutar `gboTestReadConfiguration`: revisar `variant=BASE`, raíz, `runId` y `inflight=0`. No muestra la credencial.
4. Solo el propietario, cuando decida comenzar, crea una **implementación web del proyecto de pruebas**. Ejecutar **como propietario/desplegador**; ambas sesiones ficticias deben compartir ese ejecutor. La barrera de cambio de corrida usa UserLock de ese propietario, aparte del ScriptLock original de sincronización. No seleccionar “usuario que accede”.
5. El lanzador externo necesita un `/exec` de prueba accesible sin una sesión interactiva de Google, protegido por la credencial exclusiva. Si las políticas de la cuenta no permiten ese acceso, detener este paso y resolver el acceso autorizado; el lanzador rechazará páginas de login y no enviará POST sin la capacidad aislada. No cambiar políticas/permisos del proyecto productivo.
6. Guardar privadamente la URL **nueva de pruebas**. No usar la de inventario real. Abrirla muestra `_gboTest.testOnly=true`, raíz autorizada, ID de proyecto y variante. El lanzador verifica esos datos antes de cualquier POST. No registra la URL.

La ejecución como desplegador y su opción de identidad están descritas en la [documentación de web apps de Google](https://developers.google.com/apps-script/guides/web) y el [manifiesto de web apps](https://developers.google.com/apps-script/manifest/web-app-api-executable). Esto justifica el requisito de un único propietario ejecutor para la barrera de ensayo; no sustituye al ScriptLock del protocolo.

## 5. Ejecutar los dos clientes

Requisitos locales: Node 20 o posterior para el lanzador; Python 3 estándar para comparar XLSX. No se necesita Android Studio, ADB, APK, token OAuth productivo ni `clasp`.

1. Abrir `ejecutor/CREAR_VIDEO_FICTICIO.html` localmente en un navegador. Pulsar Generar, guardar el WebM ficticio y reproducirlo. Debe superar **262144 bytes** y pesar como máximo 32 MiB para ejercitar al menos dos bloques reanudables. Si el navegador no genera suficiente tamaño, usar un MP4/WebM ficticio conocido de ese tamaño. No grabar personas ni inventario real. Conservar exactamente el mismo archivo para ambas variantes.
2. Ejecutar `ejecutor/EJECUTAR_GOOGLE.ps1` desde PowerShell. Solicita URL NUEVA, ID de proyecto, ruta del video, carga adicional, credencial con entrada oculta y `node.exe` si no está en PATH. No imprime la credencial.
3. Elegir **0** para el ensayo corto: ocho padres, SIC A/B/C/D, dos muestras asfálticas, dos SCAP y dos ediciones SIC. Elegir **30** para carga: 98 padres (34 SIC, 32 Pavimentos, 32 SCAP), además de las dos ediciones; dos clientes en paralelo. Máximo admitido: 40 adicionales por familia. La carga puede durar varios minutos y no evita las cuotas de Google.
4. El perfil real predeterminado usa una foto **sintética de 2048×1536, 2883090 bytes**, sin EXIF. Los PDF son documentos ficticios válidos; los XLSX son fixtures de pruebas existentes derivados de Android. Los actores son `TEST_CLIENT_147` y `TEST_CLIENT_149`. No se autentican contra el Servicio de Usuarios.
5. Al comienzo se provoca un ScriptLock ocupado **35 segundos** para observar BUSY al agotarse `tryLock(30000)`, seguido de reintento normal. Es un retraso artificial, separado de los percentiles normales. No se quitó el lock ni se acortó su timeout.
6. Se reservan inicialmente los nombres en orden fijo como preparación del conjunto, para comparar los mismos números/rutas entre variantes. Después ambos clientes vuelven a reservar, suben staging y hacen commit concurrentemente. La asignación inicial de números concurrentes sin preasignación queda como escenario adicional Google, no como cobertura de esta corrida.
7. Se descartan intencionalmente respuestas de staging, commit y finish de video **después de recibirlas**, repitiendo exactamente la identidad/revisión. Se comprueba replay del ACK. El video consulta offset, envía bloques de 256 KiB y consulta progreso tras ignorar una respuesta intermedia. Son pérdidas de respuesta provocadas por el cliente; no se afirma haber cortado físicamente Internet.
8. Esperar el resultado. El lanzador no declara sincronizado un staging, un porcentaje o una fotografía aislada: exige ACK del padre con UUID/revisión/contentHash/archivos coincidentes y video `complete=true`.

Resultados locales del operador: `RESULTADOS_GOOGLE/BASE_<runId>/resultado.json`, `eventos.jsonl`, `avance.json` y tres Excel descargados. Los eventos se conservan incrementalmente, sin payloads, fotos, token ni URLs. `avance.json` contiene solo fixtures ficticios preparados y ACK; no credenciales ni sesión de video. Si se interrumpe el cliente, ejecutar otra vez con **misma corrida, mismos archivos y misma carga** para reanudar. Una corrida completa no se repite: hay que crear una nueva. No borrar su carpeta para reiniciar métricas.

## 6. Cambiar a CANDIDATO en el MISMO proyecto

1. Esperar el final del lanzador. Revisar en Ejecuciones de Apps Script que no haya solicitudes activas; `gboTestReadConfiguration` debe mostrar `inflight=0`.
2. Conservar descargados todos los resultados BASE. No borrar su subcarpeta, reservas, journals ni propiedades. No ejecutar dos lanzadores mientras se cambia el código.
3. Reemplazar **solo** `SyncV3.gs`, `SyncV3Core.gs` y el marcador `TestVariant.gs` por los contenidos de `CANDIDATO_OPTIMIZADO_PRUEBAS`. Los otros seis módulos, helpers y manifiesto son iguales; no volver a pegar una segunda copia. La base anterior seguirá disponible en el ZIP.
4. Ejecutar `gboTestStartNewRun`: crea `TEST_CANDIDATO_<otroRunId>` dentro de la **misma raíz autorizada**. Una configuración de variante equivocada, una solicitud vieja o un contador activo bloquean el cambio. No reutilizar la carpeta BASE para comparar tiempos de un conjunto nuevo.
5. Crear una versión de **esa implementación de pruebas**, conservando su URL de pruebas. Ejecutar primero `gboTestReadConfiguration` y comprobar con GET la variante CANDIDATO. Un `/exec` que siga apuntando a una versión BASE antigua rechazará la nueva corrida; no hay que saltarse esa comprobación.
6. Volver a lanzar PowerShell con el mismo video, perfil y carga. Se crean los mismos UUID ficticios en un registro y una carpeta separados, por lo que no se cruzan ACK ni revisiones con BASE.

Si una ejecución fue cancelada forzosamente por Google, el contador puede quedar positivo. **No se reinicia automáticamente**: detener clientes, comprobar en Ejecuciones que todas terminaron, conservar evidencias y, solo el propietario en el proyecto de pruebas, restablecer `GBO_TEST_INFLIGHT=0`. Entonces abrir una corrida nueva. No aplicar esta operación al servidor real.

## 7. Comparar y ensayar actualización con historial

Comparación desde la carpeta descomprimida:

```powershell
python ejecutor/comparar.py RESULTADOS_GOOGLE/BASE_<id>/resultado.json RESULTADOS_GOOGLE/CANDIDATO_<id>/resultado.json --out COMPARACION_GOOGLE.json
```

El comparador rechaza evidencias marcadas LOCAL, corridas incompletas, fixtures/cargas diferentes, divergencias de UUID/revisión/contentHash/rutas/hash de archivos o partes XLSX. Inspecciona Excel como ZIP/XML, cuenta filas, comprueba Observaciones heredadas/literales/vacías y celdas sin estilo/fórmula. Los IDs físicos Google se comparan dentro de cada corrida; son distintos por diseño entre carpetas. No se compara binariamente el contenedor ZIP como sustituto de sus partes: `Utilities.zip` puede variar sus metadatos. Abrir también los XLSX descargados y comprobar imagen/estructura/columnas.

**Ensayo adicional de cambio sobre historial V20 (no mezclar sus tiempos con la comparación):**

1. Conservar la BASE terminada y el candidato ya desplegado. Mantener todos los clientes detenidos e `inflight=0`.
2. Desde el editor CANDIDATO ejecutar `gboTestResumePreviousBase`. Solo cambia el selector del proyecto de pruebas y su epoch; no mueve ni copia archivos. Selecciona el último historial BASE, verificando de nuevo su padre autorizado. La propiedad `folderVariant=BASE` lo hace explícito.
3. Abrir GET y lanzar de nuevo el ejecutor con los mismos fixtures/carga. Detecta este modo y guarda en `UPGRADE_<runId>`. Edita únicamente `SIC/TEST_SIC_A` de revisión 3 a 4, conserva carpeta/fileId, exige replay idéntico, rechaza una revisión obsoleta y comprueba que los demás UUID/revisiones no cambian y no quedan journals PENDING.
4. El resultado esperado es `GOOGLE_UPGRADE_PROTOCOL_PASSED`. Conservarlo junto a BASE/CANDIDATO. Este paso modifica solo datos ficticios BASE, después de haber conservado su comparación.

## 8. Decisión y cierre

Aplicar `CRITERIOS_PUBLICACION.md`. Las mediciones y límites están en `MEDICION_Y_RESULTADOS.md`. **Ningún mensaje del ejecutor ni del comparador aprueba por sí mismo producción.** Faltan las ejecuciones Google y el manifiesto original.

Al terminar, el propietario puede desactivar exclusivamente la implementación de pruebas y retirar su credencial. Conservar ZIP, versiones, resultados y carpetas de ensayo como respaldo; este paquete no incluye limpieza ni borrados automáticos.

Una promoción futura requiere autorización independiente: respaldar V20 y su configuración privada, cambiar únicamente los dos módulos de optimización originales revisados **sin los adaptadores de ensayo**, crear una nueva versión y actualizar la implementación productiva existente conservando URL/propiedades/raíz/permisos. No copiar `Codigo`, `Test*`, manifiesto de pruebas ni flags de esta carpeta a producción. Para revertir, coordinar pausa/fin de ejecuciones y apuntar esa misma implementación a V20; conservar journals/bindings/entregas posteriores, sin restaurar manifiestos viejos sobre datos nuevos. Nada de eso se ha ejecutado aquí.
