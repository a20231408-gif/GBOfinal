# GBO P0.2 — Validación de la configuración restaurada

30/10/2026, Lima. Repositorio `C:\GBO`; rama `codex/observaciones-campo-asus-20261030`; HEAD `0131135cd216febc409c6ba579771073cbce8383`. Android conserva 1.4.18 (32), Room 21. Se revisaron los informes P0.1, P0 y consolidado. No se implementaron funciones ni se modificaron fuentes, pruebas, versiones o dependencias.

## 1. Resultado ejecutivo

- **Configuración Release y certificado histórico: PASS.** La clave recuperada coincide con la identidad fijada en el proyecto; no hubo bypass ni nueva firma.
- **Los cuatro ensayos antes bloqueados: 20/20 casos PASS**, clases completas, V2 legítimo habilitado.
- **Selección completa solicitada y HTTP: 77 casos ejecutados, 76 PASS y 1 FAIL**, cero errores y cero omitidas. Se repitió y conservó el mismo fallo; no se declara PASS general.
- **BuildConfig efectivo:** GBO_PHOTO_NAMES_V2_ENABLED=true; SIC17A_ENABLED=false; SIC17B_ENABLED=false. No se cambiaron flags.
- **Capacidades del servidor real: no verificadas.** Todo el tráfico de los ensayos fue simulado; no se contactó Drive, Apps Script ni Usuarios.
- Entorno local recuperado para validar; aún no existe aprobación de distribución, validación física de tablets ni resolución acreditada del incidente de Flavio.

## 2. Archivos y configuración reconocidos

| Archivo | Comprobación |
|---|---|
| server.local.properties | Presente en raíz, no rastreado e ignorado; servicio HTTPS configurado y flags reconocidos |
| drive.local.properties | Presente en raíz, no rastreado e ignorado; token presente y formato de endpoint /exec válido |
| keystore.properties | Presente en raíz, no rastreado e ignorado; las cuatro propiedades de firma están presentes |
| gbo-inventario-vial-release | Archivo binario de firma en raíz, sin extensión; reconocido y abierto por la guarda Release |

El archivo de firma aparecía inicialmente sin seguimiento, pero no estaba ignorado por carecer de extensión .jks/.keystore. Se añadió únicamente `/gbo-inventario-vial-release` a `.git/info/exclude`, exclusión local no versionada. No se añadió la clave al índice ni a un commit. BuildConfig y los productos de compilación también permanecen ignorados.

La ruta GBO_RELEASE_STORE_FILE no existía en ASUS. Se ajustó exclusivamente esa línea de keystore.properties a la ubicación relativa local del archivo recuperado. Se comprobó en memoria que las demás propiedades no variaran; se conservaron sus bytes fuera de esa sustitución. No se copiaron secretos ni contenido privado a informes, consola, respaldo o comandos. Durante validación, los cuatro archivos privados permanecieron sin cambios después de esa corrección de ruta.

No hay sobrescrituras detectadas para las cuatro propiedades GBO_RELEASE_* mediante variables de entorno del proceso, ORG_GRADLE_PROJECT_*, gradle.properties del proyecto o del usuario. No se suministraron valores -P de firma. La tarea efectiva de Release confirma la configuración resuelta.

Usuarios y Drive tienen endpoints distintos. Se validaron formato y separación en memoria, sin publicar valores ni probar acceso. No se editó ninguno. **No se puede certificar igualdad con los originales Huawei sin un manifiesto privado fiable o comparación directa en origen.** La procedencia del traslado es la declarada por el propietario; la firma sí se verificó criptográficamente. La igualdad de endpoints debe comprobarse privadamente si se requiere acreditar esa parte del traslado.

### SDK y herramientas

La primera tentativa de tests falló antes de ejecutar casos: local.properties contiene sdk.dir hacia un directorio inexistente. Se usó el SDK ya instalado en ASUS mediante ANDROID_HOME y ANDROID_SDK_ROOT, asignados solo al proceso. No se modificó local.properties. Se comprobó plataforma android-37.0 y Build Tools 36.0.0 presentes. JAVA_HOME se asignó al JBR de Android Studio para cada ejecución.

Para repetir desde otra terminal sin cambiar archivos privados:

```powershell
Set-Location C:\GBO
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
```

El aviso sobre sdk.dir obsoleto puede seguir apareciendo; la ejecución con el SDK ASUS sí funciona. Pendiente corregir esa configuración local de desarrollo de forma explícita o conservar estas variables por proceso; no confundir este problema con firma o permisos de Google.

## 3. Firma Release

Se ejecutó `:app:validateReleaseConfiguration` sin gboValidationBuild ni otra omisión de guardas. La tarea informó `Release configuration and historical certificate verified; no secrets displayed` y terminó correctamente. En la última ejecución: 6,02 s de proceso (Gradle comunicó 5 s).

La tarea abrió el keystore con su configuración legítima, acreditó clave privada/alias y comparó internamente el certificado SHA-256 con el fingerprint histórico público:

`0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315`

También superó las guardas locales de configuración Usuarios/Drive. Esto acredita identidad y configuración local, **no** conexión real, permisos remotos, un APK firmado nuevo ni compatibilidad de instalación. No se ejecutó assembleRelease, no se generó otra clave ni se modificó el fingerprint esperado.

## 4. V2, SCAP y contrato

Se verificaron las tres banderas solicitadas tanto en el archivo recuperado mediante carga Java Properties como en el BuildConfig Debug regenerado por Gradle:

| Bandera efectiva | Valor |
|---|---|
| GBO_PHOTO_NAMES_V2_ENABLED | true |
| SIC17A_ENABLED | false |
| SIC17B_ENABLED | false |

No se activaron artificialmente SIC-17A/SIC-17B ni se reinterpretó un PASS como habilitación de esos módulos en la interfaz.

El cliente y las simulaciones prueban syncProtocol 3, pathVersion 4, numeración estable, categorías compatibles, readablePhotoNamingVersion=2 y panel SCAP V2; además se verifican revisiones, stage y ACK íntegro. No es evidencia de las capacidades actuales del despliegue productivo. Una versión Android idéntica en dos tablets tampoco garantiza flags o configuración idénticos.

## 5. Pruebas ejecutadas

Gradle offline, sin daemon persistente ni configuration cache, 1 worker, heap 2048 MiB, 2 procesadores visibles; ejecutor derivado de tools/migracion/pruebas.init.gradle, que fuerza ejecución de Test. Datos ficticios y transportes/interceptores simulados; sin pruebas instrumentadas de integración real.

| Clase completa | Ejecutados | PASS | FAIL |
|---|---:|---:|---:|
| PavementAnnotationSync147Test | 1 | 1 | 0 |
| ScapFinalP0Test | 9 | 9 | 0 |
| SyncRecovery10DTest | 7 | 7 | 0 |
| HotfixPerformance146Test | 3 | 3 | 0 |
| AutomaticSyncRegressionTest | 21 | 21 | 0 |
| DriveDeliveryPresentationTest | 11 | 11 | 0 |
| DriveDeliveryCardUiTest | 4 | 4 | 0 |
| DriveDeliveryListsTest | 6 | 6 | 0 |
| DriveDeliveryListsUiTest | 3 | 3 | 0 |
| ReadablePhotoNames10Test | 4 | 4 | 0 |
| PhotoTransport10Test | 6 | 5 | 1 |
| ScapHttpAuthorizationP0Test | 2 | 2 | 0 |
| **Total** | **77** | **76** | **1** |

Se ejecutaron todos los métodos de las clases; cero omitidas y cero errores. Los cuatro ensayos previamente bloqueados alcanzaron sus aserciones V2, incluida recuperación SCAP con pérdida de ACK y el ensayo de carga con dos bases simuladas. Pasaron offline/cola/revisiones, BUSY, staging recuperable, ACK estricto, nombres reservados, HTTP 401/403 y UI sin falsa confirmación. Una regresión de recuperación JPEG, descrita abajo, sigue sin acreditar sus condiciones finales.

### Evidencias y aislamiento

- Carpeta nueva raíz: `tmp/p02-20261030/`, ignorada por Git. Logs filtrados en memoria para omitir valores privados conocidos y URLs; no se imprime BuildConfig completo.
- Primer intento: firma PASS, tests detenidos antes de ejecutar por SDK inexistente. Los XML que entonces seguían en app/build pertenecían a P0.1: las copias iniciales `xml/` y `resultados.json` **no son resultados P0.2**. `ADVERTENCIA_XML_ANTERIORES.json` lo registra; no se cuentan esos 17 casos como ejecutados hoy.
- `reintento-sdk/`: firma PASS y 77 casos, 76/1; 182,06 s con recompilación por flags/configuración restaurados. Se conservaron XML, logs y las copias previas de salidas sintéticas relativas de UI/Pavimentos.
- Algunas pruebas históricas escriben capturas/evidencias sintéticas en rutas relativas fijas. Para la repetición final se aisló además el workingDir del proceso Test mediante init temporal, sin editar pruebas. Las salidas relativas quedaron bajo sandbox nuevo; no se redirigieron registros reales.
- **Evidencia definitiva:** `tmp/p02-20261030/validacion-final/`: `release-check.log/json`, `tests.log/json`, `resultados.json`, `xml/`, `evidence/`, `sandbox/` y `p02.init.gradle`. Repetición de los 77 casos: mismo resultado 76/1, 50,05 s con compilación caliente (Gradle comunicó 49 s).
- Se copiaron solo XML actualizados durante la ejecución final, no resultados anteriores. No se suman repeticiones como casos distintos. Los temporales nativos usan rutas cortas nuevas tmp/t2, t3 y t4; no se borraron evidencias anteriores.

Los tiempos no son una comparación nueva ASUS/Huawei ni una medida Google. No hubo escrituras de red a servicios productivos. No se compilaron ni distribuyeron APK en esta fase.

## 6. Fallo restante: causa investigada

`PhotoTransport10Test.rejectedCachedRevisionIsBackedUpAndRebuiltWithSameIdentityAndPhoto` falla con `SyncPending: Pendiente de reservar el nombre fotográfico en el servidor`.

El fixture crea una foto e invoca directamente `AutomaticDelivery.prepare(context, db, intent)` sin registrar una reserva V2. Ahora el parámetro por defecto readable toma el valor true del BuildConfig legítimo. `ReadablePhotoNames.name` exige la reserva y detiene la preparación inicial, antes de simular el rechazo NOT_JPEG y alcanzar las aserciones de reparación.

El drenaje ordinario tiene una fase de reserva antes de preparar; las pruebas de ese recorrido y la recuperación de ACK sí aprobaron. El fallo reproducido es una precondición faltante del fixture al ejecutarlo bajo V2, no evidencia suficiente de pérdida de fotos en producción. **No se declara aprobada la reparación JPEG de ese caso.** No se añadió una reserva ficticia a datos reales, no se deshabilitó V2 ni se alteró el test para obtener PASS.

Siguiente revisión acotada: adaptar el fixture para negociar/registrar una reserva con transporte simulado válido antes de la primera preparación, conservar todas las aserciones de UUID/ruta/hash/original/copia rechazada y repetir la clase completa bajo V2. Corresponde a una tarea posterior autorizada de pruebas; no se hizo aquí.

## 7. Diferencias frente a P0.1 y errores pendientes

- Antes faltaban configuración y firma; ahora están presentes, protegidas de Git y la firma histórica pasa.
- Antes V2=false; ahora true desde el archivo recuperado, sin cambios artificiales. SIC17A/B permanecen false.
- Los cuatro fallos de perfil anteriores desaparecen al ejecutar realmente sus clases completas. Aparece una incompatibilidad adicional del fixture JPEG con V2, antes oculta por el perfil distinto.
- Se detectó sdk.dir inválido; los ensayos funcionan con SDK ASUS asignado al proceso. La ruta local obsoleta no se modificó.
- Capacidades remotas, permisos reales, revisión/ACK de ambas tablets y archivos en destino siguen sin comprobarse. No hay verificación física del caso de Flavio.
- Los antecedentes de fallos históricos fuera de esta selección no se reejecutaron ni se declaran resueltos. No hay certificación de suite general.

## 8. Riesgos y preparación para pruebas físicas

La identidad de firma ya permite preparar una futura compilación legítima, pero no autoriza distribución. No existe aquí una nueva Release ensamblada/auditada; 1.4.18 (32) se conservó y no debe reutilizarse automáticamente para distribuir correcciones nuevas. El único fallo actual sigue pendiente, así como validación física, permisos/capacidades remotas y coherencia de archivos/ACK reales.

No se modificó Google Drive, Apps Script V21, Servicio de Usuarios, cuentas, carpetas o permisos. No se accedió a tablets, no se instaló APK ni se enviaron datos reales. Los nombres ordinales del servidor y las propuestas de migración no se cambian al restaurar configuración ASUS.

Se está en condiciones de **preparar** el ensayo físico, usando la ficha comparativa de P0.1 y el checklist P0, pero su ejecución sigue requiriendo autorización de acceso/instalación y, para envíos, destino aislado aprobado. Solo un ACK íntegro de revisión actual más archivos SCAP/SIC comprobados en el destino esperado permitirá evaluar el incidente de Flavio.

## 9. Recomendación P0.3 y estado Git

Prioridad: corregir únicamente la precondición V2 del fixture PhotoTransport10Test con reserva simulada y repetir toda la selección; no modificar lógica de producción por un fallo que aún no demuestra un defecto productivo. Regularizar luego la ruta SDK local del entorno de desarrollo con autorización, y preparar la comparación de tablets mediante lectura autorizada, sin reenviar ni editar fichas para recolectar datos.

En esta fase solo se creó este informe como archivo versionable; no se hizo commit ni push. Cambios locales privados autorizados: ruta del keystore y exclusión .git/info/exclude. Todos los archivos rastreados de la base permanecen intactos. Los cuatro privados y BuildConfig generado están ignorados y fuera del índice. Nunca añadirlos a commits ni trasladarlos por Git. Las comprobaciones finales de integridad se conservan en tmp/p02-20261030 sin secretos.
