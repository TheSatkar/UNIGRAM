package com.unigram.collection;

import com.unigram.model.Conversation;
import com.unigram.model.Listing;
import com.unigram.model.Message;
import com.unigram.model.ProductReference;
import com.unigram.model.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory storage component utilizing Java Collections Framework.
 * Manages users, product listings, conversations, and conversation message histories
 * without external database dependencies.
 */
public class InMemoryStorage {

    // Maps for fast O(1) primary key lookup
    private final Map<String, User> userMap;
    private final Map<String, ProductReference> productMap;
    private final Map<String, Conversation> conversationMap;

    // One-to-many relationship: conversationId -> ordered list of messages
    private final Map<String, List<Message>> messageHistoryMap;

    // Marketplace listings and wishlists
    private final Map<Long, Listing> listingMap;
    private final Map<Long, String> pendingEditsMap;
    private final Map<String, Set<Long>> wishlistMap;
    private final AtomicLong nextListingId;

    public InMemoryStorage() {
        this.userMap = new ConcurrentHashMap<>();
        this.productMap = new ConcurrentHashMap<>();
        this.conversationMap = new ConcurrentHashMap<>();
        this.messageHistoryMap = new ConcurrentHashMap<>();
        this.listingMap = new ConcurrentHashMap<>();
        this.pendingEditsMap = new ConcurrentHashMap<>();
        this.wishlistMap = new ConcurrentHashMap<>();
        this.nextListingId = new AtomicLong(100);
    }

    // User Operations

    public void saveUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null.");
        }
        this.userMap.put(user.getUserId(), user);
    }

    public User getUser(String userId) {
        return this.userMap.get(userId);
    }

    public boolean existsUser(String userId) {
        return userId != null && this.userMap.containsKey(userId);
    }

    public Collection<User> getAllUsers() {
        return Collections.unmodifiableCollection(this.userMap.values());
    }

    public Optional<User> findUserByEmail(String email) {
        if (email == null) return Optional.empty();
        String clean = email.trim().toLowerCase();
        return this.userMap.values().stream()
                .filter(u -> u.getEmail() != null && u.getEmail().trim().equalsIgnoreCase(clean))
                .findFirst();
    }

    // Marketplace Listing Operations

    public long saveListing(Listing listing) {
        if (listing == null) throw new IllegalArgumentException("Listing cannot be null.");
        if (listing.getId() <= 0) {
            listing.setId(nextListingId.incrementAndGet());
        }
        this.listingMap.put(listing.getId(), listing);
        return listing.getId();
    }

    public Listing getListing(long id) {
        return this.listingMap.get(id);
    }

    public boolean existsListing(long id) {
        return this.listingMap.containsKey(id);
    }

    public Collection<Listing> getAllListings() {
        return Collections.unmodifiableCollection(this.listingMap.values());
    }

    public boolean deleteListing(long id) {
        this.pendingEditsMap.remove(id);
        this.wishlistMap.values().forEach(set -> set.remove(id));
        return this.listingMap.remove(id) != null;
    }

    public void savePendingEdit(long listingId, String proposedChangesJson) {
        this.pendingEditsMap.put(listingId, proposedChangesJson);
    }

    public Optional<String> getPendingEdit(long listingId) {
        return Optional.ofNullable(this.pendingEditsMap.get(listingId));
    }

    public void deletePendingEdit(long listingId) {
        this.pendingEditsMap.remove(listingId);
    }

    // Wishlist Operations

    public boolean addToWishlist(String userId, long listingId) {
        if (userId == null) return false;
        return this.wishlistMap.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(listingId);
    }

    public boolean removeFromWishlist(String userId, long listingId) {
        if (userId == null) return false;
        Set<Long> set = this.wishlistMap.get(userId);
        return set != null && set.remove(listingId);
    }

    public List<Long> getWishlistListingIds(String userId) {
        if (userId == null) return new ArrayList<>();
        Set<Long> set = this.wishlistMap.get(userId);
        return set != null ? new ArrayList<>(set) : new ArrayList<>();
    }

    // Product Reference Operations

    public void saveProduct(ProductReference product) {
        if (product == null) {
            throw new IllegalArgumentException("ProductReference cannot be null.");
        }
        this.productMap.put(product.getProductId(), product);
    }

    public ProductReference getProduct(String productId) {
        return this.productMap.get(productId);
    }

    public boolean existsProduct(String productId) {
        return productId != null && this.productMap.containsKey(productId);
    }

    public Collection<ProductReference> getAllProducts() {
        return Collections.unmodifiableCollection(this.productMap.values());
    }

    // Conversation Operations

    public void saveConversation(Conversation conversation) {
        if (conversation == null) {
            throw new IllegalArgumentException("Conversation cannot be null.");
        }
        this.conversationMap.put(conversation.getConversationId(), conversation);
        // Ensure message list is initialized for this conversation
        this.messageHistoryMap.putIfAbsent(conversation.getConversationId(), new ArrayList<>());
    }

    public Conversation getConversation(String conversationId) {
        return this.conversationMap.get(conversationId);
    }

    public boolean existsConversation(String conversationId) {
        return conversationId != null && this.conversationMap.containsKey(conversationId);
    }

    public Collection<Conversation> getAllConversations() {
        return Collections.unmodifiableCollection(this.conversationMap.values());
    }

    // Message Operations

    public void addMessage(Message message) {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null.");
        }
        List<Message> history = this.messageHistoryMap.computeIfAbsent(
                message.getConversationId(),
                k -> new ArrayList<>()
        );
        history.add(message);
    }

    public List<Message> getMessagesByConversationId(String conversationId) {
        List<Message> messages = this.messageHistoryMap.get(conversationId);
        if (messages == null) {
            return new ArrayList<>();
        }
        // Return a defensive copy to protect internal state
        return new ArrayList<>(messages);
    }

    /**
     * Clears all stored data. Useful for testing and environment resets.
     */
    public void clear() {
        this.userMap.clear();
        this.productMap.clear();
        this.conversationMap.clear();
        this.messageHistoryMap.clear();
        this.listingMap.clear();
        this.pendingEditsMap.clear();
        this.wishlistMap.clear();
    }
}
