package com.unigram.exception;

/**
 * Thrown when an operation references a conversation that does not exist in the system.
 */
public class ConversationNotFoundException extends UniGramException {

    private final String conversationId;

    public ConversationNotFoundException(String conversationId) {
        super("Conversation not found with ID: " + conversationId);
        this.conversationId = conversationId;
    }

    public ConversationNotFoundException(String conversationId, String message) {
        super(message);
        this.conversationId = conversationId;
    }

    public ConversationNotFoundException(String conversationId, String message, Throwable cause) {
        super(message, cause);
        this.conversationId = conversationId;
    }

    public String getConversationId() {
        return conversationId;
    }
}
