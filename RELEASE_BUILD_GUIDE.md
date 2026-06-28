# Guia de build release — MaridoBH Profissional Android v1.0

## 1. Configurar modo produção

No arquivo `gradle.properties`:

```properties
MBH_DEBUG_MODE=false
MBH_SERVER_URL=https://maridobh.com.br
MBH_PROFESSIONAL_PATH=/painel-profissional/
```

## 2. Configurar Firebase

Adicione o arquivo:

```text
app/google-services.json
```

Depois siga `FIREBASE_SETUP.md`.

## 3. Gerar chave de assinatura

No Android Studio:

```text
Build > Generate Signed Bundle / APK
```

Escolha **Android App Bundle** para Play Store.

## 4. Sugestão de versionamento

Versão atual:

```text
versionName 1.0.0
versionCode 100
```

Próximas versões:

```text
1.0.1 → correções pequenas
1.1.0 → recursos novos leves
2.0.0 → grande atualização
```

## 5. Antes de publicar

Verifique:

- Política de privacidade publicada.
- Explicação clara sobre uso de localização.
- Permissões solicitadas somente quando necessárias.
- Push testado.
- Login testado.
- Upload testado.
- Localização Precisa testada.
- Jornada testada.
