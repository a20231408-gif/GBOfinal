# RELEASE_BLOCKED_MISSING_SIGNING_KEY

La base de nuevos SIC se conserva: Room 15, migraciones históricas, cinco SIC nuevos, 207 imágenes y módulos existentes. Versionado preparado: applicationId `com.tuempresa.inventariovial`, máximo conocido 10, nuevo versionCode 11, versionName `1.4.0-sic-production`.

Los dos APK adjuntos se verificaron con el mismo certificado público GBO:

`0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315`

**Falta la keystore histórica con la clave privada correspondiente y su configuración local autorizada**: archivo, contraseña del almacén, alias y contraseña de la clave. No se encontraron claves release en Downloads/proyectos GBO ni en las ubicaciones accesibles revisadas del perfil y disco D; algunas carpetas del sistema denegaron lectura. Solo se localizaron debug.keystore y claves de caché de Gradle, que no se usan para producción. No hay variables GBO_RELEASE_* configuradas.

También falta `server.local.properties` en la base actual. Existe `drive.local.properties`; su contenido se mantiene privado. La producción debe utilizar configuración funcional autorizada; no se fabrica ni se extrae un token de APK históricos para suplirla.

`gradlew assembleRelease --no-configuration-cache --rerun-tasks` se ejecutó sin gboValidationBuild y falla explícitamente en validateReleaseConfiguration con RELEASE_BLOCKED_MISSING_SIGNING_KEY. No se produjo ni se entrega una supuesta GBO_InventarioVial_PRODUCTION_RELEASE.apk. No se generó keystore nueva ni se usó fallback debug.

Para desbloquear: proporcionar/configurar localmente la keystore original que coincida con ese certificado y el server.local.properties autorizado. Si las tablets instaladas usan otra firma, primero identificar su APK/certificado exacto y resolver la continuidad de esa identidad. No enviar passwords al chat ni incluirlos en el ZIP.

Estado: **VERSIONADO CORREGIDO. CERTIFICADO HISTÓRICO IDENTIFICADO. FIRMA DE LA NUEVA RELEASE BLOQUEADA POR CLAVE AUSENTE. ACTUALIZACIÓN REAL PENDIENTE.**
