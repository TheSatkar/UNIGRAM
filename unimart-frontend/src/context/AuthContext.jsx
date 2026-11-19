import React, { createContext, useContext, useState, useEffect } from 'react';
import { authApi } from '../api/authApi.js';
import { SEEDED_USERS } from '../lib/mockUsers.js';
import { isAdmin as checkIsAdmin } from '../lib/permissions.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  // Authoritative session initialization: strictly from local storage cache; never from URL query params
  const [user, setUser] = useState(() => {
    try {
      const raw = localStorage.getItem('unimart_active_session_user_v1');
      if (raw) return JSON.parse(raw);
    } catch (_e) {}
    return null;
  });

  const [loading, setLoading] = useState(false);

  useEffect(() => {
    let mounted = true;
    authApi.getCurrentUser().then((res) => {
      if (mounted && res?.data !== undefined) {
        setUser(res.data);
      }
    });
    return () => {
      mounted = false;
    };
  }, []);

  const login = async (credentialsOrUser) => {
    setLoading(true);
    try {
      // If user object already authenticated via authApi (e.g. Google SSO or existing session)
      if (credentialsOrUser && credentialsOrUser.email && !credentialsOrUser.password) {
        authApi.setCurrentUser(credentialsOrUser);
        setUser(credentialsOrUser);
        return credentialsOrUser;
      }
      // If credentials object passed { email, password }
      const res = await authApi.login(credentialsOrUser);
      setUser(res.data);
      return res.data;
    } finally {
      setLoading(false);
    }
  };

  const register = async (userData) => {
    setLoading(true);
    try {
      const res = await authApi.register(userData);
      setUser(res.data);
      return res.data;
    } finally {
      setLoading(false);
    }
  };

  const logout = async () => {
    setLoading(true);
    try {
      await authApi.logout();
      setUser(null);
    } finally {
      setLoading(false);
    }
  };

  // Developer switch account: available for local testing / demo mode
  const switchAccount = (accountKey) => {
    if (accountKey === 'guest' || accountKey === null) {
      logout();
      return;
    }
    const found = SEEDED_USERS.find(
      (u) => u.id === accountKey || u.email === accountKey
    );
    if (found) {
      const { password: _p, ...safeFound } = found;
      const demoUser = {
        ...safeFound,
        isDemoMode: true,
        storageNotice: 'LOCAL_DEMO_EPHEMERAL'
      };
      authApi.setCurrentUser(demoUser);
      setUser(demoUser);
    }
  };

  const isDemoMode = Boolean(user?.isDemoMode) || !authApi.getToken();

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        role: user?.role || 'guest',
        isAdmin: checkIsAdmin(user),
        isDemoMode,
        login,
        register,
        logout,
        switchAccount,
        seededUsers: SEEDED_USERS
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return ctx;
}
