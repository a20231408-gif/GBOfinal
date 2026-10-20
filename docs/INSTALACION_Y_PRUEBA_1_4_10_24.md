# Candidata 1.4.10 (24): instalación manual y prueba física

**Solo para prueba controlada. La aprobación para campo está pendiente del PDF nativo y del comportamiento físico en la tablet.** No se autorizó distribución a ingenieros ni publicación en Google Play. El resultado de compilación, firma y hash se entrega por separado en RELEASE_CANDIDATE_1_4_10_24.md.

## Actualizar manualmente conservando los datos

1. Antes de actualizar, comprobar en la tablet que GBO es la Release 1.4.9. Anotar cuántas fichas/muestras y entregas pendientes existen y conservar un respaldo local mediante las opciones de exportación actuales. No borrar ni finalizar fichas reales para preparar esta prueba.
2. Copiar a la tablet únicamente la APK candidata indicada en el informe. Abrirla desde Archivos y elegir **Actualizar/Instalar** sobre la aplicación existente. No desinstalar, no limpiar almacenamiento y no restaurar la aplicación desde cero.
3. La firma se contrasta con la APK local 1.4.9 verificada. No se consultó la instalación concreta de la tablet. Si Android rechaza la actualización o informa conflicto de firma/paquete, **detenerse y conservar ambas APK y los datos**. No resolverlo eliminando la aplicación ni sustituyendo la firma.
4. Abrir GBO y verificar versión 1.4.10, usuario, configuración, rutas y registros existentes. Comprobar que siguen presentes las entregas pendientes. No cambiar usuario, PIN, huella de tablet, token, URL ni destino de fichas existentes.

## Preparar la prueba sin enviar datos ficticios a Drive

Trabajar exclusivamente con una muestra nueva identificada como **TEST_1410_PAVIMENTOS**. Antes de su primer cierre/envío, en «Destino de esta ficha» elegir **«Elegir Solo dispositivo»** y confirmar. Verificar el texto **«Solo dispositivo · no se programa Drive»**. Mantener modo avión durante captura, cierre, reapertura y exportación. No autorizar el envío posterior de esos datos a Drive.

Una malla de prueba útil es de 2 columnas × 10 filas, ancho total 7 m y largo 50 m: losas de 3,5 × 5 m. Completar los campos normales y PR coherentes, sin modificar ninguna muestra de campo. Definir la malla antes de registrar daños, pues la validación impide cambiarla después.

## Checklist físico

- [ ] **24 — Grieta D:** localizar una banda cerca de una junta vertical y otra cerca de una horizontal, en losas distintas. Deben verse grietas próximas/paralelas relacionadas con su zona, sin contorno rectangular de reparación ni desplazamiento a otra junta. Registrar también una ubicación interior para comprobar que no salta a un borde distante.
- [ ] **29 — Parche grande:** capturar dos esquinas opuestas de una zona mayor de 0,5 m². Probar un rectángulo alargado de borde y otro interior, con distinta orientación predominante. Deben conservar extensión, ubicación y contorno cerrado con trama; no deben parecer una grieta D. La captura actual es rectangular X/Y, no un polígono libre inclinado.
- [ ] **30 — Parche pequeño:** registrar un parche interior de 0,6 × 0,6 m (0,36 m²). Debe quedar dentro de su losa, a escala y reconocible como reparación. Su tamaño visual no debe cambiar automáticamente código, cantidad o severidad.
- [ ] **22 — Rotura de esquina:** registrar las cuatro orientaciones en losas diferentes, tocando cerca de dos bordes adyacentes de cada esquina. Invertir el orden de dos toques en algún caso. La grieta debe llegar a los bordes correctos sin salir de la losa; un par ambiguo debe pedir corrección. Los valores originales X1/Y1/X2/Y2 deben conservarse.
- [ ] **26 — Sello de junta:** seleccionar el daño y tocar directamente juntas/cruces. Comprobar vista previa, volver a tocar para corregir, deshacer y cancelar: cancelar no debe crear un daño nuevo. Repetir, usar «Confirmar juntas y severidad» y aceptar la validación normal. Debe existir una sola evaluación GLOBAL; cada marca no es otra cantidad PCI.
- [ ] **Guardado y reapertura:** guardar, cerrar la muestra de prueba, salir y volver a abrir GBO sin Internet. Verificar daños, severidades, cantidades, fotos, marcas, coordenadas y correlativos. Reabrir para corregir solamente la muestra ficticia y comprobar que editar no duplica el daño.
- [ ] **PDF nativo horizontal:** usar **Reporte PDF** dentro de GBO y abrir el archivo generado en la tablet. Confirmar página apaisada, figuras equivalentes a pantalla, losas numeradas sin reinicios, correspondencia #ítem/tabla y texto legible. Y/largo aumenta hacia la derecha y X/ancho hacia abajo: es un giro visual de los mismos datos.
- [ ] **Segmentación:** generar además muestras ficticias cortas/largas cuando sea necesario (por ejemplo 10, 60 y 100 m, con PR y malla coherentes), siempre «Solo dispositivo». Los cortes deben avanzar por el eje longitudinal, aprovechar el ancho y conservar la identidad de una falla que continúa. Comparar el rango Y/PR de cada sección; no sumar dos veces una falla repetida por continuación.
- [ ] **Excel y fotografías:** exportar el Excel local y comprobar códigos, severidades, cantidades y coordenadas. Abrir las fotos y verificar su asociación. Revisar el PDF a tamaño real y, si es posible, imprimirlo apaisado al 100 %, comprobando márgenes, leyendas y tablas.

## Resultado a devolver

Indicar modelo y versión Android, versión GBO, nombre de la muestra ficticia, casillas aprobadas/fallidas, página/ítem afectado y una captura del PDF o la pantalla sin credenciales. Conservar el PDF nativo exportado. Los ejemplos de ordenador del 19/10 son simulados y no sustituyen este resultado.

Si aparece pérdida de datos, bloqueo al guardar/reabrir, una figura mal situada o PDF cortado/invertido, detener la aprobación para campo y conservar los datos. No desinstalar para volver a 1.4.9: una APK anterior puede ser rechazada como downgrade. El retorno de versión debe planificarse preservando almacenamiento y compatibilidad, no forzarse sobre datos reales.
