package com.unigram.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a single message sent within a conversation.
 */
public class Message {

    private final String messageId;
    private final String conversationId;
    private final String senderId;
    private final String content;
    private final LocalDateTime sentAt;

    public Message(String messageId, String conversationId, String senderId, String content, LocalDateTime sentAt) {
        if (messageId == null || messageId.trim().isEmpty()) {
            throw new IllegalArgumentException("Message ID cannot be null or empty.");
        }
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Conversation ID cannot be null or empty.");
        }
        if (senderId == null || senderId.trim().isEmpty()) {
            throw new IllegalArgumentException("Sender ID cannot be null or empty.");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be null or empty.");
        }

        this.messageId = messageId.trim();
        this.conversationId = conversationId.trim();
        this.senderId = senderId.trim();
        this.content = content.trim();
        this.sentAt = sentAt != null ? sentAt : LocalDateTime.now();
    }

    public Message(String messageId, String conversationId, String senderId, String content) {
        this(messageId, conversationId, senderId, content, LocalDateTime.now());
    }

    public String getMessageId() {
        return messageId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Message other)) return false;
        return Objects.equals(messageId, other.messageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId);
    }

    @Override
    public String toString() {
        return "Message{messageId='" + messageId + "', senderId='" + senderId + "', sentAt=" + sentAt + "}";
    }
}
