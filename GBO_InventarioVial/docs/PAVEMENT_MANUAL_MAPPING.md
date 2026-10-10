# Validación de Pavimentos contra el manual

Fuente: `docs/Manual_PCI_URCI.pdf`, idéntico al adjunto del usuario; 286 páginas. SHA-256: `a83c5efa2d5a045f0f0c2e197783ccff5b1b6c4248da3f2a8f39e3022ca9394d`.

Las páginas citadas son la numeración impresa del manual, coincidente con el índice PDF. Capítulo 5: asfalto 69–132, concreto 133–190, vías no pavimentadas 225–238. El catálogo técnico empaquetado contiene los extractos de medición y severidad; se cotejó con esos intervalos del PDF adjunto.

`Geometría de croquis` describe un símbolo conceptual implementado a partir de la descripción del deterioro. El manual sustenta la clase, medida y severidad; no prescribe estas recetas de Canvas. La receta no cambia cantidades ni medidas. El HTML original se revisó solo como referencia visual. No se presentan símbolos como mediciones fotogramétricas.

| Código manual | Nombre del manual | Superficie | Unidad | Severidades válidas | Geometría de croquis | Página / sección |
|---|---|---|---|---|---|---|
| 1 | Piel de cocodrilo (fatiga) | Asfalto | m² | L / M / H | NETWORK | 69–72 · cap.5, daño 1 |
| 2 | Exudación | Asfalto | m² | L / M / H | AREA | 73–75 · cap.5, daño 2 |
| 3 | Agrietamiento en bloque | Asfalto | m² | L / M / H | NETWORK | 76–78 · cap.5, daño 3 |
| 4 | Abultamientos y hundimientos | Asfalto | m | L / M / H | TRANSVERSE_PATTERN | 79–81 · cap.5, daño 4 |
| 5 | Corrugación | Asfalto | m² | L / M / H | TRANSVERSE_PATTERN | 82–84 · cap.5, daño 5 |
| 6 | Depresión | Asfalto | m² | L / M / H | DEPRESSION | 85–87 · cap.5, daño 6 |
| 7 | Grieta de borde | Asfalto | m | L / M / H | IRREGULAR_LINE | 88–90 · cap.5, daño 7 |
| 8 | Grieta de reflexión de juntas de PCCP | Asfalto | m | L / M / H | IRREGULAR_LINE | 91–94 · cap.5, daño 8 |
| 9 | Desnivel carril / berma | Asfalto | m | L / M / H | LINE | 95–97 · cap.5, daño 9 |
| 10 | Grietas longitudinales y transversales | Asfalto | m | L / M / H | IRREGULAR_LINE | 98–101 · cap.5, daño 10 |
| 11 | Parcheo y acometidas de servicios | Asfalto | m² | L / M / H | PATCH | 102–105 · cap.5, daño 11 |
| 12 | Pulimento de agregados | Asfalto | m² | No aplica | AREA | 106–107 · cap.5, daño 12 |
| 13 | Huecos | Asfalto | Un. | L / M / H | POINT | 108–110 · cap.5, daño 13 |
| 14 | Cruce de vía férrea | Asfalto | m² | L / M / H | TRANSVERSE_PATTERN | 111–113 · cap.5, daño 14 |
| 15 | Ahuellamiento | Asfalto | m² | L / M / H | LONGITUDINAL_BANDS | 114–116 · cap.5, daño 15 |
| 16 | Desplazamiento | Asfalto | m² | L / M / H | SWELL | 117–119 · cap.5, daño 16 |
| 17 | Grieta parabólica o por deslizamiento | Asfalto | m² | L / M / H | PARABOLIC | 120–122 · cap.5, daño 17 |
| 18 | Hinchamiento | Asfalto | m² | L / M / H | SWELL | 123–124 · cap.5, daño 18 |
| 19 | Desprendimiento de agregado grueso | Asfalto | m² | M / H | SCATTER | 125–126 · cap.5, daño 19 |
| 20 | Meteorización (desgaste superficial) | Asfalto | m² | L / M / H | SCATTER | 127–132 · cap.5, daño 20 |
| 21 | Rotura por pandeo | Concreto | losas | L / M / H | TRANSVERSE_PATTERN | 133–135 · cap.5, daño 21 |
| 22 | Rotura de la esquina | Concreto | losas | L / M / H | CORNER | 136–139 · cap.5, daño 22 |
| 23 | Losa dividida | Concreto | losas | L / M / H | NETWORK | 140–142 · cap.5, daño 23 |
| 24 | Grieta de durabilidad (“D”) | Concreto | losas | L / M / H | NETWORK | 143–146 · cap.5, daño 24 |
| 25 | Escalonamiento | Concreto | losas | L / M / H | JOINT | 147–149 · cap.5, daño 25 |
| 26 | Daño del sello de junta | Concreto | Global (sin densidad) | L / M / H | GLOBAL | 150–153 · cap.5, daño 26 |
| 27 | Desnivel carril / berma | Concreto | losas | L / M / H | LINE | 154–156 · cap.5, daño 27 |
| 28 | Grietas lineales (longitudinales transversales y diagonales) | Concreto | losas | L / M / H | IRREGULAR_LINE | 157–160 · cap.5, daño 28 |
| 29 | Parcheo grande (> 0.5 m²) y acometidas de servicios | Concreto | losas | L / M / H | PATCH | 161–163 · cap.5, daño 29 |
| 30 | Parcheo pequeño (≤ 0.5 m²) | Concreto | losas | L / M / H | PATCH | 164–166 · cap.5, daño 30 |
| 31 | Pulimento de agregados | Concreto | losas | No aplica | AREA | 167–168 · cap.5, daño 31 |
| 32 | Desprendimientos | Concreto | losas | No aplica | SCATTER | 169–170 · cap.5, daño 32 |
| 33 | Bombeo | Concreto | losas | No aplica | JOINT | 171–172 · cap.5, daño 33 |
| 34 | Punzonamiento | Concreto | losas | L / M / H | SLAB | 173–175 · cap.5, daño 34 |
| 35 | Cruce de vía férrea | Concreto | losas | L / M / H | TRANSVERSE_PATTERN | 176–178 · cap.5, daño 35 |
| 36 | Descascaramiento por agrietamiento | Concreto | losas | L / M / H | NETWORK | 179–181 · cap.5, daño 36 |
| 37 | Grietas de retracción | Concreto | losas | No aplica | IRREGULAR_LINE | 182–182 · cap.5, daño 37 |
| 38 | Astillamiento de esquina | Concreto | losas | L / M / H | CORNER | 183–185 · cap.5, daño 38 |
| 39 | Astillamiento de junta | Concreto | losas | L / M / H | JOINT | 186–190 · cap.5, daño 39 |
| 81 | Sección transversal inadecuada | No pavimentado | m | L / M / H | TRANSVERSE_PATTERN | 225–226 · cap.5, daño 81 |
| 82 | Drenaje lateral inadecuado | No pavimentado | m | L / M / H | LINE | 227–228 · cap.5, daño 82 |
| 83 | Corrugaciones | No pavimentado | m² | L / M / H | TRANSVERSE_PATTERN | 229–230 · cap.5, daño 83 |
| 84 | Polvo | No pavimentado | Global (sin densidad) | L / M / H | GLOBAL | 231–232 · cap.5, daño 84 |
| 85 | Huecos | No pavimentado | Un. | L / M / H | POINT | 233–234 · cap.5, daño 85 |
| 86 | Huellas | No pavimentado | m² | L / M / H | LONGITUDINAL_BANDS | 235–236 · cap.5, daño 86 |
| 87 | Agregado suelto | No pavimentado | m | L / M / H | LINE | 237–238 · cap.5, daño 87 |

## Reglas de medición conservadas o corregidas

- Unidad asfáltica y no pavimentada: área recomendada 230 ± 90 m², advertencia fuera de 140–320 m² (pp.22/39). Concreto: 20 ± 8 losas, juntas con espaciamiento menor de 8 m; filas orientadas longitudinalmente. Las losas largas requieren subdivisión explícita con juntas imaginarias antes del cálculo (pp.39/159).
- Concreto: losas únicas afectadas / total de losas × 100. Sello de junta 26 se evalúa globalmente; no se calcula densidad por losas (pp.150–153/254).
- Esquina 22, D 24, parches 29/30 y astillamientos 38/39: conservar el mayor nivel por losa para cada código. Losa dividida 23 M/H excluye otros deterioros de esa losa (p.140). Dos grietas lineales 28 M en la misma losa se cuentan H (p.159); cada registro representa una grieta/observación distinta, sin duplicar registros para el mismo fenómeno.
- Punzonamiento 34: p.173 describe dos recomendaciones alternativas. La captura debe clasificarlo según el criterio técnico adoptado; no se elige automáticamente una de esas recomendaciones. Bombeo 33 exige seleccionar las losas afectadas, incluidas ambas losas de una junta según p.171.
- Huecos asfálticos mayores de 0.75 m: cantidad equivalente = área **medida** / 0.47 m², M si profundidad ≤25 mm, H si >25 mm (p.108). Huecos no pavimentados mayores de 0.90 m: área **medida** / 0.65 m² (p.233). No se sustituye área medida por πr²; datos históricos sin área quedan pendientes.
- Los límites ambiguos de cuadros de severidad se conservan como selección manual; las sugerencias de huecos no resuelven por cuenta propia fronteras compartidas o intervalos ausentes.
- URCI 81: máximo largo de unidad (p.225). URCI 82: máximo 2 × largo (p.227). URCI 87: cada cordón se mide por separado; no imponer un límite de una sola longitud (p.237).
- Las curvas VD/CDV del baseline tienen 0 curvas digitalizadas/validadas. Se conserva la negativa a extrapolar/inventar valores y se muestra pendiente cuando falta una curva. PCI: umbral 2 y denominador 98 (pp.52–54); URCI: umbral 5 y denominador 95 (pp.59–60). Las iteraciones siguen en cálculo y snapshot, sin hoja visual en Excel.
- Severidad nueva se almacena L/M/H; B/A históricos se traducen en lectura y migración. Sin severidad es null, nunca una severidad de color. Amarillo/naranja/rojo son presentación solicitada por el usuario.
- Las grietas usan UUID + algoritmo determinístico en coordenadas métricas. Inicio/fin son exactos; solo varían puntos intermedios. Canvas y PDF invocan la misma función. El recorte PDF no desplaza ni recalcula los daños.

## Separación de SIC

Los códigos anteriores son PCI/URCI del manual y no son los códigos contractuales SIC. Las cuatro salidas usan las columnas de las imágenes adjuntas; las correspondencias no inequívocas requieren captura explícita. La contradicción 07/13 de SIC-14A está documentada y bloquea esas filas. Ver `PARALLEL_C_INTEGRATION.md`.
