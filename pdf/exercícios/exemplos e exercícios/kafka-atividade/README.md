# Apache Kafka com Java — Atividade Pratica

Projeto Maven de suporte à atividade prática da disciplina de Sistemas Distribuídos.

---

## Pré-requisitos

| Ferramenta | Versão mínima |
|------------|---------------|
| Java       | 17            |
| Maven      | 3.8           |
| Docker     | 24            |
| Docker Compose | 2.x       |

---

## Estrutura do Projeto

```
kafka-atividade/
├── docker-compose.yml              # Kafka (KRaft) + Kafka-UI
├── pom.xml
└── src/main/java/kafka/
    ├── model/
    │   ├── KafkaConfig.java        # Constantes compartilhadas
    │   └── EventoEncomenda.java    # Modelo de domínio + serialização JSON
    ├── producer/
    │   └── EventoEncomendaProducer.java   # Tarefa 2
    └── consumer/
        ├── RastreamentoConsumer.java      # Tarefa 3 — grupo-rastreamento
        ├── AnalyticsConsumer.java         # Tarefa 3 — grupo-analytics (CSV)
        ├── ConsumerComDLQ.java            # Tarefa 4 — retry + DLQ
        └── DlqMonitorConsumer.java        # Tarefa 4 — monitor da DLQ
```

---

## Inicialização do Ambiente

```bash
# 1. Subir Kafka + Kafka-UI
docker compose up -d

# 2. Aguardar o broker inicializar (≈ 15 segundos)
docker compose logs kafka | grep "started"

# 3. Acessar a Kafka-UI
# http://localhost:8080
```

---

## Criação dos Tópicos (Tarefa 1)

Execute os comandos dentro do container Kafka:

```bash
# Tópico principal — 3 partições, retenção de 30 dias
docker exec -it kafka kafka-topics \
  --create --topic logfast.eventos.encomenda \
  --partitions 3 --replication-factor 1 \
  --config retention.ms=2592000000 \
  --bootstrap-server localhost:9092

# Tópico DLQ — 1 partição, retenção ilimitada
docker exec -it kafka kafka-topics \
  --create --topic logfast.dlq \
  --partitions 1 --replication-factor 1 \
  --config retention.ms=-1 \
  --bootstrap-server localhost:9092

# Verificar os tópicos criados
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092
```

---

## Compilação

```bash
mvn clean package
```

---

## Execução das Classes

### Tarefa 2 — Producer

```bash
mvn exec:java -Dexec.mainClass="kafka.producer.EventoEncomendaProducer"
```

### Tarefa 3 — Consumer Groups (executar em terminais separados)

```bash
# Terminal 1 — grupo-rastreamento
mvn exec:java -Dexec.mainClass="kafka.consumer.RastreamentoConsumer"

# Terminal 2 — grupo-analytics (gera eventos.csv)
mvn exec:java -Dexec.mainClass="kafka.consumer.AnalyticsConsumer"
```

### Tarefa 4 — Retry e DLQ (executar em terminais separados)

```bash
# Terminal 1 — consumer com retry e DLQ
mvn exec:java -Dexec.mainClass="kafka.consumer.ConsumerComDLQ"

# Terminal 2 — monitor da DLQ
mvn exec:java -Dexec.mainClass="kafka.consumer.DlqMonitorConsumer"
```

---

## Comandos CLI Úteis

```bash
# Descrever tópico (partições, réplicas, ISR)
docker exec -it kafka kafka-topics \
  --describe --topic logfast.eventos.encomenda \
  --bootstrap-server localhost:9092

# Consumir mensagens no terminal (from-beginning)
docker exec -it kafka kafka-console-consumer \
  --topic logfast.eventos.encomenda \
  --from-beginning --property print.key=true \
  --bootstrap-server localhost:9092

# Verificar offsets e lag de um Consumer Group
docker exec -it kafka kafka-consumer-groups \
  --describe --group grupo-rastreamento \
  --bootstrap-server localhost:9092

# Resetar offset para o início (usar com cautela)
docker exec -it kafka kafka-consumer-groups \
  --reset-offsets --group grupo-analytics \
  --topic logfast.eventos.encomenda \
  --to-earliest --execute \
  --bootstrap-server localhost:9092

# Listar todos os Consumer Groups
docker exec -it kafka kafka-consumer-groups \
  --list --bootstrap-server localhost:9092
```

---

## Encerrar o Ambiente

```bash
docker compose down          # encerra os containers (preserva volumes)
docker compose down -v       # encerra e remove volumes (apaga histórico)
```

---

## Referências

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [kafka-clients 3.7.0 Javadoc](https://javadoc.io/doc/org.apache.kafka/kafka-clients/3.7.0)
- [Kafka-UI](https://github.com/provectus/kafka-ui)
