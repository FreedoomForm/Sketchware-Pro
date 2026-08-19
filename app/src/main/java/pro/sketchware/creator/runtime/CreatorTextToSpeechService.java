package pro.sketchware.creator.runtime;

import android.speech.tts.TextToSpeech;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

/** Runtime-native TextToSpeech component with explicit initialization status. */
public final class CreatorTextToSpeechService implements CreatorRuntimeService, TextToSpeech.OnInitListener {
    private final CreatorRuntimeEnvironment environment;
    private TextToSpeech textToSpeech;
    private boolean ready;

    public CreatorTextToSpeechService(CreatorRuntimeEnvironment environment) {
        this.environment = environment;
        this.textToSpeech = new TextToSpeech(environment.getContext(), this);
    }
    @Override public String getId() { return "text_to_speech"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        if ("is_speaking".equals(action)) {
            return CreatorRuntimeServiceArguments.succeeded("value", ready && textToSpeech.isSpeaking());
        }
        if ("set_pitch".equals(action) || "set_rate".equals(action)) {
            if (!ready) return CreatorRuntimeServiceArguments.failed("Text-to-speech is still initializing.");
            try {
                float value = CreatorRuntimeServiceArguments.floatValue(arguments, "value", 1f);
                if (value <= 0f || value > 4f) {
                    return CreatorRuntimeServiceArguments.invalid("Text-to-speech pitch and rate must be greater than 0 and at most 4.");
                }
                if ("set_pitch".equals(action)) textToSpeech.setPitch(value);
                else textToSpeech.setSpeechRate(value);
                return CreatorRuntimeServiceArguments.succeeded("action", action, "value", value);
            } catch (NumberFormatException error) {
                return CreatorRuntimeServiceArguments.invalid("Text-to-speech pitch and rate must be numeric.");
            }
        }
        if ("speak".equals(action)) {
            String text = CreatorRuntimeServiceArguments.string(arguments, "text");
            if (text == null) return CreatorRuntimeServiceArguments.invalid("speak requires text.");
            if (!ready) return CreatorRuntimeServiceArguments.failed("Text-to-speech is still initializing.");
            String language = CreatorRuntimeServiceArguments.string(arguments, "language");
            if (language != null) textToSpeech.setLanguage(Locale.forLanguageTag(language));
            int outcome = textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "creator-runtime");
            return outcome == TextToSpeech.SUCCESS ? CreatorRuntimeServiceArguments.succeeded("spoken", true)
                    : CreatorRuntimeServiceArguments.failed("Android could not queue the text-to-speech request.");
        }
        if ("stop".equals(action)) {
            textToSpeech.stop();
            return CreatorRuntimeServiceArguments.succeeded("stopped", true);
        }
        if ("shutdown".equals(action)) {
            textToSpeech.shutdown();
            ready = false;
            return CreatorRuntimeServiceArguments.succeeded("shutdown", true);
        }
        return CreatorRuntimeServiceArguments.invalid("Unsupported text-to-speech action: " + action);
    }

    @Override public void onInit(int status) {
        ready = status == TextToSpeech.SUCCESS;
        environment.publish(getId(), ready ? "ready" : "error", CreatorRuntimeServiceArguments.output("ready", ready));
    }
}
