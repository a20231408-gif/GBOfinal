# Inspección de pavimentos — continuación

Esta entrega continúa el proyecto GBO original (:app, Kotlin, Compose, Material 3, Room y AuthGraph). La opción **Condición del pavimento** está en Inicio y muestra **Versión 1.1-pavimentos**, para distinguirla de la entrega anterior. Incluye medición por puntos, edición y movimiento en el esquema.

## Etapa 1: captura y persistencia

- Cabecera: tramo, ruta, muestra, carril, calzada, progresivas, ancho/largo, ancho de carril, fecha e inspector autenticado; rígido con filas y columnas.
- Esquema con la misma escala en X/Y, origen inferior izquierdo, malla de 1 o 0,5 m, zoom/desplazamiento con gestos, etiquetas código/correlativo y losas numeradas.
- Dos toques para líneas y áreas; un toque para centro del hueco y diámetro obligatorio; selección de losas; registro global sin dibujo para 26/84.
- Aceptar antes de guardar. Coordenadas, diámetro, medidas, severidad, losas, fisura y observación editables. Seleccionar un daño → Mover en esquema → arrastrar → Terminar movimiento. El movimiento conserva las medidas y limita las coordenadas a la muestra. La longitud/área se puede calcular de nuevo dejando vacía la medida manual.
- Sugerencias para huecos con diámetro y profundidad según cuadros 5-6 y 5-16. Los límites ambiguos no generan sugerencia. El inspector confirma la severidad.
- Catálogo completo JSON y descripciones de severidad tomadas de la extracción anterior del manual. Botón para consultar las reglas de medición originales de cada daño con su página de referencia.
- Advertencias por muestra fuera de rango, largo de losa y exclusiones 2/12, 19/20, losa dividida M/A. Límites por área y drenaje 82. Losas 21/33 en juntas: seleccionar ambas losas según indicación en captura.
- Fotografías de cámara comprimidas, guardadas en archivos internos y enlazadas; previsualización al seleccionar el daño.
- Guardado local, autor por daño, cierre de solo lectura; migraciones Room 10→11→12→13. Upsert conserva hijos al editar cabeceras y daños. Se verifica la propiedad real del daño y la fotografía antes de modificar datos.

## Etapa 2: cálculo

- Agrupación por daño/severidad, densidad por área o por losas distintas; globales sin densidad geométrica.
- Motor de corrección PCI con retención fraccionaria de m, iteraciones q/total/CDV y máximo.
- Curvas desde assets/pavement/curvas; puntos vacíos por solicitud. No existe ningún valor deducible inventado.
- Cada curva debe tener estado VALIDADA, escala explícita LOG10 o LINEAL y puntos ordenados [[x,y], ...]. No se extrapola ni se crea un q no documentado. Las correcciones usan q, puntos y estado VALIDADA; las globales requieren valor_global validado.
- Los ejes se configuran por curva: hay gráficos rígidos/URCI lineales. No se fuerza logaritmo a todos los gráficos.
- Mensaje “no calculable” con la curva que falta. Al cerrar se conserva una instantánea JSON de totales, iteraciones, resultado o pendientes.
- Rangos implementados con límite inferior incluido: 85 Bueno, 70 Satisfactorio, 55 Regular, 40 Malo, 25 Muy malo, 10 Grave, menos de 10 Fallado. Validar convención de extremos antes de uso contractual.
- URCI implementado según la decisión del usuario: manual pp.59–60, umbral 5 y factor 9/95, incluyendo la reducción iterativa a 5. PCI mantiene umbral 2 y 9/98. Las curvas permanecen pendientes de digitalización.

## Etapas 3 y 4: reportes

- PDF por muestra: cabecera, esquema vectorial, daños, medidas/coordenadas, fotos, totales, densidades, VD, iteraciones y resultado o motivo pendiente. Paginación para listas extensas.
- Excel por superficie, con hojas Fallas, Resumen, Detalle e Iteraciones. Fallas conserva exactamente las 13 columnas de Hoja2, sin agregar columnas allí. X/Y corresponden al inicio o centro.
- Los datos adicionales se llevan a Detalle. Resumen agrupa por ruta, tramo y superficie. Sin configuración, identifica expresamente la media de muestras exportadas. En “Sección y muestras adicionales” se guarda el área total de sección y las muestras adicionales en Room; el resumen aplica las ecuaciones 4–6 (manual pp.55–56, también URCI según p.60). Requiere muestras cerradas y calculables, al menos una representativa, y áreas consistentes. No mezcla distintas rutas aunque tengan el mismo nombre de tramo. Solo el creador de la configuración puede modificarla.
- Se usa el selector de archivos de Android para guardar y compartir posteriormente. Los destinos locales funcionan sin conexión; elegir un proveedor de nube depende de ese proveedor.

## Etapa 5: usuarios y filtros

- Reutiliza usuarios, login y bloqueo de AuthGraph. No hay nuevas contraseñas ni servicio paralelo.
- El inspector creador puede editar sus borradores; el resto consulta y exporta. Las muestras cerradas son de solo lectura.
- Selector de superficie e inspector, búsqueda por tramo/ruta e intervalo de fechas ISO.
- No se habilita sincronización remota de pavimentos sin definir primero su contrato con el servidor. Los archivos exportados son la salida disponible después del trabajo offline.

## Prueba manual en Android Studio

1. Extraer el proyecto, abrir su carpeta raíz y configurar el SDK local en Android Studio; local.properties se excluye del paquete por ser específico del equipo.
2. Sincronizar Gradle y ejecutar :app. Usar el login ya existente.
3. Inicio → Condición del pavimento → elegir superficie → Nueva muestra.
4. Modificar cabecera; por ejemplo 5 × 46 m para asfalto (dato de prueba, no valor predeterminado). Guardar.
5. Elegir daño 1, marcar dos esquinas, verificar medidas, Aceptar. Repetir con daño 4 y 13; en hueco ingresar diámetro y profundidad medidos.
6. Seleccionar un registro, Modificar, cambiar coordenadas o severidad y verificar que se conserva su fotografía. Eliminar un registro no debe afectar a otros.
7. Probar rígido: configurar grilla, seleccionar losas y Aceptar. Global 26/84 debe registrarse sin dibujo y solo una vez por muestra.
8. Tomar foto, cerrar y abrir de nuevo la app sin conexión; consultar daños e imágenes. Exportar PDF y Excel desde la muestra.
9. Cerrar muestra: queda de solo lectura. Con daños, sin curvas digitalizadas debe indicar no calculable; nunca mostrar un número estimado.
10. Configurar “Sección y muestras adicionales”: indicar el área completa de sección y marcar las adicionales. En evaluación censal no marcar adicionales. Exportar Excel y revisar la explicación en Resumen.
11. Verificar que en Inicio aparece “Versión 1.1-pavimentos”. Si no aparece, se está ejecutando otra copia o un APK anterior.

## TODO pendientes

- // TODO: validar con manual: digitalizar/revisar curvas y constantes globales 26/84, con referencias y escalas por curva.
- // TODO: validar con manual: cantidades equivalentes de huecos mayores a 0,75 m (13) y 0,90 m (85), frente a cantidad 1 exigida por registro. La captura conserva 1 por hueco y bloquea el índice en esos casos.
- // TODO: validar con manual: reglas especiales completas del capítulo 5.4, variantes de punzonamiento 34, conflictos de severidad sobre una misma losa y losas irregulares/largas.
- // TODO: validar con manual: separación Calzada/Faja, límites ambiguos de cuadros de severidad y límites de calificación.
- Pendiente recibir el ejemplo resuelto prometido para añadir la prueba de aceptación numérica. Las pruebas sintéticas de interpolación no son curvas del manual.
- La georreferenciación GPS del nuevo módulo y sincronización con servidor requieren una etapa posterior; X/Y actuales son coordenadas locales en metros.
- La recuperación editable de muestras cerradas no está implementada; se conserva el cierre de solo lectura y su instantánea para no alterar reportes históricos.
- Probar cámara, rotación y gestos en la tablet física. La compilación y las pruebas JVM no sustituyen esa verificación.

## Propuesta adicional

Para una siguiente entrega: respaldo/sincronización con el servidor existente, georreferenciación del origen y rumbo de muestra, reabrir con auditoría y firma de revisión. Estos cambios no se presentan como campos del Excel.
