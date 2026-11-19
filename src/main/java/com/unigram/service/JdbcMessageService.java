package com.unigram.service;

import com.unigram.database.DatabaseConfig;
import com.unigram.database.DatabaseConnectionFactory;
import com.unigram.database.DatabaseException;
import com.unigram.exception.ConversationNotFoundException;
import com.unigram.exception.InvalidMessageException;
import com.unigram.exception.UnauthorizedConversationAccessException;
import com.unigram.model.Message;
import com.unigram.repository.MessageJdbcDao;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * MySQL database-backed implementation of MessageService.
 * Manages message dispatching, participant authorization, and chronological history
 * with explicit JDBC transaction boundaries ensuring commit confirmation before reporting success.
 */
public class JdbcMessageService {

    private final DatabaseConfig config;
    private final JdbcChatService chatService;
    private final MessageJdbcDao messageDao;

    public JdbcMessageService(DatabaseConfig config, JdbcChatService chatService) {
        this(config, chatService, new MessageJdbcDao());
    }

    public JdbcMessageService(DatabaseConfig config, JdbcChatService chatService, MessageJdbcDao messageDao) {
        if (config == null) {
            throw new IllegalArgumentException("DatabaseConfig cannot be null.");
        }
        if (chatService == null) {
            throw new IllegalArgumentException("JdbcChatService cannot be null.");
        }
        this.config = config;
        this.chatService = chatService;
        this.messageDao = messageDao != null ? messageDao : new MessageJdbcDao();
    }

    /**
     * Sends a message to an existing conversation in MySQL.
     * Enforces that the message is never reported as sent or successful until
     * the database transaction commits successfully.
     *
     * @param conversationId target conversation ID
     * @param senderId       student ID of the sender
     * @param content        text content of the message
     * @return the successfully persisted Message entity
     * @throws ConversationNotFoundException            if conversation does not exist
     * @throws UnauthorizedConversationAccessException if sender is not an authorized participant
     * @throws InvalidMessageException                  if content is empty or blank
     * @throws DatabaseException                        if database connection or commit fails
     */
    public Message sendMessage(String conversationId, String senderId, String content)
            throws ConversationNotFoundException, UnauthorizedConversationAccessException,
                   InvalidMessageException, DatabaseException {

        // 1. Content validation
        if (content == null || content.trim().isEmpty()) {
            throw new InvalidMessageException("Message content cannot be null or blank.");
        }

        // 2 & 3. Conversation existence and participant authorization validation
        this.chatService.validateParticipant(conversationId, senderId);

        String cleanConvId = conversationId.trim();
        String cleanSenderId = senderId.trim();
        String cleanContent = content.trim();

        // 4. Open dedicated connection and perform transactional insert
        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            conn.setAutoCommit(false);
            try {
                String messageId = "msg_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
                Message message = new Message(messageId, cleanConvId, cleanSenderId, cleanContent, LocalDateTime.now());

                messageDao.insert(conn, message);

                // Commit transaction to disk
                conn.commit();
                return message;

            } catch (SQLException e) {
                conn.rollback();
                throw new DatabaseException("Failed to persist message in transaction: " + e.getMessage(), e);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Database connection error: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves the complete chronological message history for a conversation from MySQL.
     * Access is restricted to authorized conversation participants.
     *
     * @param conversationId   target conversation ID
     * @param requestingUserId student ID requesting the history
     * @return List of messages in strict chronological order (sent_at ASC)
     * @throws ConversationNotFoundException            if conversation does not exist
     * @throws UnauthorizedConversationAccessException if user is not a participant
     * @throws DatabaseException                        if database error occurs
     */
    public List<Message> getMessageHistory(String conversationId, String requestingUserId)
            throws ConversationNotFoundException, UnauthorizedConversationAccessException, DatabaseException {

        this.chatService.validateParticipant(conversationId, requestingUserId);

        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            List<Message> history = messageDao.findByConversationId(conn, conversationId.trim());
            return Collections.unmodifiableList(history);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve message history: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves the latest message sent in a conversation from MySQL.
     *
     * @param conversationId   target conversation ID
     * @param requestingUserId student ID requesting the latest message
     * @return Optional containing latest Message if present, or empty Optional
     * @throws ConversationNotFoundException            if conversation does not exist
     * @throws UnauthorizedConversationAccessException if requesting user is not a participant
     * @throws DatabaseException                        if database error occurs
     */
    public Optional<Message> getLatestMessage(String conversationId, String requestingUserId)
            throws ConversationNotFoundException, UnauthorizedConversationAccessException, DatabaseException {

        this.chatService.validateParticipant(conversationId, requestingUserId);

        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            return messageDao.findLatestByConversationId(conn, conversationId.trim());
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve latest message: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves total message count for a conversation from MySQL.
     *
     * @param conversationId target conversation ID
     * @return number of messages
     * @throws DatabaseException if database error occurs
     */
    public int getMessageCount(String conversationId) throws DatabaseException {
        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            return messageDao.countByConversationId(conn, conversationId.trim());
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count messages: " + e.getMessage(), e);
        }
    }

    public DatabaseConfig getConfig() {
        return config;
    }

    public JdbcChatService getChatService() {
        return chatService;
    }
}
