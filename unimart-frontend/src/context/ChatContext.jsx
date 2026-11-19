import React, { createContext, useContext, useState, useEffect, useRef, useCallback } from 'react';
import { useAuth } from './AuthContext.jsx';
import { unigramApi } from '../services/unigramApi.js';
import { mockListings } from '../data/mockListings.js';
import { SEEDED_USERS } from '../lib/mockUsers.js';

const ChatContext = createContext(null);

function getConversationStorageKey(userId) {
  return userId ? `unimart_user_conversations_v7_${userId}` : null;
}

function loadStoredConversations(userId) {
  const key = getConversationStorageKey(userId);
  if (!key) return [];
  try {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) : [];
  } catch (_e) {
    return [];
  }
}

function persistStoredConversations(userId, updater) {
  const key = getConversationStorageKey(userId);
  if (!key) return [];
  try {
    const current = loadStoredConversations(userId);
    const updated = typeof updater === 'function' ? updater(current) : updater;
    localStorage.setItem(key, JSON.stringify(updated));
    return updated;
  } catch (_e) {
    return [];
  }
}

/**
 * Format timestamp into user-friendly time string (e.g. "8:30 pm")
 */
function formatTime(isoString) {
  if (!isoString) return '';
  try {
    const d = new Date(isoString);
    return d.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit', hour12: true }).toLowerCase();
  } catch (_e) {
    return '';
  }
}

/**
 * Format date into separator string (e.g. "Today", "Nov 18")
 */
function formatDate(isoString) {
  if (!isoString) return 'Today';
  try {
    const d = new Date(isoString);
    const now = new Date();
    if (d.toDateString() === now.toDateString()) {
      return 'Today';
    }
    return d.toLocaleDateString([], { month: 'short', day: 'numeric' });
  } catch (_e) {
    return 'Today';
  }
}

/**
 * Map a Uni-Gram Java message entity to UniMart UI message format.
 */
function mapJavaMessageToUi(javaMsg, currentUserId, otherUserName) {
  const isUser = javaMsg.senderId === currentUserId;
  return {
    id: javaMsg.messageId,
    sender: isUser ? 'user' : 'host',
    senderId: javaMsg.senderId,
    senderName: isUser ? 'You' : (otherUserName || javaMsg.senderId),
    timestamp: formatTime(javaMsg.sentAt),
    date: formatDate(javaMsg.sentAt),
    type: 'text',
    text: javaMsg.content,
    readReceipt: 'Delivered',
    rawSentAt: javaMsg.sentAt,
    reactions: []
  };
}

/**
 * Helper to resolve listing metadata by productId.
 */
function resolveListing(productId, fallbackItem = null) {
  if (fallbackItem && String(fallbackItem.id) === String(productId)) {
    return {
      id: fallbackItem.id,
      title: fallbackItem.title,
      price: fallbackItem.price,
      origPrice: fallbackItem.origPrice,
      condition: fallbackItem.condition || 'Verified',
      image: fallbackItem.image || (fallbackItem.photos && fallbackItem.photos[0]) || null,
      category: fallbackItem.category || 'General',
      meetupSpot: fallbackItem.notes || fallbackItem.meetupSpot || fallbackItem.pickupSpot || 'Library Entrance',
      status: 'Available',
      sublabel: `${fallbackItem.condition || 'Verified'} • ${fallbackItem.category || 'Item'}`
    };
  }

  const found = mockListings.find((l) => String(l.id) === String(productId));
  if (found) {
    return {
      id: found.id,
      title: found.title,
      price: found.price,
      origPrice: found.origPrice,
      condition: found.condition || 'Verified',
      image: found.image || null,
      category: found.category || 'General',
      meetupSpot: found.notes || 'Library Entrance',
      status: 'Available',
      sublabel: `${found.condition || 'Verified'} • ${found.category || 'Item'}`
    };
  }

  return {
    id: productId,
    title: `Listing #${productId}`,
    price: 0,
    category: 'Marketplace',
    condition: 'Verified',
    meetupSpot: 'Campus Center',
    status: 'Available'
  };
}

/**
 * Helper to resolve participant profile info.
 */
function resolveParticipant(otherUserId, roleLabel = 'Seller') {
  const user = SEEDED_USERS.find((u) => u.id === otherUserId || u.studentId === otherUserId);
  const name = user ? user.name : (otherUserId === 'student_b' ? 'Priya K.' : (otherUserId === 'student_a' ? 'Satkar Singh' : otherUserId));
  const dept = user?.department || 'Chitkara Campus Member';
  const initial = name.charAt(0).toUpperCase() || 'S';

  return {
    id: otherUserId,
    name,
    displayName: name,
    shortName: name.split(' ')[0],
    roleLabel,
    badge: initial,
    department: dept,
    avatar: initial,
    avatarBg: '#70182c',
    online: true,
    verified: true,
    responseTime: '15 mins',
    rating: '4.9 ★ (Verified Student)'
  };
}

export function ChatProvider({ children }) {
  const { user } = useAuth() || {};
  const currentUserId = user?.id || null;

  const [conversations, setConversations] = useState(() => {
    if (!currentUserId) return [];
    return loadStoredConversations(currentUserId);
  });
  const [activeConversationId, setActiveConversationId] = useState(null);
  const [isOpen, setIsOpen] = useState(false);
  const [isMinimized, setIsMinimized] = useState(false);
  const [loading, setLoading] = useState(false);
  const [sending, setSending] = useState(false);
  const [apiError, setApiError] = useState(null);
  const [backendConnected, setBackendConnected] = useState(false);

  // Active conversation object
  const activeConversation = conversations.find((c) => c.id === activeConversationId) || (conversations.length > 0 ? conversations[0] : null);

  // Total unread count
  const unreadTotal = conversations.reduce((acc, conv) => acc + (conv.unreadCount || 0), 0);

  /**
   * Refetch conversation list from Uni-Gram Java API, including full
   * message snippets and unread indicators for each participant.
   */
  const loadConversationsFromBackend = useCallback(async () => {
    if (!currentUserId) {
      setConversations([]);
      setActiveConversationId(null);
      return;
    }

    try {
      const res = await unigramApi.getConversations(currentUserId);
      setBackendConnected(true);
      setApiError(null);

      const javaConvs = res.data || [];
      if (javaConvs.length === 0) {
        // Fallback to user-scoped stored conversations if backend is empty
        const userStored = loadStoredConversations(currentUserId);
        if (userStored.length > 0) {
          setConversations(userStored);
          setActiveConversationId((prev) => (userStored.some((c) => c.id === prev) ? prev : userStored[0].id));
        }
        return;
      }

      // Populate full message history for each conversation in parallel
      const populated = await Promise.all(
        javaConvs.map(async (jc) => {
          const isBuyer = jc.buyerId === currentUserId;
          const otherId = isBuyer ? jc.sellerId : jc.buyerId;
          const roleLabel = isBuyer ? 'Seller' : 'Buyer';
          const participant = resolveParticipant(otherId, roleLabel);
          const listing = resolveListing(jc.productId);

          let uiMsgs = [];
          try {
            const msgRes = await unigramApi.getMessageHistory(jc.conversationId, currentUserId);
            const javaMsgs = msgRes.data || [];
            uiMsgs = javaMsgs.map((m) => mapJavaMessageToUi(m, currentUserId, participant?.name));
          } catch (_e) {
            uiMsgs = [];
          }

          // Compute unread: if there are messages from the other user
          const lastMsg = uiMsgs.length > 0 ? uiMsgs[uiMsgs.length - 1] : null;
          const unreadCount = lastMsg && lastMsg.sender !== 'user' ? 1 : 0;

          return {
            id: jc.conversationId,
            conversationId: jc.conversationId,
            buyerId: jc.buyerId,
            sellerId: jc.sellerId,
            productId: jc.productId,
            participant,
            listing,
            unreadCount,
            lastUpdated: lastMsg && lastMsg.rawSentAt
              ? new Date(lastMsg.rawSentAt).getTime()
              : (jc.createdAt ? new Date(jc.createdAt).getTime() : Date.now()),
            messages: uiMsgs
          };
        })
      );

      // Sort conversations newest-activity first
      populated.sort((a, b) => (b.lastUpdated || 0) - (a.lastUpdated || 0));

      // Persist to user-scoped storage
      persistStoredConversations(currentUserId, (prevStored) => {
        const map = new Map(prevStored.map((c) => [c.id, c]));
        for (const c of populated) {
          const existing = map.get(c.id);
          if (existing && existing.messages && existing.messages.length > (c.messages?.length || 0)) {
            map.set(c.id, { ...c, messages: existing.messages });
          } else {
            map.set(c.id, c);
          }
        }
        return Array.from(map.values());
      });

      setConversations(populated);

      // Automatically select the most recent conversation if none is active or active was temporary
      setActiveConversationId((prev) => {
        if (prev && !prev.startsWith('temp_') && !prev.includes('_student_') && populated.some((c) => c.id === prev)) {
          return prev;
        }
        return populated.length > 0 ? populated[0].id : null;
      });

    } catch (err) {
      console.warn('[Uni-Gram Chat] Failed to load conversations from Java backend:', err.message);
      setBackendConnected(false);
      // Seamlessly fall back to user-scoped local cache
      const userStored = loadStoredConversations(currentUserId);
      if (userStored.length > 0) {
        setConversations(userStored);
        setActiveConversationId((prev) => (userStored.some((c) => c.id === prev) ? prev : userStored[0].id));
      }
    }
  }, [currentUserId]);

  const prevUserIdRef = useRef(currentUserId);
  useEffect(() => {
    if (currentUserId) {
      // Immediate instant hydration from user-scoped storage
      const userStored = loadStoredConversations(currentUserId);
      if (userStored.length > 0) {
        setConversations(userStored);
        setActiveConversationId((prev) => (userStored.some((c) => c.id === prev) ? prev : userStored[0].id));
      }

      unigramApi.syncUser({
        userId: currentUserId,
        fullName: user?.name || 'Campus Student',
        email: user?.email || ''
      }).catch((_e) => {});

      loadConversationsFromBackend();
    } else if (prevUserIdRef.current && !currentUserId) {
      setConversations([]);
      setActiveConversationId(null);
    }
    prevUserIdRef.current = currentUserId;
  }, [currentUserId, loadConversationsFromBackend, user?.name, user?.email]);

  // Background polling every 4s so Satkar and Priya see new messages and threads automatically
  useEffect(() => {
    if (!currentUserId) return;
    const interval = setInterval(() => {
      loadConversationsFromBackend();
    }, 4000);
    return () => clearInterval(interval);
  }, [currentUserId, loadConversationsFromBackend]);

  /**
   * Fetch latest message history for a conversation from Java API.
   */
  const fetchMessagesForConversation = useCallback(async (convId) => {
    if (!convId || !currentUserId) return;
    // Strictly prevent querying backend for temporary client-side IDs
    if (convId.startsWith('temp_') || (convId.startsWith('conv_') && convId.includes('_student_'))) {
      return;
    }

    try {
      const res = await unigramApi.getMessageHistory(convId, currentUserId);
      const javaMsgs = res.data || [];

      setConversations((prev) =>
        prev.map((c) => {
          if (c.id === convId || c.conversationId === convId) {
            const otherName = c.participant?.name;
            const uiMsgs = javaMsgs.map((m) => mapJavaMessageToUi(m, currentUserId, otherName));
            return {
              ...c,
              messages: uiMsgs,
              lastUpdated: javaMsgs.length > 0
                ? new Date(javaMsgs[javaMsgs.length - 1].sentAt).getTime()
                : c.lastUpdated
            };
          }
          return c;
        })
      );
      setBackendConnected(true);
      setApiError(null);
    } catch (err) {
      console.warn(`[Uni-Gram Chat] Error fetching messages for ${convId}:`, err.message);
      setApiError(err.message);
    }
  }, [currentUserId]);

  // Load message history whenever activeConversationId changes
  useEffect(() => {
    if (activeConversationId) {
      fetchMessagesForConversation(activeConversationId);
    }
  }, [activeConversationId, fetchMessagesForConversation]);

  // Polling loop: poll active conversation every 3.5 seconds while chat is open
  const isPollingRef = useRef(false);
  useEffect(() => {
    if (!isOpen || isMinimized || !activeConversationId || !currentUserId) {
      return;
    }

    const intervalId = setInterval(async () => {
      if (isPollingRef.current) return;
      isPollingRef.current = true;
      try {
        await fetchMessagesForConversation(activeConversationId);
      } finally {
        isPollingRef.current = false;
      }
    }, 3500);

    return () => {
      clearInterval(intervalId);
    };
  }, [isOpen, isMinimized, activeConversationId, currentUserId, fetchMessagesForConversation]);

  /**
   * Open chat directly for a specific marketplace listing item.
   * Immediately opens the modal with item context, then synchronizes
   * buyer, seller, and product with the Uni-Gram Java API.
   */
  const openChatWithItem = async (item) => {
    if (!item) return;

    // 1. Immediately open and un-minimize ChatModal so user sees immediate response
    setIsOpen(true);
    setIsMinimized(false);
    setApiError(null);

    const productId = String(item.id || '1');
    let sellerId = item.seller_id || item.sellerId;
    if (!sellerId) {
      if (item.seller && item.seller.includes('Priya')) {
        sellerId = 'student_b';
      } else if (item.seller && item.seller.includes('Satkar')) {
        sellerId = 'student_a';
      } else {
        sellerId = 'seller_' + productId;
      }
    }
    sellerId = String(sellerId);

    const sellerName = item.seller_name || item.seller || 'Campus Seller';
    const sellerEmail = item.seller_email || '';

    // Check if user owns this listing
    if (currentUserId && currentUserId === sellerId) {
      setApiError('This is your own listing (buyer and seller are identical). You cannot message yourself.');
      return;
    }

    // Check if we already have a real existing conversation for this product and seller
    const existingReal = conversations.find(
      (c) => c.productId === productId && (c.sellerId === sellerId || c.buyerId === sellerId) && !c.id.startsWith('temp_') && !c.id.includes('_student_')
    );

    if (existingReal) {
      setActiveConversationId(existingReal.id);
      fetchMessagesForConversation(existingReal.id);
      return;
    }

    // 2. Resolve participant and listing immediately for optimistic local UI state
    const participant = resolveParticipant(sellerId, 'Seller');
    const listing = resolveListing(productId, item);
    const tempConvId = `temp_${productId}_${sellerId}`;

    // 3. Immediately set active conversation so UI renders product title and seller immediately
    setConversations((prev) => {
      const idx = prev.findIndex((c) => c.productId === productId);
      if (idx !== -1) return prev;
      return [
        {
          id: tempConvId,
          conversationId: tempConvId,
          buyerId: currentUserId || 'guest',
          sellerId,
          productId,
          participant,
          listing,
          unreadCount: 0,
          lastUpdated: Date.now(),
          messages: []
        },
        ...prev
      ];
    });
    setActiveConversationId(tempConvId);

    // 4. If guest user, prompt to sign in but keep modal open
    if (!currentUserId) {
      setApiError('You are viewing as Guest. Sign in with a student account to message this seller.');
      return;
    }

    setLoading(true);

    try {
      // Step A: Sync buyer user reference
      await unigramApi.syncUser({
        userId: currentUserId,
        fullName: user?.name || 'Campus Student',
        email: user?.email || ''
      }).catch((_e) => {});

      // Step B: Sync seller user reference
      await unigramApi.syncUser({
        userId: sellerId,
        fullName: sellerName,
        email: sellerEmail
      }).catch((_e) => {});

      // Step C: Sync product reference
      await unigramApi.syncProduct({
        productId,
        productTitle: item.title || 'Marketplace Item'
      }).catch((_e) => {});

      // Step D: Create or retrieve conversation from Java backend
      const convRes = await unigramApi.createConversation({
        buyerId: currentUserId,
        sellerId,
        productId
      });

      const javaConv = convRes?.data;
      const targetConvId = javaConv?.conversationId || javaConv?.id || javaConv?.conversation_id;

      if (targetConvId) {
        // Update state with persistent Java ID
        const updatedConvObj = {
          id: targetConvId,
          conversationId: targetConvId,
          buyerId: currentUserId,
          sellerId,
          productId,
          participant,
          listing,
          unreadCount: 0,
          lastUpdated: Date.now(),
          messages: []
        };

        setConversations((prev) => {
          const idx = prev.findIndex((c) => c.id === tempConvId || c.productId === productId);
          if (idx !== -1) {
            const next = [...prev];
            next[idx] = { ...next[idx], ...updatedConvObj };
            return next;
          }
          return [updatedConvObj, ...prev];
        });

        persistStoredConversations((prevStored) => {
          const idx = prevStored.findIndex((c) => c.id === tempConvId || c.id === targetConvId || (c.productId === productId && (c.sellerId === sellerId || c.buyerId === currentUserId)));
          if (idx !== -1) {
            const next = [...prevStored];
            next[idx] = { ...next[idx], ...updatedConvObj, messages: next[idx].messages || [] };
            return next;
          }
          return [updatedConvObj, ...prevStored];
        });

        setActiveConversationId(targetConvId);

        // Step E: Fetch full message history from Java/MySQL
        await fetchMessagesForConversation(targetConvId);
        setBackendConnected(true);
        setApiError(null);
      }

    } catch (err) {
      console.warn('[Uni-Gram Chat] Backend sync skipped or unavailable:', err);
      setBackendConnected(false);
      setApiError(`Uni-Gram Java Backend offline (http://127.0.0.1:8080). Launch Main.java (Option 15) to enable MySQL persistence.`);
    } finally {
      setLoading(false);
    }
  };

  const openConversation = (convId) => {
    setActiveConversationId(convId);
    markAsRead(convId);
    setIsOpen(true);
    setIsMinimized(false);
    fetchMessagesForConversation(convId);
  };

  const openInbox = () => {
    setIsOpen(true);
    setIsMinimized(false);
    loadConversationsFromBackend();
  };

  const closeChat = () => {
    setIsOpen(false);
    setIsMinimized(false);
    setApiError(null);
  };

  const toggleChat = () => {
    setIsOpen((prev) => {
      const next = !prev;
      if (next && currentUserId) {
        loadConversationsFromBackend();
      }
      return next;
    });
    setIsMinimized(false);
    setApiError(null);
  };

  const toggleMinimize = () => {
    setIsMinimized((prev) => !prev);
  };

  const markAsRead = (convId) => {
    setConversations((prev) =>
      prev.map((c) => {
        if (c.id === convId && c.unreadCount > 0) {
          return { ...c, unreadCount: 0 };
        }
        return c;
      })
    );
  };

  /**
   * Dispatches a message to the active conversation via Java HTTP API.
   * Persists to MySQL when connected; displays with offline status when backend is down.
   */
  const sendMessage = async (text) => {
    if (!activeConversationId || !text || !text.trim()) {
      return;
    }

    if (!currentUserId) {
      setApiError('Please sign in to send messages.');
      return;
    }

    const cleanText = text.trim();
    setSending(true);
    setApiError(null);

    let targetConvId = activeConversationId;
    const activeConv = activeConversation;
    const otherName = activeConv?.participant?.name;

    // If conversation is still in temporary state, auto-create on backend first
    if (targetConvId.startsWith('temp_') || targetConvId.includes('_student_')) {
      if (activeConv) {
        try {
          const convRes = await unigramApi.createConversation({
            buyerId: currentUserId,
            sellerId: activeConv.sellerId,
            productId: activeConv.productId
          });
          const javaConv = convRes?.data;
          const realId = javaConv?.conversationId || javaConv?.id || javaConv?.conversation_id;
          if (realId) {
            targetConvId = realId;
            setConversations((prev) =>
              prev.map((c) => (c.id === activeConversationId ? { ...c, id: realId, conversationId: realId } : c))
            );
            setActiveConversationId(realId);
          }
        } catch (_e) {
          // Continue to fallback if offline
        }
      }
    }

    try {
      const res = await unigramApi.sendMessage(targetConvId, currentUserId, cleanText);
      const javaMsg = res.data;

      // Transform persisted Java message into UI message
      const uiMsg = mapJavaMessageToUi(javaMsg, currentUserId, otherName);

      setConversations((prev) =>
        prev.map((c) => {
          if (c.id === targetConvId || c.conversationId === targetConvId) {
            const exists = c.messages.some((m) => m.id === uiMsg.id);
            const nextMessages = exists ? c.messages : [...c.messages, uiMsg];
            return {
              ...c,
              lastUpdated: Date.now(),
              messages: nextMessages
            };
          }
          return c;
        })
      );

      persistStoredConversations(currentUserId, (prevStored) => {
        let found = false;
        const next = prevStored.map((c) => {
          if (c.id === targetConvId || c.conversationId === targetConvId) {
            found = true;
            const exists = (c.messages || []).some((m) => m.id === uiMsg.id);
            const nextMessages = exists ? c.messages : [...(c.messages || []), uiMsg];
            return {
              ...c,
              lastUpdated: Date.now(),
              messages: nextMessages
            };
          }
          return c;
        });
        if (!found && activeConv) {
          next.push({
            ...activeConv,
            id: targetConvId,
            conversationId: targetConvId,
            lastUpdated: Date.now(),
            messages: [uiMsg]
          });
        }
        return next;
      });

      setBackendConnected(true);
      // Trigger background sync to refresh conversation listings and snippet
      loadConversationsFromBackend();

    } catch (err) {
      console.warn('[Uni-Gram Chat] Send message failed, applying local fallback:', err);
      const localMsg = {
        id: 'msg_local_' + Date.now(),
        sender: 'user',
        senderId: currentUserId,
        senderName: user?.name || 'You',
        timestamp: formatTime(new Date().toISOString()),
        date: 'Today',
        type: 'text',
        text: cleanText,
        readReceipt: 'Delivered',
        rawSentAt: new Date().toISOString(),
        reactions: []
      };

      setConversations((prev) =>
        prev.map((c) => {
          if (c.id === targetConvId || c.conversationId === targetConvId) {
            return {
              ...c,
              lastUpdated: Date.now(),
              messages: [...(c.messages || []), localMsg]
            };
          }
          return c;
        })
      );

      persistStoredConversations(currentUserId, (prevStored) => {
        let found = false;
        const next = prevStored.map((c) => {
          if (c.id === targetConvId || c.conversationId === targetConvId) {
            found = true;
            return {
              ...c,
              lastUpdated: Date.now(),
              messages: [...(c.messages || []), localMsg]
            };
          }
          return c;
        });
        if (!found && activeConv) {
          next.push({
            ...activeConv,
            id: targetConvId,
            conversationId: targetConvId,
            lastUpdated: Date.now(),
            messages: [localMsg]
          });
        }
        return next;
      });

      setBackendConnected(false);
      setApiError('Java backend offline (http://127.0.0.1:8080). Message queued locally; start Main.java (Option 15) to persist to MySQL.');
    } finally {
      setSending(false);
    }
  };

  const toggleReaction = (messageId, emoji) => {
    if (!activeConversationId) return;

    setConversations((prev) =>
      prev.map((c) => {
        if (c.id === activeConversationId) {
          return {
            ...c,
            messages: c.messages.map((m) => {
              if (m.id === messageId) {
                const current = m.reactions || [];
                const hasEmoji = current.includes(emoji);
                const next = hasEmoji ? current.filter((e) => e !== emoji) : [...current, emoji];
                return { ...m, reactions: next };
              }
              return m;
            })
          };
        }
        return c;
      })
    );
  };

  const clearAllChats = () => {
    setConversations([]);
    setActiveConversationId(null);
    setIsOpen(false);
  };

  return (
    <ChatContext.Provider
      value={{
        conversations,
        activeConversation,
        activeConversationId,
        isOpen,
        isMinimized,
        unreadTotal,
        loading,
        sending,
        apiError,
        backendConnected,
        openChatWithItem,
        openConversation,
        openInbox,
        closeChat,
        toggleChat,
        toggleMinimize,
        sendMessage,
        toggleReaction,
        clearAllChats,
        refreshConversations: loadConversationsFromBackend
      }}
    >
      {children}
    </ChatContext.Provider>
  );
}

export function useChat() {
  const context = useContext(ChatContext);
  if (!context) {
    throw new Error('useChat must be used within a ChatProvider');
  }
  return context;
}

export default ChatContext;
