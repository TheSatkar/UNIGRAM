package com.unigram.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a one-to-one conversation between a buyer and a seller regarding a specific product listing.
 * Stores ID references to avoid early object-graph coupling.
 * Collections of messages are intentionally deferred to Phase 2.
 */
public class Conversation {

    private final String conversationId;
    private final String buyerId;
    private final String sellerId;
    private final String productId;
    private final LocalDateTime createdAt;

    /**
     * Primary constructor with explicit creation timestamp.
     *
     * @param conversationId unique conversation identifier
     * @param buyerId        unique user ID of the buyer
     * @param sellerId       unique user ID of the seller
     * @param productId      unique product/listing ID being discussed
     * @param createdAt      timestamp when the conversation was initiated
     */
    public Conversation(String conversationId, String buyerId, String sellerId, String productId, LocalDateTime createdAt) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Conversation ID cannot be null or empty.");
        }
        if (buyerId == null || buyerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Initiating student ID cannot be null or empty.");
        }
        if (sellerId == null || sellerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient student ID cannot be null or empty.");
        }
        if (buyerId.trim().equalsIgnoreCase(sellerId.trim())) {
            throw new IllegalArgumentException("A user cannot start a conversation with themselves (buyer and seller cannot be identical).");
        }

        this.conversationId = conversationId.trim();
        this.buyerId = buyerId.trim();
        this.sellerId = sellerId.trim();
        this.productId = (productId != null && !productId.trim().isEmpty()) ? productId.trim() : null;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    /**
     * Overloaded constructor for product-referenced conversations with automatic timestamp.
     */
    public Conversation(String conversationId, String buyerId, String sellerId, String productId) {
        this(conversationId, buyerId, sellerId, productId, LocalDateTime.now());
    }

    /**
     * Overloaded constructor for direct student-to-student conversations with explicit timestamp.
     */
    public Conversation(String conversationId, String participantOneId, String participantTwoId, LocalDateTime createdAt) {
        this(conversationId, participantOneId, participantTwoId, null, createdAt);
    }

    /**
     * Overloaded constructor for direct student-to-student conversations with automatic timestamp.
     */
    public Conversation(String conversationId, String participantOneId, String participantTwoId) {
        this(conversationId, participantOneId, participantTwoId, null, LocalDateTime.now());
    }

    public boolean isDirectConversation() {
        return this.productId == null;
    }

    // Getters

    public String getConversationId() {
        return this.conversationId;
    }

    public String getBuyerId() {
        return this.buyerId;
    }

    public String getSellerId() {
        return this.sellerId;
    }

    public String getProductId() {
        return this.productId;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    /**
     * Helper method to verify whether a given user is an authorized participant in this conversation.
     *
     * @param userId user identifier to check
     * @return true if user is either the buyer or the seller
     */
    public boolean isParticipant(String userId) {
        if (userId == null) return false;
        String cleanId = userId.trim();
        return cleanId.equalsIgnoreCase(this.buyerId) || cleanId.equalsIgnoreCase(this.sellerId);
    }

    // Method Overrides

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Conversation that = (Conversation) o;
        return Objects.equals(this.conversationId, that.conversationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.conversationId);
    }

    @Override
    public String toString() {
        return "Conversation{" +
                "conversationId='" + this.conversationId + '\'' +
                ", buyerId='" + this.buyerId + '\'' +
                ", sellerId='" + this.sellerId + '\'' +
                ", productId='" + this.productId + '\'' +
                ", createdAt=" + this.createdAt +
                '}';
    }
}
