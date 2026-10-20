# GBO Ruta Clara 1.4.5 (19) — cierre para validación

Fecha: 15 de octubre de 2026. Proyecto `D:\Downloads\GBO_PRUEBAS_8B`. Rama `codex/observaciones-campo-manual-10I`, HEAD base `5de1242581dc460ea38d45006020d022f8d4da78`. Trabajo local sin commit ni push.

## Correcciones

**Proyecto con fecha:** corregido. Al abrir una sesión nueva se propone `Inventario Vial - dd/MM/yyyy`, calculado con `LocalDate.now()` en la zona local de Android. El texto es completamente editable y se inicializa una sola vez por apertura. Al abrir de nuevo otro día se calcula el nuevo valor. Configurar una sesión carga su nombre guardado, aunque corresponda a otra fecha.

Se revisaron las versiones disponibles de `SessionDialog`, incluidos el baseline 66610e8 y los cambios 24e1830/3afcb57 y el checkpoint 5de1242. El proyecto aparecía vacío para una sesión nueva en esas referencias. No se pudo demostrar un formato histórico de autocompletado; se aplica expresamente el formato alternativo indicado por el propietario. No se atribuye esa ausencia al rediseño.

Regresiones adicionales comprobables en el mismo flujo:

- Las entradas del diálogo usaban `remember`: ahora Proyecto, Ruta, Calzada y sentido utilizan `rememberSaveable` asociado a la identidad de la sesión. Se conservan durante recreación de Activity; no se almacenan PIN ni tokens.
- El controlador eliminaba espacios de los extremos del proyecto al guardar. Se conserva ahora literalmente el texto mostrado, manteniendo el rechazo de nombres vacíos. Esto se aplica solo al guardado explícito de la sesión que se está creando o configurando; no renombra otras sesiones.

Se mantienen el UUID de la sesión al configurar, operador autenticado, dispositivo y hora de inicio. Iniciar sigue generando su UUID y hora reales. Finalizar conserva el flujo original: cierra la sesión y marca sus registros pendientes para sincronizar la información de cierre; esa marca técnica actualiza `updatedAt` sin reescribir sus datos de inventario.

**Selección manual de ruta:** la batería demostró una segunda regresión real. `SurveyHeader` notificaba por separado ruta y tramo. En `SicFormCore`, ambos callbacks copiaban el mismo borrador anterior; el segundo restauraba la ruta anterior. Una ficha SIC-22 cambiada a PE-3NG se guardaba como PE-22A. Se cambia a un único evento con ruta y tramo, adaptando los dos consumidores. No se cambia el catálogo, la sesión, la numeración ni el orden contractual.

El caso de reproducción falló dos veces antes de la corrección y aprobó después: guarda PE-3NG/UC con el mismo UUID, reabre el borrador y crea otra ficha con los valores de la sesión PE-22A/CD. La nueva prueba `SurveyRouteSelectionTest` exige un único cambio y que editar luego la calzada conserve la ruta elegida, los PR, distancias, lado y sentido. El otro consumidor, `sic/LegacySicElementForm.kt`, conserva su actualización explícita de las fuentes manuales.

Archivos funcionales: `field/FieldUi.kt`, `field/FieldController.kt`, nuevo `field/SessionProjectName.kt`, `field/SurveyHeader.kt`, `field/SicFormCore.kt` y `sic/LegacySicElementForm.kt`. Pruebas nuevas: `SessionProjectNameTest.kt`, `SessionProjectDialogTest.kt` y `SurveyRouteSelectionTest.kt`; se refuerza `FieldSessionFormsTest`.

## Conservación y alcance

Se conservan recuperación 1.4.4, etapas visuales A/B, WebP, logo, dashboard, herramientas, encabezado del mapa y correcciones de autenticación. No hay nuevo rediseño ni assets nuevos.

La propagación Ruta/Calzada se comprueba mediante las suites de sesión y módulos: los nuevos formularios compatibles parten del contexto de sesión guardado en Room; un borrador o registro existente conserva sus valores propios, incluidos los vacíos históricos. Cambiar una ficha no cambia la sesión ni otras fichas. No se modificó esa implementación para este cierre.

No se modifican usuarios, claves, endpoints, ROOT_FOLDER_ID, Apps Script, WorkManager, esquemas Room, protocolos de sincronización ni registros reales. La copia independiente anterior a Ruta Clara no aporta archivos a este proyecto.

Respaldo previo: `tmp/cierre-ruta-clara-1.4.5-20261015/fuentes-antes.zip` (449 archivos), copias Debug/Release 1.4.4 y `baseline.json` con hashes. Se conservan las entregas anteriores de `output/`.

Versión propuesta: 1.4.5 (19), mismo `applicationId=com.tuempresa.inventariovial`. El nombre visible de la aplicación pasa de Inventario Vial a **GBO Ruta Clara**, conforme al nombre comercial solicitado. Es el único cambio previsto en el manifest fuente; no cambia actividades, permisos, providers, servicios ni restricciones de seguridad. La versión de Room sigue siendo 19 y no está ligada al versionCode de la APK.

## Método y resultados de pruebas

La batería amplia utiliza la configuración aislada ya existente `gboValidationBuild=true`, que omite los endpoints/credenciales locales. No se editan los archivos privados. La suite `PhotoNamesIntegration10ETest` exige expresamente la bandera ordinaria de fotografías V2; se ejecuta por separado con su transporte HTTP interceptado en memoria. Debug y Release de entrega se compilan después con configuración ordinaria, sin el flag de validación.

Las evidencias generadas por pruebas antiguas que escribían en docs se redirigen a `app/build/outputs/cierre-1.4.5/`. Se conservan los resultados históricos; ese ajuste no modifica expectativas ni el código de la aplicación.

La tanda inicial de proyecto y valores de sesión ejecutó 17 casos: 16 aprobados y uno fallido por una expectativa del nuevo fixture sobre la marca técnica de cierre. Se verificó la consulta existente `closeSessions/markOpenSessionRecordsPending`, y se corrigió la prueba para exigir esa marca y comprobar que todo el contenido del registro permanece igual. Los resultados iniciales se conservan.

La primera batería amplia se interrumpió al agotarse C: durante la extracción del runtime nativo de Robolectric. Sus errores de E/S y carga nativa no constituyen resultados válidos de regresión. Se conservó su log, se trasladaron de forma recuperable solo seis directorios temporales de esa ejecución a D: y se configuraron los temporales siguientes en D:, un único proceso de pruebas y renovación de JVM cada 20 clases. Tras liberar espacio el propietario, la reanudación encontró 6,8 GiB libres en C: y ningún proceso Java pendiente. No se encontró `ESTADO_PAUSA_GBO.md` en el proyecto ni en la búsqueda de Downloads; se reconstruyó el punto de continuación con Git, `baseline.json`, los logs y los respaldos existentes.

La primera ruta temporal en D: aún produjo nombres superiores al límite nativo de Windows. Se conservaron resultados parciales y se acortó la ruta, utilizando finalmente `D:\gbo145-tmp` para las repeticiones focalizadas. El caso de persistencia de Pavimentos que fallaba al abrir SQLite aprobó con esta ruta, sin cambiar el producto.

La batería amplia completa ejecutó **692 casos: 616 aprobados y 76 fallidos**. Se conservaron XML y log completos antes de repetir solo casos afectados. Entre los fallos se encontró la regresión real de ruta descrita arriba; los otros resultados exigieron revisar fixtures y contratos vigentes, en lugar de revertir funcionalidades aprobadas:

- Migraciones que exigían Room 11/15/18 o reconstruían una base 17 cambiando solo `user_version` de la base 19. Ahora usan las migraciones reales hasta 19 y comparan todas las columnas históricas; verifican además que los nuevos campos empiezan vacíos. No se modificó ninguna migración del producto.
- Expectativas anteriores a Drive V4: números asignados por el cliente, rutas SIB, ACK de ruta incorrecta y estado «Subiendo» sin un worker activo. Las pruebas verifican ahora UUID estables, rechazo de ACK incorrecto y los estados actuales. No se cambió el protocolo ni el ordenamiento.
- Pruebas que esperaban bytes PNG con nombre JPEG o un XLSX idéntico después de editar Observaciones. Ahora exigen JPEG real sin tocar el original y que cambie solo la celda de Observaciones, manteniendo todas las demás partes del libro.
- Datos ficticios incompatibles con las reglas aprobadas: lado de túnel, más de tres cifras enteras de distancia, cierre SCAP sin confirmar pendientes y preparación de entrega sin PR. Se ajusta la entrada ficticia; se mantienen los rechazos de entradas inválidas, protección de operador y de filas anuladas.
- Fixtures Compose sin AndroidX Startup/WorkManager, consultas asíncronas no esperadas, segunda llamada a `setContent`, controles fuera del viewport y textos anteriores. Se aíslan sus workers sin ejecutar transportes y se usan controles reales actuales.
- La comparación de catálogo SCAP deja de depender de un JSON de extracción ausente y lee el XLSX aprobado que se empaqueta. Los exportadores históricos conservan el tratamiento explícito del bloque D.4 y la visibilidad de la plantilla; las plantillas canónicas conservan sus pruebas propias.

Los cambios a pruebas no desactivan casos, no añaden `Ignore` y no cambian la aplicación para satisfacer expectativas obsoletas. Cada repetición y sus resultados quedan separados en la carpeta `validacion` de la entrega.

**Resultado consolidado: 698 casos distintos aprobados, 0 fallidos pendientes, 0 omitidos.** Se calcula tomando el último resultado de cada caso de la batería amplia y sus repeticiones focalizadas, más fotografías V2 ordinarias y la nueva prueba de ruta. No se presenta como una última ejecución monolítica de 698 pruebas. El antiguo nombre del caso PNG se sustituye por su prueba de JPEG real, sin contar dos veces el mismo caso.

| Ejecución local | Casos | Fallos en esa ejecución |
|---|---:|---:|
| Batería amplia inicial | 692 | 76 |
| Aceptación, sincronización, formulario y render | 34 | 1 |
| Fecha, ruta, formularios y migraciones | 57 | 2 |
| Dos fixtures finales de migración de fotos | 2 | 0 |
| Configuración ordinaria: fotografías V2 y reapertura Pavimentos | 6 | 0 |
| Repetición de interfaz afectada | 26 | 4 |
| Contratos/fixtures pendientes | 38 | 6 |
| Seis casos restantes | 6 | 0 |

`validacion/resumen-pruebas.json` contiene el recuento y método; `validacion/casos-verificados.csv` identifica cada caso y el XML de su última fase. Los XML de las fases anteriores se conservan para mostrar los fallos y cómo quedaron resueltos. También se conservan los errores de invocación o recursos en los logs; no se cuentan como pruebas aprobadas.

Cobertura local: autenticación y transporte simulado, inicio/configuración/cierre de sesión, ruta/calzada entre SIC/SCAP/Pavimentos, borradores y UUID, observaciones y XLSX, fotografías originales/JPEG/V2, videos SCAP, ACK/revisiones/reintentos offline, ordenamiento/KMZ, dashboard/navegación y Compose en ambas orientaciones con texto grande. Se verifican migraciones hasta Room 19 desde los esquemas históricos disponibles; no hay esquemas exportados 1–3 y no se afirma una actualización física desde ellos.

La Debug ordinaria terminó correctamente y la tarea `validateReleaseConfiguration` confirmó la configuración de servicios y el certificado histórico, sin exponer secretos. La APK Release se genera con `assembleRelease` ordinario, sin `gboValidationBuild` ni flags exclusivos de pruebas. Permanecen los avisos existentes del JDK/AGP y una API de memoria de Android obsoleta; no se modifican dependencias para silenciarlos.

## APK y compatibilidad

APK generada y verificada: `output/GBO_RUTA_CLARA_RELEASE_1.4.5_19/GBO_Ruta_Clara_1.4.5_19_RELEASE.apk`.

- Nombre visible: GBO Ruta Clara.
- Versión: **1.4.5 (19)**; paquete `com.tuempresa.inventariovial`.
- Tamaño: **26.224.916 bytes**.
- SHA-256 APK: `916a9f7250cd301fb789ae613b5cecc9e93176c142479ec47d91d0cfa6f181d4`.
- Firma Android verificada, no depurable, sin `testOnly` ni split y alineación de 4 bytes/16 KiB validada mediante Android Build Tools.
- `assembleRelease`: **BUILD SUCCESSFUL**, con `lintVitalRelease` y `verifyReleaseIdentity` aprobados.

La copia de entrega tiene el mismo hash que la salida de compilación y pasó de nuevo el control de identidad sobre su ruta final. `validacion/identidad-apk.json` contiene versión, SDK, esquemas de firma, certificado, alineación y hash; `SHA256SUMS.txt` permite verificar la copia transferida.

Certificado requerido: `0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315`. No se genera una clave nueva. La firma se verifica privadamente antes de compilar y de nuevo sobre el APK empaquetado.

**Certificado histórico confirmado en el APK final.** La comparación del manifest empaquetado con la candidata Release 1.4.4 (18) muestra únicamente tres atributos distintos: versionCode, versionName y nombre comercial. Paquete, permisos, componentes, SDK, red y restricciones de copia permanecen iguales. Se incluyen ambos manifests y su diferencia.

La configuración Release conserva fotografías V2 y videos remotos habilitados, tal como estaban en los archivos locales. `ACCESS_CONTROL_ENABLED=false` conserva su valor anterior y el login Usuario/PIN existente; no se interpreta como ausencia del login ni se activa otra autorización. La tarea de prevalidación comprobó configuración HTTPS de Usuarios/Drive y clave histórica. No se imprimen URLs privadas, tokens ni contraseñas en los informes.

`validacion/conservacion.json` confirma los cuatro archivos privados sin cambios e ignorados por Git, los APK anteriores con hash intacto y el inventario exacto de fuentes cambiadas frente al respaldo. Autenticación, recursos visuales, Room/migraciones, WorkManager y backend permanecen iguales frente a ese respaldo. Se conservó también el mapping anterior antes de ejecutar R8 y se incluye el mapping nuevo en la entrega local.

VersionCode 19 supera las Release históricas 16, 17 y la candidata histórica 18. Paquete y certificado deben coincidir para proponer la actualización. Esto no prueba una instalación conservando datos. Las migraciones y persistencia locales se verifican con bases ficticias; la actualización instalada sigue siendo una prueba distinta.

No instalar esta Release sobre una Debug de certificado diferente. No desinstalar ni limpiar para resolver un rechazo de Android. La reversión a un APK con código inferior no es una actualización ordinaria; conservar los respaldos y planificar cualquier recuperación por separado.

## Validación final del propietario

1. Elegir una tablet de prueba con Release histórica identificada, respaldo recuperable y registro de fichas/medios/entregas pendientes. La prueba debe estar autorizada y aislada de los datos de ingenieros.
2. Verificar el SHA-256 de la candidata entregada y abrirla para **actualizar** la instalación compatible. Ante rechazo por firma o versión, detenerse; no desinstalar ni borrar datos.
3. Comparar antes/después usuarios, configuración, identidad, UUID, borradores, observaciones, fotos, videos y colas. Probar inicio de sesión, funcionamiento offline, reapertura y una sesión nueva: fecha editable, giro, ruta/calzada y propagación a SIC, SCAP y Pavimentos.
4. Cualquier entrega ficticia a Drive requiere un ámbito expresamente autorizado. Esta ejecución no hace solicitudes a servicios reales ni prueba ACK de producción. El funcionamiento de Usuarios anterior/nuevo está confirmado por el propietario, no se presenta como una nueva prueba remota del agente.
5. Aprobar por escrito los resultados de esa prueba antes de distribuir a los ingenieros. Entregar exactamente el APK cuyo hash consta en este informe y conservar las versiones anteriores y su inventario por tablet.

## Estado de entrega

| Punto | Resultado |
|---|---|
| Autocompletado con fecha local y edición/rotación | Implementado y probado |
| Otros fallos funcionales corregidos | Pérdida de ruta al actualizar tramo; conservación del texto y entradas de sesión |
| Dashboard, herramientas, mapa, assets A/B | Conservados íntegros; regresiones locales aprobadas |
| SIC, SCAP, Pavimentos, borradores, UUID y sincronización | Pruebas locales aprobadas; protocolos y numeración conservados |
| Debug ordinaria | Compilada |
| Release 1.4.5 (19) con firma histórica | Compilada y APK auditada |
| Candidata para prueba final en tablet compatible | Preparada |
| Actualización física conservando datos / ACK de producción | No ejecutados en este cierre |
| Aprobación para distribuir a ingenieros | Pendiente del propietario tras la prueba física |
| Commit / push / instalación / Apps Script | No realizados |

Rama conservada: `codex/observaciones-campo-manual-10I`. HEAD sigue siendo `5de1242581dc460ea38d45006020d022f8d4da78`; el trabajo de Ruta Clara y de este cierre permanece local sin commit ni push. No se modifica el checkpoint, `main`, la copia estable anterior ni ninguna tablet.
