package org.example.aegon.handlers;

import org.example.aegon.models.ChatMessage;
import org.example.aegon.models.Message;
import org.example.aegon.repository.MessageRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    // userId -> WebSocketSession
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final MessageRepository messageRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatWebSocketHandler(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // Do nothing yet
        // User must explicitly identify themselves

    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message)
            throws Exception {

        JsonNode jsonNode = objectMapper.readTree(message.getPayload());
        String type = jsonNode.get("type").asText();

        if ("REGISTER".equals(type)) {
            String userId = jsonNode.get("userId").asText();
            sessions.put(userId, session);
            return;
        }

        if ("MESSAGE".equals(type)) {
            ChatMessage chatMessage =
                    objectMapper.treeToValue(jsonNode.get("data"), ChatMessage.class);

            // 1️⃣ DURABLE WRITE (truth)
            Message saved = messageRepository.save(
                    new Message(chatMessage.getSenderId(),
                            chatMessage.getReceiverId(),
                            chatMessage.getContent())
            );

            // 2️⃣ ACK sender (sent = stored)
            session.sendMessage(new TextMessage(
                    "{\"type\":\"SENT_ACK\",\"messageId\":" + saved.getId() + "}"
            ));

            WebSocketSession receiverSession =
                    sessions.get(chatMessage.getReceiverId());

            if (receiverSession != null && receiverSession.isOpen()) {
                receiverSession.sendMessage(
                        new TextMessage(objectMapper.writeValueAsString(chatMessage))
                );
            }
            // else: DROP silently (this is intentional)
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.values().remove(session);
    }
}
