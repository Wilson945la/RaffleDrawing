package com.caohua.raffle.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import com.caohua.raffle.websocket.RaffleWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final RaffleWebSocketHandler raffleWebSocketHandler;

    public WebSocketConfig(RaffleWebSocketHandler raffleWebSocketHandler) {
        this.raffleWebSocketHandler = raffleWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(raffleWebSocketHandler, "/ws/raffle")
                .setAllowedOrigins("*");
    }
}
