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
 * - Bearer session token management via apiClient
 * - Strict role validation from authoritative server records
 */

import { getTable, setTable, delay } from './db.js';
import { sha256Hex } from '../lib/crypto.js';
import { isIntegratedMode } from '../lib/appMode.js';
import { apiRequest, getStoredToken, setStoredToken, API_BASE_URL } from './apiClient.js';

const SESSION_KEY = 'unimart_active_session_user_v1';
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
    return getStoredToken();
  },

  setToken(token) {
    setStoredToken(token);
  },

  getCurrentUserFromStorage() {
    try {
      const raw = localStorage.getItem(SESSION_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch {
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
    } catch {
      // ignore
    }
  },

  /**
   * Log in user with Chitkara email and password.
   */
  async login({ email, password }) {
    const normalizedEmail = (email || '').trim().toLowerCase();
    if (!normalizedEmail.endsWith('@chitkara.edu.in')) {
      throw new Error('Only official university emails ending in @chitkara.edu.in are allowed on UniMart.');
    }
    if (!password) {
      throw new Error('Password is required.');
    }

    if (isIntegratedMode()) {
      const res = await apiRequest('/api/auth/login', {
        method: 'POST',
        body: { email: normalizedEmail, password }
      });
      const payload = res.data || {};
      const user = payload.user || payload;
      const token = payload.token || null;
      if (token) this.setToken(token);
      this.setCurrentUser(user);
      return { data: user, token, isDemoMode: false };
    }

    // 2. Fallback: Local Demo Database with SHA-256 verification (only in explicit OFFLINE mode)
    await delay(250);
    const users = getTable('users') || [];
    const passwordHash = await sha256Hex(password);

    const matchedUser = users.find((u) => {
      const uEmail = (u.email || '').toLowerCase().trim();
      if (uEmail !== normalizedEmail) return false;
      return u.password_hash === passwordHash;
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
    return { data: sessionUser, token: 'demo_token_' + Date.now(), isDemoMode: true };
  },

  /**
   * Register new user with official university email and profile info.
   */
  async register({ name, email, student_id, password }) {
    if (!name || !name.trim()) throw new Error('Full Name is required.');
    const normalizedEmail = (email || '').trim().toLowerCase();
    if (!normalizedEmail.endsWith('@chitkara.edu.in')) {
      throw new Error('Registration is restricted to Chitkara University emails (@chitkara.edu.in).');
    }
    if (!password || password.length < 6) {
      throw new Error('Password must be at least 6 characters long.');
    }

    if (isIntegratedMode()) {
      const res = await apiRequest('/api/auth/register', {
        method: 'POST',
        body: { name, email: normalizedEmail, studentId: student_id, password }
      });
      const payload = res.data || {};
      const user = payload.user || payload;
      const token = payload.token || null;
      if (token) this.setToken(token);
      this.setCurrentUser(user);
      return { data: user, token, isDemoMode: false };
    }

    // 2. Fallback: Local Demo Database (only in explicit OFFLINE mode)
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
    const rawToken = profile?.idToken || profile?.credential || (typeof profile === 'string' ? profile : null);
    if (!rawToken && IS_PROD_AUTH) {
      throw new Error('Google ID Token (JWT) is required for production authentication.');
    }

    const normEmail = (profile?.email || '').toLowerCase().trim();
    if (normEmail && !normEmail.endsWith('@chitkara.edu.in')) {
      throw new Error('Access restricted: Only official Chitkara University accounts (@chitkara.edu.in) may sign in.');
    }

    if (isIntegratedMode()) {
      const res = await apiRequest('/api/auth/google', {
        method: 'POST',
        body: {
          idToken: rawToken,
          email: normEmail || undefined,
          name: profile?.name,
          picture: profile?.picture,
          sub: profile?.sub
        }
      });
      const payload = res.data || {};
      const user = payload.user || payload;
      const token = payload.token || null;
      if (token) this.setToken(token);
      this.setCurrentUser(user);
      return { data: user, token, isDemoMode: false };
    }

    // Demo fallback strictly in offline demo mode
    const users = getTable('users') || [];
    let user = users.find((u) => u.email.toLowerCase() === normEmail);
    if (!user) {
      user = {
        id: `google_${profile?.sub || Date.now()}`,
        name: profile?.name || normEmail.split('@')[0],
        email: normEmail,
        student_id: normEmail.split('@')[0].toUpperCase(),
        role: 'student',
        avatar: profile?.picture,
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
   * Retrieve currently authenticated session user.
   */
  async getCurrentUser() {
    const token = this.getToken();
    if (token) {
      try {
        const res = await apiRequest('/api/auth/me', { method: 'GET' });
        const user = res.data;
        this.setCurrentUser(user);
        return { data: user, isDemoMode: false };
      } catch (err) {
        if (err.status === 401 || err.status === 403 || err.status === 404) {
          this.setCurrentUser(null);
          return { data: null };
        }
        if (isIntegratedMode() || IS_PROD_AUTH) {
          return { data: null };
        }
      }
    } else if (isIntegratedMode() || IS_PROD_AUTH) {
      return { data: null };
    }

    const localUser = this.getCurrentUserFromStorage();
    if (localUser && localUser.isDemoMode) {
      return { data: localUser, isDemoMode: true };
    }
    return { data: null };
  },

  /**
   * End session
   */
  async logout() {
    const token = this.getToken();
    if (token) {
      try {
        await apiRequest('/api/auth/logout', { method: 'POST' });
      } catch {
        // ignore
      }
    }

    this.setCurrentUser(null);
    return { success: true };
  },

  /**
   * Password Recovery: Request OTP
   */
  async requestPasswordResetOtp(email) {
    const normalizedEmail = (email || '').trim().toLowerCase();
    if (!normalizedEmail.endsWith('@chitkara.edu.in')) {
      throw new Error('Only official Chitkara University emails ending in @chitkara.edu.in are supported.');
    }

    if (isIntegratedMode()) {
      const res = await apiRequest('/api/auth/forgot-password/request-otp', {
        method: 'POST',
        body: { email: normalizedEmail }
      });
      return res.data;
    }

    // Demo fallback for offline presentation
    await delay(350);
    const users = getTable('users') || [];
    const exists = users.some((u) => u.email.toLowerCase() === normalizedEmail);
    if (!exists) {
      throw new Error('No account found with this email. Please check your credentials.');
    }

    const otp = String(Math.floor(100000 + Math.random() * 900000));
    demoOtpStore.set(normalizedEmail, {
      otp,
      expiresAt: Date.now() + 10 * 60 * 1000,
      attemptsRemaining: 5
    });

    return {
      success: true,
      simulated: true,
      otp,
      message: `Simulated OTP [${otp}] generated for ${normalizedEmail}.`
    };
  },

  /**
   * Password Recovery: Verify OTP
   */
  async verifyPasswordResetOtp(email, otp) {
    const normalizedEmail = (email || '').trim().toLowerCase();
    const cleanOtp = (otp || '').trim();

    if (isIntegratedMode()) {
      const res = await apiRequest('/api/auth/forgot-password/verify-otp', {
        method: 'POST',
        body: { email: normalizedEmail, otp: cleanOtp }
      });
      return res.data;
    }

    // Demo fallback for offline presentation
    await delay(300);
    const record = demoOtpStore.get(normalizedEmail);
    if (!record) {
      throw new Error('No OTP request found for this email. Please request a new code.');
    }
    if (Date.now() > record.expiresAt) {
      demoOtpStore.delete(normalizedEmail);
      throw new Error('This verification code has expired. Please request a new one.');
    }
    if (record.otp !== cleanOtp) {
      record.attemptsRemaining--;
      if (record.attemptsRemaining <= 0) {
        demoOtpStore.delete(normalizedEmail);
        throw new Error('Too many invalid attempts. This OTP has been invalidated for security.');
      }
      throw new Error(`Invalid code. ${record.attemptsRemaining} attempts remaining.`);
    }

    demoOtpStore.delete(normalizedEmail);
    const resetToken = 'reset_' + Date.now() + '_' + Math.random().toString(36).substring(2, 8);
    demoOtpStore.set(resetToken, { email: normalizedEmail, expiresAt: Date.now() + 15 * 60 * 1000 });

    return { success: true, resetToken, message: 'OTP verified successfully.' };
  },

  /**
   * Password Recovery: Set New Password
   */
  async resetPassword({ resetToken, newPassword }) {
    if (!resetToken || !newPassword) {
      throw new Error('Reset token and new password are required.');
    }
    if (newPassword.length < 6) {
      throw new Error('Password must be at least 6 characters long.');
    }

    if (isIntegratedMode()) {
      const res = await apiRequest('/api/auth/forgot-password/reset', {
        method: 'POST',
        body: { resetToken, newPassword }
      });
      return res.data;
    }

    // Demo fallback for offline presentation
    await delay(350);
    const tokenRecord = demoOtpStore.get(resetToken);
    if (!tokenRecord || Date.now() > tokenRecord.expiresAt) {
      throw new Error('Password reset session has expired. Please start the recovery process again.');
    }

    const email = tokenRecord.email;
    demoOtpStore.delete(resetToken);

    const users = getTable('users') || [];
    const userIndex = users.findIndex((u) => u.email.toLowerCase() === email.toLowerCase());
    if (userIndex === -1) {
      throw new Error('User account not found.');
    }

    const newHash = await sha256Hex(newPassword);
    users[userIndex].password_hash = newHash;
    setTable('users', users);

    return { success: true, message: 'Password updated successfully. Please log in with your new password.' };
  }
};

export default authApi;
