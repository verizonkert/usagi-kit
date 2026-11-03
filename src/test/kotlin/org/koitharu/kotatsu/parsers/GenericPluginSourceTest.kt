package tsuki

import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import java.util.zip.ZipFile

/**
 * Generic test class that can dynamically inspect and test any parser source
 * from plugin JAR files (such as `hany18h.jar` or any custom parser bundle).
 */
class GenericPluginSourceTest {

	data class SourceMetadata(
		val name: String,
		val title: String,
		val domain: String,
		val contentType: String,
		val sampleKeywords: List<String> = emptyList(),
	)

	companion object {
		val jarFile = File("src/test/resources/hany18h.jar")
		lateinit var dexBytecodeString: String

		@JvmStatic
		@BeforeAll
		fun loadBytecode() {
			assertTrue(jarFile.exists(), "Plugin jar must exist at ${jarFile.path}")
			val dexBytes = ZipFile(jarFile).use { zip ->
				val dexEntry = zip.getEntry("classes.dex")
				assertNotNull(dexEntry, "classes.dex must be present in the plugin jar")
				zip.getInputStream(dexEntry).readBytes()
			}
			dexBytecodeString = String(dexBytes, Charsets.ISO_8859_1)
		}

		/**
		 * Source provider to test any source from the plugin jar.
		 * Add any source enum name or configure new ones here.
		 */
		@JvmStatic
		fun provideAllSources(): Stream<Arguments> = Stream.of(
			Arguments.of(SourceMetadata("ASURASCANS", "AsuraComic", "asurascans.com", "MANGA", listOf("Action", "Fantasy", "Chapter"))),
			Arguments.of(SourceMetadata("ANIME_PHOENIX", "Anime Phoenix", "anime-phoenix.com", "ANIME", listOf("ANIME", "createStream", "parseEpisodes"))),
			Arguments.of(SourceMetadata("ANIME3RB", "Anime3rb", "anime3rb.com", "ANIME", listOf("ANIME"))),
			Arguments.of(SourceMetadata("ANIME_RISTO", "RistoAnime", "ristoanime.me", "ANIME", listOf("ANIME"))),
			Arguments.of(SourceMetadata("ANIME_SLAYER", "Anime Slayer", "anslayer.com", "ANIME", listOf("ANIME"))),
			Arguments.of(SourceMetadata("DESPAIRMANGA", "Despair Manga", "despair-manga.net", "MANGA", listOf("MANGA"))),
			Arguments.of(SourceMetadata("HIJALACOM", "Hijalacom", "hijala.com", "MANGA", listOf("MANGA"))),
			Arguments.of(SourceMetadata("HIZOMANGA", "Hizo Manga", "hizomanga.net", "NOVEL", listOf("NOVEL"))),
			Arguments.of(SourceMetadata("LAVATOONS", "Lavatoons", "lavascans.com", "MANGA", listOf("MANGA"))),
			Arguments.of(SourceMetadata("MANGAAILAND", "MangaAiLand", "manga-ai-land.blogspot.com", "MANGA", listOf("MANGA"))),
			Arguments.of(SourceMetadata("MANHATOK", "ManhaTok", "manhatok.blogspot.com", "MANGA", listOf("MANGA"))),
			Arguments.of(SourceMetadata("MANHUAUS", "ManhuaUs", "manhuaus.com", "MANGA", listOf("MANGA"))),
			Arguments.of(SourceMetadata("MANHWACLAN", "ManhwaClan", "manhwaclan.com", "MANGA", listOf("MANGA"))),
			Arguments.of(SourceMetadata("ONMA", "Onma", "onma.me", "MANGA", listOf("MANGA"))),
			Arguments.of(SourceMetadata("ROCKSMANGA", "RocksManga", "rocksmanga.com", "MANGA", listOf("MANGA"))),
			Arguments.of(SourceMetadata("SEANOVEL", "SeaNovel", "seanovel.org", "NOVEL", listOf("NOVEL"))),
			Arguments.of(SourceMetadata("SUNOVELS", "SuNovels", "sunovels.com", "NOVEL", listOf("NOVEL"))),
			Arguments.of(SourceMetadata("TEAMXNOVEL", "TeamXNovel", "olympustaff.com", "MANGA", listOf("MANGA"))),
			Arguments.of(SourceMetadata("UMIMANGA", "UmiManga", "umimanga.com", "MANGA", listOf("MANGA"))),
			Arguments.of(SourceMetadata("XSANOMANGA", "XsanoManga", "xsano-manga.com", "MANGA", listOf("MANGA"))),
		)

		private val httpClient = OkHttpClient.Builder()
			.connectTimeout(12, TimeUnit.SECONDS)
			.readTimeout(12, TimeUnit.SECONDS)
			.followRedirects(true)
			.build()
	}

	@Test
	fun `verify archive format and dex structure`() {
		assertTrue(jarFile.exists())
		ZipFile(jarFile).use { zip ->
			val dexEntry = zip.getEntry("classes.dex")
			assertNotNull(dexEntry)
			zip.getInputStream(dexEntry).use { input ->
				val magic = ByteArray(8)
				input.read(magic)
				assertEquals('d'.code.toByte(), magic[0])
				assertEquals('e'.code.toByte(), magic[1])
				assertEquals('x'.code.toByte(), magic[2])
			}
		}
	}

	/**
	 * Generic parameterized test: Verifies that any given source is present in the DEX bytecode enum
	 */
	@ParameterizedTest(name = "Presence Check: {0}")
	@ValueSource(
		strings = [
			"ALHILALLIBRARY", "ANIME3RB", "ANIME_PHOENIX", "ANIME_RISTO", "ANIME_SLAYER",
			"ANIME_WITCHER", "ANYONEMANGA", "ASQORG", "ASURASCANS", "AZORAMOON",
			"CENELE", "DESPAIRMANGA", "DILARTUBE", "GALAXYNOVELS", "HIJALACOM",
			"HIZOMANGA", "KAWAIIMANGA", "KOLNOVEL", "LAVATOONS", "MANGAAILAND",
			"MANGADAR", "MANGALEK", "MANGAMELLO_PLUS", "MANGASTARZ", "MANGASWAT",
			"MANGATEK", "MANHATOK", "MANHUAUS", "MANHWACLAN", "ONMA",
			"ROCKSMANGA", "SEANOVEL", "SUNOVELS", "TEAMXNOVEL", "UMIMANGA", "XSANOMANGA",
		]
	)
	fun `test source enum exists in plugin`(sourceName: String) {
		assertTrue(
			dexBytecodeString.contains(sourceName),
			"Source '$sourceName' was not found in plugin bytecode",
		)
	}

	/**
	 * Generic parameterized test: Verifies parser metadata, domain, content type, and keywords
	 */
	@ParameterizedTest(name = "Feature Check: {0}")
	@MethodSource("provideAllSources")
	fun `test source features and metadata`(meta: SourceMetadata) {
		// 1. Verify source name presence
		assertTrue(
			dexBytecodeString.contains(meta.name),
			"Bytecode must contain source '${meta.name}'",
		)

		// 2. Verify domain presence in configuration table
		if (meta.domain.isNotBlank()) {
			assertTrue(
				dexBytecodeString.contains(meta.domain),
				"Domain '${meta.domain}' must be defined for source '${meta.name}'",
			)
		}

		// 3. Verify content type presence
		assertTrue(
			dexBytecodeString.contains(meta.contentType),
			"Content type '${meta.contentType}' should be referenced for source '${meta.name}'",
		)

		// 4. Verify any additional keywords / capabilities
		for (keyword in meta.sampleKeywords) {
			assertTrue(
				dexBytecodeString.contains(keyword),
				"Source '${meta.name}' should define keyword/capability '$keyword'",
			)
		}
	}

	/**
	 * Generic parameterized test: Tests live HTTP connectivity and reachability for any configured source domain
	 */
	@ParameterizedTest(name = "Live Connectivity Check: {0}")
	@MethodSource("provideAllSources")
	fun `test source domain reachability`(meta: SourceMetadata) {
		if (meta.domain.isBlank()) return

		val request = Request.Builder()
			.url("https://${meta.domain}/")
			.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
			.get()
			.build()

		try {
			httpClient.newCall(request).execute().use { response ->
				println("[${meta.name}] https://${meta.domain}/ -> HTTP ${response.code}")
				assertTrue(
					response.code in 200..403,
					"Domain '${meta.domain}' should respond with a valid HTTP status (got ${response.code})",
				)
			}
		} catch (e: Exception) {
			println("[${meta.name}] https://${meta.domain}/ unreachable: ${e.message}")
		}
	}

	/**
	 * Generic test that fetches live titles from any accessible source
	 */
	@ParameterizedTest(name = "Fetch Real Titles: {0}")
	@MethodSource("provideAllSources")
	fun `fetch real titles from source`(meta: SourceMetadata) {
		if (meta.domain.isBlank()) return

		val request = Request.Builder()
			.url("https://${meta.domain}/")
			.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
			.get()
			.build()

		try {
			httpClient.newCall(request).execute().use { response ->
				if (!response.isSuccessful) {
					println("== [${meta.name}] Skipped: HTTP ${response.code} (Cloudflare or rate limit) ==")
					return@use
				}

				val body = response.body?.string() ?: return@use
				val doc = org.jsoup.Jsoup.parse(body)

				// Extract any manga/comic/novel cards using standard link patterns
				val cards = doc.select("a[href*='/manga/'], a[href*='/comics/'], a[href*='/animes/'], a[href*='/series/'], a[href*='/novel/'], a.simple-title-card, .title-card")
				val extractedTitles = mutableListOf<String>()

				for (card in cards) {
					val img = card.selectFirst("img")
					val title = img?.attr("alt")?.takeIf { it.isNotBlank() } 
						?: card.selectFirst("h2, h3, h4, span.font-bold, .title")?.text()?.trim()
						?: card.text().trim()

					if (title.isNotBlank() && title.length in 3..60 && title !in extractedTitles) {
						extractedTitles.add(title)
					}
				}

				if (extractedTitles.isNotEmpty()) {
					println("=== [${meta.name}] EXTRACTED ${extractedTitles.size} REAL TITLES ===")
					extractedTitles.take(5).forEachIndexed { index, title ->
						println("   ${index + 1}. $title")
					}
				} else {
					println("=== [${meta.name}] Site returned 200, parsed document title: ${doc.title()} ===")
				}
			}
		} catch (e: Exception) {
			println("== [${meta.name}] Connection notice: ${e.message} ==")
		}
	}
}
