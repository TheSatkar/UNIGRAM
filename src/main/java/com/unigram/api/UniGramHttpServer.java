package com.unigram.api;

import com.sun.net.httpserver.HttpServer;
import com.unigram.database.DatabaseConfig;
import com.unigram.service.JdbcChatService;
import com.unigram.service.JdbcMessageService;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Standalone HTTP server for the Uni-Gram messaging backend.
 * Built on native JDK com.sun.net.httpserver.HttpServer and backed by an ExecutorService
 * thread pool for handling concurrent client requests.
 */
public class UniGramHttpServer {

    public static final String ENV_API_PORT = "UNIGRAM_API_PORT";
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 8080;
    public static final int THREAD_POOL_SIZE = 10;

    private final String host;
    private final int requestedPort;
    private final DatabaseConfig dbConfig;
    private final CorsConfig corsConfig;
    private final com.unigram.collection.InMemoryStorage sharedStorage;

    private HttpServer server;
    private ExecutorService executor;
    private ApiRouter router;
    private int actualPort;
    private volatile boolean running = false;

    public UniGramHttpServer() {
        this(DEFAULT_HOST, resolvePort(), new DatabaseConfig(), new CorsConfig(), null);
    }

    public UniGramHttpServer(DatabaseConfig dbConfig) {
        this(DEFAULT_HOST, resolvePort(), dbConfig, new CorsConfig(), null);
    }

    public UniGramHttpServer(int port, DatabaseConfig dbConfig) {
        this(DEFAULT_HOST, port, dbConfig, new CorsConfig(), null);
    }

    public UniGramHttpServer(int port, DatabaseConfig dbConfig, com.unigram.collection.InMemoryStorage storage) {
        this(DEFAULT_HOST, port, dbConfig, new CorsConfig(), storage);
    }

    public UniGramHttpServer(String host, int port, DatabaseConfig dbConfig, CorsConfig corsConfig) {
        this(host, port, dbConfig, corsConfig, null);
    }

    public UniGramHttpServer(String host, int port, DatabaseConfig dbConfig, CorsConfig corsConfig, com.unigram.collection.InMemoryStorage storage) {
        this.host = (host != null && !host.trim().isEmpty()) ? host.trim() : DEFAULT_HOST;
        this.requestedPort = port >= 0 ? port : DEFAULT_PORT;
        this.dbConfig = dbConfig != null ? dbConfig : new DatabaseConfig();
        this.corsConfig = corsConfig != null ? corsConfig : new CorsConfig();
        this.actualPort = this.requestedPort;
        this.sharedStorage = storage;
    }

    private static int resolvePort() {
        String envPort = System.getenv(ENV_API_PORT);
        if (envPort != null && !envPort.trim().isEmpty()) {
            try {
                int p = Integer.parseInt(envPort.trim());
                if (p >= 0 && p <= 65535) {
                    return p;
                }
            } catch (NumberFormatException ignored) {
                // Fall back to default
            }
        }
        return DEFAULT_PORT;
    }

    /**
     * Starts the HTTP server and binds the thread pool executor.
     *
     * @throws IOException if server fails to bind (e.g. port already in use)
     */
    public synchronized void start() throws IOException {
        if (running) {
            return;
        }

        try {
            InetSocketAddress address = new InetSocketAddress(host, requestedPort);
            this.server = HttpServer.create(address, 0);

            // Configure named daemon thread pool for request handling
            AtomicInteger threadCounter = new AtomicInteger(0);
            ThreadFactory factory = r -> {
                Thread t = new Thread(r, "unigram-http-worker-" + threadCounter.incrementAndGet());
                t.setDaemon(true);
                return t;
            };
            this.executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE, factory);
            this.server.setExecutor(this.executor);

            // Wire database services
            JdbcChatService chatService = new JdbcChatService(dbConfig);
            JdbcMessageService messageService = new JdbcMessageService(dbConfig, chatService);

            // Register central API router under /api
            if (sharedStorage != null) {
                this.router = new ApiRouter(dbConfig, chatService, messageService, corsConfig, sharedStorage);
            } else {
                this.router = new ApiRouter(dbConfig, chatService, messageService, corsConfig);
            }
            this.server.createContext("/api", this.router);

            // Register fallback root context for uniform JSON 404 errors on non-API routes
            this.server.createContext("/", exchange -> {
                corsConfig.applyCorsHeaders(exchange);
                String p = exchange.getRequestURI().getPath();
                String json = JsonUtil.toJson(ApiResponse.error("Endpoint not found: " + p));
                byte[] b = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(404, b.length);
                try (java.io.OutputStream os = exchange.getResponseBody()) {
                    os.write(b);
                } finally {
                    exchange.close();
                }
            });

            this.server.start();
            this.actualPort = this.server.getAddress().getPort();
            this.running = true;

            System.out.println("[UniGramHttpServer] Started successfully on " + getBaseUrl() + " (pool size: " + THREAD_POOL_SIZE + ")");

        } catch (BindException e) {
            throw new IOException("Failed to start HTTP server: Port " + requestedPort + " is already in use by another application. " +
                    "Please stop the conflicting process or set a different port via UNIGRAM_API_PORT.", e);
        } catch (IOException e) {
            throw new IOException("Failed to start HTTP server on " + host + ":" + requestedPort + ": " + e.getMessage(), e);
        }
    }

    /**
     * Gracefully stops the HTTP server and shuts down its thread pool executor.
     *
     * @param delaySeconds maximum time in seconds to wait for in-flight requests to complete
     */
    public synchronized void stop(int delaySeconds) {
        if (!running) {
            return;
        }

        int delay = Math.max(0, delaySeconds);
        System.out.println("[UniGramHttpServer] Stopping HTTP server on port " + actualPort + " (delay: " + delay + "s)...");

        if (server != null) {
            try {
                server.stop(delay);
            } catch (Exception e) {
                System.err.println("[UniGramHttpServer] Error stopping server: " + e.getMessage());
            }
        }

        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(delay + 2L, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        this.running = false;
        System.out.println("[UniGramHttpServer] Stopped successfully.");
    }

    public boolean isRunning() {
        return running;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return actualPort;
    }

    public String getBaseUrl() {
        return "http://" + host + ":" + actualPort;
    }

    public DatabaseConfig getDbConfig() {
        return dbConfig;
    }

    public CorsConfig getCorsConfig() {
        return corsConfig;
    }

    public ApiRouter getRouter() {
        return router;
    }

    public static void main(String[] args) {
        int port = resolvePort();
        DatabaseConfig config = new DatabaseConfig();
        UniGramHttpServer server = new UniGramHttpServer(port, config);
        try {
            server.start();
            System.out.println("[UniGramHttpServer] Running on " + server.getBaseUrl() + ". Press Ctrl+C to stop.");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(1)));
            while (server.isRunning()) {
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            System.err.println("[UniGramHttpServer] Failed to start: " + e.getMessage());
            System.exit(1);
        }
    }
}
