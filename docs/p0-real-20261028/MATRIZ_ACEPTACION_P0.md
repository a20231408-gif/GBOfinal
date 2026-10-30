# Matriz de aceptación — pruebas locales

Resultado final: **193 PASS, 0 FAIL, 0 omitidas**; 189 casos JVM/Robolectric en 30 clases y 4 casos de archivos grandes en JVM de 256 MiB. No sumar nuevamente los reintentos. Véase evidencias/PRUEBAS_FINALES.json.

| Caso | Comprobación ejecutada | Resultado / límite |
|---|---|---|
| A | Room/constructor SCAP, 7 fotos + 7 croquis, ZIP verificado | PASS local; sin ACK Google |
| B | 54 fotos + 3 croquis, Excel, continuación y reapertura de Room | PASS local; originales conservados |
| C | SCAP completo de 629.885.776 bytes, 54 fotos + 3 croquis + 20 videos | PASS, proveedor de disco ficticio; bytes de videos ficticios, no prueba de reproducción |
| D/E/F | SIC-17/17A/17B incompletos, nulos reales, metadata provisional y avisos; SIC-17 independiente | PASS; escritor remoto estricto conservado |
| G/H | Compose, URI publicado, MIME y permiso; abrir Excel real y abrir Descargas; simulación MediaStore 29/34 | PASS local; visores externos/Files en tablet pendientes |
| I | Dos/tres puentes, edición/reexportación por identidad, conservar celdas, filas y plantillas históricas | PASS; también escritura interrumpida y publicación recuperable |
| J | Cierre y outbox en Room sin transporte, reapertura, reintento/error/BUSY y archivos conservados | PASS con transporte controlado; ejecución del SO en tablet pendiente |
| K | GET falso V20→V21 y recuperación del mismo UUID/revisión/preparación | PASS HTTP simulado; no GET Google en este encargo |
| L | 16 stages, commit con respuesta perdida, nuevo commit sin restaging, ACK íntegro, observador fallido después del ACK y 30 carreras exportación/ACK | PASS HTTP simulado; falta conciliar el caso de campo con ACK real |
| M | JVM 256 MiB: ZIP >512 MiB, entrada ZIP64 >4 GiB/campo 0001, poco espacio, interrupción y originales intactos | PASS; no prueba de ZIP físico >4 GiB ni certificación de memoria de toda la app en Android |
| N | Regresiones SCAP, SIC-20, geometría/integridad/PDF Pavimentos, persistencia, nombres y videos | PASS JVM/Robolectric; PDF/actualización en tablet pendientes |

No se ejecutaron cargas de prueba ni cambios en Google, instalaciones o borrados. Las capacidades y límites de V21 se contrastaron con su copia privada previamente verificada; las mediciones Windows/JVM no prueban rendimiento de Google.

Las pruebas de Google necesarias siguen siendo una entrega controlada autorizada, revisión actual con ACK completo, recuentos/hashes del manifiesto y videos con ACK separado. Si el padre excede 80 archivos/24 MiB por archivo/160 MiB por revisión, mantener copia local y aviso; no fabricar confirmación. El caso de videos separados tiene límites distintos. Ver LIMITES_DRIVE_Y_PROPUESTA_NO_IMPLEMENTADA.md.
