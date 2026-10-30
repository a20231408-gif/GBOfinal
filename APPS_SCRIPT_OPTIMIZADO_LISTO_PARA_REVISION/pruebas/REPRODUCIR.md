# Ejecución local registrada

Desde la raíz `D:\Downloads\GBO_PRUEBAS_8B`, con Node disponible:

```powershell
node --test --test-concurrency=1 --test-reporter=tap APPS_SCRIPT_OPTIMIZADO_LISTO_PARA_REVISION/pruebas/compatibilidad.test.cjs google-apps-script/Hotfix146.test.cjs google-apps-script/VideoNamesV2.test.cjs google-apps-script/VideoV1Adapter.test.cjs google-apps-script/SicObservationsPlain10K.test.cjs google-apps-script/SicObservations10I.test.cjs
```

El harness requiere Python para ZIP. En esta computadora usa el runtime local ya configurado en DriveV4Harness; en otro equipo se puede indicar su ejecutable con `GBO_AUDIT_PYTHON`. No requiere credenciales ni conexión de producción.

Resultado registrado: 43 casos aprobados, 0 fallidos u omitidos. Siete comprueban los archivos empaquetados con dependencias locales; el resto son regresiones del hotfix, Observaciones 10I/10K y videos 10H. El nombre de un caso que menciona «Published capability» verifica el doGet **local dentro de un VM**, no el servidor publicado.

La primera ejecución del caso nuevo de Observaciones intentó editar una fixture revision=1000 con revision=2; se corrigió la inicialización de esa fixture a 1/base=0. No se cambió el backend para hacer pasar la prueba. El registro inicial se conserva y el informe final identifica esa diferencia.

No se repitió la carga anterior de 28 registros porque los .gs de la propuesta son idénticos a los ya probados. Sus resultados previos están en docs/hotfix-1.4.6/evidencia; no se presentan como nueva prueba ni como tiempos Google. El ensayo nuevo de cinco filas mide llamadas unzip/zip, no rendimiento productivo ni concurrencia de ejecuciones Google.
