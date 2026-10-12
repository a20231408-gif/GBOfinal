# Registro de archivos de prueba — Drive V4

Raíz autorizada: [GBO Inventario Vial](https://drive.google.com/drive/folders/1FQBVRD3BK1rOBfC8NdRzPQt3fvU40V23). Se conservó todo el contenido; no hubo limpieza. Esta lista deduplica IDs de ACK y muestra la última revisión observada de cada archivo. No es un inventario general del Drive del usuario.

## Elementos ficticios

| UUID | Carpeta del elemento | Ruta lógica |
|---|---|---|
| `6dc3301b-bda9-4432-8f62-b6efe38ef2b1` | `1RaHw4dsHeJ6Psn4gVobLF4ISCv1oTVyr` | PE-22A/ALCANTARILLAS/ALCANTARILLA 1 |
| `21d8814d-d8af-4be7-a141-33ece3df1008` | `1ThdhYYAcs5hzVlUMdcRyS5TV0fKXJazV` | PE-22A/ALCANTARILLAS/ALCANTARILLA 2 |
| `53524c18-69d3-4ede-966c-32da68f11642` | `1vh6fizYLHtRwUIhZ8c8SLWfoE-AT_vP-` | PE-22A/FAJAS_CALZADA/FAJA 1 |
| `e40cebdf-8039-4c6c-969a-0d7db73b8a94` | `1lxlgd4LL7KKs6GbtyoZM3nhof1dCCb2i` | PE-22A/FAJAS_CALZADA/FAJA 2 |

## Archivos finales recibidos

| Clave | ID | Ruta relativa / lógica declarada por ACK |
|---|---|---|
| `photo/337ca20c-c40a-401c-bd22-61a44bcd64cc` | `1dQys05jjfwni-3X0ebruitvi1zzPaEHQ` | PE-22A/ALCANTARILLAS/ALCANTARILLA 1/ENTRADA/F_d4df6bf44c810344.jpg |
| `photo/d9517d4a-e9e4-4180-845f-337f2c25e8ec` | `1DtLCiOaUYP7ZbZOForTGxtwZJ4Ye3vEL` | PE-22A/ALCANTARILLAS/ALCANTARILLA 1/SALIDA/F_f480ed6faf078a39.jpg |
| `metadata` | `1RycD2xaoL8nn2hnIs45FBZ6OoAkYl648` | PE-22A/ALCANTARILLAS/ALCANTARILLA 1/metadata.json |
| `SIC-18` | `1tv1viCM-jbB7-L3rp77xevTZ34qalTtn` | PE-22A/EXCEL_SIC/SIC-18.xlsx |
| `photo/eae70d0f-e274-4ba2-9679-fcfbdfeaa438` | `1uxTApbwdQajDksuqu_9pSKOSUcjRxOkZ` | PE-22A/ALCANTARILLAS/ALCANTARILLA 2/ENTRADA/F_ebe4b75ec1a30781.jpg |
| `photo/66044bfe-7613-4156-b206-ff023f07b8b7` | `13MWf_d3hI0cUlCLTD736KX_ZVy4sWrB9` | PE-22A/ALCANTARILLAS/ALCANTARILLA 2/SALIDA/F_47545b4c25eb8a5b.jpg |
| `metadata` | `1feAkIhFPQCqLbBnSREK6gNcp5CS3uSLM` | PE-22A/ALCANTARILLAS/ALCANTARILLA 2/metadata.json |
| `metadata` | `1wocJ5Sxyho2dhetucwx7xwWNWk_MTaNz` | PE-22A/FAJAS_CALZADA/FAJA 1/metadata.json |
| `SIC-08` | `1TqTHGZGdjWhxirRo81Frlk2SgiiIlwzX` | PE-22A/EXCEL_SIC/SIC-08.xlsx |
| `photo/c16e441e-481b-375c-94d5-43328227251a` | `1ad2sDQFlZwUfHpR2FJ0aY28UK83_qHA3` | PE-22A/FAJAS_CALZADA/FAJA 2/GENERAL/F_011d88f4ab63331f.jpg |
| `metadata` | `1GkqN_HHMuNhun86C5_MGAL-PgL_086xR` | PE-22A/FAJAS_CALZADA/FAJA 2/metadata.json |

Hashes, categorías, orden y revisiones están en [ARCHIVOS_ACK.json](evidencia/ARCHIVOS_ACK.json), [ANDROID_REAL.json](evidencia/ANDROID_REAL.json) y [OFFLINE_WORKMANAGER_REAL.json](evidencia/OFFLINE_WORKMANAGER_REAL.json). Las fotos remotas permanecen bajo las carpetas categorizadas del elemento; carpetas y pertenencia leídas independientemente en [CARPETAS_FOTOGRAFICAS.json](evidencia/CARPETAS_FOTOGRAFICAS.json).

## Archivos internos del servidor

El backend inicializó `._GBO_SYNC_V3` (`1hId-uBumusk5AdOFmeTbj7huPN4B35A4`) y `STAGING` (`1PXumCvvLr_P6jRELFcZlxPJksvjeS0sc`). Los journals, bindings, record y aggregate observados, así como rutas/familias/EXCEL_SIC, figuran con IDs en [ESTRUCTURA_DRIVE.json](evidencia/ESTRUCTURA_DRIVE.json). No se descargaron contenidos de esos archivos internos ni se hizo una auditoría recursiva de versiones internas/staging.

El staging negativo utilizó `SIC/fbc976e2-7760-4e5a-8c16-65e701b337b8` y no creó un registro final. Los intentos iniciales de fixture fallaron antes de enviar entregas y no se incluyen como registros remotos. Los borradores locales y datos de emuladores previos se conservaron.

Las filas contractuales se reconocen por PE-22A/CD/PR0010 y distancias 12, 23, 34 y 45 m. UUID y marca PRUEBA 8B constan en metadatos/observaciones; los Excel contractuales no tienen una columna añadida de prueba. Cualquier eliminación futura requiere autorización específica y conservar coherencia del registro interno.
