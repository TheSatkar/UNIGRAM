package com.unigram.api;

/**
 * Request payload for dispatching a message into a conversation thread.
 * Sender identity is sourced separately from the development header X-Dev-User-Id.
 */
public class SendMessageRequest {

    private String content;

    public SendMessageRequest() {
    }

    public SendMessageRequest(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
