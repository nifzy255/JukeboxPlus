package me.nifzy.jukeboxplus.client.music;

import java.nio.file.Path;

public record MusicTrack(
        String name,
        Path file,
        double durationSeconds
) {
}