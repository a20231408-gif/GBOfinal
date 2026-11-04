# Revisión final Android PR #12 — 04/11/2026

Dictamen: **LISTO PARA INTEGRAR en la rama de PR #11**. Esto aprueba integración de fuente; la APK legítima y su aceptación física siguen pendientes del propietario.

Fuente revisada: c78921c0a491e8719dc0860ebe663ad5f892d485. Base confirmada en remoto: f9f6f84402907d4e85a556c01a24f65b602439bb, codex/scap-pavimentos-cierre-20261103. PR: https://github.com/a20231408-gif/GBOfinal/pull/12. Rama: handoff/scap-cierre-final-20261104.

## Alcance y hallazgos

Se leyó el diff completo contra la base: cuatro archivos, versión 1.4.33/47, fixture ZIP y dos documentos. Las correcciones funcionales SCAP ya están en f9f6f84; PR #12 no las vuelve a implementar. No se encontraron defectos bloqueantes ni cambios accidentales de backend, esquemas Room, package, firma, configuración privada o credenciales. Los datos ficticios añadidos están exclusivamente en pruebas; el hash público del certificado histórico se conserva sin cambios.

Se revisaron además las conexiones de la fuente integrada:

- ScapWorkspace mantiene una sola fila principal Exportar SCAP / Ver documentos / Guardar borrador. ScapReviewPanel llama ScapExportPanel con showPrimaryActions=false. Guardar como y ZIP quedan en opciones secundarias. Tras exportar, Abrir Excel y Ver documentos son acciones contextuales deliberadas. El estado compacto de Drive no añade los controles técnicos del panel completo.
- Editor, panel de exportación y documentos se delimitan por UUID. Las consultas usan SCAP/UUID; openTarget rechaza documentos ajenos, recupera la referencia READY vigente y verifica tamaño/hash antes de abrir. Los consolidados SIC mantienen su resolución específica por carretera.
- Exportar consulta savedSnapshot y exige igualdad con la ficha persistida. SAF requiere concesión persistente de lectura/escritura y base verificada; no promueve selecciones antiguas de carpetas individuales a base común. La sugerencia secundaria de carpeta se obtiene del registro actual.
- ScapDocumentNames.folder exige nombre confirmado y genera Puente NOMBRE, sin paréntesis añadidos. Ejemplo: PE-3NG/PUENTES/Puente PRUEBA 7/SCAP - PE-3NG - PRUEBA 7.xlsx. La raíz autorizada puede preceder esa ruta.
- La publicación verifica bytes/hash; el renombrado conserva fuentes y journal, verifica propietario y referencias, y bloquea contenido histórico ambiguo. PR #12 no cambia fotografía, SIC, Pavimentos, outbox ni contratos de sincronización. Guardar borrador no acredita exportación ni ACK.

## Evidencia y límites

Se consultaron los XML preservados de integrada-73-final: 13 clases, 73 pruebas, cero fallos, errores u omitidas. La compilación Kotlin/Java de fuente 47 ya está acreditada. No se repitieron suites ni compilaciones: esta intervención solo añade documentación. La prueba focal de ficha sin nombre y el fixture ZIP conservan sus resultados previos. Evidencias locales: C:\Users\CORE I7\GBO_SCAP_CONTINUACION_20261104.

No se garantiza comportamiento físico de proveedores SAF a partir de Robolectric. No se generó APK ni se operó sobre dispositivos o Apps Script. V21 continúa según el estado acreditado; no se hizo nueva auditoría remota del backend.

## Incorporación por el propietario, sin modificar main

1. Abrir PR #12 y confirmar destino codex/scap-pavimentos-cierre-20261103 y origen handoff/scap-cierre-final-20261104. Su último commit puede ser el checkpoint documental de esta revisión; comprobar que solo añade este informe respecto a c78921c.
2. Confirmar que la base sigue en f9f6f84. Si cambió, revisar exclusivamente los cambios nuevos y los conflictos antes de integrar; no forzar ni sustituir la rama.
3. Marcar PR #12 listo para revisión y, cuando el propietario lo decida, integrarlo en esa base con Create a merge commit. Conservar PR #11 abierto, sin fusionarlo con su base ni con main. No cambiar el destino de ninguno de los PR.
4. En ASUS, con árbol limpio, obtener origin y actualizar la rama de PR #11 mediante git pull --ff-only origin codex/scap-pavimentos-cierre-20261103. Confirmar que c78921c es ancestro de HEAD y que fuente declara 1.4.33/47.
5. Usar configuración original y firma Debug histórica si el destino futuro es el OPPO Debug. Seguir RECUPERACION_ANDROID.md y su init-script legítimo; no usar gboValidationBuild para candidata. Verificar package, versión, certificado histórico y SHA-256 de la única APK generada. No sustituir la firma Debug por Release.
6. Conservar APK y manifiesto privadamente. La instalación/aceptación física requiere respaldo vigente y autorización del propietario; mantener OPPO offline con sus dos QUEUED y Xiaomi excluida. No sincronizar para validar esta integración.

La revisión no autoriza ninguna operación productiva de Apps Script. Sus bloqueos y continuidad permanecen en EXPEDIENTE_CORTE_INSTITUCIONAL_20261103.md y PROGRESO_ACUMULADO_GBO.md.