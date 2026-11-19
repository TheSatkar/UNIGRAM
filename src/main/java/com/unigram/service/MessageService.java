package com.unigram.service;

import com.unigram.collection.InMemoryStorage;
import com.unigram.exception.ConversationNotFoundException;
import com.unigram.exception.InvalidMessageException;
import com.unigram.exception.UnauthorizedConversationAccessException;
import com.unigram.model.Message;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service managing message dispatching, validation, chronological sequencing,
 * and conversation history retrieval.
 */
public class MessageService {

    private final InMemoryStorage storage;
    private final ChatService chatService;

    // Standard comparator ordering messages chronologically (oldest first)
    public static final Comparator<Message> CHRONOLOGICAL_COMPARATOR = Comparator.comparing(Message::getSentAt);

    public MessageService(InMemoryStorage storage, ChatService chatService) {
        if (storage == null) {
            throw new IllegalArgumentException("InMemoryStorage cannot be null.");
        }
        if (chatService == null) {
            throw new IllegalArgumentException("ChatService cannot be null.");
        }
        this.storage = storage;
        this.chatService = chatService;
    }

    /**
     * Sends a message to an existing conversation.
     *
     * Validates:
     * 1. Message content must not be blank.
     * 2. Conversation must exist.
     * 3. Sender must be an authorized participant in the conversation.
     *
     * @param conversationId target conversation ID
     * @param senderId       ID of user sending the message
     * @param content        text content of the message
     * @return the dispatched Message entity
     * @throws ConversationNotFoundException            if target conversation does not exist
     * @throws UnauthorizedConversationAccessException if sender is not a participant
     * @throws InvalidMessageException                  if content is empty or blank
     */
    public Message sendMessage(String conversationId, String senderId, String content)
            throws ConversationNotFoundException, UnauthorizedConversationAccessException, InvalidMessageException {

        // 1. Content validation
        if (content == null || content.trim().isEmpty()) {
            throw new InvalidMessageException("Message content cannot be null or blank.");
        }

        // 2 & 3. Conversation existence and participant authorization validation
        this.chatService.validateParticipant(conversationId, senderId);

        // 4. Generate unique message identifier
        String messageId = "msg_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // 5. Create immutable Message entity
        Message message = new Message(messageId, conversationId, senderId, content.trim(), LocalDateTime.now());

        // 6. Persist to in-memory store
        this.storage.addMessage(message);

        return message;
    }

    /**
     * Retrieves the complete message history for a conversation, sorted in chronological order.
     * Access is restricted to authorized conversation participants.
     *
     * @param conversationId   target conversation ID
     * @param requestingUserId ID of the user requesting the history
     * @return List of messages in chronological order
     * @throws ConversationNotFoundException            if conversation does not exist
     * @throws UnauthorizedConversationAccessException if user is not a participant
     */
    public List<Message> getMessageHistory(String conversationId, String requestingUserId)
            throws ConversationNotFoundException, UnauthorizedConversationAccessException {

        // Validate requester authorization
        this.chatService.validateParticipant(conversationId, requestingUserId);

        // Retrieve messages
        List<Message> messages = this.storage.getMessagesByConversationId(conversationId);

        // Sort chronologically using Collections.sort and Comparator
        messages.sort(CHRONOLOGICAL_COMPARATOR);

        return Collections.unmodifiableList(messages);
    }

    /**
     * Retrieves the latest message sent in a conversation.
     *
     * @param conversationId   target conversation ID
     * @param requestingUserId ID of the user requesting the latest message
     * @return Optional containing the latest Message if present, or empty Optional
     * @throws ConversationNotFoundException            if conversation does not exist
     * @throws UnauthorizedConversationAccessException if requesting user is not a participant
     */
    public Optional<Message> getLatestMessage(String conversationId, String requestingUserId)
            throws ConversationNotFoundException, UnauthorizedConversationAccessException {

        List<Message> history = getMessageHistory(conversationId, requestingUserId);
        if (history.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(history.get(history.size() - 1));
    }

    /**
     * Returns the total count of messages in a conversation.
     *
     * @param conversationId target conversation ID
     * @return number of messages
     */
    public int getMessageCount(String conversationId) {
        return this.storage.getMessagesByConversationId(conversationId).size();
    }
}
