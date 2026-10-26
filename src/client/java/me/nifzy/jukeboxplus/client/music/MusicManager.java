package me.nifzy.jukeboxplus.client.music;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.nifzy.jukeboxplus.Jukebox;
import me.nifzy.jukeboxplus.client.SoundManagerAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

@Environment(EnvType.CLIENT)
public class MusicManager {

    private static final int SOUND_RECOVERY_GRACE_TICKS = 10;
    private static final double TRACK_END_TOLERANCE = 0.25;
    private static final long SAVE_INTERVAL_MS = 2000L;

    private final Minecraft minecraft;
    private final MusicLibrary library;
    private final Random random = new Random();

    private final Path configFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private ExternalMusicSound currentSound;
    private int currentTrackIndex = -1;

    private boolean paused = false;

    private long playbackStartTime = 0L;
    private double pausedPosition = 0.0;

    private float pitch = 1.0F;
    private float speed = 1.0F;

    private boolean shuffle = false;
    private RepeatMode repeatMode = RepeatMode.OFF;

    private int soundRecoveryGraceTicks = 0;

    private long lastSaveTime = 0L;

    private String savedTrackName;
    private double savedPosition = 0.0;
    private boolean savedPlaying = false;

    private static final float[] VALUES = {
            0.5F, 0.75F, 1.0F, 1.25F, 1.5F, 2.0F
    };

    private enum RepeatMode {
        OFF,
        TRACK,
        ALL
    }

    public MusicManager(MusicLibrary library) {
        this.minecraft = Minecraft.getInstance();
        this.library = library;

        this.configFile = FabricLoader.getInstance()
                .getGameDir()
                .resolve("jukebox")
                .resolve("config.json");

        loadState();

        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
    }

    private void tick() {
        if (currentSound == null || paused) {
            return;
        }

        if (soundRecoveryGraceTicks > 0) {
            soundRecoveryGraceTicks--;
            return;
        }

        double duration = currentSound.getTrack().durationSeconds();

        if (duration <= 0.0) {
            return;
        }

        double position = getPlaybackPosition();

        boolean soundActive = ((SoundManagerAccess) minecraft.getSoundManager())
                .jukebox$isSoundActive(currentSound);

        if (!soundActive) {
            if (position >= duration - TRACK_END_TOLERANCE) {
                handleTrackFinished();
            } else {
                recoverSound(position);
            }

            return;
        }

        if (position >= duration - 0.05) {
            handleTrackFinished();
        }
    }

    private void handleTrackFinished() {
        List<MusicTrack> tracks = library.getTracks();

        if (tracks.isEmpty()) {
            stop();
            return;
        }

        if (repeatMode == RepeatMode.TRACK) {
            MusicTrack track = currentSound.getTrack();
            play(track);
            return;
        }

        if (!shuffle && repeatMode == RepeatMode.OFF && currentTrackIndex >= tracks.size() - 1) {
            stop();
            return;
        }

        next();
    }

    private void recoverSound(double position) {
        if (currentSound == null || paused) {
            return;
        }

        MusicTrack track = currentSound.getTrack();

        currentSound = createSound(track, position);
        playbackStartTime = System.nanoTime() - (long) ((position / speed) * 1_000_000_000L);

        minecraft.getSoundManager().play(currentSound);
        applyPitch();

        soundRecoveryGraceTicks = SOUND_RECOVERY_GRACE_TICKS;

        Jukebox.LOGGER.info(
                "Recovered Jukebox+ audio: {} at {} seconds",
                track.name(),
                String.format("%.2f", position)
        );
    }

    public void play(MusicTrack track) {
        playFromPosition(track, 0.0);
    }

    private void playFromPosition(MusicTrack track, double position) {
        stop();

        List<MusicTrack> tracks = library.getTracks();
        currentTrackIndex = tracks.indexOf(track);

        minecraft.getMusicManager().stopPlaying();

        currentSound = createSound(track, position);
        paused = false;
        pausedPosition = position;

        playbackStartTime = System.nanoTime() - (long) ((position / speed) * 1_000_000_000L);

        minecraft.getSoundManager().play(currentSound);
        applyPitch();

        soundRecoveryGraceTicks = SOUND_RECOVERY_GRACE_TICKS;

        saveState();

        Jukebox.LOGGER.info(
                "Playing Jukebox+ track: {} from {} seconds | pitch={}x speed={}x",
                track.name(),
                String.format("%.2f", position),
                String.format("%.2f", pitch),
                String.format("%.2f", speed)
        );
    }

    private ExternalMusicSound createSound(MusicTrack track, double position) {
        ExternalMusicSound sound = new ExternalMusicSound(track);
        sound.setStartPosition(position);
        sound.setPitch(pitch);
        sound.setSpeed(speed);
        return sound;
    }

    public void next() {
        List<MusicTrack> tracks = library.getTracks();

        if (tracks.isEmpty()) {
            return;
        }

        if (shuffle) {
            currentTrackIndex = getRandomTrackIndex(tracks.size());
        } else {
            if (currentTrackIndex < 0) {
                currentTrackIndex = 0;
            } else {
                currentTrackIndex++;

                if (currentTrackIndex >= tracks.size()) {
                    currentTrackIndex = 0;
                }
            }
        }

        play(tracks.get(currentTrackIndex));
    }

    public void previous() {
        List<MusicTrack> tracks = library.getTracks();

        if (tracks.isEmpty()) {
            return;
        }

        if (shuffle) {
            currentTrackIndex = getRandomTrackIndex(tracks.size());
        } else {
            if (currentTrackIndex < 0) {
                currentTrackIndex = tracks.size() - 1;
            } else {
                currentTrackIndex--;

                if (currentTrackIndex < 0) {
                    currentTrackIndex = tracks.size() - 1;
                }
            }
        }

        play(tracks.get(currentTrackIndex));
    }

    private int getRandomTrackIndex(int size) {
        if (size <= 1) {
            return 0;
        }

        int newIndex;

        do {
            newIndex = random.nextInt(size);
        } while (newIndex == currentTrackIndex);

        return newIndex;
    }

    public void pause() {
        if (currentSound == null || paused) {
            return;
        }

        pausedPosition = getPlaybackPosition();

        minecraft.getSoundManager().stop(currentSound);
        paused = true;

        saveState();

        Jukebox.LOGGER.info(
                "Paused Jukebox+ track: {} at {} seconds",
                currentSound.getTrack().name(),
                String.format("%.2f", pausedPosition)
        );
    }

    public void resume() {
        if (currentSound == null || !paused) {
            return;
        }

        double position = pausedPosition;
        MusicTrack track = currentSound.getTrack();

        minecraft.getSoundManager().stop(currentSound);

        currentSound = createSound(track, position);
        paused = false;

        playbackStartTime = System.nanoTime() - (long) ((position / speed) * 1_000_000_000L);

        minecraft.getSoundManager().play(currentSound);
        applyPitch();

        soundRecoveryGraceTicks = SOUND_RECOVERY_GRACE_TICKS;

        saveState();

        Jukebox.LOGGER.info(
                "Resumed Jukebox+ track: {} at {} seconds | pitch={}x speed={}x",
                track.name(),
                String.format("%.2f", position),
                String.format("%.2f", pitch),
                String.format("%.2f", speed)
        );
    }

    public void togglePause() {
        if (paused) {
            resume();
        } else {
            pause();
        }
    }

    public void seek(double seconds) {
        if (currentSound == null) {
            return;
        }

        double duration = currentSound.getTrack().durationSeconds();

        if (duration <= 0.0) {
            return;
        }

        double target = Math.max(0.0, Math.min(seconds, duration));
        MusicTrack track = currentSound.getTrack();
        boolean wasPaused = paused;

        minecraft.getSoundManager().stop(currentSound);

        currentSound = createSound(track, target);
        pausedPosition = target;

        playbackStartTime = System.nanoTime() - (long) ((target / speed) * 1_000_000_000L);

        if (!wasPaused) {
            minecraft.getSoundManager().play(currentSound);
            applyPitch();

            soundRecoveryGraceTicks = SOUND_RECOVERY_GRACE_TICKS;
        }

        saveState();

        Jukebox.LOGGER.info(
                "Seeked Jukebox+ track {} to {} seconds | pitch={}x speed={}x",
                track.name(),
                String.format("%.2f", target),
                String.format("%.2f", pitch),
                String.format("%.2f", speed)
        );
    }

    public void stop() {
        if (currentSound != null) {
            minecraft.getSoundManager().stop(currentSound);

            Jukebox.LOGGER.info(
                    "Stopped Jukebox+ track: {}",
                    currentSound.getTrack().name()
            );

            currentSound = null;
            paused = false;
            playbackStartTime = 0L;
            pausedPosition = 0.0;
            soundRecoveryGraceTicks = 0;
        }

        saveState();
    }

    public boolean isPlaying() {
        return currentSound != null && !paused;
    }

    public boolean isPaused() {
        return currentSound != null && paused;
    }

    public MusicTrack getCurrentTrack() {
        if (currentSound == null) {
            return null;
        }

        return currentSound.getTrack();
    }

    public double getPlaybackPosition() {
        if (currentSound == null) {
            return 0.0;
        }

        if (paused) {
            return pausedPosition;
        }

        double elapsedSeconds = (System.nanoTime() - playbackStartTime) / 1_000_000_000.0;
        double playbackPosition = elapsedSeconds * speed;
        double duration = currentSound.getTrack().durationSeconds();

        if (duration > 0.0) {
            return Math.min(Math.max(0.0, playbackPosition), duration);
        }

        return Math.max(0.0, playbackPosition);
    }

    public float getPitch() {
        return pitch;
    }

    public void cyclePitch() {
        pitch = getNextValue(pitch);

        recreateCurrentSound();
        saveState();

        Jukebox.LOGGER.info(
                "Jukebox+ pitch changed to {}x",
                String.format("%.2f", pitch)
        );
    }

    public float getSpeed() {
        return speed;
    }

    public void cycleSpeed() {
        speed = getNextValue(speed);

        recreateCurrentSound();
        saveState();

        Jukebox.LOGGER.info(
                "Jukebox+ speed changed to {}x",
                String.format("%.2f", speed)
        );
    }

    private float getNextValue(float current) {
        int currentIndex = 0;

        for (int i = 0; i < VALUES.length; i++) {
            if (Math.abs(VALUES[i] - current) < 0.001F) {
                currentIndex = i;
                break;
            }
        }

        return VALUES[(currentIndex + 1) % VALUES.length];
    }

    private void recreateCurrentSound() {
        if (currentSound == null) {
            return;
        }

        double position = getPlaybackPosition();
        MusicTrack track = currentSound.getTrack();
        boolean wasPaused = paused;

        minecraft.getSoundManager().stop(currentSound);

        currentSound = createSound(track, position);
        pausedPosition = position;

        playbackStartTime = System.nanoTime() - (long) ((position / speed) * 1_000_000_000L);

        if (!wasPaused) {
            minecraft.getSoundManager().play(currentSound);
            applyPitch();

            soundRecoveryGraceTicks = SOUND_RECOVERY_GRACE_TICKS;
        }
    }

    private void applyPitch() {
        if (currentSound == null || paused) {
            return;
        }

        ((SoundManagerAccess) minecraft.getSoundManager()).jukebox$setPitch(currentSound, pitch);
    }

    public boolean isShuffleEnabled() {
        return shuffle;
    }

    public void toggleShuffle() {
        shuffle = !shuffle;

        saveState();

        Jukebox.LOGGER.info(
                "Jukebox+ shuffle: {}",
                shuffle ? "ON" : "OFF"
        );
    }

    public String getRepeatMode() {
        return switch (repeatMode) {
            case OFF -> "OFF";
            case TRACK -> "TRACK";
            case ALL -> "ALL";
        };
    }

    public void cycleRepeat() {
        repeatMode = switch (repeatMode) {
            case OFF -> RepeatMode.TRACK;
            case TRACK -> RepeatMode.ALL;
            case ALL -> RepeatMode.OFF;
        };

        saveState();

        Jukebox.LOGGER.info(
                "Jukebox+ repeat: {}",
                getRepeatMode()
        );
    }

    private void loadState() {
        if (!Files.exists(configFile)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(configFile)) {
            JukeboxState state = gson.fromJson(reader, JukeboxState.class);

            if (state == null) {
                return;
            }

            pitch = clampValue(state.pitch, 0.1F, 4.0F);
            speed = clampValue(state.speed, 0.1F, 4.0F);
            shuffle = state.shuffle;

            try {
                repeatMode = RepeatMode.valueOf(state.repeatMode);
            } catch (Exception ignored) {
                repeatMode = RepeatMode.OFF;
            }

            savedTrackName = state.trackName;
            savedPosition = Math.max(0.0, state.position);
            savedPlaying = state.playing;
        } catch (Exception e) {
            Jukebox.LOGGER.error("Failed to load Jukebox+ config", e);
        }
    }

    private void saveState() {
        try {
            Files.createDirectories(configFile.getParent());

            JukeboxState state = new JukeboxState();

            if (currentSound != null) {
                state.trackName = currentSound.getTrack().name();
                state.position = getPlaybackPosition();
                state.playing = !paused;
            } else {
                state.trackName = savedTrackName;
                state.position = savedPosition;
                state.playing = savedPlaying;
            }

            state.pitch = pitch;
            state.speed = speed;
            state.shuffle = shuffle;
            state.repeatMode = repeatMode.name();

            try (Writer writer = Files.newBufferedWriter(configFile)) {
                gson.toJson(state, writer);
            }

            lastSaveTime = System.currentTimeMillis();
        } catch (IOException e) {
            Jukebox.LOGGER.error("Failed to save Jukebox+ config", e);
        }
    }

    private float clampValue(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static class JukeboxState {
        String trackName;
        double position;
        boolean playing;
        float pitch = 1.0F;
        float speed = 1.0F;
        boolean shuffle = false;
        String repeatMode = "OFF";
    }
}