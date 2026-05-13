package br.pucgoias.leilao.controller;

import br.pucgoias.leilao.model.Lance;
import br.pucgoias.leilao.model.LeilaoState;
import br.pucgoias.leilao.service.LeilaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/leilao")
@CrossOrigin("*")
public class LeilaoController {

    private final LeilaoService leilaoService;

    public LeilaoController(LeilaoService leilaoService) {
        this.leilaoService = leilaoService;
    }

    /**
     * Endpoint de polling: o cliente chama esse endpoint repetidamente
     * (ex: a cada 2 segundos) para verificar se houve novos lances.
     *
     * PROBLEMA: o servidor responde com os mesmos dados na maioria das vezes,
     * desperdiçando banda e processamento.
     */
    @GetMapping
    public LeilaoState getEstado() {
        return leilaoService.getEstado();
    }

    /**
     * Endpoint para fazer um lance.
     */
    @PostMapping("/lance")
    public ResponseEntity<Map<String, Object>> fazerLance(@RequestBody Lance lance) {
        boolean aceito = leilaoService.fazerLance(lance);

        if (aceito) {
            return ResponseEntity.ok(Map.of(
                "sucesso", true,
                "mensagem", "Lance de R$ " + lance.getValor() + " aceito!"
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                "sucesso", false,
                "mensagem", "Lance recusado. Verifique o valor mínimo ou se o leilão está ativo."
            ));
        }
    }

    /**
     * Reinicia o leilão (útil para demonstrações em sala).
     */
    @PostMapping("/reiniciar")
    public ResponseEntity<Map<String, String>> reiniciar() {
        leilaoService.reiniciarLeilao();
        return ResponseEntity.ok(Map.of("mensagem", "Leilão reiniciado com sucesso!"));
    }
}
