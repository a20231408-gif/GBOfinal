# Estructura Drive y recuperación

## Hallazgo real de solo lectura

Se inspeccionó PE-3N/ALCANTARILLAS/ALCANTARILLA 1 (entrega exitosa). Contiene metadata.json y REVISIONES. Dos carpetas de revisión identificadas: 1791844058048_8f2af849 y 1791844008225_b69e2228. En la primera se observaron SIC-18.xlsx, metadata.json, record_*.json, binding_v4_*.json y aggregate_*.json. No se movió/eliminó ni se descargó contenido de fichas reales.

`SyncV3.gs` crea el archivo histórico desde `protect(file)` antes de actualizar un archivo definitivo. El número es la revisión y el sufijo deriva del hash de contenido. Los JSON son respaldos de registro, binding, agregado y metadatos necesarios para rollback/recuperación. SIC-18.xlsx es la copia de seguridad del consolidado que será reemplazado, no una segunda entrega contractual independiente. El Excel vigente permanece bajo RUTA/EXCEL_SIC. La carpeta de respaldo anterior se obtenía en el elemento y podía alojar una copia de Excel originada en EXCEL_SIC: esta ubicación técnica causa confusión, no demuestra corrupción.

## Distribución preparada para futuras entregas

```text
RAIZ/
  RUTA/
    ALCANTARILLAS/
      ALCANTARILLA 1/
        PANORAMICA/
        ENTRADA/
        SALIDA/
        metadata.json
    EXCEL_SIC/
      SIC-18.xlsx
  ._GBO_SYNC_V3/
    STAGING/...
    REVISIONS/<hash-clave>/<revision>_<hash>/...
    journals, registros, aggregates, bindings y reservas de nombres
```

SCAP y otras familias mantienen sus categorías reales; pavimentos V2 coloca las fotografías bajo PANEL_FOTOGRAFICO según contrato. Las rutas técnicas concretas existentes se conservan y se referencian por ID; el esquema anterior es descriptivo. El prefijo `._` no oculta el contenido en Drive ni constituye un control de acceso: revisar permisos reales. No cambiar ACL sin permiso del propietario y sin comprobar que el backend conserva acceso.

## Migración anterior — procedimiento preparado, no ejecutado

1. Inventario de solo lectura por ID: archivo/carpeta, padres, permisos, hash/tamaño, journal/archiveId y binding que lo referencian. Distinguir vigente contractual, backup y staging. No clasificar únicamente por extensión Excel.
2. Exportar manifiesto firmado/fechado y copia verificable de fuente/archivos de recuperación, en ubicación autorizada y protegida. Registrar cada operación con ID idempotente y origen/destino; exigir respaldo íntegro antes del primer movimiento.
3. Comprobar toda referencia: IDs de archivos, carpetas de revisiones, journals abiertos y operaciones de rollback. Congelar/coordinar escritores para evitar cambios concurrentes; si la revisión/hash no coincide, detener el lote sin sobreescribir evidencia nueva. Identificar permisos heredados que cambiarían al mover.
4. Tras autorización explícita del propietario, mover solo objetos técnicos usando los mismos IDs, lote pequeño por revisión. Persistir intención y resultado en journal; reintentar por operación/ID sin duplicar. No mover Excel contractual ni renombrar elementos. Comprobar lector de journal y rollback antes de continuar.
5. Verificar hashes, todos los IDs/referencias, permisos y un rollback ficticio. Manifestar incidencias y padres finales. Mantener copia anterior hasta aceptación.
6. Rollback: recorrer operaciones confirmadas en orden inverso, reponer padres originales por ID y verificar hash/referencias/ACL. No borrar objetos compartidos ni reemplazar registros nuevos para restaurar un respaldo antiguo. Si hay conflicto, detener y conciliar.

No se ha generado/ejecutado un script de movimiento sobre Drive; se entrega este procedimiento para una futura autorización. El parche afecta solo respaldos futuros. Históricos y journals abiertos conservan sus IDs y posiciones.

## Nombres de fotos

Reserva central bajo ScriptLock: `NNN_FAM<numero-historico>_CAT.jpg`, máximo 20 caracteres, secuencia 001–999 por identidad de elemento. UUID/fileId sigue siendo clave; número local visible nunca asigna la secuencia global. Histórico conserva nombre anterior, incluido nombre largo. Reemplazo con UUID nuevo usa siguiente número; retirados no reutilizados. Un reintento consulta la misma reserva. Límites/categorías desconocidas rechazan de forma segura sin truncar ni renombrar. Catálogo completo en PhotoNamesV2Core.js: 23 familias nominales más MUESTRA por superficie (PAF/PRG/PNP), categorías CORE/SCAP reales. PAN/ENT/SAL para alcantarillas, ARR/ABA/TAB para categorías de puentes. BVT identifica barandas/veredas/tuberías; OEP, otro elemento de puente. Validación de catálogo cubre las 24 familias V4.
