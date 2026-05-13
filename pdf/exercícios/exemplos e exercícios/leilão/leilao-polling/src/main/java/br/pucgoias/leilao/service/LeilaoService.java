package br.pucgoias.leilao.service;

import br.pucgoias.leilao.model.Lance;
import br.pucgoias.leilao.model.LeilaoState;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class LeilaoService {

    private final LeilaoState estado = new LeilaoState();
    private final AtomicLong totalRequisicoes = new AtomicLong(0);

    // Duração total do leilão em segundos (3 minutos para demonstração)
    private static final long DURACAO_LEILAO = 180;

    public LeilaoService() {
        reiniciarLeilao();
    }

    /** Reinicia o leilão com um novo item */
    public void reiniciarLeilao() {
        estado.setItem("Obra de Arte: \"O Sonho Digital\"");
        estado.setDescricao("Pintura a óleo sobre tela (120x90cm). Artista: Ana Lima. Ano: 2024.");
        estado.setImagemUrl("https://picsum.photos/seed/arte42/600/400");
        estado.setLanceMinimo(500.00);
        estado.setLanceAtual(500.00);
        estado.setMaiorLancador("Nenhum lance ainda");
        estado.setTempoRestante(DURACAO_LEILAO);
        estado.setAtivo(true);
        estado.setHistorico(new ArrayList<>());
        totalRequisicoes.set(0);
        System.out.println("[LEILÃO] Novo leilão iniciado!");
    }

    /** Chamado a cada segundo pelo @Scheduled para fazer a contagem regressiva */
    @Scheduled(fixedDelay = 1000)
    public void decrementarTempo() {
        if (!estado.isAtivo()) return;

        long tempo = estado.getTempoRestante();
        if (tempo > 0) {
            estado.setTempoRestante(tempo - 1);
        } else {
            estado.setAtivo(false);
            System.out.println("[LEILÃO] Encerrado! Vencedor: " + estado.getMaiorLancador()
                    + " | Lance: R$ " + estado.getLanceAtual());
            System.out.println("[LEILÃO] Total de requisições HTTP recebidas (polling): "
                    + totalRequisicoes.get());
        }
    }

    /**
     * Retorna o estado atual do leilão e incrementa o contador de requisições.
     * Esse contador deixa evidente o custo do polling durante a demonstração.
     */
    public LeilaoState getEstado() {
        long total = totalRequisicoes.incrementAndGet();
        estado.setTotalRequisicoes(total);
        return estado;
    }

    /**
     * Registra um novo lance.
     * Retorna true se o lance foi aceito, false caso contrário.
     */
    public boolean fazerLance(Lance lance) {
        if (!estado.isAtivo()) return false;

        // Lance deve ser maior que o atual + incremento mínimo de R$ 10
        double minimoAceito = estado.getLanceAtual() + 10.0;
        if (lance.getValor() < minimoAceito) return false;

        estado.setLanceAtual(lance.getValor());
        estado.setMaiorLancador(lance.getUsuario());

        // Adiciona ao histórico (máximo 10 últimos lances visíveis)
        estado.getHistorico().add(0, lance);
        if (estado.getHistorico().size() > 10) {
            estado.getHistorico().remove(estado.getHistorico().size() - 1);
        }

        System.out.println("[LANCE] " + lance.getUsuario()
                + " deu lance de R$ " + lance.getValor());
        return true;
    }
}
