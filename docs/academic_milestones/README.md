# Academic Milestone Verifications (Phases 2–9)

This directory archives the early stage academic milestone verification suites for UniMart + Uni-Gram.
These verification files demonstrate individual Java syllabus topics completed during academic coursework and curriculum reviews.

Active integration and regression testing for the platform is maintained in `src/test/java/com/unigram/`:
- `GoogleTokenVerifierTest.java`: Cryptographic token verification, signatures, clock-skew, claims negative testing.
- `SecurityAndPersistenceRegressionTest.java`: Comprehensive security and MySQL durability regression tests.
- `EndToEndIntegrationVerification.java`: Full lifecycle integration between React, Java HTTP API, and MySQL persistence.
- `Phase11Verification.java`: Final milestone system integrity and syllabus feature verification.

---

## Course Syllabus Mapping

| Milestone File | Primary Java Syllabus Topic | Core Demonstration Areas |
| :--- | :--- | :--- |
| **Phase2Verification.java** | Object-Oriented Programming (OOP) & Collections | Java Collections framework (`List`, `Map`, `Set`), entity encapsulation, immutability, comparator sorting. |
| **Phase3Verification.java** | Custom Exception Hierarchy | Checked vs. unchecked exceptions, exception bubbling, domain error encapsulation (`ConversationNotFoundException`, etc.). |
| **Phase4Verification.java** | Multithreading & Thread Lifecycle | Core `Thread` class, `Runnable` interface, thread states (`NEW`, `RUNNABLE`, `BLOCKED`, `WAITING`, `TIMED_WAITING`, `TERMINATED`). |
| **Phase5Verification.java** | Concurrency & Thread Synchronization | Critical sections, synchronized blocks/methods, race condition mitigation, thread safety guarantees. |
| **Phase6Verification.java** | Inter-Thread Communication & Producer-Consumer | `wait()`, `notify()`, `notifyAll()`, bounded buffer blocking queues for asynchronous message delivery. |
| **Phase7Verification.java** | JDBC Persistence & Transaction Management | Raw JDBC (`DriverManager`, `Connection`, `PreparedStatement`, `ResultSet`), atomic transactions (`setAutoCommit(false)`, `commit()`, `rollback()`). |
| **Phase8Verification.java** | Java Streams API & Modern Concurrency | Functional pipelines, `Stream` filtering/mapping/reducing, `ExecutorService`, thread pool task submission. |
| **Phase9Verification.java** | Java File I/O & Serialization | Standard I/O streams, binary and text reporting, audit log persistence, safe resource management with try-with-resources. |

All features verified in these milestone files remain active and testable in the main application via the interactive console menu in `com.unigram.Main` (Menu options 1–15).
