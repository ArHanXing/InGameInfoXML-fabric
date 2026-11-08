package df11zomgraves.ingameinfo.command;

import java.nio.file.Path;
import java.nio.file.Paths;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import df11zomgraves.ingameinfo.InGameInfoCore;
import df11zomgraves.ingameinfo.InGameInfoXML;
import df11zomgraves.ingameinfo.command.arguments.AlignArgument;
import df11zomgraves.ingameinfo.command.arguments.FileArgument;
import df11zomgraves.ingameinfo.handler.ConfigurationHandler;
import df11zomgraves.ingameinfo.reference.Alignment;
import df11zomgraves.ingameinfo.reference.Names;
import net.minecraft.client.Minecraft;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import net.fabricmc.loader.api.FabricLoader;

public class InGameInfoCommand {

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		final String strGet = Names.Command.GET;
		final String strSet = Names.Command.SET;
		final String strAlign = Names.Command.ALIENMENT;
		final String x = "X";
		final String y = "Y";
		final String seed = "Seed";
		final String filename = "filename";
		final String value = "value";
		LiteralArgumentBuilder<FabricClientCommandSource> igiCommand = ClientCommandManager.literal(Names.Command.NAME);
		// igi alignment get TOPLEFT
		igiCommand.then(ClientCommandManager.literal(strAlign).then(ClientCommandManager.literal(strGet).then(ClientCommandManager.argument(strAlign, AlignArgument.GetAlignment())
				.executes(source -> GetAlignment(AlignArgument.GetString(source, strAlign))))));
		
		// igi alignment set TOPLEFT 2 2
		igiCommand.then(ClientCommandManager.literal(strAlign).then(ClientCommandManager.literal(strSet).then(ClientCommandManager.argument(strAlign, AlignArgument.GetAlignment())
				.then(ClientCommandManager.argument(x, IntegerArgumentType.integer()).then(ClientCommandManager.argument(y, IntegerArgumentType.integer())
						.executes(source -> SetAlignment(AlignArgument.GetString(source, strAlign), IntegerArgumentType.getInteger(source, x),
								IntegerArgumentType.getInteger(source, y))))))));
		
		// igi load ingameinfo.xml
		igiCommand.then(ClientCommandManager.literal("load").then(ClientCommandManager.argument(filename, FileArgument.files())
				.executes(source -> loadFile(FileArgument.GetString(source, filename)))));
		
		// igi setseed -2793514409027566328
		igiCommand.then(ClientCommandManager.literal("setseed").then(ClientCommandManager.argument(seed, LongArgumentType.longArg())
				.executes(source -> SetSeed(LongArgumentType.getLong(source, seed)))));
		
		// igi setmiddlecenter MIDDLECENTER
		igiCommand.then(ClientCommandManager.literal("setmiddlecenter").then(ClientCommandManager.argument(strAlign, AlignArgument.GetAlignment())
				.executes(source -> SetMiddleCenterAlignment(AlignArgument.GetString(source, strAlign)))));
		
		// igi config showInChat true
		igiCommand.then(ClientCommandManager.literal("config").then(ClientCommandManager.literal(Names.Config.SHOW_IN_CHAT)
				.then(ClientCommandManager.argument(value, BoolArgumentType.bool()).executes(
						source -> setBoolean(Names.Config.SHOW_IN_CHAT, BoolArgumentType.getBool(source, value))))));
		
		// igi config showOnPlayerList true
		igiCommand.then(ClientCommandManager.literal("config").then(ClientCommandManager.literal(Names.Config.SHOW_ON_PLAYER_LIST)
				.then(ClientCommandManager.argument(value, BoolArgumentType.bool()).executes(
						source -> setBoolean(Names.Config.SHOW_ON_PLAYER_LIST, BoolArgumentType.getBool(source, value))))));
		
		// igi config showOverlayPotions true
		igiCommand.then(ClientCommandManager.literal("config").then(ClientCommandManager.literal(Names.Config.SHOW_OVERLAY_POTIONS)
				.then(ClientCommandManager.argument(value, BoolArgumentType.bool()).executes(
						source -> setBoolean(Names.Config.SHOW_OVERLAY_POTIONS, BoolArgumentType.getBool(source, value))))));
		
		// igi config showOverlayItemIcons true
		igiCommand.then(ClientCommandManager.literal("config").then(ClientCommandManager.literal(Names.Config.SHOW_OVERLAY_ITEM_ICONS)
				.then(ClientCommandManager.argument(value, BoolArgumentType.bool()).executes(
						source -> setBoolean(Names.Config.SHOW_OVERLAY_ITEM_ICONS, BoolArgumentType.getBool(source, value))))));
		
		// igi config numericAmplifier true
		igiCommand.then(ClientCommandManager.literal("config").then(ClientCommandManager.literal(Names.Config.NUMERIC_AMPLIFIER)
				.then(ClientCommandManager.argument(value, BoolArgumentType.bool()).executes(
						source -> setBoolean(Names.Config.NUMERIC_AMPLIFIER, BoolArgumentType.getBool(source, value))))));
		
		// igi config sendSeedToChat true
		igiCommand.then(ClientCommandManager.literal("config").then(ClientCommandManager.literal(Names.Config.SEND_SEED_TO_CHAT)
				.then(ClientCommandManager.argument(value, BoolArgumentType.bool()).executes(
						source -> setBoolean(Names.Config.SEND_SEED_TO_CHAT, BoolArgumentType.getBool(source, value))))));
		
		// igi config showSurvivalHUD true
		igiCommand.then(ClientCommandManager.literal("config").then(ClientCommandManager.literal(Names.Config.SHOW_SURVIVAL_HUD)
				.then(ClientCommandManager.argument(value, BoolArgumentType.bool()).executes(
						source -> setBoolean(Names.Config.SHOW_SURVIVAL_HUD, BoolArgumentType.getBool(source, value))))));
		
		// igi config displayInvisiblePlayer true
		igiCommand.then(ClientCommandManager.literal("config").then(ClientCommandManager.literal(Names.Config.DISPLAY_INVISIBLE_PLAYER)
				.then(ClientCommandManager.argument(value, BoolArgumentType.bool()).executes(
						source -> setBoolean(Names.Config.DISPLAY_INVISIBLE_PLAYER, BoolArgumentType.getBool(source, value))))));
		
		// igi config healthDigit 2
		igiCommand.then(ClientCommandManager.literal("config").then(ClientCommandManager.literal(Names.Config.HEALTH_DECIMAL_PLACE)
				.then(ClientCommandManager.argument(value, IntegerArgumentType.integer()).executes(
						source -> setDecimalPlace(Names.Config.HEALTH_DECIMAL_PLACE, IntegerArgumentType.getInteger(source, value))))));
		
		// igi config hungerDigit 2
		igiCommand.then(ClientCommandManager.literal("config").then(ClientCommandManager.literal(Names.Config.HUNGER_DECIMAL_PLACE)
				.then(ClientCommandManager.argument(value, IntegerArgumentType.integer()).executes(
						source -> setDecimalPlace(Names.Config.HUNGER_DECIMAL_PLACE, IntegerArgumentType.getInteger(source, value))))));
		
		// igi config msptDigit 0
		igiCommand.then(ClientCommandManager.literal("config").then(ClientCommandManager.literal(Names.Config.MSPT_DECIMAL_PLACE)
				.then(ClientCommandManager.argument(value, IntegerArgumentType.integer()).executes(
						source -> setDecimalPlace(Names.Config.MSPT_DECIMAL_PLACE, IntegerArgumentType.getInteger(source, value))))));
		
		// igi config tpsDigit 0
		igiCommand.then(ClientCommandManager.literal("config").then(ClientCommandManager.literal(Names.Config.TPS_DECIMAL_PLACE)
				.then(ClientCommandManager.argument(value, IntegerArgumentType.integer()).executes(
						source -> setDecimalPlace(Names.Config.TPS_DECIMAL_PLACE, IntegerArgumentType.getInteger(source, value))))));
		dispatcher.register(igiCommand);
	}

	public static int SetAlignment(String alignment, int x, int y) {
		String alignValue = String.format("%s %s", x, y);
		Alignment align = Alignment.parse(alignment);
		Minecraft mc = Minecraft.getInstance();
		if (align == null) {
			mc.gui.getChat().addMessage(Component.translatable(Names.Command.Message.ALIGNMENT_SET_FAILURE, alignment));
			return -1;
		}
		align.setXY(alignValue);
		ConfigurationHandler.applyConfiguration();
		mc.gui.getChat().addMessage(
				Component.translatable(Names.Command.Message.ALIGNMENT_SET_SUCCESS, alignment, align.x, align.y));
		return 1;
	}

	public static int GetAlignment(String alignment) {
		Alignment align = Alignment.parse(alignment);
		Minecraft mc = Minecraft.getInstance();
		if (align == null) {
			mc.gui.getChat().addMessage(Component.translatable(Names.Command.Message.ALIGNMENT_GET_FAILURE, alignment));
			return -1;
		}
		mc.gui.getChat().addMessage(
				Component.translatable(Names.Command.Message.ALIGNMENT_GET_SUCCESS, alignment, align.x, align.y));
		return 1;
	}

	public static int SetMiddleCenterAlignment(String alignment) {
		Alignment align = Alignment.parse(alignment);
		Minecraft mc = Minecraft.getInstance();
		if (align == null) {
			mc.gui.getChat().addMessage(
					Component.translatable(Names.Command.Message.ALIGNMENT_MIDDLECENTER_SET_FAILURE, alignment));
			return -1;
		}
		ConfigurationHandler.alignmentMiddleCenter = align;
		ConfigurationHandler.applyConfiguration();
		mc.gui.getChat().addMessage(
				Component.translatable(Names.Command.Message.ALIGNMENT_MIDDLECENTER_SET_SUCCESS, alignment));
		return 1;
	}

	public static int SetSeed(long seed) {
		Minecraft mc = Minecraft.getInstance();
		if (InGameInfoXML.serverInstalled) {
			mc.gui.getChat().addMessage(Component.translatable(Names.Command.Message.SED_SEET_SUCCESS2));
			return 1;
		}
		try {
			ConfigurationHandler.seed = seed;
			ConfigurationHandler.applyConfiguration();
			InGameInfoXML.seed = seed;
			mc.gui.getChat().addMessage(Component.translatable(Names.Command.Message.SED_SEET_SUCCESS, seed));
		} catch (final Exception e) {
			mc.gui.getChat().addMessage(Component.translatable(Names.Command.Message.SED_SEET_FAILURE));
			return -1;
		}
		return 1;
	}

	public static int loadFile(String filename) {
		Minecraft mc = Minecraft.getInstance();
		mc.gui.getChat().addMessage(Component.translatable(Names.Command.Message.LOAD, filename));
		InGameInfoCore core = InGameInfoCore.INSTANCE;
		Path configPath = FabricLoader.getInstance().getConfigDir();
		Path modConfigPath = Paths.get(configPath.toAbsolutePath().toString());

		core.setConfigDirectory(modConfigPath.toFile());
		core.setConfigFile(filename);
		ConfigurationHandler.configName = filename;
		ConfigurationHandler.applyConfiguration();
		boolean b = core.reloadConfig();
		if (!b) {
			mc.gui.getChat().addMessage(Component.translatable(Names.Command.Message.FAILURE));
			return -1;
		}
		mc.gui.getChat().addMessage(Component.translatable(Names.Command.Message.SUCCESS));
		return 1;
	}

	public static int setBoolean(String config, boolean value) {
		Minecraft mc = Minecraft.getInstance();
		if (config.equals(Names.Config.SHOW_IN_CHAT))
			ConfigurationHandler.showInChat = value;
		else if (config.equals(Names.Config.SHOW_IN_CHAT))
			ConfigurationHandler.showInChat = value;
		else if (config.equals(Names.Config.SHOW_OVERLAY_POTIONS))
			ConfigurationHandler.showOverlayPotions = value;
		else if (config.equals(Names.Config.SHOW_OVERLAY_ITEM_ICONS))
			ConfigurationHandler.showOverlayItemIcons = value;
		else if (config.equals(Names.Config.NUMERIC_AMPLIFIER))
			ConfigurationHandler.numericAmplifier = value;
		else if (config.equals(Names.Config.SEND_SEED_TO_CHAT))
			ConfigurationHandler.sendSeedToChat = value;
		else if (config.equals(Names.Config.SHOW_SURVIVAL_HUD))
			ConfigurationHandler.showSurvivalHUD = value;
		else if (config.equals(Names.Config.DISPLAY_INVISIBLE_PLAYER))
			ConfigurationHandler.displayInvisiblePlayer = value;
		else {
			mc.gui.getChat().addMessage(Component.translatable(Names.Command.Message.CONFIG_NOT_FOUND, config));
			return -1;
		}
		ConfigurationHandler.applyConfiguration();
		mc.gui.getChat().addMessage(Component.translatable(Names.Command.Message.CONFIG_SET_SUCCESS, config, value));
		return 1;
	}
	
	public static int setDecimalPlace(String config, int value) {
		Minecraft mc = Minecraft.getInstance();
		if (value < 0)
			value = 0;
		else if (value > 6)
			value = 6;
		if (config.equals(Names.Config.HEALTH_DECIMAL_PLACE))
			ConfigurationHandler.healthDecimalPlace = value;
		else if (config.equals(Names.Config.HUNGER_DECIMAL_PLACE))
			ConfigurationHandler.hungerDecimalPlace = value;
		else if (config.equals(Names.Config.MSPT_DECIMAL_PLACE))
			ConfigurationHandler.msptDecimalPlace = value;
		else if (config.equals(Names.Config.TPS_DECIMAL_PLACE))
			ConfigurationHandler.tpsDecimalPlace = value;
		else {
			mc.gui.getChat().addMessage(Component.translatable(Names.Command.Message.CONFIG_NOT_FOUND, config));
			return -1;
		}
		ConfigurationHandler.applyConfiguration();
		mc.gui.getChat().addMessage(Component.translatable(Names.Command.Message.CONFIG_SET_SUCCESS, config, value));
		return 1;
	}
}
