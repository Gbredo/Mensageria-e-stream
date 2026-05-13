# Demonstração: HTTP Polling vs WebSocket
## Leilão ao Vivo — ADS1242 | PUC Goiás

---

## Estrutura dos Projetos

```
leilao-polling/     → HTTP Polling   → porta 8080
leilao-websocket/   → WebSocket STOMP → porta 8081
```

---

## Como executar

### Pré-requisitos
- Java 21+
- Maven 3.8+

### Projeto 1 — HTTP Polling (porta 8080)
```bash
cd leilao-polling
mvn spring-boot:run
```
Acesse: http://localhost:8080

### Projeto 2 — WebSocket (porta 8081)
```bash
cd leilao-websocket
mvn spring-boot:run
```
Acesse: http://localhost:8081

> **Dica para a aula:** Execute ambos simultaneamente e abra em abas separadas.

---

## O que demonstrar em sala

### 1. Contador de requisições (Polling)
No projeto de polling, observe o painel superior:
- **Requisições HTTP**: sobe a cada 2 segundos, sem parar
- **Lances Reais**: só sobe quando alguém faz um lance
- **Reqs. Inúteis**: a diferença — desperdício puro

Peça para os alunos ficarem 1 minuto sem dar nenhum lance.
Em 1 minuto = 30 requisições HTTP → 0 novidades.

### 2. DevTools → Network
**Polling:**
- Abra F12 → Network → filtre por "leilao"
- Mostre as requisições aparecendo a cada 2s
- Cada uma tem overhead de headers HTTP

**WebSocket:**
- Abra F12 → Network → clique em "ws" ou "WS"
- Mostre o único handshake (101 Switching Protocols)
- Clique na conexão → aba "Messages" → veja os frames chegando

### 3. Latência
- No polling: o lance de um usuário leva até 2 segundos para aparecer na tela do outro
- No WebSocket: o lance aparece instantaneamente (< 10ms)

### 4. Modal de encerramento
Ao fim do leilão, ambas as telas mostram um resumo comparativo com as métricas.

### 5. Botão "Reiniciar Leilão"
Reseta o estado no servidor para nova demonstração sem reiniciar a aplicação.

---

## Endpoints REST (ambos os projetos)

| Método | URL                     | Projeto   | Descrição             |
|--------|-------------------------|-----------|-----------------------|
| GET    | /api/leilao             | Polling   | Estado atual (polling)|
| POST   | /api/leilao/lance       | Polling   | Fazer lance (REST)    |
| POST   | /api/leilao/reiniciar   | Polling   | Reiniciar leilão      |
| POST   | /api/reiniciar          | WebSocket | Reiniciar leilão      |

## Endpoints WebSocket (projeto leilao-websocket)

| Tipo     | Endereço           | Descrição                              |
|----------|--------------------|----------------------------------------|
| Handshake| /ws-leilao         | Ponto de conexão SockJS/WebSocket      |
| SEND     | /app/lance         | Cliente envia um lance ao servidor     |
| SUBSCRIBE| /topic/leilao      | Cliente recebe atualizações do servidor|

---

## Tabela Comparativa

| Critério               | HTTP Polling          | WebSocket             |
|------------------------|-----------------------|-----------------------|
| Conexões por cliente   | 1 a cada 2 segundos   | 1 permanente          |
| Tráfego sem novidades  | Contínuo              | Zero                  |
| Latência de atualização| Até 2 segundos        | < 10 ms               |
| Overhead de headers    | Sim (todo request)    | Mínimo (frames)       |
| Escalabilidade         | Baixa                 | Alta                  |
| Complexidade           | Simples               | Média                 |
| Caso de uso ideal      | Dados que mudam pouco | Dados em tempo real   |

---

## Tecnologias utilizadas

- **Spring Boot 3.2.5** (Web + WebSocket)
- **STOMP** — protocolo de mensagens sobre WebSocket
- **SockJS** — fallback para redes que bloqueiam WebSocket
- **SimpMessagingTemplate** — broadcast server-side para todos os clientes
