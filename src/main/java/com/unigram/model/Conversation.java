package com.unigram.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a direct one-to-one conversation between two students.
 */
public class Conversation {

    private final String conversationId;
    private final String participantOneId;
    private final String participantTwoId;
    private final LocalDateTime createdAt;

    public Conversation(String conversationId, String participantOneId, String participantTwoId, LocalDateTime createdAt) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Conversation ID cannot be null or empty.");
        }
        if (participantOneId == null || participantOneId.trim().isEmpty()) {
            throw new IllegalArgumentException("Initiating student ID cannot be null or empty.");
        }
        if (participantTwoId == null || participantTwoId.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient student ID cannot be null or empty.");
        }
        if (participantOneId.trim().equalsIgnoreCase(participantTwoId.trim())) {
            throw new IllegalArgumentException("A student cannot start a conversation with themselves.");
        }

        this.conversationId = conversationId.trim();
        this.participantOneId = participantOneId.trim();
        this.participantTwoId = participantTwoId.trim();
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public Conversation(String conversationId, String participantOneId, String participantTwoId) {
        this(conversationId, participantOneId, participantTwoId, LocalDateTime.now());
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getParticipantOneId() {
        return participantOneId;
    }

    public String getParticipantTwoId() {
        return participantTwoId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Checks whether the given student ID belongs to this conversation.
     */
    public boolean isParticipant(String studentId) {
        if (studentId == null) {
            return false;
        }
        String cleanId = studentId.trim();
        return cleanId.equalsIgnoreCase(participantOneId)
                || cleanId.equalsIgnoreCase(participantTwoId);
    }

    /**
     * Returns the ID of the other participant in this conversation.
     */
    public String getOtherParticipantId(String studentId) {
        if (studentId != null && studentId.trim().equalsIgnoreCase(participantOneId)) {
            return participantTwoId;
        }
        return participantOneId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Conversation other)) return false;
        return Objects.equals(conversationId, other.conversationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conversationId);
    }

    @Override
    public String toString() {
        return "Conversation{conversationId='" + conversationId + "', participants=["
                + participantOneId + ", " + participantTwoId + "], createdAt=" + createdAt + "}";
    }
}
