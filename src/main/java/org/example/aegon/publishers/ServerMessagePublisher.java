package org.example.aegon.publishers;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Service
public class ServerMessagePublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ServerMessagePublisher(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void publish(String targetServerId, Long messageId) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("type", "SERVER_DELIVER");
        payload.put("messageId", messageId);

        redisTemplate.convertAndSend(
                "server:" + targetServerId,
                payload.toString()
        );
    }
}
