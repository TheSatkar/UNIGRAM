# Uni-Gram: System Architecture & Development Phases

## 1. System Vision & Architectural Boundaries

**Uni-Gram** is an independent, high-performance messaging backend designed for university student marketplace transactions. It allows buyers and sellers to communicate about specific marketplace listings (e.g., product condition inquiries, pickup locations, price offers).

### Strict Boundary Rules
- **Standalone Project**: Operates strictly within the `UniGram-Java` workspace.
- **No UniMart Coupling**: Zero access or modification to any UniMart files. Integration with the UniMart React marketplace will occur strictly in Phase 11, only after explicit authorization.
- **Academic Purity (Java SE)**: Implemented using standard Java SE 21 features without heavy third-party enterprise frameworks (no Spring Boot, Jakarta EE, WebSockets, or ORM frameworks).
- **Appropriate Abstraction Level**: Designed specifically for a university Java programming syllabus. Avoids over-engineered enterprise patterns (e.g., elaborate generic repository frameworks); demonstrates concepts like generics, concurrency, and persistence using clean, direct, and syllabus-focused implementations.

---

## 2. Technical Stack & Component Design

### 2.1 Database Decision
- **Target Database**: A dedicated, standalone **MySQL** database.
- **No H2**: H2 is not used as the project database.
- **Deferred Driver**: The MySQL JDBC Connector (`mysql-connector-j`) will be introduced in `pom.xml` exclusively when reaching **Phase 6 (JDBC and MySQL persistence)**.
- **No Early Connections**: No database connections or pooling are initialized prior to Phase 6.

### 2.2 Concurrency Architecture
- **Message Queuing**: A dedicated, thread-safe **bounded message queue** (not a conversation queue) implementing the classic **Producer-Consumer pattern** via Java's native `wait()`, `notify()`, and `notifyAll()` primitives.
- **Thread Fundamentals**: Direct usage of `Thread` and `Runnable` to demonstrate thread state transitions and worker threads.
- **Executor Framework**: Background worker pool using `ExecutorService`, `Callable<T>`, and `Future<T>` for asynchronous notifications and offloading message persistence.

### 2.3 Generics & Collections Strategy
- **Targeted Generics Demonstration**: Generics (`<T>`) are demonstrated through focused, reusable utilities (such as a generic `ApiResponse<T>` or `MessagePayload<T>` container) rather than an artificial, complex generic repository framework.
- **Collections Framework**: Practical use of `List`, `Map`, `Queue`, and custom `Comparator<T>` for in-memory message storage, sorting, and user-listing lookups.

### 2.4 Presentation & API Layer Separation
- **Separation of Concerns**: The **Interactive Console Application** (for local CLI demonstration and viva defense) and the **Standalone Java HTTP API** (using pure `com.sun.net.httpserver.HttpServer`) remain completely distinct, decoupled presentation components.

---

## 3. Package Responsibilities

| Package | Purpose & Syllabus Focus |
|---|---|
| `com.unigram.model` | Core domain entities (`User`, `Listing`, `Message`, `Conversation`, `ListingStatus`). Immutability, encapsulation, `equals`/`hashCode`. |
| `com.unigram.exception` | Custom checked domain exceptions (e.g., `MessageDeliveryException`) and unchecked runtime exceptions (e.g., `EntityNotFoundException`). |
| `com.unigram.collection` | In-memory message stores, custom sorting comparators, and collection lookup structures. |
| `com.unigram.threading` | Concurrency subsystem: Bounded message queue with `wait()`/`notifyAll()`, `Thread`/`Runnable` workers, thread monitors, and `ExecutorService`/`Callable`/`Future`. |
| `com.unigram.database` | Pure JDBC connection management for MySQL, schema initialization, and manual ACID transaction boundaries (`setAutoCommit(false)`, `commit()`, `rollback()`). |
| `com.unigram.repository` | Focused DAO/repository interfaces and JDBC implementations for database persistence using parameterized `PreparedStatement`. |
| `com.unigram.service` | Core business workflows: message routing, listing state validation, buyer-seller permission checks, and transaction coordination. |
| `com.unigram.reporting` | Analytical reporting via Java Streams, Lambdas, and `Optional`; report export to disk using Java File I/O (`BufferedReader`/`BufferedWriter`). |
| `com.unigram.api` | Standalone Java HTTP endpoints (`com.sun.net.httpserver.HttpServer`) for future web clients. |
| `com.unigram.util` | Reusable utilities, focused generic wrappers (e.g., `Result<T>`), validation helpers, and date-time formatters. |

---

## 4. Master Phased Development Roadmap

The project strictly follows an 11-step sequential progression:

```
[Phase 1: Domain & Exceptions]
       │
       ▼
[Phase 2: Collections & In-Memory Messaging]
       │
       ▼
[Phase 3: Thread & Runnable Fundamentals]
       │
       ▼
[Phase 4: Synchronization & Producer-Consumer (wait/notify/notifyAll)]
       │
       ▼
[Phase 5: ExecutorService, Callable & Future]
       │
       ▼
[Phase 6: JDBC & MySQL Persistence]
       │
       ▼
[Phase 7: Integration: JDBC with Concurrent Messaging Engine]
       │
       ▼
[Phase 8: Lambdas, Streams, Optional, Generics & File Handling]
       │
       ▼
[Phase 9: Standalone Java HTTP API]
       │
       ▼
[Phase 10: Testing, Documentation & Viva Preparation]
       │
       ▼
[Phase 11: UniMart Integration (Only after explicit approval)]
```

### Phase Descriptions

1. **Phase 1: Domain Entities & Custom Exception Hierarchy**
   - Implement clean, encapsulated domain entities: `User`, `Listing`, `Message`, `Conversation`, and `ListingStatus` enum.
   - Build a structured exception hierarchy with custom checked exceptions (`MessageDeliveryException`, `ListingClosedException`) and runtime exceptions (`EntityNotFoundException`, `ValidationException`).
   - Validate entity integrity and exception raising using standalone Java verification.

2. **Phase 2: Collections & In-Memory Messaging Operations**
   - Implement in-memory data structures using `List`, `Map`, `Queue`, and `Set`.
   - Store and retrieve active conversations and message threads by listing ID and user ID.
   - Custom sorting using `Comparator<Message>` (chronological ordering, unread status).

3. **Phase 3: Thread & Runnable Fundamentals**
   - Create multi-threaded actors simulating concurrent buyers and sellers using `Thread` and `Runnable`.
   - Monitor and demonstrate thread lifecycles and states (`NEW`, `RUNNABLE`, `TIMED_WAITING`, `TERMINATED`).

4. **Phase 4: Synchronization & Producer-Consumer (wait/notify/notifyAll)**
   - Implement a thread-safe **bounded message queue** using native synchronization (`synchronized`).
   - Use `wait()`, `notify()`, and `notifyAll()` to handle full/empty queue conditions between message producers (buyers/sellers) and message consumer workers.
   - Prevent race conditions and deadlocks with defensive while-loop waiting guards.

5. **Phase 5: ExecutorService, Callable & Future**
   - Introduce thread pool execution via `ExecutorService`.
   - Model asynchronous message dispatching, delivery receipt generation, and background tasks using `Callable<T>` and `Future<T>`.
   - Implement graceful executor shutdown and error handling.

6. **Phase 6: JDBC & MySQL Persistence**
   - Add MySQL Connector/J dependency to `pom.xml`.
   - Implement direct JDBC connectivity, connection configuration, and schema setup (tables for users, listings, conversations, messages).
   - Implement CRUD operations using parameterized `PreparedStatement` to prevent SQL injection.
   - Demarcate manual ACID transactions with `connection.setAutoCommit(false)`, `commit()`, and `rollback()`.

7. **Phase 7: Integration of JDBC with the Concurrent Messaging Engine**
   - Connect the asynchronous message dispatching engine and consumer threads to the MySQL JDBC persistence layer.
   - Ensure thread safety during concurrent database write and read operations.

8. **Phase 8: Lambda Expressions, Streams, Optional, Generics & File Handling**
   - Build an analytics and reporting module using Java Streams, Lambda expressions, and `Optional` (e.g., calculating message volume per listing, active participants, average seller response latency).
   - Demonstrate generics via a clean, reusable utility (e.g., generic `Result<T>` / `Payload<T>` container).
   - Export generated analytics summaries to disk using Java File I/O (`Files`, `BufferedWriter`).

9. **Phase 9: Standalone Java HTTP API**
   - Build a lightweight HTTP server using Java's built-in `com.sun.net.httpserver.HttpServer`.
   - Expose endpoints to send messages, view conversations for a listing, and retrieve marketplace chat history without external frameworks.

10. **Phase 10: Testing, Documentation & Viva Preparation**
    - Execute end-to-end integration tests.
    - Prepare a complete project demonstration console walkthrough for viva presentation.
    - Finalize technical documentation and viva question-and-answer preparation guide.

11. **Phase 11: UniMart Integration**
    - **Blocked until explicit user approval**: Connect the Uni-Gram Java backend with the UniMart React frontend.

---

## 5. Build, Environment & Verification Status

- **Compilation Compatibility**: Configured for **Java 21 LTS** target compatibility (`javac --release 21`).
- **No Java 26-only Language Features**: All code is strictly maintained within Java 21 syntax and standard library APIs.
- **Maven Dependency Status**: Maven (`mvn`) is currently not recognized on the system `PATH`.
  - While initial classes and pure Java tests can be compiled and executed directly using `javac` and `java`, **Maven must be installed or added to the system PATH before automated, dependency-based test runs (`mvn test`) can be executed**.
