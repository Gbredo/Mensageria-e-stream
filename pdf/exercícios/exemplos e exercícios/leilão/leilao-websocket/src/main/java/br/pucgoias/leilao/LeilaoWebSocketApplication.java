package br.pucgoias.leilao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LeilaoWebSocketApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeilaoWebSocketApplication.class, args);
        System.out.println("\n╔══════════════════════════════════════════╗");
        System.out.println("║      LEILÃO - WEBSOCKET (STOMP)          ║");
        System.out.println("║   Acesse: http://localhost:8081           ║");
        System.out.println("╚══════════════════════════════════════════╝\n");
    }
}
