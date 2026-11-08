package df11zomgraves.ingameinfo.handler;

import df11zomgraves.ingameinfo.reference.Alignment;
import df11zomgraves.ingameinfo.reference.Names;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.world.InteractionResult;

/**
 * Configuration, ported from NeoForge's {@code ModConfigSpec} to Cloth Config's AutoConfig.
 *
 * <p>Design note: the NeoForge build already had a two-layer setup — a {@code ModConfigSpec}
 * for persistence and a set of plain static fields that the rest of the code actually reads
 * ({@code ConfigurationHandler.scale}, {@code .configName}, ...). This port keeps those static
 * fields byte-for-byte identical and only swaps out the persistence layer, so no other file in
 * the project had to change. {@link #load()} copies config -> statics (the old
 * {@code ModConfigEvent.Loading} handler), and {@link #applyConfiguration()} copies
 * statics -> config and saves (the old {@code ConfigValue.set} + {@code SPEC.save()}).
 *
 * <p>File format changes from NeoForge's {@code ingameinfoxml-client.toml} to Cloth Config's
 * {@code config/ingameinfoxml.json}. Option names are kept identical to the old keys, so
 * migrating an existing config is a copy-across job.
 */
public class ConfigurationHandler {

	// ------------------------------------------------------------------
	// Live values. Names and types are identical to the NeoForge build.
	// ------------------------------------------------------------------
	public static boolean showOverlayItemIcons;
	public static boolean showOverlayPotions;
	public static int fileInterval;
	public static boolean replaceDebug;
	public static boolean showOnPlayerList;
	public static Double scale;
	public static String configName;
	public static boolean showInChat;
	public static boolean numericAmplifier;
	public static boolean sendSeedToChat;
	public static boolean showSurvivalHUD;
	public static long seed;
	public static Alignment alignmentMiddleCenter;
	public static int healthDecimalPlace;
	public static int hungerDecimalPlace;
	public static int msptDecimalPlace;
	public static int tpsDecimalPlace;
	public static boolean displayInvisiblePlayer;

	private static ConfigHolder<Data> holder;

	/** AutoConfig data class. Field names mirror the old TOML option keys. */
	@Config(name = "ingameinfoxml")
	public static class Data implements ConfigData {

		@ConfigEntry.Category("general")
		@ConfigEntry.Gui.Tooltip
		public String filename = "ingameinfo.xml";

		@ConfigEntry.Category("general")
		@ConfigEntry.Gui.Tooltip
		public boolean showInChat = true;

		@ConfigEntry.Category("general")
		@ConfigEntry.Gui.Tooltip
		public boolean showOnPlayerList = true;

		@ConfigEntry.Category("general")
		@ConfigEntry.Gui.Tooltip
		public double scale = 1.0D;

		@ConfigEntry.Category("general")
		@ConfigEntry.Gui.Tooltip
		public int fileInterval = 5;

		@ConfigEntry.Category("general")
		@ConfigEntry.Gui.Tooltip
		public boolean showOverlayPotions = true;

		@ConfigEntry.Category("general")
		@ConfigEntry.Gui.Tooltip
		public boolean showOverlayItemIcons = true;

		@ConfigEntry.Category("general")
		@ConfigEntry.Gui.Tooltip
		public boolean numericAmplifier = false;

		@ConfigEntry.Category("general")
		@ConfigEntry.Gui.Tooltip
		public boolean sendSeedToChat = false;

		@ConfigEntry.Category("general")
		@ConfigEntry.Gui.Tooltip
		public long defaultSeedInServer = 0L;

		@ConfigEntry.Category("general")
		@ConfigEntry.Gui.Tooltip
		public Alignment alignmentForMiddleCenter = Alignment.MIDDLECENTER;

		@ConfigEntry.Category("general")
		@ConfigEntry.Gui.Tooltip
		public boolean showSurvivalHUD = true;

		@ConfigEntry.Category("general")
		@ConfigEntry.Gui.Tooltip
		public int healthDigit = 2;

		@ConfigEntry.Category("general")
		@ConfigEntry.Gui.Tooltip
		public int hungerDigit = 2;

		@ConfigEntry.Category("general")
		@ConfigEntry.Gui.Tooltip
		public int msptDigit = 1;

		@ConfigEntry.Category("general")
		@ConfigEntry.Gui.Tooltip
		public int tpsDigit = 1;

		@ConfigEntry.Category("general")
		@ConfigEntry.Gui.Tooltip
		public boolean displayInvisiblePlayer = false;

		@ConfigEntry.Category("alignment")
		@ConfigEntry.Gui.Tooltip
		public String topleft = "2 2";

		@ConfigEntry.Category("alignment")
		@ConfigEntry.Gui.Tooltip
		public String topcenter = "0 2";

		@ConfigEntry.Category("alignment")
		@ConfigEntry.Gui.Tooltip
		public String topright = "-2 2";

		@ConfigEntry.Category("alignment")
		@ConfigEntry.Gui.Tooltip
		public String middleleft = "2 0";

		@ConfigEntry.Category("alignment")
		@ConfigEntry.Gui.Tooltip
		public String middlecenter = "0 0";

		@ConfigEntry.Category("alignment")
		@ConfigEntry.Gui.Tooltip
		public String middleright = "-2 0";

		@ConfigEntry.Category("alignment")
		@ConfigEntry.Gui.Tooltip
		public String bottomleft = "2 -2";

		@ConfigEntry.Category("alignment")
		@ConfigEntry.Gui.Tooltip
		public String bottomcenter = "0 -45";

		@ConfigEntry.Category("alignment")
		@ConfigEntry.Gui.Tooltip
		public String bottomright = "-2 -2";

		/** Replaces the old {@code defineInRange} bounds, which Cloth Config has no direct
		 *  equivalent for on all types. */
		@Override
		public void validatePostLoad() throws ValidationException {
			scale = clamp(scale, 0.5D, 2.0D);
			fileInterval = clamp(fileInterval, 1, 60);
			if (filename == null || filename.isBlank()) {
				filename = "ingameinfo.xml";
			}
			if (alignmentForMiddleCenter == null) {
				alignmentForMiddleCenter = Alignment.MIDDLECENTER;
			}
			healthDigit = clamp(healthDigit, 0, 6);
			hungerDigit = clamp(hungerDigit, 0, 6);
			msptDigit = clamp(msptDigit, 0, 6);
			tpsDigit = clamp(tpsDigit, 0, 6);
		}

		private static double clamp(double v, double min, double max) {
			return v < min ? min : (v > max ? max : v);
		}

		private static int clamp(int v, int min, int max) {
			return v < min ? min : (v > max ? max : v);
		}
	}

	/** Register with AutoConfig and wire the load/save listeners. Call from client init. */
	public static void init() {
		holder = AutoConfig.register(Data.class, GsonConfigSerializer::new);
		// Mirror the old ModConfigEvent.Loading / Reloading hooks.
		holder.registerLoadListener((h, data) -> {
			load();
			return InteractionResult.SUCCESS;
		});
		holder.registerSaveListener((h, data) -> {
			load();
			return InteractionResult.SUCCESS;
		});
	}

	public static Data data() {
		return holder.getConfig();
	}

	public static ConfigHolder<Data> holder() {
		return holder;
	}

	/** config -> static fields (was {@code ModConfigEvent.Loading}). */
	public static void load() {
		final Data d = data();
		Alignment.TOPLEFT.setXY(d.topleft);
		Alignment.TOPCENTER.setXY(d.topcenter);
		Alignment.TOPRIGHT.setXY(d.topright);
		Alignment.MIDDLELEFT.setXY(d.middleleft);
		Alignment.MIDDLECENTER.setXY(d.middlecenter);
		Alignment.MIDDLERIGHT.setXY(d.middleright);
		Alignment.BOTTOMLEFT.setXY(d.bottomleft);
		Alignment.BOTTOMCENTER.setXY(d.bottomcenter);
		Alignment.BOTTOMRIGHT.setXY(d.bottomright);
		showOverlayItemIcons = d.showOverlayItemIcons;
		showOverlayPotions = d.showOverlayPotions;
		fileInterval = d.fileInterval;
		showOnPlayerList = d.showOnPlayerList;
		scale = d.scale;
		configName = d.filename;
		showInChat = d.showInChat;
		numericAmplifier = d.numericAmplifier;
		sendSeedToChat = d.sendSeedToChat;
		showSurvivalHUD = d.showSurvivalHUD;
		healthDecimalPlace = d.healthDigit;
		hungerDecimalPlace = d.hungerDigit;
		msptDecimalPlace = d.msptDigit;
		tpsDecimalPlace = d.tpsDigit;
		displayInvisiblePlayer = d.displayInvisiblePlayer;
		// Cloth Config keeps a real long, so the Integer/Long cast workaround the NeoForge
		// build needed here is gone.
		seed = d.defaultSeedInServer;
		alignmentMiddleCenter = d.alignmentForMiddleCenter;
	}

	/** static fields -> config, then save (was {@code ConfigValue.set} + {@code SPEC.save()}). */
	public static void applyConfiguration() {
		final Data d = data();
		d.showOverlayItemIcons = showOverlayItemIcons;
		d.showOverlayPotions = showOverlayPotions;
		d.fileInterval = fileInterval;
		d.showOnPlayerList = showOnPlayerList;
		d.scale = scale;
		d.filename = configName;
		d.showInChat = showInChat;
		d.numericAmplifier = numericAmplifier;
		d.sendSeedToChat = sendSeedToChat;
		d.showSurvivalHUD = showSurvivalHUD;
		d.healthDigit = healthDecimalPlace;
		d.hungerDigit = hungerDecimalPlace;
		d.msptDigit = msptDecimalPlace;
		d.tpsDigit = tpsDecimalPlace;
		d.displayInvisiblePlayer = displayInvisiblePlayer;
		d.alignmentForMiddleCenter = alignmentMiddleCenter;
		d.topleft = Alignment.TOPLEFT.getXY();
		d.topcenter = Alignment.TOPCENTER.getXY();
		d.topright = Alignment.TOPRIGHT.getXY();
		d.middleleft = Alignment.MIDDLELEFT.getXY();
		d.middlecenter = Alignment.MIDDLECENTER.getXY();
		d.middleright = Alignment.MIDDLERIGHT.getXY();
		d.bottomleft = Alignment.BOTTOMLEFT.getXY();
		d.bottomcenter = Alignment.BOTTOMCENTER.getXY();
		d.bottomright = Alignment.BOTTOMRIGHT.getXY();
		holder.save();
	}

	/** Debug helper so users can see where the config landed. */
	public static String configFilePathHint() {
		return Names.MODID + ".json (Cloth Config)";
	}
}
