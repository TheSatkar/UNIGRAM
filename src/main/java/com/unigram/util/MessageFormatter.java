package com.unigram.util;

import com.unigram.model.Message;

/**
 * Custom Functional Interface demonstrating Single Abstract Method (SAM)
 * for transforming a Message entity into a formatted text representation.
 */
@FunctionalInterface
public interface MessageFormatter {

    /**
     * Formats the given message into a display string.
     *
     * @param message Message entity
     * @return formatted string
     */
    String format(Message message);

    /**
     * Pre-defined static utility formatter for standard console logging.
     */
    static String formatConsole(Message message) {
        if (message == null) return "[null]";
        return String.format("[%s] %s: \"%s\" (at %s)",
                message.getMessageId(),
                message.getSenderId(),
                message.getContent(),
                message.getSentAt());
    }

    /**
     * Pre-defined static utility formatter for CSV preview.
     */
    static String formatCompact(Message message) {
        if (message == null) return "[null]";
        return message.getSenderId() + " -> \"" + message.getContent() + "\"";
    }
}
