# Como ativar o GitHub Actions sem enviar a pasta oculta .github

Se o Windows não deixar enviar a pasta `.github`, use este método:

1. No GitHub, entre no repositório.
2. Clique em **Actions**.
3. Clique em **set up a workflow yourself** / **configure um fluxo de trabalho você mesmo**.
4. No campo do nome do arquivo, coloque exatamente:

```text
.github/workflows/android-debug.yml
```

5. Apague o conteúdo padrão que aparecer.
6. Copie todo o conteúdo deste arquivo do projeto:

```text
workflow-para-copiar/android-debug.yml
```

7. Cole no editor do GitHub.
8. Clique em **Commit changes**.
9. Volte em **Actions** e rode o workflow **Android Debug Build**.

O projeto também contém a pasta `.github` pronta. Se seu computador permitir enviar arquivos ocultos, basta subir essa pasta normalmente.
