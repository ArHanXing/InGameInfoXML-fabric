package df11zomgraves.ingameinfo.handler;

import org.lwjgl.glfw.GLFW;

import df11zomgraves.ingameinfo.reference.Names;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;

/**
 * Keybinds. NeoForge's {@code RegisterKeyMappingsEvent} has no Fabric counterpart; Fabric
 * registers key mappings eagerly through {@link KeyBindingHelper}.
 */
public class KeyInputHandler {
	public static final KeyMapping KEY_TOGGLE = new KeyMapping(Names.Keys.TOGGLE, GLFW.GLFW_KEY_F10, Names.Keys.CATEGORY);
	public static final KeyMapping KEY_RELOAD = new KeyMapping(Names.Keys.RELOAD, GLFW.GLFW_KEY_F12, Names.Keys.CATEGORY);

	public static void register() {
		KeyBindingHelper.registerKeyBinding(KEY_TOGGLE);
		KeyBindingHelper.registerKeyBinding(KEY_RELOAD);
	}
}
