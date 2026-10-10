# SIC UI: antes y después

Documento conceptual basado en los componentes Compose reales. No son screenshots de un dispositivo; no hay tablet/AVD conectado para una validación física.

| Antes: entradas por formato | Después: entradas por elemento |
|---|---|
| SIC-08 · Fajas y SIC-09 · Anchos | **Fajas** · SIC-08 / SIC-09 |
| Alcantarilla y SIC-18A separado | **Alcantarillas** · SIC-18 / SIC-18A cuando corresponde |
| SIC-13 · Puntos críticos | **Puntos críticos** · SIC-13 secundario |
| SIC-34 · Accidentes | **Accidentes** · SIC-34 secundario |

```text
INVENTARIO VIAL
  CALZADA Y FAJAS
    Fajas
      Lista de fajas → Abrir / + Nueva faja
      Identificación común: ruta, calzada, faja, fecha
      Ubicación inicio / Ubicación fin
        GPS resumido → Mostrar detalles → Capturar GPS
      Características de la faja · SIC-08
        Tipo / Sentido
      Ancho de la faja · SIC-09
        Ancho útil / Ancho total
        Anchos pendientes → Completar anchos
      Fotografías · General / miniaturas / preview
      Guardar borrador / Revisar y finalizar / Exportar
        SIC-08.xlsx + SIC-09.xlsx cuando estén completos
  SEGURIDAD VIAL
    Señalización y seguridad · SIC-21 / SIC-22
    Puntos críticos · SIC-13
    Accidentes · SIC-34
  OBRAS DE ARTE Y DRENAJE
    Alcantarillas
      Ficha base madura SIC-18
      Ubicación / clase / tipo / ojos / sección / dimensiones
      Condición estructural y funcional
        Si alguna = 3 → Completar detalle de condición mala
          Función
          Falla estructural: ubicación / tipo
          Falla funcional: estado / causa probable
          Cabecera heredada; sin inputs duplicados
      Un conjunto de fotos categorizadas
      Revisión / Finalizar / Exportar formatos
        SIC-18.xlsx + SIC-18A.xlsx cuando corresponde y está completo
    Cunetas / drenaje
    Badenes
    Muros
    Túneles
  DERECHO DE VÍA
    Derecho de vía
  PUENTES
    Puentes / SCAP
  PAVIMENTOS
    Acceso existente, conservado
```

## Formularios puntuales o lineales

La presentación nueva usa tarjetas Identificación, Ubicación inicio y Ubicación fin (lineales), Datos del elemento, Nivel cuando corresponde, Fotografías, Observaciones y REVISIÓN. El GNSS queda colapsado mediante LocationSummaryCard. Puntos críticos mantiene el rango contractual inicio/fin; no se elimina el fin porque su nombre sugiera un punto. Los anchos de una Faja comparten esos datos y nunca los recapturan.

Se conserva el tema existente. ChoiceSelector muestra chips para conjuntos pequeños y dropdown para conjuntos grandes; SIC-34 utiliza 1 · BAJO, 2 · MEDIO y 3 · ALTO, con criterio colapsable. Las columnas pasan a una por debajo de 640 dp y a dos cuando hay espacio; el formulario tiene ancho máximo de 1000 dp.

## Revisión y estados

```text
FAJA 01
  Características · ✓
  Anchos · Pendiente
  [Completar anchos]
  [Guardar borrador] [Revisar y finalizar] [Exportar]

ALCANTARILLA
  SIC-18 · Completo
  SIC-18A · No requerido                       (estructural 2, funcional 1)
  SIC-18A · Pendiente → Completar detalle      (alguna condición 3)
  SIC-18A · Completo                          (detalle finalizado)
  SIC-18A · No requerido · antecedente conservado
    Cambio de 3 a 1/2 con detalle existente:
    [Conservar como antecedente] [Cancelar cambio]
```

## Historial y archivos

Una Faja vinculada produce una fila principal con estados SIC-08 / SIC-09; varios registros históricos de anchos se conservan como mediciones dentro de la ficha. Un SIC-09 sin sourceRecordId sigue siendo visible como anchos históricos sin vínculo. Una Alcantarilla conserva el mismo UUID para SIC-18 y SIC-18A y se abre desde un único botón.

Archivos generados agrupa los Excel por el UUID principal y muestra contexto de ruta/PR. El botón Abrir/Compartir sigue usando GeneratedFileIndex y FileProvider. Se mantienen separados los archivos contractuales y sus columnas.

La validación automatizada verifica los destinos del dashboard y el despliegue del detalle de mala condición dentro de Sic18Fields. La revisión en tablets físicas, ambas orientaciones, cámara y GNSS reales sigue pendiente.
