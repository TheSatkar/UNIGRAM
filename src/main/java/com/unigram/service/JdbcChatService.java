package com.unigram.service;

import com.unigram.database.DatabaseConfig;
import com.unigram.database.DatabaseConnectionFactory;
import com.unigram.database.DatabaseException;
import com.unigram.exception.ConversationNotFoundException;
import com.unigram.exception.UnauthorizedConversationAccessException;
import com.unigram.exception.UniGramException;
import com.unigram.exception.UserNotFoundException;
import com.unigram.model.Conversation;
import com.unigram.model.ProductReference;
import com.unigram.model.User;
import com.unigram.repository.ConversationJdbcDao;
import com.unigram.repository.ProductReferenceJdbcDao;
import com.unigram.repository.UserJdbcDao;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * MySQL database-backed implementation of ChatService.
 * Manages user registration, marketplace listings, and conversation lifecycle
 * using standard JDBC DAOs and explicit ACID transaction boundaries.
 */
public class JdbcChatService {

    private final DatabaseConfig config;
    private final UserJdbcDao userDao;
    private final ProductReferenceJdbcDao productDao;
    private final ConversationJdbcDao conversationDao;

    public JdbcChatService() {
        this(new DatabaseConfig(), new UserJdbcDao(), new ProductReferenceJdbcDao(), new ConversationJdbcDao());
    }

    public JdbcChatService(DatabaseConfig config) {
        this(config, new UserJdbcDao(), new ProductReferenceJdbcDao(), new ConversationJdbcDao());
    }

    public JdbcChatService(DatabaseConfig config,
                           UserJdbcDao userDao,
                           ProductReferenceJdbcDao productDao,
                           ConversationJdbcDao conversationDao) {
        if (config == null) {
            throw new IllegalArgumentException("DatabaseConfig cannot be null.");
        }
        this.config = config;
        this.userDao = userDao != null ? userDao : new UserJdbcDao();
        this.productDao = productDao != null ? productDao : new ProductReferenceJdbcDao();
        this.conversationDao = conversationDao != null ? conversationDao : new ConversationJdbcDao();
    }

    /**
     * Registers a new student user in the MySQL database.
     *
     * @param user user entity to register
     * @throws DatabaseException if a database access error occurs
     * @throws UniGramException  if duplicate ID or duplicate unique email is attempted
     */
    public void registerUser(User user) throws DatabaseException, UniGramException {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null.");
        }
        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            if (userDao.existsById(conn, user.getUserId())) {
                throw new UniGramException("User already registered with ID: " + user.getUserId());
            }
            userDao.insert(conn, user);
        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState())) {
                throw new UniGramException("Database constraint violation (e.g. duplicate email/ID): " + e.getMessage());
            }
            throw new DatabaseException("Failed to register user: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a user by their unique identifier from MySQL.
     *
     * @param userId user identifier
     * @return User entity
     * @throws UserNotFoundException if user does not exist
     * @throws DatabaseException     if database error occurs
     */
    public User getUser(String userId) throws UserNotFoundException, DatabaseException {
        if (userId == null || userId.trim().isEmpty()) {
            throw new UserNotFoundException(userId != null ? userId : "null");
        }
        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            Optional<User> userOpt = userDao.findById(conn, userId.trim());
            if (userOpt.isEmpty()) {
                throw new UserNotFoundException(userId.trim());
            }
            return userOpt.get();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve user '" + userId + "': " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves all registered students from MySQL.
     *
     * @return Collection of registered User entities
     * @throws DatabaseException if database error occurs
     */
    public Collection<User> getAllUsers() throws DatabaseException {
        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            return userDao.findAll(conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list users: " + e.getMessage(), e);
        }
    }

    /**
     * Registers a marketplace product reference in MySQL.
     *
     * @param product product reference entity
     * @throws DatabaseException if database error occurs
     * @throws UniGramException  if duplicate product ID is attempted
     */
    public void registerProduct(ProductReference product) throws DatabaseException, UniGramException {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null.");
        }
        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            if (productDao.existsById(conn, product.getProductId())) {
                throw new UniGramException("Product reference already exists with ID: " + product.getProductId());
            }
            productDao.insert(conn, product);
        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState())) {
                throw new UniGramException("Constraint violation registering product: " + e.getMessage());
            }
            throw new DatabaseException("Failed to register product: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves all registered product references from MySQL.
     *
     * @return Collection of registered ProductReference entities
     * @throws DatabaseException if database error occurs
     */
    public Collection<ProductReference> getAllProducts() throws DatabaseException {
        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            return productDao.findAll(conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list products: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a product reference by its ID from MySQL.
     *
     * @param productId product identifier
     * @return ProductReference entity
     * @throws UniGramException  if product reference does not exist
     * @throws DatabaseException if database error occurs
     */
    public ProductReference getProduct(String productId) throws UniGramException, DatabaseException {
        if (productId == null || productId.trim().isEmpty()) {
            throw new UniGramException("Product not found with ID: " + productId);
        }
        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            Optional<ProductReference> prodOpt = productDao.findById(conn, productId.trim());
            if (prodOpt.isEmpty()) {
                throw new UniGramException("Product not found with ID: " + productId.trim());
            }
            return prodOpt.get();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve product '" + productId + "': " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves all active conversations from MySQL.
     *
     * @return Collection of Conversation entities
     * @throws DatabaseException if database error occurs
     */
    public Collection<Conversation> getAllConversations() throws DatabaseException {
        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            return conversationDao.findAll(conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list conversations: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a new conversation in MySQL inside an atomic transaction.
     *
     * Validates:
     * 1. Buyer and seller cannot be identical (self-conversation prohibited).
     * 2. Buyer and seller must exist in the database.
     * 3. Product reference must exist in the database.
     * 4. Duplicate conversation for same buyer, seller, and product is rejected.
     *
     * @param buyerId   unique user ID of the buyer
     * @param sellerId  unique user ID of the seller
     * @param productId unique product reference ID
     * @return the created Conversation entity
     * @throws UserNotFoundException if buyer or seller does not exist
     * @throws UniGramException      if product does not exist or duplicate conversation attempted
     * @throws DatabaseException     if database transaction fails
     */
    public Conversation createConversation(String buyerId, String sellerId, String productId)
            throws UserNotFoundException, UniGramException, DatabaseException {

        if (buyerId == null || buyerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Buyer ID cannot be null or empty.");
        }
        if (sellerId == null || sellerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Seller ID cannot be null or empty.");
        }
        if (productId == null || productId.trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be null or empty.");
        }

        String cleanBuyerId = buyerId.trim();
        String cleanSellerId = sellerId.trim();
        String cleanProductId = productId.trim();

        // 1. Domain validation: prevent self-conversation
        if (cleanBuyerId.equalsIgnoreCase(cleanSellerId)) {
            throw new IllegalArgumentException("A user cannot start a conversation with themselves (buyer and seller cannot be identical).");
        }

        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            conn.setAutoCommit(false);
            try {
                // 2. Verify both users exist in database
                if (!userDao.existsById(conn, cleanBuyerId)) {
                    throw new UserNotFoundException(cleanBuyerId);
                }
                if (!userDao.existsById(conn, cleanSellerId)) {
                    throw new UserNotFoundException(cleanSellerId);
                }

                // 3. Verify product reference exists in database
                if (!productDao.existsById(conn, cleanProductId)) {
                    throw new UniGramException("Product not found with ID: " + cleanProductId);
                }

                // 4. Prevent duplicate conversation for the same buyer, seller, and product
                Optional<Conversation> existing = conversationDao.findByParticipantsAndProduct(conn, cleanBuyerId, cleanSellerId, cleanProductId);
                if (existing.isPresent()) {
                    throw new UniGramException(
                            "Duplicate conversation: A conversation already exists between users '"
                            + cleanBuyerId + "' and '" + cleanSellerId
                            + "' for product '" + cleanProductId + "' (Conversation ID: " + existing.get().getConversationId() + ")."
                    );
                }

                // 5. Generate unique conversation ID and insert
                String conversationId = "conv_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
                Conversation conversation = new Conversation(conversationId, cleanBuyerId, cleanSellerId, cleanProductId, LocalDateTime.now());

                conversationDao.insert(conn, conversation);

                conn.commit();
                return conversation;

            } catch (SQLException e) {
                conn.rollback();
                if ("23000".equals(e.getSQLState())) {
                    throw new UniGramException("Database integrity constraint violation during conversation creation: " + e.getMessage());
                }
                throw new DatabaseException("Failed to persist conversation: " + e.getMessage(), e);
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
     * Retrieves a conversation by its unique ID from MySQL.
     *
     * @param conversationId conversation identifier
     * @return Conversation entity
     * @throws ConversationNotFoundException if conversation does not exist
     * @throws DatabaseException             if database error occurs
     */
    public Conversation getConversationById(String conversationId) throws ConversationNotFoundException, DatabaseException {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new ConversationNotFoundException(conversationId != null ? conversationId : "null");
        }
        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            Optional<Conversation> convOpt = conversationDao.findById(conn, conversationId.trim());
            if (convOpt.isEmpty()) {
                throw new ConversationNotFoundException(conversationId.trim());
            }
            return convOpt.get();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve conversation: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves all conversations in which the specified user participates.
     *
     * @param userId user identifier
     * @return List of conversations belonging to user, ordered newest first
     * @throws UserNotFoundException if user does not exist
     * @throws DatabaseException     if database error occurs
     */
    public List<Conversation> getConversationsForUser(String userId) throws UserNotFoundException, DatabaseException {
        getUser(userId); // Verifies user exists
        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            return conversationDao.findByParticipant(conn, userId.trim());
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve conversations for user '" + userId + "': " + e.getMessage(), e);
        }
    }

    /**
     * Verifies that a user is an authorized participant in the conversation.
     *
     * @param conversationId target conversation ID
     * @param userId         user identifier
     * @throws ConversationNotFoundException            if conversation does not exist
     * @throws UnauthorizedConversationAccessException if user is neither buyer nor seller
     * @throws DatabaseException                        if database error occurs
     */
    public void validateParticipant(String conversationId, String userId)
            throws ConversationNotFoundException, UnauthorizedConversationAccessException, DatabaseException {

        Conversation conv = getConversationById(conversationId);
        if (!conv.isParticipant(userId)) {
            throw new UnauthorizedConversationAccessException(userId, conversationId);
        }
    }

    public DatabaseConfig getConfig() {
        return config;
    }
}
