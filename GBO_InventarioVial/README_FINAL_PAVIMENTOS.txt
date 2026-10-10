GBO INVENTARIO VIAL + FALLAS EN PAVIMENTOS
==========================================

Base: app completa de Inventario Vial SIN dataset QA.
Versión Android: 1.2-pavimentos-final.
Base Room: v12, migración aditiva 11->12.

NUEVO
- Dashboard -> Fallas en pavimentos.
- UI corporativa basada en GBO_Fallas_Pavimentos.html.
- Asfalto/PCI, rígido/PCI y no pavimentado/URCI.
- Esquema X/Y, severidad, fotos, PDF, Excel y persistencia Room.
- Ruta seleccionada desde el catálogo oficial del proyecto y tramo asociado automáticamente.
- Subida Drive propia de Evaluación Superficial.
- Code.gs v6.5: pavimentos + corrección de bloqueo permanente por fingerprint de ruta.
- SCAP D.4 omitido del flujo.

SIC
- Preparar entrega -> Sincronizar: incluye automáticamente los Excel SIC de esa ruta.
- Exportar SIC a Excel: flujo manual adicional; subir requiere pulsar el botón de Drive.

VERIFICADO EN ESTE ENTORNO
- Apps Script: 32/32 pruebas PASS, incluidas dos pruebas nuevas de pavimentos.
- Revisión estática de integración y ausencia de dataset QA.

NO VERIFICADO EN ESTE ENTORNO
- assembleDebug/testDebugUnitTest Android: el contenedor no dispone de Android SDK.
  Ejecutarlos en Android Studio/Windows antes de instalar en campo.

Ver docs/PAVIMENTOS_TDR_DRIVE.md y docs/FINAL_PAVIMENTOS_DEPLOY.md.
