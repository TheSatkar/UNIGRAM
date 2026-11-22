/**
 * Uni-Gram HTTP API Integration Service for UniMart
 *
 * Centralizes all communication between the UniMart React frontend
 * and the standalone Uni-Gram Java HTTP backend (com.sun.net.httpserver.HttpServer).
 *
 * Configurable via Vite environment variable: VITE_UNIGRAM_API_BASE_URL
 */

import { apiRequest, API_BASE_URL } from '../api/apiClient.js';

class UnigramApiError extends Error {
  constructor(message, status = 500, data = null) {
    super(message);
    this.name = 'UnigramApiError';
    this.status = status;
    this.data = data;
  }
}

async function request(endpoint, options = {}) {
  try {
    return await apiRequest(endpoint, options);
  } catch (err) {
    throw new UnigramApiError(err.message, err.status || 500, err.data || null);
  }
}

export const unigramApi = {
  getBaseUrl() {
    return API_BASE_URL;
  },

  /**
   * Health check verifying Java backend and MySQL database readiness.
   */
  async checkHealth() {
    return request('/api/health', { method: 'GET' });
  },

  /**
   * Lists all users registered in Uni-Gram.
   */
  async getUsers() {
    return request('/api/users', { method: 'GET' });
  },

  /**
   * Retrieves single student user by ID.
   */
  async getUser(userId) {
    return request(`/api/users/${encodeURIComponent(userId)}`, { method: 'GET' });
  },

  /**
   * Synchronizes a student user reference into Uni-Gram (idempotent).
   */
  async syncUser({ userId, fullName, email }) {
    return request('/api/users', {
      method: 'POST',
      body: { userId, fullName, email }
    });
  },

  /**
   * Lists all product references registered in Uni-Gram.
   */
  async getProducts() {
    return request('/api/products', { method: 'GET' });
  },

  /**
   * Synchronizes a marketplace listing reference into Uni-Gram (idempotent).
   */
  async syncProduct({ productId, productTitle }) {
    return request('/api/products', {
      method: 'POST',
      body: { productId, productTitle }
    });
  },

  /**
   * Retrieves conversations for a participant, ordered newest first.
   */
  async getConversations(participantId) {
    const query = participantId ? `?participantId=${encodeURIComponent(participantId)}` : '';
    return request(`/api/conversations${query}`, { method: 'GET' });
  },

  /**
   * Retrieves a single conversation by conversation ID.
   */
  async getConversationById(conversationId) {
    return request(`/api/conversations/${encodeURIComponent(conversationId)}`, { method: 'GET' });
  },

  /**
   * Initiates a new conversation thread in MySQL.
   * If a thread already exists, catches 409 and returns the existing conversation.
   */
  async createConversation({ buyerId, sellerId, productId }) {
    try {
      return await request('/api/conversations', {
        method: 'POST',
        body: { buyerId, sellerId, productId }
      });
    } catch (err) {
      if (err.status === 409 && err.data) {
        // Backend attached existing conversation entity on conflict
        return { success: true, data: err.data };
      }
      throw err;
    }
  },

  /**
   * Retrieves the full chronological message history for a conversation.
   * Authenticated via Bearer token with optional X-Dev-User-Id fallback.
   */
  async getMessageHistory(conversationId, devUserId) {
    const headers = {};
    const token = authApi.getToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    if (devUserId) {
      headers['X-Dev-User-Id'] = devUserId;
    }
    return request(`/api/conversations/${encodeURIComponent(conversationId)}/messages`, {
      method: 'GET',
      headers
    });
  },

  /**
   * Sends a new message in a conversation.
   * Derives sender identity strictly from verified Bearer session token.
   */
  async sendMessage(conversationId, devUserId, content) {
    const headers = {};
    const token = authApi.getToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    if (devUserId) {
      headers['X-Dev-User-Id'] = devUserId;
    }
    if (!token && !devUserId) {
      throw new UnigramApiError('Authentication required to send messages. Please sign in.', 401);
    }
    return request(`/api/conversations/${encodeURIComponent(conversationId)}/messages`, {
      method: 'POST',
      headers,
      body: { content }
    });
  }
};

export default unigramApi;
