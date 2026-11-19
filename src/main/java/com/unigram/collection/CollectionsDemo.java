package com.unigram.collection;

import com.unigram.model.User;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

/**
 * Syllabus Demonstration Class for the Java Collections Framework.
 * 
 * Provides clear, academic demonstrations of:
 * 1. ArrayList and LinkedList (List interface)
 * 2. HashSet (Set interface, duplicate prevention)
 * 3. HashMap (Map interface, key-value lookup)
 * 4. Queue and Deque (FIFO and double-ended queues)
 * 5. PriorityQueue with Comparable natural ordering
 * 6. Iterators (Iterator and ListIterator for bidirectional traversal)
 * 7. Enhanced for-each traversal
 * 8. Comparable vs Comparator
 * 9. Collections utility methods (sort, reverse, unmodifiableList, max, min)
 */
public class CollectionsDemo {

    /**
     * Demonstrates Comparable by modeling student support/moderation alerts with natural priority.
     * Lower number = higher urgency (1 is highest priority).
     */
    public static class SupportAlert implements Comparable<SupportAlert> {
        private final String alertId;
        private final int urgency; // 1 = Urgent, 2 = Medium, 3 = Normal
        private final String message;

        public SupportAlert(String alertId, int urgency, String message) {
            this.alertId = alertId;
            this.urgency = urgency;
            this.message = message;
        }

        public String getAlertId() {
            return alertId;
        }

        public int getUrgency() {
            return urgency;
        }

        public String getMessage() {
            return message;
        }

        // Natural ordering based on urgency level
        @Override
        public int compareTo(SupportAlert other) {
            return Integer.compare(this.urgency, other.urgency);
        }

        @Override
        public String toString() {
            return "Alert[ID=" + alertId + ", Urgency=" + urgency + ", Message='" + message + "']";
        }
    }

    /**
     * Executes the comprehensive Java Collections Framework demonstration.
     */
    public static void runCollectionsDemonstration() {
        System.out.println("======================================================================");
        System.out.println("       Java Collections Framework — Syllabus Demonstration           ");
        System.out.println("======================================================================\n");

        // 1. ArrayList (Fast random access, indexed list)
        System.out.println("--- 1. ArrayList Demonstration ---");
        List<String> studentList = new ArrayList<>();
        studentList.add("Alex Chen (CS)");
        studentList.add("Maya Patel (EE)");
        studentList.add("Liam O'Connor (Bio)");
        System.out.println("ArrayList initial: " + studentList);
        System.out.println("Access element at index 1: " + studentList.get(1));
        System.out.println();

        // 2. LinkedList (Fast head/tail additions and removals)
        System.out.println("--- 2. LinkedList Demonstration ---");
        LinkedList<String> recentActivity = new LinkedList<>();
        recentActivity.addFirst("Login: Maya");
        recentActivity.addLast("Message sent: Alex -> Maya");
        recentActivity.addFirst("Login: Alex");
        System.out.println("LinkedList recent activity (head to tail): " + recentActivity);
        System.out.println("Head removed: " + recentActivity.removeFirst());
        System.out.println("Tail removed: " + recentActivity.removeLast());
        System.out.println("Remaining in LinkedList: " + recentActivity);
        System.out.println();

        // 3. HashSet (Set interface: uniqueness and duplicate prevention)
        System.out.println("--- 3. HashSet Demonstration ---");
        Set<String> activeStudentTags = new HashSet<>();
        activeStudentTags.add("freshman");
        activeStudentTags.add("textbook_buyer");
        activeStudentTags.add("freshman"); // Duplicate entry
        activeStudentTags.add("dorm_seller");
        System.out.println("HashSet tags (Notice duplicate 'freshman' was rejected): " + activeStudentTags);
        System.out.println("Contains 'dorm_seller'? " + activeStudentTags.contains("dorm_seller"));
        System.out.println();

        // 4. HashMap (Key-Value associations, fast O(1) lookups)
        System.out.println("--- 4. HashMap Demonstration ---");
        Map<String, String> studentMajorMap = new HashMap<>();
        studentMajorMap.put("usr_01", "Computer Science");
        studentMajorMap.put("usr_02", "Electrical Engineering");
        studentMajorMap.put("usr_03", "Mechanical Engineering");
        System.out.println("Lookup major for 'usr_02': " + studentMajorMap.get("usr_02"));
        System.out.println("KeySet: " + studentMajorMap.keySet());
        System.out.println("Values: " + studentMajorMap.values());
        System.out.println();

        // 5. Queue (FIFO interface using ArrayDeque)
        System.out.println("--- 5. Queue (FIFO) Demonstration ---");
        Queue<String> stagingQueue = new ArrayDeque<>();
        stagingQueue.offer("Pending Msg 1");
        stagingQueue.offer("Pending Msg 2");
        stagingQueue.offer("Pending Msg 3");
        System.out.println("Queue peek front: " + stagingQueue.peek());
        System.out.println("Polled from Queue (FIFO): " + stagingQueue.poll());
        System.out.println("Polled from Queue (FIFO): " + stagingQueue.poll());
        System.out.println("Remaining Queue: " + stagingQueue);
        System.out.println();

        // 6. Deque (Double-Ended Queue using ArrayDeque)
        System.out.println("--- 6. Deque Demonstration ---");
        Deque<String> messageDeque = new ArrayDeque<>();
        messageDeque.addLast("Msg 1: Hey there");
        messageDeque.addLast("Msg 2: Still available?");
        messageDeque.addFirst("Msg 0 (Urgent Header): Listing inquiry");
        System.out.println("Deque front (oldest): " + messageDeque.peekFirst());
        System.out.println("Deque back (newest): " + messageDeque.peekLast());
        System.out.println("Full Deque: " + messageDeque);
        System.out.println();

        // 7. PriorityQueue (Demonstrating Comparable natural ordering)
        System.out.println("--- 7. PriorityQueue & Comparable Demonstration ---");
        PriorityQueue<SupportAlert> alertQueue = new PriorityQueue<>();
        alertQueue.offer(new SupportAlert("ALT-03", 3, "Profile avatar update issue (Normal)"));
        alertQueue.offer(new SupportAlert("ALT-01", 1, "Suspicious scam listing reported (Urgent!)"));
        alertQueue.offer(new SupportAlert("ALT-02", 2, "Payment dispute reported (Medium)"));

        System.out.println("Draining PriorityQueue (Notice highest urgency is served first):");
        while (!alertQueue.isEmpty()) {
            System.out.println(" -> Polled: " + alertQueue.poll());
        }
        System.out.println();

        // 8. Iterator and ListIterator (Bidirectional traversal & element removal)
        System.out.println("--- 8. Iterator and ListIterator Demonstration ---");
        List<String> notificationList = new ArrayList<>();
        notificationList.add("Note A: New message received");
        notificationList.add("Note B: Spam advertisement");
        notificationList.add("Note C: Product marked as sold");

        System.out.println("Using standard Iterator to filter out spam:");
        Iterator<String> noteIterator = notificationList.iterator();
        while (noteIterator.hasNext()) {
            String note = noteIterator.next();
            if (note.contains("Spam")) {
                noteIterator.remove(); // Safe removal during iteration
                System.out.println("    Removed: " + note);
            }
        }
        System.out.println("Notifications after Iterator cleanup: " + notificationList);

        System.out.println("Using ListIterator for bidirectional traversal:");
        ListIterator<String> listIterator = notificationList.listIterator();
        System.out.print("  Forward: ");
        while (listIterator.hasNext()) {
            System.out.print("[" + listIterator.next() + "] ");
        }
        System.out.println();
        System.out.print("  Backward: ");
        while (listIterator.hasPrevious()) {
            System.out.print("[" + listIterator.previous() + "] ");
        }
        System.out.println("\n");

        // 9. for-each traversal
        System.out.println("--- 9. Enhanced for-each Traversal ---");
        for (String student : studentList) {
            System.out.println(" * Traversed student: " + student);
        }
        System.out.println();

        // 10. Comparator & Collections Utility Sorting
        System.out.println("--- 10. Comparator & Collections Sorting Demonstration ---");
        List<User> demoUsers = new ArrayList<>();
        demoUsers.add(new User("u3", "Zoe Saldana", "zoe@uni.edu"));
        demoUsers.add(new User("u1", "Alex Chen", "alex@uni.edu"));
        demoUsers.add(new User("u2", "Maya Patel", "maya@uni.edu"));

        System.out.println("Users before sorting:");
        for (User u : demoUsers) System.out.println("  " + u.getFullName() + " (" + u.getUserId() + ")");

        // Sort by full name using custom Comparator
        Collections.sort(demoUsers, Comparator.comparing(User::getFullName));
        System.out.println("Users sorted alphabetically by full name (Collections.sort + Comparator):");
        for (User u : demoUsers) System.out.println("  " + u.getFullName() + " (" + u.getUserId() + ")");

        // Reverse order
        Collections.reverse(demoUsers);
        System.out.println("Users in reverse order (Collections.reverse):");
        for (User u : demoUsers) System.out.println("  " + u.getFullName());

        // Unmodifiable collection demonstration
        List<User> safeUserList = Collections.unmodifiableList(demoUsers);
        System.out.println("Created unmodifiable list view of size: " + safeUserList.size());
        System.out.println();
    }
}
