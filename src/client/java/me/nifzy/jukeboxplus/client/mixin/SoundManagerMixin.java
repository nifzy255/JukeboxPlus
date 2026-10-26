package me.nifzy.jukeboxplus.client.mixin;

import me.nifzy.jukeboxplus.client.SoundEngineAccess;
import me.nifzy.jukeboxplus.client.SoundManagerAccess;
import me.nifzy.jukeboxplus.client.music.ExternalMusicSound;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SoundManager.class)
public class SoundManagerMixin implements SoundManagerAccess {

    @Shadow
    private SoundEngine soundEngine;

    @Override
    public void jukebox$pause(
            ExternalMusicSound sound
    ) {
        ((SoundEngineAccess) soundEngine)
                .jukebox$pause(sound);
    }

    @Override
    public void jukebox$resume(
            ExternalMusicSound sound
    ) {
        ((SoundEngineAccess) soundEngine)
                .jukebox$resume(sound);
    }

    @Override
    public void jukebox$seek(
            ExternalMusicSound sound,
            float seconds
    ) {
        ((SoundEngineAccess) soundEngine)
                .jukebox$seek(sound, seconds);
    }

    @Override
    public void jukebox$setPitch(
            ExternalMusicSound sound,
            float pitch
    ) {
        ((SoundEngineAccess) soundEngine)
                .jukebox$setPitch(sound, pitch);
    }

    @Override
    public boolean jukebox$isSoundActive(
            ExternalMusicSound sound
    ) {
        return ((SoundEngineAccess) soundEngine)
                .jukebox$isSoundActive(sound);
    }
}