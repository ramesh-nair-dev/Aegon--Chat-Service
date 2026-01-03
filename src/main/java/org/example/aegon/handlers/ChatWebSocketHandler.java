package org.example.aegon.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.aegon.models.ChatMessage;
import org.example.aegon.models.Message;
import org.example.aegon.publishers.ServerMessagePublisher;
import org.example.aegon.repository.MessageRepository;
import org.example.aegon.service.PresenceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    /**
     * userId -> (deviceId -> WebSocketSession)
     */
    private final Map<String, Map<String, WebSocketSession>> sessions =
            new ConcurrentHashMap<>();

    private final MessageRepository messageRepository;
    private final PresenceService presenceService;
    private final ServerMessagePublisher serverMessagePublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${server.id}")
    private String serverId;

    public ChatWebSocketHandler(MessageRepository messageRepository,
                                PresenceService presenceService,
                                ServerMessagePublisher serverMessagePublisher) {
        this.messageRepository = messageRepository;
        this.presenceService = presenceService;
        this.serverMessagePublisher = serverMessagePublisher;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage)
            throws Exception {

        JsonNode root = objectMapper.readTree(textMessage.getPayload());
        String type = root.get("type").asText();


        switch (type) {
            case "REGISTER" -> handleRegister(root, session);
            case "MESSAGE" -> handleMessage(root, session);
            case "DELIVERY_ACK" -> handleDeliveryAck(root);
            case "READ_ACK" -> handleReadAck(root);
        }
    }

    // ---------- REGISTER ----------

    private void handleRegister(JsonNode root, WebSocketSession session) throws IOException {
        String userId = root.get("userId").asText();
        String deviceId = root.get("deviceId").asText();

        sessions
                .computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                .put(deviceId, session);

        presenceService.markOnline(userId, deviceId, serverId);

        replayUndeliveredMessages(userId, session);
    }

    // ---------- MESSAGE ----------

    private void handleMessage(JsonNode root, WebSocketSession senderSession)
            throws IOException {

        ChatMessage chatMessage =
                objectMapper.treeToValue(root.get("data"), ChatMessage.class);

        String senderId = chatMessage.getSenderId();
        String receiverId = chatMessage.getReceiverId();

        // 1️⃣ Durable write
        Message saved = messageRepository.save(
                new Message(senderId, receiverId, chatMessage.getContent())
        );

        // 2️⃣ ACK sender
        ObjectNode ack = objectMapper.createObjectNode();
        ack.put("type", "SENT_ACK");
        ack.put("messageId", saved.getId());

        senderSession.sendMessage(
                new TextMessage(objectMapper.writeValueAsString(ack))
        );

        // 3️⃣ Route via Redis presence
        Map<String, String> devices =
                presenceService.getUserDevices(receiverId);

        for (String targetServer : devices.values()) {
            if (serverId.equals(targetServer)) {
                deliverToLocalDevices(saved);
            } else {
                serverMessagePublisher.publish(targetServer, saved.getId());
            }
        }


    }

   // ---------- DELIVERY ACK ----------

    private void handleDeliveryAck(JsonNode root) {
        Long messageId = root.get("messageId").asLong();

        messageRepository.findById(messageId).ifPresent(msg -> {
            if (msg.getDeliveredAt() == null) {
                msg.markDelivered(LocalDateTime.now());
                messageRepository.save(msg);
            }
        });
    }

    // ---------- READ ACK ----------

    private void handleReadAck(JsonNode root) {
        Long messageId = root.get("messageId").asLong();

        messageRepository.findById(messageId).ifPresent(msg -> {
            if (msg.getDeliveredAt() != null && msg.getReadAt() == null) {
                msg.markRead(LocalDateTime.now());
                messageRepository.save(msg);
            }
        });
    }




    // ---------- REPLAY ----------

    private void replayUndeliveredMessages(String userId, WebSocketSession session)
            throws IOException {

        List<Message> pending =
                messageRepository
                        .findByReceiverIdAndDeliveredAtIsNullOrderByCreatedAt(userId);

        for (Message msg : pending) {
            ObjectNode delivery = objectMapper.createObjectNode();
            delivery.put("type", "DELIVER");
            delivery.set("data", objectMapper.valueToTree(msg));

            session.sendMessage(
                    new TextMessage(objectMapper.writeValueAsString(delivery))
            );

        }
    }

    // ---------- LOCAL DELIVERY ----------

    public void deliverToLocalDevices(Message msg) throws IOException {

        Map<String, WebSocketSession> deviceSessions =
                sessions.get(msg.getReceiverId());

        if (deviceSessions == null) return;

        ObjectNode delivery = objectMapper.createObjectNode();
        delivery.put("type", "DELIVER");
        delivery.set("data", objectMapper.valueToTree(msg));

        for (WebSocketSession ws : deviceSessions.values()) {
            if (ws.isOpen()) {
                ws.sendMessage(
                        new TextMessage(objectMapper.writeValueAsString(delivery))
                );
            }
        }
    }

    // ---------- DISCONNECT ----------

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.values()
                .forEach(deviceMap -> deviceMap.values().remove(session));
    }
}
