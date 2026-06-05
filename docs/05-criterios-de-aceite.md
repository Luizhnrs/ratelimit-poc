# Criterios de Aceite e Testes

## Criterios de aceite

### CA-001: Requisicoes dentro do limite sao aceitas

Dado um cliente identificado por `X-Client-Id`,
quando ele fizer requisicoes dentro do limite configurado,
entao a API deve responder `200 OK`.

### CA-002: Requisicoes acima do limite sao bloqueadas

Dado um limite de 10 requisicoes por 60 segundos,
quando o mesmo cliente fizer a 11a chamada dentro da janela,
entao a API deve responder `429 Too Many Requests`.

### CA-003: Limite e compartilhado entre instancias

Dado que existem duas ou mais replicas da aplicacao,
quando o Load Balancer distribuir chamadas do mesmo cliente entre as replicas,
entao o contador deve ser compartilhado e o limite deve ser aplicado globalmente.

### CA-004: Clientes diferentes possuem contadores diferentes

Dado dois clientes com `X-Client-Id` diferentes,
quando ambos chamarem o endpoint protegido,
entao cada cliente deve possuir seu proprio limite.

### CA-005: Janela expirada libera novas requisicoes

Dado um cliente que excedeu o limite,
quando a janela expirar,
entao novas requisicoes do cliente devem voltar a ser aceitas.

### CA-006: Endpoint de saude nao e limitado

Dado qualquer cliente,
quando ele chamar `/actuator/health`,
entao a chamada nao deve consumir limite e nao deve retornar `429`.

### CA-007: Falha do Redis bloqueia endpoint protegido

Dado que o Redis esta indisponivel,
quando um cliente chamar o endpoint protegido,
entao a API deve responder `503 Service Unavailable`.

## Testes automatizados sugeridos

### Unidade

- `RateLimitService` permite requisicao abaixo do limite.
- `RateLimitService` bloqueia requisicao acima do limite.
- `RateLimitService` calcula corretamente `remaining`.
- `RateLimitService` retorna `retryAfter` baseado no TTL.

### Integracao

- Aplicacao integrada com Redis via Testcontainers.
- Script Lua incrementa contador e preserva TTL.
- Endpoint protegido retorna headers esperados.
- Endpoint protegido retorna `429` apos limite.

### Contrato HTTP

- `GET /api/protected` retorna payload de sucesso.
- `GET /api/protected` retorna payload de erro quando bloqueado.
- `GET /actuator/health` permanece sem rate limit.

### Ambiente distribuido

- Subir duas replicas.
- Enviar chamadas sequenciais via Load Balancer.
- Confirmar que o bloqueio ocorre pelo contador compartilhado, nao por instancia isolada.

## Comandos de validacao manual planejados

Os comandos finais serao definidos apos a implementacao. A intencao e validar com algo semelhante a:

```bash
for i in {1..12}; do
  curl -i -H "X-Client-Id: client-a" http://localhost:8080/api/protected
done
```

Resultado esperado:

- Primeiras 10 chamadas: `200 OK`.
- Chamadas seguintes dentro da mesma janela: `429 Too Many Requests`.

## Definition of Done da primeira versao

- Requisitos documentados.
- Aplicacao implementada conforme especificacao.
- Redis usado para contador compartilhado.
- Docker Compose funcional para simulacao local.
- Manifests Kubernetes presentes.
- Testes automatizados cobrindo regra principal.
- README com instrucoes de execucao e validacao.

