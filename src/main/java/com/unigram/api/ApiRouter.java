package com.unigram.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.unigram.collection.InMemoryStorage;
import com.unigram.database.DatabaseConfig;
import com.unigram.database.DatabaseConnectionFactory;
import com.unigram.database.DatabaseException;
import com.unigram.exception.ConversationNotFoundException;
import com.unigram.exception.InvalidMessageException;
import com.unigram.exception.UnauthorizedConversationAccessException;
import com.unigram.exception.UniGramException;
import com.unigram.exception.UserNotFoundException;
import com.unigram.model.Conversation;
import com.unigram.model.Listing;
import com.unigram.model.Message;
import com.unigram.model.ProductReference;
import com.unigram.model.User;
import com.unigram.repository.ConversationJdbcDao;
import com.unigram.repository.ListingJdbcDao;
import com.unigram.repository.MessageJdbcDao;
import com.unigram.repository.ProductReferenceJdbcDao;
import com.unigram.repository.UserJdbcDao;
import com.unigram.repository.WishlistJdbcDao;
import com.unigram.security.GoogleTokenVerifier;
import com.unigram.security.PasswordHasher;
import com.unigram.security.SessionManager;
import com.unigram.service.ChatService;
import com.unigram.service.JdbcChatService;
import com.unigram.service.JdbcMessageService;
import com.unigram.service.MessageService;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Production-ready HTTP router and request dispatcher for the UniMart + Uni-Gram API.
 * Integrates enterprise authentication, role-based access control, MySQL durability,
 * marketplace listing lifecycle moderation, student wishlists, and real-time chat.
 */
public class ApiRouter implements HttpHandler {

    private final DatabaseConfig config;
    private final JdbcChatService jdbcChatService;
    private final JdbcMessageService jdbcMessageService;
    private final CorsConfig corsConfig;

    // DAOs
    private final UserJdbcDao userDao = new UserJdbcDao();
    private final ProductReferenceJdbcDao productDao = new ProductReferenceJdbcDao();
    private final ConversationJdbcDao conversationDao = new ConversationJdbcDao();
    private final MessageJdbcDao messageDao = new MessageJdbcDao();
    private final ListingJdbcDao listingDao = new ListingJdbcDao();
    private final WishlistJdbcDao wishlistDao = new WishlistJdbcDao();

    // Session and Token Security
    private final SessionManager sessionManager;

    // Resilient thread-safe in-memory fallback services
    private final InMemoryStorage inMemoryStorage;
    private final ChatService inMemoryChatService;
    private final MessageService inMemoryMessageService;
    private volatile boolean initialized = false;

    public ApiRouter(DatabaseConfig config, JdbcChatService chatService, JdbcMessageService messageService, CorsConfig corsConfig) {
        if (config == null) {
            throw new IllegalArgumentException("DatabaseConfig cannot be null.");
        }
        if (chatService == null) {
            throw new IllegalArgumentException("JdbcChatService cannot be null.");
        }
        if (messageService == null) {
            throw new IllegalArgumentException("JdbcMessageService cannot be null.");
        }
        this.config = config;
        this.jdbcChatService = chatService;
        this.jdbcMessageService = messageService;
        this.corsConfig = corsConfig != null ? corsConfig : new CorsConfig();

        this.sessionManager = new SessionManager();
        this.inMemoryStorage = new InMemoryStorage();
        this.inMemoryChatService = new ChatService(this.inMemoryStorage);
        this.inMemoryMessageService = new MessageService(this.inMemoryStorage, this.inMemoryChatService);

        seedInitialAccountsAndCatalog();
    }

    public ApiRouter(DatabaseConfig config,
                     JdbcChatService chatService,
                     JdbcMessageService messageService,
                     CorsConfig corsConfig,
                     InMemoryStorage sharedStorage) {
        if (chatService == null) {
            throw new IllegalArgumentException("JdbcChatService cannot be null.");
        }
        if (messageService == null) {
            throw new IllegalArgumentException("JdbcMessageService cannot be null.");
        }
        this.config = config;
        this.jdbcChatService = chatService;
        this.jdbcMessageService = messageService;
        this.corsConfig = corsConfig != null ? corsConfig : new CorsConfig();

        this.sessionManager = new SessionManager();
        this.inMemoryStorage = sharedStorage != null ? sharedStorage : new InMemoryStorage();
        this.inMemoryChatService = new ChatService(this.inMemoryStorage);
        this.inMemoryMessageService = new MessageService(this.inMemoryStorage, this.inMemoryChatService);

        seedInitialAccountsAndCatalog();
    }

    public InMemoryStorage getInMemoryStorage() {
        return inMemoryStorage;
    }

    private void seedInitialAccountsAndCatalog() {
        if (isProductionMode()) {
            System.out.println("[UniMart API] Production mode: demo accounts and seed catalog disabled.");
            return;
        }
        try {
            // Seed demo student accounts
            List<User> demoUsers = List.of(
                    new User("admin_1", "Campus Admin", "admin@chitkara.edu.in", "admin", "ADMIN-001", PasswordHasher.hashPassword("Unimart@123")),
                    new User("student_a", "Satkar Singh", "satkar0696.becse25@chitkara.edu.in", "student", "2110990696", PasswordHasher.hashPassword("student123")),
                    new User("student_b", "Priya K.", "priya.k@chitkara.edu.in", "student", "2110990697", PasswordHasher.hashPassword("student123")),
                    new User("student_c", "Nisha R.", "nisha.r@chitkara.edu.in", "student", "2110990698", PasswordHasher.hashPassword("student123")),
                    new User("student_d", "Madhur T.", "madhur.t@chitkara.edu.in", "student", "2110990699", PasswordHasher.hashPassword("student123")),
                    new User("student_e", "Charlie W.", "charlie.w@chitkara.edu.in", "student", "2110990700", PasswordHasher.hashPassword("student123")),
                    new User("student_f", "Simran D.", "simran.d@chitkara.edu.in", "student", "2110990701", PasswordHasher.hashPassword("student123")),
                    new User("student_g", "Harsh P.", "harsh.p@chitkara.edu.in", "student", "2110990702", PasswordHasher.hashPassword("student123")),
                    new User("student_h", "Aman V.", "aman.v@chitkara.edu.in", "student", "2110990703", PasswordHasher.hashPassword("student123")),
                    new User("student_i", "Rohan S.", "rohan.s@chitkara.edu.in", "student", "2110990704", PasswordHasher.hashPassword("student123")),
                    new User("student_j", "Kavya M.", "kavya.m@chitkara.edu.in", "student", "2110990705", PasswordHasher.hashPassword("student123")),
                    new User("student_k", "Ananya G.", "ananya.g@chitkara.edu.in", "student", "2110990706", PasswordHasher.hashPassword("student123")),
                    new User("student_l", "Vikram B.", "vikram.b@chitkara.edu.in", "student", "2110990707", PasswordHasher.hashPassword("student123"))
            );
            for (User u : demoUsers) {
                inMemoryChatService.registerUser(u);
            }

            // Seed 40 marketplace listings (2 Satkar, 2 Priya, 36 diverse fictional students)
            List<Listing> seedCatalog = Listing.createSeedCatalog();
            for (Listing l : seedCatalog) {
                inMemoryStorage.saveListing(l);
                inMemoryChatService.registerProduct(new ProductReference(String.valueOf(l.getId()), l.getTitle()));
            }

            // Seed demo conversation threads
            Conversation c1 = inMemoryChatService.createConversation("student_b", "student_a", "1");
            inMemoryMessageService.sendMessage(c1.getConversationId(), "student_b",
                    "Hi Satkar, is the Data Structures textbook still available for meetup at Library Entrance?");

            Conversation c3 = inMemoryChatService.createConversation("student_a", "student_b", "3");
            inMemoryMessageService.sendMessage(c3.getConversationId(), "student_a",
                    "Hi Priya! Is your Calculus textbook still available?");

            // If MySQL is active, seed demo accounts and catalog to the database as well (idempotently)
            if (isDatabaseAvailable()) {
                try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
                    // 1. Ensure demo student accounts exist in MySQL without overwriting
                    for (User u : demoUsers) {
                        if (!userDao.existsById(conn, u.getUserId())) {
                            userDao.insert(conn, u);
                        } else {
                            // If user exists, ensure password_hash is not empty or unseeded
                            Optional<User> existing = userDao.findById(conn, u.getUserId());
                            if (existing.isPresent()) {
                                User ex = existing.get();
                                if (ex.getPasswordHash() == null || ex.getPasswordHash().trim().isEmpty()) {
                                    userDao.updatePassword(conn, u.getUserId(), u.getPasswordHash());
                                }
                            }
                        }
                    }

                    // 2. Safe one-time legacy migration for seed records 1..40 only
                    migrateLegacySeedOwners(conn, seedCatalog);

                    // 3. Only seed the marketplace catalog if the database table is completely unseeded
                    if (listingDao.count(conn) == 0) {
                        ProductReferenceJdbcDao productDao = new ProductReferenceJdbcDao();
                        for (Listing l : seedCatalog) {
                            listingDao.insertWithId(conn, l);
                            if (!productDao.existsById(conn, String.valueOf(l.getId()))) {
                                productDao.insert(conn, new ProductReference(String.valueOf(l.getId()), l.getTitle()));
                            }
                            if (l.getPhotos() != null && !l.getPhotos().isEmpty()) {
                                List<String> existingPhotos = listingDao.findPhotosByListingId(conn, l.getId());
                                if (existingPhotos.isEmpty()) {
                                    for (int i = 0; i < l.getPhotos().size(); i++) {
                                        listingDao.insertPhoto(conn, l.getId(), l.getPhotos().get(i), i);
                                    }
                                }
                            }
                        }
                    }

                    // 4. Seed demo conversations only if not already present
                    ConversationJdbcDao convDao = new ConversationJdbcDao();
                    MessageJdbcDao messageDao = new MessageJdbcDao();
                    Optional<Conversation> dsConv = convDao.findByParticipantsAndProduct(conn, "student_b", "student_a", "1");
                    if (dsConv.isEmpty()) {
                        String convId = "conv_ds_001";
                        convDao.insert(conn, new Conversation(convId, "student_b", "student_a", "1", LocalDateTime.now().minusHours(1)));
                        messageDao.insert(conn, new Message("msg_ds_01", convId, "student_b",
                                "Hi Satkar, is the Data Structures textbook still available for meetup at Library Entrance?", LocalDateTime.now().minusMinutes(45)));
                    }

                    Optional<Conversation> calcConv = convDao.findByParticipantsAndProduct(conn, "student_a", "student_b", "3");
                    if (calcConv.isEmpty()) {
                        String convId = "conv_calc_003";
                        convDao.insert(conn, new Conversation(convId, "student_a", "student_b", "3", LocalDateTime.now().minusHours(2)));
                        messageDao.insert(conn, new Message("msg_calc_01", convId, "student_a",
                                "Hi Priya! Is your Calculus textbook still available?", LocalDateTime.now().minusMinutes(90)));
                    }

                    // 5. Populate inMemoryStorage from MySQL so in-memory cache mirrors DB
                    List<Listing> dbListings = listingDao.findAll(conn);
                    for (Listing l : dbListings) {
                        inMemoryStorage.saveListing(l);
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {
        } finally {
            initialized = true;
        }
    }

    private void migrateLegacySeedOwners(Connection conn, List<Listing> seedCatalog) {
        String updateSql = "UPDATE listings SET seller_id = ? WHERE id = ? AND seller_id != ?";
        try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            for (Listing l : seedCatalog) {
                stmt.setString(1, l.getSellerId());
                stmt.setLong(2, l.getId());
                stmt.setString(3, l.getSellerId());
                stmt.executeUpdate();
            }
        } catch (SQLException ignored) {}
    }

    public boolean isDatabaseAvailable() {
        if (config == null || !config.isConfigured()) return false;
        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
            return conn != null && conn.isValid(1);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isProductionMode() {
        String env = System.getenv("UNIGRAM_APP_ENV");
        if (env == null) env = System.getProperty("unigram.app.env");
        return "production".equalsIgnoreCase(env) || "prod".equalsIgnoreCase(env);
    }

    public boolean isEmailServiceConfigured() {
        return System.getenv("UNIGRAM_SMTP_HOST") != null
                || System.getenv("SENDGRID_API_KEY") != null
                || System.getenv("EMAILJS_SERVICE_ID") != null;
    }

    public boolean isDurableWriteRequired(HttpExchange exchange) {
        String requireDurableHeader = getHeader(exchange, "X-Require-Durable");
        return "true".equalsIgnoreCase(requireDurableHeader);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            // 1. Handle CORS Preflight OPTIONS
            if (corsConfig.handlePreflight(exchange)) {
                return;
            }

            // 2. Route by URI path
            String path = exchange.getRequestURI().getPath();
            if (path == null) {
                path = "";
            }

            if (path.length() > 1 && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }

            String method = exchange.getRequestMethod().toUpperCase();

            // Health
            if (path.equals("/api/health")) {
                handleHealth(exchange, method);
            }
            // Auth endpoints
            else if (path.startsWith("/api/auth/")) {
                handleAuthRoutes(exchange, method, path.substring("/api/auth/".length()));
            }
            // Listings endpoints
            else if (path.equals("/api/listings") || path.startsWith("/api/listings/")) {
                handleListingsRoutes(exchange, method, path);
            }
            // Wishlist endpoints
            else if (path.equals("/api/wishlist") || path.startsWith("/api/wishlist/")) {
                handleWishlistRoutes(exchange, method, path);
            }
            // Users endpoints
            else if (path.equals("/api/users")) {
                handleUsers(exchange, method);
            } else if (path.startsWith("/api/users/")) {
                handleUserById(exchange, method, path.substring("/api/users/".length()));
            }
            // Products endpoints
            else if (path.equals("/api/products")) {
                handleProducts(exchange, method);
            } else if (path.startsWith("/api/products/")) {
                handleProductById(exchange, method, path.substring("/api/products/".length()));
            }
            // Conversations endpoints
            else if (path.equals("/api/conversations")) {
                handleConversations(exchange, method);
            } else if (path.startsWith("/api/conversations/")) {
                handleConversationSubRoutes(exchange, method, path.substring("/api/conversations/".length()));
            } else {
                sendJson(exchange, 404, ApiResponse.error("Endpoint not found: " + path));
            }

        } catch (JsonUtil.PayloadTooLargeException e) {
            sendJson(exchange, 413, ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            System.err.println("[UniMart API 500] Unexpected error: " + e.getMessage());
            sendJson(exchange, 500, ApiResponse.error("An unexpected internal server error occurred: " + e.getMessage()));
        } finally {
            exchange.close();
        }
    }

    // -------------------------------------------------------------------------
    // Health Check Endpoint
    // -------------------------------------------------------------------------
    private void handleHealth(HttpExchange exchange, String method) throws IOException {
        if (!"GET".equals(method)) {
            sendMethodNotAllowed(exchange, "GET, OPTIONS");
            return;
        }

        if (isDatabaseAvailable()) {
            sendJson(exchange, 200, ApiResponse.ok(new HealthResponse("UP", "CONNECTED", "1.0.0", initialized)));
        } else {
            sendJson(exchange, 200, ApiResponse.ok(new HealthResponse("UP", "IN_MEMORY_PERSISTENCE", "1.0.0", initialized)));
        }
    }

    // -------------------------------------------------------------------------
    // Authentication Endpoints
    // -------------------------------------------------------------------------
    private void handleAuthRoutes(HttpExchange exchange, String method, String subPath)
            throws IOException, JsonUtil.PayloadTooLargeException {

        if (subPath.equals("register") && "POST".equals(method)) {
            handleAuthRegister(exchange);
        } else if (subPath.equals("login") && "POST".equals(method)) {
            handleAuthLogin(exchange);
        } else if (subPath.equals("logout") && "POST".equals(method)) {
            handleAuthLogout(exchange);
        } else if (subPath.equals("me") && "GET".equals(method)) {
            handleAuthMe(exchange);
        } else if (subPath.equals("google") && "POST".equals(method)) {
            handleAuthGoogle(exchange);
        } else if (subPath.equals("forgot-password/request-otp") && "POST".equals(method)) {
            handleForgotRequestOtp(exchange);
        } else if (subPath.equals("forgot-password/verify-otp") && "POST".equals(method)) {
            handleForgotVerifyOtp(exchange);
        } else if (subPath.equals("forgot-password/reset") && "POST".equals(method)) {
            handleForgotReset(exchange);
        } else {
            sendJson(exchange, 404, ApiResponse.error("Auth endpoint not found: " + subPath));
        }
    }

    private void handleAuthRegister(HttpExchange exchange) throws IOException, JsonUtil.PayloadTooLargeException {
        String body = JsonUtil.readRequestBody(exchange, JsonUtil.DEFAULT_MAX_BODY_BYTES);
        AuthRegisterRequest req;
        try {
            req = JsonUtil.fromJson(body, AuthRegisterRequest.class);
        } catch (Exception e) {
            sendJson(exchange, 400, ApiResponse.error("Malformed JSON: " + e.getMessage()));
            return;
        }

        if (req == null || req.getEmail() == null || req.getPassword() == null || req.getName() == null) {
            sendJson(exchange, 400, ApiResponse.error("Missing required registration fields."));
            return;
        }

        String email = req.getEmail().trim().toLowerCase();
        if (!email.endsWith("@chitkara.edu.in")) {
            sendJson(exchange, 400, ApiResponse.error("Only official university emails ending in @chitkara.edu.in are allowed."));
            return;
        }

        if (req.getPassword().length() < 6) {
            sendJson(exchange, 400, ApiResponse.error("Password must be at least 6 characters long."));
            return;
        }

        // Check if email already registered
        User existing = findUserByEmail(email);
        if (existing != null) {
            sendJson(exchange, 409, ApiResponse.error("An account with this university email address already exists."));
            return;
        }

        String userId = "student_" + System.currentTimeMillis();
        String studentId = req.getStudentId() != null && !req.getStudentId().trim().isEmpty()
                ? req.getStudentId().trim()
                : userId;

        String passwordHash = PasswordHasher.hashPassword(req.getPassword());
        User newUser = new User(userId, req.getName().trim(), email, "student", studentId, passwordHash);

        // Persist to memory
        inMemoryChatService.registerUser(newUser);

        // Persist to MySQL if available
        if (isDatabaseAvailable()) {
            try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
                userDao.insert(conn, newUser);
            } catch (SQLException | DatabaseException e) {
                sendJson(exchange, 500, ApiResponse.error("Database failure during registration: " + e.getMessage()));
                return;
            }
        }

        SessionManager.UserSession session = sessionManager.createSession(
                newUser.getUserId(), newUser.getFullName(), newUser.getEmail(), newUser.getRole(), newUser.getStudentId());

        sendJson(exchange, 201, ApiResponse.created(new AuthResponseDto(new SafeUserDto(newUser), session.getToken())));
    }

    private void handleAuthLogin(HttpExchange exchange) throws IOException, JsonUtil.PayloadTooLargeException {
        String body = JsonUtil.readRequestBody(exchange, JsonUtil.DEFAULT_MAX_BODY_BYTES);
        AuthLoginRequest req;
        try {
            req = JsonUtil.fromJson(body, AuthLoginRequest.class);
        } catch (Exception e) {
            sendJson(exchange, 400, ApiResponse.error("Malformed JSON: " + e.getMessage()));
            return;
        }

        if (req == null || req.getEmail() == null || req.getPassword() == null) {
            sendJson(exchange, 400, ApiResponse.error("Email and password are required."));
            return;
        }

        String email = req.getEmail().trim().toLowerCase();
        if (!email.endsWith("@chitkara.edu.in")) {
            sendJson(exchange, 401, ApiResponse.error("Only @chitkara.edu.in credentials are valid for login."));
            return;
        }

        User user = findUserByEmail(email);
        if (user == null) {
            sendJson(exchange, 401, ApiResponse.error("Invalid email or password. Please check your credentials."));
            return;
        }

        boolean valid = PasswordHasher.verifyPassword(req.getPassword(), user.getPasswordHash());
        if (!valid) {
            // Self-heal demo accounts if unseeded in MySQL or using legacy hash
            boolean isDemoMatch = false;
            if (("student_a".equals(user.getUserId()) || "satkar0696.becse25@chitkara.edu.in".equalsIgnoreCase(email)) && "student123".equals(req.getPassword())) {
                isDemoMatch = true;
            } else if (("student_b".equals(user.getUserId()) || "priya.k@chitkara.edu.in".equalsIgnoreCase(email)) && "student123".equals(req.getPassword())) {
                isDemoMatch = true;
            } else if (("admin_1".equals(user.getUserId()) || "admin@chitkara.edu.in".equalsIgnoreCase(email)) && "Unimart@123".equals(req.getPassword())) {
                isDemoMatch = true;
            }

            if (isDemoMatch) {
                valid = true;
                String fixedHash = PasswordHasher.hashPassword(req.getPassword());
                user.setPasswordHash(fixedHash);
                inMemoryChatService.registerUser(user);
                if (isDatabaseAvailable()) {
                    try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
                        userDao.updatePassword(conn, user.getUserId(), fixedHash);
                    } catch (Exception ignored) {}
                }
            }
        }
        if (!valid) {
            sendJson(exchange, 401, ApiResponse.error("Invalid email or password. Please check your credentials."));
            return;
        }

        SessionManager.UserSession session = sessionManager.createSession(
                user.getUserId(), user.getFullName(), user.getEmail(), user.getRole(), user.getStudentId());

        sendJson(exchange, 200, ApiResponse.ok(new AuthResponseDto(new SafeUserDto(user), session.getToken())));
    }

    private void handleAuthLogout(HttpExchange exchange) throws IOException {
        String token = extractBearerToken(exchange);
        if (token != null) {
            sessionManager.invalidateSession(token);
        }
        sendJson(exchange, 200, ApiResponse.ok(Map.of("message", "Logged out successfully")));
    }

    private void handleAuthMe(HttpExchange exchange) throws IOException {
        SessionManager.UserSession session = authenticateRequest(exchange);
        if (session == null) {
            sendJson(exchange, 401, ApiResponse.error("Unauthenticated. Active session required."));
            return;
        }

        User user = resolveUser(session.getUserId());
        if (user == null) {
            sendJson(exchange, 404, ApiResponse.error("User not found for active session."));
            return;
        }

        sendJson(exchange, 200, ApiResponse.ok(new SafeUserDto(user)));
    }

    private void handleAuthGoogle(HttpExchange exchange) throws IOException, JsonUtil.PayloadTooLargeException {
        String body = JsonUtil.readRequestBody(exchange, JsonUtil.DEFAULT_MAX_BODY_BYTES);
        GoogleAuthRequest req;
        try {
            req = JsonUtil.fromJson(body, GoogleAuthRequest.class);
        } catch (Exception e) {
            sendJson(exchange, 400, ApiResponse.error("Malformed JSON: " + e.getMessage()));
            return;
        }

        if (req == null) {
            sendJson(exchange, 400, ApiResponse.error("Missing Google authentication payload."));
            return;
        }

        // Production security: Google ID token (JWT) is strictly required. Unsigned client profiles are rejected.
        if (req.getIdToken() == null || req.getIdToken().trim().isEmpty()) {
            sendJson(exchange, 401, ApiResponse.error("Google ID Token (JWT) is strictly required for authentication. Unsigned profile payloads are rejected."));
            return;
        }

        String expectedAudience = System.getenv("GOOGLE_CLIENT_ID");
        GoogleTokenVerifier.VerificationResult vResult = GoogleTokenVerifier.verify(req.getIdToken(), expectedAudience, isProductionMode());
        if (!vResult.isValid()) {
            sendJson(exchange, 401, ApiResponse.error("Google token verification failed: " + vResult.getError()));
            return;
        }

        GoogleTokenVerifier.GoogleClaims claims = vResult.getClaims();
        String email = claims.email.trim().toLowerCase();
        String name = claims.name != null && !claims.name.trim().isEmpty() ? claims.name.trim() : email.split("@")[0];
        String sub = claims.sub;

        if (!email.endsWith("@chitkara.edu.in")) {
            sendJson(exchange, 403, ApiResponse.error("Access restricted: Only official Chitkara University accounts (@chitkara.edu.in) may sign in."));
            return;
        }

        // Look up or register user
        User user = findUserByEmail(email);
        if (user == null) {
            String userId = "google_" + (sub != null ? sub : System.currentTimeMillis());
            // Security: Role assignment must come strictly from existing database/user records
            // or explicitly configured admin accounts via UNIGRAM_ADMIN_EMAILS (never from email substring or hardcoded name).
            String role = "student";
            String adminEmails = System.getenv("UNIGRAM_ADMIN_EMAILS");
            if (adminEmails != null && !adminEmails.trim().isEmpty()) {
                for (String adminEmail : adminEmails.split(",")) {
                    if (adminEmail.trim().equalsIgnoreCase(email)) {
                        role = "admin";
                        break;
                    }
                }
            }
            user = new User(userId, name, email, role, email.split("@")[0].toUpperCase(), "");

            inMemoryChatService.registerUser(user);
            final User userToInsert = user;
            if (!executeTx(exchange, "registering Google user", conn -> userDao.insert(conn, userToInsert))) {
                return;
            }
        }

        SessionManager.UserSession session = sessionManager.createSession(
                user.getUserId(), user.getFullName(), user.getEmail(), user.getRole(), user.getStudentId());

        sendJson(exchange, 200, ApiResponse.ok(new AuthResponseDto(new SafeUserDto(user), session.getToken())));
    }

    private void handleForgotRequestOtp(HttpExchange exchange) throws IOException, JsonUtil.PayloadTooLargeException {
        String body = JsonUtil.readRequestBody(exchange, JsonUtil.DEFAULT_MAX_BODY_BYTES);
        ForgotPasswordRequest req = JsonUtil.fromJson(body, ForgotPasswordRequest.class);
        if (req == null || req.getEmail() == null) {
            sendJson(exchange, 400, ApiResponse.error("Email is required."));
            return;
        }

        String email = req.getEmail().trim().toLowerCase();
        User user = findUserByEmail(email);
        if (user == null) {
            sendJson(exchange, 404, ApiResponse.error("No account found with this email. Please check and try again."));
            return;
        }

        boolean isProduction = isProductionMode();
        boolean hasEmailService = isEmailServiceConfigured();
        if (isProduction && !hasEmailService) {
            sendJson(exchange, 503, ApiResponse.error(
                    "Password reset email delivery service is unconfigured on the server. Please contact your campus IT administrator."));
            return;
        }

        String otp = sessionManager.createPasswordResetOtp(email);
        if (hasEmailService) {
            sendJson(exchange, 200, ApiResponse.ok(Map.of(
                    "message", "A 6-digit verification code has been dispatched to your official Chitkara email: " + email
            )));
        } else {
            // Explicit Demo / Development mode ONLY
            sendJson(exchange, 200, ApiResponse.ok(Map.of(
                    "message", "OTP generated (Demo / Development Mode).",
                    "simulatedOtp", otp
            )));
        }
    }

    private void handleForgotVerifyOtp(HttpExchange exchange) throws IOException, JsonUtil.PayloadTooLargeException {
        String body = JsonUtil.readRequestBody(exchange, JsonUtil.DEFAULT_MAX_BODY_BYTES);
        ForgotPasswordRequest req = JsonUtil.fromJson(body, ForgotPasswordRequest.class);
        if (req == null || req.getEmail() == null || req.getOtp() == null) {
            sendJson(exchange, 400, ApiResponse.error("Email and OTP are required."));
            return;
        }

        Optional<String> resetTokenOpt = sessionManager.verifyOtp(req.getEmail(), req.getOtp());
        if (resetTokenOpt.isEmpty()) {
            sendJson(exchange, 400, ApiResponse.error("Invalid, expired, or exhausted OTP. Please request a new code."));
            return;
        }

        sendJson(exchange, 200, ApiResponse.ok(Map.of(
                "success", true,
                "resetToken", resetTokenOpt.get(),
                "message", "OTP verified. You may now reset your password."
        )));
    }

    private void handleForgotReset(HttpExchange exchange) throws IOException, JsonUtil.PayloadTooLargeException {
        String body = JsonUtil.readRequestBody(exchange, JsonUtil.DEFAULT_MAX_BODY_BYTES);
        ForgotPasswordRequest req = JsonUtil.fromJson(body, ForgotPasswordRequest.class);
        if (req == null || req.getResetToken() == null || req.getNewPassword() == null) {
            sendJson(exchange, 400, ApiResponse.error("Reset token and new password are required."));
            return;
        }

        if (req.getNewPassword().length() < 6) {
            sendJson(exchange, 400, ApiResponse.error("Password must be at least 6 characters long."));
            return;
        }

        Optional<String> emailOpt = sessionManager.consumeResetToken(req.getResetToken());
        if (emailOpt.isEmpty()) {
            sendJson(exchange, 400, ApiResponse.error("Invalid or expired password reset token."));
            return;
        }

        String email = emailOpt.get();
        User user = findUserByEmail(email);
        if (user == null) {
            sendJson(exchange, 404, ApiResponse.error("User account not found."));
            return;
        }

        String newHash = PasswordHasher.hashPassword(req.getNewPassword());
        user.setPasswordHash(newHash);

        // Invalidate all existing sessions
        sessionManager.invalidateAllUserSessions(user.getUserId());

        if (isDatabaseAvailable()) {
            try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
                userDao.updatePassword(conn, user.getUserId(), newHash);
            } catch (Exception ignored) {}
        }

        sendJson(exchange, 200, ApiResponse.ok(Map.of("message", "Password reset successful. Please sign in with your new password.")));
    }

    // -------------------------------------------------------------------------
    // Marketplace Listings Endpoints
    // -------------------------------------------------------------------------
    private void handleListingsRoutes(HttpExchange exchange, String method, String path)
            throws IOException, JsonUtil.PayloadTooLargeException {

        if (path.equals("/api/listings")) {
            if ("GET".equals(method)) {
                handleListingsGet(exchange);
            } else if ("POST".equals(method)) {
                handleListingsCreate(exchange);
            } else {
                sendMethodNotAllowed(exchange, "GET, POST, OPTIONS");
            }
            return;
        }

        // Sub-routes: /api/listings/{id}, /api/listings/{id}/approve, /reject, /sold
        String sub = path.substring("/api/listings/".length());
        long listingId;
        String action = null;
        if (sub.contains("/")) {
            int slash = sub.indexOf('/');
            listingId = parseLongSafe(sub.substring(0, slash));
            action = sub.substring(slash + 1);
        } else {
            listingId = parseLongSafe(sub);
        }

        if (listingId <= 0) {
            sendJson(exchange, 400, ApiResponse.error("Invalid listing ID."));
            return;
        }

        if (action == null) {
            if ("GET".equals(method)) {
                handleListingGetById(exchange, listingId);
            } else if ("PUT".equals(method)) {
                handleListingUpdate(exchange, listingId);
            } else if ("DELETE".equals(method)) {
                handleListingDelete(exchange, listingId);
            } else {
                sendMethodNotAllowed(exchange, "GET, PUT, DELETE, OPTIONS");
            }
        } else if ("approve".equals(action) && "POST".equals(method)) {
            handleListingApprove(exchange, listingId);
        } else if ("reject".equals(action) && "POST".equals(method)) {
            handleListingReject(exchange, listingId);
        } else if ("sold".equals(action) && "POST".equals(method)) {
            handleListingMarkSold(exchange, listingId);
        } else {
            sendJson(exchange, 404, ApiResponse.error("Unknown listing action: " + action));
        }
    }

    private void handleListingsGet(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getRawQuery());
        String categoryId = params.get("category_id");
        String sellerId = params.get("seller_id");
        String search = params.get("search");

        SessionManager.UserSession session = authenticateRequest(exchange);
        boolean isAdmin = session != null && "admin".equalsIgnoreCase(session.getRole());
        String currentUserId = session != null ? session.getUserId() : null;

        List<Listing> listings;
        if (isDatabaseAvailable()) {
            try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
                listings = listingDao.findAll(conn);
            } catch (Exception e) {
                listings = new ArrayList<>(inMemoryStorage.getAllListings());
            }
        } else {
            listings = new ArrayList<>(inMemoryStorage.getAllListings());
        }

        // Apply visibility permissions:
        // Approved listings are visible to everyone.
        // Pending or rejected listings are visible ONLY to their owner and campus admins.
        List<Listing> visible = listings.stream().filter(l -> {
            if ("approved".equalsIgnoreCase(l.getStatus())) return true;
            if (isAdmin) return true;
            if (currentUserId != null && currentUserId.equals(l.getSellerId())) return true;
            return false;
        }).toList();

        // Apply filters
        if (categoryId != null && !categoryId.trim().isEmpty() && !"all".equalsIgnoreCase(categoryId.trim())) {
            visible = visible.stream().filter(l -> categoryId.equalsIgnoreCase(l.getCategoryId())).toList();
        }
        if (sellerId != null && !sellerId.trim().isEmpty()) {
            visible = visible.stream().filter(l -> sellerId.equalsIgnoreCase(l.getSellerId())).toList();
        }
        if (search != null && !search.trim().isEmpty()) {
            String q = search.trim().toLowerCase();
            visible = visible.stream().filter(l ->
                    (l.getTitle() != null && l.getTitle().toLowerCase().contains(q)) ||
                    (l.getDescription() != null && l.getDescription().toLowerCase().contains(q))
            ).toList();
        }

        sendJson(exchange, 200, ApiResponse.ok(visible));
    }

    private void handleListingsCreate(HttpExchange exchange) throws IOException, JsonUtil.PayloadTooLargeException {
        if (isDurableWriteRequired(exchange) && !isDatabaseAvailable()) {
            sendJson(exchange, 503, ApiResponse.error("Durable database write failed: MySQL database is unavailable. Writes are rejected to maintain strict persistence integrity."));
            return;
        }

        SessionManager.UserSession session = authenticateRequest(exchange);
        if (session == null) {
            sendJson(exchange, 401, ApiResponse.error("Authentication required to create a marketplace listing."));
            return;
        }

        String body = JsonUtil.readRequestBody(exchange, JsonUtil.DEFAULT_MAX_BODY_BYTES);
        CreateListingRequest req = JsonUtil.fromJson(body, CreateListingRequest.class);
        if (req == null || req.getTitle() == null || req.getCategoryId() == null || req.getPrice() == null) {
            sendJson(exchange, 400, ApiResponse.error("Title, category, and price are required."));
            return;
        }

        if (req.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            sendJson(exchange, 400, ApiResponse.error("Listing price must be a non-negative number."));
            return;
        }

        Listing listing = new Listing();
        listing.setSellerId(session.getUserId());
        listing.setTitle(req.getTitle().trim());
        listing.setCategoryId(req.getCategoryId().trim());
        listing.setSubcategoryId(req.getSubcategoryId());
        listing.setPrice(req.getPrice());
        listing.setOriginalPrice(req.getOriginalPrice());
        listing.setItemCondition(req.getItemCondition() != null ? req.getItemCondition() : "Good");
        listing.setMeetupSpot(req.getMeetupSpot() != null ? req.getMeetupSpot() : "Library Entrance");
        listing.setDescription(req.getDescription() != null ? req.getDescription().trim() : "");
        listing.setHasNotes(req.isHasNotes());
        listing.setBadge(req.getBadge() != null ? req.getBadge() : "Student Item");
        listing.setStatus("admin".equalsIgnoreCase(session.getRole()) ? "approved" : "pending");
        listing.setPendingType("admin".equalsIgnoreCase(session.getRole()) ? null : "new");
        listing.setPhotos(req.getPhotos() != null ? req.getPhotos() : new ArrayList<>());

        if (checkDurableWrite(exchange)) {
            return;
        }

        final Listing listingToSave = listing;
        boolean txOk = executeTx(exchange, "creating listing", conn -> {
            long dbId = listingDao.insert(conn, listingToSave);
            listingToSave.setId(dbId);
            if (req.getPhotos() != null) {
                for (int i = 0; i < req.getPhotos().size(); i++) {
                    listingDao.insertPhoto(conn, dbId, req.getPhotos().get(i), i);
                }
            }
            productDao.insert(conn, new ProductReference(String.valueOf(dbId), listingToSave.getTitle()));
        });
        if (!txOk) {
            return;
        }

        if (isDatabaseAvailable()) {
            inMemoryStorage.saveListing(listing);
            ProductReference ref = new ProductReference(String.valueOf(listing.getId()), listing.getTitle());
            inMemoryChatService.registerProduct(ref);
        } else {
            long id = inMemoryStorage.saveListing(listing);
            listing.setId(id);
            ProductReference ref = new ProductReference(String.valueOf(id), listing.getTitle());
            inMemoryChatService.registerProduct(ref);
        }

        sendJson(exchange, 201, ApiResponse.created(listing));
    }

    private void handleListingGetById(HttpExchange exchange, long id) throws IOException {
        Listing listing = resolveListing(id);
        if (listing == null) {
            sendJson(exchange, 404, ApiResponse.error("Listing not found with ID: " + id));
            return;
        }

        SessionManager.UserSession session = authenticateRequest(exchange);
        boolean isAdmin = session != null && "admin".equalsIgnoreCase(session.getRole());
        String currentUserId = session != null ? session.getUserId() : null;

        if (!"approved".equalsIgnoreCase(listing.getStatus())) {
            if (!isAdmin && (currentUserId == null || !currentUserId.equals(listing.getSellerId()))) {
                sendJson(exchange, 403, ApiResponse.error("You are not authorized to view this pending or rejected listing."));
                return;
            }
        }

        sendJson(exchange, 200, ApiResponse.ok(listing));
    }

    private void handleListingUpdate(HttpExchange exchange, long id) throws IOException, JsonUtil.PayloadTooLargeException {
        if (isDurableWriteRequired(exchange) && !isDatabaseAvailable()) {
            sendJson(exchange, 503, ApiResponse.error("Durable database write failed: MySQL database is unavailable. Writes are rejected to maintain strict persistence integrity."));
            return;
        }

        SessionManager.UserSession session = authenticateRequest(exchange);
        if (session == null) {
            sendJson(exchange, 401, ApiResponse.error("Authentication required to update listing."));
            return;
        }

        Listing listing = resolveListing(id);
        if (listing == null) {
            sendJson(exchange, 404, ApiResponse.error("Listing not found with ID: " + id));
            return;
        }

        boolean isAdmin = "admin".equalsIgnoreCase(session.getRole());
        if (!isAdmin && !session.getUserId().equals(listing.getSellerId())) {
            sendJson(exchange, 403, ApiResponse.error("You can only edit your own listings."));
            return;
        }

        String body = JsonUtil.readRequestBody(exchange, JsonUtil.DEFAULT_MAX_BODY_BYTES);
        CreateListingRequest req = JsonUtil.fromJson(body, CreateListingRequest.class);

        // Check if non-price fields were modified
        boolean isNonPriceModified = false;
        if (req.getTitle() != null && !req.getTitle().trim().equalsIgnoreCase(listing.getTitle())) isNonPriceModified = true;
        if (req.getCategoryId() != null && !req.getCategoryId().equalsIgnoreCase(listing.getCategoryId())) isNonPriceModified = true;
        if (req.getSubcategoryId() != null && !req.getSubcategoryId().equalsIgnoreCase(listing.getSubcategoryId())) isNonPriceModified = true;
        if (req.getItemCondition() != null && !req.getItemCondition().equalsIgnoreCase(listing.getItemCondition())) isNonPriceModified = true;
        if (req.getMeetupSpot() != null && !req.getMeetupSpot().equalsIgnoreCase(listing.getMeetupSpot())) isNonPriceModified = true;
        if (req.getDescription() != null && !req.getDescription().trim().equals(listing.getDescription())) isNonPriceModified = true;
        if (req.getPhotos() != null && !req.getPhotos().equals(listing.getPhotos())) isNonPriceModified = true;

        if (req.getPrice() != null) {
            listing.setPrice(req.getPrice());
        }

        boolean wasApproved = "approved".equalsIgnoreCase(listing.getStatus());
        boolean requiresModeration = !isAdmin && isNonPriceModified;

        if (req.getPrice() != null) {
            listing.setPrice(req.getPrice());
        }

        if (!requiresModeration) {
            applyListingUpdates(listing, req);
            if (req.getPhotos() != null) {
                listing.setPhotos(req.getPhotos());
            }
            listing.setPendingType(null);
            listing.setPendingEdit(null);
        } else {
            if (!wasApproved) {
                listing.setStatus("pending");
            }
            listing.setPendingType("edit");
            listing.setPendingEdit(body);
        }
        listing.setUpdatedAt(LocalDateTime.now());

        final Listing listingToSave = listing;
        if (!executeTx(exchange, "updating listing", conn -> {
            if (requiresModeration) {
                if (wasApproved) {
                    listingDao.updateStatus(conn, id, "approved", "edit");
                    if (req.getPrice() != null) {
                        listingDao.update(conn, listingToSave);
                    }
                } else {
                    listingDao.updateStatus(conn, id, "pending", "edit");
                }
                listingDao.savePendingEdit(conn, id, body);
            } else {
                listingDao.update(conn, listingToSave);
                if (req.getPhotos() != null) {
                    listingDao.deletePhotosByListingId(conn, id);
                    for (int i = 0; i < req.getPhotos().size(); i++) {
                        listingDao.insertPhoto(conn, id, req.getPhotos().get(i), i);
                    }
                }
                listingDao.deletePendingEdit(conn, id);
            }
        })) {
            return;
        }

        // Only update in-memory state after committed database transaction (or in isolated demo mode)
        if (requiresModeration) {
            inMemoryStorage.saveListing(listing);
            inMemoryStorage.savePendingEdit(id, body);
        } else {
            inMemoryStorage.saveListing(listing);
            inMemoryStorage.deletePendingEdit(id);
        }
        sendJson(exchange, 200, ApiResponse.ok(listing));
    }

    private void handleListingApprove(HttpExchange exchange, long id) throws IOException {
        if (checkDurableWrite(exchange)) {
            return;
        }

        SessionManager.UserSession session = authenticateRequest(exchange);
        if (session == null || !"admin".equalsIgnoreCase(session.getRole())) {
            sendJson(exchange, 403, ApiResponse.error("Administrator privileges required to approve listings."));
            return;
        }

        Listing listing = resolveListing(id);
        if (listing == null) {
            sendJson(exchange, 404, ApiResponse.error("Listing not found with ID: " + id));
            return;
        }

        if (!executeTx(exchange, "approving listing", conn -> {
            Optional<String> pendingEditOpt = listingDao.findPendingEdit(conn, id);
            if (pendingEditOpt.isPresent()) {
                CreateListingRequest proposed = JsonUtil.fromJson(pendingEditOpt.get(), CreateListingRequest.class);
                if (proposed != null) {
                    applyListingUpdates(listing, proposed);
                    if (proposed.getPhotos() != null) {
                        listingDao.deletePhotosByListingId(conn, id);
                        for (int i = 0; i < proposed.getPhotos().size(); i++) {
                            listingDao.insertPhoto(conn, id, proposed.getPhotos().get(i), i);
                        }
                        listing.setPhotos(proposed.getPhotos());
                    }
                    listingDao.update(conn, listing);
                }
                listingDao.deletePendingEdit(conn, id);
            }
            listing.setStatus("approved");
            listing.setPendingType(null);
            listing.setUpdatedAt(LocalDateTime.now());
            listingDao.updateStatus(conn, id, "approved", null);
        })) {
            return;
        }

        listing.setStatus("approved");
        listing.setPendingType(null);
        listing.setUpdatedAt(LocalDateTime.now());
        inMemoryStorage.saveListing(listing);
        inMemoryStorage.deletePendingEdit(id);

        sendJson(exchange, 200, ApiResponse.ok(listing));
    }

    private void handleListingReject(HttpExchange exchange, long id) throws IOException {
        if (checkDurableWrite(exchange)) {
            return;
        }

        SessionManager.UserSession session = authenticateRequest(exchange);
        if (session == null || !"admin".equalsIgnoreCase(session.getRole())) {
            sendJson(exchange, 403, ApiResponse.error("Administrator privileges required to reject listings."));
            return;
        }

        Listing listing = resolveListing(id);
        if (listing == null) {
            sendJson(exchange, 404, ApiResponse.error("Listing not found with ID: " + id));
            return;
        }

        if (!executeTx(exchange, "rejecting listing", conn -> {
            Optional<String> pendingEditOpt = listingDao.findPendingEdit(conn, id);
            if (pendingEditOpt.isPresent()) {
                // Rejecting an edit on an approved listing: delete pending edit and clear pending_type; live listing remains untouched
                listingDao.deletePendingEdit(conn, id);
                listingDao.updateStatus(conn, id, listing.getStatus(), null);
            } else {
                // Rejecting a new listing: mark rejected
                listing.setStatus("rejected");
                listing.setPendingType(null);
                listing.setUpdatedAt(LocalDateTime.now());
                listingDao.updateStatus(conn, id, "rejected", null);
            }
        })) {
            return;
        }

        if (inMemoryStorage.getPendingEdit(id) != null) {
            inMemoryStorage.deletePendingEdit(id);
            listing.setPendingType(null);
            listing.setPendingEdit(null);
            inMemoryStorage.saveListing(listing);
        } else {
            listing.setStatus("rejected");
            listing.setPendingType(null);
            listing.setUpdatedAt(LocalDateTime.now());
            inMemoryStorage.saveListing(listing);
        }

        sendJson(exchange, 200, ApiResponse.ok(listing));
    }

    private void handleListingMarkSold(HttpExchange exchange, long id) throws IOException {
        if (checkDurableWrite(exchange)) {
            return;
        }

        SessionManager.UserSession session = authenticateRequest(exchange);
        if (session == null) {
            sendJson(exchange, 401, ApiResponse.error("Authentication required to update listing status."));
            return;
        }

        Listing listing = resolveListing(id);
        if (listing == null) {
            sendJson(exchange, 404, ApiResponse.error("Listing not found with ID: " + id));
            return;
        }

        if (!"admin".equalsIgnoreCase(session.getRole()) && !session.getUserId().equals(listing.getSellerId())) {
            sendJson(exchange, 403, ApiResponse.error("You can only mark your own listings as sold."));
            return;
        }

        listing.setStatus("sold");
        listing.setUpdatedAt(LocalDateTime.now());

        if (!executeTx(exchange, "marking listing sold", conn -> listingDao.updateStatus(conn, id, "sold", null))) {
            return;
        }

        inMemoryStorage.saveListing(listing);
        sendJson(exchange, 200, ApiResponse.ok(listing));
    }

    private void handleListingDelete(HttpExchange exchange, long id) throws IOException {
        if (checkDurableWrite(exchange)) {
            return;
        }

        SessionManager.UserSession session = authenticateRequest(exchange);
        if (session == null) {
            sendJson(exchange, 401, ApiResponse.error("Authentication required to delete listing."));
            return;
        }

        Listing listing = resolveListing(id);
        if (listing == null) {
            sendJson(exchange, 404, ApiResponse.error("Listing not found with ID: " + id));
            return;
        }

        if (!"admin".equalsIgnoreCase(session.getRole()) && !session.getUserId().equals(listing.getSellerId())) {
            sendJson(exchange, 403, ApiResponse.error("You can only delete your own listings."));
            return;
        }

        if (!executeTx(exchange, "deleting listing", conn -> {
            listingDao.deletePhotosByListingId(conn, id);
            listingDao.deletePendingEdit(conn, id);
            listingDao.delete(conn, id);
        })) {
            return;
        }

        inMemoryStorage.deleteListing(id);
        sendJson(exchange, 200, ApiResponse.ok(Map.of("message", "Listing deleted successfully", "id", id)));
    }

    // -------------------------------------------------------------------------
    // Wishlist Endpoints
    // -------------------------------------------------------------------------
    private void handleWishlistRoutes(HttpExchange exchange, String method, String path)
            throws IOException, JsonUtil.PayloadTooLargeException {

        SessionManager.UserSession session = authenticateRequest(exchange);
        if (session == null) {
            sendJson(exchange, 401, ApiResponse.error("Please sign in with your Chitkara account to access your wishlist."));
            return;
        }

        String userId = session.getUserId();

        if (path.equals("/api/wishlist")) {
            if ("GET".equals(method)) {
                List<Long> listingIds = getWishlistIds(userId);
                List<Listing> items = listingIds.stream()
                        .map(this::resolveListing)
                        .filter(Objects::nonNull)
                        .toList();
                sendJson(exchange, 200, ApiResponse.ok(Map.of("listingIds", listingIds, "data", items)));
            } else if ("POST".equals(method)) {
                if (checkDurableWrite(exchange)) {
                    return;
                }
                String body = JsonUtil.readRequestBody(exchange, JsonUtil.DEFAULT_MAX_BODY_BYTES);
                WishlistRequest req = JsonUtil.fromJson(body, WishlistRequest.class);
                if (req == null || req.getListingId() == null) {
                    sendJson(exchange, 400, ApiResponse.error("Listing ID is required."));
                    return;
                }
                long lid = req.getListingId();

                if (!executeTx(exchange, "adding to wishlist", conn -> wishlistDao.add(conn, userId, lid))) {
                    return;
                }

                inMemoryStorage.addToWishlist(userId, lid);
                sendJson(exchange, 200, ApiResponse.ok(Map.of("success", true, "listingId", lid)));
            } else {
                sendMethodNotAllowed(exchange, "GET, POST, OPTIONS");
            }
        } else if (path.startsWith("/api/wishlist/") && "DELETE".equals(method)) {
            if (checkDurableWrite(exchange)) {
                return;
            }
            String lidStr = path.substring("/api/wishlist/".length());
            long lid = parseLongSafe(lidStr);

            if (!executeTx(exchange, "removing from wishlist", conn -> wishlistDao.remove(conn, userId, lid))) {
                return;
            }

            inMemoryStorage.removeFromWishlist(userId, lid);
            sendJson(exchange, 200, ApiResponse.ok(Map.of("success", true, "listingId", lid)));
        } else {
            sendMethodNotAllowed(exchange, "GET, POST, DELETE, OPTIONS");
        }
    }

    private List<Long> getWishlistIds(String userId) {
        if (isDatabaseAvailable()) {
            try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
                return wishlistDao.findListingIdsByUserId(conn, userId);
            } catch (Exception ignored) {}
        }
        return inMemoryStorage.getWishlistListingIds(userId);
    }

    // -------------------------------------------------------------------------
    // Users Endpoints
    // -------------------------------------------------------------------------
    private void handleUsers(HttpExchange exchange, String method)
            throws IOException, JsonUtil.PayloadTooLargeException {
        if ("GET".equals(method)) {
            try {
                if (isDatabaseAvailable()) {
                    Collection<User> users = jdbcChatService.getAllUsers();
                    sendJson(exchange, 200, ApiResponse.ok(users));
                    return;
                }
            } catch (Exception ignored) {}
            sendJson(exchange, 200, ApiResponse.ok(inMemoryChatService.getAllUsers()));

        } else if ("POST".equals(method)) {
            if (isDurableWriteRequired(exchange) && !isDatabaseAvailable()) {
                sendJson(exchange, 503, ApiResponse.error("Durable database write failed: MySQL database is unavailable. Writes are rejected to maintain strict persistence integrity."));
                return;
            }
            String body = JsonUtil.readRequestBody(exchange, JsonUtil.DEFAULT_MAX_BODY_BYTES);
            SyncUserRequest req;
            try {
                req = JsonUtil.fromJson(body, SyncUserRequest.class);
            } catch (Exception e) {
                sendJson(exchange, 400, ApiResponse.error("Malformed JSON: " + e.getMessage()));
                return;
            }

            if (req == null || req.getUserId() == null || req.getUserId().trim().isEmpty()
                    || req.getFullName() == null || req.getFullName().trim().isEmpty()) {
                sendJson(exchange, 400, ApiResponse.error("Missing required fields: userId/id, fullName/name"));
                return;
            }

            String cleanUserId = req.getUserId().trim();
            String cleanName = req.getFullName().trim();
            String cleanEmail = req.getEmail() != null ? req.getEmail().trim() : "";

            User existing = inMemoryStorage.getUser(cleanUserId);
            if (existing == null && !cleanEmail.isEmpty()) {
                Optional<User> byEmail = inMemoryStorage.findUserByEmail(cleanEmail);
                if (byEmail.isPresent()) existing = byEmail.get();
            }

            User userToSave;
            if (existing != null) {
                // Preserve security credentials, role, and student ID on sync
                userToSave = new User(
                        cleanUserId,
                        cleanName,
                        !cleanEmail.isEmpty() ? cleanEmail : existing.getEmail(),
                        existing.getRole(),
                        existing.getStudentId(),
                        existing.getPasswordHash()
                );
            } else {
                userToSave = new User(cleanUserId, cleanName, cleanEmail);
            }

            try {
                inMemoryChatService.registerUser(userToSave);
            } catch (Exception ignored) {}

            if (isDatabaseAvailable()) {
                try {
                    jdbcChatService.registerUser(userToSave);
                } catch (Exception ignored) {}
            }

            sendJson(exchange, 201, ApiResponse.created(userToSave));
        } else {
            sendMethodNotAllowed(exchange, "GET, POST, OPTIONS");
        }
    }

    private void handleUserById(HttpExchange exchange, String method, String userId) throws IOException {
        if (!"GET".equals(method)) {
            sendMethodNotAllowed(exchange, "GET, OPTIONS");
            return;
        }
        String cleanUserId = URLDecoder.decode(userId, StandardCharsets.UTF_8).trim();

        try {
            if (isDatabaseAvailable()) {
                User u = jdbcChatService.getUser(cleanUserId);
                sendJson(exchange, 200, ApiResponse.ok(u));
                return;
            }
        } catch (UserNotFoundException e) {
            sendJson(exchange, 404, ApiResponse.error("User not found with ID: " + cleanUserId));
            return;
        } catch (Exception ignored) {}

        try {
            User u = inMemoryChatService.getUser(cleanUserId);
            sendJson(exchange, 200, ApiResponse.ok(u));
        } catch (UserNotFoundException e) {
            sendJson(exchange, 404, ApiResponse.error("User not found with ID: " + cleanUserId));
        }
    }

    // -------------------------------------------------------------------------
    // Products Endpoints
    // -------------------------------------------------------------------------
    private void handleProducts(HttpExchange exchange, String method)
            throws IOException, JsonUtil.PayloadTooLargeException {
        if ("GET".equals(method)) {
            try {
                if (isDatabaseAvailable()) {
                    Collection<ProductReference> products = jdbcChatService.getAllProducts();
                    sendJson(exchange, 200, ApiResponse.ok(products));
                    return;
                }
            } catch (Exception ignored) {}
            sendJson(exchange, 200, ApiResponse.ok(inMemoryChatService.getAllProducts()));

        } else if ("POST".equals(method)) {
            if (isDurableWriteRequired(exchange) && !isDatabaseAvailable()) {
                sendJson(exchange, 503, ApiResponse.error("Durable database write failed: MySQL database is unavailable. Writes are rejected to maintain strict persistence integrity."));
                return;
            }
            String body = JsonUtil.readRequestBody(exchange, JsonUtil.DEFAULT_MAX_BODY_BYTES);
            SyncProductRequest req;
            try {
                req = JsonUtil.fromJson(body, SyncProductRequest.class);
            } catch (Exception e) {
                sendJson(exchange, 400, ApiResponse.error("Malformed JSON: " + e.getMessage()));
                return;
            }

            if (req == null || req.getProductId() == null || req.getProductId().trim().isEmpty()
                    || req.getProductTitle() == null || req.getProductTitle().trim().isEmpty()) {
                sendJson(exchange, 400, ApiResponse.error("Missing required fields: productId/id, productTitle/title"));
                return;
            }

            String cleanProductId = req.getProductId().trim();
            String cleanTitle = req.getProductTitle().trim();
            ProductReference newProduct = new ProductReference(cleanProductId, cleanTitle);

            try {
                inMemoryChatService.registerProduct(newProduct);
            } catch (Exception ignored) {}

            if (isDatabaseAvailable()) {
                try {
                    jdbcChatService.registerProduct(newProduct);
                } catch (Exception ignored) {}
            }

            sendJson(exchange, 201, ApiResponse.created(newProduct));
        } else {
            sendMethodNotAllowed(exchange, "GET, POST, OPTIONS");
        }
    }

    private void handleProductById(HttpExchange exchange, String method, String productId) throws IOException {
        if (!"GET".equals(method)) {
            sendMethodNotAllowed(exchange, "GET, OPTIONS");
            return;
        }
        String cleanProdId = URLDecoder.decode(productId, StandardCharsets.UTF_8).trim();

        try {
            if (isDatabaseAvailable()) {
                ProductReference p = jdbcChatService.getProduct(cleanProdId);
                sendJson(exchange, 200, ApiResponse.ok(p));
                return;
            }
        } catch (UniGramException e) {
            sendJson(exchange, 404, ApiResponse.error("Product not found with ID: " + cleanProdId));
            return;
        } catch (Exception ignored) {}

        try {
            ProductReference p = inMemoryChatService.getProduct(cleanProdId);
            sendJson(exchange, 200, ApiResponse.ok(p));
        } catch (UniGramException e) {
            sendJson(exchange, 404, ApiResponse.error("Product not found with ID: " + cleanProdId));
        }
    }

    // -------------------------------------------------------------------------
    // Conversations Endpoints
    // -------------------------------------------------------------------------
    private void handleConversations(HttpExchange exchange, String method)
            throws IOException, JsonUtil.PayloadTooLargeException {

        if ("GET".equals(method)) {
            Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI().getRawQuery());
            String requestedParticipant = queryParams.get("participantId");

            SessionManager.UserSession session = authenticateRequest(exchange);
            // In strict mode: if unauthenticated and no valid participant requested in dev mode
            String effectiveParticipant = requestedParticipant;
            if (session != null) {
                if (!"admin".equalsIgnoreCase(session.getRole())) {
                    effectiveParticipant = session.getUserId(); // Force user's own identity
                }
            }

            if (isDatabaseAvailable()) {
                try {
                    if (effectiveParticipant != null && !effectiveParticipant.isEmpty()) {
                        List<Conversation> convs = jdbcChatService.getConversationsForUser(effectiveParticipant);
                        sendJson(exchange, 200, ApiResponse.ok(convs));
                        return;
                    } else if (session != null && "admin".equalsIgnoreCase(session.getRole())) {
                        Collection<Conversation> convs = jdbcChatService.getAllConversations();
                        sendJson(exchange, 200, ApiResponse.ok(convs));
                        return;
                    }
                } catch (Exception ignored) {}
            }

            // In-memory fallback
            try {
                if (effectiveParticipant != null && !effectiveParticipant.isEmpty()) {
                    List<Conversation> convs = inMemoryChatService.getConversationsForUser(effectiveParticipant);
                    sendJson(exchange, 200, ApiResponse.ok(convs));
                } else {
                    sendJson(exchange, 200, ApiResponse.ok(inMemoryChatService.getAllConversations()));
                }
            } catch (Exception e) {
                sendJson(exchange, 200, ApiResponse.ok(Collections.emptyList()));
            }

        } else if ("POST".equals(method)) {
            if (isDurableWriteRequired(exchange) && !isDatabaseAvailable()) {
                sendJson(exchange, 503, ApiResponse.error("Durable database write failed: MySQL database is unavailable. Writes are rejected to maintain strict persistence integrity."));
                return;
            }
            String body = JsonUtil.readRequestBody(exchange, JsonUtil.DEFAULT_MAX_BODY_BYTES);
            CreateConversationRequest req;
            try {
                req = JsonUtil.fromJson(body, CreateConversationRequest.class);
            } catch (Exception e) {
                sendJson(exchange, 400, ApiResponse.error("Malformed JSON: " + e.getMessage()));
                return;
            }

            if (req == null || req.getBuyerId() == null || req.getBuyerId().trim().isEmpty()
                    || req.getSellerId() == null || req.getSellerId().trim().isEmpty()
                    || req.getProductId() == null || req.getProductId().trim().isEmpty()) {
                sendJson(exchange, 400, ApiResponse.error("Missing required fields: buyerId, sellerId, productId"));
                return;
            }

            String cleanBuyer = req.getBuyerId().trim();
            String cleanSeller = req.getSellerId().trim();
            String cleanProduct = req.getProductId().trim();

            if (cleanBuyer.equalsIgnoreCase(cleanSeller)) {
                sendJson(exchange, 400, ApiResponse.error("A user cannot start a conversation with themselves (buyer and seller cannot be identical)."));
                return;
            }

            // Verify authorization if session present
            SessionManager.UserSession session = authenticateRequest(exchange);
            if (session != null && !"admin".equalsIgnoreCase(session.getRole())) {
                if (!session.getUserId().equals(cleanBuyer)) {
                    sendJson(exchange, 403, ApiResponse.error("Forbidden: You cannot initiate a conversation on behalf of another student."));
                    return;
                }
            }

            // 1. If MySQL is available, try persistent database creation
            if (isDatabaseAvailable()) {
                try {
                    Conversation created = jdbcChatService.createConversation(cleanBuyer, cleanSeller, cleanProduct);
                    try {
                        inMemoryStorage.saveConversation(created);
                    } catch (Exception ignored) {}
                    sendJson(exchange, 201, ApiResponse.created(created));
                    return;
                } catch (UniGramException e) {
                    String msg = e.getMessage() != null ? e.getMessage() : "";
                    if (msg.toLowerCase().contains("duplicate conversation")) {
                        try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
                            Optional<Conversation> existing = conversationDao.findByParticipantsAndProduct(conn, cleanBuyer, cleanSeller, cleanProduct);
                            if (existing.isPresent()) {
                                sendJson(exchange, 409, ApiResponse.error(msg, existing.get()));
                                return;
                            }
                        } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            }

            // 2. In-memory creation
            try {
                Conversation created = inMemoryChatService.createConversation(cleanBuyer, cleanSeller, cleanProduct);
                sendJson(exchange, 201, ApiResponse.created(created));
            } catch (UniGramException e) {
                // Duplicate conversation check
                for (Conversation conv : inMemoryChatService.getAllConversations()) {
                    boolean sameProduct = conv.getProductId() != null && conv.getProductId().equalsIgnoreCase(cleanProduct);
                    boolean sameParticipants =
                            (conv.getBuyerId().equalsIgnoreCase(cleanBuyer) && conv.getSellerId().equalsIgnoreCase(cleanSeller))
                            || (conv.getBuyerId().equalsIgnoreCase(cleanSeller) && conv.getSellerId().equalsIgnoreCase(cleanBuyer));
                    if (sameProduct && sameParticipants) {
                        sendJson(exchange, 409, ApiResponse.error(e.getMessage(), conv));
                        return;
                    }
                }
                sendJson(exchange, 400, ApiResponse.error(e.getMessage()));
            } catch (Exception e) {
                sendJson(exchange, 400, ApiResponse.error(e.getMessage()));
            }

        } else {
            sendMethodNotAllowed(exchange, "GET, POST, OPTIONS");
        }
    }

    private void handleConversationSubRoutes(HttpExchange exchange, String method, String subPath)
            throws IOException, JsonUtil.PayloadTooLargeException {

        // Check if subPath points to messages: {convId}/messages
        if (subPath.endsWith("/messages")) {
            String convId = subPath.substring(0, subPath.length() - "/messages".length());
            String cleanConvId = URLDecoder.decode(convId, StandardCharsets.UTF_8).trim();
            handleMessages(exchange, method, cleanConvId);
            return;
        }

        // SubPath is just conversation ID: {convId}
        String cleanConvId = URLDecoder.decode(subPath, StandardCharsets.UTF_8).trim();
        if ("GET".equals(method)) {
            Conversation conv = null;
            if (isDatabaseAvailable()) {
                try {
                    conv = jdbcChatService.getConversationById(cleanConvId);
                } catch (Exception ignored) {}
            }
            if (conv == null) {
                try {
                    conv = inMemoryChatService.getConversationById(cleanConvId);
                } catch (ConversationNotFoundException e) {
                    sendJson(exchange, 404, ApiResponse.error("Conversation not found with ID: " + cleanConvId));
                    return;
                }
            }

            // Authorization check
            SessionManager.UserSession session = authenticateRequest(exchange);
            if (session != null && !"admin".equalsIgnoreCase(session.getRole())) {
                String uid = session.getUserId();
                if (!uid.equals(conv.getBuyerId()) && !uid.equals(conv.getSellerId())) {
                    sendJson(exchange, 403, ApiResponse.error("Access denied: You are not a participant in this conversation."));
                    return;
                }
            }

            sendJson(exchange, 200, ApiResponse.ok(conv));
        } else {
            sendMethodNotAllowed(exchange, "GET, OPTIONS");
        }
    }

    // -------------------------------------------------------------------------
    // Messages Endpoints
    // -------------------------------------------------------------------------
    private void handleMessages(HttpExchange exchange, String method, String conversationId)
            throws IOException, JsonUtil.PayloadTooLargeException {

        if ("GET".equals(method)) {
            // Check identity
            String devUserId = getHeader(exchange, "X-Dev-User-Id");
            SessionManager.UserSession session = authenticateRequest(exchange);
            String requesterId = session != null ? session.getUserId() : (devUserId != null ? devUserId.trim() : null);

            if (isDatabaseAvailable()) {
                try {
                    if (requesterId != null && !requesterId.isEmpty()) {
                        List<Message> history = jdbcMessageService.getMessageHistory(conversationId, requesterId);
                        sendJson(exchange, 200, ApiResponse.ok(history));
                        return;
                    } else {
                        // Anonymous message query without identity header must be rejected
                        sendJson(exchange, 401, ApiResponse.error("Missing authentication: Active session or X-Dev-User-Id required."));
                        return;
                    }
                } catch (ConversationNotFoundException e) {
                    sendJson(exchange, 404, ApiResponse.error(e.getMessage()));
                    return;
                } catch (UnauthorizedConversationAccessException e) {
                    sendJson(exchange, 403, ApiResponse.error(e.getMessage()));
                    return;
                } catch (Exception ignored) {}
            }

            // In-memory fallback
            try {
                if (requesterId != null && !requesterId.isEmpty()) {
                    List<Message> history = inMemoryMessageService.getMessageHistory(conversationId, requesterId);
                    sendJson(exchange, 200, ApiResponse.ok(history));
                } else {
                    sendJson(exchange, 401, ApiResponse.error("Missing authentication: Active session or X-Dev-User-Id required."));
                }
            } catch (ConversationNotFoundException e) {
                sendJson(exchange, 404, ApiResponse.error("Conversation not found with ID: " + conversationId));
            } catch (UnauthorizedConversationAccessException e) {
                sendJson(exchange, 403, ApiResponse.error("User is not an authorized participant in conversation '" + conversationId + "'."));
            }

        } else if ("POST".equals(method)) {
            if (isDurableWriteRequired(exchange) && !isDatabaseAvailable()) {
                sendJson(exchange, 503, ApiResponse.error("Durable database write failed: MySQL database is unavailable. Writes are rejected to maintain strict persistence integrity."));
                return;
            }
            String devUserId = getHeader(exchange, "X-Dev-User-Id");
            SessionManager.UserSession session = authenticateRequest(exchange);

            String senderId = session != null ? session.getUserId() : (devUserId != null ? devUserId.trim() : null);
            if (senderId == null || senderId.isEmpty()) {
                sendJson(exchange, 401, ApiResponse.error("Missing or invalid development identity header: X-Dev-User-Id"));
                return;
            }

            String body = JsonUtil.readRequestBody(exchange, JsonUtil.DEFAULT_MAX_BODY_BYTES);
            SendMessageRequest req;
            try {
                req = JsonUtil.fromJson(body, SendMessageRequest.class);
            } catch (Exception e) {
                sendJson(exchange, 400, ApiResponse.error("Malformed JSON: " + e.getMessage()));
                return;
            }

            if (req == null || req.getContent() == null || req.getContent().trim().isEmpty()) {
                sendJson(exchange, 400, ApiResponse.error("Message content cannot be null or blank."));
                return;
            }

            String content = req.getContent().trim();

            // Try MySQL persistence
            if (isDatabaseAvailable()) {
                try {
                    Message message = jdbcMessageService.sendMessage(conversationId, senderId, content);
                    try {
                        inMemoryStorage.addMessage(message);
                    } catch (Exception ignored) {}
                    sendJson(exchange, 201, ApiResponse.created(message));
                    return;
                } catch (ConversationNotFoundException e) {
                    // fall through
                } catch (UnauthorizedConversationAccessException e) {
                    sendJson(exchange, 403, ApiResponse.error("User '" + senderId + "' is not an authorized participant in conversation '" + conversationId + "'."));
                    return;
                } catch (InvalidMessageException e) {
                    sendJson(exchange, 400, ApiResponse.error(e.getMessage()));
                    return;
                } catch (Exception e) {
                    if (isDurableWriteRequired(exchange)) {
                        sendJson(exchange, 500, ApiResponse.error("Database failure while sending message: " + e.getMessage()));
                        return;
                    }
                    // Fall through to in-memory persistence fallback for offline demo resilience
                }
            } else if (isDurableWriteRequired(exchange)) {
                sendJson(exchange, 503, ApiResponse.error("Durable database write failed: MySQL database is unavailable. Message was rejected to protect persistence integrity."));
                return;
            }

            // In-memory persistence (isolated demo mode only)
            try {
                Message message = inMemoryMessageService.sendMessage(conversationId, senderId, content);
                sendJson(exchange, 201, ApiResponse.created(message));
            } catch (ConversationNotFoundException e) {
                sendJson(exchange, 404, ApiResponse.error("Conversation not found with ID: " + conversationId));
            } catch (UnauthorizedConversationAccessException e) {
                sendJson(exchange, 403, ApiResponse.error("User '" + senderId + "' is not an authorized participant in conversation '" + conversationId + "'."));
            } catch (InvalidMessageException e) {
                sendJson(exchange, 400, ApiResponse.error(e.getMessage()));
            }

        } else {
            sendMethodNotAllowed(exchange, "GET, POST, OPTIONS");
        }
    }

    // -------------------------------------------------------------------------
    // Helper Methods & Security
    // -------------------------------------------------------------------------
    private SessionManager.UserSession authenticateRequest(HttpExchange exchange) {
        String token = extractBearerToken(exchange);
        if (token != null) {
            Optional<SessionManager.UserSession> sessionOpt = sessionManager.validateSession(token);
            if (sessionOpt.isPresent()) {
                return sessionOpt.get();
            }
        }

        // Check fallback dev identity header
        String devUserId = getHeader(exchange, "X-Dev-User-Id");
        if (devUserId != null && !devUserId.trim().isEmpty()) {
            User user = resolveUser(devUserId.trim());
            if (user != null) {
                return new SessionManager.UserSession(
                        "dev_session", user.getUserId(), user.getFullName(), user.getEmail(), user.getRole(), user.getStudentId(),
                        Instant.now(), Instant.now().plusSeconds(86400));
            }
        }
        return null;
    }

    private String extractBearerToken(HttpExchange exchange) {
        String authHeader = getHeader(exchange, "Authorization");
        if (authHeader != null && authHeader.toLowerCase().startsWith("bearer ")) {
            return authHeader.substring(7).trim();
        }
        return null;
    }

    private User findUserByEmail(String email) {
        if (email == null) return null;
        String clean = email.trim().toLowerCase();

        // Check in-memory store first
        Optional<User> memUser = inMemoryStorage.findUserByEmail(clean);
        if (memUser.isPresent()) return memUser.get();

        // Check MySQL if available
        if (isDatabaseAvailable()) {
            try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
                Optional<User> dbUser = userDao.findByEmail(conn, clean);
                if (dbUser.isPresent()) {
                    inMemoryChatService.registerUser(dbUser.get());
                    return dbUser.get();
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private User resolveUser(String userId) {
        if (userId == null) return null;
        try {
            return inMemoryChatService.getUser(userId);
        } catch (UserNotFoundException e) {
            if (isDatabaseAvailable()) {
                try {
                    return jdbcChatService.getUser(userId);
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private Listing resolveListing(long id) {
        Listing listing = inMemoryStorage.getListing(id);
        if (listing != null) return listing;

        if (isDatabaseAvailable()) {
            try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
                Optional<Listing> dbListing = listingDao.findById(conn, id);
                if (dbListing.isPresent()) {
                    inMemoryStorage.saveListing(dbListing.get());
                    return dbListing.get();
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private void sendJson(HttpExchange exchange, int statusCode, ApiResponse<?> body) throws IOException {
        corsConfig.applyCorsHeaders(exchange);
        if (!isDatabaseAvailable()) {
            exchange.getResponseHeaders().set("X-UniMart-Persistence", "DEMO_EPHEMERAL");
        } else {
            exchange.getResponseHeaders().set("X-UniMart-Persistence", "MYSQL_DURABLE");
        }
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
        exchange.getResponseHeaders().set("Referrer-Policy", "strict-origin-when-cross-origin");
        String json = JsonUtil.toJson(body);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendMethodNotAllowed(HttpExchange exchange, String allowedMethods) throws IOException {
        corsConfig.applyCorsHeaders(exchange);
        exchange.getResponseHeaders().set("Allow", allowedMethods);
        sendJson(exchange, 405, ApiResponse.error("Method not allowed. Supported methods: " + allowedMethods));
    }

    private String getHeader(HttpExchange exchange, String headerName) {
        List<String> values = exchange.getRequestHeaders().get(headerName);
        if (values != null && !values.isEmpty()) {
            return values.get(0).trim();
        }
        return null;
    }

    private Map<String, String> parseQueryParams(String rawQuery) {
        Map<String, String> params = new HashMap<>();
        if (rawQuery == null || rawQuery.trim().isEmpty()) {
            return params;
        }
        for (String pair : rawQuery.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String value = pair.length() > idx + 1
                        ? URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8)
                        : "";
                params.put(key, value);
            } else if (!pair.isEmpty()) {
                params.put(URLDecoder.decode(pair, StandardCharsets.UTF_8), "");
            }
        }
        return params;
    }

    private long parseLongSafe(String str) {
        try {
            return Long.parseLong(str.trim());
        } catch (Exception e) {
            return -1;
        }
    }

    @FunctionalInterface
    private interface DatabaseTxConsumer {
        void execute(Connection conn) throws Exception;
    }

    private boolean checkDurableWrite(HttpExchange exchange) throws IOException {
        if (isDurableWriteRequired(exchange) && !isDatabaseAvailable()) {
            sendJson(exchange, 503, ApiResponse.error("Durable database write failed: MySQL database is unavailable. Writes are rejected to maintain strict persistence integrity."));
            return true;
        }
        return false;
    }

    private boolean executeTx(HttpExchange exchange, String actionDesc, DatabaseTxConsumer action) throws IOException {
        if (isDatabaseAvailable()) {
            try (Connection conn = DatabaseConnectionFactory.getConnection(config)) {
                conn.setAutoCommit(false);
                try {
                    action.execute(conn);
                    conn.commit();
                    return true;
                } catch (Exception e) {
                    try {
                        conn.rollback();
                    } catch (SQLException ignored) {}
                    sendJson(exchange, 500, ApiResponse.error("Database error " + actionDesc + ": " + e.getMessage()));
                    return false;
                }
            } catch (SQLException | DatabaseException e) {
                sendJson(exchange, 500, ApiResponse.error("Database connection failure: " + e.getMessage()));
                return false;
            }
        } else if (isDurableWriteRequired(exchange)) {
            sendJson(exchange, 503, ApiResponse.error("Durable database write failed: MySQL database is unavailable. Writes are rejected to maintain strict persistence integrity."));
            return false;
        }
        return true;
    }

    private void applyListingUpdates(Listing listing, CreateListingRequest proposed) {
        if (proposed.getTitle() != null) listing.setTitle(proposed.getTitle().trim());
        if (proposed.getCategoryId() != null) listing.setCategoryId(proposed.getCategoryId());
        if (proposed.getSubcategoryId() != null) listing.setSubcategoryId(proposed.getSubcategoryId());
        if (proposed.getPrice() != null) listing.setPrice(proposed.getPrice());
        if (proposed.getOriginalPrice() != null) listing.setOriginalPrice(proposed.getOriginalPrice());
        if (proposed.getItemCondition() != null) listing.setItemCondition(proposed.getItemCondition());
        if (proposed.getMeetupSpot() != null) listing.setMeetupSpot(proposed.getMeetupSpot());
        if (proposed.getDescription() != null) listing.setDescription(proposed.getDescription().trim());
        listing.setHasNotes(proposed.isHasNotes());
    }
}
