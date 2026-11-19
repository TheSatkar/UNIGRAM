package com.unigram.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents an individual message within a conversation thread.
 * Encapsulates message identity, sender reference, text payload, and timestamp.
 */
public class Message {

    private final String messageId;
    private final String conversationId;
    private final String senderId;
    private final String content;
    private final LocalDateTime sentAt;

    /**
     * Primary constructor with explicit timestamp.
     *
     * @param messageId      unique message identifier
     * @param conversationId conversation thread ID to which this message belongs
     * @param senderId       user ID of the message sender
     * @param content        text body of the message (cannot be null or blank)
     * @param sentAt         timestamp when the message was dispatched
     */
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

    /**
     * Overloaded constructor automatically recording current timestamp.
     * Demonstrates constructor chaining using 'this(...)'.
     *
     * @param messageId      unique message identifier
     * @param conversationId conversation thread ID
     * @param senderId       user ID of the sender
     * @param content        message body
     */
    public Message(String messageId, String conversationId, String senderId, String content) {
        this(messageId, conversationId, senderId, content, LocalDateTime.now());
    }

    // Getters

    public String getMessageId() {
        return this.messageId;
    }

    public String getConversationId() {
        return this.conversationId;
    }

    public String getSenderId() {
        return this.senderId;
    }

    public String getContent() {
        return this.content;
    }

    public LocalDateTime getSentAt() {
        return this.sentAt;
    }

    // Method Overrides

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return Objects.equals(this.messageId, message.messageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.messageId);
    }

    @Override
    public String toString() {
        return "Message{" +
                "messageId='" + this.messageId + '\'' +
                ", conversationId='" + this.conversationId + '\'' +
                ", senderId='" + this.senderId + '\'' +
                ", content='" + this.content + '\'' +
                ", sentAt=" + this.sentAt +
                '}';
    }
}
