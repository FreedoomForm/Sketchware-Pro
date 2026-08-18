package pro.sketchware.creator.runtime;

import android.Manifest;
import android.content.Intent;
import android.speech.RecognizerIntent;
import java.util.Collections;
import java.util.Map;

/** Runtime-native speech-recognition activity bridge. */
public final class CreatorSpeechToTextService implements CreatorRuntimeService {
    private final CreatorRuntimeEnvironment environment;
    public CreatorSpeechToTextService(CreatorRuntimeEnvironment environment) { this.environment = environment; }
    @Override public String getId() { return "speech_to_text"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        if (!"listen".equals(action)) return CreatorRuntimeServiceArguments.invalid("Unsupported speech-to-text action: " + action);
        if (!environment.hasPermission(Manifest.permission.RECORD_AUDIO)) {
            environment.requestPermission(getId(), Manifest.permission.RECORD_AUDIO);
            return new Result(Status.PERMISSION_REQUIRED, Collections.<String, Object>emptyMap(),
                    "Microphone permission was requested.");
        }
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_PROMPT, CreatorRuntimeServiceArguments.string(arguments, "prompt"));
        String language = CreatorRuntimeServiceArguments.string(arguments, "language");
        if (language != null) intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language);
        if (intent.resolveActivity(environment.getActivity().getPackageManager()) == null) {
            return CreatorRuntimeServiceArguments.failed("This device has no speech-recognition activity.");
        }
        environment.launchForResult(getId(), "recognized", intent);
        return CreatorRuntimeServiceArguments.succeeded("started", true);
    }
}
