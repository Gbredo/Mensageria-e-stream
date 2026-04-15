package kafka.model;

import java.time.Instant;

/**
 * Representa um evento de mudanca de status de encomenda.
 *
 * Esta classe oferece metodos utilitarios para serializar o evento
 * em JSON (para publicacao no Kafka) e para desserializa-lo a partir
 * de um JSON recebido.
 *
 * Em producao, recomenda-se substituir a serializacao manual por
 * uma biblioteca como Jackson ou Gson.
 */
public class EventoEncomenda {

    // Possiveis valores do campo status
    public static final String STATUS_COLETADO     = "COLETADO";
    public static final String STATUS_TRIAGEM      = "TRIAGEM";
    public static final String STATUS_EM_TRANSITO  = "EM_TRANSITO";
    public static final String STATUS_SAIU_ENTREGA = "SAIU_PARA_ENTREGA";
    public static final String STATUS_ENTREGUE     = "ENTREGUE";
    public static final String STATUS_FALHA        = "FALHA";

    // Progressao padrao de status para simulacao
    public static final String[] PROGRESSAO = {
        STATUS_COLETADO,
        STATUS_TRIAGEM,
        STATUS_EM_TRANSITO,
        STATUS_SAIU_ENTREGA,
        STATUS_ENTREGUE
    };

    private String encomendaId;
    private String status;
    private String timestamp;
    private String cidade;
    private String observacao;

    public EventoEncomenda() {}

    public EventoEncomenda(String encomendaId, String status, String cidade, String observacao) {
        this.encomendaId = encomendaId;
        this.status      = status;
        this.timestamp   = Instant.now().toString();
        this.cidade      = cidade;
        this.observacao  = observacao;
    }

    // ── Serializacao manual para JSON ─────────────────────────────────────────

    /**
     * Serializa o evento em uma String JSON.
     * Utilizado pelo Producer antes de publicar no Kafka.
     */
    public String toJson() {
        return String.format(
            "{\"encomendaId\":\"%s\",\"status\":\"%s\",\"timestamp\":\"%s\"," +
            "\"cidade\":\"%s\",\"observacao\":\"%s\"}",
            encomendaId, status, timestamp, cidade, observacao
        );
    }

    /**
     * Desserializa um JSON simples para um EventoEncomenda.
     * Utilizado pelo Consumer apos receber o Value do Kafka.
     */
    public static EventoEncomenda fromJson(String json) {
        EventoEncomenda e = new EventoEncomenda();
        e.encomendaId = extrair(json, "encomendaId");
        e.status      = extrair(json, "status");
        e.timestamp   = extrair(json, "timestamp");
        e.cidade      = extrair(json, "cidade");
        e.observacao  = extrair(json, "observacao");
        return e;
    }

    private static String extrair(String json, String campo) {
        String chave = "\"" + campo + "\":\"";
        int inicio   = json.indexOf(chave);
        if (inicio == -1) return "";
        inicio += chave.length();
        int fim = json.indexOf("\"", inicio);
        return fim == -1 ? "" : json.substring(inicio, fim);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getEncomendaId() { return encomendaId; }
    public String getStatus()      { return status; }
    public String getTimestamp()   { return timestamp; }
    public String getCidade()      { return cidade; }
    public String getObservacao()  { return observacao; }

    @Override
    public String toString() {
        return String.format("[%s] %s -> %s (%s)", timestamp, encomendaId, status, cidade);
    }
}
