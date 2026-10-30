# Pruebas locales sobre fuentes actuales

**25 PASS, 0 FAIL.** Ejecución Node local, con ocho fuentes actuales completas o sus nueve fuentes integradas, simulador Drive/Properties/Lock/ContentService y ZIP real mediante Python. Se probó también el conjunto de trece módulos del proyecto aislado con reinicialización del contexto entre funciones, como nuevas ejecuciones del editor.

| Grupo | Casos | Evidencia |
|---|---:|---|
| Compatibilidad de carga SCAP | 9 | 54 fotos, 3 croquis, SCAP/manifiesto, 3 SIC, hashes, staging, ACK, revisión/rollback, historia y módulos ausentes |
| Catálogo y contratos | 9 | 20 categorías; reservas; carpeta estable; dos UUID; BUSY; rechazo de contradicciones; videos/reservas; funciones contractuales idénticas |
| Ensayo y aislamiento | 7 | Raíz/propiedades equivocadas; IDs externos; epoch obsoleto; atajos; escritura tras movimiento; solo lectura; setup idempotente; capacidades; flujo completo del editor |

`evidencias/REGRESIONES_LOCALES.tap` conserva los primeros 24 casos; tras la comprobación adicional de inflight se ejecutó nuevamente solo el grupo afectado: 7/7 PASS en `REGRESIONES_EJECUTOR_FINAL.tap`. Son 25 casos distintos aprobados (18 de backend y 7 del ejecutor), no 31 casos independientes. `RESULTADO_DRIVER_LOCAL.json` está marcado LOCAL_GOOGLE_MOCK_NOT_GOOGLE: su PASS no equivale a un ensayo ejecutado contra Google. `VALIDACION_XLSX_Y_GLOBALES.json` confirma cuatro XLSX válidos y ausencia de declaraciones globales duplicadas. El resultado de conservación incluye el cotejo SHA de fuentes/Android/artefactos protegidos.

Se corrigieron dos adaptaciones del arnés anterior al TextOutput real (parseo del BUSY y ruta del ACK) y una comprobación del test que debía simular una ejecución nueva tras generar la credencial. No eran fallos del backend. El cursor del nuevo verificador se guarda después de verificar también duplicados y carpeta, para no saltarse una comprobación fallida. Los archivos de corridas locales anteriores se conservaron.

Para repetir localmente tras descomprimir: Node y Python deben estar disponibles. Se puede definir GBO_TEST_PYTHON con la ruta a Python. Ejecutar `node --test pruebas/*.test.cjs` desde esta carpeta. Usar `GBO_ACK_OUTPUT` con un nombre NUEVO terminado en .json dentro de fixtures para conservar ACK de cada corrida; el arnés rechaza sobrescribir uno existente. Sin esa variable se conserva el comportamiento de escribir ACK_CANDIDATO_54.json (ya existe como evidencia).

No se ejecutaron Android físico, Google real, red HTTP del despliegue, reproducción de videos ni mediciones de rendimiento en esta entrega. No se tocó configuración, código productivo ni APK; las modificaciones previas del proyecto Android siguen preservadas.
