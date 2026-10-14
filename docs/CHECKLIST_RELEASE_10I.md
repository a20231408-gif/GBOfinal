# Preparación de distribución 10I

Rama: codex/observaciones-campo-manual-10I. Base válida: 3b3a698 (10H).

| Control | Resultado |
| --- | --- |
| Última Release distribuida | 1.4.2 / 16, confirmado por el propietario |
| Versión preparada en el proyecto | 1.4.3 / 17, reutilización de candidata autorizada; no se incrementa otra vez |
| Identidad de aplicación | com.tuempresa.inventariovial, sin cambios |
| Firma histórica | validateReleaseConfiguration aprobó configuración privada y certificado SHA-256 0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315 |
| Release nueva 10I | No generada: falta ejecución de dos pruebas de diálogos en Android y aceptación física de las correcciones |
| Candidatas anteriores | Seis archivos originales y copias verificados por SHA-256; output/respaldo-10H-1.4.3/respaldo.json. Ningún original sobrescrito |
| Debug ordinaria | assembleDebug aprobado; 1.4.3 / 17, testOnly=false, firmada con clave Debug existente |
| Pruebas instrumentadas | assembleDebugAndroidTest aprobado; compiladas, sin ejecución ni instalación |
| Room | Principal 19 y videos 1; esquemas, migraciones y construcción de bases sin cambios respecto de 10H. No se necesita migración nueva; no se repitió la batería antigua |
| Datos e identidad de tablet | Ninguna operación en tablet en 10I; no se desinstala, borra ni cambia identidad |
| Login existente | Conservado usuario/PIN, ADMIN/OPERADOR y autorización online de cinco minutos |
| Control adicional de dispositivo | ACCESS_CONTROL_ENABLED=false conserva el bypass del control adicional de activación; no elimina el login/PIN ni las comprobaciones de administración. No se activa ni reemplaza |
| Configuración ordinaria | Fotos V2 y videos remotos true; un proyecto app con SIC, SCAP, Pavimentos, fotos y cliente de videos. Servicios privados locales conservados |
| Credenciales en Git | Cero coincidencias de valores privados reales en los textos cambiados 10I, DOCX y PDF; configuraciones, claves y APK no versionados. Alcance limitado, no auditoría de todo el historial |
| Backend | Ningún despliegue. V3/V4, fotos V2 y videoProtocol=1 / SCAP_ONLY anunciados; no se anuncia sicObservationsVersion ni readableVideoNamingVersion |

## Bloqueos concretos antes de distribuir

1. Ejecutar PavementDialogs10ITest.rutFields y slabFields en un Android aislado, o aceptar físicamente esos dos formularios. Robolectric no estabilizó los diálogos; no se cuentan como aprobados. El SDK tiene emulador, pero no hay AVD configurado; no se creó uno ni se usó la tablet para suplirlo.
2. Revisar el manual y aprobar el agregado contractual de la última columna OBSERVACIONES por formato. SIC-18 y los otros formatos principales con campo propio reciben la columna; los complementarios sin ese campo no.
3. Publicar manualmente el parche de CAMBIOS_APPS_SCRIPT_10I.md cuando el propietario lo decida; validar Excel consolidado, edición del mismo UUID, vacío y reintento sin duplicados con registros ficticios autorizados. No afirmar confirmación remota a partir de pruebas locales.
4. Los nombres legibles de videos 10H siguen pendientes de publicación/anuncio. No bloquean la captura offline ni el protocolo SCAP V1 ya existente; sí la aceptación específica de esos nombres. Conservar procedimiento 10H y configuración publicada real.
5. El rechazo real al crear usuarios sigue sin reproducción. Obtener rol efectivo, versión y error exacto sin PIN ni datos personales; el código del servicio de Usuarios no está disponible. El arreglo responsive comprobado no demuestra que haya resuelto ese incidente.

## Compilación e instalación controladas

Abrir D:\Downloads\GBO_PRUEBAS_8B desde Android Studio y sincronizar Gradle con los archivos privados locales existentes. Seleccionar app / debug y Build APK(s). No requiere flags funcionales de pruebas ni un proyecto alternativo. La APK resultante está en app/build/outputs/apk/debug/app-debug.apk.

Esta ejecución no instala nada. Para una prueba decidida por el propietario, comparar primero la firma de la APK con la instalación destino. La Debug tiene SHA-256 d2e54cb086aa731ef046be300977d0b16c0d8f66fdcb08d0d9cfd2038bf0d872; no puede actualizar instalaciones Release con la firma histórica. No usar desinstalación, borrado de datos, descenso forzado de versión ni cambio de clave para sortear un rechazo.

Si la instalación destino es la Debug compatible y el propietario decide actualizarla, puede usar Android Studio con app/debug o ejecutar personalmente:

```powershell
& 'D:\Android\Sdk\platform-tools\adb.exe' -s 523bc083 install -r 'D:\Downloads\GBO_PRUEBAS_8B\app\build\outputs\apk\debug\app-debug.apk'
```

Si Android rechaza la actualización, conservar datos e informar el mensaje; no forzar otra modalidad. No ejecutar pruebas instrumentadas sobre la tablet de campo: en Android Studio seleccionar explícitamente un AVD aislado y ejecutar solo la clase PavementDialogs10ITest. Su compilación ya está comprobada; la ejecución sigue pendiente.

Solo después de cerrar los bloqueos y revisar resultados corresponde compilar la candidata Release con la configuración histórica. No distribuirla ni instalarla automáticamente. La candidata 10H conservada no contiene las correcciones 10I y no debe confundirse con una nueva entrega.
