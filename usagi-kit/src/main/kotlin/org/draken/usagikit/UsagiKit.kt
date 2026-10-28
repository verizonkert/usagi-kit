package org.draken.usagikit

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.draken.usagikit.bridge.DefaultUsagiMangaBridge
import org.draken.usagikit.bridge.UsagiMangaBridge
import org.draken.usagikit.context.DefaultUsagiLoaderContext
import org.draken.usagikit.loader.UsagiPluginClassLoader
import org.draken.usagikit.model.UsagiExtension
import org.draken.usagikit.model.UsagiSource
import tsuki.MangaLoaderContext
import tsuki.MangaParser
import tsuki.model.ContentType
import tsuki.model.Manga
import tsuki.model.MangaChapter
import tsuki.model.MangaListFilter
import tsuki.model.MangaPage
import tsuki.model.MangaSource
import tsuki.model.SortOrder
import java.io.File
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * UsagiKit provides a lightweight SDK and Manga Bridge to dynamically load and run
 * Usagi manga extension plugins (.jar / dex) and retrieve manga details, lists, and chapters.
 */
object UsagiKit {

    private val extensionRegistry = ConcurrentHashMap<String, UsagiExtension>()
    private val sourceRegistry = ConcurrentHashMap<String, UsagiSource>()
    private val factoryMethodMap = ConcurrentHashMap<String, Method>()
    private val bridgeCache = ConcurrentHashMap<String, UsagiMangaBridge>()

    private var appContext: Context? = null
    private var customLoaderContext: MangaLoaderContext? = null

    @Volatile
    var isInitialized: Boolean = false
        private set

    lateinit var okHttpClient: OkHttpClient
        private set

    /**
     * Initializes UsagiKit once in your Application.onCreate().
     */
    @Synchronized
    fun initialize(
        context: Context,
        customClient: OkHttpClient? = null,
        loaderContext: MangaLoaderContext? = null,
    ) {
        if (isInitialized) return

        val app = context.applicationContext
        appContext = app

        okHttpClient = customClient ?: OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(2, TimeUnit.MINUTES)
            .build()

        customLoaderContext = loaderContext ?: DefaultUsagiLoaderContext(app, okHttpClient)
        isInitialized = true
    }

    /**
     * Returns the active [MangaLoaderContext].
     */
    fun getLoaderContext(): MangaLoaderContext {
        check(isInitialized) { "UsagiKit is not initialized. Call UsagiKit.initialize(context) first." }
        return customLoaderContext!!
    }

    /**
     * Dynamically loads a Usagi extension plugin file (.jar) from storage.
     */
    suspend fun loadExtension(file: File): UsagiExtension = withContext(Dispatchers.IO) {
        check(isInitialized) { "UsagiKit is not initialized. Call UsagiKit.initialize(context) first." }
        require(file.exists() && file.isFile) { "Extension file does not exist: ${file.absolutePath}" }

        val context = appContext!!
        val codeCache = context.codeCacheDir
        
        file.setReadOnly()

        val tempJar = File(codeCache, "usagi_ext_${file.nameWithoutExtension}_${file.lastModified()}.jar")
        if (tempJar.exists()) {
            tempJar.setWritable(true, true)
            tempJar.delete()
        }

        try {
            file.copyTo(tempJar, overwrite = true)
            tempJar.setReadOnly()
        } catch (_: Throwable) {
            tempJar.delete()
        }

        val jarToLoad = if (tempJar.exists()) tempJar else file
        jarToLoad.setReadOnly()

        val classLoader = UsagiPluginClassLoader(
            dexPath = jarToLoad.absolutePath,
            optimizedDirectory = codeCache.absolutePath,
            librarySearchPath = null,
            parent = context.classLoader,
        )

        val (factoryClass, enumClass, ctxClass) = try {
            Triple(
                classLoader.loadClass("tsuki.MangaParserFactoryKt"),
                classLoader.loadClass("tsuki.model.MangaParserSource"),
                classLoader.loadClass("tsuki.MangaLoaderContext"),
            )
        } catch (_: ClassNotFoundException) {
            Triple(
                classLoader.loadClass("org.koitharu.kotatsu.parsers.MangaParserFactoryKt"),
                classLoader.loadClass("org.koitharu.kotatsu.parsers.model.MangaParserSource"),
                classLoader.loadClass("org.koitharu.kotatsu.parsers.MangaLoaderContext"),
            )
        }

        val factoryMethod = factoryClass.getMethod("newParser", enumClass, ctxClass)
        val loadedSources = mutableListOf<UsagiSource>()

        enumClass.enumConstants?.forEach { constant ->
            if (constant is MangaSource) {
                val uniqueId = "${file.name}:${constant.name}"
                val source = UsagiSource(
                    id = uniqueId,
                    name = constant.name,
                    title = constant.title,
                    locale = constant.locale,
                    contentType = constant.contentType,
                    isBroken = constant.isBroken,
                    jarName = file.name,
                    delegateConstant = constant,
                )
                sourceRegistry[uniqueId] = source
                sourceRegistry[constant.name] = source
                factoryMethodMap[uniqueId] = factoryMethod
                factoryMethodMap[constant.name] = factoryMethod
                loadedSources.add(source)
            }
        }

        val extension = UsagiExtension(
            name = file.name,
            file = file,
            sources = loadedSources,
            classLoader = classLoader,
        )

        extensionRegistry[file.name] = extension
        extension
    }

    /**
     * Loads all extension plugin files (.jar) from a given directory.
     */
    suspend fun loadExtensionsFromDir(dir: File): List<UsagiExtension> = withContext(Dispatchers.IO) {
        if (!dir.exists() || !dir.isDirectory) return@withContext emptyList()
        val jarFiles = dir.listFiles { f -> f.extension.equals("jar", ignoreCase = true) } ?: emptyArray()
        jarFiles.mapNotNull { file ->
            runCatching { loadExtension(file) }.getOrNull()
        }
    }

    /**
     * Gets a registered [UsagiSource] by its unique id or source name.
     */
    fun getSource(nameOrId: String): UsagiSource? = sourceRegistry[nameOrId]

    /**
     * Returns all registered Usagi manga sources.
     */
    fun getAllSources(): List<UsagiSource> = sourceRegistry.values.distinctBy { it.id }

    /**
     * Returns all loaded Usagi extensions.
     */
    fun getAllExtensions(): List<UsagiExtension> = extensionRegistry.values.toList()

    /**
     * Creates or retrieves a [MangaParser] instance for the given source.
     */
    fun getParser(source: MangaSource): MangaParser {
        val uSource = source as? UsagiSource ?: getSource(source.name)
            ?: throw IOException("Unsupported or unregistered source: ${source.name}")
        val method = factoryMethodMap[uSource.id] ?: factoryMethodMap[uSource.name]
            ?: throw IOException("Factory method not found for source: ${uSource.name}")

        val loaderCtx = getLoaderContext()
        return try {
            method.invoke(null, uSource.delegateConstant, loaderCtx) as MangaParser
        } catch (e: InvocationTargetException) {
            throw IOException("Failed to instantiate parser for ${uSource.name}: ${e.targetException.message}", e.targetException)
        }
    }

    /**
     * Creates or retrieves a [UsagiMangaBridge] for the given source name or ID.
     */
    fun getBridge(nameOrId: String): UsagiMangaBridge {
        val source = getSource(nameOrId) ?: throw IllegalArgumentException("Source $nameOrId not found")
        return bridgeCache.getOrPut(source.id) {
            val parser = getParser(source)
            DefaultUsagiMangaBridge(source, parser, getLoaderContext())
        }
    }

    /**
     * Creates or retrieves a [UsagiMangaBridge] for a given [MangaSource].
     */
    fun getBridge(source: MangaSource): UsagiMangaBridge {
        return bridgeCache.getOrPut(source.name) {
            val parser = getParser(source)
            DefaultUsagiMangaBridge(source, parser, getLoaderContext())
        }
    }

    // =========================================================================
    // Direct Manga Bridge operations (similar to MihonKit convenience methods)
    // =========================================================================

    fun getAvailableSortOrders(sourceName: String): Set<SortOrder> {
        return getBridge(sourceName).availableSortOrders
    }

    suspend fun getList(sourceName: String, page: Int = 1, sortOrder: SortOrder? = null, filter: MangaListFilter? = null): List<Manga> = withContext(Dispatchers.IO) {
        getBridge(sourceName).getList(page, sortOrder, filter)
    }

    suspend fun getPopularManga(sourceName: String, page: Int = 1): List<Manga> = withContext(Dispatchers.IO) {
        getBridge(sourceName).getPopularManga(page)
    }

    suspend fun getLatestManga(sourceName: String, page: Int = 1): List<Manga> = withContext(Dispatchers.IO) {
        getBridge(sourceName).getLatestManga(page)
    }

    suspend fun searchManga(sourceName: String, page: Int = 1, query: String = "", filter: MangaListFilter? = null): List<Manga> = withContext(Dispatchers.IO) {
        getBridge(sourceName).searchManga(page, query, filter)
    }

    suspend fun getDetails(sourceName: String, manga: Manga): Manga = withContext(Dispatchers.IO) {
        getBridge(sourceName).getDetails(manga)
    }

    suspend fun getPages(sourceName: String, chapter: MangaChapter): List<MangaPage> = withContext(Dispatchers.IO) {
        getBridge(sourceName).getPages(chapter)
    }

    suspend fun getPageUrl(sourceName: String, page: MangaPage): String = withContext(Dispatchers.IO) {
        getBridge(sourceName).getPageUrl(page)
    }
}
