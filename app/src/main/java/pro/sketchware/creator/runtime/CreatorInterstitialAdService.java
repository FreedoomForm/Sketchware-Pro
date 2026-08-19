package pro.sketchware.creator.runtime;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import java.util.LinkedHashMap;
import java.util.Map;

/** Runtime-native interstitial-ad service; unit IDs remain visible project configuration. */
public final class CreatorInterstitialAdService implements CreatorRuntimeService {
    private final CreatorRuntimeEnvironment environment;
    private final Map<String, InterstitialAd> interstitialAds = new LinkedHashMap<>();
    private final Map<String, String> loadedUnitIds = new LinkedHashMap<>();

    public CreatorInterstitialAdService(CreatorRuntimeEnvironment environment) { this.environment = environment; }
    @Override public String getId() { return "ads_interstitial"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        String componentId = CreatorRuntimeServiceArguments.string(arguments, "componentId");
        if (componentId == null || componentId.trim().isEmpty()) componentId = "runtime";
        final String id = componentId;
        if ("create".equals(action)) return CreatorRuntimeServiceArguments.succeeded("created", true, "componentId", id);
        if ("load".equals(action)) {
            String adUnitId = CreatorRuntimeServiceArguments.string(arguments, "adUnitId");
            if (adUnitId == null || adUnitId.trim().isEmpty()) {
                return CreatorRuntimeServiceArguments.invalid("load requires configured adUnitId.");
            }
            InterstitialAd.load(environment.getActivity(), adUnitId, new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
                @Override public void onAdLoaded(InterstitialAd ad) {
                    interstitialAds.put(id, ad);
                    loadedUnitIds.put(id, adUnitId);
                    environment.publish(getId(), "loaded", CreatorRuntimeServiceArguments.output(
                            "componentId", id, "adUnitId", adUnitId));
                }
                @Override public void onAdFailedToLoad(LoadAdError error) {
                    interstitialAds.remove(id);
                    environment.publish(getId(), "error", CreatorRuntimeServiceArguments.output(
                            "componentId", id, "action", "load", "message", error.getMessage()));
                }
            });
            return CreatorRuntimeServiceArguments.succeeded("started", true, "action", action, "componentId", id);
        }
        if ("show".equals(action)) {
            InterstitialAd interstitialAd = interstitialAds.get(id);
            if (interstitialAd == null) return CreatorRuntimeServiceArguments.failed("No loaded interstitial ad is available.");
            interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override public void onAdShowedFullScreenContent() { publish(id, "shown"); }
                @Override public void onAdDismissedFullScreenContent() { interstitialAds.remove(id); publish(id, "dismissed"); }
                @Override public void onAdFailedToShowFullScreenContent(AdError error) {
                    interstitialAds.remove(id);
                    environment.publish(getId(), "error", CreatorRuntimeServiceArguments.output(
                            "componentId", id, "action", "show", "message", error.getMessage()));
                }
            });
            interstitialAd.show(environment.getActivity());
            return CreatorRuntimeServiceArguments.succeeded("started", true, "action", action, "componentId", id,
                    "adUnitId", loadedUnitIds.get(id));
        }
        return CreatorRuntimeServiceArguments.invalid("Unsupported interstitial-ad action: " + action);
    }

    private void publish(String componentId, String event) {
        environment.publish(getId(), event, CreatorRuntimeServiceArguments.output(
                "componentId", componentId, "adUnitId", loadedUnitIds.get(componentId)));
    }
}
