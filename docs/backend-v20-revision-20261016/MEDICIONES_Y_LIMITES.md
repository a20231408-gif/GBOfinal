# Operaciones y tiempos antes/después

**Medición real del proceso local; servicios Google simulados. Cero ejecuciones Google.** Node 24.19.0, Windows, Drive/LockService en memoria y ZIP real mediante Python. Los tiempos incluyen creación de procesos Python, copia de blobs y serialización local. No incluyen Internet, espera real de 30 segundos, Drive remoto ni competencia de ejecuciones Google. La estación no es un banco de rendimiento aislado de otras tareas; tres repeticiones son una muestra pequeña, sin p95 representativo.

## Cinco commits, una fila por alta, mismo formato/ámbito

Los cuatro perfiles se ejecutan secuencialmente con orden rotado, tres repeticiones por perfil. Mismos payloads ficticios. Fechas ZIP fijas para exigir igualdad exacta de ACK, SHA y bytes entre perfiles. El contador registra llamadas reales de ese motor ejecutado dentro del arnés.

| Perfil | unzip | zip | decode base64 | SHA-256 | Llamadas a dobles Drive* | Mediana total | Rango total |
|---|---:|---:|---:|---:|---:|---:|---:|
| Original V20 | 20 | 5 | 30 | 190 | 793 | 5,710 s | 5,194–5,879 s |
| Solo staging fuera del lock | 20 | 5 | 30 | 190 | 793 | 5,215 s | 4,819–5,242 s |
| Ambos borradores | 15 | 5 | 25 | 175 | 793 | 4,697 s | 4,286–4,884 s |
| Ambos + instrumentación anterior | 15 | 5 | 25 | 175 | 793 | 4,648 s | 4,223–4,690 s |

*Incluye getters de Folder/File en memoria, no equivale al número de solicitudes HTTP ni al consumo de cuotas Google.*

**Ahorro probado:** cinco unzip menos (25%), cinco decodificaciones menos y quince SHA de inspección de plantilla menos. No se omite el SHA de entrada; se evita repetir el trabajo del parser al reutilizar la fila actual ya validada. Cinco zip y las mismas llamadas Drive: el parche no reduce por sí mismo escrituras de consolidados, copias o journals.

Diferencia de medianas observada V20 → integrado: −1,062 s para el lote ficticio (18,6%). Es un resultado local, no una predicción Google. El perfil «solo staging» también varía de tiempo aunque estos commits no invocan staging y sus operaciones son idénticas: evidencia de ruido/efecto de orden/host. Por tanto no se atribuye cada diferencia temporal al código ni se ofrece un porcentaje de aceleración productiva. Los conteos son la evidencia más estable.

## Dos muestras con cinco filas cada una

Una repetición por perfil, cabecera SIC-14 de la fixture Android. Se conservan filas/ACK/hashes entre variantes.

| Perfil | unzip | zip | Total local |
|---|---:|---:|---:|
| V20 | 25 | 2 | 5,676 s |
| Ambos borradores | 15 | 2 | 2,668 s |
| Integrado instrumentado | 15 | 2 | 2,679 s |

Ahorro de operaciones: diez unzip (40%). Tiempo de una sola muestra estadística por perfil; no generalizar a tamaños reales. Las cinco filas usan libros ficticios derivados de la misma fixture; no representan la diversidad y peso de todas las plantillas o fotografías de campo.

## Staging: veinte solicitudes de 256 KiB, tres repeticiones

Se repite la misma identidad/archivo: primer staging y replays que verifican el archivo existente. No es una carga de veinte fotografías nuevas ni de videos.

| Perfil | Mediana total local | Mediana lock ocupado | Decode dentro/fuera | SHA dentro/fuera |
|---|---:|---:|---:|---:|
| V20 | 1,880 s | 1,879 s | 20 / 0 | 80 / 0 |
| Solo staging | 1,901 s | 1,238 s | 0 / 20 | 60 / 20 |
| Ambos borradores | 1,823 s | 1,207 s | 0 / 20 | 60 / 20 |
| Integrado instrumentado | 1,847 s | 1,216 s | 0 / 20 | 60 / 20 |

Se demuestra el traslado de veinte decode y veinte SHA fuera del lock. Continúan los hashes de identidad y relectura del archivo bajo lock. No se reduce sustancialmente el trabajo total de CPU ni se elimina la espera por un commit que consolida Excel. No se midió aquí una contención real de clientes: BUSY se inyecta como rechazo inmediato, sin dormir 30 segundos.

## Evidencia y reproducibilidad

- `evidencia/mediciones-locales.json`: repeticiones completas, ACK, contadores dentro/fuera, tiempos por operación y perfiles.
- `evidencia/mediciones-locales.log`: salida final del programa, sin datos de producción.
- `evidencia/mediciones-primer-arnes.log`: primer intento de comparación interrumpido por prototipos de objetos entre contextos VM de Node, no por datos distintos. Se normalizó JSON para comparar respuestas, igual que en la suite. Se conserva el fallo; ningún .gs se cambió para resolverlo.
- `pruebas/MedicionesV20.cjs`: ejecutable local con los originales/copias del ZIP en el tmp indicado por el arnés. No tiene clientes HTTP.

No se adjudica un resultado de 28/28 ACK de simulaciones anteriores al servidor real, ni estos segundos a la incidencia 2/17 SIC y 1/11 Pavimentos.

## Medición Google pendiente

En una copia aislada autorizada, usar las fuentes V20 y luego el candidato, misma raíz ficticia y un solo proyecto escritor, manifiesto/servicios equivalentes y tamaños representativos. Dos clientes/usuarios de prueba. Separar SIC, SCAP y Pavimentos, registros y cantidad de archivos. Verificar reintentos de BUSY, pérdidas de respuesta e interrupciones, y comparar UUID/revisión/ACK completo contra archivos y filas.

Capturar por ejecución: operación, espera del lock, ocupación, recuperación, unpack/pack, transacción y ACK; en cliente, tiempo de extremo a extremo y bytes/reintentos. La propuesta `v3LockTiming` solo registra campos numéricos y operación; no incluye UUID, fotos, usuario, URL ni token. Sus fases son anidadas: no sumarlas. Video conserva su protocolo, pero su sesión/binario no queda desglosado por esta instrumentación. Esa evidencia adicional es necesaria para atribuir el retraso observado y decidir si implementar la segunda propuesta.
