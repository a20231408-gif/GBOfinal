# Calificaciones, ceros y pendientes SCAP

## Implementado para fichas nuevas

En las fichas LOSA y MODULAR creadas por esta candidata, los seis porcentajes G.1 de cada elemento de plantilla comienzan en **0**, persistidos en Room y en los valores de captura. Un elemento añadido posteriormente a una de estas fichas también recibe los ceros iniciales. La fuente `ZERO_PERCENT_DEFAULT_NOT_EVALUATED` distingue el valor inicial de una afirmación de inspección. No se asignan fechas, dimensiones, metrado, coordenadas ni descripciones mediante esta regla.

Cada porcentaje admite 0–100. El operador puede editar o dejar vacío un campo; guardar y reabrir conserva exactamente esa decisión. El XLSX exporta los porcentajes guardados como números. La inicialización solo ocurre al crear la ficha/elemento, no al abrir ni migrar fichas históricas. No sobrescribe valores manuales.

## Excepciones contractuales expresas

- **F.1 «Estado» no es una calificación numérica.** Es presencia/aplicabilidad: `UNSET`, `PRESENT`, `NOT_APPLICABLE`. Afecta qué elementos se evalúan. No admite un cero contractual equivalente a «sin evaluar». Se conserva la decisión pendiente y las opciones existentes; no se marca automáticamente «Presente» ni «No aplica». Por ello pueden seguir apareciendo advertencias F1-104, F1-110, etc. hasta decidir su aplicabilidad. No son ceros rechazados y, por sí solas, no impiden el cierre con advertencias aceptadas.
- **Nivel 0 G.1 = «Muy Bueno».** No se asigna automáticamente una evaluación global 0 ni 100% al nivel 0. Se inicializa el *porcentaje de cada nivel* a 0%; esto no afirma ausencia de defectos.
- Una distribución de seis ceros suma 0%, por lo que permanece **no evaluada**. Para un resultado contractual de condición válido la distribución debe sumar 100% dentro de la tolerancia existente. La fórmula, factores y clasificación no se modificaron.
- Los nulos históricos permanecen nulos. Vacío, 0%, «No aplica» y condición «Muy Bueno» no son intercambiables.

Guardar borradores parciales no exige una distribución válida. Los valores fuera de escala, fechas o dimensiones inválidas siguen protegidos para el cierre/envío existente; se puede conservar y exportar localmente el borrador sin inventar un resultado contractual.
