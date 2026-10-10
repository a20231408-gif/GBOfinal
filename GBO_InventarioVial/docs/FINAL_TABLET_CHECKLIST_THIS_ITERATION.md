# Validación física pendiente — esta iteración

Todos los pasos están **pendientes de ejecución por el usuario en tablet**. Registrar modelo, Android, certificado de instalación actual, hora, versión 14, resultado PASS/FAIL, diagnóstico si falla y evidencia. APK debug validation no incorpora configuración Drive; para subida real se requiere build/entorno configurado conforme a política existente. No desinstalar para simular actualización; no sustituir firma corporativa por debug.

1. [ ] Iniciar sesión normal en la tablet administradora, sin borrar datos.
2. [ ] Comprobar botón TABLET_ADMIN solo con permiso explícito; ADMIN sin permiso no basta.
3. [ ] Abrir panel, reingresar PIN; comprobar autorización 15 min y usuario actual sin pedir username.
4. [ ] En tablet destino copiar huella completa de 64 hex y registrarla con alias en administradora.
5. [ ] Copiar token mostrado una sola vez; cerrar aviso y comprobar que no existe Ver token posteriormente.
6. [ ] Configurar URL existente y token en tablet nueva; no tocar identidad ni borrar registros.
7. [ ] Sincronizar usuarios; comprobar login normal offline después de sync válido.
8. [ ] Listar tablets: alias, activa/revocada, huella abreviada; ninguna columna token/hash.
9. [ ] Revocar destino con confirmación; no revocar la administradora desde su sesión.
10. [ ] Sincronizar destino: debe rechazar DEVICE_REVOKED y conservar Room/fotos.
11. [ ] Reactivar destino con confirmación.
12. [ ] Con el mismo token anterior comprobar acceso restaurado y datos conservados.
13. [ ] Rotar token del destino con confirmación; copiar nuevo token una vez.
14. [ ] Comprobar rechazo del token anterior y de sus sesiones administrativas.
15. [ ] En login destino usar Actualizar token autorizado, nuevo token y verificación online; comprobar acceso con misma huella.
16. [ ] Abrir historial y comprobar fechas/actor/alias/acción/huella abreviada, sin secretos.
17. [ ] Owner retira permiso mediante applyTabletAdminPermission en editor; no desde APK.
18. [ ] Próxima operación privilegiada debe fallar y cerrar panel; sesión normal debe seguir. Probar también TTL sin matar app.
19. [ ] Usar app continuamente 30–60 minutos y anotar horas/cualquier salida real.
20. [ ] Tomar/importar/reemplazar/preview varias fotos grandes; alternar módulos sin perder borradores.
21. [ ] Abrir selector de 207 señales y recorrerlo; comprobar imágenes y scroll fluido.
22. [ ] Abrir mapas offline y vista previa de fotos; mover/zoom sin Internet.
23. [ ] Crear/editar borrador PCI y URCI; verificar cálculos previos conservados.
24. [ ] Crear SCAP y alternar A–G; guardar, cerrar/abrir y retomar.
25. [ ] Enviar app a background por poco tiempo y luego por más de 15 minutos.
26. [ ] Volver: comprobar PIN por timeout largo y ficha retenida; no cierre de Activity.
27. [ ] Bloquear pantalla con ficha en curso.
28. [ ] Desbloquear y comprobar draft/fotos/UUID.
29. [ ] Abrir otra aplicación y cámara/selector externo.
30. [ ] Volver y revisar valores originales sin inventar ceros.
31. [ ] Abrir diagnóstico; leer última salida/reasonCode real o Causa no disponible.
32. [ ] Exportar gbo_diagnostics_YYYYMMDD.json; revisar que no incluya PIN/tokens/nombres/coords.
33. [ ] Crear nueva ficha tipo LOSA, confirmar selección y fuente correcta.
34. [ ] Abrir C.5: Apoyo 1 y 2 ya deben ser visibles sin añadir.
35. [ ] Elegir Izquierda/Derecha; cerrar/reabrir y verificar LEFT/RIGHT conservado.
36. [ ] Añadir Apoyo 3, completar algo, guardar/reabrir; exportar continuación.
37. [ ] F.1 debe contener 17 elementos reales de fuente, inicialmente Sin completar.
38. [ ] Editar nombre/código/descripcion global; añadir manual; comprobar exportación.
39. [ ] G.1 debe mostrar los 17 incluso UNSET, más manuales aplicables.
40. [ ] Marcar un elemento No aplica: desaparece G.1 y sus pendientes.
41. [ ] Reactivar: vuelve con metrado/calificaciones/descripcion anteriores.
42. [ ] G.1 ingresar 90 en nivel 1 y 10 en nivel 2; dejar resto vacío; revisar cálculo.
43. [ ] Exportar SCAP confirmando faltantes cuando corresponda.
44. [ ] Abrir Excel: null vacíos, 90/10 reales, nombres físicos/logos/formulas/estilos correctos.
45. [ ] F.2 selector: revisar todos los nombres extraídos, incluidos ejemplos no abreviados.
46. [ ] Editar nombre de foto; comprobar campo y caption exportado.
47. [ ] Añadir registro, descripcion/fecha, tomar/importar; preview, reemplazar/remover/reordenar.
48. [ ] Exportar fotos vertical/horizontal; comprobar completas sin stretch/crop y fechas.
49. [ ] Dejar al menos diez datos faltantes y guardar borrador.
50. [ ] Abrir Ver pendientes: contar warnings separados de valores inválidos.
51. [ ] Tocar pendientes A, Apoyo2.Material, F.1 descripción, F.2 foto, G.1 metrado.
52. [ ] Cada clic cambia sección y lleva al UUID/campo exacto, foco de texto y highlight breve.
53. [ ] Exportar de todos modos; revisar entradas faltantes vacías y completion INCOMPLETE.
54. [ ] Subir de todos modos en build configurada, revisar destino mostrado; ACK/sync separado, editar luego manteniendo UUID/fotos/Drive y versión anterior.

Repetir pasos 33–54 en **MODULAR**: F.1 y G.1 iniciales 10, verificar mapeo Muro F.1 510/G.1 504 y restore de No aplica. Repetir recuperación tras process death con borrador ya marcado Guardado local. En normal/administración comprobar timeout sin pérdida de datos. No marcar PASS por inspección de código.

Adicionales negativos: PIN incorrecto, usuario inactivo, ENGINEER sin permiso, ADMIN sin permiso, huella nohex/63/65 caracteres, duplicada, self-revoke, token de usuarios en endpoints tablets y viceversa, retirado permiso durante sesión, token rotado erróneo en destino conserva credencial cifrada previa. No publicar capturas con token visible.
