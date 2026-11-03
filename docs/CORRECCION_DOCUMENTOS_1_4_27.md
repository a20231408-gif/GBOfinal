# Documentos públicos estables — 1.4.27 (41)

02/11/2026. Rama local `codex/documentos-publicos-estables-20261102`, desde cierre 1.4.26
`b79456ed0fd30430517228290013582cd0b5e56c`. Fuente compilada:
`72a100b7ceda38b4001f7a615c81ffcdbe188d98`. Sin push, instalación ni distribución.

## Cambio entregado

Para nuevas exportaciones normales:

```text
Descargas/GBO Inventario Vial/
  PE-22A/ALCANTARILLAS/Alcantarilla - PE-22A - PR 10+120/
    SIC-18.xlsx                 (o SIC-18_PROVISIONAL.xlsx mientras esté incompleta)
    SIC-18A.xlsx                (cuando corresponda)
    FOTOGRAFIAS/...
```

No se añaden `_PROVISIONALES`, `REVISIONES/R_timestamp` ni carpetas MEDIOS por guardado.
Los nombres humanos se reservan por ficha; homónimos usan sufijo local persistente, sin inventar
numeración contractual. Cambiar un nombre no traslada automáticamente sus directorios históricos.
Si no hay carretera se usa SIN_CARRETERA; los documentos siguen marcados provisionales según datos.

`StableLocalPublication` mantiene bytes inmutables y journal recuperable en almacenamiento privado
`files/local_publications_v5/`. Prepara/verifica antes de publicar. Solo la copia anterior creada
por este publicador puede pasar a historial mediante renombrado verificado, sin borrado:
`.Historial - ...` en la misma carpeta, accesible desde la opción Historial de GBO y oculta del
listado normal. Conserva además su fuente privada. Al completar un documento, el nombre provisional
pasa al historial y queda el nombre final como actual. Un reintento viejo no sustituye la versión nueva.
Si falta una copia pública propia, puede recrearse desde los bytes privados verificados.

Conflictos, copias compartidas, modificaciones externas, falta de espacio o permisos detienen la
publicación sin sobrescribir documentos ajenos. Historial requiere espacio; no se purga automáticamente.
Una interrupción puede dejar temporalmente la copia anterior en Historial; Reintentar recupera la
promoción pendiente sin perder originales. No se promete transacción atómica entre todos los archivos
del paquete ni que un proveedor sin renombrado recuperable permita la sustitución.

No se movieron ni borraron históricos existentes. Sus planes congelados siguen usando el formato
antiguo; la regeneración explícita crea un trabajo LAYOUT_V5 separado. SIC-17/18A, SCAP, Pavimentos,
consolidados EXCEL_SIC locales, exportaciones manuales y archivos organizados por fecha conservan
sus políticas particulares de contenido/orden. Room 21 sin migración.

Comparación con la fuente V21 privada: usa carretera/familia, identidad/numeración del servidor y
EXCEL_SIC compartido por carretera. El nombre descriptivo local no acredita un nombre remoto.
DriveMirrorPlan permanece intacto: exige ACK completo y bytes exactos, conserva rutas confirmadas
y su excepción de archivo de conflictos `_REVISIONES_CONFIRMADAS`. No se cambió contrato ACK,
sincronización, reservas, endpoint, ROOT ni backend. No se ejecutó HTTP real.

## Pruebas y evidencia

**138 casos únicos PASS, 17 clases, cero fallos u omitidos en sus últimas ejecuciones**.
Selecciones focalizadas acumuladas, no suite histórica completa ni prueba física de esta APK.
Cubre publicación inicial/repetida, bytes idénticos, recuperación de copia ausente, revisión vieja,
interrupciones, conflictos externos/compartidos, plantillas, fotos, ZIP, apertura URI, historial,
MediaStore real con proveedor de prueba y SAF Android 8/9 con cambio/pérdida de URI de renombrado.
Incluye regresiones de SIC/SCAP, cronología Pavimentos, PDF, limpieza protegida y ACK sin cambios.

Evidencia: `C:\GBO\tmp\documentos-estables-20261102\RESULTADOS_UNICOS.json` y XML separados
en subdirectorios primera/adicional/final/recuperacion-saf/recuperacion-final-inicial/ultimos.
Se conservan fallos iniciales: fixture HTTP sin kind/recordId/baseRevision, expectativa antigua de
prefijo ZIP, expectativas históricas de cronología/provisionalidad y fixture que reutilizaba URIs
tras eliminar una entrada. Se corrigieron fixtures/aserciones al contrato vigente, sin cambiar
producción para forzar resultados. El fixture del ZIP de más de 512 MiB se adaptó al renombrado;
esa prueba pesada NO se repitió ni cuenta entre los 138.

Archivos funcionales: LocalDocumentLocation, LocalExportBuilder, LocalCopies,
LocalCopyPresentation, nuevo StableLocalPublication y fallback LocalBridgeDocument; versión en
app/build.gradle.kts. DriveMirrorPlan revisado, sin modificaciones.

## APK privada

`C:\Users\ASUS\GBO_CANDIDATA_PRIVADA_20261102_1_4_27_41\GBO_1.4.27_41_DEBUG_ORIGINAL_DOCUMENTOS_ESTABLES_NO_INSTALAR.apk`

SHA-256: `710842ecb7b5f31f91daa0edc4594c409058eb83d559ed40bc0ff6a1ad1670db`.
Package `com.tuempresa.inventariovial`, 1.4.27/41 Debug, certificado histórico
`d2e54cb086aa731ef046be300977d0b16c0d8f66fdcb08d0d9cfd2038bf0d872`.
Firma, alineación 16 KiB y configuración original contrastadas con APK 1.4.26 fijada por hash.
Mismos endpoints/credencial original, sin valores publicados. V2 habilitado, SIC17A/B deshabilitados.
No Release nueva. APK 1.4.26 preservada. Compatibilidad de actualización por package/firma/código,
sin instalación física de 1.4.27 acreditada.

Aceptación pendiente, tras autorización de instalación y sin sincronizar: en teléfono limpio,
guardar una ficha ficticia tres veces, comprobar una sola carpeta estable y la versión actual;
abrir documentos/fotos y versiones desde Historial; comprobar que otra ficha homónima no se pisa.
No usar datos reales ni migrar históricos. **Candidata local verificable; NO-GO de distribución
general/online hasta aceptación física y cierre de subida real pendiente del documento de continuidad.**
