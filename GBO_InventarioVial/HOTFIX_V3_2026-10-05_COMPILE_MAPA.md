# Hotfix V3 — compatibilidad del mapa VIDEO_SHOWCASE

## Error corregido
Android Studio reportaba:
`LiveRoadMap.kt:151:68 Unresolved reference 'colorFamily'`.

## Causa
`LiveRoadMap.kt` había sido restaurado desde `GBO_InventarioVial_VIDEO_SHOWCASE`, pero el archivo
`AssetPresentationCatalog.kt` seguía siendo la variante anterior de la rama MAPA. El mapa del showcase
requiere dos APIs de presentación que esa variante no tenía:
- `AssetPresentationCatalog.colorFamily(...)`
- `AssetFamily.sicGroup`

## Corrección
Se mantuvo el catálogo funcional de la rama MAPA y se añadieron únicamente las piezas de compatibilidad
necesarias para el mapa estable:
- `sicGroup` en cada `AssetFamily` existente.
- `AssetPresentationCatalog.sicGroup(item)`.
- `AssetPresentationCatalog.colorFamily(item)`.
- Leyenda agrupada una vez por SIC 17–23.

No se copió completo el catálogo del showcase porque aquel catálogo usa relaciones/clases adicionales
(p. ej. SIC-21 detallado) que no existen en `InventoryRecordWithPhotos` de esta rama y habrían introducido
nuevos errores de compilación.

## Versión
- versionCode: 8
- versionName: 1.3.3-final-mapa-hotfix-map-compat
