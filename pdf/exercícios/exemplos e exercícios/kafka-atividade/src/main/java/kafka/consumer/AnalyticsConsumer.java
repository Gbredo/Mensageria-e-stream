package kafka.consumer;

import kafka.model.EventoEncomenda;
import kafka.model.KafkaConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

/**
 * Consumer Group: grupo-analytics
 *
 * Persiste todos os eventos recebidos em um arquivo CSV para fins
 * de auditoria e relatorios historicos.
 *
 * Conceitos demonstrados:
 *   - Consumer Group independente do grupo-rastreamento
 *   - Isolamento de offsets: cada grupo mantem seu proprio progresso
 *   - Ordem de operacoes: flush() do arquivo ANTES de commitSync()
 *   - Garantia at-least-once: offset confirmado apos persistencia
 *
 * Arquivo gerado: eventos.csv (no diretorio de execucao)
 * Para encerrar graciosamente, pressione Ctrl+C.
 */
public class AnalyticsConsumer {

    private static final String ARQUIVO_CSV = "eventos.csv";

    public static void main(String[] args) {

        Properties props = new Properties();

        props.put("bootstrap.servers", KafkaConfig.BOOTSTRAP_SERVERS);
        props.put("key.deserializer",   KafkaConfig.STRING_DESERIALIZER);
        props.put("value.deserializer", KafkaConfig.STRING_DESERIALIZER);

        // group.id DIFERENTE do RastreamentoConsumer.
        // O Kafka mantera offsets completamente separados para este grupo.
        // Os dois consumers podem rodar simultaneamente sem interferencia.
        props.put("group.id",           KafkaConfig.GROUP_ANALYTICS);
        props.put("auto.offset.reset",  "earliest");
        props.put("enable.auto.commit", "false");
        props.put("max.poll.records",   "100");

        System.out.println("=== Consumer de Analytics iniciado ===");
        System.out.printf("Group  : %s%n",    KafkaConfig.GROUP_ANALYTICS);
        System.out.printf("Topico : %s%n",    KafkaConfig.TOPICO_ENCOMENDAS);
        System.out.printf("Saida  : %s%n%n",  ARQUIVO_CSV);

        // FileWriter com append=true: preserva registros de execucoes anteriores.
        // Em producao, avaliar se o comportamento correto e truncar ou acumular.
        try (var consumer = new KafkaConsumer<String, String>(props);
             var writer   = new PrintWriter(new FileWriter(ARQUIVO_CSV, true))) {

            // Cabecalho do CSV (gravado apenas na primeira linha do arquivo)
            writer.println("encomendaId,status,timestamp,cidade");

            consumer.subscribe(List.of(KafkaConfig.TOPICO_ENCOMENDAS));

            while (true) {
                var lote = consumer.poll(Duration.ofMillis(300));

                for (ConsumerRecord<String, String> record : lote) {
                    EventoEncomenda evento = EventoEncomenda.fromJson(record.value());

                    // Grava uma linha CSV por evento
                    writer.printf("%s,%s,%s,%s%n",
                        evento.getEncomendaId(),
                        evento.getStatus(),
                        evento.getTimestamp(),
                        evento.getCidade()
                    );

                    System.out.printf("[part=%d off=%4d] %s -> %s%n",
                        record.partition(),
                        record.offset(),
                        evento.getEncomendaId(),
                        evento.getStatus()
                    );
                }

                if (!lote.isEmpty()) {
                    // A ordem importa: persiste os dados em disco ANTES de
                    // confirmar o offset. Se a aplicacao falhar entre o flush()
                    // e o commitSync(), o lote sera reprocessado e as linhas
                    // gravadas novamente — tratamento de duplicatas necessario.
                    writer.flush();
                    consumer.commitSync();
                    System.out.printf("Lote de %d eventos gravado no CSV.%n%n",
                        lote.count());
                }
            }

        } catch (IOException ex) {
            System.err.println("Erro ao abrir o arquivo CSV: " + ex.getMessage());
        }
    }
}
