/**
 * Utility for extracting Next.js data from HTML or RSC responses.
 * Based on the Next.js extraction logic from the Keiyoushi repository.
 */
@file:JvmName("NextJsUtils")

package tsuki.util.json

import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.jsoup.nodes.Document
import tsuki.util.parseHtml

private val NEXT_F_REGEX = Regex("""self\.__next_f\.push\(\s*(\[.*])\s*\)\s*;?\s*$""", RegexOption.DOT_MATCHES_ALL)

/**
 * Recursively inspects a JSON tree ([JSONObject] or [JSONArray]) to find a node matching [predicate].
 */
private fun extractValueNextJs(
    payload: Any,
    predicate: (Any) -> Boolean,
): Any? {
    if (payload !is JSONObject && payload !is JSONArray) return null
    if (predicate(payload)) {
        return payload
    }

    if (payload is JSONObject) {
        val keys = payload.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val child = payload.opt(key)
            if (child != null) {
                val result = extractValueNextJs(child, predicate)
                if (result != null) return result
            }
        }
    } else if (payload is JSONArray) {
        for (i in 0 until payload.length()) {
            val child = payload.opt(i)
            if (child != null) {
                val result = extractValueNextJs(child, predicate)
                if (result != null) return result
            }
        }
    }
    return null
}

/**
 * Extracts React Server Component (RSC) streamed payloads from Next.js App Router (`self.__next_f.push`) inline scripts.
 */
private fun Document.extractAppRouterPayloads(): List<Any> = select("script:not([src])")
    .map { it.data() }
    .filter { "self.__next_f.push" in it }
    .flatMap { script ->
        try {
            val raw = NEXT_F_REGEX.find(script)?.groupValues?.get(1) ?: return@flatMap emptyList()
            val arr = JSONArray(raw)
            val content = arr.optString(1, null) ?: return@flatMap emptyList()

            extractRscPayloads(content)
        } catch (_: Exception) {
            emptyList()
        }
    }

/**
 * Extracts serialized state from Next.js Pages Router (`<script id="__NEXT_DATA__">`) elements.
 */
private fun Document.extractPagesRouterPayloads(): List<Any> {
    val data = selectFirst("script#__NEXT_DATA__")?.data() ?: return emptyList()
    return try {
        val root = JSONObject(data)
        val pageProps = root.optJSONObject("props")?.optJSONObject("pageProps")
        listOfNotNull(pageProps, root)
    } catch (_: Exception) {
        emptyList()
    }
}

/**
 * Parses React Server Component (RSC) chunked text format payloads into JSON objects or arrays.
 */
private fun extractRscPayloads(body: String): List<Any> {
    val results = mutableListOf<Any>()
    var pos = 0

    while (pos < body.length) {
        val colonIdx = body.indexOf(':', pos)
        if (colonIdx == -1) break

        val id = body.substring(pos, colonIdx)
        if (id.isEmpty() || !id.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) {
            pos++
            continue
        }

        pos = colonIdx + 1
        if (pos >= body.length) break

        if (body[pos] == 'T') {
            pos++
            val commaIdx = body.indexOf(',', pos)
            if (commaIdx == -1) break
            val byteLen = body.substring(pos, commaIdx).toIntOrNull(16) ?: break
            pos = commaIdx + 1
            var bytes = 0
            val start = pos
            while (pos < body.length && bytes < byteLen) {
                when {
                    body[pos].code < 0x80 -> bytes += 1
                    body[pos].code < 0x800 -> bytes += 2
                    Character.isHighSurrogate(body[pos]) -> {
                        bytes += 4
                        pos++
                    }
                    else -> bytes += 3
                }
                pos++
            }
            try {
                results.add(parseJsonType(body.substring(start, pos)))
            } catch (_: Exception) {}
        } else {
            val (element, end) = parseJsonAt(body, pos)
            if (element != null) results.add(element)
            pos = end
        }
    }

    return results
}

/**
 * Parses a JSON string into either a [JSONObject] or [JSONArray].
 */
private fun parseJsonType(jsonStr: String): Any {
    val trimmed = jsonStr.trim()
    return when {
        trimmed.startsWith("{") -> JSONObject(trimmed)
        trimmed.startsWith("[") -> JSONArray(trimmed)
        else -> throw JSONException("Not a JSONObject or JSONArray")
    }
}

/**
 * Parses the first balanced JSON structure found starting at [start].
 */
private fun parseJsonAt(body: String, start: Int): Pair<Any?, Int> {
    if (start >= body.length) return Pair(null, start)

    var depth = 0
    var inString = false
    var escape = false
    var i = start

    while (i < body.length) {
        val c = body[i++]
        if (escape) {
            escape = false
            continue
        }
        if (c == '\\' && inString) {
            escape = true
            continue
        }
        if (c == '"') {
            inString = !inString
            continue
        }
        if (inString) continue
        when (c) {
            '{', '[' -> depth++
            '}', ']' -> if (--depth == 0) {
                return try {
                    Pair(parseJsonType(body.substring(start, i)), i)
                } catch (_: Exception) {
                    Pair(null, i)
                }
            }
        }
        if (depth == 0 && c.isWhitespace()) {
            return try {
                Pair(parseJsonType(body.substring(start, i - 1)), i)
            } catch (_: Exception) {
                Pair(null, i)
            }
        }
    }
    return Pair(null, i)
}

/**
 * Searches and extracts Next.js state (from both App Router RSC chunks and Pages Router `__NEXT_DATA__`)
 * that matches the given [predicate].
 *
 * @param predicate Matching rule applied to each JSON payload node.
 * @return The first matched JSON node, or `null` if none found.
 */
public fun Document.extractNextJs(
    predicate: (Any) -> Boolean,
): Any? {
    val payloads = extractAppRouterPayloads().ifEmpty { extractPagesRouterPayloads() }
    for (payload in payloads) {
        val result = extractValueNextJs(payload, predicate)
        if (result != null) return result
    }
    return null
}

/**
 * Searches and extracts typed Next.js data matching [predicate] from this HTML document.
 */
public inline fun <reified T : Any> Document.extractNextJsTyped(
    noinline predicate: (Any) -> Boolean,
): T? = extractNextJs(predicate) as? T

/**
 * Parses raw Next.js React Server Component (RSC) chunk responses and extracts data matching [predicate].
 */
public fun String.extractNextJsRsc(
    predicate: (Any) -> Boolean,
): Any? {
    for (payload in extractRscPayloads(this)) {
        val result = extractValueNextJs(payload, predicate)
        if (result != null) return result
    }
    return null
}

/**
 * Parses raw Next.js React Server Component (RSC) chunk responses and extracts typed data of type [T].
 */
public inline fun <reified T : Any> String.extractNextJsRscTyped(
    noinline predicate: (Any) -> Boolean,
): T? = extractNextJsRsc(predicate) as? T

/**
 * Extracts Next.js data from an OkHttp [Response], automatically differentiating between
 * HTML pages (`text/html`) and RSC chunks (`text/x-component`).
 *
 * @param predicate Matching rule for finding the desired JSON node.
 * @return Matched JSON object or array, or `null`.
 */
public fun Response.extractNextJs(
    predicate: (Any) -> Boolean,
): Any? = use { response ->
    val contentType = response.header("Content-Type") ?: ""
    return when {
        "text/x-component" in contentType -> response.body.string().extractNextJsRsc(predicate)
        "text/html" in contentType -> response.parseHtml().extractNextJs(predicate)
        else -> error("Unsupported Content-Type for Next.js extraction: $contentType")
    }
}

/**
 * Extracts typed Next.js data of type [T] from an OkHttp [Response].
 */
public inline fun <reified T : Any> Response.extractNextJsTyped(
    noinline predicate: (Any) -> Boolean,
): T? = extractNextJs(predicate) as? T
