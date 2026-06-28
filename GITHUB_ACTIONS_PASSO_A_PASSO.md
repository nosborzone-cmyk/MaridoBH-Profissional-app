# Passo a passo para compilar no GitHub Actions

## 1. Criar repositório

Crie um repositório privado, por exemplo:

```text
MaridoBH-Profissional-Android
```

## 2. Enviar os arquivos

Não envie o ZIP fechado.

Extraia o ZIP e envie o conteúdo da pasta. A raiz do repositório precisa conter `app/`, `.github/`, `gradlew`, `settings.gradle` e `build.gradle`.

## 3. Verificar o workflow

Vá em:

```text
Actions
```

Você deve ver:

```text
Android Debug APK
```

Se não aparecer, a pasta `.github` não subiu. Use o arquivo `GITHUB_WORKFLOW_android-debug.yml` como backup.

## 4. Rodar o build

Clique em:

```text
Run workflow
```

## 5. Baixar o APK

Quando o processo terminar, abra o build e baixe:

```text
Artifacts > MaridoBH-Profissional-debug-apk
```

Esse APK é de teste/debug.
