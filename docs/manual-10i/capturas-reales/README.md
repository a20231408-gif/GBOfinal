# Capturas reales para la guía visual 10J

El 14 de octubre ADB se ejecutó con permisos de lectura del dispositivo, pero `adb devices -l` no listó dispositivos y el serial 523bc083 no estaba disponible. **No se obtuvieron capturas reales en ese intento.** No se sustituyen por interfaces dibujadas ni por fotos de un emulador que no se haya ejecutado.

Después de ese intento, el propietario **canceló la obtención de capturas por ADB** y decidió tomar y trabajar las imágenes por separado con ChatGPT. No se sigue esperando una conexión ni se ejecutará el procedimiento a continuación; queda como referencia opcional ya preparada. La guía y sus posiciones se entregan sin capturas.

## Procedimiento breve

1. Conectar la tablet de pruebas por USB, desbloquearla y autorizar ADB. Abrir GBO manualmente, iniciar sesión sin compartir el PIN y ocultar teclado/notificaciones/credenciales.
2. Mostrar una de las pantallas de la lista. Usar únicamente registros ficticios identificados TEST_10I/TEST_10J; no crear, guardar ni finalizar registros solo para conseguir una captura sin decidir antes su efecto de sincronización. Si no existe una ficha ficticia adecuada, dejar ese espacio pendiente.
3. Ejecutar desde la raíz del proyecto, por ejemplo:

```powershell
.\tools\capturar_manual_10j.ps1 -Nombre '01-inicio' -Descripcion 'Inicio con módulos de GBO'
```

El script verifica el serial y que GBO esté en primer plano. Solo ejecuta screencap/pull y consultas de versión; no pulsa botones ni modifica registros. Si la sesión vence, el propietario introduce personalmente su PIN. Si no hay dispositivo, termina sin captura.

4. Revisar visualmente el PNG en pendientes-privadas (ignorado por Git): ningún nombre personal, PIN, token, URL privada, notificación o ficha real. Una pantalla con datos indebidos se descarta de la selección, sin modificarla para aparentar una pantalla distinta. Preferir repetirla con un contexto ficticio limpio.
5. Tras aprobar una captura, copiarla con el nombre de la lista al directorio actual y registrar su SHA-256 en manifiesto.json con `privacyReviewed=true`. El constructor solo incorpora imágenes de ese manifiesto tras verificar hash y aprobación; las capturas privadas nunca se incorporan automáticamente.
6. Si hacen falta indicadores, conservar el PNG original y colocar números/flechas discretos **como anotaciones** sobre la copia de publicación, sin cambiar etiquetas, valores ni componentes. Registrar el original y anotación. En esta entrega no se colocan flechas sobre espacios vacíos.
7. Ejecutar el constructor, actualizar campos/exportar PDF con Word y revisar todas las páginas. La APK instalada puede ser anterior a 10I/10J: verificar versión y comportamiento antes de atribuir a una captura la corrección nueva. No se instala otra APK en esta tarea.

## Pantallas a abrir manualmente

| Nombre de PNG | Pantalla exacta | Qué debe verse |
| --- | --- | --- |
| 01-inicio.png | Inicio, ya autenticado | Entradas de SIC, Pavimentos y Puentes; sin nombre personal |
| 02-sic-formulario.png | Nuevo registro SIC-18 ficticio | Ruta/PR y campos contractuales, sin guardar para capturar |
| 03-sic-observaciones.png | Final del formulario SIC ficticio | Campo Observaciones y acción normal de revisión/guardado |
| 04-fotografias.png | Fotografías de una ficha ficticia | Tomar foto/Incorporar fotos y miniatura ficticia |
| 05-pavimentos-cabecera.png | Pavimentos, cabecera ficticia | Superficie, ruta, muestra y dimensiones |
| 06-rigido-esquema.png | Muestra rígida ficticia con daños | Losa y localización parcial real, si la APK instalada ya la soporta |
| 07-ahuellamiento.png | Editor de Ahuellamiento | Profundidad media, sin ancho de fisura en versión corregida |
| 08-foto-dano.png | Fotos de un daño ficticio | UUID/daño correcto, fotografía, reemplazo/retiro |
| 09-puentes.png | Inspección Puente ficticio existente | Navegación por secciones; no rellenar un SCAP completo |
| 10-video-scap.png | Multimedia de categoría de Puente ficticio | FOTO/VIDEO y estado real del video, si ya existe |
| 11-drive.png | Detalles de entrega de ficha ficticia | Estado/revisión y confirmación real; nunca inventar un ACK |
| 12-exportaciones.png | Revisar y finalizar de muestra ficticia | Reporte PDF, Excel de superficie, Subir muestra a Drive |

No provocar un error ni una subida de producción para ilustrar errores frecuentes. Esa sección puede usar texto corto hasta que exista una captura autorizada del error real y anonimizada.

Los espacios de la guía están preparados con estos nombres. No es necesario abrir todas las pantallas en una sesión: capturar la disponible, revisar e incorporar solo esa. El manual anterior de 14 páginas permanece como respaldo y referencia técnica.
