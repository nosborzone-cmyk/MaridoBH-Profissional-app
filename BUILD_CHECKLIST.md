# Checklist de teste — MaridoBH Profissional Android v1.0

## Abertura

- [ ] Splash aparece corretamente.
- [ ] App abre sem travar.
- [ ] WebView carrega `https://maridobh.com.br/painel-profissional/`.
- [ ] Erro de internet mostra tela amigável.

## Login

- [ ] Profissional consegue entrar.
- [ ] Sessão permanece salva ao fechar e abrir o app.
- [ ] Logout funciona corretamente.

## Painel

- [ ] Dashboard profissional aparece correto.
- [ ] Oportunidades abrem normalmente.
- [ ] Meu Trabalho/Meus Serviços abre normalmente.
- [ ] Chat abre corretamente.
- [ ] Chamados abrem corretamente.

## Upload

- [ ] Envio de foto pelo chat funciona.
- [ ] Seleção de galeria funciona.
- [ ] Câmera funciona, se habilitada no WebView.

## Push

- [ ] Token FCM é gerado.
- [ ] Token é enviado ao plugin.
- [ ] Push de teste chega no aparelho.
- [ ] Ao tocar no push, app abre no destino correto.

## Localização

- [ ] Permissão de localização é solicitada.
- [ ] Localização Precisa ativa corretamente.
- [ ] Localização é enviada ao plugin.
- [ ] Qualidade da localização aparece no diagnóstico.
- [ ] Desativar Localização Precisa funciona.

## Jornada

- [ ] Iniciar Jornada funciona.
- [ ] Tempo online é registrado.
- [ ] Encerrar Jornada mostra resumo.
- [ ] Evento de jornada é enviado ao plugin.

## Diagnóstico

- [ ] Internet detectada.
- [ ] GPS detectado.
- [ ] Push detectado.
- [ ] Bridge detectada.
- [ ] Sincronização detectada.
- [ ] Botões de correção abrem configurações corretas.

## Offline

- [ ] Sem internet, ações entram na fila.
- [ ] Ao voltar internet, fila sincroniza.
- [ ] Usuário vê status de sincronização.

## Release

- [ ] `MBH_DEBUG_MODE=false`.
- [ ] App assinado.
- [ ] AAB gerado.
- [ ] Política de privacidade disponível.
