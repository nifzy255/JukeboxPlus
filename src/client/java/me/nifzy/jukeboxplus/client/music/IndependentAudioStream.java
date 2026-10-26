package me.nifzy.jukeboxplus.client.music;

import it.unimi.dsi.fastutil.floats.FloatConsumer;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.FloatSampleSource;
import net.minecraft.client.sounds.JOrbisAudioStream;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Resamples a JOrbis audio stream while keeping the original
 * sample rate and pitch processing separate.
 *
 * The ratio determines how quickly the decoded audio is
 * consumed:
 *
 *   1.0 = normal
 *   2.0 = twice as fast
 *   0.5 = half speed
 */
public class IndependentAudioStream implements FloatSampleSource {

    private static final int OUTPUT_FRAMES_PER_CHUNK = 2048;

    private final JOrbisAudioStream source;
    private final AudioFormat format;
    private final int channels;
    private final double ratio;

    /*
     * Decoded PCM samples waiting to be resampled.
     *
     * Samples are interleaved:
     *
     * L R L R L R ...
     */
    private final List<Float> samples = new ArrayList<>();

    /*
     * Position inside the decoded source, measured in frames.
     *
     * This is a double so interpolation can move between
     * individual source samples smoothly.
     */
    private double sourceFramePosition = 0.0;

    private boolean sourceFinished = false;
    private boolean closed = false;

    public IndependentAudioStream(
            JOrbisAudioStream source,
            double ratio
    ) {
        this.source = source;
        this.format = source.getFormat();

        this.channels = format.getChannels();

        this.ratio = Math.max(
                0.01,
                ratio
        );

        if (channels <= 0) {
            throw new IllegalArgumentException(
                    "Audio stream has no channels."
            );
        }
    }

    @Override
    public AudioFormat getFormat() {
        return format;
    }

    @Override
    public boolean readChunk(FloatConsumer consumer)
            throws IOException {

        if (closed || sourceFinished) {
            return false;
        }

        int outputFrames = 0;

        while (outputFrames < OUTPUT_FRAMES_PER_CHUNK) {

            /*
             * We need the current frame and the next frame
             * for linear interpolation.
             */
            int requiredFrame =
                    (int) Math.floor(sourceFramePosition) + 2;

            if (!ensureFrames(requiredFrame)) {
                /*
                 * We have reached the end of the source.
                 *
                 * There may still be enough samples to output
                 * the final frame.
                 */
                int availableFrames =
                        samples.size() / channels;

                int currentFrame =
                        (int) Math.floor(sourceFramePosition);

                if (currentFrame >= availableFrames) {
                    sourceFinished = true;
                    break;
                }
            }

            int frameIndex =
                    (int) Math.floor(sourceFramePosition);

            double fraction =
                    sourceFramePosition - frameIndex;

            int frameOffset =
                    frameIndex * channels;

            int nextFrameOffset =
                    (frameIndex + 1) * channels;

            int availableFrames =
                    samples.size() / channels;

            if (frameIndex >= availableFrames) {
                sourceFinished = true;
                break;
            }

            /*
             * If the next frame doesn't exist, just use the
             * current frame. This handles the final sample.
             */
            boolean hasNextFrame =
                    frameIndex + 1 < availableFrames;

            for (int channel = 0; channel < channels; channel++) {

                float current =
                        samples.get(frameOffset + channel);

                float next =
                        hasNextFrame
                                ? samples.get(
                                nextFrameOffset + channel
                        )
                                : current;

                float interpolated =
                        (float) (
                                current
                                        + (
                                        next - current
                                ) * fraction
                        );

                consumer.accept(interpolated);
            }

            sourceFramePosition += ratio;

            outputFrames++;
        }

        return outputFrames > 0;
    }

    /**
     * Ensures that at least the requested number of frames
     * are decoded into the internal PCM buffer.
     */
    private boolean ensureFrames(int requiredFrames)
            throws IOException {

        while (
                samples.size() / channels
                        < requiredFrames
        ) {

            if (sourceFinished) {
                break;
            }

            List<Float> decoded =
                    new ArrayList<>();

            boolean hasMore =
                    source.readChunk(
                            decoded::add
                    );

            if (!decoded.isEmpty()) {
                samples.addAll(decoded);
            }

            if (!hasMore) {
                sourceFinished = true;
                break;
            }

            /*
             * Safety check in case the decoder reports that it
             * has more data but gives us no samples.
             */
            if (decoded.isEmpty()) {
                sourceFinished = true;
                break;
            }
        }

        return samples.size() / channels >= requiredFrames;
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }

        closed = true;
        samples.clear();

        source.close();
    }
}