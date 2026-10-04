# WeightTracker — app Android

Versão Android nativa (Kotlin + Jetpack Compose) do app Streamlit `streamlit_app.py`.

## O que tem

O visual segue o canvas **Design** (fundo menta, azul profundo, fonte Figtree, textos em português).

| Aba | Conteúdo |
|---|---|
| **Painel** | Saudação com a data; cartão do peso (variação vs. anterior e na semana, progresso até a meta); "Quando chego na meta" (data prevista a −1 kg/semana e linha do tempo); Marcos; gráfico "Evolução" (pesagens, tendência, projeção e meta — toque para ver uma pesagem); calendário do mês com resumo; últimos registros com IMC |
| **Registrar** | Data, peso (aceita `80,5` ou `80.5`), nota; histórico com exclusão; "DESFAZER" após salvar ou excluir |
| **Ajustes** | Altura e peso meta; backup (exportar JSON/CSV, importar JSON); armazenamento; apagar todos os registros |

Os dados ficam no arquivo `weight_data.json` do próprio celular, com **Backup do Android** (Google) automático.
O nome da saudação ("Olá, Carol") está em `app/src/main/res/values/strings.xml` (`user_name`).

A fonte Figtree (SIL Open Font License) é baixada pelo Gradle durante o build; sem internet, o app usa a fonte do sistema.

O formato JSON é o mesmo do botão *Download backup (JSON)* do Streamlit, então dá para levar os dados de um para o outro.

## Instalar no celular

1. No celular, abra a página **Releases** do repositório → **WeightTracker APK (latest)** e toque em **WeightTracker.apk**:
   https://github.com/carolinamattiazzoreder-spec/loseweightpersonalapp/releases/tag/apk-latest
2. Abra o arquivo baixado. O Android vai pedir para permitir "instalar apps desconhecidos" — permita para o app que você usou para abrir o arquivo.
3. Para trazer seus dados: no app Streamlit vá em *Settings → Download backup (JSON)*, depois no Android em *Settings → Import JSON backup*.

## Compilar localmente

Abra a pasta `android/` no Android Studio, ou rode:

```bash
cd android
./gradlew testDebugUnitTest assembleDebug
# APK em app/build/outputs/apk/debug/app-debug.apk
```

Requer JDK 17 e o Android SDK (API 35).
