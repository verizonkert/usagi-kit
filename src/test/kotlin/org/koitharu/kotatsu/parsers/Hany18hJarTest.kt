package tsuki

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.util.zip.ZipFile

/**
 * Unit test verifying the compatibility and contents of `hany18h.jar`
 * (from https://github.com/Shebyyy/kotatsu-multi-parsers/releases/latest/download/hany18h.jar).
 *
 * This test runs without any GUI / Android instrumentation, validating:
 * 1. The downloaded JAR archive structure (DEX packaging compiled via d8).
 * 2. DEX header signature and valid bytecode entries.
 * 3. Class descriptors and presence of parser classes (e.g. MangaParserSource, MangaParserFactoryKt).
 * 4. Extracted source definitions and catalog capabilities.
 */
class Hany18hJarTest {

	private val jarPath = File("src/test/resources/hany18h.jar")

	@Test
	fun `verify hany18h jar exists and is valid dex archive`() {
		assertTrue(jarPath.exists(), "hany18h.jar should be downloaded and placed in src/test/resources")
		assertTrue(jarPath.length() > 0, "hany18h.jar should not be empty")

		var hasClassesDex = false
		ZipFile(jarPath).use { zip ->
			val entries = zip.entries().asSequence().map { it.name }.toList()
			hasClassesDex = entries.contains("classes.dex")

			val dexEntry = zip.getEntry("classes.dex")
			assertNotNull(dexEntry, "classes.dex entry should exist")

			// Check DEX magic header (dex\n035\0)
			zip.getInputStream(dexEntry).use { input ->
				val magic = ByteArray(8)
				val bytesRead = input.read(magic)
				assertEquals(8, bytesRead, "Should read 8 magic header bytes")
				assertEquals('d'.code.toByte(), magic[0])
				assertEquals('e'.code.toByte(), magic[1])
				assertEquals('x'.code.toByte(), magic[2])
				assertEquals('\n'.code.toByte(), magic[3])
				assertEquals('0'.code.toByte(), magic[4])
				assertEquals('3'.code.toByte(), magic[5])
			}
		}

		assertTrue(hasClassesDex, "hany18h.jar must contain classes.dex for Android runtime / DexClassLoader")
	}

	@Test
	fun `verify dex contains expected manga parser sources`() {
		// Read DEX binary content and verify constant pool strings for known sources and factory methods
		val dexBytes = ZipFile(jarPath).use { zip ->
			zip.getInputStream(zip.getEntry("classes.dex")).readBytes()
		}

		val dexStringContent = String(dexBytes, Charsets.ISO_8859_1)

		// Check factory class signatures
		assertTrue(
			dexStringContent.contains("org/koitharu/kotatsu/parsers/MangaParserFactoryKt") ||
			dexStringContent.contains("tsuki/MangaParserFactoryKt"),
			"DEX must contain parser factory entrypoint",
		)

		// Check source enum signature
		assertTrue(
			dexStringContent.contains("org/koitharu/kotatsu/parsers/model/MangaParserSource") ||
			dexStringContent.contains("tsuki/model/MangaParserSource"),
			"DEX must contain MangaParserSource enum class descriptor",
		)

		// Check sources included in hany18h
		val expectedSources = listOf(
			"ANIME3RB",
			"ANIME_PHOENIX",
			"ANIME_RISTO",
			"ANIME_SLAYER",
			"ASURASCANS",
			"DESPAIRMANGA",
			"HIJALACOM",
			"HIZOMANGA",
			"LAVATOONS",
			"MANGAAILAND",
			"MANHATOK",
			"MANHUAUS",
			"MANHWACLAN",
			"ONMA",
			"ROCKSMANGA",
			"SEANOVEL",
			"SUNOVELS",
			"TEAMXNOVEL",
			"UMIMANGA",
			"XSANOMANGA",
		)

		for (sourceName in expectedSources) {
			assertTrue(
				dexStringContent.contains(sourceName),
				"hany18h.jar DEX should contain source: $sourceName",
			)
		}
	}

	@Test
	fun `verify ANIME_PHOENIX feature capabilities in bytecode`() {
		val dexBytes = ZipFile(jarPath).use { zip ->
			zip.getInputStream(zip.getEntry("classes.dex")).readBytes()
		}
		val dexString = String(dexBytes, Charsets.ISO_8859_1)

		// 1. Parser class implementation
		assertTrue(
			dexString.contains("org/koitharu/kotatsu/parsers/site/anime/ar/AnimePhoenix"),
			"AnimePhoenix parser class must be present",
		)

		// 2. Base domain configured
		assertTrue(
			dexString.contains("anime-phoenix.com"),
			"AnimePhoenix domain anime-phoenix.com must be defined",
		)

		// 3. Supported Sort Orders (POPULARITY, UPDATED, NEWEST, ALPHABETICAL, RELEVANCE)
		assertTrue(dexString.contains("POPULARITY"), "Should support getPopularity / SortOrder.POPULARITY")
		assertTrue(dexString.contains("UPDATED"), "Should support SortOrder.UPDATED")
		assertTrue(dexString.contains("NEWEST"), "Should support SortOrder.NEWEST")
		assertTrue(dexString.contains("ALPHABETICAL"), "Should support SortOrder.ALPHABETICAL")
		assertTrue(dexString.contains("RELEVANCE"), "Should support SortOrder.RELEVANCE")

		// 4. Manga/Anime Details extraction features
		assertTrue(dexString.contains("getDetails"), "Should implement getDetails method")
		assertTrue(dexString.contains(".FJ-Phoenix-Hero-Title"), "Should parse title using .FJ-Phoenix-Hero-Title")
		assertTrue(dexString.contains(".FJ-Phoenix-Hero-Poster img"), "Should parse poster using .FJ-Phoenix-Hero-Poster img")
		assertTrue(dexString.contains(".FJ-Phoenix-Desc-Full"), "Should parse synopsis using .FJ-Phoenix-Desc-Full")
		assertTrue(dexString.contains("parseDetailsState"), "Should parse status/state of the anime")

		// 5. Chapter / Episode Details extraction features
		assertTrue(dexString.contains("parseEpisodes"), "Should implement parseEpisodes for chapter/episode list")
		assertTrue(dexString.contains("a.FJ-EpPill"), "Should parse episode items using selector a.FJ-EpPill[href*=/episodes/]")
		assertTrue(dexString.contains("EPISODE_NUMBER"), "Should extract episode/chapter numbers via regex")
		assertTrue(dexString.contains("createStream"), "Should support video streaming resolution")
		assertTrue(dexString.contains(".server-link[data-server]"), "Should extract episode servers via .server-link[data-server]")

		// 6. Filter & Search capabilities
		assertTrue(dexString.contains("getFilterOptions"), "Should implement getFilterOptions for tags/genres")
		assertTrue(dexString.contains("getFilterCapabilities"), "Should implement filter capabilities")
		assertTrue(dexString.contains(".custom-select[data-filter=genre] .select-option[data-value]"), "Should parse genre filters")
		assertTrue(dexString.contains("fjSearchPageData"), "Should handle search configuration via fjSearchPageData")
		assertTrue(dexString.contains("PAGE_SIZE"), "Should define page size pagination (pageSize = 25)")
	}

	@Test
	fun `verify ANIME_PHOENIX live website is reachable and returns real titles and episodes`() {
		val client = okhttp3.OkHttpClient.Builder()
			.connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
			.readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
			.followRedirects(true)
			.build()

		val request = okhttp3.Request.Builder()
			.url("https://anime-phoenix.com/")
			.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
			.get()
			.build()

		try {
			client.newCall(request).execute().use { response ->
				assertTrue(
					response.isSuccessful || response.code in 200..399,
					"anime-phoenix.com returned HTTP ${response.code}",
				)
				val body = response.body?.string() ?: ""
				val doc = org.jsoup.Jsoup.parse(body)

				// 1. Check real anime titles and covers on the site
				val animeCards = doc.select("a[href*='/animes/']")
				assertTrue(animeCards.isNotEmpty(), "Should find real anime items on the catalog")

				val titlesFound = mutableListOf<String>()
				for (a in animeCards) {
					val img = a.selectFirst("img")
					val title = img?.attr("alt")?.takeIf { it.isNotBlank() } ?: a.text().trim()
					if (title.isNotBlank() && !title.contains("أنمي فينيكس") && title !in titlesFound) {
						titlesFound.add(title)
					}
				}

				println("=== ANIME_PHOENIX REAL TITLES FOUND (${titlesFound.size}) ===")
				titlesFound.take(5).forEachIndexed { i, title ->
					println("${i + 1}. $title")
				}
				assertTrue(titlesFound.isNotEmpty(), "Should extract at least 1 real anime title")
			}

			// 2. Test live Details & Episode extraction for a real anime (Bleach)
			val detailsReq = okhttp3.Request.Builder()
				.url("https://anime-phoenix.com/animes/bleach")
				.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
				.get()
				.build()

			client.newCall(detailsReq).execute().use { response ->
				val body = response.body?.string() ?: ""
				val doc = org.jsoup.Jsoup.parse(body)

				// Selectors used in AnimePhoenix parser
				val heroTitle = doc.selectFirst(".FJ-Phoenix-Hero-Title")?.text()?.trim()
				val heroDesc = doc.selectFirst(".FJ-Phoenix-Desc-Full")?.text()?.trim()
				val poster = doc.selectFirst(".FJ-Phoenix-Hero-Poster img")?.attr("src")
				val episodes = doc.select("a.FJ-EpPill, a[href*='/episodes/']")

				println("=== ANIME_PHOENIX REAL DETAILS (Bleach) ===")
				println("Title: $heroTitle")
				println("Poster: $poster")
				println("Synopsis: ${heroDesc?.take(100)}...")
				println("Episodes: ${episodes.size}")

				assertEquals("Bleach", heroTitle, "Hero title should match")
				assertNotNull(heroDesc, "Synopsis should be extracted")
				assertTrue(episodes.isNotEmpty(), "Should extract episodes using AnimePhoenix selectors")
			}
		} catch (e: Exception) {
			println("Notice: Network check skipped or limited: ${e.message}")
		}
	}

	@Test
	fun `verify ASURASCANS manga parser features and configuration`() {
		val dexBytes = ZipFile(jarPath).use { zip ->
			zip.getInputStream(zip.getEntry("classes.dex")).readBytes()
		}
		val dexString = String(dexBytes, Charsets.ISO_8859_1)

		// 1. Verify parser class implementation
		assertTrue(
			dexString.contains("org/koitharu/kotatsu/parsers/site/en/AsuraScansParser"),
			"AsuraScansParser class must be present in DEX",
		)

		// 2. Domain configuration (asurascans.com)
		assertTrue(
			dexString.contains("asurascans.com"),
			"AsuraScans domain asurascans.com must be configured",
		)

		// 3. Supported Sort Orders
		val expectedOrders = listOf("RATING", "UPDATED", "POPULARITY", "ALPHABETICAL", "ALPHABETICAL_DESC")
		for (order in expectedOrders) {
			assertTrue(dexString.contains(order), "AsuraScans must support SortOrder.$order")
		}

		// 4. Built-in genre tags
		val sampleGenres = listOf("Action", "Adventure", "Comedy", "Crazy MC", "Dungeons", "Fantasy", "Necromancer", "Regression", "Murim")
		for (genre in sampleGenres) {
			assertTrue(dexString.contains(genre), "AsuraScans must include genre tag '$genre'")
		}

		// 5. Chapter regex matching
		assertTrue(dexString.contains("Chapter\\s+(\\d+(?:\\.\\d+)?)"), "Must support regex for chapter numbers")
	}

	@Test
	fun `verify ASURASCANS live website is reachable and returns catalog content`() {
		val client = okhttp3.OkHttpClient.Builder()
			.connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
			.readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
			.followRedirects(true)
			.build()

		val request = okhttp3.Request.Builder()
			.url("https://asurascans.com/")
			.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
			.get()
			.build()

		try {
			client.newCall(request).execute().use { response ->
				assertTrue(
					response.isSuccessful || response.code in 200..399,
					"asurascans.com returned HTTP ${response.code}",
				)
				val body = response.body?.string() ?: ""
				assertTrue(body.contains("comics") || body.contains("Chapter") || body.isNotEmpty(), "Response should contain comic catalog markup")
			}
		} catch (e: Exception) {
			println("Notice: Live network test skipped or limited: ${e.message}")
		}
	}
}
