package com.unigram.util;

import com.unigram.model.Message;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Standard Java CSV parser and exporter for Uni-Gram Message entities.
 *
 * Implements RFC 4180 compliant CSV serialization:
 * - UTF-8 character encoding preservation (supporting Unicode, accented text, and emojis)
 * - Escapes fields containing commas, double quotes, and newlines
 * - Converts inner double quotes to double-quotes ("" -> ")
 * - Parses quoted fields and handles malformed row validation
 * - Operates strictly via try-with-resources
 */
public class CsvMessageHelper {

    public static final String CSV_HEADER = "message_id,conversation_id,sender_id,content,sent_at";
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Exports a list of Message objects to a UTF-8 encoded CSV file.
     *
     * @param messages list of Message objects to export
     * @param targetPath file path on disk
     * @throws IOException if an I/O error occurs
     */
    public static void exportMessagesToCsv(List<Message> messages, Path targetPath) throws IOException {
        if (targetPath.getParent() != null) {
            Files.createDirectories(targetPath.getParent());
        }

        try (BufferedWriter writer = Files.newBufferedWriter(targetPath, StandardCharsets.UTF_8)) {
            writer.write(CSV_HEADER);
            writer.newLine();

            if (messages != null) {
                for (Message m : messages) {
                    writer.write(formatMessageToCsvRow(m));
                    writer.newLine();
                }
            }
        }
    }

    /**
     * Reads and parses a UTF-8 encoded CSV file into a list of Message objects.
     *
     * @param csvPath file path on disk
     * @return List of parsed Message entities
     * @throws IOException if an I/O error occurs
     */
    public static List<Message> importMessagesFromCsv(Path csvPath) throws IOException {
        if (!Files.exists(csvPath)) {
            throw new IOException("CSV file does not exist at: " + csvPath);
        }

        List<Message> result = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (header == null || !header.trim().equalsIgnoreCase(CSV_HEADER)) {
                throw new IllegalArgumentException("Invalid or missing CSV header. Expected: " + CSV_HEADER);
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) continue; // Skip blank lines

                List<String> columns = parseCsvLine(line);
                if (columns.size() != 5) {
                    throw new IllegalArgumentException(String.format(
                            "Malformed CSV at line %d: Expected 5 columns but found %d. Row: %s",
                            lineNumber, columns.size(), line));
                }

                String messageId = columns.get(0).trim();
                String conversationId = columns.get(1).trim();
                String senderId = columns.get(2).trim();
                String content = columns.get(3);
                String timestampStr = columns.get(4).trim();

                try {
                    LocalDateTime sentAt = LocalDateTime.parse(timestampStr, ISO_FORMATTER);
                    result.add(new Message(messageId, conversationId, senderId, content, sentAt));
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException(String.format(
                            "Malformed timestamp at line %d ('%s'): %s", lineNumber, timestampStr, e.getMessage()), e);
                }
            }
        }
        return result;
    }

    /**
     * Formats a single Message entity into an RFC 4180 CSV row string.
     */
    public static String formatMessageToCsvRow(Message message) {
        if (message == null) return "";
        return escapeField(message.getMessageId()) + "," +
               escapeField(message.getConversationId()) + "," +
               escapeField(message.getSenderId()) + "," +
               escapeField(message.getContent()) + "," +
               escapeField(message.getSentAt() != null ? message.getSentAt().format(ISO_FORMATTER) : "");
    }

    /**
     * Escapes a single string field according to RFC 4180.
     */
    public static String escapeField(String field) {
        if (field == null) return "";
        boolean containsComma = field.contains(",");
        boolean containsQuote = field.contains("\"");
        boolean containsNewline = field.contains("\n") || field.contains("\r");

        if (containsComma || containsQuote || containsNewline) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    /**
     * State-machine CSV line parser handling embedded commas and escaped double quotes ("").
     */
    public static List<String> parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        if (line == null) return tokens;

        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '\"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                    // Escaped quote ("")
                    current.append('\"');
                    i++; // skip next quote
                } else {
                    // Toggle quote mode
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        tokens.add(current.toString());
        return tokens;
    }
}
