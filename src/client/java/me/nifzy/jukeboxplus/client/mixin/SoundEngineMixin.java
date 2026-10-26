package me.nifzy.jukeboxplus.client.mixin;

import com.mojang.blaze3d.audio.Channel;
import me.nifzy.jukeboxplus.client.ChannelAccess;
import me.nifzy.jukeboxplus.client.SoundEngineAccess;
import me.nifzy.jukeboxplus.client.music.ExternalMusicSound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess.ChannelHandle;
import net.minecraft.client.sounds.SoundEngine;
import org.lwjgl.openal.AL10;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(SoundEngine.class)
public class SoundEngineMixin implements SoundEngineAccess {

    @Shadow
    private Map<SoundInstance, ChannelHandle> instanceToChannel;

    @Override
    public void jukebox$pause(ExternalMusicSound sound) {
        ChannelHandle handle = instanceToChannel.get(sound);

        if (handle != null) {
            handle.execute(channel -> channel.pause());
        }
    }

    @Override
    public void jukebox$resume(ExternalMusicSound sound) {
        ChannelHandle handle = instanceToChannel.get(sound);

        if (handle != null) {
            handle.execute(channel -> channel.unpause());
        }
    }

    @Override
    public void jukebox$seek(
            ExternalMusicSound sound,
            float seconds
    ) {
        ChannelHandle handle = instanceToChannel.get(sound);

        if (handle != null) {
            handle.execute(channel -> {

                int source =
                        ((ChannelAccess) channel).jukebox$getSource();

                AL10.alSourcef(
                        source,
                        0x1024,
                        Math.max(0.0F, seconds)
                );
            });
        }
    }

    @Override
    public void jukebox$setPitch(
            ExternalMusicSound sound,
            float pitch
    ) {
        ChannelHandle handle =
                instanceToChannel.get(sound);

        if (handle != null) {
            handle.execute(channel ->
                    channel.setPitch(pitch)
            );
        }
    }

    @Override
    public boolean jukebox$isSoundActive(
            ExternalMusicSound sound
    ) {
        return instanceToChannel.containsKey(sound);
    }
}