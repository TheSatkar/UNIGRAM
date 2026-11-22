# UniMart + Uni-Gram — Essential Viva Code Guide

Welcome to the essential guide for your 2nd-year college Java and Full-Stack project viva.
This document explains the architecture and critical files you need to know.

---

## 🗺️ Recommended Learning Order

To understand this project without reading tens of thousands of lines, read in this exact order:

1. **The Big Picture**: How React (Port 5173), Java HTTP API (Port 8080), and MySQL (Port 3306) talk to each other.
2. **Java Entry Point & Server**: `src/main/java/com/unigram/Main.java` & `UniGramHttpServer.java`.
3. **HTTP Routing & API**: `src/main/java/com/unigram/api/ApiRouter.java`.
4. **Database & Persistence**: `DatabaseConfig.java` & `DatabaseConnectionFactory.java`.
5. **Repositories (JDBC)**: `UserJdbcDao.java`, `ListingJdbcDao.java`, `ConversationJdbcDao.java`, `MessageJdbcDao.java`.
6. **Security & Session**: `SessionManager.java` & `PasswordHasher.java`.
7. **Frontend Core**: `main.jsx`, `App.jsx`, and `apiClient.js`.
8. **Frontend State & UI**: `AuthContext.jsx`, `ChatContext.jsx`, `Dashboard.jsx`, `AdminPage.jsx`.
9. **Academic Syllabus Modules**: Threading, Locks, Producer-Consumer, and Java Streams.

---

## 1. System Architecture Overview

```
 [Browser (React 19 + Vite)]
      │
      │ HTTP REST JSON (Bearer Session Tokens, CORS, X-Require-Durable)
      ▼
 [Java 21 HTTP Server (HttpServer on port 8080)]
      │
      ├─► ApiRouter.java (Routes requests, validates sessions & input)
      ├─► SessionManager.java (In-memory 24h Bearer tokens)
      ├─► PasswordHasher.java (PBKDF2 with SHA-256 & Salt)
      │
      ▼
 [JDBC DAO Layer (Raw JDBC - No ORM)]
      │
      ├─► UserJdbcDao (users table)
      ├─► ListingJdbcDao (listings, listing_photos, listing_pending_edits)
      ├─► ConversationJdbcDao (conversations table)
      ├─► MessageJdbcDao (messages table)
      └─► WishlistJdbcDao (wishlists table)
      │
      ▼
 [MySQL Database (unigram_db)]
```

---

## 2. React Frontend Entry & Core Architecture

### `unimart-frontend/src/main.jsx`
- **Purpose**: Frontend bootstrap file. Mounts React into `<div id="root">`.
- **Key Wrappers**:
  - `<Provider store={store}>`: Redux store wrapper.
  - `<BrowserRouter>`: Client-side routing.
  - `<AuthProvider>`: User authentication state.
  - `<ThemeProvider>`: Campus Dark/Light mode theme.
  - `<ChatProvider>`: Live messaging and chat modals.

### `unimart-frontend/src/App.jsx`
- **Purpose**: Defines top-level routes.
- **Routes**:
  - `/`: Student Marketplace Dashboard (`Dashboard.jsx`).
  - `/login`: Student & Admin Authentication Gateway (`LoginPage.jsx`).
  - `/admin`: Campus Moderation Panel (`AdminPage.jsx` — Protected, admin only).
  - `*`: 404 page (`NotFound.jsx`).

### `unimart-frontend/src/api/apiClient.js`
- **Purpose**: Centralized HTTP client for all backend communication.
- **Key Methods**:
  - `apiRequest(endpoint, options)`: Automatically attaches `Authorization: Bearer <token>`, sets `Accept: application/json`, serializes request bodies, and injects `X-Require-Durable: true` when running in Integrated Mode.
  - `getStoredToken()` / `setStoredToken(token)`: Manages the session token in browser `localStorage`.
- **Viva Point**: If professors ask *"How does React communicate with Java?"*, answer: *"Through `apiClient.js`, which sends JSON fetch requests to `http://127.0.0.1:8080` with Bearer tokens."*

---

## 3. Important UI Components

### `Dashboard.jsx` & `ListingGrid.jsx`
- **Purpose**: Displays the marketplace catalog with category filter tabs, search bar, and price sorting.
- **Key Features**:
  - Filters listings by approval status (students only see approved listings; sellers see their own pending items).
  - Opens the `SellListingModal` to create or edit listings.
  - Connects to `ChatContext` when clicking "Message Seller".

### `LoginPage.jsx`
- **Purpose**: Unified login and registration gateway.
- **Key Features**:
  - Enforces `@chitkara.edu.in` university email domain constraints.
  - Quick-Fill buttons for testing (Student A, Student B, Campus Admin).
  - Secure password reset with 6-digit OTP verification.
  - Optional Google Sign-In with cryptographic Google ID Token exchange.

### `AdminPage.jsx`
- **Purpose**: Moderation portal for campus staff.
- **Key Features**:
  - Moderation Queue showing pending items and pending edits.
  - Visual diff modal (`AdminEditDiffViewer.jsx`) showing proposed changes before approval.
  - Approve and Reject actions backed by backend authorization checks.

### `ChatModal.jsx` & `ChatContext.jsx`
- **Purpose**: Real-time buyer-seller messaging modal.
- **Key Features**:
  - Shows conversations grouped by student and item.
  - Fetches message history chronologically from `/api/conversations/{id}/messages`.
  - Sends messages and updates conversation previews.

---

## 4. Java HTTP Backend Entry Point

### `src/main/java/com/unigram/Main.java`
- **Purpose**: Main executable class for Uni-Gram.
- **Two Execution Modes**:
  1. **Server Mode (`--server`)**: Runs `UniGramHttpServer` on port 8080 as a background daemon for the React website (used by `run-demo.bat`).
  2. **Console Menu Mode (default)**: Interactive console menu (Options 1–15) demonstrating threading, collections, JDBC, bounded queues, and Streams for professor viva grading.

### `src/main/java/com/unigram/api/UniGramHttpServer.java`
- **Purpose**: Lightweight HTTP server built using Java's built-in `com.sun.net.httpserver.HttpServer`.
- **Key Points**:
  - Binds to port `8080` (or `PORT` environment variable).
  - Uses an `ExecutorService` thread pool (10 worker threads) to handle multiple HTTP requests concurrently.
  - Dispatches all incoming requests under `/api/` to `ApiRouter`.

---

## 5. Java HTTP Router (`ApiRouter.java`)

### `src/main/java/com/unigram/api/ApiRouter.java`
- **Purpose**: Main controller that routes HTTP endpoints, handles CORS, validates tokens, and runs database transactions.
- **Key Helper Methods**:
  - `authenticateRequest(HttpExchange)`: Extracts `Authorization: Bearer <token>` and returns the active `UserSession`.
  - `checkDurableWrite(HttpExchange)`: If `X-Require-Durable: true` is sent and MySQL is down, immediately responds with HTTP 503 so that data is never silently lost.
  - `executeTx(HttpExchange, actionDesc, txConsumer)`: Standardized JDBC transaction wrapper that disables auto-commit, commits upon success, and rolls back on errors.
- **Core Endpoints Handled**:
  - `GET /api/health`: Health status and persistence mode (`CONNECTED` or `IN_MEMORY_PERSISTENCE`).
  - `POST /api/auth/login`: Verifies credentials and generates a 24-hour Bearer session token.
  - `POST /api/auth/register`: Validates university email domain and hashes passwords.
  - `GET /api/listings`: Lists listings matching current user permissions.
  - `POST /api/listings`: Creates a new listing with status `pending`.
  - `POST /api/listings/{id}/approve`: Approves a listing (Admin only).
  - `POST /api/listings/{id}/reject`: Rejects a listing (Admin only).
  - `GET /api/conversations`: Fetches user's conversations.
  - `POST /api/conversations/{id}/messages`: Appends a new message.

---

## 6. Authentication & Security

### `src/main/java/com/unigram/security/PasswordHasher.java`
- **Purpose**: Cryptographic password hashing.
- **Implementation**: Uses `PBKDF2WithHmacSHA256` with 65,536 iterations and a random 16-byte cryptographic salt. Passwords are never stored in plaintext.

### `src/main/java/com/unigram/security/SessionManager.java`
- **Purpose**: Generates and manages Bearer session tokens.
- **Implementation**:
  - Tokens are secure random 32-byte hexadecimal strings.
  - Stored in a thread-safe `ConcurrentHashMap` with 24-hour expiration.
  - Supports token revocation on explicit logout or password change.

---

## 7. Marketplace Workflow & Listing Moderation

### Listing Lifecycle:
1. **Creation**:
   - Student creates item via `POST /api/listings`.
   - Saved to MySQL `listings` table with `status = 'pending'`, `pending_type = 'new'`.
   - Invisible to other students; visible only to creator and admin.
2. **Approval**:
   - Admin approves item via `POST /api/listings/{id}/approve`.
   - `status` transitions to `'approved'`, `pending_type = null`.
   - Item appears immediately on everyone's dashboard.
3. **Price-Only Edit**:
   - Creator edits only the price.
   - Updated immediately in `listings` table without requiring admin approval.
4. **Non-Price Edit**:
   - Creator edits title, description, or photos.
   - Live listing remains visible to students with its last approved details.
   - Proposed changes are stored in `listing_pending_edits` table.
   - Admin reviews the diff in the Admin Panel and clicks Approve or Reject.

---

## 8. Database & Persistence Layer (JDBC)

### Database Configuration:
- `src/main/java/com/unigram/database/DatabaseConfig.java`: Reads `UNIGRAM_DB_URL`, `UNIGRAM_DB_USER`, `UNIGRAM_DB_PASSWORD` (defaults to `jdbc:mysql://localhost:3306/unigram_db`).
- `src/main/java/com/unigram/database/DatabaseConnectionFactory.java`: Creates raw JDBC connections using `DriverManager`.

### Key Tables & DAOs:
- `users` (`UserJdbcDao.java`): User accounts, hashed passwords, roles (`student` vs `admin`).
- `listings` (`ListingJdbcDao.java`): Item title, price, condition, status (`pending`, `approved`, `sold`, `rejected`).
- `listing_photos` (`ListingJdbcDao.java`): Photo URLs linked by `listing_id`.
- `listing_pending_edits` (`ListingJdbcDao.java`): JSON payload of unapproved edits.
- `conversations` (`ConversationJdbcDao.java`): Chats between student buyers and sellers.
- `messages` (`MessageJdbcDao.java`): Chronological chat messages.
- `wishlists` (`WishlistJdbcDao.java`): User-saved listing bookmarks.

---

## 9. Academic Java Syllabus Concepts Demonstrated

| Topic | Java Class | How It Is Demonstrated |
| :--- | :--- | :--- |
| **Object-Oriented Programming (OOP)** | `com.unigram.model.*` | Encapsulation with private fields, getters/setters, entity immutability. |
| **Custom Exception Hierarchy** | `com.unigram.exception.*` | Checked and unchecked exceptions (`UniGramException`, `UserNotFoundException`, `ConversationNotFoundException`). |
| **Multithreading Lifecycle** | `ThreadLifecycleDemo.java` | Demonstrates thread states (`NEW`, `RUNNABLE`, `TIMED_WAITING`, `TERMINATED`). |
| **Thread Synchronization** | `SynchronizationRaceDemo.java` | Synchronized blocks preventing race conditions during concurrent updates. |
| **Producer-Consumer Pattern** | `ProducerConsumerDemo.java` | Custom bounded buffer (`MessageQueue`) using low-level `wait()` and `notifyAll()`. |
| **Raw JDBC & Transactions** | `ListingJdbcDao.java`, `ApiRouter.java` | Direct SQL with `PreparedStatement`, `setAutoCommit(false)`, `commit()`, and `rollback()`. |
| **Java Streams API** | `StreamApiDemo.java`, `ApiRouter.java` | Functional filtering, sorting, mapping, and collectors (`.stream().filter(...).toList()`). |
| **ExecutorService & ThreadPool** | `UniGramHttpServer.java`, `ExecutorServiceBasicsDemo.java` | Fixed thread pool concurrency handling simultaneous HTTP requests. |

---

## 10. How to Run and Demonstrate the Project

1. **Start the Application**:
   - Double-click `run-demo.bat`.
   - The launcher will check JDK & Node, compile Java sources, boot `com.unigram.Main --server` on port 8080, check MySQL connection, start Vite on port 5173, and launch your browser.
2. **Presenting the Live Website**:
   - Log in as **Student A** (`satkar0696.becse25@chitkara.edu.in` / `Student@1234`).
   - Create a listing -> shows as **Pending Approval**.
   - Log in as **Campus Admin** in an incognito tab (`admin@chitkara.edu.in` / `Admin@1234`).
   - Open `/admin` and click **Approve**.
   - Log in as **Student B** (`priya.k@chitkara.edu.in` / `Student@1234`), view the listing, add to Wishlist, and click **Message Seller** to start a chat.
3. **Presenting the Academic Console Menu**:
   - Open a terminal and run:
     ```powershell
     java -cp "bin;lib/*" com.unigram.Main
     ```
   - Menu options 1–15 allow you to run and demonstrate any individual syllabus concept on command for the examiner.
