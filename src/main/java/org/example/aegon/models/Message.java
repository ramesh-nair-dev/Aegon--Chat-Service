package org.example.aegon.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
public class Message extends BaseClass {

    @Column(nullable = false)
    private String senderId;

    @Column(nullable = false)
    private String receiverId;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column
    private LocalDateTime deliveredAt;

    protected Message() {
        // JPA only
    }

    public Message(String senderId, String receiverId, String content) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public String getContent() {
        return content;
    }

    public void markDelivered(LocalDateTime time) {
        this.deliveredAt = time;
    }
}
