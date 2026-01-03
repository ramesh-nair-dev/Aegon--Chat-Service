package org.example.aegon.redis;

import org.example.aegon.handlers.ChatWebSocketHandler;
import org.example.aegon.models.Message;
import org.example.aegon.repository.MessageRepository;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class ServerMessageSubscriber implements MessageListener {

    private final MessageRepository messageRepository;
    private final ChatWebSocketHandler chatWebSocketHandler;

    public ServerMessageSubscriber(MessageRepository messageRepository,
                                   ChatWebSocketHandler chatWebSocketHandler) {
        this.messageRepository = messageRepository;
        this.chatWebSocketHandler = chatWebSocketHandler;
    }

    @Override
    public void onMessage(org.springframework.data.redis.connection.Message message,
                          byte[] pattern) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(message.getBody());

            if (!"SERVER_DELIVER".equals(root.get("type").asText())) {
                return;
            }

            Long messageId = root.get("messageId").asLong();

            Message msg = messageRepository.findById(messageId).orElse(null);
            if (msg == null) return;

            chatWebSocketHandler.deliverToLocalDevices(msg);

        } catch (Exception e) {
            e.printStackTrace(); // log properly in real systems
        }
    }
}
