package kafka.consumer;

import kafka.model.KafkaConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

/**
 * Consumer Group: grupo-dlq-monitor
 *
 * Monitora o topico de Dead Letter Queue (logfast.dlq) e imprime
 * um relatorio formatado para cada evento inprocessavel recebido.
 *
 * Este consumer demonstra como os headers adicionados pelo ConsumerComDLQ
 * podem ser utilizados para diagnostico sem necessidade de analisar
 * o payload do evento.
 *
 * Conceitos demonstrados:
 *   - Leitura de headers de um ConsumerRecord
 *   - Consumer dedicado a monitoramento de DLQ
 *   - Relatorio estruturado de falhas
 */
public class DlqMonitorConsumer {

    public static void main(String[] args) {

        Properties props = new Properties();
        props.put("bootstrap.servers", KafkaConfig.BOOTSTRAP_SERVERS);
        props.put("key.deserializer",   KafkaConfig.STRING_DESERIALIZER);
        props.put("value.deserializer", KafkaConfig.STRING_DESERIALIZER);
        props.put("group.id",           KafkaConfig.GROUP_DLQ_MONITOR);

        // latest: monitora apenas novos eventos que chegarem a DLQ
        // Use "earliest" para inspecionar eventos ja existentes na DLQ
        props.put("auto.offset.reset",  "earliest");
        props.put("enable.auto.commit", "false");

        System.out.println("=== Monitor da DLQ iniciado ===");
        System.out.printf("Group  : %s%n",   KafkaConfig.GROUP_DLQ_MONITOR);
        System.out.printf("Topico : %s%n%n", KafkaConfig.TOPICO_DLQ);

        try (var consumer = new KafkaConsumer<String, String>(props)) {
            consumer.subscribe(List.of(KafkaConfig.TOPICO_DLQ));

            while (true) {
                var lote = consumer.poll(Duration.ofMillis(300));

                for (ConsumerRecord<String, String> record : lote) {
                    imprimirRelatorio(record);
                }

                if (!lote.isEmpty()) {
                    consumer.commitSync();
                }
            }
        }
    }

    private static void imprimirRelatorio(ConsumerRecord<String, String> record) {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("  EVENTO NA DLQ");
        System.out.printf("  Key             : %s%n", record.key());
        System.out.printf("  Part / Offset   : %d / %d%n", record.partition(), record.offset());
        System.out.printf("  Topico de origem: %s%n", lerHeader(record, "topico-origem"));
        System.out.printf("  Particao origem : %s%n", lerHeader(record, "particao-origem"));
        System.out.printf("  Offset origem   : %s%n", lerHeader(record, "offset-origem"));
        System.out.printf("  Motivo          : %s%n", lerHeader(record, "motivo"));
        System.out.printf("  Erro            : %s%n", lerHeader(record, "mensagem-erro"));
        System.out.printf("  Timestamp DLQ   : %s%n", lerHeader(record, "timestamp-dlq"));
        System.out.printf("  Payload         : %s%n", record.value());
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    private static String lerHeader(ConsumerRecord<?, ?> record, String nome) {
        Header header = record.headers().lastHeader(nome);
        return header != null ? new String(header.value()) : "(ausente)";
    }
}
