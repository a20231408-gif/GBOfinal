# Esquema de carpetas propuesto antes de implementar

11/10/2026. syncProtocol permanece 3; pathVersion pasa a 4.
RUTA/FAMILIA/ELEMENTO N/CATEGORIA/N fotografías.
Excel contractual consolidado: RUTA/EXCEL_SIC/SIC-XX.xlsx, separado de fotografías individuales. SCAP queda individual dentro de PUENTES/PUENTE N/SCAP/<UUID inspección>, con fotografías por categoría bajo el puente.
SIC-08: FAJAS_CALZADA/FAJA N. SIC-09: ANCHOS_FAJA/ANCHO FAJA N. Son registros independientes; los enlaces históricos no autorizan compartir identidad ni carpeta.
SIC-21: SEÑALIZACION_Y_SEGURIDAD_VIAL/MARCAS, TACHAS, SEGURIDAD_VIAL. SIC-22: SENALIZACION_VERTICAL.
El servidor asigna número por ruta/familia bajo ScriptLock; no ordena ni renumera carpetas por progresiva. UUID y folderId permanecen estables. Android muestra UUID pendiente hasta recibir ACK.
No se mueve ni elimina contenido SIB previo. Cambiar una identidad histórica de esquema exige migración autorizada; reintentos históricos idénticos conservan su recibo.

## Esquema final del checkpoint

La ubicación EXCEL_SIC propuesta arriba quedó implementada después de documentarla. El catálogo completo está en MATRIZ_SIC.md. Los nombres de familia/categoría se normalizan sin tildes a partir de claves estables; materiales, condiciones y subtipos no generan carpetas.

Pavimentos conservan RUTA/EVALUACION_SUPERFICIAL/<tramo>/<superficie>/ y ahora asignan MUESTRA N al UUID de muestra mediante la misma reserva atómica. Se mantienen PANEL_FOTOGRAFICO, HOJAS_CAMPO y DATOS_APP. Sus SIC-14/14A/15/16 consolidados también van a RUTA/EXCEL_SIC/.

Las nuevas identidades se reservan de forma durable bajo ScriptLock antes de escribir archivos. Una interrupción conserva esa reserva para el reintento. Cambios de PR no cambian la identidad; mover ruta/familia requiere reconciliación autorizada. Android valida pathVersion, hash, revisión, IDs y rutas del ACK antes de marcar entregado.
