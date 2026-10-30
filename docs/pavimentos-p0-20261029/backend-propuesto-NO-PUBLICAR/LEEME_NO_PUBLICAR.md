# PROPUESTA DE CONSOLIDACIÓN CRONOLÓGICA — NO PUBLICAR

Esta carpeta es un parche local de compatibilidad para Pavimentos. No modifica la V21 desplegada. No es una optimización de velocidad ni una autorización de publicación.

## Archivos y alcance

- `PavementSicChronologyV1.gs`: módulo nuevo sin servicios Google, reconoce exclusivamente SIC-14, SIC-14A, SIC-15 y SIC-16, valida fecha ISO real y hora de registro persistida, compara fecha/hora/UUID.
- `SyncV3Core.gs`: módulo completo derivado de la copia privada de la versión inmutable 21. Cambios limitados a aceptar la extensión `pavementSicChronologicalVersion=1`, PR/distancia inicial nullable, autenticar fecha/hora dentro del SHA-256 y consolidar cronológicamente esos cuatro SIC.
- `CAPACIDADES_FRAGMENTO_NO_REEMPLAZAR_CODIGO.txt`: añadir solo esa propiedad al doGet existente cuando se revisen y ensayen juntos los módulos. No incluye Código.gs, secretos, raíces ni endpoints.
- `SyncV3Core_V21_A_PROPUESTA.diff`: diferencias contra V21. Debe regenerarse con las fuentes finales y revisarse antes de cualquier publicación.

No se cambia SyncV3.gs, ScriptLock, staging, transacciones, numeración, reservas, fotos V2, SCAP, videos, Observaciones, firmas ni ACK. El hash de solicitudes antiguas sigue siendo idéntico. Las solicitudes nuevas incluyen fecha/hora en el hash. Una modificación de esos metadatos invalida el hash original.

## V21 actual y respaldo sin PR

El validador V21 exige PR/distancia numéricos en `sicRows`. Por tanto, no basta con hacer opcionales las casillas de Android. El cliente preparado entrega en V21 los SIC individuales dentro de los archivos del padre (`DATOS_APP/SIC-14.xlsx`, etc.), junto con PDF, Excel nativo, JSON y fotos. No manda filas incompatibles al consolidador. El ACK de esos archivos confirma un respaldo individual, NO un consolidado remoto cronológico. La interfaz informa la limitación. Los libros remotos existentes permanecen intactos y pueden contener filas anteriores.

## Bloqueo de históricos: no inventar fechas

Las filas históricas V21 no incluyen necesariamente fecha/hora de registro en los manifiestos, y el JSON antiguo tampoco incluía `createdAt`. Drive.createdTime, revisiones, fecha de carga y orden de carpeta NO son sustitutos válidos. Si un agregado contiene una fila sin estos metadatos y se solicita el contrato cronológico, el candidato rechaza `PAVEMENT_REGISTRATION_RECONCILIATION_REQUIRED` antes de comenzar la transacción. No reemplaza el Excel ni simula cronología.

Antes de activar, hay que inventariar esas filas y conciliar fecha/hora de registro acreditadas, preferentemente desde Room de la tablet propietaria. Esa conciliación necesita un plan específico, respaldo y autorización; este parche no migra ni elimina históricos. Una APK antigua que trate de reemplazar filas cronológicas sin los metadatos también puede necesitar conciliación. No se activa producción con este impedimento sin resolver.

## Ensayo requerido y reversión

1. Descargar y respaldar privadamente las fuentes vigentes, manifiestos y Excel del entorno de ensayo. Registrar versión/deployment actuales sin publicar sus identificadores.
2. Comparar SyncV3Core actual con la referencia V21: conservar cualquier modificación posterior antes de incorporar el diff. No subir un módulo viejo completo a ciegas.
3. En un entorno aislado ya autorizado, instalar el helper, integrar el diff y añadir únicamente la propiedad de capacidades. Usar credenciales y raíz de prueba separadas. No ejecutar fixtures en producción.
4. Probar los cuatro formatos sin PR, varios propietarios, fechas empatadas, edición de UUID, respuesta perdida, BUSY, hashes y ACK. Abrir los XLSX reales y comprobar filas. Incluir una fila histórica sin fecha acreditada: debe fallar antes de escribir el consolidado.
5. Revisar equivalencia SCAP/SIC restantes, fotos, videos y numeración. Solo después decidir si procede solicitar autorización para actualizar el despliegue existente, conservando URL.
6. Reversión: devolver el deployment a la versión inmutable anterior. No borrar archivos, manifiestos, journals o colas. Si llegaron solicitudes con la extensión nueva, sus preparaciones permanecen congeladas y Android detecta falta de capacidad; no deben reinterpretarse como solicitudes antiguas. Conciliar esos UUID/revisiones antes de reintentar. Conservar el respaldo y la evidencia para recuperación dirigida.

Estado: NO-GO para publicación. Faltan ensayo Google y conciliación histórica. Las pruebas Node son simulaciones locales y no acreditan operaciones ni tiempos reales de Google.
