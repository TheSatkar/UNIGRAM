package com.unigram.exception;

/**
 * Thrown when a student tries to view or message a conversation they do not belong to.
 */
public class UnauthorizedConversationAccessException extends UniGramException {

    public UnauthorizedConversationAccessException(String userId, String conversationId) {
        super("Student '" + userId + "' is not a participant in conversation '" + conversationId + "'.");
    }
}
