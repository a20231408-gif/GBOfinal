# Corrección SCAP de esta iteración

F.1 inicial persistido: LOSA 17 / MODULAR 10. Cada elemento mantiene UUID/ID estable, templateCode/exportCode/defaultName, customName/customCode/globalDescription y applicability en scap_values. F.1 permite nombre, código de impresión y descripción observada; plantilla solo No aplica, manuales se retiran con confirmación conservando fotos/observaciones. General observations permanecen accesibles.

G.1 usa la misma colección, incluyendo UNSET. Permite código de exportación, nombre, metrado, unidad/factor y porcentajes 0–5. Null inicial en todas las calificaciones, null → 0 solo en operación matemática, no en almacenamiento ni XLSX. Suma ≠100% no bloquea guardar/export/Drive; invalida agregado. Todo vacío no produce clasificación inventada. Reactivación conserva datos. Manual agrega simultáneamente F.1/G.1, sin catálogo duplicado independiente.

Factor de importancia explícitamente vacío permanece pendiente: el modelo numérico legado no convierte ese vacío en un factor efectivo 0 para calcular ni lo muestra como tal. Códigos o fecha fotográfica que el usuario vacíe también se identifican como pendientes. El código para cada hoja sigue editable sin modificar el ID/UUID lógico.

C.5: apoyos base 1/2, dropdown Izquierda/Derecha con LEFT/RIGHT; +Apoyo3 conserva la numeración; soporte adicional se imprime en bloque clonado. La UI no permite retirar apoyos base de nuevas fichas canónicas.

F.2 canónico: solo panel fotográfico, encabezado NRO/FECHA/DESCRIPCIÓN/DIGITAL; Nombre/Tipo editable con defaults completos extraídos, descripción, fecha capture/editada, imagen. Nuevo registro vacío guardable, cámara/seleccionar/reemplazar/remover/preview/reordenar; reemplazo conserva UUID y originales. Caption NOMBRE: DESCRIPCIÓN; relación de aspecto completa, sin recorte ni estiramiento. Se conserva el camino histórico para fichas anteriores sin tipo LOSA/MODULAR.

Pendientes: id, label, severity, code MISSING_VALUE/INVALID_VALUE y ScapFieldRoute; UUID/clave. Clic cambia sección, desplaza lista y campo, enfoca cuando es texto y resalta 1.8 s. No aplica excluye pendientes dependientes y G.1. No se generan destinos parseando texto de errores.

Entrega: DRAFT siempre guardable si persistible; valores técnicos inválidos conservan texto pero bloquean export. Falta de datos requiere confirmación de export/Drive. Metadata local Excel revision y completion.status, syncStatus separado. ACK de revisión vieja nunca borra edits. Destino completo v2 conservado; destino incompleto v1 existente con advertencia/destino visible. No hubo despliegue remoto. Una ficha incompleta subida mantiene edición local y referencias Drive.

Pruebas: Baseline real: 370 pruebas; 355 PASS y 15 FAIL. Última ejecución registrada: 380 pruebas; 365 PASS y 15 FAIL; nuevos fallos: 0; identidades anteriores ausentes: 0. Evidencia JUnit/XML, captions de source, ambas fuentes por hash, dimensiones/merges/styles/defined names, continuidad UUID/ratings/supports, blanks y JPEG original. Validación física en Excel/tablet pendiente.

Límites técnicos de plantilla que siguen vigentes: 31 elementos G.1, 32 registros fotográficos, dos tramos, tres pilares, una junta, 15 puntos de perfil, un croquis/tipo. Los límites se muestran como errores técnicos explícitos, no se truncarán registros silenciosamente.
