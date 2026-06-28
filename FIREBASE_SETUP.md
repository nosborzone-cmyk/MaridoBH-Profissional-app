# Configuração Firebase — MaridoBH Profissional

1. Acesse o Firebase Console.
2. Crie um projeto ou use um projeto existente.
3. Adicione um app Android.
4. Use o package name:

```text
br.com.maridobh.profissional
```

5. Baixe o arquivo `google-services.json`.
6. Coloque o arquivo em:

```text
app/google-services.json
```

7. Na próxima etapa do projeto, ative o plugin `com.google.gms.google-services` no Gradle.

## Observação

O Módulo 3 já possui o serviço de mensagens e o gerenciador de token. Sem o `google-services.json`, o app funciona normalmente como WebView, mas o token FCM real não será gerado.


## Dependências Firebase já aplicadas

Este pacote já inclui no projeto Android:

- `com.google.gms.google-services` versão `4.5.0` no Gradle raiz.
- Firebase BoM `34.15.0`.
- `firebase-analytics`.
- `firebase-messaging`.

O plugin Google Services é aplicado automaticamente somente quando existir:

```text
app/google-services.json
```

Assim o projeto ainda consegue sincronizar no Android Studio antes de você baixar esse arquivo no Firebase.
Depois que baixar o `google-services.json`, coloque exatamente em:

```text
MaridoBH-Profissional-Android-v1.0/app/google-services.json
```

Depois sincronize o Gradle novamente.
