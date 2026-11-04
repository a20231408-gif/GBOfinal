# Autorización operativa pendiente — 04/11/2026

Checkpoint previo 920b75b, rama codex/scap-pavimentos-cierre-20261103, PR #11. Android fuera de este hito.

## Identidad: resultado actual

La conexión Drive encontró una carpeta llamada GBO Inventario Vial bajo 13. PUCP_SOFTWARE. Sus metadatos devuelven access_not_verified, sin owners ni permissions; canShare=false corresponde al usuario conectado y no acredita permisos de GBO. No se identificó el proyecto original por búsqueda de Apps Script. El navegador integrado mostró Iniciar sesión. Propiedad de carpeta/ancestros, propietario del proyecto, ID/deployer/ejecutor V21 y permisos efectivos de GBO: NO VERIFICADOS actualmente. No hay base para afirmar que la carpeta es el destino institucional nuevo, la raíz histórica o propiedad personal. Comparar IDs con el anexo privado en sesión institucional antes de cualquier escritura. La captura histórica no sustituye esta lectura.

## Código exacto y autorización del respaldo

Revisar tools/lectura-propuesta/PrivatePropertiesBackup.PROPOSED.gs. No está instalado ni ejecutado. No forma parte del candidato V22. Método: getProperties una sola vez, canonización compatible con properties-backup-verify.cjs, UTF-8, conteo, SHA-256, scriptId y UTC; archivo JSON completo nuevo en Mi unidad institucional, sin conceder permisos, enlace público ni logs; comprobación de propietario/permisos y readback completo antes de devolver solo metadata. Límite deliberado 8 MiB de JSON UTF-8; exceso aborta antes de crear archivo. No se supone que sea un límite de Google.

La autorización debe nombrar proyecto original/scriptId, cuenta institucional, instalación del archivo exacto en editor, dos ejecuciones separadas, lectura completa de ScriptProperties y creación de dos archivos privados nuevos en Mi unidad. Requiere servicios PropertiesService, ScriptApp, Utilities y Drive avanzado ya habilitado, además de acceso Drive de escritura para los respaldos y lectura para verificación; revisar scopes existentes antes de consentir cualquier expansión. No habilitar servicios, cambiar manifiesto ni OAuth sin aprobación específica. Esta opción SÍ escribe archivos nuevos de respaldo; si solo se autoriza lectura estricta sin nuevos archivos, no ejecutarla y seleccionar otro transporte autorizado.

Antes de ejecutar: confirmar Mi unidad de GBO y su política institucional; no usar unidad compartida ni carpeta histórica. Hay una ventana entre crear y comprobar permisos: la autorización debe aceptar el almacenamiento inicial bajo las reglas privadas de Mi unidad; si la organización no puede garantizarlo, no ejecutar. Ante error de permisos después de crear, conservar evidencia privada, detenerse y revisar el archivo; no cambiar permisos ni borrar automáticamente. Ante respuesta perdida, revisar archivos por hora/metadata antes de reintentar para evitar respaldos no contabilizados.

Retorno privado: el propietario abre Drive con GBO, localiza cada archivo por ID devuelto, descarga a ASUS fuera de Git y comprueba tamaño exacto y SHA-256 de bytes contra metadata. No pegar contenidos ni IDs en respuestas públicas. Ejecutar properties-backup-verify.cjs A.json B.json; un PASS no acredita pausa ni procedencia por sí solo. Custodia privada y cifrado conforme CIFRADO_Y_TRASLADO.md. No se requiere crear proyecto ni deployment ni modificar propiedades, ROOT o históricos.

## Lecturas de inventario por autorizar

Autorizar expresamente 32 llamadas: dos ciclos completos de gboPilotInventoryReadonly00 a 15 ya presentes en el editor original, bajo pausa acreditada. Solo metadata y blobs del registro, sin escritores, recuperación ni reintentos. Guardar BEGIN/ROW/REFS/END íntegros en privado, conciliador local y comparación por IDs/UUID/revisión/contentHash/ACK/referencias entre ciclos y contra histórico. Revisar altas, bajas, cambios, duplicados, ilegibles, PENDING y los 13 bindings históricos sin adoptar. No imponer 207/207/220 al inventario nuevo. Sin autorización específica de ejecución, inventario actualizado NO EJECUTADO.

Un doPost terminado puede ser rechazado o perder respuesta. Confirmación exige correlación de key/revisión con journal, record, ACK y archivos/hash; el estado de ejecución solo no demuestra escritura. Antes/durante/después revisar ejecuciones activas y estabilidad de firmas de ambas lecturas. Dos lecturas iguales no demuestran pausa de clientes desconocidos.

## Matriz operativa para completar en privado

| Cliente | Estado acreditado actual | Responsable | Evidencia y UTC de pausa | Última llamada/cola | Liberación |
|---|---|---|---|---|---|
| OPPO | Histórico offline, dos QUEUED; no revalidado hoy | Pendiente | Pendiente | Preservar | No autorizar aquí |
| Xiaomi | Excluida; estado actual no comprobado | Pendiente | Pendiente | Desconocido | No autorizar aquí |
| Tablets de campo (una fila por instalación) | Desconocido | Pendiente | Pendiente | Desconocido | Pendiente |
| Integraciones/otros clientes del endpoint | Desconocido | Pendiente | Pendiente | Desconocido | Pendiente |

Secuencia: identificar propietario/ejecutor/destinos -> inventariar todos los clientes y acreditar pausa -> autorizar respaldo privado -> capturas completas y lectura doble estable -> conciliar diferencias -> aprobación independiente B/DRAIN_ONLY. Conservar V21 mientras falte cualquier puerta. OPEN exige otra autorización; no usar OPPO ni Xiaomi en ensayo. Propiedad ajena a GBO: detener plan de escritura y conciliar con custodio, sin transferencias automáticas.

## Continuar con compañero B

Recuperar último SHA de origin/codex/scap-pavimentos-cierre-20261103; leer este archivo, PREFLIGHT_INSTITUCIONAL_PENDIENTE_20261104.md y expediente de corte. Acceso faltante: sesión institucional legítima y anexo privado con IDs exactos; no secretos por chat. Primero verificar identidad/propiedad. Presentar y obtener autorización específica para el código propuesto y las 32 lecturas. No ejecutar el código por estar publicado en Git. No tocar Android ni repetir 73/76 suites. Publicar solo evidencia saneada. V21 sigue sin cambios por esta intervención.