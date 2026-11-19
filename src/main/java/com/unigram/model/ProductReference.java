package com.unigram.model;

import java.util.Objects;

/**
 * Represents a lightweight reference to an external marketplace listing.
 * Stores only reference data without implementing marketplace business logic.
 */
public class ProductReference {

    // Immutable identity field
    private final String productId;

    // Descriptive metadata
    private String productTitle;

    /**
     * Primary constructor initializing product reference.
     *
     * @param productId    unique product/listing identifier (cannot be null or blank)
     * @param productTitle title of the marketplace item
     */
    public ProductReference(String productId, String productTitle) {
        if (productId == null || productId.trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be null or empty.");
        }
        this.productId = productId.trim();
        this.productTitle = productTitle != null && !productTitle.trim().isEmpty() 
                ? productTitle.trim() 
                : "Untitled Listing";
    }

    /**
     * Overloaded constructor with default title.
     * Demonstrates constructor chaining using 'this(...)'.
     *
     * @param productId unique product/listing identifier
     */
    public ProductReference(String productId) {
        this(productId, "Untitled Listing");
    }

    // Getters and Setters

    public String getProductId() {
        return this.productId;
    }

    public String getProductTitle() {
        return this.productTitle;
    }

    public void setProductTitle(String productTitle) {
        this.productTitle = productTitle != null && !productTitle.trim().isEmpty()
                ? productTitle.trim()
                : "Untitled Listing";
    }

    // Method Overrides

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductReference that = (ProductReference) o;
        return Objects.equals(this.productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.productId);
    }

    @Override
    public String toString() {
        return "ProductReference{" +
                "productId='" + this.productId + '\'' +
                ", productTitle='" + this.productTitle + '\'' +
                '}';
    }
}
