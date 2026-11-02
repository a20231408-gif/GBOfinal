# Validación manual reportada e integración — 01/11/2026

Resultados comunicados por el propietario para GBO **1.4.22 (36) Debug** en su Xiaomi.
No son observaciones nuevas de ADB ni una verificación del SHA instalado. Sustituyen el
estado anterior «0/17» como estado vigente; no convierten los parciales en PASS.
El respaldo privado anterior a las pruebas permanece intacto. No se accedió ni modificó
la tablet durante esta integración. Antes de futuras operaciones que modifiquen sus datos,
solicitar autorización independiente para un segundo respaldo privado del estado actual.
No restaurar el respaldo anterior encima del estado actual.

## P01–P17

| Caso | Resultado reportado | Alcance y siguiente comprobación |
|---|---|---|
| P01 | PARCIAL | Históricos visibles; 30 en listado anual y 14 confirmadas/2 pendientes en Entregas Drive tienen ámbitos distintos. Conciliar sin asumir pérdida ni ACK de todas las fichas. |
| P02 | PARCIAL | Conflicto de identidad comprensible y sin reintento inseguro; colisión real remota pendiente de lectura. |
| P03 | PARCIAL | 17A/B deshabilitados sin obligación roja; revisar ubicación SIC-17 y texto técnico de opcionales. |
| P04 | PARCIAL | Documentos actuales/históricos accesibles; navegación y nombres técnicos confusos. |
| P05 | PASS reportado | Filtros y búsqueda funcionan. No acredita sincronización. |
| P06 | PARCIAL | Fechas/estados filtran correctamente; formato visible inconsistente. |
| P07 | PASS de guardado y búsqueda | Borrador guardado aparece al buscar. No acredita cierre ni subida. |
| P08 | FALLO DE EXPERIENCIA | Diferencia 42+995 → 43+005 visible solo tras seleccionar kilómetros reales. |
| P09 | FALLO PRIORITARIO | Todos 30/34 con históricos; Pendientes 7/11. Cuatro descartados reaparecen: no se demuestra borrado permanente. Restauración poco evidente. |
| P10 | PASS de selección | Individual/masiva; no acredita borrado definitivo ni limpieza. |
| P11 | PENDIENTE | Validación física completa. |
| P12 | PENDIENTE | Validación física completa, incluyendo juntas/severidades. |
| P13 | PENDIENTE | Validación física completa de consolidación. |
| P14 | PENDIENTE | Validación física completa. |
| P15 | PENDIENTE | Validación física completa de archivos. |
| P16 | PASS SOLO DE INSPECCIÓN | 30 visibles, 6 elegibles, 24 protegidos. No se ejecutó limpieza. |
| P17 | PARCIAL | Abre documentos/historial; ubicación conduce a Descargas general. |

## Cambios y su alcance

- **PR #2, A:** revisión completa y selección ASUS 29/29 PASS antes del merge `bc1e307`.
  Mantiene última copia READY durante FAILED/WRITING; reemplazo válido e historial conservados.
  Archivos: LocalFilesUi.kt, LocalFiles148UiTest.kt y entrega de A. Se conserva autoría.
- **PR #1, B:** abierto en borrador en `282b8df50a9a695ecc93c64f6ed0cb460511990f`.
  **No integrado**. Revisión aislada de ACK de Pavimentos/fixture Dashboard con V2=true:
  27/27 PASS, seis clases. Incluye SyncRecovery10DTest 9/9; no se debilitaron aserciones.
  Primer intento: seis errores de inicialización por temporal inexistente, sin ejecutar casos;
  corregido creando el temporal y repetido. Fuente del PR intacta; solo snapshot privado de
  compilación habilitó el flag V2 sin endpoints/tokens. Compatible por inspección con renombrado
  en SyncV3.gs local (HC/M y prefijo fotográfico histórico); HTTP productivo no probado.
- **P09:** DISCARDED_LOCAL cambia estado de captura y updatedAt; conserva UUID/fotos/datos.
  La guarda existente rechaza descarte/restauración si hay outbox, reservas o evidencia
  remitida. No se modificó esa guarda ni la base de tablet. El error estaba en el filtro:
  cualquier captura distinta de DRAFT sin ACK, incluido DISCARDED_LOCAL, entraba en Pendientes.
  Nueva vista **Descartados**, botón **Restaurar borrador local**, etiquetas separadas de
  captura/Drive/copia; descartados y anulados fuera de pendientes activos aun con históricos.
  Conserva protección de entregas y restauración sin programar sincronización.
- **P08:** referencia persistida EXPLICIT_KILOMETRES calcula 10 m inmediatamente sin mostrar
  un error contractual ajeno. Sin procedencia acreditada se muestra también la diferencia
  auxiliar de 10 m, pero no se declara longitud contractual ni se persiste una referencia
  inventada. PR numérico o catálogo sin equivalencia no prueba kilómetro real. La longitud
  medida no se sobrescribe. Queda PARCIAL respecto a inferir automáticamente procedencia.
- **P04/P06/P17:** últimas copias válidas accesibles con A; fechas de listados/ficha en
  dd/MM/yyyy, meses en español; carreteras registradas elegibles sin crear catálogo nuevo.
  Mostrar ubicación informa ruta y limitación, Copiar ubicación copia texto, Abrir archivo
  mantiene URI exacto. No se fabrica un permiso SAF ni una carpeta exacta para MediaStore.
  Intento fallido actual sigue visible y el historial sigue accesible; no se ocultan errores.
- **SIC/SCAP:** diagnóstico de opcionales deshabilitados identifica sus campos como históricos,
  sin exigirlos para entrega. Se conserva la discrepancia SIC/SCAP y su resolución explícita
  ya existente en ScapSicLocation; no se escoge progresiva ni cambia identidad. Conflicto remoto
  continúa bloqueado. Antigüedad del primer intento se distingue de duración real de subida.
- **Daño 26/H con cero:** PavementRules.quantity retorna 0 para GLOBAL, cuya deducción usa
  valor_global sin densidad. Ese cero se imprimía en tablas PDF como si fuese cantidad medida.
  Presentación cambia a «No aplica (global)», conservando gravedad, ocurrencias y cálculo PCI.
  No se reescribe el PDF histórico ni los registros de daños. Validación física pendiente.

## Automatización, artefactos y evidencias

Los resultados de la selección final de integración se registran en el resumen ejecutivo
GBO_INTEGRACION_EQUIPO_Y_OBSERVACIONES.md. Logs/XML por ejecución en
`tmp/integracion-equipo-20261101/`, excluido de Git. No sumar las repeticiones ni los casos
de B aislado como si estuvieran integrados. Ninguna selección equivale a suite histórica.

Fuente propuesta **1.4.23 (37)**, package original, Room 21 sin cambio. Solo se permite Debug
de validación sin credenciales en esta fase. **APK oficial aplazada:** PR #1 sigue borrador;
el encargo prohíbe candidata oficial hasta cerrar esa fase. No instalar el Debug de validación.
Las APK privadas anteriores siguen preservadas; no se modifica su firma ni se distribuyen.

## Contratos revisados y límites

- AutomaticSync conserva WorkManager CONNECTED, señal agrupada, APPEND_OR_REPLACE, recuperación
  periódica y backoff exponencial con jitter. Distingue conexión, cola, ejecución, error,
  conflicto y ACK. BRIDGE_IDENTITY_CONFLICT no admite reintento; no se habilitó envío real.
- Descarte reversible, limpieza local con ZIP/ACK/revisión y borrado remoto son operaciones
  distintas. No se encontró un endpoint administrativo de eliminación de fichas en el router
  revisado. La manipulación interna transaccional no equivale a autorización de borrado remoto.
  Cualquier futuro borrado requiere diseño independiente de tombstone/archivo, identidad,
  autorización, auditoría y rollback lógico. No se implementó endpoint destructivo.
- V21 y ambas raíces permanecen intactos. Inventario real/ACK/colisión y coste de coexistencia
  siguen sin conciliar; legacy/vídeo no enrutados. No hay GO productivo por pruebas locales.

## Próxima validación física (con autorización y segundo respaldo previo)

1. Abrir Descartados; comprobar cuatro fichas conservadas y conteos en Todos/Pendientes.
2. Restaurar una ficha autorizada: vuelve como borrador, conserva fotos y no genera entrega.
3. Referencia kilométrica guardada: 42+995 → 43+005 = 10 m al abrir; referencia contractual
   sin equivalencia sigue advertida; longitud medida e históricos intactos.
4. Revisar fechas/meses, copia válida durante fallo, reemplazo READY e historial. Abrir URI
   exacto y copiar ruta; no interpretar Descargas general como carpeta exacta.
5. Revisar PDF nuevo de daño GLOBAL y continuar P11–P15 sin sincronizaciones ficticias.
