> ANTECEDENTE DEL PAQUETE V1. Para la corrida BASE existente manda LEEME_REPARACION_BASE_V20.md y el informe de diagnostico v2. No ejecutar crear corrida/reiniciar/limpiar al seguir este documento anterior.

# Entrega de ensayo, 17/10/2026

**Listo para revisar y montar en GBO_Inventario_PRUEBAS_V20; no aprobado para producción.** Empezar por `GOOGLE_TEST_SETUP.md`.

- Dos carpetas ejecutables con los ocho módulos completos: BASE y candidato previamente revisado, con la misma protección/medición. Solo cambian SyncV3, SyncV3Core y el marcador de variante.
- Raíz autorizada fijada en código. Barreras de proyecto, propiedades, corrida, epoch, IDs y ascendencia, antes de mutaciones. Sin raíz/URL/credencial productiva en configuración ejecutable.
- Ejecutor y fixtures SIC/SCAP/Pavimentos, fotos V2, PDF/XLSX, Observaciones, video, dos clientes, BUSY y pérdidas de ACK provocadas. Carga corta y carga 30/familia, más cambio del candidato sobre historial BASE.
- 95 pruebas previas preservadas como antecedente; **23 casos locales nuevos en su resultado final**. Sus reejecuciones no aumentan el número de casos. Ver TAP/JSON; el fallo inicial del ejecutor también queda documentado.
- Medición local del recorrido integrado: 75→60 unzip; UUID, revisiones, archivos y partes Excel iguales. **Google real: cero ejecuciones y tiempos no medidos.**
- Documentados instalación manual de la copia, credencial de prueba, comparación, criterios, respaldo, eventual promoción y reversión; caché de históricos permanece como propuesta no implementada.

Pendientes concretos:

1. Ejecutar ambas variantes y UPGRADE en Google; revisar métricas y logs, sin conectar tablets ni cambiar su URL.
2. Confirmar manifiesto original V20, scopes, servicios y librerías: no se recibió `appsscript.json`. Se entrega uno mínimo V8/Drive v3, no se certifica equivalencia histórica exacta.
3. Generar/reproducir un video ficticio real de más de 256 KiB con el HTML local o aportar uno conocido; usar los mismos bytes en ambos ensayos.
4. Validar plantillas contractuales adicionales y cancelación abrupta/recuperación en Google. Las simulaciones no acreditan latencia real ni funcionamiento físico de una APK.
5. Revisión y aprobación posterior del propietario antes de tocar los dos módulos productivos. Nunca copiar a producción Codigo ni los helpers/configuración de ensayo.

No se publicó nada en Google, no se escribió en Drive, no se modificaron Apps Script productivo o Servicio de Usuarios, Android/configuraciones privadas o la APK 1.4.9. No hubo commit ni push. `evidencia/CONSERVACION.json` acredita conservación local por hashes.
