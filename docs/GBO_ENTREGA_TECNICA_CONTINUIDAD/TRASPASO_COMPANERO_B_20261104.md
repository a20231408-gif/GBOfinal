# Traspaso autónomo para compañero B — 04/11/2026

## Estado y fuente

Android 1.4.33 (47): LISTO PARA INTEGRAR fuente, revisión en REVISION_FINAL_PR12_20261104.md. Fuente Android c78921c0a491e8719dc0860ebe663ad5f892d485; revisión documental 9089d6cedc2a242ab66899ad745586f910d9c139. Rama handoff/scap-cierre-final-20261104, PR #12 https://github.com/a20231408-gif/GBOfinal/pull/12, destino codex/scap-pavimentos-cierre-20261103 (PR #11). No main ni merges automáticos. El checkpoint posterior añade herramientas locales y este traspaso; obtener su SHA con git ls-remote origin refs/heads/handoff/scap-cierre-final-20261104 y comprobar HEAD antes de continuar.

Android: 73/73 integradas y compilación Kotlin/Java acreditadas, no repetidas. No APK nueva: ASUS debe usar configuración original y firma histórica conforme RECUPERACION_ANDROID.md. La revisión de Android sigue vigente: los cambios posteriores no modifican app/. OPPO offline con dos QUEUED, Xiaomi excluida.

## Avance backend local

Defecto confirmado en readonly-reconcile.cjs: 16 buckets distintos podían incluir una etiqueta fuera de 0–f; no se comprobaba identidad común de raíz/ejecutor. Corregido, rechazando conjuntos incompletos o mezclados antes de conciliarlos. PreflightReadonlyPaged.test.cjs: 10/10 PASS (ocho existentes y dos nuevos). No se modificaron lectores .gs, router, módulos V22 ni backend remoto.

Nueva herramienta properties-backup-verify.cjs, exclusivamente Node local. Recibe dos archivos JSON privados, valida conteo y digest de cada captura, exige mismo script y segunda fecha posterior, y compara contenido completo. No imprime claves, valores ni errores de parseo que pudieran contener secretos. Cinco pruebas PASS: más de 50 propiedades, truncamiento, alteración, proyecto/fecha y Unicode/vacíos. Estas pruebas no son una repetición ni sustitución de las 76 backend previas.

Uso local: node backend/candidato-original-reanudable/properties-backup-verify.cjs CAPTURA_A_PRIVADA.json CAPTURA_B_PRIVADA.json

Sobre esperado: schema=1, scriptId, capturedAt (ISO UTC), count, sha256 y properties (objeto de cadenas). Canonización exacta del hash: JSON.stringify(Object.keys(properties).sort().map(k => [k, properties[k]])), SHA-256 sobre bytes UTF-8, hexadecimal minúsculo. No normalizar valores, espacios ni saltos de línea.

Un hash calculado sobre 50 propiedades copiadas de la UI NO demuestra respaldo completo. Conteo y hash deben proceder del objeto completo devuelto por getProperties en el proyecto original durante una lectura autorizada. El verificador declara remoteProvenanceVerified=false y clientPauseVerified=false: comprueba integridad interna y coincidencia, no procedencia ni ausencia de clientes. Tampoco convierte este sobre en una instrucción de restauración.

## Próxima lectura autorizable — preparada, NO ejecutada

1. El responsable debe acreditar custodia/pausa de todos los clientes de la URL original durante toda la ventana. Registrar dispositivos, responsable, hora UTC y comprobación de pausa, en privado. Cero ejecuciones visibles no demuestra pausa. No alterar las colas del OPPO para lograrla.
2. Obtener autorización específica para leer el proyecto original, su editor, versiones/deployment, ejecuciones y propiedades. Registrar scriptId/deploymentId contra el anexo privado. Nuevos doPost invalidan la presunción de vigencia del inventario anterior; no prueban por sí solos escritura. Si continúan, detener la aceptación de estabilidad.
3. El método de extracción completa de ScriptProperties sigue pendiente de autorización: una función editor-only propuesta leería una única vez PropertiesService.getScriptProperties().getProperties(), y derivaría de ese mismo objeto count y sha256 según la canonización anterior. Preparar retorno del sobre completo mediante canal privado aprobado. No volcar valores en logs compartidos, no escribir en Drive ni crear un deployment para extraerlos. Si la herramienta de retorno trunca, no acreditar el respaldo. La selección y autorización del transporte privado de retorno es un bloqueo real; no se ha instalado ni ejecutado un lector nuevo.
4. Guardar dos capturas independientes, completas, con fecha/procedencia, fuera de Git, bajo acceso restringido. Ejecutar el verificador local. Un PASS acredita igualdad entre esas lecturas, no pausa ni ausencia de cambios intermedios. Conservar también hash del archivo original y copia privada del editor antes de proponer cambios.
5. Tras autorización de lectores existentes, ejecutar gboPilotInventoryReadonly00 hasta 15; conservar BEGIN/ROW/REFS/END completos. Usar readonly-reconcile.cjs con 16 particiones del mismo ciclo, misma raíz/ejecutor y firma. Repetir la lectura de estabilidad conforme al expediente, sin mezclar ciclos ni copiar END antiguos. Conciliar novedades contra la captura histórica 207 journals/207 records/220 bindings/13 huérfanos; nunca imponer esos conteos al estado nuevo. Mantener los 13 históricos sin adopción automática.
6. Revisar diferencias antes de cualquier propuesta de corte. Propiedades completas, pausa e inventario estable son puertas independientes. No desplegar V22, no OPEN, no escrituras ni nuevos lectores remotos bajo esta revisión local. B/DRAIN_ONLY y C/OPEN necesitan autorizaciones específicas separadas.

## Recursos y evidencia para otra computadora

GitHub contiene código, pruebas, este traspaso y documentación saneada. Puede usarse Claude Code o Codex: comprobar rama/SHA, leer este documento y el expediente vigente; ejecutar únicamente pruebas afectadas por cambios futuros. Node es suficiente para herramientas backend locales; Android necesita SDK/JBR fijados.

ASUS/custodio: server.local.properties y drive.local.properties originales; firma Debug histórica para candidata compatible OPPO, con init-script del kit; APK46 y su manifiesto; anexo institucional con script/deployment/raíces; paquete V22 privado revisado y manifiesto d26e19de83ad867561d9215ab0c4bada4f4e60db9b7055504a08797ce845e161; capturas anteriores completas disponibles y sus limitaciones. No solicitar tokens por chat ni copiar secretos a Git.

Evidencia en esta computadora: C:\Users\CORE I7\GBO_SCAP_CONTINUACION_20261104, con XML Android preservados y backend-continuidad/properties-tests.log. Los resultados de conciliación 10/10 se registran en backend-continuidad/inventory-tests-result.txt; no se ejecutó contra inventarios privados reales ni contra Google.

Bloqueo siguiente: autorización institucional y transporte privado verificado para capturas completas, más pausa real de clientes. No existe una corrección de código que pueda acreditar esas condiciones externas. V21 intacto; V22 preparado sin despliegue. No se inició trabajo adicional de riesgo para consumir presupuesto.