package me.nifzy.jukeboxplus.client.mixin;

import me.nifzy.jukeboxplus.client.JukeboxClient;
import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MusicManager.class)
public class MusicManagerMixin {

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void jukebox$blockVanillaMusic(CallbackInfo ci) {
		if (JukeboxClient.MUSIC_MANAGER != null
				&& JukeboxClient.MUSIC_MANAGER.isPlaying()) {
			ci.cancel();
		}
	}
}