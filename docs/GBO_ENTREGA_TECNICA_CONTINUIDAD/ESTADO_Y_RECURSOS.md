# Estado, pruebas y recursos de la entrega

Actualización backend 04/11/2026: Hito A cerrado, revisión estática 16/16 y sintaxis válida; UI institucional muestra mismo deployment V21. No funciones remotas ni suites repetidas. B=DRAIN_ONLY requiere aprobación y pausa; C=OPEN requiere otra aprobación. Ver expediente vigente. Evidencias Android siguientes conservan su fecha.

Vigencia de captura Android: 03/11/2026 Lima; las rutas con 20261104T04… usan UTC. No confundir fechas ni una evidencia antigua con verificación actual. Los resultados físicos posteriores deben anotarse con versión y dispositivo; el respaldo pre-46 no los cubre automáticamente.

## Android y aceptación

APK conservada: **GBO 1.4.32 (46) Debug**, package `com.tuempresa.inventariovial`.

Ruta ASUS: `C:\Users\ASUS\GBO_CANDIDATA_PRIVADA_20261103_1_4_32_46\GBO_1.4.32_46_DEBUG_ORIGINAL_EXPORTACION_SCAP_NO_INSTALAR.apk`.
Dentro del kit: `PRIVADO/APK_1_4_32/`.
SHA-256: `fd3e70a26f3b157017425b64ecab03a996990862795c94402f873b52e0af9363`.
El nombre NO_INSTALAR conserva la prohibición histórica; el propietario autorizó específicamente su instalación en OPPO en esta sesión. No autoriza otros dispositivos ni distribución.

Identidad/firma/alineación verificadas. Instalada sobre 1.4.31/45 en el único OPPO CPH2557 autorizado mediante `adb install -r -t`; confirmación oficial del propietario. Sin desinstalar, pm clear, restaurar ni usar Xiaomi. Descarga de la APK instalada con el mismo SHA. Modo avión y Wi-Fi apagado. 580/580 archivos preservados antes de abrir; arranque PASS. Tres SQLite íntegras y 43 tablas de inventario idénticas después de apertura; fotos/documentos fuente sin cambios. Solo seis tablas de metadata WorkManager cambiaron al iniciar. Datos de referencia: 11 registros de inventario, siete SCAP, dos muestras Pavimentos, 11 fotos, seis entradas de historial fotográfico y **dos filas outbox QUEUED**. No se modificaron sus intentos/ACK ni se sincronizaron. Estos conteos no deben imponerse a capturas posteriores.

| Comprobación | Evidencia vigente | Pendiente / límite |
|---|---|---|
| Regresión del ajuste SCAP 1.4.32 | 61/61 PASS, diez clases, cero omitidos | No suite histórica completa; no repetida en esta entrega |
| Regresiones anteriores | 117 Android / 76 backend acreditadas antes de este hito | No sumar selecciones solapadas ni atribuir mocks a producción |
| Compilación original APK46 | Verificada en hito anterior; firma/configuración original comparada con APK45 | No nueva APK ni cambios funcionales en esta entrega |
| Actualización y arranque OPPO46 | PASS técnico, bases y tablas protegidas comparadas | No equivale a aceptación funcional |
| A: exportar PE-3NG tras usar PE-22A | PENDIENTE de respuesta del propietario | Guardado principal directo, nombre dentro de GBO y carpeta propia |
| B: abrir Excel de PRUEBA 2 | PENDIENTE | Comprobar datos, carretera, nombre y BORRADOR si corresponde |
| C: PRUEBA 3/4 y reapertura | PENDIENTE | Destinos por identidad sin confusión ni migración histórica |
| D: cancelar Guardar como y Ver archivos | PENDIENTE | No publicación por cancelación; sin PROVISIONAL técnico como documento útil; históricos Android conservados |
| Fuente desde clon limpio | Kotlin/Java PASS, sin APK nueva | SDK/JBR/cache instalados de ASUS, no otra computadora real |
| Backend reconstruible | 14/14 módulos idénticos en texto; 12 en bytes | Dos LF/CRLF; usar paquete revisado y manifiesto para corte |
| Inventario institucional | 16/16 lotes, segunda lectura estable | Captura histórica, no se volvió a ejecutar hoy para transferir |
| Ejecutor efectivo del endpoint candidato | Guarda preparada con Drive.About | Falta prueba runtime del deployment bajo GBO tras corte autorizado |
| Nuevo Drive | Lectura previa: GBO dueño de destino/ancestros, capacidad escritura, sin edición general | Revalidar antes de escribir; no probado con nueva entrega |
| Sincronización nueva V22, ACK, hashes, dueño GBO e idempotencia | NO EJECUTADOS en nuevo Drive | Requiere corte autorizado, dispositivo limpio y clientes pausados |
| P11–P15 / observaciones históricas no acreditadas | PENDIENTES según matriz de continuidad | No convertir las pruebas offline nuevas en PASS de sincronización |
| Restauración portable del OPPO | NO PROBADA | No restaurar en tablet real para completar esta entrega |

## Captura institucional y protección

16 lotes completos, conciliación real y estabilidad por segunda lectura: 207 journals **COMPLETE**, 207 records y 220 bindings; cero journals PENDING/ROLLED_BACK/desconocidos en esa captura; cero records/bindings ilegibles y cero reviewJournals. Los ACK de los records conciliados coincidían en key/revisión/hash y carpeta vinculada. No equivale a descargar/verificar hashes de todos los archivos remotos.

Hay **13 bindings sin record confirmado**, individualizados privadamente, todos históricos: preservar raíz/identidad/reserva, sin adopción automática. La captura cuenta 634 entradas journal/record/binding dentro de un registro de 891 entradas; el contenido de reservas fotográficas y todos los binarios remotos no fue auditado. El kit conserva estas limitaciones, no presenta el inventario como certificación completa de Drive.

V21 sigue siendo el último deployment acreditado; editor y deployment se distinguen. La captura `editor/` es anterior a la adición del lector paginado: conservar también el lector versionado y la evidencia de su instalación/ejecución, no tratar la captura de once archivos como un pull actual del editor. No se hicieron lecturas ni modificaciones Google durante este cierre.

## Material público frente a privado

| Recurso | Quién lo necesita / acceso | Ubicación en kit |
|---|---|---|
| Fuente Android/backend, fixtures, ejemplos e instrucciones | Cualquier desarrollador con acceso GitHub; sin tablet ni Google | GitHub + bundle privado de la rama para contingencia |
| APK46 y comprobaciones | Responsable autorizado del piloto; contiene configuración incorporada | `PRIVADO/APK_1_4_32/` |
| Configuración Android original y versión portátil | Mantenedor autorizado; no colaborador de mocks | `PRIVADO/configuracion/originales/` y `portatil/` |
| Debug Huawei y Release histórica | Custodio autorizado de firmas; son identidades diferentes | `PRIVADO/firmas/` |
| Captura V21, editor y procedencia | Integrador backend autorizado; fuentes pueden contener secretos | `PRIVADO/apps-script/{v21,editor,CAPTURA_PRIVADA.json}` |
| Paquete candidato exacto | Publicador institucional, no ejecutar por recibirlo | `PRIVADO/apps-script/paquete-final-cache-no-activar/` |
| 50 ScriptProperties, anexo, archivos exactos, configuración propuesta | Publicador/custodio institucional | `PRIVADO/apps-script/operacion-institucional/` |
| Lotes 0–f, conciliaciones, permisos y capturas institucionales | Revisor autorizado; contienen IDs reales | Misma carpeta operacional privada |
| Nuevo respaldo OPPO con documentos y bases | Propietario/custodio, no requisito del desarrollador común | `PRIVADO/respaldo-oppo-pre-46/` |
| Logs de clon/compilación y reconstrucción | Revisor autorizado; pueden contener rutas/configuración generada | `VERIFICACION/` |
| Hashes de transferencia | Receptor legítimo; manifiesto también privado por inventariar datos | `MANIFIESTOS/` |

No transferir cookies, sesiones del navegador, `.clasprc`, tokens OAuth o códigos 2FA. En la otra laptop iniciar sesión oficialmente con la identidad autorizada. Acceso de editor personal no garantiza publicación como GBO ni acceso a archivos institucionales.

## Respaldos conservados en ASUS

Nuevo: `C:\Users\ASUS\GBO_OPPO_RESPALDO_PRE_1_4_32_20261104T040935Z`. TAR `originales/datos-internos-originales.tar`, SHA-256 `537683eed7123d65034d8c7fc8f7eda30e887c111b640b5355ac2bbe2d78dc34`. Tres SQLite integrity_check=ok, foreign_key_check=0; 74 referencias internas, 109 documentos recuperables (107 URI y dos fuentes privadas exactas), cero originales/documentos necesarios sin cobertura. Sin árboles SAF referenciados/grants observados y sin rutas externas /storage o /sdcard en columnas examinadas. No es permiso para ignorar nuevas referencias futuras.

Anterior OPPO: `C:\Users\ASUS\GBO_OPPO_RESPALDO_PRE_1_4_31_20261103T235200Z`, TAR SHA `5221a00afa0b5130c412de684aa6d9c3fca83a881fef6491b15f2a2de9442dd0`, intacto; no cubre la última prueba PE-3NG. Los respaldos Xiaomi anteriores permanecen en sus directorios originales y no se sustituyeron. El inventario privado del kit lista los existentes y lo efectivamente incluido, sin afirmar que se copiaron todos los respaldos históricos.

La copia nueva del respaldo fue comprobada archivo a archivo usando rutas largas Windows; `MANIFIESTO_COPIA_RUTAS_LARGAS.json` acredita esa copia completa. Se conserva el TAR canónico, no restaurar el árbol de verificación sobre un dispositivo. Las claves Android Keystore no salen por run-as; sesiones cifradas, permisos SAF y MediaStore pueden no recuperarse en otra UID/laptop/tablet. Los scripts forenses tienen rutas/seriales de ASUS: son evidencia, no automatizaciones portátiles para ejecutar sin revisar.
