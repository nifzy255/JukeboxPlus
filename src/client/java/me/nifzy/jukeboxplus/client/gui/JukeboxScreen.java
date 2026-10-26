package me.nifzy.jukeboxplus.client.gui;

import me.nifzy.jukeboxplus.client.JukeboxClient;
import me.nifzy.jukeboxplus.client.music.MusicTrack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class JukeboxScreen extends Screen {

    public JukeboxScreen() {
        super(Component.literal("Jukebox+"));
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        int panelWidth = 420;
        int panelHeight = 300;

        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        int right = left + panelWidth;
        int bottom = top + panelHeight;

        // Main panel
        graphics.fill(
                left,
                top,
                right,
                bottom,
                0xFF18181E
        );

        // Header
        graphics.fill(
                left,
                top,
                right,
                top + 38,
                0xFF202027
        );

        // Title
        graphics.text(
                this.font,
                "♪  JUKEBOX+",
                left + 14,
                top + 13,
                0xFFF0EBDD,
                false
        );

        // Close button
        graphics.text(
                this.font,
                "×",
                right - 18,
                top + 13,
                0xFFB8B5AE,
                false
        );

        // Album art
        int artSize = 120;

        int artLeft =
                left + (panelWidth - artSize) / 2;

        int artTop =
                top + 58;

        graphics.fill(
                artLeft,
                artTop,
                artLeft + artSize,
                artTop + artSize,
                0xFF292932
        );

        graphics.text(
                this.font,
                "♪",
                artLeft + 56,
                artTop + 51,
                0xFFE7DFCF,
                false
        );

        // Current track
        MusicTrack currentTrack = null;

        if (JukeboxClient.MUSIC_MANAGER != null) {
            currentTrack =
                    JukeboxClient.MUSIC_MANAGER
                            .getCurrentTrack();
        }

        String trackName =
                currentTrack != null
                        ? currentTrack.name()
                        : "Nothing Playing";

        int trackWidth =
                this.font.width(trackName);

        graphics.text(
                this.font,
                trackName,
                this.width / 2 - trackWidth / 2,
                top + 192,
                0xFFF0EBDD,
                false
        );

        // Subtitle
        String subtitle = "Jukebox+";

        int subtitleWidth =
                this.font.width(subtitle);

        graphics.text(
                this.font,
                subtitle,
                this.width / 2 - subtitleWidth / 2,
                top + 207,
                0xFF96939A,
                false
        );

        // Progress bar
        int barLeft = left + 35;
        int barRight = right - 35;
        int barWidth = barRight - barLeft;
        int barY = top + 232;

        graphics.fill(
                barLeft,
                barY,
                barRight,
                barY + 3,
                0xFF383840
        );

        double currentPosition = 0.0;
        double durationSeconds = 0.0;

        if (
                currentTrack != null
                        && JukeboxClient.MUSIC_MANAGER != null
        ) {
            currentPosition =
                    JukeboxClient.MUSIC_MANAGER
                            .getPlaybackPosition();

            durationSeconds =
                    currentTrack.durationSeconds();
        }

        double progress = 0.0;

        if (durationSeconds > 0.0) {
            progress =
                    currentPosition
                            / durationSeconds;

            progress =
                    Math.max(
                            0.0,
                            Math.min(1.0, progress)
                    );
        }

        int progressWidth =
                (int) Math.round(
                        barWidth * progress
                );

        if (progressWidth > 0) {
            graphics.fill(
                    barLeft,
                    barY,
                    barLeft + progressWidth,
                    barY + 3,
                    0xFFE7DFCF
            );
        }

        // Progress knob
        if (currentTrack != null) {

            int knobX =
                    barLeft + progressWidth;

            graphics.fill(
                    knobX - 3,
                    barY - 2,
                    knobX + 3,
                    barY + 5,
                    0xFFE7DFCF
            );
        }

        // Current time
        String currentTime =
                formatDuration(currentPosition);

        graphics.text(
                this.font,
                currentTime,
                barLeft,
                barY + 8,
                0xFF96939A,
                false
        );

        // Total duration
        String duration =
                currentTrack != null
                        ? formatDuration(durationSeconds)
                        : "0:00";

        graphics.text(
                this.font,
                duration,
                barRight
                        - this.font.width(duration),
                barY + 8,
                0xFF96939A,
                false
        );

        // --------------------------------------------------
        // Transport controls
        // --------------------------------------------------

        int transportY = top + 252;

        // Previous
        drawCenteredText(
                graphics,
                "◀",
                this.width / 2 - 55,
                transportY,
                0xFFD8D3C8
        );

        // Play / Pause
        String playPauseIcon = "▶";

        if (
                JukeboxClient.MUSIC_MANAGER != null
                        && JukeboxClient.MUSIC_MANAGER
                        .isPlaying()
        ) {
            playPauseIcon = "⏸";
        }

        drawCenteredText(
                graphics,
                playPauseIcon,
                this.width / 2,
                transportY - 3,
                0xFFF0EBDD
        );

        // Next
        drawCenteredText(
                graphics,
                "▶",
                this.width / 2 + 55,
                transportY,
                0xFFD8D3C8
        );

        // --------------------------------------------------
        // Bottom controls
        //
        // [PITCH] [SPEED] [SHUFFLE] [REPEAT] [PLAYLIST]
        // --------------------------------------------------

        int buttonHeight = 20;
        int buttonY = bottom - 30;

        int buttonWidth = 68;
        int gap = 5;

        int totalWidth =
                buttonWidth * 5
                        + gap * 4;

        int buttonsLeft =
                left
                        + (panelWidth - totalWidth)
                        / 2;

        int pitchLeft =
                buttonsLeft;

        int speedLeft =
                pitchLeft
                        + buttonWidth
                        + gap;

        int shuffleLeft =
                speedLeft
                        + buttonWidth
                        + gap;

        int repeatLeft =
                shuffleLeft
                        + buttonWidth
                        + gap;

        int playlistLeft =
                repeatLeft
                        + buttonWidth
                        + gap;

        // Get manager once
        var manager =
                JukeboxClient.MUSIC_MANAGER;

        // -----------------------------------------
        // Pitch
        // -----------------------------------------

        graphics.fill(
                pitchLeft,
                buttonY,
                pitchLeft + buttonWidth,
                buttonY + buttonHeight,
                0xFF292932
        );

        String pitchText =
                "PITCH "
                        + String.format(
                        "%.2f",
                        manager != null
                                ? manager.getPitch()
                                : 1.0F
                )
                        + "×";

        drawButtonText(
                graphics,
                pitchText,
                pitchLeft,
                buttonY,
                buttonWidth,
                buttonHeight
        );

        // -----------------------------------------
        // Speed
        // -----------------------------------------

        graphics.fill(
                speedLeft,
                buttonY,
                speedLeft + buttonWidth,
                buttonY + buttonHeight,
                0xFF292932
        );

        String speedText =
                "SPEED "
                        + String.format(
                        "%.2f",
                        manager != null
                                ? manager.getSpeed()
                                : 1.0F
                )
                        + "×";

        drawButtonText(
                graphics,
                speedText,
                speedLeft,
                buttonY,
                buttonWidth,
                buttonHeight
        );

        // -----------------------------------------
        // Shuffle
        // -----------------------------------------

        graphics.fill(
                shuffleLeft,
                buttonY,
                shuffleLeft + buttonWidth,
                buttonY + buttonHeight,
                0xFF292932
        );

        String shuffleText =
                manager != null
                        && manager.isShuffleEnabled()
                        ? "SHUFFLE ON"
                        : "SHUFFLE OFF";

        drawButtonText(
                graphics,
                shuffleText,
                shuffleLeft,
                buttonY,
                buttonWidth,
                buttonHeight
        );

        // -----------------------------------------
        // Repeat
        // -----------------------------------------

        graphics.fill(
                repeatLeft,
                buttonY,
                repeatLeft + buttonWidth,
                buttonY + buttonHeight,
                0xFF292932
        );

        String repeatText =
                "REPEAT "
                        + (
                        manager != null
                                ? manager.getRepeatMode()
                                : "OFF"
                );

        drawButtonText(
                graphics,
                repeatText,
                repeatLeft,
                buttonY,
                buttonWidth,
                buttonHeight
        );

        // -----------------------------------------
        // Playlist
        // -----------------------------------------

        graphics.fill(
                playlistLeft,
                buttonY,
                playlistLeft + buttonWidth,
                buttonY + buttonHeight,
                0xFF292932
        );

        drawButtonText(
                graphics,
                "PLAYLIST",
                playlistLeft,
                buttonY,
                buttonWidth,
                buttonHeight
        );
    }

    private void drawButtonText(
            GuiGraphicsExtractor graphics,
            String text,
            int left,
            int top,
            int width,
            int height
    ) {
        int textWidth =
                this.font.width(text);

        graphics.text(
                this.font,
                text,
                left
                        + (width - textWidth) / 2,
                top + 6,
                0xFFE7DFCF,
                false
        );
    }

    private String formatDuration(
            double seconds
    ) {
        int totalSeconds =
                (int) Math.max(
                        0,
                        Math.round(seconds)
                );

        int minutes =
                totalSeconds / 60;

        int remainingSeconds =
                totalSeconds % 60;

        return String.format(
                "%d:%02d",
                minutes,
                remainingSeconds
        );
    }

    private void drawCenteredText(
            GuiGraphicsExtractor graphics,
            String text,
            int centerX,
            int y,
            int color
    ) {
        int width =
                this.font.width(text);

        graphics.text(
                this.font,
                text,
                centerX - width / 2,
                y,
                color,
                false
        );
    }

    @Override
    public boolean mouseClicked(
            MouseButtonEvent event,
            boolean doubleClick
    ) {
        int panelWidth = 420;
        int panelHeight = 300;

        int left =
                (this.width - panelWidth) / 2;

        int top =
                (this.height - panelHeight) / 2;

        int right =
                left + panelWidth;

        int bottom =
                top + panelHeight;

        // -----------------------------------------
        // Progress bar
        // -----------------------------------------

        int barLeft = left + 35;
        int barRight = right - 35;
        int barY = top + 232;

        if (
                event.x() >= barLeft
                        && event.x() <= barRight
                        && event.y() >= barY - 6
                        && event.y() <= barY + 9
        ) {
            if (
                    JukeboxClient.MUSIC_MANAGER
                            != null
            ) {
                MusicTrack currentTrack =
                        JukeboxClient.MUSIC_MANAGER
                                .getCurrentTrack();

                if (currentTrack != null) {

                    double duration =
                            currentTrack
                                    .durationSeconds();

                    if (duration > 0.0) {

                        double progress =
                                (
                                        event.x()
                                                - barLeft
                                )
                                        / (double)
                                        (
                                                barRight
                                                        - barLeft
                                        );

                        progress =
                                Math.max(
                                        0.0,
                                        Math.min(
                                                1.0,
                                                progress
                                        )
                                );

                        double target =
                                duration
                                        * progress;

                        JukeboxClient.MUSIC_MANAGER
                                .seek(target);
                    }
                }
            }

            return true;
        }

        // -----------------------------------------
        // Transport buttons
        // -----------------------------------------

        int transportY = top + 252;

        // Previous
        if (
                distanceSquared(
                        event.x(),
                        event.y(),
                        this.width / 2 - 55,
                        transportY
                ) <= 18 * 18
        ) {
            if (
                    JukeboxClient.MUSIC_MANAGER
                            != null
            ) {
                JukeboxClient.MUSIC_MANAGER
                        .previous();
            }

            return true;
        }

        // Play / Pause
        if (
                distanceSquared(
                        event.x(),
                        event.y(),
                        this.width / 2,
                        transportY - 3
                ) <= 20 * 20
        ) {
            if (
                    JukeboxClient.MUSIC_MANAGER
                            != null
            ) {
                JukeboxClient.MUSIC_MANAGER
                        .togglePause();
            }

            return true;
        }

        // Next
        if (
                distanceSquared(
                        event.x(),
                        event.y(),
                        this.width / 2 + 55,
                        transportY
                ) <= 18 * 18
        ) {
            if (
                    JukeboxClient.MUSIC_MANAGER
                            != null
            ) {
                JukeboxClient.MUSIC_MANAGER
                        .next();
            }

            return true;
        }

        // -----------------------------------------
        // Bottom controls
        // -----------------------------------------

        int buttonHeight = 20;
        int buttonY = bottom - 30;

        int buttonWidth = 68;
        int gap = 5;

        int totalWidth =
                buttonWidth * 5
                        + gap * 4;

        int buttonsLeft =
                left
                        + (panelWidth - totalWidth)
                        / 2;

        int pitchLeft =
                buttonsLeft;

        int speedLeft =
                pitchLeft
                        + buttonWidth
                        + gap;

        int shuffleLeft =
                speedLeft
                        + buttonWidth
                        + gap;

        int repeatLeft =
                shuffleLeft
                        + buttonWidth
                        + gap;

        int playlistLeft =
                repeatLeft
                        + buttonWidth
                        + gap;

        // -----------------------------------------
        // Pitch
        // -----------------------------------------

        if (
                isInside(
                        event.x(),
                        event.y(),
                        pitchLeft,
                        buttonY,
                        pitchLeft + buttonWidth,
                        buttonY + buttonHeight
                )
        ) {
            if (
                    JukeboxClient.MUSIC_MANAGER
                            != null
            ) {
                JukeboxClient.MUSIC_MANAGER
                        .cyclePitch();
            }

            return true;
        }

        // -----------------------------------------
        // Speed
        // -----------------------------------------

        if (
                isInside(
                        event.x(),
                        event.y(),
                        speedLeft,
                        buttonY,
                        speedLeft + buttonWidth,
                        buttonY + buttonHeight
                )
        ) {
            if (
                    JukeboxClient.MUSIC_MANAGER
                            != null
            ) {
                JukeboxClient.MUSIC_MANAGER
                        .cycleSpeed();
            }

            return true;
        }

        // -----------------------------------------
        // Shuffle
        // -----------------------------------------

        if (
                isInside(
                        event.x(),
                        event.y(),
                        shuffleLeft,
                        buttonY,
                        shuffleLeft + buttonWidth,
                        buttonY + buttonHeight
                )
        ) {
            if (
                    JukeboxClient.MUSIC_MANAGER
                            != null
            ) {
                JukeboxClient.MUSIC_MANAGER
                        .toggleShuffle();
            }

            return true;
        }

        // -----------------------------------------
        // Repeat
        // -----------------------------------------

        if (
                isInside(
                        event.x(),
                        event.y(),
                        repeatLeft,
                        buttonY,
                        repeatLeft + buttonWidth,
                        buttonY + buttonHeight
                )
        ) {
            if (
                    JukeboxClient.MUSIC_MANAGER
                            != null
            ) {
                JukeboxClient.MUSIC_MANAGER
                        .cycleRepeat();
            }

            return true;
        }

        // -----------------------------------------
        // Playlist
        // -----------------------------------------

        if (
                isInside(
                        event.x(),
                        event.y(),
                        playlistLeft,
                        buttonY,
                        playlistLeft + buttonWidth,
                        buttonY + buttonHeight
                )
        ) {
            this.minecraft.gui.setScreen(
                    new PlaylistScreen(this)
            );

            return true;
        }

        // -----------------------------------------
        // Close
        // -----------------------------------------

        if (
                event.x() >= right - 30
                        && event.x() <= right
                        && event.y() >= top
                        && event.y() <= top + 38
        ) {
            this.onClose();
            return true;
        }

        return super.mouseClicked(
                event,
                doubleClick
        );
    }

    private boolean isInside(
            double x,
            double y,
            double left,
            double top,
            double right,
            double bottom
    ) {
        return x >= left
                && x <= right
                && y >= top
                && y <= bottom;
    }

    private int distanceSquared(
            double x1,
            double y1,
            double x2,
            double y2
    ) {
        double dx = x1 - x2;
        double dy = y1 - y2;

        return (int) (
                dx * dx + dy * dy
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}