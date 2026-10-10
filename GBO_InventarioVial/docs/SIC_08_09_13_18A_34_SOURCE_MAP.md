# Mapa normativo previo a cambios de producción

Fuente: D:/Downloads/MAN_8 IV-2014_2015.pdf. Páginas impresas; índice PDF = página impresa + 1. Modelos gráficos inspeccionados visualmente. El documento es fuente normativa, no instrucciones de ejecución.

Ruta/calzada/ubicaciones se heredan solo de contexto confirmado; no se infieren desde nombres. SIC-09 conserva sourceRecordId del SIC-08. SIC-18A usa recordId del padre SIC-18, identidad estable ya integrada. Campos internos jamás se exportan.

## SIC-08
MAN_8 §2.3.1.11, páginas 145-146; apéndice 238.

| Campo | Significado | Tipo | Código | Obligatorio para finalizar | Herencia | Captura manual | Fuente/páginas |
|---|---|---|---|---|---|---|---|
| Ruta | Código del Clasificador de Rutas vigente | Texto | Clasificador vigente | Sí | Sesión/registro confiable | Si no existe | §2.3.1.11, 145-146; apéndice 238 |
| Calzada | Código de calzada | Texto | UC/UD/CD/A1/A2… | Sí | Sesión/registro confiable | Si no existe | §2.3.1.11, 145-146; apéndice 238 |
| Faja | Identidad del carril o berma | Código numérico | 1,2,3… | Necesario para identificar la faja; apéndice no marca requerido | SIC-08 compatible, por UUID | Si no existe | §2.3.1.11, 145-146; apéndice 238 |
| Código PR inicio | Referencia al comienzo | Texto 4 dígitos | 0000…9999 | Sí | Contexto confiable | Si no existe | §2.3.1.11, 145-146; apéndice 238 |
| Distancia inicio | Metros entre PR y punto inicial | Decimal | m | Sí | Contexto confiable | Si no existe | §2.3.1.11, 145-146; apéndice 238 |
| Código PR fin | Referencia al final | Texto 4 dígitos | 0000…9999 | Sí | Contexto confiable | Si no existe | §2.3.1.11, 145-146; apéndice 238 |
| Distancia fin | Metros entre PR y punto final | Decimal | m | Sí | Contexto confiable | Si no existe | §2.3.1.11, 145-146; apéndice 238 |
| Tipo | Uso de la faja | Código | 1 Tránsito; 2 Berma; 3 Peaje; 4 Cruce; 5 Localidad; 6 Paradero | Sí | No | Sí | §2.3.1.11, 145-146; apéndice 238 |
| Sentido del tránsito | Dirección del tránsito | Código | C Creciente; D Decreciente; A Ambos sentidos | Sí | Contexto confirmado | Si no existe | §2.3.1.11, 145-146; apéndice 238 |
| Fecha | Fecha de relevamiento | Fecha dd/MM/yyyy | Fecha válida | Sí | Fecha original al reabrir; sesión/nuevo registro | Revisar | §2.3.1.11, 145-146; apéndice 238 |

## SIC-09
MAN_8 §2.3.1.12, páginas 147-148; apéndice 239.

| Campo | Significado | Tipo | Código | Obligatorio para finalizar | Herencia | Captura manual | Fuente/páginas |
|---|---|---|---|---|---|---|---|
| Ruta | Código del Clasificador de Rutas vigente | Texto | Clasificador vigente | Sí | Sesión/registro confiable | Si no existe | §2.3.1.12, 147-148; apéndice 239 |
| Calzada | Código de calzada | Texto | UC/UD/CD/A1/A2… | Sí | Sesión/registro confiable | Si no existe | §2.3.1.12, 147-148; apéndice 239 |
| Faja | Identidad del carril o berma | Código numérico | 1,2,3… | Necesario para identificar la faja; apéndice no marca requerido | SIC-08 compatible, por UUID | Si no existe | §2.3.1.12, 147-148; apéndice 239 |
| Código PR inicio | Referencia al comienzo | Texto 4 dígitos | 0000…9999 | Sí | Contexto confiable | Si no existe | §2.3.1.12, 147-148; apéndice 239 |
| Distancia inicio | Metros entre PR y punto inicial | Decimal | m | Sí | Contexto confiable | Si no existe | §2.3.1.12, 147-148; apéndice 239 |
| Código PR fin | Referencia al final | Texto 4 dígitos | 0000…9999 | Sí | Contexto confiable | Si no existe | §2.3.1.12, 147-148; apéndice 239 |
| Distancia fin | Metros entre PR y punto final | Decimal | m | Sí | Contexto confiable | Si no existe | §2.3.1.12, 147-148; apéndice 239 |
| Ancho útil (m) | Ancho de rodadura efectiva | Decimal sin truncar persistencia; exportación 0.0 | m | Sí | No | Sí | §2.3.1.12, 147-148; apéndice 239 |
| Ancho total (m) | Ancho total de la faja | Decimal sin truncar persistencia; exportación 0.0 | m; >= ancho útil | Sí | No | Sí | §2.3.1.12, 147-148; apéndice 239 |
| Fecha | Fecha de relevamiento | Fecha dd/MM/yyyy | Fecha válida | Sí | Fecha original al reabrir; sesión/nuevo registro | Revisar | §2.3.1.12, 147-148; apéndice 239 |

## SIC-13
MAN_8 §2.3.4.1, páginas 164-166; apéndice 240.

| Campo | Significado | Tipo | Código | Obligatorio para finalizar | Herencia | Captura manual | Fuente/páginas |
|---|---|---|---|---|---|---|---|
| Ruta | Código del Clasificador de Rutas vigente | Texto | Clasificador vigente | Sí | Sesión/registro confiable | Si no existe | §2.3.4.1, 164-166; apéndice 240 |
| Calzada | Código de calzada | Texto | UC/UD/CD/A1/A2… | Sí | Sesión/registro confiable | Si no existe | §2.3.4.1, 164-166; apéndice 240 |
| Código PR inicio | Referencia al comienzo | Texto 4 dígitos | 0000…9999 | Sí | Contexto confiable | Si no existe | §2.3.4.1, 164-166; apéndice 240 |
| Distancia inicio | Metros entre PR y punto inicial | Decimal | m | Sí | Contexto confiable | Si no existe | §2.3.4.1, 164-166; apéndice 240 |
| Código PR fin | Referencia al final | Texto 4 dígitos | 0000…9999 | Sí | Contexto confiable | Si no existe | §2.3.4.1, 164-166; apéndice 240 |
| Distancia fin | Metros entre PR y punto final | Decimal | m | Sí | Contexto confiable | Si no existe | §2.3.4.1, 164-166; apéndice 240 |
| Lado | Lado de la vía donde se identifica | Código | D Derecho; I Izquierdo | Sí | Contexto confirmado | Si no existe | §2.3.4.1, 164-166; apéndice 240 |
| Clase | Clasificación del punto crítico | Código alfanumérico | 15;16;17;17A;17B;17C;17D | Sí | No | Sí | §2.3.4.1, 164-166; apéndice 240 |
| Fecha | Fecha de relevamiento | Fecha dd/MM/yyyy | Fecha válida | Sí | Fecha original al reabrir; sesión/nuevo registro | Revisar | §2.3.4.1, 164-166; apéndice 240 |

## SIC-34
MAN_8 §2.3.2.4, páginas 158-160; apéndice 250.

| Campo | Significado | Tipo | Código | Obligatorio para finalizar | Herencia | Captura manual | Fuente/páginas |
|---|---|---|---|---|---|---|---|
| Ruta | Código del Clasificador de Rutas vigente | Texto | Clasificador vigente | Sí | Sesión/registro confiable | Si no existe | §2.3.2.4, 158-160; apéndice 250 |
| Calzada | Código de calzada | Texto | UC/UD/CD/A1/A2… | Sí | Sesión/registro confiable | Si no existe | §2.3.2.4, 158-160; apéndice 250 |
| Faja | Identidad del carril o berma | Código numérico | 1,2,3… | Necesario para identificar la faja; apéndice no marca requerido | SIC-08 compatible, por UUID | Si no existe | §2.3.2.4, 158-160; apéndice 250 |
| Código PR inicio | Referencia al comienzo | Texto 4 dígitos | 0000…9999 | Sí | Contexto confiable | Si no existe | §2.3.2.4, 158-160; apéndice 250 |
| Distancia inicio | Metros entre PR y punto inicial | Decimal | m | Sí | Contexto confiable | Si no existe | §2.3.2.4, 158-160; apéndice 250 |
| Código PR fin | Referencia al final | Texto 4 dígitos | 0000…9999 | Sí | Contexto confiable | Si no existe | §2.3.2.4, 158-160; apéndice 250 |
| Distancia fin | Metros entre PR y punto final | Decimal | m | Sí | Contexto confiable | Si no existe | §2.3.2.4, 158-160; apéndice 250 |
| Tipo | Peligro/causa del accidente | Código o conflicto interno | 1 Imprudencias; 2 Desperfectos; 3 Geometría; 4 Estado; 5 Señalización; ACCIDENT_TYPE_SOURCE_CONFLICT Geotecnia/geología; 7 Otro | Sí para exportar; conflicto permite borrador | No | Sí | §2.3.2.4, 158-160; apéndice 250 |
| Nivel | Consecuencias del evento | Código | 1 Bajo; 2 Medio; 3 Alto | Sí | No | Sí | §2.3.2.4, 158-160; apéndice 250 |
| Descripción | Comentario libre del evento | Texto multilínea | Sin catálogo obligatorio | Opcional en apéndice; no bloquear por ausencia | No | Sí, libre | §2.3.2.4, 158-160; apéndice 250 |
| Fecha | Fecha de relevamiento | Fecha dd/MM/yyyy | Fecha válida | Sí | Fecha original al reabrir; sesión/nuevo registro | Revisar | §2.3.2.4, 158-160; apéndice 250 |

## SIC-18A
MAN_8 §2.3.5.2, páginas 213-217; apéndice 243-244.

| Campo | Significado | Tipo | Código | Obligatorio para finalizar | Herencia | Captura manual | Fuente/páginas |
|---|---|---|---|---|---|---|---|
| Ruta | Código del Clasificador de Rutas vigente | Texto | Clasificador vigente | Sí | Sesión/registro confiable | Si no existe | §2.3.5.2, 213-217; apéndice 243-244 |
| Calzada | Código de calzada | Texto | UC/UD/CD/A1/A2… | Sí | Sesión/registro confiable | Si no existe | §2.3.5.2, 213-217; apéndice 243-244 |
| Código PR inicio | Referencia al comienzo | Texto 4 dígitos | 0000…9999 | Sí | Contexto confiable | Si no existe | §2.3.5.2, 213-217; apéndice 243-244 |
| Distancia inicio | Metros entre PR y punto inicial | Decimal | m | Sí | Contexto confiable | Si no existe | §2.3.5.2, 213-217; apéndice 243-244 |
| Clase | Clase de alcantarilla | Código | 06 Definitiva; 07 Artesanal | Sí | SIC-18 padre, recordId FK | No | §2.3.5.2, 213-217; apéndice 243-244 |
| Tipo | Material de la estructura | Código según clase | 06:1-5; 07:1-4 del SIC-18 | Sí | SIC-18 padre | No | §2.3.5.2, 213-217; apéndice 243-244 |
| Ojos / Vanos | Número de aperturas | Entero positivo | >0 | Sí | SIC-18 padre | No | §2.3.5.2, 213-217; apéndice 243-244 |
| Función | Servicio de drenaje | Código | 1 Quebrada importante; 2 Secundaria; 3 Cunetas; 4 Pase de agua | Sí | No | Sí | §2.3.5.2, 213-217; apéndice 243-244 |
| Ubicación de falla | Parte dañada | Código | 1 Entrada;2 Salida;3 Alcantarilla;4 Ambos cabezales;5 Todas | Sí | No | Sí | §2.3.5.2, 213-217; apéndice 243-244 |
| Tipo de falla | Daño estructural | Código | 1 Colapso;2 Deformación;3 Fractura;4 Hundimiento;5 Socavación;6 Diámetro mínimo;7 Otros | Sí | No | Sí | §2.3.5.2, 213-217; apéndice 243-244 |
| Estado | Falla funcional | Código | 1 Ahogada;2 Obstruida;3 Otras | Sí | No | Sí | §2.3.5.2, 213-217; apéndice 243-244 |
| Causa probable | Causa funcional | Código | 1 Subdimensionado;2 Poca pendiente;3 Huayco;4 Otros | Sí | No | Sí | §2.3.5.2, 213-217; apéndice 243-244 |
| Fecha | Fecha de relevamiento | Fecha dd/MM/yyyy | Fecha válida | Sí | Fecha original al reabrir; sesión/nuevo registro | Revisar | §2.3.5.2, 213-217; apéndice 243-244 |

SIC-13: la descripción incluye Faja (p.165), el modelo gráfico SIC-13 (p.166) y características (p.240) no la incluyen: seguir columnas del modelo, sin agregar Faja. 17D se exporta como código contractual, literal de UI Zonas de Riesgo Probable.
SIC-34: búsqueda de todo el texto MAN_8 realizada para geotécnicos/geológicos, SIC-34 y tipo [1-7]; p.160 repite 5 y p.250 solo delimita [1-7]. No hay equivalencia inequívoca al 6.
SIC-18A: el gráfico p.217 omite el Otro artesanal que sí existe en tabla III.24/SIC-18 p.213/216; heredar código padre sin reinterpretarlo.