# Corte institucional V22 — expediente vigente al 04/11/2026 (Lima)

**Hito A terminado localmente. LISTO PARA CORTE CONTROLADO, condicionado a autorización y puertas previas; NO-GO productivo/general.** V22 no está autorizado ni desplegado. Este expediente sustituye la propuesta anterior que agrupaba OPEN con la publicación y consideraba el OPPO como posible dispositivo conectado. La aceptación física Android pendiente no impide preparar/publicar DRAIN_ONLY; sí limita la aceptación final y la distribución.

Base verificada local/remoto: `f44d953f68e0e885579f71a0660cac8ee93840a2`; rama `codex/scap-pavimentos-cierre-20261103`, PR #11 abierto en borrador, sin merge. #8–#10 ya contenidos. La actualización de este hito es documental, sin cambios Android/backend ni APK.

## A. Evidencia y código exacto

Lectura del navegador integrado con sesión institucional: proyecto original y deployment/URL coinciden con el anexo; versión activa 21; «Ejecutar como» muestra GBO; historial visible llega a V21 y HEAD del 3 de noviembre; cero activadores visibles para esa cuenta. No se editó código/configuración ni se ejecutaron funciones. Estos datos no prueban el runtime de V22, la ausencia de triggers de otros propietarios ni la pausa de clientes.

Paquete `PRIVADO/apps-script/paquete-final-cache-no-activar/` del kit. Manifiesto revisado SHA-256:
`d26e19de83ad867561d9215ab0c4bada4f4e60db9b7055504a08797ce845e161`.

| Acción autorizable | Archivos exactos |
|---|---|
| Reemplazar cinco existentes | `Código.gs` desde `Codigo.gs`; `PhotoNamesV2Core.gs`; `SyncV3.gs`; `SyncV3Core.gs`; `VideoV1.gs` |
| Añadir cuatro, solo tras verificar ausencia/conflictos | `AuthorizedRootGuard.gs`; `DeliveryRootRouting.gs`; `NewRootAdmission.gs`; `PublicDeliveryNames.gs` |
| Conservar cinco | `appsscript.json`; `ScapPhotoPanelV2.gs`; `SicObservationsV1Core.gs`; `VideoNamesV2Core.gs`; `VideoV1Core.gs` |
| Conservar lectores del editor | `PreflightReadonly.gs`; `PreflightReadonlyPaged.gs` |

Los hashes individuales y nombres editor están en `CORTE_ARCHIVOS_EXACTOS_PRIVADO.json`. No subir manifiestos privados ni crear otro router. Conservados autenticación, literal ROOT histórico, Servicio de Usuarios, URL, deployment ID y scopes. El destino nuevo exacto figura en CONFIG privada y en el encargo del propietario; el destino anterior obsoleto está excluido.

Revisión actual: **16/16 comprobaciones estáticas** de manifiesto, 14 hashes, procedencia V21, cinco módulos intactos, ocho módulos locales equivalentes, router reconstruido, autenticación/ROOT, destino, modo y 13 bindings. Sintaxis de 13 scripts y JSON del manifest válida, sin invocar funciones. Ningún defecto funcional nuevo confirmado. Las **76/76 backend** previas siguen acreditadas por su log privado; no repetidas, no sumar a verificaciones estáticas. Las 117 Android y 61 posteriores tampoco se repitieron ni se suman como casos únicos.

## B. Propiedades y protección

Solo estas cinco claves se proponen establecer; comparar primero las 50 propiedades respaldadas con el estado actual y conservar las ajenas:

| Clave | Propuesta |
|---|---|
| `GBO_ROOT_ROUTING_CONFIG` | JSON privado versionado: raíz histórica READ_ONLY; destino/ancestros institucionales; inventario/hash; admisión/ejecutor; época; `activatedAt` fijado al instante real del corte; modo `DRAIN_ONLY` |
| `GBO_RESUMABLE_ROOT_ID` | Literal histórico original, nunca el destino nuevo |
| `GBO_AUTHORIZED_ROOT_ANCESTOR_IDS` | `[]` para el ancla histórica compartida; no reemplaza la cadena del destino nuevo |
| `GBO_PUBLIC_NAMES_ENABLED` | `false`, sin cambio adicional de nombres remotos |
| `GBO_ROOT_ROUTING_ENABLED` | `true`, después de CONFIG válida y las anteriores, bajo pausa operativa |

`GBO_ROOT_ROUTING_ACTIVATED` **no se crea/edita/borra manualmente**. Es la cerca durable del backend previa al primer efecto. Si ya existe, detener y conciliar antes de aplicar otra época. `GBO_SYNC_V3_ENABLED` y `GBO_PHOTO_NAMES_V2_ENABLED` estaban true en la captura: revalidarlas y conservarlas; un cambio exige revisar el corte, no corregir valores silenciosamente. Tokens/credenciales no se copian a documentos públicos.

Protecciones comprobadas en fuente y pruebas existentes:

- Drive.About exige ejecutor institucional; destino y ancestros con propiedad institucional, identidad exacta, no papelera, escritura y sin edición general. La UI por sí sola no sustituye la guarda.
- **207 journals COMPLETE, 207 records, 220 bindings**, inventario previo estable en 16/16 lotes y segunda lectura. **13 bindings sin record: PRESERVE_OLD_ROOT_NO_ADOPTION**. Sin borrar, liberar, adoptar, renumerar, migrar ni suplir records. Identidad histórica queda en su raíz y se bloquean sus escrituras, incluso replay/vídeo. DRAIN_ONLY no promete reparar históricos.
- Nuevas capturas requieren sobre v2, época, fecha original posterior al corte, revisión base cero y ausencia de autoridad en ambas raíces. Pin y marca persistentes antes de efectos; UUID, revisión, reservas, hashes y ACK no se relajan. Legacy incompatible se rechaza: aceptar expresamente este impacto de disponibilidad.
- Reanudación conserva journal/IDs y opera con ScriptLock. BUSY/incompleto no produce ACK. DRAIN_ONLY permite continuaciones conocidas compatibles en raíces habilitadas; no nuevas capturas/revisiones/vídeos. Conserva consolidados por raíz y filas/IDs de V21. No une consolidados históricos con nuevos.

## C. Hito B: única autorización pendiente para DRAIN_ONLY

La autorización debe cubrir las acciones siguientes, **condicionadas a que el responsable acredite la pausa de sincronización de todos los demás clientes**. OPPO (dos QUEUED) y Xiaomi quedan excluidos y offline. No bastan cero ejecuciones visibles o modo DRAIN_ONLY para acreditar esa pausa.

1. Lectura previa del editor, versiones/deployment, triggers/ejecuciones visibles, propiedades y permisos/destino/ancestros. Captura privada nueva de lo que se modificará, sin sobrescribir las anteriores. Comparar con V21/paquete/anexo. Ejecución autorizada de los lectores paginados existentes para obtener inventario previo completo y estable; no v3Store, recuperación ni carga. Si la fuente, configuración, permisos o conjunto 207/207/220/13 cambió, detener la parte dependiente y conciliar antes de editar.
2. Aplicar únicamente los cinco reemplazos, cuatro adiciones y cinco propiedades anteriores. Conservar lectores y manifest. No usar clasp push. Crear V22 **solo si sigue libre** y editar el mismo deployment conservando URL, ejecutor institucional y acceso. No nueva implementación, ROOT literal, Usuarios, OAuth ni permisos.
3. Mantener DRAIN_ONLY; GET real de capacidades debe acreditar `rootGuardReady=true`, `rootRoutingVersion=2`, época/instante previstos, continuidad y contrato V21. El modo DRAIN_ONLY se verifica en CONFIG: GET no publica el modo y no debe inferirse de `rootGuardReady`. Si runtime falla, no probar cargas ni cambiar identidad.
4. Ejecutar `gboWarmRootRegistryReadCache` hasta `complete=true`. Solo ScriptCache desechable; cero escrituras Drive/propiedades/pins. Medir latencia real. La referencia es **634 proyecciones** de journals/records/bindings históricos, no las 891 entradas totales: reservas fotográficas no forman parte de esa proyección. Cache completa no prueba inmutabilidad de todos los archivos.
5. Lectores paginados posteriores y comparación privada de conjuntos, IDs, revisiones, ACK, hashes de contenido del registro y los 13 bindings contra el previo. Cero cambios atribuibles al corte; mismo estado durable, sin marca/pin nuevo por las verificaciones. Si no se puede probar estabilidad, conservar evidencia y detener la fase afectada. No afirmar verificación global de binarios o de reservas fotográficas.
6. Checkpoint con resultados reales, fuente/versión aplicada, propiedades privadas y manifiesto/bundle. Mantener DRAIN_ONLY sin enviar fichas. Si el corte falla antes de marca/pin/efecto, rollback condicionado al respaldo inmediato; después de cualquiera de ellos, únicamente pausa/recuperación compatible, nunca V21 a ciegas ni routing deshabilitado.

**La aprobación de B NO autoriza OPEN, entregas ficticias, reintentos de colas, instalación, distribución ni eliminación.** El estado de las 207/207/220 autoridades deberá volver a comprobarse al ejecutarlo; la captura anterior no acredita su estado actual.

## D. Hito C: ensayo real posterior, autorización independiente

Preparar un dispositivo limpio expresamente autorizado, con APK46/configuración/firma legítimas, outbox vacío y sin reservas/intentos anteriores. **No usar el OPPO actual ni la Xiaomi**; no modificar sus datos para simular aislamiento. Identificar todas las instalaciones que usan la URL y hacer que el responsable mantenga su sincronización pausada toda la ventana. OPEN no filtra por dispositivo y puede admitir a cualquier cliente compatible autenticado; si no se acredita la pausa, no abrirlo.

Solo después del PASS de B, solicitar específicamente OPEN y las fichas ficticias elegidas. Capturarlas después de `activatedAt`, con reloj correcto, ruta del catálogo y elementos/progresivas verificados como inexistentes. Para las tres familias: dos alcantarillas para conservación multirregistro SIC-18/18A, un puente SCAP con foto/croquis y una muestra Pavimentos; una por vez. Menos fichas solo acreditan el alcance realmente ensayado. Vídeo queda fuera salvo autorización/ensayo propios.

Inventariar destinos y consolidados antes: elegir ámbito sin consolidados existentes cuando sea posible. Si existe uno compartido, su modificación necesita inclusión expresa en la autorización, copia privada previa de bytes/IDs/hash/filas y verificación de todas las filas anteriores. Si no es protegible, no cargar. La etiqueta PRUEBA no prueba inexistencia.

Por entrega: ACK completo persistente y misma revisión/UUID/contentHash; descarga SHA-256/tamaño de cada archivo; rutas/nombres; propietario GBO de cada archivo; consolidado íntegro; ausencia de cambios ajenos. Un replay idéntico autorizado conserva ACK/IDs/filas sin duplicados. Ante fallo: DRAIN_ONLY compatible, guardar IDs/journal/errores; no borrar, repetir desde cero o revertir a V21 después de efectos. La nueva raíz no recibe automáticamente archivos históricos.

## E. Cierre y límites

No hay nuevo defecto local de backend confirmado ni cambios Android indispensables para este hito. Quedan comprobaciones operativas reales: pausa de clientes, preflight fresco, ejecución institucional efectiva, caché/latencia real, inmutabilidad histórica tras corte y aceptación conectada. Solo B puede acreditarlas para DRAIN_ONLY; C acredita entregas y no concede distribución general.

Android permanece 1.4.32/46, aceptación SCAP A–D pendiente de respuesta del propietario, sin reinstalar ni generar APK. Binarios remotos globales/reservas completas no certificados. **LISTO PARA CORTE CONTROLADO condicionado; NO-GO productivo/general.**

Continuidad: `docs/PROGRESO_ACUMULADO_GBO.md`. Evidencias de esta revisión y UI exclusivamente privadas: `%USERPROFILE%/GBO_CIERRE_V22_PRIVADO_20261104`; kit actualizado conservando el paquete original y sus hashes. No se ejecutó ninguna fase B/C por preparar este documento.
