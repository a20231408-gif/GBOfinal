# Release candidata GBO Ruta Clara 1.4.10 (24)

**Compilada y firmada. Pendiente de prueba física: no aprobada todavía para campo.**

Fecha: 19/10/2026, America/Lima. Proyecto: `D:\Downloads\GBO_PRUEBAS_8B`.

## Artefacto verificado

- APK: `D:\Downloads\GBO_PRUEBAS_8B\output\GBO_RUTA_CLARA_CANDIDATA_1.4.10_24\GBO_Ruta_Clara_1.4.10_24_CANDIDATA.apk`
- applicationId: `com.tuempresa.inventariovial`.
- versionName: **1.4.10**; versionCode: **24**.
- Variante Release, debuggable=false, testOnly=false, APK completo sin splits.
- SHA-256 APK: `f220e11faa1b0b67aba09a47dc507dc072ca6826f8070da25ad80f37a96270cc`.
- Certificado SHA-256: `0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315`.
- Firma válida, misma que la APK 1.4.9 (23) analizada; esquemas v2/v3: `{'v4': False, 'v3.1': False, 'v2': True, 'v3': True, 'v1': False}`.
- Alineación ZIP y 16 KiB verificadas. Tamaño: 26,355,985 bytes.
- minSdk 26, targetSdk 37; valores de 1.4.9: 26/37.

## Compilación y pruebas

Se ejecutaron tareas Gradle ordinarias `validateReleaseConfiguration`, `testDebugUnitTest` con seis clases focalizadas y `assembleRelease`, incluyendo su verificador final `verifyReleaseIdentity`. No se activó gboValidationBuild ni se sustituyó la configuración real. No se usó configuration cache en estas ejecuciones para evitar persistir sus parámetros privados; se trabajó con dependencias locales en modo offline.

**27 casos distintos aprobados, 0 fallos finales, sin omitir casos.** La primera tanda pasó 26 y la prueba de interfaz falló al abrir su SQLite ficticio con una ruta temporal Windows de 263 caracteres. Se conservó el fallo, se acortó exclusivamente java.io.tmpdir del arnés y se repitió solo ese caso: aprobado. No se alteró la base de datos de la aplicación ni el código funcional para resolverlo.

La Release terminó correctamente y su firma se comprobó otra vez sobre el APK copiado. La identidad del esquema Room 20 se encuentra en ambas APK: **True**. El adaptador SyntheticPdf146 es exclusivo de pruebas y no está en la APK; el artefacto utiliza PdfDocument nativo de Android. Su funcionamiento físico y la impresión siguen pendientes.

Avisos no bloqueantes: opción existente android.disallowKotlinSourceSets experimental; API preexistente TRIM_MEMORY_COMPLETE deprecada; avisos de acceso nativo del JDK durante los tests. No se modificaron módulos ajenos para eliminar esos avisos.

## Conservación y alcance

Se verificaron los hashes de los 16 archivos de la entrega aprobada. El respaldo de fuentes/configuración pública está en `tmp/release-candidate-1.4.10-20261019/FUENTES_ANTES_DE_1_4_10.zip`; el build.gradle anterior y los APK generados previos también están respaldados allí. No se extrajo el ZIP de entrega sobre el proyecto.

La única modificación nueva de fuente/configuración del proyecto para esta compilación es app/build.gradle.kts: versión 23→24 y 1.4.9→1.4.10. Se conservaron todas las correcciones de Pavimentos, esquemas, contratos syncProtocol=3/pathVersion=4, Room y configuración de servicios. Los cinco archivos .properties originales y 15 campos de configuración efectiva Release se contrastaron mediante hashes, sin publicar sus valores. La APK 1.4.9 conserva su SHA-256 `508e68d1b3134d1b850a9dee60f4b35c9e3053e5d3073e4ea79edcce4f9756c6`.

No se instaló en tablets/celulares, no se borraron datos, no se publicó APK/Play, no se modificó Apps Script, GBO LAB ni Servicio de Usuarios y no se hizo commit/push.

## Instalación y prueba del propietario

Consultar `docs/INSTALACION_Y_PRUEBA_1_4_10_24.md`, copiado también junto al APK. Copiar el APK a la tablet y elegir **Actualizar** sobre la Release 1.4.9, con respaldo previo. La coincidencia de firma corresponde a la APK local verificada; no se inspeccionó la instalación concreta. Ante un rechazo/conflicto, detenerse: no desinstalar ni borrar almacenamiento.

Checklist: grieta D horizontal/vertical; parche grande de borde e interior; parche pequeño interior; cuatro esquinas del daño 22; selección/vista previa/deshacer/cancelar/confirmar del 26; guardado/cierre/reapertura; PDF nativo horizontal corto/largo y varias secciones, números/tabla/coordenadas; fotos y Excel conservados. Usar muestras ficticias **Solo dispositivo**, en modo avión, sin envío a Drive.

**Decisión: candidata disponible exclusivamente para validación física del propietario. No declarar aptitud para campo antes de aprobar ese checklist.**
