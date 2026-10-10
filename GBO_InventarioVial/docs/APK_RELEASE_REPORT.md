# APK_RELEASE_REPORT

## Estado

**VERSIONADO CORREGIDO. FIRMA DE LA NUEVA RELEASE BLOQUEADA. ACTUALIZACIÓN REAL NOT TESTED.** No se declara resuelto el problema de instalación y no se entrega una supuesta APK de producción.

Base exclusiva: `GBO_InventarioVial_SIC_08_09_13_18A_34_SOURCE.zip`, correspondiente a SIC_NEW_FORMATS_REPORT.md. Room 15, MIGRATION_14_15 y todas las migraciones/SIC/módulos se conservan. Se verificaron hashes idénticos de los 576 archivos protegidos app/src, schemas, backend y Apps Script.

## Identidad preparada

| Campo | Valor |
|---|---|
| previousMaximumVersionCode | 10 |
| newVersionCode | 11 |
| versionName | 1.4.0-sic-production |
| applicationId | com.tuempresa.inventariovial |
| Room | 15 |
| minSdk declarado | 26 |
| targetSdk declarado | 37 |
| certificado público esperado SHA-256 | 0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315 |
| nueva APK de producción | NO GENERADA |
| SHA-256/tamaño/certificado de nueva release | NO DISPONIBLES |

VERSIONCODE10 se comparó byte por byte con su ZIP integrado predecesor: únicamente app/build.gradle.kts difiere, con versionCode 8→10. No se copiaron sus fuentes sobre SIC. El máximo entre APK históricos encontrados es 8; el reporte de corrección menciona 9 y la fuente VERSIONCODE10 verifica 10. La nueva iteración usa 11, superior al máximo conocido previo.

## Auditoría de firma y bloqueo

68 APK examinados con herramientas Android SDK oficiales; 46 tienen el applicationId principal. FIELD_SIGNATURE_A es GBO corporativa, compartida por los dos APK adjuntos y doce APK principales. FIELD_SIGNATURE_B es Android Debug, presente en 34 APK principales, incluido el debug SIC anterior. Los grupos de dispositivos concretos permanecen UNKNOWN.

SIGNATURE_MISMATCH_CONFIRMED entre el APK SIC debug previo y los APK históricos GBO adjuntos. No se atribuye sin evidencia un certificado a un fabricante/tablet particular. No se combinan firmas, no se genera clave nueva ni se deshabilita seguridad del dispositivo. Se fija la identidad pública GBO, derivada del certificado de APK conocidos.

No se encontró la keystore privada GBO ni variables GBO_RELEASE_* configuradas; tampoco server.local.properties. Se solicitó al usuario la ubicación/configuración histórica, sin solicitar passwords por chat. drive.local.properties existente se mantiene privado. La nueva release debe utilizar configuración autorizada local, sin gboValidationBuild; aquí su configuración funcional completa no puede verificarse.

`assembleRelease --no-configuration-cache --rerun-tasks` se ejecutó sin gboValidationBuild: BUILD FAILED en validateReleaseConfiguration por RELEASE_BLOCKED_MISSING_SIGNING_KEY. Es el bloqueo previsto y no un APK unsigned o firmado con debug. RELEASE_BLOCKED_MISSING_SIGNING_KEY.md detalla lo faltante.

## Cambios de esta iteración

- app/build.gradle.kts: versionCode 11, versionName de trazabilidad, signingConfigs solo cuando existe configuración local explícita, validación de clave privada/certificado y configuración funcional, y verifyReleaseIdentity sobre APK completo. Se migraron dos llamadas deprecated assets.srcDir a assets.directories.add manteniendo exactamente las rutas existentes.
- .gitignore: exclusión de configuración local y archivos de firma.
- keystore.properties.example: solo nombres y valores vacíos.
- tools/apk_identity_audit.ps1: diagnóstico/gate con aapt, apksigner, zipalign, package/version/certificado, rechazo de APK split/test/debug y SHA-256. Compatible con Windows PowerShell, sin depender de Get-FileHash.
- tools/verify_production_preservation.py y tools/package_production_source.py: evidencias, preservación y empaquetado seguro.
- Documentación/evidencia de versión, firma, política, actualización pendiente y bloqueo.

No se editaron Kotlin de aplicación, Room/schema/migraciones, UI/SIC, cálculos, SCAP/Puentes/Pavimentos, KMZ, protocolo Drive V2, User Service, autenticación, fingerprint, token o fotos limpias.

## Pruebas

Suite completa: **347 total, 332 passed, 15 failed, 15 baselineFailures, 0 newFailures, 0 skipped**. Coinciden las identidades históricas; las pruebas de migración nuevas y los cinco SIC siguen pasando. La salida Gradle BUILD FAILED de tests corresponde solo a esos 15 fallos autorizados.

Cuatro controles del script de identidad PASS: referencia histórica correcta aceptada; certificado distinto, versionCode distinto y applicationId distinto rechazados. La referencia positiva no es una nueva release y no prueba actualización real.

`assembleDebug --no-configuration-cache -PgboValidationBuild=true`: BUILD SUCCESSFUL. Debug de validación 11 verificado con 207 assets idénticos, package correcto, minSdk 26, APK íntegro y alineado. Este APK es **SOLO VALIDACIÓN**, tiene certificado Android Debug y endpoints/credenciales omitidos. No se distribuye como actualización de campo.

Debug de validación SHA-256: `5638f4f83ef54479ead54faee68be122986ed0ebb642bde91da72af191e39592`; tamaño: 39434221 bytes. No confundir este hash con un APK production release que no existe.

## Actualización y fabricantes

9→nueva: NOT TESTED. 10→nueva: NOT TESTED. Histórico 8→nueva: NOT TESTED. No hay dispositivo conectado ni AVD configurado y falta firma de nueva producción. No se ejecutó adb install -r y no se afirmó Success ni conservación física de datos.

Samsung: se verificaron integridad, apksigner, package, SDK y zipalign de las referencias y del debug nuevo; no hay APK nueva de producción ni prueba en Samsung. No se aplica hack por fabricante.

Huawei/Lenovo: la firma distinta entre APK conocidos está confirmada; la correspondencia con sus instalaciones exactas sigue pendiente. Xiaomi/OPPO: el comportamiento comunicado por el usuario no sustituye una comparación de sus APK instalados. Consultar APK_SIGNATURE_AUDIT.md y APK_IN_PLACE_UPGRADE_TEST.md.

Pendiente: aportar/configurar la clave histórica y server.local.properties autorizados; compilar release y pasar verifyReleaseIdentity; probar al menos una actualización real sin desinstalar, conservando Room, registros, fotos, borradores, archivos, usuarios, configuración y relaciones. No se recomienda desinstalar. No hubo commit ni push.
