package com.unigram;

import com.unigram.database.DatabaseConfig;
import com.unigram.database.DatabaseConnectionFactory;
import com.unigram.database.DatabaseException;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Phase 6 Verification Suite: JDBC and MySQL Persistence Architecture.
 *
 * Verifies:
 *  1. Database configuration loading and validation
 *  2. Connection establishment and closure via try-with-resources
 *  3. Database metadata inspection (DBMS name/version, driver name)
 *  4. User CRUD operations via UserJdbcDao
 *  5. ProductReference CRUD operations via ProductReferenceJdbcDao
 *  6. Conversation CRUD operations via ConversationJdbcDao
 *  7. Message CRUD operations and chronological ordering verification
 *  8. PreparedStatement escaping for SQL injection payloads, quotes, and special characters
 *  9. Foreign key constraint enforcement (referential integrity failure throws SQLException)
 * 10. Unique constraint enforcement (duplicate primary key throws SQLException)
 * 11. Transaction commit atomicity
 * 12. Transaction rollback atomicity on exception
 * 13. Resource closure and connection validity
 *
 * Design:
 * - Uses test-specific prefixes ("tst_") to isolate verification data.
 * - Handles database unavailability gracefully: if credentials or server are not reachable,
 *   offline tests run and online tests report SKIPPED with clear setup instructions.
 */
public class Phase6Verification {

    private static int totalAssertions = 0;
    private static int passedAssertions = 0;
    private static int failedAssertions = 0;
    private static int skippedTests = 0;

    private static final DatabaseConfig config = new DatabaseConfig();
    private static final UserJdbcDao userDao = new UserJdbcDao();
    private static final ProductReferenceJdbcDao productDao = new ProductReferenceJdbcDao();
    private static final ConversationJdbcDao conversationDao = new ConversationJdbcDao();
    private static final MessageJdbcDao messageDao = new MessageJdbcDao();

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("          Uni-Gram — Phase 6 Verification Test Suite                  ");
        System.out.println("======================================================================\n");

        // 1. Offline Test: Configuration Object Validation
        testConfigurationLoading();

        // Check if database is reachable
        boolean dbAvailable = checkDatabaseReachability();

        if (!dbAvailable) {
            System.out.println("\n----------------------------------------------------------------------");
            System.out.println("[!] NOTICE: MySQL Database is not configured or not currently reachable.");
            System.out.println("Offline tests completed. Online JDBC integration tests are SKIPPED.");
            System.out.println("To execute full live MySQL tests, configure your environment variables:");
            System.out.println("  $env:UNIGRAM_DB_URL = \"jdbc:mysql://localhost:3306/unigram_db?serverTimezone=UTC\"");
            System.out.println("  $env:UNIGRAM_DB_USER = \"<mysql-username>\"");
            System.out.println("  $env:UNIGRAM_DB_PASSWORD = \"<mysql-password>\"");
            System.out.println("----------------------------------------------------------------------\n");
            skippedTests = 12;
            printSummary(false);
            return;
        }

        // Live MySQL Tests
        try {
            cleanupAllTestData();

            testConnectionEstablishment();
            testDatabaseMetadata();
            testUserCrud();
            testProductReferenceCrud();
            testConversationCrud();
            testMessageCrudAndChronology();
            testPreparedStatementEscaping();
            testForeignKeyConstraints();
            testUniqueConstraints();
            testTransactionCommit();
            testTransactionRollback();
            testResourceClosure();

        } finally {
            cleanupAllTestData();
        }

        printSummary(true);
        if (failedAssertions > 0) {
            System.exit(1);
        }
    }

    // -------------------------------------------------------------------------
    // Test 1: Configuration Loading (Offline)
    // -------------------------------------------------------------------------
    private static void testConfigurationLoading() {
        System.out.println("[Test 1] Database Configuration Loading and Validation");
        DatabaseConfig customConfig = new DatabaseConfig("jdbc:mysql://localhost:3306/custom_db", "test_user", "test_pass");
        assertTrue("Custom URL matches", "jdbc:mysql://localhost:3306/custom_db".equals(customConfig.getJdbcUrl()));
        assertTrue("Custom User matches", "test_user".equals(customConfig.getUsername()));
        assertTrue("Custom Password matches", "test_pass".equals(customConfig.getPassword()));
        assertTrue("Custom isConfigured is true", customConfig.isConfigured());

        DatabaseConfig emptyConfig = new DatabaseConfig("", "", "");
        assertTrue("Empty config isConfigured is false", !emptyConfig.isConfigured());
        System.out.println();
    }

    private static boolean checkDatabaseReachability() {
        if (!config.isConfigured()) {
            return false;
        }
        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            return conn != null && conn.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Test 2: Connection Establishment
    // -------------------------------------------------------------------------
    private static void testConnectionEstablishment() {
        System.out.println("[Test 2] Connection Establishment & Try-with-Resources");
        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            assertTrue("Connection is not null", conn != null);
            assertTrue("Connection is open and valid", conn != null && !conn.isClosed() && conn.isValid(2));
            assertTrue("Auto-commit is enabled by default", conn != null && conn.getAutoCommit());
        } catch (Exception e) {
            fail("Connection failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Test 3: Database Metadata
    // -------------------------------------------------------------------------
    private static void testDatabaseMetadata() {
        System.out.println("[Test 3] Database Metadata Inspection");
        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            DatabaseMetaData meta = conn.getMetaData();
            assertTrue("Product name is MySQL", meta.getDatabaseProductName().toLowerCase().contains("mysql"));
            assertTrue("Driver name contains MySQL", meta.getDriverName().toLowerCase().contains("mysql"));
            assertTrue("Supports transactions", meta.supportsTransactions());
        } catch (Exception e) {
            fail("Metadata retrieval failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Test 4: User CRUD
    // -------------------------------------------------------------------------
    private static void testUserCrud() {
        System.out.println("[Test 4] User CRUD Operations (UserJdbcDao)");
        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            User user = new User("tst_usr_1", "Test Student One", "tst1@uni.edu");

            // Insert
            boolean inserted = userDao.insert(conn, user);
            assertTrue("User inserted successfully", inserted);
            assertTrue("User existsById returns true", userDao.existsById(conn, user.getUserId()));

            // Find
            Optional<User> found = userDao.findById(conn, user.getUserId());
            assertTrue("Found user is present", found.isPresent());
            assertTrue("Found user ID matches", found.map(u -> u.getUserId().equals("tst_usr_1")).orElse(false));
            assertTrue("Found user name matches", found.map(u -> u.getFullName().equals("Test Student One")).orElse(false));

            // Update
            user.setFullName("Updated Student One");
            user.setEmail("tst1_updated@uni.edu");
            boolean updated = userDao.update(conn, user);
            assertTrue("User updated successfully", updated);
            Optional<User> updatedUser = userDao.findById(conn, user.getUserId());
            assertTrue("Updated name reflected", updatedUser.map(u -> u.getFullName().equals("Updated Student One")).orElse(false));

            // Delete
            boolean deleted = userDao.delete(conn, user.getUserId());
            assertTrue("User deleted successfully", deleted);
            assertTrue("User existsById returns false after delete", !userDao.existsById(conn, user.getUserId()));

        } catch (Exception e) {
            fail("User CRUD test failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Test 5: Product Reference CRUD
    // -------------------------------------------------------------------------
    private static void testProductReferenceCrud() {
        System.out.println("[Test 5] ProductReference CRUD Operations (ProductReferenceJdbcDao)");
        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            ProductReference prod = new ProductReference("tst_prod_1", "Linear Algebra Textbook");

            // Insert
            boolean inserted = productDao.insert(conn, prod);
            assertTrue("Product inserted successfully", inserted);
            assertTrue("Product existsById returns true", productDao.existsById(conn, prod.getProductId()));

            // Find
            Optional<ProductReference> found = productDao.findById(conn, prod.getProductId());
            assertTrue("Found product is present", found.isPresent());
            assertTrue("Found product title matches", found.map(p -> p.getProductTitle().equals("Linear Algebra Textbook")).orElse(false));

            // Update
            prod.setProductTitle("Linear Algebra (Second Edition)");
            boolean updated = productDao.update(conn, prod);
            assertTrue("Product updated successfully", updated);

            // Delete
            boolean deleted = productDao.delete(conn, prod.getProductId());
            assertTrue("Product deleted successfully", deleted);
            assertTrue("Product existsById returns false after delete", !productDao.existsById(conn, prod.getProductId()));

        } catch (Exception e) {
            fail("Product CRUD test failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Test 6: Conversation CRUD
    // -------------------------------------------------------------------------
    private static void testConversationCrud() {
        System.out.println("[Test 6] Conversation CRUD Operations (ConversationJdbcDao)");
        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            User buyer = new User("tst_usr_b", "Buyer", "buyer@test.edu");
            User seller = new User("tst_usr_s", "Seller", "seller@test.edu");
            ProductReference prod = new ProductReference("tst_prod_c", "Physics Lab Kit");

            userDao.insert(conn, buyer);
            userDao.insert(conn, seller);
            productDao.insert(conn, prod);

            Conversation conv = new Conversation("tst_conv_1", buyer.getUserId(), seller.getUserId(), prod.getProductId());
            boolean inserted = conversationDao.insert(conn, conv);
            assertTrue("Conversation inserted successfully", inserted);
            assertTrue("Conversation existsById returns true", conversationDao.existsById(conn, conv.getConversationId()));

            Optional<Conversation> found = conversationDao.findById(conn, conv.getConversationId());
            assertTrue("Conversation found by ID", found.isPresent());
            assertTrue("Conversation buyer matches", found.map(c -> c.getBuyerId().equals("tst_usr_b")).orElse(false));

            List<Conversation> buyerConvs = conversationDao.findByUser(conn, "tst_usr_b");
            assertTrue("Buyer conversation list size is 1", buyerConvs.size() == 1);

            // Clean up
            conversationDao.delete(conn, conv.getConversationId());
            productDao.delete(conn, prod.getProductId());
            userDao.delete(conn, buyer.getUserId());
            userDao.delete(conn, seller.getUserId());

        } catch (Exception e) {
            fail("Conversation CRUD test failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Test 7: Message CRUD and Chronology
    // -------------------------------------------------------------------------
    private static void testMessageCrudAndChronology() {
        System.out.println("[Test 7] Message CRUD & Chronological Order (MessageJdbcDao)");
        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            User buyer = new User("tst_usr_mb", "Msg Buyer", "mb@test.edu");
            User seller = new User("tst_usr_ms", "Msg Seller", "ms@test.edu");
            ProductReference prod = new ProductReference("tst_prod_m", "Data Structures Guide");

            userDao.insert(conn, buyer);
            userDao.insert(conn, seller);
            productDao.insert(conn, prod);

            Conversation conv = new Conversation("tst_conv_m", buyer.getUserId(), seller.getUserId(), prod.getProductId());
            conversationDao.insert(conn, conv);

            LocalDateTime baseTime = LocalDateTime.now().minusHours(1);
            Message m1 = new Message("tst_msg_1", conv.getConversationId(), buyer.getUserId(), "First message", baseTime);
            Message m2 = new Message("tst_msg_2", conv.getConversationId(), seller.getUserId(), "Second message", baseTime.plusMinutes(10));
            Message m3 = new Message("tst_msg_3", conv.getConversationId(), buyer.getUserId(), "Third message", baseTime.plusMinutes(20));

            // Insert in non-chronological order to test SQL ordering
            messageDao.insert(conn, m2);
            messageDao.insert(conn, m1);
            messageDao.insert(conn, m3);

            List<Message> history = messageDao.findByConversationId(conn, conv.getConversationId());
            assertTrue("History contains 3 messages", history.size() == 3);
            assertTrue("Message 1 is chronologically first", history.get(0).getMessageId().equals("tst_msg_1"));
            assertTrue("Message 2 is chronologically second", history.get(1).getMessageId().equals("tst_msg_2"));
            assertTrue("Message 3 is chronologically third", history.get(2).getMessageId().equals("tst_msg_3"));

            Optional<Message> latest = messageDao.findLatestByConversationId(conn, conv.getConversationId());
            assertTrue("Latest message is present", latest.isPresent());
            assertTrue("Latest message is Message 3", latest.map(m -> m.getMessageId().equals("tst_msg_3")).orElse(false));

            // Clean up
            conversationDao.delete(conn, conv.getConversationId());
            productDao.delete(conn, prod.getProductId());
            userDao.delete(conn, buyer.getUserId());
            userDao.delete(conn, seller.getUserId());

        } catch (Exception e) {
            fail("Message CRUD test failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Test 8: PreparedStatement Escaping
    // -------------------------------------------------------------------------
    private static void testPreparedStatementEscaping() {
        System.out.println("[Test 8] PreparedStatement SQL Injection & Escaping Behavior");
        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            String trickyId = "tst_sec_1";
            String maliciousName = "Robert'); DROP TABLE students; --";
            String unicodeEmail = "o'connor.test+café@uni.edu";

            User trickyUser = new User(trickyId, maliciousName, unicodeEmail);
            boolean inserted = userDao.insert(conn, trickyUser);
            assertTrue("PreparedStatement inserted user with SQL syntax safely", inserted);

            Optional<User> fetched = userDao.findById(conn, trickyId);
            assertTrue("User retrieved successfully", fetched.isPresent());
            assertTrue("Name contains uncorrupted payload text", fetched.map(u -> u.getFullName().equals(maliciousName)).orElse(false));
            assertTrue("Email contains uncorrupted quotes/accents", fetched.map(u -> u.getEmail().equals(unicodeEmail)).orElse(false));

            userDao.delete(conn, trickyId);

        } catch (Exception e) {
            fail("PreparedStatement escaping test failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Test 9: Foreign Key Constraint Enforcement
    // -------------------------------------------------------------------------
    private static void testForeignKeyConstraints() {
        System.out.println("[Test 9] Foreign Key Constraint Enforcement");
        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            // Attempt inserting conversation with non-existent users
            Conversation ghostConv = new Conversation("tst_conv_ghost", "non_existent_b", "non_existent_s", "non_existent_p");
            boolean caughtFk = false;
            try {
                conversationDao.insert(conn, ghostConv);
            } catch (SQLException e) {
                caughtFk = true;
                // MySQL FK violation SQLState is 23000
                assertTrue("SQLException SQLState is 23000 (integrity constraint)", "23000".equals(e.getSQLState()));
            }
            assertTrue("Inserting conversation with invalid FK threw SQLException", caughtFk);

        } catch (Exception e) {
            fail("Foreign key test failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Test 10: Unique Constraint Enforcement
    // -------------------------------------------------------------------------
    private static void testUniqueConstraints() {
        System.out.println("[Test 10] Unique Constraint Enforcement");
        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            User u1 = new User("tst_uniq_1", "Original User", "uniq1@uni.edu");
            userDao.insert(conn, u1);

            User duplicate = new User("tst_uniq_1", "Duplicate User", "uniq2@uni.edu");
            boolean caughtDup = false;
            try {
                userDao.insert(conn, duplicate);
            } catch (SQLException e) {
                caughtDup = true;
                assertTrue("SQLException indicates duplicate key (SQLState 23000)", "23000".equals(e.getSQLState()));
            }
            assertTrue("Inserting duplicate primary key threw SQLException", caughtDup);

            userDao.delete(conn, u1.getUserId());

        } catch (Exception e) {
            fail("Unique constraint test failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Test 11: Transaction Commit Atomicity
    // -------------------------------------------------------------------------
    private static void testTransactionCommit() {
        System.out.println("[Test 11] Transaction Commit Atomicity");
        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            conn.setAutoCommit(false);

            User buyer = new User("tst_tx_c_b", "Tx Commit Buyer", "tx_c_b@uni.edu");
            User seller = new User("tst_tx_c_s", "Tx Commit Seller", "tx_c_s@uni.edu");
            ProductReference product = new ProductReference("tst_tx_c_p", "Tx Commit Product");
            Conversation conv = new Conversation("tst_tx_c_conv", buyer.getUserId(), seller.getUserId(), product.getProductId());

            userDao.insert(conn, buyer);
            userDao.insert(conn, seller);
            productDao.insert(conn, product);
            conversationDao.insert(conn, conv);

            conn.commit();
            conn.setAutoCommit(true);

            // Verify all related records exist in database
            assertTrue("Buyer committed", userDao.existsById(conn, buyer.getUserId()));
            assertTrue("Seller committed", userDao.existsById(conn, seller.getUserId()));
            assertTrue("Product committed", productDao.existsById(conn, product.getProductId()));
            assertTrue("Conversation committed", conversationDao.existsById(conn, conv.getConversationId()));

            // Cleanup
            conversationDao.delete(conn, conv.getConversationId());
            productDao.delete(conn, product.getProductId());
            userDao.delete(conn, buyer.getUserId());
            userDao.delete(conn, seller.getUserId());

        } catch (Exception e) {
            fail("Transaction commit test failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Test 12: Transaction Rollback Atomicity
    // -------------------------------------------------------------------------
    private static void testTransactionRollback() {
        System.out.println("[Test 12] Transaction Rollback Atomicity");
        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            conn.setAutoCommit(false);

            User buyer = new User("tst_tx_r_b", "Tx Rollback Buyer", "tx_r_b@uni.edu");
            User seller = new User("tst_tx_r_s", "Tx Rollback Seller", "tx_r_s@uni.edu");

            // Insert valid users inside transaction
            userDao.insert(conn, buyer);
            userDao.insert(conn, seller);

            // Attempt invalid operation: inserting conversation referencing non-existent product "ghost_product_9999"
            // Note: buyer and seller are distinct valid users, so domain validation passes;
            // foreign key constraint in MySQL throws SQLException!
            boolean caughtError = false;
            Conversation invalidConv = new Conversation("tst_tx_r_conv", buyer.getUserId(), seller.getUserId(), "ghost_product_9999");

            try {
                conversationDao.insert(conn, invalidConv);
                conn.commit();
            } catch (SQLException e) {
                caughtError = true;
                conn.rollback();
            } finally {
                conn.setAutoCommit(true);
            }

            assertTrue("SQLException caught causing rollback", caughtError);
            assertTrue("Buyer was rolled back and does NOT exist in DB", !userDao.existsById(conn, buyer.getUserId()));
            assertTrue("Seller was rolled back and does NOT exist in DB", !userDao.existsById(conn, seller.getUserId()));
            assertTrue("Conversation does NOT exist in DB", !conversationDao.existsById(conn, invalidConv.getConversationId()));

            // Safety cleanup just in case
            conversationDao.delete(conn, invalidConv.getConversationId());
            userDao.delete(conn, buyer.getUserId());
            userDao.delete(conn, seller.getUserId());

        } catch (Exception e) {
            fail("Transaction rollback test failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Test 13: Resource Closure Verification
    // -------------------------------------------------------------------------
    private static void testResourceClosure() {
        System.out.println("[Test 13] Connection and Resource Closure Verification");
        Connection capturedConn = null;
        PreparedStatement capturedStmt = null;
        ResultSet capturedRs = null;

        try {
            try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
                capturedConn = conn;
                String sql = "SELECT 1";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    capturedStmt = stmt;
                    try (ResultSet rs = stmt.executeQuery()) {
                        capturedRs = rs;
                        assertTrue("ResultSet is valid during try block", rs.next());
                    }
                }
            }
        } catch (Exception e) {
            fail("Resource closure test failed: " + e.getMessage());
        }

        try {
            assertTrue("ResultSet is closed after try-with-resources", capturedRs != null && capturedRs.isClosed());
            assertTrue("Statement is closed after try-with-resources", capturedStmt != null && capturedStmt.isClosed());
            assertTrue("Connection is closed after try-with-resources", capturedConn != null && capturedConn.isClosed());
        } catch (SQLException e) {
            fail("Checking closed state threw SQLException: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Helpers & Cleanup
    // -------------------------------------------------------------------------
    private static void cleanupAllTestData() {
        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM messages WHERE message_id LIKE 'tst_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM conversations WHERE conversation_id LIKE 'tst_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM product_references WHERE product_id LIKE 'tst_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE user_id LIKE 'tst_%'")) {
                ps.executeUpdate();
            }
        } catch (Exception ignored) {
            // Best effort cleanup
        }
    }

    private static void assertTrue(String message, boolean condition) {
        totalAssertions++;
        if (condition) {
            passedAssertions++;
            System.out.println("  [PASS] " + message);
        } else {
            failedAssertions++;
            System.err.println("  [FAIL] " + message);
        }
    }

    private static void fail(String message) {
        totalAssertions++;
        failedAssertions++;
        System.err.println("  [FAIL] " + message);
    }

    private static void printSummary(boolean onlineTestsRun) {
        System.out.println("======================================================================");
        System.out.println("                   Phase 6 Verification Summary                       ");
        System.out.println("======================================================================");
        System.out.println("Total Assertions Evaluated : " + totalAssertions);
        System.out.println("Assertions Passed          : " + passedAssertions);
        System.out.println("Assertions Failed          : " + failedAssertions);
        if (!onlineTestsRun) {
            System.out.println("Online Test Suites Skipped : " + skippedTests + " (MySQL DB not configured/running)");
            System.out.println("Status                     : OFFLINE TESTS PASSED / ONLINE PENDING CONFIG");
        } else {
            System.out.println("Status                     : " + (failedAssertions == 0 ? "ALL PASS" : "FAILURES DETECTED"));
        }
        System.out.println("======================================================================\n");
    }
}
