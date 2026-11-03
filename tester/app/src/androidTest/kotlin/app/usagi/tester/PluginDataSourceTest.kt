package app.usagi.tester

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import tsuki.model.MangaListFilter
import tsuki.model.SortOrder
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class PluginDataSourceTest {

    @Test
    fun testAnimePhoenixRealData() = runBlocking {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val pluginLoader = MangaPluginLoader(appContext)

        // 1. Ensure hany18h.jar is copied into internal filesDir
        val targetJar = File(appContext.filesDir, "custom_plugin.jar")
        val optDir = File(appContext.cacheDir, "dex_opt")
        optDir.mkdirs()

        // Read pushed jar from /data/local/tmp/hany18h.jar or sdcard
        val sourceJar = File("/data/local/tmp/hany18h.jar")
        assertTrue("Source JAR must exist on device at ${sourceJar.absolutePath}", sourceJar.exists())
        
        sourceJar.inputStream().use { input ->
            FileOutputStream(targetJar).use { output ->
                input.copyTo(output)
            }
        }
        targetJar.setReadOnly()

        val uri = android.net.Uri.fromFile(targetJar)
        val loaded = pluginLoader.loadPluginFromUri(uri)
        assertTrue("Plugin should load successfully: ${pluginLoader.lastError}", loaded)

        val sources = pluginLoader.getAvailableSources()
        println("=== AVAILABLE SOURCES IN JAR (${sources.size}) ===")
        println(sources.joinToString(", "))
        assertTrue("ANIME_PHOENIX should be in available sources", sources.contains("ANIME_PHOENIX"))

        // 2. Instantiate parser for ANIME_PHOENIX
        val loaderContext = TestMangaLoaderContext()
        val parser = pluginLoader.createParser("ANIME_PHOENIX", loaderContext)
        assertNotNull("Parser instance should not be null", parser)

        // 3. Fetch real live manga/anime list
        println("=== FETCHING REAL TITLES FROM ANIME_PHOENIX (SortOrder.POPULARITY) ===")
        val animeList = try {
            parser.getList(offset = 0, order = SortOrder.POPULARITY, filter = MangaListFilter())
        } catch (e: Exception) {
            println("Fetch with POPULARITY failed (${e.message}), trying SortOrder.UPDATED...")
            parser.getList(offset = 0, order = SortOrder.UPDATED, filter = MangaListFilter())
        }

        println("Found ${animeList.size} titles:")
        animeList.take(10).forEachIndexed { index, manga ->
            println("${index + 1}. [${manga.id}] ${manga.title}")
            println("   URL: ${manga.url}")
            println("   Public URL: ${manga.publicUrl}")
            println("   Cover: ${manga.coverUrl}")
        }

        assertTrue("Should return at least 1 manga/anime title from ANIME_PHOENIX", animeList.isNotEmpty())

        // 4. Fetch details for the first anime
        val firstAnime = animeList.first()
        println("=== FETCHING DETAILS FOR: ${firstAnime.title} ===")
        val details = parser.getDetails(firstAnime)
        println("Title: ${details.title}")
        println("Description: ${details.description?.take(150)}...")
        println("State: ${details.state}")
        println("Rating: ${details.rating}")
        println("Total Episodes/Chapters: ${details.chapters?.size ?: 0}")

        details.chapters?.take(5)?.forEachIndexed { i, ep ->
            println("  Episode ${i + 1}: ${ep.title} (Number: ${ep.number}) -> ${ep.url}")
        }

        assertNotNull("Anime title should not be null", details.title)
    }
}
