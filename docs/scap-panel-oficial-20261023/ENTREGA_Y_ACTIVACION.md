# SCAP Puentes — panel fotográfico oficial 01–20

## Estado y alcance

Implementación Android y parche compatible de backend preparados localmente. **No publicado en Google. No validado físicamente en tablet ni mediante escritura real en Drive. No constituye una entrega aprobada para campo.**

El catálogo completo y su orden están en `CATALOGO_OFICIAL.json`. Se utiliza para puentes losa, modulares y fichas SCAP históricas. No se cambian los formularios SIC, progresivas, cálculo contractual, usuarios, firma, Room, WorkManager, identidad de fichas o tablet, URL, permisos ni ScriptProperties.

Resultados finales: **67 pruebas Android y 46 pruebas Node aprobadas**, sin fallos finales ni pruebas omitidas. Detalle por clase y ejecución: `evidencias/RESULTADOS.json`, XML JUnit y TAP. Las pruebas de backend son simulaciones locales del adaptador V20 y sus módulos, no mediciones ni confirmaciones Google.

Se conservan los fallos intermedios: un fixture de Room en Windows produjo `SQLITE_CANTOPEN`; se acortaron su método y nombre de base y pasó con SQLite nativo, cierre y reapertura reales, sin cambiar el modo de diario de la app. Dos fixtures anteriores de fotos preparaban nombres sin su reserva V2; se completó su reserva ficticia mediante el helper existente, sin desactivar flags ni relajar validaciones. Las ejecuciones finales de esas clases pasaron. En el test negativo de backend, una identidad cruzada era rechazada antes de alcanzar la validación de categoría: el test ahora comprueba expresamente ambas barreras. No se contabilizan reejecuciones como pruebas adicionales.

## Cambios Android

- El panel F.2 presenta primero los 20 apartados oficiales numerados, cada uno con fotos y videos. Las categorías de los dos catálogos de puente se actualizan sin cambiar las plantillas XLSX originales ni sus columnas/fórmulas.
- Los nuevos registros fotográficos usan una categoría estable `SCAP20_01`…`SCAP20_20`. Captura, importación y creación de registro sin imagen comparten ese catálogo. No se crean nuevas categorías con hashes de etiquetas.
- El formulario SIC-17 independiente conserva sus 15 códigos fotográficos anteriores; no recibe códigos exclusivos de SCAP que su contrato remoto no admite. La asociación opcional de una fotografía a un elemento SCAP también se conserva.
- La categoría se elige al crear la evidencia. Después se conserva para evitar cambiar inadvertidamente una asociación reservada en el servidor. Se mantienen reemplazo de imagen, descripción, fecha y originales. Los botones de orden cambian evidencias dentro del mismo apartado; no mueven categorías oficiales.
- Los videos usan los mismos códigos y encabezados. Las asociaciones anteriores y las sesiones reanudables se conservan. La lista utiliza el nombre confirmado cuando existe; los UUID siguen siendo claves técnicas, no nombres públicos de archivo.
- La tabla/panel fotográfico del Excel SCAP, el ZIP, los respaldos provisionales y el índice de continuación siguen el orden oficial. Varias fotos de un apartado conservan su orden interno. El límite contractual de 32 fotos en una plantilla no cambia; las adicionales mantienen el anexo local ya implementado.
- La generación local automática usa subcarpetas oficiales y nombres descriptivos. No altera los originales privados ni sus rutas Room. Las nuevas copias legibles se separan por generación/revisión para conservar las anteriores sin depender de hashes en el nombre de cada foto/video.
- Se genera una nueva caché de paquete SCAP (versión interna 9). No se sustituyen manifiestos de sincronización ya congelados ni se reinician trabajos/revisiones.

## Nombres

El ordinal oficial de dos cifras es fijo y distinto del contador de archivos. Los nombres cortos llevan `01_PAN_DER`, `03_PERFIL`, `08_RODADURA`, `15_ESTRIBO_IZQ`, `18_TABLERO_INF`, `20_CAUCE_ABAJO`, etc. Se conservan todos los conceptos de la lista autorizada; no se abrevian de manera ambigua las dos orillas o direcciones.

Las carpetas contienen el ordinal y la descripción completa normalizada: espacios y signos se convierten a `_`, sin acentos, conservando los nombres visibles exactos en la interfaz. Ejemplo: `06_SENALES_INFORMATIVAS_ACCESO_IZQUIERDO`.

Ejemplos comprobables en las pruebas y archivos de evidencia:

| Destino | Nombre / estructura |
|---|---|
| ZIP/local sin numeración remota reservada | `fotos/01_VISTA_PANORAMICA_LADO_DERECHO/01_PAN_DER_001.jpg` |
| Copia documental tablet | `FOTOGRAFIAS/08_SUPERFICIE_DE_RODADURA/08_RODADURA_001.jpg` |
| Nueva reserva fotográfica servidor, puente 7 | `01_PUE7_PAN_DER_001.jpg` |
| Nuevo video servidor, puente 7 | `18_PUE7_TABLERO_INF_001.mp4` |
| Carpeta de video | `VIDEOS/18_TABLERO_VISTA_INFERIOR/` |

El último contador de fotos lo reserva el servidor y puede ser distinto del ordinal del apartado; mantiene la identidad y la prevención de duplicados. Los videos conservan sus contadores por categoría. No se inventa un número global de puente antes de obtenerlo del servidor. Al existir reserva/ACK, las copias usan sus nombres acreditados. El espejo confirmado de Drive conserva exactamente la ruta del ACK, incluida la separación existente `SCAP/<identidad de inspección>/`; ese identificador técnico evita mezclar inspecciones del mismo puente.

Se mantiene JPEG para transporte de fotos. Una copia local de un original PNG/WebP/HEIC conserva su extensión real. Videos: MP4/WebM/3GP. No se amplía el tamaño de video ni se convierten archivos por cambiarles la extensión.

## Compatibilidad y registros anteriores

No hay migración masiva ni reclasificación persistida al abrir una ficha. Los originales, UUID, índices de captura, reservas y ACK anteriores permanecen intactos.

Se agrupan visualmente equivalencias razonables, manteniendo el origen: acceso izquierdo/derecho, superficie de rodadura (incluida la errata anterior RODARURA), señal del acceso correspondiente, vereda/baranda de cada dirección, pantalla de estribo correspondiente, cara inferior de losa y cauce de cada dirección. Los dos antiguos códigos de vista lateral se agrupan en las panorámicas correspondientes. Los valores originales siguen en los datos.

**No se inventa una correspondencia** para `AGUAS ARRIBA`, `AGUAS ABAJO`, acceso genérico o drenaje combinado arriba/abajo: podrían referirse a distintos apartados nuevos. Aparecen después de los 20 oficiales como históricos. No se habilita captura nueva en esos grupos.

Una equivalencia visual no mueve archivos históricos en Drive ni cambia su categoría técnica. Las reservas antiguas son inmutables: cambiar de categoría el mismo UUID produciría un conflicto real de integridad. Los nombres/carpetas antiguos pueden seguir existiendo exclusivamente para esas evidencias anteriores. Su eventual reorganización remota requiere un procedimiento de migración aprobado por el propietario; no se ejecuta en esta entrega.

Las APK antiguas continúan enviando sus categorías anteriores al backend ampliado. **No debe volver a instalarse una APK antigua sobre datos que ya contengan nuevas categorías** sin un plan de recuperación: esa APK no conoce los nuevos códigos y nombres largos. No se instala ni se degrada ninguna aplicación en este trabajo.

## Dependencia de servidor: protección de la cola

La producción revisada V20 no conoce los códigos nuevos y el cliente anterior limita nombres V2 a 20 caracteres. Enviar las nuevas categorías sin coordinación produciría rechazo o nombres incoherentes.

El cliente exige `scapPhotoPanelVersion=2` y nombres V2 antes de reservar o subir estas nuevas fotos/videos. Si falta capacidad, conserva la evidencia offline y muestra que está pendiente de actualizar el servidor. Las fotografías quedan en `PENDING_VALIDATION` y los videos en `BACKEND_REQUIRED`; no se registra ACK ni entrega confirmada para esos archivos. Los archivos históricos mantienen su flujo anterior. No se utiliza GENERAL/OTHER para ocultar la incompatibilidad.

La consulta real de solo lectura del 23/10/2026 confirmó `syncProtocol=3`, `pathVersion=4`, nombres fotográficos V2, `videoProtocol=1`, `videoScope=SCAP_ONLY` y nombres de video V2. **No existe aún la capacidad `scapPhotoPanelVersion` en la respuesta publicada.** No se enviaron credenciales ni registros de prueba; la evidencia contiene únicamente campos de capacidad permitidos, sin URL ni identificadores privados (`evidencias/CAPACIDADES_SOLO_LECTURA.json`).

El nombre nuevo tiene un límite seguro de 80 caracteres, exclusivo de las nuevas categorías, equivalente al límite de componente de ruta existente. Las validaciones anteriores de nombres históricos/SIC/Pavimentos se conservan. Los nuevos códigos se admiten únicamente en SCAP; `videoScope=SCAP_ONLY` y `videoProtocol=1` continúan sin cambios.

## Archivos de backend preparados, sin publicar

En `backend-parche-NO-PUBLICADO/`:

| Archivo | Acción futura y contenido |
|---|---|
| `ScapPhotoPanelV2.gs` | Añadir: catálogo oficial, carpetas y constructor estricto de nombres; sin llamadas de servicio ni secretos. |
| `PhotoNamesV2Core.gs` | Reemplazo **previa comparación**: admite categorías oficiales solo para reservas SCAP y genera nombres descriptivos. Conserva reservas históricas y contadores existentes. |
| `VideoNamesV2Core.gs` | Reemplazo **previa comparación**: resuelve nuevos códigos y nombres, mantiene sesiones/UUID y nombres ya reservados. |
| `SyncV3Core.gs` | Reemplazo **previa comparación**: amplía exclusivamente la validación y carpeta de las categorías SCAP nuevas; no cambia transacciones, ScriptLock, ACK, consolidación ni ordenamiento. |

Las bases son las tres fuentes originales V20 previamente recibidas; sus SHA-256 están en `BASES_SHA256.json` y el cambio exacto en `CAMBIOS_BACKEND.patch`. No se han mezclado las optimizaciones de lock/workbook, cambios de ordenamiento ni otros parches anteriores pendientes de publicar. Las implementaciones existentes de Observaciones y videos se conservan.

No se entrega ni sustituye un `Codigo.gs` completo. `CAPACIDAD_Codigo_FRAGMENTO_NO_REEMPLAZAR.txt` contiene únicamente la propiedad que el propietario deberá añadir a la respuesta existente de capacidades después de verificar paridad. La capacidad solo anuncia 2 si los módulos completos están presentes y la configuración V2 existente ya lo permite. No se cambia ni se añade una ScriptProperty.

## Procedimiento coordinado y reversión

1. **Respaldo del propietario:** exportar las ocho fuentes del GBO_Inventario publicado, anotar versión/despliegue actual y conservar configuración de forma privada. No modificar el Servicio de Usuarios. No copiar propiedades ni secretos a Git, documentos o capturas.
2. Comparar las tres fuentes publicadas con sus bases V20 de `BASES_SHA256.json`. Si difieren, revisar y trasladar únicamente los bloques de `CAMBIOS_BACKEND.patch`; no sobrescribir mejoras productivas desconocidas. Revisar las cuatro fuentes y el fragmento de capacidad.
3. En el proyecto independiente de pruebas y su raíz autorizada, incorporar el módulo nuevo y sustituir las otras tres fuentes. Añadir la propiedad de capacidades al `doGet` existente. Mantener los helpers de aislamiento y los demás módulos del ensayo sin cambios. No copiar una raíz productiva.
4. Ejecutar primero las pruebas locales adjuntas. Después, **con autorización de escritura en la raíz ficticia**, usar dos fichas ficticias de distintos clientes: fotos 01,03,08,15,20, dos fotos del mismo apartado y video 18. Comprobar rutas, bytes SHA-256, ACK de padre y ACK propio de video. Repetir la misma revisión tras pérdida de respuesta: mismos IDs, sin duplicados. Ensayar un registro histórico y una edición sin renombrar su reserva anterior.
5. Validar físicamente captura/importación, borrador y reapertura offline, Excel/ZIP/copia pública tablet. Para las primeras escrituras Google usar el ejecutor aislado del proyecto de pruebas; no cambiar la URL de la APK a ciegas. La comprobación física contra el servicio real se coordina posteriormente con el propietario y una ficha ficticia autorizada. Conciliar el espejo de Drive contra el ACK, no contra una etiqueta local de estado.
6. Solo después de aprobar paridad y pruebas Google: el propietario podrá crear una versión de GBO_Inventario y editar el despliegue existente para apuntar a esa versión, **conservando URL /exec**, propiedades, raíz y permisos. Verificar capacidades antes de habilitar el piloto Android. Este trabajo no ejecuta esa publicación.
7. **Reversión:** devolver el despliegue a la versión respaldada; los clientes nuevos dejarán las categorías oficiales en espera al desaparecer la capacidad. No borrar originales, reservas, journals ni archivos nuevos. Conservarlos para recuperar con el backend compatible. No reinstalar automáticamente APK antiguas ni modificar Room.

## Checklist físico pendiente y decisión

Crear ficha ficticia → confirmar 20 encabezados → asociar fotos 01,03,08,15,20 y video 18 → guardar borrador → cerrar/reabrir sin Internet → exportar y verificar nombres/orden/bytes → reconectar al servidor de pruebas compatible → comprobar ACK padre y video por separado → repetir sin duplicados → revisar que un registro histórico conserva sus archivos.

**GO para validación física offline de la candidata 1.4.13 (27). NO-GO para distribución o para declarar Drive validado mientras falten el backend compatible y la prueba física/Google.** El propietario confirmó que no existe ninguna versión 27 o superior distribuida y autorizó esta numeración. La captura offline puede probarse antes de publicar el servidor; sus entregas nuevas quedarán conservadas en espera de capacidad. Se mantiene la firma histórica y la configuración legítima. El resultado de compilación y verificación del artefacto se entrega en `RELEASE_1.4.13.md` y `evidencias/RELEASE_1.4.13.json`.
