package br.pucgoias.leilao.controller;

import br.pucgoias.leilao.model.Lance;
import br.pucgoias.leilao.model.LeilaoState;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Controller
public class LeilaoController {

    /** SimpMessagingTemplate: envia mensagens do servidor para clientes inscritos */
    private final SimpMessagingTemplate messagingTemplate;

    private final LeilaoState estado = new LeilaoState();
    private final AtomicInteger clientesConectados = new AtomicInteger(0);
    private final AtomicLong totalMensagens = new AtomicLong(0);
    private final AtomicLong totalLances = new AtomicLong(0);

    private static final long DURACAO_LEILAO = 180; // segundos

    public LeilaoController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        reiniciarEstado();
    }

    // ─────────────────────────────────────────────────────────────────
    // CONTROLE DE SESSÕES: conta clientes conectados
    // ─────────────────────────────────────────────────────────────────

    /**
     * Disparado automaticamente quando um cliente abre a conexão WebSocket.
     * Diferente do HTTP, a conexão PERSISTE — não há nova conexão a cada segundo.
     */
    @EventListener
    public void onConnect(SessionConnectedEvent event) {
        int total = clientesConectados.incrementAndGet();
        System.out.println("[WS] Cliente conectado. Total: " + total);
        // Envia o estado atual imediatamente para o novo cliente
        broadcastEstado();
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        int total = clientesConectados.decrementAndGet();
        System.out.println("[WS] Cliente desconectado. Total: " + total);
    }

    // ─────────────────────────────────────────────────────────────────
    // RECEBIMENTO DE LANCES VIA WEBSOCKET
    // ─────────────────────────────────────────────────────────────────

    /**
     * Recebe um lance enviado pelo cliente via STOMP.
     *
     * Fluxo:
     *   Cliente → STOMP SEND "/app/lance" → este método
     *   Servidor → STOMP broadcast "/topic/leilao" → TODOS os clientes conectados
     *
     * O retorno @SendTo publica o estado atualizado no tópico,
     * que é entregue instantaneamente a todos os subscribers.
     */
    @MessageMapping("/lance")        // cliente envia para /app/lance
    @SendTo("/topic/leilao")          // servidor retorna para todos em /topic/leilao
    public LeilaoState receberLance(Lance lance) {
        if (!estado.isAtivo()) return estado;

        double minimoAceito = estado.getLanceAtual() + 10.0;
        if (lance.getValor() < minimoAceito) return estado;

        // Define o horário no servidor (mais confiável)
        lance.setHorario(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

        estado.setLanceAtual(lance.getValor());
        estado.setMaiorLancador(lance.getUsuario());
        estado.getHistorico().add(0, lance);
        if (estado.getHistorico().size() > 10) {
            estado.getHistorico().remove(estado.getHistorico().size() - 1);
        }

        long lances = totalLances.incrementAndGet();
        long msgs = totalMensagens.incrementAndGet();

        estado.setTotalLances(lances);
        estado.setTotalMensagensEnviadas(msgs);
        estado.setClientesConectados(clientesConectados.get());

        System.out.println("[LANCE] " + lance.getUsuario()
                + " → R$ " + lance.getValor()
                + " | Broadcast para " + clientesConectados.get() + " cliente(s)");

        return estado;
    }

    // ─────────────────────────────────────────────────────────────────
    // TIMER: o servidor envia atualizações a cada segundo
    //        MAS somente o tempo mudou — nenhuma requisição HTTP necessária
    // ─────────────────────────────────────────────────────────────────

    @Scheduled(fixedDelay = 1000)
    public void decrementarTempo() {
        if (!estado.isAtivo()) return;

        if (estado.getTempoRestante() > 0) {
            estado.setTempoRestante(estado.getTempoRestante() - 1);
        } else {
            estado.setAtivo(false);
            System.out.println("[LEILÃO] Encerrado! Vencedor: " + estado.getMaiorLancador());
            System.out.println("[WS] Total de mensagens enviadas pelo servidor: " + totalMensagens.get());
        }

        // Transmite o timer atualizado a cada segundo (1 msg → todos os clientes)
        broadcastEstado();
    }

    /** Envia o estado atual para todos os clientes inscritos em /topic/leilao */
    private void broadcastEstado() {
        estado.setClientesConectados(clientesConectados.get());
        estado.setTotalMensagensEnviadas(totalMensagens.incrementAndGet());
        estado.setTotalLances(totalLances.get());
        messagingTemplate.convertAndSend("/topic/leilao", estado);
    }

    // ─────────────────────────────────────────────────────────────────
    // ENDPOINT REST para reiniciar o leilão
    // ─────────────────────────────────────────────────────────────────

    @RestController
    @RequestMapping("/api")
    @CrossOrigin("*")
    class RestEndpoints {

        @PostMapping("/reiniciar")
        public Map<String, String> reiniciar() {
            reiniciarEstado();
            broadcastEstado();
            return Map.of("mensagem", "Leilão reiniciado com sucesso!");
        }
    }

    private void reiniciarEstado() {
        estado.setItem("Obra de Arte: \"O Sonho Digital\"");
        estado.setDescricao("Pintura a óleo sobre tela (120x90cm). Artista: Ana Lima. Ano: 2024.");
        estado.setImagemUrl("https://picsum.photos/seed/arte42/600/400");
        estado.setLanceMinimo(500.00);
        estado.setLanceAtual(500.00);
        estado.setMaiorLancador("Nenhum lance ainda");
        estado.setTempoRestante(DURACAO_LEILAO);
        estado.setAtivo(true);
        estado.setHistorico(new ArrayList<>());
        totalLances.set(0);
        totalMensagens.set(0);
        System.out.println("[LEILÃO] Novo leilão iniciado via WebSocket!");
    }
}
