# GBO Ruta Clara 1.4.4 — recuperación local

Fecha: 2026-10-15. Candidata local; no distribuida ni instalada. Este informe continúa y actualiza la situación del documento de etapa A fechado el 14 de octubre; no debe interpretarse aquella consulta ADB como el estado actual de la tablet.

## Estado y respaldo

Rama `codex/observaciones-campo-manual-10I`, base `5de1242581dc460ea38d45006020d022f8d4da78`; checkpoint `checkpoint-pre-redisenio-visual-2026-10-14` conservado. Sin commit ni push. Se conservaron los cambios visuales pendientes, los cinco recursos aprobados, los documentos y archivos ajenos a esta corrección.

Antes de editar se creó `tmp/ruta-clara-recovery-20261015/backups/source-before.zip` con 152 fuentes/recursos y copias de las APK locales. El manifiesto de respaldo contiene hashes; no copias de claves ni valores de configuración privada. También se conserva `backups/release-1.4.3-17-historical.apk`, idéntica a `D:\Downloads\GBO V0.apk`. `app/release/app-release.apk` no se sobrescribe.

APK históricas verificadas realmente con herramientas Android:

| Archivo local | Versión | Certificado |
|---|---|---|
| `D:\Downloads\GBO actualizacion.apk` | 1.4.2 (16) | Release histórica |
| `D:\Downloads\GBO V0.apk` | 1.4.3 (17) | Release histórica |
| `diagnostico-apk/base-instalada.apk` | 1.4.3 (17) | Android Debug |

No se encontró `GBO V0(1).apk` en el inventario local de Downloads. La APK extraída coincide con el SHA-256 indicado por el propietario. No hay una consulta actual de sus registros privados: ADB no detectó dispositivos en esta ejecución.

## Autenticación: lo comprobado y su límite

No está demostrada la causa del intento fallido contra el servicio publicado. No consta una ejecución doPost correlacionada ni se ha hecho una solicitud remota en este trabajo. La lista local vacía no explica ese rechazo con `ACCESS_CONTROL_ENABLED=false`. No se atribuye el fallo al token ni a una huella incorrecta sin evidencia.

Defectos Android comprobables corregidos:

- `UserServiceClient.kt`: los errores distinguen configuración protegida, autorización local, envío HTTPS, redirección, HTTP, lectura de respuesta y contrato JSON. Una interrupción durante la lectura ahora se clasifica como transporte, sin perder su etapa. Se muestran etapa y HTTP cuando existe, nunca URL, token, PIN ni cuerpo remoto.
- Se rechazan redirecciones malformadas, repetidas o fuera del destino HTTPS permitido. Se conserva el salto único a `script.googleusercontent.com` mediante GET sin reenviar el cuerpo secreto del POST. No se añade una confianza TLS especial ni un bypass.
- `success` debe ser booleano real. Un contrato de Inventario con `ok`, HTML o un texto `"true"` no equivale a una respuesta válida de Usuarios. Los códigos de error remotos pasan por una lista permitida; un texto arbitrario no entra en la excepción.
- `UserSyncRepository.kt`: diferencia usuarios inválidos de un fallo de almacenamiento. Un fallo al escribir la auditoría de error ya no oculta la causa original. La validación, la transacción de reemplazo y la caché offline se conservan.

No se sustituyeron login, PIN, algoritmos de verificación, huella, cifrado Android Keystore, permisos ni caducidad de sesión. La rotación conserva URL y huella; reemplaza el token cifrado únicamente tras aceptación del servidor y limpia el CharArray suministrado. Aceptación del token no demuestra que una sincronización posterior de usuarios termine correctamente.

Usuarios sigue utilizando `action/deviceId/tabletToken/success`. Inventario y Drive mantienen sus propios contratos y configuración. No se modificaron sus fuentes, Room, WorkManager, esquemas, registros ni despliegues.

## Presentación preservada

Login y configuración inicial mantienen los fondos WebP aprobados, logo original proporcionado sobre blanco, tarjetas opacas, disposición horizontal/vertical, scroll e insets de teclado. Animación Compose de 200 ms sujeta a la escala de animación de Android, sin retrasar acciones.

`LoginFormInputs` conserva las entradas solo en memoria durante la recreación de Activity. No guarda PIN/token en Bundle, SavedStateHandle, preferencias ni logs; al abandonar acceso se limpian las referencias. No se han alterado otras pantallas.

Los cuatro PNG de `app/build/outputs/ruta-clara/renderizados-locales/` son renderizados de la UI Android/Compose en Robolectric, **no capturas de la tablet**. Las pruebas de altura reducida y fuente 2× comprueban acceso mediante scroll; la interacción con un teclado físico/IME real sigue pendiente.

## Compatibilidad y preparación de distribución

Se preparó `versionName=1.4.4`, `versionCode=18`, manteniendo `com.tuempresa.inventariovial`. La tarea de validación abrió privadamente la clave histórica y confirmó su certificado antes de compilar. No se generó otra clave ni se imprimieron contraseñas.

Certificado Release esperado y comprobado en las referencias: `0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315`.

La compatibilidad de paquete, firma y versión permite proponer una actualización sobre las Release históricas 16/17. No sustituye una prueba de instalación conservando datos. No hay AVD disponible ni dispositivo conectado autorizado como entorno aislado; esa prueba permanece pendiente. Las migraciones se verifican localmente con datos ficticios.

La Release **no actualiza** la Debug de certificado `d2e54cb086aa731ef046be300977d0b16c0d8f66fdcb08d0d9cfd2038bf0d872`. No se propone desinstalar ni limpiar esa tablet. El respaldo 1.4.3 (17) puede ser candidato de actualización de una Release 16 con su misma firma; no es una reversión instalable ordinaria sobre 18 y su funcionamiento remoto tampoco queda aprobado por su firma.

## Pendientes externos y decisión

Mantener las instalaciones de campo actuales hasta cerrar la prueba controlada. No distribuir Ruta Clara solamente porque compila o tiene la firma correcta.

Para investigar el caso remoto basta inicialmente con el nuevo mensaje de etapa/HTTP, hora del intento con zona horaria y su correlación en Ejecuciones del Apps Script de Usuarios. Si hay que revisar código publicado, facilitar únicamente las funciones de recepción/autorización y estructura de respuesta, sin tokens, PIN ni propiedades privadas. No hace falta compartir credenciales en el chat.

Para probar contra el servicio real hace falta autorización explícita para un intento de sincronización de una tablet/ficha administrativa identificada: incluso syncUsers podría generar auditorías. Una entrega de inventario ficticio a Drive requiere además ámbito y carpeta autorizados. Esta ejecución no hace ninguna de esas solicitudes.

Tras aprobar la validación, el propietario deberá:

1. Elegir una tablet de prueba con Release histórica y respaldo recuperable; identificar pendientes, versión y firma antes de actualizar. No usar la Debug de firma distinta.
2. Instalar la candidata como actualización, sin desinstalar ni limpiar. Verificar identidad, usuarios, configuración, Room, fotos, videos y colas antes/después. Si Android rechaza la actualización, detenerse.
3. Comprobar login, sincronización de usuarios, retorno de segundo plano, modo offline y una entrega ficticia expresamente autorizada con su ACK. Comprobar ambos sentidos de pantalla y teclado real.
4. Solo con esos resultados aprobados, distribuir exactamente la APK cuyo SHA-256 figura en la entrega. Conservar el respaldo y registro de versiones por tablet. Una reversión posterior a 18 necesitaría una estrategia compatible con datos y versionCode; no forzar downgrade.

Los parches de Inventario siguen independientes y sin publicar: `docs/parche-apps-script-10k/{SicObservationsV1Core.gs,SyncV3.gs,SyncV3Core.gs}`, la incorporación puntual de capacidad en `Code.gs`, y los módulos de nombres legibles de video descritos en `CAMBIOS_APPS_SCRIPT_10H.md`. Seguir los procedimientos 10H/10I/10K, preservando el Code.gs y configuración realmente publicados. No mezclarlos con Usuarios.

## Resultados de esta ejecución

Pruebas focalizadas: **67 casos aprobados**, en dos ejecuciones complementarias; no se repitió la auditoría general de inventario.

| Suite | Casos aprobados |
|---|---:|
| UserAuthenticationTest | 26 |
| UserServiceTransportTest | 17 |
| AuthForegroundRecoveryTest | 2 |
| AuthRutaClaraScreenTest | 6 |
| AuthRutaClaraRenderTest | 4 |
| DeviceFingerprintTest | 3 |
| TabletAdminAndDiagnosticsTest | 4 |
| InventoryRoomMigrationTest | 3 |
| IntegratedMigration12To14Test | 1 |
| SicMigration14To15Test | 1 |

Las dos últimas suites esperaban obsoletamente Room 18; se ajustó la expectativa a 19, que ya estaba en el código y esquema antes de esta tarea. Se conservaron todas las comprobaciones de tablas, columnas, valores, duplicados y reapertura. Pasaron al repetir únicamente esas dos suites. No se modificó ninguna migración.

En `UserAuthenticationTest` se inicializó WorkManager con ejecutores que no ejecutan trabajo: el fixture de sesión de campo puede encolar sin contactar endpoints reales. Se añadieron pruebas de clasificación de usuario inválido y fallo de auditoría. Los 17 casos HTTP interceptan todas las solicitudes en memoria y no llaman a producción.

Debug compilada correctamente. Los cuatro renderizados fueron revisados visualmente. El control de configuración confirmó que Debug y Release ordinarias contienen los mismos valores locales de servidor, Drive y banderas relevantes, sin mostrarlos. Los cuatro `.properties` privados conservaron su hash. Esto no comprueba la URL/token cifrados que haya en una tablet ni que el servidor los acepte.

Evidencia local conservada en `tmp/ruta-clara-recovery-20261015`: logs de compilación, XML de `tests-initial` y `tests-migrations-final`, manifiesto de respaldo y comprobaciones de preservación/configuración. Los primeros intentos de compilación requirieron corregir la declaración suspend del punto de prueba y el iterador JSON del fixture; los resultados anteriores no se presentan como aprobaciones.

Release compilada con configuración ordinaria, sin flags de pruebas: `assembleRelease` y `verifyReleaseIdentity` terminaron correctamente. Lint vital pasó. La verificación del APK exige firma válida v2/v3, certificado histórico único, paquete y versión esperados, alineación incluida la de páginas de 16 KiB, APK completo y ausencia de condición Debug/test/split.

- Entrega: `output/ruta-clara-1.4.4-18-20261015/GBO_Ruta_Clara_1.4.4_18_CANDIDATA.apk`.
- Tamaño: **25 885 703 bytes**.
- applicationId: `com.tuempresa.inventariovial`.
- versionName / versionCode: **1.4.4 / 18**.
- SHA-256 del APK: `e8172e1a1cb0a23d2a07bab1d370a933eef6136d20a65cfcadcd3c8bdf14728e`.
- SHA-256 del certificado: `0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315`.

**Decisión: candidata local apta para continuar la validación controlada; distribución de campo pendiente.** No se ha probado login remoto, sincronización real de usuarios, ACK de Drive ni actualización instalada. Conservar las instalaciones actuales; no sustituirlas automáticamente por esta candidata ni por el respaldo 17.
