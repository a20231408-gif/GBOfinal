# Publicación institucional V22: procedimiento manual condicionado

**NO EJECUTADO / NO AUTORIZADO.** No guardar código remoto, crear versión, cambiar propiedades ni deployment por el hecho de recibir este kit. Requiere autorización consolidada del propietario y acceso institucional legítimo. No usar la cuenta personal para asumir la identidad ejecutora GBO. No crear proyectos, carpetas de ensayo ni deployment paralelos.

Fuente de decisión: `docs/EXPEDIENTE_CORTE_INSTITUCIONAL_20261103.md` y `PRIVADO/apps-script/operacion-institucional/EXPEDIENTE_CORTE_ANEXO_PRIVADO.md`. El anexo identifica scriptId, deploymentId, URL `/exec`, raíces, cadena de ancestros, configuración propuesta y evidencias. No copiar sus valores a este documento público. No usar una URL `/dev` o de ensayos en Android. Servicio de Usuarios independiente, sin cambios.

## 1. Identificación y condiciones anteriores a toda edición

1. Con la cuenta institucional, abrir **GBO_Inventario original** y comparar scriptId de Configuración del proyecto con el anexo. El nombre no basta. Completar personalmente el flujo oficial si Google solicita seguridad; no compartir códigos ni tokens y no eludir controles.
2. Implementar → Administrar implementaciones: identificar el MISMO deployment ID y URL que utiliza Android, versión activa V21, ejecutar como y propietario/desplegador. No deducir la identidad de ejecución del texto «Yo» ni del correo del editor. Conservar captura privada y referencia V21 para recuperación.
3. Verificar si cambiaron fuente editor, historial/versiones, deployment, manifiesto o propiedades desde las capturas. Consultar también desencadenadores y ejecuciones activas antes de editar código HEAD. Si existe una ejecución/trigger que pueda consumir el editor o cambió la fuente, detener esa fase y revisar; no disparar funciones para comprobarlo.
4. Revalidar por lectura destino institucional, ID exacto y cadena de ancestros del anexo, propiedad GBO, no papelera, capacidades necesarias y ausencia de edición general. No sustituir por la carpeta anterior desactualizada. No cambiar permisos ni ROOT para superar una discrepancia.
5. Revalidar inventario/conciliación y conservación de los 13 bindings sin record; el lector paginado ya existe. Cualquier nueva ejecución requiere alcance de lectura autorizado y evidencia privada. La captura estable de 207/207/220 no acredita que hoy no haya novedades.
6. Pausa operativa verificable de sincronización de otros clientes por el responsable. Obtener respaldo privado fresco de la configuración/código que se va a tocar, comparar con el kit y registrar su SHA. No restaurar las 50 propiedades antiguas encima de cambios posteriores.

Si alguna condición falla, no modificar esa parte del servidor. La disponibilidad del segundo factor no equivale a autorizar el corte.

## 2. Archivos exactos del candidato revisado

Paquete privado: `PRIVADO/apps-script/paquete-final-cache-no-activar/`.
Manifiesto privado revisado SHA-256: `d26e19de83ad867561d9215ab0c4bada4f4e60db9b7055504a08797ce845e161`.
Operaciones exactas y SHA de cada archivo: `PRIVADO/apps-script/operacion-institucional/CORTE_ARCHIVOS_EXACTOS_PRIVADO.json`.

| Operación | Archivos |
|---|---|
| Reemplazar contenido de archivos existentes | `Código.gs` desde `Codigo.gs` del paquete; `PhotoNamesV2Core.gs`; `SyncV3.gs`; `SyncV3Core.gs`; `VideoV1.gs` |
| Añadir, solo si no existen o tras conciliación expresa | `AuthorizedRootGuard.gs`; `DeliveryRootRouting.gs`; `NewRootAdmission.gs`; `PublicDeliveryNames.gs` |
| Conservar sin cambios | `appsscript.json`; `ScapPhotoPanelV2.gs`; `SicObservationsV1Core.gs`; `VideoNamesV2Core.gs`; `VideoV1Core.gs` |
| Conservar adicionalmente en editor | `PreflightReadonly.gs`; `PreflightReadonlyPaged.gs` |

Son cinco reemplazos, cuatro adiciones y cinco conservados en el paquete de 14, más lectores del editor. No crear un segundo router Codigo/Código ni borrar lectores. `MANIFIESTO_PRIVADO.json` no es archivo de Apps Script. No pegar la carpeta completa ni usar clasp push para ahorrar pasos. No tocar los cuatro archivos KEEP o scopes del manifiesto para corregir saltos de línea. El paquete reconstruido desde GitHub tiene equivalencia textual; el candidato privado revisado conserva los bytes de referencia.

En el editor original, después de autorización: abrir cada archivo, reemplazar únicamente su contenido previsto, añadir los cuatro módulos ausentes, guardar y comprobar ausencia de funciones duplicadas y errores de sintaxis. No ejecutar doPost, v3Store, inicializadores o recuperaciones. Ningún guardado de fuente crea por sí mismo una versión publicada, pero también debe estar autorizado.

## 3. ScriptProperties: nombres y sentido, no secretos

Leer los valores exactos propuestos únicamente del anexo/JSON privados. La configuración se validó en mocks; no es evidencia de permisos reales ni una autorización para escribir.

| Clave | Acción futura y guarda |
|---|---|
| `GBO_ROOT_ROUTING_CONFIG` | JSON versionado: raíces antigua/nueva, cadena del destino, inventario/hash, `oldRootReadOnly`, admisión institucional, época e instante UTC del corte, modo inicial `DRAIN_ONLY` |
| `GBO_RESUMABLE_ROOT_ID` | ID histórico original; no sustituir el literal ROOT ni usar aquí el destino nuevo |
| `GBO_AUTHORIZED_ROOT_ANCESTOR_IDS` | `[]` previsto para el ancla histórica compartida; no sustituye la cadena obligatoria del destino nuevo |
| `GBO_PUBLIC_NAMES_ENABLED` | `false`; no activar otra migración de nombres remotos en este corte |
| `GBO_ROOT_ROUTING_ENABLED` | `true` solo al habilitar el conjunto autorizado, después de CONFIG válida y los demás campos preparados |
| `GBO_ROOT_ROUTING_ACTIVATED` | NO crear, editar ni borrar manualmente; cerca durable escrita por el backend antes del primer efecto autorizado |

Las seis claves estaban ausentes en el respaldo institucional de 50 propiedades. Comparar de nuevo antes del corte. Conservar todas las demás propiedades; no mostrar/copiar tokens en informes. No modificar credenciales, usuarios, OAuth, API token, ROOT_FOLDER_ID literal ni IDs históricos. Fijar `activatedAt` al instante real de corte, no a la fecha de preparación; conservar una sola época. Si existe marca de activación o una época ya aplicada, detenerse: no sustituirlas.

## 4. Versión, deployment original y DRAIN_ONLY

Solo después de la aprobación: preparar las propiedades y archivos conciliados bajo pausa operativa; crear una **nueva versión del proyecto original**, V22 únicamente si sigue libre. Si la numeración/historial difiere, detener y revisar, no publicar otra versión por analogía. En Administrar implementaciones, editar el deployment original y seleccionar esa versión, conservando ID/URL y la identidad ejecutora institucional. No crear Nueva implementación ni cambiar el acceso general.

Mantener inicialmente DRAIN_ONLY. El GET de capacidades no carga fichas y debe acreditar `rootGuardReady=true`, `rootRoutingVersion=2`, época/instante correctos y continuidad anunciada. La guarda realiza Drive.About y exige la identidad institucional configurada: estar logueado como GBO en el navegador no sustituye esta comprobación runtime. Si falla, no abrir admisión ni enviar registros.

La función preparada `gboWarmRootRegistryReadCache` permite precalentar el índice hasta `complete=true`, con lectura presupuestada y solo ScriptCache desechable. Ejecutarla únicamente como parte del corte autorizado. No escribe Drive ni pins/propiedades; caché perdida obliga a reconstruir, nunca a ignorar filas. Medir latencia real y coste; presupuesto cooperativo de una función no limita una llamada Drive individual que se bloquee. Una lectura parcial/BUSY no acredita inventario completo ni ACK.

## 5. Protección histórica y admisión nueva

207 journals COMPLETE, 207 records y 220 bindings conciliados en una lectura real de 16/16 lotes con segunda lectura estable. Los 13 bindings sin record confirmado tienen acción privada **PRESERVE_OLD_ROOT_NO_ADOPTION**: no borrar, liberar, adoptar, renumerar, migrar ni crear registros que los suplanten.

La raíz antigua preparada es READ_ONLY, incluso para replay/vídeo de históricos. No prometer que DRAIN_ONLY recupera esas entregas históricas: solo permite continuaciones conocidas compatibles en raíces habilitadas. Identidad, reserva, binding o revisión histórica conserva su raíz y se bloquea si exige escribir allí. Conflicto, referencia ilegible o evidencia en ambas raíces bloquea la operación afectada; no inventar UUID ni fecha.

Una ficha realmente nueva requiere sobre negociado v2, época correcta, captureCreatedAt original posterior al corte, baseRevision=0 y ausencia de identidad previa en ambas raíces. Se fija un pin durable antes de efectos. Edición posterior no rejuvenece una ficha vieja. Android sobre V21 conserva su contrato sin esos metadatos negociados. Vídeo nuevo requiere SCAP padre confirmado y verificaciones de revisión/ACK/raíz; legacy no se habilita automáticamente.

## 6. Fallos, pausa y recuperación

| Momento | Acción segura autorizable | No hacer |
|---|---|---|
| Antes de editar/publicar | Detener operación, preservar capturas y solicitar revisión de discrepancias | Cambiar ROOT/permisos para forzar coincidencia |
| Código/propiedades editados, sin marca ni efecto nuevo | Verificar ausencia de efectos y volver al deployment V21 y a la configuración exacta anterior mediante rollback autorizado | Restaurar ciegamente un respaldo viejo o borrar claves ajenas |
| Marca `GBO_ROOT_ROUTING_ACTIVATED`, pin o cualquier efecto nuevo | Pausar admisión en DRAIN_ONLY compatible, conservar época/raíces/marca, journals, staging, IDs y evidencias; revisar corrección compatible | Volver a V21, desactivar routing o retirar la marca; podría saltarse protección y duplicar entregas |
| PENDING o respuesta perdida | Reconciliar journal/operaciones/archivos por IDs y ACK; reanudar solo por contrato idempotente autorizado | Reiniciar carga grande, borrar staging o crear otra identidad |
| BUSY, caché incompleta, permisos o hash inválido | No confirmar ACK; conservar origen y evidencias, detener solo la operación afectada | Convertir FAIL en PASS, relajar hash/ACK o usar otro propietario |

Reanudar una intervención significa verificar primero el último estado durable y las evidencias; no ejecutar de nuevo toda la secuencia de publicación. Después de una primera escritura no existe rollback simple a V21. El paquete no activa borrado administrativo/tombstones destructivos ni limpieza histórica.

## 7. Ensayo conectado mínimo, autorización posterior

No usar el OPPO actual: la captura pre-46 contiene **dos filas QUEUED** que se conservaron sin cambios. Tampoco la Xiaomi. Elegir un dispositivo limpio expresamente autorizado, misma APK/configuración, sin outbox/reservas anteriores; un perfil de usuario nuevo o instalación adicional también exige autorización específica. No conseguir aislamiento borrando o editando pendientes.

OPEN no es una lista de dispositivos permitidos. Requiere aceptación física, ejecutor/permisos/capacidades acreditados, relojes correctos, otros clientes pausados y autorización de prueba. Comprobar por lectura que las identidades, progresivas, carpeta de elemento y consolidados elegidos son seguros. Si existe un consolidado, conservar IDs/hash/bytes/filas antes y exigir conservación de filas; si no puede garantizarse, no cargar. No inventar una carretera fuera de catálogo.

Para el alcance completo preparado: dos alcantarillas nuevas para SIC-18/18A, un SCAP con foto/croquis y una muestra Pavimentos, una por vez, solo tras autorización expresa de esas fichas. Si se autoriza inicialmente una sola, limita la evidencia a esa familia. Cada una debe verificar archivo descargado/bytes/tamaño/SHA, nombres/rutas, propietario **GBO de cada archivo**, ACK completo persistente (key/UUID/revisión/contentHash y archivos), consolidado y ausencia de cambio ajeno. Un replay idéntico autorizado debe conservar ACK/IDs sin duplicados. Vídeo queda sin GO si no se ensaya explícitamente. No borrar las pruebas al terminar.

Ante FAIL, pausar en DRAIN_ONLY, preservar operaciones y presentar causa; no distribuir APK. La aceptación offline y los mocks no acreditan sincronización, dueño GBO ni ACK real.
