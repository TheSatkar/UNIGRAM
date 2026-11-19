package com.unigram.util;

import com.unigram.model.Message;

/**
 * Custom Functional Interface demonstrating Single Abstract Method (SAM)
 * for filtering messages with default composition methods.
 */
@FunctionalInterface
public interface MessageFilter {

    /**
     * Evaluates this predicate on the given message.
     *
     * @param message Message entity to evaluate
     * @return true if the message satisfies the filter condition, false otherwise
     */
    boolean accept(Message message);

    /**
     * Returns a composed filter that represents a short-circuiting logical AND of this filter and another.
     */
    default MessageFilter and(MessageFilter other) {
        if (other == null) {
            throw new IllegalArgumentException("Other filter cannot be null.");
        }
        return m -> this.accept(m) && other.accept(m);
    }

    /**
     * Returns a composed filter that represents a short-circuiting logical OR of this filter and another.
     */
    default MessageFilter or(MessageFilter other) {
        if (other == null) {
            throw new IllegalArgumentException("Other filter cannot be null.");
        }
        return m -> this.accept(m) || other.accept(m);
    }

    /**
     * Returns a filter that represents the logical negation of this filter.
     */
    default MessageFilter negate() {
        return m -> !this.accept(m);
    }
}
