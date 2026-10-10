# Integración PARALLEL C · Pavimentos / PCI / URCI

Base: carpeta GBO_FIX recibida, Room 12. Rama lógica PARALLEL C; la carpeta no contiene `.git`. No se creó un repositorio ni se realizó commit o push.

## Room 13 provisional

Único archivo compartido de producción modificado: `InventoryDatabase.kt`, para registrar versión y migración. La migración está aislada en `pavement/PavementMigration.kt`. CORE debe reservar/renumerar 13 si otra rama también usa ese número y encadenar ambas migraciones. No usar migración destructiva ni modificar el historial 1–12.

SQL completo 12 → 13, ejecutado dentro de la transacción de migración de Room:

```sql
ALTER TABLE pavement_samples ADD COLUMN slabWidthM REAL;
ALTER TABLE pavement_samples ADD COLUMN slabLengthM REAL;
ALTER TABLE pavement_samples ADD COLUMN sicDataJson TEXT;
UPDATE pavement_samples
SET slabWidthM = anchoM / slabColumns,
    slabLengthM = largoM / slabRows
WHERE surface = 'RIGID' AND slabRows > 0 AND slabColumns > 0;
DROP INDEX IF EXISTS index_pavement_samples_ruta_tramo_surface_muestra;
CREATE UNIQUE INDEX index_pavement_samples_ruta_tramo_calzada_surface_muestra
ON pavement_samples(ruta, tramo, calzada, surface, muestra);
UPDATE pavement_damages
SET severity = CASE severity WHEN 'B' THEN 'L' WHEN 'A' THEN 'H' ELSE severity END;
```

Los snapshots de cálculo cerrados se conservan byte a byte; el lector y las salidas aceptan B/A históricos. Las curvas empaquetadas conservan sus IDs B/M/A y el calculador traduce L/H al buscarlas. Las progresivas históricas no se reescriben masivamente: la cabecera normaliza al guardar. Los campos TEXT existentes conservan el contrato binario; los valores nuevos usan `00+000` con precisión de milímetros, con conversión numérica centralizada. No se infieren códigos PR a partir de kilómetros.

La geometría rígida es filas longitudinales × columnas transversales. `largoM = slabRows × slabLengthM`; `anchoM = slabColumns × slabWidthM`. Las coordenadas y los IDs de losas existentes no cambian. La muestra permite modificar número, inicio y geometría antes de registrar daños; después de capturar daños se conserva el bloqueo existente de dimensiones.

## Contratos con CORE

`PavementGeneratedFiles.records(context)` devuelve el manifiesto durable en `filesDir/pavement/exports/manifest.json`. Cada registro contiene ID, muestra, superficie, categoría, MIME, ruta local, SHA-256, URI de copia externa y fecha; fotografías entregadas incluyen `photoId` y `damageId`. Ver `PAVEMENT_GENERATED_FILES.json` para categorías y origen de fotos. Los archivos se conservan localmente aunque falle la copia SAF o la subida; CORE deberá definir retención y confirmar envíos antes de limpiar copias.

Las exportaciones SAF de PDF, Excel de superficie y SIC crean copia local durable. El uploader existente de Pavimentos preserva sus PDF, JSON, XLSX y fotos marcadas antes de iniciar red. No se modifica el router global, el protocolo `uploadPavementFile`, los servicios de usuarios, SCAP, exportadores de puentes ni KMZ global. SIC no se envía automáticamente por una categoría nueva al servidor actual: CORE debe consumir los registros `SIC_SIC14`, `SIC_SIC14A`, `SIC_SIC15`, `SIC_SIC16` y decidir su ruta contractual.

El JSON nativo agrega dimensiones de losas, suplemento SIC y snapshot de cálculo a los campos previos; los lectores de schema 1 deben tolerar campos adicionales. Los originales fotográficos siguen referenciados por `pavement_damage_photos`; la copia marcada tiene identidad distinta en el manifiesto.

Los complementos SIC pueden completarse por el inspector creador incluso después del cierre. Esto actualiza solo `sicDataJson` y `updatedAt`; no reabre la muestra, no modifica daños, fecha, geometría ni snapshot. Cada valor manual lleva `MANUAL_PENDING_EXTERNAL_SOURCE`. Un futuro adaptador SIC-3 podrá sustituirlo con evidencia y origen propios sin recapturar ruta/calzada/fecha.

## SIC y fuentes

Las imágenes adjuntas 1, 2, 3 y 5 sustentan las trece columnas de SIC-14, SIC-14A, SIC-15 y SIC-16. Se conservan en `docs/reference/pavement/`. No existe un XLSX nativo de estos cuatro formatos en la base; las hojas se reconstruyen con las columnas y agrupaciones conocidas de las imágenes. No se afirma equivalencia visual o contractual con un libro original no suministrado.

Se inspeccionó `docs/Formato_para_programa_1.xlsx`: contiene Hoja1 de captura de muestra y Hoja2 con las trece columnas del formato de Fallas; no contiene las hojas SIC solicitadas. La evidencia de esa búsqueda está en `native-template-inventory.json`.

SIC-14A tiene una contradicción: la lista identifica Huecos como 13, pero la anotación dice densidad solo para 07-Huecos. Se bloquea la exportación cuando intervienen los tipos 07 o 13 hasta confirmar esa regla con la plantilla contractual. El resto conserva infraestructura y captura de faltantes. Las equivalencias ambiguas entre PCI/URCI y SIC quedan pendientes de selección explícita; nunca se exporta el código PCI como si fuera SIC. No se añaden columnas PCI, inspector o muestra que no aparecen en las fuentes.

Las imágenes SIC-15A y SIC-16A se preservan como referencia suministrada; esos formatos no forman parte de los cuatro solicitados y no se habilitan como nuevas salidas.

## Conflictos previstos

- Número Room y lista de migraciones en `InventoryDatabase.kt`; integrar `13.json` después de decidir la versión final.
- Código Pavimentos si CORE cambia su uploader o contrato JSON: conservar las adiciones de metadatos y el registro local, sin duplicar envíos.
- `PavementModuleTest` agrega la migración nueva a su builder; `PavementReportTest` ahora espera segmentación. Los tests antiguos que omiten migraciones completas siguen en los fallos del baseline.

## Google Earth: evaluación, sin implementación

Es viable generar fuera de la app un `GroundOverlay` con imagen local dentro de KMZ. KML soporta límites, rotación y cuadrilátero de georreferenciación; Earth Pro permite superponer imágenes. Fuentes: [KML Tutorial](https://developers.google.com/kml/documentation/kml_tut), [KML Reference](https://developers.google.com/kml/documentation/kmlreference), [Google Earth Pro: imágenes superpuestas](https://support.google.com/earth/answer/148099?hl=en).

**COMPLEJIDAD:** baja para serializar una imagen y KML; media para colocarla correctamente, porque la cabecera local X/Y no proporciona coordenadas de cuatro esquinas ni orientación geográfica. CORE tendría que proporcionar una referencia de eje, coordenadas y rumbo o cuatro esquinas verificadas. Un tramo curvo podría requerir varios segmentos; una sola imagen rectangular no representa su curvatura.

**RIESGO:** desplazamiento por precisión GNSS, orientación equivocada, proyección/escala y diferencias de soporte entre clientes Earth. La precisión métrica del croquis no demuestra precisión geográfica del overlay. No convertir una progresiva en latitud/longitud sin eje y referencia verificables.

**BENEFICIO:** contexto de ubicación y comparación visual con el entorno para revisar la inspección. La exportación estática propuesta no necesita SDK ni credenciales adicionales en Android y evita consultas de red para crear el archivo; la imagen base de Earth puede requerir conectividad o caché del cliente.

Decisión: dejar la generación/importación KMZ a CORE. No se incorporó SDK, credenciales, servicios ni georreferenciación inferida.
