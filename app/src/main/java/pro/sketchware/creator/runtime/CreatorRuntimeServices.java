package pro.sketchware.creator.runtime;

import android.content.Context;

/** Creates the complete reviewed service set that ships with a Creator Runtime project. */
public final class CreatorRuntimeServices {
    private CreatorRuntimeServices() { }

    public static CreatorRuntimeServiceDispatcher defaults(Context context, String projectId,
                                                           CreatorRuntimeEnvironment environment,
                                                           CreatorTimerService.Listener timerListener) {
        return new CreatorRuntimeServiceDispatcher()
                .register(new CreatorStorageService(context, projectId))
                .register(new CreatorFileService(environment))
                .register(new CreatorVibratorService(context))
                .register(new CreatorCalendarService())
                .register(new CreatorTimerService(timerListener))
                .register(new CreatorIntentService(environment))
                .register(new CreatorUiService(environment))
                .register(new CreatorDialogService(environment))
                .register(new CreatorNetworkService(environment))
                .register(new CreatorAnimatorService(environment))
                .register(new CreatorDatePickerService(environment))
                .register(new CreatorTimePickerService(environment))
                .register(new CreatorMediaService(environment))
                .register(new CreatorGyroscopeService(environment))
                .register(new CreatorNotificationService(environment))
                .register(new CreatorCameraService(environment))
                .register(new CreatorFilePickerService(environment))
                .register(new CreatorTextToSpeechService(environment))
                .register(new CreatorSpeechToTextService(environment))
                .register(new CreatorBluetoothService(environment))
                .register(new CreatorLocationService(environment))
                .register(new CreatorFirebaseDatabaseService(environment))
                .register(new CreatorFirebaseAuthService(environment))
                .register(new CreatorFirebaseStorageService(environment))
                .register(new CreatorFirebaseCloudMessageService(environment))
                .register(new CreatorFirebaseAuthPhoneService(environment))
                .register(new CreatorFirebaseGoogleLoginService(environment))
                .register(new CreatorInterstitialAdService(environment))
                .register(new CreatorRewardedAdService(environment))
                .register(new CreatorFragmentAdapterService(environment));
    }
}
