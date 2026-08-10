package com.sketchware.ai.llm.http;

import static com.google.common.truth.Truth.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

/**
 * Unit tests for {@link SseParser}.
 */
public class SseParserTest {

    @Test public void parsesSimpleDataEvent() {
        String input = "data: {\"hello\":\"world\"}\n\n";
        SseParser parser = new SseParser(stream(input));
        java.util.List<SseEvent> events = new java.util.ArrayList<>();
        for (SseEvent e : parser) events.add(e);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).event).isNull();
        assertThat(events.get(0).data).isEqualTo("{\"hello\":\"world\"}");
    }

    @Test public void parsesEventAndData() {
        String input = "event: message_start\ndata: {\"type\":\"message_start\"}\n\n";
        SseParser parser = new SseParser(stream(input));
        java.util.List<SseEvent> events = new java.util.ArrayList<>();
        for (SseEvent e : parser) events.add(e);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).event).isEqualTo("message_start");
        assertThat(events.get(0).data).isEqualTo("{\"type\":\"message_start\"}");
    }

    @Test public void parsesMultipleEvents() {
        String input =
            "event: first\ndata: 1\n\n" +
            "event: second\ndata: 2\n\n";
        SseParser parser = new SseParser(stream(input));
        java.util.List<SseEvent> events = new java.util.ArrayList<>();
        for (SseEvent e : parser) events.add(e);
        assertThat(events).hasSize(2);
        assertThat(events.get(0).event).isEqualTo("first");
        assertThat(events.get(0).data).isEqualTo("1");
        assertThat(events.get(1).event).isEqualTo("second");
        assertThat(events.get(1).data).isEqualTo("2");
    }

    @Test public void joinsMultiLineData() {
        String input = "data: line1\ndata: line2\ndata: line3\n\n";
        SseParser parser = new SseParser(stream(input));
        java.util.List<SseEvent> events = new java.util.ArrayList<>();
        for (SseEvent e : parser) events.add(e);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).data).isEqualTo("line1\nline2\nline3");
    }

    @Test public void skipsComments() {
        String input = ": this is a comment\ndata: real\n\n";
        SseParser parser = new SseParser(stream(input));
        java.util.List<SseEvent> events = new java.util.ArrayList<>();
        for (SseEvent e : parser) events.add(e);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).data).isEqualTo("real");
    }

    @Test public void preservesDoneMarker() {
        String input = "data: [DONE]\n\n";
        SseParser parser = new SseParser(stream(input));
        java.util.List<SseEvent> events = new java.util.ArrayList<>();
        for (SseEvent e : parser) events.add(e);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).data).isEqualTo("[DONE]");
    }

    @Test public void parsesAnthropicLikeStream() {
        String input =
            "event: message_start\ndata: {\"type\":\"message_start\"}\n\n" +
            "event: content_block_delta\ndata: {\"type\":\"content_block_delta\",\"delta\":{\"type\":\"text_delta\",\"text\":\"Hello\"}}\n\n" +
            "event: content_block_delta\ndata: {\"type\":\"content_block_delta\",\"delta\":{\"type\":\"text_delta\",\"text\":\" world\"}}\n\n" +
            "event: message_stop\ndata: {\"type\":\"message_stop\"}\n\n";
        SseParser parser = new SseParser(stream(input));
        java.util.List<SseEvent> events = new java.util.ArrayList<>();
        for (SseEvent e : parser) events.add(e);
        assertThat(events).hasSize(4);
        assertThat(events.get(0).event).isEqualTo("message_start");
        assertThat(events.get(1).event).isEqualTo("content_block_delta");
        assertThat(events.get(2).event).isEqualTo("content_block_delta");
        assertThat(events.get(3).event).isEqualTo("message_stop");
    }

    @Test public void parsesOpenAiLikeStream() {
        String input =
            "data: {\"choices\":[{\"delta\":{\"content\":\"Hello\"}}]}\n\n" +
            "data: {\"choices\":[{\"delta\":{\"content\":\" world\"}}]}\n\n" +
            "data: [DONE]\n\n";
        SseParser parser = new SseParser(stream(input));
        java.util.List<SseEvent> events = new java.util.ArrayList<>();
        for (SseEvent e : parser) events.add(e);
        assertThat(events).hasSize(3);
        assertThat(events.get(0).event).isNull();
        assertThat(events.get(0).data).contains("Hello");
        assertThat(events.get(1).data).contains(" world");
        assertThat(events.get(2).data).isEqualTo("[DONE]");
    }

    @Test public void handlesCRLFLineEndings() {
        String input = "event: test\r\ndata: payload\r\n\r\n";
        SseParser parser = new SseParser(stream(input));
        java.util.List<SseEvent> events = new java.util.ArrayList<>();
        for (SseEvent e : parser) events.add(e);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).event).isEqualTo("test");
        assertThat(events.get(0).data).isEqualTo("payload");
    }

    @Test public void dataWithLeadingSpaceIsTrimmedToOneSpace() {
        // Per spec, "data: value" → "value"; "data:value" → "value" (one leading space is stripped).
        String input = "data:   spaced\n\n";
        SseParser parser = new SseParser(stream(input));
        java.util.List<SseEvent> events = new java.util.ArrayList<>();
        for (SseEvent e : parser) events.add(e);
        assertThat(events).hasSize(1);
        // The parser strips exactly one leading space.
        assertThat(events.get(0).data).isEqualTo("  spaced");
    }

    @Test public void emptyInputStreamProducesNoEvents() {
        SseParser parser = new SseParser(stream(""));
        java.util.List<SseEvent> events = new java.util.ArrayList<>();
        for (SseEvent e : parser) events.add(e);
        assertThat(events).isEmpty();
    }

    @Test public void blankDataLineProducesEmptyEvent() {
        // "data:" with no value is a valid event with empty data.
        String input = "data:\n\n";
        SseParser parser = new SseParser(stream(input));
        java.util.List<SseEvent> events = new java.util.ArrayList<>();
        for (SseEvent e : parser) events.add(e);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).data).isEqualTo("");
    }

    private ByteArrayInputStream stream(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }
}
