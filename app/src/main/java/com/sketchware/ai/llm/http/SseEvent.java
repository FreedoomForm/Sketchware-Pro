package com.sketchware.ai.llm.http;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * One Server-Sent-Event: an optional {@code event} field and a non-empty
 * {@code data} field.
 */
public final class SseEvent {
    public final String event;
    public final String data;

    public SseEvent(String event, String data) {
        this.event = event;
        this.data = data;
    }

    @NonNull
    @Override
    public String toString() {
        return "SseEvent{event='" + event + "', data='" + (data == null ? "" : (data.length() > 200 ? data.substring(0, 200) + "..." : data)) + "'}";
    }
}
