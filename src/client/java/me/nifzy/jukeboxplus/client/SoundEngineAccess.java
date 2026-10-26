package me.nifzy.jukeboxplus.client;

import me.nifzy.jukeboxplus.client.music.ExternalMusicSound;

public interface SoundEngineAccess {

    void jukebox$pause(
            ExternalMusicSound sound
    );

    void jukebox$resume(
            ExternalMusicSound sound
    );

    void jukebox$seek(
            ExternalMusicSound sound,
            float seconds
    );

    void jukebox$setPitch(
            ExternalMusicSound sound,
            float pitch
    );

    boolean jukebox$isSoundActive(
            ExternalMusicSound sound
    );
}