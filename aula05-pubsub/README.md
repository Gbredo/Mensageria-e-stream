# Aula 05 — Arquitetura Pub/Sub com Python + STOMP + ActiveMQ

## Pré-requisitos
- Docker & Docker Compose
- Python 3.10+
- Node.js 18+ (para o frontend React)

## Como executar

### 1. Subir o Message Broker
```bash
docker-compose up -d
```
Acesse o painel: http://localhost:8161 (admin / admin)

### 2. Criar ambiente virtual e instalar dependências
```bash
python -m venv .venv
source .venv/bin/activate   # Linux/macOS
.venv\Scripts\activate      # Windows

pip install -r requirements.txt
```

### 3. Iniciar os Workers (em terminais separados)
```bash
# Terminal 2
python worker_faturamento.py

# Terminal 3
python worker_logistica.py
```

### 4. Iniciar o Gateway (API Flask)
```bash
# Terminal 1
python gateway.py
```

### 5. Rodar os testes
```bash
pytest test_gateway.py -v
```

### 6. Frontend React
Copie o arquivo `frontend/App.jsx` para o seu projeto React (CRA ou Vite).

## Arquitetura
```
[React :3000] → POST → [gateway.py :5000] → STOMP → [ActiveMQ :61613]
                                                           ↓           ↓
                                              [worker_faturamento] [worker_logistica]
```

## Protocolo STOMP
- Tópicos são prefixados com `/topic/`
- Durable Subscriptions requerem `client-id` + `activemq.subscriptionName`
- Porta padrão STOMP no ActiveMQ: **61613**
