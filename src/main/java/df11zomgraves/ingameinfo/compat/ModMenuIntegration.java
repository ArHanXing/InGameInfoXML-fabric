package df11zomgraves.ingameinfo.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import df11zomgraves.ingameinfo.handler.ConfigurationHandler;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * ModMenu hook, so Cloth Config's generated screen is reachable from the mod list.
 *
 * <p>This is the Fabric analogue of the NeoForge build registering
 * {@code IConfigScreenFactory}/{@code ConfigurationScreen} on the mod container. The
 * {@code modmenu} entrypoint is only ever invoked by ModMenu itself, so when ModMenu is not
 * installed nothing loads this class and the reference below never resolves — which is why
 * ModMenu stays a {@code suggest} rather than a hard dependency.
 */
@Environment(EnvType.CLIENT)
public class ModMenuIntegration implements ModMenuApi {

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> AutoConfig
				.getConfigScreen(ConfigurationHandler.Data.class, parent)
				.get();
	}
}
