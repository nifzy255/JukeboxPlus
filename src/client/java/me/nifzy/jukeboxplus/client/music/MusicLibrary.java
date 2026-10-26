package me.nifzy.jukeboxplus.client.music;

import me.nifzy.jukeboxplus.Jukebox;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.sounds.JOrbisAudioStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class MusicLibrary {

    private static final double MINIMUM_TRACK_DURATION = 5.0;

    private final Path musicDirectory;
    private final List<MusicTrack> tracks = new ArrayList<>();

    public MusicLibrary() {
        this.musicDirectory = FabricLoader.getInstance()
                .getGameDir()
                .resolve("jukebox");

        createDirectory();
        refresh();
    }

    private void createDirectory() {
        try {
            Files.createDirectories(musicDirectory);
        } catch (IOException e) {
            Jukebox.LOGGER.error(
                    "Failed to create Jukebox+ music directory: {}",
                    musicDirectory,
                    e
            );
        }
    }

    public void refresh() {
        tracks.clear();
        createDirectory();

        try (Stream<Path> files = Files.list(musicDirectory)) {
            files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName()
                            .toString()
                            .toLowerCase(Locale.ROOT)
                            .endsWith(".ogg"))
                    .sorted(Comparator.comparing(path ->
                            path.getFileName()
                                    .toString()
                                    .toLowerCase(Locale.ROOT)))
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();

                        String name = fileName.substring(
                                0,
                                fileName.length() - ".ogg".length()
                        );

                        double duration = getDuration(path);

                        if (duration < MINIMUM_TRACK_DURATION) {
                            Jukebox.LOGGER.info(
                                    "Ignoring track under 5 seconds: {} ({})",
                                    name,
                                    formatDuration(duration)
                            );
                            return;
                        }

                        tracks.add(new MusicTrack(
                                name,
                                path,
                                duration
                        ));

                        Jukebox.LOGGER.info(
                                "Loaded track: {} ({})",
                                name,
                                formatDuration(duration)
                        );
                    });

        } catch (IOException e) {
            Jukebox.LOGGER.error(
                    "Failed to scan Jukebox+ music directory",
                    e
            );
        }

        Jukebox.LOGGER.info(
                "Jukebox+ found {} music track(s).",
                tracks.size()
        );
    }

    private double getDuration(Path path) {
        try (InputStream input = Files.newInputStream(path);
             JOrbisAudioStream stream = new JOrbisAudioStream(input)) {

            int channels = stream.getFormat().getChannels();
            float sampleRate = stream.getFormat().getSampleRate();

            if (channels <= 0 || sampleRate <= 0) {
                Jukebox.LOGGER.warn(
                        "Invalid audio format for {}",
                        path
                );
                return 0.0;
            }

            long[] sampleCount = {0};

            while (stream.readChunk(value -> sampleCount[0]++)) {
                // Decode until the end of the OGG stream.
            }

            long samplesPerChannel = sampleCount[0] / channels;

            return samplesPerChannel / sampleRate;

        } catch (IOException e) {
            Jukebox.LOGGER.error(
                    "Failed to determine duration of Jukebox+ track: {}",
                    path,
                    e
            );

            return 0.0;
        }
    }

    private String formatDuration(double seconds) {
        int totalSeconds = (int) Math.round(seconds);

        int minutes = totalSeconds / 60;
        int remainingSeconds = totalSeconds % 60;

        return String.format(
                Locale.ROOT,
                "%d:%02d",
                minutes,
                remainingSeconds
        );
    }

    public Path getMusicDirectory() {
        return musicDirectory;
    }

    public List<MusicTrack> getTracks() {
        return List.copyOf(tracks);
    }
}