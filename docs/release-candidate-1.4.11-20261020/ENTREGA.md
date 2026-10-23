# Release candidata GBO Ruta Clara 1.4.11 (25) — 20/10/2026

**Resultado: BUILD SUCCESSFUL. Candidata para prueba física; todavía no aprobada para campo.**

- APK: `D:\Downloads\GBO_PRUEBAS_8B\output\GBO_RUTA_CLARA_CANDIDATA_1.4.11_25\GBO_Ruta_Clara_1.4.11_25_CANDIDATA.apk`
- applicationId: `com.tuempresa.inventariovial`; versionName: **1.4.11**; versionCode: **25**.
- Firma válida v2/v3, certificado histórico SHA-256 `0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315`. Coincide con las APK Release locales 1.4.9 (23) y 1.4.10 (24). No se verificó la aplicación instalada en cada tablet.
- SHA-256 del APK: `bf665364c8c3b1431454a8076a21fc13f0f71c35a3cb1e112fb3f95c95465933`.
- Tamaño: 26388753 bytes; APK completo, sin split, sin testOnly ni debuggable; alineación de 16 KiB verificada. minSdk 26; targetSdk 37.

## Compilación y comprobaciones

Gradle ordinario `:app:assembleRelease`, offline, sin `gboValidationBuild`, con configuración y firma legítimas existentes. Pasaron validateReleaseConfiguration, lintVitalRelease, empaquetado y verifyReleaseIdentity. Duración medida local: 883.24 segundos. Después se verificó el APK copiado en la carpeta de entrega con Android Build Tools y apksigner.

Se conservan las 35 pruebas focalizadas locales aprobadas del hotfix; las 13 fuentes funcionales/de prueba de su manifiesto SHA-256 no cambiaron al compilar. No se repitieron ni se presentan como pruebas de la variante Release en un dispositivo: se ejecutaron antes de subir la numeración, bajo JVM/Robolectric SDK34, Compose y Room/SQLite. La compilación no ejecuta automáticamente pruebas físicas o cargas Google.

En esta etapa solo se modificaron dos valores en app/build.gradle.kts: 1.4.10/24 → 1.4.11/25. Las cinco propiedades privadas mantienen sus hashes y no cambió el código que genera las 15 constantes de configuración ajenas a la versión. La huella adicional de BuildConfig corresponde a una captura de esta misma compilación; no se presenta como comparación independiente de constantes contra una APK anterior. Los once APK anteriores conservan sus hashes. Se respaldaron las fuentes validadas antes de numerarlas y el APK anterior del directorio de compilación en `tmp/release-candidate-1.4.11-20261020/`.

Se comprobó la misma identidad del esquema Room20 dentro del DEX de 1.4.9, 1.4.10 y 1.4.11, y el mismo catálogo road_reference.json. Es una comprobación estática de compatibilidad; no acredita una migración/actualización física ya ejecutada. Firma y applicationId originales conservados. Sin modificaciones de endpoints, tokens, contratos remotos, Apps Script o datos reales. Sin instalación, distribución, commit ni push.

Advertencias no bloqueantes del compilador: acceso reflectivo a enumConstants en InventoryDraftStore y uso obsoleto de TRIM_MEMORY_COMPLETE en StabilityDiagnostics. Sus fuentes funcionales previas están cubiertas por las pruebas indicadas; no se introdujeron cambios adicionales para silenciar advertencias. No hubo errores de compilación ni lint vital.

## Alcance y pendientes

Incluye la corrección de destino de borradores SIC-20, validación de identidad y diagnóstico seguro descritos en `docs/HOTFIX_SIC20_LONGITUD_1_4_11.md`. Protege borradores contradictorios sin reparación automática, cambio de UUID ni retirada de fotografías.

PE-28H/CD sigue requiriendo equivalencias contractuales acreditadas para PR0010/0011. La candidata no inventa esos datos. Persiste el riesgo diagnosticado de interpretar PR FIELD_CAPTURED como referencia oficial; un número calculado con una proyección local no certifica calibración contractual. No cambió el algoritmo ni se reescribieron longitudes históricas. Ver el informe del hotfix antes de validar ese dato para campo.

La prueba física de actualización y de Muro/Túnel/Badén sigue pendiente. El ACK se probó con transporte simulado, sin servidor real. Una prueba real de subida requiere autorización independiente.

## Instalación manual

Solo sobre una instalación Release con la firma histórica compatible: copiar el APK, abrirlo y elegir **Actualizar**, conservando los datos. Ante incompatibilidad de firma o instalación Debug con otro certificado, detenerse; no desinstalar ni borrar datos. No se realizó instalación automática.

Seguir `PRUEBA_FISICA.md`: guardar con foto y observación, reabrir por Inicio/CONTINUAR y Mis registros/detalle/editar, alternar los tres subtipos, girar y cerrar/reabrir. Conservar evidencia privada de UUID y fotos; no compartir credenciales. La distribución a ingenieros queda pendiente de aceptación física.
