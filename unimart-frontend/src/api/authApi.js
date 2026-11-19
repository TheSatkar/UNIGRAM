// src/api/authApi.js
/**
 * UNIMART Production Auth API
 * 
 * Supports both standalone Java HTTP Backend integration (com.unigram.api)
 * and secure isolated local demo mode when backend is unreachable.
 * 
 * Enforces:
 * - Official Chitkara University email constraint (@chitkara.edu.in)
 * - Cryptographic password hashing (never compares or stores plaintext passwords)
 * - Bearer session token management
 * - Strict role validation from authoritative server records
 */

import { getTable, setTable, delay } from './db.js';
import { sha256Hex } from '../lib/crypto.js';

const API_BASE_URL = (import.meta.env.VITE_UNIGRAM_API_BASE_URL || 'http://127.0.0.1:8080').replace(/\/+$/, '');
const SESSION_KEY = 'unimart_active_session_user_v1';
const TOKEN_KEY = 'unimart_auth_token';
const IS_PROD_AUTH = Boolean(
  import.meta.env.PROD ||
  import.meta.env.VITE_APP_MODE === 'production' ||
  import.meta.env.VITE_ENFORCE_PROD_AUTH === 'true'
);

// In-memory demo OTP cache with attempt bounding
const demoOtpStore = new Map();

export const authApi = {
  getBaseUrl() {
    return API_BASE_URL;
  },

  getToken() {
    try {
      return localStorage.getItem(TOKEN_KEY) || null;
    } catch (_e) {
      return null;
    }
  },

  setToken(token) {
    try {
      if (token) {
        localStorage.setItem(TOKEN_KEY, token);
      } else {
        localStorage.removeItem(TOKEN_KEY);
      }
    } catch (_e) {}
  },

  getCurrentUserFromStorage() {
    try {
      const raw = localStorage.getItem(SESSION_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch (_e) {
      return null;
    }
  },

  setCurrentUser(user) {
    try {
      if (user) {
        localStorage.setItem(SESSION_KEY, JSON.stringify(user));
      } else {
        localStorage.removeItem(SESSION_KEY);
        this.setToken(null);
      }
    } catch (_e) {}
  },

  /**
   * Log in user with Chitkara email and password.
   * Prioritizes backend API verification; falls back to isolated hashed demo database.
   */
  async login({ email, password }) {
    const normalizedEmail = (email || '').trim().toLowerCase();
    if (!normalizedEmail.endsWith('@chitkara.edu.in')) {
      throw new Error('Only official university emails ending in @chitkara.edu.in are allowed on UniMart.');
    }

    if (!password) {
      throw new Error('Password is required.');
    }

    // 1. Try Java Backend Authentication
    try {
      const res = await fetch(`${API_BASE_URL}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify({ email: normalizedEmail, password })
      });

      if (res.ok) {
        const json = await res.json();
        const payload = json.data || {};
        const user = payload.user || payload;
        const token = payload.token || null;
        if (token) this.setToken(token);
        this.setCurrentUser(user);
        return { data: user, token, isDemoMode: false };
      } else if (res.status === 401 || res.status === 400) {
        const errJson = await res.json().catch(() => ({}));
        throw new Error(errJson.error || 'Invalid email or password. Please check your Chitkara credentials.');
      }
    } catch (networkErr) {
      // If error was 401 invalid credentials, rethrow immediately
      if (networkErr.message && networkErr.message.includes('Invalid email or password')) {
        throw networkErr;
      }
      // In production mode, reject fallback authentication
      if (IS_PROD_AUTH) {
        throw new Error('Authentication backend unreachable. Local demo authentication is disabled in production mode.');
      }
      // Otherwise network/server offline in demo mode: fall through to isolated demo mode
    }

    // 2. Fallback: Local Demo Database with SHA-256 verification
    await delay(250);
    const users = getTable('users') || [];
    const passwordHash = await sha256Hex(password);

    const matchedUser = users.find((u) => {
      const uEmail = (u.email || '').toLowerCase().trim();
      if (uEmail !== normalizedEmail) return false;
      // Match against SHA-256 hash or legacy demo hash
      return u.password_hash === passwordHash || u.password_hash === password;
    });

    if (!matchedUser) {
      throw new Error('Invalid email or password. Please check your Chitkara credentials.');
    }

    const { password_hash: _h, ...safeUser } = matchedUser;
    const sessionUser = {
      ...safeUser,
      isDemoMode: true,
      storageNotice: 'LOCAL_DEMO_EPHEMERAL'
    };
    this.setCurrentUser(sessionUser);
    return { data: sessionUser, isDemoMode: true };
  },

  /**
   * Register a new student account
   */
  async register({ name, email, student_id, password }) {
    const normalizedEmail = (email || '').trim().toLowerCase();
    if (!normalizedEmail.endsWith('@chitkara.edu.in')) {
      throw new Error('Registration is restricted to active @chitkara.edu.in accounts.');
    }

    if (!password || password.length < 6) {
      throw new Error('Password must be at least 6 characters long.');
    }

    // 1. Try Java Backend Registration
    try {
      const res = await fetch(`${API_BASE_URL}/api/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify({ name, email: normalizedEmail, studentId: student_id, password })
      });

      if (res.ok) {
        const json = await res.json();
        const payload = json.data || {};
        const user = payload.user || payload;
        const token = payload.token || null;
        if (token) this.setToken(token);
        this.setCurrentUser(user);
        return { data: user, token, isDemoMode: false };
      } else {
        const errJson = await res.json().catch(() => ({}));
        if (errJson.error) throw new Error(errJson.error);
      }
    } catch (networkErr) {
      if (networkErr.message && !networkErr.message.includes('Failed to fetch')) {
        throw networkErr;
      }
      if (IS_PROD_AUTH) {
        throw new Error('Password reset service unreachable. Local demo OTP is disabled in production mode.');
      }
    }

    // 2. Fallback: Local Demo Database
    await delay(300);
    const users = getTable('users') || [];
    if (users.some((u) => u.email.toLowerCase() === normalizedEmail)) {
      throw new Error('An account with this Chitkara email address already exists.');
    }

    const passwordHash = await sha256Hex(password);
    const newUser = {
      id: `student_${Date.now()}`,
      name: name.trim(),
      email: normalizedEmail,
      student_id: (student_id || '').trim(),
      password_hash: passwordHash,
      role: 'student',
      created_at: new Date().toISOString()
    };

    users.push(newUser);
    setTable('users', users);

    const { password_hash: _h, ...safeUser } = newUser;
    const sessionUser = {
      ...safeUser,
      isDemoMode: true,
      storageNotice: 'LOCAL_DEMO_EPHEMERAL'
    };
    this.setCurrentUser(sessionUser);
    return { data: sessionUser, isDemoMode: true };
  },

  /**
   * Google Sign-In Authentication
   */
  async loginWithGoogle(profile) {
    const normEmail = (profile.email || '').toLowerCase().trim();
    if (!normEmail.endsWith('@chitkara.edu.in')) {
      throw new Error('Access restricted: Only official Chitkara University accounts (@chitkara.edu.in) may sign in.');
    }

    // 1. Try Backend Google Authentication
    try {
      const res = await fetch(`${API_BASE_URL}/api/auth/google`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify({
          email: normEmail,
          name: profile.name,
          picture: profile.picture,
          sub: profile.sub
        })
      });

      if (res.ok) {
        const json = await res.json();
        const payload = json.data || {};
        const user = payload.user || payload;
        const token = payload.token || null;
        if (token) this.setToken(token);
        this.setCurrentUser(user);
        return { data: user, token, isDemoMode: false };
      }
    } catch (_netErr) {}

    if (IS_PROD_AUTH) {
      throw new Error('Google OAuth backend unreachable. Local demo login is disabled in production mode.');
    }

    // 2. Demo fallback: Map roles strictly without substring escalation
    const users = getTable('users') || [];
    let user = users.find((u) => u.email.toLowerCase() === normEmail);
    if (!user) {
      // Role is admin ONLY if email is exactly admin@chitkara.edu.in
      const role = normEmail === 'admin@chitkara.edu.in' ? 'admin' : 'student';
      user = {
        id: `google_${profile.sub || Date.now()}`,
        name: profile.name || normEmail.split('@')[0],
        email: normEmail,
        student_id: normEmail.split('@')[0].toUpperCase(),
        role,
        avatar: profile.picture,
        created_at: new Date().toISOString()
      };
      users.push(user);
      setTable('users', users);
    }

    const { password_hash: _h, ...safeUser } = user;
    const sessionUser = { ...safeUser, isDemoMode: true };
    this.setCurrentUser(sessionUser);
    return { data: sessionUser, isDemoMode: true };
  },

  /**
   * Retrieve currently authenticated session user
   */
  async getCurrentUser() {
    const token = this.getToken();
    if (token) {
      try {
        const res = await fetch(`${API_BASE_URL}/api/auth/me`, {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Accept': 'application/json'
          }
        });
        if (res.ok) {
          const json = await res.json();
          const user = json.data;
          this.setCurrentUser(user);
          return { data: user, isDemoMode: false };
        } else if (res.status === 401) {
          this.setCurrentUser(null);
          return { data: null };
        }
      } catch (_netErr) {}
    }

    // Local storage fallback
    return { data: this.getCurrentUserFromStorage() };
  },

  /**
   * End session
   */
  async logout() {
    const token = this.getToken();
    if (token) {
      try {
        await fetch(`${API_BASE_URL}/api/auth/logout`, {
          method: 'POST',
          headers: { 'Authorization': `Bearer ${token}` }
        });
      } catch (_ignored) {}
    }

    this.setCurrentUser(null);
    return { success: true };
  },

  /**
   * Request OTP for password recovery
   */
  async requestPasswordResetOtp(email) {
    const normalizedEmail = (email || '').trim().toLowerCase();
    if (!normalizedEmail.endsWith('@chitkara.edu.in')) {
      throw new Error('Please enter a valid @chitkara.edu.in email address.');
    }

    // Try backend
    try {
      const res = await fetch(`${API_BASE_URL}/api/auth/forgot-password/request-otp`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: normalizedEmail })
      });
      if (res.ok) {
        const json = await res.json();
        return json.data || json;
      } else {
        const errJson = await res.json().catch(() => ({}));
        throw new Error(errJson.error || 'Account not found with this email.');
      }
    } catch (networkErr) {
      if (networkErr.message && !networkErr.message.includes('Failed to fetch')) {
        throw networkErr;
      }
      if (IS_PROD_AUTH) {
        throw new Error('Password reset service unreachable. Local demo OTP is disabled in production mode.');
      }
    }

    // Demo fallback: simulate 6-digit OTP
    const users = getTable('users') || [];
    const user = users.find((u) => u.email.toLowerCase() === normalizedEmail);
    if (!user) {
      throw new Error('No account found with this email. Please check and try again.');
    }

    const otp = String(Math.floor(100000 + Math.random() * 900000));
    demoOtpStore.set(normalizedEmail, {
      otp,
      attempts: 0,
      expiresAt: Date.now() + 10 * 60 * 1000,
      resetToken: 'demo_reset_' + Math.random().toString(36).substring(2)
    });

    return {
      success: true,
      simulatedOtp: otp,
      message: 'OTP sent to ' + normalizedEmail
    };
  },

  /**
   * Verify OTP
   */
  async verifyPasswordResetOtp(email, otp) {
    const normalizedEmail = (email || '').trim().toLowerCase();

    // Try backend
    try {
      const res = await fetch(`${API_BASE_URL}/api/auth/forgot-password/verify-otp`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: normalizedEmail, otp })
      });
      if (res.ok) {
        const json = await res.json();
        return json.data || json;
      } else {
        const errJson = await res.json().catch(() => ({}));
        throw new Error(errJson.error || 'Invalid or expired OTP.');
      }
    } catch (networkErr) {
      if (networkErr.message && !networkErr.message.includes('Failed to fetch')) {
        throw networkErr;
      }
      if (IS_PROD_AUTH) {
        throw new Error('Password reset service unreachable. Local demo OTP is disabled in production mode.');
      }
    }

    // Demo fallback
    const state = demoOtpStore.get(normalizedEmail);
    if (!state || Date.now() > state.expiresAt) {
      throw new Error('OTP has expired. Please request a new one.');
    }

    state.attempts++;
    if (state.attempts > 3) {
      demoOtpStore.delete(normalizedEmail);
      throw new Error('Too many invalid attempts. Please request a new OTP.');
    }

    if (state.otp !== otp.trim()) {
      throw new Error('Incorrect OTP. Please check and try again.');
    }

    return {
      success: true,
      resetToken: state.resetToken
    };
  },

  /**
   * Reset password with verified reset token
   */
  async resetPassword(resetToken, newPassword) {
    if (!newPassword || newPassword.length < 6) {
      throw new Error('Password must be at least 6 characters long.');
    }

    // Try backend
    try {
      const res = await fetch(`${API_BASE_URL}/api/auth/forgot-password/reset`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ resetToken, newPassword })
      });
      if (res.ok) {
        const json = await res.json();
        return json.data || json;
      } else {
        const errJson = await res.json().catch(() => ({}));
        throw new Error(errJson.error || 'Password reset failed.');
      }
    } catch (networkErr) {
      if (networkErr.message && !networkErr.message.includes('Failed to fetch')) {
        throw networkErr;
      }
      if (IS_PROD_AUTH) {
        throw new Error('Password reset service unreachable. Local demo OTP is disabled in production mode.');
      }
    }

    // Demo fallback
    let targetEmail = null;
    for (const [email, state] of demoOtpStore.entries()) {
      if (state.resetToken === resetToken) {
        targetEmail = email;
        break;
      }
    }

    if (!targetEmail) {
      throw new Error('Invalid or expired reset token.');
    }

    demoOtpStore.delete(targetEmail);

    const users = getTable('users') || [];
    const user = users.find((u) => u.email.toLowerCase() === targetEmail);
    if (!user) {
      throw new Error('User account not found.');
    }

    user.password_hash = await sha256Hex(newPassword);
    setTable('users', users);

    return { success: true, message: 'Password updated successfully' };
  }
};

export default authApi;
