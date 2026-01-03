package org.example.aegon.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class PresenceService {

    private static final int TTL_SECONDS = 30;

    private final StringRedisTemplate redisTemplate;

    public PresenceService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void markOnline(String userId, String deviceId, String serverId) {
        redisTemplate.opsForValue()
                .set(key(userId, deviceId), serverId, TTL_SECONDS, TimeUnit.SECONDS);
    }

    public Map<String, String> getUserDevices(String userId) {
        Set<String> keys = redisTemplate.keys("presence:" + userId + ":*");
        Map<String, String> result = new HashMap<>();

        if (keys != null) {
            for (String key : keys) {
                String deviceId = key.substring(key.lastIndexOf(":") + 1);
                String serverId = redisTemplate.opsForValue().get(key);
                result.put(deviceId, serverId);
            }
        }
        return result;
    }
    public void refresh(String userId, String deviceId) {
        redisTemplate.expire(key(userId, deviceId), TTL_SECONDS, TimeUnit.SECONDS);
    }

    private String key(String userId, String deviceId) {
        return "presence:" + userId + ":" + deviceId;
    }
}
