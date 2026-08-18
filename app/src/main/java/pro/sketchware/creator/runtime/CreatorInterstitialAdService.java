package pro.sketchware.creator.runtime;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import java.util.Map;

/** Runtime-native interstitial-ad service; unit IDs remain visible project configuration. */
public final class CreatorInterstitialAdService implements CreatorRuntimeService {
    private final CreatorRuntimeEnvironment environment;
    private InterstitialAd interstitialAd;
    private String loadedUnitId;

    public CreatorInterstitialAdService(CreatorRuntimeEnvironment environment) { this.environment = environment; }
    @Override public String getId() { return "ads_interstitial"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        if ("load".equals(action)) {
            String adUnitId = CreatorRuntimeServiceArguments.string(arguments, "adUnitId");
            if (adUnitId == null) return CreatorRuntimeServiceArguments.invalid("load requires configured adUnitId.");
            InterstitialAd.load(environment.getActivity(), adUnitId, new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
                @Override public void onAdLoaded(InterstitialAd ad) {
                    interstitialAd = ad;
                    loadedUnitId = adUnitId;
                    environment.publish(getId(), "loaded", CreatorRuntimeServiceArguments.output("adUnitId", adUnitId));
                }
                @Override public void onAdFailedToLoad(LoadAdError error) {
                    interstitialAd = null;
                    environment.publish(getId(), "error", CreatorRuntimeServiceArguments.output(
                            "action", "load", "message", error.getMessage()));
                }
            });
            return CreatorRuntimeServiceArguments.succeeded("started", true, "action", action);
        }
        if ("show".equals(action)) {
            if (interstitialAd == null) return CreatorRuntimeServiceArguments.failed("No loaded interstitial ad is available.");
            interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override public void onAdShowedFullScreenContent() { publish("shown"); }
                @Override public void onAdDismissedFullScreenContent() { interstitialAd = null; publish("dismissed"); }
                @Override public void onAdFailedToShowFullScreenContent(AdError error) {
                    interstitialAd = null;
                    environment.publish(getId(), "error", CreatorRuntimeServiceArguments.output(
                            "action", "show", "message", error.getMessage()));
                }
            });
            interstitialAd.show(environment.getActivity());
            return CreatorRuntimeServiceArguments.succeeded("started", true, "action", action, "adUnitId", loadedUnitId);
        }
        return CreatorRuntimeServiceArguments.invalid("Unsupported interstitial-ad action: " + action);
    }
    private void publish(String event) {
        environment.publish(getId(), event, CreatorRuntimeServiceArguments.output("adUnitId", loadedUnitId));
    }
}
