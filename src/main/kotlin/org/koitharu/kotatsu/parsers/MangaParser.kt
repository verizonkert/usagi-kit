@file:Suppress("unused", "DEPRECATION")

package org.koitharu.kotatsu.parsers

@Deprecated(
    message = "Legacy Kotatsu class. Use tsuki.MangaParser instead.",
    replaceWith = ReplaceWith("MangaParser", "tsuki.MangaParser")
)
public interface MangaParser : tsuki.MangaParser {
    @Deprecated("Legacy Kotatsu accessor")
    public fun getSource(): org.koitharu.kotatsu.parsers.model.MangaParserSource? =
        source as? org.koitharu.kotatsu.parsers.model.MangaParserSource
}

@Deprecated(
    message = "Legacy Kotatsu class. Use tsuki.MangaLoaderContext instead.",
    replaceWith = ReplaceWith("MangaLoaderContext", "tsuki.MangaLoaderContext")
)
public abstract class MangaLoaderContext : tsuki.MangaLoaderContext() {
    public open fun getConfig(source: org.koitharu.kotatsu.parsers.model.MangaSource): org.koitharu.kotatsu.parsers.config.MangaSourceConfig {
        val src = (source as? tsuki.model.MangaSource) ?: object : tsuki.model.MangaSource {
            override val name: String = (source as? Enum<*>)?.name ?: source.toString()
        }
        val cfg = getConfig(src)
        return object : org.koitharu.kotatsu.parsers.config.MangaSourceConfig {
            override fun <T> get(key: tsuki.config.ConfigKey<T>): T = cfg[key]
            override fun <T> get(key: org.koitharu.kotatsu.parsers.config.ConfigKey<T>): T = key.defaultValue
        }
    }
}

@Deprecated(
    message = "Legacy Kotatsu class. Use tsuki.MangaParserAuthProvider instead.",
    replaceWith = ReplaceWith("MangaParserAuthProvider", "tsuki.MangaParserAuthProvider")
)
public interface MangaParserAuthProvider : tsuki.MangaParserAuthProvider

@Deprecated(
    message = "Legacy Kotatsu class. Use tsuki.MangaSourceParser instead.",
    replaceWith = ReplaceWith("MangaSourceParser", "tsuki.MangaSourceParser")
)
public typealias MangaSourceParser = tsuki.MangaSourceParser

@Deprecated(
    message = "Legacy Kotatsu class. Use tsuki.ErrorMessages instead.",
    replaceWith = ReplaceWith("ErrorMessages", "tsuki.ErrorMessages")
)
public typealias ErrorMessages = tsuki.ErrorMessages

// Core parser implementations
public typealias AbstractMangaParser = tsuki.core.AbstractMangaParser
public typealias FlexibleMangaParser = tsuki.core.FlexibleMangaParser
public typealias FlexiblePagedMangaParser = tsuki.core.FlexiblePagedMangaParser
public typealias MangaParserWrapper = tsuki.core.MangaParserWrapper
public typealias PagedMangaParser = tsuki.core.PagedMangaParser
public typealias SinglePageMangaParser = tsuki.core.SinglePageMangaParser
