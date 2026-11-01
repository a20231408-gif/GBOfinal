# Coordinación de tres frentes

Base funcional exacta: `b3ed30eb19340d26abc909387964a1469b46e7f1` (1.4.22/36).
Partir del HEAD publicado que la contiene, según ONBOARDING_COLABORADORES.md.
No hay GO productivo. Pruebas históricas 82 Android/48 backend fueron selecciones locales;
P01–P17 físicas siguen pendientes. No reinstalar, distribuir ni desplegar por aprobar mocks.

| Frente / rama sugerida | Archivos bajo responsabilidad | Aceptación | Dependencias y límites |
|---|---|---|---|
| A: documentos y UX / `codex/colaborador-locales` | `localfiles/LocalCopyPresentation.kt`, `LocalFilesUi.kt`, `UnifiedRecordsScreen.kt`, pruebas LocalCopy/LocalFiles | Caso reproducible con fixture, actuales primero/historial accesible, abrir documentos, cero borrados históricos y cero envíos por acción manual; mantener UUID/revisión/ACK | Coordinar cualquier cambio de LocalCopies.kt con B. No pedir tablet, fotos reales ni firma histórica. |
| B: contratos Android / `codex/colaborador-android-contratos` | `core/AutomaticDelivery.kt`, `AutomaticSync.kt`, `SicContributionPolicy.kt`, pruebas SyncRecovery/BridgeIdentity/SIC/longitud/Pavimentos | Transporte simulado, hash y ACK estrictos, identidad estable, opcionales según flags, longitud 10 m y no regresión de autoguardado/daño 26 | Congelar contrato JSON con C antes de cambiar campos. No cambiar Room, PCI, versión/dependencias o app/build.gradle.kts sin coordinación. Fixtures autocontenidos sin endpoints reales. |
| C: backend y raíces / `codex/colaborador-backend-raices` | `backend/candidato-original-reanudable/` y sus tests; mocks sintéticos nuevos propios | Históricos conservan raíz/ACK, nuevos van al destino aprobado, replay/interrupción sin duplicados; documentar legacy/vídeo no enrutados y rollback compatible | Solo fuente y mocks; no Apps Script/Drive. Reconciliación real y permisos quedan en manos del operador autorizado. No alterar MockActual compartido sin acuerdo con B. |

Los criterios son trabajo propuesto, no funcionalidades ya aceptadas. Comprobar primero lo
implementado: no rehacer conflicto permanente, regeneración, limpieza o nombres V2 ya existentes.
Registrar responsable, PR y casos PASS/FAIL/BLOCKED en la descripción de cada entrega.

Un responsable por archivo: avisar antes de tocar archivos compartidos. Mantener PR pequeños,
actualizar la rama desde integración solo con árbol limpio y resolver conflictos conservando
ambos trabajos. El mantenedor integra PR en orden A/B/C según dependencias reales y repite
solo la selección afectada. La actualización común de versión/APK y la firma quedan centralizadas.

Ningún frente necesita datos reales, keystore Release, tablet ni acceso Google. Si un caso
solo se reproduce con un activo privado, entregar reproducción sintética o declarar BLOCKED;
no convertir el privado en fixture. Inventario productivo, OAuth, permisos, corte, instalación
y aceptación física son una fase separada con autorizaciones específicas.
