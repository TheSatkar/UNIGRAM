package com.unigram.service;

import com.unigram.collection.InMemoryStorage;
import com.unigram.exception.ConversationNotFoundException;
import com.unigram.exception.UnauthorizedConversationAccessException;
import com.unigram.exception.UniGramException;
import com.unigram.exception.UserNotFoundException;
import com.unigram.model.Conversation;
import com.unigram.model.ProductReference;
import com.unigram.model.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Service managing user registration, product reference registration,
 * and conversation lifecycle operations.
 */
public class ChatService {

    private final InMemoryStorage storage;

    public ChatService(InMemoryStorage storage) {
        if (storage == null) {
            throw new IllegalArgumentException("InMemoryStorage cannot be null.");
        }
        this.storage = storage;
    }

    /**
     * Registers a new student user.
     *
     * @param user user entity to register
     */
    public void registerUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null.");
        }
        this.storage.saveUser(user);
    }

    /**
     * Retrieves a user by their unique identifier.
     *
     * @param userId user identifier
     * @return User entity
     * @throws UserNotFoundException if user does not exist
     */
    public User getUser(String userId) throws UserNotFoundException {
        if (userId == null || !this.storage.existsUser(userId)) {
            throw new UserNotFoundException(userId != null ? userId : "null");
        }
        return this.storage.getUser(userId);
    }

    /**
     * Retrieves all registered students.
     *
     * @return Collection of registered User entities
     */
    public java.util.Collection<User> getAllUsers() {
        return this.storage.getAllUsers();
    }

    /**
     * Registers a marketplace product reference.
     *
     * @param product product reference entity
     */
    public void registerProduct(ProductReference product) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null.");
        }
        this.storage.saveProduct(product);
    }

    /**
     * Retrieves all registered product references.
     *
     * @return Collection of registered ProductReference entities
     */
    public java.util.Collection<ProductReference> getAllProducts() {
        return this.storage.getAllProducts();
    }

    /**
     * Retrieves all active conversations in the system.
     *
     * @return Collection of Conversation entities
     */
    public java.util.Collection<Conversation> getAllConversations() {
        return this.storage.getAllConversations();
    }

    /**
     * Retrieves a product reference by its ID.
     *
     * @param productId product identifier
     * @return ProductReference entity
     * @throws UniGramException if product reference does not exist
     */
    public ProductReference getProduct(String productId) throws UniGramException {
        if (productId == null || !this.storage.existsProduct(productId)) {
            throw new UniGramException("Product not found with ID: " + productId);
        }
        return this.storage.getProduct(productId);
    }

    /**
     * Creates a new conversation between a buyer and a seller regarding a specific product.
     *
     * Validates:
     * 1. Buyer and seller cannot be identical.
     * 2. Buyer and seller must exist.
     * 3. Product must exist.
     * 4. A duplicate conversation for the same buyer, seller, and product is rejected.
     * (Different products between the same buyer and seller create distinct conversations).
     *
     * @param buyerId   unique user ID of the buyer
     * @param sellerId  unique user ID of the seller
     * @param productId unique product reference ID
     * @return the created Conversation entity
     * @throws UserNotFoundException if buyer or seller does not exist
     * @throws UniGramException      if product does not exist or duplicate conversation is attempted
     */
    public Conversation createConversation(String buyerId, String sellerId, String productId)
            throws UserNotFoundException, UniGramException {

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

        // 1. Prevent self-conversation
        if (cleanBuyerId.equalsIgnoreCase(cleanSellerId)) {
            throw new IllegalArgumentException("A user cannot start a conversation with themselves (buyer and seller cannot be identical).");
        }

        // 2. Verify both users exist
        getUser(cleanBuyerId);
        getUser(cleanSellerId);

        // 3. Verify product exists
        getProduct(cleanProductId);

        // 4. Prevent duplicate conversation for the same buyer, seller, and product
        for (Conversation existing : this.storage.getAllConversations()) {
            boolean sameProduct = existing.getProductId() != null && existing.getProductId().equalsIgnoreCase(cleanProductId);
            boolean sameParticipants =
                    (existing.getBuyerId().equalsIgnoreCase(cleanBuyerId) && existing.getSellerId().equalsIgnoreCase(cleanSellerId))
                    || (existing.getBuyerId().equalsIgnoreCase(cleanSellerId) && existing.getSellerId().equalsIgnoreCase(cleanBuyerId));

            if (sameProduct && sameParticipants) {
                throw new UniGramException(
                        "Duplicate conversation: A conversation already exists between users '" 
                        + cleanBuyerId + "' and '" + cleanSellerId 
                        + "' for product '" + cleanProductId + "' (Conversation ID: " + existing.getConversationId() + ")."
                );
            }
        }

        // 5. Generate unique conversation ID and instantiate
        String conversationId = "conv_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Conversation conversation = new Conversation(conversationId, cleanBuyerId, cleanSellerId, cleanProductId, LocalDateTime.now());

        // 6. Save in storage
        this.storage.saveConversation(conversation);
        return conversation;
    }

    /**
     * Starts or retrieves an existing direct student-to-student conversation.
     * Synchronized to prevent duplicate direct threads under concurrent initiation.
     *
     * @param initiatorId initiating student's ID
     * @param recipientId recipient student's ID
     * @return existing or newly created direct Conversation entity
     * @throws UserNotFoundException if either student does not exist
     */
    public synchronized Conversation startDirectConversation(String initiatorId, String recipientId)
            throws UserNotFoundException {

        if (initiatorId == null || initiatorId.trim().isEmpty()) {
            throw new IllegalArgumentException("Initiating student ID cannot be null or empty.");
        }
        if (recipientId == null || recipientId.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient student ID cannot be null or empty.");
        }

        String cleanInitiatorId = initiatorId.trim();
        String cleanRecipientId = recipientId.trim();

        if (cleanInitiatorId.equalsIgnoreCase(cleanRecipientId)) {
            throw new IllegalArgumentException("A student cannot start a conversation with themselves.");
        }

        getUser(cleanInitiatorId);
        getUser(cleanRecipientId);

        for (Conversation existing : this.storage.getAllConversations()) {
            if (!existing.isDirectConversation()) {
                continue;
            }
            boolean sameParticipants =
                    (existing.getBuyerId().equalsIgnoreCase(cleanInitiatorId) && existing.getSellerId().equalsIgnoreCase(cleanRecipientId))
                    || (existing.getBuyerId().equalsIgnoreCase(cleanRecipientId) && existing.getSellerId().equalsIgnoreCase(cleanInitiatorId));
            if (sameParticipants) {
                return existing;
            }
        }

        String conversationId = "conv_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Conversation conversation = new Conversation(conversationId, cleanInitiatorId, cleanRecipientId, LocalDateTime.now());
        this.storage.saveConversation(conversation);
        return conversation;
    }

    /**
     * Retrieves a conversation by its unique ID.
     *
     * @param conversationId conversation identifier
     * @return Conversation entity
     * @throws ConversationNotFoundException if conversation does not exist
     */
    public Conversation getConversationById(String conversationId) throws ConversationNotFoundException {
        if (conversationId == null || !this.storage.existsConversation(conversationId)) {
            throw new ConversationNotFoundException(conversationId != null ? conversationId : "null");
        }
        return this.storage.getConversation(conversationId);
    }

    /**
     * Retrieves all conversations where the specified user is either the buyer or the seller.
     *
     * @param userId user identifier
     * @return List of conversations belonging to the user
     * @throws UserNotFoundException if user does not exist
     */
    public List<Conversation> getConversationsForUser(String userId) throws UserNotFoundException {
        getUser(userId); // Verifies user existence

        List<Conversation> userConversations = new ArrayList<>();
        for (Conversation conv : this.storage.getAllConversations()) {
            if (conv.isParticipant(userId)) {
                userConversations.add(conv);
            }
        }

        // Sort by creation time descending (most recent first) using Collections.sort and Comparator
        userConversations.sort(Comparator.comparing(Conversation::getCreatedAt).reversed());
        return userConversations;
    }

    /**
     * Verifies that a user is an authorized participant in the conversation.
     *
     * @param conversationId conversation identifier
     * @param userId         user identifier
     * @throws ConversationNotFoundException            if conversation does not exist
     * @throws UnauthorizedConversationAccessException if user is not buyer or seller
     */
    public void validateParticipant(String conversationId, String userId)
            throws ConversationNotFoundException, UnauthorizedConversationAccessException {

        Conversation conversation = getConversationById(conversationId);
        if (!conversation.isParticipant(userId)) {
            throw new UnauthorizedConversationAccessException(userId, conversationId);
        }
    }
}
