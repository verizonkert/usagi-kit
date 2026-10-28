package org.draken.usagikit.model

import tsuki.model.ContentType
import tsuki.model.MangaSource
import java.io.File

/**
 * Metadata representing a loaded Usagi extension/plugin (.jar or dex container).
 */
data class UsagiExtension(
    val name: String,
    val file: File,
    val sources: List<UsagiSource>,
    val classLoader: ClassLoader,
)

/**
 * Represents a specific manga source defined within a Usagi extension.
 */
data class UsagiSource(
    val id: String,
    override val name: String,
    override val title: String = name,
    override val locale: String = "",
    override val contentType: ContentType = ContentType.MANGA,
    override val isBroken: Boolean = false,
    val jarName: String = "",
    val delegateConstant: Any? = null,
) : MangaSource
