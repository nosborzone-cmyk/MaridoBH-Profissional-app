# MaridoBH Profissional Android 2.3

## Auditoria WebView e integrações externas

Correções implementadas:

- Botão **Abrir rota** agora abre fora do WebView:
  - Google Maps
  - Waze
  - `geo:`
  - `google.navigation:`
  - links `google.com/maps`
- Links externos agora são tratados corretamente:
  - WhatsApp
  - telefone (`tel:`)
  - e-mail (`mailto:`)
  - links externos HTTP/HTTPS fora do domínio MaridoBH
- Upload de arquivos revisado:
  - seleção da galeria
  - múltiplas imagens quando o formulário permitir
  - câmera via seletor do Android
  - retorno correto do arquivo para o formulário HTML
- Download de arquivos habilitado via DownloadManager.
- Notificações FCM revisadas:
  - leitura de `deep_link`, `deeplink`, `url`, `pedido_id`, `chat_id`, `chamado_id`
  - abertura direta do pedido, chat, chamado, oportunidades ou perfil
  - suporte a intent recebido ao abrir o app pela notificação
- Ícone pequeno de notificação trocado para recurso vetorial compatível.

## Observação sobre notificações

Para abertura direta funcionar com o app fechado, prefira enviar push FCM com `data` contendo um dos campos:

```json
{
  "title": "Nova mensagem",
  "body": "Você recebeu uma mensagem no pedido #50",
  "deep_link": "maridobh://chat/50"
}
```

Também funciona com:

```json
{
  "pedido_id": "50",
  "type": "chat"
}
```
