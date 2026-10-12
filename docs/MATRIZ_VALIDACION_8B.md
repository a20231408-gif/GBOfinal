# Matriz verificable — Prompt 8B

Estados: **APROBADO REAL** contra endpoint/Drive, **APROBADO LOCAL** sin entrega remota, **PENDIENTE FÍSICO** exige interacción/dispositivo, **FALLIDO** intento con causa/evidencia, **BLOQUEADO** exige configuración o credencial ausente. Un rechazo esperado es aprobación de una prueba negativa.

Identidades ficticias, PE-22A / CD / PR 0010:

| Alias | UUID | Distancia | Última revisión confirmada | Destino |
|---|---|---|---|---|
| A, SIC-18 | `6dc3301b-bda9-4432-8f62-b6efe38ef2b1` | 12 m | 1791777214558 | ALCANTARILLAS/ALCANTARILLA 1 |
| B, SIC-18 | `21d8814d-d8af-4be7-a141-33ece3df1008` | 23 m | 1791777130552 | ALCANTARILLAS/ALCANTARILLA 2 |
| C, SIC-08 | `53524c18-69d3-4ede-966c-32da68f11642` | 34 m | 1791777296307 | FAJAS_CALZADA/FAJA 1 |
| D, SIC-08 offline | `e40cebdf-8039-4c6c-969a-0d7db73b8a94` | 45 m | 1791777418102 | FAJAS_CALZADA/FAJA 2 |

Evidencias JSON en `integracion-real-8b/evidencia/`. Identificadores de archivos y carpetas completos en [ARCHIVOS_DRIVE.md](integracion-real-8b/ARCHIVOS_DRIVE.md).

| Caso / entrada | Acción y resultado | Estado | Evidencia / límite |
|---|---|---|---|
| Base 7/8A | Verificar hashes publicados y crear 8B desde 8A | APROBADO LOCAL | Hashes en entrega; ramas protegidas. |
| Conexión real V4 | GET capacidades y POST/ACK de A–D con configuración real | APROBADO REAL | ANDROID_REAL / AUTENTICACION_REAL; no basta GET aislado. |
| Borrador C incompleto | Guardar sin PR final, cero intención de entrega; completar mismo UUID | APROBADO LOCAL | ANDROID_REAL, `draft`; botones de formulario no ejercitados. |
| Creación A/B/C | Repositorios existentes generan cola; drenaje existente confirma revision/syncedRevision | APROBADO REAL | ANDROID_REAL, eventos delivery; datos ficticios. |
| Fotografías A/B | 4 JPEG, 2 categorías, orden 1/2, lectura independiente de cada carpeta | APROBADO REAL | CARPETAS_FOTOGRAFICAS y LECTURA_DRIVE; no cámara física. |
| Foto D | Persistencia hash/UUID, subida GENERAL y lectura posterior | APROBADO REAL | OFFLINE_WORKMANAGER_REAL / LECTURA_OFFLINE_DRIVE. |
| ORIGINAL_CLEAN | 5 archivos con SHA ACK y píxeles originales; originales locales intactos | APROBADO REAL | LECTURA_DRIVE / LECTURA_OFFLINE_DRIVE; EXIF explica cambio de bytes. |
| Familias estables | A/B retienen carpetas 1/2 en edits/retry, categoría aislada, sin carpetas SIB | APROBADO REAL | ANDROID_REAL / ESTRUCTURA_DRIVE; alcance solo ruta/familias ensayadas. |
| XLSX SIC-18 | Abrir ZIP/OOXML real, 13 columnas, 2 filas offsets 12/23, hash ACK | APROBADO REAL | LECTURA_DRIVE; archivo `1tv1viCM-jbB7-L3rp77xevTZ34qalTtn`. |
| Separación SIC-08 | Otro formato y offline generan 2 filas 34/45 en archivo independiente | APROBADO REAL | LECTURA_OFFLINE_DRIVE; `1TqTHGZGdjWhxirRo81Frlk2SgiiIlwzX`; segunda ruta no ensayada. |
| Edición A | 1→2→1 con UUID/carpeta constantes, baseRevision previo, fila actualizada | APROBADO REAL | ANDROID_REAL; lecturas finales A=1/B=2 sin duplicados. |
| Dos almacenes | Room A/B independientes envían al mismo consolidado y preservan fila del otro | APROBADO REAL | ANDROID_REAL; equivalente para concurrencia de datos, mismo token/app, no autenticación multiusuario. |
| ACK perdido B | Error local controlado tras ACK real; replay de misma revision devuelve alreadyExists | APROBADO REAL | `lost-real-ack-retry`; no corte físico de red durante el POST. |
| Conflicto A | Base 1791777079065 frente a remoto 1791777214558 rechazada; local conserva condición 3 | APROBADO REAL | `concurrent-real-conflict`, CONFLICT y respuesta BASE_REVISION_CONFLICT. |
| Persistencia C | Cierre/reapertura real de SQLite antes del drenaje | APROBADO LOCAL | `offline-room-reopen` no desactiva red; caso D sí lo hace. |
| Offline D automático | Desactivar conectividad Android, guardar foto, force-stop, reabrir sin red y habilitarla | APROBADO REAL | OFFLINE_WORKMANAGER_REAL; AutomaticSyncWorker confirma sin drenaje manual, misma revisión. |
| Token ausente/incorrecto | POST Android rechazados, ok=false/No autorizado | APROBADO REAL | AUTENTICACION_REAL; incorrecto aleatorio, no token histórico. |
| Hash corrupto | Staging `{}` con hash cero rechazado HASH_OR_FILE_SIZE | APROBADO REAL | NEGATIVAS_HTTP; key `SIC/fbc976e2-7760-4e5a-8c16-65e701b337b8`, no registro final. |
| Protocolo staging 2 | POST incompatible rechazado INVALID_STAGE | APROBADO REAL | NEGATIVAS_HTTP; no implica haber provocado un ACK incompatible del servidor. |
| Respuesta/ACK incompatibles | Protección Android aprobada localmente en 8A | APROBADO LOCAL | Entrega 8A referenciada, no repetida; servidor real devolvió contrato compatible. |
| SCAP original/exportación/foto | Abrir plantilla Android, 10 hojas; bundle con foto de inspección aislada | APROBADO LOCAL | ScapAndroidExportTest y ScapDriveDeviceTest seleccionados, 2/2; no POST SCAP. |
| Pavimento PDF/XLSX | Render de muestra ficticia y daño, páginas legibles, XLSX | APROBADO LOCAL | PavementReportTest seleccionado, 1/1; sin Drive ni curvas PCI completas. |
| Debug conectado | Compilar con configuración local real e instalar en nuevo emulador | APROBADO REAL | APK_CONECTADO y casos Android; no cambio de firma/versionCode/Room. |
| Debug validación / secretos | Compilar con propiedades vacías y auditar identidad; APK/config ignorados | APROBADO LOCAL | APK_VALIDACION; BuildConfig final vacío para token/endpoints. |
| Fixture SIC inicial (2 ejecuciones) | Doble inserción tipada causó ABORT antes de entrega; corregido solo harness | FALLIDO | INTENTO_FIXTURE_FALLIDO, detalle de intentos en VALIDACION.json; resuelto por prueba final. |
| Instalación física | Firma Debug difiere de app SM-T733; datos preservados | BLOQUEADO | No desinstalar ni sobrescribir; hace falta flujo de firma compatible o dispositivo limpio destinado a pruebas. |
| Token histórico | No disponible localmente como credencial histórica separada | BLOQUEADO | No se solicitó por chat ni se inventó; token incorrecto sí se probó. |
| Revocación tablet/usuarios | server.local.properties ausente; token compartido no acredita revocación individual | BLOQUEADO | Servicio independiente intacto, no aptitud de producción. |
| Cámara/importación/abrir-compartir XLSX | Requiere interacción de operador y aplicación receptora | PENDIENTE FÍSICO | Procedimiento concreto en PRUEBAS_REALES_DRIVE_8B. |
| SCAP/pavimentos remotos y navegación | Completar inspección/muestra, volver a detalle/Mis registros, rotación | PENDIENTE FÍSICO | Exportadores locales aprobados; no se afirma entrega o UI física aprobada. |
| Cambio durante entrega, resolución de conflicto, red intermitente en vuelo | Variantes operativas no ejecutadas | PENDIENTE FÍSICO | Retry controlado y desconexión antes de crear sí aprobados; no equivalen a estas variantes. |

No hay fallo demostrado pendiente del código productivo. Los pendientes y bloqueos permanecen abiertos; esta matriz no constituye autorización de despliegue.
