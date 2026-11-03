@file:Suppress("unused", "DEPRECATION")

package org.koitharu.kotatsu.parsers.config

@Deprecated("Legacy Kotatsu class. Use tsuki.config.ConfigKey instead.", ReplaceWith("ConfigKey<T>", "tsuki.config.ConfigKey"))
public open class ConfigKey<T>(
    @JvmField public val key: String,
    public open val defaultValue: T
) {
    public class Domain(vararg val presetValues: String) : ConfigKey<String>("domain", presetValues.first())
    public class ShowSuspiciousContent(defaultValue: Boolean) : ConfigKey<Boolean>("show_suspicious", defaultValue)
    public class UserAgent(defaultValue: String) : ConfigKey<String>("user_agent", defaultValue)
    public class SplitByTranslations(defaultValue: Boolean) : ConfigKey<Boolean>("split_translations", defaultValue)
    public class PreferredImageServer(val presetValues: Map<String?, String?>, defaultValue: String?) : ConfigKey<String?>("img_server", defaultValue)
    public class UserInput(key: String) : ConfigKey<String?>("input.$key", null)
}

@Deprecated("Legacy Kotatsu class. Use tsuki.config.MangaSourceConfig instead.", ReplaceWith("MangaSourceConfig", "tsuki.config.MangaSourceConfig"))
public interface MangaSourceConfig : tsuki.config.MangaSourceConfig {
    public operator fun <T> get(key: ConfigKey<T>): T
}
