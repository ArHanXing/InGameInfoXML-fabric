package df11zomgraves.ingameinfo.api;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import df11zomgraves.ingameinfo.InGameInfoXML;
import df11zomgraves.ingameinfo.tag.Tag;
import df11zomgraves.ingameinfo.tag.TagRegistry;
import df11zomgraves.ingameinfo.value.Value;
import df11zomgraves.ingameinfo.value.ValueRegistry;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Public extension API for InGame Info XML.
 *
 * <h2>Why a queue instead of a plain registry call</h2>
 * This mod registers its built-in tags in its own client initializer. A third-party mod
 * cannot know whether its own initializer runs before or after that, so a naive
 * {@code TagRegistry.register(...)} call would be a race: register too early and the entry
 * may be dropped or collide with a built-in of the same name, register too late and the
 * registry has already been consumed.
 *
 * <p>This API removes the ordering question entirely: {@link #registerTag} always works,
 * whenever it is called. Registrations made before the built-ins are initialized are queued
 * and flushed once the registry is ready; registrations made after are applied directly.
 *
 * <h2>Threading</h2>
 * Registration is expected on the client thread (mod initializers, or a client-tick callback).
 * The internal queue is thread-safe, but ordering is only meaningful from a single thread.
 *
 * <h2>Typical use</h2>
 * <pre>{@code
 * public class MyModClient implements ClientModInitializer {
 *     @Override
 *     public void onInitializeClient() {
 *         if (FabricLoader.getInstance().isModLoaded("ingameinfoxml")) {
 *             InGameInfoAPI.registerTag(new TagMyValue().setName("mymod_value"));
 *         }
 *     }
 * }
 * }</pre>
 *
 * @see Tag
 * @see Value
 */
public final class InGameInfoAPI {

	/** Tags registered before {@link #markReady()} ran. */
	private static final List<Tag> PENDING_TAGS = new CopyOnWriteArrayList<>();
	/** Values registered before {@link #markReady()} ran. */
	private static final List<Value> PENDING_VALUES = new CopyOnWriteArrayList<>();
	/** Callbacks to run once the built-in registries are initialized. */
	private static final List<Runnable> READY_CALLBACKS = new CopyOnWriteArrayList<>();

	private static volatile boolean ready = false;

	private InGameInfoAPI() {
	}

	// ------------------------------------------------------------------
	// Registration
	// ------------------------------------------------------------------

	/**
	 * Registers a tag. Safe to call at any point in the mod-loading or game lifecycle.
	 *
	 * <p>The tag's {@link Tag#getName() name} is what a user writes as {@code {name}} in an XML
	 * config, as {@code {name}} in a plain-text config, or as a {@code <var>name</var>} element
	 * in XML. {@link Tag#setAliases(String...)} additionally registers extra names for the same
	 * tag. Names are matched case-insensitively.
	 *
	 * @param tag the tag instance; its name must already be set and must not collide with a
	 *            built-in tag, or the registration is rejected and logged
	 * @throws IllegalArgumentException if {@code tag} is null or has no name
	 */
	public static void registerTag(final Tag tag) {
		if (tag == null) {
			throw new IllegalArgumentException("tag must not be null");
		}
		if (tag.getName() == null || tag.getName().isBlank()) {
			throw new IllegalArgumentException(
					"tag name must be set before registering (call setName(...) first)");
		}

		if (ready) {
			TagRegistry.INSTANCE.register(tag);
		} else {
			PENDING_TAGS.add(tag);
		}
	}

	/**
	 * Registers a value type (an operator or literal usable inside XML elements such as
	 * {@code <max>} / {@code <if>} / {@code <operation>}).
	 *
	 * <p>The instance acts as a prototype: only its concrete class and aliases are recorded,
	 * and fresh instances are created reflectively on demand, so it must have a public no-arg
	 * constructor — the same requirement the built-in value types already meet.
	 *
	 * @param value the prototype instance; its name must already be set
	 * @throws IllegalArgumentException if {@code value} is null or has no name
	 */
	public static void registerValue(final Value value) {
		if (value == null) {
			throw new IllegalArgumentException("value must not be null");
		}
		if (value.getName() == null || value.getName().isBlank()) {
			throw new IllegalArgumentException(
					"value name must be set before registering (call setName(...) first)");
		}

		if (ready) {
			ValueRegistry.INSTANCE.register(value);
		} else {
			PENDING_VALUES.add(value);
		}
	}

	/**
	 * Runs {@code callback} once the built-in tag and value registries are ready, or
	 * immediately if they already are. Use this when you need to inspect the registry (for
	 * example to check whether a name is already taken) before registering.
	 */
	public static void onReady(final Runnable callback) {
		if (callback == null) {
			throw new IllegalArgumentException("callback must not be null");
		}
		if (ready) {
			callback.run();
		} else {
			READY_CALLBACKS.add(callback);
		}
	}

	// ------------------------------------------------------------------
	// Queries
	// ------------------------------------------------------------------

	/** Whether the built-in registries have been initialized and queued entries flushed. */
	public static boolean isReady() {
		return ready;
	}

	/**
	 * Whether a tag name is already registered. Useful to skip registering an override: a
	 * duplicate name is rejected rather than silently replacing the built-in.
	 *
	 * @param name the tag name, matched case-insensitively; surrounding {@code {}} are ignored
	 */
	public static boolean isTagRegistered(final String name) {
		if (name == null) {
			return false;
		}
		return TagRegistry.INSTANCE.isRegistered(normalizeTagName(name));
	}

	/** All registered tag names including aliases, sorted, for diagnostics or a config editor. */
	public static List<String> getRegisteredTagNames() {
		return TagRegistry.INSTANCE.getRegisteredNames();
	}

	/**
	 * Convenience soft-dependency check. Prefer this over hard-depending on another mod: only
	 * register integration tags when the other mod is actually present.
	 *
	 * @param modId the Fabric mod id, e.g. {@code "botania"}
	 */
	public static boolean isModLoaded(final String modId) {
		return modId != null && FabricLoader.getInstance().isModLoaded(modId);
	}

	// ------------------------------------------------------------------
	// Lifecycle (called by this mod, not by third parties)
	// ------------------------------------------------------------------

	/**
	 * Flushes queued registrations and marks the API ready. Called by InGame Info XML after its
	 * own built-in registries are initialized. Third-party mods must not call this.
	 */
	public static void markReady() {
		if (ready) {
			return;
		}
		ready = true;

		final int pendingValues = PENDING_VALUES.size();
		final int pendingTags = PENDING_TAGS.size();

		for (final Value value : PENDING_VALUES) {
			ValueRegistry.INSTANCE.register(value);
		}
		PENDING_VALUES.clear();

		for (final Tag tag : PENDING_TAGS) {
			TagRegistry.INSTANCE.register(tag);
		}
		PENDING_TAGS.clear();

		if (pendingValues > 0 || pendingTags > 0) {
			InGameInfoXML.logger.info(
					"Applied {} queued value(s) and {} queued tag(s) from other mods.",
					pendingValues, pendingTags);
		}

		for (final Runnable callback : READY_CALLBACKS) {
			try {
				callback.run();
			} catch (final Throwable t) {
				InGameInfoXML.logger.error("Extension ready-callback failed", t);
			}
		}
		READY_CALLBACKS.clear();

		InGameInfoXML.logger.info("Extension API ready ({} tags total).",
				TagRegistry.INSTANCE.getRegisteredTags().size());
	}

	/** Strips surrounding {@code {}} so both {@code mana} and {@code {mana}} are accepted. */
	private static String normalizeTagName(final String name) {
		String n = name.trim();
		if (n.startsWith("{")) {
			n = n.substring(1);
		}
		if (n.endsWith("}")) {
			n = n.substring(0, n.length() - 1);
		}
		return n.toLowerCase(Locale.ENGLISH);
	}
}
