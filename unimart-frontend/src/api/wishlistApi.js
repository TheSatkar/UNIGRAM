// src/api/wishlistApi.js
/**
 * UNIMART Production Wishlist API
 * 
 * Interacts directly with the Java HTTP API (/api/wishlist) backed by MySQL.
 * Enforces server-side authorization: Guests cannot wishlist items (requires authenticated Chitkara account).
 */

import { getTable, setTable, delay } from './db.js';
import { listingsApi, adaptServerListing } from './listingsApi.js';
import { authApi } from './authApi.js';

const API_BASE_URL = (import.meta.env.VITE_UNIGRAM_API_BASE_URL || 'http://127.0.0.1:8080').replace(/\/+$/, '');
const IS_PROD_MODE = Boolean(
  import.meta.env.PROD ||
  import.meta.env.VITE_APP_MODE === 'production' ||
  import.meta.env.VITE_ENFORCE_PROD_AUTH === 'true'
);

async function requestWishlist(endpoint, options = {}) {
  const url = `${API_BASE_URL}${endpoint}`;
  const headers = {
    'Accept': 'application/json',
    ...(options.headers || {})
  };

  const token = authApi.getToken();
  if (token && !headers['Authorization']) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  if (IS_PROD_MODE && !headers['X-Require-Durable']) {
    headers['X-Require-Durable'] = 'true';
  }

  if (options.body && typeof options.body === 'object') {
    headers['Content-Type'] = 'application/json';
    options.body = JSON.stringify(options.body);
  }

  const res = await fetch(url, { ...options, headers });
  let json = null;
  const contentType = res.headers.get('content-type') || '';
  if (contentType.includes('application/json')) {
    try {
      json = await res.json();
    } catch (_e) {
      json = null;
    }
  }

  if (!res.ok) {
    const errorMsg = json?.error || `HTTP ${res.status}: ${res.statusText}`;
    const err = new Error(errorMsg);
    err.status = res.status;
    err.data = json?.data || null;
    throw err;
  }

  return json;
}

export const wishlistApi = {
  /**
   * Retrieve all wishlist items for a given user
   */
  async getWishlist(userId) {
    if (!userId) {
      throw new Error('Please log in with your Chitkara account to view your wishlist.');
    }

    try {
      const res = await requestWishlist('/api/wishlist', { method: 'GET' });
      const payload = res.data || {};
      const rawItems = payload.data || [];
      const listingIds = payload.listingIds || [];
      return {
        data: rawItems.map(adaptServerListing),
        listingIds: listingIds.map(Number)
      };
    } catch (err) {
      if (IS_PROD_MODE || err.status === 401) {
        throw err;
      }
    }

    // Demo fallback
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

    try {
      await requestWishlist('/api/wishlist', {
        method: 'POST',
        body: { listingId: Number(listingId) }
      });
      return { success: true, message: 'Added to wishlist!' };
    } catch (err) {
      if (IS_PROD_MODE || err.status === 401) {
        throw err;
      }
    }

    // Demo fallback
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

    try {
      await requestWishlist(`/api/wishlist/${listingId}`, {
        method: 'DELETE'
      });
      return { success: true, message: 'Removed from wishlist.' };
    } catch (err) {
      if (IS_PROD_MODE || err.status === 401) {
        throw err;
      }
    }

    // Demo fallback
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
