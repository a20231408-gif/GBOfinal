# Actualización manual de GBO_Inventario (10H)

No se ha desplegado, activado ninguna propiedad, ni escrito en Drive de producción durante 10H. No se modifica `GBO - Servicio de Usuarios`.

El GET real comprobó V3/V4, fotografías V2 y Video V1, sin anunciar aún nombres de video V2. Su rootFolderId **difiere de la referencia local de Code.gs**. Por eso NO reemplazar completo el Code.gs publicado: incorporar únicamente el campo de capacidades indicado, preservando la raíz publicada. Los tres módulos de video usan el ROOT_FOLDER_ID del proyecto existente, sin introducir otra raíz.

## Archivos completos entregados

| Fuente del repositorio | Destino en el proyecto existente |
| --- | --- |
| `google-apps-script/VideoNamesV2Core.js` | Nuevo `VideoNamesV2Core.gs` |
| `google-apps-script/VideoV1Core.js` | Actualizar el archivo existente del motor VideoV1, extensión .gs |
| `google-apps-script/VideoV1.gs` | Actualizar adaptador existente VideoV1.gs |
| `google-apps-script/Code.gs` | Solo incorporar el campo GET indicado abajo; archivo completo de referencia disponible |

No agregar copias de módulos existentes con las mismas variables globales. Los archivos .test.cjs son pruebas locales, no se instalan en Apps Script.

## Procedimiento del propietario

1. Guardar una copia privada del código y de la implementación publicada actuales. Conservar ROOT_FOLDER_ID, API_TOKEN privado, URL /exec, propiedades y servicio avanzado Drive. No pegar el token ficticio `PON_AQUI_TU_TOKEN_ACTUAL` del archivo de referencia encima del token de producción.
2. Actualizar los tres módulos indicados en el mismo proyecto `GBO_Inventario`. En el objeto devuelto por doGet de Code.gs, agregar:

   ```javascript
   readableVideoNamingVersion: typeof videoNamesV2Enabled!=="undefined" && videoNamesV2Enabled() ? 2 : 0,
   ```

3. Conservar `GBO_VIDEO_V1_ENABLED=true`, V3 y fotografías V2 ya habilitados. No se necesitan propiedades nuevas. `VideoNamesV2.ENABLED=true` es una constante del módulo nuevo, no una ScriptProperty.
4. El propietario actualiza manualmente la implementación existente a una nueva versión, manteniendo su identidad y /exec. Este trabajo no ejecuta ese paso.
5. Consultar GET y comprobar ok=true, syncProtocol=3, pathVersion=4, readablePhotoNamingVersion=2, videoProtocol=1, videoScope=SCAP_ONLY y readableVideoNamingVersion=2. Solo después comprobar un puente ficticio autorizado siguiendo NOMBRES_VIDEOS_10H.md.

Las reservas nuevas se guardan en la carpeta técnica existente `._GBO_SYNC_V3/VIDEO_V1`, con un JSON por identidad de puente; no se agregan ScriptProperties. Se exige la reserva V4 del padre confirmado. No se reconstruyen Excel ni se renombran videos existentes.

## Reversión coordinada

Para suspender solo nuevas reservas legibles, cambiar **en el módulo nuevo** `const ENABLED=true` a false y publicar manualmente una nueva versión del mismo despliegue. Conservar el adaptador VideoV1 y el motor actualizados: contienen el soporte de diarios V2 existentes. GET anunciará readableVideoNamingVersion=0; los clientes vuelven al transporte/nombres anteriores para videos nuevos. Los diarios V2 conservan nombres/carpetas/fileId y pueden terminar de confirmarse. Una llamada ya iniciada puede completar su reserva antes de la reversión.

No restaurar ciegamente el adaptador anterior: desconocería la carpeta final guardada en diarios V2. Para suspender **todos** los videos, el propietario puede volver false la propiedad ya existente GBO_VIDEO_V1_ENABLED, que anuncia videoProtocol=0. No borrar originales, reservas, binarios parciales ni diarios. No cambiar la raíz/token/URL, ni mover evidencias históricas. Retomar exige reactivar el mismo protocolo y verificar los ACK conservados.
