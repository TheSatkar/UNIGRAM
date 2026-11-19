package com.unigram.api;

/**
 * Request payload for creating a new marketplace conversation.
 */
public class CreateConversationRequest {

    private String buyerId;
    private String sellerId;
    private String productId;

    public CreateConversationRequest() {
    }

    public CreateConversationRequest(String buyerId, String sellerId, String productId) {
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.productId = productId;
    }

    public String getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(String buyerId) {
        this.buyerId = buyerId;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }
}
