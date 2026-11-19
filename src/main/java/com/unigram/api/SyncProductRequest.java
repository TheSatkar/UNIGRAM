package com.unigram.api;

/**
 * Request payload for synchronizing a marketplace product reference from UniMart into Uni-Gram.
 * Supports flexible field names (productId/id, productTitle/title) for seamless React integration.
 */
public class SyncProductRequest {

    private String productId;
    private String id;
    private String productTitle;
    private String title;

    public SyncProductRequest() {
    }

    public SyncProductRequest(String productId, String productTitle) {
        this.productId = productId;
        this.productTitle = productTitle;
    }

    public String getProductId() {
        if (productId != null && !productId.trim().isEmpty()) {
            return productId.trim();
        }
        return id != null ? id.trim() : null;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProductTitle() {
        if (productTitle != null && !productTitle.trim().isEmpty()) {
            return productTitle.trim();
        }
        return title != null ? title.trim() : null;
    }

    public void setProductTitle(String productTitle) {
        this.productTitle = productTitle;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
