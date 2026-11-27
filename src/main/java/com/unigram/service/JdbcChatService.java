package com.unigram.service;

import com.unigram.database.DatabaseConfig;
import com.unigram.database.DatabaseConnectionFactory;
import com.unigram.database.DatabaseException;
import com.unigram.exception.ConversationNotFoundException;
import com.unigram.exception.UnauthorizedConversationAccessException;
import com.unigram.exception.UniGramException;
import com.unigram.exception.UserNotFoundException;
import com.unigram.model.Conversation;
import com.unigram.model.User;
import com.unigram.repository.ConversationJdbcDao;
import com.unigram.repository.UserJdbcDao;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service managing student registration, authentication, and direct
 * one-to-one conversations backed by MySQL via JDBC transactions.
 */
public class JdbcChatService {

    private final DatabaseConfig config;
    private final UserJdbcDao userDao;
    private final ConversationJdbcDao conversationDao;

    public JdbcChatService(DatabaseConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("DatabaseConfig cannot be null.");
        }
        this.config = config;
        this.userDao = new UserJdbcDao();
        this.conversationDao = new ConversationJdbcDao();
    }

    /**
     * Registers a new student with a salted SHA-256 password hash,
     * rejecting duplicate student IDs or emails.
     */
    public User registerUser(String studentId, String fullName, String email, String rawPassword)
            throws DatabaseException, UniGramException {
        if (rawPassword == null || rawPassword.trim().length() < 4) {
            throw new IllegalArgumentException("Password must be at least 4 characters long.");
        }
        String hash = PasswordUtil.hashPassword(rawPassword);
        User user = new User(studentId, fullName, email, hash);

        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            if (userDao.existsById(conn, user.getUserId())) {
                throw new UniGramException("A student is already registered with ID: " + user.getUserId());
            }
            if (userDao.existsByEmail(conn, user.getEmail())) {
                throw new UniGramException("A student is already registered with email: " + user.getEmail());
            }
            userDao.insert(conn, user);
            return user;
        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState())) {
                throw new UniGramException("Duplicate student ID or email is not allowed.");
            }
            throw new DatabaseException("Failed to register student: " + e.getMessage(), e);
        }
    }

    /**
     * Authenticates a student by ID and password.
     * Demo accounts seeded without a password hash default to "student123" on first login.
     */
    public User authenticate(String studentId, String rawPassword)
            throws DatabaseException, UniGramException {
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID cannot be empty.");
        }
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }

        String cleanId = studentId.trim();
        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            Optional<User> found = userDao.findById(conn, cleanId);
            if (found.isEmpty()) {
                throw new UniGramException("Invalid student ID or password.");
            }
            User user = found.get();
            String storedHash = user.getPasswordHash();

            // Initialize salted hash on first login for SQL-seeded demo accounts
            if (storedHash == null || storedHash.isEmpty()) {
                if ("student123".equals(rawPassword)) {
                    String newHash = PasswordUtil.hashPassword(rawPassword);
                    userDao.updatePasswordHash(conn, user.getUserId(), newHash);
                    user.setPasswordHash(newHash);
                    return user;
                }
                throw new UniGramException("Invalid student ID or password.");
            }

            if (!PasswordUtil.verifyPassword(rawPassword, storedHash)) {
                throw new UniGramException("Invalid student ID or password.");
            }
            return user;
        } catch (SQLException e) {
            throw new DatabaseException("Authentication failed due to database error: " + e.getMessage(), e);
        }
    }

    public User getUser(String studentId) throws UserNotFoundException, DatabaseException {
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new UserNotFoundException(studentId != null ? studentId : "null");
        }
        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            Optional<User> found = userDao.findById(conn, studentId.trim());
            if (found.isEmpty()) {
                throw new UserNotFoundException(studentId.trim());
            }
            return found.get();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve student: " + e.getMessage(), e);
        }
    }

    public List<User> getAllUsers() throws DatabaseException {
        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            return userDao.findAll(conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list registered students: " + e.getMessage(), e);
        }
    }

    /**
     * Starts a direct conversation between two students, or returns the existing
     * conversation if one already exists in either direction.
     */
    public Conversation startOrGetConversation(String initiatorId, String recipientId)
            throws UserNotFoundException, UniGramException, DatabaseException {

        if (initiatorId == null || initiatorId.trim().isEmpty()) {
            throw new IllegalArgumentException("Your student ID cannot be empty.");
        }
        if (recipientId == null || recipientId.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient student ID cannot be empty.");
        }

        String cleanInitiator = initiatorId.trim();
        String cleanRecipient = recipientId.trim();

        if (cleanInitiator.equalsIgnoreCase(cleanRecipient)) {
            throw new IllegalArgumentException("A student cannot start a conversation with themselves.");
        }

        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            conn.setAutoCommit(false);
            try {
                if (!userDao.existsById(conn, cleanInitiator)) {
                    throw new UserNotFoundException(cleanInitiator);
                }
                if (!userDao.existsById(conn, cleanRecipient)) {
                    throw new UserNotFoundException(cleanRecipient);
                }

                Optional<Conversation> existing = conversationDao.findByParticipants(conn, cleanInitiator, cleanRecipient);
                if (existing.isPresent()) {
                    conn.commit();
                    return existing.get();
                }

                String conversationId = conversationDao.generateNextConversationId(conn);
                Conversation conversation = new Conversation(conversationId, cleanInitiator, cleanRecipient, LocalDateTime.now());
                conversationDao.insert(conn, conversation);
                conn.commit();
                return conversation;

            } catch (SQLException e) {
                conn.rollback();
                if ("23000".equals(e.getSQLState())) {
                    Optional<Conversation> concurrentExisting =
                            conversationDao.findByParticipants(conn, cleanInitiator, cleanRecipient);
                    if (concurrentExisting.isPresent()) {
                        return concurrentExisting.get();
                    }
                    throw new UniGramException("Could not create conversation due to duplicate constraint.");
                }
                throw new DatabaseException("Failed to create conversation: " + e.getMessage(), e);
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

    public Conversation getConversationById(String conversationId)
            throws ConversationNotFoundException, DatabaseException {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new ConversationNotFoundException(conversationId != null ? conversationId : "null");
        }
        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            Optional<Conversation> found = conversationDao.findById(conn, conversationId.trim());
            if (found.isEmpty()) {
                throw new ConversationNotFoundException(conversationId.trim());
            }
            return found.get();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve conversation: " + e.getMessage(), e);
        }
    }

    public List<Conversation> getConversationsForUser(String studentId)
            throws UserNotFoundException, DatabaseException {
        getUser(studentId);
        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            return conversationDao.findByParticipant(conn, studentId.trim());
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve conversations: " + e.getMessage(), e);
        }
    }

    public void validateParticipant(String conversationId, String studentId)
            throws ConversationNotFoundException, UnauthorizedConversationAccessException, DatabaseException {
        Conversation conv = getConversationById(conversationId);
        if (!conv.isParticipant(studentId)) {
            throw new UnauthorizedConversationAccessException(studentId, conversationId);
        }
    }
}
