# Guía breve para ingenieros — versión 9B

## Registrar y revisar

Abra el módulo: verá sus elementos, número local, tipo, progresiva, condición y estado. Los borradores e históricos siguen disponibles. Use **Nuevo elemento**, **Detalle**, **Editar**, **Fotografías** o **Revisar y finalizar**. El número local permanece asociado al elemento; la identidad Drive, cuando existe, se muestra aparte.

Las tarjetas de condición muestran descripción y selección: verde favorable, amarillo intermedia y rojo desfavorable según el catálogo de la ficha. SCAP estadístico y severidades de pavimento conservan sus escalas propias.

**Guardar borrador** conserva la captura incompleta. **Revisar y finalizar** muestra un resumen; corrija los pendientes antes de confirmar. Abrir el resumen no envía nada. Después de validar, **Entrega pendiente** significa que la tablet conserva una revisión en cola; **Sincronizado** exige confirmación del servidor. Error o conflicto conserva la edición y requiere revisar el mensaje.

## Fotos y videos

Las categorías aparecen desde el inicio, incluso con contador cero. Incorpore varias fotos, amplíe una con **Ver fotografía**, reemplace o retire cuando corresponda. GENERAL conserva fotos históricas. Los originales y UUID se mantienen. Los paneles SCAP siguen nombres de la plantilla elegida.

Los videos aparecen en el panel multimedia por categoría. Puede grabar/importar MP4, WebM o 3GP de hasta 32 MiB, reproducirlos sin Internet y consultar su estado. **Pendiente del backend** es el estado esperado de esta versión: la subida remota está deshabilitada hasta actualizar y validar Apps Script. **Reintentar** conserva el UUID. Retirar un video conserva su original y no elimina la evidencia remota.

Los cierres de detalle/fotos y exportaciones corregidos en Prompt 9 fueron aceptados manualmente por el usuario en su tablet. Las funciones nuevas requieren la lista `PRUEBAS_TABLET_9.md`.

## Orden y GPS

En **Orden de elementos**, seleccione la ruta e importe KML/KMZ para su orden local. Revise el sentido y el orden de segmentos del archivo; puede invertir el sentido. La distancia se acumula sobre las líneas. Los huecos no se rellenan con distancia recta. Puede usar **Capturar GPS para orden local** para ubicar al operador.

Un punto sin GPS, alejado, impreciso o en un cruce queda señalado para revisión. **Revisar posición local manual** permite introducir metros acumulados sobre el eje. Esto modifica únicamente la presentación local: conserva UUID, revisión y nombre/carpeta Drive. La georreferenciación contractual sigue siendo externa.

En **Umbrales GNSS**, la precisión ±m es una incertidumbre estimada. Los valores por defecto son: eje 30 m; matching 15 m; recorrido 10 m; separación 3 m; aviso 1 m. La pantalla muestra los valores configurados y explica su efecto. Un umbral menor no mejora físicamente el receptor.

## Sesión y carpeta técnica

Los botones rojos **Finalizar sesión de campo** y **Cerrar sesión** conservan las comprobaciones existentes. Finalice un recorrido abierto antes de salir. Cerrar sesión no borra datos, fotos, videos ni entregas pendientes.

`._GBO_SYNC_V3` contiene staging, asociaciones de identidad y journals técnicos que permiten recuperar entregas y evitar duplicados. No editar, borrar, mover ni renombrar esa carpeta ni sus archivos. El parche de videos añade un registro técnico separado cuando se active. Consulte las fichas en las carpetas de familia y **EXCEL_SIC**.

La APK Debug de validación omite endpoints/credenciales. Comprueba interfaz y trabajo offline; no prueba entrega real. No desinstale ni borre datos de la aplicación de campo para instalar una APK con otra firma. Use el dispositivo/perfil de validación previsto por el responsable.
