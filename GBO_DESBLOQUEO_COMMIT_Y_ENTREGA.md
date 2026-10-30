# GBO — Decisión ejecutiva de entrega · 30/10/2026

Estado preservado antes de la continuación funcional solicitada; la matriz final posterior lo actualizará.

**Recomendación: NO-GO PILOTO con sincronización. La distribución general tampoco está autorizada.**

| Pregunta | Decisión |
|---|---|
| ¿APK 1.4.19 (33) lista para piloto con el backend anterior? | **NO-GO.** La APK está compilada y firmada, pero falta evidencia de una entrega grande íntegra con el despliegue vigente. Su código de referencia reprodujo el timeout de 59 archivos. Consultar capacidades por HTTP no acredita commit, ACK ni seguridad de los pendientes. |
| ¿Backend nuevo listo para transición controlada? | **Listo como base técnica para preparar la transición; NO-GO para ejecutar el corte o usarlo en campo.** La corrección permanece deliberadamente limitada a raíces privadas de pruebas. Falta preparar un despliegue operativo revisado, una configuración/APK de campo vinculada a él y el tratamiento de pendientes anteriores. |

**Evidencia cerrada.** Recuperación real con journal **COMPLETE, 67/67 operaciones, 59 archivos y tres SIC**; **62/62 hashes, propietarios y rutas verificados**; se conservaron los 14 IDs originales. ACK HTTP real repetible, sin duplicados, y BUSY concurrente comprobado. Recuperación: 10 llamadas de commit, 881,786 s activos acumulados; máximo observado 125,106 s por llamada. Selección Android de esta intervención: **35/35 PASS**, incluidos dos casos nuevos; backend local: **10/10 PASS**. Se conservan los 198 casos Android aprobados anteriormente; no se declara aprobada toda la suite histórica. No se ampliaron timeouts Android.

**APK exacta de campo a conservar, sin instalar todavía:**

`C:\GBO\tmp\entrega-20261030\release-online\GBO_1.4.19_33_CANDIDATA_NO_DISTRIBUIR.apk`

- Versión **1.4.19 (33)**, package `com.tuempresa.inventariovial`; firma histórica verificada. SHA-256 del archivo comprobado de nuevo, sin cambios:
  `0aa733c794f097eefce06be3a2c1fb0ba555d45cd53702ade51b552165dc4254`.
- Conserva el endpoint y las propiedades legítimas del backend anterior. Room sigue en 21, con regresiones de migración aprobadas; actualización física y pendientes de las tablets aún no verificados.
- La APK separada **ENSAYO** queda excluida de cualquier entrega de campo. Ninguna APK de campo fue dirigida a la raíz ficticia ni recibió el token de pruebas.

**Riesgos críticos restantes:** commit grande no acreditado en el backend anterior; pendientes y ACK vigentes de las tablets sin conciliar; ausencia del paquete operativo de transición del backend corregido. No cambiar endpoints, desinstalar, borrar datos ni reutilizar paquetes preparados contra otra raíz.

**Única acción mínima del propietario:** autorizar **la preparación del paquete de transición al backend corregido**, con destino operativo legítimo y conciliación explícita de pendientes. Esto **no autoriza el corte ni la instalación**. Una autorización de instalación por sí sola no elimina los bloqueos anteriores. Se prioriza esta vía sobre distribuir la APK contra el backend anterior sin acreditar entregas grandes.

**Consolidación:** `a9e1393` — backend reanudable aislado; `7e9739e` — regresiones Android de continuación. Sin push, cambios en main, instalaciones o modificaciones de producción. Se preservaron fuentes, journal, IDs y evidencias; por instrucción de cierre no se continúa con nuevos commits de ensayo ni compilaciones.

La operación HTTP que estaba activa terminó con 59/59 archivos en staging de la segunda raíz privada. No se inició su commit ni se acreditó un segundo ACK.
