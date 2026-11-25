package com.unigram.exception;

/**
 * Thrown when an operation references a conversation ID that does not exist.
 */
public class ConversationNotFoundException extends UniGramException {

    public ConversationNotFoundException(String conversationId) {
        super("Conversation not found with ID: " + conversationId);
    }
}
