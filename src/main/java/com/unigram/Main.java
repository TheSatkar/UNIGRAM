package com.unigram;

import com.unigram.database.DatabaseConfig;
import com.unigram.database.DatabaseConnectionFactory;
import com.unigram.database.DatabaseException;
import com.unigram.exception.ConversationNotFoundException;
import com.unigram.exception.InvalidMessageException;
import com.unigram.exception.UnauthorizedConversationAccessException;
import com.unigram.exception.UniGramException;
import com.unigram.exception.UserNotFoundException;
import com.unigram.model.Conversation;
import com.unigram.model.Message;
import com.unigram.model.User;
import com.unigram.service.JdbcChatService;
import com.unigram.service.JdbcMessageService;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Entry point for the standalone Uni-Gram terminal messaging application.
 */
public class Main {

    private final DatabaseConfig databaseConfig;
    private final JdbcChatService chatService;
    private final JdbcMessageService messageService;
    private final Scanner scanner;

    public Main(DatabaseConfig databaseConfig, Scanner scanner) {
        this.databaseConfig = databaseConfig;
        this.chatService = new JdbcChatService(databaseConfig);
        this.messageService = new JdbcMessageService(databaseConfig, this.chatService);
        this.scanner = scanner;
    }

    public static void main(String[] args) {
        DatabaseConfig config = new DatabaseConfig();
        try (Scanner scanner = new Scanner(System.in)) {
            Main app = new Main(config, scanner);
            app.run();
        }
    }

    public void run() {
        System.out.println("======================================================================");
        System.out.println("                              UNI-GRAM");
        System.out.println("                      Campus Messaging System");
        System.out.println("                        Chitkara University");
        System.out.println("======================================================================\n");

        boolean running = true;
        while (running) {
            printMainMenu();
            System.out.print("Select an option [0-5]: ");
            String choice = scanner.nextLine().trim();
            System.out.println();

            switch (choice) {
                case "1" -> handleViewStudents();
                case "2" -> handleRegisterStudent();
                case "3" -> handleStartConversation();
                case "4" -> handleViewMyConversations();
                case "5" -> handleOpenConversation();
                case "0" -> {
                    System.out.println("Exiting Uni-Gram. Goodbye!");
                    running = false;
                }
                default -> System.out.println("[!] Invalid choice. Please enter a number between 0 and 5.\n");
            }
        }
    }

    private void printMainMenu() {
        System.out.println("MAIN MENU");
        System.out.println("----------------------------------------------------------------------");
        System.out.println("1. View Registered Students");
        System.out.println("2. Register New Student");
        System.out.println("3. Start a Conversation");
        System.out.println("4. View My Conversations");
        System.out.println("5. Open Conversation & Send Messages");
        System.out.println();
        System.out.println("0. Exit Uni-Gram");
        System.out.println("----------------------------------------------------------------------");
    }

    private boolean ensureDatabaseReady() {
        if (!databaseConfig.isConfigured()) {
            System.out.println("[!] Database configuration is missing.");
            System.out.println("    Create a .env file in the project folder using .env.example as a template.\n");
            return false;
        }
        try (Connection conn = DatabaseConnectionFactory.getConnection(databaseConfig)) {
            if (!conn.isValid(2)) {
                System.out.println("[!] Could not validate MySQL connection. Ensure MySQL Server is running.\n");
                return false;
            }
            return true;
        } catch (Exception e) {
            System.out.println("[!] Database error: " + e.getMessage() + "\n");
            return false;
        }
    }

    private void handleViewStudents() {
        System.out.println("--- Registered Students ---");
        if (!ensureDatabaseReady()) return;

        try {
            List<User> students = chatService.getAllUsers();
            if (students.isEmpty()) {
                System.out.println("No students registered yet.\n");
                return;
            }
            for (User student : students) {
                System.out.printf("  - ID: %-14s | Name: %-20s | Email: %s%n",
                        student.getUserId(), student.getFullName(), student.getEmail());
            }
            System.out.println();
        } catch (DatabaseException e) {
            System.out.println("[!] Database Error: " + e.getMessage() + "\n");
        }
    }

    private void handleRegisterStudent() {
        System.out.println("--- Register New Student ---");
        if (!ensureDatabaseReady()) return;

        try {
            System.out.print("Enter Student ID (e.g., student_f): ");
            String id = scanner.nextLine().trim();
            System.out.print("Enter Full Name                  : ");
            String name = scanner.nextLine().trim();
            System.out.print("Enter University Email           : ");
            String email = scanner.nextLine().trim();

            User student = new User(id, name, email);
            chatService.registerUser(student);
            System.out.println("[OK] Student registered: " + student.getFullName() + " (" + student.getUserId() + ")\n");
        } catch (IllegalArgumentException | UniGramException e) {
            System.out.println("[!] Registration rejected: " + e.getMessage() + "\n");
        } catch (DatabaseException e) {
            System.out.println("[!] Database Error: " + e.getMessage() + "\n");
        }
    }

    private void handleStartConversation() {
        System.out.println("--- Start a Conversation ---");
        if (!ensureDatabaseReady()) return;

        try {
            List<User> students = chatService.getAllUsers();
            if (!students.isEmpty()) {
                System.out.println("Registered Students:");
                for (User student : students) {
                    System.out.printf("  - %-14s (%s)%n", student.getUserId(), student.getFullName());
                }
                System.out.println();
            }

            System.out.print("Enter Your Student ID      : ");
            String initiatorId = scanner.nextLine().trim();
            System.out.print("Enter Recipient Student ID : ");
            String recipientId = scanner.nextLine().trim();

            Conversation conv = chatService.startOrGetConversation(initiatorId, recipientId);
            System.out.println("[OK] Conversation ready!");
            System.out.println("     Conversation ID : " + conv.getConversationId());
            System.out.println("     Participants    : " + conv.getParticipantOneId() + " <-> " + conv.getParticipantTwoId());
            System.out.println("     Created At      : " + conv.getCreatedAt() + "\n");
        } catch (UserNotFoundException e) {
            System.out.println("[!] Student not found: " + e.getMessage() + "\n");
        } catch (IllegalArgumentException | UniGramException e) {
            System.out.println("[!] Validation error: " + e.getMessage() + "\n");
        } catch (DatabaseException e) {
            System.out.println("[!] Database Error: " + e.getMessage() + "\n");
        }
    }

    private void handleViewMyConversations() {
        System.out.println("--- View My Conversations ---");
        if (!ensureDatabaseReady()) return;

        System.out.print("Enter Your Student ID: ");
        String studentId = scanner.nextLine().trim();

        try {
            List<Conversation> conversations = chatService.getConversationsForUser(studentId);
            if (conversations.isEmpty()) {
                System.out.println("No conversations found for student: " + studentId + "\n");
                return;
            }

            System.out.println("Conversations for " + studentId + " (" + conversations.size() + "):");
            for (Conversation conv : conversations) {
                String otherId = conv.getOtherParticipantId(studentId);
                String otherDisplay = otherId;
                try {
                    User other = chatService.getUser(otherId);
                    otherDisplay = other.getFullName() + " (" + other.getUserId() + ")";
                } catch (UserNotFoundException ignored) {
                }
                int msgCount = messageService.getMessageCount(conv.getConversationId());
                System.out.printf("  - [ID: %s] With: %-28s | Messages: %d%n",
                        conv.getConversationId(), otherDisplay, msgCount);
            }
            System.out.println();
        } catch (UserNotFoundException e) {
            System.out.println("[!] Student not found: " + e.getMessage() + "\n");
        } catch (DatabaseException e) {
            System.out.println("[!] Database Error: " + e.getMessage() + "\n");
        }
    }

    private void handleOpenConversation() {
        System.out.println("--- Open Conversation & Send Messages ---");
        if (!ensureDatabaseReady()) return;

        try {
            List<Conversation> all = chatService.getAllConversations();
            if (all.isEmpty()) {
                System.out.println("No conversations exist yet. Use Option 3 to start one.\n");
                return;
            }

            System.out.println("Available Conversations:");
            for (Conversation c : all) {
                System.out.printf("  -> ID: %-14s | Participants: %s <-> %s%n",
                        c.getConversationId(), c.getParticipantOneId(), c.getParticipantTwoId());
            }
            System.out.println();

            System.out.print("Enter Conversation ID to open: ");
            String convId = scanner.nextLine().trim();
            Conversation conv = chatService.getConversationById(convId);

            boolean inChat = true;
            while (inChat) {
                System.out.println("\n----------------------------------------------------------------------");
                System.out.println("Conversation ID : " + conv.getConversationId());
                System.out.println("Participants    : " + conv.getParticipantOneId() + " <-> " + conv.getParticipantTwoId());
                System.out.println("----------------------------------------------------------------------");
                System.out.println("1. View Message History");
                System.out.println("2. Send a Message");
                System.out.println("3. View Latest Message");
                System.out.println("4. Return to Main Menu");
                System.out.print("Choose action [1-4]: ");
                String action = scanner.nextLine().trim();
                System.out.println();

                switch (action) {
                    case "1" -> {
                        System.out.print("Enter Your Student ID to verify access: ");
                        String viewerId = scanner.nextLine().trim();
                        try {
                            List<Message> history = messageService.getMessageHistory(conv.getConversationId(), viewerId);
                            System.out.println("\n--- Message History (" + history.size() + " messages) ---");
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
                        }
                    }
                    case "2" -> {
                        System.out.print("Enter Sender Student ID: ");
                        String senderId = scanner.nextLine().trim();
                        System.out.print("Enter Message Content  : ");
                        String content = scanner.nextLine();
                        try {
                            Message sent = messageService.sendMessage(conv.getConversationId(), senderId, content);
                            System.out.println("[OK] Message sent!");
                            System.out.println("     Message ID : " + sent.getMessageId());
                            System.out.println("     Timestamp  : " + sent.getSentAt());
                        } catch (UnauthorizedConversationAccessException e) {
                            System.out.println("[!] Access Denied: " + e.getMessage());
                        } catch (InvalidMessageException e) {
                            System.out.println("[!] Validation Error: " + e.getMessage());
                        }
                    }
                    case "3" -> {
                        System.out.print("Enter Your Student ID to verify access: ");
                        String requesterId = scanner.nextLine().trim();
                        try {
                            Optional<Message> latest = messageService.getLatestMessage(conv.getConversationId(), requesterId);
                            if (latest.isPresent()) {
                                Message m = latest.get();
                                System.out.printf("Latest Message [%s at %s]:%n  \"%s\"%n",
                                        m.getSenderId(), m.getSentAt(), m.getContent());
                            } else {
                                System.out.println("No messages sent yet in this conversation.");
                            }
                        } catch (UnauthorizedConversationAccessException e) {
                            System.out.println("[!] Access Denied: " + e.getMessage());
                        }
                    }
                    case "4" -> inChat = false;
                    default -> System.out.println("[!] Invalid choice. Enter 1, 2, 3, or 4.");
                }
            }
        } catch (ConversationNotFoundException e) {
            System.out.println("[!] Conversation not found: " + e.getMessage() + "\n");
        } catch (DatabaseException e) {
            System.out.println("[!] Database Error: " + e.getMessage() + "\n");
        }
    }
}
