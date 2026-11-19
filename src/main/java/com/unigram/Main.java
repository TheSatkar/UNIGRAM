package com.unigram;

import com.unigram.collection.CollectionsDemo;
import com.unigram.collection.InMemoryStorage;
import com.unigram.database.DatabaseConfig;
import com.unigram.database.DatabaseConnectionDemo;
import com.unigram.database.DatabaseConnectionFactory;
import com.unigram.database.DatabaseException;
import com.unigram.exception.ConversationNotFoundException;
import com.unigram.exception.InvalidMessageException;
import com.unigram.exception.UnauthorizedConversationAccessException;
import com.unigram.exception.UniGramException;
import com.unigram.exception.UserNotFoundException;
import com.unigram.model.Conversation;
import com.unigram.model.Message;
import com.unigram.model.ProductReference;
import com.unigram.model.User;
import com.unigram.repository.ConversationJdbcDao;
import com.unigram.repository.MessageJdbcDao;
import com.unigram.repository.ProductReferenceJdbcDao;
import com.unigram.repository.UserJdbcDao;
import com.unigram.service.ChatService;
import com.unigram.service.JdbcChatService;
import com.unigram.service.JdbcMessageService;
import com.unigram.service.MessageService;
import com.unigram.threading.AdvancedThreadStatesDemo;
import com.unigram.threading.ConcurrentJdbcMessageEngine;
import com.unigram.threading.ExecutorServiceSyllabusService;
import com.unigram.threading.JdbcMessageProducer;
import com.unigram.threading.ProducerConsumerDemo;
import com.unigram.threading.SynchronizationRaceDemo;
import com.unigram.threading.ThreadingSyllabusService;
import com.unigram.reporting.Phase8SyllabusService;
import com.unigram.api.UniGramHttpServer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Uni-Gram Interactive Console Application.
 *
 * In Phase 7:
 * - Normal messaging operations (Menu Options 1-7) persist to MySQL via JdbcChatService and JdbcMessageService.
 * - In-memory storage is preserved for Phase 2 collections demonstration.
 * - Custom bounded Producer-Consumer persistence engine is accessible via Option 13.
 */
public class Main {

    private final ChatService inMemoryChatService;
    private final MessageService inMemoryMessageService;
    private final DatabaseConfig databaseConfig;
    private final JdbcChatService jdbcChatService;
    private final JdbcMessageService jdbcMessageService;
    private final ThreadingSyllabusService threadingService;
    private final ExecutorServiceSyllabusService executorSyllabusService;
    private final DatabaseConnectionDemo databaseDemo;
    private final Phase8SyllabusService phase8Service;
    private final Scanner scanner;
    private UniGramHttpServer apiServer;

    public Main(ChatService chatService, MessageService messageService, Scanner scanner) {
        this(chatService, messageService, new DatabaseConfig(), scanner);
    }

    public Main(ChatService chatService, MessageService messageService, DatabaseConfig databaseConfig, Scanner scanner) {
        this.inMemoryChatService = chatService;
        this.inMemoryMessageService = messageService;
        this.databaseConfig = databaseConfig != null ? databaseConfig : new DatabaseConfig();
        this.jdbcChatService = new JdbcChatService(this.databaseConfig);
        this.jdbcMessageService = new JdbcMessageService(this.databaseConfig, this.jdbcChatService);
        this.threadingService = new ThreadingSyllabusService();
        this.executorSyllabusService = new ExecutorServiceSyllabusService();
        this.databaseDemo = new DatabaseConnectionDemo(this.databaseConfig);
        this.phase8Service = new Phase8SyllabusService();
        this.scanner = scanner;
    }

    public static void main(String[] args) {
        if (args != null && args.length > 0 && ("--server".equalsIgnoreCase(args[0]) || "-s".equalsIgnoreCase(args[0]))) {
            startDirectServer();
            return;
        }

        InMemoryStorage storage = new InMemoryStorage();
        ChatService chatService = new ChatService(storage);
        MessageService messageService = new MessageService(storage, chatService);
        Scanner scanner = new Scanner(System.in);

        Main app = new Main(chatService, messageService, scanner);
        app.seedInitialData();
        app.startInteractiveSession();
    }

    /**
     * Direct CLI entry point to launch the HTTP API server as a daemon.
     */
    public static void startDirectServer() {
        System.out.println("======================================================================");
        System.out.println("           Uni-Gram — Standalone Java HTTP REST API Server            ");
        System.out.println("======================================================================\n");
        DatabaseConfig config = new DatabaseConfig();
        seedDatabaseDirect(config);

        UniGramHttpServer server = new UniGramHttpServer(config);
        try {
            server.start();
            System.out.println("[+] Uni-Gram HTTP Server active at: " + server.getBaseUrl());
            System.out.println("    Endpoints: /api/health, /api/users, /api/products, /api/conversations");
            System.out.println("    Press Ctrl+C to terminate server.\n");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n[!] Shutting down Uni-Gram HTTP Server...");
                server.stop(1);
                System.out.println("[✓] Server stopped cleanly.");
            }));

            Thread.currentThread().join();
        } catch (Exception e) {
            System.err.println("[!] Failed to start HTTP server: " + e.getMessage());
        }
    }

    /**
     * Seeds initial demo students, listings, and conversations into MySQL.
     */
    public static void seedDatabaseDirect(DatabaseConfig dbConfig) {
        if (!dbConfig.isConfigured()) return;
        try (java.sql.Connection conn = DatabaseConnectionFactory.getConnection(dbConfig)) {
            UserJdbcDao userDao = new UserJdbcDao();
            ProductReferenceJdbcDao productDao = new ProductReferenceJdbcDao();
            ConversationJdbcDao convDao = new ConversationJdbcDao();
            MessageJdbcDao messageDao = new MessageJdbcDao();

            // 1. Seed demo student accounts
            if (!userDao.existsById(conn, "student_a")) {
                userDao.insert(conn, new User("student_a", "Satkar Singh", "satkar0696.becse25@chitkara.edu.in"));
            }
            if (!userDao.existsById(conn, "student_b")) {
                userDao.insert(conn, new User("student_b", "Priya K.", "priya.k@chitkara.edu.in"));
            }
            if (!userDao.existsById(conn, "admin_1")) {
                userDao.insert(conn, new User("admin_1", "Campus Admin", "admin@chitkara.edu.in"));
            }

            // Legacy accounts
            if (!userDao.existsById(conn, "usr_student_A")) {
                userDao.insert(conn, new User("usr_student_A", "Alex Chen", "alex.chen@university.edu"));
            }
            if (!userDao.existsById(conn, "usr_student_B")) {
                userDao.insert(conn, new User("usr_student_B", "Maya Patel", "maya.patel@university.edu"));
            }

            // 2. Seed marketplace product references
            if (!productDao.existsById(conn, "1")) {
                productDao.insert(conn, new ProductReference("1", "Calculus"));
            }
            if (!productDao.existsById(conn, "2")) {
                productDao.insert(conn, new ProductReference("2", "Handcrafted Matcha Mug"));
            }
            if (!productDao.existsById(conn, "3")) {
                productDao.insert(conn, new ProductReference("3", "Color Graphing Calculator"));
            }
            if (!productDao.existsById(conn, "4")) {
                productDao.insert(conn, new ProductReference("4", "Botanical Room Spray: Cedar & Wild Lavender (50ml)"));
            }
            if (!productDao.existsById(conn, "5")) {
                productDao.insert(conn, new ProductReference("5", "Organic Chemistry as a Second Language (Klein)"));
            }

            // 3. Seed demo conversation for Product 5 (Organic Chemistry): Buyer=student_b (Priya), Seller=student_a (Satkar)
            Optional<Conversation> chemConv = convDao.findByParticipantsAndProduct(conn, "student_b", "student_a", "5");
            if (chemConv.isEmpty()) {
                String chemConvId = "conv_chem_001";
                convDao.insert(conn, new Conversation(chemConvId, "student_b", "student_a", "5", LocalDateTime.now().minusHours(1)));
                messageDao.insert(conn, new Message("msg_chem_01", chemConvId, "student_b",
                        "Hi Satkar, is the Organic Chemistry book still available for meetup at Library Entrance?", LocalDateTime.now().minusMinutes(45)));
            }

            // 4. Seed demo conversation for Product 1 (Calculus): Buyer=student_a (Satkar), Seller=student_b (Priya)
            Optional<Conversation> calcConv = convDao.findByParticipantsAndProduct(conn, "student_a", "student_b", "1");
            if (calcConv.isEmpty()) {
                String calcConvId = "conv_calc_001";
                convDao.insert(conn, new Conversation(calcConvId, "student_a", "student_b", "1", LocalDateTime.now().minusHours(2)));
                messageDao.insert(conn, new Message("msg_calc_01", calcConvId, "student_a",
                        "Hi Priya! Is your Calculus textbook still available?", LocalDateTime.now().minusMinutes(90)));
                messageDao.insert(conn, new Message("msg_calc_02", calcConvId, "student_b",
                        "Yes Satkar, it is! In great condition with no markings.", LocalDateTime.now().minusMinutes(75)));
            }

        } catch (Exception e) {
            System.out.println("[!] Notice during database seeding: " + e.getMessage());
        }
    }

    /**
     * Seeds initial demo students, listings, and a sample conversation.
     * Seeds both in-memory store (for collections demo) and MySQL database (if reachable and not present).
     */
    public void seedInitialData() {
        // 1. Seed in-memory store for Phase 2 collections demo
        try {
            User studentA = new User("usr_student_A", "Alex Chen", "alex.chen@university.edu");
            User studentB = new User("usr_student_B", "Maya Patel", "maya.patel@university.edu");
            inMemoryChatService.registerUser(studentA);
            inMemoryChatService.registerUser(studentB);

            ProductReference product1 = new ProductReference("prod_cs201_book", "Algorithms & Data Structures (4th Edition)");
            ProductReference product2 = new ProductReference("prod_calc_ti84", "TI-84 Plus Graphing Calculator");
            inMemoryChatService.registerProduct(product1);
            inMemoryChatService.registerProduct(product2);

            Conversation sampleConv = inMemoryChatService.createConversation(
                    studentA.getUserId(),
                    studentB.getUserId(),
                    product1.getProductId()
            );

            inMemoryMessageService.sendMessage(
                    sampleConv.getConversationId(),
                    studentA.getUserId(),
                    "Hi Maya, is your Algorithms textbook still available?"
            );
            inMemoryMessageService.sendMessage(
                    sampleConv.getConversationId(),
                    studentB.getUserId(),
                    "Hi Alex! Yes it is. In great shape, no highlighting."
            );
        } catch (Exception ignored) {
        }

        // 2. Seed MySQL database idempotently if configured and reachable
        seedDatabaseDirect(this.databaseConfig);
    }

    /**
     * Checks if database connection is available before executing normal operations.
     */
    private boolean checkDatabaseOrWarn() {
        if (!databaseConfig.isConfigured()) {
            System.out.println("----------------------------------------------------------------------");
            System.out.println("[!] NOTICE: MySQL Database is not configured or reachable.");
            System.out.println("Normal application messaging operations require a connected database.");
            System.out.println("To configure, set the following environment variables in your shell:");
            System.out.println("  $env:UNIGRAM_DB_URL = \"jdbc:mysql://localhost:3306/unigram_db?serverTimezone=UTC\"");
            System.out.println("  $env:UNIGRAM_DB_USER = \"root\"");
            System.out.println("  $env:UNIGRAM_DB_PASSWORD = \"<your-mysql-password>\"");
            System.out.println("Use Option 12 to test the connection or run demonstrations.");
            System.out.println("----------------------------------------------------------------------\n");
            return false;
        }
        try (java.sql.Connection conn = DatabaseConnectionFactory.getConnection(databaseConfig)) {
            if (!conn.isValid(2)) {
                System.out.println("[!] MySQL connection check failed. Please ensure MySQL Server is running.\n");
                return false;
            }
            return true;
        } catch (Exception e) {
            System.out.println("[!] Database connection error: " + e.getMessage());
            System.out.println("Please ensure MySQL service (MySQL800) is running and credentials are valid.\n");
            return false;
        }
    }

    /**
     * Primary interactive loop.
     */
    public void startInteractiveSession() {
        System.out.println("======================================================================");
        System.out.println("            Welcome to Uni-Gram Interactive Console Engine            ");
        System.out.println("          Student-to-Student Marketplace Messaging Backend            ");
        System.out.println("======================================================================\n");

        boolean running = true;
        while (running) {
            printMainMenu();
            System.out.print("Select an option [0-15]: ");
            String choice = scanner.nextLine().trim();
            System.out.println();

            switch (choice) {
                case "1":
                    handleListStudents();
                    break;
                case "2":
                    handleRegisterStudent();
                    break;
                case "3":
                    handleListProducts();
                    break;
                case "4":
                    handleRegisterProduct();
                    break;
                case "5":
                    handleCreateConversation();
                    break;
                case "6":
                    handleListConversationsForStudent();
                    break;
                case "7":
                    handleOpenConversation();
                    break;
                case "8":
                    System.out.println("--- Running Java Collections Framework Syllabus Demonstration ---");
                    CollectionsDemo.runCollectionsDemonstration();
                    break;
                case "9":
                    handleThreadingMenu();
                    break;
                case "10":
                    handlePhase4Menu();
                    break;
                case "11":
                    handlePhase5Menu();
                    break;
                case "12":
                    handlePhase6Menu();
                    break;
                case "13":
                    handleConcurrentMessagingDemo();
                    break;
                case "14":
                    handlePhase8Menu();
                    break;
                case "15":
                    handlePhase9Menu();
                    break;
                case "0":
                    if (apiServer != null && apiServer.isRunning()) {
                        System.out.println("Stopping background HTTP API Server before exit...");
                        apiServer.stop(1);
                    }
                    System.out.println("Exiting Uni-Gram console safely. Goodbye!");
                    running = false;
                    break;
                default:
                    System.out.println("[!] Invalid choice. Please enter a valid menu number.\n");
            }
        }
    }

    private void printMainMenu() {
        System.out.println("--------------------------- MAIN MENU --------------------------------");
        System.out.println(" 1. View Registered Students (MySQL)");
        System.out.println(" 2. Register New Student (MySQL)");
        System.out.println(" 3. View Available Marketplace Products (MySQL)");
        System.out.println(" 4. Register New Product Listing Reference (MySQL)");
        System.out.println(" 5. Create New Conversation in Database (Buyer, Seller & Product)");
        System.out.println(" 6. View All Conversations for a Student (MySQL)");
        System.out.println(" 7. Open Conversation (Send & Read Messages via MySQL)");
        System.out.println(" 8. Run Collections Syllabus Demonstration (Phase 2 In-Memory)");
        System.out.println(" 9. Thread & Concurrency Fundamentals (Phase 3)");
        System.out.println("10. Synchronization & Producer-Consumer (Phase 4 In-Memory)");
        System.out.println("11. ExecutorService, Callable & Future (Phase 5)");
        System.out.println("12. JDBC & MySQL Database Operations (Phase 6)");
        System.out.println("13. Concurrent MySQL Messaging Engine Demo (Phase 7)");
        System.out.println("14. Lambda, Streams, Optional, Generics & File Handling (Phase 8)");
        System.out.println("15. Standalone HTTP API Server (Phase 9)");
        System.out.println(" 0. Exit Application");
        System.out.println("----------------------------------------------------------------------");
    }

    private void handlePhase8Menu() {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("--- LAMBDA, STREAMS, OPTIONAL, GENERICS & FILE HANDLING (PHASE 8) ----");
            System.out.println(" 1. Functional Interfaces (Predicate, Function, Consumer, Supplier)");
            System.out.println(" 2. Lambda Expressions & Anonymous Inner Class Comparison");
            System.out.println(" 3. Method References (Static, Instance & Constructor References)");
            System.out.println(" 4. Stream API (filter, map, sorted, distinct, reduce, Collectors)");
            System.out.println(" 5. Optional API (Null-defense patterns, orElse, map, flatMap)");
            System.out.println(" 6. Generics & Wildcard Variance (PECS principle)");
            System.out.println(" 7. Modern Date and Time API (java.time calculations & formatting)");
            System.out.println(" 8. Java NIO.2 File Handling (UTF-8 reading, writing & deletion)");
            System.out.println(" 9. CSV Export and Import (RFC 4180 quotes, commas & Unicode)");
            System.out.println("10. Run All Phase 8 Demonstrations Sequentially");
            System.out.println(" 0. Return to Main Menu");
            System.out.println("----------------------------------------------------------------------");
            System.out.print("Select an option [0-10]: ");
            String choice = scanner.nextLine().trim();
            System.out.println();

            switch (choice) {
                case "1":
                    phase8Service.demonstrateFunctionalInterfaces();
                    break;
                case "2":
                    phase8Service.demonstrateLambdaExpressions();
                    break;
                case "3":
                    phase8Service.demonstrateMethodReferences();
                    break;
                case "4":
                    phase8Service.demonstrateStreamApi();
                    break;
                case "5":
                    phase8Service.demonstrateOptional();
                    break;
                case "6":
                    phase8Service.demonstrateGenerics();
                    break;
                case "7":
                    phase8Service.demonstrateDateTime();
                    break;
                case "8":
                    phase8Service.demonstrateFileHandling();
                    break;
                case "9":
                    phase8Service.demonstrateCsvExportAndImport();
                    break;
                case "10":
                    phase8Service.runAllDemonstrations();
                    break;
                case "0":
                    inMenu = false;
                    System.out.println("Returning to Main Menu...\n");
                    break;
                default:
                    System.out.println("[!] Invalid choice. Please enter a number between 0 and 10.\n");
            }
        }
    }

    private void handleThreadingMenu() {
        boolean inThreadingMenu = true;
        while (inThreadingMenu) {
            System.out.println("---------------- THREAD & CONCURRENCY MENU (PHASE 3) -----------------");
            System.out.println(" 1. Extending Thread vs Implementing Runnable");
            System.out.println(" 2. Compare start() vs run()");
            System.out.println(" 3. Demonstrate sleep() and join() Thread Coordination");
            System.out.println(" 4. Demonstrate Thread Interruption & Handling");
            System.out.println(" 5. Observe Thread Lifecycle States (NEW, RUNNABLE, TIMED_WAITING, TERMINATED)");
            System.out.println(" 6. Demonstrate Daemon Thread Behavior");
            System.out.println(" 7. Demonstrate Thread Names, IDs, and Priorities");
            System.out.println(" 8. Run All Threading Demonstrations Sequentially");
            System.out.println(" 0. Return to Main Menu");
            System.out.println("----------------------------------------------------------------------");
            System.out.print("Select an option [0-8]: ");
            String choice = scanner.nextLine().trim();
            System.out.println();

            switch (choice) {
                case "1":
                    threadingService.demonstrateThreadVsRunnable();
                    break;
                case "2":
                    threadingService.demonstrateStartVsRun();
                    break;
                case "3":
                    threadingService.demonstrateSleepAndJoin();
                    break;
                case "4":
                    threadingService.demonstrateInterruption();
                    break;
                case "5":
                    threadingService.demonstrateLifecycleStates();
                    break;
                case "6":
                    threadingService.demonstrateDaemonThread();
                    break;
                case "7":
                    threadingService.demonstrateThreadProperties();
                    break;
                case "8":
                    threadingService.runAllDemonstrations();
                    break;
                case "0":
                    inThreadingMenu = false;
                    break;
                default:
                    System.out.println("[!] Invalid choice. Please enter a number between 0 and 8.\n");
            }
        }
    }

    private void handlePhase4Menu() {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("------------ SYNCHRONIZATION & PRODUCER-CONSUMER (PHASE 4) -----------");
            System.out.println(" 1. Basic Producer-Consumer (Single Producer, Single Consumer)");
            System.out.println(" 2. Bounded Queue Backpressure (Queue Full / Queue Empty Blocking)");
            System.out.println(" 3. Multi-Producer Multi-Consumer (MPMC) Queue Demonstration");
            System.out.println(" 4. Race Condition vs Thread-Safe Counter (synchronized)");
            System.out.println(" 5. Observe Thread States: BLOCKED vs WAITING vs TIMED_WAITING");
            System.out.println(" 6. Safe Queue Shutdown & Draining Demonstration");
            System.out.println(" 7. Run All Phase 4 Demonstrations");
            System.out.println(" 0. Return to Main Menu");
            System.out.println("----------------------------------------------------------------------");
            System.out.print("Select an option [0-7]: ");
            String choice = scanner.nextLine().trim();
            System.out.println();

            switch (choice) {
                case "1":
                    ProducerConsumerDemo.runBasicProducerConsumer();
                    break;
                case "2":
                    ProducerConsumerDemo.runBoundedQueueFullEmptyDemo();
                    break;
                case "3":
                    ProducerConsumerDemo.runMultiProducerMultiConsumer();
                    break;
                case "4":
                    SynchronizationRaceDemo.runRaceConditionDemo();
                    break;
                case "5":
                    AdvancedThreadStatesDemo.demonstrateStates();
                    break;
                case "6":
                    ProducerConsumerDemo.runShutdownDemo();
                    break;
                case "7":
                    ProducerConsumerDemo.runBasicProducerConsumer();
                    ProducerConsumerDemo.runBoundedQueueFullEmptyDemo();
                    ProducerConsumerDemo.runMultiProducerMultiConsumer();
                    SynchronizationRaceDemo.runRaceConditionDemo();
                    AdvancedThreadStatesDemo.demonstrateStates();
                    ProducerConsumerDemo.runShutdownDemo();
                    break;
                case "0":
                    inMenu = false;
                    break;
                default:
                    System.out.println("[!] Invalid choice. Please enter a number between 0 and 7.\n");
            }
        }
    }

    private void handlePhase5Menu() {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("---------- EXECUTORSERVICE, CALLABLE & FUTURE (PHASE 5) -------------");
            System.out.println(" 1. Single-Thread Executor Demonstration");
            System.out.println(" 2. Fixed-Thread Pool Demonstration (3 Workers)");
            System.out.println(" 3. Cached-Thread Pool Demonstration");
            System.out.println(" 4. execute() vs submit() Demonstration");
            System.out.println(" 5. Callable & Future Result Demonstration");
            System.out.println(" 6. Future Exception Handling (ExecutionException)");
            System.out.println(" 7. Future Cancellation & Interruption");
            System.out.println(" 8. ScheduledExecutorService Demonstration");
            System.out.println(" 9. Executor Shutdown Lifecycle Demonstration");
            System.out.println(" 10. Run All Phase 5 Demonstrations");
            System.out.println(" 0. Return to Main Menu");
            System.out.println("----------------------------------------------------------------------");
            System.out.print("Select an option [0-10]: ");
            String choice = scanner.nextLine().trim();
            System.out.println();

            switch (choice) {
                case "1":
                    executorSyllabusService.demonstrateSingleThreadExecutor();
                    break;
                case "2":
                    executorSyllabusService.demonstrateFixedThreadPool();
                    break;
                case "3":
                    executorSyllabusService.demonstrateCachedThreadPool();
                    break;
                case "4":
                    executorSyllabusService.demonstrateExecuteVsSubmit();
                    break;
                case "5":
                    executorSyllabusService.demonstrateCallableAndFuture();
                    break;
                case "6":
                    executorSyllabusService.demonstrateFutureExceptions();
                    break;
                case "7":
                    executorSyllabusService.demonstrateCancellation();
                    break;
                case "8":
                    executorSyllabusService.demonstrateScheduling();
                    break;
                case "9":
                    executorSyllabusService.demonstrateShutdownLifecycle();
                    break;
                case "10":
                    executorSyllabusService.runAllDemonstrations();
                    break;
                case "0":
                    inMenu = false;
                    break;
                default:
                    System.out.println("[!] Invalid choice. Please enter a number between 0 and 10.\n");
            }
        }
    }

    private void handleListStudents() {
        System.out.println("=== Registered Students (MySQL) ===");
        if (!checkDatabaseOrWarn()) return;
        try {
            Collection<User> users = jdbcChatService.getAllUsers();
            if (users.isEmpty()) {
                System.out.println("No students registered in database currently.");
            } else {
                for (User u : users) {
                    System.out.printf("  - ID: %-16s | Name: %-20s | Email: %s%n",
                            u.getUserId(), u.getFullName(), u.getEmail());
                }
            }
            System.out.println();
        } catch (DatabaseException e) {
            System.out.println("[!] Database Error: " + e.getMessage() + "\n");
        }
    }

    private void handleRegisterStudent() {
        System.out.println("=== Register New Student (MySQL) ===");
        if (!checkDatabaseOrWarn()) return;
        try {
            System.out.print("Enter Student ID (e.g., usr_student_C): ");
            String id = scanner.nextLine().trim();
            System.out.print("Enter Full Name: ");
            String name = scanner.nextLine().trim();
            System.out.print("Enter University Email: ");
            String email = scanner.nextLine().trim();

            User user = new User(id, name, email);
            jdbcChatService.registerUser(user);
            System.out.println("[✓] Student persisted to MySQL successfully: " + user.getFullName() + " (" + user.getUserId() + ")\n");
        } catch (IllegalArgumentException e) {
            System.out.println("[!] Validation Error: " + e.getMessage() + "\n");
        } catch (UniGramException e) {
            System.out.println("[!] Business Rule Rejection: " + e.getMessage() + "\n");
        } catch (DatabaseException e) {
            System.out.println("[!] Database Error: " + e.getMessage() + "\n");
        }
    }

    private void handleListProducts() {
        System.out.println("=== Available Marketplace Products (MySQL) ===");
        if (!checkDatabaseOrWarn()) return;
        try {
            Collection<ProductReference> products = jdbcChatService.getAllProducts();
            if (products.isEmpty()) {
                System.out.println("No products registered in database currently.");
            } else {
                for (ProductReference p : products) {
                    System.out.printf("  - ID: %-18s | Title: %s%n", p.getProductId(), p.getProductTitle());
                }
            }
            System.out.println();
        } catch (DatabaseException e) {
            System.out.println("[!] Database Error: " + e.getMessage() + "\n");
        }
    }

    private void handleRegisterProduct() {
        System.out.println("=== Register New Marketplace Product (MySQL) ===");
        if (!checkDatabaseOrWarn()) return;
        try {
            System.out.print("Enter Product ID (e.g., prod_laptop_dell): ");
            String id = scanner.nextLine().trim();
            System.out.print("Enter Product Title: ");
            String title = scanner.nextLine().trim();

            ProductReference product = new ProductReference(id, title);
            jdbcChatService.registerProduct(product);
            System.out.println("[✓] Product persisted to MySQL successfully: " + product.getProductTitle() + " [" + product.getProductId() + "]\n");
        } catch (IllegalArgumentException e) {
            System.out.println("[!] Validation Error: " + e.getMessage() + "\n");
        } catch (UniGramException e) {
            System.out.println("[!] Business Rule Rejection: " + e.getMessage() + "\n");
        } catch (DatabaseException e) {
            System.out.println("[!] Database Error: " + e.getMessage() + "\n");
        }
    }

    private void handleCreateConversation() {
        System.out.println("=== Create New Conversation (MySQL) ===");
        if (!checkDatabaseOrWarn()) return;
        handleListStudents();
        handleListProducts();

        try {
            System.out.print("Enter Buyer User ID  : ");
            String buyerId = scanner.nextLine().trim();
            System.out.print("Enter Seller User ID : ");
            String sellerId = scanner.nextLine().trim();
            System.out.print("Enter Product ID     : ");
            String productId = scanner.nextLine().trim();

            Conversation conv = jdbcChatService.createConversation(buyerId, sellerId, productId);
            System.out.println("[✓] Conversation committed to MySQL successfully!");
            System.out.println("    Conversation ID: " + conv.getConversationId());
            System.out.println("    Buyer          : " + conv.getBuyerId());
            System.out.println("    Seller         : " + conv.getSellerId());
            System.out.println("    Product ID     : " + conv.getProductId());
            System.out.println("    Created At     : " + conv.getCreatedAt() + "\n");
        } catch (UserNotFoundException e) {
            System.out.println("[!] User Not Found: " + e.getMessage() + "\n");
        } catch (UniGramException e) {
            System.out.println("[!] Conversation Creation Rejected: " + e.getMessage() + "\n");
        } catch (IllegalArgumentException e) {
            System.out.println("[!] Validation Error: " + e.getMessage() + "\n");
        } catch (DatabaseException e) {
            System.out.println("[!] Database Error: " + e.getMessage() + "\n");
        }
    }

    private void handleListConversationsForStudent() {
        System.out.println("=== View Student Conversations (MySQL) ===");
        if (!checkDatabaseOrWarn()) return;
        System.out.print("Enter Student ID: ");
        String userId = scanner.nextLine().trim();

        try {
            List<Conversation> conversations = jdbcChatService.getConversationsForUser(userId);
            if (conversations.isEmpty()) {
                System.out.println("No active conversations found in database for student: " + userId + "\n");
                return;
            }

            System.out.println("Active conversations for " + userId + " (" + conversations.size() + " total):");
            for (Conversation c : conversations) {
                ProductReference pr = jdbcChatService.getProduct(c.getProductId());
                int msgCount = jdbcMessageService.getMessageCount(c.getConversationId());
                System.out.printf("  - [ID: %s] Product: \"%s\" | Buyer: %s | Seller: %s | Messages: %d%n",
                        c.getConversationId(), pr.getProductTitle(), c.getBuyerId(), c.getSellerId(), msgCount);
            }
            System.out.println();
        } catch (UserNotFoundException e) {
            System.out.println("[!] Error: " + e.getMessage() + "\n");
        } catch (UniGramException e) {
            System.out.println("[!] Error: " + e.getMessage() + "\n");
        } catch (DatabaseException e) {
            System.out.println("[!] Database Error: " + e.getMessage() + "\n");
        }
    }

    private void handleOpenConversation() {
        System.out.println("=== Open Conversation (MySQL) ===");
        if (!checkDatabaseOrWarn()) return;
        Collection<Conversation> all;
        try {
            all = jdbcChatService.getAllConversations();
        } catch (DatabaseException e) {
            System.out.println("[!] Error retrieving conversations: " + e.getMessage() + "\n");
            return;
        }

        if (all.isEmpty()) {
            System.out.println("No conversations exist in MySQL currently. Create one first.\n");
            return;
        }

        System.out.println("Available Conversations:");
        for (Conversation c : all) {
            try {
                ProductReference pr = jdbcChatService.getProduct(c.getProductId());
                System.out.printf("  -> ID: %-15s | Item: %-35s | Buyer: %-14s | Seller: %s%n",
                        c.getConversationId(), pr.getProductTitle(), c.getBuyerId(), c.getSellerId());
            } catch (UniGramException | DatabaseException e) {
                System.out.printf("  -> ID: %s | Buyer: %s | Seller: %s%n", c.getConversationId(), c.getBuyerId(), c.getSellerId());
            }
        }
        System.out.println();

        System.out.print("Enter Conversation ID to open: ");
        String convId = scanner.nextLine().trim();

        try {
            Conversation conv = jdbcChatService.getConversationById(convId);
            ProductReference pr = jdbcChatService.getProduct(conv.getProductId());

            boolean inChat = true;
            while (inChat) {
                System.out.println("\n----------------------------------------------------------------------");
                System.out.println("Active Conversation: " + conv.getConversationId());
                System.out.println("Product: " + pr.getProductTitle() + " [" + pr.getProductId() + "]");
                System.out.println("Participants: Buyer=" + conv.getBuyerId() + " | Seller=" + conv.getSellerId());
                System.out.println("----------------------------------------------------------------------");
                System.out.println("1. View Complete Message History (MySQL)");
                System.out.println("2. Send a Message (Committed to MySQL)");
                System.out.println("3. View Latest Message (MySQL)");
                System.out.println("4. Return to Main Menu");
                System.out.print("Choose action [1-4]: ");
                String action = scanner.nextLine().trim();
                System.out.println();

                switch (action) {
                    case "1":
                        System.out.print("Enter your Student ID to verify access: ");
                        String viewerId = scanner.nextLine().trim();
                        try {
                            List<Message> history = jdbcMessageService.getMessageHistory(conv.getConversationId(), viewerId);
                            System.out.println("\n--- Message History (" + history.size() + " messages from MySQL) ---");
                            if (history.isEmpty()) {
                                System.out.println("No messages yet in this conversation.");
                            } else {
                                for (int i = 0; i < history.size(); i++) {
                                    Message m = history.get(i);
                                    System.out.printf("  [%d] %-14s (%s):%n      \"%s\"%n",
                                            (i + 1), m.getSenderId(), m.getSentAt(), m.getContent());
                                }
                            }
                        } catch (UnauthorizedConversationAccessException e) {
                            System.out.println("[!] Access Denied: " + e.getMessage());
                        } catch (DatabaseException e) {
                            System.out.println("[!] Database Error: " + e.getMessage());
                        }
                        break;

                    case "2":
                        System.out.print("Enter Sender Student ID: ");
                        String senderId = scanner.nextLine().trim();
                        System.out.print("Enter Message Content: ");
                        String content = scanner.nextLine();

                        try {
                            Message sent = jdbcMessageService.sendMessage(conv.getConversationId(), senderId, content);
                            System.out.println("[✓] Message transaction committed to MySQL successfully!");
                            System.out.println("    Message ID: " + sent.getMessageId());
                            System.out.println("    Timestamp : " + sent.getSentAt());
                        } catch (UnauthorizedConversationAccessException e) {
                            System.out.println("[!] Security Rejection: " + e.getMessage());
                        } catch (InvalidMessageException e) {
                            System.out.println("[!] Validation Rejection: " + e.getMessage());
                        } catch (DatabaseException e) {
                            System.out.println("[!] Database Error: " + e.getMessage());
                        }
                        break;

                    case "3":
                        System.out.print("Enter your Student ID to verify access: ");
                        String requesterId = scanner.nextLine().trim();
                        try {
                            Optional<Message> latest = jdbcMessageService.getLatestMessage(conv.getConversationId(), requesterId);
                            if (latest.isPresent()) {
                                Message m = latest.get();
                                System.out.printf("Latest Message: [%s] at %s:%n  \"%s\"%n",
                                        m.getSenderId(), m.getSentAt(), m.getContent());
                            } else {
                                System.out.println("No messages sent yet in this conversation.");
                            }
                        } catch (UnauthorizedConversationAccessException e) {
                            System.out.println("[!] Access Denied: " + e.getMessage());
                        } catch (DatabaseException e) {
                            System.out.println("[!] Database Error: " + e.getMessage());
                        }
                        break;

                    case "4":
                        inChat = false;
                        break;

                    default:
                        System.out.println("[!] Invalid choice. Enter 1, 2, 3, or 4.");
                }
            }
        } catch (ConversationNotFoundException e) {
            System.out.println("[!] Error: " + e.getMessage() + "\n");
        } catch (UniGramException | DatabaseException e) {
            System.out.println("[!] Error: " + e.getMessage() + "\n");
        }
    }

    private void handlePhase6Menu() {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("\n======================================================================");
            System.out.println("          Uni-Gram — Phase 6: Database & JDBC Operations              ");
            System.out.println("======================================================================");
            System.out.println("  1. Test MySQL Database Connection");
            System.out.println("  2. Display Database Information & Metadata");
            System.out.println("  3. Demonstrate Statement vs PreparedStatement (SQL Injection Prevention)");
            System.out.println("  4. Insert and Retrieve Demonstration Users");
            System.out.println("  5. Update and Delete Demonstration Records");
            System.out.println("  6. Demonstrate Product Reference CRUD Operations");
            System.out.println("  7. Demonstrate Conversation JDBC Operations");
            System.out.println("  8. Demonstrate Message JDBC Operations");
            System.out.println("  9. Demonstrate ResultSet and Java Object Mapping");
            System.out.println(" 10. Demonstrate Transaction Commit and Rollback (ACID)");
            System.out.println(" 11. Run All JDBC Demonstrations Sequentially");
            System.out.println("  0. Return to Main Menu");
            System.out.print("Choose an option [0-11]: ");
            String choice = scanner.nextLine().trim();
            System.out.println();

            switch (choice) {
                case "1":
                    databaseDemo.testConnection();
                    break;
                case "2":
                    databaseDemo.displayDatabaseMetadata();
                    break;
                case "3":
                    databaseDemo.demonstrateStatementVsPreparedStatement();
                    break;
                case "4":
                    databaseDemo.demonstrateUserInsertAndRetrieve();
                    break;
                case "5":
                    databaseDemo.demonstrateUserUpdateAndDelete();
                    break;
                case "6":
                    databaseDemo.demonstrateProductCrud();
                    break;
                case "7":
                    databaseDemo.demonstrateConversationOperations();
                    break;
                case "8":
                    databaseDemo.demonstrateMessageOperations();
                    break;
                case "9":
                    databaseDemo.demonstrateResultSetMapping();
                    break;
                case "10":
                    databaseDemo.demonstrateTransactions();
                    break;
                case "11":
                    databaseDemo.runAllDemonstrations();
                    break;
                case "0":
                    inMenu = false;
                    System.out.println("Returning to Main Menu...\n");
                    break;
                default:
                    System.out.println("[!] Invalid choice. Please enter a number between 0 and 11.\n");
            }
        }
    }

    /**
     * Option 13: Practical demonstration of concurrent message persistence with bounded queue,
     * producers, consumers, and MySQL transactions.
     */
    private void handleConcurrentMessagingDemo() {
        System.out.println("======================================================================");
        System.out.println("  Phase 7: Concurrent MySQL Producer-Consumer Messaging Engine Demo   ");
        System.out.println("======================================================================\n");

        if (!checkDatabaseOrWarn()) return;

        try (java.sql.Connection conn = DatabaseConnectionFactory.getConnection(databaseConfig)) {
            UserJdbcDao userDao = new UserJdbcDao();
            ProductReferenceJdbcDao productDao = new ProductReferenceJdbcDao();
            ConversationJdbcDao conversationDao = new ConversationJdbcDao();
            MessageJdbcDao messageDao = new MessageJdbcDao();

            // Setup demo participants and conversation in MySQL
            User buyer = new User("usr_p7_demo_b", "Concurrent Buyer", "p7_b@demo.edu");
            User seller = new User("usr_p7_demo_s", "Concurrent Seller", "p7_s@demo.edu");
            ProductReference product = new ProductReference("prod_p7_demo_item", "Operating Systems Textbook");

            if (!userDao.existsById(conn, buyer.getUserId())) userDao.insert(conn, buyer);
            if (!userDao.existsById(conn, seller.getUserId())) userDao.insert(conn, seller);
            if (!productDao.existsById(conn, product.getProductId())) productDao.insert(conn, product);

            String convId = "conv_p7_concurrent_demo";
            if (!conversationDao.existsById(conn, convId)) {
                conversationDao.insert(conn, new Conversation(convId, buyer.getUserId(), seller.getUserId(), product.getProductId()));
            }

            System.out.println("1. Initialized MySQL test conversation: " + convId);
            System.out.println("2. Starting ConcurrentJdbcMessageEngine (Capacity: 4, Consumers: 2)...");

            ConcurrentJdbcMessageEngine engine = new ConcurrentJdbcMessageEngine(databaseConfig, 4, 2, 50);
            engine.start();

            // Prepare 6 valid messages from 2 producers
            List<Message> batch1 = new ArrayList<>();
            List<Message> batch2 = new ArrayList<>();
            for (int i = 1; i <= 3; i++) {
                batch1.add(new Message("msg_p7_p1_" + i, convId, buyer.getUserId(), "Producer 1 Message #" + i, LocalDateTime.now()));
                batch2.add(new Message("msg_p7_p2_" + i, convId, seller.getUserId(), "Producer 2 Message #" + i, LocalDateTime.now()));
            }

            System.out.println("3. Spawning 2 concurrent producers to enqueue 6 messages...");
            Thread p1 = new Thread(new JdbcMessageProducer("Producer-1", engine.getQueue(), batch1, engine.getTracker(), 20));
            Thread p2 = new Thread(new JdbcMessageProducer("Producer-2", engine.getQueue(), batch2, engine.getTracker(), 20));

            p1.start();
            p2.start();

            p1.join();
            p2.join();

            System.out.println("\n4. Closing queue and draining remaining messages into MySQL...");
            boolean cleanShutdown = engine.shutdownAndAwait(5000);
            System.out.println("   Queue drained and shutdown clean: " + cleanShutdown);

            System.out.println("\n5. Querying persisted messages directly from MySQL database:");
            List<Message> persistedFromDb = messageDao.findByConversationId(conn, convId);
            for (Message m : persistedFromDb) {
                System.out.printf("   [DB VERIFIED] [%s] %-16s: \"%s\" (at %s)%n",
                        m.getMessageId(), m.getSenderId(), m.getContent(), m.getSentAt());
            }
            System.out.printf("   Total messages persisted to MySQL: %d%n", persistedFromDb.size());

            // Cleanup demo records
            conversationDao.delete(conn, convId);
            productDao.delete(conn, product.getProductId());
            userDao.delete(conn, buyer.getUserId());
            userDao.delete(conn, seller.getUserId());
            System.out.println("[✓] Demonstration test records cleaned up from database.\n");

        } catch (Exception e) {
            System.out.println("[!] Error in concurrent messaging demonstration: " + e.getMessage() + "\n");
        }
    }

    // -------------------------------------------------------------------------
    // Phase 9: Standalone HTTP API Server Menu Handlers
    // -------------------------------------------------------------------------
    private void handlePhase9Menu() {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("-------------- STANDALONE HTTP API SERVER (PHASE 9) ------------------");
            System.out.println(" 1. Start HTTP Server (Background)");
            System.out.println(" 2. Check Server Status");
            System.out.println(" 3. Stop HTTP Server (Graceful Shutdown)");
            System.out.println(" 0. Return to Main Menu");
            System.out.println("----------------------------------------------------------------------");
            System.out.print("Select an option [0-3]: ");
            String subChoice = scanner.nextLine().trim();
            System.out.println();

            switch (subChoice) {
                case "1":
                    handleStartHttpServer();
                    break;
                case "2":
                    handleCheckHttpServerStatus();
                    break;
                case "3":
                    handleStopHttpServer();
                    break;
                case "0":
                    inMenu = false;
                    break;
                default:
                    System.out.println("[!] Invalid choice. Please enter 0, 1, 2, or 3.\n");
            }
        }
    }

    private void handleStartHttpServer() {
        if (apiServer != null && apiServer.isRunning()) {
            System.out.println("[!] HTTP Server is already running on " + apiServer.getBaseUrl() + "\n");
            return;
        }

        System.out.println("Starting Uni-Gram HTTP Server...");
        try {
            if (apiServer == null) {
                apiServer = new UniGramHttpServer(databaseConfig);
            }
            apiServer.start();
            System.out.println("[+] HTTP API Server successfully started in the background!");
            System.out.println("    Base URL:    " + apiServer.getBaseUrl());
            System.out.println("    Endpoints:   /api/health, /api/users, /api/products, /api/conversations");
            System.out.println("    Thread Pool: " + UniGramHttpServer.THREAD_POOL_SIZE + " concurrent workers\n");
        } catch (Exception e) {
            System.out.println("[!] Could not start HTTP server: " + e.getMessage() + "\n");
        }
    }

    private void handleCheckHttpServerStatus() {
        if (apiServer == null || !apiServer.isRunning()) {
            System.out.println("HTTP Server Status: STOPPED (Inactive)\n");
            return;
        }
        System.out.println("HTTP Server Status: RUNNING");
        System.out.println("  Host:        " + apiServer.getHost());
        System.out.println("  Port:        " + apiServer.getPort());
        System.out.println("  Base URL:    " + apiServer.getBaseUrl());
        System.out.println("  Health URL:  " + apiServer.getBaseUrl() + "/api/health");
        System.out.println("  CORS Origin: " + String.join(", ", apiServer.getCorsConfig().getAllowedOrigins()) + "\n");
    }

    private void handleStopHttpServer() {
        if (apiServer == null || !apiServer.isRunning()) {
            System.out.println("[!] HTTP Server is not currently running.\n");
            return;
        }
        System.out.println("Initiating graceful shutdown (2-second drain window)...");
        apiServer.stop(2);
        System.out.println("[+] HTTP API Server stopped cleanly.\n");
    }
}
