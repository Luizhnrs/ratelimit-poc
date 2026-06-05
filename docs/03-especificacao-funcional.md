# Especificacao Funcional

## Modelo de rate limiting

Para a primeira versao, o sistema usara janela fixa.

Exemplo:

- Limite: 10 requisicoes.
- Janela: 60 segundos.
- Cliente: `client-a`.

Durante uma janela de 60 segundos, `client-a` pode fazer ate 10 requisicoes ao endpoint protegido. A partir da 11a requisicao dentro da mesma janela, o sistema deve responder `429 Too Many Requests`.

## Chave Redis

Formato proposto:

```text
rate-limit:{clientId}:{route}
```

Exemplo:

```text
rate-limit:client-a:/api/protected
```

## Fluxo de decisao

1. Requisicao chega ao Load Balancer.
2. Load Balancer encaminha para uma instancia Spring Boot.
3. Filtro de rate limiting identifica o cliente.
4. Filtro monta a chave Redis.
5. Aplicacao executa operacao atomica no Redis:
   - incrementa contador;
   - define expiracao se a chave for nova;
   - retorna total atual e tempo restante.
6. Se contador atual for menor ou igual ao limite, a requisicao segue para o controller.
7. Se contador atual for maior que o limite, a requisicao e bloqueada com `429`.

## Contrato HTTP

### `GET /api/protected`

Endpoint protegido por rate limiting.

#### Requisicao

Headers opcionais:

```http
X-Client-Id: client-a
```

#### Resposta permitida

```http
HTTP/1.1 200 OK
X-RateLimit-Limit: 10
X-RateLimit-Remaining: 9
X-RateLimit-Reset: 60
```

```json
{
  "message": "Request accepted",
  "instance": "rate-limiter-api-1"
}
```

#### Resposta bloqueada

```http
HTTP/1.1 429 Too Many Requests
X-RateLimit-Limit: 10
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 42
Retry-After: 42
```

```json
{
  "error": "rate_limit_exceeded",
  "message": "Too many requests. Try again later."
}
```

## Headers de resposta

### `X-RateLimit-Limit`

Quantidade maxima de requisicoes permitidas por janela.

### `X-RateLimit-Remaining`

Quantidade restante de requisicoes permitidas na janela atual. Deve ser `0` quando o limite for excedido.

### `X-RateLimit-Reset`

Tempo aproximado, em segundos, ate a janela atual expirar.

### `Retry-After`

Tempo em segundos para o cliente tentar novamente. Deve ser retornado apenas em respostas `429`.

## Regras de negocio

### RN-001: Endpoint de saude nao consome limite

Chamadas a `/actuator/health` nao devem incrementar contadores de rate limit.

### RN-002: Limite por cliente e rota

O limite deve ser calculado por combinacao de cliente e rota protegida.

### RN-003: Cliente sem header

Quando `X-Client-Id` nao for informado, o sistema deve usar o IP remoto como identificador.

### RN-004: Falha de Redis

Na versao inicial, se o Redis estiver indisponivel, a API deve responder `503 Service Unavailable` para endpoints protegidos.

Motivo: falhar fechado protege a API e deixa evidente que o componente central de controle esta indisponivel.

### RN-005: Reset de janela

Apos a expiracao da chave no Redis, o cliente deve voltar a ter o limite completo disponivel.

## Parametros configuraveis

| Parametro | Padrao | Descricao |
| --- | ---: | --- |
| `RATE_LIMIT_MAX_REQUESTS` | `10` | Maximo de requisicoes por janela |
| `RATE_LIMIT_WINDOW_SECONDS` | `60` | Duracao da janela em segundos |
| `SPRING_DATA_REDIS_HOST` | `localhost` | Host do Redis |
| `SPRING_DATA_REDIS_PORT` | `6379` | Porta do Redis |

