# Uni-Gram — Standalone Campus Messaging System

A minimal, standalone Java terminal application for student-to-student messaging at **Chitkara University**, backed by **MySQL 8.0** via **JDBC**.

---

## Required Software

- **Java JDK 21+** (`javac` and `java`)
- **MySQL Server 8.0+**
- **Maven** (or **IntelliJ IDEA** with built-in Maven support)

---

## Database Setup

1. Start your local MySQL 8.0 server (`MySQL80`).
2. Run the schema script to create `unigram_standalone_db` and seed 5 demo students:
   ```sql
   SOURCE database/schema.sql;
   ```
   *(Or open `database/schema.sql` in MySQL Workbench and execute the script.)*

---

## Configuration

1. Copy `.env.example` to `.env` in the `Final-java-project` root folder.
2. Update `.env` with your local MySQL username and password:
   ```env
   UNIGRAM_DB_URL=jdbc:mysql://localhost:3306/unigram_standalone_db?serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false
   UNIGRAM_DB_USER=root
   UNIGRAM_DB_PASSWORD=your_mysql_password
   ```

> **Note:** Never commit `.env` to version control. It is listed in `.gitignore`.

---

## Running in IntelliJ IDEA

1. Open `Final-java-project` in IntelliJ IDEA (**File → Open** and select `pom.xml` or the `Final-java-project` folder).
2. Let IntelliJ load the Maven dependencies (`mysql-connector-j`).
3. Open `src/main/java/com/unigram/Main.java` and click **Run `Main.main()`**.

---

## Running from Windows Terminal

- Double-click `run.bat` or run it from Command Prompt / PowerShell inside `Final-java-project`:
  ```bat
  run.bat
  ```
- Or using Maven directly:
  ```bat
  mvn compile
  java -cp "target\classes;%USERPROFILE%\.m2\repository\com\mysql\mysql-connector-j\8.4.0\mysql-connector-j-8.4.0.jar" com.unigram.Main
  ```

---

## Project Structure

```text
Final-java-project/
├── .env.example
├── .gitignore
├── JAVA_VIVA_GUIDE.md
├── pom.xml
├── README.md
├── run.bat
├── database/
│   └── schema.sql
└── src/
    ├── main/java/com/unigram/
    │   ├── Main.java
    │   ├── database/
    │   │   ├── DatabaseConfig.java
    │   │   ├── DatabaseConnectionFactory.java
    │   │   └── DatabaseException.java
    │   ├── exception/
    │   │   ├── ConversationNotFoundException.java
    │   │   ├── InvalidMessageException.java
    │   │   ├── UnauthorizedConversationAccessException.java
    │   │   ├── UniGramException.java
    │   │   └── UserNotFoundException.java
    │   ├── model/
    │   │   ├── Conversation.java
    │   │   ├── Message.java
    │   │   └── User.java
    │   ├── repository/
    │   │   ├── ConversationJdbcDao.java
    │   │   ├── MessageJdbcDao.java
    │   │   └── UserJdbcDao.java
    │   └── service/
    │       ├── JdbcChatService.java
    │       └── JdbcMessageService.java
    └── test/java/com/unigram/
        └── StandaloneMessagingTest.java
```
