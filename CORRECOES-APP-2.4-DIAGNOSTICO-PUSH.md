# MaridoBH Profissional Android 2.4

Correções aplicadas:

- Indicador/Radar do app movido para o canto inferior esquerdo para não cobrir o menu hambúrguer do WordPress.
- Indicador com visual arredondado e cores consistentes.
- Diagnóstico passa a reconhecer a Localização Precisa quando o painel solicita geolocalização pelo WebView.
- Ao conceder permissão de localização para o site, o app também ativa o gerenciador nativo de Localização Precisa.
- Deep links de push foram ampliados para aceitar mais formatos de payload: `pedido_id`, `servico_id`, `atendimento_id`, `entity_id`, `screen`, `type`, `target`, `url`, `deep_link` etc.
- Notificação adiciona marcador `mbh_from_push` e preserva todos os dados recebidos para abrir o destino correto.
- Versão atualizada para 2.4.0.

Observação importante:
Para abrir direto o pedido ao tocar na notificação, o push enviado pelo plugin precisa incluir pelo menos um identificador do pedido/serviço, por exemplo:

```json
{
  "pedido_id": "52",
  "type": "pedido"
}
```

ou um link direto:

```json
{
  "deep_link": "maridobh://pedido/52"
}
```
