# Firma de producción GBO

Identidad esperada, derivada de los dos APK históricos adjuntos y verificada con apksigner:

`EXPECTED_RELEASE_CERT_SHA256=0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315`

La clave privada correspondiente no está disponible en las ubicaciones revisadas. El certificado público dentro de un APK permite identificar la firma, pero no sustituye la keystore para firmar nuevas versiones. No se generó clave nueva ni se reutilizó automáticamente debug.keystore.

La configuración autorizada se mantiene fuera de Git y de los ZIP: `keystore.properties` local o variables de entorno/propiedades Gradle `GBO_RELEASE_STORE_FILE`, `GBO_RELEASE_STORE_PASSWORD`, `GBO_RELEASE_KEY_ALIAS`, `GBO_RELEASE_KEY_PASSWORD`. `keystore.properties.example` contiene solo campos vacíos. No escribir contraseñas en comandos compartidos, README, reportes ni logs.

`validateReleaseConfiguration` bloquea release antes de compilar si falta la firma, si se usa gboValidationBuild, si faltan endpoints funcionales, si no se puede abrir la clave privada o si su certificado no coincide. Se valida el certificado, no el nombre de archivo de la keystore. La clave del sistema de caché de Gradle no es una keystore de firma APK y no se utiliza.

Con la keystore histórica y configuración local autorizadas disponibles:

1. Ejecutar `gradlew assembleRelease --no-configuration-cache --rerun-tasks` sin gboValidationBuild.
2. `assembleRelease` finaliza con `verifyReleaseIdentity`, que usa herramientas oficiales Android SDK y exige package, versionCode, certificado esperado, firma v2/v3 válida, APK completo y alineación. También puede ejecutarse `gradlew verifyReleaseIdentity --no-configuration-cache`.
3. La entrega se copia solamente después de pasar ese control y de comprobar actualización real conservando datos. Un APK heredado en app/release o app/build no constituye una release nueva.

El script `tools/apk_identity_audit.ps1` admite uno o más archivos, descubre Build Tools del SDK y muestra solo información pública de identidad/hashes. Admite `-JsonOutput`, `-ExpectedPackage`, `-ExpectedVersionCode`, `-ExpectedCertificateSha256` y `-RequireProduction`. Se comprobó en Windows PowerShell el paso de una referencia histórica válida y el rechazo de certificado, versión y package incorrectos.

No se cambia el applicationId, fingerprint, ANDROID_ID, token, User Service ni autenticación. [La documentación de firma Android](https://developer.android.com/studio/publish/app-signing) explica la continuidad de firma requerida para actualizaciones.
