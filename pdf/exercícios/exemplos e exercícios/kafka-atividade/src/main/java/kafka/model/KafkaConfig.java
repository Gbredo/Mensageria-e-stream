package kafka.model;

/**
 * Constantes de configuracao compartilhadas entre produtores e consumidores.
 *
 * Centralizar as configuracoes aqui evita repeticao e facilita
 * a alteracao do ambiente (ex: trocar localhost por um host remoto).
 */
public final class KafkaConfig {

    private KafkaConfig() {}

    // ── Conexao ──────────────────────────────────────────────────────────────
    public static final String BOOTSTRAP_SERVERS = "localhost:9092";

    // ── Topicos ──────────────────────────────────────────────────────────────
    public static final String TOPICO_ENCOMENDAS = "logfast.eventos.encomenda";
    public static final String TOPICO_DLQ        = "logfast.dlq";

    // ── Consumer Groups ──────────────────────────────────────────────────────
    public static final String GROUP_RASTREAMENTO = "grupo-rastreamento";
    public static final String GROUP_ANALYTICS    = "grupo-analytics";
    public static final String GROUP_DLQ_DEMO     = "grupo-dlq-demo";
    public static final String GROUP_DLQ_MONITOR  = "grupo-dlq-monitor";

    // ── Serializadores / Desserializadores ───────────────────────────────────
    public static final String STRING_SERIALIZER =
        "org.apache.kafka.common.serialization.StringSerializer";
    public static final String STRING_DESERIALIZER =
        "org.apache.kafka.common.serialization.StringDeserializer";

    // ── Parametros de retry para DLQ ─────────────────────────────────────────
    public static final int MAX_TENTATIVAS = 3;
}
