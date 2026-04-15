package kafka.consumer;

import kafka.model.EventoEncomenda;
import kafka.model.KafkaConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Consumer Group: grupo-rastreamento
 *
 * Mantem em memoria o ultimo status conhecido de cada encomenda.
 * Imprime o estado consolidado apos cada lote processado.
 *
 * Conceitos demonstrados:
 *   - Inscricao em topico via consumer.subscribe()
 *   - Commit manual de offset com commitSync()
 *   - Garantia at-least-once: offset confirmado somente apos processamento
 *   - Loop de polling como padrao de consumo no Kafka
 *
 * Para encerrar o consumer graciosamente, pressione Ctrl+C.
 * O ShutdownHook imprimira o estado final das encomendas.
 */
public class RastreamentoConsumer {

    // Mapa em memoria: encomendaId -> ultimo status recebido
    private static final Map<String, String> ESTADO_ATUAL = new HashMap<>();

    public static void main(String[] args) {

        Properties props = new Properties();

        // ── Configuracao de conexao ───────────────────────────────────────────
        props.put("bootstrap.servers", KafkaConfig.BOOTSTRAP_SERVERS);
        props.put("key.deserializer",   KafkaConfig.STRING_DESERIALIZER);
        props.put("value.deserializer", KafkaConfig.STRING_DESERIALIZER);

        // ── Configuracao do Consumer Group ────────────────────────────────────
        // group.id e o identificador deste consumer group.
        // O Kafka distribui as particoes do topico entre todos os consumers
        // com o mesmo group.id. Ao subir multiplas instancias desta classe,
        // o Kafka rebalanceia automaticamente as particoes entre elas.
        props.put("group.id", KafkaConfig.GROUP_RASTREAMENTO);

        // ── Comportamento de offset ───────────────────────────────────────────
        // earliest: ao nao encontrar offset gravado para o group.id
        // (primeira execucao), inicia a leitura do offset 0 de cada particao.
        // Use "latest" em producao para iniciar apenas dos eventos novos.
        props.put("auto.offset.reset", "earliest");

        // Desabilita o commit automatico. O offset sera confirmado manualmente
        // via commitSync() apos o processamento bem-sucedido de cada lote.
        props.put("enable.auto.commit", "false");

        // Maximo de registros retornados por poll().
        // Ajustar conforme a capacidade de processamento da aplicacao.
        props.put("max.poll.records", "100");

        System.out.println("=== Consumer de Rastreamento iniciado ===");
        System.out.printf("Group  : %s%n", KafkaConfig.GROUP_RASTREAMENTO);
        System.out.printf("Topico : %s%n%n", KafkaConfig.TOPICO_ENCOMENDAS);

        // ShutdownHook: imprime o estado final ao encerrar com Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n=== Estado final das encomendas ===");
            ESTADO_ATUAL.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> System.out.printf("  %-10s -> %s%n", e.getKey(), e.getValue()));
        }));

        try (var consumer = new KafkaConsumer<String, String>(props)) {

            // Inscreve o consumer no topico.
            // O broker atribuira as particoes a este consumer com base no group.id.
            consumer.subscribe(List.of(KafkaConfig.TOPICO_ENCOMENDAS));

            // Loop de polling: padrao de consumo no Kafka.
            // poll() solicita um lote de registros ao broker e retorna
            // imediatamente caso nao haja eventos novos ou apos 300ms.
            // IMPORTANTE: poll() deve ser chamado regularmente. O broker usa
            // o intervalo entre chamadas para detectar consumers mortos
            // (via heartbeat implicito). Se poll() nao for chamado dentro de
            // max.poll.interval.ms (padrao: 5min), o consumer e removido do grupo.
            while (true) {
                ConsumerRecords<String, String> lote =
                    consumer.poll(Duration.ofMillis(300));

                for (ConsumerRecord<String, String> record : lote) {
                    processar(record);
                }

                if (!lote.isEmpty()) {
                    // Commit manual: grava o offset em __consumer_offsets.
                    // commitSync() bloqueia ate a confirmacao do broker.
                    // O offset e confirmado somente apos o lote inteiro
                    // ter sido processado sem excecao.
                    consumer.commitSync();
                    imprimirEstado();
                }
            }
        }
    }

    private static void processar(ConsumerRecord<String, String> record) {
        EventoEncomenda evento = EventoEncomenda.fromJson(record.value());

        // Atualiza o estado: como os eventos sao ordenados por particao
        // e a Key e o ID da encomenda, o ultimo evento lido representa
        // o status mais recente da encomenda.
        ESTADO_ATUAL.put(record.key(), evento.getStatus());

        System.out.printf("[part=%d off=%4d] %-10s -> %-15s | %s%n",
            record.partition(),
            record.offset(),
            record.key(),
            evento.getStatus(),
            evento.getCidade()
        );
    }

    private static void imprimirEstado() {
        System.out.println("  --- Estado consolidado ---");
        ESTADO_ATUAL.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(e -> System.out.printf("  %-10s -> %s%n", e.getKey(), e.getValue()));
        System.out.println();
    }
}
