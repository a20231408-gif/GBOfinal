# Auditoría de firmas APK

Base de desarrollo: nuevos SIC / Room 15. Se ejecutaron aapt, apksigner verify --verbose --print-certs y zipalign sobre cada APK encontrado en los proyectos GBO de Downloads. Los APK antiguos se usan exclusivamente como evidencia; no se copian sus fuentes.

68 APK auditados; 46 corresponden al applicationId principal. Máximo APK observado: 8. VERSIONCODE10 contiene 10; máximo conocido final 10, nueva versión 11.

Los dos APK adjuntos coinciden con FIELD_SIGNATURE_A, certificado corporativo GBO. Los otros certificados corresponden a APK debug históricos; que se hayan distribuido a un grupo concreto de dispositivos no está confirmado.

## FIELD_SIGNATURE_A

SHA-256: `0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315`

Subject: `C=PE, ST=Lima, L=Lima, O=GBO Ingenieros Consultores S.A.C., OU=Desarrollo de Software, CN=GBO Ingenieros Consultores S.A.C.`

Cantidad: 12. Grupo de dispositivos: UNKNOWN.

## FIELD_SIGNATURE_B

SHA-256: `d2e54cb086aa731ef046be300977d0b16c0d8f66fdcb08d0d9cfd2038bf0d872`

Subject: `C=US, O=Android, CN=Android Debug`

Cantidad: 34. Grupo de dispositivos: UNKNOWN.

## Matriz completa

| APK (ruta de referencia) | VersionCode | Certificado | Verified | Zipalign 4 | Grupo de dispositivos |
|---|---:|---|---|---|---|
| `D:\Downloads\GBO Inventario.apk` | 5 | FIELD_SIGNATURE_A | True | True | UNKNOWN |
| `D:\Downloads\GBO Inventario (1).apk` | 8 | FIELD_SIGNATURE_A | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_QA_VALIDACION_FINAL\GBO_InventarioVial\output\GBO_InventarioVial_Ronda2-debug.apk` | 1 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_FINAL_ESTABLE_HUELLA\GBO_InventarioVial_FINAL_ESTABLE\output\GBO_InventarioVial_Ronda2-debug.apk` | 1 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_QA_VALIDACION_FINAL\GBO_InventarioVial\app\build\outputs\apk\debug\app-debug.apk` | 1 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_QA_VALIDACION_FINAL\GBO_InventarioVial\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk` | 0 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_FINAL_INGENIEROS_MAPA\GBO_InventarioVial_FINAL_INGENIEROS_MAPA\app\release\app-release.apk` | 5 | FIELD_SIGNATURE_A | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_FINAL_ESTABLE_HUELLA\GBO_InventarioVial_FINAL_ESTABLE\app\build\outputs\apk\debug\app-debug.apk` | 4 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_FINAL_ESTABLE_HUELLA\GBO_InventarioVial_FINAL_ESTABLE\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk` | 0 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_FINAL_INGENIEROS_MAPA\GBO_InventarioVial_FINAL_INGENIEROS_MAPA\app\build\outputs\apk\release\app-release.apk` | 5 | FIELD_SIGNATURE_A | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_FINAL_INGENIEROS_MAPA\GBO_InventarioVial_FINAL_INGENIEROS_MAPA\app\build\outputs\apk\debug\app-debug.apk` | 5 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_FINAL_INGENIEROS_MAPA\GBO_InventarioVial_FINAL_INGENIEROS_MAPA\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk` | 0 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_DRIVE_BLOCKED_HOTFIX\GBO_DRIVE_BLOCKED_HOTFIX\app\build\outputs\apk\debug\app-debug.apk` | 1 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_FIX\output\GBO_InventarioVial_TABLET_UX_2026-10-07-debug.apk` | 8 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_FIX\output\GBO_InventarioVial_PUENTES_SIC_2026-10-06-debug.apk` | 8 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_DRIVE_BLOCKED_HOTFIX\GBO_DRIVE_BLOCKED_HOTFIX\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk` | 0 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_FIX\app\release\GBO Inventario.apk` | 8 | FIELD_SIGNATURE_A | True | True | UNKNOWN |
| `D:\Downloads\GBO_FIX\app\build\outputs\apk\release\app-release.apk` | 8 | FIELD_SIGNATURE_A | True | True | UNKNOWN |
| `D:\Downloads\GBO_FIX\app\build\outputs\apk\debug\app-debug.apk` | 8 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_FIX\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk` | 0 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_DRIVE_BLOCKED_HOTFIX\GBO_DRIVE_BLOCKED_HOTFIX\app\build\intermediates\apk\debug\app-debug.apk` | 1 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_FINAL_ESTABLE_HUELLA\GBO_InventarioVial_FINAL_ESTABLE\app\build\intermediates\apk\debug\app-debug.apk` | 4 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_FINAL_INGENIEROS_MAPA\GBO_InventarioVial_FINAL_INGENIEROS_MAPA\app\build\intermediates\apk\debug\app-debug.apk` | 5 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_FINAL_ESTABLE_V2\GBO_InventarioVial_FINAL_ESTABLE_V2\output\GBO_InventarioVial_Ronda2-debug.apk` | 1 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_QA_VALIDACION_FINAL\GBO_InventarioVial\app\build\intermediates\apk\debug\app-debug.apk` | 1 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_QA_VALIDACION_CAMPO\GBO_InventarioVial\output\GBO_InventarioVial_Ronda2-debug.apk` | 1 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_FINAL_PAVIMENTOS_DRIVE\GBO_InventarioVial_FINAL_PAVIMENTOS\output\GBO_InventarioVial_Ronda2-debug.apk` | 1 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_QA_VALIDACION_CAMPO\GBO_InventarioVial\app\build\outputs\apk\debug\app-debug.apk` | 1 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_QA_VALIDACION_CAMPO\GBO_InventarioVial\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk` | 0 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_FINAL_SCAP_2019_ORDEN_1_18\GBO_InventarioVial_FINAL_SCAP_2019_ORDEN_1_18\app\release\GBO Inventario version SCAP 2019.apk` | 7 | FIELD_SIGNATURE_A | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_FINAL_SCAP_2019_ORDEN_1_18\GBO_InventarioVial_FINAL_SCAP_2019_ORDEN_1_18\app\build\outputs\apk\release\app-release.apk` | 7 | FIELD_SIGNATURE_A | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_FINAL_SCAP_2019_ORDEN_1_18\GBO_InventarioVial_FINAL_SCAP_2019_ORDEN_1_18\app\build\outputs\apk\debug\app-debug.apk` | 7 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_FINAL_SCAP_2019_ORDEN_1_18\GBO_InventarioVial_FINAL_SCAP_2019_ORDEN_1_18\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk` | 0 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_VIDEO_SHOWCASE\GBO_InventarioVial_VIDEO_SHOWCASE\app\build\outputs\apk\debug\app-debug.apk` | 1 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_VIDEO_SHOWCASE\GBO_InventarioVial_VIDEO_SHOWCASE\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk` | 0 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_FINAL_PAVIMENTOS_DRIVE\GBO_InventarioVial_FINAL_PAVIMENTOS\app\build\outputs\apk\debug\app-debug.apk` | 3 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_FINAL_PAVIMENTOS_DRIVE\GBO_InventarioVial_FINAL_PAVIMENTOS\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk` | 0 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_QA_DATASET_INTEGRAL\GBO_InventarioVial\app\build\intermediates\apk\debug\app-debug.apk` | 1 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_INTEGRADO_FINAL_SOURCE\GBO_InventarioVial\output\sic-new\GBO_InventarioVial_SIC_08_09_13_18A_34_DEBUG.apk` | 8 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_INTEGRADO_FINAL_SOURCE\GBO_InventarioVial\app\release\GBO Inventario.apk` | 8 | FIELD_SIGNATURE_A | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_INTEGRADO_FINAL_SOURCE\GBO_InventarioVial\app\build\outputs\apk\release\app-release.apk` | 8 | FIELD_SIGNATURE_A | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_INTEGRADO_FINAL_SOURCE\GBO_InventarioVial\app\build\outputs\apk\debug\app-debug.apk` | 8 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_INTEGRADO_FINAL_SOURCE\GBO_InventarioVial\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk` | 0 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_VIDEO_SHOWCASE\GBO_InventarioVial_VIDEO_SHOWCASE\app\build\intermediates\apk\debug\app-debug.apk` | 1 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_QA_VALIDACION_CAMPO\GBO_InventarioVial\app\build\intermediates\apk\debug\app-debug.apk` | 1 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_FINAL_SCAP_2019_ORDEN_1_18\GBO_InventarioVial_FINAL_SCAP_2019_ORDEN_1_18\app\build\intermediates\apk\debug\app-debug.apk` | 7 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_FINAL_PAVIMENTOS_DRIVE\GBO_InventarioVial_FINAL_PAVIMENTOS\app\build\intermediates\apk\debug\app-debug.apk` | 3 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_FINAL_INTEGRADO\GBO_InventarioVial\output\GBO_InventarioVial_Ronda2-debug.apk` | 1 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_QA_DATASET_INTEGRAL_LOGIN_HOTFIX\GBO_InventarioVial_QA_DATASET_INTEGRAL_LOGIN_HOTFIX\app\build\outputs\apk\debug\app-debug.apk` | 1 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_QA_DATASET_INTEGRAL_LOGIN_HOTFIX\GBO_InventarioVial_QA_DATASET_INTEGRAL_LOGIN_HOTFIX\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk` | 0 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_INTEGRADO_FINAL_SOURCE\GBO_InventarioVial\app\build\intermediates\apk\debug\app-debug.apk` | 8 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_FINAL_INTEGRADO\GBO_InventarioVial\app\build\outputs\apk\debug\app-debug.apk` | 1 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_FINAL_INTEGRADO\GBO_InventarioVial\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk` | 0 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_FUSIONADO_FALLAS\GBO_InventarioVial_FUSIONADO_FALLAS\output\GBO_InventarioVial_Ronda2-debug.apk` | 1 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_FUSIONADO_FALLAS\GBO_InventarioVial_FUSIONADO_FALLAS\app\build\outputs\apk\debug\app-debug.apk` | 1 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_FUSIONADO_FALLAS\GBO_InventarioVial_FUSIONADO_FALLAS\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk` | 0 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_QA_DATASET_INTEGRAL_LOGIN_HOTFIX\GBO_InventarioVial_QA_DATASET_INTEGRAL_LOGIN_HOTFIX\app\build\intermediates\apk\debug\app-debug.apk` | 1 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_FINAL_INTEGRADO\GBO_InventarioVial\app\build\intermediates\apk\debug\app-debug.apk` | 1 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_FUSIONADO_FALLAS\GBO_InventarioVial_FUSIONADO_FALLAS\app\build\intermediates\apk\debug\app-debug.apk` | 1 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_FINAL_INGENIEROS_MAPA_HOTFIX_V3_COMPILE_MAPA_2026-10-05\GBO_InventarioVial_FINAL_INGENIEROS_MAPA_HOTFIX_V2_WORK\output\GBO_InventarioVial_TABLET_UX_2026-10-07-debug.apk` | 8 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_FINAL_INGENIEROS_MAPA_HOTFIX_V3_COMPILE_MAPA_2026-10-05\GBO_InventarioVial_FINAL_INGENIEROS_MAPA_HOTFIX_V2_WORK\output\GBO_InventarioVial_PUENTES_SIC_2026-10-06-debug.apk` | 8 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_FINAL_INGENIEROS_MAPA_HOTFIX_V3_COMPILE_MAPA_2026-10-05\GBO_InventarioVial_FINAL_INGENIEROS_MAPA_HOTFIX_V2_WORK\app\release\GBO Inventario_antes_de_Integracion.apk` | 8 | FIELD_SIGNATURE_A | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_FINAL_INGENIEROS_MAPA_HOTFIX_V3_COMPILE_MAPA_2026-10-05\GBO_InventarioVial_FINAL_INGENIEROS_MAPA_HOTFIX_V2_WORK\app\build\outputs\apk\release\app-release.apk` | 8 | FIELD_SIGNATURE_A | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_FINAL_INGENIEROS_MAPA_HOTFIX_V3_COMPILE_MAPA_2026-10-05\GBO_InventarioVial_FINAL_INGENIEROS_MAPA_HOTFIX_V2_WORK\app\build\outputs\apk\debug\app-debug.apk` | 8 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_FINAL_INGENIEROS_MAPA_HOTFIX_V3_COMPILE_MAPA_2026-10-05\GBO_InventarioVial_FINAL_INGENIEROS_MAPA_HOTFIX_V2_WORK\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk` | 0 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_FINAL_INGENIEROS_MAPA_HOTFIX_ORDEN_SENALES_SOURCE_2026-10-05\GBO_InventarioVial_FINAL_INGENIEROS_MAPA_HOTFIX\app\build\outputs\apk\debug\app-debug.apk` | 6 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_FINAL_INGENIEROS_MAPA_HOTFIX_ORDEN_SENALES_SOURCE_2026-10-05\GBO_InventarioVial_FINAL_INGENIEROS_MAPA_HOTFIX\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk` | 0 | FIELD_SIGNATURE_B | True | True | UNKNOWN |
| `D:\Downloads\GBO_InventarioVial_FINAL_INGENIEROS_MAPA_HOTFIX_ORDEN_SENALES_SOURCE_2026-10-05\GBO_InventarioVial_FINAL_INGENIEROS_MAPA_HOTFIX\app\build\intermediates\apk\debug\app-debug.apk` | 6 | FIELD_SIGNATURE_B | True | True | UNKNOWN |

Todos los valores completos de package, versionName, SHA-256 y SHA-1 del certificado, esquemas de firma, minSdk, SHA-256 del APK y alineación están en `final-evidence/apk-signature-audit.json`. Los APK test se identifican y no cuentan para el máximo del applicationId principal.

## Conclusiones de compatibilidad

La APK SIC debug entregada antes usa certificado Android Debug distinto del certificado GBO de los dos adjuntos: SIGNATURE_MISMATCH_CONFIRMED entre esos archivos. No se puede atribuir aún un certificado a Huawei/Lenovo/Xiaomi/OPPO/Samsung sin el APK exacto instalado o diagnóstico de cada tablet. Se conserva certificado GBO como identidad esperada de producción; no se encontró su clave privada.

Android exige la misma identidad de firma o una prueba de rotación válida para actualizar. Aumentar versionCode no resuelve firmas distintas. No se combina ni sustituye la firma; no se genera keystore nueva. Fuentes: [App signing](https://developer.android.com/studio/publish/app-signing), [App signing AOSP](https://source.android.com/docs/security/features/apksigning).
