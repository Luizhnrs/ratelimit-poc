# Requisitos

## Requisitos funcionais

### RF-001: Endpoint protegido

A aplicacao deve expor um endpoint HTTP protegido por rate limiting.

Exemplo inicial:

- `GET /api/protected`

Quando a chamada estiver dentro do limite, a API deve responder com sucesso.

### RF-002: Endpoint publico de saude

A aplicacao deve expor um endpoint de saude sem rate limiting.

Exemplo inicial:

- `GET /actuator/health`

Esse endpoint deve ser usado por Docker, Kubernetes e ferramentas locais para verificar disponibilidade da instancia.

### RF-003: Identificacao do cliente

O sistema deve identificar o consumidor da API para aplicar o limite.

Ordem proposta para a versao inicial:

1. Header `X-Client-Id`, quando presente.
2. IP remoto da requisicao, quando o header nao estiver presente.

### RF-004: Contador distribuido

O contador de requisicoes deve ser compartilhado entre todas as instancias da aplicacao por meio do Redis.

Uma requisicao enviada para a instancia A deve consumir o mesmo limite observado pela instancia B.

### RF-005: Resposta quando permitido

Quando a requisicao estiver dentro do limite, o sistema deve retornar:

- Status HTTP `200 OK`.
- Payload simples indicando sucesso.
- Headers informativos de rate limit.

Headers propostos:

- `X-RateLimit-Limit`
- `X-RateLimit-Remaining`
- `X-RateLimit-Reset`

### RF-006: Resposta quando bloqueado

Quando o cliente exceder o limite, o sistema deve retornar:

- Status HTTP `429 Too Many Requests`.
- Payload de erro simples.
- Header `Retry-After`.
- Headers informativos de rate limit.

### RF-007: Configuracao por propriedades

O limite de requisicoes e a janela de tempo devem ser configuraveis por propriedades ou variaveis de ambiente.

Parametros iniciais:

- `RATE_LIMIT_MAX_REQUESTS`, padrao `10`.
- `RATE_LIMIT_WINDOW_SECONDS`, padrao `60`.

### RF-008: Simulacao com multiplas instancias

O projeto deve permitir rodar ao menos duas instancias da aplicacao localmente, ambas conectadas ao mesmo Redis.

### RF-009: Execucao em Kubernetes local

O projeto deve conter manifests Kubernetes para executar:

- Redis.
- Aplicacao Spring Boot com multiplas replicas.
- Service para a aplicacao.
- Opcionalmente Ingress ou mecanismo equivalente de acesso local.

## Requisitos nao funcionais

### RNF-000: Versoes base

O projeto deve usar Java 25 e Spring Boot 4.0.6 como stack base da aplicacao.

### RNF-001: Consistencia operacional

O rate limiting deve ser consistente entre instancias. O Redis deve ser a fonte de verdade para contagem.

### RNF-002: Atomicidade

A operacao de incrementar contador e definir expiracao deve ser atomica para evitar inconsistencias sob concorrencia.

Implementacao sugerida:

- Script Lua executado no Redis.

### RNF-003: Simplicidade local

O projeto deve ser facil de rodar localmente, com comandos documentados.

### RNF-004: Baixo acoplamento

A regra de rate limiting deve ficar isolada em componente proprio, evitando espalhar logica de controle em controllers.

### RNF-005: Observabilidade minima

A aplicacao deve registrar logs basicos quando uma requisicao for bloqueada por rate limit.

### RNF-006: Testabilidade

A regra de rate limiting deve permitir testes automatizados, incluindo cenarios de permissao, bloqueio e reset da janela.

### RNF-007: Compatibilidade com escalabilidade horizontal

Adicionar ou remover replicas da aplicacao nao deve alterar a semantica do limite aplicado a um cliente.
