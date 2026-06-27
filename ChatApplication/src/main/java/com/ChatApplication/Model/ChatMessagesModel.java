package com.ChatApplication.Model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDateTime;

@Document(collection = "chats_messages")
public class ChatMessagesModel {
    @Id
    private String id;
    private String chatId;
    private String userId;
    private String message;
    private String sessionId;
    private String role;

    @CreatedDate
    private Instant timestamp;

    // no-args constructor required by some frameworks / for deserialization
    public ChatMessagesModel() {
    }

    // full-args constructor
    public ChatMessagesModel(String id, String chatId, String userId, String message, Instant timestamp, String sessionId, String role) {
        this.id = id;
        this.chatId = chatId;
        this.userId = userId;
        this.message = message;
        this.timestamp = timestamp;
        this.sessionId = sessionId;
        this.role = role;
    }

    // getters
    public String getId() {
        return id;
    }

    public String getChatId() {
        return chatId;
    }

    public String getUserId() {
        return userId;
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getSessionId() {
        return sessionId;
    }
    public String getRole() {
        return role;
    }

    // setters
    public void setId(String id) {
        this.id = id;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setTimestamp(Instant  timestamp) {
        this.timestamp = timestamp;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    public void setRole(String role) {
        this.role = role;
    }

}
