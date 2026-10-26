package me.nifzy.jukeboxplus.client.music;

import net.fabricmc.fabric.api.client.sound.v1.FabricSoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;

public class ExternalMusicSound extends AbstractSoundInstance
        implements FabricSoundInstance {

    private final MusicTrack track;

    private volatile double startPosition = 0.0;

    /*
     * Pitch and speed are intentionally independent.
     */
    private volatile float pitch = 1.0F;
    private volatile float speed = 1.0F;

    public ExternalMusicSound(MusicTrack track) {
        super(
                Identifier.fromNamespaceAndPath(
                        "jukebox",
                        "external_music"
                ),
                SoundSource.MUSIC,
                SoundInstance.createUnseededRandom()
        );

        this.track = track;

        this.volume = 1.0F;
        this.looping = false;
        this.delay = 0;
        this.relative = true;
    }

    public void setStartPosition(double seconds) {
        this.startPosition = Math.max(0.0, seconds);
    }

    public void setPitch(float pitch) {
        this.pitch = Math.max(
                0.1F,
                Math.min(pitch, 4.0F)
        );
    }

    public float getPitch() {
        return pitch;
    }

    public void setSpeed(float speed) {
        this.speed = Math.max(
                0.1F,
                Math.min(speed, 4.0F)
        );
    }

    public float getSpeed() {
        return speed;
    }

    /*
     * OpenAL pitch also changes playback rate.
     *
     * The custom stream compensates for that so:
     *
     * final speed = requested speed
     * final pitch = requested pitch
     */
    public double getResampleRatio() {
        return speed / pitch;
    }

    @Override
    public CompletableFuture<AudioStream> getAudioStream(
            SoundBufferLibrary library,
            Identifier id,
            boolean repeatInstantly
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                InputStream inputStream =
                        Files.newInputStream(track.file());

                JOrbisAudioStream stream =
                        new JOrbisAudioStream(inputStream);

                skipToPosition(
                        stream,
                        startPosition
                );

                return new IndependentAudioStream(
                        stream,
                        getResampleRatio()
                );

            } catch (IOException e) {
                throw new RuntimeException(
                        "Failed to open Jukebox+ track: "
                                + track.file(),
                        e
                );
            }
        });
    }

    private void skipToPosition(
            JOrbisAudioStream stream,
            double seconds
    ) throws IOException {

        if (seconds <= 0.0) {
            return;
        }

        int channels =
                stream.getFormat().getChannels();

        float sampleRate =
                stream.getFormat().getSampleRate();

        if (channels <= 0 || sampleRate <= 0) {
            return;
        }

        long targetFloatSamples =
                (long) (
                        seconds
                                * sampleRate
                                * channels
                );

        long[] decodedFloatSamples = {0};

        while (
                decodedFloatSamples[0]
                        < targetFloatSamples
        ) {
            boolean hasMore =
                    stream.readChunk(value ->
                            decodedFloatSamples[0]++
                    );

            if (!hasMore) {
                break;
            }
        }
    }

    public MusicTrack getTrack() {
        return track;
    }
}