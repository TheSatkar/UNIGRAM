package com.unigram.reporting;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Demonstrates the modern Java Date and Time API (java.time):
 * 1. LocalDate, LocalTime, LocalDateTime
 * 2. Instant (machine UTC timestamp) and ZonedDateTime (timezone conversion)
 * 3. Duration (time-based difference) vs Period (date-based difference)
 * 4. DateTimeFormatter parsing and custom formatting
 * 5. Time calculations relevant to message expiration and chat intervals
 */
public class DateTimeDemo {

    public static void runDemonstration() {
        System.out.println("======================================================================");
        System.out.println("     7. Java Date and Time API (java.time) Demonstration              ");
        System.out.println("======================================================================\n");

        // 1. Core Classes: LocalDate, LocalTime, LocalDateTime
        System.out.println("1. Core Date and Time Classes:");
        LocalDate today = LocalDate.now();
        LocalTime currentTime = LocalTime.now();
        LocalDateTime currentDateTime = LocalDateTime.now();

        System.out.println("   LocalDate.now()         : " + today);
        System.out.println("   LocalTime.now()         : " + currentTime);
        System.out.println("   LocalDateTime.now()     : " + currentDateTime);

        // 2. Machine Timestamp (Instant) & Timezone (ZonedDateTime)
        System.out.println("\n2. Instant (UTC Epoch) & Timezone Representations:");
        Instant epochNow = Instant.now();
        System.out.println("   Instant.now() (UTC Epoch): " + epochNow);

        ZonedDateTime utcZoned = currentDateTime.atZone(ZoneId.of("UTC"));
        ZonedDateTime tokyoZoned = currentDateTime.atZone(ZoneId.of("Asia/Tokyo"));
        ZonedDateTime nyZoned = currentDateTime.atZone(ZoneId.of("America/New_York"));

        System.out.println("   UTC ZonedDateTime        : " + utcZoned);
        System.out.println("   Tokyo ZonedDateTime      : " + tokyoZoned);
        System.out.println("   New York ZonedDateTime   : " + nyZoned);

        // 3. Duration (Hours, Minutes, Seconds difference)
        System.out.println("\n3. Duration Calculations between Message Timestamps:");
        LocalDateTime msg1Sent = LocalDateTime.now().minusMinutes(72).minusSeconds(45);
        LocalDateTime msg2Sent = LocalDateTime.now();

        Duration duration = Duration.between(msg1Sent, msg2Sent);
        System.out.printf("   Message 1: %s%n", msg1Sent);
        System.out.printf("   Message 2: %s%n", msg2Sent);
        System.out.printf("   Time Elapsed: %d hours, %d minutes, %d seconds (Total seconds: %d)%n",
                duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart(), duration.toSeconds());

        // 4. Period (Days, Months, Years difference)
        System.out.println("\n4. Period Calculations between Calendar Dates:");
        LocalDate semesterStart = LocalDate.of(2026, 8, 15);
        LocalDate semesterEnd = LocalDate.of(2026, 12, 20);

        Period semesterDuration = Period.between(semesterStart, semesterEnd);
        System.out.printf("   Semester Duration: %d months and %d days (Total days approx: %d)%n",
                semesterDuration.getMonths(), semesterDuration.getDays(), semesterStart.until(semesterEnd).getDays());

        // 5. DateTimeFormatter: Formatting & Parsing
        System.out.println("\n5. Formatting & Parsing with DateTimeFormatter:");
        DateTimeFormatter humanReadableFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy 'at' hh:mm:ss a");
        String formattedDate = currentDateTime.format(humanReadableFormatter);
        System.out.println("   Formatted Custom Pattern : " + formattedDate);

        // Parsing ISO String back to LocalDateTime
        String isoInput = "2026-11-18T14:30:00";
        LocalDateTime parsed = LocalDateTime.parse(isoInput, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        System.out.println("   Parsed from ISO String   : " + parsed);

        // 6. Time Arithmetic (Adding intervals)
        System.out.println("\n6. Date & Time Arithmetic (Message Expiry & Offer Validity):");
        LocalDateTime offerExpires = currentDateTime.plusDays(3).plusHours(6);
        System.out.println("   Offer created: " + currentDateTime);
        System.out.println("   Offer expires: " + offerExpires);

        System.out.println("\n>> Academic Viva Note: java.time vs java.util.Date <<");
        System.out.println("1. Immutability & Thread-Safety: All java.time classes are immutable and thread-safe,");
        System.out.println("   unlike legacy java.util.Date and SimpleDateFormat which had shared mutable state.");
        System.out.println("2. Clarity of Domain: Separate classes represent pure dates (LocalDate), pure times");
        System.out.println("   (LocalTime), timestamps (Instant), and zoned dates (ZonedDateTime).");
        System.out.println("3. ISO Standard: Default toString and parse methods follow ISO-8601 standard.");
        System.out.println("======================================================================\n");
    }
}
