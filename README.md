# ⚖️ WeightTracker

App pessoal para acompanhar o peso até a meta. Existe em duas versões que usam o mesmo formato de backup (JSON), então dá para levar os dados de uma para a outra.

| | Android (principal) | Web (Streamlit) |
|---|---|---|
| Onde está | [`android/`](android/README.md) | [`streamlit_app.py`](streamlit_app.py) |
| Dados | No celular (`weight_data.json`) + Backup do Android | Dropbox (`WeightTrackerData.xlsx`) |
| Tecnologia | Kotlin + Jetpack Compose | Python + Streamlit |

## 📱 App Android

**Baixar a versão mais recente:** página de [Releases](https://github.com/carolinamattiazzoreder-spec/loseweightpersonalapp/releases/latest) → `WeightTracker.apk`. Abra o arquivo no celular para instalar ou atualizar por cima.

- **Painel** — peso atual com variação, progresso até a meta, data prevista de chegada (−1 kg/semana), Marcos a cada 5 kg, gráfico "Evolução" (7d / 30d / 90d / Tudo, com tendência, projeção e resumo), calendário do mês e últimos registros com IMC.
- **Registrar** — data, peso com até 2 casas decimais (aceita `76,85` ou `76.85`), nota, histórico com exclusão e "Desfazer".
- **Ajustes** — nome, altura e meta; exportar backup (JSON/CSV) e importar; verificar atualização.

Todas as versões a partir da 1.4 são assinadas com a mesma chave, então atualizações instalam por cima sem perder os registros. Detalhes de build e instalação em [`android/README.md`](android/README.md).

### Como sai uma versão nova

1. Mude `versionName`/`versionCode` em `android/app/build.gradle.kts`.
2. Ao chegar na `main`, o GitHub Actions roda os testes, gera o APK e publica o release `vX.Y`.
3. Pushes em outras branches atualizam o pré-release `apk-latest` (build de teste).

## 🌐 App web (Streamlit)

```bash
pip install -r requirements.txt
streamlit run streamlit_app.py
```

Precisa das credenciais do Dropbox em `.streamlit/secrets.toml` (`app_key`, `app_secret`, `refresh_token`; o `get_token.py` ajuda a gerar o token).
