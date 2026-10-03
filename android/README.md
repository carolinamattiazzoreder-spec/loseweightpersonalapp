# WeightTracker — app Android

Versão Android nativa (Kotlin + Jetpack Compose) do app Streamlit `streamlit_app.py`.

## O que tem

| Streamlit | Android |
|---|---|
| 📊 Dashboard (peso atual, variação, IMC, meta, previsão de chegada, progresso, gráfico, tabela) | Aba **Dashboard** — mesmos cartões; gráfico com projeção de −1 kg/semana e linha da meta (toque num ponto para ver o valor) |
| ➕ Log Weight (registrar / apagar) | Aba **Log Weight** — data, peso (aceita `80,5` ou `80.5`), nota; lista com botão de apagar |
| ⚙️ Settings (perfil, exportar, importar, apagar tudo) | Aba **Settings** — altura e meta, exportar JSON/CSV, importar JSON, apagar tudo |
| Dropbox (`WeightTrackerData.xlsx`) + backup a cada 16 h | Arquivo `weight_data.json` no próprio celular + **Backup do Android** (Google) automático |

O formato JSON é o mesmo do botão *Download backup (JSON)* do Streamlit, então dá para levar os dados de um para o outro.

## Instalar no celular

1. No GitHub, abra **Actions → Android APK → execução mais recente** e baixe o artefato **WeightTracker-apk** (um `.zip` com `app-debug.apk`).
2. Passe o `app-debug.apk` para o celular e abra. O Android vai pedir para permitir "instalar apps desconhecidos" — permita para o app que você usou para abrir o arquivo.
3. Para trazer seus dados: no app Streamlit vá em *Settings → Download backup (JSON)*, depois no Android em *Settings → Import JSON backup*.

## Compilar localmente

Abra a pasta `android/` no Android Studio, ou rode:

```bash
cd android
./gradlew testDebugUnitTest assembleDebug
# APK em app/build/outputs/apk/debug/app-debug.apk
```

Requer JDK 17 e o Android SDK (API 35).
