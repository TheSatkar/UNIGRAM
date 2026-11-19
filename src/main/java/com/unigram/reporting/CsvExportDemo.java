package com.unigram.reporting;

import com.unigram.model.Message;
import com.unigram.util.CsvMessageHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Demonstrates RFC 4180 compliant CSV Export and Import for Uni-Gram messages:
 * 1. Serializing domain entities to CSV format
 * 2. Handling tricky CSV cases: commas inside content, quotes, newlines, and Unicode/emojis
 * 3. Reading and parsing CSV back into typed Java domain objects
 * 4. Graceful handling and validation of malformed input lines
 * 5. Deterministic file cleanup
 */
public class CsvExportDemo {

    public static final Path CSV_DIR = Paths.get("data", "phase8", "demo");
    public static final Path EXPORT_FILE = CSV_DIR.resolve("conversation_messages.csv");
    public static final Path MALFORMED_FILE = CSV_DIR.resolve("malformed_test.csv");

    public static void runDemonstration() {
        System.out.println("======================================================================");
        System.out.println("     9. CSV Export & Import with RFC 4180 Compliance Demonstration    ");
        System.out.println("======================================================================\n");

        LocalDateTime base = LocalDateTime.now().minusHours(2);

        // Tricky messages covering commas, quotes, special characters, and Unicode
        List<Message> sampleMessages = List.of(
                new Message("msg_csv_1", "conv_csv_01", "usr_alex", "Hi, is this textbook available?", base),
                new Message("msg_csv_2", "conv_csv_01", "usr_maya", "Yes! It's \"Algorithms in Java\", 4th edition.", base.plusMinutes(5)),
                new Message("msg_csv_3", "conv_csv_01", "usr_alex", "Can we meet at 3:00, or later? 📚", base.plusMinutes(10)),
                new Message("msg_csv_4", "conv_csv_01", "usr_maya", "Line 1 note:\nCall me before coming, please.", base.plusMinutes(15)),
                new Message("msg_csv_5", "conv_csv_01", "usr_alex", "Confirmed! Merci beaucoup, café soon! ☕", base.plusMinutes(20))
        );

        try {
            // 1. Export Messages to CSV
            System.out.println("1. Exporting 5 messages to CSV (" + EXPORT_FILE + "):");
            CsvMessageHelper.exportMessagesToCsv(sampleMessages, EXPORT_FILE);
            System.out.println("   [✓] Successfully exported to: " + EXPORT_FILE.toAbsolutePath());
            System.out.printf("   File size on disk: %d bytes%n", Files.size(EXPORT_FILE));

            // Display raw CSV contents from disk
            System.out.println("\n2. Inspecting Raw CSV File Content on Disk:");
            List<String> rawLines = Files.readAllLines(EXPORT_FILE);
            for (int i = 0; i < rawLines.size(); i++) {
                System.out.printf("   [%d] %s%n", i, rawLines.get(i));
            }

            // 3. Import Messages from CSV back into Java Objects
            System.out.println("\n3. Importing CSV Back into List<Message> Domain Objects:");
            List<Message> imported = CsvMessageHelper.importMessagesFromCsv(EXPORT_FILE);
            System.out.printf("   Successfully parsed %d messages from CSV:%n", imported.size());
            for (Message m : imported) {
                System.out.printf("   - [%s] %-10s at %s: \"%s\"%n",
                        m.getMessageId(), m.getSenderId(), m.getSentAt(), m.getContent());
            }

            // Verify content integrity
            boolean matches = imported.size() == sampleMessages.size() &&
                    imported.get(1).getContent().equals(sampleMessages.get(1).getContent()) &&
                    imported.get(4).getContent().contains("☕");
            System.out.println("   [✓] Content fidelity and Unicode preservation verified: " + matches);

            // 4. Malformed CSV Validation Demonstration
            System.out.println("\n4. Testing Malformed CSV Validation:");
            Files.writeString(MALFORMED_FILE,
                    CsvMessageHelper.CSV_HEADER + "\n" +
                    "bad_row_1,conv_1,usr_1\n" // Missing 2 columns!
            );

            try {
                CsvMessageHelper.importMessagesFromCsv(MALFORMED_FILE);
                System.err.println("   [FAIL] Expected malformed CSV error was not thrown.");
            } catch (IllegalArgumentException e) {
                System.out.println("   [✓] Caught expected validation error for malformed row:");
                System.out.println("       " + e.getMessage());
            }

            // 5. Cleanup
            Files.deleteIfExists(EXPORT_FILE);
            Files.deleteIfExists(MALFORMED_FILE);
            System.out.println("\n5. Controlled demonstration files cleaned up.");

            System.out.println("\n>> Academic Viva Note: CSV RFC 4180 Rules <<");
            System.out.println("1. Quoting: Any field containing commas, double quotes, or newlines MUST be enclosed in quotes.");
            System.out.println("2. Escape Quotes: Inner quotes are represented by two consecutive double quotes (\"\").");
            System.out.println("3. Encoding: Standard CSV requires explicit UTF-8 to preserve international text and emojis.");
            System.out.println("======================================================================\n");

        } catch (IOException e) {
            System.out.println("[!] CSV Processing Error: " + e.getMessage());
        }
    }
}
