package br.pucgoias.leilao.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuração do WebSocket com STOMP (Simple Text Oriented Messaging Protocol).
 *
 * Conceitos importantes para apresentar em sala:
 *
 * 1. STOMP é um protocolo de mensagens que roda SOBRE o WebSocket.
 *    WebSocket é o transporte; STOMP é o protocolo de mensagens.
 *
 * 2. Message Broker: responsável por rotear mensagens entre clientes e servidor.
 *    Usamos o broker embutido do Spring (SimpleBroker) para fins didáticos.
 *    Em produção, usaríamos RabbitMQ ou ActiveMQ como broker externo.
 *
 * 3. Prefixos:
 *    - "/app"    → mensagens destinadas ao servidor (métodos @MessageMapping)
 *    - "/topic"  → tópicos públicos — o broker entrega a TODOS os inscritos
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Ativa o broker embutido para o prefixo /topic
        // Tudo publicado em /topic/* será entregue a todos os subscribers
        registry.enableSimpleBroker("/topic");

        // Mensagens enviadas pelo cliente com prefixo /app são roteadas
        // para métodos @MessageMapping no servidor
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint de handshake WebSocket.
        // withSockJS() adiciona fallback para navegadores que não suportam WS nativamente.
        registry.addEndpoint("/ws-leilao")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
