package me.nifzy.jukeboxplus.client;

import me.nifzy.jukeboxplus.Jukebox;
import me.nifzy.jukeboxplus.client.music.MusicLibrary;
import me.nifzy.jukeboxplus.client.music.MusicManager;
import me.nifzy.jukeboxplus.client.music.MusicTrack;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class JukeboxClient implements ClientModInitializer {

	public static MusicLibrary MUSIC_LIBRARY;
	public static MusicManager MUSIC_MANAGER;

	private static int startupTicks = 0;
	private static boolean testStarted = false;

	@Override
	public void onInitializeClient() {
		MUSIC_LIBRARY = new MusicLibrary();
		MUSIC_MANAGER = new MusicManager(MUSIC_LIBRARY);

		JukeboxKeybinds.initialize();

		Jukebox.LOGGER.info(
				"Jukebox+ initialized with {} track(s).",
				MUSIC_LIBRARY.getTracks().size()
		);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (testStarted) {
				return;
			}

			if (MUSIC_LIBRARY.getTracks().isEmpty()) {
				testStarted = true;
				return;
			}

			/*
			 * Give Minecraft's initial sound system time to finish
			 * loading/reloading before starting our external track.
			 */
			startupTicks++;

			if (startupTicks < 100) {
				return;
			}

			MusicTrack testTrack = MUSIC_LIBRARY.getTracks().getFirst();

			MUSIC_MANAGER.play(testTrack);
			testStarted = true;
		});
	}
}