# Uni-Gram & UniMart: Integration Architecture & Implementation Guide (Phase 11 Complete)

This technical specification details the integration architecture between the **Uni-Gram Core Java backend** and the **UniMart React marketplace frontend**, successfully implemented and operational in **Phase 11**.

---

## 1. Architectural Overview & Boundaries

```
┌──────────────────────────────────────────────────────────┐
│                   UniMart React Frontend                 │
│   (ChatModal, FloatingChatButton, Listing Chat Buttons)   │
└────────────────────────────┬─────────────────────────────┘
                             │
                      HTTP / REST (JSON)
                      CORS: localhost:5173 / localhost:3000
                             │
                             ▼
┌──────────────────────────────────────────────────────────┐
│             Uni-Gram Java HTTP API Server                │
│             (com.sun.net.httpserver.HttpServer)          │
│             Port: 8080 (Loopback 127.0.0.1)              │
├──────────────────────────────────────────────────────────┤
│ - ApiRouter & JsonUtil (Google Gson 2.11.0)              │
│ - ExecutorService Thread Pool (10 Concurrent Workers)    │
│ - 64 KB Payload Limit Enforcement                        │
├──────────────────────────────────────────────────────────┤
│ - JdbcChatService & JdbcMessageService                   │
│ - Independent Connection-Per-Request JDBC Hygiene        │
└────────────────────────────┬─────────────────────────────┘
                             │
                        MySQL JDBC
                             ▼
┌──────────────────────────────────────────────────────────┐
│                     MySQL Server 8.0                     │
│                  Database: unigram_db                    │
│   (users, product_references, conversations, messages)   │
└──────────────────────────────────────────────────────────┘
```

Uni-Gram operates as an independent, decoupled messaging service. It does not replicate UniMart's product catalog, order management, payment processing, or user auth credentials. It persists only:
1. Student participant references (`users`).
2. Marketplace listing references (`product_references`).
3. Relational conversation threads (`conversations`).
4. Chronological, transaction-committed message payloads (`messages`).

---

## 2. API Contract Specification

All endpoints communicate over HTTP with UTF-8 JSON. Every response conforms to a standard envelope:

```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "timestamp": "2026-11-18T20:30:00.123"
}
```

On failure:
```json
{
  "success": false,
  "data": null,
  "error": "Detailed, sanitized error description",
  "timestamp": "2026-11-18T20:30:00.123"
}
```

### Endpoints Summary

| Endpoint | Method | Headers / Query Params | Request Body | Success Code | Error Codes |
|---|---|---|---|:---:|:---:|
| `/api/health` | `GET` | — | — | `200 OK` | `503`, `405` |
| `/api/users` | `GET` | — | — | `200 OK` | `405`, `503` |
| `/api/users/{userId}` | `GET` | — | — | `200 OK` | `404`, `405`, `503` |
| `/api/products` | `GET` | — | — | `200 OK` | `405`, `503` |
| `/api/conversations` | `GET` | `?participantId={userId}` | — | `200 OK` | `404`, `405`, `503` |
| `/api/conversations/{id}` | `GET` | — | — | `200 OK` | `404`, `405`, `503` |
| `/api/conversations` | `POST` | `Content-Type: application/json` | `{"buyerId":"...","sellerId":"...","productId":"..."}` | `201 Created` | `400`, `404`, `409`, `503` |
| `/api/conversations/{id}/messages` | `GET` | `X-Dev-User-Id` *(optional)* | — | `200 OK` | `403`, `404`, `405`, `503` |
| `/api/conversations/{id}/messages` | `POST` | `Content-Type: application/json`<br>`X-Dev-User-Id: {senderId}` | `{"content":"..."}` | `201 Created` | `400`, `401`, `403`, `404`, `503` |

### Detailed Endpoint Payloads

#### 1. Server Health Check
- **Request**: `GET /api/health`
- **Response** (`200 OK`):
  ```json
  {
    "success": true,
    "data": {
      "status": "UP",
      "database": "CONNECTED",
      "apiVersion": "1.0.0"
    },
    "timestamp": "2026-11-18T20:30:00"
  }
  ```
- **Response** (`503 Service Unavailable` if MySQL is down):
  ```json
  {
    "success": false,
    "data": {
      "status": "DEGRADED",
      "database": "DISCONNECTED",
      "apiVersion": "1.0.0"
    },
    "error": "Database service unavailable",
    "timestamp": "2026-11-18T20:30:00"
  }
  ```

#### 2. Create Conversation
- **Request**: `POST /api/conversations`
- **Headers**: `Content-Type: application/json`
- **Body**:
  ```json
  {
    "buyerId": "usr_student_A",
    "sellerId": "usr_student_B",
    "productId": "prod_calc_ti84"
  }
  ```
- **Response** (`201 Created`):
  ```json
  {
    "success": true,
    "data": {
      "conversationId": "conv_a8f9b2c1",
      "buyerId": "usr_student_A",
      "sellerId": "usr_student_B",
      "productId": "prod_calc_ti84",
      "createdAt": "2026-11-18T20:30:00"
    },
    "timestamp": "2026-11-18T20:30:00"
  }
  ```
- **Error Responses**:
  - `400 Bad Request`: Missing fields or self-conversation (`buyerId.equals(sellerId)`).
  - `404 Not Found`: `buyerId`, `sellerId`, or `productId` does not exist in Uni-Gram database.
  - `409 Conflict`: A conversation thread already exists between these users for this product.

#### 3. Send Message
- **Request**: `POST /api/conversations/{conversationId}/messages`
- **Headers**:
  - `Content-Type: application/json`
  - `X-Dev-User-Id: usr_student_A`
- **Body**:
  ```json
  {
    "content": "Hi Maya, is the graphing calculator still available?"
  }
  ```
- **Response** (`201 Created`):
  ```json
  {
    "success": true,
    "data": {
      "messageId": "msg_4e1a90b2",
      "conversationId": "conv_a8f9b2c1",
      "senderId": "usr_student_A",
      "content": "Hi Maya, is the graphing calculator still available?",
      "sentAt": "2026-11-18T20:30:05.123"
    },
    "timestamp": "2026-11-18T20:30:05"
  }
  ```
- **Error Responses**:
  - `400 Bad Request`: `content` is null, blank, or exceeds max character length.
  - `401 Unauthorized`: `X-Dev-User-Id` header is missing or empty.
  - `403 Forbidden`: Sender is not an authorized participant (neither buyer nor seller) of this conversation.
  - `404 Not Found`: Conversation ID does not exist.
  - `413 Payload Too Large`: Request payload exceeds 64 KB.

#### 4. Retrieve Message History
- **Request**: `GET /api/conversations/{conversationId}/messages`
- **Headers**: `X-Dev-User-Id: usr_student_A` *(optional for dev; required when enforcing participant privacy)*
- **Response** (`200 OK`):
  ```json
  {
    "success": true,
    "data": [
      {
        "messageId": "msg_4e1a90b2",
        "conversationId": "conv_a8f9b2c1",
        "senderId": "usr_student_A",
        "content": "Hi Maya, is the graphing calculator still available?",
        "sentAt": "2026-11-18T20:30:05"
      },
      {
        "messageId": "msg_8f2b31c4",
        "conversationId": "conv_a8f9b2c1",
        "senderId": "usr_student_B",
        "content": "Yes Alex, it is! Works perfectly.",
        "sentAt": "2026-11-18T20:31:12"
      }
    ],
    "timestamp": "2026-11-18T20:32:00"
  }
  ```

---

## 3. React Integration Mapping

The expected UI components in the UniMart React client map directly to the Java HTTP API endpoints:

```
UniMart React UI Component                       Uni-Gram Java HTTP Backend
─────────────────────────────────────────────────────────────────────────────
1. Product Listing Detail Page
   └── [ Chat with Seller ] Button ─────────► POST /api/conversations
                                              { buyerId, sellerId, productId }
                                              (If 409 Conflict, open existing conversation)

2. FloatingChatButton / Chat Drawer
   └── Conversation Threads List ───────────► GET /api/conversations?participantId={userId}

3. ChatModal (Active Thread)
   ├── Message History Display ─────────────► GET /api/conversations/{id}/messages
   │   (Polling interval e.g. 3-5 sec)
   │
   └── Send Message Form [ Send ] ──────────► POST /api/conversations/{id}/messages
                                              Headers: X-Dev-User-Id: {currentUser}
                                              Body: { content: text }

4. UI State Management
   ├── Loading Spinner ─────────────────────► Triggered while fetch() Promise is pending
   ├── Error Banner / Toast ────────────────► Populated from response.error when success=false
   └── Offline Fallback Notice ─────────────► Triggered if GET /api/health returns 503 or fails
```

### Conflict Resolution for Conversation Initiation
When a student clicks "Chat with Seller" on a listing:
1. The frontend dispatches `POST /api/conversations` with `buyerId`, `sellerId`, `productId`.
2. If status is `201 Created`, the modal opens with the new empty conversation thread.
3. If status is `409 Conflict` (conversation already exists between these users for this listing), the frontend queries `GET /api/conversations?participantId={buyerId}`, locates the matching conversation ID, and directly opens the existing thread with full message history.

---

## 4. User Identity Integration Strategy

### Current Development State (`X-Dev-User-Id`)
In Phase 9, identity is passed via the `X-Dev-User-Id` HTTP header. 
- **Purpose**: Provides simple decoupling so the Java backend can be tested via `curl`, PowerShell `Invoke-RestMethod`, and automated test scripts without requiring complex OAuth/JWT mocks.
- **Security Boundary**: `X-Dev-User-Id` is purely a **development identity mechanism**, NOT authentication. It can be forged by any client and must never be exposed to public networks.

### Phase 11 Implementation Summary
The integration was completed with the following verified components:
1. **Frontend Repository Structure**: Cloned the complete UniMart main branch into `unimart-frontend/` (React 19.2.8, Vite 8.2.2, Redux Toolkit, React Router 7).
2. **Centralized API Client**: Created `unimart-frontend/src/services/unigramApi.js` targeting `VITE_UNIGRAM_API_BASE_URL` (`http://127.0.0.1:8080`) configured via `.env`.
3. **Idempotent User & Product Sync**:
   - `POST /api/users` synchronizes UniMart `AuthContext` users (`id`, `name`, `email`) into Uni-Gram's `users` table.
   - `POST /api/products` synchronizes UniMart marketplace listings (`id`, `title`) into `product_references`.
4. **Conversation Management & Duplicate Handling**:
   - `POST /api/conversations` creates a one-to-one thread between buyer, seller, and product.
   - If the thread already exists, the server responds with `409 Conflict` and embeds the existing `Conversation` in the `data` field, allowing the UI to open the existing conversation seamlessly.
5. **Direct Persistence**: Messages sent from `ChatModal.jsx` are persisted to MySQL with `X-Dev-User-Id` header validation.
6. **Adaptive Polling**: When the chat modal is open, messages poll at a 3.5s interval; polling halts immediately when the modal is closed or unmounted.

---

## 5. Product References Mapping

- **UniMart Role**: UniMart manages listings, inventory, pricing, item condition, descriptions, and pictures.
- **Uni-Gram Role**: Uni-Gram stores only lightweight references:
  - `product_id`: Matching UniMart's listing identifier (e.g. `prod_calc_ti84`).
  - `product_title`: Snapshot listing name (e.g. `TI-84 Plus Graphing Calculator`).
- **No Catalog Duplication**: Conversations only reference the `product_id`. UniMart frontend components pass the listing ID when opening a conversation; Uni-Gram validates foreign keys against `product_references` and maintains the thread context.

---

## 6. Environment & Development Configuration

### Environment Variables
Configure these variables in your terminal or development environment:

```powershell
# MySQL Database Connection (Production Application)
$env:UNIGRAM_DB_URL = "jdbc:mysql://localhost:3306/unigram_db?serverTimezone=UTC"
$env:UNIGRAM_DB_USER = "unigram_app"
$env:UNIGRAM_DB_PASSWORD = "your_secure_password"

# MySQL Test Database (Automated Test Suites)
$env:UNIGRAM_TEST_DB_URL = "jdbc:mysql://localhost:3306/unigram_test_db?serverTimezone=UTC"
$env:UNIGRAM_TEST_DB_USER = "unigram_app"
$env:UNIGRAM_TEST_DB_PASSWORD = "your_secure_password"

# HTTP API Configuration
$env:UNIGRAM_API_PORT = "8080"
$env:UNIGRAM_CORS_ORIGINS = "http://localhost:5173,http://localhost:3000"
```

### Local Development Startup Sequence
1. Ensure MySQL Server 8.0 is running on port 3306.
2. Initialize schemas if not already present:
   - `docs/database/schema.sql` $\rightarrow$ `unigram_db`
   - `docs/database/schema_test.sql` $\rightarrow$ `unigram_test_db`
3. Launch Uni-Gram:
   ```powershell
   & "C:\Program Files\Java\jdk-26.0.1\bin\java.exe" -cp "target/classes;lib/mysql-connector-j-8.4.0.jar;lib/gson-2.11.0.jar" com.unigram.Main
   ```
4. In the console menu, select `15` $\rightarrow$ `1` to start the HTTP server on `http://127.0.0.1:8080`.
5. In Phase 11, the React frontend will point its API client (Axios or Fetch) to `http://127.0.0.1:8080/api`.

---

## 7. Current Architectural Limitations & Production Roadmap

| Dimension | Current Development Status | Production Requirement (Future Roadmap) |
|---|---|---|
| **Authentication** | `X-Dev-User-Id` header (trust-on-first-use development mock). | Cryptographic JWT tokens or session cookies validated by UniMart identity provider. |
| **Real-Time Updates** | Client HTTP polling (`GET /messages` every 3–5 seconds). | WebSocket (`ws://`) or Server-Sent Events (SSE) for push notifications. |
| **Network Binding** | Bound strictly to loopback (`127.0.0.1`). | Reverse proxy (NGINX / Caddy) with TLS / HTTPS certificates. |
| **Connection Pooling** | Direct `DriverManager` connection-per-request. | Production connection pool (e.g. HikariCP) to handle high concurrency. |
| **Data Retention** | Indefinite message storage in InnoDB tables. | Archival policy and message pagination (`?page=1&limit=50`). |

---

*This document serves as the formal architectural blueprint for Phase 11 integration.*
