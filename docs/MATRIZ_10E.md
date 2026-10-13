# Matriz 10E

| Comprobación | Estado / evidencia |
|---|---|
| Seis pruebas físicas 10D: registro/edición, giro, cámara/fotos, ACK final, recuperación y Pavimentos | Aprobadas por el propietario; no repetidas |
| Causa del override V2=false | Confirmada en Gradle y evidencia APK 10D; override retirado |
| Propiedad V2 del checkout | Clave ausente al inicio; añadida true únicamente en archivo privado ignorado |
| Capacidad real del servidor | GET aprobado: V2=2, V3, rutas V4, numeración estable; sin POST |
| Debug normal y BuildConfig V2=true | BUILD SUCCESSFUL en 5m12s, incluyendo pruebas; APK y clase compilada verificadas |
| Configuración ordinaria y diagnóstico | Aprobada: PhotoNamesIntegration10ETest |
| Reserva V2, reintento y ACK completo | Aprobada: PhotoNamesIntegration10ETest; datos/HTTP ficticios |
| Snapshot V1 pendiente, nombres y bytes conservados | Aprobada: PhotoNamesIntegration10ETest |
| Fotografía histórica V1 dentro de reserva V2 | Aprobada: PhotoNamesIntegration10ETest |
| Identidad persistente, preferencias y reapertura Room | Aprobada: PhotoNamesIntegration10ETest; no instalación física |
| Reserva estable, rechazo inválido, servidor antiguo y convivencia V1/V2 | 4/4 aprobadas: ReadablePhotoNames10Test |
| applicationId, versión y certificado frente a Debug 10D | Coinciden los cuatro valores; firma APK verificada |
| Firma de la aplicación instalada en tablet | Pendiente: ADB sin dispositivos; detenerse si Run informa incompatibilidad |
| `app/release/` | Inspección de archivos preexistentes; no borrado, incorporación a Git ni compilación Release |
| Videos Puentes/SCAP | Cliente existente conservado; GET videoProtocol=0; bandera remota false |
| Prueba V2 en Alcantarilla ficticia real | Pendiente del propietario; instrucciones mínimas en ENTREGA_10E |

Solo se ejecutan las dos clases focalizadas indicadas. Las consultas reales son GET; todos los POST de pruebas son interceptados y ficticios.

**Resultado local: 9/9 aprobadas, 0 fallidas, 0 omitidas.** XML originales y resumen `results.json` en `campo-10e/evidencia/`. Los otros tests se compilan como parte del source set, pero no se ejecutan. La revisión de privacidad no encontró valores privados en los archivos de entrega.
