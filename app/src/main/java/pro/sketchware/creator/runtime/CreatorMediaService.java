package pro.sketchware.creator.runtime;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;
import java.util.LinkedHashMap;
import java.util.Map;

/** Runtime-native MediaPlayer and SoundPool service. */
public final class CreatorMediaService implements CreatorRuntimeService {
    private final CreatorRuntimeEnvironment environment;
    private final Map<String, MediaPlayer> players = new LinkedHashMap<>();
    private final Map<String, Integer> sounds = new LinkedHashMap<>();
    private final Map<String, SoundPool> soundPools = new LinkedHashMap<>();

    public CreatorMediaService(CreatorRuntimeEnvironment environment) { this.environment = environment; }
    @Override public String getId() { return "media"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        String id = CreatorRuntimeServiceArguments.string(arguments, "id");
        if (action == null || id == null) return CreatorRuntimeServiceArguments.invalid("media requires action and id.");
        try {
            if ("load".equals(action)) {
                String source = CreatorRuntimeServiceArguments.string(arguments, "source");
                if (source == null) return CreatorRuntimeServiceArguments.invalid("load requires source URI.");
                releasePlayer(id);
                MediaPlayer player = MediaPlayer.create(environment.getContext(), Uri.parse(source));
                if (player == null) return CreatorRuntimeServiceArguments.failed("Media source could not be prepared.");
                player.setOnCompletionListener(complete -> environment.publish(getId(), "completed",
                        CreatorRuntimeServiceArguments.output("id", id)));
                players.put(id, player);
                return CreatorRuntimeServiceArguments.succeeded("id", id, "loaded", true);
            }
            if ("load_resource".equals(action)) {
                String resourceName = CreatorRuntimeServiceArguments.string(arguments, "resourceName");
                if (resourceName == null) return CreatorRuntimeServiceArguments.invalid("load_resource requires resourceName.");
                int resourceId = environment.getContext().getResources().getIdentifier(resourceName.toLowerCase(), "raw",
                        environment.getContext().getPackageName());
                if (resourceId == 0) return CreatorRuntimeServiceArguments.failed("Media resource was not found: " + resourceName);
                releasePlayer(id);
                MediaPlayer player = MediaPlayer.create(environment.getContext(), resourceId);
                if (player == null) return CreatorRuntimeServiceArguments.failed("Media resource could not be prepared.");
                player.setOnCompletionListener(complete -> environment.publish(getId(), "completed",
                        CreatorRuntimeServiceArguments.output("id", id)));
                players.put(id, player);
                return CreatorRuntimeServiceArguments.succeeded("id", id, "loaded", true);
            }
            if ("play".equals(action) || "pause".equals(action) || "stop".equals(action) || "release".equals(action)
                    || "seek".equals(action) || "set_looping".equals(action) || "current_position".equals(action)
                    || "duration".equals(action) || "is_playing".equals(action) || "is_looping".equals(action)) {
                MediaPlayer player = players.get(id);
                if (player == null) return CreatorRuntimeServiceArguments.invalid("No media source is loaded for " + id + ".");
                if ("play".equals(action)) player.start();
                else if ("pause".equals(action)) player.pause();
                else if ("stop".equals(action)) player.stop();
                else if ("seek".equals(action)) player.seekTo((int) CreatorRuntimeServiceArguments.longValue(arguments, "positionMs", 0L));
                else if ("set_looping".equals(action)) player.setLooping(Boolean.parseBoolean(
                        String.valueOf(arguments.get("looping"))));
                else releasePlayer(id);
                if ("current_position".equals(action)) return CreatorRuntimeServiceArguments.succeeded("id", id, "value", player.getCurrentPosition());
                if ("duration".equals(action)) return CreatorRuntimeServiceArguments.succeeded("id", id, "value", player.getDuration());
                if ("is_playing".equals(action)) return CreatorRuntimeServiceArguments.succeeded("id", id, "value", player.isPlaying());
                if ("is_looping".equals(action)) return CreatorRuntimeServiceArguments.succeeded("id", id, "value", player.isLooping());
                return CreatorRuntimeServiceArguments.succeeded("id", id, "action", action);
            }
            if ("sound_load_resource".equals(action)) {
                int resourceId = (int) CreatorRuntimeServiceArguments.longValue(arguments, "resourceId", 0L);
                if (resourceId == 0) return CreatorRuntimeServiceArguments.invalid("sound_load_resource requires resourceId.");
                sounds.put(id, soundPool(id).load(environment.getContext(), resourceId, 1));
                return CreatorRuntimeServiceArguments.succeeded("id", id, "loaded", true);
            }
            if ("sound_create".equals(action)) {
                int maxStreams = (int) CreatorRuntimeServiceArguments.longValue(arguments, "maxStreams", 8L);
                if (maxStreams < 1 || maxStreams > 64) return CreatorRuntimeServiceArguments.invalid("sound_create maxStreams must be between 1 and 64.");
                releaseSoundPool(id);
                soundPools.put(id, createSoundPool(maxStreams));
                return CreatorRuntimeServiceArguments.succeeded("id", id, "created", true, "maxStreams", maxStreams);
            }
            if ("sound_load_name".equals(action)) {
                String resourceName = CreatorRuntimeServiceArguments.string(arguments, "resourceName");
                if (resourceName == null) return CreatorRuntimeServiceArguments.invalid("sound_load_name requires resourceName.");
                int resourceId = environment.getContext().getResources().getIdentifier(resourceName.toLowerCase(), "raw",
                        environment.getContext().getPackageName());
                if (resourceId == 0) return CreatorRuntimeServiceArguments.failed("Sound resource was not found: " + resourceName);
                int soundId = soundPool(id).load(environment.getContext(), resourceId, 1);
                sounds.put(id, soundId);
                return CreatorRuntimeServiceArguments.succeeded("id", id, "resourceName", resourceName, "soundId", soundId, "loaded", true);
            }
            if ("sound_play".equals(action)) {
                Integer soundId = sounds.get(id);
                if (soundId == null) return CreatorRuntimeServiceArguments.invalid("No sound resource is loaded for " + id + ".");
                float volume = CreatorRuntimeServiceArguments.floatValue(arguments, "volume", 1f);
                int streamId = soundPool(id).play(soundId, volume, volume, 1, 0, 1f);
                return CreatorRuntimeServiceArguments.succeeded("id", id, "streamId", streamId, "played", true);
            }
            if ("sound_play_stream".equals(action)) {
                int soundId = (int) CreatorRuntimeServiceArguments.longValue(arguments, "soundId", sounds.containsKey(id) ? sounds.get(id) : 0L);
                if (soundId == 0) return CreatorRuntimeServiceArguments.invalid("sound_play_stream requires a loaded soundId.");
                int loop = (int) CreatorRuntimeServiceArguments.longValue(arguments, "loop", 0L);
                int streamId = soundPool(id).play(soundId, 1f, 1f, 1, loop, 1f);
                return CreatorRuntimeServiceArguments.succeeded("id", id, "soundId", soundId, "streamId", streamId, "played", true);
            }
            if ("sound_stop_stream".equals(action)) {
                int streamId = (int) CreatorRuntimeServiceArguments.longValue(arguments, "streamId", 0L);
                if (streamId == 0) return CreatorRuntimeServiceArguments.invalid("sound_stop_stream requires streamId.");
                soundPool(id).stop(streamId);
                return CreatorRuntimeServiceArguments.succeeded("id", id, "streamId", streamId, "stopped", true);
            }
            return CreatorRuntimeServiceArguments.invalid("Unsupported media action: " + action);
        } catch (RuntimeException error) {
            return CreatorRuntimeServiceArguments.failed(error.getMessage());
        }
    }

    private void releasePlayer(String id) {
        MediaPlayer player = players.remove(id);
        if (player != null) player.release();
    }

    private SoundPool soundPool(String id) {
        SoundPool pool = soundPools.get(id);
        if (pool == null) {
            pool = createSoundPool(8);
            soundPools.put(id, pool);
        }
        return pool;
    }

    private static SoundPool createSoundPool(int maxStreams) {
        return new SoundPool.Builder().setMaxStreams(maxStreams)
                .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
    }

    private void releaseSoundPool(String id) {
        SoundPool pool = soundPools.remove(id);
        if (pool != null) pool.release();
        sounds.remove(id);
    }
}
