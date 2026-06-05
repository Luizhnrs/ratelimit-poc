# Visao e Escopo

## Contexto

APIs publicas ou internas expostas em ambientes escalaveis precisam controlar o volume de chamadas para evitar abuso, proteger recursos downstream e manter previsibilidade operacional.

Em uma arquitetura com multiplas instancias atras de um Load Balancer, um rate limiter em memoria local nao e suficiente, porque cada instancia manteria seu proprio contador. O projeto deve demonstrar um rate limiter distribuido, usando Redis como fonte compartilhada de estado.

## Objetivo

Criar uma aplicacao Spring Boot simples que exponha endpoints HTTP protegidos por rate limiting distribuido. A aplicacao deve rodar localmente com Redis e permitir simulacao de multiplas instancias atras de um Load Balancer.

## Publico alvo

- Desenvolvedores backend estudando sistemas distribuidos.
- Pessoas aprendendo Spring Boot, Redis e Kubernetes.
- Avaliadores tecnicos interessados em observar design, testes e comportamento sob escala horizontal.

## Escopo inicial

O projeto deve conter:

- API Spring Boot com ao menos um endpoint protegido.
- Redis como armazenamento compartilhado dos contadores de rate limit.
- Algoritmo simples e previsivel para janela de tempo.
- Suporte a multiplas instancias da aplicacao.
- Configuracao local com Docker.
- Manifests Kubernetes para rodar a aplicacao com replicas.
- Documentacao para executar, testar e validar o comportamento.

## Fora do escopo inicial

- Autenticacao real de usuarios.
- Painel administrativo.
- Persistencia em banco relacional.
- Cluster Redis de producao.
- Observabilidade avancada com Prometheus, Grafana ou tracing distribuido.
- Algoritmos complexos como sliding window log ou token bucket distribuido com refill continuo.

## Hipoteses

- A aplicacao sera criada com Java 25.
- O framework principal sera Spring Boot 4.0.6.
- O ambiente local tera Docker disponivel.
- O Redis pode rodar como container.
- O cluster Kubernetes local pode ser Kind, Minikube ou Docker Desktop Kubernetes.
- O rate limit inicial sera aplicado por IP ou por header de cliente, sem autenticacao real.
