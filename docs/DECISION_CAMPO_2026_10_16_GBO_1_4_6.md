# GBO Ruta Clara 1.4.6 (20) - decisión para el 16 de octubre

**Corte: 15/10/2026, 20:20, Lima. GO condicionado a la prueba física indicada abajo, exclusivamente para piloto controlado con respaldo. NO-GO como única fuente de información y sin aprobación de distribución general.**

## 1. Qué está listo y qué falta esta noche

La candidata existente compiló Release y pasó firma histórica, identidad y validaciones de empaquetado. Su SHA-256 vuelve a coincidir: `ae61c79fefdb6e02d9af7a6ffdf7abbaa26a710d42e2ba75fd67db8affbd4ea0`. No se generó otra APK. Se conservaron usuarios, configuración privada, Room y migraciones.

Se revisó la evidencia previa: **53 comprobaciones Android aprobadas**, incluidos UUID/fotos, SIC-22, reapertura del borrador, ediciones, BUSY y recuperación; 9 pruebas backend aisladas aprobadas. La carga simulada confirmó 17 SIC y 11 muestras con sus 28 ACK; **no mide Apps Script real**. Los ensayos Room de integridad/carga usan bases en memoria: no certifican por sí solos persistencia tras reiniciar una tablet.

El código usa Room en disco, cola persistente y cierre/validación transaccional. Sin Internet conserva ficha y medios; con BUSY conserva preparación, revisión y ACK anterior. En 1.4.6 BUSY es transitorio; solicitudes nuevas usan un trabajo único KEEP, reintento exponencial desde 30 s y recuperación periódica de 15 min, sujeta a Android. No se promete entrega inmediata ni rendimiento real todavía.

**Prueba física de aceptación: 5-8 minutos, responsable propietario/ingeniero.** ADB se ejecutó, pero no detectó dispositivos. El PDF nativo sigue sin verificación física; el simulador no pudo inicializar su escritor nativo y la carga usó un sustituto exclusivo de pruebas.

1. En tablet de pruebas autorizada que ya tenga 1.4.6, activar modo avión y comprobar la versión. Si requiere instalarla, aprobar antes actualización con firma compatible y respaldo; no desinstalar ni borrar datos.
2. Crear SIC ficticio `TEST_CAMPO_146`, guardar borrador con foto, reabrir, cambiar un texto y finalizar. Crear muestra Pavimentos ficticia completa con daño y foto, guardar y cerrar. Anotar sus identidades y cantidades.
3. Cerrar la app y reiniciar la tablet. Reabrir ambos: mismo UUID, texto, PR/distancia, estado y fotos legibles; ningún registro duplicado. El SIC-22 vertical debe seguir siendo señal vertical.
4. En la muestra, usar **Reporte PDF**, guardar en Descargas y abrir con un visor: páginas legibles, muestra/PR correctos y fotografía presente. Registrar PASÓ/FALLÓ, versión y hora. Si falla guardado, reapertura o PDF, no aprobar el piloto de esta candidata. Mantener esta tablet offline hasta autorizar expresamente el envío de las fichas ficticias.

## 2. Ordenamiento: cuatro conceptos distintos

| Capa | Resultado comprobado en el código actual |
|---|---|
| Vista local | «Importar KML/KMZ para orden local» proyecta GNSS sobre segmentos; invertir sentido o posición manual solo afecta la presentación. La lista visual puede diferir del Excel. |
| Entrega técnica Android | `GNSS_MAP_MATCH` usa eje calculado o persistido; sin eje utilizable, `CONTRACTUAL_PR` usa PR/distancia. La herramienta separada «Importar ruta KMZ» sí puede alimentar esta referencia; bloquea activación sin calibrar sobre rutas con PR oficiales. |
| Consolidado Excel | Un ámbito por carretera y formato; dentro, calzada, sentido y PR numérico + distancia, creciente/decreciente según sentido; UUID desempata. No ordena por llegada ni por número de carpeta. |
| Carpeta del servidor | Reserva estable por identidad, ámbito y familia. Una inserción retroactiva no renumera carpetas existentes. |

**Caso ejecutado hoy en Drive simulado, con XLSX real leído:** capturar A(PR10), B(PR20), C(PR30), D(PR25), misma carretera/calzada y sentido creciente, produce **filas A, B, D, C**. Carpetas: A=1, B=2, C=3, D=4; reenvíos idénticos conservan IDs y archivos, sin filas duplicadas. En sentido decreciente se invierte la progresiva. El KMZ del operador **no controla el backend**: el importador de orden local solo presenta; la otra herramienta puede influir en el eje enviado, pero no transmite geometría al servidor ni reemplaza PR/distancia del consolidado. Esto aún no demuestra paridad del despliegue real.

---

## 3. Incidencia real: hechos y límites

Las capturas ya aportadas muestran 7 sincronizados, 15 pendientes y 0 errores; el centro añade 2 «Subiendo». Una muestra lleva 3/6 archivos procesados y unos 24 min; otra está en cola unos 18 min; ambas tienen revisión confirmada 0. Dos entregas anteriores, del 14/10, muestran 3/3 confirmados. El reporte de 2/17 SIC y 1/11 muestras corresponde al trabajo informado; los 7 globales no permiten atribuir entregas a hoy.

**Demuestran entrega parcial o pendiente de confirmación, no pérdida ni causa exacta.** 3/6 no es ACK de ficha; cero errores no significa cola vacía. «Subiendo» persistido no prueba actividad HTTP continua ni dos subidas simultáneas. El tiempo mostrado parte del primer intento y puede incluir cola, suspensión y reintentos. BUSY/lock, preparación, commit/consolidación, red y planificación Android siguen siendo hipótesis que hay que separar.

La 1.4.6 muestra «Subiendo» solo con WorkManager RUNNING y clave/revisión activas; exige ACK final válido y revisión coincidente para confirmar. DRAFT requiere finalizar; QUEUED/UPDATE_PENDING esperan envío; UPLOADING requiere contrastar ejecución; ERROR reintenta si procede; BLOCKED, CONFLICT y PENDING_VALIDATION requieren revisión; UNCONFIGURED conserva cola hasta corregir configuración; SYNCED exige confirmación. Dashboard y centro comparten ese criterio. Las capturas de otra versión no prueban el comportamiento corregido.

## 4. Evidencia mínima y servidor: sin terminal ni nuevas capturas

En **1.4.6**: Inicio → **Herramientas → Administración y cuenta → DIAGNÓSTICO DE ESTABILIDAD → Exportar diagnóstico**. Compartir/guardar el JSON mediante una aplicación local compatible. Primero exportarlo offline; después de una ventana de sincronización autorizada, exportar otro a destino distinto para conservar ambos. No pulsar el diagnóstico fotográfico para esta exportación. Actualizar una tablet con datos exige autorización y respaldo previos; la instrumentación no reconstruye tiempos anteriores a instalar 1.4.6.

El JSON aporta versión/hora, `localRecords` y `outboxByFamily` (estados por familia), `validatedWithoutOutbox`, ACK inválidos, archivos preparados/confirmados, recuentos desconocidos, `syncWork` (estados/intentos) y `syncPerformance` (preparación, reserva, envío, espera, commit y ACK). No incluye UUID, fotos, nombres, textos, coordenadas, tokens ni URL privada. Los archivos esperando ACK pueden estar ya transferidos; no sumar tiempos de fases superpuestas. Permite separar bloqueo local, planificación y espera HTTP; **no aislar tiempo de lock o consolidación del servidor**.

Para precisar la causa falta correlación del propietario con **ejecuciones Apps Script del mismo intervalo**, operación, duración, resultado BUSY/error/éxito y versión desplegada, sin payload ni secretos. No se necesitan repetir las capturas. Si producción no registra fases/lock, sus tiempos internos quedan desconocidos; no desplegar instrumentación esta noche por suposición.

**Lectura real efectuada el 15/10, 20:20 Lima:** un GET sin token/cuerpo devolvió HTTP 200; anuncia syncProtocol=3, pathVersion=4, numeración estable, fotografías legibles V2, videoProtocol=1/SCAP_ONLY, nombres de video V2, observaciones SIC=1 y Pavimentos=1. **Capacidades no prueban paridad de código.** Sin acceso al contenido de la versión desplegada no se certifica igualdad: el propietario debe consultar Administrar implementaciones, identificar versión exacta y obtener su código mediante acceso de lectura autorizado; comparar Code, SyncV3/Core, fotos, videos y observaciones con los archivos locales y propuestas. Revisar diferencias de locks, reservas, journals y consolidación antes de cualquier publicación. No confundir código del editor con versión publicada. No se publicó 10H, 10K ni hotfix.

## 5. Protocolo breve para mañana

1. **Antes de salir:** superar prueba física; propietario autoriza uso/actualización; comprobar espacio y batería. Asignar elementos distintos por tablet, sin editar el mismo UUID desde dos dispositivos.
2. **Captura offline:** seleccionar ruta/calzada/sentido, anotar PR y distancia contractual. Usar GNSS y distancia al eje solo con precisión fiable; una estimación KML no sustituye PR. Guardar fotos y comprobar que se abren; guardar/finalizar SIC o cerrar muestra y verificar reapertura local.
3. **Respaldo:** SIC, Herramientas → Exportaciones avanzadas → Generar Excel/ZIP → Guardar en el dispositivo. Pavimentos, Reporte PDF y Excel de superficie. Copiar exportaciones a un segundo soporte autorizado; no son un respaldo completo de Room/fotos/videos. Conservar originales en la tablet y una bitácora independiente de capturas/PR/fotos.
4. **Sincronización escalonada:** habilitar conexión por turnos de tablet, app visible y alimentación disponible. No recrear fichas ni pulsar reintento repetidamente. Ante espera prolongada, conservar datos y exportar diagnóstico; los 15 min de recuperación no son un plazo garantizado.
5. **Conciliación:** por fecha, familia y tablet, cruzar bitácora/UUID, revisión local=confirmada, ACK y archivos/filas realmente presentes en Drive. Una transferencia parcial no cuenta. Registrar diferencias y mantener respaldo hasta conciliación completa. Primer piloto con SIC y Pavimentos, bajo supervisión; cualquier prueba ficticia en producción requiere aprobación previa.

**Decisión vigente:** GO condicionado al ensayo físico y al respaldo, para piloto supervisado. NO-GO como fuente única mientras falten esa aceptación, conciliación y evidencia de sincronización real. Sin distribución general, instalaciones ni escrituras productivas realizadas en esta revisión.

Fuentes verificables: evidencia previa en `docs/hotfix-1.4.6/evidencia/`; comprobaciones de hoy en `docs/hotfix-1.4.6/operacion-16-octubre/` (capacidades sanitizadas y caso A/B/D/C). No se repitieron las baterías aprobadas ni se modificó código funcional.
