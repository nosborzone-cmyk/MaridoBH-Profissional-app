# Módulo 5 — Smart Diagnostics & Offline Sync

Esta versão adiciona a base de diagnóstico inteligente e sincronização offline do app MaridoBH Profissional.

## Incluído

- Central de diagnóstico do app.
- Indicadores de Internet, Push, GPS, Localização Precisa, Jornada, Sincronização, Bateria e Bridge Mobile.
- Fila offline local para localização, dispositivo e jornada.
- Sincronização automática ao abrir/retomar o app.
- Botão de sincronização manual no diagnóstico.
- Ponte JavaScript com novos métodos:
  - `MaridoBHAndroid.getDiagnostics()`
  - `MaridoBHAndroid.getPendingSyncCount()`
  - `MaridoBHAndroid.syncPending()`
  - `MaridoBHAndroid.openBatterySettings()`
- Injeção de estado mobile no WebView:
  - `window.MBH_APP.diagnostics`
  - `window.MBH_APP.sync`

## Como testar

1. Abra o projeto no Android Studio.
2. Sincronize o Gradle.
3. Execute o app em um dispositivo Android.
4. Toque no selo superior do app para abrir o Diagnóstico.
5. Desative a internet e acione Localização/Jornada para gerar itens pendentes.
6. Reative a internet e toque em `Sincronizar agora`.

## Observação

A fila offline foi pensada para eventos leves. Fotos e anexos grandes devem usar uma etapa posterior com upload em partes/WorkManager.
