# Uni-Gram — Java Viva Code Guide

This guide covers every Java class in the standalone **Uni-Gram** project, how the layers interact with MySQL, and 30 viva questions with concise answers.

---

## 1. Retained Java Files

| File | Package | ~Lines | Purpose & Key Methods |
|---|---|---|---|
| `Main.java` | `com.unigram` | ~225 | Terminal UI and menu loop (`run`, `printMainMenu`, `ensureDatabaseReady`, `handleViewStudents`, `handleRegisterStudent`, `handleStartConversation`, `handleViewMyConversations`, `handleOpenConversation`) |
| `User.java` | `com.unigram.model` | ~65 | Student entity (`userId`, `fullName`, `email`, validation in constructor/setters, `equals`, `hashCode`) |
| `Conversation.java` | `com.unigram.model` | ~80 | Direct 1-to-1 chat entity (`isParticipant`, `getOtherParticipantId`, overloaded constructors) |
| `Message.java` | `com.unigram.model` | ~70 | Chat message entity (`messageId`, `conversationId`, `senderId`, `content`, `sentAt`) |
| `DatabaseConfig.java` | `com.unigram.database` | ~95 | Reads `.env`, `-D` properties, and env vars (`resolveConfig`, `isConfigured`, `validate`) |
| `DatabaseConnectionFactory.java` | `com.unigram.database` | ~45 | Opens JDBC connections (`getConnection`, `sanitizeMessage`) |
| `DatabaseException.java` | `com.unigram.database` | ~15 | Checked exception wrapping SQL/connection failures |
| `UniGramException.java` | `com.unigram.exception` | ~15 | Base checked domain exception |
| `UserNotFoundException.java` | `com.unigram.exception` | ~12 | Thrown when a student ID does not exist |
| `ConversationNotFoundException.java` | `com.unigram.exception` | ~12 | Thrown when a conversation ID does not exist |
| `UnauthorizedConversationAccessException.java` | `com.unigram.exception` | ~12 | Thrown when a non-participant accesses or messages a thread |
| `InvalidMessageException.java` | `com.unigram.exception` | ~12 | Thrown when message content is empty/blank |
| `UserJdbcDao.java` | `com.unigram.repository` | ~65 | SQL operations for `users` (`insert`, `findById`, `findAll`, `existsById`, `existsByEmail`) |
| `ConversationJdbcDao.java` | `com.unigram.repository` | ~95 | SQL operations for `conversations` (`insert`, `findById`, `findByParticipant`, `findByParticipants`, `findAll`) |
| `MessageJdbcDao.java` | `com.unigram.repository` | ~75 | SQL operations for `messages` (`insert`, `findByConversationId`, `findLatestByConversationId`, `countByConversationId`) |
| `JdbcChatService.java` | `com.unigram.service` | ~160 | Business rules & transactions for students and conversations (`registerUser`, `startOrGetConversation`, `validateParticipant`) |
| `JdbcMessageService.java` | `com.unigram.service` | ~95 | Business rules & transactions for messages (`sendMessage`, `getMessageHistory`, `getLatestMessage`) |
| `StandaloneMessagingTest.java` | `com.unigram` *(test)* | ~140 | Automated MySQL verification test using an isolated `unigram_standalone_test_db` schema |

---

## 2. Java Concepts Used vs. Not Implemented

### Genuinely Used in This Application
- **OOP & Encapsulation**: Private fields with constructor validation and getters/setters in `User`, `Conversation`, `Message`.
- **Constructor Overloading & Chaining**: `Conversation(...)` and `Message(...)` chain `this(...)`.
- **Inheritance & Custom Checked Exceptions**: `UniGramException` hierarchy (`UserNotFoundException`, `ConversationNotFoundException`, `UnauthorizedConversationAccessException`, `InvalidMessageException`) plus `DatabaseException`.
- **Collections & Generics**: `List<User>`, `List<Conversation>`, `List<Message>`, `ArrayList<>`, `Optional<T>`, `Collections.unmodifiableList(...)`.
- **JDBC & PreparedStatement**: Parameterized SQL queries preventing SQL injection; `ResultSet` mapping to Java objects.
- **JDBC Transactions**: `conn.setAutoCommit(false)`, `conn.commit()`, `conn.rollback()` in `JdbcChatService.startOrGetConversation` and `JdbcMessageService.sendMessage`.
- **Try-with-Resources**: Automatic closing of `Connection`, `PreparedStatement`, `ResultSet`, `BufferedReader`, and `Scanner`.
- **Java Time API**: `LocalDateTime` and `java.sql.Timestamp` conversion.

### Not Implemented in This Standalone Build
- HTTP REST server, JSON serialization (Gson), React frontend, Google OAuth, password hashing/sessions, marketplace listings, custom `Thread`/producer-consumer demos, and Java Streams/CSV export are **intentionally excluded**. *(Only `StandaloneMessagingTest` uses `ExecutorService` to test concurrent duplicate-prevention.)*

---

## 3. Core Flows

1. **Student Registration**: `Main.handleRegisterStudent()` -> constructs `User` (validates non-empty fields) -> `JdbcChatService.registerUser()` checks `existsById` and `existsByEmail` -> `UserJdbcDao.insert()` runs `INSERT INTO users`.
2. **Conversation Creation**: `Main.handleStartConversation()` -> `JdbcChatService.startOrGetConversation(initiatorId, recipientId)` -> rejects self-conversation -> opens transaction -> verifies both students exist -> checks `findByParticipants` in both directions -> inserts new `Conversation` if absent (or catches `SQLState 23000` on concurrent insert and fetches the existing row) -> `conn.commit()`.
3. **Message Sending & Retrieval**: `Main.handleOpenConversation()` -> `JdbcMessageService.sendMessage()` validates non-empty text -> calls `chatService.validateParticipant()` -> opens transaction -> `MessageJdbcDao.insert()` -> `conn.commit()`. History retrieval runs `ORDER BY sent_at ASC` after verifying participant access.

---

## 4. Database Relationships (`unigram_standalone_db`)

- `users (user_id PK)` 1 ─── * `conversations (participant_one_id FK, participant_two_id FK)`
- `conversations (conversation_id PK)` 1 ─── * `messages (conversation_id FK)`
- `users (user_id PK)` 1 ─── * `messages (sender_id FK)`
- `conversations.direct_pair_key` (`VIRTUAL` generated column + `UNIQUE KEY`) enforces one conversation per student pair regardless of initiation order.

---

## 5. Recommended Study Order

1. `model/User.java`, `model/Conversation.java`, `model/Message.java`
2. `exception/*.java` & `database/DatabaseException.java`
3. `database/DatabaseConfig.java` & `database/DatabaseConnectionFactory.java`
4. `repository/*.java` (DAOs)
5. `service/JdbcChatService.java` & `service/JdbcMessageService.java`
6. `Main.java` & `database/schema.sql`

---

## 6. 30 Project-Specific Viva Questions & Answers

1. **What is Uni-Gram?** A standalone Java terminal campus messaging application backed by MySQL 8.0 via JDBC.
2. **Why is the code split into `model`, `repository`, `service`, and `database` packages?** Separation of concerns: models hold data, DAOs execute SQL, services enforce business rules/transactions, and `Main` handles user I/O.
3. **Why use `PreparedStatement` instead of `Statement`?** It precompiles SQL and binds parameters safely, preventing SQL injection.
4. **How do you prevent resource leaks with JDBC?** Every `Connection`, `PreparedStatement`, and `ResultSet` is opened inside a `try (...)` (try-with-resources) block.
5. **Why is `userId` `final` in `User`?** A student's primary key identity should not change after object creation.
6. **How does `Conversation` validate participants?** Its constructor rejects null/empty IDs and throws `IllegalArgumentException` if `participantOneId` equals `participantTwoId` (case-insensitive).
7. **How does `Conversation.isParticipant` work?** It compares `studentId.trim()` case-insensitively against both `participantOneId` and `participantTwoId`.
8. **How do you prevent duplicate conversations between A and B?** Both in Java (`findByParticipants` checking `(A,B)` or `(B,A)`) and in MySQL (`direct_pair_key` virtual generated column with a `UNIQUE` index).
9. **What happens if Student A and Student B start a conversation at the exact same time?** One transaction commits; the second hits MySQL `SQLState 23000`, rolls back, and returns the existing conversation via `findByParticipants`.
10. **Why use a `VIRTUAL` generated column in MySQL instead of `STORED`?** MySQL 8.0 InnoDB does not allow `STORED` generated columns on columns referenced by `ON UPDATE CASCADE` foreign keys, whereas `VIRTUAL` works cleanly with `UNIQUE KEY`.
11. **What does `conn.setAutoCommit(false)` do?** It starts an explicit database transaction so inserts are only saved when `conn.commit()` runs.
12. **When is `conn.rollback()` called?** Whenever a `SQLException` or domain exception occurs inside a transaction block.
13. **Why is `conn.setAutoCommit(true)` in a `finally` block?** To restore the connection's default state before closing it.
14. **Why does `JdbcMessageService.sendMessage` return `Message` only after `conn.commit()`?** So the UI never reports a message as sent unless it is durably saved in MySQL.
15. **How are messages kept in chronological order?** `MessageJdbcDao.findByConversationId` uses `ORDER BY sent_at ASC` backed by `INDEX idx_msg_conv_chronological (conversation_id, sent_at ASC)`.
16. **How do you block non-participants from reading a chat?** `JdbcMessageService` calls `chatService.validateParticipant(conversationId, studentId)`, which throws `UnauthorizedConversationAccessException` if `isParticipant` is false.
17. **What happens when an empty or whitespace-only message is sent?** `JdbcMessageService.sendMessage` throws `InvalidMessageException` before opening a database connection.
18. **Why use `Optional<T>` in DAO `findById` methods?** It explicitly represents "found or not found" without returning `null`.
19. **Why return `Collections.unmodifiableList(history)` from `getMessageHistory`?** To prevent callers from accidentally mutating the returned message list.
20. **How does `DatabaseConfig` protect passwords?** Passwords are read from `.env` (gitignored) or environment variables, never hardcoded, and `DatabaseConnectionFactory.sanitizeMessage` masks them in error strings.
21. **What is the priority order in `DatabaseConfig`?** 1) `.env` file in the project root, 2) Java `-D` system properties, 3) OS environment variables.
22. **Why is `DriverManager.setLoginTimeout(3)` used?** So a stopped MySQL server fails fast in 3 seconds instead of hanging.
23. **Why does `UniGramException` extend `Exception` instead of `RuntimeException`?** To make domain errors checked exceptions so callers (`Main`) are forced by the compiler to handle them cleanly.
24. **What does `ON DELETE CASCADE` on `messages.conversation_id` do?** Deleting a conversation automatically removes its child messages without orphan rows.
25. **What does `ON DELETE RESTRICT` on `conversations` foreign keys do?** It prevents deleting a student while they still have active conversations.
26. **Why use `DATETIME(3)` in MySQL?** It stores millisecond precision so messages sent within the same second still sort accurately.
27. **How does `View My Conversations` show the other student's name?** It calls `conv.getOtherParticipantId(studentId)` and looks up that student's `User` record.
28. **Why does `Main` use `switch` expressions (`case "1" -> ...`)?** Available since Java 14+, it eliminates fall-through bugs (`break` omissions) and keeps the menu dispatch concise.
29. **What happens if `.env` is missing when the app starts?** `ensureDatabaseReady()` catches `isConfigured() == false` and prints a clear setup message without crashing.
30. **How could UniMart marketplace integration be added later?** By adding an optional `productId` reference to `Conversation` and linking a `listings` service without changing the core messaging tables or DAOs.
