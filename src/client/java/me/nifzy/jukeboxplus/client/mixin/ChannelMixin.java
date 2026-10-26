package me.nifzy.jukeboxplus.client.mixin;

import com.mojang.blaze3d.audio.Channel;
import me.nifzy.jukeboxplus.client.ChannelAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Channel.class)
public class ChannelMixin implements ChannelAccess {

    @Shadow
    private int source;

    @Override
    public int jukebox$getSource() {
        return source;
    }
}