package kafka.producer;

import kafka.model.EventoEncomenda;
import kafka.model.KafkaConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Properties;

/**
 * Producer que simula o sistema de campo da LogFast.
 *
 * Publica eventos de progressao de status para 5 encomendas,
 * passando por todos os estagios do ciclo de vida definidos
 * em EventoEncomenda.PROGRESSAO.
 *
 * Conceitos demonstrados:
 *   - Utilizacao de Key para garantir ordenacao por encomenda
 *   - Garantia de entrega: acks=all + enable.idempotence=true
 *   - Callback assincrono para monitoramento do envio
 *   - Uso de linger.ms para otimizacao de lotes
 */
public class EventoEncomendaProducer {

    public static void main(String[] args) throws InterruptedException {

        Properties props = new Properties();

        // ── Configuracao de conexao ───────────────────────────────────────────
        props.put("bootstrap.servers", KafkaConfig.BOOTSTRAP_SERVERS);
        props.put("key.serializer",    KafkaConfig.STRING_SERIALIZER);
        props.put("value.serializer",  KafkaConfig.STRING_SERIALIZER);

        // ── Garantias de entrega ──────────────────────────────────────────────
        // acks=all: aguarda confirmacao de todos os replicas sincronos (ISR)
        props.put("acks", "all");

        // Previne duplicatas em retransmissoes causadas por falhas de rede.
        // O broker associa um Producer ID (PID) a este producer e rejeita
        // registros com o mesmo PID e numero de sequencia.
        props.put("enable.idempotence", "true");

        // Numero de tentativas automaticas em erros transientes
        props.put("retries", "3");

        // ── Otimizacao de throughput ──────────────────────────────────────────
        // Aguarda 5ms para agrupar registros em lotes maiores antes do envio.
        // Aumenta ligeiramente a latencia, mas reduz o numero de requisicoes.
        props.put("linger.ms", "5");

        System.out.println("=== Producer iniciado ===");
        System.out.printf("Topico : %s%n", KafkaConfig.TOPICO_ENCOMENDAS);
        System.out.printf("Broker : %s%n%n", KafkaConfig.BOOTSTRAP_SERVERS);

        // try-with-resources garante producer.close() ao final, mesmo em excecoes
        try (var producer = new KafkaProducer<String, String>(props)) {

            String[] encomendas = {"ENC-001", "ENC-002", "ENC-003", "ENC-004", "ENC-005"};
            String[] cidades    = {"Sao Paulo", "Campinas", "Goiania", "Brasilia", "Belo Horizonte"};

            for (int i = 0; i < encomendas.length; i++) {
                String encId  = encomendas[i];
                String cidade = cidades[i];

                for (String status : EventoEncomenda.PROGRESSAO) {

                    EventoEncomenda evento = new EventoEncomenda(
                        encId,
                        status,
                        cidade,
                        "Evento gerado automaticamente - status: " + status
                    );

                    // A Key e o codigo da encomenda.
                    // O Kafka aplica hash murmur2 sobre a Key para determinar
                    // a particao de destino. Todos os eventos de ENC-001 vao
                    // para a mesma particao, garantindo ordenacao causal.
                    String key   = encId;
                    String value = evento.toJson();

                    var record = new ProducerRecord<>(
                        KafkaConfig.TOPICO_ENCOMENDAS,
                        key,
                        value
                    );

                    // Envio assincrono com callback.
                    // O callback e invocado na I/O thread interna do producer,
                    // nao na thread principal. Operacoes de sincronizacao sao
                    // necessarias ao acessar estado compartilhado no callback.
                    producer.send(record, EventoEncomendaProducer::onEnvio);

                    // Intervalo simulado entre eventos do mesmo ciclo
                    Thread.sleep(50);
                }

                System.out.printf("Ciclo completo para %s (%d eventos publicados)%n",
                    encId, EventoEncomenda.PROGRESSAO.length);
            }

            // Aguarda a confirmacao de todos os registros pendentes no buffer
            // antes de encerrar o producer.
            producer.flush();
            System.out.printf("%nTodos os eventos publicados. Total: %d%n",
                encomendas.length * EventoEncomenda.PROGRESSAO.length);
        }
    }

    /**
     * Callback invocado pelo producer apos a resposta do broker.
     *
     * @param meta Metadados do registro confirmado (particao e offset)
     * @param ex   Excecao em caso de falha; null em caso de sucesso
     */
    private static void onEnvio(RecordMetadata meta, Exception ex) {
        if (ex != null) {
            System.err.println("ERRO ao publicar: " + ex.getMessage());
            return;
        }
        System.out.printf("OK | topico=%-30s part=%d off=%d%n",
            meta.topic(), meta.partition(), meta.offset());
    }
}
