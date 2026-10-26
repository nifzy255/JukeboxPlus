package me.nifzy.jukeboxplus.client.gui;

import me.nifzy.jukeboxplus.client.JukeboxClient;
import me.nifzy.jukeboxplus.client.music.MusicLibrary;
import me.nifzy.jukeboxplus.client.music.MusicManager;
import me.nifzy.jukeboxplus.client.music.MusicTrack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

public class PlaylistScreen extends Screen {

    private final Screen parent;

    private int scrollOffset = 0;

    public PlaylistScreen(Screen parent) {
        super(Component.literal("Jukebox+ Playlist"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        scrollOffset = 0;
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

        graphics.fill(
                0,
                0,
                this.width,
                this.height,
                0xAA000000
        );

        graphics.fill(
                left,
                top,
                right,
                bottom,
                0xFF18181E
        );

        graphics.fill(
                left,
                top,
                right,
                top + 38,
                0xFF202027
        );

        graphics.text(
                this.font,
                "←",
                left + 12,
                top + 12,
                0xFFD8D3C8,
                false
        );

        graphics.text(
                this.font,
                "PLAYLIST",
                left + 38,
                top + 13,
                0xFFF0EBDD,
                false
        );

        MusicLibrary library = JukeboxClient.MUSIC_LIBRARY;

        if (library == null) {
            return;
        }

        List<MusicTrack> tracks = library.getTracks();

        if (tracks.isEmpty()) {
            String text = "No .ogg files found";

            graphics.text(
                    this.font,
                    text,
                    this.width / 2 - this.font.width(text) / 2,
                    top + 100,
                    0xFF96939A,
                    false
            );

            return;
        }

        int rowTop = top + 48;
        int rowHeight = 32;
        int visibleTracks = 7;

        int maxScroll = Math.max(0, tracks.size() - visibleTracks);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        for (int i = 0; i < visibleTracks; i++) {
            int trackIndex = scrollOffset + i;

            if (trackIndex >= tracks.size()) {
                break;
            }

            MusicTrack track = tracks.get(trackIndex);

            int currentTop = rowTop + i * rowHeight;
            int currentBottom = currentTop + rowHeight - 2;

            boolean hovered =
                    mouseX >= left + 10
                            && mouseX <= right - 10
                            && mouseY >= currentTop
                            && mouseY <= currentBottom;

            boolean playing =
                    JukeboxClient.MUSIC_MANAGER != null
                            && track.equals(
                            JukeboxClient.MUSIC_MANAGER.getCurrentTrack()
                    );

            if (hovered) {
                graphics.fill(
                        left + 10,
                        currentTop,
                        right - 10,
                        currentBottom,
                        0xFF292932
                );
            }

            String indicator = playing ? "▶" : "♪";

            graphics.text(
                    this.font,
                    indicator,
                    left + 18,
                    currentTop + 9,
                    playing
                            ? 0xFFE7DFCF
                            : 0xFF96939A,
                    false
            );

            graphics.text(
                    this.font,
                    track.name(),
                    left + 42,
                    currentTop + 9,
                    0xFFF0EBDD,
                    false
            );
        }

        String count = tracks.size() + " track"
                + (tracks.size() == 1 ? "" : "s");

        graphics.text(
                this.font,
                count,
                left + 14,
                bottom - 17,
                0xFF77747C,
                false
        );

        if (tracks.size() > visibleTracks) {
            String scrollText =
                    (scrollOffset + 1)
                            + "-"
                            + Math.min(
                            scrollOffset + visibleTracks,
                            tracks.size()
                    )
                            + " / "
                            + tracks.size();

            graphics.text(
                    this.font,
                    scrollText,
                    right - 14 - this.font.width(scrollText),
                    bottom - 17,
                    0xFF77747C,
                    false
            );
        }
    }

    @Override
    public boolean mouseClicked(
            MouseButtonEvent event,
            boolean doubleClick
    ) {
        double mouseX = event.x();
        double mouseY = event.y();

        int panelWidth = 420;
        int panelHeight = 300;

        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;

        if (mouseX >= left
                && mouseX <= left + 30
                && mouseY >= top
                && mouseY <= top + 38) {

            this.minecraft.gui.setScreen(parent);

            return true;
        }

        MusicLibrary library = JukeboxClient.MUSIC_LIBRARY;

        if (library == null) {
            return super.mouseClicked(event, doubleClick);
        }

        List<MusicTrack> tracks = library.getTracks();

        int rowTop = top + 48;
        int rowHeight = 32;
        int visibleTracks = 7;

        int maxScroll = Math.max(0, tracks.size() - visibleTracks);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        for (int i = 0; i < visibleTracks; i++) {
            int trackIndex = scrollOffset + i;

            if (trackIndex >= tracks.size()) {
                break;
            }

            int currentTop = rowTop + i * rowHeight;
            int currentBottom = currentTop + rowHeight - 2;

            if (mouseX >= left + 10
                    && mouseX <= left + panelWidth - 10
                    && mouseY >= currentTop
                    && mouseY <= currentBottom) {

                MusicTrack track = tracks.get(trackIndex);

                MusicManager manager = JukeboxClient.MUSIC_MANAGER;

                if (manager != null) {
                    manager.play(track);
                }

                this.minecraft.gui.setScreen(parent);

                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {
        MusicLibrary library = JukeboxClient.MUSIC_LIBRARY;

        if (library == null) {
            return super.mouseScrolled(
                    mouseX,
                    mouseY,
                    horizontalAmount,
                    verticalAmount
            );
        }

        List<MusicTrack> tracks = library.getTracks();

        int visibleTracks = 7;
        int maxScroll = Math.max(0, tracks.size() - visibleTracks);

        if (maxScroll > 0) {
            if (verticalAmount < 0) {
                scrollOffset++;
            } else if (verticalAmount > 0) {
                scrollOffset--;
            }

            scrollOffset = Math.max(
                    0,
                    Math.min(scrollOffset, maxScroll)
            );

            return true;
        }

        return super.mouseScrolled(
                mouseX,
                mouseY,
                horizontalAmount,
                verticalAmount
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}