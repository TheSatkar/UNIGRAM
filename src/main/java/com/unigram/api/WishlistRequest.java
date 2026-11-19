package com.unigram.api;

public class WishlistRequest {
    private Long listingId;
    private Long listing_id;

    public Long getListingId() {
        return listingId != null ? listingId : listing_id;
    }
    public void setListingId(Long listingId) { this.listingId = listingId; }
    public void setListing_id(Long listing_id) { this.listing_id = listing_id; }
}
