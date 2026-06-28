# Localização Precisa e Jornada de Trabalho

## Conceito

O profissional continua recebendo oportunidades enquanto tiver plano ativo. A Localização Precisa não substitui o plano; ela apenas melhora a qualidade da distribuição.

## Estados exibidos

### Radar aproximado
O app usa a última localização conhecida ou os dados cadastrados no perfil.

### GPS preciso
O app está compartilhando localização mais recente com o plugin.

### Jornada ativa
O profissional iniciou sua jornada de trabalho. O app registra o tempo online e sincroniza com o servidor.

## Qualidade da localização

- `excelente`: localização recente e com precisão até 15m.
- `boa`: precisão entre 15m e 60m.
- `baixa`: precisão acima de 60m.
- `desatualizada`: localização antiga.
- `sem_localizacao`: nenhuma localização registrada.

## Integração com o plugin

O plugin deve tratar:

```text
POST /wp-json/mbh/v1/mobile/location
POST /wp-json/mbh/v1/mobile/work-session
```

Essas rotas devem validar usuário autenticado e registrar as informações no perfil do profissional/dispositivo.
