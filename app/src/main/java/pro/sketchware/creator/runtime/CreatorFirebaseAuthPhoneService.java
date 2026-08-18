package pro.sketchware.creator.runtime;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Runtime-native Firebase phone-verification service. */
public final class CreatorFirebaseAuthPhoneService implements CreatorRuntimeService {
    private final CreatorRuntimeEnvironment environment;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private String verificationId;

    public CreatorFirebaseAuthPhoneService(CreatorRuntimeEnvironment environment) { this.environment = environment; }
    @Override public String getId() { return "firebase_auth_phone"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        if ("send_code".equals(action)) {
            String number = CreatorRuntimeServiceArguments.string(arguments, "phoneNumber");
            if (number == null) return CreatorRuntimeServiceArguments.invalid("send_code requires phoneNumber in E.164 format.");
            PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber(number).setTimeout(60L, TimeUnit.SECONDS).setActivity(environment.getActivity())
                    .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        @Override public void onVerificationCompleted(PhoneAuthCredential credential) {
                            auth.signInWithCredential(credential).addOnSuccessListener(result -> environment.publish(getId(), "signed_in",
                                    CreatorRuntimeServiceArguments.output("uid", result.getUser() == null ? null : result.getUser().getUid())))
                                    .addOnFailureListener(error -> publishError("sign_in", error));
                        }
                        @Override public void onVerificationFailed(com.google.firebase.FirebaseException error) { publishError("send_code", error); }
                        @Override public void onCodeSent(String id, PhoneAuthProvider.ForceResendingToken token) {
                            verificationId = id;
                            environment.publish(getId(), "code_sent", CreatorRuntimeServiceArguments.output("verificationId", id));
                        }
                    }).build();
            PhoneAuthProvider.verifyPhoneNumber(options);
            return CreatorRuntimeServiceArguments.succeeded("started", true);
        }
        if ("confirm_code".equals(action)) {
            String code = CreatorRuntimeServiceArguments.string(arguments, "code");
            String id = CreatorRuntimeServiceArguments.string(arguments, "verificationId");
            if (id == null) id = verificationId;
            if (id == null || code == null) return CreatorRuntimeServiceArguments.invalid("confirm_code requires verificationId and code.");
            auth.signInWithCredential(PhoneAuthProvider.getCredential(id, code))
                    .addOnSuccessListener(result -> environment.publish(getId(), "signed_in",
                            CreatorRuntimeServiceArguments.output("uid", result.getUser() == null ? null : result.getUser().getUid())))
                    .addOnFailureListener(error -> publishError("confirm_code", error));
            return CreatorRuntimeServiceArguments.succeeded("started", true);
        }
        return CreatorRuntimeServiceArguments.invalid("Unsupported phone-auth action: " + action);
    }

    private void publishError(String action, Exception error) {
        environment.publish(getId(), "error", CreatorRuntimeServiceArguments.output(
                "action", action, "message", error.getMessage()));
    }
}
