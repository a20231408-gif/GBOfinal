# Integración de equipo y observaciones — 01/11/2026

**NO-GO productivo. Correcciones Android verificadas localmente; candidata oficial aplazada.**

- **PR #2/A integrado** mediante merge normal `bc1e307`; preserva autoría y commit
  `a9d066f44cb15ffedbb212692fbe7697b408afe0`. Regresión ASUS previa 29/29 PASS.
- **PR #1/B no integrado:** continúa abierto en borrador en
  `282b8df50a9a695ecc93c64f6ed0cb460511990f`. Revisión aislada 27/27 PASS con V2=true,
  sin credenciales, incluida recuperación 9/9. Falta que el autor lo deje listo y revisión
  final sobre integración actualizada. No se marca listo ni fusiona en contra de esa condición.
- **Código consolidado:** `c44befa4f0f6c41af8c177b2fdcade806c00246c`, versión fuente
  **1.4.23 (37)**. `f14095c`: captura descartada separada de Pendientes, vista Descartados y
  restauración individual visible; fechas/meses y ubicación descriptiva. `6e0f174`: longitud
  kilométrica acreditada inmediata/diferencia auxiliar explícita. `7608ede`: GLOBAL sin falsa
  cantidad cero en tablas PDF, mensajes SCAP/ubicación/antigüedad. No cambia Room, PCI ni backend.

## Resultados exactos

Selección final **103/103 PASS**, 18 clases, 0 fallos/errores/omitidos; Debug de validación y
signingReport PASS, Gradle 1m1s. Incluye descarte/restauración persistente, selección masiva,
conteos 30/34 y 7/7, UUID/ACK protegidos, archivos/historial, SIC, conflictos, longitudes,
exportación, PCI y recuperación. No es suite histórica completa ni prueba HTTP. El fixture
Dashboard de B todavía no está en integración. No sumar repeticiones de A/B al total final.

Primer intento integración: 94/96 PASS, dos fallos UI (etiqueta antigua/gesto fuera de viewport).
Segunda selección: 9/11 PASS (gesto UI y directorio de evidencia ausente en test histórico SIC).
Tercera: 12/13 PASS (faltaba identidad Android ficticia en fixture de restauración). Se corrigió
la interacción de accesibilidad, etiqueta y fixtures, sin relajar las guardas ni aserciones.
Repetición focalizada restauración 1/1 PASS; selección final 103/103. B tuvo primero seis fallos
de inicialización por temporal inexistente: no ejecutó sus condiciones; repetición 27/27 PASS.
XML/logs separados en `tmp/integracion-equipo-20261101/`.

## APK y pendientes concretos

Solo se compiló **Debug de validación sin credenciales, NO INSTALAR**:
`C:\GBO\tmp\integracion-equipo-20261101\final\GBO_1.4.23_37_VALIDACION_SIN_CREDENCIALES_NO_INSTALAR.apk`.
SHA256 `18bf3a9c4c9e2f625d0c7b812380194c3b01b2fdec94912f7f3afb33c81c62fe`.
Identidad y firma Debug histórica verificadas, alineado; no es APK de campo ni Release.
Ningún artefacto binario se publica. Candidatas privadas anteriores conservadas.

Pendiente Android: integrar B cuando deje de ser borrador y repetir regresión afectada;
después preparar candidata oficial limpia con firmas legítimas. P08 sigue parcial para
procedencia desconocida: PR numérico no prueba kilómetro real; no se inventó equivalencia.
P03 necesita conciliación real de ubicación, P11–P15 validación completa y P04/P17 visores/SAF
reales. Las correcciones nuevas no tienen aceptación física. Matriz reportada P01–P17:
`docs/VALIDACION_MANUAL_GBO_20261101.md`; los antiguos «0/17» ya no son el estado vigente.

Pendiente backend: OAuth/inventario estable/colisión histórica, permisos públicos de edición,
coste de lectura, legacy/vídeo y conciliación de ambas raíces antes de cualquier corte.
V21, ROOT, usuarios, Drive y coexistencia desactivada siguen intactos. No se hizo HTTP.

Próxima acción: el autor de B entrega PR listo; integrador revisa SHA y repite los contratos.
Antes de tocar datos de tablet, pedir autorización independiente para segundo respaldo actual.
Luego instalación autorizada y comprobación offline de Descartados/restauración, longitud,
fechas/documentos y PDF GLOBAL; continuar P11–P15. No restaurar la base vieja ni limpiar datos.
No hay autorización de distribución, sincronización ficticia, instalación ni corte productivo.
