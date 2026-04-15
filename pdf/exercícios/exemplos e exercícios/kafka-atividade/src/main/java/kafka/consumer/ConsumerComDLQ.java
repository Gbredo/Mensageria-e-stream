package kafka.consumer;

import kafka.model.EventoEncomenda;
import kafka.model.KafkaConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;

/**
 * Consumer Group: grupo-dlq-demo
 *
 * Demonstra o padrao de tratamento de falhas com Dead Letter Queue (DLQ).
 *
 * Para cada evento recebido:
 *   1. Tenta processar (ate MAX_TENTATIVAS vezes).
 *   2. Em caso de sucesso, avanca normalmente.
 *   3. Apos esgotar as tentativas, encaminha o evento para o topico DLQ
 *      com headers de diagnostico e continua o processamento do lote.
 *
 * Conceitos demonstrados:
 *   - Padrao Retry com backoff progressivo
 *   - Dead Letter Queue como topico Kafka separado
 *   - Headers como metadados de diagnostico
 *   - commitSync() mesmo apos encaminhamento para DLQ (avanca o offset)
 *
 * IMPORTANTE: o topico logfast.dlq deve existir antes de executar.
 * Cria-lo via: kafka-topics --create --topic logfast.dlq
 *              --partitions 1 --replication-factor 1
 *              --config retention.ms=-1
 *              --bootstrap-server localhost:9092
 */
public class ConsumerComDLQ {

    public static void main(String[] args) {

        // ── Configuracao do Consumer ──────────────────────────────────────────
        Properties propsConsumer = new Properties();
        propsConsumer.put("bootstrap.servers", KafkaConfig.BOOTSTRAP_SERVERS);
        propsConsumer.put("key.deserializer",   KafkaConfig.STRING_DESERIALIZER);
        propsConsumer.put("value.deserializer", KafkaConfig.STRING_DESERIALIZER);
        propsConsumer.put("group.id",           KafkaConfig.GROUP_DLQ_DEMO);
        propsConsumer.put("auto.offset.reset",  "earliest");
        propsConsumer.put("enable.auto.commit", "false");

        // ── Configuracao do Producer DLQ ──────────────────────────────────────
        Properties propsProducer = new Properties();
        propsProducer.put("bootstrap.servers", KafkaConfig.BOOTSTRAP_SERVERS);
        propsProducer.put("key.serializer",    KafkaConfig.STRING_SERIALIZER);
        propsProducer.put("value.serializer",  KafkaConfig.STRING_SERIALIZER);
        propsProducer.put("acks",              "all");

        System.out.println("=== Consumer com DLQ iniciado ===");
        System.out.printf("Group  : %s%n", KafkaConfig.GROUP_DLQ_DEMO);
        System.out.printf("Topico : %s%n", KafkaConfig.TOPICO_ENCOMENDAS);
        System.out.printf("DLQ    : %s%n%n", KafkaConfig.TOPICO_DLQ);

        try (var consumer    = new KafkaConsumer<String, String>(propsConsumer);
             var dlqProducer = new KafkaProducer<String, String>(propsProducer)) {

            consumer.subscribe(List.of(KafkaConfig.TOPICO_ENCOMENDAS));

            while (true) {
                var lote = consumer.poll(Duration.ofMillis(300));

                for (var record : lote) {
                    processarComRetry(record, dlqProducer);
                }

                // O commitSync() e chamado mesmo que alguns eventos tenham
                // sido encaminhados para a DLQ. Isso e intencional: o consumer
                // deve avancar no offset para nao reprocessar indefinidamente
                // os mesmos eventos problematicos. Eles ja foram preservados
                // na DLQ para analise e reprocessamento posterior.
                if (!lote.isEmpty()) {
                    consumer.commitSync();
                }
            }
        }
    }

    /**
     * Tenta processar o evento ate MAX_TENTATIVAS vezes.
     * Se todas as tentativas falharem, encaminha para a DLQ.
     */
    private static void processarComRetry(
            ConsumerRecord<String, String> record,
            KafkaProducer<String, String> dlqProducer) {

        int tentativa       = 0;
        Exception ultimaEx  = null;

        while (tentativa < KafkaConfig.MAX_TENTATIVAS) {
            try {
                processarEvento(record);
                // Sucesso: registra e encerra as tentativas
                System.out.printf("OK [tentativa=%d] key=%-10s status=%s%n",
                    tentativa + 1,
                    record.key(),
                    EventoEncomenda.fromJson(record.value()).getStatus()
                );
                return;

            } catch (Exception ex) {
                tentativa++;
                ultimaEx = ex;
                System.err.printf("FALHA [tentativa=%d/%d] key=%s: %s%n",
                    tentativa, KafkaConfig.MAX_TENTATIVAS,
                    record.key(), ex.getMessage());

                // Backoff progressivo: espera cresce a cada tentativa
                // (100ms, 200ms, 300ms) para nao sobrecarregar o sistema
                if (tentativa < KafkaConfig.MAX_TENTATIVAS) {
                    try { Thread.sleep(100L * tentativa); }
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            }
        }

        // Esgotou as tentativas: encaminha para a DLQ
        encaminharParaDlq(record, dlqProducer, ultimaEx);
    }

    /**
     * Logica de negocio do evento.
     *
     * Lanca RuntimeException para eventos com status TRIAGEM,
     * simulando uma falha de processamento para demonstracao.
     */
    private static void processarEvento(ConsumerRecord<String, String> record) {
        EventoEncomenda evento = EventoEncomenda.fromJson(record.value());

        // SIMULACAO DE FALHA: eventos com status TRIAGEM lancam excecao.
        // Em producao, substituir por logica real (ex: chamada a servico
        // externo, validacao de schema, persistencia em banco de dados).
        if (EventoEncomenda.STATUS_TRIAGEM.equals(evento.getStatus())) {
            throw new RuntimeException(
                "Status TRIAGEM requer validacao manual — processamento rejeitado.");
        }

        System.out.printf("Processado: %s%n", evento);
    }

    /**
     * Publica o evento na DLQ com headers de diagnostico.
     *
     * Os headers permitem que o consumer monitor (DlqMonitorConsumer)
     * e as equipes de operacoes identifiquem a causa da falha sem
     * precisar analisar o payload do evento.
     */
    private static void encaminharParaDlq(
            ConsumerRecord<String, String> record,
            KafkaProducer<String, String> dlqProducer,
            Exception causa) {

        // Preserva a Key e o Value originais para permitir reprocessamento
        var dlqRecord = new ProducerRecord<>(
            KafkaConfig.TOPICO_DLQ,
            record.key(),
            record.value()
        );

        // Headers de diagnostico
        dlqRecord.headers().add("motivo",
            "MAX_TENTATIVAS_ATINGIDO".getBytes());
        dlqRecord.headers().add("topico-origem",
            record.topic().getBytes());
        dlqRecord.headers().add("particao-origem",
            String.valueOf(record.partition()).getBytes());
        dlqRecord.headers().add("offset-origem",
            String.valueOf(record.offset()).getBytes());
        dlqRecord.headers().add("mensagem-erro",
            (causa != null ? causa.getMessage() : "desconhecido").getBytes());
        dlqRecord.headers().add("timestamp-dlq",
            Instant.now().toString().getBytes());

        dlqProducer.send(dlqRecord, (meta, ex) -> {
            if (ex != null) {
                System.err.println("CRITICO: falha ao publicar na DLQ: " + ex.getMessage());
            } else {
                System.err.printf("DLQ    : key=%-10s -> part=%d off=%d%n",
                    record.key(), meta.partition(), meta.offset());
            }
        });

        dlqProducer.flush();
    }
}
