# Correcciones de continuidad desde 10J

Base: 96d71e0647516e4e89a5ee5b0eb86292d188306f. Rama: codex/observaciones-campo-manual-10I. Alcance limitado a los cinco puntos solicitados; no se repitieron diagnósticos de 10J ni tareas de manual/ADB.

## Implementaciones

| Punto | Implementado | Comprobación |
| --- | --- | --- |
| Revisar y finalizar SIC | Los formularios principales guardan automáticamente el estado actual como borrador al revisar y confirman sobre el mismo UUID. Señalización y formularios históricos reutilizan saveDraft/saveRecord; SIC-08/09/13/34 reutilizan SicFieldRepository; alcantarilla unificada conserva su borrador de edición. El editor técnico guarda la revisión actual y valida la cabecera recién editada, no la anterior. Se conserva la confirmación normal antes de encolar | Tres casos aprobados: persistencia y confirmación de los cuatro formatos autónomos, cabecera editada de alcantarilla con control de revisión obsoleta y borrador incompleto sin outbox |
| OBSERVACIONES | Encabezado/datos con estilo Normal sin bordes ni relleno, sin ancho personalizado ni inclusión en filtro. Se conserva el estilo de otras columnas y el alcance original de fusiones y logos; se incluye la nota en impresión. El backend normaliza también las filas anteriores al empacar el consolidado | Un caso XLSX real aprobado: Normal, inlineStr, filtro contractual, impresión y fusiones; once Node relacionados aprobados, incluidos formatos mixtos, UUID y ACK perdido |
| Filtro de Pavimentos | Lee la ruta de sesión al abrir; permite reemplazarla o dejarla vacía y evita sobrescribir una entrada manual mientras termina la lectura inicial | Componente real aprobado: toma PE-3N de sesión, permite PE-22A o vacío y no modifica la sesión |
| Mapa en dashboard | Nuevo acceso MAPA conectado al callback ya existente que abre AssetOrderScreen y su visor offline/KMZ; conserva el acceso de Herramientas | Botón real aprobado: visible y despacha el callback existente una vez |
| Distancia y precisión GNSS | Indicadores de consulta en GPS opcional de formularios y posición de operador del mapa. Usa la geometría local de la ruta/calzada y muestra precisión registrada; solo estima distancia con posición GNSS válida, reciente, precisión dentro del umbral y correspondencia no ambigua | Tres casos aprobados: distancia conocida ~5.56 m, entradas inválidas/manuales/antiguas/imprecisas/sin eje y cruce ambiguo; sin cambios de geometría |

Los formatos SIC complementarios conservan su flujo propio de guardado y sus vínculos de cabecera; no se convierten en fichas nuevas ni se les inventa una revisión independiente. Los cambios no añaden otra pantalla de confirmación, login ni mecanismo de usuarios.

## Alcance exacto de la finalización

Pulsar Revisar guarda el borrador actual; si faltan datos, la validación los indica y no confirma ni entrega la ficha. Confirmar guarda los valores revisados y crea/actualiza una única intención persistente de sincronización para ese UUID. Encolado no significa ACK de Drive. El control de revisión abierta sigue rechazando una confirmación obsoleta; los borradores automáticos del ViewModel se serializan y no escriben durante la confirmación o mientras hay una revisión pendiente.

## GNSS y orden contractual

La distancia es perpendicular al eje local seleccionado, calculada mediante los motores existentes; no es distancia de PR ni una equivalencia contractual. Se aplica la antigüedad máxima ya existente de QualityConfig (30 segundos) y la precisión máxima de MatchConfig (15 m), respetando el umbral de distancia al eje configurado. La precisión del receptor se muestra como registrada, nunca como exactitud certificada del mapa. Un dato antiguo conserva esa precisión histórica, pero pide actualizar posición y no muestra distancia actual.

Sin coordenadas válidas, hora fiable, fuente GNSS/GPS/RTK, precisión positiva finita o geometría suficiente, no se inventan cero metros ni una precisión. Cruces ambiguos/fuera del umbral se señalan para revisión. No se solicita un nuevo permiso ni se bloquea el formulario por esta consulta.

**No se modificaron** InventoryTechnicalOrder, proyecciones contractuales, números históricos, PR, bindings o fórmulas. Los indicadores no persisten axisMeasureM ni recalculan filas de Excel. Calibrar/validar una proyección para cambiar el orden contractual sigue siendo una propuesta fuera de esta implementación.

## Backend y entrega

CAMBIOS_APPS_SCRIPT_10K.md explica la actualización; docs/parche-apps-script-10k contiene tres módulos completos, sin credenciales. No se publicaron ni se enviaron registros. El estilo de los consolidados remotos requiere que el propietario publique el parche y valide un archivo final autorizado; los existentes no se reescribieron en esta ejecución.

No se genera Release, no se instala APK, no se limpian datos/Drive ni se cambian usuarios, firma, applicationId o versionCode. Se conserva el trabajo previo no versionado.

## Resultados finales y límites

**9/9 casos Android/JVM nuevos y 11/11 Node relacionados aprobados.** Se ejecutaron solo SicFinalize10KTest, SicPlainObservations10KTest, Navigation10KTest y GnssIndicators10KTest, más los dos archivos Node de Observaciones. No se repitió una batería general. Son repositorios/Room, componentes Compose y exportadores reales con datos ficticios en Windows; no equivalen a una prueba en tablet ni a un envío al servidor publicado. Los diálogos de plataforma no se automatizaron de nuevo con el harness bloqueado de 10I.

assembleDebug aprobó. APK ordinaria en app/build/outputs/apk/debug/app-debug.apk; com.tuempresa.inventariovial, 1.4.3 / 17, testOnly=false, firma Debug existente verificada. Tamaño 40 914 991 bytes; SHA-256 **cae729bf5d286204a84cbb9c9fbe8968bf6da972c07f504b52f0907e425f1fb9**. La firma Debug no sustituye la Release histórica; no se instala automáticamente.

Evidencias: campo-10k/evidencia/resultados.json y apk-debug.json. Logs privados locales: app/build/cierre-10k/focalizadas.log y backend-observaciones.log. Código de la APK: 8e3cd5a; los cambios posteriores de cierre son informes/evidencias.

**Implementado en código:** los cinco puntos de la tabla. **Pendiente externo:** publicación manual del parche Apps Script y aceptación física del flujo completo en la tablet por el propietario. **Solo propuesta:** usar una proyección calibrada para alterar el orden contractual/numeración; no se hizo esa modificación. No se presenta el estilo remoto como ya publicado ni el encolado como sincronización confirmada.

Prueba física corta sugerida, a realizar por el propietario en una instalación Debug compatible: editar una ficha ficticia y pulsar directamente Revisar y finalizar; confirmar y comprobar mismo UUID/revisión/outbox. Abrir su Excel local y comprobar OBSERVACIONES plana y fuera del filtro. Verificar ruta inicial/editable de Pavimentos y acceso MAPA. Con un GNSS fresco y eje local válido, revisar precisión/distancia y su desaparición al envejecer el fix. Cualquier envío ficticio a producción requiere autorización previa; esta tarea no lo ejecuta.

Commits por etapas: 204338e y 154bda0 (SIC), b1d1ac4 (Observaciones y parche), 86fef62 (ruta de Pavimentos), 3e7546a (MAPA), 8e3cd5a (indicadores GNSS). Se publicaron en origin en la rama existente. El SHA del cierre documental se comunicará tras verificar el remoto.
