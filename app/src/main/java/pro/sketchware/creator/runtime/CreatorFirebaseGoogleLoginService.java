package pro.sketchware.creator.runtime;

import android.content.Intent;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import java.util.Map;

/** Runtime-native Google sign-in launcher; returned activity results are published by the runtime environment. */
public final class CreatorFirebaseGoogleLoginService implements CreatorRuntimeService {
    private final CreatorRuntimeEnvironment environment;
    public CreatorFirebaseGoogleLoginService(CreatorRuntimeEnvironment environment) { this.environment = environment; }
    @Override public String getId() { return "firebase_auth_google"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        if (!"sign_in".equals(action)) return CreatorRuntimeServiceArguments.invalid("Unsupported Google sign-in action: " + action);
        String clientId = CreatorRuntimeServiceArguments.string(arguments, "webClientId");
        if (clientId == null) return CreatorRuntimeServiceArguments.invalid("sign_in requires webClientId from the configured Firebase project.");
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail().requestIdToken(clientId).build();
        GoogleSignInClient client = GoogleSignIn.getClient(environment.getActivity(), options);
        Intent intent = client.getSignInIntent();
        environment.launchForResult(getId(), "result", intent);
        return CreatorRuntimeServiceArguments.succeeded("started", true);
    }
}
