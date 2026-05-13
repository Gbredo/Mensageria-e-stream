package br.pucgoias.leilao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LeilaoPollingApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeilaoPollingApplication.class, args);
        System.out.println("\n╔══════════════════════════════════════════╗");
        System.out.println("║   LEILÃO - HTTP POLLING (SEM WEBSOCKET)  ║");
        System.out.println("║   Acesse: http://localhost:8080           ║");
        System.out.println("╚══════════════════════════════════════════╝\n");
    }
}
