# Auditoría operativa del backend original — 01/11/2026

**Resultado provisional: NO-GO. Inventario real bloqueado por consentimiento OAuth.**
No se ejecutaron entregas, reintentos, migraciones, cambios de permisos, cambios de raíz,
publicaciones ni activación del enrutamiento dual. No se modificó fuente de Apps Script.

## Evidencia obtenida en esta intervención

- Base local: `dda0e2398933d8853e963fa95361a81aaddd351d`, rama
  `codex/observaciones-campo-asus-20261030`. Árbol inicialmente limpio. Android 1.4.22 (36).
- El editor del proyecto original `GBO_Inventario` muestra la cuenta legítima GBO.
  Esto acredita la sesión de edición; aún no acredita la identidad ejecutora en Drive.
- Se leyó íntegramente `PreflightReadonly.gs` del editor, sin editarlo. La función
  `gboPilotInventoryReadonly` verifica la identidad por Drive.About, pagina con nextPageToken,
  lee journals/records/bindings y compara dos listados por ID/nombre/modifiedTime.
  No se encontraron escrituras Drive ni de propiedades en ese archivo.
- Se solicitó una ejecución de esa función. Google mostró **«Se requiere autorización»**.
  Se pidió al usuario completar el consentimiento oficial. La última comprobación del
  registro no contiene resumen ni filas. No se inició una segunda ejecución.
- Tres lecturas de metadatos mediante el conector Drive devolvieron **anyone:writer** en
  `1.- ARCHIVOS CAMPO`, `05 V CAMPO` y `ARCHIVOS DE CAMPO`. No se alteraron los permisos.
  El conector no devolvió en estas respuestas padres, capacidades ni detalles de herencia:
  no permite concluir qué permiso es directo/heredado, ni acredita escritura como GBO.
- Una consulta de hijos del destino devolvió cero resultados por el conector. **No se
  interpreta como carpeta vacía ni como ausencia acreditada de registro**, pues la cobertura
  de acceso de ese conector no quedó verificada. La consulta de metadatos de la raíz antigua
  devolvió `access_not_verified`; no sustituye al auditor ejecutado como GBO.

## Inventario y conciliación: resultados todavía no obtenidos

| Dato solicitado | Estado verificable |
|---|---|
| Total COMPLETE, PENDING, ROLLED_BACK, desconocidos e ilegibles | NO OBTENIDO; no significa cero |
| Inconsistencias de ACK/revisión y nombres duplicados | NO OBTENIDO |
| Segunda lectura estable y tiempos reales | NO OBTENIDO |
| UUID/revisiones/bindings/ACK por cada raíz | NO OBTENIDO |
| Registro remoto que colisiona con SCAP «Puente a - Prueba 2» | NO IDENTIFICADO |
| Uso y pendientes reales legacy/vídeo | NO OBTENIDO |

La condición de conflicto demostrada por código es un bridgeCode ya presente con identityId
distinto. El nombre visible no demuestra esa colisión. No cambiar UUID, numeración ni raíz
para sortearla. Se necesita contrastar en evidencia privada el paquete histórico congelado
con los registros remotos: key, recordId, identityId, bridgeCode, routeCode, revisión,
contentHash, reserva/binding y ACK. No hubo lectura ni modificación de la tablet.

## Alcance insuficiente del auditor actual para aprobar modo dual

El auditor existente cubre una sola raíz (ROOT_FOLDER_ID original); no se debe cambiar ese
literal para auditar el destino. No incluye un inventario íntegro de staging, vídeo o legacy;
cuenta reservas fotográficas sin leer su identidad/contenido. Tampoco emite bridgeCode ni
routeCode de los registros, necesarios para resolver la colisión. La estabilidad compara
metadatos de dos listados; no es una segunda lectura completa de cada JSON ni un bloqueo
contra nuevas escrituras de tablets. Las comprobaciones ACK son de metadatos del registro,
no una verificación de todos los bytes publicados en Drive.

Tras obtener su salida, hace falta complementar con lecturas directas autorizadas de ambas
raíces, incluidas reservas/staging/vídeo y los registros relacionados con la colisión. Si se
necesita instrumentar un auditor adicional, preparar primero una propuesta estrictamente
de lectura; no sustituir módulos productivos ni publicar otra versión por esta auditoría.

## Contraste con la propuesta local

`backend/candidato-original-reanudable/DeliveryRootRouting.gs` sigue local y desactivado.
Lee ambos registros completos por solicitud bajo ScriptLock, incluidos JSON record/binding/
journal/pin. Tiene presupuesto cooperativo de 85 segundos; una llamada Drive individual no
se cancela a ese límite. Sin inventario/latencias reales no puede estimarse capacidad ni
declararse que ese coste sea aceptable. El tiempo del auditor, cuando exista, será orientativo:
usa llamadas distintas y una sola raíz, no es un benchmark del enrutamiento completo.

En modo dual solo admite reserva V2, staging V3 y commit V3. Legacy/vídeo quedan bloqueados.
No activar mientras no se conozca su uso y no exista una decisión de compatibilidad.
Conserva autoridades antiguas; detecta colisiones globales e identidades repartidas. Staging
SCAP sin identidad ni autoridad requiere conciliación. No mover ACK ni reservas a la nueva raíz.

## Plan de conciliación (condicional; no inventario de casos reales)

| Estado encontrado | Acción necesaria antes de habilitar operaciones |
|---|---|
| COMPLETE con record/ACK/revisión/hash coherentes | Conservar raíz/identidad/binding; verificar asociación y no reenviar |
| COMPLETE con record ausente, ilegible o ACK discordante | Bloquear esa entrega; contrastar JSON/IDs/evidencia local; no fabricar ACK |
| PENDING con continuationVersion=1 | Conciliar revisión/hash/intenciones/IDs/operaciones y staging; proponer continuación exacta para autorización posterior, sin ejecutarla ahora |
| PENDING histórico sin versión de continuación | Mantener bloqueado; preparar recuperación específica revisada, sin adopción automática |
| ROLLED_BACK | Verificar operaciones/autoridades residuales antes de permitir nueva entrega; conservar evidencia |
| Ilegible, desconocido, duplicado o autoridad en dos raíces | Preservar; identificar alcance; no interpretar fallo como ausencia. El candidato puede bloquear más solicitudes si un registro ilegible impide leer una raíz completa |
| bridgeCode compartido por UUID diferentes | Determinar si son fichas distintas, duplicación histórica o metadato incompleto; decisión humana y corrección específica preservando historial |
| Legacy/vídeo preparado o pendiente | Mantener camino compatible; no activar modo dual que bloquearía esas acciones |

## Acciones exactas pendientes

1. Usuario completa el consentimiento oficial del original con GBO, sin compartir códigos.
   No aceptar ámbitos inesperados sin revisar; no eludir advertencias de seguridad.
2. Recuperar salida completa con marcador final; guardar filas reales fuera de Git, con
   hashes y conteos comprobados. Si la primera ejecución no arrancó, ejecutar una sola vez
   tras verificarlo. Comprobar segunda lectura y cambios concurrentes.
3. Ejecutar `gboPilotDestinationReadonly` ya existente: identidad de runtime, cadena de
   padres, capacidades y permisos. Completar detalle de herencia si la respuesta lo omite.
4. Complementar las lecturas descritas y resolver la colisión concreta sin reintentar.
5. Elaborar conciliación por journal real y coste observado, verificar compatibilidad
   legacy/vídeo y preparar diff/rollback del mismo proyecto/despliegue.
6. Presentar por separado cualquier restricción de permisos públicos y el eventual corte
   para autorización explícita. Ninguna de esas modificaciones está autorizada aquí.

Producción: V21 es el último estado previamente acreditado, no revalidado por esta ejecución.
No se autoriza GO por recuperar acceso. Respaldo privado de tablet verificado anteriormente;
P01–P17 continúan **0/17, sin ejecutar**. No hubo compilaciones ni pruebas Android nuevas.
