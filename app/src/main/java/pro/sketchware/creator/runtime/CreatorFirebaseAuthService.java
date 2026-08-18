package pro.sketchware.creator.runtime;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.Map;

/** Runtime-native Firebase email, anonymous, and sign-out authentication service. */
public final class CreatorFirebaseAuthService implements CreatorRuntimeService {
    private final CreatorRuntimeEnvironment environment;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    public CreatorFirebaseAuthService(CreatorRuntimeEnvironment environment) { this.environment = environment; }
    @Override public String getId() { return "firebase_auth"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        if ("status".equals(action)) return status();
        if ("sign_out".equals(action)) {
            auth.signOut();
            return CreatorRuntimeServiceArguments.succeeded("signedOut", true);
        }
        if ("anonymous".equals(action)) {
            auth.signInAnonymously().addOnSuccessListener(result -> publishUser("signed_in", result.getUser()))
                    .addOnFailureListener(error -> publishError(action, error));
            return CreatorRuntimeServiceArguments.succeeded("started", true, "action", action);
        }
        String email = CreatorRuntimeServiceArguments.string(arguments, "email");
        String password = CreatorRuntimeServiceArguments.string(arguments, "password");
        if ("sign_in".equals(action) || "register".equals(action)) {
            if (email == null || password == null) return CreatorRuntimeServiceArguments.invalid(action + " requires email and password.");
            ("sign_in".equals(action) ? auth.signInWithEmailAndPassword(email, password)
                    : auth.createUserWithEmailAndPassword(email, password))
                    .addOnSuccessListener(result -> publishUser("signed_in", result.getUser()))
                    .addOnFailureListener(error -> publishError(action, error));
            return CreatorRuntimeServiceArguments.succeeded("started", true, "action", action);
        }
        return CreatorRuntimeServiceArguments.invalid("Unsupported firebase auth action: " + action);
    }

    private Result status() {
        FirebaseUser user = auth.getCurrentUser();
        return CreatorRuntimeServiceArguments.succeeded("signedIn", user != null,
                "uid", user == null ? null : user.getUid(), "email", user == null ? null : user.getEmail());
    }
    private void publishUser(String event, FirebaseUser user) {
        environment.publish(getId(), event, CreatorRuntimeServiceArguments.output(
                "uid", user == null ? null : user.getUid(), "email", user == null ? null : user.getEmail()));
    }
    private void publishError(String action, Exception error) {
        environment.publish(getId(), "error", CreatorRuntimeServiceArguments.output(
                "action", action, "message", error.getMessage()));
    }
}
