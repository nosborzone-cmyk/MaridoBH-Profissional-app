# MaridoBH Profissional Android 2.0

Projeto Android Studio preparado para compilar na nuvem com **GitHub Actions**.

## Estrutura correta na raiz do repositório

Ao abrir o repositório no GitHub, você deve ver estes arquivos/pastas na raiz:

```text
app/
gradle/
.github/
gradlew
gradlew.bat
settings.gradle
build.gradle
gradle.properties
README.md
```

Se aparecer apenas um arquivo `.zip`, o GitHub não vai compilar. Extraia o ZIP e envie o conteúdo da pasta.

## Como gerar o APK pelo GitHub

1. Crie um repositório privado no GitHub.
2. Extraia este ZIP.
3. Envie o **conteúdo** da pasta extraída para o repositório.
4. Entre em **Actions**.
5. Abra o workflow **Android Debug APK**.
6. Clique em **Run workflow**.
7. Quando terminar, baixe o APK em **Artifacts**.

## Firebase

Este projeto já contém `app/google-services.json` para o pacote:

```text
com.maridobh.profissional
```

O plugin MaridoBH continua sendo o painel de controle para Firebase, push, dispositivos, localização e configuração remota.

## Observação sobre a pasta `.github`

Se a tela do GitHub Actions mostrar “Get started with GitHub Actions”, significa que a pasta oculta `.github/workflows/` não foi enviada.

Nesse caso:

1. Crie manualmente no GitHub o arquivo:

```text
.github/workflows/android-debug.yml
```

2. Copie o conteúdo do arquivo de backup que está na raiz:

```text
GITHUB_WORKFLOW_android-debug.yml
```

3. Salve. O workflow aparecerá na aba Actions.
