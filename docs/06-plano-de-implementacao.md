# Plano de Implementacao

## Objetivo do plano

Implementar o projeto de forma incremental, mantendo a especificacao como fonte de verdade. Cada fase deve gerar um resultado executavel ou verificavel antes da proxima etapa.

## Principios de execucao

- Implementar somente o que esta coberto pela especificacao atual.
- Manter cada fase pequena o suficiente para ser testada isoladamente.
- Priorizar comportamento distribuido correto antes de refinamentos visuais ou operacionais.
- Documentar comandos de execucao conforme forem validados.
- Evitar dependencias desnecessarias para manter o projeto didatico.

## Fase 0: Preparacao do projeto

### Objetivo

Criar a estrutura inicial da aplicacao Spring Boot com Java 25 e Spring Boot 4.0.6.

### Entregaveis

- `pom.xml` ou `build.gradle` configurado.
- Estrutura de pacotes Java.
- Classe principal da aplicacao.
- Configuracao base em `application.yml`.
- README atualizado com requisitos locais.

### Decisoes

- Usar Maven como ferramenta de build, salvo decisao posterior em contrario.
- Usar Spring Web para endpoints HTTP.
- Usar Spring Data Redis para comunicacao com Redis.
- Usar Spring Boot Actuator para health checks.

### Criterios de conclusao

- Projeto compila localmente.
- Aplicacao inicia sem Redis para endpoints nao protegidos, se possivel.
- `/actuator/health` responde corretamente.

## Fase 1: Modelo interno de rate limiting

### Objetivo

Criar a regra de decisao de rate limiting sem ainda acoplar diretamente ao controller.

### Entregaveis

- `RateLimitProperties`.
- `RateLimitDecision`.
- Interface `RateLimitService`.
- Implementacao inicial usando Redis.
- Script Lua para incremento atomico e TTL.

### Estrutura sugerida

```text
src/main/java/com/example/ratelimiter
  config
    RateLimitProperties.java
    RedisConfig.java
  ratelimit
    RateLimitDecision.java
    RateLimitService.java
    RedisRateLimitService.java
```

### Criterios de conclusao

- O servico retorna permitido quando o contador esta abaixo ou igual ao limite.
- O servico retorna bloqueado quando o contador excede o limite.
- O TTL do Redis e usado para calcular `retryAfter` e `reset`.

## Fase 2: Filtro HTTP e contrato da API

### Objetivo

Aplicar a regra de rate limiting no fluxo HTTP.

### Entregaveis

- `RateLimitFilter`.
- `ProtectedController`.
- Tratamento de erro para `429 Too Many Requests`.
- Tratamento de erro para `503 Service Unavailable` quando Redis falhar.
- Headers de resposta:
  - `X-RateLimit-Limit`
  - `X-RateLimit-Remaining`
  - `X-RateLimit-Reset`
  - `Retry-After`, apenas em bloqueio.

### Endpoints

- `GET /api/protected`
- `GET /actuator/health`

### Criterios de conclusao

- Chamadas dentro do limite retornam `200`.
- Chamadas acima do limite retornam `429`.
- Health check nao consome limite.
- Cliente e identificado por `X-Client-Id` ou IP remoto.

## Fase 3: Testes automatizados

### Objetivo

Garantir que a regra principal funcione antes de empacotar a aplicacao.

### Entregaveis

- Testes unitarios para a decisao de rate limit.
- Testes de integracao com Redis.
- Testes HTTP para endpoint protegido.

### Ferramentas sugeridas

- JUnit.
- Spring Boot Test.
- Testcontainers para Redis, se compativel com a stack escolhida.

### Cenarios minimos

- Cliente abaixo do limite recebe `200`.
- Cliente excedendo limite recebe `429`.
- Clientes diferentes possuem contadores independentes.
- Apos expiracao da janela, cliente volta a receber `200`.
- Falha de Redis retorna `503` para endpoint protegido.

### Criterios de conclusao

- Suite automatizada passa localmente.
- Testes cobrem os criterios de aceite principais do documento `05-criterios-de-aceite.md`.

## Fase 4: Containerizacao

### Objetivo

Empacotar a aplicacao para execucao local via Docker.

### Entregaveis

- `Dockerfile`.
- `.dockerignore`.
- Configuracao de variaveis de ambiente.
- Imagem da aplicacao executando com Java 25.

### Criterios de conclusao

- Imagem Docker e construida localmente.
- Container inicia e conecta ao Redis.
- Endpoint protegido funciona dentro do container.

## Fase 5: Simulacao distribuida com Docker Compose

### Objetivo

Simular multiplas instancias atras de um Load Balancer sem exigir Kubernetes imediatamente.

### Entregaveis

- `docker-compose.yml`.
- Configuracao Nginx como Load Balancer.
- Redis compartilhado.
- Duas ou mais instancias da aplicacao.

### Topologia esperada

```text
Cliente -> Nginx -> App 1
                 -> App 2
                 -> App N
Apps -> Redis
```

### Criterios de conclusao

- Chamadas passam pelo Load Balancer.
- Respostas indicam, quando possivel, qual instancia atendeu.
- O limite e aplicado globalmente, mesmo com chamadas distribuidas entre instancias.

## Fase 6: Kubernetes local

### Objetivo

Executar o mesmo sistema em Kubernetes local.

### Entregaveis

- Namespace opcional.
- Deployment do Redis.
- Service do Redis.
- Deployment da aplicacao com replicas.
- Service da aplicacao.
- ConfigMap ou variaveis de ambiente para rate limit.
- Instrucoes para acesso local.

### Estrutura sugerida

```text
k8s/
  redis.yaml
  app-config.yaml
  app-deployment.yaml
  app-service.yaml
```

### Criterios de conclusao

- Pods sobem com sucesso.
- Aplicacao resolve Redis pelo Service interno.
- Service distribui chamadas entre replicas.
- Rate limiting permanece compartilhado.

## Fase 7: Validacao operacional

### Objetivo

Documentar e executar testes manuais que provem o comportamento distribuido.

### Entregaveis

- Comandos `curl` para teste local.
- Script simples de carga, se necessario.
- Evidencias textuais no README ou documento proprio.

### Validacoes

1. Enviar requisicoes com mesmo `X-Client-Id` ate exceder o limite.
2. Confirmar retorno `429`.
3. Alterar `X-Client-Id` e confirmar novo contador.
4. Aguardar expiracao da janela e confirmar reset.
5. Escalar replicas e confirmar que o limite nao muda.

### Criterios de conclusao

- O comportamento esperado pode ser reproduzido com comandos documentados.
- O projeto demonstra claramente protecao de API em ambiente escalavel local.

## Fase 8: Polimento e documentacao final

### Objetivo

Preparar o projeto para leitura, execucao e avaliacao por outra pessoa.

### Entregaveis

- README final com:
  - requisitos;
  - como rodar localmente;
  - como rodar com Docker Compose;
  - como rodar no Kubernetes;
  - como validar o rate limiting.
- Comentarios de codigo apenas onde agregarem clareza.
- Organizacao final dos documentos de spec.

### Criterios de conclusao

- Uma pessoa nova consegue executar o projeto seguindo o README.
- A relacao entre requisitos, implementacao e testes esta clara.
- Nenhuma fase essencial depende de conhecimento implicito.

## Ordem recomendada de execucao

1. Fase 0: Preparacao do projeto.
2. Fase 1: Modelo interno de rate limiting.
3. Fase 2: Filtro HTTP e contrato da API.
4. Fase 3: Testes automatizados.
5. Fase 4: Containerizacao.
6. Fase 5: Simulacao distribuida com Docker Compose.
7. Fase 6: Kubernetes local.
8. Fase 7: Validacao operacional.
9. Fase 8: Polimento e documentacao final.

## Riscos e mitigacoes

| Risco | Impacto | Mitigacao |
| --- | --- | --- |
| Java 25 ou Spring Boot 4.0.6 indisponivel em repositorios locais | Pode bloquear build inicial | Validar toolchain na Fase 0 antes de escrever codigo demais |
| Redis indisponivel | Endpoint protegido retorna `503` | Documentar dependencia e usar Docker para ambiente local |
| Load Balancer mascarar IP real | Identificacao por IP pode ficar incorreta | Priorizar `X-Client-Id` nos testes |
| Concorrencia gerar contadores inconsistentes | Rate limit incorreto | Usar script Lua atomico no Redis |
| Kubernetes local variar por ambiente | Instrucoes podem divergir | Documentar caminho principal e alternativas para Kind/Minikube |

## Backlog inicial

| ID | Item | Fase |
| --- | --- | --- |
| BL-001 | Criar build Java 25 com Spring Boot 4.0.6 | 0 |
| BL-002 | Criar endpoint de health | 0 |
| BL-003 | Criar propriedades de rate limit | 1 |
| BL-004 | Implementar servico Redis com Lua | 1 |
| BL-005 | Criar filtro HTTP de rate limit | 2 |
| BL-006 | Criar endpoint protegido | 2 |
| BL-007 | Adicionar testes unitarios e integracao | 3 |
| BL-008 | Criar Dockerfile | 4 |
| BL-009 | Criar Compose com Redis, apps e Nginx | 5 |
| BL-010 | Criar manifests Kubernetes | 6 |
| BL-011 | Documentar validacao manual | 7 |
| BL-012 | Revisar README final | 8 |

