package df11zomgraves.ingameinfo.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import df11zomgraves.ingameinfo.handler.ConfigurationHandler;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

/**
 * Suppresses vanilla HUD layers.
 *
 * <p>This is the one genuinely new piece of code in the port. NeoForge exposes
 * {@code RenderGuiLayerEvent.Pre} plus {@code VanillaGuiLayers}, so the original
 * {@code Ticker.onRenderGuiOverlayEvent} could cancel individual layers by id. Fabric API
 * 1.21.1 has no such event: the multi-layer HUD API was backported in Fabric API 0.116.16 and
 * then reverted in 0.116.17, leaving only {@code HudRenderCallback} (which cannot cancel
 * vanilla drawing). A mixin is therefore the only way to keep this feature.
 *
 * <p>Mapping of the original NeoForge layer ids onto 1.21.1 mojmap methods, verified against
 * the real 1.21.1 client jar rather than from memory:
 * <ul>
 *   <li>{@code PLAYER_HEALTH} + {@code ARMOR_LEVEL} + {@code AIR_LEVEL}
 *       -> {@code Gui.renderPlayerHealth} (armour is drawn by {@code renderArmor} and the air
 *       bubbles from {@code AIR_SPRITE}, both invoked from inside this method)</li>
 *   <li>{@code FOOD_LEVEL} -> {@code Gui.renderFood}</li>
 *   <li>{@code VEHICLE_HEALTH} -> {@code Gui.renderVehicleHealth}</li>
 *   <li>{@code EFFECTS} -> {@code Gui.renderEffects} (gated on showOverlayPotions)</li>
 * </ul>
 * The mixin config is declared client-only in fabric.mod.json, so a dedicated server never
 * applies it (the {@code Gui} class does not exist there).
 */
@Mixin(Gui.class)
public abstract class GuiMixin {

	@Inject(method = "renderPlayerHealth", at = @At("HEAD"), cancellable = true)
	private void ingameinfo$hidePlayerHealth(GuiGraphics guiGraphics, CallbackInfo ci) {
		if (!ConfigurationHandler.showSurvivalHUD) {
			ci.cancel();
		}
	}

	@Inject(method = "renderFood", at = @At("HEAD"), cancellable = true)
	private void ingameinfo$hideFood(GuiGraphics guiGraphics, Player player, int x, int y, CallbackInfo ci) {
		if (!ConfigurationHandler.showSurvivalHUD) {
			ci.cancel();
		}
	}

	@Inject(method = "renderVehicleHealth", at = @At("HEAD"), cancellable = true)
	private void ingameinfo$hideVehicleHealth(GuiGraphics guiGraphics, CallbackInfo ci) {
		if (!ConfigurationHandler.showSurvivalHUD) {
			ci.cancel();
		}
	}

	@Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
	private void ingameinfo$hideEffects(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		if (!ConfigurationHandler.showOverlayPotions) {
			ci.cancel();
		}
	}
}
