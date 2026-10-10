> DOCUMENTO HISTÓRICO. La decisión final de integración del 08/10/2026 lo sustituye: fotografía original limpia, sin watermark. Ver FINAL_INTEGRATION_REPORT.md.

# Corrección de pavimentos: regla y fotografías

Cambios limitados a pavimentos y sus tests. Sin cambios de esquema Room, fórmulas PCI/URCI, SIC, SCAP, mapa, autenticación o contratos Drive. Sin commit ni push.

## Arquitectura

PavementScheme dibuja la regla en un margen izquierdo de 64 dp. PavementScale comparte la conversión longitudinal con cuadrícula, daños, losas, punto inicial y toque: Y local = (largo - metros) * escala; metros = largo - Y local / escala. El Canvas aplica después la traslación vertical de 15 px + pan.y. La escala uniforme incluye zoom y conserva el origen inferior izquierdo. Cambiar la malla solo cambia la separación de cuadrícula.

Ticks menores cada metro (hasta 1000 referencias; en longitudes mayores se espacian para evitar trabajo ilimitado). Marcas principales cada 5 m y extremos reales. Etiquetas de 14 sp cada 5 m, ampliando el intervalo a múltiplos de 5 si no hay altura suficiente. Las etiquetas fuera del viewport se omiten. La altura del esquema compacto es 420 dp para mejorar la lectura de 40 m.

GetContent(image/*) sigue el patrón de copia interna existente de CulvertPhotos. La cámara conserva createPhotoFile/FileProvider; la importación crea directamente una copia única en el mismo directorio administrado, sin generar un URI de cámara innecesario. La imagen se copia a filesDir/images, se valida y se normaliza como JPEG con orientación corregida, igual que las fotografías de cámara. La base de datos conserva ruta absoluta y UUID en pavement_damage_photos, vinculada a damageId. No se persisten URI temporales. El destino del selector se captura antes de abrirlo y no depende de la selección posterior.

Cada foto tiene tarjeta numerada, miniatura, Ver y Eliminar (solo para muestras editables). El diálogo confirma la eliminación de esa foto. El repositorio verifica propiedad y estado editable, borra únicamente la fila por UUID y elimina el archivo solo si ya no está referenciado en fotos de pavimentos, su directorio canónico es filesDir/images y su nombre comienza por PAVIMENTO_. Las referencias externas se retiran sin borrar el original. No se modifica el daño.

## Archivos

- app/src/main/java/com/tuempresa/inventariovial/pavement/PavementScale.kt
- app/src/main/java/com/tuempresa/inventariovial/pavement/PavementPhotos.kt
- app/src/main/java/com/tuempresa/inventariovial/pavement/PavementDao.kt
- app/src/main/java/com/tuempresa/inventariovial/pavement/PavementRepository.kt
- app/src/main/java/com/tuempresa/inventariovial/pavement/ui/PavementScheme.kt
- app/src/main/java/com/tuempresa/inventariovial/pavement/ui/PavementWorkspace.kt
- app/src/test/java/com/tuempresa/inventariovial/PavementModuleTest.kt
- app/src/test/java/com/tuempresa/inventariovial/PavementTouchTest.kt
- Este documento y output/pavement-validation*.log.

## Validación manual pendiente en tablet

1. Abrir Fallas en pavimentos.
2. Crear una muestra de aproximadamente 40 m.
3. Confirmar regla lateral 0, 5, 10, 15, 20, 25, 30, 35, 40 m; 0 en el extremo inferior.
4. Cambiar malla 0.5 / 1.0 m.
5. Confirmar que las distancias de regla no cambian.
6. Crear un daño a Y aproximadamente 20 m.
7. Confirmar alineación con 20 m y repetir con zoom/pan.
8. Tomar tres fotos y ver tres tarjetas.
9. Eliminar la central con confirmación; conservar las otras dos.
10. Seleccionar JPEG y PNG desde almacenamiento/galería.
11. Cerrar y reabrir la muestra; reiniciar tablet.
12. Confirmar persistencia y asociación exclusiva al daño; comprobar otro daño.
13. Confirmar que PCI/URCI conserva el resultado. Probar Mover en esquema.
14. Generar reporte/exportación actual y comprobar fotos y cálculo.
15. Eliminar una imagen importada y verificar que el original de galería permanece.

No se recibieron las dos capturas mencionadas: el único adjunto disponible era el texto de solicitud. No se realizó validación física en tablet.

## Resultado automático final

- gradlew.bat testDebugUnitTest assembleDebug --continue: código 1, BUILD FAILED in 1m 51s; 230 tests, 15 fallos. Los 25 tests de pavimentos pasaron. assembleDebug se completó (UP-TO-DATE en esta ejecución).
- gradlew.bat testDebugUnitTest --tests '*Pavement*' assembleDebug: código 0, BUILD SUCCESSFUL in 39s; 25 tests, 0 fallos; assembleDebug completado.
- APK: app/build/outputs/apk/debug/app-debug.apk.
- Logs: output/pavement-validation-final.log y output/pavement-validation-focused.log.
- Detalle de los 15 fallos externos: output/pavement-test-summary.txt. Incluyen expectativas antiguas de versión 11 frente a 12, rutas incompletas de migración configuradas por tests, aserciones SCAP/SIC, referencias de mapa y autenticación. No se alteraron esos módulos ni sus tests.

Tests añadidos: transformaciones y ticks para 8/10/40/100 m; intervalos adaptativos; importación PNG con normalización JPEG; reapertura de base de datos y disponibilidad de archivos; múltiples fotos por daño y rechazo de eliminación con otro damageId; eliminación de la foto central y preservación de original externo; igualdad del daño y resultado de cálculo antes/después. Compose: cambio de malla conserva posición métrica, tres botones Eliminar y confirmación/cancelación. Se actualizó el test de toque para el margen de regla. Los tests existentes de PCI/URCI, movimiento y reporte pasaron.

Limitaciones: la suite global no está verde; falta validación física en tablet (incluyendo selector real de galería, cámara y reinicio). No se realizó commit ni push.
