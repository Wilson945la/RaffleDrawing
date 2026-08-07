package com.caohua.raffle.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class RaffleWebSocketHandler extends TextWebSocketHandler {

    private final CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
    }

    public void broadcastNewWinner(String userName, String prizeName) {
        String message = String.format(
            "{\"type\":\"new_winner\",\"userName\":\"%s\",\"prizeName\":\"%s\",\"time\":%d}",
            escapeJson(userName), escapeJson(prizeName), System.currentTimeMillis()
        );
        broadcast(message);
    }

    public void broadcastEventUpdate(boolean active, String title) {
        String message = String.format(
            "{\"type\":\"event_update\",\"active\":%b,\"title\":\"%s\"}",
            active, escapeJson(title)
        );
        broadcast(message);
    }

    public void broadcastResultChanged(String action, Long resultId) {
        String message = String.format(
            "{\"type\":\"result_changed\",\"action\":\"%s\",\"resultId\":%d}",
            escapeJson(action), resultId
        );
        broadcast(message);
    }

    public void broadcast(String message) {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    // Ignore send errors
                }
            }
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
