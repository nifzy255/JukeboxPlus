package me.nifzy.jukeboxplus.client;

import com.mojang.blaze3d.platform.InputConstants;
import me.nifzy.jukeboxplus.Jukebox;
import me.nifzy.jukeboxplus.client.gui.JukeboxScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class JukeboxKeybinds {

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(
                    Identifier.fromNamespaceAndPath(
                            Jukebox.MOD_ID,
                            "main"
                    )
            );

    public static final KeyMapping OPEN_JUKEBOX =
            KeyMappingHelper.registerKeyMapping(
                    new KeyMapping(
                            "key.jukebox.open",
                            InputConstants.Type.KEYBOARD,
                            InputConstants.KEY_M,
                            CATEGORY
                    )
            );

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_JUKEBOX.consumeClick()) {
                client.gui.setScreen(new JukeboxScreen());
            }
        });
    }
}