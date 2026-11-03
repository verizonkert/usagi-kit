package app.usagi.tester

import android.content.Context
import dalvik.system.DexClassLoader
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Interceptor
import okhttp3.Response
import tsuki.InternalParsersApi
import tsuki.MangaLoaderContext
import tsuki.MangaParser
import tsuki.config.ConfigKey
import tsuki.core.AbstractMangaParser
import tsuki.model.*
import java.io.File
import java.io.FileOutputStream
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Isolated ClassLoader that loads plugin classes and legacy org.koitharu.kotatsu.parsers.*
 * directly from the JAR first, preventing collision with the host APK's classes.
 */
class IsolatedPluginClassLoader(
    dexPath: String,
    optimizedDirectory: String?,
    librarySearchPath: String?,
    private val hostClassLoader: ClassLoader
) : DexClassLoader(dexPath, optimizedDirectory, librarySearchPath, ClassLoader.getSystemClassLoader()) {

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        // 1. Check if already loaded in this DexClassLoader
        val loaded = findLoadedClass(name)
        if (loaded != null) {
            return loaded
        }

        // 2. Plugin-specific entry points and site parsers MUST come from plugin DEX
        if (name == "tsuki.MangaParserFactoryKt" ||
            name == "org.koitharu.kotatsu.parsers.MangaParserFactoryKt" ||
            name == "tsuki.model.MangaParserSource" ||
            name == "org.koitharu.kotatsu.parsers.model.MangaParserSource" ||
            name.contains(".site.") ||
            name.contains("Parser")) {
            try {
                return findClass(name)
            } catch (_: ClassNotFoundException) {
                // proceed if not in plugin DEX
            }
        }

        // 3. Shared framework/host classes MUST come from hostClassLoader
        if (name == "org.koitharu.kotatsu.parsers.MangaLoaderContext" ||
            name == "tsuki.MangaLoaderContext" ||
            name == "tsuki.MangaParser" ||
            name == "tsuki.core.AbstractMangaParser" ||
            name.startsWith("org.koitharu.kotatsu.parsers.config.") ||
            name.startsWith("tsuki.config.") ||
            (name.startsWith("tsuki.model.") && name != "tsuki.model.MangaParserSource")) {
            try {
                return hostClassLoader.loadClass(name)
            } catch (_: ClassNotFoundException) {
                // fallback
            }
        }

        // 4. Try loading from this plugin's DEX first
        try {
            return findClass(name)
        } catch (_: ClassNotFoundException) {
            // Not in JAR dex, proceed to fallback
        }

        // 5. Fallback to host APK classloader (for android.*, okhttp3.*, kotlin.*, etc.)
        return hostClassLoader.loadClass(name)
    }
}

class MangaPluginLoader(private val context: Context) {

    private var classLoader: ClassLoader? = null
    private var newParserMethod: Method? = null
    private var sourcesEnumClass: Class<*>? = null

    var lastError: String? = null
        private set

    /**
     * Loads a JAR file from an external URI (device storage / file picker)
     */
    fun loadPluginFromUri(uri: android.net.Uri): Boolean {
        lastError = null
        return try {
            val jarFile = File(context.filesDir, "custom_plugin.jar")
            val optDir = File(context.cacheDir, "dex_opt")
            optDir.mkdirs()

            if (jarFile.exists()) {
                jarFile.delete()
            }

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(jarFile).use { output ->
                    input.copyTo(output)
                }
            } ?: run {
                lastError = "Cannot open input stream from file picker URI"
                return false
            }

            jarFile.setReadOnly()
            initClassLoader(jarFile, optDir)
        } catch (e: Throwable) {
            e.printStackTrace()
            lastError = "${e::class.java.simpleName}: ${e.message ?: e.cause?.message ?: "Unknown error"}"
            false
        }
    }

    private fun initClassLoader(jarFile: File, optDir: File): Boolean {
        val loader = IsolatedPluginClassLoader(
            jarFile.absolutePath,
            optDir.absolutePath,
            null,
            context.classLoader
        )
        classLoader = loader

        val (factoryClass, enumClass) = try {
            // 1. First attempt: Legacy Kotatsu format
            val f = loader.loadClass("org.koitharu.kotatsu.parsers.MangaParserFactoryKt")
            val e = loader.loadClass("org.koitharu.kotatsu.parsers.model.MangaParserSource")
            f to e
        } catch (_: ClassNotFoundException) {
            // 2. Second attempt: Tsuki / Usagi-kit format
            val f = loader.loadClass("tsuki.MangaParserFactoryKt")
            val e = loader.loadClass("tsuki.model.MangaParserSource")
            f to e
        }

        sourcesEnumClass = enumClass
        newParserMethod = factoryClass.methods.first { it.name == "newParser" }
        return true
    }

    /**
     * Dynamically finds any available .jar inside assets and initializes it.
     */
    fun loadDefaultPluginFromAssets(): String? {
        return try {
            val assetJars = context.assets.list("")?.filter { it.endsWith(".jar", ignoreCase = true) } ?: emptyList()
            val targetJarName = assetJars.firstOrNull() ?: return null

            val jarFile = File(context.filesDir, targetJarName)
            val optDir = File(context.cacheDir, "dex_opt")
            optDir.mkdirs()

            if (!jarFile.exists()) {
                context.assets.open(targetJarName).use { input ->
                    FileOutputStream(jarFile).use { output ->
                        input.copyTo(output)
                    }
                }
                jarFile.setReadOnly()
            }

            if (initClassLoader(jarFile, optDir)) targetJarName else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Returns all source enum names inside the manga plugin
     */
    fun getAvailableSources(): List<String> {
        val enumClass = sourcesEnumClass ?: return emptyList()
        val values = enumClass.enumConstants as? Array<*> ?: return emptyList()
        return values.mapNotNull {
            (it as? Enum<*>)?.name ?: it?.toString()
        }
    }

    /**
     * Instantiates a real MangaParser from the loaded JAR!
     */
    fun createParser(sourceName: String, loaderContext: MangaLoaderContext): MangaParser {
        val enumClass = sourcesEnumClass ?: error("Plugin not loaded")
        val method = newParserMethod ?: error("Factory method not found")
        val loader = classLoader ?: error("ClassLoader not initialized")

        @Suppress("UNCHECKED_CAST")
        val sourceEnum = java.lang.Enum.valueOf(enumClass.asSubclass(Enum::class.java), sourceName)

        val contextParamType = method.parameterTypes[1]
        val contextArg = if (contextParamType.isInstance(loaderContext)) {
            loaderContext
        } else {
            createContextProxy(loader, contextParamType, loaderContext)
        }

        val rawParser = try {
            method.invoke(null, sourceEnum, contextArg)
        } catch (e: InvocationTargetException) {
            throw e.targetException ?: e
        } ?: error("Parser instantiation returned null")

        return if (rawParser is MangaParser) {
            rawParser
        } else {
            KotatsuParserAdapter(
                rawParser = rawParser,
                sourceName = sourceName,
                context = loaderContext,
                pluginClassLoader = loader
            )
        }
    }

    private fun createContextProxy(
        loader: ClassLoader,
        targetContextClass: Class<*>,
        hostContext: MangaLoaderContext
    ): Any {
        if (targetContextClass.isInterface) {
            return Proxy.newProxyInstance(
                loader,
                arrayOf(targetContextClass),
                InvocationHandler { _, m, args ->
                    val hostMethod = hostContext.javaClass.methods.firstOrNull {
                        it.name == m.name && it.parameterCount == m.parameterCount
                    } ?: return@InvocationHandler null
                    if (args == null) hostMethod.invoke(hostContext) else hostMethod.invoke(hostContext, *args)
                }
            )
        }

        return object : org.koitharu.kotatsu.parsers.MangaLoaderContext() {
            override val httpClient = hostContext.httpClient
            override val cookieJar = hostContext.cookieJar
            override fun newParserInstance(source: tsuki.model.MangaSource): tsuki.MangaParser = hostContext.newParserInstance(source)
            override fun getParserSources(): List<tsuki.model.MangaSource> = hostContext.getParserSources()
            override suspend fun evaluateJs(script: String): String? = hostContext.evaluateJs(script)
            override suspend fun evaluateJs(baseUrl: String, script: String): String? = hostContext.evaluateJs(baseUrl, script)
            override fun getConfig(source: tsuki.model.MangaSource): tsuki.config.MangaSourceConfig = hostContext.getConfig(source)
            override fun getDefaultUserAgent(): String = hostContext.getDefaultUserAgent()
            override fun redrawImageResponse(response: okhttp3.Response, redraw: (tsuki.bitmap.Bitmap) -> tsuki.bitmap.Bitmap): okhttp3.Response =
                hostContext.redrawImageResponse(response, redraw)
            override fun createBitmap(width: Int, height: Int): tsuki.bitmap.Bitmap = hostContext.createBitmap(width, height)
        }
    }
}

/**
 * Robust adapter bridging legacy org.koitharu.kotatsu.parsers.MangaParser instances to tsuki.MangaParser.
 * Safely converts models (SortOrder, Manga, MangaChapter, MangaPage) across ClassLoader boundaries.
 */
@OptIn(InternalParsersApi::class)
class KotatsuParserAdapter(
    private val rawParser: Any,
    sourceName: String,
    context: MangaLoaderContext,
    private val pluginClassLoader: ClassLoader
) : AbstractMangaParser(
    context = context,
    source = object : MangaSource {
        override val name: String = sourceName
    }
) {
    private val rawMangaCache = ConcurrentHashMap<Long, Any>()
    private val rawChapterCache = ConcurrentHashMap<Long, Any>()
    private val rawPageCache = ConcurrentHashMap<Long, Any>()

    override val configKeyDomain: ConfigKey.Domain by lazy {
        try {
            val d = rawParser.javaClass.getMethod("getDomain").invoke(rawParser) as? String ?: ""
            ConfigKey.Domain(d.ifEmpty { "unknown" })
        } catch (_: Exception) {
            ConfigKey.Domain("unknown")
        }
    }

    override val availableSortOrders: Set<SortOrder> by lazy {
        try {
            val m = rawParser.javaClass.getMethod("getAvailableSortOrders")
            val set = m.invoke(rawParser) as? Set<*> ?: emptySet<Any>()
            set.mapNotNull { item ->
                val name = (item as? Enum<*>)?.name ?: item?.toString()
                name?.let { runCatching { SortOrder.valueOf(it) }.getOrNull() }
            }.toSet().ifEmpty { setOf(SortOrder.POPULARITY) }
        } catch (_: Exception) {
            setOf(SortOrder.POPULARITY)
        }
    }

    private fun getProperty(obj: Any, name: String): Any? {
        val clazz = obj.javaClass
        // 1. Try getter method getXyz()
        val getterName = "get" + name.replaceFirstChar { it.uppercase() }
        try {
            return clazz.getMethod(getterName).invoke(obj)
        } catch (_: Throwable) {}

        // 2. Try getter method isXyz()
        val isGetterName = "is" + name.replaceFirstChar { it.uppercase() }
        try {
            return clazz.getMethod(isGetterName).invoke(obj)
        } catch (_: Throwable) {}

        // 3. Try method with exact property name
        try {
            return clazz.getMethod(name).invoke(obj)
        } catch (_: Throwable) {}

        // 4. Try public field (crucial for Kotlin @JvmField!)
        try {
            return clazz.getField(name).get(obj)
        } catch (_: Throwable) {}

        // 5. Try declared field
        try {
            val f = clazz.getDeclaredField(name)
            f.isAccessible = true
            return f.get(obj)
        } catch (_: Throwable) {}

        return null
    }

    override val filterCapabilities: MangaListFilterCapabilities by lazy {
        try {
            val m = rawParser.javaClass.getMethod("getFilterCapabilities")
            val rawCap = m.invoke(rawParser)
            if (rawCap != null) {
                MangaListFilterCapabilities(
                    isSearchSupported = getProperty(rawCap, "isSearchSupported") as? Boolean ?: true,
                    isTagsExclusionSupported = getProperty(rawCap, "isTagsExclusionSupported") as? Boolean ?: false,
                    isMultipleTagsSupported = getProperty(rawCap, "isMultipleTagsSupported") as? Boolean ?: true,
                    isSearchWithFiltersSupported = getProperty(rawCap, "isSearchWithFiltersSupported") as? Boolean ?: true,
                    isOriginalLocaleSupported = getProperty(rawCap, "isOriginalLocaleSupported") as? Boolean ?: false,
                    isYearSupported = getProperty(rawCap, "isYearSupported") as? Boolean ?: false,
                )
            } else {
                MangaListFilterCapabilities()
            }
        } catch (_: Throwable) {
            MangaListFilterCapabilities()
        }
    }

    override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
    }

    override suspend fun getRelatedManga(seed: Manga): List<Manga> = emptyList()

    override suspend fun getFavicons(): Favicons = Favicons(emptyList(), null)

    override suspend fun getFilterOptions(): MangaListFilterOptions {
        return try {
            val m = rawParser.javaClass.methods.firstOrNull { it.name == "getFilterOptions" && it.parameterTypes.size == 1 }
                ?: return MangaListFilterOptions()
            val rawOpts = invokeSuspend1(m, rawParser) ?: return MangaListFilterOptions()

            val rawTags = getProperty(rawOpts, "availableTags") as? Set<*>
            val tags = rawTags?.mapNotNull { t ->
                if (t == null) return@mapNotNull null
                val title = getProperty(t, "title") as? String ?: t.toString()
                val key = getProperty(t, "key") as? String ?: title
                tsuki.model.MangaTag(title = title, key = key, source = source)
            }?.toSet() ?: emptySet()

            val rawStates = getProperty(rawOpts, "availableStates") as? Set<*>
            val states = rawStates?.mapNotNull { s ->
                val name = (s as? Enum<*>)?.name ?: s?.toString()
                name?.let { runCatching { tsuki.model.MangaState.valueOf(it) }.getOrNull() }
            }?.toSet() ?: emptySet()

            val rawRatings = getProperty(rawOpts, "availableContentRating") as? Set<*>
            val ratings = rawRatings?.mapNotNull { r ->
                val name = (r as? Enum<*>)?.name ?: r?.toString()
                name?.let { runCatching { tsuki.model.ContentRating.valueOf(it) }.getOrNull() }
            }?.toSet() ?: emptySet()

            val rawDemographics = getProperty(rawOpts, "availableDemographics") as? Set<*>
            val demographics = rawDemographics?.mapNotNull { d ->
                val name = (d as? Enum<*>)?.name ?: d?.toString()
                name?.let { runCatching { tsuki.model.Demographic.valueOf(it) }.getOrNull() }
            }?.toSet() ?: emptySet()

            val rawContentTypes = getProperty(rawOpts, "availableContentTypes") as? Set<*>
            val contentTypes = rawContentTypes?.mapNotNull { c ->
                val name = (c as? Enum<*>)?.name ?: c?.toString()
                name?.let { runCatching { tsuki.model.ContentType.valueOf(it) }.getOrNull() }
            }?.toSet() ?: emptySet()

            val rawLocales = getProperty(rawOpts, "availableLocales") as? Set<*>
            val locales = rawLocales?.mapNotNull { it as? java.util.Locale }?.toSet() ?: emptySet()

            MangaListFilterOptions(
                availableTags = tags,
                availableStates = states,
                availableContentRating = ratings,
                availableDemographics = demographics,
                availableContentTypes = contentTypes,
                availableLocales = locales
            )
        } catch (_: Exception) {
            MangaListFilterOptions()
        }
    }

    override suspend fun getList(offset: Int, order: SortOrder, filter: MangaListFilter): List<Manga> {
        val jarOrderClass = pluginClassLoader.loadClass("org.koitharu.kotatsu.parsers.model.SortOrder")
        val jarOrder = java.lang.Enum.valueOf(jarOrderClass.asSubclass(Enum::class.java), order.name)

        val jarFilterClass = pluginClassLoader.loadClass("org.koitharu.kotatsu.parsers.model.MangaListFilter")
        val jarFilterBuilderClass = pluginClassLoader.loadClass("org.koitharu.kotatsu.parsers.model.MangaListFilter\$Builder")
        val builder = jarFilterBuilderClass.getConstructor().newInstance()
        if (!filter.query.isNullOrBlank()) {
            builder.javaClass.getMethod("query", String::class.java).invoke(builder, filter.query)
        }
        val jarFilter = builder.javaClass.getMethod("build").invoke(builder)

        val method = rawParser.javaClass.methods.first {
            it.name == "getList" && it.parameterTypes.size == 4
        }

        val rawList = invokeSuspend3(method, rawParser, offset, jarOrder, jarFilter) as? List<*> ?: emptyList<Any>()
        return rawList.mapNotNull { rawItem ->
            if (rawItem == null) return@mapNotNull null
            val m = mapToManga(rawItem)
            rawMangaCache[m.id] = rawItem
            m
        }
    }

    override suspend fun getDetails(manga: Manga): Manga {
        val rawItem = rawMangaCache[manga.id]
        val method = rawParser.javaClass.methods.firstOrNull {
            it.name == "getDetails" && it.parameterTypes.size == 2
        } ?: return manga

        val rawDetailed = if (rawItem != null) {
            invokeSuspend1(method, rawParser, rawItem)
        } else {
            null
        } ?: return manga

        rawMangaCache[manga.id] = rawDetailed
        return mapToManga(rawDetailed)
    }

    override suspend fun getPages(chapter: MangaChapter): List<MangaPage> {
        val rawChapter = rawChapterCache[chapter.id] ?: return emptyList()
        val method = rawParser.javaClass.methods.firstOrNull {
            it.name == "getPages" && it.parameterTypes.size == 2
        } ?: return emptyList()

        val rawPages = invokeSuspend1(method, rawParser, rawChapter) as? List<*> ?: return emptyList()
        return rawPages.mapNotNull { rawPage ->
            if (rawPage == null) return@mapNotNull null
            val id = (getProperty(rawPage, "id") as? Number)?.toLong() ?: 0L
            val url = getProperty(rawPage, "url") as? String ?: ""
            val preview = getProperty(rawPage, "preview") as? String
            rawPageCache[id] = rawPage
            MangaPage(id = id, url = url, preview = preview, source = source)
        }
    }

    override suspend fun getPageUrl(page: MangaPage): String {
        val rawPage = rawPageCache[page.id] ?: return page.url
        val method = rawParser.javaClass.methods.firstOrNull {
            it.name == "getPageUrl" && it.parameterTypes.size == 2
        } ?: return page.url
        return invokeSuspend1(method, rawParser, rawPage) as? String ?: page.url
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        return if (rawParser is Interceptor) {
            rawParser.intercept(chain)
        } else {
            chain.proceed(chain.request())
        }
    }

    private fun mapToManga(raw: Any): Manga {
        fun get(prop: String): Any? = getProperty(raw, prop)

        val id = (get("id") as? Number)?.toLong() ?: 0L
        val title = get("title") as? String ?: ""
        val altTitle = get("altTitle") as? String
        val altTitlesRaw = get("altTitles") as? Set<*>
        val altTitles = altTitlesRaw?.mapNotNull { it as? String }?.toSet() ?: altTitle?.let { setOf(it) } ?: emptySet()
        val url = get("url") as? String ?: ""
        val publicUrl = get("publicUrl") as? String ?: url
        val rating = (get("rating") as? Number)?.toFloat() ?: -1f
        val isNsfw = (get("isNsfw") as? Boolean) ?: (get("nsfw") as? Boolean) ?: false
        val coverUrl = get("coverUrl") as? String
        val largeCoverUrl = get("largeCoverUrl") as? String
        val description = get("description") as? String
        val author = get("author") as? String
        val authorsRaw = get("authors") as? Set<*>
        val authors = authorsRaw?.mapNotNull { it as? String }?.toSet() ?: author?.let { setOf(it) } ?: emptySet()

        val rawState = get("state") as? Enum<*>
        val state = rawState?.let {
            runCatching { tsuki.model.MangaState.valueOf(it.name) }.getOrNull()
        }

        val rawTags = get("tags") as? Collection<*>
        val tags = rawTags?.mapNotNull { t ->
            if (t == null) return@mapNotNull null
            val tTitle = getProperty(t, "title") as? String ?: t.toString()
            val tKey = getProperty(t, "key") as? String ?: tTitle
            tsuki.model.MangaTag(title = tTitle, key = tKey, source = source)
        }?.toSet() ?: emptySet()

        val rawChapters = get("chapters") as? List<*>
        val chapters = rawChapters?.mapNotNull { c ->
            if (c == null) return@mapNotNull null
            val cId = (getProperty(c, "id") as? Number)?.toLong() ?: 0L
            val cTitle = (getProperty(c, "title") as? String) ?: (getProperty(c, "name") as? String)
            val cNumber = (getProperty(c, "number") as? Number)?.toFloat() ?: 0f
            val cVolume = (getProperty(c, "volume") as? Number)?.toInt() ?: 0
            val cUrl = getProperty(c, "url") as? String ?: ""
            val cUploadDate = (getProperty(c, "uploadDate") as? Number)?.toLong() ?: 0L
            val cScanlator = getProperty(c, "scanlator") as? String
            val cBranch = getProperty(c, "branch") as? String
            rawChapterCache[cId] = c
            tsuki.model.MangaChapter(
                id = cId,
                title = cTitle,
                number = cNumber,
                volume = cVolume,
                url = cUrl,
                scanlator = cScanlator,
                uploadDate = cUploadDate,
                branch = cBranch,
                source = source
            )
        }

        return Manga(
            id = id,
            title = title,
            altTitles = altTitles,
            url = url,
            publicUrl = publicUrl,
            rating = rating,
            contentRating = null,
            coverUrl = coverUrl,
            tags = tags,
            state = state,
            authors = authors,
            largeCoverUrl = largeCoverUrl,
            description = description,
            chapters = chapters,
            source = source
        )
    }

    private suspend fun invokeSuspend1(method: Method, target: Any, arg1: Any? = null): Any? =
        suspendCancellableCoroutine { cont ->
            val rawCont = object : kotlin.coroutines.Continuation<Any?> {
                override val context = cont.context
                override fun resumeWith(result: Result<Any?>) {
                    if (result.isSuccess) cont.resume(result.getOrNull(), null)
                    else cont.resumeWithException(result.exceptionOrNull()!!)
                }
            }
            try {
                val res = if (arg1 != null) {
                    method.invoke(target, arg1, rawCont)
                } else {
                    method.invoke(target, rawCont)
                }
                if (res != kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED) {
                    cont.resume(res, null)
                }
            } catch (e: InvocationTargetException) {
                cont.resumeWithException(e.targetException ?: e)
            } catch (e: Throwable) {
                cont.resumeWithException(e)
            }
        }

    private suspend fun invokeSuspend3(method: Method, target: Any, arg1: Any, arg2: Any, arg3: Any): Any? =
        suspendCancellableCoroutine { cont ->
            val rawCont = object : kotlin.coroutines.Continuation<Any?> {
                override val context = cont.context
                override fun resumeWith(result: Result<Any?>) {
                    if (result.isSuccess) cont.resume(result.getOrNull(), null)
                    else cont.resumeWithException(result.exceptionOrNull()!!)
                }
            }
            try {
                val res = method.invoke(target, arg1, arg2, arg3, rawCont)
                if (res != kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED) {
                    cont.resume(res, null)
                }
            } catch (e: InvocationTargetException) {
                cont.resumeWithException(e.targetException ?: e)
            } catch (e: Throwable) {
                cont.resumeWithException(e)
            }
        }
}
