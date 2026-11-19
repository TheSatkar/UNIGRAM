package com.unigram.model;

import java.time.LocalDateTime;

/**
 * Bookmark entity associating a student user with a saved marketplace listing.
 */
public class WishlistItem {

    private String userId;
    private long listingId;
    private LocalDateTime createdAt;

    public WishlistItem() {
        this.createdAt = LocalDateTime.now();
    }

    public WishlistItem(String userId, long listingId) {
        this();
        this.userId = userId;
        this.listingId = listingId;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public long getListingId() { return listingId; }
    public void setListingId(long listingId) { this.listingId = listingId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
