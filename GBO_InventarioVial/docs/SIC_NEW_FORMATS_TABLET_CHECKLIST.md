# Validación física en tablet — nuevos SIC

Pendiente de ejecución y aprobación del usuario. Las pruebas automatizadas no sustituyen esta revisión. Instalar el APK debug sobre una copia de los datos de prueba; mantener el usuario autorizado y comprobar que los datos anteriores sobreviven a Room 15. El APK de validación omite credenciales locales: la sincronización remota requiere una compilación con la configuración autorizada y actualizar el Apps Script con el registro de familias de esta entrega.

- [ ] Iniciar sesión con usuario autorizado; abrir dashboard y verificar grupos, navegación de regreso y formatos anteriores.
- [ ] Verificar borradores, completos y pendientes sync. Un XLSX local o lote PREPARED no debe figurar sincronizado; solo un ACK validado por el flujo existente.

## SIC-08

- [ ] Crear faja con Ruta/Calzada y ubicación inicial del recorrido actual precargadas cuando exista contexto confiable.
- [ ] Revisar PR inicio/fin, distancias y fecha; seleccionar cada tipo 1–6 y sentido C/D/A.
- [ ] Guardar incompleto y con decimal parcialmente escrito como borrador; salir y reabrir; verificar fecha y texto conservados.
- [ ] Editar borrador, revisar faltantes y finalizar; comprobar consulta sin edición accidental.
- [ ] Exportar, abrir y compartir SIC-08.xlsx; verificar diez columnas y encabezado corporativo.

## SIC-09

- [ ] Con una sola faja compatible del recorrido, comprobar herencia automática. Con varias, seleccionar la faja por ruta/calzada/faja/PR.
- [ ] Ver “Datos tomados de SIC-08”; confirmar que Ruta, Calzada, Faja y ubicación se heredan por identidad estable.
- [ ] Capturar anchos con más de un decimal, guardar y reabrir; verificar precisión conservada.
- [ ] Ancho total menor que útil: advertencia, borrador permitido y finalización/exportación bloqueadas.
- [ ] Finalizar y exportar; Excel presenta anchos a un decimal y SIC-08 permanece intacto.

## SIC-13

- [ ] Crear zona con inicio/fin y D/I; no ofrecer “Sin objeto”.
- [ ] Seleccionar 15/16/17/17A/17B/17C/17D y expandir “¿Qué incluye esta clase?”.
- [ ] Confirmar 17D “Zonas de Riesgo Probable”; revisar discrepancia documentada con Tabla III.13.
- [ ] Guardar borrador, editar, finalizar y exportar; verificar nueve columnas, sin Faja, y ambos logos del encabezado dentro de la tabla.

## SIC-34

- [ ] Crear accidente con Faja, ubicación, tipo, nivel y descripción multilínea libre.
- [ ] Verificar niveles 1/2/3 con explicaciones de daños leves, significativos/lesiones y pérdidas humanas.
- [ ] Tipos 1/2/3/4/5/7 finalizan y exportan normalmente.
- [ ] Problemas Geotécnicos y Geológicos: mensaje “Tipo pendiente de equivalencia contractual”, borrador conservado y exportación bloqueada. Nunca exportar un 6 inventado.
- [ ] Reabrir borrador y comprobar descripción y fecha.

## SIC-18A

- [ ] Crear SIC-18 con condición estructural o funcional 3; verificar llamada “Condición mala detectada” y “Completar SIC-18A”.
- [ ] Confirmar que no se crea automáticamente el complemento. Desde dashboard seleccionar una alcantarilla existente.
- [ ] Ver “Tomado de SIC-18”: Ruta, Calzada, PR, distancia, Clase, Tipo y Ojos/Vanos heredados; no recapturarlos.
- [ ] Guardar complemento incompleto y reabrir en historial; mismo UUID, fecha y fotografías de SIC-18.
- [ ] Completar función 1–4, ubicación 1–5, falla 1–7, estado 1–3 y causa 1–4; revisar y finalizar.
- [ ] Añadir varias fotos PANORAMICA/ENTRADA/SALIDA, previsualizar y reabrir; comprobar originales limpios y ausencia de duplicados.
- [ ] Exportar, abrir y compartir; trece columnas. En lote Drive, SIC-18A.xlsx comparte carpeta/identidad con SIC-18.xlsx. Borrador SIC-18A excluido del lote final.

## Señalización global

- [ ] En SIC-22, buscar “R-1” y “Pare”: ver imagen, R-1 y PARE.
- [ ] Buscar “P-1” y seleccionar P-1A/P-1B: comprobar orientación y preview actualizado.
- [ ] Buscar I-2A: ver poste nacional, número de poste y una sola ubicación; sin ubicación final.
- [ ] Cambiar a código manual y sin identificar: no conservar imagen del catálogo anterior.
- [ ] Repetir en edición técnica/historial SIC-22 para verificar el mismo componente reutilizable.
- [ ] Desplazarse repetidamente por todos los grupos y volver; comprobar fluidez, memoria y ausencia de OOM en la tablet.
- [ ] Abrir SIGNAL_ASSET_AUDIT.html y comparar imágenes con MAN_6, especialmente R-5-1/2/3/4, R-45/45A, R-54A/B, P-25B e I-2A.

## Regresión y aceptación

- [ ] Abrir fichas, fotos, usuarios y borradores anteriores; comprobar migración sin pérdida.
- [ ] Validar Puentes/SIC-17/17A/17B, SCAP LOSA/MODULAR y Pavimentos/PCI/URCI existentes.
- [ ] Validar cámara/fotos limpias, archivos generados, abrir/compartir y Drive configurado.
- [ ] Registrar modelo de tablet, Android, observaciones y aprobación o ajustes requeridos.
