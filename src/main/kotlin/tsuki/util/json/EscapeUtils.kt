package tsuki.util.json

/**
 * Decodes JSON-encoded escape sequences in this string into their raw character representations.
 *
 * Supported escape sequences:
 * - `\\`, `\/`, `\"`, `\'`
 * - `\n`, `\r`, `\t`, `\b`
 * - Unicode escapes: `\uXXXX` (where XXXX is a 4-digit hexadecimal number)
 *
 * @return The unescaped plain string.
 * @throws IllegalArgumentException if an invalid escape sequence or incomplete unicode sequence is encountered.
 */
public fun String.unescapeJson(): String {
	val builder = StringBuilder()
	var i = 0
	while (i < length) {
		val delimiter = this[i]
		i++ // consume letter or backslash
		if (delimiter == '\\' && i < length) {
			val ch = this[i]
			i++

			when (ch) {
				'\\', '/', '"', '\'' -> builder.append(ch)
				'n' -> builder.append('\n')
				'r' -> builder.append('\r')
				't' -> builder.append('\t')
				'b' -> builder.append('\b')
				'u' -> {
					require(i + 4 <= length) { "Not enough unicode digits!" }
					var code = 0
					for (offset in 0 until 4) {
						val digit = Character.digit(this[i + offset], 16)
						require(digit != -1) { "Bad character in unicode escape" }
						code = (code shl 4) or digit
					}
					i += 4 // consume those four digits.
					builder.append(code.toChar())
				}

				else -> throw IllegalArgumentException("Illegal escape sequence: \\$ch")
			}
		} else {
			builder.append(delimiter)
		}
	}
	return builder.toString()
}
