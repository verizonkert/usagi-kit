package tsuki.config

/**
 * Read-only configuration container associated with a specific [tsuki.model.MangaSource].
 *
 * Provides type-safe retrieval of user preferences, active domains, network headers,
 * and custom source options via [ConfigKey].
 */
public interface MangaSourceConfig {

	/**
	 * Retrieves the current configuration value associated with the specified [key].
	 *
	 * If no user preference has been set or persisted, the key's [ConfigKey.defaultValue] is returned.
	 *
	 * @param T The type of value stored for this key.
	 * @param key The strongly typed descriptor of the configuration entry.
	 * @return The active configuration value of type [T].
	 */
	public operator fun <T> get(key: ConfigKey<T>): T
}

