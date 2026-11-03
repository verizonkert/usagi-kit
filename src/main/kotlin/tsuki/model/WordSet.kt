package tsuki.model

import tsuki.InternalParsersApi
import java.util.Locale

/**
 * A fast, case-insensitive string matcher optimized for date parsing, token checking,
 * and keyword matching in manga sources.
 *
 * Words provided to the set are sanitized, deduplicated, and pre-normalized for high-throughput queries.
 */
@InternalParsersApi
public class WordSet(
    private vararg val words: String
) : Set<String> {

    // Cleaned, lowercased set for fast O(1) exact lookups and deduplication
    private val normalizedWords: Set<String> = words
        .mapNotNull { it.trim().lowercase(Locale.ROOT).ifEmpty { null } }
        .toSet()

    // Store array of lowercased tokens for fast sequential substring scanning without iterator allocations
    private val lowerWordsArray: Array<String> = normalizedWords.toTypedArray()

    /**
     * Checks if any word in this set appears anywhere as a substring within [dateString].
     * Parameter name retained for binary and named-argument compatibility.
     *
     * @param dateString The input string to inspect.
     * @return `true` if any word matches as a substring, `false` otherwise.
     */
    public fun anyWordIn(dateString: String): Boolean {
        if (dateString.isEmpty() || lowerWordsArray.isEmpty()) return false
        val lowerText = dateString.lowercase(Locale.ROOT)
        for (word in lowerWordsArray) {
            if (lowerText.contains(word)) return true
        }
        return false
    }

    /**
     * Checks if [dateString] begins with any word in this set, ignoring case.
     * Parameter name retained for binary and named-argument compatibility.
     *
     * @param dateString The input string to inspect.
     * @return `true` if the string starts with any word, `false` otherwise.
     */
    public fun startsWith(dateString: String): Boolean {
        if (dateString.isEmpty() || lowerWordsArray.isEmpty()) return false
        val lowerText = dateString.lowercase(Locale.ROOT)
        for (word in lowerWordsArray) {
            if (lowerText.startsWith(word)) return true
        }
        return false
    }

    /**
     * Checks if [dateString] ends with any word in this set, ignoring case.
     * Parameter name retained for binary and named-argument compatibility.
     *
     * @param dateString The input string to inspect.
     * @return `true` if the string ends with any word, `false` otherwise.
     */
    public fun endsWith(dateString: String): Boolean {
        if (dateString.isEmpty() || lowerWordsArray.isEmpty()) return false
        val lowerText = dateString.lowercase(Locale.ROOT)
        for (word in lowerWordsArray) {
            if (lowerText.endsWith(word)) return true
        }
        return false
    }

    /**
     * Checks if any word in this set matches a standalone whole word in [text]
     * (delimited by spaces or punctuation), preventing false positives like "m" matching "month".
     *
     * @param text The input string to tokenize and check.
     * @return `true` if any standalone word matches an entry in this set.
     */
    public fun matchesWholeWord(text: String): Boolean {
        if (text.isEmpty() || normalizedWords.isEmpty()) return false
        return text.splitToSequence(' ', '\t', '\n', ',', '.', '-', '/')
            .filter { it.isNotEmpty() }
            .any { it.lowercase(Locale.ROOT) in normalizedWords }
    }

    // --- Standard Set<String> Implementation (O(1) exact contains) ---

    override val size: Int
        get() = normalizedWords.size

    override fun isEmpty(): Boolean = normalizedWords.isEmpty()

    override fun contains(element: String): Boolean =
        normalizedWords.contains(element.lowercase(Locale.ROOT))

    override fun containsAll(elements: Collection<String>): Boolean =
        elements.all { contains(it) }

    override fun iterator(): Iterator<String> = normalizedWords.iterator()
}
