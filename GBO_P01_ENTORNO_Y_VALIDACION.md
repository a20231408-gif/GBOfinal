# GBO P0.1 — Entorno ASUS y validación

30/10/2026, Lima. Repositorio `C:\GBO`. Rama `codex/observaciones-campo-asus-20261030`. Sin funcionalidades nuevas ni intervención en dispositivos/producción.

## 1. Consolidación y seguridad

HEAD inicial: `cfb49517454124c3e17438a098d7482994d97fe7`. Se revisaron ocho archivos rastreados modificados, una prueba nueva y el informe P0. Se preservaron sin cambiar sus bytes, versiones ni criterios de pruebas.

Commits locales creados:

| Hash completo | Contenido |
|---|---|
| `3fd2dbf0f4b770fc0eede1296227ecf930c0e18c` | Clasificación HTTP 401/403 y pruebas de autorización |
| `6f84c82bcc8a9821655db4ad453b2e47bb4a13b0` | Estados/progreso/pendientes SCAP, ámbitos de contadores y pruebas |
| `33c9d8ec814fe60be7a6f511eb4b0199baf9dc05` | Informe P0 y resultados anteriores |

Este informe se conserva en un commit documental posterior. Su hash y el HEAD final quedan en `tmp/p01-20261030/commits-finales.txt`, fuera de este documento para evitar autorreferencia. La rama de migración permanece en dbe7247. No se modificó main ni se hizo push.

Revisión: diferencias completas y archivos nuevos leídos; solo se añadieron rutas explícitas de texto Kotlin/Markdown. Cero coincidencias en patrones dirigidos de clave privada PEM, API key Google, token OAuth y token GitHub. No hay propiedades privadas, certificados binarios, keystores, bases, fotografías ni APK entre los candidatos/commits. El fingerprint público de certificado citado para comparación no es una clave ni un certificado privado. Gitleaks no está disponible en PATH; no se declara escaneo exhaustivo de secretos históricos ni garantía universal.

Diez PNG no rastreadas permanecen en su ubicación original, fuera del índice. Se copiaron a `tmp/p01-20261030/png-respaldo/`, ignorado por Git, verificando tamaño/SHA-256 de cada copia. Inventario: `png-manifest.json`. No se presupone que su contenido esté autorizado para publicación. No se borraron, renombraron ni añadieron al repositorio.

Respaldo de los diez textos P0 antes de los commits: `p0-textos.zip` y `p0.patch` en esa misma carpeta. Evidencias y bundle final de la rama también quedan allí. Son respaldos en el mismo disco; no sustituyen una copia privada independiente. El bundle incluye historia Git y no debe publicarse automáticamente.

Estado final esperado y comprobado al cierre: sin cambios pendientes en archivos rastreados; únicamente las diez PNG originales sin seguimiento. No se ocultan mediante .gitignore ni se incluyen con git add general.

## 2. Configuración ASUS comprobada

| Elemento | Resultado actual |
|---|---|
| local.properties | Existe, ignorado; SDK ASUS ya permite compilar |
| server.local.properties | Ausente |
| drive.local.properties | Ausente |
| keystore.properties | Ausente |
| Keystore histórico | No localizado en la búsqueda de archivos de firma dentro de C:\GBO ni configurado; no se afirma ausencia en toda la laptop o respaldos externos |
| GBO_RELEASE_STORE_FILE / STORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD | Las cuatro variables ausentes en ámbitos proceso, usuario y equipo |
| JAVA_HOME / ANDROID_HOME / ANDROID_SDK_ROOT | Ausentes en esos tres ámbitos; JAVA_HOME se asignó solo al proceso Gradle de esta validación |
| BuildConfig Debug existente | GBO_PHOTO_NAMES_V2_ENABLED=false, SIC17A_ENABLED=false y SIC17B_ENABLED=false; no se cambiaron |
| Capacidades remotas | No consultadas ni verificadas; no hay autorización/configuración para declarar compatibilidad actual |

Las banderas ausentes toman su valor por defecto false. Compilar Debug no acredita configuración de servicios ni indica cómo están compiladas las tablets. No se cambiaron los destinos productivos ni se crearon archivos privados ficticios.

Requisitos cliente/servidor SCAP a comprobar tras restauración: V2 habilitado legítimamente en el cliente; syncProtocol=3, pathVersion=4, stableFamilyNumbering=true, photoCategoryVersion=1, readablePhotoNamingVersion=2 y panel SCAP compatible (scapPhotoPanelVersion al menos 2). La capacidad técnica sin GPS también debe coincidir cuando el payload la necesita. Esto es el contrato inspeccionado, no un resultado remoto medido hoy.

## 3. Recuperación segura desde Huawei

### Qué trasladar

Desde Huawei o un respaldo privado verificado: server.local.properties legítimo con sus flags actuales, drive.local.properties legítimo, keystore.properties y el archivo exacto del keystore histórico. Conservar alias/contraseñas existentes. No usar debug.keystore, generar otra clave ni cambiar el endpoint de Usuarios por el de Drive.

El respaldo Huawei documentado está bajo `D:\Downloads\GBO_RESPALDO_MIGRACION_20261029_041941Z`; su disponibilidad y contenido actuales no fueron comprobados desde ASUS. Los ZIP históricos no se consideran cifrados por tener ACL. Usar medio cifrado desbloqueado localmente o canal privado autenticado; no correo abierto, Git, chat ni carpeta de sincronización pública. No trasladar OAuth/clasp, cachés, bases de tablets ni fotos reales como parte de esta recuperación.

### Copia Windows sin sobrescritura

Los siguientes comandos son una guía para ejecutar manualmente después de disponer del medio privado; **no se ejecutaron**. Preparar una carpeta origen con los tres archivos .properties originales y elegir por separado el keystore histórico. Las entradas solicitadas son rutas locales, nunca contraseñas ni tokens.

```powershell
Set-Location C:\GBO
$gboOrigen = (Resolve-Path -LiteralPath (Read-Host 'Carpeta privada origen desbloqueada')).Path
$gboKeyOrigen = (Get-Item -LiteralPath (Read-Host 'Ruta del keystore historico')).FullName
$gboKeyDestino = 'C:\GBO\.private-signing\historico.jks'
$gboConfigs = @('server.local.properties','drive.local.properties','keystore.properties')
foreach ($gboNombre in $gboConfigs) {
    if (!(Test-Path -LiteralPath (Join-Path $gboOrigen $gboNombre) -PathType Leaf)) {
        throw "Falta archivo origen: $gboNombre"
    }
    if (Test-Path -LiteralPath (Join-Path 'C:\GBO' $gboNombre)) {
        throw "El destino ya existe: $gboNombre. Conservar ambas copias y conciliar fuera de este procedimiento."
    }
    git check-ignore -q -- $gboNombre
    if ($LASTEXITCODE -ne 0) { throw 'Destino privado sin exclusion Git' }
}
if (!(Test-Path -LiteralPath $gboKeyOrigen -PathType Leaf)) { throw 'Keystore origen no valido' }
if (Test-Path -LiteralPath $gboKeyDestino) { throw 'Keystore destino ya existe; detener' }
git check-ignore -q -- .private-signing/historico.jks
if ($LASTEXITCODE -ne 0) { throw 'Keystore sin exclusion Git' }
New-Item -ItemType Directory -Path 'C:\GBO\.private-signing' -Force | Out-Null
foreach ($gboNombre in $gboConfigs) {
    $gboDesde = Join-Path $gboOrigen $gboNombre
    $gboHasta = Join-Path 'C:\GBO' $gboNombre
    Copy-Item -LiteralPath $gboDesde -Destination $gboHasta -ErrorAction Stop
    if ((Get-FileHash -LiteralPath $gboDesde).Hash -ne (Get-FileHash -LiteralPath $gboHasta).Hash) {
        throw 'Copia privada no coincide; detener y conservar evidencia'
    }
}
Copy-Item -LiteralPath $gboKeyOrigen -Destination $gboKeyDestino -ErrorAction Stop
if ((Get-FileHash -LiteralPath $gboKeyOrigen).Hash -ne (Get-FileHash -LiteralPath $gboKeyDestino).Hash) {
    throw 'Keystore copiado no coincide; detener'
}
```

No ejecutar la copia simultáneamente con otra restauración; las comprobaciones previas evitan reemplazos ordinarios. Si aparece un destino existente o un error parcial, detenerse, conservarlo y elegir una restauración nueva; no borrar para forzar una repetición.

Restringir por NTFS el acceso a estos cuatro archivos al propietario y SYSTEM mediante Propiedades → Seguridad → Opciones avanzadas. Las ACL no sustituyen el cifrado durante traslado. En un editor local, adaptar **solo** GBO_RELEASE_STORE_FILE en keystore.properties a `.private-signing/historico.jks`; no mostrar el contenido en terminal ni editar endpoints/flags/alias/contraseñas. Guardar la evidencia de copia anterior separada; esa adaptación de ruta cambiará intencionadamente el hash de keystore.properties, no el del keystore. No abrir los privados con herramientas que los envíen a terceros.

### Terminal y verificación

```powershell
Set-Location C:\GBO
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
if (!(Test-Path -LiteralPath "$env:JAVA_HOME\bin\java.exe")) { throw 'JBR no encontrado' }
if (!(Test-Path -LiteralPath $env:ANDROID_HOME)) { throw 'SDK no encontrado' }
.\gradlew.bat --no-configuration-cache :app:validateReleaseConfiguration
git status --short
```

No se necesita definir contraseñas como variables de entorno si se recupera keystore.properties. El build también acepta GBO_RELEASE_* desde variables o propiedades Gradle, que prevalecen sobre el archivo: si aparecen después, revisar su procedencia privadamente, sin imprimirlas. No copiar contraseñas al historial de comandos. No activar gboValidationBuild ni otros bypass.

validateReleaseConfiguration abre el keystore y compara internamente el certificado con el SHA-256 histórico público `0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315`. Su PASS es necesario para acreditar identidad/configuración local; no prueba conexión real con Google. Si falta la clave, no llega a esa comparación. Ante discrepancia, detenerse; no cambiar el certificado esperado ni usar otra firma. No es necesario imprimir/exportar el certificado para compararlo.

## 4. Pruebas ejecutadas y pendientes

Validación P0.1 ejecutada offline sobre los mismos archivos preservados, con JBR asignado al proceso, un worker, heap 2048 MiB, ejecutor de migración derivado solo para aislar evidencias y ruta temporal corta:

| Comprobación actual | Resultado |
|---|---|
| DriveDeliveryPresentationTest | 11/11 PASS |
| DriveDeliveryCardUiTest | 4/4 PASS |
| ScapHttpAuthorizationP0Test | 2/2 PASS |
| Total focalizado | 17/17 PASS; 0 errores, 0 omitidas; Gradle 29 s |
| validateReleaseConfiguration | FAIL, RELEASE_BLOCKED_MISSING_SIGNING_KEY; Gradle 5 s |
| Comparación del certificado histórico | NO EJECUTADA: guarda de firma detiene la tarea |

XML actuales en `tmp/p01-20261030/xml/`; resultados.json, pruebas.log y release.log en la misma carpeta. Los XML/log del P0 anterior se conservaron en tmp/p0-scap-20261030. No se recompiló APK ni se ejecutó Release en P0.1. El antecedente P0 es 88 casos distintos, 84 aprobados y 4 fallidos; no se presenta como nueva ejecución ni se suman sus repeticiones a estos 17.

Los cuatro casos bloqueados **no se reejecutaron con un perfil inventado ni se marcaron PASS**:

- PavementAnnotationSync147Test.rigidAnnotationsAndPhotoSurviveOfflineBusyLostAckAndAuthorizedUpdate.
- ScapFinalP0Test.actualPreparationFailureIsActionableAndDoesNotLoseTheSnapshot.
- SyncRecovery10DTest.scapV20BlockRecoversWithFreshV21CapabilitiesAndLostAckNeverRestagesSixteenFiles.
- HotfixPerformance146Test.load.

Cuando exista configuración legítima: ejecutar primero la guarda de Release; luego las cuatro clases completas, sin excluir métodos ni alterar BuildConfig. Crear una carpeta NUEVA de evidencias y un init temporal derivado de tools/migracion/pruebas.init.gradle con gbo.p0.evidence/gbo.large.evidence dirigidos allí, como en tmp/p01-20261030/p01.init.gradle; usar una ruta java.io.tmpdir corta y existente. Conservar el XML anterior antes de otra ejecución de la misma tarea Test.

Comando base para ese perfil recuperado (cambiar únicamente el init/ruta temporal por directorios nuevos ya creados):

```powershell
.\gradlew.bat --offline --no-configuration-cache -I tmp/p01-20261030/p01.init.gradle `
  '-Dgbo.migration.temp=C:\GBO\tmp\t1' :app:testDebugUnitTest `
  --tests com.tuempresa.inventariovial.PavementAnnotationSync147Test `
  --tests com.tuempresa.inventariovial.ScapFinalP0Test `
  --tests com.tuempresa.inventariovial.SyncRecovery10DTest `
  --tests com.tuempresa.inventariovial.HotfixPerformance146Test
```

No ejecutar exactamente esas rutas si ya contienen resultados: preparar otras nuevas y actualizar ambos argumentos. Estos ensayos usan datos ficticios y transportes controlados; no habilitar pruebas instrumentadas de integración real. Después, repetir AutomaticSyncRegressionTest, DriveDeliveryPresentationTest, DriveDeliveryCardUiTest, DriveDeliveryListsTest, DriveDeliveryListsUiTest, ReadablePhotoNames10Test y PhotoTransport10Test. Un PASS exige recorrer reserva V2, BUSY/pérdida de respuesta, stages, commit y ACK actuales donde cada test lo requiere; una salida temprana por configuración no acredita esas condiciones.

## 5. Ficha comparativa de tablets preparada

**No se accedió a ninguna tablet.** Completar tras autorización, sin pulsar sincronizar, cerrar/editar fichas, cambiar permisos, red, batería, cuenta o destino para obtener la primera instantánea. Usar códigos de caso internos en vez de nombres de puentes o UUID completos en el informe compartido.

| Campo | Flavio | Julián | Recogida de solo lectura / criterio |
|---|---|---|---|
| Fecha/hora y zona de captura | No recogido | No recogido | Misma ventana; anotar hora del dispositivo sin corregirla |
| Versión Android de GBO y versionCode | No recogido | No recogido | Información de app / versión visible; no asumir 1.4.18 (32) por las fuentes ASUS |
| Procedencia y tipo de build | No recogido | No recogido | Registro de instalación autorizado; no extraer APK automáticamente |
| Sistema operativo / restricciones de batería | No recogido | No recogido | Ajustes de solo lectura; no desactivar restricciones |
| Sesión y rol | No recogido | No recogido | Activa/expirada y rol; sin nombres, PIN, contraseña ni tokens |
| Permisos pertinentes | No recogido | No recogido | Concedido/denegado de cámara, ubicación y acceso documental según Android; sin cambiarlos |
| Red y conexión | No recogido | No recogido | Offline/Wi-Fi/móvil, acceso aparente; sin SSID, IP ni identificadores |
| Destino de la ficha | No recogido | No recogido | Drive/Solo dispositivo; no autorizar envío para medir |
| Última entrega confirmada | No recogido | No recogido | Hora y revisión de ACK íntegro; distinguir fecha del intento |
| Entregas pendientes | No recogido | No recogido | Registrar pantalla/ámbito/filtro; no comparar conteo de archivos con fichas |
| Cola y trabajador | No recogido | No recogido | DRAFT/QUEUED/UPLOADING/ERROR/BLOCKED/PENDING_VALIDATION/SYNCED y activo/en espera |
| Revisión local / preparada / confirmada | No recogido | No recogido | Anotar los tres números y si coinciden; no exportar payload completo |
| Motivo de bloqueo | No recogido | No recogido | Código y mensaje saneado; ocultar nombres, rutas, URL y IDs |
| V2 cliente | No recogido | No recogido | Procedencia del build/diagnóstico permitido; no inferirlo de la versión ni activar flag |
| Capacidades V21/V2 observadas | No recogido | No recogido | Última evidencia disponible y su hora; si no existe, Desconocido, sin hacer nueva petición |
| ACK de revisión actual | No recogido | No recogido | Sí/No/No verificable según validación íntegra; staging no equivale a ACK |
| SCAP / fotos / croquis / videos | No recogido | No recogido | Cantidad preparada/procesada/confirmada por revisión; videos por separado; sin copiar medios |
| SIC asociados | No recogido | No recogido | Formatos en pendingSic, sicRows, sicFiles y retenidos si la UI los expone; no inventar ausentes |
| Tiempos agregados | No recogido | No recogido | Preparación/envío/espera/ACK; HTTP mezcla red/servidor, sin atribuir causa remota |

Recogida propuesta: lectura presencial guiada de pantallas y diagnóstico agregado existente. Si se autorizan capturas, recortar/sanear localmente UUID, rutas, nombres, correos, enlaces e imágenes antes de compartirlas. No copiar base Room, SharedPreferences, BuildConfig con endpoints, logs completos, paquetes de soporte sin revisar ni claves. No habilitar ADB, exportaciones, extracción de APK o acceso Google como parte implícita de esta ficha.

Si un dato no es visible sin tocar el estado, marcar No disponible. La comprobación física del destino y sus archivos requiere autorización separada de acceso Google; no se afirma que existe una entrega real por ver solo un contador. El caso de Flavio sigue sin verificación real de ACK y archivos completos.

## 6. Bloqueos de liberación y siguiente acción

Bloquean una Release: firma histórica no restaurada/verificada, configuración legítima de Usuarios/Drive ausente, flags V2 no recuperados y cuatro regresiones integrales pendientes. Además faltan comparación de tablets, validación física y decisión de versión para distribuir cambios nuevos; no reutilizar automáticamente 1.4.18 (32) como nueva entrega. La política remota de nombres ordinales y la evolución de Google no se corrigen restaurando archivos locales.

Siguiente acción técnica: traslado privado de los cuatro archivos legítimos, adaptación exclusiva de la ruta del keystore y validateReleaseConfiguration. Después ejecutar las regresiones pendientes con evidencia nueva. Preparar la recogida de solo lectura en ambas tablets únicamente cuando el propietario la autorice.

No se modificó Google Drive, Apps Script V21 ni Servicio de Usuarios; no se instalaron APK, hicieron subidas reales, publicaron Release ni enviaron cambios al remoto. Esta tarea consolida y prepara: no acredita capacidad completa de Release todavía ni resolución del incidente de campo.
