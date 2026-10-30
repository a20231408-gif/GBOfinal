# Migración ASUS — preparación terminada, sin publicar

Rama local: `codex/migracion-asus-20261029`. HEAD/base: `607d499300859321aa7a282b10ee7dfde17d14f8`. No hay commit de migración ni rama remota nueva. Coincide en commits con `origin/checkpoint/gbo-2026-10-23`; los avances locales pendientes no están aún en GitHub. Sin upstream en esta rama antes de la autorización.

## Propuesta concreta

1485 archivos en el índice: 361 nuevos, 121 modificados y 195 retirados exclusivamente del seguimiento frente a HEAD. La lista completa está en [ARCHIVOS_PROPUESTOS.md](ARCHIVOS_PROPUESTOS.md). Todos los retirados siguen físicamente en Huawei y en el respaldo. No quedan cambios fuera del índice ni archivos sin clasificar visibles para Git al preparar este informe; los privados/generados están ignorados.

Se preservan las fuentes 1.4.18 (32), Room 21, interfaz y correcciones de campo existentes. Las únicas adaptaciones de migración son exclusiones de Git, documentación, referencia V21 saneada, ejemplos privados y rutas/opt-in de herramientas y pruebas. No cambió el código Android de ejecución, configuración legítima, firma o contratos.

## Verificaciones

- Debug desde exportación nueva del índice, sin salidas previas: PASS, 402,02 s.
- Incremental con comentario reversible solo en copia aislada: PASS, 196,59 s.
- 52 casos focalizados: PASS, 0 fallos/errores/omitidos, 132,62 s con compilación caliente.
- Configuración Release real en copia aislada: PASS, 18,27 s; sin bypass de configuración.
- Compilación de tests instrumentados: PASS, 47,06 s. No se ejecutaron en tablet ni Google.
- Revisión sintáctica: 29 Python, 182 GS/CJS y helper PowerShell, sin errores.
- Todos los archivos de app/src, esquemas, buildSrc, wrapper y entradas de plantillas requeridas están indexados.
- `git diff --check`: cinco avisos de espacios/líneas finales en fuentes históricas y documentación, sin errores funcionales; se conserva su formato.
- Primera tentativa de pruebas: 20 fallos de caché Robolectric/32 aprobadas, conservada; corregido únicamente el ejecutor offline SDK28/34. No fue un cambio funcional de GBO.
- Release 32 existente: firma histórica y metadatos válidos, APK intacta. No se repitió R8; el anterior assembleRelease aprobado y su fallo/reintento de memoria se documentan en MIGRACION_ASUS.md.

No se declara PASS de la suite general: siguen cinco expectativas históricas sin conciliar en AutomaticDocuments149Test/Engineering2DataTest. Las pruebas locales no certifican campo, PDF físico ni rendimiento/entregas Google.

## Seguridad y preservación

Repositorio comprobado **PÚBLICO**. Contenido propuesto revisado: 1.485 archivos y 789 entradas de archivos comprimidos, cero hallazgos Gitleaks y cero coincidencias con credenciales/identificadores privados conocidos; detalles en VERIFICACIONES.json. La revisión histórica encontró un falso positivo de prosa y cero coincidencias con los secretos actuales en 1.930 blobs. Sí quedaron IDs de carpetas/despliegues en el historial, incluido el ID de la raíz V21 en ocho blobs. Los IDs no son credenciales, pero no se ocultan retroactivamente saneando esta propuesta. No hubo reescritura de historia, cambios de visibilidad o de permisos.

main remoto se conserva en c42bb8158da6f24ff9cedbee7e4858fe104d1e31. El tag checkpoint-pre-redisenio-visual-2026-10-14 conserva su objeto anotado y commit 5de1242581dc460ea38d45006020d022f8d4da78. La rama histórica 10I ya estaba en aab9cf95f4f049418bbf1ebf2563f33373e843a5 y no se movió.

Respaldo privado completo y archivos de traslado en D:\Downloads\GBO_RESPALDO_MIGRACION_20261029_041941Z. ZIP con ACL restringida, **sin cifrado**: trasladar por medio/canal cifrado. Incluye firma, configuración, fuentes privadas y respaldos separados de APK 31/32. No se copió ninguna sesión OAuth de clasp. Recrear sdk.dir en ASUS, adaptar solo la ruta local del keystore y autorizar clasp de nuevo si se necesita después.

## Backend y pendientes

backend/referencia-v21 identifica la fuente inmutable V21 disponible; Código completo saneado como .example, módulos puros byte por byte y manifiesto/procedencia. No se contactó ni modificó Google. Optimizaciones y cronología remota de Pavimentos siguen como propuestas sin publicar; los paquetes Google históricos requieren restauración privada antes de utilizarlos.

GO para preparar publicación y posterior reconstrucción en ASUS, condicionado a la autorización del propietario y traslado seguro de configuración/firma. No equivale a aprobación de campo. **Detenido antes de commit/push, como solicita la fase 7.**
