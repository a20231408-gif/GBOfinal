# GBO — observaciones de campo y ASUS, 30/10/2026 (Lima)

## Base y evidencia revisada

Repositorio local C:\GBO. Base dbe7247, rama inicial codex/migracion-asus-20261029. Rama de trabajo creada: codex/observaciones-campo-asus-20261030. Sin commit, push, instalación, distribución, peticiones Drive ni despliegues.

Se leyó la conversación GBO SERVIDOR y sus dos adjuntos recuperados: INFORME_PREPARACION.md y MIGRACION_ASUS.md. También la documentación local de migración, P0 real 1.4.17, Pavimentos P0 1.4.18 y nombres/carpetas. Los informes adjuntos describen la preparación anterior a publicar; Git local acredita ahora el commit de migración dbe7247. El adjunto MIGRACION_ASUS.md y su copia del repositorio tienen hashes diferentes: se conservaron ambas fuentes y no se sustituyeron.

Fuentes presentes: Android 1.4.18 (32), Room 21. La candidata histórica y su firma descritas por Huawei no se han verificado físicamente en ASUS. El cambio local previo AGP 9.4.1→9.5.0 / KSP 2.2.10-2.0.2→2.3.6 se conserva sin atribuirlo a este trabajo. No se modificaron otros archivos de compilación ni configuración privada.

## Diagnóstico ASUS

- JBR disponible bajo Android Studio; JAVA_HOME no estaba definido en terminal. Se asignó solo al proceso de las comprobaciones, sin cambiar configuración global.
- local.properties existe. server.local.properties, drive.local.properties y keystore.properties no existen. No se mostraron valores privados ni se inventaron endpoints, credenciales o firma.
- Gradle compila Debug con el SDK local y dependencias ya disponibles, sin descargas durante estas comprobaciones.
- Python no está disponible como comando utilizable: el benchmark Python documentado no pudo iniciarse. Se utilizó Gradle directamente.
- validateReleaseConfiguration falla con RELEASE_BLOCKED_MISSING_SIGNING_KEY, sin firma Debug alternativa. Tras restaurar firma habrá que validar también servidor y Drive; este primer fallo no certifica lo demás.
- No se ha medido un benchmark ASUS/Huawei comparable. Los 21 s de la última operación conjunta son una ejecución caliente de compilación y tests, no una compilación fría ni el incremental controlado de Huawei. No se midió memoria.

## Matriz por fases

| Fase | Estado y trabajo concreto |
|---|---|
| 0. Reconocimiento | Completado: evidencia, rama, versión, diferencias previas y presencia de configuración. |
| 1. Recuperación Drive / Puentes | Ya existían nueva negociación de capacidades, conservación de UUID/revisión/staging, reintento de commit y ACK estricto. El ensayo SCAP actual queda bloqueado por nombres V2 deshabilitados al faltar configuración privada; no demuestra un fallo remoto. Restaurar configuración legítima y repetir el ensayo local antes de proponer cambios de recuperación. No se tocó backend. |
| 2. Filtros y descarte | Implementados en Mis registros: año/mes/día desplegables y filtros Borradores/Enviados/Sin confirmar. Enviados exige ACK completo de revisión actual. Selección múltiple y todos los borradores visibles, con confirmación y descarte reversible. |
| 3. Longitud lineal | Confirmación del propietario: 42 km+995 → 43 km+5 = 10 m. Implementado cálculo explícito en alta y edición de elementos lineales, con validación de ruta/calzada/sentido/offsets. Primera fase AUXILIAR: no persiste otra referencia ni sustituye longitud contractual o medida guardada. Falta diseñar su persistencia y exportación coherentes antes de declarar cerrada la corrección integral. |
| 4. Nombres/rutas | Reservas V2 y nombres/carpetas legibles ya existen. Cuatro regresiones locales de nombres aprobadas; se conservan rutas congeladas y reservas. La frase larga solicitada para cada foto no se ha impuesto al protocolo V2: requiere una convención compatible y prueba de cliente/servidor. No se renombró histórico. |
| 5. Junta 26 | Función ya incorporada: ocurrencias L/M/H, conservación histórica y global PCI manual independiente. Pruebas locales de UI, migración y Pavimentos aprobadas. No se duplicaron funcionalidades ni se inventó agregación de severidades. Pendiente validación física. |
| 6. Migración Google | Preparada en MIGRACION_GOOGLE_PREPARADA.md para carpeta y cuenta propietaria; sin ejecución ni cambio de raíz, URL, permisos o propiedades. |

## Alcance de la nueva interfaz

La fecha viene de surveyDate en SIC y el registro vial vinculado de SCAP, y de fecha en Pavimentos. Reconoce ISO y dd/MM/aaaa con fechas válidas. No usa fecha de edición, exportación o subida. Una fecha desconocida permanece al limpiar filtros y no coincide con filtros activos. El orden técnico existente se conserva.

El descarte múltiple admite SIC y Pavimentos DRAFT. SCAP queda excluido por su vínculo e identidad propios. El grupo revisado conserva revisiones y se ejecuta en una transacción: si cualquier fila cambió, tiene sincronización/preparación/recibo anterior o incumple las reglas individuales existentes, todo el grupo se revierte. Cada ficha puede restaurarse con sus UUID y originales. No hay borrado físico ni remoto; no se ejecutó descarte alguno sobre registros reales. No se presenta esta fase como eliminación definitiva ni limpieza general de entregas.

La selección se aplica a los borradores del filtro visible; cambiar filtros no autoriza descartar registros ocultos. El diálogo congela identidades y revisiones. El resto de las comprobaciones individuales, incluido propietario de Pavimentos, se conserva.

## Validación y fallos conservados

Última ejecución: assembleDebug + 28 pruebas, 0 fallos, 0 errores, 0 omitidas. Clases: ContractualLength111 (6), LocalDraftDiscard10C (7), PavementJointPreviewUi (2), PavementMigration21 (1), PavementP0Additional (5), ReadablePhotoNames10 (4), RecordDateFilter (2), UnifiedRecords149Ui (1).

Comando de pruebas: Gradle offline con tools/migracion/pruebas.init.gradle y selección explícita de esas ocho clases. Crear tmp/tests antes de usar ese init script en una laptop nueva. No se ejecutó instrumentación ni Google. Evidencias locales ignoradas por Git: tmp/asus-20261030/final/resumen.json y final/xml.

Primera ejecución de cuatro clases: 18 casos, 16 aprobados y 2 fallidos. Se preservan XML en tmp/asus-20261030/baseline/xml. Diagnóstico adicional en diagnostico-sync/xml:

1. SCAP: «El panel oficial SCAP requiere nombres fotográficos V2». Con server.local.properties ausente, el flag se genera false. La aserción de recuperación con pérdida de ACK no puede llegar a la etapa esperada. Las otras siete pruebas de esa clase aprobaron. No se cambió la bandera ni se omitió el caso para producir un PASS.
2. Pavimentos: el ensayo escribe evidencia usando gbo.p0.evidence y se ejecutó inicialmente sin el init script. Repitiendo con el ejecutor documentado y su directorio temporal creado: cinco casos aprobados, incluido ACK V21 sin PR. El primer reintento con el init falló por tmp/tests inexistente; también se conserva como fallo de preparación, no como regresión funcional.

Las aserciones de ambos ensayos ahora incluyen estado y mensaje al fallar; no se cambiaron sus condiciones de aprobación. La suite histórica completa no está acreditada; siguen además los cinco fallos históricos documentados en Huawei. No se certifica una entrega real, recuperación de fichas reales ni funcionamiento en tablet.

APK generada: app/build/outputs/apk/debug/app-debug.apk, SHA-256 2d10ddc875d51c98b44ac41f85ac19bf10d6e47574f2aaae08a99f7700586f90. Solo comprobación local, sin instalación/distribución; conserva numeración 1.4.18 (32), no representa una nueva Release firmada.

## Próximos pasos que siguen pendientes

1. Traslado privado legítimo de configuración/firma histórica desde el respaldo, sin pegar secretos aquí. No reemplazar archivos existentes sin conservarlos.
2. Repetir recuperación SCAP y validación Release en esa configuración; comparar firma histórica antes de preparar otra candidata distribuible.
3. Incorporar referencia kilométrica persistida por registro y su procedencia en JSON/Excel, sin reinterpretar PR históricos ni marcarla como longitud contractual certificada. El auxiliar ya resuelve el ejemplo de campo.
4. Acordar y validar la convención de nombres largos sin cambiar reservas/ACK ni rutas preparadas; mantener las copias históricas.
5. Validación física de filtros, juntas, archivos y selección en una tablet de pruebas con datos ficticios.
6. Completar inventario y ensayo de migración aislado; presentar plan con cuentas/raíz identificadas por canal privado antes de solicitar autorización de ejecución.
