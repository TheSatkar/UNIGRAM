package com.unigram.reporting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Demonstrates standard Java File I/O and Java NIO.2 operations:
 * 1. Path and Paths abstraction
 * 2. Files utility class (createDirectories, exists, size, deleteIfExists)
 * 3. BufferedReader and BufferedWriter with explicit UTF-8 character encoding
 * 4. Files.readString and Files.writeString convenience methods
 * 5. Deterministic resource cleanup with try-with-resources
 * 6. Isolated demonstration sandbox directory: data/phase8/demo/
 */
public class FileHandlingDemo {

    public static final Path DEMO_DIR = Paths.get("data", "phase8", "demo");

    public static void runDemonstration() {
        System.out.println("======================================================================");
        System.out.println("     8. Java NIO.2 File I/O & Character Encoding Demonstration        ");
        System.out.println("======================================================================\n");

        Path sampleFile = DEMO_DIR.resolve("system_audit_log.txt");

        try {
            // 1. Directory creation
            System.out.println("1. Directory Creation (Files.createDirectories):");
            if (!Files.exists(DEMO_DIR)) {
                Files.createDirectories(DEMO_DIR);
                System.out.println("   Created demonstration directory: " + DEMO_DIR.toAbsolutePath());
            } else {
                System.out.println("   Demonstration directory already exists: " + DEMO_DIR.toAbsolutePath());
            }

            // 2. Writing text via BufferedWriter & try-with-resources
            System.out.println("\n2. Writing to File via BufferedWriter (UTF-8 encoding):");
            try (BufferedWriter writer = Files.newBufferedWriter(sampleFile, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                writer.write("=== Uni-Gram Server Audit Log ===");
                writer.newLine();
                writer.write("[INFO] System initialized at: " + java.time.LocalDateTime.now());
                writer.newLine();
                writer.write("[EVENT] Marketplace listing prod_calc_ti84 referenced by user usr_alex.");
                writer.newLine();
                writer.write("[UNICODE] Multilingual text verification: Café, Résumé, 日本語, 🚀.");
                writer.newLine();
            }
            System.out.println("   Successfully written audit log to: " + sampleFile);

            // 3. File existence and metadata inspection
            System.out.println("\n3. File Existence & Metadata Inspection:");
            System.out.printf("   Files.exists(path)    : %b%n", Files.exists(sampleFile));
            System.out.printf("   Files.isReadable(path): %b%n", Files.isReadable(sampleFile));
            System.out.printf("   Files.size(path)      : %d bytes%n", Files.size(sampleFile));

            // 4. Reading text via BufferedReader & try-with-resources
            System.out.println("\n4. Reading Text File Line-by-Line via BufferedReader:");
            try (BufferedReader reader = Files.newBufferedReader(sampleFile, StandardCharsets.UTF_8)) {
                String line;
                int lineCount = 1;
                while ((line = reader.readLine()) != null) {
                    System.out.printf("   Line %d: %s%n", lineCount++, line);
                }
            }

            // 5. High-level modern read/write methods: Files.readString and Files.writeString
            System.out.println("\n5. Modern Java readString() demonstration:");
            String fullContent = Files.readString(sampleFile, StandardCharsets.UTF_8);
            System.out.printf("   Total characters read via Files.readString(): %d chars%n", fullContent.length());

            // 6. Controlled file cleanup
            System.out.println("\n6. Controlled File Cleanup (Files.deleteIfExists):");
            boolean deleted = Files.deleteIfExists(sampleFile);
            System.out.printf("   Cleaned up demo file (%s): %b%n", sampleFile.getFileName(), deleted);
            System.out.printf("   File exists after cleanup? %b%n", Files.exists(sampleFile));

            System.out.println("\n>> Academic Viva Note: Java I/O vs Java NIO.2 <<");
            System.out.println("1. Legacy java.io.File had limited error handling (boolean returns without exceptions),");
            System.out.println("   no support for symbolic links, and poor scalability on large directory trees.");
            System.out.println("2. Java NIO.2 (java.nio.file) introduced Path, Paths, and Files with rich metadata,");
            System.out.println("   detailed exceptions, stream integration, and explicit character set encoding.");
            System.out.println("======================================================================\n");

        } catch (IOException e) {
            System.out.println("[!] File I/O Error: " + e.getMessage());
        }
    }
}
