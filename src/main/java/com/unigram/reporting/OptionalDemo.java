package com.unigram.reporting;

import com.unigram.model.Message;
import com.unigram.model.User;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Demonstrates the java.util.Optional API:
 * 1. Creation: empty(), of(), ofNullable()
 * 2. Inspection: isPresent(), isEmpty(), ifPresent(), ifPresentOrElse()
 * 3. Fallbacks: orElse(), orElseGet(), orElseThrow()
 * 4. Transformation: map(), flatMap()
 * 5. Architectural justification for returning Optional from DAOs
 */
public class OptionalDemo {

    public static void runDemonstration() {
        System.out.println("======================================================================");
        System.out.println("     5. Optional API & Null-Defense Patterns Demonstration            ");
        System.out.println("======================================================================\n");

        User existingUser = new User("usr_opt_1", "Linus Torvalds", "linus@kernel.org");
        User nullUser = null;

        // 1. Creation Variants
        System.out.println("1. Optional Creation Variants:");
        Optional<User> emptyOpt = Optional.empty();
        Optional<User> presentOpt = Optional.of(existingUser); // Throws NullPointerException if null!
        Optional<User> nullableOpt1 = Optional.ofNullable(existingUser);
        Optional<User> nullableOpt2 = Optional.ofNullable(nullUser); // Returns Optional.empty() safely

        System.out.printf("   emptyOpt.isPresent()    : %b%n", emptyOpt.isPresent());
        System.out.printf("   presentOpt.isPresent()  : %b%n", presentOpt.isPresent());
        System.out.printf("   nullableOpt2.isEmpty()  : %b%n", nullableOpt2.isEmpty());

        // 2. Inspection & Conditional Execution: ifPresent vs ifPresentOrElse
        System.out.println("\n2. Conditional Actions (ifPresent & ifPresentOrElse):");
        presentOpt.ifPresent(u -> System.out.println("   [Found] User name: " + u.getFullName()));

        nullableOpt2.ifPresentOrElse(
                u -> System.out.println("   [Found]: " + u.getFullName()),
                () -> System.out.println("   [NotFound Action] Executed fallback Runnable: User does not exist.")
        );

        // 3. Fallback Strategies: orElse vs orElseGet
        System.out.println("\n3. Fallback Strategies (orElse vs orElseGet):");
        User guestFallback = new User("usr_guest", "Guest Student", "guest@uni.edu");

        // orElse: evaluated eagerly regardless of whether optional is present
        User u1 = nullableOpt2.orElse(guestFallback);
        System.out.println("   orElse result: " + u1.getFullName());

        // orElseGet: evaluated lazily only when optional is empty (avoids unnecessary computation)
        User u2 = nullableOpt2.orElseGet(() -> {
            System.out.println("   [Lazy Supplier Triggered] Generating synthetic fallback user...");
            return new User("usr_lazy", "Lazy User", "lazy@uni.edu");
        });
        System.out.println("   orElseGet result: " + u2.getFullName());

        // 4. orElseThrow: Explicit Domain Exception on Missing Entity
        System.out.println("\n4. orElseThrow Exception Handling:");
        try {
            nullableOpt2.orElseThrow(() -> new IllegalArgumentException("Mandatory student profile not found in catalog."));
        } catch (IllegalArgumentException e) {
            System.out.println("   Caught expected exception: " + e.getMessage());
        }

        // 5. Transformation via map() and flatMap()
        System.out.println("\n5. Functional Transformations (map & flatMap):");
        // map(): extracts email string from User optional
        Optional<String> emailOpt = presentOpt.map(User::getEmail);
        emailOpt.ifPresent(e -> System.out.println("   Mapped User -> Email: " + e));

        // flatMap(): flattens nested Optional<Optional<T>>
        Optional<Message> sampleMsg = Optional.of(new Message("m_opt", "c1", "usr_opt_1", "Testing Optional", LocalDateTime.now()));
        Optional<String> contentUpper = sampleMsg
                .map(Message::getContent)
                .map(String::toUpperCase);
        System.out.println("   Transformed Message Content: " + contentUpper.orElse("EMPTY"));

        System.out.println("\n>> Academic Viva Note: Purpose of Optional <<");
        System.out.println("1. Optional is specifically intended as a RETURN TYPE for methods where a value may be absent.");
        System.out.println("2. It forces callers to explicitly handle absence, preventing NullPointerException.");
        System.out.println("3. Best Practice: Do NOT use Optional as method parameters, class fields, or wrap collections.");
        System.out.println("======================================================================\n");
    }
}
