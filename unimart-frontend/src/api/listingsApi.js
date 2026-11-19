// src/api/listingsApi.js
/**
 * UNIMART Production Listings API
 * 
 * Directly integrates with the authoritative Uni-Gram Java HTTP backend (com.unigram.api)
 * backed by persistent MySQL tables (listings, listing_photos, listing_pending_edits).
 *
 * Enforces college business rules:
 * - New listings start as 'pending' for admin verification.
 * - Price-only edits go live immediately.
 * - Non-price edits move to 'pending' state and store proposed changes in listing_pending_edits.
 * - Rejecting an edit reverts to the previously approved version.
 * - Pending/rejected listings are visible only to the listing owner and admins.
 * - Server authorizes all operations via Bearer session tokens.
 * - Local mock storage is isolated and enabled only in demo fallback mode.
 */

import { getTable, setTable, delay } from './db.js';
import { categories } from '../lib/categories.js';
import { authApi } from './authApi.js';

const API_BASE_URL = (import.meta.env.VITE_UNIGRAM_API_BASE_URL || 'http://127.0.0.1:8080').replace(/\/+$/, '');
const IS_PROD_MODE = Boolean(
  import.meta.env.PROD ||
  import.meta.env.VITE_APP_MODE === 'production' ||
  import.meta.env.VITE_ENFORCE_PROD_AUTH === 'true'
);

async function requestBackend(endpoint, options = {}) {
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

export function adaptServerListing(item) {
  if (!item) return null;
  const id = Number(item.id);
  const photos = Array.isArray(item.photos) ? item.photos : [];
  const catObj = categories.find((c) => c.id === item.categoryId || c.id === item.category_id);
  const subcatObj = catObj?.subcategories?.find((s) => s.id === item.subcategoryId || s.id === item.subcategory_id);

  let pendingEditChanges = null;
  if (item.pendingEdit) {
    try {
      pendingEditChanges = typeof item.pendingEdit === 'string' ? JSON.parse(item.pendingEdit) : item.pendingEdit;
    } catch (_e) {
      pendingEditChanges = null;
    }
  }

  const status = item.status || 'pending';
  const badge = status === 'pending'
    ? 'Waiting for Approval'
    : status === 'sold'
    ? 'Sold'
    : status === 'rejected'
    ? 'Rejected'
    : (item.badge || 'Verified');

  return {
    id,
    seller_id: item.sellerId || item.seller_id,
    seller: item.sellerName || item.seller_name || item.seller || 'Campus Student',
    seller_name: item.sellerName || item.seller_name || 'Campus Student',
    seller_department: item.sellerDepartment || item.seller_department || null,
    seller_email: item.sellerEmail || item.seller_email || '',
    title: item.title,
    category_id: item.categoryId || item.category_id,
    category: catObj ? catObj.name : 'General',
    subcategory_id: item.subcategoryId || item.subcategory_id || null,
    subcategory: subcatObj ? subcatObj.name : (item.subcategory || null),
    price: Number(item.price),
    original_price: item.originalPrice != null ? Number(item.originalPrice) : (item.original_price != null ? Number(item.original_price) : null),
    origPrice: item.originalPrice != null ? Number(item.originalPrice) : (item.original_price != null ? Number(item.original_price) : null),
    item_condition: item.itemCondition || item.item_condition || item.condition || 'Good',
    condition: item.itemCondition || item.item_condition || item.condition || 'Good',
    meetup_spot: item.meetupSpot || item.meetup_spot || item.notes || 'Library Entrance',
    notes: item.meetupSpot || item.meetup_spot || item.notes || 'Library Entrance',
    description: item.description || '',
    has_notes: Boolean(item.hasNotes !== undefined ? item.hasNotes : item.has_notes),
    hasNotes: Boolean(item.hasNotes !== undefined ? item.hasNotes : item.has_notes),
    photos: photos,
    image: photos[0] || null,
    badge,
    status,
    pending_type: item.pendingType || item.pending_type || null,
    hasPendingEdit: Boolean(pendingEditChanges),
    pendingEdit: pendingEditChanges,
    pendingEditCreatedAt: item.updatedAt || item.updated_at || null,
    created_at: item.createdAt || item.created_at,
    updated_at: item.updatedAt || item.updated_at
  };
}

function populateListingDemo(item, photosTable, usersTable, pendingEditsTable) {
  if (!item) return null;
  const photos = (photosTable || [])
    .filter((p) => p.listing_id === item.id)
    .sort((a, b) => (a.sort_order || 0) - (b.sort_order || 0))
    .map((p) => p.url);

  const seller = (usersTable || []).find((u) => u.id === item.seller_id);
  const sellerDisplayName = seller
    ? (seller.department ? `${seller.name} (${seller.department})` : seller.name)
    : (item.seller || 'Campus Student');
  const catObj = categories.find((c) => c.id === item.category_id);
  const subcatObj = catObj?.subcategories?.find((s) => s.id === item.subcategory_id);

  const pendingEdits = pendingEditsTable !== undefined ? pendingEditsTable : (getTable('listing_pending_edits') || []);
  const pendingEditEntry = (pendingEdits || []).find((pe) => Number(pe.listing_id) === Number(item.id));

  return {
    ...item,
    photos,
    image: photos[0] || null,
    seller: sellerDisplayName,
    seller_name: seller ? seller.name : 'Campus Student',
    seller_department: seller?.department || null,
    seller_email: seller ? seller.email : '',
    category: catObj ? catObj.name : 'General',
    subcategory: subcatObj ? subcatObj.name : (item.subcategory || null),
    subcategory_id: item.subcategory_id || null,
    condition: item.item_condition || item.condition,
    origPrice: item.original_price || item.origPrice,
    notes: item.meetup_spot || item.notes,
    hasNotes: item.hasNotes !== undefined ? item.hasNotes : Boolean(item.has_notes),
    has_notes: item.hasNotes !== undefined ? item.hasNotes : Boolean(item.has_notes),
    badge: item.status === 'pending'
      ? 'Waiting for Approval'
      : item.status === 'sold'
      ? 'Sold'
      : item.status === 'rejected'
      ? 'Rejected'
      : (item.badge || 'Verified'),
    hasPendingEdit: Boolean(pendingEditEntry),
    pendingEdit: pendingEditEntry ? pendingEditEntry.proposed_changes : null,
    pendingEditCreatedAt: pendingEditEntry ? pendingEditEntry.created_at : null
  };
}

export const listingsApi = {
  /**
   * Fetch all listings accessible to current user (with optional category & search filter).
   * Prioritizes Java API backend; falls back to demo mode when offline in non-production.
   */
  async getListings({ category_id, search, seller_id, currentUser } = {}) {
    // 1. Try Java Backend
    try {
      const qParams = new URLSearchParams();
      if (category_id && category_id !== 'All') qParams.set('category_id', category_id);
      if (seller_id) qParams.set('seller_id', seller_id);
      if (search && search.trim()) qParams.set('search', search.trim());

      const queryStr = qParams.toString() ? `?${qParams.toString()}` : '';
      const res = await requestBackend(`/api/listings${queryStr}`, { method: 'GET' });
      const rawListings = res.data || [];
      const adapted = rawListings.map(adaptServerListing);
      return { data: adapted };
    } catch (err) {
      if (IS_PROD_MODE) {
        throw new Error(`Failed to load listings from marketplace backend: ${err.message}`);
      }
    }

    // 2. Demo fallback
    await delay(200);
    const listings = getTable('listings') || [];
    const photos = getTable('listing_photos') || [];
    const users = getTable('users') || [];
    const pendingEdits = getTable('listing_pending_edits') || [];

    const isAdmin = currentUser?.role === 'admin';
    const currentUserId = currentUser?.id;

    let filtered = listings.filter((item) => {
      if (item.status === 'approved') return true;
      if (isAdmin) return true;
      if (currentUserId && item.seller_id === currentUserId) return true;
      return false;
    });

    if (category_id && category_id !== 'All') {
      filtered = filtered.filter((item) => item.category_id === category_id);
    }
    if (seller_id) {
      filtered = filtered.filter((item) => item.seller_id === seller_id);
    }
    if (search && search.trim()) {
      const q = search.trim().toLowerCase();
      filtered = filtered.filter((item) => {
        const catObj = categories.find((c) => c.id === item.category_id);
        const catName = catObj ? catObj.name.toLowerCase() : '';
        return (
          (item.title || '').toLowerCase().includes(q) ||
          (item.description || '').toLowerCase().includes(q) ||
          (item.meetup_spot || '').toLowerCase().includes(q) ||
          catName.includes(q)
        );
      });
    }

    filtered.sort((a, b) => new Date(b.created_at || 0) - new Date(a.created_at || 0));
    return { data: filtered.map((item) => populateListingDemo(item, photos, users, pendingEdits)) };
  },

  /**
   * Get single listing by ID
   */
  async getListingById(id, currentUser) {
    try {
      const res = await requestBackend(`/api/listings/${id}`, { method: 'GET' });
      return { data: adaptServerListing(res.data) };
    } catch (err) {
      if (IS_PROD_MODE || err.status === 404 || err.status === 403) {
        throw err;
      }
    }

    // Demo fallback
    await delay(150);
    const listings = getTable('listings') || [];
    const photos = getTable('listing_photos') || [];
    const users = getTable('users') || [];
    const pendingEdits = getTable('listing_pending_edits') || [];

    const item = listings.find((l) => Number(l.id) === Number(id));
    if (!item) throw new Error('Listing not found');

    const isAdmin = currentUser?.role === 'admin';
    const isOwner = currentUser?.id && item.seller_id === currentUser.id;
    if (item.status !== 'approved' && !isAdmin && !isOwner) {
      throw new Error('Listing is pending approval and visible only to its creator.');
    }

    return { data: populateListingDemo(item, photos, users, pendingEdits) };
  },

  /**
   * Create new listing. Assigns stable backend-generated identifier.
   */
  async createListing(data, currentUser) {
    if (!currentUser) {
      throw new Error('Guests cannot list items. Please log in with your Chitkara credentials.');
    }

    const payload = {
      title: (data.title || '').trim(),
      categoryId: data.category_id || data.categoryId || 'books-notes',
      subcategoryId: data.subcategory_id || data.subcategoryId || null,
      price: Number(data.price),
      originalPrice: data.origPrice ? Number(data.origPrice) : (data.original_price ? Number(data.original_price) : null),
      itemCondition: data.condition || data.item_condition || 'Good',
      meetupSpot: data.notes || data.meetup_spot || 'Library Entrance',
      description: data.description || '',
      hasNotes: Boolean(data.hasNotes || data.has_notes),
      badge: data.badge || null,
      photos: Array.isArray(data.photos) ? data.photos : []
    };

    try {
      const res = await requestBackend('/api/listings', {
        method: 'POST',
        body: payload
      });
      return {
        data: adaptServerListing(res.data),
        message: 'Listing submitted for campus admin approval.'
      };
    } catch (err) {
      if (IS_PROD_MODE) throw err;
    }

    // Isolated demo fallback
    await delay(250);
    const listings = getTable('listings') || [];
    const photosTable = getTable('listing_photos') || [];
    const users = getTable('users') || [];
    const pendingEdits = getTable('listing_pending_edits') || [];

    const newId = Date.now();
    const newListing = {
      id: newId,
      seller_id: currentUser.id,
      title: payload.title,
      category_id: payload.categoryId,
      subcategory_id: payload.subcategoryId,
      price: payload.price,
      original_price: payload.originalPrice,
      item_condition: payload.itemCondition,
      meetup_spot: payload.meetupSpot,
      description: payload.description,
      has_notes: payload.hasNotes,
      badge: payload.badge,
      status: 'pending',
      pending_type: 'new',
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString()
    };

    listings.unshift(newListing);
    setTable('listings', listings);

    payload.photos.forEach((url, idx) => {
      photosTable.push({
        id: Date.now() + idx,
        listing_id: newId,
        url,
        sort_order: idx
      });
    });
    setTable('listing_photos', photosTable);

    return {
      data: populateListingDemo(newListing, photosTable, users, pendingEdits),
      message: 'Listing submitted for campus admin approval.'
    };
  },

  /**
   * Update existing listing:
   * Price-only updates go live immediately; non-price edits require moderation.
   */
  async updateListing(id, updates, currentUser) {
    if (!currentUser) {
      throw new Error('Authentication required to edit listings.');
    }

    const payload = {
      title: updates.title,
      categoryId: updates.category_id || updates.categoryId,
      subcategoryId: updates.subcategory_id || updates.subcategoryId,
      price: updates.price !== undefined ? Number(updates.price) : undefined,
      originalPrice: updates.origPrice !== undefined ? Number(updates.origPrice) : (updates.original_price !== undefined ? Number(updates.original_price) : undefined),
      itemCondition: updates.condition || updates.item_condition,
      meetupSpot: updates.notes || updates.meetup_spot,
      description: updates.description,
      hasNotes: updates.hasNotes !== undefined ? updates.hasNotes : updates.has_notes,
      badge: updates.badge,
      photos: updates.photos
    };

    try {
      const res = await requestBackend(`/api/listings/${id}`, {
        method: 'PUT',
        body: payload
      });
      const adapted = adaptServerListing(res.data);
      const isPending = adapted.status === 'pending';
      return {
        data: adapted,
        editType: isPending ? 'pending' : 'price',
        message: isPending ? 'Changes submitted for admin approval.' : 'Listing price updated successfully!'
      };
    } catch (err) {
      if (IS_PROD_MODE || err.status === 403 || err.status === 404) throw err;
    }

    // Demo fallback
    await delay(200);
    const listings = getTable('listings') || [];
    const itemIndex = listings.findIndex((l) => Number(l.id) === Number(id));
    if (itemIndex === -1) throw new Error('Listing not found');
    const existing = listings[itemIndex];

    const updated = {
      ...existing,
      ...updates,
      price: updates.price !== undefined ? Number(updates.price) : existing.price,
      updated_at: new Date().toISOString()
    };
    listings[itemIndex] = updated;
    setTable('listings', listings);

    return {
      data: populateListingDemo(updated, getTable('listing_photos'), getTable('users'), getTable('listing_pending_edits')),
      editType: 'price',
      message: 'Listing updated successfully!'
    };
  },

  /**
   * Approve listing (Admin only)
   * The Java server authorizes the operation from the verified session token.
   */
  async approveListing(id, currentUser) {
    try {
      const res = await requestBackend(`/api/listings/${id}/approve`, {
        method: 'POST'
      });
      return {
        data: adaptServerListing(res.data),
        message: `Listing "${res.data.title}" approved!`
      };
    } catch (err) {
      if (IS_PROD_MODE || err.status === 403 || err.status === 404) throw err;
    }

    // Demo fallback
    await delay(150);
    const listings = getTable('listings') || [];
    const itemIndex = listings.findIndex((l) => Number(l.id) === Number(id));
    if (itemIndex === -1) throw new Error('Listing not found');

    listings[itemIndex].status = 'approved';
    listings[itemIndex].pending_type = null;
    listings[itemIndex].updated_at = new Date().toISOString();
    setTable('listings', listings);

    return {
      data: populateListingDemo(listings[itemIndex], getTable('listing_photos'), getTable('users'), getTable('listing_pending_edits')),
      message: `Listing "${listings[itemIndex].title}" approved!`
    };
  },

  /**
   * Reject listing (Admin only)
   * The Java server authorizes the operation from the verified session token.
   */
  async rejectListing(id, currentUser) {
    try {
      const res = await requestBackend(`/api/listings/${id}/reject`, {
        method: 'POST'
      });
      return {
        data: adaptServerListing(res.data),
        message: `Listing "${res.data.title}" marked as rejected.`
      };
    } catch (err) {
      if (IS_PROD_MODE || err.status === 403 || err.status === 404) throw err;
    }

    // Demo fallback
    await delay(150);
    const listings = getTable('listings') || [];
    const itemIndex = listings.findIndex((l) => Number(l.id) === Number(id));
    if (itemIndex === -1) throw new Error('Listing not found');

    listings[itemIndex].status = 'rejected';
    listings[itemIndex].pending_type = null;
    listings[itemIndex].updated_at = new Date().toISOString();
    setTable('listings', listings);

    return {
      data: populateListingDemo(listings[itemIndex], getTable('listing_photos'), getTable('users'), getTable('listing_pending_edits')),
      message: `Listing "${listings[itemIndex].title}" marked as rejected.`
    };
  },

  /**
   * Delete listing (Owner or Admin)
   */
  async deleteListing(id, currentUser) {
    try {
      await requestBackend(`/api/listings/${id}`, {
        method: 'DELETE'
      });
      return { success: true, message: 'Listing deleted.' };
    } catch (err) {
      if (IS_PROD_MODE || err.status === 403 || err.status === 404) throw err;
    }

    // Demo fallback
    await delay(150);
    let listings = getTable('listings') || [];
    listings = listings.filter((l) => Number(l.id) !== Number(id));
    setTable('listings', listings);
    return { success: true, message: 'Listing deleted.' };
  }
};

export default listingsApi;
