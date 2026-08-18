package pro.sketchware.creator.runtime;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import java.util.Map;

/** Runtime-native rewarded-ad service with explicit reward event publishing. */
public final class CreatorRewardedAdService implements CreatorRuntimeService {
    private final CreatorRuntimeEnvironment environment;
    private RewardedAd rewardedAd;
    private String loadedUnitId;

    public CreatorRewardedAdService(CreatorRuntimeEnvironment environment) { this.environment = environment; }
    @Override public String getId() { return "ads_rewarded"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        if ("load".equals(action)) {
            String adUnitId = CreatorRuntimeServiceArguments.string(arguments, "adUnitId");
            if (adUnitId == null) return CreatorRuntimeServiceArguments.invalid("load requires configured adUnitId.");
            RewardedAd.load(environment.getActivity(), adUnitId, new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
                @Override public void onAdLoaded(RewardedAd ad) {
                    rewardedAd = ad;
                    loadedUnitId = adUnitId;
                    environment.publish(getId(), "loaded", CreatorRuntimeServiceArguments.output("adUnitId", adUnitId));
                }
                @Override public void onAdFailedToLoad(LoadAdError error) {
                    rewardedAd = null;
                    environment.publish(getId(), "error", CreatorRuntimeServiceArguments.output(
                            "action", "load", "message", error.getMessage()));
                }
            });
            return CreatorRuntimeServiceArguments.succeeded("started", true, "action", action);
        }
        if ("show".equals(action)) {
            if (rewardedAd == null) return CreatorRuntimeServiceArguments.failed("No loaded rewarded ad is available.");
            rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override public void onAdShowedFullScreenContent() { publish("shown"); }
                @Override public void onAdDismissedFullScreenContent() { rewardedAd = null; publish("dismissed"); }
                @Override public void onAdFailedToShowFullScreenContent(AdError error) {
                    rewardedAd = null;
                    environment.publish(getId(), "error", CreatorRuntimeServiceArguments.output(
                            "action", "show", "message", error.getMessage()));
                }
            });
            rewardedAd.show(environment.getActivity(), new OnUserEarnedRewardListener() {
                @Override public void onUserEarnedReward(RewardItem item) {
                    environment.publish(getId(), "reward", CreatorRuntimeServiceArguments.output(
                            "type", item.getType(), "amount", item.getAmount(), "adUnitId", loadedUnitId));
                }
            });
            return CreatorRuntimeServiceArguments.succeeded("started", true, "action", action, "adUnitId", loadedUnitId);
        }
        return CreatorRuntimeServiceArguments.invalid("Unsupported rewarded-ad action: " + action);
    }
    private void publish(String event) {
        environment.publish(getId(), event, CreatorRuntimeServiceArguments.output("adUnitId", loadedUnitId));
    }
}
