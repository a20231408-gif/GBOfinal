# Fuentes necesarias de GBO_Inventario

Necesito las fuentes correspondientes a la **versión numérica que atiende el despliegue /exec usado por las tablets**, no una copia del editor sin comprobar su versión. En Administrar implementaciones, el propietario puede consultar la versión asignada; contrastarla con Historial del proyecto o una exportación de esa versión obtenida con acceso de lectura autorizado. Un borrador guardado no cambia automáticamente la implementación publicada.

## Lista exacta por nombre lógico

1. **Code.gs** completo y redactado: `doGet`, `doPost`, validación de token, despacho de `reservePhotoNamesV2`, `stageRecordV3`, `commitRecordV3`, `beginVideoV1`, `finishVideoV1`, rutas legacy y helpers de carpetas/respuesta. No reemplazar este archivo con el del repositorio.
2. **SyncV3.gs**: adaptador completo, lock, staging, reserva, `v3Store`, recuperación, lectura/escritura de archivos y metadatos, empaquetado XLSX.
3. **SyncV3Core.gs**: motor que declara `SyncV3`, validación/canonical/hash, revisión/base, orden técnico, consolidación, transacción y ACK. Si tiene otro nombre, proporcionar ese archivo real, no renombrarlo en producción.
4. **PhotoNamesV2Core.gs**: módulo que declara `PhotoNamesV2`; incluir adaptadores/helpers separados de reserva si el proyecto publicado los tiene.
5. **SicObservationsV1Core.gs**: módulo que declara `SicObservationsV1`, especialmente `inspect`, `select`, `inherit`, `plain` y `printExtent`.
6. **VideoV1.gs**: adaptador de videos, locks y almacén de diarios/sesiones.
7. **VideoV1Core.gs**: motor `VideoV1`, begin/finish, compatibilidad histórica y ACK.
8. **VideoNamesV2Core.gs**: nombres legibles 10H, catálogo y compatibilidad de reservas antiguas. Si no existe, indicar que falta; no crearlo todavía.
9. **appsscript.json** de esa versión: runtime, servicios avanzados y versiones (Drive), scopes y metadatos de despliegue que consten en el manifiesto. Redactar cualquier identificador privado que haya añadido el propietario; mantener nombres de servicios, versiones y scopes para poder compararlos.
10. **Inventario de los demás .gs y librerías** de esa misma versión. Proporcionar también cualquier módulo extra que declare o intercepte los símbolos anteriores, maneje `ScriptLock`, journals, bindings, consolidado o permisos de inventario. Ejemplo: un `InventoryAuthorization10C` si realmente estuviera integrado. No añadirlo desde la referencia local por suposición.

Si el proyecto separó o renombró funciones, importa su contenido completo con su nombre real. Incluir todos los helpers llamados por los módulos, no solo extractos de las funciones supuestamente lentas. Si hay librerías externas, sus nombres de API y versiones, sin credenciales. No se necesita código ni acceso a GBO - Servicio de Usuarios.

## Redacción de información privada

- En **la copia para revisión**, sustituir los valores de API_TOKEN, ROOT_FOLDER_ID y cualquier token, clave, correo, identificador privado, URL de servicio, sesión de subida o credencial incrustada por marcadores constantes distintos como `<REDACTED_API_TOKEN>` y `<REDACTED_ROOT_ID>`.
- Conservar nombres de constantes, funciones, condiciones y referencias; no eliminar las ramas de autorización. Indicar qué líneas fueron redactadas. Si hay valores privados calculados, redactar el literal de origen conservando la estructura lógica necesaria.
- **No enviar valores de ScriptProperties**, ni siquiera parcialmente. Tampoco exportar cachés, diarios reales, fichas, fotos o payloads. Los nombres de las propiedades referenciadas en el código bastan para la comparación de código; si falta una capacidad, el propietario verifica privadamente su configuración.
- No pegar la copia redactada sobre producción: sus marcadores son solo para análisis.

## Metadatos mínimos, sin secretos

Nombre GBO_Inventario, número de versión publicada, fecha/hora y zona de consulta, confirmación del propietario de que corresponde al endpoint instalado, y modalidad categórica de ejecución/acceso del despliegue (sin direcciones de correo ni listas de usuarios). No necesito la URL /exec, IDs privados de implementación ni identificadores de raíz para preparar el merge.

Con esas fuentes produciré una comparación a tres vías: **publicado verificado / referencia local / delta de optimización**. Cada función productiva divergente se conserva o se integra con justificación y prueba, nunca mediante un reemplazo completo supuesto. Hasta recibirlas, la comparación con producción queda explícitamente pendiente.
