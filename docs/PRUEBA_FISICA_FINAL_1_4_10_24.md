# Validación física final — candidata 1.4.10 (24)

Pendiente de ejecución por el propietario. No acredita operación en campo ni entrega a Drive. No se instaló automáticamente.

## Actualización conservando datos

1. Respaldar las exportaciones y evidencias disponibles de la instalación existente. No borrar bases, fotografías, vídeos ni configuraciones.
2. Usar exclusivamente la APK final indicada en `CIERRE_CANDIDATA_FINAL_1_4_10_24.md`; conservar también las APK anteriores. La comparación de firma se refiere a la APK local 1.4.9, no a una lectura de la instalación de cada tablet.
3. Copiarla a la tablet y abrirla con el instalador de Android: debe ofrecer **Actualizar**. Si aparece conflicto o rechazo, detenerse y conservar todo; no desinstalar ni limpiar almacenamiento. Una instalación Debug puede tener otro certificado.
4. Comprobar versión 1.4.10, usuarios, configuración, rutas y registros anteriores. No compartir PIN, token ni URL del servicio.

## Ensayo local breve (datos completamente ficticios)

Activar modo avión antes de crear fichas. Identificar los datos como `TEST_RC24_FINAL`. Utilizar el destino **Solo dispositivo** donde esté disponible; no reconectar hasta revisar que los registros ficticios no estén destinados a Drive. Esta guía no autoriza una subida productiva.

| Prueba | Resultado verificable esperado |
|---|---|
| Muro SIC-20 | Guardar borrador con foto y observaciones, volver a Inicio, cerrar/reabrir GBO, continuar y finalizar mediante sus validaciones normales. Mismo elemento/UUID, clase Muro, fotografía y texto intactos, sin ficha duplicada. |
| Badén SIC-20 | Repetir por separado; debe conservar Badén al reabrir/finalizar y exportar su clase. Un borrador histórico contradictorio debe bloquearse sin sobrescribirlo; no intentar repararlo a mano. |
| Escalonamiento 25 | Probar juntas H/V compartidas, vista previa y cancelación. Elegir una junta si la intersección es ambigua. Segmento y pequeña marca perpendicular; cantidad original de losas. |
| Desnivel carril/berma 27 | Confirmar borde exterior X=0 y, en otro daño, X=ancho. Verificar marca roja visible en ambos bordes tanto en pantalla como PDF. No debe desplazarse ni interpretarse una junta interior como berma. |
| Grieta D y parches | D24 en cuatro esquinas; parche 29 grande anclado/interior y 30 pequeño; invertir extremos cuando corresponda. Sin rellenos fuera de muestra, medidas y losas intactas. |
| Esquina 22 y junta 26 | Comprobar cuatro esquinas, marcas de junta/cruce, cancelar/confirmar y reapertura. |
| PDF y Excel | Exportar PDF horizontal nativo de muestra corta y larga, abrirlo con un lector de la tablet y ampliar. Revisar marcas 25/27, tablas, referencias, cortes y texto. Abrir XLSX; cantidades/observaciones y PCI deben corresponder. Si no hay curva PCI disponible, respetar el estado informado; no inventar valor. |
| Fotografías y copia local | Reabrir fotos y archivos exportados tras cerrar la aplicación; comprobar presencia en la ubicación elegida y edición conservando el mismo registro. Copia local no equivale a ACK Drive. |
| Cola offline | En registros destinados a Drive, verificar que pendiente/espera sigue pendiente sin Internet, con revisión confirmada sin cambios. No reenviar conflictos/bloqueos indiscriminadamente ni borrar preparaciones. |

Registrar por fila PASS/FAIL, hora y una evidencia no sensible. Ante pérdida, cambio de tipo, foto ausente, PDF ilegible o incompatibilidad de instalación: detener validación y conservar el estado para diagnóstico.

## Prueba de Drive separada: requiere autorización específica

Antes de reconectar, acordar raíz/carpeta autorizada y registros ficticios permitidos. Enviar escalonadamente una ficha y una muestra; comprobar identidad, revisión local = revisión confirmada, ACK válido y archivos en Drive. Repetir solo para demostrar recuperación y ausencia de duplicados, conservando número estable de carpeta. “3/6 archivos” y “Subiendo” no son confirmación.

Desde **Diagnóstico de estabilidad → Exportar diagnóstico** se puede guardar/compartir el JSON agregado de cola, WorkManager y tiempos, sin terminal. No sustituye los registros de ejecución del Apps Script para explicar tiempos del servidor. La incidencia histórica de entregas demoradas requiere esa conciliación real.

**Criterio:** GO solo para validación física controlada. El uso como única fuente y la distribución general siguen sin aprobarse hasta cerrar prueba nativa, actualización real y conciliación de sincronización autorizada.
