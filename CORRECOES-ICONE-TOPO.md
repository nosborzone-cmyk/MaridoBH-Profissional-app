# Correções aplicadas

## Ícone do aplicativo
- Substituído o ícone antigo que usava a imagem inteira do splash.
- Criados ícones corretos em `mipmap-mdpi`, `mipmap-hdpi`, `mipmap-xhdpi`, `mipmap-xxhdpi` e `mipmap-xxxhdpi`.
- Criado suporte a Adaptive Icon em `mipmap-anydpi-v26`.
- Manifest atualizado para usar `@mipmap/ic_launcher` e `@mipmap/ic_launcher_round`.

## Topo / área segura
- `MainActivity.kt` ajustado para respeitar a área segura do Android.
- Conteúdo do WebView agora recebe padding superior conforme a status bar/notch.
- Status bar ajustada para fundo branco com ícones escuros.
- Botão de diagnóstico permanece dentro da área segura.
