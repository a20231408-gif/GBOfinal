# Prueba de actualización sin desinstalar

**NOT TESTED**: no hay APK nueva de producción firmada con la clave GBO. `adb devices -l` no encontró dispositivos y el emulador no tiene AVD configurado. No se instaló ni modificó ninguna aplicación de una tablet de campo. El bloqueo de clave impide efectuar una prueba válida nueva aunque se preparara un emulador.

| Origen | Evidencia disponible | Actualización a producción 11 | Datos conservados |
|---|---|---|---|
| APK histórico 9 | El reporte VERSIONCODE10 lo menciona; no se encontró APK con 9 en la auditoría | NOT TESTED | NOT TESTED |
| APK histórico 10 | Source VERSIONCODE10 disponible; no APK con 10 en la auditoría | NOT TESTED | NOT TESTED |
| GBO Inventario (1).apk, 8 | Certificado GBO verificado | NOT TESTED | NOT TESTED |
| APK antes de integración, 8 | Certificado GBO verificado | NOT TESTED | NOT TESTED |

Se repiten las pruebas automatizadas Room 14→15 y 12→13→14→15 y los cinco formatos nuevos, pero no equivalen a adb install -r ni demuestran conservación de datos en una actualización real.

Procedimiento pendiente en un entorno de prueba con la misma firma histórica:

1. Instalar el APK histórico y abrirlo.
2. Crear registro, foto real de prueba, borrador, configuración y datos de usuario. Registrar valores y hashes de archivos; conservar un respaldo de prueba.
3. Cerrar la app. Instalar la nueva release con `adb install -r NUEVA.apk`, sin desinstalar y sin borrar datos. Registrar Success o error exacto.
4. Abrirla y verificar Room 15, registros, historial, fecha, fotografías accesibles con los mismos hashes, configuración, usuario y huella/autorización existentes.
5. Crear/guardar/reabrir/exportar SIC-08/09/13/34 y SIC-18A ligado al padre; comprobar relaciones, borradores y fotos.
6. Repetir con APK 9 y 10 si llegan a estar disponibles. Registrar modelo/Android y certificado realmente instalado por cada grupo de tablets.

Si hay firma distinta, detener la actualización y registrar SIGNATURE_MISMATCH_CONFIRMED para esos archivos/dispositivos comprobados. No hacer auto-rebind, no deshabilitar DEVICE_REVOKED y no sacrificar datos locales.
