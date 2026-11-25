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
 * Service managing message sending, participant authorization, and
 * chronological message history backed by MySQL transactions.
 */
public class JdbcMessageService {

    private final DatabaseConfig config;
    private final JdbcChatService chatService;
    private final MessageJdbcDao messageDao;

    public JdbcMessageService(DatabaseConfig config, JdbcChatService chatService) {
        if (config == null) {
            throw new IllegalArgumentException("DatabaseConfig cannot be null.");
        }
        if (chatService == null) {
            throw new IllegalArgumentException("JdbcChatService cannot be null.");
        }
        this.config = config;
        this.chatService = chatService;
        this.messageDao = new MessageJdbcDao();
    }

    /**
     * Sends a message within an existing conversation using an explicit transaction.
     */
    public Message sendMessage(String conversationId, String senderId, String content)
            throws ConversationNotFoundException, UnauthorizedConversationAccessException,
                   InvalidMessageException, DatabaseException {

        if (content == null || content.trim().isEmpty()) {
            throw new InvalidMessageException("Message content cannot be empty.");
        }

        chatService.validateParticipant(conversationId, senderId);

        String cleanConvId = conversationId.trim();
        String cleanSenderId = senderId.trim();
        String cleanContent = content.trim();

        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            conn.setAutoCommit(false);
            try {
                String messageId = "msg_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
                Message message = new Message(messageId, cleanConvId, cleanSenderId, cleanContent, LocalDateTime.now());
                messageDao.insert(conn, message);
                conn.commit();
                return message;
            } catch (SQLException e) {
                conn.rollback();
                throw new DatabaseException("Failed to send message: " + e.getMessage(), e);
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

    public List<Message> getMessageHistory(String conversationId, String requestingStudentId)
            throws ConversationNotFoundException, UnauthorizedConversationAccessException, DatabaseException {
        chatService.validateParticipant(conversationId, requestingStudentId);
        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            return Collections.unmodifiableList(messageDao.findByConversationId(conn, conversationId.trim()));
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve message history: " + e.getMessage(), e);
        }
    }

    public Optional<Message> getLatestMessage(String conversationId, String requestingStudentId)
            throws ConversationNotFoundException, UnauthorizedConversationAccessException, DatabaseException {
        chatService.validateParticipant(conversationId, requestingStudentId);
        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            return messageDao.findLatestByConversationId(conn, conversationId.trim());
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve latest message: " + e.getMessage(), e);
        }
    }

    public int getMessageCount(String conversationId) throws DatabaseException {
        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            return messageDao.countByConversationId(conn, conversationId.trim());
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count messages: " + e.getMessage(), e);
        }
    }
}
