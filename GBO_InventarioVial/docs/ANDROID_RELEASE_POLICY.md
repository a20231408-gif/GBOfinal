# Política de releases Android GBO

1. Mantener un único applicationId: `com.tuempresa.inventariovial`.
2. Usar la keystore histórica cuyo certificado coincide con la huella fijada en RELEASE_SIGNING.md. Controlar el certificado; el nombre del archivo no identifica una clave.
3. No cambiar la firma, generar otra clave ni utilizar debug como reemplazo de la identidad de producción.
4. Nunca reducir ni reutilizar versionCode. La iteración de paridad UI prepara versionCode 13, superior al debug conocido 12. La identidad y los controles de firma se conservan.
5. Incrementar versionCode en cada APK distribuida y registrar el máximo de todas las referencias verificadas. versionName sirve para trazabilidad, no reemplaza versionCode.
6. Ejecutar verifyReleaseIdentity antes de entregar: package, versión, certificado, firma verificada, alineación y APK completo.
7. Registrar tamaño y SHA-256 de cada APK distribuida, con fecha, destinatarios/grupos conocidos y resultado de prueba de actualización.
8. Conservar copia controlada de cada release y respaldo seguro de su keystore/credenciales, fuera del source ZIP.
9. Nunca distribuir debug APK como versión de campo. La build con gboValidationBuild omite configuración funcional y solo sirve para pruebas.
10. No incluir passwords, tokens, PIN, configuración privada, keystore ni claves en Git, logs o entregables fuente.

Para cada release ejecutar tests de migración y suite completa comparada con las identidades baseline; actualizar sin desinstalar en un entorno de prueba conservando Room, fotos, borradores, configuración y relaciones. Si aparecen varias firmas históricas, registrar qué instalación usa cada certificado antes de planificar su continuidad. No ocultar el conflicto ni recomendar desinstalar como solución normal.

La producción falla cerrada si falta configuración o la identidad no coincide. No declarar instalación resuelta hasta verificar al menos una actualización in-place real con datos conservados.
