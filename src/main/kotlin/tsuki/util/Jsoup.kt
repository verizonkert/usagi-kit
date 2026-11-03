@file:JvmName("JsoupUtils")

package tsuki.util

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.jsoup.select.QueryParser
import org.jsoup.select.Selector
import tsuki.InternalParsersApi
import tsuki.exception.ParseException
import kotlin.contracts.contract

/**
 * Returns the host name derived from this element's base URI, or `null` if empty or malformed.
 */
public val Element.host: String?
	get() {
		val uri = baseUri()
		return if (uri.isEmpty()) {
			null
		} else {
			uri.toHttpUrlOrNull()?.host
		}
	}

/**
 * Returns an attribute value trimmed, or `null` if it is missing or blank.
 *
 * @see Element.attr which returns an empty string instead of null.
 */
public fun Element.attrOrNull(attributeKey: String): String? = attr(attributeKey).takeUnless { it.isBlank() }?.trim()

/**
 * Returns an attribute value, or throws [ParseException] if the attribute does not exist on this element.
 *
 * @throws ParseException if [attributeKey] is missing.
 */
public fun Element.attrOrThrow(attributeKey: String): String = if (hasAttr(attributeKey)) {
	attr(attributeKey)
} else {
	throw ParseException("Attribute \"$attributeKey\" is missing at element \"$this\"", baseUri())
}

/**
 * Resolves an attribute value as a relative path URL, or `null` if missing, empty, or a `data:` URI.
 */
public fun Element.attrAsRelativeUrlOrNull(attributeKey: String): String? {
	val attr = attrOrNull(attributeKey) ?: return null
	if (attr.isEmpty() || attr.startsWith("data:")) {
		return null
	}
	if (attr.startsWith('/')) {
		return attr
	}
	val host = baseUri().toHttpUrlOrNull()?.host ?: return null
	return attr.substringAfter(host)
}

/**
 * Resolves an attribute value as a relative path URL, throwing an [IllegalArgumentException] if absent.
 */
public fun Element.attrAsRelativeUrl(attributeKey: String): String {
	return requireNotNull(attrAsRelativeUrlOrNull(attributeKey)) {
		"Cannot get relative url for $attributeKey: \"${attr(attributeKey)}\""
	}
}

/**
 * Resolves an attribute value as an absolute URL against this element's [baseUri], or `null` if missing or invalid.
 */
public fun Element.attrAsAbsoluteUrlOrNull(attributeKey: String): String? {
	val attr = attrOrNull(attributeKey) ?: return null
	if (attr.isEmpty() || attr.startsWith("data:")) {
		return null
	}
	return (baseUri().toHttpUrlOrNull()?.resolve(attr) ?: return null).toString()
}

/**
 * Resolves an attribute value as an absolute URL, throwing a [ParseException] if missing or unresolved.
 */
public fun Element.attrAsAbsoluteUrl(attributeKey: String): String {
	return parseNotNull(attrAsAbsoluteUrlOrNull(attributeKey)) {
		"Cannot get absolute url for $attributeKey: \"${attr(attributeKey)}\""
	}
}

/**
 * Extracts a CSS property value from this element's inline `style` attribute, or `null` if absent.
 */
public fun Element.styleValueOrNull(property: String): String? {
	val style = attrOrNull("style") ?: return null
	var startIndex = 0
	while (startIndex < style.length) {
		val propIndex = style.indexOf(property, startIndex, ignoreCase = true)
		if (propIndex == -1) return null

		val beforeOk = propIndex == 0 || style[propIndex - 1].isWhitespace() || style[propIndex - 1] == ';'
		val afterIndex = propIndex + property.length
		if (beforeOk && afterIndex < style.length) {
			var colonIndex = afterIndex
			while (colonIndex < style.length && style[colonIndex].isWhitespace()) {
				colonIndex++
			}
			if (colonIndex < style.length && style[colonIndex] == ':') {
				val semiIndex = style.indexOf(';', startIndex = colonIndex + 1)
				val rawVal = if (semiIndex == -1) {
					style.substring(colonIndex + 1)
				} else {
					style.substring(colonIndex + 1, semiIndex)
				}
				return rawVal.trim().nullIfEmpty()
			}
		}
		startIndex = propIndex + property.length
	}
	return null
}

/**
 * Selects the first child element matching [cssQuery], throwing [ParseException] with contextual details if absent.
 */
public fun Element.selectFirstOrThrow(cssQuery: String): Element = parseNotNull(Selector.selectFirst(cssQuery, this)) {
	"Cannot find \"$cssQuery\""
}

/**
 * Selects child elements matching [cssQuery], throwing [ParseException] if the resulting list is empty.
 */
public fun Element.selectOrThrow(cssQuery: String): Elements {
	return Selector.select(cssQuery, this).ifEmpty {
		throw ParseException("Empty result for \"$cssQuery\"", baseUri())
	}
}

/**
 * Finds an element by its ID attribute, throwing [ParseException] if missing.
 */
public fun Element.requireElementById(id: String): Element = parseNotNull(getElementById(id)) {
	"Cannot find \"#$id\""
}

/**
 * Selects the last child element matching [cssQuery], or `null` if none match.
 */
public fun Element.selectLast(cssQuery: String): Element? = select(cssQuery).lastOrNull()

/**
 * Selects the last child element matching [cssQuery], throwing [ParseException] if none match.
 */
public fun Element.selectLastOrThrow(cssQuery: String): Element = parseNotNull(selectLast(cssQuery)) {
	"Cannot find \"$cssQuery\""
}

/**
 * Returns trimmed text content of this element, or `null` if empty.
 */
public fun Element.textOrNull(): String? = text().nullIfEmpty()

/**
 * Returns combined trimmed text content of these elements, or `null` if empty.
 */
public fun Elements.textOrNull(): String? = text().nullIfEmpty()

/**
 * Returns own direct text (excluding child elements), or `null` if empty.
 */
public fun Element.ownTextOrNull(): String? = ownText().nullIfEmpty()

/**
 * Traverses up the ancestor chain to find the nearest parent element matching [query].
 */
public fun Element.selectFirstParent(query: String): Element? {
	val selector = QueryParser.parse(query)
	val parents = parents()
	val root = parents.lastOrNull() ?: return null
	return parents.firstOrNull {
		selector.matches(root, it)
	}
}

/**
 * Traverses up the ancestor chain to find the nearest parent matching [query], throwing [ParseException] if missing.
 */
public fun Element.selectFirstParentOrThrow(query: String): Element = parseNotNull(selectFirstParent(query)) {
	"Cannot find parent \"$query\""
}

/**
 * Returns the first non-empty attribute value from the provided candidates [names], or `null` if none match.
 */
public fun Element.attrOrNull(vararg names: String): String? {
	for (name in names) {
		val value = attr(name)
		if (value.isNotEmpty()) {
			return value.trim()
		}
	}
	return null
}

/**
 * Discovers and resolves an image URL from common image/lazy-loading attributes (`data-src`, `data-original`, `src`, etc.).
 */
@InternalParsersApi
@JvmOverloads
public fun Element.src(
	names: Array<String> = arrayOf(
		"data-src",
		"data-cfsrc",
		"data-original",
		"data-original-src",
		"data-cdn",
		"data-sizes",
		"data-lazy-src",
		"data-srcset",
		"original-src",
		"data-wpfc-original-src",
		"src",
	),
): String? {
	for (name in names) {
		val value = attrAsAbsoluteUrlOrNull(name)
		if (value != null) {
			return value
		}
	}
	return null
}

/**
 * Resolves the primary image source attribute via [src], throwing [ParseException] if absent.
 */
@InternalParsersApi
public fun Element.requireSrc(): String = parseNotNull(src()) {
	"Image src not found"
}

/**
 * Parses this element's CSS background styling into a [CSSBackground] sprite descriptor.
 */
public fun Element.backgroundOrNull(): CSSBackground? = CSSBackground.parse(this)

/**
 * Extracts the `content` attribute of an element identified by [itemprop] microdata.
 */
public fun Element.metaValue(itemprop: String): String? = getElementsByAttributeValue("itemprop", itemprop)
	.firstNotNullOfOrNull { element ->
		element.attrOrNull("content")
	}

/**
 * Extracts a URL from a CSS `url(...)` declaration.
 */
public fun String.cssUrl(): String? {
	val fromIndex = indexOf("url(")
	if (fromIndex == -1) {
		return null
	}
	val toIndex = indexOf(')', startIndex = fromIndex)
	return if (toIndex == -1) {
		null
	} else {
		substring(fromIndex + 4, toIndex).trim().trim('\'', '"').nullIfEmpty()
	}
}

/**
 * Asserts that [value] is non-null, throwing a [ParseException] with [lazyMessage] and the element's [baseUri] if null.
 */
public inline fun <T : Any> Element.parseNotNull(value: T?, lazyMessage: () -> String): T {
	contract {
		returns() implies (value != null)
	}

	if (value == null) {
		val message = lazyMessage()
		throw ParseException(message, baseUri())
	} else {
		return value
	}
}
