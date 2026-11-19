package com.unigram.exception;

/**
 * Thrown when a user attempts to access or send a message to a conversation
 * in which they are neither the registered buyer nor the seller.
 */
public class UnauthorizedConversationAccessException extends UniGramException {

    private final String userId;
    private final String conversationId;

    public UnauthorizedConversationAccessException(String userId, String conversationId) {
        super("User '" + userId + "' is not authorized to access conversation '" + conversationId + "'.");
        this.userId = userId;
        this.conversationId = conversationId;
    }

    public UnauthorizedConversationAccessException(String userId, String conversationId, String message) {
        super(message);
        this.userId = userId;
        this.conversationId = conversationId;
    }

    public UnauthorizedConversationAccessException(String userId, String conversationId, String message, Throwable cause) {
        super(message, cause);
        this.userId = userId;
        this.conversationId = conversationId;
    }

    public String getUserId() {
        return userId;
    }

    public String getConversationId() {
        return conversationId;
    }
}
