package com.unigram.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain entity representing a Chitkara student marketplace listing.
 */
public class Listing {

    private long id;
    private String sellerId;
    private String title;
    private String categoryId;
    private String subcategoryId;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String itemCondition;
    private String meetupSpot;
    private String description;
    private boolean hasNotes;
    private String badge;
    private String status; // "pending", "approved", "rejected", "sold"
    private String pendingType; // "new", "edit", null
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> photos = new ArrayList<>();

    public Listing() {
        this.status = "pending";
        this.pendingType = "new";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Listing(long id, String sellerId, String title, String categoryId, BigDecimal price,
                   String itemCondition, String meetupSpot, String description) {
        this();
        this.id = id;
        this.sellerId = sellerId;
        this.title = title;
        this.categoryId = categoryId;
        this.price = price;
        this.itemCondition = itemCondition;
        this.meetupSpot = meetupSpot;
        this.description = description;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getSubcategoryId() { return subcategoryId; }
    public void setSubcategoryId(String subcategoryId) { this.subcategoryId = subcategoryId; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }

    public String getItemCondition() { return itemCondition; }
    public void setItemCondition(String itemCondition) { this.itemCondition = itemCondition; }

    public String getMeetupSpot() { return meetupSpot; }
    public void setMeetupSpot(String meetupSpot) { this.meetupSpot = meetupSpot; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isHasNotes() { return hasNotes; }
    public void setHasNotes(boolean hasNotes) { this.hasNotes = hasNotes; }

    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPendingType() { return pendingType; }
    public void setPendingType(String pendingType) { this.pendingType = pendingType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<String> getPhotos() { return photos; }
    public void setPhotos(List<String> photos) { this.photos = photos != null ? photos : new ArrayList<>(); }
}
