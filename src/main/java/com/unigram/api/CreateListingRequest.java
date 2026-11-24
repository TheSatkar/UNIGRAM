package com.unigram.api;

import java.math.BigDecimal;
import java.util.List;

public class CreateListingRequest {
    private String title;
    private String category;
    private String category_id;
    private String categoryId;
    private String subCategory;
    private String subcategory_id;
    private String subcategoryId;
    private BigDecimal price;
    private BigDecimal original_price;
    private BigDecimal originalPrice;
    private String item_condition;
    private String condition;
    private String meetup_spot;
    private String meetupSpot;
    private String notes;
    private String description;
    private Boolean has_notes;
    private Boolean hasNotes;
    private String badge;
    private List<String> photos;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategoryId() {
        if (categoryId != null) return categoryId;
        if (category_id != null) return category_id;
        return category;
    }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public void setCategory_id(String category_id) { this.category_id = category_id; }
    public void setCategory(String category) { this.category = category; }

    public String getSubcategoryId() {
        if (subcategoryId != null) return subcategoryId;
        if (subcategory_id != null) return subcategory_id;
        return subCategory;
    }
    public void setSubcategoryId(String subcategoryId) { this.subcategoryId = subcategoryId; }
    public void setSubcategory_id(String subcategory_id) { this.subcategory_id = subcategory_id; }
    public void setSubCategory(String subCategory) { this.subCategory = subCategory; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getOriginalPrice() {
        return originalPrice != null ? originalPrice : original_price;
    }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
    public void setOriginal_price(BigDecimal original_price) { this.original_price = original_price; }

    public String getItemCondition() {
        return condition != null ? condition : item_condition;
    }
    public void setCondition(String condition) { this.condition = condition; }
    public void setItem_condition(String item_condition) { this.item_condition = item_condition; }

    public String getMeetupSpot() {
        return meetup_spot != null ? meetup_spot : notes;
    }
    public void setMeetup_spot(String meetup_spot) { this.meetup_spot = meetup_spot; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isHasNotes() {
        if (hasNotes != null) return hasNotes;
        if (has_notes != null) return has_notes;
        return false;
    }
    public void setHasNotes(Boolean hasNotes) { this.hasNotes = hasNotes; }
    public void setHas_notes(Boolean has_notes) { this.has_notes = has_notes; }

    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }

    public List<String> getPhotos() { return photos; }
    public void setPhotos(List<String> photos) { this.photos = photos; }
}
