package com.sketchware.ai.llm.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Minimal but correct SSE parser. Reads an InputStream line by line
 * (according to the SSE spec, lines are terminated by \r\n, \r, or \n)
 * and produces {@link SseEvent} objects on the empty-line boundary.
 *
 * <p>Implements {@link Iterable} so the caller can use a for-each loop
 * to drain the stream.
 *
 * <p>This parser supports the spec's multi-line {@code data:} concatenation
 * (joined with "\n") and the {@code event:} field.
 */
public final class SseParser implements Iterable<SseEvent>, AutoCloseable {

    private final BufferedReader reader;
    private final boolean closeUnderlying;

    public SseParser(InputStream in) {
        this(in, true);
    }

    public SseParser(InputStream in, boolean closeUnderlying) {
        this.reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        this.closeUnderlying = closeUnderlying;
    }

    @Override
    public Iterator<SseEvent> iterator() {
        return new EventIterator();
    }

    @Override
    public void close() throws IOException {
        if (closeUnderlying) reader.close();
    }

    private final class EventIterator implements Iterator<SseEvent> {
        private SseEvent next;
        private boolean done;

        @Override
        public boolean hasNext() {
            if (next != null) return true;
            if (done) return false;
            try {
                next = readOne();
            } catch (IOException ioe) {
                done = true;
                if (ioe instanceof java.io.EOFException) return false;
                // swallow - end of stream
                return false;
            }
            if (next == null) {
                done = true;
                return false;
            }
            return true;
        }

        @Override
        public SseEvent next() {
            if (!hasNext()) throw new NoSuchElementException();
            SseEvent result = next;
            next = null;
            return result;
        }

        private SseEvent readOne() throws IOException {
            StringBuilder event = new StringBuilder();
            StringBuilder data = new StringBuilder();
            boolean sawAny = false;
            boolean sawData = false;
            String line;
            while ((line = reader.readLine()) != null) {
                sawAny = true;
                if (line.isEmpty()) {
                    if (sawData || event.length() > 0) {
                        return new SseEvent(
                                event.length() == 0 ? null : event.toString(),
                                data.toString());
                    }
                    continue;
                }
                if (line.startsWith(":")) continue; // comment
                int colon = line.indexOf(':');
                String field = colon == -1 ? line : line.substring(0, colon);
                String value = colon == -1 ? "" : line.substring(colon + 1);
                if (value.startsWith(" ")) value = value.substring(1);
                if (field.equals("event")) {
                    if (event.length() > 0) event.append("\n");
                    event.append(value);
                } else if (field.equals("data")) {
                    sawData = true;
                    if (data.length() > 0) data.append("\n");
                    data.append(value);
                }
            }
            if (!sawAny) throw new java.io.EOFException();
            if (sawData || event.length() > 0) {
                return new SseEvent(
                        event.length() == 0 ? null : event.toString(),
                        data.toString());
            }
            return null;
        }
    }
}
