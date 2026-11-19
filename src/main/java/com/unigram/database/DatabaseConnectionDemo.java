package com.unigram.database;

import com.unigram.model.Conversation;
import com.unigram.model.Message;
import com.unigram.model.ProductReference;
import com.unigram.model.User;
import com.unigram.repository.ConversationJdbcDao;
import com.unigram.repository.MessageJdbcDao;
import com.unigram.repository.ProductReferenceJdbcDao;
import com.unigram.repository.UserJdbcDao;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Interactive and pedagogical demonstration suite for JDBC fundamentals:
 * 1. DriverManager, Connection, and DatabaseMetaData inspection
 * 2. Statement vs PreparedStatement (SQL Injection prevention)
 * 3. executeQuery() vs executeUpdate()
 * 4. CRUD operations and ResultSet mapping into model classes
 * 5. SQLException details (SQLState, errorCode, constraint violations)
 * 6. ACID Transactions: commit() and rollback() demonstrating atomicity
 */
public class DatabaseConnectionDemo {

    private final DatabaseConfig config;
    private final UserJdbcDao userDao = new UserJdbcDao();
    private final ProductReferenceJdbcDao productDao = new ProductReferenceJdbcDao();
    private final ConversationJdbcDao conversationDao = new ConversationJdbcDao();
    private final MessageJdbcDao messageDao = new MessageJdbcDao();

    public DatabaseConnectionDemo() {
        this.config = new DatabaseConfig();
    }

    public DatabaseConnectionDemo(DatabaseConfig config) {
        this.config = config != null ? config : new DatabaseConfig();
    }

    /**
     * Checks if MySQL connection is available. If not, prints guidance.
     */
    public boolean checkDatabaseAvailability() {
        if (!config.isConfigured()) {
            printConfigurationGuidance();
            return false;
        }
        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            return conn.isValid(2);
        } catch (DatabaseException | SQLException e) {
            System.out.println("[!] MySQL Connection Failed: " + e.getMessage());
            printConfigurationGuidance();
            return false;
        }
    }

    public void printConfigurationGuidance() {
        System.out.println("\n----------------------------------------------------------------------");
        System.out.println("[!] NOTICE: MySQL Connection is not yet configured or reachable.");
        System.out.println("To connect to your local MySQL 8.0 Server, set the following environment variables:");
        System.out.println("  $env:UNIGRAM_DB_URL = \"jdbc:mysql://localhost:3306/unigram_db?serverTimezone=UTC\"");
        System.out.println("  $env:UNIGRAM_DB_USER = \"<your-mysql-username>\"");
        System.out.println("  $env:UNIGRAM_DB_PASSWORD = \"<your-mysql-password>\"");
        System.out.println("And ensure unigram_db exists by running 'docs/database/schema.sql' in MySQL Workbench.");
        System.out.println("----------------------------------------------------------------------\n");
    }

    /**
     * Option 1: Test MySQL database connection.
     */
    public void testConnection() {
        System.out.println("======================================================================");
        System.out.println("     1. Testing MySQL Database Connection                             ");
        System.out.println("======================================================================\n");

        if (!checkDatabaseAvailability()) return;

        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            System.out.println("[✓] MySQL Database connection successful!");
            System.out.printf("  Connection Class: %s%n", conn.getClass().getName());
            System.out.printf("  Auto-Commit Mode: %b%n", conn.getAutoCommit());
            System.out.printf("  Is Valid (2s)   : %b%n", conn.isValid(2));
            System.out.printf("  Catalog (DB)    : %s%n", conn.getCatalog());
            System.out.println("[✓] Connection verified and will be automatically closed by try-with-resources.");
        } catch (Exception e) {
            System.out.println("[!] Error testing connection: " + e.getMessage());
        }
        System.out.println();
    }

    /**
     * Option 2: Display database information and metadata.
     */
    public void displayDatabaseMetadata() {
        System.out.println("======================================================================");
        System.out.println("     2. Database Information & Metadata Inspection                    ");
        System.out.println("======================================================================\n");

        if (!checkDatabaseAvailability()) return;

        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            DatabaseMetaData meta = conn.getMetaData();
            System.out.println("[✓] Database Metadata retrieved successfully:");
            System.out.printf("  DBMS Product Name   : %s%n", meta.getDatabaseProductName());
            System.out.printf("  DBMS Version        : %s%n", meta.getDatabaseProductVersion());
            System.out.printf("  DBMS Major/Minor    : %d.%d%n", meta.getDatabaseMajorVersion(), meta.getDatabaseMinorVersion());
            System.out.printf("  Driver Name         : %s%n", meta.getDriverName());
            System.out.printf("  Driver Version      : %s%n", meta.getDriverVersion());
            System.out.printf("  JDBC Version        : %d.%d%n", meta.getJDBCMajorVersion(), meta.getJDBCMinorVersion());
            System.out.printf("  Database User       : %s%n", meta.getUserName());
            System.out.printf("  Connected Catalog   : %s%n", conn.getCatalog());
            System.out.printf("  Supports Transactions: %b%n", meta.supportsTransactions());
            System.out.printf("  Default Isolation   : %d%n", meta.getDefaultTransactionIsolation());
        } catch (Exception e) {
            System.out.println("[!] Error reading metadata: " + e.getMessage());
        }
        System.out.println();
    }

    /**
     * Option 3: Statement vs PreparedStatement (SQL Injection prevention).
     */
    public void demonstrateStatementVsPreparedStatement() {
        System.out.println("======================================================================");
        System.out.println("     3. Statement vs PreparedStatement (SQL Injection Prevention)     ");
        System.out.println("======================================================================\n");

        if (!checkDatabaseAvailability()) return;

        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            // Seed a test user
            User victim = new User("usr_viva_sec", "Dr. Alan Turing", "alan@cambridge.edu");
            userDao.insert(conn, victim);

            // Attacker malicious input
            String maliciousInput = "usr_viva_sec' OR '1'='1";

            System.out.println("Simulating search query with malicious payload: \"" + maliciousInput + "\"");

            // Vulnerable Statement approach (Demonstration only!)
            System.out.println("\nApproach A: Dynamic String Concatenation with Statement (VULNERABLE):");
            String vulnerableSql = "SELECT user_id, display_name FROM users WHERE user_id = '" + maliciousInput + "'";
            System.out.println("  Constructed Query: " + vulnerableSql);
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(vulnerableSql)) {
                int matchedRows = 0;
                while (rs.next()) {
                    matchedRows++;
                    System.out.printf("  [INTRUSION LEAK] Found record: %s (%s)%n",
                            rs.getString("user_id"), rs.getString("display_name"));
                }
                System.out.printf("  -> Vulnerable Statement matched %d record(s)! Attacker bypassed filter.%n", matchedRows);
            }

            // Secure PreparedStatement approach
            System.out.println("\nApproach B: Parameterized PreparedStatement (SECURE):");
            String safeSql = "SELECT user_id, display_name FROM users WHERE user_id = ?";
            System.out.println("  Precompiled Template: " + safeSql);
            try (PreparedStatement pstmt = conn.prepareStatement(safeSql)) {
                pstmt.setString(1, maliciousInput); // Escaped automatically as literal value
                try (ResultSet rs = pstmt.executeQuery()) {
                    int matchedRows = 0;
                    while (rs.next()) matchedRows++;
                    System.out.printf("  -> PreparedStatement matched %d records. Payload treated as literal string!%n", matchedRows);
                }
            }

            // Clean up
            userDao.delete(conn, victim.getUserId());

            System.out.println("\n>> Academic Viva Note: PreparedStatement Safety <<");
            System.out.println("1. PreparedStatement precompiles the SQL template on the database server.");
            System.out.println("2. Parameters supplied via setString() are transferred separately from the query syntax,");
            System.out.println("   making it mathematically impossible for input data to alter the query logic.");

        } catch (Exception e) {
            System.out.println("[!] Error: " + e.getMessage());
        }
        System.out.println();
    }

    /**
     * Option 4: Insert and retrieve demonstration users.
     */
    public void demonstrateUserInsertAndRetrieve() {
        System.out.println("======================================================================");
        System.out.println("     4. User Insert & Retrieval Demonstration                         ");
        System.out.println("======================================================================\n");

        if (!checkDatabaseAvailability()) return;

        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            User u1 = new User("usr_demo_101", "Ada Lovelace", "ada@computing.org");
            User u2 = new User("usr_demo_102", "Charles Babbage", "charles@engines.org");

            System.out.println("1. Inserting user: " + u1.getFullName() + " (" + u1.getUserId() + ")");
            userDao.insert(conn, u1);

            System.out.println("2. Inserting user: " + u2.getFullName() + " (" + u2.getUserId() + ")");
            userDao.insert(conn, u2);

            System.out.println("\n3. Retrieving users by ID:");
            Optional<User> r1 = userDao.findById(conn, u1.getUserId());
            Optional<User> r2 = userDao.findById(conn, u2.getUserId());

            r1.ifPresent(u -> System.out.printf("   Found: ID=%s, Name=\"%s\", Email=\"%s\"%n", u.getUserId(), u.getFullName(), u.getEmail()));
            r2.ifPresent(u -> System.out.printf("   Found: ID=%s, Name=\"%s\", Email=\"%s\"%n", u.getUserId(), u.getFullName(), u.getEmail()));

            System.out.printf("%n4. Total users in table: %d%n", userDao.count(conn));

            // Clean up
            userDao.delete(conn, u1.getUserId());
            userDao.delete(conn, u2.getUserId());
            System.out.println("[✓] Demonstration users cleaned up.");
        } catch (Exception e) {
            System.out.println("[!] Error: " + e.getMessage());
        }
        System.out.println();
    }

    /**
     * Option 5: Update and delete demonstration records.
     */
    public void demonstrateUserUpdateAndDelete() {
        System.out.println("======================================================================");
        System.out.println("     5. Record Update & Delete Demonstration                          ");
        System.out.println("======================================================================\n");

        if (!checkDatabaseAvailability()) return;

        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            User testUser = new User("usr_demo_upd", "Original Name", "original@test.com");
            userDao.insert(conn, testUser);
            System.out.println("1. Created user: " + testUser);

            testUser.setFullName("Updated Name via JDBC");
            testUser.setEmail("updated@test.com");
            boolean updated = userDao.update(conn, testUser);
            System.out.println("2. userDao.update() returned: " + updated);

            Optional<User> fetched = userDao.findById(conn, testUser.getUserId());
            fetched.ifPresent(u -> System.out.printf("3. Verified Updated Record: Name=\"%s\", Email=\"%s\"%n", u.getFullName(), u.getEmail()));

            boolean deleted = userDao.delete(conn, testUser.getUserId());
            System.out.println("4. userDao.delete() returned: " + deleted);

            Optional<User> verifyDeleted = userDao.findById(conn, testUser.getUserId());
            System.out.println("5. Record exists after delete? " + verifyDeleted.isPresent());
            System.out.println("[✓] Update and delete verified.");
        } catch (Exception e) {
            System.out.println("[!] Error: " + e.getMessage());
        }
        System.out.println();
    }

    /**
     * Option 6: Product reference CRUD operations.
     */
    public void demonstrateProductCrud() {
        System.out.println("======================================================================");
        System.out.println("     6. Product Reference CRUD Operations                             ");
        System.out.println("======================================================================\n");

        if (!checkDatabaseAvailability()) return;

        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            ProductReference product = new ProductReference("prod_demo_calc", "Calculus: Early Transcendentals (8th Ed)");
            System.out.println("1. [CREATE] Inserting product: " + product.getProductTitle());
            productDao.insert(conn, product);

            System.out.println("2. [READ] Finding product by ID: " + product.getProductId());
            Optional<ProductReference> found = productDao.findById(conn, product.getProductId());
            found.ifPresent(p -> System.out.printf("   Found: ID=%s, Title=\"%s\"%n", p.getProductId(), p.getProductTitle()));

            System.out.println("3. [UPDATE] Updating product title...");
            product.setProductTitle("Calculus: Early Transcendentals (9th Edition)");
            productDao.update(conn, product);
            Optional<ProductReference> updated = productDao.findById(conn, product.getProductId());
            updated.ifPresent(p -> System.out.println("   Verified updated title: " + p.getProductTitle()));

            System.out.println("4. [DELETE] Deleting product...");
            productDao.delete(conn, product.getProductId());
            System.out.println("   Verified deleted exists: " + productDao.existsById(conn, product.getProductId()));
            System.out.println("[✓] ProductReference CRUD verified.");
        } catch (Exception e) {
            System.out.println("[!] Error: " + e.getMessage());
        }
        System.out.println();
    }

    /**
     * Option 7: Conversation JDBC operations.
     */
    public void demonstrateConversationOperations() {
        System.out.println("======================================================================");
        System.out.println("     7. Conversation JDBC Operations & Constraints                    ");
        System.out.println("======================================================================\n");

        if (!checkDatabaseAvailability()) return;

        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            User b = new User("usr_conv_b", "Buyer Student", "b@test.edu");
            User s = new User("usr_conv_s", "Seller Student", "s@test.edu");
            ProductReference p = new ProductReference("prod_conv_1", "Lab Microscope");

            userDao.insert(conn, b);
            userDao.insert(conn, s);
            productDao.insert(conn, p);

            Conversation conv = new Conversation("conv_jdbc_demo", b.getUserId(), s.getUserId(), p.getProductId());
            System.out.println("1. Inserting conversation: " + conv.getConversationId());
            conversationDao.insert(conn, conv);

            System.out.println("2. Querying conversation by ID:");
            Optional<Conversation> found = conversationDao.findById(conn, conv.getConversationId());
            found.ifPresent(c -> System.out.printf("   Found: ID=%s | Buyer=%s | Seller=%s | Product=%s%n",
                    c.getConversationId(), c.getBuyerId(), c.getSellerId(), c.getProductId()));

            System.out.println("3. Querying conversations for buyer " + b.getUserId() + ":");
            List<Conversation> buyerConvs = conversationDao.findByUser(conn, b.getUserId());
            System.out.printf("   Buyer has %d active conversation(s).%n", buyerConvs.size());

            // Clean up
            conversationDao.delete(conn, conv.getConversationId());
            productDao.delete(conn, p.getProductId());
            userDao.delete(conn, b.getUserId());
            userDao.delete(conn, s.getUserId());
            System.out.println("[✓] Conversation JDBC operations verified.");
        } catch (Exception e) {
            System.out.println("[!] Error: " + e.getMessage());
        }
        System.out.println();
    }

    /**
     * Option 8: Message JDBC operations.
     */
    public void demonstrateMessageOperations() {
        System.out.println("======================================================================");
        System.out.println("     8. Message JDBC Operations & Chronological Retrieval             ");
        System.out.println("======================================================================\n");

        if (!checkDatabaseAvailability()) return;

        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            User b = new User("usr_msg_b", "Buyer Msg", "b_msg@test.edu");
            User s = new User("usr_msg_s", "Seller Msg", "s_msg@test.edu");
            ProductReference p = new ProductReference("prod_msg_1", "Organic Chemistry Notes");
            userDao.insert(conn, b);
            userDao.insert(conn, s);
            productDao.insert(conn, p);

            Conversation conv = new Conversation("conv_msg_demo", b.getUserId(), s.getUserId(), p.getProductId());
            conversationDao.insert(conn, conv);

            Message m1 = new Message("msg_demo_1", conv.getConversationId(), b.getUserId(), "Hi! Is this still available?", LocalDateTime.now().minusMinutes(5));
            Message m2 = new Message("msg_demo_2", conv.getConversationId(), s.getUserId(), "Yes! Can meet at the library.", LocalDateTime.now().minusMinutes(2));

            System.out.println("1. Inserting 2 timestamped messages...");
            messageDao.insert(conn, m1);
            messageDao.insert(conn, m2);

            System.out.println("2. Retrieving chronological message thread (ORDER BY sent_at ASC):");
            List<Message> thread = messageDao.findByConversationId(conn, conv.getConversationId());
            for (Message m : thread) {
                System.out.printf("   [%s] %s: \"%s\" (at %s)%n", m.getMessageId(), m.getSenderId(), m.getContent(), m.getSentAt());
            }

            System.out.println("3. Retrieving latest message:");
            Optional<Message> latest = messageDao.findLatestByConversationId(conn, conv.getConversationId());
            latest.ifPresent(m -> System.out.printf("   Latest: [%s] from %s: \"%s\"%n", m.getMessageId(), m.getSenderId(), m.getContent()));

            // Clean up
            conversationDao.delete(conn, conv.getConversationId());
            productDao.delete(conn, p.getProductId());
            userDao.delete(conn, b.getUserId());
            userDao.delete(conn, s.getUserId());
            System.out.println("[✓] Message JDBC operations verified.");
        } catch (Exception e) {
            System.out.println("[!] Error: " + e.getMessage());
        }
        System.out.println();
    }

    /**
     * Option 9: ResultSet and Java object mapping.
     */
    public void demonstrateResultSetMapping() {
        System.out.println("======================================================================");
        System.out.println("     9. ResultSet and Java Object Mapping Explanation                 ");
        System.out.println("======================================================================\n");

        if (!checkDatabaseAvailability()) return;

        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            User demo = new User("usr_map_demo", "Margaret Hamilton", "margaret@apollo.nasa.gov");
            userDao.insert(conn, demo);

            String sql = "SELECT user_id, display_name, email, created_at FROM users WHERE user_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, demo.getUserId());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("Step-by-step extraction from JDBC ResultSet cursor:");
                        String userId = rs.getString("user_id");
                        String name = rs.getString("display_name");
                        String email = rs.getString("email");
                        java.sql.Timestamp ts = rs.getTimestamp("created_at");

                        System.out.printf("  rs.getString(\"user_id\")      -> \"%s\"%n", userId);
                        System.out.printf("  rs.getString(\"display_name\")  -> \"%s\"%n", name);
                        System.out.printf("  rs.getString(\"email\")         -> \"%s\"%n", email);
                        System.out.printf("  rs.getTimestamp(\"created_at\") -> %s%n", ts);

                        User mapped = new User(userId, name, email);
                        System.out.printf("%nResulting Java Domain Object:%n  %s%n", mapped);
                    }
                }
            }

            userDao.delete(conn, demo.getUserId());
            System.out.println("[✓] ResultSet mapping demonstrated successfully.");
        } catch (Exception e) {
            System.out.println("[!] Error: " + e.getMessage());
        }
        System.out.println();
    }

    /**
     * Option 10: ACID Transactions (commit & rollback).
     */
    public void demonstrateTransactions() {
        System.out.println("======================================================================");
        System.out.println("     10. ACID Transactions Demonstration (commit vs rollback)         ");
        System.out.println("======================================================================\n");

        if (!checkDatabaseAvailability()) return;

        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            // Case A: Successful Transaction
            System.out.println("Case A: Successful Atomic Transaction (setAutoCommit(false) -> commit()):");
            conn.setAutoCommit(false);
            System.out.println("  [Step 1] setAutoCommit(false) disabled auto-commit mode.");

            User u1 = new User("usr_tx_1", "Tx Student 1", "tx1@uni.edu");
            ProductReference p1 = new ProductReference("prod_tx_1", "Tx Textbook");
            userDao.insert(conn, u1);
            productDao.insert(conn, p1);
            System.out.println("  [Step 2] Executed insert user and insert product.");

            conn.commit();
            System.out.println("  [Step 3] conn.commit() persisted both records atomically.");
            conn.setAutoCommit(true);

            // Case B: Transaction Rollback upon Failure
            System.out.println("\nCase B: Failure with Rollback (Atomicity preserved):");
            conn.setAutoCommit(false);

            try {
                User u2 = new User("usr_tx_2", "Tx Student 2", "tx2@uni.edu");
                userDao.insert(conn, u2);
                System.out.println("  [Step 1] Inserted second user 'usr_tx_2'.");

                System.out.println("  [Step 2] Intentionally attempting invalid SQL (violating foreign key constraint)...");
                // Attempting to insert conversation referencing a non-existent product 'prod_ghost_999'
                Conversation invalidConv = new Conversation("conv_invalid", u1.getUserId(), u2.getUserId(), "prod_ghost_999");
                conversationDao.insert(conn, invalidConv); // Will throw SQLException!

                conn.commit();
            } catch (SQLException e) {
                System.out.println("  [Step 3] Caught Expected SQLException: " + e.getMessage());
                System.out.println("  [Step 4] Calling conn.rollback()...");
                conn.rollback();
                System.out.println("  [Step 5] Rollback complete. Any partial inserts were undone.");
            } finally {
                conn.setAutoCommit(true);
            }

            // Verify that usr_tx_2 was NOT persisted
            Optional<User> rolledBackUser = userDao.findById(conn, "usr_tx_2");
            System.out.printf("  Verification: Does 'usr_tx_2' exist in database? %b (Expected: false)%n",
                    rolledBackUser.isPresent());

            // Clean up u1 and p1
            userDao.delete(conn, u1.getUserId());
            productDao.delete(conn, p1.getProductId());

            System.out.println("\n>> Academic Viva Note: Atomicity (ACID) <<");
            System.out.println("Atomicity guarantees that all operations within a transaction either");
            System.out.println("complete successfully together (commit), or none of them take effect (rollback).");
            System.out.println("Partially completed database state is prevented.");

        } catch (Exception e) {
            System.out.println("[!] Error: " + e.getMessage());
        }
        System.out.println();
    }

    /**
     * Option 11: Runs all JDBC demonstrations sequentially.
     */
    public void runAllDemonstrations() {
        testConnection();
        displayDatabaseMetadata();
        demonstrateStatementVsPreparedStatement();
        demonstrateUserInsertAndRetrieve();
        demonstrateUserUpdateAndDelete();
        demonstrateProductCrud();
        demonstrateConversationOperations();
        demonstrateMessageOperations();
        demonstrateResultSetMapping();
        demonstrateTransactions();
    }
}
