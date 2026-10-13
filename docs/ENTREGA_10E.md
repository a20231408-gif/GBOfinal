# Entrega 10E — fotografías V2 en Run normal

Base: `be950cc97573b8d8610b7396f6ed0a41bd47733d`. Rama: `codex/cierre-fotografias-v2-10E`.

## Causa y corrección

10D produjo la APK ordinaria con `-PgboPhotoNamesV2Test=false`. El bloque Debug tenía prioridad sobre el valor de `server.local.properties`, por lo que BuildConfig terminó con V2=false aunque la propiedad privada estuviera habilitada. Se eliminó ese override: un parámetro antiguo ahora detiene Gradle con una explicación, evitando una desactivación silenciosa.

Al iniciar 10E, el archivo privado de este checkout contenía SERVER_BASE_URL y las opciones anteriores, pero **no contenía GBO_PHOTO_NAMES_V2_ENABLED**. Se añadió exclusivamente `GBO_PHOTO_NAMES_V2_ENABLED=true`; no se modificaron servicios, credenciales, ACCESS_CONTROL_ENABLED ni otras opciones. Este ajuste local queda fuera de Git. En otros checkouts también debe existir esa propiedad privada. No se añadió flavor, variante ni applicationId.

Run normal, seleccionando `app` / `debug` en Android Studio, lee los archivos privados sin parámetros fotográficos especiales. La compilación de entrega usa `:app:assembleDebug`, sin `gboPhotoNamesV2Test` ni `gboValidationBuild`. JAVA_HOME se ajusta solamente en el proceso al JBR existente de Android Studio.

## Capacidad y compatibilidad

Consulta real exclusivamente GET, registrada en `campo-10e/evidencia/server-capabilities.json`: readablePhotoNamingVersion=2, syncProtocol=3, pathVersion=4, stableFamilyNumbering=true, photoCategoryVersion=1, photoNamingVersion=1, technicalDeliveryWithoutGps=true. Permite la negociación V2 existente. No se desplegó Apps Script ni se escribió inventario remoto.

Se conserva el procedimiento existente: negociar capacidades → reservar nombres → preparar snapshot → staging idempotente → commit → validar ACK completo. Una reserva no confirma entrega. Los snapshots ya preparados conservan sus nombres y protocolo; las reservas históricas mantienen nombres V1. UUID, binding, numeración, límites contractuales, hashes, reintentos y comprobación del ACK no cambian. Con servidor sin V2, el cliente conserva la cola y evita una reserva incompatible.

En **Detalles de entrega Drive**, se muestra V2 habilitado en la compilación. **Consultar capacidad fotográfica** ejecuta un GET y muestra versiones públicas y el protocolo de una nueva entrega. Si la consulta falla, muestra “no comprobada”; no supone capacidad disponible. No presenta respuesta cruda, tokens, PIN ni endpoints privados. El estado de una entrega anterior sigue determinado por su propio snapshot y recibo.

## Identidad e instalación

Se conservan applicationId `com.tuempresa.inventariovial`, versión 1.4.2 y versionCode 16. No se cambió firma, Room, preferencias ni cálculo de identidad. DeviceAccessManager deriva la misma identidad del ANDROID_ID y conserva preferencias al recrearse; las pruebas locales comprueban identidad y reapertura de registros pendientes. Esto no sustituye una prueba de actualización física.

No hay dispositivo conectado por ADB. La APK se verifica contra el certificado Debug documentado en 10D. La firma de la instalación actualmente presente en la tablet no se pudo consultar. Si Android Studio indica incompatibilidad de firma, **detener la instalación**; no aceptar desinstalar, borrar datos ni cambiar claves. Mantener el mismo usuario Android y la clave Debug usada en las instalaciones anteriores.

Se inspeccionó `app/release/`: APK, mapping.txt y output-metadata.json preexistentes. No se borraron, no se reconstruyó Release ni se añadieron esos archivos a Git. Su APK tiene una firma distinta de Debug; no usarla como actualización de esta entrega.

## Prueba manual mínima de V2

1. En el mismo checkout, comprobar la propiedad privada V2=true, elegir `app` / `debug` y pulsar Run con la tablet habitual. No pasar overrides de 10D ni seleccionar una compilación de validación. Actualizar sobre la aplicación compatible existente.
2. Consultar Detalles de entrega Drive: compilación V2 habilitada, servidor readablePhotoNamingVersion=2 y nueva entrega V3 / V4 / nombres V2 con reserva.
3. Crear **una Alcantarilla totalmente ficticia y autorizada**, identificándola como prueba, con fotografías de prueba. Validar/cerrar y esperar “Sincronizado”; comprobar revisión confirmada igual a local, UUID y destino confirmado.
4. Comprobar en su destino Drive el nombre representativo de las fotos nuevas, dentro del límite de 20 caracteres. Las fotos históricas mantienen sus nombres. No modificar fichas existentes de ingenieros, borrar registros ni usar una entrega bloqueada real para escritura.

No repetir las seis pruebas físicas aprobadas por el propietario en 10D.

## Videos: siguiente etapa

El cliente existente de Puentes/SCAP permanece en la aplicación normal. VideoHttp negocia `videoProtocol=1` y `videoScope=SCAP_ONLY`; VideoSync exige la bandera local habilitada, ficha padre confirmada, hash del original y ACK completo del video. SIC y Pavimentos quedan fuera de alcance.

El servidor real anuncia videoProtocol=0 y la compilación mantiene GBO_VIDEO_REMOTE_ENABLED=false. No se activaron cargas remotas. Para la etapa autorizada posterior: el propietario debe validar y habilitar el backend de videos, comprobar protocol=1 y scope=SCAP_ONLY, establecer privadamente `GBO_VIDEO_REMOTE_ENABLED=true`, recompilar mediante Run normal y probar únicamente un Puente/SCAP ficticio con padre confirmado. No basta activar la bandera del cliente. Esta entrega no requiere actualizar archivos del backend para fotografías V2.

## Resultado y pendientes

**BUILD SUCCESSFUL en 5m12s; 9/9 pruebas focalizadas aprobadas, 0 fallidas.** Una sola APK ordinaria: `app/build/outputs/apk/debug/app-debug.apk`, 40.305.994 bytes. SHA-256: `c11a404dbcef2e4ab89cf6440c9ced74975a061f0988b1d30982f4e46f0b1f22`. Certificado Debug: `d2e54cb086aa731ef046be300977d0b16c0d8f66fdcb08d0d9cfd2038bf0d872`, idéntico al documentado en 10D. Se verificó BuildConfig compilado V2=true, servicios presentes, videos remotos=false y ACCESS_CONTROL_ENABLED=false sin publicar valores privados. APK y archivos de configuración permanecen fuera de Git.

Archivos de implementación modificados: `app/build.gradle.kts`, `core/AutomaticDelivery.kt`, `core/AutomaticSyncPanel.kt`; nuevos: `core/PhotoNamingDiagnostic.kt` y `PhotoNamesIntegration10ETest.kt`. Se agregan este informe, `MATRIZ_10E.md` y evidencia JSON/XML. El cambio privado de configuración no forma parte del commit.

Resultados detallados en `MATRIZ_10E.md` y `campo-10e/evidencia/`. Pendientes: prueba física de la nueva entrega V2 ficticia y consulta de firma de la instalación presente en la tablet. Los videos remotos permanecen pendientes de su siguiente etapa autorizada. Las seis comprobaciones físicas de 10D se registran como aprobadas por el propietario, sin repetirlas.
