# Firebase / Push Nativo Android — Teste rápido

## 1. Criar projeto no Firebase
1. Acesse Firebase Console.
2. Crie ou selecione o projeto do MaridoBH.
3. Adicione um app Android com o package:

```text
br.com.maridobh.profissional
```

4. Baixe o arquivo:

```text
google-services.json
```

5. Coloque o arquivo em:

```text
app/google-services.json
```

O Gradle aplica o plugin `com.google.gms.google-services` automaticamente quando esse arquivo existir.

## 2. Configurar o servidor WordPress
No admin do WordPress:

```text
MaridoBH Serviços → Mobile / APK → Firebase Cloud Messaging
```

Configure preferencialmente a **Service Account JSON**:

Firebase Console → Configurações do Projeto → Contas de serviço → Gerar nova chave privada.

Cole o JSON no campo do plugin e salve.

## 3. Testar token do APK
1. Compile o app no Android Studio.
2. Instale no celular.
3. Faça login no painel do profissional dentro do app.
4. Aguarde alguns segundos.
5. No WordPress, vá em:

```text
MaridoBH Serviços → Mobile / APK → Dispositivos recentes
```

Deve aparecer um dispositivo `ANDROID` com provider `fcm` e token preenchido.

## 4. Enviar push de teste
No mesmo menu, use:

```text
Teste de push Android
```

Informe o ID do usuário profissional e clique em **Enviar push de teste**.

## Observações
- O OneSignal continua atendendo o PWA/Web.
- O Firebase atende o APK Android.
- Se o app estiver aberto, a mensagem pode aparecer como notificação local gerada pelo app.
- Se o app estiver fechado, o FCM deve entregar a notificação nativa.
