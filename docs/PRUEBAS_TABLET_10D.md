# Pruebas físicas 10D — pendientes

Dispositivo indicado por el propietario: Huawei MatePad 12 X, LRT-W09, HarmonyOS 4.3.0. No equivaler HarmonyOS a un API Android. Registrar el API reportado por la app/diagnóstico o, mediante USB autorizado, `adb shell getprop ro.build.version.sdk`; no contiene credenciales.

## Instalación

Usar la APK Debug de app/build/outputs/apk/debug/app-debug.apk con las configuraciones locales privadas. Copiarla a un dispositivo de pruebas y abrirla, o usar `adb install -r app/build/outputs/apk/debug/app-debug.apk`. Si falla por firma frente a la APK de campo, no desinstalar, no usar pm clear ni borrar almacenamiento. Detener la instalación y conservar los datos. La APK incluye configuración privada: compartir solo con las personas autorizadas; no publicar en Git.

## Formularios y giro

1. Abrir un registro ficticio de Alcantarilla. Comparar alta/edición: ruta/calzada/sentido, PR inicial, datos contractuales, función/condición/fallas, fotografías y observaciones; ausencia de PR fin y sugerencia GNSS.
2. Escribir datos incompletos sin guardar, girar horizontal→vertical→horizontal. Permanecer en la misma ficha; comprobar texto crudo, selección y desplazamiento. No debe aumentar el número de registros/revisiones por el giro.
3. Guardar y editar la misma ficha, verificar UUID en Detalles de entrega Drive. Repetir el giro con texto todavía sin guardar y diálogo de revisión abierto.
4. Repetir brevemente en los demás formatos SIC y en SCAP, sin alterar fichas reales.

## Cámara y galería

1. Usar cámara externa predeterminada; girar antes y después de capturar, volver a la ficha y abrir vista previa. Repetir con la lente interna seleccionada si está disponible.
2. Registrar qué app de cámara se abrió. Cancelación o fallo de una app externa no demuestra error del renderer GBO; conservar archivo original y mensaje. Un fallo de Camera2 interna corresponde a GBO/lente/permiso y debe incluir paso de reproducción.
3. Importar JPEG con EXIF, PNG y HEIC compatible con el dispositivo. Comprobar orientación y aspecto, sin estiramiento; volver tras picker/giro sin perder destino o categoría. Confirmar orientación del original en otro visor autorizado.
4. Fotos de Pavimentos: dos daños del mismo tipo, varias fotos por daño, panel general con las mismas asociaciones. Retirar/reemplazar y revisar historial; cerrar offline y recuperar la muestra.

## Sincronización y ACK

Usar solo fichas completamente ficticias. No editar/borrar registros de los ingenieros para reproducir.

1. Finalizar con conexión y observar fases, archivos procesados/pendientes y tiempo transcurrido. Solo esperar estado completado con revisión local=confirmada y ACK verificable.
2. Desconectar durante envío o confirmación; registrar key/UUID, revisión y mensaje. No considerar archivos existentes en Drive como prueba de ACK. Reconectar/reintentar y comprobar mismo UUID/carpeta/archivos, sin duplicados.
3. Editar y validar otra revisión durante una entrega. Comprobar que el ACK anterior no oculta la edición y que el Excel final corresponde a la revisión confirmada nueva.
4. Para Pavimentos, verificar además el PDF real (en 10C el contenedor de las pruebas JVM fue simulado). Cámara, reinicio físico, PDF y Drive se aprueban solo con esta evidencia real.

## Capacidad coordinada

No activar nombres V2 o video en producción unilateralmente. Cuando el propietario active la capacidad correspondiente, usar una ficha ficticia para comprobar reserva, máximo 20 caracteres, prefijo estable, reintento/ACK y nombres reales; conservar nombres/bindings históricos. Video únicamente SCAP, con padre confirmado y videoProtocol=1. Anulación remota permanece deshabilitada.

Registro de resultado: fecha, modelo/API real, APK SHA-256, orientación inicial/final, tipo de cámara, UUID ficticio, revisión local/confirmada, resultado y captura del mensaje. Ninguna prueba física 10D está aprobada todavía.
