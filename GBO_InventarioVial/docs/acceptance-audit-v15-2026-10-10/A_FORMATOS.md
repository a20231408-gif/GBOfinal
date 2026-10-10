# A. Inventario automático de formatos

24 formatos/flujos: 12 SIC inventario, dos complementos de puente, dos SCAP canónicos + histórico, tres superficies y cuatro SIC de pavimentos. Enum runtime en scenarios/format-inventory.json; campos/estados completos en A_FORMATOS.json. SIC-15A/16A no están implementados como exportadores. Flags standalone SIC17A/B false no eliminan la proyección SCAP existente.

PASS en Lectura XLSX significa exclusivamente archivo real generado por el exportador Android, abierto con openpyxl, ZIP CRC/XML válidos y al menos una fila sintética. No aprueba formulario, normas, cálculos ni Drive. Las funciones físicas de apertura, campos/catálogos, ruta/tramo/calzada/PR/GPS, guardar/editar/reabrir, fotos y validación tienen NO PROBADO; el JSON las enumera individualmente.

| Formato | Columnas/hojas | Lectura XLSX | Integridad oficial | Carpetas Drive | Subida real | Estado aceptación | Evidencia local |
|---|---|---|---|---|---|---|---|
| SIC-17 | 20 | PASS | NO PROBADO | FAIL | NO PROBADO | FAIL | scenarios/all-sic/SIC-17.xlsx |
| SIC-18 | 13 | PASS | NO PROBADO | FAIL | NO PROBADO | FAIL | scenarios/all-sic/SIC-18.xlsx |
| SIC-19 | 13 | PASS | NO PROBADO | FAIL | NO PROBADO | FAIL | scenarios/all-sic/SIC-19.xlsx |
| SIC-20 | 14 | PASS | NO PROBADO | FAIL | NO PROBADO | FAIL | scenarios/all-sic/SIC-20.xlsx |
| SIC-21 | 12 | PASS | NO PROBADO | FAIL | NO PROBADO | FAIL | scenarios/all-sic/SIC-21.xlsx |
| SIC-22 | 12 | PASS | NO PROBADO | FAIL | NO PROBADO | FAIL | scenarios/all-sic/SIC-22.xlsx |
| SIC-23 | 12 | PASS | NO PROBADO | FAIL | NO PROBADO | FAIL | scenarios/all-sic/SIC-23.xlsx |
| SIC-08 | 10 | PASS | NO PROBADO | FAIL | NO PROBADO | FAIL | scenarios/all-sic/SIC-08.xlsx |
| SIC-09 | 10 | PASS | NO PROBADO | FAIL | NO PROBADO | FAIL | scenarios/all-sic/SIC-09.xlsx |
| SIC-13 | 9 | PASS | NO PROBADO | FAIL | NO PROBADO | FAIL | scenarios/all-sic/SIC-13.xlsx |
| SIC-34 | 11 | PASS | NO PROBADO | FAIL | NO PROBADO | FAIL | scenarios/all-sic/SIC-34.xlsx |
| SIC-18A | 13 | PASS | NO PROBADO | FAIL | NO PROBADO | FAIL | scenarios/all-sic/SIC-18A.xlsx |
| SIC-17A | 16 | NO PROBADO | NO PROBADO | NO PROBADO | NO PROBADO | NO PROBADO | scenarios/format-inventory.json |
| SIC-17B | 18 | NO PROBADO | NO PROBADO | NO PROBADO | NO PROBADO | NO PROBADO | scenarios/format-inventory.json |
| SCAP LOSA | 9 hojas canónicas / histórico distinto | PASS | NO PROBADO | NO PROBADO | NO PROBADO | NO PROBADO | scenarios/04-puente-losa/SCAP_LOSA.xlsx |
| SCAP MODULAR | 9 hojas canónicas / histórico distinto | PASS | NO PROBADO | NO PROBADO | NO PROBADO | NO PROBADO | scenarios/05-puente-modular/SCAP_MODULAR.xlsx |
| SCAP HISTORICO | 9 hojas canónicas / histórico distinto | NO PROBADO | NO PROBADO | NO PROBADO | NO PROBADO | NO PROBADO | evidence/full-junit; legacy D4 findings E10/E11 |
| PAV ASPHALT | 13 columnas Fallas + Resumen/Detalle | PASS | NO PROBADO | FAIL | NO PROBADO | FAIL | scenarios/06-asfaltico/PAV_ASF.xlsx; M_001.json; calculation.json |
| PAV RIGID | 13 columnas Fallas + Resumen/Detalle | PASS | NO PROBADO | FAIL | NO PROBADO | FAIL | scenarios/07-rigido/PAV_RIG.xlsx; M_001.json; calculation.json |
| PAV UNSURFACED | 13 columnas Fallas + Resumen/Detalle | PASS | NO PROBADO | FAIL | NO PROBADO | FAIL | scenarios/08-no-pavimentado/PAV_NPV.xlsx; M_001.json; calculation.json |
| SIC-14 | 13 | PASS | NO PROBADO | FAIL | NO PROBADO | FAIL | scenarios/06-asfaltico/SIC-14.xlsx |
| SIC-14A | 13 | PASS | NO PROBADO | FAIL | NO PROBADO | FAIL | scenarios/06-asfaltico/SIC-14A.xlsx |
| SIC-15 | 13 | PASS | NO PROBADO | FAIL | NO PROBADO | FAIL | scenarios/07-rigido/SIC-15.xlsx |
| SIC-16 | 13 | PASS | NO PROBADO | FAIL | NO PROBADO | FAIL | scenarios/08-no-pavimentado/SIC-16.xlsx |

Los incompletos SCAP del fixture son deliberados: no certifican cobertura de todos los campos, apoyos, tipos, porcentajes o límites. Fuente/contradicciones en REQUISITOS_Y_CONTRADICCIONES.md. Repetir protocolo F en cada fila; el estado FAIL de carpetas compara con la solicitud, no simula una subida fallida.
