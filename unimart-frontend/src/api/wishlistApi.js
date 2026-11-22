// src/api/wishlistApi.js
/**
 * UNIMART Production Wishlist API
 * 
 * Interacts directly with the Java HTTP API (/api/wishlist) backed by MySQL.
 * Enforces server-side authorization: Guests cannot wishlist items (requires authenticated Chitkara account).
 */

import { getTable, setTable, delay } from './db.js';
import { listingsApi, adaptServerListing } from './listingsApi.js';
import { isIntegratedMode } from '../lib/appMode.js';
import { apiRequest } from './apiClient.js';

export const wishlistApi = {
  /**
   * Retrieve all wishlist items for a given user
   */
  async getWishlist(userId) {
    if (!userId) {
      throw new Error('Please log in with your Chitkara account to view your wishlist.');
    }

    if (isIntegratedMode()) {
      const res = await apiRequest('/api/wishlist', { method: 'GET' });
      const payload = res.data || {};
      const rawItems = payload.data || [];
      const listingIds = payload.listingIds || [];
      return {
        data: rawItems.map(adaptServerListing),
        listingIds: listingIds.map(Number)
      };
    }

    // Demo fallback (only in explicit OFFLINE mode)
    await delay(150);
    const wishlistTable = getTable('wishlist') || [];
    const userWishlist = wishlistTable.filter((w) => w.user_id === userId);

    const listingsRes = await listingsApi.getListings();
    const allListings = listingsRes.data || [];

    const items = userWishlist
      .map((w) => allListings.find((l) => Number(l.id) === Number(w.listing_id)))
      .filter(Boolean);

    return {
      data: items,
      listingIds: userWishlist.map((w) => Number(w.listing_id))
    };
  },

  /**
   * Add a listing to user's wishlist
   */
  async addToWishlist(userId, listingId) {
    if (!userId) {
      throw new Error('Please log in with your Chitkara account to save items.');
    }

    if (isIntegratedMode()) {
      await apiRequest('/api/wishlist', {
        method: 'POST',
        body: { listingId: Number(listingId) }
      });
      return { success: true, message: 'Added to wishlist!' };
    }

    // Demo fallback (only in explicit OFFLINE mode)
    await delay(100);
    const wishlistTable = getTable('wishlist') || [];
    const exists = wishlistTable.some(
      (w) => w.user_id === userId && Number(w.listing_id) === Number(listingId)
    );

    if (!exists) {
      wishlistTable.push({
        user_id: userId,
        listing_id: Number(listingId),
        created_at: new Date().toISOString()
      });
      setTable('wishlist', wishlistTable);
    }

    return { success: true, message: 'Added to wishlist!' };
  },

  /**
   * Remove a listing from user's wishlist
   */
  async removeFromWishlist(userId, listingId) {
    if (!userId) {
      throw new Error('Authentication required.');
    }

    if (isIntegratedMode()) {
      await apiRequest(`/api/wishlist/${listingId}`, { method: 'DELETE' });
      return { success: true, message: 'Removed from wishlist.' };
    }

    // Demo fallback (only in explicit OFFLINE mode)
    await delay(100);
    let wishlistTable = getTable('wishlist') || [];
    wishlistTable = wishlistTable.filter(
      (w) => !(w.user_id === userId && Number(w.listing_id) === Number(listingId))
    );
    setTable('wishlist', wishlistTable);

    return { success: true, message: 'Removed from wishlist.' };
  },

  /**
   * Toggle item in user's wishlist
   */
  async toggleWishlist(userId, listingId) {
    const res = await this.getWishlist(userId).catch(() => ({ listingIds: [] }));
    const isSaved = (res.listingIds || []).includes(Number(listingId));
    if (isSaved) {
      return this.removeFromWishlist(userId, listingId);
    } else {
      return this.addToWishlist(userId, listingId);
    }
  }
};

export default wishlistApi;
