package com.unigram;

import com.unigram.model.Conversation;
import com.unigram.model.Message;
import com.unigram.model.ProductReference;
import com.unigram.model.User;
import com.unigram.util.CsvMessageHelper;
import com.unigram.util.MessageFilter;
import com.unigram.util.MessageFormatter;
import com.unigram.util.ResultWrapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Phase 8 Automated Verification Suite:
 * Lambda Expressions, Streams, Optional, Generics, Date/Time & File Handling.
 *
 * Verifies all 20 required Phase 8 scenarios using pure standard Java.
 */
public class Phase8Verification {

    private static int totalAssertions = 0;
    private static int passedAssertions = 0;
    private static int failedAssertions = 0;

    private static final Path TEST_DIR = Paths.get("data", "phase8", "test");

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("          Uni-Gram — Phase 8 Verification Test Suite                  ");
        System.out.println("======================================================================\n");

        try {
            cleanupTestFiles();

            testScenario1_FunctionalInterfaceBehavior();
            testScenario2_LambdaExecution();
            testScenario3_MethodReferenceResults();
            testScenario4_StreamFiltering();
            testScenario5_StreamMapping();
            testScenario6_StreamSorting();
            testScenario7_StreamAggregation();
            testScenario8_OptionalPresentAndEmpty();
            testScenario9_GenericClassTypeSafety();
            testScenario10_BoundedGenericBehavior();
            testScenario11_WildcardVariancePecs();
            testScenario12_DateTimeFormattingAndCalculations();
            testScenario13_TextFileWritingAndReading();
            testScenario14_Utf8AndUnicodePreservation();
            testScenario15_CsvExport();
            testScenario16_CsvImport();
            testScenario17_QuotedFieldsAndCommasInCsv();
            testScenario18_MalformedCsvHandling();
            testScenario19_ResourceClosure();
            testScenario20_SafeCleanupOfTestFiles();

        } finally {
            cleanupTestFiles();
        }

        printSummary();
        if (failedAssertions > 0) {
            System.exit(1);
        }
    }

    // -------------------------------------------------------------------------
    // Scenario 1: Functional Interface Behaviour
    // -------------------------------------------------------------------------
    private static void testScenario1_FunctionalInterfaceBehavior() {
        System.out.println("[Scenario 1] Functional Interface Behavior (Built-in & Custom SAM)");
        Message m = new Message("m1", "c1", "usr_alex", "Is this available?", LocalDateTime.now());

        // Built-in Predicate
        Predicate<Message> pred = msg -> "usr_alex".equals(msg.getSenderId());
        assertTrue("Predicate evaluates true for matching sender", pred.test(m));

        // Built-in Function
        Function<Message, Integer> func = msg -> msg.getContent().length();
        assertTrue("Function correctly computes string length", func.apply(m) == 18);

        // Built-in Consumer
        List<String> sink = new ArrayList<>();
        Consumer<Message> cons = msg -> sink.add(msg.getMessageId());
        cons.accept(m);
        assertTrue("Consumer executed side-effect on collection", sink.contains("m1"));

        // Built-in Supplier
        Supplier<String> supp = () -> "Default";
        assertTrue("Supplier delivers lazily evaluated value", "Default".equals(supp.get()));

        // Custom SAM Interface: MessageFilter (with composition)
        MessageFilter byAlex = msg -> "usr_alex".equals(msg.getSenderId());
        MessageFilter hasQuestion = msg -> msg.getContent().contains("?");
        MessageFilter composed = byAlex.and(hasQuestion);
        assertTrue("Custom MessageFilter and() composition succeeds", composed.accept(m));
        assertTrue("Custom MessageFilter negate() succeeds", byAlex.negate().accept(new Message("m2", "c1", "usr_bob", "Hello", LocalDateTime.now())));

        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 2: Lambda Execution
    // -------------------------------------------------------------------------
    private static void testScenario2_LambdaExecution() {
        System.out.println("[Scenario 2] Lambda Execution Variants (Zero, One, Multi-parameter & Block)");

        // Zero parameter
        Supplier<Integer> zeroParam = () -> 42;
        assertTrue("Zero-param lambda executes", zeroParam.get() == 42);

        // Single parameter (expression lambda)
        Function<Integer, Integer> square = x -> x * x;
        assertTrue("Single-param expression lambda computes square", square.apply(9) == 81);

        // Multi-parameter (block lambda)
        BiFunction<Integer, Integer, Integer> blockSum = (a, b) -> {
            int result = a + b;
            return result;
        };
        assertTrue("Multi-param block lambda computes sum", blockSum.apply(15, 27) == 42);

        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 3: Method Reference Results
    // -------------------------------------------------------------------------
    private static void testScenario3_MethodReferenceResults() {
        System.out.println("[Scenario 3] Method Reference Results (Static, Instance & Constructor)");
        Message msg = new Message("m_ref", "c1", "usr_test", "Reference test content", LocalDateTime.now());

        // 1. Static method reference
        Function<Message, String> staticRef = MessageFormatter::formatCompact;
        assertTrue("Static method ref formats string", staticRef.apply(msg).contains("usr_test -> \"Reference test content\""));

        // 2. Instance method of arbitrary object
        Function<Message, String> instanceRef = Message::getContent;
        assertTrue("Arbitrary instance method ref extracts content", "Reference test content".equals(instanceRef.apply(msg)));

        // 3. Instance method of existing object
        StringBuilder sb = new StringBuilder("ID: ");
        Consumer<String> appendRef = sb::append;
        appendRef.accept("msg_99");
        assertTrue("Particular object method ref appends text", "ID: msg_99".equals(sb.toString()));

        // 4. Constructor reference
        BiFunction<String, String, ProductReference> ctorRef = ProductReference::new;
        ProductReference pr = ctorRef.apply("p_ref", "Book Title");
        assertTrue("Constructor ref instantiates ProductReference", pr != null && "Book Title".equals(pr.getProductTitle()));

        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 4: Stream Filtering
    // -------------------------------------------------------------------------
    private static void testScenario4_StreamFiltering() {
        System.out.println("[Scenario 4] Stream Filtering (filter)");
        List<Message> list = List.of(
                new Message("m1", "c1", "usr_1", "Alpha", LocalDateTime.now()),
                new Message("m2", "c1", "usr_2", "Beta", LocalDateTime.now()),
                new Message("m3", "c1", "usr_1", "Gamma", LocalDateTime.now())
        );

        List<Message> filtered = list.stream()
                .filter(m -> "usr_1".equals(m.getSenderId()))
                .collect(Collectors.toList());

        assertTrue("Filtered list has exactly 2 messages", filtered.size() == 2);
        assertTrue("All filtered messages are from usr_1", filtered.stream().allMatch(m -> "usr_1".equals(m.getSenderId())));
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 5: Stream Mapping
    // -------------------------------------------------------------------------
    private static void testScenario5_StreamMapping() {
        System.out.println("[Scenario 5] Stream Mapping (map & distinct)");
        List<Message> list = List.of(
                new Message("m1", "c1", "usr_1", "Hello", LocalDateTime.now()),
                new Message("m2", "c1", "usr_2", "World", LocalDateTime.now()),
                new Message("m3", "c1", "usr_1", "Test", LocalDateTime.now())
        );

        List<String> senders = list.stream()
                .map(Message::getSenderId)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        assertTrue("Distinct senders count is 2", senders.size() == 2);
        assertTrue("Senders contains usr_1 and usr_2", senders.contains("usr_1") && senders.contains("usr_2"));
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 6: Stream Sorting
    // -------------------------------------------------------------------------
    private static void testScenario6_StreamSorting() {
        System.out.println("[Scenario 6] Stream Sorting (sorted)");
        LocalDateTime now = LocalDateTime.now();
        List<Message> list = List.of(
                new Message("m3", "c1", "usr_1", "Three", now.plusMinutes(20)),
                new Message("m1", "c1", "usr_1", "One", now),
                new Message("m2", "c1", "usr_1", "Two", now.plusMinutes(10))
        );

        List<Message> sorted = list.stream()
                .sorted(Comparator.comparing(Message::getSentAt))
                .collect(Collectors.toList());

        assertTrue("First sorted message is m1", "m1".equals(sorted.get(0).getMessageId()));
        assertTrue("Second sorted message is m2", "m2".equals(sorted.get(1).getMessageId()));
        assertTrue("Third sorted message is m3", "m3".equals(sorted.get(2).getMessageId()));
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 7: Stream Aggregation
    // -------------------------------------------------------------------------
    private static void testScenario7_StreamAggregation() {
        System.out.println("[Scenario 7] Stream Aggregation (count, reduce, groupingBy, joining)");
        List<Message> list = List.of(
                new Message("m1", "conv_a", "usr_1", "Short", LocalDateTime.now()),
                new Message("m2", "conv_a", "usr_2", "Medium text", LocalDateTime.now()),
                new Message("m3", "conv_b", "usr_1", "Long text message content", LocalDateTime.now())
        );

        // count
        long total = list.stream().count();
        assertTrue("count() returns 3", total == 3);

        // reduce: total content length
        int totalLength = list.stream()
                .map(m -> m.getContent().length())
                .reduce(0, Integer::sum);
        assertTrue("reduce() sums content length correctly", totalLength == (5 + 11 + 25));

        // groupingBy conversation ID
        Map<String, List<Message>> grouped = list.stream()
                .collect(Collectors.groupingBy(Message::getConversationId));
        assertTrue("grouped conv_a has 2 messages", grouped.get("conv_a").size() == 2);
        assertTrue("grouped conv_b has 1 message", grouped.get("conv_b").size() == 1);

        // joining
        String joined = list.stream()
                .map(Message::getMessageId)
                .collect(Collectors.joining(","));
        assertTrue("joining() joins IDs with commas", "m1,m2,m3".equals(joined));

        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 8: Optional Present and Empty Handling
    // -------------------------------------------------------------------------
    private static void testScenario8_OptionalPresentAndEmpty() {
        System.out.println("[Scenario 8] Optional API Handling (empty, ofNullable, orElse, map, flatMap)");

        User user = new User("usr_opt", "Ada Lovelace", "ada@test.edu");

        Optional<User> presentOpt = Optional.ofNullable(user);
        Optional<User> emptyOpt = Optional.ofNullable(null);

        assertTrue("presentOpt.isPresent() is true", presentOpt.isPresent());
        assertTrue("emptyOpt.isEmpty() is true", emptyOpt.isEmpty());

        // orElse
        User fallback = new User("usr_fb", "Fallback", "fb@test.edu");
        User retrieved = emptyOpt.orElse(fallback);
        assertTrue("orElse returns fallback when empty", "usr_fb".equals(retrieved.getUserId()));

        // orElseThrow
        boolean caught = false;
        try {
            emptyOpt.orElseThrow(() -> new IllegalStateException("Missing user"));
        } catch (IllegalStateException e) {
            caught = true;
        }
        assertTrue("orElseThrow throws expected exception", caught);

        // map
        Optional<String> nameOpt = presentOpt.map(User::getFullName);
        assertTrue("map() extracts full name", nameOpt.isPresent() && "Ada Lovelace".equals(nameOpt.get()));

        // flatMap
        Optional<String> flatMapped = presentOpt.flatMap(u -> Optional.of(u.getEmail()));
        assertTrue("flatMap() delivers flattened optional", flatMapped.isPresent() && "ada@test.edu".equals(flatMapped.get()));

        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 9: Generic Class Type Safety
    // -------------------------------------------------------------------------
    private static void testScenario9_GenericClassTypeSafety() {
        System.out.println("[Scenario 9] Generic Class Type Safety (ResultWrapper<T>)");

        User u = new User("usr_g1", "Alan Turing", "alan@turing.org");
        ResultWrapper<User> userWrapper = ResultWrapper.success(u);
        ResultWrapper<Integer> intWrapper = ResultWrapper.success(42);

        assertTrue("User wrapper holds User instance", userWrapper.isSuccess() && userWrapper.getData() instanceof User);
        assertTrue("Integer wrapper holds Integer instance", intWrapper.isSuccess() && intWrapper.getData().equals(42));

        ResultWrapper<String> failedWrapper = ResultWrapper.failure("Operation failed");
        assertTrue("Failed wrapper reports isSuccess == false", !failedWrapper.isSuccess());
        assertTrue("Failed wrapper holds error message", "Operation failed".equals(failedWrapper.getErrorMessage()));

        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 10: Bounded Generic Behavior
    // -------------------------------------------------------------------------
    private static void testScenario10_BoundedGenericBehavior() {
        System.out.println("[Scenario 10] Bounded Generic Behavior (<U extends Comparable<U>>)");

        String maxString = ResultWrapper.findMax("Apple", "Zebra");
        assertTrue("findMax correctly finds Zebra as greater string", "Zebra".equals(maxString));

        Integer maxInt = ResultWrapper.findMax(100, 500);
        assertTrue("findMax correctly finds 500 as greater integer", maxInt == 500);

        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 11: Wildcard Variance & PECS
    // -------------------------------------------------------------------------
    private static void testScenario11_WildcardVariancePecs() {
        System.out.println("[Scenario 11] Wildcard Variance & PECS (Producer Extends, Consumer Super)");

        // Upper bounded: <? extends Number>
        List<Integer> intList = List.of(1, 2, 3);
        List<Number> copiedNumbers = ResultWrapper.copyFromProducer(intList);
        assertTrue("copyFromProducer copies items into List<Number>", copiedNumbers.size() == 3);

        // Lower bounded: <? super Integer>
        List<Number> numberConsumer = new ArrayList<>();
        ResultWrapper.drainToConsumer(List.of(10, 20), numberConsumer);
        assertTrue("drainToConsumer drains into List<Number>", numberConsumer.size() == 2 && numberConsumer.contains(10));

        // Unbounded wildcard: List<?>
        int count = ResultWrapper.countElements(numberConsumer);
        assertTrue("countElements accurately counts unknown wildcard list", count == 2);

        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 12: Date and Time Formatting and Calculations
    // -------------------------------------------------------------------------
    private static void testScenario12_DateTimeFormattingAndCalculations() {
        System.out.println("[Scenario 12] Date & Time API Calculations and Formatting (java.time)");

        LocalDateTime start = LocalDateTime.of(2026, 11, 18, 10, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 18, 11, 30, 45);

        // Duration calculation
        Duration duration = Duration.between(start, end);
        assertTrue("Duration is 90 minutes", duration.toMinutes() == 90);
        assertTrue("Duration is 5445 seconds", duration.toSeconds() == 5445);

        // Period calculation
        LocalDate date1 = LocalDate.of(2026, 1, 1);
        LocalDate date2 = LocalDate.of(2026, 4, 15);
        Period period = Period.between(date1, date2);
        assertTrue("Period is 3 months and 14 days", period.getMonths() == 3 && period.getDays() == 14);

        // Formatting & Parsing
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formatted = start.format(formatter);
        assertTrue("Formatted date matches pattern", "2026-11-18 10:00:00".equals(formatted));

        LocalDateTime parsed = LocalDateTime.parse(formatted, formatter);
        assertTrue("Parsed date equals original", start.equals(parsed));

        // Timezone conversion
        ZonedDateTime utcZone = start.atZone(ZoneId.of("UTC"));
        assertTrue("UTC Zone matches", "UTC".equals(utcZone.getZone().getId()));

        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 13: Text File Writing and Reading
    // -------------------------------------------------------------------------
    private static void testScenario13_TextFileWritingAndReading() {
        System.out.println("[Scenario 13] Text File Writing and Reading (Java NIO.2)");
        Path file = TEST_DIR.resolve("test_io.txt");
        try {
            Files.createDirectories(TEST_DIR);
            String content = "Line 1: Uni-Gram Messaging System\nLine 2: Persistent Verification\nLine 3: Java 21 LTS";

            // Writing via Files.writeString (UTF-8)
            Files.writeString(file, content, StandardCharsets.UTF_8);
            assertTrue("File exists on disk after writing", Files.exists(file));

            // Reading via Files.readString (UTF-8)
            String readBack = Files.readString(file, StandardCharsets.UTF_8);
            assertTrue("Read content exactly equals written content", content.equals(readBack));

        } catch (IOException e) {
            fail("File writing/reading failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 14: UTF-8 and Unicode Preservation
    // -------------------------------------------------------------------------
    private static void testScenario14_Utf8AndUnicodePreservation() {
        System.out.println("[Scenario 14] UTF-8 and Unicode Preservation (Accents, International text & Emojis)");
        Path unicodeFile = TEST_DIR.resolve("unicode_test.txt");
        try {
            Files.createDirectories(TEST_DIR);
            String unicodeText = "Café, Résumé, Universität, 日本語, 🚀, 📚, ☕";

            Files.writeString(unicodeFile, unicodeText, StandardCharsets.UTF_8);
            String readBack = Files.readString(unicodeFile, StandardCharsets.UTF_8);

            assertTrue("Unicode text read back without corruption", unicodeText.equals(readBack));
            assertTrue("Emojis preserved accurately", readBack.contains("🚀") && readBack.contains("☕"));

        } catch (IOException e) {
            fail("Unicode preservation test failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 15: CSV Export
    // -------------------------------------------------------------------------
    private static void testScenario15_CsvExport() {
        System.out.println("[Scenario 15] CSV Export Operation (CsvMessageHelper.exportMessagesToCsv)");
        Path csvFile = TEST_DIR.resolve("messages_export.csv");
        try {
            Files.createDirectories(TEST_DIR);
            List<Message> messages = List.of(
                    new Message("m_csv_1", "conv_1", "usr_alex", "Hi Maya", LocalDateTime.now()),
                    new Message("m_csv_2", "conv_1", "usr_maya", "Hello Alex", LocalDateTime.now().plusMinutes(1))
            );

            CsvMessageHelper.exportMessagesToCsv(messages, csvFile);
            assertTrue("CSV export file created", Files.exists(csvFile));
            assertTrue("CSV file size > 0", Files.size(csvFile) > 0);

            List<String> lines = Files.readAllLines(csvFile, StandardCharsets.UTF_8);
            assertTrue("CSV file has header plus 2 rows (3 lines)", lines.size() == 3);
            assertTrue("Header matches expected standard", CsvMessageHelper.CSV_HEADER.equalsIgnoreCase(lines.get(0)));

        } catch (IOException e) {
            fail("CSV export failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 16: CSV Import
    // -------------------------------------------------------------------------
    private static void testScenario16_CsvImport() {
        System.out.println("[Scenario 16] CSV Import Operation (CsvMessageHelper.importMessagesFromCsv)");
        Path csvFile = TEST_DIR.resolve("messages_export.csv");
        try {
            List<Message> imported = CsvMessageHelper.importMessagesFromCsv(csvFile);
            assertTrue("Imported messages list size is 2", imported.size() == 2);
            assertTrue("First imported message ID is m_csv_1", "m_csv_1".equals(imported.get(0).getMessageId()));
            assertTrue("First message content is 'Hi Maya'", "Hi Maya".equals(imported.get(0).getContent()));
            assertTrue("Second imported message ID is m_csv_2", "m_csv_2".equals(imported.get(1).getMessageId()));

        } catch (IOException e) {
            fail("CSV import failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 17: Quoted Fields and Commas in CSV
    // -------------------------------------------------------------------------
    private static void testScenario17_QuotedFieldsAndCommasInCsv() {
        System.out.println("[Scenario 17] Quoted Fields, Commas, and Embedded Quotes in CSV (RFC 4180)");
        Path complexCsv = TEST_DIR.resolve("complex_messages.csv");
        try {
            String trickyContent1 = "Hello, world! Meeting at 3, or 4?";
            String trickyContent2 = "She said, \"Algorithms are fun!\"";
            String trickyContent3 = "Textbook condition: Like new, no highlights. Price: $45.";

            List<Message> complexMessages = List.of(
                    new Message("m_tricky_1", "c1", "usr_1", trickyContent1, LocalDateTime.now()),
                    new Message("m_tricky_2", "c1", "usr_2", trickyContent2, LocalDateTime.now().plusMinutes(1)),
                    new Message("m_tricky_3", "c1", "usr_1", trickyContent3, LocalDateTime.now().plusMinutes(2))
            );

            CsvMessageHelper.exportMessagesToCsv(complexMessages, complexCsv);
            List<Message> reimported = CsvMessageHelper.importMessagesFromCsv(complexCsv);

            assertTrue("All 3 complex messages imported", reimported.size() == 3);
            assertTrue("Embedded comma content preserved exactly", trickyContent1.equals(reimported.get(0).getContent()));
            assertTrue("Embedded double quotes preserved exactly", trickyContent2.equals(reimported.get(1).getContent()));
            assertTrue("Multiple commas preserved exactly", trickyContent3.equals(reimported.get(2).getContent()));

        } catch (IOException e) {
            fail("Complex CSV test failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 18: Malformed CSV Handling
    // -------------------------------------------------------------------------
    private static void testScenario18_MalformedCsvHandling() {
        System.out.println("[Scenario 18] Malformed CSV Input Validation");
        Path malformedMissingCols = TEST_DIR.resolve("malformed_missing_cols.csv");
        Path malformedBadTimestamp = TEST_DIR.resolve("malformed_bad_ts.csv");

        try {
            // Case A: Missing columns
            Files.writeString(malformedMissingCols, CsvMessageHelper.CSV_HEADER + "\nm_bad_1,c1,usr_1\n", StandardCharsets.UTF_8);
            boolean caughtMissing = false;
            try {
                CsvMessageHelper.importMessagesFromCsv(malformedMissingCols);
            } catch (IllegalArgumentException e) {
                caughtMissing = true;
                assertTrue("Exception message identifies expected 5 columns", e.getMessage().contains("Expected 5 columns"));
            }
            assertTrue("Malformed row with missing columns threw IllegalArgumentException", caughtMissing);

            // Case B: Corrupted timestamp format
            Files.writeString(malformedBadTimestamp, CsvMessageHelper.CSV_HEADER + "\nm_bad_2,c1,usr_1,Hello,NOT_A_TIMESTAMP\n", StandardCharsets.UTF_8);
            boolean caughtTs = false;
            try {
                CsvMessageHelper.importMessagesFromCsv(malformedBadTimestamp);
            } catch (IllegalArgumentException e) {
                caughtTs = true;
                assertTrue("Exception message identifies malformed timestamp", e.getMessage().contains("Malformed timestamp"));
            }
            assertTrue("Malformed timestamp threw IllegalArgumentException", caughtTs);

        } catch (IOException e) {
            fail("Malformed CSV test failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 19: Resource Closure Verification
    // -------------------------------------------------------------------------
    private static void testScenario19_ResourceClosure() {
        System.out.println("[Scenario 19] Deterministic Resource Closure via try-with-resources");
        Path file = TEST_DIR.resolve("resource_closure.txt");
        try {
            BufferedWriter capturedWriter;
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                capturedWriter = writer;
                writer.write("Testing resource closure.");
            }

            // Attempting to write to capturedWriter outside the try block MUST throw IOException
            boolean caughtClosed = false;
            try {
                capturedWriter.write("This should fail because stream is closed.");
            } catch (IOException e) {
                caughtClosed = true;
            }
            assertTrue("BufferedWriter was closed deterministically after try-with-resources block", caughtClosed);

            BufferedReader capturedReader;
            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                capturedReader = reader;
                reader.readLine();
            }

            boolean caughtReaderClosed = false;
            try {
                capturedReader.readLine();
            } catch (IOException e) {
                caughtReaderClosed = true;
            }
            assertTrue("BufferedReader was closed deterministically after try-with-resources block", caughtReaderClosed);

        } catch (IOException e) {
            fail("Resource closure test failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Scenario 20: Safe Cleanup of Test Files
    // -------------------------------------------------------------------------
    private static void testScenario20_SafeCleanupOfTestFiles() {
        System.out.println("[Scenario 20] Safe Cleanup of Demonstration Files");
        try {
            Path dummyFile = TEST_DIR.resolve("dummy_for_cleanup.txt");
            Files.writeString(dummyFile, "cleanup me", StandardCharsets.UTF_8);
            assertTrue("Dummy test file exists prior to cleanup", Files.exists(dummyFile));

            boolean deleted = Files.deleteIfExists(dummyFile);
            assertTrue("deleteIfExists returned true", deleted);
            assertTrue("File no longer exists after cleanup", !Files.exists(dummyFile));

        } catch (IOException e) {
            fail("Cleanup test failed: " + e.getMessage());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Helpers & Cleanup
    // -------------------------------------------------------------------------
    private static void cleanupTestFiles() {
        try {
            if (Files.exists(TEST_DIR)) {
                try (var stream = Files.list(TEST_DIR)) {
                    for (Path p : stream.toList()) {
                        Files.deleteIfExists(p);
                    }
                }
                Files.deleteIfExists(TEST_DIR);
            }
        } catch (Exception ignored) {
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

    private static void printSummary() {
        System.out.println("======================================================================");
        System.out.println("                   Phase 8 Verification Summary                       ");
        System.out.println("======================================================================");
        System.out.println("Total Assertions Evaluated : " + totalAssertions);
        System.out.println("Assertions Passed          : " + passedAssertions);
        System.out.println("Assertions Failed          : " + failedAssertions);
        System.out.println("Status                     : " + (failedAssertions == 0 ? "ALL 20 SCENARIOS PASSED (100%)" : "FAILURES DETECTED"));
        System.out.println("======================================================================\n");
    }
}
