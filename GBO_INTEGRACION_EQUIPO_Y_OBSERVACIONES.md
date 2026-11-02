# Integración final A+B y candidata original — 02/11/2026

**Candidatas originales preparadas; NO INSTALAR / NO DISTRIBUIR. NO-GO productivo.**

- PR #2/A integrado por merge `bc1e307`, conserva autoría.
- PR #1/B verificado Ready for review en `99cc388b5118b264cb618697892fef3c738727a3`.
  Solo cinco archivos esperados; sin cambios nuevos inesperados. Merge normal
  **`5123f48e0c03894cc3a1e9ed90b411af55bc5ff8`**, fuente exacta de las APK.
  Conserva autoría, rechazo estricto de ACK con ruta incorrecta, nombres V1/V2 y fixture
  Dashboard sin relajar la guarda de UUID. El bloqueo anterior de borrador está resuelto.
- No se cambiaron funcionalidades adicionales, Room 21, PCI, backend, main ni datos reales.

## Pruebas efectivamente ejecutadas

**113/113 casos únicos PASS**, 21 clases, cero fallos, errores u omitidos; Gradle 1m32s.
Selección anterior de 103 más Dashboard (4), ACK Pavimentos (4) e integración PDF Pavimentos (2).
V2 habilitado con perfil explícito de mocks sin credenciales. Incluye rutas/renombrado V1/V2,
ACK inválido y replay del mismo snapshot, revisión/hash/UUID/fotos conservados, recuperación,
descarte/restauración persistente, filtros/fechas/búsqueda, longitud, copias/historial, SIC/SCAP
y GLOBAL/PCI. No sumar los 27 previos de B ni repeticiones. No es suite histórica completa,
HTTP productivo o validación física. Logs/XML: `tmp/integracion-final-b-20261102/regresiones/`.

Compilación posterior **original**, con `gboValidationBuild=false` y sin init-script de mocks:
validateReleaseConfiguration, assembleDebug, assembleRelease y signingReport **PASS**, 16m29s.
Package `com.tuempresa.inventariovial`, versión **1.4.23 (37)**. Manifest, firma histórica y
alineación 16 KiB verificados. Configuración DEX contrastada offline con propiedades legítimas
y APK 1.4.22 de hash acreditado: endpoints originales Drive/Usuarios, sin endpoint de ensayo;
V2=true, 17A=false, 17B=false. No se imprimieron valores privados ni se hizo HTTP.

## APK privadas

Directorio fuera de Git, ACL propietario/SYSTEM:
`C:\Users\ASUS\GBO_CANDIDATAS_PRIVADAS_20261102_1_4_23_37\`

| Variante | Archivo | SHA-256 |
|---|---|---|
| Debug original, NO INSTALAR | GBO_1.4.23_37_DEBUG_ORIGINAL_PILOTO_NO_INSTALAR.apk | `7e680ba7d971b6848fa10131d9efbdd6049de976b36e12d93c0ef42e91ce23c5` |
| Release histórica, NO DISTRIBUIR | GBO_1.4.23_37_RELEASE_HISTORICA_NO_DISTRIBUIR.apk | `24f02b0ae00270534d70818cd5bef697afd6b9fe1c3472f7442fcfe20dc9f988` |

Debug certificado D2E54C… compatible con la última instalación verificada; Release 0DA8A2…
es distinto y no sustituye Debug. Revalidar la instalación actual antes de actualizar.
Auditorías completas y VALIDACION_PRIVADA.json junto a APK, sin credenciales impresas.
Los binarios no se publican. El Debug previo de validación sin credenciales no es esta candidata.
El SHA documental publicado se verifica con HEAD y origin; difiere del SHA fuente de APK.

## Tablet y bloqueos restantes

No se conectó, extrajo, instaló, desinstaló ni limpió la tablet. Tiene datos posteriores al
primer respaldo, que permanece intacto. Procedimiento listo en
`docs/SEGUNDO_RESPALDO_TABLET_20261102.md`: pedir autorización independiente para segundo
respaldo privado, confirmar guardado/modo avión/Wi-Fi apagado, run-as/TAR binario, SHA y SQLite
offline, cobertura de originales y limitaciones Keystore/SAF. Si es insuficiente, detenerse.
No restaurar la base vieja ni instalar por autorizar un respaldo. Instalación requiere otro permiso.

Matriz física vigente: `docs/VALIDACION_MANUAL_GBO_20261101.md`. P01–P10/P17 afectados deben
repetirse tras actualización autorizada; P16 solo inspección salvo permiso de limpieza;
**P11–P15 todavía pendientes**. Los parciales de 1.4.22 no se convierten en PASS.

V21 sin modificar, sin despliegue, cambio ROOT, Drive ni activación de coexistencia.
OAuth, inventario paginado estable/journals, ACK y colisión SCAP reales, permisos públicos,
raíces, coste y legacy/vídeo siguen pendientes. No hay autorización de sincronización ficticia,
distribución general ni corte. **NO-GO productivo; próxima acción: segundo respaldo autorizado.**
