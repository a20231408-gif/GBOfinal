# Prueba física controlada — 1.4.18 (32)

No se ha instalado ni distribuido automáticamente. No se autoriza distribución general ni escrituras Google mediante este checklist.

1. Activa modo avión y guarda/cierra normalmente los formularios antes de actualizar. Conserva la APK anterior y genera una copia local de las fichas/evidencias importantes con las herramientas existentes. Anota cuántos registros pendientes hay. No desinstales ni limpies almacenamiento.
2. Instala manualmente la APK candidata como actualización. Comprueba en la información de GBO **1.4.18 (32)**. Si Android informa incompatibilidad de firma, detente: no desinstales para sortearla.
3. Sin Internet, abre fichas y fotografías existentes, incluida una entrega pendiente. Comprueba que siguen disponibles y no aparezcan como confirmadas sin ACK. No edites registros ajenos para probar.
4. Para fichas ficticias nuevas, antes de finalizar abre **Destino de esta ficha → Elegir Solo dispositivo → Confirmar**. Así no se programa una carga productiva al recuperar Internet. Identifícalas como `TEST_PV_P0_32`.
5. En Pavimentos, prueba SIC-14 y SIC-14A en asfalto, SIC-15 en rígido y SIC-16 en no pavimentado. Deja Faja, PR inicial/final y distancias vacíos. Guarda y exporta; abre cada Excel: esas celdas deben estar vacías y los datos técnicos escritos deben conservarse. Los pendientes técnicos deben advertirse como provisionales, sin bloquear el respaldo.
6. Exporta dos muestras de una ruta de pruebas con fechas de registro distintas. Reexporta tras editar una medida: deben seguir dos filas, ordenadas por fecha de registro, y solo cambiar la fila editada. Si coinciden las fechas, se desempata por hora persistida y UUID. Comprueba también que un PR voluntario se conserva, pero no determina el orden.
7. En rígidos, selecciona **26 · Daño del sello de junta**. Marca una junta y guarda gravedad baja. Usa **Agregar otro daño de junta** para una segunda media y una tercera alta, en segmentos interiores distintos. Guarda, cierra y reabre: deben seguir tres UUID/ocurrencias con su asignación y gravedad.
8. Edita la segunda; las otras dos deben permanecer iguales. Añade evidencia a una y elimínala individualmente: deben quedar las otras dos activas, con la evidencia retirada conservada en el historial documental y `DANOS_ARCHIVADOS.json`. No elimines ficheros manualmente.
9. Sin global manual debe aparecer **Pendiente de evaluación global** y el PCI no debe presentarse como definitivo. Exporta: debe funcionar y mostrar el pendiente. Establece después una calificación **Global** manual: no deben sumarse tres deducciones. Editar una junta no debe sustituir automáticamente ese global. En una ficha histórica, verifica que su global previo sigue intacto.
10. Abre el PDF horizontal nativo, el Excel nativo y los SIC desde GBO/Archivos. Revisa marcas, severidades, filas y fotografías. Comprueba `EXCEL_SIC` en el destino público autorizado; un estado preparado no sustituye abrir los archivos.

## Comprobación Drive, solo con autorización separada

Utiliza una única ficha ficticia autorizada. El administrador puede autorizar posteriormente su envío desde **Autorizar envío posterior a Drive** si se creó en Solo dispositivo. No actives el envío de otras fichas de prueba.

- Con V21, espera un ACK completo del padre y de PDF, Excel nativo, JSON, SIC documentales y fotos. Revisa `DATOS_APP/SIC-14.xlsx` o el formato correspondiente en la carpeta estable de la muestra.
- La app debe distinguir **respaldo individual confirmado** de **consolidado cronológico remoto pendiente**. No aceptar esta última parte como entregada solo porque el padre tenga ACK.
- Un reintento debe conservar UUID, revisión, hashes y carpeta. Una pérdida de respuesta no debe duplicar fichas. Si no hay ACK, conservar todo y dejar pendiente.
- El parche de cronología del servidor no está publicado. Su ensayo Google y conciliación histórica son independientes de esta instalación.

Resultado a devolver: versión instalada, PASS/FAIL por punto, error literal si aparece y nombres de archivos ficticios comprobados. No incluir PIN, tokens, URL privadas ni registros de ingenieros.
