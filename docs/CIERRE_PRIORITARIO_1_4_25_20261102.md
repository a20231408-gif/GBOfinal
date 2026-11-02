# Cierre mínimo para piloto offline — 02/11/2026

**GO técnico para preparar un piloto OFFLINE acotado de 1.4.25 (39), sujeto a autorización
de instalación y aceptación manual. NO-GO general/online.** No se accedió a la tablet,
Google ni producción. La instalada declarada sigue siendo 1.4.24 (38).

| Pregunta | Resultado y límite | Aceptación mínima en tablet, después de autorizar actualización |
|---|---|---|
| ¿Puede filtrar? | Sí: búsqueda, familia, carretera, progresiva y fecha combinadas, orden cronológico y estados. Se conserva la implementación. Todos incluye borradores y oculta descartados por defecto; no existe un filtro exclusivo ACTIVE. | Combinar filtros y comprobar dos fichas conocidas, sin editar. |
| ¿Puede seleccionar y descartar? | Sí, SIC/Pavimentos DRAFT exclusivamente locales. La selección operada se limita a los borradores actualmente visibles seleccionados; revisión revalidada en transacción. | Descartar solo un borrador de prueba identificado; comprobar que el vecino sigue activo. |
| ¿Puede descartar todos? | Sí: «Descartar todos los borradores visibles», limitado por los filtros. No significa toda la base. No salta silenciosamente protegidos: uno protegido o cambiado cancela el lote completo, incluida su auditoría. | Dos borradores de prueba visibles, uno fuera del filtro; descartar los dos y conservar el tercero. |
| ¿Puede restaurar? | Sí, individualmente desde Descartados, con mismas guardas; conserva UUID, relaciones, originales y revisión incrementada. No restaura automáticamente otros registros. | Restaurar uno, reabrir GBO y comprobar el otro aún descartado. |
| ¿Qué protege? | SCAP/padre vinculado, origen/aporte SIC relacionado, fotos/documentos compartidos conocidos, registros no DRAFT, cualquier outbox incluso histórico, ACK/reserva, staging/manifest no verificable, evidencia enviada, recorrido activo y Pavimentos de otro inspector. | Una ficha protegida cancela el grupo sin modificar ACK/outbox ni las demás fichas. |
| ¿Qué limpieza queda fuera? | No hay borrado general definitivo ni eliminación remota. Limpieza de copias confirmadas existe con respaldo/ACK/guardas; `16b21b3` agrega protección cruzada antes y después del ZIP. **No usarla en este piloto**: falta aceptación física del proveedor/SAF. No se habilitó un borrado nuevo. | No ejecutar limpieza de confirmados ni acciones irreversibles durante este piloto. |
| ¿Offline sin entregas inesperadas? | Descartar/restaurar no crea intenciones ni modifica ACK. Para captura: modo avión y Wi-Fi apagado, elegir **Solo dispositivo antes del cierre** en cada ficha nueva. La ausencia de elección mantiene el comportamiento Drive anterior; las colas previas se conservan y pueden reanudarse al volver online. | Revisar destino de cada ficha y mantener aislamiento; no cerrar fichas cuyo destino no se haya comprobado. |
| ¿Qué falta para online? | Inventario productivo paginado y estable, conciliación de journals/raíces/ACK y colisión SCAP, OAuth efectivo, permisos, legacy/vídeo/coste y corte original autorizado. P04/P17 siguen con aceptación parcial y P11–P15 pendientes. V21 no fue modificado. | Resolver bloqueos backend y validar físicamente sincronización/ACK con autorización separada. |

Corrección mínima: `LocalDraftDiscard.kt`, `InventoryDao.kt`, `LocalFilesData.kt`.
Guardas nuevas para relaciones SIC en ambas direcciones y referencias documentales/fotográficas
entre fichas, incluidas originales SIC/Pavimentos y fuentes retenidas tras limpiar copias públicas.
Comparación por referencias almacenadas; no acredita consumidores externos o alias desconocidos.
El descarte conserva bytes y relaciones, nunca borra ni reenvía. No se rediseñaron filtros,
nombres, backend ni interfaz. Se preservó íntegro el commit `16b21b3`.

Pruebas: **137/137 casos únicos PASS**, 24 clases, cero fallos/errores/omitidos, 1m20s.
Selección integrada anterior de 131 más seis casos: origen/aporte SIC, documento compartido,
foto original SIC/Pavimentos compartida, rollback con ACK y ambos botones con filtros combinados.
Incluye selección masiva, persistencia tras reabrir Room, restauración individual, UUID/fotos,
ACK/revisiones, V1/V2, SCAP/SIC, longitud y PCI. Mocks V2 sin credenciales ni HTTP; no suite histórica
completa ni aceptación física. Evidencia nueva: `tmp/cierre-prioritario-20261102/final/xml/`
y `resultados.json`. Reproducción inicial: 10 casos, 2 fallos de protección; ejecución intermedia:
28 casos, 1 fallo por IDs repetidos entre fixtures UI, corregido aislando sus identidades.
No sumar estas repeticiones a los 137. Limpieza compartida de 16b tiene sus dos regresiones dentro
de la selección. La evidencia privada previa acredita dos restauraciones individuales SIC,
no una migración; no se cambiaron estados reales (ver informe de cierre 1.4.24).

Candidata privada Debug original, compilada con `gboValidationBuild=false`, sin perfil de mocks:

`C:\Users\ASUS\GBO_CANDIDATA_PRIVADA_20261102_1_4_25_39\GBO_1.4.25_39_DEBUG_ORIGINAL_PILOTO_OFFLINE_NO_INSTALAR.apk`

- SHA-256: `b07e424d5411750aa69136b2c6de772406893feae5f4b0196f423333ebb4bc0c`.
- Fuente Android: `cc9d9b4373d8766b937be7d91aceeaf478da62b1`; fixes `daad19e` y `16b21b3`.
- Package `com.tuempresa.inventariovial`, versión 1.4.25 (39), firma Debug histórica
  `d2e54cb086aa731ef046be300977d0b16c0d8f66fdcb08d0d9cfd2038bf0d872`, firma/alineación 16 KiB PASS.
- Identidad compatible con APK 1.4.24 acreditada; Room 21/esquemas sin cambios. No se probó instalación
  en esta intervención ni actualización sobre Release de otra firma. Código 39 nuevo en integración;
  sin tag 1.4.25 observado, sin consulta a una tienda.
- Endpoints/token originales contrastados offline con baseline acreditada, sin valores publicados;
  cero endpoints Google Script ajenos. V2=true, SIC17A/B=false. Auditorías junto a la APK, ACL privada.
- Sin Release nueva, sin instalación/distribución. APK con configuración privada excluida de Git.

Próximo paso: propietario autoriza actualización de esta APK después de confirmar respaldo actual;
ejecutar únicamente la aceptación reversible anterior sobre fichas de prueba locales identificadas.
Este GO acotado no autoriza distribución ni sincronización online.
