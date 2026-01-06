package dev.redstone.packagelogger;

import dev.redstone.packagelogger.config.ModConfig;
import dev.redstone.packagelogger.screen.SimpleConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class PackageLoggerClient implements ClientModInitializer {
	private static KeyBinding configKeyBinding;

	@Override
	public void onInitializeClient() {
		// Lade Konfiguration
		ModConfig.load();

		// Registriere Keybinding (F6 = Config)
		configKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.package-logger.config",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_F6,
				new net.minecraft.client.option.KeyBinding.Category(
						net.minecraft.util.Identifier.of("package-logger", "category"))));

		// Registriere Tick-Event für Keybinding
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (configKeyBinding.wasPressed()) {
				if (client.currentScreen == null) {
					client.setScreen(new SimpleConfigScreen(null));
				}
			}
		});

		System.out.println("[PackageLogger] Initialized! Press F6 to open config.");
	}
}