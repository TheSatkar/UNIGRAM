# Uni-Gram: Student-to-Student Messaging Backend

A standalone Java backend application designed for university marketplace messaging, enabling buyers and sellers to communicate about individual marketplace listings.

---

## 📌 Project Overview

Uni-Gram provides a concurrent, transaction-safe, and decoupled messaging engine tailored for student marketplaces (such as UniMart). It is built strictly using **core Java SE** principles to demonstrate mastery of the standard Java curriculum without relying on heavy enterprise frameworks like Spring Boot or WebSockets.

### Core Domain Scenario
- **Users**: University students acting as buyers or sellers.
- **Listings**: Marketplace items (textbooks, electronics, lab gear, furniture) referenced in conversations.
- **Conversations & Messages**: One-to-one message threads tied to specific listings.
- **Concurrence & Safety**: Asynchronous message dispatching, producer-consumer queuing, bounded buffers, and thread-safe operations.
- **Data Persistence**: Direct JDBC queries, PreparedStatement hygiene, and ACID transaction boundaries.

---

## 🎓 Academic Syllabus Alignment

This project is structured specifically to showcase and demonstrate key concepts from the **Programming in Java** syllabus:

| Syllabus Area | How Uni-Gram Demonstrates It |
|---|---|
| **OOP & Exception Handling** | Strong encapsulation, inheritance hierarchies, custom domain exceptions (checked & unchecked), polymorphism. |
| **Java Collections Framework** | Usage of `List`, `Map`, `Set`, `Deque`, custom comparators, bounded capacity buffers, and thread-safe collections. |
| **Threading & Thread Lifecycle** | Worker threads, `Runnable` implementations, tracking thread states (`NEW`, `RUNNABLE`, `BLOCKED`, `WAITING`, `TIMED_WAITING`, `TERMINATED`). |
| **Synchronization & Wait/Notify** | Thread synchronization, mutex locking, and classic Producer-Consumer pattern using `wait()`, `notify()`, and `notifyAll()`. |
| **ExecutorService & Futures** | Modern concurrency with thread pools, `Callable<T>`, and `Future<T>` for asynchronous background tasks and scheduled cleanup. |
| **JDBC, PreparedStatement & CRUD** | Pure JDBC database interaction, SQL injection prevention via `PreparedStatement`, and full CRUD operations. |
| **Transactions** | Explicit transaction demarcation with `connection.setAutoCommit(false)`, `connection.commit()`, and `connection.rollback()`. |
| **Streams & Lambdas** | Declarative analytics and reporting using functional interfaces, predicates, collectors, and stream pipelines. |
| **Generics & File I/O** | Reusable generic utility wrappers (`Result<T>`), report exports to disk using Java I/O (`BufferedReader`/`BufferedWriter`). |
| **Testing & Quality** | Unit testing with JUnit 5, test fixtures, defensive coding, and structured documentation. |

---

## 📁 Package Architecture

The project follows a clean, modular architecture:

```
com.unigram
├── api          # User interface layer (Interactive CLI console & lightweight native HTTP handlers)
├── collection   # Custom bounded collections, priority queues, and collection algorithms
├── database     # Pure JDBC connection handling, schema management, and transaction controllers
├── exception    # Domain exception hierarchy (checked business exceptions & runtime failures)
├── model        # Core domain entities (User, Listing, Message, Conversation, AuditLog)
├── reporting    # Analytics & reporting engine using Java Streams and Lambda expressions
├── repository   # DAO / Repository pattern abstraction layers for data persistence
├── service      # Core business workflows, message routing, and validation logic
├── threading    # Concurrency engine (Producer-Consumer queue, wait/notify, ExecutorService)
└── util         # Reusable utilities (validation, date/time formatting, file I/O helpers)
```

---

## 🧵 Phase 4: Synchronization & Producer-Consumer Architecture

Phase 4 introduces thread synchronization and the classic Producer-Consumer design pattern tailored for the university Java syllabus and viva defense:

### Core Architectural Components
- **`MessageQueue`**: Custom thread-safe bounded buffer holding `Message` objects with fixed capacity.
- **`MessageProducer`**: Worker thread creating/receiving valid `Message` entities and enqueuing them.
- **`MessageConsumer`**: Worker thread retrieving and processing messages into a thread-safe demonstration sink.
- **Guarded Suspension (`wait()` / `notifyAll()`)**:
  - Producers wait when the queue is full (`while (queue.size() >= capacity) wait();`).
  - Consumers wait when the queue is empty (`while (queue.isEmpty()) wait();`).
  - Condition loops (`while`) prevent catastrophic failures from **spurious wakeups**.
  - `notifyAll()` awakens all waiting threads so both producers and consumers can re-evaluate conditions.
- **Safe Queue Shutdown & Draining**:
  - Calling `queue.close()` transitions the queue to closed and broadcasts `notifyAll()`.
  - Future enqueues are rejected with `IllegalStateException`.
  - Consumers drain remaining queued messages before gracefully terminating when `dequeue()` returns `null`.
- **Race Condition vs Deadlock Demonstration**:
  - `SynchronizationRaceDemo` demonstrates non-atomic incrementing (`count++`) causing lost updates, contrasted with an atomic `synchronized` counter.
  - Compares race conditions (data inconsistency under uncontrolled concurrency) vs deadlocks (permanent circular wait standstill).
- **Thread States (`BLOCKED` vs `WAITING` vs `TIMED_WAITING`)**:
  - `BLOCKED`: Thread waiting to acquire an intrinsic monitor lock held by another thread.
  - `WAITING`: Thread voluntarily relinquished the monitor lock by calling `Object.wait()` and waits indefinitely for `notifyAll()`.
  - `TIMED_WAITING`: Thread sleeping via `Thread.sleep()` or waiting with timeout.

---

## ⚡ Phase 5: ExecutorService, Callable & Future Architecture

Phase 5 introduces high-level thread management and asynchronous task execution from `java.util.concurrent`:

### Core Architectural Concepts
- **Thread Pools**:
  - `newSingleThreadExecutor()`: Sequential task execution with a single dedicated worker thread.
  - `newFixedThreadPool(n)`: Fixed number of concurrent worker threads reused from a blocking queue.
  - `newCachedThreadPool()`: Dynamically creates new threads for burst traffic and reuses idle threads (60s keepalive).
- **Task Submission (`execute` vs `submit`)**:
  - `execute(Runnable)`: Fire-and-forget execution with void return; exceptions terminate the worker unless caught.
  - `submit(Runnable)`: Returns a `Future<?>` whose `get()` method yields `null` upon completion.
  - `submit(Callable<T>)`: Returns a `Future<T>` delivering the computed value of type `T`.
- **`Callable<T>` vs `Runnable`**:
  - `Runnable.run()`: Returns `void`, cannot throw checked exceptions.
  - `Callable.call()`: Returns `V`, throws `Exception`.
- **`Future<T>` Lifecycle & Methods**:
  - `get()`: Blocking call awaiting completion.
  - `get(timeout, unit)`: Bounded wait throwing `TimeoutException` if computation exceeds the limit.
  - `isDone()`: Non-blocking poll returning `true` once completed, cancelled, or failed.
  - `cancel(mayInterruptIfRunning)`: Cancels the task; triggers `Thread.interrupt()` if running cooperatively.
  - `isCancelled()`: Returns `true` if cancelled before normal completion.
- **Exception Propagation**:
  - Exceptions thrown inside `Callable.call()` are captured by the executor and re-thrown as `ExecutionException`.
  - The original checked/unchecked exception is retrieved via `e.getCause()`.
- **Scheduling with `ScheduledExecutorService`**:
  - `schedule(task, delay, unit)`: One-shot delayed task.
  - `scheduleAtFixedRate(task, initialDelay, period, unit)`: Periodic executions started at fixed time intervals ($t_0, t_0 + p, t_0 + 2p$).
  - `scheduleWithFixedDelay(task, initialDelay, delay, unit)`: Subsequent executions start only after a fixed delay following completion of the previous run.
- **Shutdown Lifecycle**:
  - `shutdown()`: Graceful shutdown rejecting new submissions while allowing queued tasks to complete.
  - `shutdownNow()`: Forceful shutdown sending interrupts to active threads and draining unexecuted tasks.
  - `awaitTermination(timeout, unit)`: Blocks until all tasks have terminated or timeout elapses.
  - `RejectedExecutionException`: Thrown when submitting tasks to an executor that has been shut down.

---

## 🗄️ Phase 6: JDBC & MySQL Persistence Architecture

Phase 6 introduces relational database persistence using pure standard JDBC (Java Database Connectivity) with MySQL Server 8.0:

### Core Architectural Components
- **MySQL Connector/J 8.4.0 Driver**: Official MySQL JDBC Type-4 driver placed in `lib/mysql-connector-j-8.4.0.jar` and configured in `pom.xml`.
- **Relational Schema Design (`unigram_db`)**:
  - `users`: User profiles with `user_id` (PK, VARCHAR(64)), `display_name`, and unique `email`.
  - `product_references`: Marketplace listings with `product_id` (PK, VARCHAR(64)) and `product_title`.
  - `conversations`: One-to-one threads linking `buyer_id` (FK -> users), `seller_id` (FK -> users), and `product_id` (FK -> product_references) with unique composite constraint `(buyer_id, seller_id, product_id)`.
  - `messages`: Chronological message records with `message_id` (PK), `conversation_id` (FK -> conversations ON DELETE CASCADE), `sender_id` (FK -> users), `content` (TEXT), and `sent_at` (DATETIME(6)).
  - Performance index: `idx_messages_conversation_sent (conversation_id, sent_at ASC)`.
  - Character encoding: `utf8mb4` with collation `utf8mb4_unicode_ci` and `InnoDB` storage engine.
- **Connection Management & Security**:
  - `DatabaseConfig`: Secure configuration reader extracting parameters from environment variables (`UNIGRAM_DB_URL`, `UNIGRAM_DB_USER`, `UNIGRAM_DB_PASSWORD`) without hardcoding credentials.
  - `DatabaseConnectionFactory`: Encapsulates `DriverManager.getConnection()` and provides connection health checks via `conn.isValid(2)`.
  - `try-with-resources`: Ensures deterministic closure of `Connection`, `PreparedStatement`, and `ResultSet` objects, eliminating connection leaks.
- **PreparedStatement vs Statement (SQL Injection Defense)**:
  - Demonstrates how dynamic string concatenation in `Statement` allows SQL injection payloads (e.g. `' OR '1'='1`) to manipulate queries.
  - Demonstrates how parameterized `PreparedStatement` precompiles SQL templates and treats user inputs strictly as literal values.
- **ACID Transactions**:
  - Atomicity demonstrated via `conn.setAutoCommit(false)`, `conn.commit()`, and `conn.rollback()`.
  - Multi-table operations guarantee either complete persistence or total rollback upon failure (such as constraint violations).
- **Data Access Object (DAO) Layer**:
  - `UserJdbcDao`, `ProductReferenceJdbcDao`, `ConversationJdbcDao`, `MessageJdbcDao` in `com.unigram.repository` mapping relational rows into Java domain models.

### MySQL Setup & Environment Configuration
1. Initialize the database schema in MySQL:
   ```powershell
   & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p < docs/database/schema.sql
   ```
2. Optional: Seed demo data:
   ```powershell
   & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p < docs/database/seed_demo.sql
   ```
3. Set environment variables (PowerShell):
   ```powershell
   $env:UNIGRAM_DB_URL = "jdbc:mysql://localhost:3306/unigram_db?serverTimezone=UTC"
   $env:UNIGRAM_DB_USER = "root"
   $env:UNIGRAM_DB_PASSWORD = "your_mysql_password"
   ```

---

## ⚡ Phase 7: MySQL Persistence & Concurrent Messaging Integration

Phase 7 integrates relational MySQL persistence with the concurrent messaging engine and promotes the interactive console to database-backed operations:

### Core Architectural Components
- **`JdbcChatService`**:
  - Implements user registration, product reference registration, and conversation management backed by MySQL Server 8.0.
  - Enforces domain rules: self-conversation rejection (`IllegalArgumentException`), duplicate conversation prevention (`UniGramException`), and existence checks before conversation creation.
  - Enforces ACID transactions for conversation creation ensuring participant and product referential integrity.
- **`JdbcMessageService`**:
  - Validates non-blank message content and verifies sender participant authorization.
  - Persists messages inside explicit database transactions (`conn.setAutoCommit(false)`), guaranteeing that **a message is never reported as sent or successful until `conn.commit()` succeeds**.
  - Retrieves chronological history directly using SQL index ordering (`ORDER BY sent_at ASC`).
- **Concurrent Producer-Consumer Persistence Pipeline**:
  - **`MessageQueue`**: Custom bounded buffer from Phase 4 using intrinsic locks (`synchronized`) and guarded suspension (`wait()` / `notifyAll()`).
  - **`JdbcMessageProducer`**: Enqueues messages, records `QUEUED` status, and honors queue-full backpressure.
  - **`JdbcMessageConsumer`**: Dequeues messages **without holding the queue monitor lock during slow database I/O**, opens a dedicated connection per thread, and executes an atomic insert transaction.
  - **`MessagePersistenceTracker`**: Distinctly tracks states: `QUEUED` $\rightarrow$ `PERSISTED_SUCCESS` or `PERSISTENCE_FAILED` (with rollback and error recording).
  - **`ConcurrentJdbcMessageEngine`**: High-level coordinator managing queue capacity, worker threads, draining on shutdown, and termination cleanup.
- **Database Safety Guard**:
  - Normal application strictly targets `unigram_db`.
  - Automated verification suite strictly targets `unigram_test_db` and explicitly refuses execution if pointed at `unigram_db`.

---

## 💡 Phase 8: Lambda Expressions, Streams, Optional, Generics & File Handling

Phase 8 demonstrates modern functional programming, type-safe generic abstractions, and robust file I/O:

### Core Architectural Concepts
- **Functional Interfaces & SAM Concept**:
  - Built-in interfaces from `java.util.function`: `Predicate<T>`, `Function<T, R>`, `Consumer<T>`, `Supplier<T>`, `BiFunction<T, U, R>`, `BiPredicate<T, U>`.
  - Custom `@FunctionalInterface` SAM interfaces: `MessageFilter` (with default methods `and()`, `or()`, `negate()`) and `MessageFormatter`.
  - Higher-order methods accepting functional behaviors as arguments.
- **Lambda Expressions & Method References**:
  - Syntax variants: zero-parameter, single-parameter expression lambdas, and multi-parameter block lambdas.
  - Compared with anonymous inner classes: lambdas use `invokedynamic` (no disk `.class` clutter), use lexical scoping where `this` refers to enclosing instance, and require captured variables to be effectively final.
  - The 4 method reference types:
    1. Static: `MessageFormatter::formatConsole`
    2. Instance of arbitrary object: `Message::getContent`
    3. Instance of particular object: `System.out::println`
    4. Constructor: `ProductReference::new`, `ArrayList::new`
- **Stream API Operations & Collectors**:
  - Intermediate operations: `filter()`, `map()`, `sorted()`, `distinct()`, `limit()`.
  - Terminal operations: `forEach()`, `collect()`, `count()`, `reduce()`.
  - Advanced collectors: `Collectors.toList()`, `Collectors.groupingBy()`, `Collectors.partitioningBy()`, `Collectors.joining()`.
  - Sequential vs Parallel Streams: Demonstrates multi-threaded execution across `ForkJoinPool.commonPool()` and explains why parallel streams must never be used with JDBC transactions (thread-safety violations on JDBC connections).
- **Optional API & Null Defense**:
  - Creation: `Optional.empty()`, `Optional.of()`, `Optional.ofNullable()`.
  - Conditional handling: `isPresent()`, `isEmpty()`, `ifPresent()`, `ifPresentOrElse()`.
  - Fallbacks: `orElse()` (eager), `orElseGet()` (lazy supplier), `orElseThrow()`.
  - Transformations: `map()` and `flatMap()`.
  - Architectural rationale for returning `Optional<T>` from DAOs to prevent `NullPointerException`.
- **Java Generics & Variance (PECS)**:
  - Generic container: `ResultWrapper<T>` and multi-parameter `Pair<K, V>`.
  - Bounded type parameters: `<U extends Comparable<U>>`.
  - Wildcards: Upper-bounded `<? extends T>` (Producer Extends), Lower-bounded `<? super T>` (Consumer Super), Unbounded `<?>`.
  - PECS guideline: **Producer Extends, Consumer Super**.
- **Modern Date and Time API (`java.time`)**:
  - `LocalDate`, `LocalTime`, `LocalDateTime`, `Instant` (UTC epoch), and `ZonedDateTime`.
  - Calculations: `Duration` (time-based) and `Period` (date-based).
  - Formatting & parsing via `DateTimeFormatter` (ISO-8601 and custom human-readable patterns).
- **File Handling & RFC 4180 CSV Processing**:
  - Java NIO.2: `Path`, `Paths`, `Files` with explicit UTF-8 encoding.
  - Resource safety via `try-with-resources` on `BufferedReader` and `BufferedWriter`.
  - `CsvMessageHelper`: Exports messages to CSV and parses them back into `Message` domain models. Handles embedded commas, double quotes (`""`), and Unicode/emojis natively, with validation for malformed rows.

---

## 🌐 Phase 9: Standalone Java HTTP API Architecture

Phase 9 exposes the MySQL-backed messaging services over a RESTful HTTP API built exclusively on standard JDK libraries (`com.sun.net.httpserver.HttpServer`) without relying on Spring Boot, Hibernate, JPA, or WebSockets.

### 1. Architectural Highlights
- **Native JDK HTTP Server**: Built on `com.sun.net.httpserver.HttpServer` listening on `127.0.0.1:8080` by default (configurable via `UNIGRAM_API_PORT`).
- **Concurrent Request Dispatching**: Employs an `ExecutorService` fixed thread pool (`unigram-http-worker-*`) of 10 workers for concurrent request handling.
- **Strict Thread & Connection Safety**: HTTP request worker threads **never share a single JDBC Connection**. Each request invokes transactional services (`JdbcChatService`, `JdbcMessageService`), which open and close dedicated connections per operation.
- **Thin Handlers & Zero Duplication**: HTTP handlers do not duplicate business rules; all domain checks (self-conversations, non-existent users/products, duplicate conversations, sender authorization, non-blank content) remain in the service layer.
- **JSON Serialization (Gson 2.11.0)**: Uses Google Gson bundled in `lib/gson-2.11.0.jar` with a custom `LocalDateTime` ISO-8601 type adapter, HTML escaping disabled to preserve quotes/Unicode/emojis, and an enforced 64 KB request payload limit.
- **CORS Handling**: Configurable permitted origins via `UNIGRAM_CORS_ORIGINS` (defaults: `http://localhost:5173,http://localhost:3000`). Fully handles HTTP `OPTIONS` preflights (204 No Content), strictly allowing `GET, POST, OPTIONS` and `Content-Type, X-Dev-User-Id`.
- **Development Identity Context (`X-Dev-User-Id`)**: Message sending reads sender ID from request header `X-Dev-User-Id`. *(Viva note: This header is purely for local decoupling and development testing; in Phase 11, it will be mapped to authenticated UniMart session context).*
- **Clean Lifecycle & Console Integration**: Interactive console in `Main.java` provides Option 15 with sub-options to start in background, check status, and gracefully terminate with bounded in-flight request draining.

### 2. HTTP Endpoints & Status Codes

| Endpoint | Method | Headers / Query | Request Body | Status Codes | Description |
|---|---|---|---|---|---|
| `/api/health` | `GET` | — | — | 200, 503, 405 | Returns server and MySQL database connectivity status. |
| `/api/users` | `GET` | — | — | 200, 405, 503 | Lists all registered students in Uni-Gram. |
| `/api/users/{userId}` | `GET` | — | — | 200, 404, 405, 503 | Retrieves student details by user ID. |
| `/api/products` | `GET` | — | — | 200, 405, 503 | Lists marketplace product references stored in Uni-Gram. |
| `/api/conversations` | `GET` | `?participantId={userId}` | — | 200, 404, 405, 503 | Lists conversations involving student (or all if omitted). |
| `/api/conversations/{id}` | `GET` | — | — | 200, 404, 405, 503 | Retrieves conversation metadata by conversation ID. |
| `/api/conversations` | `POST` | `Content-Type: application/json` | `{"buyerId":"...","sellerId":"...","productId":"..."}` | 201, 400, 404, 409, 503 | Initiates a conversation (rejects self/duplicate). |
| `/api/conversations/{id}/messages` | `GET` | `X-Dev-User-Id` (optional) | — | 200, 403, 404, 405, 503 | Retrieves chronological messages in conversation. |
| `/api/conversations/{id}/messages` | `POST` | `Content-Type: application/json`<br>`X-Dev-User-Id: {senderId}` | `{"content":"..."}` | 201, 400, 401, 403, 404, 503 | Sends and persists message to MySQL in transaction. |

---

## 🛠️ Prerequisites & Setup

### Environment
- **Java Development Kit (JDK)**: JDK 21+ (Installed on host: Oracle JDK 26.0.1; configured for Java 21 LTS release target in `pom.xml`).
- **Dependencies**: 
  - MySQL Connector/J 8.4.0 (`lib/mysql-connector-j-8.4.0.jar`)
  - Google Gson 2.11.0 (`lib/gson-2.11.0.jar`)
- **Database**: MySQL Server 8.0 (Service: `MySQL800` on port 3306).
  - Application Database: `unigram_db` (initialized via `docs/database/schema.sql`)
  - Automated Test Database: `unigram_test_db` (initialized via `docs/database/schema_test.sql`)

### 🔑 Environment Variable Setup

Uni-Gram reads database credentials strictly from environment variables without hardcoded defaults:

```powershell
# Set Application Database Connection (unigram_db)
$env:UNIGRAM_DB_URL = "jdbc:mysql://localhost:3306/unigram_db?serverTimezone=UTC"
$env:UNIGRAM_DB_USER = "unigram_app"
$env:UNIGRAM_DB_PASSWORD = "your_mysql_password"

# Set Test Database Connection (unigram_test_db for safe automated tests)
$env:UNIGRAM_TEST_DB_URL = "jdbc:mysql://localhost:3306/unigram_test_db?serverTimezone=UTC"
$env:UNIGRAM_TEST_DB_USER = "unigram_app"
$env:UNIGRAM_TEST_DB_PASSWORD = "your_mysql_password"

# (Optional) HTTP API Port & CORS Settings
$env:UNIGRAM_API_PORT = "8080"
$env:UNIGRAM_CORS_ORIGINS = "http://localhost:5173,http://localhost:3000"
```

### Direct JDK Build & Test Commands (No Maven Required)

1. **Compile All Project Classes (Targeting Java 21)**:
   ```powershell
   & "C:\Program Files\Java\jdk-26.0.1\bin\javac.exe" --release 21 -cp "lib/mysql-connector-j-8.4.0.jar;lib/gson-2.11.0.jar" -d target/classes (Get-ChildItem -Recurse -Filter *.java src/main/java | Select-Object -ExpandProperty FullName)
   & "C:\Program Files\Java\jdk-26.0.1\bin\javac.exe" --release 21 -cp "target/classes;lib/mysql-connector-j-8.4.0.jar;lib/gson-2.11.0.jar" -d target/test-classes (Get-ChildItem -Recurse -Filter *.java src/test/java | Select-Object -ExpandProperty FullName)
   ```

2. **Run Verification Test Suites**:
   ```powershell
   # Phase 2 Regression Tests (12 tests)
   & "C:\Program Files\Java\jdk-26.0.1\bin\java.exe" -cp "target/classes;target/test-classes;lib/mysql-connector-j-8.4.0.jar;lib/gson-2.11.0.jar" com.unigram.Phase2Verification

   # Phase 3 Threading Tests (9 tests / 17 assertions)
   & "C:\Program Files\Java\jdk-26.0.1\bin\java.exe" -cp "target/classes;target/test-classes;lib/mysql-connector-j-8.4.0.jar;lib/gson-2.11.0.jar" com.unigram.Phase3Verification

   # Phase 4 Synchronization & Producer-Consumer Tests (10 tests / 28 assertions)
   & "C:\Program Files\Java\jdk-26.0.1\bin\java.exe" -cp "target/classes;target/test-classes;lib/mysql-connector-j-8.4.0.jar;lib/gson-2.11.0.jar" com.unigram.Phase4Verification

   # Phase 5 ExecutorService, Callable & Future Tests (15 tests / 37 assertions)
   & "C:\Program Files\Java\jdk-26.0.1\bin\java.exe" -cp "target/classes;target/test-classes;lib/mysql-connector-j-8.4.0.jar;lib/gson-2.11.0.jar" com.unigram.Phase5Verification

   # Phase 6 JDBC & MySQL Tests (13 test scenarios / 51 assertions)
   & "C:\Program Files\Java\jdk-26.0.1\bin\java.exe" -cp "target/classes;target/test-classes;lib/mysql-connector-j-8.4.0.jar;lib/gson-2.11.0.jar" com.unigram.Phase6Verification

   # Phase 7 MySQL Persistence & Concurrent Integration Tests (20 test scenarios)
   & "C:\Program Files\Java\jdk-26.0.1\bin\java.exe" -cp "target/classes;target/test-classes;lib/mysql-connector-j-8.4.0.jar;lib/gson-2.11.0.jar" com.unigram.Phase7Verification

   # Phase 8 Lambda, Streams, Generics & File I/O Tests (20 test scenarios / 71 assertions)
   & "C:\Program Files\Java\jdk-26.0.1\bin\java.exe" -cp "target/classes;target/test-classes;lib/mysql-connector-j-8.4.0.jar;lib/gson-2.11.0.jar" com.unigram.Phase8Verification

   # Phase 9 Standalone Java HTTP API Tests (23 test scenarios / ephemeral port)
   & "C:\Program Files\Java\jdk-26.0.1\bin\java.exe" -cp "target/classes;target/test-classes;lib/mysql-connector-j-8.4.0.jar;lib/gson-2.11.0.jar" com.unigram.Phase9Verification
   ```

3. **Run Interactive Console Application**:
   ```powershell
   & "C:\Program Files\Java\jdk-26.0.1\bin\java.exe" -cp "target/classes;lib/mysql-connector-j-8.4.0.jar;lib/gson-2.11.0.jar" com.unigram.Main
   ```

### 💻 Manual HTTP API Testing via PowerShell (`Invoke-RestMethod`)

To test the API manually:
1. Open PowerShell and start the main console application:
   ```powershell
   & "C:\Program Files\Java\jdk-26.0.1\bin\java.exe" -cp "target/classes;lib/mysql-connector-j-8.4.0.jar;lib/gson-2.11.0.jar" com.unigram.Main
   ```
2. In the menu, select `15` (Standalone HTTP API Server), then choose `1` to start the HTTP Server on `http://127.0.0.1:8080`.
3. Open a **second** PowerShell window and run the following commands:

```powershell
# 1. Health Check
Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/health" -Method GET | ConvertTo-Json -Depth 4

# 2. List All Users
Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/users" -Method GET | ConvertTo-Json -Depth 4

# 3. Lookup Specific User
Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/users/usr_student_A" -Method GET | ConvertTo-Json -Depth 4

# 4. List All Products
Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/products" -Method GET | ConvertTo-Json -Depth 4

# 5. Create a New Conversation
$body = @{
    buyerId   = "usr_student_A"
    sellerId  = "usr_student_B"
    productId = "prod_calc_ti84"
} | ConvertTo-Json
$convResponse = Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/conversations" -Method POST -ContentType "application/json" -Body $body
$convResponse | ConvertTo-Json -Depth 4
$convId = $convResponse.data.conversationId

# 6. Query Conversations for Student A
Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/conversations?participantId=usr_student_A" -Method GET | ConvertTo-Json -Depth 4

# 7. Send a Message (Persisted directly to MySQL)
$msgBody = @{ content = "Hi Maya! Is the calculator still available?" } | ConvertTo-Json
$msgResponse = Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/conversations/$convId/messages" `
    -Method POST -ContentType "application/json" -Headers @{ "X-Dev-User-Id" = "usr_student_A" } -Body $msgBody
$msgResponse | ConvertTo-Json -Depth 4

# 8. Retrieve Message History
Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/conversations/$convId/messages" `
    -Method GET -Headers @{ "X-Dev-User-Id" = "usr_student_A" } | ConvertTo-Json -Depth 4
```

---

## 🚀 Phase 11: Complete Frontend and Backend Integration (UniMart React + Uni-Gram Java)

Phase 11 completes the full integration between the Uni-Gram Core Java messaging backend and the UniMart React campus marketplace:

### Architectural Components & Flow
```
┌────────────────────────────────────────────────────────┐
│             UniMart React Marketplace Frontend         │
│          (React 19.2.8 + Vite 8.2.2 + Bootstrap 5)     │
│   ListingCard ("Chat") ──► ChatModal / FloatingChat    │
└───────────────────────────┬────────────────────────────┘
                            │
               HTTP / REST (JSON) via Fetch
               CORS: http://localhost:5173 / localhost:3000
               Client: unimart-frontend/src/services/unigramApi.js
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│           Uni-Gram Java HTTP REST API Backend          │
│       (com.sun.net.httpserver.HttpServer : 8080)       │
│  - POST /api/users (Idempotent Student Sync)           │
│  - POST /api/products (Idempotent Product Sync)        │
│  - POST /api/conversations (Thread Init & 409 Rescue)  │
│  - POST /api/conversations/{id}/messages (Persistence) │
│  - GET  /api/conversations/{id}/messages (Chronology)  │
└───────────────────────────┬────────────────────────────┘
                            │
                    MySQL JDBC (InnoDB)
                            ▼
┌────────────────────────────────────────────────────────┐
│                  Local MySQL Database                  │
│       (users, product_references, conversations,       │
│                   messages, audit_logs)                │
└────────────────────────────────────────────────────────┘
```

### Key Integration Deliverables
1. **Frontend Codebase**: Cloned complete main branch of UniMart into `unimart-frontend/` (Git history preserved, commit `2432275`).
2. **Centralized API Client**: Implemented `unimart-frontend/src/services/unigramApi.js` reading `VITE_UNIGRAM_API_BASE_URL` from `.env`.
3. **Synchronous Participant & Product Mapping**:
   - `AuthContext` student accounts sync to `users` table via `POST /api/users`.
   - Marketplace listings sync to `product_references` via `POST /api/products`.
4. **Resilient Thread Handling**: Duplicate conversation attempts return `409 Conflict` along with the existing `Conversation` payload, allowing seamless resumption of existing chats without runtime exceptions.
5. **Adaptive Background Polling**: Active conversations poll the backend every 3.5 seconds while the chat modal is open; intervals are immediately cleared when minimized or closed.
6. **Automated Verification**: End-to-end integration test suite implemented in `src/test/java/com/unigram/Phase11Verification.java`.

---

## 🚦 Current Development Progress

- [x] **Phase 1: Domain Entities & Custom Exception Hierarchy** (Completed)
- [x] **Phase 2: Collections & In-Memory Messaging Operations** (Completed)
- [x] **Phase 3: Thread & Runnable Fundamentals** (Completed)
- [x] **Phase 4: Synchronization & Producer-Consumer (wait/notify/notifyAll)** (Completed)
- [x] **Phase 5: ExecutorService, Callable & Future** (Completed)
- [x] **Phase 6: JDBC & MySQL Persistence** (Completed)
- [x] **Phase 7: Integration of JDBC with Concurrent Messaging Engine** (Completed)
- [x] **Phase 8: Lambda Expressions, Streams, Optional, Generics & File Handling** (Completed)
- [x] **Phase 9: Standalone Java HTTP API** (Completed)
- [x] **Phase 10: Final Backend Audit & Integration Preparation** (Completed)
- [x] **Phase 11: UniMart React Integration & End-to-End Persistence** (Completed)

---

## 📋 Integration Documentation

Comprehensive documentation detailing the integration architecture between the Java messaging backend and the UniMart React application is available in [docs/integration-readiness.md](docs/integration-readiness.md). It outlines the REST API contracts, React UI mappings (ChatModal, FloatingChatButton), user identity strategy (`X-Dev-User-Id`), environment setup, and verification instructions.

---

## 🏃 Running the Integrated System

### Terminal 1: Launch Uni-Gram Java Backend
```powershell
# Set database environment variables (if running with live MySQL)
$env:UNIGRAM_DB_URL = "jdbc:mysql://localhost:3306/unigram_db?serverTimezone=UTC"
$env:UNIGRAM_DB_USER = "root"
$env:UNIGRAM_DB_PASSWORD = "<your-mysql-password>"

# Launch direct daemon HTTP Server:
& "C:\Program Files\Java\jdk-26.0.1\bin\java.exe" -cp "target/classes;lib/mysql-connector-j-8.4.0.jar;lib/gson-2.11.0.jar" com.unigram.Main --server

# Or launch interactive CLI and choose Option 15 -> 1:
& "C:\Program Files\Java\jdk-26.0.1\bin\java.exe" -cp "target/classes;lib/mysql-connector-j-8.4.0.jar;lib/gson-2.11.0.jar" com.unigram.Main
```
*Select Option 15 -> Option 1 to start the HTTP Server on `http://127.0.0.1:8080`.*

### Terminal 2: Launch UniMart React Frontend
```powershell
cd unimart-frontend
npm run dev
```
*Open `http://localhost:5173` in your browser. Browse products, click **"Chat"** on any item, and send messages directly through to the Java backend!*
