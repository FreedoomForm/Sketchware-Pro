package pro.sketchware.creator.runtime;

import android.Manifest;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Map;

/** Runtime-native speech recognizer with explicit listen, stop, and shutdown actions. */
public final class CreatorSpeechToTextService implements CreatorRuntimeService, RecognitionListener {
    private final CreatorRuntimeEnvironment environment;
    private SpeechRecognizer recognizer;
    private boolean listening;
    public CreatorSpeechToTextService(CreatorRuntimeEnvironment environment) { this.environment = environment; }
    @Override public String getId() { return "speech_to_text"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        if ("stop".equals(action)) {
            if (recognizer != null && listening) recognizer.stopListening();
            listening = false;
            return CreatorRuntimeServiceArguments.succeeded("stopped", true);
        }
        if ("shutdown".equals(action)) {
            if (recognizer != null) {
                recognizer.cancel();
                recognizer.destroy();
                recognizer = null;
            }
            listening = false;
            return CreatorRuntimeServiceArguments.succeeded("shutdown", true);
        }
        if (!"listen".equals(action)) return CreatorRuntimeServiceArguments.invalid("Unsupported speech-to-text action: " + action);
        if (!environment.hasPermission(Manifest.permission.RECORD_AUDIO)) {
            environment.requestPermission(getId(), Manifest.permission.RECORD_AUDIO);
            return new Result(Status.PERMISSION_REQUIRED, Collections.<String, Object>emptyMap(),
                    "Microphone permission was requested.");
        }
        if (!SpeechRecognizer.isRecognitionAvailable(environment.getContext())) {
            return CreatorRuntimeServiceArguments.failed("This device has no speech-recognition service.");
        }
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(environment.getContext());
            recognizer.setRecognitionListener(this);
        }
        android.content.Intent intent = new android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_PROMPT, CreatorRuntimeServiceArguments.string(arguments, "prompt"));
        String language = CreatorRuntimeServiceArguments.string(arguments, "language");
        if (language != null) intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language);
        recognizer.startListening(intent);
        listening = true;
        return CreatorRuntimeServiceArguments.succeeded("started", true);
    }

    @Override public void onReadyForSpeech(Bundle params) { environment.publish(getId(), "ready", Collections.<String, Object>emptyMap()); }
    @Override public void onBeginningOfSpeech() { environment.publish(getId(), "beginning", Collections.<String, Object>emptyMap()); }
    @Override public void onRmsChanged(float rmsdB) { }
    @Override public void onBufferReceived(byte[] buffer) { }
    @Override public void onEndOfSpeech() { environment.publish(getId(), "end", Collections.<String, Object>emptyMap()); }
    @Override public void onError(int error) {
        listening = false;
        environment.publish(getId(), "error", CreatorRuntimeServiceArguments.output("code", error));
    }
    @Override public void onResults(Bundle results) {
        listening = false;
        ArrayList<String> matches = results == null ? null : results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        environment.publish(getId(), "recognized", CreatorRuntimeServiceArguments.output(
                "text", matches == null || matches.isEmpty() ? "" : matches.get(0),
                "matches", matches == null ? Collections.<String>emptyList() : new ArrayList<>(matches)));
    }
    @Override public void onPartialResults(Bundle partialResults) {
        ArrayList<String> matches = partialResults == null ? null : partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        environment.publish(getId(), "partial", CreatorRuntimeServiceArguments.output(
                "text", matches == null || matches.isEmpty() ? "" : matches.get(0)));
    }
    @Override public void onEvent(int eventType, Bundle params) { }
}
